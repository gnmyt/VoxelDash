package de.gnm.voxeldash.api.entities.motd;

import java.util.ArrayList;
import java.util.List;

public class MotdLine {

    /**
     * The ordered segments that make up this line.
     */
    public List<MotdSegment> segments = new ArrayList<>();

    public MotdLine() {
    }

    public MotdLine(List<MotdSegment> segments) {
        this.segments = segments;
    }
}
