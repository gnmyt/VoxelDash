package de.gnm.voxeldash.api.entities.players;

public class IpHistoryEntry {

    public String ip;
    public long firstSeen;
    public long lastSeen;
    public int count;

    public IpHistoryEntry() {
    }

    public IpHistoryEntry(String ip, long firstSeen, long lastSeen, int count) {
        this.ip = ip;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.count = count;
    }
}
