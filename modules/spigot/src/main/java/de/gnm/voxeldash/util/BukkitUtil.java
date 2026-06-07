package de.gnm.voxeldash.util;

import de.gnm.voxeldash.VoxelDashSpigot;
import de.gnm.voxeldash.api.helper.OfflinePlayerReader;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BukkitUtil {

    private BukkitUtil() {
    }

    /**
     * Runs a task on the main server thread and waits for it to complete.
     * If already on the main thread, runs immediately.
     *
     * @param runnable the task to run
     */
    public static void runOnMainThread(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Runnable wrapped = () -> {
            try {
                runnable.run();
            } finally {
                latch.countDown();
            }
        };

        if (!SchedulerCompat.runOnGlobalRegion(VoxelDashSpigot.getInstance(), wrapped)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    wrapped.run();
                }
            }.runTask(VoxelDashSpigot.getInstance());
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    public static OfflinePlayerReader offlineReader() {
        File root = new File(System.getProperty("user.dir"));
        File world = Bukkit.getWorlds().isEmpty()
                ? new File(Bukkit.getWorldContainer(), "world")
                : Bukkit.getWorlds().get(0).getWorldFolder();
        return new OfflinePlayerReader(root).setWorldFolder(world);
    }

}
