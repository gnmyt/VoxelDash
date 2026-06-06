package de.gnm.loader.msmp;

import com.fasterxml.jackson.databind.JsonNode;
import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.pipes.players.BanPipe;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MsmpBanPipe implements BanPipe {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");
    private static final String BAN_SOURCE = "VoxelDash";

    private final MsmpClient client;
    private final BanPipe fallback;

    public MsmpBanPipe(MsmpClient client, BanPipe fallback) {
        this.client = client;
        this.fallback = fallback;
    }

    @Override
    public ArrayList<BannedPlayer> getBannedPlayers() {
        if (client.isConnected()) {
            try {
                JsonNode result = client.call(MsmpSupport.BANS, null);
                ArrayList<BannedPlayer> players = new ArrayList<>();
                for (JsonNode entry : MsmpSupport.elements(result, "banlist")) {
                    JsonNode node = MsmpSupport.playerNode(entry);
                    String name = MsmpSupport.name(node);
                    if (name == null) {
                        continue;
                    }
                    UUID uuid = MsmpSupport.uuid(node, name);
                    String reason = entry.hasNonNull("reason") ? entry.get("reason").asText() : null;
                    String source = entry.hasNonNull("source") ? entry.get("source").asText() : "Unknown";
                    Date banDate = MsmpSupport.instant(entry.get("created"));
                    Date expiry = MsmpSupport.instant(entry.get("expires"));
                    players.add(new BannedPlayer(name, uuid, reason, banDate, expiry, source));
                }
                return players;
            } catch (Exception e) {
                LOG.warn("MSMP getBannedPlayers failed, falling back", e);
            }
        }
        return fallback.getBannedPlayers();
    }

    @Override
    public void banPlayer(String playerName, String reason) {
        if (client.isConnected()) {
            try {
                Map<String, Object> ban = new HashMap<>();
                ban.put("player", MsmpSupport.nameRef(playerName));
                if (reason != null && !reason.isEmpty()) {
                    ban.put("reason", reason);
                }
                ban.put("source", BAN_SOURCE);
                client.call(MsmpSupport.BANS_ADD, MsmpSupport.params("add", Collections.singletonList(ban)));
                return;
            } catch (Exception e) {
                LOG.warn("MSMP banPlayer failed, falling back", e);
            }
        }
        fallback.banPlayer(playerName, reason);
    }

    @Override
    public void unbanPlayer(String playerName) {
        if (client.isConnected()) {
            try {
                client.call(MsmpSupport.BANS_REMOVE,
                        MsmpSupport.params("remove", Collections.singletonList(MsmpSupport.nameRef(playerName))));
                return;
            } catch (Exception e) {
                LOG.warn("MSMP unbanPlayer failed, falling back", e);
            }
        }
        fallback.unbanPlayer(playerName);
    }
}
