package de.gnm.voxeldash.api.entities.players;

public class TeleportCapabilities {

    public boolean coords;
    public boolean toPlayer;
    public boolean toSpawn;

    /**
     * Proxy-style "send the player to another backend server" (BungeeCord).
     */
    public boolean toServer;

    public TeleportCapabilities() {
    }

    public TeleportCapabilities(boolean coords, boolean toPlayer, boolean toSpawn, boolean toServer) {
        this.coords = coords;
        this.toPlayer = toPlayer;
        this.toSpawn = toSpawn;
        this.toServer = toServer;
    }
}
