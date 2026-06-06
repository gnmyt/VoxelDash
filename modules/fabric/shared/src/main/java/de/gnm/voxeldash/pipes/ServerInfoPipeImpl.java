package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import de.gnm.voxeldash.util.FabricUtil;

public class ServerInfoPipeImpl implements ServerInfoPipe {

    @Override
    public String getServerSoftware() {
        return "fabric";
    }

    @Override
    public String getServerVersion() {
        return FabricUtil.compat().serverVersion();
    }

    @Override
    public int getServerPort() {
        return FabricUtil.compat().serverPort();
    }
}
