package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import de.gnm.voxeldash.util.ForgeUtil;

public class ServerInfoPipeImpl implements ServerInfoPipe {

    @Override
    public String getServerSoftware() {
        return "forge";
    }

    @Override
    public String getServerVersion() {
        return ForgeUtil.compat().serverVersion();
    }

    @Override
    public int getServerPort() {
        return ForgeUtil.compat().serverPort();
    }
}
