package de.gnm.voxeldash.widgets;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.controller.WidgetRegistry;
import de.gnm.voxeldash.api.entities.widget.Widget;
import de.gnm.voxeldash.api.entities.widget.WidgetSize;
import de.gnm.voxeldash.api.entities.widget.WidgetType;
import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import net.minecraft.server.MinecraftServer;

import java.lang.management.ManagementFactory;
import java.util.*;

public class ForgeWidgetProvider {

    private final VoxelDashMod mod;
    private final WidgetDataCollector dataCollector;

    public ForgeWidgetProvider(VoxelDashMod mod) {
        this.mod = mod;
        this.dataCollector = new WidgetDataCollector(this::getCurrentTps);
    }

    /**
     * Registers all widgets and starts data collection
     */
    public void register() {
        dataCollector.start(10);

        WidgetRegistry registry = mod.getLoader().getWidgetRegistry();
        ServerInfoPipe serverInfoPipe = mod.getLoader().getPipe(ServerInfoPipe.class);

        registry.registerWidget(new Widget(
                "memory_usage",
                "overview.widgets.memory_usage",
                WidgetType.AREA_CHART,
                dataCollector::getMemoryData,
                WidgetSize.CHART,
                "hsl(var(--chart-1))",
                "MB"
        ));

        registry.registerWidget(new Widget(
                "cpu_usage",
                "overview.widgets.cpu_usage",
                WidgetType.LINE_CHART,
                dataCollector::getCpuData,
                WidgetSize.CHART,
                "hsl(var(--chart-2))",
                "%"
        ));

        registry.registerWidget(new Widget(
                "tps",
                "overview.widgets.tps",
                WidgetType.LINE_CHART,
                dataCollector::getTpsData,
                WidgetSize.CHART,
                "hsl(var(--chart-4))",
                "tps"
        ));

        registry.registerWidget(new Widget(
                "server_info",
                "overview.widgets.server_info",
                WidgetType.INFO_CARD,
                WidgetSize.INFO,
                "hsl(var(--primary))",
                () -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("software", serverInfoPipe.getServerSoftware());
                    info.put("version", serverInfoPipe.getServerVersion());
                    info.put("port", serverInfoPipe.getServerPort());
                    return info;
                }
        ));

        registry.registerWidget(new Widget(
                "online_players",
                "overview.widgets.online_players",
                WidgetType.STAT_CARD,
                WidgetSize.INFO,
                "hsl(var(--chart-3))",
                () -> {
                    Map<String, Object> stats = new LinkedHashMap<>();
                    MinecraftServer server = VoxelDashMod.getServer();
                    if (server != null) {
                        stats.put("value", server.getPlayerCount());
                        stats.put("max", server.getMaxPlayers());
                    } else {
                        stats.put("value", 0);
                        stats.put("max", 0);
                    }
                    return stats;
                }
        ));

        registry.registerWidget(new Widget(
                "memory_stat",
                "overview.widgets.memory_stat",
                WidgetType.PROGRESS,
                WidgetSize.INFO,
                "hsl(var(--muted-foreground))",
                () -> {
                    Map<String, Object> stats = new LinkedHashMap<>();
                    stats.put("used", dataCollector.getCurrentMemoryUsage());
                    stats.put("max", dataCollector.getMaxMemory());
                    stats.put("allocated", dataCollector.getAllocatedMemory());
                    stats.put("percentage", dataCollector.getMemoryPercentage());
                    return stats;
                }
        ));

        registry.registerWidget(new Widget(
                "tps_stat",
                "overview.widgets.tps_stat",
                WidgetType.STAT_CARD,
                WidgetSize.INFO,
                "hsl(var(--foreground))",
                () -> {
                    double tps = getCurrentTps();
                    Map<String, Object> stats = new LinkedHashMap<>();
                    stats.put("value", Math.round(tps * 100.0) / 100.0);
                    stats.put("max", 20);
                    stats.put("status", tps >= 19 ? "good" : tps >= 15 ? "warning" : "critical");
                    return stats;
                }
        ));

        registry.registerWidget(new Widget(
                "uptime",
                "overview.widgets.uptime",
                WidgetType.STAT_CARD,
                WidgetSize.INFO,
                "hsl(var(--chart-5))",
                () -> {
                    long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
                    long seconds = uptimeMs / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;

                    Map<String, Object> stats = new LinkedHashMap<>();
                    stats.put("days", days);
                    stats.put("hours", hours % 24);
                    stats.put("minutes", minutes % 60);
                    stats.put("seconds", seconds % 60);
                    stats.put("totalSeconds", seconds);
                    return stats;
                }
        ));

        registry.registerWidget(new Widget(
                "worlds",
                "overview.widgets.worlds",
                WidgetType.INFO_CARD,
                WidgetSize.INFO,
                "hsl(var(--chart-2))",
                () -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("count", dataCollector.getWorldCount());
                    info.put("entities", dataCollector.getEntityCount());
                    info.put("loadedChunks", dataCollector.getLoadedChunkCount());
                    return info;
                }
        ));

        registry.registerWidget(new Widget(
                "system_info",
                "overview.widgets.system_info",
                WidgetType.INFO_CARD,
                WidgetSize.INFO,
                "hsl(var(--muted-foreground))",
                () -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("javaVersion", System.getProperty("java.version"));
                    info.put("os", System.getProperty("os.name"));
                    info.put("processors", dataCollector.getAvailableProcessors());
                    return info;
                }
        ));
    }

    /**
     * Stops data collection
     */
    public void shutdown() {
        dataCollector.stop();
    }

    /**
     * Gets current server TPS
     */
    private double getCurrentTps() {
        MinecraftServer server = VoxelDashMod.getServer();
        if (server == null) {
            return 20.0;
        }

        try {
            long[] tickTimes = server.getTickTimesNanos();
            if (tickTimes == null || tickTimes.length == 0) {
                return 20.0;
            }

            long sum = 0;
            for (long tickTime : tickTimes) {
                sum += tickTime;
            }
            double avgTickTime = sum / (double) tickTimes.length;

            double tps = 1_000_000_000.0 / avgTickTime;
            return Math.min(20.0, tps);
        } catch (Throwable t) {
            return 20.0;
        }
    }
}
