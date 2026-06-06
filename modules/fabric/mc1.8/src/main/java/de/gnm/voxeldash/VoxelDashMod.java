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
import de.gnm.voxeldash.api.pipes.players.OperatorPipe;
import de.gnm.voxeldash.api.pipes.players.WhitelistPipe;
import de.gnm.voxeldash.api.pipes.resources.ResourcePipe;
import de.gnm.voxeldash.api.pipes.worlds.WorldPipe;
import de.gnm.voxeldash.listener.ConsoleListenerBeta9;
import de.gnm.voxeldash.pipes.*;
import de.gnm.voxeldash.util.FabricUtil;
import de.gnm.voxeldash.util.VersionCompat;
import de.gnm.voxeldash.widgets.FabricWidgetProvider;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.legacyfabric.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VoxelDashMod implements DedicatedServerModInitializer {

    public static final String MODID = "voxeldash";

    private static VoxelDashMod instance;
    private static MinecraftServer server;
    private VoxelDashLoader loader;
    private BackupHelper backupHelper;
    private FabricWidgetProvider widgetProvider;
    private final Logger logger = Logger.getLogger("VoxelDash");

    @Override
    public void onInitializeServer() {
        instance = this;
        logger.info("Starting VoxelDash Fabric (Legacy 1.8.9)...");

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
    }

    private void onServerStarted(MinecraftServer minecraftServer) {
        server = minecraftServer;
        FabricUtil.setCompat(new VersionCompat());

        try {
            initializeLoader();
            registerPipes();
            registerFeatures();
            registerActions();
            registerWidgets();

            loader.startup();

            ConsoleListenerBeta9.register(this);

            logger.info("VoxelDash is now running!");
            logger.info("Web interface available at http://localhost:7867");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start VoxelDash", e);
        }
    }

    private void onServerStopping(MinecraftServer minecraftServer) {
        ConsoleListenerBeta9.unregister();
        if (widgetProvider != null) widgetProvider.shutdown();
        if (loader != null) {
            loader.shutdown();
            logger.info("VoxelDash has been disabled");
        }
    }

    private void initializeLoader() {
        File serverRoot = new File(System.getProperty("user.dir"));
        loader = new VoxelDashLoader();
        loader.setServerRoot(serverRoot);

        File dataFolder = new File(serverRoot, "config/voxeldash");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        loader.setDatabaseFile(new File(dataFolder, "voxeldash.db").getAbsolutePath());

        backupHelper = new BackupHelper(new File(serverRoot, "backups"));
    }

    private void registerPipes() {
        loader.registerPipe(ServerInfoPipe.class, new ServerInfoPipeImpl());
        loader.registerPipe(QuickActionPipe.class, new QuickActionPipeImpl());
        loader.registerPipe(OperatorPipe.class, new OperatorPipeImpl());
        loader.registerPipe(WhitelistPipe.class, new WhitelistPipeImpl());
        loader.registerPipe(OnlinePlayerPipe.class, new OnlinePlayerPipeImpl());
        loader.registerPipe(BanPipe.class, new BanPipeImpl());
        loader.registerPipe(WorldPipe.class, new WorldPipeImpl());
        loader.registerPipe(ResourcePipe.class, new ResourcePipeImpl());
        loader.registerPipe(MotdPipe.class, new MotdPipeImpl());
    }

    private void registerActions() {
        ActionRegistry registry = loader.getActionRegistry();
        QuickActionPipe quickAction = loader.getPipe(QuickActionPipe.class);

        registry.registerAction(new ScheduleAction("command", "schedules.actions.command",
                ActionInputType.TEXT, "schedules.actions.command_input",
                metadata -> FabricUtil.compat().runCommand(metadata)));

        registry.registerAction(new ScheduleAction("broadcast", "schedules.actions.broadcast",
                ActionInputType.TEXTAREA, "schedules.actions.broadcast_input",
                metadata -> FabricUtil.compat().broadcast(metadata)));

        registry.registerAction(new ScheduleAction("reload", "schedules.actions.reload",
                ActionInputType.NONE, null, metadata -> quickAction.reloadServer()));

        registry.registerAction(new ScheduleAction("stop", "schedules.actions.stop",
                ActionInputType.NONE, null, metadata -> quickAction.stopServer()));

        registry.registerAction(new ScheduleAction("backup", "schedules.actions.backup",
                ActionInputType.NUMBER, "schedules.actions.backup_input",
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
                        logger.log(Level.SEVERE, "Failed to create backup", e);
                    }
                }));

        registry.registerAction(new ScheduleAction("kick_all", "schedules.actions.kick_all",
                ActionInputType.TEXT, "schedules.actions.kick_all_input",
                metadata -> FabricUtil.compat().kickAll(
                        (metadata != null && !metadata.isEmpty()) ? metadata : "Server maintenance")));
    }

    private void registerFeatures() {
        loader.registerFeatures(Feature.FileManager, Feature.Properties, Feature.SSH, Feature.Backups,
                Feature.Console, Feature.Players, Feature.Schedules, Feature.Worlds, Feature.Resources, Feature.Motd);

        loader.registerBackupParts(BackupPart.WORLDS, BackupPart.MODS, BackupPart.CONFIGS, BackupPart.LOGS);
    }

    private void registerWidgets() {
        widgetProvider = new FabricWidgetProvider(this);
        widgetProvider.register();
    }

    public static VoxelDashMod getInstance() {
        return instance;
    }

    public VoxelDashLoader getLoader() {
        return loader;
    }

    public static MinecraftServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
}
