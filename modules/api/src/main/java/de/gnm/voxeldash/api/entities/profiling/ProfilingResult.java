package de.gnm.voxeldash.api.entities.profiling;

import java.util.ArrayList;
import java.util.List;

public class ProfilingResult {

    /**
     * Synthetic root of the aggregated call tree (its children are the stack
     * entry points).
     */
    public CallNode root;

    /**
     * Per plugin/mod cost, sorted by self time descending.
     */
    public List<ResourceCost> byResource = new ArrayList<>();

    /**
     * Total number of stack samples collected.
     */
    public long sampleCount;

    /**
     * Wall-clock duration of the session in milliseconds.
     */
    public long durationMs;

    /**
     * Name of the sampled thread.
     */
    public String threadName;

    public ProfilingResult() {
    }
}
