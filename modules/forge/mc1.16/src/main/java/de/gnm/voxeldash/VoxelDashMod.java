package de.gnm.voxeldash;

import de.gnm.voxeldash.api.controller.ActionRegistry;
import de.gnm.voxeldash.api.entities.BackupPart;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.schedule.ActionInputType;
import de.gnm.voxeldash.api.entities.schedule.ScheduleAction;
import de.gnm.voxeldash.api.helper.BackupHelper;
import de.gnm.voxeldash.api.pipes.MotdPipe;
import de.gnm.voxeldash.api.pipes.ProfilingPipe;
import de.gnm.voxeldash.api.pipes.GameRulePipe;
import de.gnm.voxeldash.api.pipes.QuickActionPipe;
import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import de.gnm.voxeldash.api.pipes.players.BanPipe;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import de.gnm.voxeldash.api.pipes.players.OperatorPipe;
import de.gnm.voxeldash.api.pipes.players.WhitelistPipe;
import de.gnm.voxeldash.api.controller.PlayerDataController;
import de.gnm.voxeldash.api.pipes.players.InventoryPipe;
import de.gnm.voxeldash.api.pipes.players.MessagePipe;
import de.gnm.voxeldash.api.pipes.players.ProfilePipe;
import de.gnm.voxeldash.api.pipes.players.PunishmentPipe;
import de.gnm.voxeldash.api.pipes.players.TeleportPipe;
import de.gnm.voxeldash.api.pipes.resources.ResourcePipe;
import de.gnm.voxeldash.api.pipes.worlds.WorldPipe;
import de.gnm.voxeldash.command.PasswordCommand;
import de.gnm.voxeldash.listener.ConsoleListener;
import de.gnm.voxeldash.pipes.*;
import de.gnm.voxeldash.util.ForgeUtil;
import de.gnm.voxeldash.util.VersionCompat;
import de.gnm.voxeldash.widgets.ForgeWidgetProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

@Mod(VoxelDashMod.MODID)
public class VoxelDashMod {

    public static final String MODID = "voxeldash";

    private static VoxelDashMod instance;
    private static MinecraftServer server;
    private VoxelDashLoader loader;
    private BackupHelper backupHelper;
    private ForgeWidgetProvider widgetProvider;
    private final Logger logger = Logger.getLogger("VoxelDash");

    public VoxelDashMod() {
        instance = this;
        logger.info("Starting VoxelDash Forge...");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PasswordCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(FMLServerStartedEvent event) {
        server = event.getServer();
        ForgeUtil.setCompat(new VersionCompat());

        try {
            initializeLoader();
            registerPipes();
            registerFeatures();
            registerActions();
            registerWidgets();

            loader.startup();

            ConsoleListener.register(this);

            logger.info("VoxelDash is now running!");
            logger.info("Web interface available at http://localhost:7867");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start VoxelDash", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(FMLServerStoppingEvent event) {
        ConsoleListener.unregister();
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
        loader.registerPipe(ProfilingPipe.class, new ProfilingPipeImpl());
        loader.registerPipe(GameRulePipe.class, new GameRulePipeImpl());

        loader.registerPipe(InventoryPipe.class, new InventoryPipeImpl());
        loader.registerPipe(TeleportPipe.class, new TeleportPipeImpl());
        loader.registerPipe(MessagePipe.class, new MessagePipeImpl());
        loader.registerPipe(ProfilePipe.class, new ProfilePipeImpl());
        loader.registerPipe(PunishmentPipe.class, new PunishmentPipeImpl());
        ForgeUtil.compat().registerMuteCheck(uuid -> {
            try {
                return loader.getController(PlayerDataController.class).isMuted(uuid);
            } catch (Throwable t) {
                return false;
            }
        });
    }

    private void registerActions() {
        ActionRegistry registry = loader.getActionRegistry();
        QuickActionPipe quickAction = loader.getPipe(QuickActionPipe.class);

        registry.registerAction(new ScheduleAction("command", "schedules.actions.command",
                ActionInputType.TEXT, "schedules.actions.command_input",
                metadata -> ForgeUtil.compat().runCommand(metadata)));

        registry.registerAction(new ScheduleAction("broadcast", "schedules.actions.broadcast",
                ActionInputType.TEXTAREA, "schedules.actions.broadcast_input",
                metadata -> ForgeUtil.compat().broadcast(metadata)));

        registry.registerAction(new ScheduleAction("reload", "schedules.actions.reload",
                ActionInputType.NONE, null, metadata -> quickAction.reloadServer()));

        registry.registerAction(new ScheduleAction("stop", "schedules.actions.stop",
                ActionInputType.NONE, null, metadata -> quickAction.stopServer()));

        registry.registerAction(new ScheduleAction("backup", "schedules.actions.backup",
                ActionInputType.NUMBER, "schedules.actions.backup_input",
                metadata -> {
                    try {
                        backupHelper.createScheduledBackup(metadata);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to create backup", e);
                    }
                }));

        registry.registerAction(new ScheduleAction("kick_all", "schedules.actions.kick_all",
                ActionInputType.TEXT, "schedules.actions.kick_all_input",
                metadata -> ForgeUtil.compat().kickAll(
                        (metadata != null && !metadata.isEmpty()) ? metadata : "Server maintenance")));
    }

    private void registerFeatures() {
        loader.registerFeatures(Feature.FileManager, Feature.Properties, Feature.SSH, Feature.Backups,
                Feature.Console, Feature.Players, Feature.Schedules, Feature.Worlds, Feature.Resources, Feature.Motd, Feature.Profiling, Feature.GameRules);

        loader.registerBackupParts(BackupPart.WORLDS, BackupPart.MODS, BackupPart.CONFIGS, BackupPart.LOGS);
    }

    private void registerWidgets() {
        widgetProvider = new ForgeWidgetProvider(this);
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
