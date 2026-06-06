package de.gnm.voxeldash.api.entities.profiling;

public class LiveMetrics {

    /**
     * Ticks per second (0-20). 20 is healthy.
     */
    public double tps;

    /**
     * Mean milliseconds per tick. Anything above 50 means the server cannot
     * keep up with 20 TPS.
     */
    public double mspt;

    /**
     * Used heap memory in megabytes.
     */
    public long heapUsedMb;

    /**
     * Maximum heap memory in megabytes (-Xmx).
     */
    public long heapMaxMb;

    /**
     * Used non-heap (metaspace etc.) memory in megabytes.
     */
    public long nonHeapMb;

    /**
     * Total number of garbage collections since JVM start.
     */
    public long gcCount;

    /**
     * Total time spent in garbage collection since JVM start, in milliseconds.
     */
    public long gcTimeMs;

    /**
     * Number of live JVM threads.
     */
    public int threadCount;

    /**
     * Total loaded entities across all worlds (-1 if unknown).
     */
    public int entityCount = -1;

    /**
     * Total loaded chunks across all worlds (-1 if unknown).
     */
    public int chunkCount = -1;

    public LiveMetrics() {
    }
}
