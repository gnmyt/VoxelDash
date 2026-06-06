package de.gnm.voxeldash.api.entities.profiling;

public class ProfilingConfig {

    /**
     * Sampling interval in milliseconds. Lower = more accurate, more overhead.
     * Clamped to a sane range by the profiler.
     */
    public int intervalMs = 10;

    /**
     * Auto-stop after this many seconds. {@code 0} means run until explicitly
     * stopped. Clamped to a maximum by the profiler.
     */
    public int durationSec = 0;

    /**
     * Whether to sample all server-owned threads rather than just the main
     * tick thread. Reserved; currently only the main thread is sampled.
     */
    public boolean allThreads = false;

    public ProfilingConfig() {
    }
}
