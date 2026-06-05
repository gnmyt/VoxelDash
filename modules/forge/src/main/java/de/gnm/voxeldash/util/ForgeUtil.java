package de.gnm.voxeldash.util;

import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ForgeUtil {

    private static MinecraftServer server;

    private ForgeUtil() {
    }

    /**
     * Sets the server instance
     *
     * @param server the Minecraft server instance
     */
    public static void setServer(MinecraftServer server) {
        ForgeUtil.server = server;
    }

    /**
     * Gets the server instance
     *
     * @return the Minecraft server instance
     */
    public static MinecraftServer getServer() {
        return server;
    }

    /**
     * Runs a task on the main server thread and waits for it to complete.
     * If already on the main thread, runs immediately.
     *
     * @param runnable the task to run
     */
    public static void runOnMainThread(Runnable runnable) {
        if (server == null) {
            runnable.run();
            return;
        }

        if (server.isSameThread()) {
            runnable.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            server.execute(() -> {
                try {
                    runnable.run();
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Runs a task on the main server thread asynchronously (fire and forget)
     *
     * @param runnable the task to run
     */
    public static void runOnMainThreadAsync(Runnable runnable) {
        if (server == null) {
            runnable.run();
            return;
        }

        if (server.isSameThread()) {
            runnable.run();
        } else {
            server.execute(runnable);
        }
    }
}
