package de.gnm.loader.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.PermissionController;
import de.gnm.voxeldash.api.event.EventDispatcher;
import de.gnm.voxeldash.api.event.console.ConsoleMessageReceivedEvent;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetupHandler {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int PASSWORD_LENGTH = 10;
    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Pattern CHAT_PATTERN = Pattern.compile("<(\\w+)> (.+)$");
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^/?voxeldash setup$", Pattern.CASE_INSENSITIVE);

    private final VoxelDashLoader loader;
    private final File serverRoot;
    private final OutputStream consoleOutput;

    public SetupHandler(VoxelDashLoader loader, File serverRoot, OutputStream consoleOutput) {
        this.loader = loader;
        this.serverRoot = serverRoot;
        this.consoleOutput = consoleOutput;
    }

    public void register(EventDispatcher dispatcher) {
        dispatcher.registerListener(ConsoleMessageReceivedEvent.class, this::onConsoleMessage);
    }

    private void onConsoleMessage(ConsoleMessageReceivedEvent event) {
        Matcher chatMatcher = CHAT_PATTERN.matcher(event.getMessage());
        if (!chatMatcher.find()) {
            return;
        }

        String playerName = chatMatcher.group(1);
        String message = chatMatcher.group(2).trim();

        if (!COMMAND_PATTERN.matcher(message).matches()) {
            return;
        }

        handle(playerName);
    }

    private void handle(String playerName) {
        try {
            AccountController accounts = loader.getController(AccountController.class);

            boolean firstAccount = !accounts.hasAnyAccounts();
            if (!firstAccount && !isOperator(playerName)) {
                tell(playerName, "You must be a server operator to set up a VoxelDash account.", "red");
                return;
            }

            String password = generatePassword();

            if (accounts.accountExists(playerName)) {
                accounts.changePassword(playerName, password);
            } else {
                accounts.createAccount(playerName, password);
                int userId = accounts.getUserId(playerName);
                loader.getController(PermissionController.class).initializePermissions(userId);
            }

            tell(playerName, "Your VoxelDash login -> username: " + playerName + " | password: " + password, "green");
        } catch (Exception e) {
            LOG.error("Failed to set up VoxelDash account for " + playerName, e);
            tell(playerName, "Something went wrong while setting up your VoxelDash account.", "red");
        }
    }

    /**
     * Generates a random alphanumeric password.
     */
    private String generatePassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    /**
     * Checks whether a player is listed as an operator in ops.json.
     */
    private boolean isOperator(String playerName) {
        File opsFile = new File(serverRoot, "ops.json");
        if (!opsFile.exists()) {
            return false;
        }

        try {
            JsonNode ops = MAPPER.readTree(opsFile);
            if (ops.isArray()) {
                for (JsonNode op : ops) {
                    JsonNode name = op.get("name");
                    if (name != null && name.asText().equalsIgnoreCase(playerName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to read ops.json", e);
        }

        return false;
    }

    /**
     * Sends a colored message to the player by writing a tellraw command to the server's stdin.
     */
    private void tell(String playerName, String message, String color) {
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        PrintWriter writer = new PrintWriter(consoleOutput, true);
        writer.println("tellraw " + playerName + " {\"text\":\"[VoxelDash] " + escaped + "\",\"color\":\"" + color + "\"}");
    }
}
