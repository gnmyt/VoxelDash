package de.gnm.voxeldash.api.entities;

public enum BackupPart {

    /**
     * Backups the complete server directory
     */
    ROOT(0, "root"),

    /**
     * Backups the world directories (every top-level directory containing a level.dat)
     */
    WORLDS(1, "worlds"),

    /**
     * Backups the plugins directory
     */
    PLUGINS(2, "plugins"),

    /**
     * Backups the configs (e.g. the server.properties, bukkit.yml, config/, etc.)
     */
    CONFIGS(4, "configs"),

    /**
     * Backups the logs directory
     */
    LOGS(8, "logs"),

    /**
     * Backups the mods directory
     */
    MODS(16, "mods");

    private final int backupBit;
    private final String id;

    /**
     * Constructor of the {@link BackupPart}
     * @param backupBit The backup bit of the backup part
     * @param id        The stable lowercase identifier (used as i18n key)
     */
    BackupPart(int backupBit, String id) {
        this.backupBit = backupBit;
        this.id = id;
    }

    /**
     * Gets the backup bit of the backup part
     * @return the backup bit of the backup part
     */
    public int getBackupBit() {
        return backupBit;
    }

    /**
     * Gets the stable lowercase identifier of the backup part
     * @return the identifier of the backup part
     */
    public String getId() {
        return id;
    }

    /**
     * Converts the given backup parts to a backup bit
     *
     * @param backupParts The backup parts you want to convert
     * @return the backup bit of the given backup parts
     */
    public static int toBackupBit(BackupPart... backupParts) {
        int backupBit = 0;
        for (BackupPart backupPart : backupParts) {
            backupBit |= backupPart.getBackupBit();
        }
        return backupBit;
    }

    /**
     * Converts the given backup bit to backup parts
     *
     * @param backupBit The backup bit you want to convert
     * @return the backup parts of the given backup bit
     */
    public static BackupPart[] fromBackupBit(int backupBit) {
        if (backupBit < 0) return new BackupPart[0];
        if (backupBit == 0) return new BackupPart[]{ROOT};

        int length = Integer.bitCount(backupBit);
        BackupPart[] backupParts = new BackupPart[length];
        int index = 0;

        for (BackupPart backupPart : values()) {
            if ((backupBit & backupPart.getBackupBit()) != 0) {
                backupParts[index++] = backupPart;
            }
        }

        return backupParts;
    }


    /**
     * Checks if the given backup bit is valid
     * @param backupBit The backup bit you want to check
     * @return <code>true</code> if the backup bit is valid, otherwise <code>false</code>
     */
    public static boolean isValidBackupBit(int backupBit) {
        return fromBackupBit(backupBit).length > 0;
    }
}
