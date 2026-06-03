package de.gnm.voxeldash.api.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.api.event.console.ConsoleMessageReceivedEvent;
import io.undertow.websockets.core.*;
import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WebSocketReceiver extends AbstractReceiveListener {
    private static final String EVENT_TYPE_ATTACH = "ATTACH";
    private static final String EVENT_TYPE_DETACH = "DETACH";
    private static final int MAX_LOG_LINES = 500;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, WebSocketEventHandler<?>> eventHandlers = new HashMap<>();
    private final VoxelDashLoader loader;

    /**
     * Basic constructor of the {@link WebSocketReceiver}
     *
     * @param loader The loader
     */
    public WebSocketReceiver(VoxelDashLoader loader) {
        this.loader = loader;
        initializeEventHandlers();
    }

    /**
     * Initializes the event handlers
     */
    private void initializeEventHandlers() {
        registerEventHandler("CONSOLE", new WebSocketEventHandler<>(
                ConsoleMessageReceivedEvent.class,
                event -> createEventMessage("CONSOLE", event.getMessage())
        ));
    }

    /**
     * Creates a JSON message for the given event type and message
     *
     * @param eventType The event type
     * @param message   The message
     * @return The JSON message
     */
    private String createEventMessage(String eventType, String message) {
        try {
            ObjectNode node = mapper.createObjectNode()
                    .put("event", eventType)
                    .put("message", message);
            return mapper.writeValueAsString(node);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Initializes the console history for the given channel
     *
     * @param channel The channel
     * @throws IOException If an error occurs while reading the log file
     */
    private void initializeConsoleHistory(WebSocketChannel channel) throws IOException {
        File logFile = loader.getLogFile();
        if (!logFile.exists()) {
            return;
        }

        try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
                .setFile(logFile)
                .setCharset(StandardCharsets.UTF_8)
                .get()) {
            List<String> lastLines = new ArrayList<>();
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null && count < MAX_LOG_LINES) {
                lastLines.add(0, line);
                count++;
            }

            for (String logLine : lastLines) {
                String message = createEventMessage("CONSOLE", logLine);
                if (message != null) {
                    WebSockets.sendText(message, channel, null);
                }
            }
        }
    }

    /**
     * Registers an event handler with the given name
     *
     * @param name    The name
     * @param handler The handler
     */
    private void registerEventHandler(String name, WebSocketEventHandler<?> handler) {
        eventHandlers.put(name, handler);
    }

    /**
     * Called when a new WebSocket connection is established.
     *
     * @param channel The channel
     * @param message The message
     * @throws IOException If an error occurs while handling the message
     */
    @Override
    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
        try {
            JsonNode jsonNode = mapper.readTree(message.getData());
            handleWebSocketMessage(channel, jsonNode);
        } catch (IOException e) {
            sendErrorMessage(channel, "Invalid JSON format");
        }
    }

    /**
     * Handles the given WebSocket message
     *
     * @param channel  The channel
     * @param jsonNode The JSON node
     * @throws IOException If an error occurs while handling the message
     */
    private void handleWebSocketMessage(WebSocketChannel channel, JsonNode jsonNode) throws IOException {
        String eventType = Optional.ofNullable(jsonNode.get("event"))
                .map(JsonNode::asText)
                .orElseThrow(() -> new IOException("Missing event type"));

        String eventName = Optional.ofNullable(jsonNode.get("name"))
                .map(JsonNode::asText)
                .orElseThrow(() -> new IOException("Missing event name"));

        switch (eventType) {
            case EVENT_TYPE_ATTACH:
                handleAttach(channel, eventName);
                break;
            case EVENT_TYPE_DETACH:
                handleDetach(eventName);
                break;
            default:
                sendErrorMessage(channel, "Invalid event type");
                break;
        }
    }

    /**
     * Handles the 'attach' event
     *
     * @param channel The channel
     * @param name    The name
     * @throws IOException If an error occurs while handling the event
     */
    private void handleAttach(WebSocketChannel channel, String name) throws IOException {
        WebSocketEventHandler<?> handler = getEventHandler(name);
        handler.attach(channel, loader);

        if ("CONSOLE".equals(name)) {
            initializeConsoleHistory(channel);
        }
    }

    /**
     * Handles the 'detach' event
     *
     * @param name The name
     * @throws IOException If an error occurs while handling the event
     */
    private void handleDetach(String name) throws IOException {
        WebSocketEventHandler<?> handler = getEventHandler(name);
        handler.detach(loader);
    }

    /**
     * Gets the event handler with the given name
     *
     * @param name The name
     * @return The event handler
     * @throws IOException If the event handler is unknown
     */
    private WebSocketEventHandler<?> getEventHandler(String name) throws IOException {
        return Optional.ofNullable(eventHandlers.get(name))
                .orElseThrow(() -> new IOException("Unknown event handler: " + name));
    }

    /**
     * Sends an error message to the given channel
     *
     * @param channel The channel
     * @param message The message
     * @throws IOException If an error occurs while sending the message
     */
    private void sendErrorMessage(WebSocketChannel channel, String message) throws IOException {
        String jsonMessage = createEventMessage("ERROR", message);
        if (jsonMessage != null) {
            WebSockets.sendText(jsonMessage, channel, null);
        }
    }

    /**
     * Called when a new WebSocket connection is established.
     *
     * @param webSocketChannel The channel
     * @param channel          The channel
     */
    @Override
    protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) {
        eventHandlers.values().forEach(handler -> handler.detach(loader));
    }
}