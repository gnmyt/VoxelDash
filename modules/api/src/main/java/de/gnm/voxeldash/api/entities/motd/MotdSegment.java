package de.gnm.voxeldash.api.entities.motd;

public class MotdSegment {

    /**
     * The literal text of this segment.
     */
    public String text = "";

    /**
     * The color of this segment. Either one of the 16 vanilla color names
     * (e.g. {@code "red"}), a hex string ({@code "#RRGGBB"}), or {@code null}
     * to use the default (white).
     */
    public String color;

    public boolean bold;
    public boolean italic;
    public boolean underlined;
    public boolean strikethrough;
    public boolean obfuscated;

    public MotdSegment() {
    }

    public MotdSegment(String text) {
        this.text = text;
    }

    /**
     * @return {@code true} if this segment carries a hex color ({@code #RRGGBB}).
     */
    public boolean isHexColor() {
        return color != null && color.startsWith("#");
    }
}
