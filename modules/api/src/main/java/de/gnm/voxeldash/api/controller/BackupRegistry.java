package de.gnm.voxeldash.api.controller;

import de.gnm.voxeldash.api.entities.BackupPart;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds the set of {@link BackupPart}s that the current module offers for backups.
 * <p>
 * Each platform module registers the categories that make sense for it (e.g. a Spigot
 * server offers {@link BackupPart#PLUGINS} while a modded server offers
 * {@link BackupPart#MODS}). The {@link BackupPart#ROOT} ("complete server") option is
 * always available and does not need to be registered.
 */
public class BackupRegistry {

    private final Set<BackupPart> parts = new LinkedHashSet<>();

    /**
     * Registers one or more backup parts as offered by this module.
     *
     * @param backupParts the parts to offer
     */
    public void register(BackupPart... backupParts) {
        for (BackupPart part : backupParts) {
            if (part != BackupPart.ROOT) {
                parts.add(part);
            }
        }
    }

    /**
     * Gets all offered backup parts. {@link BackupPart#ROOT} is always reported first,
     * followed by the registered granular parts in registration order.
     *
     * @return the offered backup parts
     */
    public List<BackupPart> getParts() {
        List<BackupPart> result = new ArrayList<>();
        result.add(BackupPart.ROOT);
        result.addAll(parts);
        return result;
    }
}
