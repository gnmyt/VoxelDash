package de.gnm.voxeldash.api.ssh;

import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.SSHController;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

public class SSHManager {

    private final VoxelDashLoader loader;
    private final SSHController sshController;
    private final AccountController accountManager;
    private final File serverRoot;
    private SshServer sshServer;

    public SSHManager(VoxelDashLoader loader, SSHController sshController, AccountController accountManager, File serverRoot) {
        this.loader = loader;
        this.sshController = sshController;
        this.accountManager = accountManager;
        this.serverRoot = serverRoot;
    }

    /**
     * Starts the SSH server
     * @param port The port of the SSH server
     * @throws IOException Will be thrown if the SSH server could not be started
     */
    public void start(int port) throws IOException {
        if (sshServer != null) sshServer.stop();

        sshServer = SshServer.setUpDefaultServer();
        sshServer.setShellFactory(new SSHShellFactory(loader, sshController));
        sshServer.setKeyPairProvider(new SSHKeyProvider(sshController.getPublicKey(), sshController.getPrivateKey()));

        SftpSubsystemFactory.Builder builder = new SftpSubsystemFactory.Builder();
        builder.addSftpEventListener(new SFTPListener(sshController));

        sshServer.setSubsystemFactories(Collections.singletonList(builder.build()));
        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(serverRoot.getAbsolutePath()).toAbsolutePath().normalize()));

        sshServer.setPasswordAuthenticator(new SSHAuthenticator(accountManager));

        sshServer.setPort(port);
        sshServer.start();
    }

    /**
     * Stops the SSH server
     * @throws IOException Will be thrown if the SSH server could not be stopped
     */
    public void stop() throws IOException {
        if (sshServer != null) sshServer.stop();
        sshServer = null;
    }

    /**
     * Gets the current SSH server
     * @return the current {@link SshServer}
     */
    public SshServer getSshServer() {
        return sshServer;
    }
}
