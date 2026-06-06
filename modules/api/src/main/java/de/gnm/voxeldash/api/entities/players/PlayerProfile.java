package de.gnm.voxeldash.api.entities.players;

import java.util.ArrayList;
import java.util.List;

public class PlayerProfile {

    public String name;
    public String uuid;
    public boolean online;

    /**
     * First time the player joined (epoch millis), or 0 if unknown.
     */
    public long firstJoin;

    /**
     * Last time the player was seen (epoch millis), or 0 if unknown / currently online.
     */
    public long lastSeen;

    /**
     * Total playtime in milliseconds (0 if unknown).
     */
    public long playtimeMillis;

    public double health;
    public int foodLevel;
    public String gamemode;
    public String dimension;
    public boolean op;
    public boolean banned;
    public boolean whitelisted;

    public boolean muted;
    public long muteExpiry;
    public List<IpHistoryEntry> ipHistory = new ArrayList<>();

    public PlayerProfile() {
    }
}
