package de.gnm.voxeldash.api.ssh;

import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.api.event.console.ConsoleMessageReceivedEvent;
import de.gnm.voxeldash.api.pipes.QuickActionPipe;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SSHShell implements Command {

    private static final int HISTORY_LINES = 200;

    private final VoxelDashLoader loader;
    private final StringBuilder commandBuffer = new StringBuilder();
    private final Object writeLock = new Object();

    private OutputStream out;
    private InputStream in;
    private ExitCallback callback;
    private Consumer<ConsoleMessageReceivedEvent> consoleListener;
    private Thread commandExecutionThread;

    public SSHShell(VoxelDashLoader loader) {
        this.loader = loader;
    }

    @Override
    public void setExitCallback(ExitCallback exitCallback) {
        this.callback = exitCallback;
    }

    @Override
    public void setErrorStream(OutputStream outputStream) {
    }

    @Override
    public void setInputStream(InputStream inputStream) {
        this.in = inputStream;
    }

    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.out = outputStream;
    }

    /**
     * Starts a new ssh session
     * @param channelSession The current {@link ChannelSession}
     * @param environment The current {@link Environment}
     */
    @Override
    public void start(ChannelSession channelSession, Environment environment) {
        sendHistory();

        consoleListener = event -> writeLine(event.getMessage());
        loader.getEventDispatcher().registerListener(ConsoleMessageReceivedEvent.class, consoleListener);

        commandExecutionThread = new Thread(this::executeCommands);
        commandExecutionThread.start();

        channelSession.addCloseFutureListener(future -> {
            cleanup();
            callback.onExit(0);
        });
    }

    /**
     * Streams the tail of the server log so the session starts with recent context
     */
    private void sendHistory() {
        File logFile = loader.getLogFile();
        if (logFile == null || !logFile.exists()) {
            return;
        }

        try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
                .setFile(logFile)
                .setCharset(StandardCharsets.UTF_8)
                .get()) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null && lines.size() < HISTORY_LINES) {
                lines.add(0, line);
            }
            for (String historyLine : lines) {
                writeLine(historyLine);
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Reads the client input, echoes it and forwards completed commands to the server
     */
    private void executeCommands() {
        try {
            while (true) {
                while (in.available() > 0) {
                    int c = in.read();

                    if (c == '\r' || c == '\n') {
                        synchronized (writeLock) {
                            out.write('\r');
                            out.write('\n');
                            out.flush();
                        }
                        executeCommand(commandBuffer.toString());
                        commandBuffer.setLength(0);
                    } else if (c == '\b' || c == 127) {
                        handleBackspace();
                    } else if (c == 3) {
                        callback.onExit(0);
                        return;
                    } else {
                        appendCommandBuffer(c);
                    }
                }

                if (Thread.interrupted()) break;

                Thread.sleep(100);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Forwards the given command to the server through the {@link QuickActionPipe}
     * @param commandString The command that should be executed
     */
    private void executeCommand(String commandString) {
        String command = commandString.trim();
        if (command.isEmpty()) {
            return;
        }

        QuickActionPipe pipe = loader.getPipe(QuickActionPipe.class);
        if (pipe != null) {
            pipe.sendCommand(command);
        }
    }

    /**
     * Writes a single line to the client
     * @param line The line to write
     */
    private void writeLine(String line) {
        if (line == null) {
            return;
        }
        synchronized (writeLock) {
            try {
                out.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Handles the backspace
     * @throws IOException Will be thrown if the backspace could not be handled
     */
    private void handleBackspace() throws IOException {
        if (commandBuffer.length() > 0) {
            commandBuffer.setLength(commandBuffer.length() - 1);
            synchronized (writeLock) {
                out.write("\b \b".getBytes());
                out.flush();
            }
        }
    }

    /**
     * Appends the given character to the command buffer and echoes it
     * @param c The character that should be appended
     * @throws IOException Will be thrown if the character could not be echoed
     */
    private void appendCommandBuffer(int c) throws IOException {
        if (c >= 32 && c <= 126) {
            commandBuffer.append((char) c);
            synchronized (writeLock) {
                out.write(c);
                out.flush();
            }
        }
    }

    /**
     * Unregisters the console listener and stops the command thread
     */
    private void cleanup() {
        if (consoleListener != null) {
            loader.getEventDispatcher().unregisterListener(ConsoleMessageReceivedEvent.class, consoleListener);
            consoleListener = null;
        }
        if (commandExecutionThread != null) {
            commandExecutionThread.interrupt();
        }
    }

    @Override
    public void destroy(ChannelSession channelSession) {
        cleanup();
        callback.onExit(0);
    }
}
