package de.gnm.voxeldash;

import de.gnm.voxeldash.api.controller.ActionRegistry;
import de.gnm.voxeldash.api.entities.BackupPart;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.schedule.ActionInputType;
import de.gnm.voxeldash.api.entities.schedule.ScheduleAction;
import de.gnm.voxeldash.api.helper.BackupHelper;
import de.gnm.voxeldash.api.pipes.QuickActionPipe;
import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import de.gnm.voxeldash.api.pipes.players.BanPipe;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import de.gnm.voxeldash.api.pipes.players.OperatorPipe;
import de.gnm.voxeldash.api.pipes.players.WhitelistPipe;
import de.gnm.voxeldash.api.pipes.resources.ResourcePipe;
import de.gnm.voxeldash.api.pipes.worlds.WorldPipe;
import de.gnm.voxeldash.command.PasswordCommand;
import de.gnm.voxeldash.listener.ConsoleListener;
import de.gnm.voxeldash.pipes.BanPipeImpl;
import de.gnm.voxeldash.pipes.OnlinePlayerPipeImpl;
import de.gnm.voxeldash.pipes.OperatorPipeImpl;
import de.gnm.voxeldash.pipes.QuickActionPipeImpl;
import de.gnm.voxeldash.pipes.ResourcePipeImpl;
import de.gnm.voxeldash.pipes.ServerInfoPipeImpl;
import de.gnm.voxeldash.pipes.WhitelistPipeImpl;
import de.gnm.voxeldash.pipes.WorldPipeImpl;
import de.gnm.voxeldash.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import de.gnm.voxeldash.widgets.SpigotWidgetProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class VoxelDashSpigot extends JavaPlugin {

    private static VoxelDashSpigot instance;
    private VoxelDashLoader loader;
    private BackupHelper backupHelper;
    private SpigotWidgetProvider widgetProvider;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("Starting VoxelDash Spigot...");

        try {
            initializeLoader();
            registerPipes();
            registerFeatures();
            registerActions();
            registerWidgets();
            
            loader.startup();

            ConsoleListener.register(this);

            getCommand("voxeldash").setExecutor(new PasswordCommand(this));

            getLogger().info("VoxelDash is now running!");
            getLogger().info("Web interface available at http://localhost:7867");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to start VoxelDash", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        ConsoleListener.unregister();
        
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
        File serverRoot = getServer().getWorldContainer().getAbsoluteFile();
        
        loader = new VoxelDashLoader();
        loader.setServerRoot(serverRoot);
        loader.setDatabaseFile(new File(getDataFolder(), "voxeldash.db").getAbsolutePath());

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        backupHelper = new BackupHelper(new File(serverRoot, "backups"));
    }

    /**
     * Registers all pipe implementations using the Spigot API
     */
    private void registerPipes() {
        loader.registerPipe(ServerInfoPipe.class, new ServerInfoPipeImpl());
        loader.registerPipe(QuickActionPipe.class, new QuickActionPipeImpl());
        loader.registerPipe(OperatorPipe.class, new OperatorPipeImpl());
        loader.registerPipe(WhitelistPipe.class, new WhitelistPipeImpl());
        loader.registerPipe(OnlinePlayerPipe.class, new OnlinePlayerPipeImpl());
        loader.registerPipe(BanPipe.class, new BanPipeImpl());
        loader.registerPipe(WorldPipe.class, new WorldPipeImpl());
        loader.registerPipe(ResourcePipe.class, new ResourcePipeImpl());
    }

    /**
     * Registers all schedule actions available for Spigot servers
     */
    private void registerActions() {
        ActionRegistry registry = loader.getActionRegistry();
        QuickActionPipe quickAction = loader.getPipe(QuickActionPipe.class);

        registry.registerAction(new ScheduleAction(
            "command",
            "schedules.actions.command",
            ActionInputType.TEXT,
            "schedules.actions.command_input",
            metadata -> BukkitUtil.runOnMainThread(() -> 
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), metadata))
        ));

        registry.registerAction(new ScheduleAction(
            "broadcast",
            "schedules.actions.broadcast",
            ActionInputType.TEXTAREA,
            "schedules.actions.broadcast_input",
            metadata -> BukkitUtil.runOnMainThread(() ->
                Bukkit.broadcastMessage(metadata))
        ));

        registry.registerAction(new ScheduleAction(
            "reload",
            "schedules.actions.reload",
            ActionInputType.NONE,
            null,
            metadata -> BukkitUtil.runOnMainThread(() ->
                quickAction.reloadServer())
        ));

        registry.registerAction(new ScheduleAction(
            "stop",
            "schedules.actions.stop",
            ActionInputType.NONE,
            null,
            metadata -> BukkitUtil.runOnMainThread(() ->
                quickAction.stopServer())
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
            metadata -> BukkitUtil.runOnMainThread(() -> {
                String reason = (metadata != null && !metadata.isEmpty()) ? metadata : "Server maintenance";
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.kickPlayer(reason);
                }
            })
        ));
    }

    /**
     * Registers all features available for Spigot servers
     */
    private void registerFeatures() {
        loader.registerFeatures(
                Feature.FileManager,
                Feature.Properties,
                Feature.SSH,
                Feature.Backups,
                Feature.Console,
                Feature.Players,
                Feature.Schedules,
                Feature.Worlds,
                Feature.Resources
        );

        loader.registerBackupParts(BackupPart.WORLDS, BackupPart.PLUGINS, BackupPart.CONFIGS, BackupPart.LOGS);
    }

    /**
     * Registers all dashboard widgets for Spigot servers
     */
    private void registerWidgets() {
        widgetProvider = new SpigotWidgetProvider(this);
        widgetProvider.register();
    }

    /**
     * Gets the plugin instance
     *
     * @return the plugin instance
     */
    public static VoxelDashSpigot getInstance() {
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
