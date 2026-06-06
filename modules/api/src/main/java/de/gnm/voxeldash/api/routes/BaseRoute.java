package de.gnm.voxeldash.api.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gnm.voxeldash.VoxelDashLoader;

import java.io.File;

public abstract class BaseRoute {

    private final ObjectMapper mapper = new ObjectMapper();
    VoxelDashLoader loader;
    File serverRoot;

    /**
     * Get a controller by its type
     *
     * @param controllerType The type of the controller
     * @param <T>            The type of the controller
     * @return The controller
     */
    public <T> T getController(Class<T> controllerType) {
        return loader.getController(controllerType);
    }

    /**
     * Gets a pipe of the given type
     *
     * @param pipeType the type of the pipe
     * @param <T>      the type of the pipe
     * @return the pipe
     */
    public <T> T getPipe(Class<T> pipeType) {
        return loader.getPipe(pipeType);
    }

    /**
     * Gets a pipe of the given type, or {@code null} if no implementation is
     * registered on this platform. Use this for optional/capability-gated pipes
     * (e.g. inventory on a proxy) instead of {@link #getPipe(Class)}, which throws.
     *
     * @param pipeType the type of the pipe
     * @param <T>      the type of the pipe
     * @return the pipe, or null if not registered
     */
    public <T> T getPipeOrNull(Class<T> pipeType) {
        return loader.getPipeOrNull(pipeType);
    }

    /**
     * Sets the loader
     *
     * @param loader The loader
     */
    public void setLoader(VoxelDashLoader loader) {
        this.loader = loader;
    }

    /**
     * Gets the loader
     *
     * @return The loader
     */
    public VoxelDashLoader getLoader() {
        return loader;
    }

    /**
     * Set the server root
     *
     * @return The server root
     */
    public File getServerRoot() {
        return serverRoot;
    }

    /**
     * Get the object mapper
     * @return The object mapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Set the server root
     *
     * @param serverRoot The server root
     */
    public void setServerRoot(File serverRoot) {
        this.serverRoot = serverRoot;
    }
}
