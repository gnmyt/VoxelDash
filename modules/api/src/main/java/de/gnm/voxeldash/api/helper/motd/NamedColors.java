package de.gnm.voxeldash.api.helper.motd;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NamedColors {

    private static final Map<String, NamedColor> BY_NAME = new LinkedHashMap<>();

    static {
        register("black", '0', 0x000000);
        register("dark_blue", '1', 0x0000AA);
        register("dark_green", '2', 0x00AA00);
        register("dark_aqua", '3', 0x00AAAA);
        register("dark_red", '4', 0xAA0000);
        register("dark_purple", '5', 0xAA00AA);
        register("gold", '6', 0xFFAA00);
        register("gray", '7', 0xAAAAAA);
        register("dark_gray", '8', 0x555555);
        register("blue", '9', 0x5555FF);
        register("green", 'a', 0x55FF55);
        register("aqua", 'b', 0x55FFFF);
        register("red", 'c', 0xFF5555);
        register("light_purple", 'd', 0xFF55FF);
        register("yellow", 'e', 0xFFFF55);
        register("white", 'f', 0xFFFFFF);
    }

    private NamedColors() {
    }

    private static void register(String name, char code, int rgb) {
        BY_NAME.put(name, new NamedColor(name, code, rgb));
    }

    /**
     * @return the named color with the given name, or {@code null} if unknown.
     */
    public static NamedColor byName(String name) {
        if (name == null) {
            return null;
        }
        return BY_NAME.get(name.toLowerCase());
    }

    /**
     * @return the legacy code character for the named color, or {@code 'f'}
     * (white) if the name is unknown.
     */
    public static char codeOf(String name) {
        NamedColor color = byName(name);
        return color != null ? color.code : 'f';
    }

    /**
     * Finds the named color closest (in RGB space) to the given hex color.
     *
     * @param hex a {@code #RRGGBB} string
     * @return the nearest named color, or white if the hex cannot be parsed.
     */
    public static NamedColor nearest(String hex) {
        int rgb;
        try {
            rgb = Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Exception e) {
            return byName("white");
        }

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        NamedColor best = byName("white");
        long bestDistance = Long.MAX_VALUE;
        for (NamedColor candidate : BY_NAME.values()) {
            int cr = (candidate.rgb >> 16) & 0xFF;
            int cg = (candidate.rgb >> 8) & 0xFF;
            int cb = candidate.rgb & 0xFF;
            long distance = (long) (r - cr) * (r - cr)
                    + (long) (g - cg) * (g - cg)
                    + (long) (b - cb) * (b - cb);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * A single named color.
     */
    public static final class NamedColor {
        public final String name;
        public final char code;
        public final int rgb;

        NamedColor(String name, char code, int rgb) {
            this.name = name;
            this.code = code;
            this.rgb = rgb;
        }
    }
}
