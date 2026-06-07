package de.gnm.voxeldash.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class SchedulerCompat {

    private static final boolean FOLIA = detectFolia();

    private static Object globalScheduler;
    private static Method executeMethod;
    private static Method runAtFixedRateMethod;

    private SchedulerCompat() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object globalScheduler() throws Exception {
        if (globalScheduler == null) {
            globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
        }
        return globalScheduler;
    }

    /**
     * Runs a task once on Folia's global region thread. Returns {@code false} on non-Folia servers so
     * the caller can fall back to the BukkitScheduler.
     */
    public static boolean runOnGlobalRegion(Plugin plugin, Runnable task) {
        if (!FOLIA) return false;
        try {
            Object scheduler = globalScheduler();
            if (executeMethod == null) {
                executeMethod = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
            }
            executeMethod.invoke(scheduler, plugin, task);
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to schedule task on Folia global region", e);
        }
    }

    /**
     * Schedules a repeating task. On Folia it runs on the global region scheduler, otherwise on the
     * BukkitScheduler. Returns an opaque handle understood by {@link #cancel(Object)}.
     */
    public static Object runTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
        try {
            Object scheduler = globalScheduler();
            if (runAtFixedRateMethod == null) {
                runAtFixedRateMethod = scheduler.getClass()
                        .getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            }
            Consumer<Object> wrapped = ignored -> task.run();
            return runAtFixedRateMethod.invoke(scheduler, plugin, wrapped, Math.max(1L, delayTicks), periodTicks);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to schedule repeating task on Folia", e);
        }
    }

    /**
     * Cancels a handle returned by {@link #runTimer}. Both {@code BukkitTask} and Folia's
     * {@code ScheduledTask} expose a no-arg {@code cancel()} method.
     */
    public static void cancel(Object handle) {
        if (handle == null) return;
        try {
            handle.getClass().getMethod("cancel").invoke(handle);
        } catch (Exception ignored) {
        }
    }
}
