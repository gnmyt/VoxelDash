package de.gnm.loader.pipes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gnm.loader.helper.NBTHelper;
import de.gnm.loader.helper.PlayerTracker;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import net.querz.nbt.tag.CompoundTag;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.UUID;

public class OnlinePlayerPipeImpl implements OnlinePlayerPipe {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");
    private static final long SAVE_DEBOUNCE_MS = 3000;

    private final BufferedWriter consoleWriter;
    private final PlayerTracker playerTracker;
    private final File worldFolder;

    private volatile long lastSaveAll = 0;

    public OnlinePlayerPipeImpl(OutputStream console, PlayerTracker playerTracker, File worldFolder) {
        this.consoleWriter = new BufferedWriter(new OutputStreamWriter(console));
        this.playerTracker = playerTracker;
        this.worldFolder = worldFolder;
    }

    @Override
    public ArrayList<OnlinePlayer> getOnlinePlayers() {
        ArrayList<OnlinePlayer> players = new ArrayList<>();

        ensureFreshPlayerData();

        for (PlayerTracker.TrackedPlayer tracked : playerTracker.getOnlinePlayers()) {
            players.add(buildOnlinePlayer(
                    tracked.getName(),
                    tracked.getUuid(),
                    tracked.getIpAddress(),
                    tracked.getJoinTime()
            ));
        }

        return players;
    }

    public OnlinePlayer buildOnlinePlayer(String name, UUID uuid, String ipAddress, long joinTime) {
        CompoundTag playerData = NBTHelper.readPlayerData(worldFolder, uuid);

        double health = NBTHelper.getHealth(playerData);
        int foodLevel = NBTHelper.getFoodLevel(playerData);
        String gameMode = NBTHelper.getGameMode(playerData);
        String dimension = NBTHelper.getDimension(playerData);

        String worldName = dimensionToWorldName(dimension);
        boolean isOp = isOperator(name);
        long sessionTime = System.currentTimeMillis() - joinTime;

        return new OnlinePlayer(name, uuid, worldName, ipAddress, health, foodLevel, isOp, gameMode, sessionTime);
    }

    @Override
    public void kickPlayer(String playerName, String reason) {
        try {
            String command = "kick " + playerName;
            if (reason != null && !reason.isEmpty()) {
                command += " " + reason;
            }
            consoleWriter.write(command + System.lineSeparator());
            consoleWriter.flush();
        } catch (Exception e) {
            LOG.error("Failed to kick player " + playerName, e);
        }
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        try {
            String mode = gamemode.toLowerCase();
            consoleWriter.write("gamemode " + mode + " " + playerName + System.lineSeparator());
            consoleWriter.flush();
            saveAll();
        } catch (Exception e) {
            LOG.error("Failed to set gamemode for " + playerName, e);
        }
    }

    @Override
    public void teleportToWorld(String playerName, String worldName) {
        try {
            String dimension = worldNameToDimension(worldName);
            consoleWriter.write("execute in " + dimension + " run tp " + playerName + " ~ ~ ~" + System.lineSeparator());
            consoleWriter.flush();
            saveAll();
        } catch (Exception e) {
            LOG.error("Failed to teleport " + playerName + " to " + worldName, e);
        }
    }

    public void ensureFreshPlayerData() {
        if (System.currentTimeMillis() - lastSaveAll >= SAVE_DEBOUNCE_MS) {
            saveAll();
        }
    }

    private void saveAll() {
        try {
            consoleWriter.write("save-all" + System.lineSeparator());
            consoleWriter.flush();
            lastSaveAll = System.currentTimeMillis();
            Thread.sleep(100);
        } catch (Exception e) {
            LOG.debug("Failed to trigger world save", e);
        }
    }

    /**
     * Converts a Minecraft dimension identifier to a friendly world name
     */
    private String dimensionToWorldName(String dimension) {
        if (dimension == null) return "Overworld";
        switch (dimension) {
            case "minecraft:overworld": return "Overworld";
            case "minecraft:the_nether": return "Nether";
            case "minecraft:the_end": return "The End";
            default:
                if (dimension.contains(":")) {
                    return dimension.substring(dimension.indexOf(":") + 1);
                }
                return dimension;
        }
    }

    /**
     * Converts a friendly world name to a Minecraft dimension identifier
     */
    private String worldNameToDimension(String worldName) {
        if (worldName == null) return "minecraft:overworld";
        switch (worldName.toLowerCase()) {
            case "overworld": case "world": return "minecraft:overworld";
            case "nether": case "the_nether": case "world_nether": return "minecraft:the_nether";
            case "end": case "the_end": case "world_the_end": return "minecraft:the_end";
            default: return worldName.contains(":") ? worldName : "minecraft:" + worldName;
        }
    }

    /**
     * Checks if a player is an operator by reading ops.json
     */
    private boolean isOperator(String playerName) {
        try {
            File opsFile = new File("ops.json");
            if (!opsFile.exists()) return false;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode ops = mapper.readTree(opsFile);

            for (JsonNode op : ops) {
                if (op.has("name") && op.get("name").asText().equalsIgnoreCase(playerName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to check operator status for " + playerName, e);
        }
        return false;
    }
}
