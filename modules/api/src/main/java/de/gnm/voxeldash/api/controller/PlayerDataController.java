package de.gnm.voxeldash.api.controller;

import de.gnm.voxeldash.api.entities.players.IpHistoryEntry;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerDataController extends BaseController {

    public PlayerDataController(Connection connection) {
        super(connection);
        createTables();
    }

    private void createTables() {
        executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_mutes (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "reason TEXT, " +
                        "expiry INTEGER NOT NULL DEFAULT 0, " +
                        "created_at INTEGER NOT NULL DEFAULT 0, " +
                        "source TEXT)"
        );

        executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_ips (" +
                        "uuid TEXT NOT NULL, " +
                        "ip TEXT NOT NULL, " +
                        "first_seen INTEGER NOT NULL DEFAULT 0, " +
                        "last_seen INTEGER NOT NULL DEFAULT 0, " +
                        "count INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY (uuid, ip))"
        );
    }

    /**
     * Mutes a player until {@code expiry} (epoch millis; 0 = permanent).
     */
    public void mute(UUID uuid, String reason, long expiry, String source, long now) {
        executeUpdate(
                "INSERT INTO player_mutes (uuid, reason, expiry, created_at, source) VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET reason = excluded.reason, expiry = excluded.expiry, " +
                        "created_at = excluded.created_at, source = excluded.source",
                uuid.toString(), reason, expiry, now, source
        );
    }

    public void unmute(UUID uuid) {
        executeUpdate("DELETE FROM player_mutes WHERE uuid = ?", uuid.toString());
    }

    /**
     * Whether the player is currently muted, taking expiry into account. Expired
     * rows are purged lazily on read.
     */
    public boolean isMuted(UUID uuid) {
        HashMap<String, Object> row = getSingleResult("SELECT expiry FROM player_mutes WHERE uuid = ?", uuid.toString());
        if (row == null) {
            return false;
        }
        long expiry = ((Number) row.get("expiry")).longValue();
        if (expiry != 0 && expiry <= System.currentTimeMillis()) {
            unmute(uuid);
            return false;
        }
        return true;
    }

    /**
     * Returns the mute expiry (epoch millis; 0 = permanent), or -1 if not muted.
     */
    public long getMuteExpiry(UUID uuid) {
        HashMap<String, Object> row = getSingleResult("SELECT expiry FROM player_mutes WHERE uuid = ?", uuid.toString());
        if (row == null) {
            return -1;
        }
        long expiry = ((Number) row.get("expiry")).longValue();
        if (expiry != 0 && expiry <= System.currentTimeMillis()) {
            unmute(uuid);
            return -1;
        }
        return expiry;
    }

    /**
     * Records that {@code uuid} was seen from {@code ip} at {@code now}. Idempotent
     * per (uuid, ip): updates last-seen and increments the connection count.
     */
    public void recordIp(UUID uuid, String ip, long now) {
        if (ip == null || ip.isEmpty()) {
            return;
        }
        executeUpdate(
                "INSERT INTO player_ips (uuid, ip, first_seen, last_seen, count) VALUES (?, ?, ?, ?, 1) " +
                        "ON CONFLICT(uuid, ip) DO UPDATE SET last_seen = excluded.last_seen, count = count + 1",
                uuid.toString(), ip, now, now
        );
    }

    public List<IpHistoryEntry> getIps(UUID uuid) {
        ArrayList<HashMap<String, Object>> rows = getMultipleResults(
                "SELECT * FROM player_ips WHERE uuid = ? ORDER BY last_seen DESC", uuid.toString());
        List<IpHistoryEntry> ips = new ArrayList<>();
        if (rows == null) {
            return ips;
        }
        for (HashMap<String, Object> row : rows) {
            ips.add(new IpHistoryEntry(
                    (String) row.get("ip"),
                    ((Number) row.get("first_seen")).longValue(),
                    ((Number) row.get("last_seen")).longValue(),
                    ((Number) row.get("count")).intValue()
            ));
        }
        return ips;
    }
}
