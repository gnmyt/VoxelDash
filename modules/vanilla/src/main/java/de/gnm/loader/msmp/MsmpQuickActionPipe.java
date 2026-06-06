package de.gnm.loader.msmp;

import de.gnm.voxeldash.api.pipes.QuickActionPipe;
import org.apache.log4j.Logger;

public class MsmpQuickActionPipe implements QuickActionPipe {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");

    private final MsmpClient client;
    private final QuickActionPipe fallback;

    public MsmpQuickActionPipe(MsmpClient client, QuickActionPipe fallback) {
        this.client = client;
        this.fallback = fallback;
    }

    @Override
    public void reloadServer() {
        fallback.reloadServer();
    }

    @Override
    public void stopServer() {
        if (client.isConnected()) {
            try {
                client.call(MsmpSupport.SERVER_STOP, null);
                return;
            } catch (Exception e) {
                LOG.warn("MSMP stopServer failed, falling back", e);
            }
        }
        fallback.stopServer();
    }

    @Override
    public void sendCommand(String command) {
        fallback.sendCommand(command);
    }
}
