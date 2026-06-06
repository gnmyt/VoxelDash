package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.profiling.LiveMetrics;
import de.gnm.voxeldash.api.entities.profiling.ProfilingCapabilities;
import de.gnm.voxeldash.api.entities.profiling.ProfilingConfig;
import de.gnm.voxeldash.api.entities.profiling.ProfilingResult;
import de.gnm.voxeldash.api.entities.profiling.ProfilingStatus;
import de.gnm.voxeldash.api.pipes.ProfilingPipe;
import de.gnm.voxeldash.api.profiling.JarPackageIndex;
import de.gnm.voxeldash.api.profiling.PackagePrefixAttributor;
import de.gnm.voxeldash.api.profiling.ResourceRef;
import de.gnm.voxeldash.api.profiling.SamplingProfiler;
import de.gnm.voxeldash.util.ForgeCompat;
import de.gnm.voxeldash.util.ForgeUtil;

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ProfilingPipeImpl implements ProfilingPipe {

    private static final Set<String> BUILTIN_MODS = new HashSet<>(Arrays.asList(
            "java", "minecraft", "forge", "neoforge", "fml", "voxeldash"));

    private volatile Thread serverThread;
    private SamplingProfiler profiler;
    private ProfilingResult lastResult;

    public ProfilingPipeImpl() {
        captureServerThread();
    }

    private static ForgeCompat compat() {
        return ForgeUtil.compat();
    }

    @Override
    public ProfilingCapabilities getCapabilities() {
        return ProfilingCapabilities.inProcessSampling();
    }

    @Override
    public LiveMetrics getMetrics() {
        LiveMetrics metrics = new LiveMetrics();
        try {
            metrics.tps = compat().currentTps();
        } catch (Throwable ignored) {
            metrics.tps = 20.0;
        }

        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        metrics.heapUsedMb = memory.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long max = memory.getHeapMemoryUsage().getMax();
        metrics.heapMaxMb = max > 0 ? max / (1024 * 1024) : 0;
        metrics.nonHeapMb = memory.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);

        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() > 0) gcCount += gc.getCollectionCount();
            if (gc.getCollectionTime() > 0) gcTime += gc.getCollectionTime();
        }
        metrics.gcCount = gcCount;
        metrics.gcTimeMs = gcTime;
        metrics.threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

        try {
            ForgeCompat.WorldStats stats = compat().worldStats();
            if (stats != null) {
                metrics.entityCount = stats.entityCount;
                metrics.chunkCount = stats.loadedChunkCount;
            }
        } catch (Throwable ignored) {
        }
        return metrics;
    }

    @Override
    public synchronized ProfilingStatus start(ProfilingConfig config) {
        if (serverThread == null) {
            captureServerThread();
        }
        if (serverThread == null) {
            return ProfilingStatus.unsupported("Could not locate the server thread.");
        }
        if (profiler != null) {
            profiler.stop();
        }
        profiler = new SamplingProfiler(serverThread, buildAttributor(),
                config.intervalMs, config.durationSec * 1000L);
        profiler.start();
        return profiler.status();
    }

    @Override
    public synchronized ProfilingStatus getStatus() {
        return profiler == null ? new ProfilingStatus() : profiler.status();
    }

    @Override
    public synchronized ProfilingResult stop() {
        if (profiler == null) {
            return lastResult != null ? lastResult : new ProfilingResult();
        }
        lastResult = profiler.stop();
        profiler = null;
        return lastResult;
    }

    private void captureServerThread() {
        try {
            compat().runOnMainThread(() -> serverThread = Thread.currentThread());
        } catch (Throwable ignored) {
        }
    }

    private PackagePrefixAttributor buildAttributor() {
        PackagePrefixAttributor attributor = new PackagePrefixAttributor();
        File modsDir = compat().modsFolder();
        if (modsDir == null) {
            return attributor;
        }
        for (ForgeCompat.ModEntry mod : compat().loadedMods()) {
            if (mod.modId != null && BUILTIN_MODS.contains(mod.modId.toLowerCase())) {
                continue;
            }
            if (mod.sourceFileName == null) {
                continue;
            }
            File jar = new File(modsDir, mod.sourceFileName);
            String name = mod.name != null ? mod.name : mod.modId;
            for (String prefix : JarPackageIndex.derivePrefixes(jar)) {
                attributor.register(prefix, name, ResourceRef.TYPE_MOD);
            }
        }
        return attributor;
    }
}
