#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::net::TcpStream;
use std::sync::Mutex;
use std::time::Duration;

use tauri::{
    menu::{Menu, MenuItem},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    Manager, RunEvent, WindowEvent,
};
use tauri_plugin_shell::process::CommandChild;
use tauri_plugin_shell::ShellExt;

const PORT: u16 = 7867;

struct Backend(Mutex<Option<CommandChild>>);

fn backend_up() -> bool {
    let addr = format!("127.0.0.1:{PORT}");
    match addr.parse() {
        Ok(sock) => TcpStream::connect_timeout(&sock, Duration::from_millis(250)).is_ok(),
        Err(_) => false,
    }
}

fn show_main(app: &tauri::AppHandle) {
    if let Some(window) = app.get_webview_window("main") {
        let _ = window.show();
        let _ = window.unminimize();
        let _ = window.set_focus();
    }
}

fn quit_app(app: &tauri::AppHandle) {
    if let Some(child) = app.state::<Backend>().0.lock().unwrap().take() {
        let _ = child.kill();
    }
    app.exit(0);
}

fn spawn_backend(app: &tauri::App) {
    if backend_up() {
        println!("[voxeldash] backend already running on :{PORT}, reusing it");
        return;
    }

    let data_dir = match app.path().app_local_data_dir() {
        Ok(dir) => dir.join("data"),
        Err(e) => {
            eprintln!("[voxeldash] cannot resolve data dir: {e}");
            return;
        }
    };
    let _ = std::fs::create_dir_all(&data_dir);

    let ui_dir = match app.path().resource_dir() {
        Ok(dir) => dir.join("webui"),
        Err(e) => {
            eprintln!("[voxeldash] cannot resolve resource dir: {e}");
            return;
        }
    };

    let cmd = match app.shell().sidecar("voxeldash-one") {
        Ok(cmd) => cmd,
        Err(e) => {
            eprintln!("[voxeldash] backend binary not found: {e}");
            return;
        }
    };

    let cmd = cmd
        .env("PORT", PORT.to_string())
        .env("MASTER_HOST", "127.0.0.1")
        .env("VOXELDASH_DESKTOP", "1")
        .env("VOXELDASH_HOME", data_dir.to_string_lossy().to_string())
        .env("VOXELDASH_UI", ui_dir.to_string_lossy().to_string());

    match cmd.spawn() {
        Ok((mut rx, child)) => {
            app.state::<Backend>().0.lock().unwrap().replace(child);
            tauri::async_runtime::spawn(async move {
                use tauri_plugin_shell::process::CommandEvent;
                while let Some(event) = rx.recv().await {
                    match event {
                        CommandEvent::Stdout(line) => {
                            print!("[one] {}", String::from_utf8_lossy(&line))
                        }
                        CommandEvent::Stderr(line) => {
                            eprint!("[one] {}", String::from_utf8_lossy(&line))
                        }
                        CommandEvent::Terminated(payload) => {
                            eprintln!("[voxeldash] backend exited: {:?}", payload.code)
                        }
                        _ => {}
                    }
                }
            });
        }
        Err(e) => eprintln!("[voxeldash] failed to spawn backend: {e}"),
    }
}

fn build_tray(app: &tauri::App) -> tauri::Result<()> {
    let show = MenuItem::with_id(app, "show", "Open VoxelDash One", true, None::<&str>)?;
    let quit = MenuItem::with_id(app, "quit", "Quit", true, None::<&str>)?;
    let menu = Menu::with_items(app, &[&show, &quit])?;

    let mut builder = TrayIconBuilder::with_id("main")
        .tooltip("VoxelDash One")
        .menu(&menu)
        .show_menu_on_left_click(false)
        .on_menu_event(|app, event| match event.id.as_ref() {
            "show" => show_main(app),
            "quit" => quit_app(app),
            _ => {}
        })
        .on_tray_icon_event(|tray, event| {
            if let TrayIconEvent::Click {
                button: MouseButton::Left,
                button_state: MouseButtonState::Up,
                ..
            } = event
            {
                show_main(tray.app_handle());
            }
        });

    if let Some(icon) = app.default_window_icon() {
        builder = builder.icon(icon.clone());
    }

    builder.build(app)?;
    Ok(())
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| {
            show_main(app);
        }))
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_opener::init())
        .manage(Backend(Mutex::new(None)))
        .setup(|app| {
            spawn_backend(app);
            build_tray(app)?;

            let handle = app.handle().clone();
            std::thread::spawn(move || {
                for _ in 0..600 {
                    if backend_up() {
                        break;
                    }
                    std::thread::sleep(Duration::from_millis(100));
                }
                if let Some(window) = handle.get_webview_window("main") {
                    let _ = window.eval(&format!(
                        "window.location.replace('http://127.0.0.1:{PORT}')"
                    ));
                    let _ = window.show();
                    let _ = window.set_focus();
                }
            });

            Ok(())
        })
        .on_window_event(|window, event| {
            if let WindowEvent::CloseRequested { api, .. } = event {
                api.prevent_close();
                let _ = window.hide();
            }
        })
        .build(tauri::generate_context!())
        .expect("error while building VoxelDash One")
        .run(|app, event| {
            if let RunEvent::ExitRequested { api, code, .. } = event {
                if code.is_none() {
                    api.prevent_exit();
                }
            }
            let _ = app;
        });
}
