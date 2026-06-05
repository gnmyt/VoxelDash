package de.gnm.voxeldash.listener;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.event.console.ConsoleMessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.time.format.DateTimeFormatter;

public class ConsoleListenerBeta9 extends AbstractAppender {

    private static final String APPENDER_NAME = "VoxelDashForgeConsoleAppender";
    private static ConsoleListenerBeta9 instance;

    private final VoxelDashMod mod;

    private ConsoleListenerBeta9(VoxelDashMod mod) {
        super(APPENDER_NAME, null, null);
        this.mod = mod;
    }

    /**
     * Registers the console listener
     *
     * @param mod the VoxelDash mod instance
     */
    public static void register(VoxelDashMod mod) {
        if (instance != null) {
            return;
        }

        instance = new ConsoleListenerBeta9(mod);
        instance.start();

        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addAppender(instance);
    }

    /**
     * Unregisters the console listener
     */
    public static void unregister() {
        if (instance == null) {
            return;
        }

        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.removeAppender(instance);

        instance.stop();
        instance = null;
    }

    @Override
    public void append(LogEvent event) {
        if (mod.getLoader() == null) {
            return;
        }

        String message = event.getMessage().getFormattedMessage();
        if (message == null || message.isEmpty()) {
            return;
        }

        String formattedMessage = String.format("[%s] [%s/%s]: %s",
                java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                event.getLoggerName(),
                event.getLevel().name(),
                message
        );

        try {
            mod.getLoader().getEventDispatcher().dispatch(new ConsoleMessageReceivedEvent(formattedMessage));
        } catch (Exception ignored) {
        }
    }
}
