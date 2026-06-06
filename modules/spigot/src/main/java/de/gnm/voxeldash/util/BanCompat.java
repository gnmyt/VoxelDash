package de.gnm.voxeldash.util;

import de.gnm.voxeldash.api.entities.BannedPlayer;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Version independent access to the server ban list, preferring the modern profile ban list and
 * falling back to the legacy name list via reflection so the class still loads on 1.8.9.
 */
public final class BanCompat {

    private BanCompat() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<BannedPlayer> listBans() {
        List<BannedPlayer> result = new ArrayList<>();

        BanList banList = resolveBanList();
        if (banList == null) {
            return result;
        }

        for (Object raw : entriesOf(banList)) {
            if (!(raw instanceof BanEntry)) {
                continue;
            }
            BanEntry entry = (BanEntry) raw;

            String name = resolveName(entry);
            if (name == null || name.isEmpty()) {
                continue;
            }

            UUID uuid = resolveUuid(entry, name);
            if (uuid == null) {
                continue;
            }

            result.add(new BannedPlayer(name, uuid, entry.getReason(),
                    entry.getCreated(), entry.getExpiration(), entry.getSource()));
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    public static void ban(String name, String reason, String source) {
        BanList banList = resolveBanList();
        if (banList != null) {
            banList.addBan(name, reason, (Date) null, source);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void tempBan(String name, String reason, Date expiry, String source) {
        BanList banList = resolveBanList();
        if (banList != null) {
            banList.addBan(name, reason, expiry, source);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void unban(String name) {
        BanList banList = resolveBanList();
        if (banList != null) {
            banList.pardon(name);
        }
    }

    @SuppressWarnings("rawtypes")
    private static BanList resolveBanList() {
        try {
            return Bukkit.getBanList(BanList.Type.valueOf("PROFILE"));
        } catch (Throwable ignored) {
        }
        try {
            return Bukkit.getBanList(BanList.Type.NAME);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private static Set entriesOf(BanList banList) {
        for (String name : new String[]{"getEntries", "getBanEntries"}) {
            Object value = invoke(banList, name);
            if (value instanceof Set) {
                return (Set) value;
            }
        }
        return Collections.emptySet();
    }

    private static String resolveName(BanEntry entry) {
        Object name = invoke(invoke(entry, "getBanTarget"), "getName");
        if (name instanceof String && !((String) name).isEmpty()) {
            return (String) name;
        }
        try {
            return entry.getTarget();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static UUID resolveUuid(BanEntry entry, String name) {
        Object id = invoke(invoke(entry, "getBanTarget"), "getUniqueId");
        if (id instanceof UUID) {
            return (UUID) id;
        }
        try {
            return Bukkit.getOfflinePlayer(name).getUniqueId();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, String method) {
        if (target == null) {
            return null;
        }
        try {
            Method m = target.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
