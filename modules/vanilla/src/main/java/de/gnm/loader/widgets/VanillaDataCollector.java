package de.gnm.loader.widgets;

import de.gnm.loader.helper.NBTHelper;
import de.gnm.loader.helper.PlayerTracker;
import de.gnm.voxeldash.api.entities.widget.WidgetDataPoint;
import de.gnm.voxeldash.api.helper.PropertyHelper;
import net.querz.nbt.tag.CompoundTag;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class VanillaDataCollector {

    private static final int MAX_DATA_POINTS = 60;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    private final Deque<WidgetDataPoint> playerCountData = new ConcurrentLinkedDeque<>();
    private final Deque<WidgetDataPoint> cpuData = new ConcurrentLinkedDeque<>();

    private final PlayerTracker playerTracker;
    private final File serverRoot;

    private Timer collectionTimer;

    public VanillaDataCollector(PlayerTracker playerTracker, File serverRoot) {
        this.playerTracker = playerTracker;
        this.serverRoot = serverRoot;
    }

    /**
     * Starts collecting data at regular intervals
     *
     * @param intervalSeconds Interval between collections in seconds
     */
    public void start(int intervalSeconds) {
        collectionTimer = new Timer("VanillaDataCollector", true);
        collectionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collectData();
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
    }

    /**
     * Collects all data points
     */
    private void collectData() {
        String timeLabel = TIME_FORMAT.format(new Date());
        long timestamp = System.currentTimeMillis();

        int playerCount = playerTracker.getOnlinePlayerCount();
        addDataPoint(playerCountData, new WidgetDataPoint(timestamp, timeLabel, playerCount));

        double cpuUsage = getCpuUsage();
        addDataPoint(cpuData, new WidgetDataPoint(timestamp, timeLabel, cpuUsage));
    }

    /**
     * Gets the system CPU usage
     */
    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage();

            if (cpuLoad < 0) {
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
                } else {
                    cpuLoad = 0;
                }
            } else {
                int processors = Runtime.getRuntime().availableProcessors();
                cpuLoad = (cpuLoad / processors) * 100;
            }

            return Math.max(0, Math.min(100, cpuLoad));
        } catch (Exception e) {
            return 0;
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
     * Gets player count data points
     */
    public List<WidgetDataPoint> getPlayerCountData() {
        return new ArrayList<>(playerCountData);
    }

    /**
     * Gets CPU usage data points
     */
    public List<WidgetDataPoint> getCpuData() {
        return new ArrayList<>(cpuData);
    }

    /**
     * Gets the current player count
     */
    public int getCurrentPlayerCount() {
        return playerTracker.getOnlinePlayerCount();
    }

    /**
     * Gets the max players from server.properties
     */
    public int getMaxPlayers() {
        String maxPlayers = PropertyHelper.getProperty("max-players");
        if (maxPlayers != null) {
            try {
                return Integer.parseInt(maxPlayers);
            } catch (NumberFormatException ignored) {
            }
        }
        return 20;
    }

    /**
     * Gets the world count (vanilla always has 3 dimensions)
     */
    public int getWorldCount() {
        return 3;
    }

    /**
     * Gets the number of available processors
     */
    public int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Gets the world folder
     */
    private File getWorldFolder() {
        String levelName = PropertyHelper.getProperty("level-name");
        if (levelName == null) {
            levelName = "world";
        }
        return new File(serverRoot, levelName);
    }

    /**
     * Gets world data from level.dat
     */
    public CompoundTag getWorldData() {
        return NBTHelper.readLevelData(getWorldFolder());
    }

    /**
     * Gets the current world time
     */
    public long getWorldTime() {
        CompoundTag levelData = getWorldData();
        return NBTHelper.getWorldTime(levelData);
    }

    /**
     * Gets the difficulty
     */
    public String getDifficulty() {
        CompoundTag levelData = getWorldData();
        return NBTHelper.getDifficulty(levelData);
    }

    /**
     * Gets whether the world is hardcore
     */
    public boolean isHardcore() {
        CompoundTag levelData = getWorldData();
        return NBTHelper.isHardcore(levelData);
    }

    /**
     * Formats time in ticks to a human-readable format
     */
    public String formatWorldTime(long ticks) {
        long adjustedTicks = (ticks + 6000) % 24000;
        int hours = (int) (adjustedTicks / 1000);
        int minutes = (int) ((adjustedTicks % 1000) * 60 / 1000);
        return String.format("%02d:%02d", hours, minutes);
    }
}
