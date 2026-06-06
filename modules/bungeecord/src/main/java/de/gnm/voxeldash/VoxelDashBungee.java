package de.gnm.voxeldash;

import de.gnm.voxeldash.api.controller.ActionRegistry;
import de.gnm.voxeldash.api.entities.BackupPart;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.schedule.ActionInputType;
import de.gnm.voxeldash.api.entities.schedule.ScheduleAction;
import de.gnm.voxeldash.api.helper.BackupHelper;
import de.gnm.voxeldash.api.pipes.MotdPipe;
import de.gnm.voxeldash.api.pipes.QuickActionPipe;
import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import de.gnm.voxeldash.api.pipes.players.BanPipe;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import de.gnm.voxeldash.api.pipes.players.WhitelistPipe;
import de.gnm.voxeldash.api.pipes.resources.ResourcePipe;
import de.gnm.voxeldash.command.PasswordCommand;
import de.gnm.voxeldash.listener.ConsoleListener;
import de.gnm.voxeldash.listener.LoginListener;
import de.gnm.voxeldash.pipes.*;
import de.gnm.voxeldash.util.BungeeUtil;
import de.gnm.voxeldash.widgets.BungeeWidgetProvider;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.util.logging.Level;

public class VoxelDashBungee extends Plugin {

    private static VoxelDashBungee instance;
    private VoxelDashLoader loader;
    private BackupHelper backupHelper;
    private BungeeWidgetProvider widgetProvider;

    @Override
    public void onEnable() {
        instance = this;
        BungeeUtil.setProxy(getProxy());

        getLogger().info("Starting VoxelDash BungeeCord...");

        try {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                getLogger().severe("SQLite JDBC driver not found!");
                throw new RuntimeException(e);
            }

            initializeLoader();
            registerPipes();
            registerFeatures();
            registerActions();
            registerWidgets();

            loader.startup();

            ConsoleListener.register(this);

            getProxy().getPluginManager().registerCommand(this, new PasswordCommand(this));

            getLogger().info("VoxelDash is now running!");
            getLogger().info("Web interface available at http://localhost:7867");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to start VoxelDash", e);
        }
    }

    @Override
    public void onDisable() {
        ConsoleListener.unregister(this);

        if (widgetProvider != null) {
            widgetProvider.shutdown();
        }

        if (loader != null) {
            loader.shutdown();
            getLogger().info("VoxelDash has been disabled");
        }
    }

    /**
     * Initializes the VoxelDash loader
     */
    private void initializeLoader() {
        File serverRoot = new File(System.getProperty("user.dir"));

        loader = new VoxelDashLoader();
        loader.setServerRoot(serverRoot);
        loader.setDatabaseFile(new File(getDataFolder(), "voxeldash.db").getAbsolutePath());

        loader.setLogFile(new File(serverRoot, "proxy.log.0"));

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        backupHelper = new BackupHelper(new File(serverRoot, "backups"));
    }

    /**
     * Registers all pipe implementations using the BungeeCord API
     */
    private void registerPipes() {
        getProxy().getPluginManager().registerListener(this, new LoginListener());
        
        loader.registerPipe(ServerInfoPipe.class, new ServerInfoPipeImpl());
        loader.registerPipe(QuickActionPipe.class, new QuickActionPipeImpl(this));
        loader.registerPipe(OnlinePlayerPipe.class, new OnlinePlayerPipeImpl());
        loader.registerPipe(ResourcePipe.class, new ResourcePipeImpl());

        loader.registerPipe(WhitelistPipe.class, new WhitelistPipeImpl());
        loader.registerPipe(BanPipe.class, new BanPipeImpl());

        MotdPipeImpl motdPipe = new MotdPipeImpl();
        loader.registerPipe(MotdPipe.class, motdPipe);
        getProxy().getPluginManager().registerListener(this, motdPipe);
    }

    /**
     * Registers all schedule actions available for BungeeCord
     */
    private void registerActions() {
        ActionRegistry registry = loader.getActionRegistry();
        QuickActionPipe quickAction = loader.getPipe(QuickActionPipe.class);

        registry.registerAction(new ScheduleAction(
            "command",
            "schedules.actions.command",
            ActionInputType.TEXT,
            "schedules.actions.command_input",
            metadata -> ProxyServer.getInstance().getPluginManager().dispatchCommand(
                    ProxyServer.getInstance().getConsole(), metadata)
        ));

        registry.registerAction(new ScheduleAction(
            "broadcast",
            "schedules.actions.broadcast",
            ActionInputType.TEXTAREA,
            "schedules.actions.broadcast_input",
            metadata -> {
                for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                    player.sendMessage(new TextComponent(metadata));
                }
            }
        ));

        registry.registerAction(new ScheduleAction(
            "reload",
            "schedules.actions.reload",
            ActionInputType.NONE,
            null,
            metadata -> quickAction.reloadServer()
        ));

        registry.registerAction(new ScheduleAction(
            "stop",
            "schedules.actions.stop",
            ActionInputType.NONE,
            null,
            metadata -> quickAction.stopServer()
        ));

        registry.registerAction(new ScheduleAction(
            "backup",
            "schedules.actions.backup",
            ActionInputType.NUMBER,
            "schedules.actions.backup_input",
            metadata -> {
                try {
                    int backupMode = 0;
                    if (metadata != null && !metadata.isEmpty()) {
                        try {
                            backupMode = Integer.parseInt(metadata);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    backupHelper.createBackup(String.valueOf(backupMode),
                        backupHelper.getBackupDirectories(backupMode).toArray(new File[0]));
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to create backup", e);
                }
            }
        ));

        registry.registerAction(new ScheduleAction(
            "kick_all",
            "schedules.actions.kick_all",
            ActionInputType.TEXT,
            "schedules.actions.kick_all_input",
            metadata -> {
                String reason = (metadata != null && !metadata.isEmpty()) ? metadata : "Server maintenance";
                for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                    player.disconnect(new TextComponent(reason));
                }
            }
        ));
    }

    /**
     * Registers all features available for BungeeCord
     * Note: Some features are disabled as they don't apply to a proxy server
     */
    private void registerFeatures() {
        loader.registerFeatures(
                Feature.FileManager,
                Feature.SSH,
                Feature.Backups,
                Feature.Console,
                Feature.Players,
                Feature.Schedules,
                Feature.Resources,
                Feature.Motd
        );

        loader.registerBackupParts(BackupPart.PLUGINS, BackupPart.CONFIGS, BackupPart.LOGS);
    }

    /**
     * Registers all dashboard widgets for BungeeCord
     */
    private void registerWidgets() {
        widgetProvider = new BungeeWidgetProvider(this);
        widgetProvider.register();
    }

    /**
     * Gets the plugin instance
     *
     * @return the plugin instance
     */
    public static VoxelDashBungee getInstance() {
        return instance;
    }

    /**
     * Gets the VoxelDash loader
     *
     * @return the VoxelDash loader
     */
    public VoxelDashLoader getLoader() {
        return loader;
    }
}
