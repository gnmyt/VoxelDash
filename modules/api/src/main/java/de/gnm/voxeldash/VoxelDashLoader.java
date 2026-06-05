package de.gnm.voxeldash;


import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.ActionRegistry;
import de.gnm.voxeldash.api.controller.ApiKeyController;
import de.gnm.voxeldash.api.controller.ControllerManager;
import de.gnm.voxeldash.api.controller.PermissionController;
import de.gnm.voxeldash.api.controller.ScheduleController;
import de.gnm.voxeldash.api.controller.SSHController;
import de.gnm.voxeldash.api.controller.SessionController;
import de.gnm.voxeldash.api.controller.WidgetRegistry;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.event.EventDispatcher;
import de.gnm.voxeldash.api.handlers.BaseHandler;
import de.gnm.voxeldash.api.handlers.StaticHandler;
import de.gnm.voxeldash.api.handlers.WebSocketHandler;
import de.gnm.voxeldash.api.helper.ScheduleExecutor;
import de.gnm.voxeldash.api.http.HTTPMethod;
import de.gnm.voxeldash.api.http.RouteMeta;
import de.gnm.voxeldash.api.pipes.BasePipe;
import de.gnm.voxeldash.api.routes.BaseRoute;
import de.gnm.voxeldash.api.routes.BackupRouter;
import de.gnm.voxeldash.api.routes.InfoRouter;
import de.gnm.voxeldash.api.routes.PingRouter;
import de.gnm.voxeldash.api.routes.PropertyRouter;
import de.gnm.voxeldash.api.routes.QuickActionRouter;
import de.gnm.voxeldash.api.routes.ScheduleRouter;
import de.gnm.voxeldash.api.routes.SessionRouter;
import de.gnm.voxeldash.api.routes.UserRouter;
import de.gnm.voxeldash.api.routes.WidgetRouter;
import de.gnm.voxeldash.api.routes.files.FileRouter;
import de.gnm.voxeldash.api.routes.files.FolderRouter;
import de.gnm.voxeldash.api.routes.players.PlayerRouter;
import de.gnm.voxeldash.api.routes.resources.ResourceRouter;
import de.gnm.voxeldash.api.routes.resources.StoreRouter;
import de.gnm.voxeldash.api.routes.service.SSHRouter;
import de.gnm.voxeldash.api.routes.worlds.WorldRouter;
import de.gnm.voxeldash.api.tunnel.ConnectionConfig;
import de.gnm.voxeldash.api.tunnel.MasterTunnelClient;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public class VoxelDashLoader {
    private final Map<Class<?>, BasePipe> pipes = new HashMap<>();
    private final List<Feature> availableFeatures = new ArrayList<>();
    private final ControllerManager controllerManager = new ControllerManager();
    private final BaseHandler routeHandler = new BaseHandler(this);
    private final WebSocketHandler webSocketHandler = new WebSocketHandler(this);
    private final EventDispatcher eventDispatcher = new EventDispatcher();
    private final ActionRegistry actionRegistry = new ActionRegistry();
    private final WidgetRegistry widgetRegistry = new WidgetRegistry();
    private String databaseFile = "voxeldash.db";
    private File serverRoot = new File(System.getProperty("user.dir"));
    private File logFile = new File("logs/latest.log");
    private Undertow httpServer;
    private ScheduleExecutor scheduleExecutor;
    private ConnectionConfig connection;
    private MasterTunnelClient tunnel;
    private String internalToken;
    private static final String INTERNAL_ACCOUNT = "__voxeldash_internal__";

    /**
     * Registers a pipe with the given type
     *
     * @param pipeType the type of the pipe
     * @param pipe     the pipe to register
     */
    public void registerPipe(Class<? extends BasePipe> pipeType, BasePipe pipe) {
        pipes.put(pipeType, pipe);
    }

    /**
     * Initializes the server
     */
    private void initialize() {
        connection = ConnectionConfig.detect();

        registerRoutes();

        controllerManager.setConnection(String.format("jdbc:sqlite:%s", databaseFile));

        PathHandler handler = new PathHandler()
                .addPrefixPath("/api", routeHandler)
                .addPrefixPath("/api/ws", new WebSocketProtocolHandshakeHandler(webSocketHandler))
                .addPrefixPath("/", new StaticHandler());

        if (connection != null) {
            httpServer = Undertow.builder().addHttpListener(connection.getApiPort(), "127.0.0.1").setHandler(handler).build();
        } else {
            httpServer = Undertow.builder().addHttpListener(7867, "0.0.0.0").setHandler(handler).build();
        }

        controllerManager.registerController(AccountController.class);

        controllerManager.registerController(SessionController.class);

        controllerManager.registerController(PermissionController.class);

        controllerManager.registerController(SSHController.class);
        getController(SSHController.class).initialize(getController(AccountController.class), serverRoot);

        controllerManager.registerController(ScheduleController.class);

        controllerManager.registerController(ApiKeyController.class);

        registerFeatures(Feature.UserManagement);

        if (connection != null) {
            prepareConnectedMode();
        }

        scheduleExecutor = new ScheduleExecutor(this);
        scheduleExecutor.start();
    }

    private void prepareConnectedMode() {
        AccountController accounts = getController(AccountController.class);
        SessionController sessions = getController(SessionController.class);
        PermissionController permissions = getController(PermissionController.class);

        if (!accounts.accountExists(INTERNAL_ACCOUNT)) {
            accounts.createAccount(INTERNAL_ACCOUNT, java.util.UUID.randomUUID().toString());
        }

        int userId = accounts.getUserId(INTERNAL_ACCOUNT);

        for (Feature feature : Feature.values()) {
            permissions.setPermission(userId, feature, PermissionLevel.FULL);
        }

        sessions.destroyAllSessionsForUser(userId);
        internalToken = sessions.generateSessionToken(userId, "voxeldash-internal");
    }

    public void registerRoutes() {
        for (Class<? extends BaseRoute> clazz : ROUTES) {
            try {
                BaseRoute baseRoute = clazz.getDeclaredConstructor().newInstance();

                baseRoute.setLoader(this);
                baseRoute.setServerRoot(serverRoot);

                for (Method method : clazz.getDeclaredMethods()) {
                    Path routePath = method.getAnnotation(Path.class);
                    if (routePath == null) {
                        continue;
                    }

                    de.gnm.voxeldash.api.annotations.Method routeMethod = method.getAnnotation(de.gnm.voxeldash.api.annotations.Method.class);

                    RouteMeta routeMeta = new RouteMeta(baseRoute, method, routeMethod != null ? routeMethod.value() : HTTPMethod.GET, routePath.value());
                    routeHandler.registerRoute(routeMeta);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Explicit registry of every {@link BaseRoute} to register at startup
     * (see {@link #registerRoutes()}).
     */
    private static final List<Class<? extends BaseRoute>> ROUTES = Arrays.asList(
            BackupRouter.class, FileRouter.class, FolderRouter.class, InfoRouter.class,
            PingRouter.class, PlayerRouter.class, PropertyRouter.class, QuickActionRouter.class,
            ResourceRouter.class, StoreRouter.class, ScheduleRouter.class, SSHRouter.class,
            SessionRouter.class, UserRouter.class, WidgetRouter.class, WorldRouter.class
    );

    /**
     * Gets the name of the route package
     *
     * @return the name of the route package
     */
    public static String getRoutePackageName() {
        return VoxelDashLoader.class.getPackage().getName() + ".api.routes";
    }

    /**
     * Starts the server
     */
    public void startup() {
        initialize();

        if (httpServer != null) {
            httpServer.start();
        }

        if (connection != null && internalToken != null) {
            tunnel = new MasterTunnelClient(this, connection, internalToken);
            tunnel.connect();
        }
    }

    /**
     * Shuts down the server
     */
    public void shutdown() {
        if (tunnel != null) {
            tunnel.shutdown();
            tunnel = null;
        }

        if (scheduleExecutor != null) {
            scheduleExecutor.stop();
        }

        pipes.clear();

        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
    }

    /**
     * Gets a pipe of the given type
     *
     * @param pipeType the type of the pipe
     * @param <T>      the type of the pipe
     * @return the pipe
     */
    public <T> T getPipe(Class<T> pipeType) {
        BasePipe pipe = pipes.get(pipeType);
        if (pipe == null) {
            throw new IllegalStateException("No handler registered for type: " + pipeType.getName());
        }

        if (pipeType.isInstance(pipe)) {
            return (T) pipe;
        } else {
            throw new IllegalStateException("Registered handler is not of type: " + pipeType.getName());
        }
    }

    /**
     * Gets the event dispatcher
     *
     * @return the event dispatcher
     */
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Gets the action registry for schedule actions
     *
     * @return the action registry
     */
    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    /**
     * Gets the widget registry for dashboard widgets
     *
     * @return the widget registry
     */
    public WidgetRegistry getWidgetRegistry() {
        return widgetRegistry;
    }

    /**
     * Gets a controller of the given type
     *
     * @param controllerType the type of the controller
     * @param <T>            the type of the controller
     * @return the controller
     */
    public <T> T getController(Class<T> controllerType) {
        return controllerManager.getController(controllerType);
    }

    /**
     * Registers a feature
     *
     * @param feature the feature to register
     */
    public void registerFeatures(Feature... feature) {
        availableFeatures.addAll(Arrays.asList(feature));
    }

    /**
     * Gets the available features
     *
     * @return the available features
     */
    public List<Feature> getAvailableFeatures() {
        return availableFeatures;
    }

    /**
     * Sets the database file
     *
     * @param databaseFile the database file
     */
    public void setDatabaseFile(String databaseFile) {
        this.databaseFile = databaseFile;
    }

    /**
     * Sets and creates the server root
     *
     * @param serverRoot the server root
     */
    public void setServerRoot(File serverRoot) {
        if (!serverRoot.exists()) {
            serverRoot.mkdirs();
        }

        if (!serverRoot.isDirectory()) {
            throw new IllegalArgumentException("The server root must be a directory.");
        }

        this.serverRoot = serverRoot;
    }

    /**
     * Gets the log file path
     *
     * @return the log file
     */
    public File getLogFile() {
        return logFile;
    }

    /**
     * Sets the log file path for console output
     *
     * @param logFile the log file
     */
    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }
}
