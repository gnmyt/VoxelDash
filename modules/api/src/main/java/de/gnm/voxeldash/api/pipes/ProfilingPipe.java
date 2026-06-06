package de.gnm.voxeldash.api.pipes;

import de.gnm.voxeldash.api.entities.profiling.*;

public interface ProfilingPipe extends BasePipe {

    /**
     * @return what this platform can profile (sampling, deep mode, etc.).
     */
    ProfilingCapabilities getCapabilities();

    /**
     * @return a cheap snapshot of current performance (TPS, MSPT, memory, GC).
     */
    LiveMetrics getMetrics();

    /**
     * Starts a sampling session. On platforms without sampling support this
     * returns an unsupported status and does nothing.
     *
     * @param config the session parameters
     * @return the resulting status
     */
    ProfilingStatus start(ProfilingConfig config);

    /**
     * @return the current profiler status (running, elapsed, sample count).
     */
    ProfilingStatus getStatus();

    /**
     * Stops the active session (if any) and returns the aggregated result.
     *
     * @return the call tree and per-resource cost breakdown
     */
    ProfilingResult stop();
}
