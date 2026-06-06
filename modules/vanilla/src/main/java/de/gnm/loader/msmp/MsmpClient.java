package de.gnm.loader.msmp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MsmpClient {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");

    private static final int CONNECT_ATTEMPTS = 6;
    private static final long CONNECT_RETRY_DELAY_MS = 2000;
    private static final long CONNECT_TIMEOUT_MS = 3000;
    private static final long CALL_TIMEOUT_MS = 5000;

    private final int port;
    private final String secret;
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient httpClient;

    private final AtomicInteger idCounter = new AtomicInteger();
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    private volatile WebSocket webSocket;
    private volatile boolean connected = false;
    private volatile boolean enabled;

    public MsmpClient(int port, String secret) {
        this.port = port;
        this.secret = secret;
        this.enabled = port > 0 && secret != null && !secret.isEmpty();
        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .build();
    }

    public void start() {
        if (!enabled) {
            LOG.info("Management server not configured, using console/file access");
            return;
        }
        scheduleConnect();
    }

    public void notifyServerVersion(String version) {
        if (enabled && !supportsManagementProtocol(version)) {
            LOG.info("Server version " + version + " has no management protocol, using console/file access");
            enabled = false;
            disconnect();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public JsonNode call(String method, Object params) throws Exception {
        WebSocket socket = this.webSocket;
        if (!connected || socket == null) {
            throw new IOException("MSMP not connected");
        }

        int id = idCounter.incrementAndGet();
        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        if (params != null) {
            request.set("params", mapper.valueToTree(params));
        }

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);

        if (!socket.send(mapper.writeValueAsString(request))) {
            pending.remove(id);
            throw new IOException("Failed to send MSMP request for " + method);
        }

        try {
            JsonNode response = future.get(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            JsonNode error = response.get("error");
            if (error != null && !error.isNull()) {
                throw new IOException("MSMP error for " + method + ": " + error);
            }
            return response.get("result");
        } finally {
            pending.remove(id);
        }
    }

    public void disconnect() {
        connected = false;
        WebSocket socket = this.webSocket;
        if (socket != null) {
            socket.cancel();
            this.webSocket = null;
        }
        failPending();
    }

    private void scheduleConnect() {
        if (!enabled || connected || !connecting.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(this::connectLoop, "VoxelDash-MSMP");
        thread.setDaemon(true);
        thread.start();
    }

    private void connectLoop() {
        try {
            for (int attempt = 1; attempt <= CONNECT_ATTEMPTS && enabled && !connected; attempt++) {
                if (tryConnect()) {
                    LOG.info("Connected to management server on localhost:" + port);
                    return;
                }
                Thread.sleep(CONNECT_RETRY_DELAY_MS);
            }
            if (enabled && !connected) {
                LOG.info("Management server unreachable on localhost:" + port + ", using console/file access");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            connecting.set(false);
        }
    }

    private boolean tryConnect() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean opened = new AtomicBoolean(false);

        Request request = new Request.Builder()
                .url("ws://localhost:" + port)
                .addHeader("Authorization", "Bearer " + secret)
                .build();

        WebSocket socket = httpClient.newWebSocket(request, new Listener(latch, opened));

        try {
            latch.await(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (opened.get()) {
            return true;
        }
        socket.cancel();
        return false;
    }

    private void onDisconnected() {
        boolean wasConnected = connected;
        connected = false;
        this.webSocket = null;
        failPending();
        if (wasConnected && enabled) {
            scheduleConnect();
        }
    }

    private void failPending() {
        for (Integer id : pending.keySet()) {
            CompletableFuture<JsonNode> future = pending.remove(id);
            if (future != null) {
                future.completeExceptionally(new IOException("MSMP disconnected"));
            }
        }
    }

    private static boolean supportsManagementProtocol(String version) {
        if (version == null) {
            return true;
        }
        String[] parts = version.split("\\.");
        try {
            int major = parts.length > 0 ? Integer.parseInt(parts[0].replaceAll("\\D", "")) : 0;
            int minor = parts.length > 1 ? Integer.parseInt(parts[1].replaceAll("\\D", "")) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2].replaceAll("\\D", "")) : 0;
            if (major != 1) {
                return major > 1;
            }
            if (minor != 21) {
                return minor > 21;
            }
            return patch >= 9;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private final class Listener extends WebSocketListener {

        private final CountDownLatch latch;
        private final AtomicBoolean opened;

        private Listener(CountDownLatch latch, AtomicBoolean opened) {
            this.latch = latch;
            this.opened = opened;
        }

        @Override
        public void onOpen(WebSocket socket, Response response) {
            webSocket = socket;
            connected = true;
            opened.set(true);
            latch.countDown();
        }

        @Override
        public void onMessage(WebSocket socket, String text) {
            try {
                JsonNode node = mapper.readTree(text);
                JsonNode idNode = node.get("id");
                if (idNode != null && !idNode.isNull()) {
                    CompletableFuture<JsonNode> future = pending.remove(idNode.asInt());
                    if (future != null) {
                        future.complete(node);
                    }
                }
            } catch (Exception e) {
                LOG.debug("Failed to parse MSMP message: " + text, e);
            }
        }

        @Override
        public void onFailure(WebSocket socket, Throwable t, Response response) {
            latch.countDown();
            onDisconnected();
        }

        @Override
        public void onClosed(WebSocket socket, int code, String reason) {
            latch.countDown();
            onDisconnected();
        }
    }
}
