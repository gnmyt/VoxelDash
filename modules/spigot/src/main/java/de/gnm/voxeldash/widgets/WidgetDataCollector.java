package de.gnm.voxeldash.widgets;

import de.gnm.voxeldash.api.entities.widget.WidgetDataPoint;
import de.gnm.voxeldash.util.SchedulerCompat;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

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
    private Object schedulerHandle;
    private JavaPlugin plugin;

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
     * @param plugin          The plugin instance for Bukkit scheduler
     */
    public void start(int intervalSeconds, JavaPlugin plugin) {
        this.plugin = plugin;

        collectionTimer = new Timer("WidgetDataCollector", true);
        collectionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collectAsyncSafeData();
            }
        }, 0, intervalSeconds * 1000L);

        schedulerHandle = SchedulerCompat.runTimer(plugin, this::collectMainThreadData, 0L, intervalSeconds * 20L);
    }

    /**
     * Stops the data collection
     */
    public void stop() {
        if (collectionTimer != null) {
            collectionTimer.cancel();
            collectionTimer = null;
        }
        if (schedulerHandle != null) {
            SchedulerCompat.cancel(schedulerHandle);
            schedulerHandle = null;
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

        cachedWorldCount.set(Bukkit.getWorlds().size());
        try {
            int totalEntities = Bukkit.getWorlds().stream()
                    .mapToInt(world -> world.getEntities().size())
                    .sum();
            cachedEntityCount.set(totalEntities);
            int totalChunks = Bukkit.getWorlds().stream()
                    .mapToInt(world -> world.getLoadedChunks().length)
                    .sum();
            cachedChunkCount.set(totalChunks);
        } catch (Throwable ignored) {
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
     * Gets the number of available processors
     */
    public int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }
}
