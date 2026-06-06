package de.gnm.voxeldash.api;

import java.io.InputStream;
import java.util.Properties;

public final class VoxelDashVersion {

    private static final String VERSION = load();

    private VoxelDashVersion() {
    }

    public static String get() {
        return VERSION;
    }

    private static String load() {
        try (InputStream in = VoxelDashVersion.class.getResourceAsStream("/voxeldash-version.properties")) {
            if (in == null) return "unknown";
            Properties properties = new Properties();
            properties.load(in);
            String version = properties.getProperty("version");
            if (version == null) return "unknown";
            version = version.trim();
            if (version.isEmpty() || version.startsWith("${")) return "unknown";
            return version;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
