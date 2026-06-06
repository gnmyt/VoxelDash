package de.gnm.loader;

import de.gnm.loader.command.SetupHandler;
import de.gnm.loader.helper.PlayerTracker;
import de.gnm.loader.helper.ServerHelper;
import de.gnm.loader.msmp.MsmpBanPipe;
import de.gnm.loader.msmp.MsmpClient;
import de.gnm.loader.msmp.MsmpOnlinePlayerPipe;
import de.gnm.loader.msmp.MsmpOperatorPipe;
import de.gnm.loader.msmp.MsmpQuickActionPipe;
import de.gnm.loader.msmp.MsmpWhitelistPipe;
import de.gnm.loader.pipes.*;
import de.gnm.loader.widgets.VanillaWidgetProvider;
import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.api.controller.ActionRegistry;
import de.gnm.voxeldash.api.entities.BackupPart;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.schedule.ActionInputType;
import de.gnm.voxeldash.api.entities.schedule.ScheduleAction;
import de.gnm.voxeldash.api.event.console.ConsoleMessageReceivedEvent;
import de.gnm.voxeldash.api.helper.BackupHelper;
import de.gnm.voxeldash.api.helper.PropertyHelper;
import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import de.gnm.voxeldash.api.pipes.players.BanPipe;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import de.gnm.voxeldash.api.pipes.players.OperatorPipe;
import de.gnm.voxeldash.api.pipes.QuickActionPipe;
import de.gnm.voxeldash.api.pipes.players.WhitelistPipe;
import de.gnm.voxeldash.api.pipes.resources.ResourcePipe;
import de.gnm.voxeldash.api.pipes.worlds.WorldPipe;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;

public class VoxelDashVanilla {

    private static final File SERVER_ROOT = new File(System.getProperty("user.dir"));
    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");

    private static final VoxelDashLoader loader = new VoxelDashLoader();
    private static final ServerHelper serverHelper = new ServerHelper(SERVER_ROOT, loader.getEventDispatcher());
    private static final BackupHelper backupHelper = new BackupHelper(new File(SERVER_ROOT, "backups"));

    private static String serverVersion = null;
    private static PlayerTracker playerTracker = null;
    private static VanillaWidgetProvider widgetProvider = null;
    private static MsmpClient msmpClient = null;

    /**
     * Main method. Starts the VoxelDash Vanilla server
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        LOG.info("Starting VoxelDash Vanilla...");

        loader.getEventDispatcher().registerListener(ConsoleMessageReceivedEvent.class, event -> {
            if (serverVersion == null && event.getMessage().contains("Starting minecraft server version")) {
                serverVersion = event.getMessage().split("Starting minecraft server version ")[1].split(" ")[0];
                loader.registerPipe(ServerInfoPipe.class, new ServerInfoPipeImpl(serverVersion));
                if (msmpClient != null) {
                    msmpClient.notifyServerVersion(serverVersion);
                }
            }
            LOG.info(event.getMessage());
        });

        loader.setServerRoot(SERVER_ROOT);
        loader.startup();
        new Thread(serverHelper::startup).start();

        while (serverHelper.getOutputStream() == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOG.error("Failed to start VoxelDash Vanilla", e);
            }
        }

        playerTracker = new PlayerTracker(SERVER_ROOT, loader.getEventDispatcher());

        msmpClient = new MsmpClient(serverHelper.getManagementPort(), serverHelper.getManagementSecret());
        msmpClient.start();
        if (serverVersion != null) {
            msmpClient.notifyServerVersion(serverVersion);
        }

        registerPipes();
        registerFeatures();
        registerActions();
        registerWidgets();
        registerCommands();

        LOG.info("Web interface available at http://localhost:7867");
    }

    /**
     * Registers the in-game command handlers driven by console output
     */
    protected static void registerCommands() {
        SetupHandler setupHandler = new SetupHandler(loader, SERVER_ROOT, serverHelper.getOutputStream());
        setupHandler.register(loader.getEventDispatcher());
    }

