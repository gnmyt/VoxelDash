package de.gnm.voxeldash.api.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ControllerManager {

    private final Map<Class<?>, BaseController> controller = new HashMap<>();
    Connection connection = null;

    /**
     * Set the connection to the database
     *
     * @param jdbcUrl The JDBC URL of the database
     */
    public void setConnection(String jdbcUrl) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(jdbcUrl);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register a controller
     *
     * @param controllerType The type of the controller
     */
    public void registerController(Class<?> controllerType) {
        controller.put(controllerType, createControllerInstance(controllerType));
    }

    /**
     * Get a controller
     *
     * @param controllerType The type of the controller
     * @param <T>            The type of the controller
     * @return The controller
     */
    public <T> T getController(Class<T> controllerType) {
        BaseController controllerInstance = controller.get(controllerType);
        if (controllerInstance == null) {
            throw new IllegalStateException("No controller registered for type: " + controllerType.getName());
        }

        if (controllerType.isInstance(controllerInstance)) {
            return (T) controllerInstance;
        } else {
            throw new IllegalStateException("Registered controller is not of type: " + controllerType.getName());
        }
    }

    /**
     * Create an instance of a controller
     *
     * @param controllerType The type of the controller
     * @return The controller instance
     */
    private BaseController createControllerInstance(Class<?> controllerType) {
        try {
            return (BaseController) controllerType.getConstructor(Connection.class).newInstance(connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
