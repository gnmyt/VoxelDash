package de.gnm.voxeldash.api.tunnel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.logging.Logger;

public class ConnectionConfig {

    private static final Logger LOG = Logger.getLogger("VoxelDash");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String masterHost;
    private final int masterPort;
    private final String token;
    private final int apiPort;

    private ConnectionConfig(String masterHost, int masterPort, String token, int apiPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.token = token;
        this.apiPort = apiPort;
    }

    /**
     * Attempts to detect a connected-mode configuration from the {@code VOXELDASH_MASTER}
     * environment variable.
     *
     * @return the parsed configuration, or {@code null} if the server runs standalone
     */
    public static ConnectionConfig detect() {
        String env = System.getenv("VOXELDASH_MASTER");
        if (env == null || env.trim().isEmpty()) return null;

        try {
            return parse(MAPPER.readTree(env));
        } catch (Exception e) {
            LOG.warning("Failed to parse VOXELDASH_MASTER environment variable: " + e.getMessage());
            return null;
        }
    }

    private static ConnectionConfig parse(JsonNode node) {
        if (node == null || !node.hasNonNull("token")) return null;

        String host = node.has("masterHost") ? node.get("masterHost").asText("127.0.0.1") : "127.0.0.1";
        int masterPort = node.has("masterPort") ? node.get("masterPort").asInt() : 0;
        int apiPort = node.has("apiPort") ? node.get("apiPort").asInt() : 0;
        String token = node.get("token").asText();

        if (masterPort <= 0 || apiPort <= 0 || token.trim().isEmpty()) return null;

        return new ConnectionConfig(host, masterPort, token, apiPort);
    }

    public String getMasterHost() {
        return masterHost;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public String getToken() {
        return token;
    }

    public int getApiPort() {
        return apiPort;
    }

    /**
     * @return the WebSocket URL the server uses to reverse-connect to the master
     */
    public String getTunnelUrl() {
        return "ws://" + masterHost + ":" + masterPort + "/tunnel";
    }

    /**
     * @return the loopback base URL the tunnel uses to reach this server's own api
     */
    public String getLoopbackBase() {
        return "http://127.0.0.1:" + apiPort;
    }
}
