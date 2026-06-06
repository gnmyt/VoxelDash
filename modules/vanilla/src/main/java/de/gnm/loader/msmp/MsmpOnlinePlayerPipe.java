package de.gnm.loader.msmp;

import com.fasterxml.jackson.databind.JsonNode;
import de.gnm.loader.helper.PlayerTracker;
import de.gnm.loader.pipes.OnlinePlayerPipeImpl;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MsmpOnlinePlayerPipe implements OnlinePlayerPipe {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");

    private final MsmpClient client;
    private final OnlinePlayerPipeImpl fallback;
    private final PlayerTracker playerTracker;

    public MsmpOnlinePlayerPipe(MsmpClient client, OnlinePlayerPipeImpl fallback, PlayerTracker playerTracker) {
        this.client = client;
        this.fallback = fallback;
        this.playerTracker = playerTracker;
    }

    @Override
    public ArrayList<OnlinePlayer> getOnlinePlayers() {
        if (client.isConnected()) {
            try {
                JsonNode result = client.call(MsmpSupport.PLAYERS, null);
                fallback.ensureFreshPlayerData();
                ArrayList<OnlinePlayer> players = new ArrayList<>();
                for (JsonNode entry : MsmpSupport.elements(result, "players")) {
                    JsonNode node = MsmpSupport.playerNode(entry);
                    String name = MsmpSupport.name(node);
                    if (name == null) {
                        continue;
                    }
                    PlayerTracker.TrackedPlayer tracked = findTracked(name);
                    String ip = tracked != null ? tracked.getIpAddress() : "Unknown";
                    long joinTime = tracked != null ? tracked.getJoinTime() : System.currentTimeMillis();
                    players.add(fallback.buildOnlinePlayer(name, MsmpSupport.uuid(node, name), ip, joinTime));
                }
                return players;
            } catch (Exception e) {
                LOG.warn("MSMP getOnlinePlayers failed, falling back", e);
            }
        }
        return fallback.getOnlinePlayers();
    }

    @Override
    public void kickPlayer(String playerName, String reason) {
        if (client.isConnected()) {
            try {
                Map<String, Object> kick = new HashMap<>();
                kick.put("player", MsmpSupport.nameRef(playerName));
                if (reason != null && !reason.isEmpty()) {
                    kick.put("message", Collections.singletonMap("literal", reason));
                }
                client.call(MsmpSupport.PLAYERS_KICK, MsmpSupport.params("kick", Collections.singletonList(kick)));
                return;
            } catch (Exception e) {
                LOG.warn("MSMP kickPlayer failed, falling back", e);
            }
        }
        fallback.kickPlayer(playerName, reason);
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        fallback.setGamemode(playerName, gamemode);
    }

    @Override
    public void teleportToWorld(String playerName, String worldName) {
        fallback.teleportToWorld(playerName, worldName);
    }

    private PlayerTracker.TrackedPlayer findTracked(String name) {
        for (PlayerTracker.TrackedPlayer tracked : playerTracker.getOnlinePlayers()) {
            if (tracked.getName().equalsIgnoreCase(name)) {
                return tracked;
            }
        }
        return null;
    }
}