    /**
     * Registers all pipes
     */
    protected static void registerPipes() {
        OutputStream outputStream = serverHelper.getOutputStream();
        File worldFolder = getWorldFolder();

        loader.registerPipe(ServerInfoPipe.class, new ServerInfoPipeImpl(serverVersion != null ? serverVersion : "Unknown"));

        OperatorPipeImpl operatorFallback = new OperatorPipeImpl(outputStream);
        WhitelistPipeImpl whitelistFallback = new WhitelistPipeImpl(outputStream);
        QuickActionPipeImpl quickActionFallback = new QuickActionPipeImpl(outputStream);
        OnlinePlayerPipeImpl onlinePlayerFallback = new OnlinePlayerPipeImpl(outputStream, playerTracker, worldFolder);
        BanPipeImpl banFallback = new BanPipeImpl(outputStream);

        loader.registerPipe(OperatorPipe.class, new MsmpOperatorPipe(msmpClient, operatorFallback));
        loader.registerPipe(WhitelistPipe.class, new MsmpWhitelistPipe(msmpClient, whitelistFallback));
        loader.registerPipe(QuickActionPipe.class, new MsmpQuickActionPipe(msmpClient, quickActionFallback));
        loader.registerPipe(OnlinePlayerPipe.class, new MsmpOnlinePlayerPipe(msmpClient, onlinePlayerFallback, playerTracker));
        loader.registerPipe(BanPipe.class, new MsmpBanPipe(msmpClient, banFallback));

        loader.registerPipe(WorldPipe.class, new WorldPipeImpl(outputStream, SERVER_ROOT));
        loader.registerPipe(ResourcePipe.class, new ResourcePipeImpl(outputStream, SERVER_ROOT));
    }

    /**
     * Registers all features for vanilla
     */
    protected static void registerFeatures() {
        loader.registerFeatures(
                Feature.FileManager,
                Feature.Properties,
                Feature.SSH,
                Feature.Backups,
                Feature.Console,
                Feature.Schedules,
                Feature.Players,
                Feature.Worlds,
                Feature.Resources
        );

        loader.registerBackupParts(BackupPart.WORLDS, BackupPart.CONFIGS, BackupPart.LOGS);
    }

    /**
     * Registers all dashboard widgets for vanilla servers
     */
    protected static void registerWidgets() {
        ServerInfoPipe serverInfoPipe = loader.getPipe(ServerInfoPipe.class);
        widgetProvider = new VanillaWidgetProvider(
                loader.getWidgetRegistry(),
                serverInfoPipe,
                playerTracker,
                SERVER_ROOT
        );
        widgetProvider.register();
    }

    /**
     * Gets the world folder based on server.properties
     */
    private static File getWorldFolder() {
        String levelName = PropertyHelper.getProperty("level-name");
        if (levelName == null) {
            levelName = "world";
        }
        return new File(SERVER_ROOT, levelName);
    }

    /**
     * Registers all schedule actions for vanilla servers
     */
    protected static void registerActions() {
        ActionRegistry registry = loader.getActionRegistry();
        OutputStream outputStream = serverHelper.getOutputStream();
        QuickActionPipe quickAction = loader.getPipe(QuickActionPipe.class);

        registry.registerAction(new ScheduleAction(
            "command",
            "schedules.actions.command",
            ActionInputType.TEXT,
            "schedules.actions.command_input",
            metadata -> {
                PrintWriter writer = new PrintWriter(outputStream, true);
                writer.println(metadata);
            }
        ));

        registry.registerAction(new ScheduleAction(
            "broadcast",
            "schedules.actions.broadcast",
            ActionInputType.TEXTAREA,
            "schedules.actions.broadcast_input",
            metadata -> {
                PrintWriter writer = new PrintWriter(outputStream, true);
                writer.println("say " + metadata);
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
                    LOG.error("Failed to create backup", e);
                }
            }
        ));

        registry.registerAction(new ScheduleAction(
            "kick_all",
            "schedules.actions.kick_all",
            ActionInputType.TEXT,
            "schedules.actions.kick_all_input",
            metadata -> {
                PrintWriter writer = new PrintWriter(outputStream, true);
                String reason = (metadata != null && !metadata.isEmpty()) ? metadata : "Server maintenance";
                writer.println("kick @a " + reason);
            }
        ));
    }

}
