package de.gnm.voxeldash.api.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gnm.voxeldash.api.entities.motd.Motd;
import de.gnm.voxeldash.api.helper.motd.LegacyMotdSerializer;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class MotdHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final File MOTD_FILE = new File("voxeldash-motd.json");
    private static final File ICON_FILE = new File("server-icon.png");

    private MotdHelper() {
    }

    /**
     * Loads the canonical MOTD. On first use (no {@code voxeldash-motd.json})
     * the existing {@code server.properties} {@code motd=} value is imported so
     * the current MOTD is not lost.
     *
     * @return the MOTD (never {@code null})
     */
    public static Motd getMotd() {
        if (MOTD_FILE.exists()) {
            try {
                Motd motd = MAPPER.readValue(MOTD_FILE, Motd.class);
                if (motd != null && motd.lines != null) {
                    return motd;
                }
            } catch (Exception ignored) {
            }
        }

        String existing = PropertyHelper.getProperty("motd");
        if (existing != null && !existing.isEmpty()) {
            return LegacyMotdSerializer.fromLegacy(existing);
        }
        return Motd.empty();
    }

    /**
     * Persists the canonical MOTD.
     *
     * @param motd the model to store
     */
    public static void saveMotd(Motd motd) {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(MOTD_FILE, motd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the server icon file ({@code server-icon.png}).
     */
    public static File getIconFile() {
        return ICON_FILE;
    }

    /**
     * @return {@code true} if a server icon is present.
     */
    public static boolean hasIcon() {
        return ICON_FILE.exists();
    }

    /**
     * @return the server icon as a base64 string, or {@code null} if none.
     */
    public static String getIconBase64() {
        if (!ICON_FILE.exists()) {
            return null;
        }
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(ICON_FILE.toPath()));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Writes the server icon PNG bytes to {@code server-icon.png}.
     *
     * @param png the raw PNG bytes
     */
    public static void saveIcon(byte[] png) {
        try {
            Files.write(ICON_FILE.toPath(), png);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the server icon if present.
     */
    public static void clearIcon() {
        try {
            Files.deleteIfExists(ICON_FILE.toPath());
        } catch (Exception ignored) {
        }
    }
}
