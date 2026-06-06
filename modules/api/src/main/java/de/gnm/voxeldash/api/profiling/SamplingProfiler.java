package de.gnm.voxeldash.api.profiling;

import de.gnm.voxeldash.api.entities.profiling.CallNode;
import de.gnm.voxeldash.api.entities.profiling.ProfilingResult;
import de.gnm.voxeldash.api.entities.profiling.ProfilingStatus;
import de.gnm.voxeldash.api.entities.profiling.ResourceCost;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class SamplingProfiler {

    private static final int MIN_INTERVAL_MS = 1;
    private static final int MAX_INTERVAL_MS = 1000;
    private static final long MAX_DURATION_MS = 10 * 60 * 1000L;

    private final Thread target;
    private final ResourceAttributor attributor;
    private final int intervalMs;
    private final long maxDurationMs;

    private final CallNode root = new CallNode(null, "(root)", null, null);
    private final AtomicLong sampleCount = new AtomicLong();

    private volatile boolean sampling;
    private volatile long startNanos;
    private volatile long endNanos;
    private Thread worker;

    /**
     * @param target     the thread to sample (typically the server tick thread)
     * @param attributor maps frames to plugins/mods
     * @param intervalMs requested sampling interval (clamped to a sane range)
     * @param durationMs auto-stop after this many ms; {@code <= 0} means run
     *                   until {@link #stop()} (still capped at 10 minutes)
     */
    public SamplingProfiler(Thread target, ResourceAttributor attributor, int intervalMs, long durationMs) {
        this.target = target;
        this.attributor = attributor;
        this.intervalMs = Math.max(MIN_INTERVAL_MS, Math.min(MAX_INTERVAL_MS, intervalMs));
        this.maxDurationMs = durationMs > 0 ? Math.min(durationMs, MAX_DURATION_MS) : MAX_DURATION_MS;
    }

    /**
     * Aggregates the call tree into a per-resource cost breakdown. Self time is
     * summed per resource over every frame; total time counts each sample once
     * per resource (at the resource's topmost frame on the path) to avoid
     * double-counting recursion.
     */
    static List<ResourceCost> aggregateByResource(CallNode root, long totalSamples) {
        Map<String, ResourceCost> costs = new LinkedHashMap<>();
        collectSelf(root, costs);
        collectTotal(root, new HashSet<String>(), costs);

        List<ResourceCost> list = new ArrayList<>(costs.values());
        double denom = totalSamples > 0 ? totalSamples : 1;
        for (ResourceCost cost : list) {
            cost.selfPct = cost.selfSamples * 100.0 / denom;
            cost.totalPct = cost.totalSamples * 100.0 / denom;
        }
        list.sort(Comparator
                .comparingLong((ResourceCost c) -> c.selfSamples)
                .thenComparingLong(c -> c.totalSamples)
                .reversed());
        return list;
    }

    private static void collectSelf(CallNode node, Map<String, ResourceCost> costs) {
        if (node.resource != null && node.selfSamples > 0) {
            costOf(costs, node.resource, node.resourceType).selfSamples += node.selfSamples;
        }
        for (CallNode child : node.children) {
            collectSelf(child, costs);
        }
    }

    private static void collectTotal(CallNode node, Set<String> seen, Map<String, ResourceCost> costs) {
        for (CallNode child : node.children) {
            boolean added = false;
            if (child.resource != null && !seen.contains(child.resource)) {
                costOf(costs, child.resource, child.resourceType).totalSamples += child.totalSamples;
                seen.add(child.resource);
                added = true;
            }
            collectTotal(child, seen, costs);
            if (added) {
                seen.remove(child.resource);
            }
        }
    }

    private static ResourceCost costOf(Map<String, ResourceCost> costs, String resource, String type) {
        ResourceCost cost = costs.get(resource);
        if (cost == null) {
            cost = new ResourceCost(resource, type);
            costs.put(resource, cost);
        }
        return cost;
    }

    /**
     * Starts the sampling loop. No-op if already running.
     */
    public synchronized void start() {
        if (sampling) {
            return;
        }
        sampling = true;
        startNanos = System.nanoTime();
        endNanos = 0;
        worker = new Thread(this::loop, "VoxelDash-Profiler");
        worker.setDaemon(true);
        worker.start();
    }

    private void loop() {
        long deadlineNanos = startNanos + maxDurationMs * 1_000_000L;
        while (sampling) {
            if (System.nanoTime() >= deadlineNanos) {
                break;
            }
            StackTraceElement[] trace = target.getStackTrace();
            if (trace.length > 0) {
                record(trace);
                sampleCount.incrementAndGet();
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                break;
            }
        }
        sampling = false;
        endNanos = System.nanoTime();
    }

    /**
     * Folds a single stack trace into the call tree. {@code trace[0]} is the
     * top (currently-executing) frame, so we walk from the bottom up to build a
     * root→leaf path and credit the top frame with self time.
     */
    private void record(StackTraceElement[] trace) {
        synchronized (root) {
            CallNode node = root;
            root.totalSamples++;
            for (int i = trace.length - 1; i >= 0; i--) {
                StackTraceElement frame = trace[i];
                String className = frame.getClassName();
                ResourceRef ref = attributor.attribute(className);
                node = node.child(className, frame.getMethodName(), ref.name, ref.type);
                node.totalSamples++;
            }
            node.selfSamples++;
        }
    }

    /**
     * @return a live status snapshot (safe to call at any time)
     */
    public ProfilingStatus status() {
        ProfilingStatus status = new ProfilingStatus();
        status.running = sampling;
        status.sampleCount = sampleCount.get();
        status.elapsedMs = elapsedMs();
        return status;
    }

    private long elapsedMs() {
        if (startNanos == 0) {
            return 0;
        }
        long end = (!sampling && endNanos != 0) ? endNanos : System.nanoTime();
        return (end - startNanos) / 1_000_000L;
    }

    /**
     * Stops sampling (if running) and builds the aggregated result.
     *
     * @return the profiling result with the call tree and per-resource costs
     */
    public ProfilingResult stop() {
        sampling = false;
        Thread w = worker;
        if (w != null) {
            w.interrupt();
            try {
                w.join(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        synchronized (root) {
            ProfilingResult result = new ProfilingResult();
            result.root = root;
            result.sampleCount = sampleCount.get();
            result.durationMs = elapsedMs();
            result.threadName = target.getName();
            result.byResource = aggregateByResource(root, result.sampleCount);
            return result;
        }
    }
}
