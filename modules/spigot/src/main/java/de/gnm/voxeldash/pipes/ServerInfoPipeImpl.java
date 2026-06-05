package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import org.bukkit.Bukkit;

public class ServerInfoPipeImpl implements ServerInfoPipe {

    @Override
    public String getServerSoftware() {
        return Bukkit.getName().toLowerCase();
    }

    @Override
    public String getServerVersion() {
        return Bukkit.getBukkitVersion().split("-")[0].replaceAll("\\.build\\..*$", "");
    }

    @Override
    public int getServerPort() {
        return Bukkit.getPort();
    }
}
