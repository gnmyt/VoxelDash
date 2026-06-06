import {ReactNode, useEffect, useState} from "react";
import {getCurrentWindow} from "@tauri-apps/api/window";
import {CopyIcon, MinusIcon, SquareIcon, XIcon} from "@phosphor-icons/react";
import logo from "@/assets/images/logo.png";
import {t} from "i18next";

const ControlButton = ({onClick, label, danger, children}: {
    onClick: () => void;
    label: string;
    danger?: boolean;
    children: ReactNode;
}) => (
    <button
        type="button"
        aria-label={label}
        onClick={onClick}
        className={`flex h-8 w-[46px] items-center justify-center text-muted-foreground transition-colors hover:text-foreground ${
            danger ? "hover:bg-destructive hover:text-destructive-foreground" : "hover:bg-foreground/10"
        }`}
    >
        {children}
    </button>
);

export const TitleBar = () => {
    const [maximized, setMaximized] = useState(false);
    const appWindow = getCurrentWindow();

    useEffect(() => {
        let unlisten: (() => void) | undefined;
        const sync = () => appWindow.isMaximized().then(setMaximized).catch(() => {
        });
        sync();
        appWindow.onResized(sync).then((fn) => {
            unlisten = fn;
        }).catch(() => {
        });
        return () => unlisten?.();
    }, [appWindow]);

    return (
        <div
            data-tauri-drag-region
            className="flex h-8 shrink-0 select-none items-center justify-between border-b border-border/60 bg-sidebar"
        >
            <div data-tauri-drag-region className="pointer-events-none flex items-center gap-2 pl-3">
                <img src={logo} alt="" className="size-4 rounded-[4px]"/>
                <span className="font-display text-xs font-semibold text-muted-foreground">VoxelDash One</span>
            </div>
            <div className="flex h-full">
                <ControlButton label={t("titlebar.minimize")} onClick={() => appWindow.minimize()}>
                    <MinusIcon size={14} weight="bold"/>
                </ControlButton>
                <ControlButton label={maximized ? t("titlebar.restore") : t("titlebar.maximize")} onClick={() => appWindow.toggleMaximize()}>
                    {maximized ? <CopyIcon size={13} weight="bold"/> : <SquareIcon size={12} weight="bold"/>}
                </ControlButton>
                <ControlButton label={t("titlebar.close")} danger onClick={() => appWindow.hide()}>
                    <XIcon size={15} weight="bold"/>
                </ControlButton>
            </div>
        </div>
    );
};
