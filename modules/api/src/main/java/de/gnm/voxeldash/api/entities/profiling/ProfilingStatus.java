package de.gnm.voxeldash.api.entities.profiling;

public class ProfilingStatus {

    /**
     * Whether a sampling session is currently active.
     */
    public boolean running;

    /**
     * Milliseconds elapsed since the active session started (0 when idle).
     */
    public long elapsedMs;

    /**
     * Number of stack samples collected so far in the active session.
     */
    public long sampleCount;

    /**
     * Whether sampling is supported on this platform at all. {@code false} on
     * out-of-process platforms (vanilla) where only live metrics are available.
     */
    public boolean supported = true;

    /**
     * Optional human-readable note (e.g. why sampling is unsupported).
     */
    public String message;

    public ProfilingStatus() {
    }

    public static ProfilingStatus unsupported(String message) {
        ProfilingStatus status = new ProfilingStatus();
        status.supported = false;
        status.running = false;
        status.message = message;
        return status;
    }
}
