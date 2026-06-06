package de.gnm.voxeldash.api.ssh;

import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.api.controller.SSHController;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;

import java.io.IOException;

public class SSHShellFactory implements ShellFactory {

    private final VoxelDashLoader loader;
    private final SSHController sshController;

    public SSHShellFactory(VoxelDashLoader loader, SSHController sshController) {
        this.loader = loader;
        this.sshController = sshController;
    }

    @Override
    public Command createShell(ChannelSession channelSession) throws IOException {
        return sshController.isConsoleEnabled() ? new SSHShell(loader) : new SSHShellDisabled();
    }
}
