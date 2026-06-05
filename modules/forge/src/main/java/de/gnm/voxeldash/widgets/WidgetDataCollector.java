package de.gnm.voxeldash.widgets;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.widget.WidgetDataPoint;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class WidgetDataCollector {

    private static final int MAX_DATA_POINTS = 60;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    private final Deque<WidgetDataPoint> memoryData = new ConcurrentLinkedDeque<>();
    private final Deque<WidgetDataPoint> cpuData = new ConcurrentLinkedDeque<>();
    private final Deque<WidgetDataPoint> tpsData = new ConcurrentLinkedDeque<>();

    private final Supplier<Double> tpsSupplier;

    private final AtomicInteger cachedWorldCount = new AtomicInteger(0);
    private final AtomicInteger cachedEntityCount = new AtomicInteger(0);
    private final AtomicInteger cachedChunkCount = new AtomicInteger(0);

    private Timer collectionTimer;
    private Timer mainThreadTimer;

    /**
     * Creates a new data collector
     *
     * @param tpsSupplier Supplier for current TPS
     */
    public WidgetDataCollector(Supplier<Double> tpsSupplier) {
        this.tpsSupplier = tpsSupplier;
    }

    /**
     * Starts collecting data at regular intervals
     *
     * @param intervalSeconds Interval between collections in seconds
     */
    public void start(int intervalSeconds) {
        collectionTimer = new Timer("WidgetDataCollector", true);
        collectionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collectAsyncSafeData();
            }
        }, 0, intervalSeconds * 1000L);

        mainThreadTimer = new Timer("WidgetMainThreadCollector", true);
        mainThreadTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ForgeUtil.runOnMainThreadAsync(() -> collectMainThreadData());
            }
        }, 0, intervalSeconds * 1000L);
    }

    /**
     * Stops the data collection
     */
    public void stop() {
        if (collectionTimer != null) {
            collectionTimer.cancel();
            collectionTimer = null;
        }
        if (mainThreadTimer != null) {
            mainThreadTimer.cancel();
            mainThreadTimer = null;
        }
    }

    /**
     * Collects data that is safe to collect from any thread
     */
    private void collectAsyncSafeData() {
        String timeLabel = TIME_FORMAT.format(new Date());
        long timestamp = System.currentTimeMillis();

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        addDataPoint(memoryData, new WidgetDataPoint(timestamp, timeLabel, usedMemory));

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getSystemLoadAverage();
        if (cpuLoad < 0) {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
            } else {
                cpuLoad = 0;
            }
        }
        addDataPoint(cpuData, new WidgetDataPoint(timestamp, timeLabel, Math.max(0, cpuLoad)));
    }

    /**
     * Collects data that must be collected on the main thread
     */
    private void collectMainThreadData() {
        String timeLabel = TIME_FORMAT.format(new Date());
        long timestamp = System.currentTimeMillis();

        double tps = tpsSupplier.get();
        addDataPoint(tpsData, new WidgetDataPoint(timestamp, timeLabel, tps));

        MinecraftServer server = VoxelDashMod.getServer();
        if (server != null) {
            int worldCount = 0;
            int totalEntities = 0;
            int totalChunks = 0;

            for (ServerLevel world : server.getAllLevels()) {
                worldCount++;
                for (Entity ignored : world.getAllEntities()) {
                    totalEntities++;
                }
                totalChunks += world.getChunkSource().getLoadedChunksCount();
            }

            cachedWorldCount.set(worldCount);
            cachedEntityCount.set(totalEntities);
            cachedChunkCount.set(totalChunks);
        }
    }

    /**
     * Adds a data point to a deque, removing oldest if at capacity
     */
    private void addDataPoint(Deque<WidgetDataPoint> deque, WidgetDataPoint point) {
        deque.addLast(point);
        while (deque.size() > MAX_DATA_POINTS) {
            deque.removeFirst();
        }
    }

    /**
     * Gets memory usage data points
     */
    public List<WidgetDataPoint> getMemoryData() {
        return new ArrayList<>(memoryData);
    }

    /**
     * Gets CPU usage data points
     */
    public List<WidgetDataPoint> getCpuData() {
        return new ArrayList<>(cpuData);
    }

    /**
     * Gets TPS data points
     */
    public List<WidgetDataPoint> getTpsData() {
        return new ArrayList<>(tpsData);
    }

    /**
     * Gets cached world count
     */
    public int getWorldCount() {
        return cachedWorldCount.get();
    }

    /**
     * Gets cached entity count
     */
    public int getEntityCount() {
        return cachedEntityCount.get();
    }

    /**
     * Gets cached loaded chunk count
     */
    public int getLoadedChunkCount() {
        return cachedChunkCount.get();
    }

    /**
     * Gets current memory usage in MB
     */
    public long getCurrentMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    /**
     * Gets maximum memory in MB
     */
    public long getMaxMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
    }

    /**
     * Gets allocated memory in MB
     */
    public long getAllocatedMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024);
    }

    /**
     * Gets memory usage as percentage
     */
    public double getMemoryPercentage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long used = memoryBean.getHeapMemoryUsage().getUsed();
        long max = memoryBean.getHeapMemoryUsage().getMax();
        return (double) used / max * 100;
    }

    /**
     * Gets available processors
     */
    public int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }
}
