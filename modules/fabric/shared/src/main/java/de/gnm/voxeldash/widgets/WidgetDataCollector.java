package de.gnm.voxeldash.widgets;

import de.gnm.voxeldash.api.entities.widget.WidgetDataPoint;
import de.gnm.voxeldash.util.FabricCompat;
import de.gnm.voxeldash.util.FabricUtil;

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

    public WidgetDataCollector(Supplier<Double> tpsSupplier) {
        this.tpsSupplier = tpsSupplier;
    }

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
                FabricUtil.compat().runOnMainThreadAsync(() -> collectMainThreadData());
            }
        }, 0, intervalSeconds * 1000L);
    }

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

    private void collectMainThreadData() {
        String timeLabel = TIME_FORMAT.format(new Date());
        long timestamp = System.currentTimeMillis();

        double tps = tpsSupplier.get();
        addDataPoint(tpsData, new WidgetDataPoint(timestamp, timeLabel, tps));

        FabricCompat.WorldStats stats = FabricUtil.compat().worldStats();
        if (stats != null) {
            cachedWorldCount.set(stats.worldCount);
            cachedEntityCount.set(stats.entityCount);
            cachedChunkCount.set(stats.loadedChunkCount);
        }
    }

    private void addDataPoint(Deque<WidgetDataPoint> deque, WidgetDataPoint point) {
        deque.addLast(point);
        while (deque.size() > MAX_DATA_POINTS) {
            deque.removeFirst();
        }
    }

    public List<WidgetDataPoint> getMemoryData() {
        return new ArrayList<>(memoryData);
    }

    public List<WidgetDataPoint> getCpuData() {
        return new ArrayList<>(cpuData);
    }

    public List<WidgetDataPoint> getTpsData() {
        return new ArrayList<>(tpsData);
    }

    public int getWorldCount() {
        return cachedWorldCount.get();
    }

    public int getEntityCount() {
        return cachedEntityCount.get();
    }

    public int getLoadedChunkCount() {
        return cachedChunkCount.get();
    }

    public long getCurrentMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    public long getMaxMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
    }

    public long getAllocatedMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024);
    }

    public double getMemoryPercentage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long used = memoryBean.getHeapMemoryUsage().getUsed();
        long max = memoryBean.getHeapMemoryUsage().getMax();
        return (double) used / max * 100;
    }

    public int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }
}
