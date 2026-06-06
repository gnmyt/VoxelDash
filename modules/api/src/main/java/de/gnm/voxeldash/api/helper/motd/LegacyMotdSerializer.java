package de.gnm.voxeldash.api.helper.motd;

import de.gnm.voxeldash.api.entities.motd.Motd;
import de.gnm.voxeldash.api.entities.motd.MotdLine;
import de.gnm.voxeldash.api.entities.motd.MotdSegment;

public final class LegacyMotdSerializer {

    /**
     * The section sign that prefixes every legacy color/format code.
     */
    public static final char SECTION = '§';

    private LegacyMotdSerializer() {
    }

    /**
     * Serializes the MOTD to a legacy {@code §}-coded string.
     *
     * @param motd         the canonical model
     * @param hexSupported whether the target renders {@code §x} hex sequences
     * @return the legacy string (never {@code null})
     */
    public static String toLegacy(Motd motd, boolean hexSupported) {
        StringBuilder out = new StringBuilder();
        if (motd == null || motd.lines == null) {
            return "";
        }

        for (int li = 0; li < motd.lines.size(); li++) {
            if (li > 0) {
                out.append('\n');
            }
            MotdLine line = motd.lines.get(li);
            if (line == null || line.segments == null) {
                continue;
            }
            for (MotdSegment segment : line.segments) {
                appendSegment(out, segment, hexSupported);
            }
        }
        return out.toString();
    }

    private static void appendSegment(StringBuilder out, MotdSegment segment, boolean hexSupported) {
        if (segment == null) {
            return;
        }

        if (segment.color == null) {
            out.append(SECTION).append('r');
        } else if (segment.isHexColor()) {
            if (hexSupported) {
                appendHex(out, segment.color);
            } else {
                out.append(SECTION).append(NamedColors.nearest(segment.color).code);
            }
        } else {
            out.append(SECTION).append(NamedColors.codeOf(segment.color));
        }

        if (segment.bold) out.append(SECTION).append('l');
        if (segment.italic) out.append(SECTION).append('o');
        if (segment.underlined) out.append(SECTION).append('n');
        if (segment.strikethrough) out.append(SECTION).append('m');
        if (segment.obfuscated) out.append(SECTION).append('k');

        if (segment.text != null) {
            out.append(segment.text);
        }
    }

    private static void appendHex(StringBuilder out, String hex) {
        String digits = hex.replace("#", "");
        if (digits.length() != 6) {
            out.append(SECTION).append(NamedColors.nearest(hex).code);
            return;
        }
        out.append(SECTION).append('x');
        for (int i = 0; i < 6; i++) {
            out.append(SECTION).append(Character.toLowerCase(digits.charAt(i)));
        }
    }

    public static Motd fromLegacy(String legacy) {
        Motd motd = new Motd();
        if (legacy == null) {
            motd.lines.add(new MotdLine());
            return motd;
        }

        String normalized = legacy.replace("\\n", "\n");
        String[] rawLines = normalized.split("\n", -1);

        for (String rawLine : rawLines) {
            motd.lines.add(parseLine(rawLine));
        }
        if (motd.lines.isEmpty()) {
            motd.lines.add(new MotdLine());
        }
        return motd;
    }

    private static MotdLine parseLine(String raw) {
        MotdLine line = new MotdLine();
        MotdSegment current = new MotdSegment();
        StringBuilder text = new StringBuilder();

        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            boolean isCode = (c == SECTION || c == '&') && i + 1 < raw.length();
            if (!isCode) {
                text.append(c);
                i++;
                continue;
            }

            char code = Character.toLowerCase(raw.charAt(i + 1));

            if (code == 'x' && i + 13 < raw.length()) {
                StringBuilder hex = new StringBuilder("#");
                boolean ok = true;
                for (int k = 0; k < 6; k++) {
                    int sectionIndex = i + 2 + k * 2;
                    int digitIndex = sectionIndex + 1;
                    char marker = raw.charAt(sectionIndex);
                    if ((marker != SECTION && marker != '&') || digitIndex >= raw.length()) {
                        ok = false;
                        break;
                    }
                    hex.append(raw.charAt(digitIndex));
                }
                if (ok) {
                    current = flush(line, current, text);
                    current.color = hex.toString();
                    resetStyles(current);
                    i += 14;
                    continue;
                }
            }

            if (isColorCode(code)) {
                current = flush(line, current, text);
                current.color = nameForCode(code);
                resetStyles(current);
                i += 2;
            } else if (code == 'r') {
                current = flush(line, current, text);
                current.color = null;
                resetStyles(current);
                i += 2;
            } else if (isFormatCode(code)) {
                current = flush(line, current, text);
                applyFormat(current, code);
                i += 2;
            } else {
                text.append(c);
                i++;
            }
        }

        if (text.length() > 0) {
            current.text = text.toString();
            line.segments.add(current);
        }
        if (line.segments.isEmpty()) {
            line.segments.add(new MotdSegment());
        }
        return line;
    }

    /**
     * Pushes any accumulated text onto the line and returns a fresh segment that
     * inherits the current segment's color and formatting.
     */
    private static MotdSegment flush(MotdLine line, MotdSegment current, StringBuilder text) {
        if (text.length() > 0) {
            current.text = text.toString();
            line.segments.add(current);
            text.setLength(0);

            MotdSegment next = new MotdSegment();
            next.color = current.color;
            next.bold = current.bold;
            next.italic = current.italic;
            next.underlined = current.underlined;
            next.strikethrough = current.strikethrough;
            next.obfuscated = current.obfuscated;
            return next;
        }
        return current;
    }

    private static boolean isColorCode(char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

    private static boolean isFormatCode(char code) {
        return code == 'l' || code == 'o' || code == 'n' || code == 'm' || code == 'k';
    }

    private static void resetStyles(MotdSegment segment) {
        segment.bold = false;
        segment.italic = false;
        segment.underlined = false;
        segment.strikethrough = false;
        segment.obfuscated = false;
    }

    private static void applyFormat(MotdSegment segment, char code) {
        switch (code) {
            case 'l':
                segment.bold = true;
                break;
            case 'o':
                segment.italic = true;
                break;
            case 'n':
                segment.underlined = true;
                break;
            case 'm':
                segment.strikethrough = true;
                break;
            case 'k':
                segment.obfuscated = true;
                break;
            default:
                break;
        }
    }

    private static String nameForCode(char code) {
        switch (code) {
            case '0':
                return "black";
            case '1':
                return "dark_blue";
            case '2':
                return "dark_green";
            case '3':
                return "dark_aqua";
            case '4':
                return "dark_red";
            case '5':
                return "dark_purple";
            case '6':
                return "gold";
            case '7':
                return "gray";
            case '8':
                return "dark_gray";
            case '9':
                return "blue";
            case 'a':
                return "green";
            case 'b':
                return "aqua";
            case 'c':
                return "red";
            case 'd':
                return "light_purple";
            case 'e':
                return "yellow";
            case 'f':
                return "white";
            default:
                return "white";
        }
    }
}
