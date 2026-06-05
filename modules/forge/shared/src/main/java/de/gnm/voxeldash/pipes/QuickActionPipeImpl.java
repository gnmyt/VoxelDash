package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.pipes.QuickActionPipe;
import de.gnm.voxeldash.util.ForgeUtil;

public class QuickActionPipeImpl implements QuickActionPipe {

    @Override
    public void reloadServer() {
        ForgeUtil.compat().reloadServer();
    }

    @Override
    public void stopServer() {
        ForgeUtil.compat().stopServer();
    }

    @Override
    public void sendCommand(String command) {
        ForgeUtil.compat().runCommand(command);
    }
}
