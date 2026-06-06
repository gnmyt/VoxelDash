package de.gnm.voxeldash.api.entities;

public enum Feature {

    FileManager(1),
    Properties(2),
    SSH(4),
    Backups(8),
    Console(16),
    Players(32),
    UserManagement(64),
    Schedules(128),
    Worlds(256),
    Resources(512),
    Motd(1024),
    Profiling(2048);

    private final int permissionBit;

    Feature(int permissionBit) {
        this.permissionBit = permissionBit;
    }

    /**
     * Gets the permission bit of the feature
     *
     * @return the permission bit of the feature
     */
    public int getPermissionBit() {
        return permissionBit;
    }

    /**
     * Converts the given features to a permission bit
     *
     * @param features The features you want to convert
     * @return the permission bit of the given features
     */
    public static int toPermissionBit(Feature... features) {
        int permissionBit = 0;
        for (Feature feature : features) {
            permissionBit |= feature.getPermissionBit();
        }
        return permissionBit;
    }

    /**
     * Converts the given permission bit to features
     *
     * @param permissionBit The permission bit you want to convert
     * @return the features of the given permission bit
     */
    public static Feature[] fromPermissionBit(int permissionBit) {
        int length = Integer.bitCount(permissionBit);
        Feature[] features = new Feature[length];
        int index = 0;
        for (Feature feature : values()) {
            if ((permissionBit & feature.getPermissionBit()) != 0) {
                features[index++] = feature;
            }
        }
        return features;
    }

}
