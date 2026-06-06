package de.gnm.voxeldash.api.entities.motd;

import java.util.ArrayList;
import java.util.List;

public class Motd {

    /**
     * The MOTD lines (at most two are rendered by the client).
     */
    public List<MotdLine> lines = new ArrayList<>();

    public Motd() {
    }

    /**
     * @return an empty MOTD with a single blank line.
     */
    public static Motd empty() {
        Motd motd = new Motd();
        motd.lines.add(new MotdLine());
        return motd;
    }

    /**
     * @return the MOTD with all formatting stripped, lines joined by {@code \n}.
     */
    public String plainText() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            for (MotdSegment segment : lines.get(i).segments) {
                if (segment.text != null) {
                    builder.append(segment.text);
                }
            }
        }
        return builder.toString();
    }
}
