package de.gnm.loader.msmp;

import com.fasterxml.jackson.databind.JsonNode;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.pipes.players.OperatorPipe;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MsmpOperatorPipe implements OperatorPipe {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");
    private static final int FULL_OPERATOR_LEVEL = 4;

    private final MsmpClient client;
    private final OperatorPipe fallback;

    public MsmpOperatorPipe(MsmpClient client, OperatorPipe fallback) {
        this.client = client;
        this.fallback = fallback;
    }

    @Override
    public ArrayList<OfflinePlayer> getOperators() {
        if (client.isConnected()) {
            try {
                JsonNode result = client.call(MsmpSupport.OPERATORS, null);
                ArrayList<OfflinePlayer> operators = new ArrayList<>();
                for (JsonNode entry : MsmpSupport.elements(result, "operators")) {
                    JsonNode node = MsmpSupport.playerNode(entry);
                    String name = MsmpSupport.name(node);
                    if (name != null) {
                        operators.add(new OfflinePlayer(name, MsmpSupport.uuid(node, name)));
                    }
                }
                return operators;
            } catch (Exception e) {
                LOG.warn("MSMP getOperators failed, falling back", e);
            }
        }
        return fallback.getOperators();
    }

    @Override
    public void setOp(String playerName) {
        if (client.isConnected()) {
            try {
                Map<String, Object> operator = new HashMap<>();
                operator.put("player", MsmpSupport.nameRef(playerName));
                operator.put("permissionLevel", FULL_OPERATOR_LEVEL);
                operator.put("bypassesPlayerLimit", false);
                client.call(MsmpSupport.OPERATORS_ADD, MsmpSupport.params("add", Collections.singletonList(operator)));
                return;
            } catch (Exception e) {
                LOG.warn("MSMP setOp failed, falling back", e);
            }
        }
        fallback.setOp(playerName);
    }

    @Override
    public void deOp(String playerName) {
        if (client.isConnected()) {
            try {
                client.call(MsmpSupport.OPERATORS_REMOVE,
                        MsmpSupport.params("remove", Collections.singletonList(MsmpSupport.nameRef(playerName))));
                return;
            } catch (Exception e) {
                LOG.warn("MSMP deOp failed, falling back", e);
            }
        }
        fallback.deOp(playerName);
    }
}
