package de.gnm.voxeldash.api.tunnel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.VoxelDashLoader;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MasterTunnelClient {

    private static final Logger LOG = Logger.getLogger("VoxelDash");
    private static final int BODY_CHUNK = 256 * 1024;
    private static final long HEARTBEAT_SECONDS = 20;
    private static final long MAX_BACKOFF_MS = 30_000;

    private final VoxelDashLoader loader;
    private final ConnectionConfig config;
    private final String internalToken;
    private final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build();

    private final ThreadPoolExecutor workers = (ThreadPoolExecutor) Executors.newFixedThreadPool(8, runnable -> {
        Thread thread = new Thread(runnable, "voxeldash-tunnel-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "voxeldash-tunnel-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, WebSocket> consoleStreams = new ConcurrentHashMap<>();

    private volatile WebSocket tunnel;
    private volatile boolean running = false;
    private long backoffMs = 1000;

    public MasterTunnelClient(VoxelDashLoader loader, ConnectionConfig config, String internalToken) {
        this.loader = loader;
        this.config = config;
        this.internalToken = internalToken;
    }

    /**
     * Opens the tunnel and keeps it alive (reconnecting on failure).
     */
    public void connect() {
        running = true;
        openSocket();
    }

    /**
     * Shuts the tunnel down and releases all resources.
     */
    public void shutdown() {
        running = false;
        if (tunnel != null) tunnel.close(1000, "shutdown");
        consoleStreams.values().forEach(socket -> socket.close(1000, "shutdown"));
        consoleStreams.clear();
        scheduler.shutdownNow();
        workers.shutdownNow();
    }

    private void openSocket() {
        if (!running) return;
        LOG.info("[VoxelDash One] Connecting to master at " + config.getTunnelUrl());
        Request request = new Request.Builder().url(config.getTunnelUrl()).build();
        tunnel = httpClient.newWebSocket(request, new TunnelListener());
    }

    private void scheduleReconnect() {
        if (!running) return;
        long delay = backoffMs;
        backoffMs = Math.min(MAX_BACKOFF_MS, backoffMs * 2);
        long jitter = (long) (delay * 0.2 * Math.random());
        LOG.info("[VoxelDash One] Reconnecting in " + (delay + jitter) + "ms");
        scheduler.schedule(this::openSocket, delay + jitter, TimeUnit.MILLISECONDS);
    }

    private void send(JsonNode node) {
        WebSocket socket = tunnel;
        if (socket == null) return;
        try {
            socket.send(mapper.writeValueAsString(node));
        } catch (Exception e) {
            LOG.warning("[VoxelDash One] Failed to send frame: " + e.getMessage());
        }
    }

    private ObjectNode frame(String type) {
        return mapper.createObjectNode().put("type", type);
    }

    private final class TunnelListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            backoffMs = 1000;
            LOG.info("[VoxelDash One] Tunnel established");
            ObjectNode hello = frame("hello");
            hello.put("token", config.getToken());
            send(hello);
            scheduler.scheduleAtFixedRate(() -> send(frame("ping").put("ts", System.currentTimeMillis())),
                    HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                JsonNode node = mapper.readTree(text);
                dispatch(node);
            } catch (Exception e) {
                LOG.warning("[VoxelDash One] Failed to handle frame: " + e.getMessage());
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(1000, null);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            LOG.info("[VoxelDash One] Tunnel closed: " + reason);
            scheduleReconnect();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            LOG.warning("[VoxelDash One] Tunnel failure: " + t.getMessage());
            scheduleReconnect();
        }
    }

    private void dispatch(JsonNode node) {
        String type = node.path("type").asText("");
        switch (type) {
            case "http" -> workers.submit(() -> handleHttp(node));
            case "ws-open" -> workers.submit(() -> openConsoleStream(node.path("streamId").asText()));
            case "ws-msg" -> forwardConsoleMessage(node.path("streamId").asText(), node.path("data").asText());
            case "ws-close" -> closeConsoleStream(node.path("streamId").asText());
            case "ping" -> send(frame("pong").put("ts", node.path("ts").asLong()));
            case "pong", "hello-ack" -> { /* no-op */ }
            default -> LOG.warning("[VoxelDash One] Unknown frame type: " + type);
        }
    }

    private void handleHttp(JsonNode node) {
        String id = node.path("id").asText();
        String method = node.path("method").asText("GET").toUpperCase();
        String path = node.path("path").asText("/");
        String query = node.path("query").asText("");

        try {
            String url = config.getLoopbackBase() + path + (query.isEmpty() ? "" : "?" + query);
            Request.Builder builder = new Request.Builder().url(url);

            String contentType = null;
            JsonNode headers = node.get("headers");
            if (headers != null && headers.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = headers.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String key = entry.getKey();
                    String lower = key.toLowerCase();
                    if (isHopByHop(lower) || lower.equals("authorization") || lower.equals("host")
                            || lower.equals("content-length")) {
                        continue;
                    }
                    if (lower.equals("content-type")) contentType = entry.getValue().asText();
                    builder.header(key, entry.getValue().asText());
                }
            }
            builder.header("Authorization", "Bearer " + internalToken);

            RequestBody body = null;
            if (!method.equals("GET") && !method.equals("HEAD")) {
                byte[] raw = node.hasNonNull("bodyB64")
                        ? Base64.getDecoder().decode(node.get("bodyB64").asText())
                        : new byte[0];
                body = RequestBody.create(raw, contentType != null ? MediaType.parse(contentType) : null);
            }
            builder.method(method, body);

            try (Response response = httpClient.newCall(builder.build()).execute()) {
                ObjectNode head = frame("http-res");
                head.put("id", id);
                head.put("status", response.code());
                head.set("headers", responseHeaders(response.headers()));
                ResponseBody responseBody = response.body();
                head.put("hasBody", responseBody != null);
                send(head);

                if (responseBody != null) {
                    streamBody(id, responseBody.byteStream());
                }
            }
        } catch (Exception e) {
            LOG.warning("[VoxelDash One] Loopback request failed (" + method + " " + path + "): " + e.getMessage());
            ObjectNode head = frame("http-res");
            head.put("id", id);
            head.put("status", 502);
            head.set("headers", mapper.createObjectNode());
            head.put("hasBody", false);
            send(head);
        }
    }

    private void streamBody(String id, InputStream in) throws IOException {
        byte[] current = new byte[BODY_CHUNK];
        int currentLen = fill(in, current);
        int seq = 0;

        if (currentLen <= 0) {
            sendChunk(id, seq, current, 0, true);
            return;
        }

        while (true) {
            byte[] next = new byte[BODY_CHUNK];
            int nextLen = fill(in, next);
            boolean last = nextLen <= 0;
            sendChunk(id, seq++, current, currentLen, last);
            if (last) break;
            current = next;
            currentLen = nextLen;
        }
    }

    private void sendChunk(String id, int seq, byte[] data, int len, boolean last) {
        ObjectNode chunk = frame("http-body-chunk");
        chunk.put("id", id);
        chunk.put("seq", seq);
        chunk.put("dataB64", Base64.getEncoder().encodeToString(len == data.length ? data : java.util.Arrays.copyOf(data, len)));
        chunk.put("last", last);
        send(chunk);
    }

    /**
     * Reads from the stream until the buffer is full or the stream ends.
     *
     * @return the number of bytes read (0 means immediate EOF)
     */
    private int fill(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = in.read(buffer, offset, buffer.length - offset);
            if (read == -1) break;
            offset += read;
        }
        return offset;
    }

    private ObjectNode responseHeaders(Headers headers) {
        ObjectNode result = mapper.createObjectNode();
        Set<String> names = headers.names();
        for (String name : names) {
            if (isHopByHop(name.toLowerCase())) continue;
            if (name.equalsIgnoreCase("content-type")) {
                String preferred = null;
                for (String value : headers.values(name)) {
                    if (preferred == null || preferred.toLowerCase().startsWith("text/html")) {
                        preferred = value;
                    }
                }
                result.put("Content-Type", preferred);
            } else {
                result.put(name, headers.get(name));
            }
        }
        return result;
    }

    private boolean isHopByHop(String header) {
        switch (header) {
            case "connection":
            case "keep-alive":
            case "transfer-encoding":
            case "upgrade":
            case "proxy-connection":
                return true;
            default:
                return false;
        }
    }

    private void openConsoleStream(String streamId) {
        String url = "ws://127.0.0.1:" + config.getApiPort() + "/api/ws?sessionToken=" + internalToken;
        Request request = new Request.Builder().url(url).build();
        WebSocket socket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                send(frame("ws-open-ack").put("streamId", streamId));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                send(frame("ws-msg").put("streamId", streamId).put("data", text));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                consoleStreams.remove(streamId);
                send(frame("ws-close").put("streamId", streamId));
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                consoleStreams.remove(streamId);
                send(frame("ws-open-err").put("streamId", streamId).put("reason", t.getMessage()));
            }
        });
        consoleStreams.put(streamId, socket);
    }

    private void forwardConsoleMessage(String streamId, String data) {
        WebSocket socket = consoleStreams.get(streamId);
        if (socket != null) socket.send(data);
    }

    private void closeConsoleStream(String streamId) {
        WebSocket socket = consoleStreams.remove(streamId);
        if (socket != null) socket.close(1000, "closed by master");
    }
}
