package de.gnm.voxeldash.api.pipes.players;

import de.gnm.voxeldash.api.entities.players.TeleportCapabilities;
import de.gnm.voxeldash.api.pipes.BasePipe;

public interface TeleportPipe extends BasePipe {

    /**
     * Reports which teleport modes this platform supports.
     */
    TeleportCapabilities getCapabilities();

    /**
     * Teleports a player to absolute coordinates.
     *
     * @param playerName the player to teleport
     * @param x          target x
     * @param y          target y
     * @param z          target z
     * @param world      target world name, or null to stay in the current world
     */
    void teleportToCoords(String playerName, double x, double y, double z, String world);

    /**
     * Teleports a player to another player.
     *
     * @param playerName the player to teleport
     * @param targetName the player to teleport to
     */
    void teleportToPlayer(String playerName, String targetName);

    /**
     * Teleports a player to the world spawn.
     *
     * @param playerName the player to teleport
     */
    void teleportToSpawn(String playerName);

    /**
     * Proxy-only: sends a player to another backend server.
     *
     * @param playerName the player to move
     * @param serverName the target backend server name
     */
    void teleportToServer(String playerName, String serverName);
}
