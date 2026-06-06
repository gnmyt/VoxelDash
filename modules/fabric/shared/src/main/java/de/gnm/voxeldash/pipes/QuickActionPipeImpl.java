package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.pipes.QuickActionPipe;
import de.gnm.voxeldash.util.FabricUtil;

public class QuickActionPipeImpl implements QuickActionPipe {

    @Override
    public void reloadServer() {
        FabricUtil.compat().reloadServer();
    }

    @Override
    public void stopServer() {
        FabricUtil.compat().stopServer();
    }

    @Override
    public void sendCommand(String command) {
        FabricUtil.compat().runCommand(command);
    }
}
