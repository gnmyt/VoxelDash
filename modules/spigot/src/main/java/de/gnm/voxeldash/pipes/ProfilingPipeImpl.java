package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashSpigot;
import de.gnm.voxeldash.api.entities.profiling.*;
import de.gnm.voxeldash.api.pipes.ProfilingPipe;
import de.gnm.voxeldash.api.profiling.JarPackageIndex;
import de.gnm.voxeldash.api.profiling.PackagePrefixAttributor;
import de.gnm.voxeldash.api.profiling.ResourceRef;
import de.gnm.voxeldash.api.profiling.SamplingProfiler;
import de.gnm.voxeldash.util.BukkitUtil;
import de.gnm.voxeldash.util.SchedulerCompat;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.io.InputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ProfilingPipeImpl implements ProfilingPipe {

    private final VoxelDashSpigot plugin;

    private volatile Thread serverThread;
    private volatile int entityCount = -1;
    private volatile int chunkCount = -1;

    private SamplingProfiler profiler;
    private ProfilingResult lastResult;

    public ProfilingPipeImpl(VoxelDashSpigot plugin) {
        this.plugin = plugin;
        BukkitUtil.runOnMainThread(() -> serverThread = Thread.currentThread());
        SchedulerCompat.runTimer(plugin, this::updateCounts, 0L, 40L);
    }

    @Override
    public ProfilingCapabilities getCapabilities() {
        return ProfilingCapabilities.inProcessSampling();
    }

    @Override
    public LiveMetrics getMetrics() {
        LiveMetrics metrics = new LiveMetrics();
        metrics.tps = currentTps();
        metrics.mspt = currentMspt();

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
        metrics.entityCount = entityCount;
        metrics.chunkCount = chunkCount;
        return metrics;
    }

    @Override
    public synchronized ProfilingStatus start(ProfilingConfig config) {
        if (serverThread == null) {
            BukkitUtil.runOnMainThread(() -> serverThread = Thread.currentThread());
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
        if (profiler == null) {
            return new ProfilingStatus();
        }
        return profiler.status();
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

    private void updateCounts() {
        try {
            int entities = 0;
            int chunks = 0;
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                entities += world.getEntities().size();
                chunks += world.getLoadedChunks().length;
            }
            entityCount = entities;
            chunkCount = chunks;
        } catch (Throwable ignored) {
        }
    }

    private double currentTps() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            double[] recentTps = (double[]) server.getClass().getField("recentTps").get(server);
            return Math.min(20.0, recentTps[0]);
        } catch (Throwable t) {
            return 20.0;
        }
    }

    private double currentMspt() {
        try {
            Object value = Bukkit.getServer().getClass().getMethod("getAverageTickTime").invoke(Bukkit.getServer());
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Throwable ignored) {
        }
        return 0.0;
    }

    /**
     * Builds the package-prefix attributor by scanning every plugin jar in the
     * plugins folder for its declared name, main class and class packages.
     */
    private PackagePrefixAttributor buildAttributor() {
        PackagePrefixAttributor attributor = new PackagePrefixAttributor();
        File pluginsDir = plugin.getDataFolder().getParentFile();
        File[] jars = pluginsDir != null ? pluginsDir.listFiles((dir, name) -> name.endsWith(".jar")) : null;
        if (jars == null) {
            return attributor;
        }
        for (File jar : jars) {
            if (jar.getName().toLowerCase().contains("voxeldash")) {
                continue;
            }
            PluginDescriptionFile desc = readDescription(jar);
            String name = desc != null ? desc.getName() : jar.getName().replace(".jar", "");
            if (desc != null) {
                String mainPrefix = JarPackageIndex.mainClassPrefix(desc.getMain());
                if (mainPrefix != null) {
                    attributor.register(mainPrefix, name, ResourceRef.TYPE_PLUGIN);
                }
            }
            for (String prefix : JarPackageIndex.derivePrefixes(jar)) {
                attributor.register(prefix, name, ResourceRef.TYPE_PLUGIN);
            }
        }
        return attributor;
    }

    private PluginDescriptionFile readDescription(File jar) {
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry("plugin.yml");
            if (entry == null) {
                return null;
            }
            try (InputStream in = jarFile.getInputStream(entry)) {
                return new PluginDescriptionFile(in);
            }
        } catch (Throwable t) {
            return null;
        }
    }
}
