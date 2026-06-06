package de.gnm.voxeldash.api.controller;

import com.fasterxml.jackson.databind.node.BinaryNode;
import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.api.ssh.SSHManager;
import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.common.session.helpers.AbstractSession;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.sql.Connection;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class SSHController extends BaseController {

    AttributeRepository.AttributeKey<Boolean> isSFTP = new AttributeRepository.AttributeKey<>();
    private SSHManager sshManager;

    public SSHController(Connection connection) {
        super(connection);

        createTable();
    }

    /**
     * Helper method to create the sshConfig table
     */
    private void createTable() {
        executeUpdate("CREATE TABLE IF NOT EXISTS sshConfig (configKey TEXT PRIMARY KEY, configValue TEXT)");
    }

    /**
     * Check if the SSH server is enabled
     * @return True if the SSH server is enabled
     */
    public boolean isEnabled() {
        HashMap<String, Object> singleResult = getSingleResult("SELECT configValue FROM sshConfig WHERE configKey = 'ssh_enabled'");
        return singleResult != null && singleResult.get("configValue").equals("true");
    }

    /**
     * Set the SSH server enabled
     * @param enabled True if the SSH server should be enabled
     */
    public void setEnabled(boolean enabled) {
        executeUpdate("INSERT OR REPLACE INTO sshConfig (configKey, configValue) VALUES ('ssh_enabled', ?)", enabled ? "true" : "false");

        if (enabled) {
            try {
                sshManager.start(getPort());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                sshManager.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create the default configuration for the SSH server
     */
    public void createDefaultConfig() {
        if (getSingleResult("SELECT * FROM sshConfig WHERE configKey = 'ssh_enabled'") == null) {
            executeUpdate("INSERT INTO sshConfig (configKey, configValue) VALUES ('ssh_enabled', 'false')");
        }

        if (getSingleResult("SELECT * FROM sshConfig WHERE configKey = 'ssh_port'") == null) {
            executeUpdate("INSERT INTO sshConfig (configKey, configValue) VALUES ('ssh_port', '22')");
        }

        if (getSingleResult("SELECT * FROM sshConfig WHERE configKey = 'ssh_console'") == null) {
            executeUpdate("INSERT INTO sshConfig (configKey, configValue) VALUES ('ssh_console', 'true')");
        }

        if (getSingleResult("SELECT * FROM sshConfig WHERE configKey = 'ssh_sftp'") == null) {
            executeUpdate("INSERT INTO sshConfig (configKey, configValue) VALUES ('ssh_sftp', 'true')");
        }
    }

    /**
     * Get the port of the SSH server
     * @return The port of the SSH server
     */
    public int getPort() {
        HashMap<String, Object> singleResult = getSingleResult("SELECT configValue FROM sshConfig WHERE configKey = 'ssh_port'");
        return singleResult != null ? Integer.parseInt((String) singleResult.get("configValue")) : 0;
    }

    /**
     * Set the port of the SSH server
     * @param port The new port of the SSH server
     */
    public void setPort(int port) {
        executeUpdate("INSERT OR REPLACE INTO sshConfig (configKey, configValue) VALUES ('ssh_port', ?)", String.valueOf(port));

        if (isEnabled()) {
            try {
                sshManager.stop();
                sshManager.start(port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check if the console is enabled
     * @return True if the console is enabled
     */
    public boolean isConsoleEnabled() {
        HashMap<String, Object> singleResult = getSingleResult("SELECT configValue FROM sshConfig WHERE configKey = 'ssh_console'");
        return singleResult != null && singleResult.get("configValue").equals("true");
    }

    /**
     * Set the console enabled
     * @param enabled True if the console should be enabled
     */
    public void setConsoleEnabled(boolean enabled) {
        executeUpdate("INSERT OR REPLACE INTO sshConfig (configKey, configValue) VALUES ('ssh_console', ?)", enabled ? "true" : "false");
    }

    /**
     * Check if the SFTP is enabled
     * @return True if the SFTP is enabled
     */
    public boolean isSFTPEnabled() {
        HashMap<String, Object> singleResult = getSingleResult("SELECT configValue FROM sshConfig WHERE configKey = 'ssh_sftp'");
        return singleResult != null && singleResult.get("configValue").equals("true");
    }

    /**
     * Set the SFTP enabled
     * @param enabled True if the SFTP should be enabled
     */
    public void setSFTPEnabled(boolean enabled) {
        executeUpdate("INSERT OR REPLACE INTO sshConfig (configKey, configValue) VALUES ('ssh_sftp', ?)", enabled ? "true" : "false");
    }

    /**
     * Get the public key of the SSH server
     * @return The public key of the SSH server
     */
    public String getPublicKey() {
        HashMap<String, Object> singleResult = getSingleResult("SELECT configValue FROM sshConfig WHERE configKey = 'ssh_public'");
        return singleResult != null ? (String) singleResult.get("configValue") : null;
    }

    /**
     * Set the public key of the SSH server
     * @param publicKey The new public key of the SSH server
     */
    public void setPublicKey(String publicKey) {
        executeUpdate("INSERT OR REPLACE INTO sshConfig (configKey, configValue) VALUES ('ssh_public', ?)", publicKey);
    }

    /**
     * Get the private key of the SSH server
     * @return The private key of the SSH server
     */
    public String getPrivateKey() {
        HashMap<String, Object> singleResult = getSingleResult("SELECT configValue FROM sshConfig WHERE configKey = 'ssh_private'");
        return singleResult != null ? (String) singleResult.get("configValue") : null;
    }

    /**
     * Set the private key of the SSH server
     * @param privateKey The new private key of the SSH server
     */
    public void setPrivateKey(String privateKey) {
        executeUpdate("INSERT OR REPLACE INTO sshConfig (configKey, configValue) VALUES ('ssh_private', ?)", privateKey);
    }

    /**
     * Generates the public and private keys for the SSH server
     */
    private void generateKeys() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            setPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the SSH controller
     * @param loader The loader
     * @param accountController The account controller
     * @param serverRoot The server root
     */
    public void initialize(VoxelDashLoader loader, AccountController accountController, File serverRoot) {
        createDefaultConfig();
        if (sshManager == null) {
            if (getPublicKey() == null || getPrivateKey() == null) {
                generateKeys();
            }

            sshManager = new SSHManager(loader, this, accountController, serverRoot);
        }

        if (isEnabled()) {
            try {
                sshManager.start(getPort());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets all active sessions of the SSH server
     * @return All active sessions of the SSH server
     */
    public List<AbstractSession> getActiveSessions() {
        return sshManager.getSshServer().getActiveSessions();
    }

    /**
     * Gets a session by its id
     * @param id The id of the session
     * @return The session with the given id
     */
    public AbstractSession getSessionById(String id) {
        return sshManager.getSshServer().getActiveSessions().stream().filter(session ->
                BinaryNode.valueOf(session.getSessionId()).asText().equals(id)).findFirst().orElse(null);
    }

    /**
     * Gets the isSFTP attribute key
     * @return The isSFTP attribute key
     */
    public AttributeRepository.AttributeKey<Boolean> getIsSFTP() {
        return isSFTP;
    }
}
