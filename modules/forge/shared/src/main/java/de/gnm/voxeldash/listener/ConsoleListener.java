package de.gnm.voxeldash.listener;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.event.console.ConsoleMessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.time.format.DateTimeFormatter;

public class ConsoleListener extends AbstractAppender {

    private static final String APPENDER_NAME = "VoxelDashForgeConsoleAppender";
    private static ConsoleListener instance;

    private final VoxelDashMod mod;

    private ConsoleListener(VoxelDashMod mod) {
        super(APPENDER_NAME, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
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

        instance = new ConsoleListener(mod);
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
