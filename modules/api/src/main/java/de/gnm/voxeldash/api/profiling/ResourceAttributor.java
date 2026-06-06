package de.gnm.voxeldash.api.profiling;

public interface ResourceAttributor {

    /**
     * Attributes a single stack frame to an owning resource. Must never return
     * {@code null}; use {@link ResourceRef#UNKNOWN} when the owner cannot be
     * determined.
     *
     * @param className fully-qualified declaring class name of the frame
     * @return the owning resource (never {@code null})
     */
    ResourceRef attribute(String className);
}
