package de.gnm.loader.widgets;

import de.gnm.loader.helper.PlayerTracker;
import de.gnm.voxeldash.api.controller.WidgetRegistry;
import de.gnm.voxeldash.api.entities.widget.Widget;
import de.gnm.voxeldash.api.entities.widget.WidgetSize;
import de.gnm.voxeldash.api.entities.widget.WidgetType;
import de.gnm.voxeldash.api.helper.PropertyHelper;
import de.gnm.voxeldash.api.pipes.ServerInfoPipe;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.*;

public class VanillaWidgetProvider {

    private final WidgetRegistry widgetRegistry;
    private final ServerInfoPipe serverInfoPipe;
    private final VanillaDataCollector dataCollector;

    public VanillaWidgetProvider(WidgetRegistry widgetRegistry, ServerInfoPipe serverInfoPipe,
                                 PlayerTracker playerTracker, File serverRoot) {
        this.widgetRegistry = widgetRegistry;
        this.serverInfoPipe = serverInfoPipe;
        this.dataCollector = new VanillaDataCollector(playerTracker, serverRoot);
    }

    /**
     * Registers all widgets and starts data collection
     */
    public void register() {
        dataCollector.start(10);

        widgetRegistry.registerWidget(new Widget(
                "memory_usage",
                "overview.widgets.memory_usage",
                WidgetType.AREA_CHART,
                dataCollector::getMemoryData,
                WidgetSize.CHART,
                "hsl(var(--chart-1))",
                "MB"
        ));

        widgetRegistry.registerWidget(new Widget(
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

        widgetRegistry.registerWidget(new Widget(
                "online_players",
                "overview.widgets.online_players",
                WidgetType.STAT_CARD,
                WidgetSize.INFO,
                "hsl(var(--chart-3))",
                () -> {
                    Map<String, Object> stats = new LinkedHashMap<>();
                    stats.put("value", dataCollector.getCurrentPlayerCount());
                    stats.put("max", dataCollector.getMaxPlayers());
                    return stats;
                }
        ));

        widgetRegistry.registerWidget(new Widget(
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

        widgetRegistry.registerWidget(new Widget(
                "player_history",
                "overview.widgets.player_history",
                WidgetType.LINE_CHART,
                dataCollector::getPlayerCountData,
                WidgetSize.CHART,
                "hsl(var(--chart-1))",
                "players"
        ));

        widgetRegistry.registerWidget(new Widget(
                "cpu_usage",
                "overview.widgets.cpu_usage",
                WidgetType.LINE_CHART,
                dataCollector::getCpuData,
                WidgetSize.CHART,
                "hsl(var(--chart-2))",
                "%"
        ));

        widgetRegistry.registerWidget(new Widget(
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

        widgetRegistry.registerWidget(new Widget(
                "world_info",
                "overview.widgets.world_info",
                WidgetType.INFO_CARD,
                WidgetSize.INFO,
                "hsl(var(--chart-2))",
                () -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("count", dataCollector.getWorldCount());
                    info.put("time", dataCollector.formatWorldTime(dataCollector.getWorldTime()));
                    info.put("difficulty", dataCollector.getDifficulty());
                    info.put("hardcore", dataCollector.isHardcore());
                    return info;
                }
        ));

        widgetRegistry.registerWidget(new Widget(
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

        widgetRegistry.registerWidget(new Widget(
                "game_settings",
                "overview.widgets.game_settings",
                WidgetType.INFO_CARD,
                WidgetSize.INFO,
                "hsl(var(--chart-4))",
                () -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("pvp", "true".equals(PropertyHelper.getProperty("pvp")));
                    info.put("gamemode", PropertyHelper.getProperty("gamemode"));
                    info.put("spawnProtection", PropertyHelper.getProperty("spawn-protection"));
                    info.put("viewDistance", PropertyHelper.getProperty("view-distance"));
                    return info;
                }
        ));
    }
}
