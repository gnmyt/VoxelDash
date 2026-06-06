package de.gnm.loader.msmp;

import com.fasterxml.jackson.databind.JsonNode;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.pipes.players.WhitelistPipe;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;

public class MsmpWhitelistPipe implements WhitelistPipe {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");

    private final MsmpClient client;
    private final WhitelistPipe fallback;

    public MsmpWhitelistPipe(MsmpClient client, WhitelistPipe fallback) {
        this.client = client;
        this.fallback = fallback;
    }

    @Override
    public void setStatus(boolean status) {
        if (client.isConnected()) {
            try {
                client.call(MsmpSupport.USE_ALLOWLIST_SET, MsmpSupport.params("use", status));
                return;
            } catch (Exception e) {
                LOG.warn("MSMP setStatus failed, falling back", e);
            }
        }
        fallback.setStatus(status);
    }

    @Override
    public boolean getStatus() {
        if (client.isConnected()) {
            try {
                JsonNode result = client.call(MsmpSupport.USE_ALLOWLIST, null);
                if (result != null && result.hasNonNull("used")) {
                    return result.get("used").asBoolean();
                }
            } catch (Exception e) {
                LOG.warn("MSMP getStatus failed, falling back", e);
            }
        }
        return fallback.getStatus();
    }

    @Override
    public ArrayList<OfflinePlayer> getWhitelistedPlayers() {
        if (client.isConnected()) {
            try {
                JsonNode result = client.call(MsmpSupport.ALLOWLIST, null);
                ArrayList<OfflinePlayer> whitelist = new ArrayList<>();
                for (JsonNode entry : MsmpSupport.elements(result, "allowlist")) {
                    JsonNode node = MsmpSupport.playerNode(entry);
                    String name = MsmpSupport.name(node);
                    if (name != null) {
                        whitelist.add(new OfflinePlayer(name, MsmpSupport.uuid(node, name)));
                    }
                }
                return whitelist;
            } catch (Exception e) {
                LOG.warn("MSMP getWhitelistedPlayers failed, falling back", e);
            }
        }
        return fallback.getWhitelistedPlayers();
    }

    @Override
    public void addPlayer(String playerName) {
        if (client.isConnected()) {
            try {
                client.call(MsmpSupport.ALLOWLIST_ADD,
                        MsmpSupport.params("add", Collections.singletonList(MsmpSupport.nameRef(playerName))));
                return;
            } catch (Exception e) {
                LOG.warn("MSMP whitelist addPlayer failed, falling back", e);
            }
        }
        fallback.addPlayer(playerName);
    }

    @Override
    public void removePlayer(String playerName) {
        if (client.isConnected()) {
            try {
                client.call(MsmpSupport.ALLOWLIST_REMOVE,
                        MsmpSupport.params("remove", Collections.singletonList(MsmpSupport.nameRef(playerName))));
                return;
            } catch (Exception e) {
                LOG.warn("MSMP whitelist removePlayer failed, falling back", e);
            }
        }
        fallback.removePlayer(playerName);
    }
}
