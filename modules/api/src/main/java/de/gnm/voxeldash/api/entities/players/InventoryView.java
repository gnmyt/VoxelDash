package de.gnm.voxeldash.api.entities.players;

import java.util.ArrayList;
import java.util.List;

public class InventoryView {

    /**
     * The container type: {@code "inventory"} or {@code "enderchest"}.
     */
    public String type;

    /**
     * The items in the container. Empty slots are simply omitted; each item
     * carries its own {@link InventoryItem#slot}.
     */
    public List<InventoryItem> items = new ArrayList<>();

    /**
     * Whether this snapshot was read from the live server (true) or from offline
     * NBT on disk (false). Offline snapshots are read-only.
     */
    public boolean live;

    public InventoryView() {
    }

    public InventoryView(String type, boolean live) {
        this.type = type;
        this.live = live;
    }
}
