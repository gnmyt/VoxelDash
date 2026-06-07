package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.pipes.QuickActionPipe;
import de.gnm.voxeldash.util.BukkitUtil;
import org.bukkit.Bukkit;

public class QuickActionPipeImpl implements QuickActionPipe {

    @Override
    public void reloadServer() {
        BukkitUtil.runOnMainThread(Bukkit::reloadData);
    }

    @Override
    public void stopServer() {
        BukkitUtil.runOnMainThread(Bukkit::shutdown);
    }

    @Override
    public void sendCommand(String command) {
        String cleanCommand = command.startsWith("/") ? command.substring(1) : command;
        BukkitUtil.runOnMainThread(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cleanCommand));
    }
}
