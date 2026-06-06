package de.gnm.voxeldash.api.entities.profiling;

public class ProfilingCapabilities {

    /**
     * Whether in-process thread sampling is available.
     */
    public boolean sampling;

    public ProfilingCapabilities() {
    }

    public ProfilingCapabilities(boolean sampling) {
        this.sampling = sampling;
    }

    /**
     * Capabilities for an in-process platform that can sample the server thread.
     */
    public static ProfilingCapabilities inProcessSampling() {
        return new ProfilingCapabilities(true);
    }
}
