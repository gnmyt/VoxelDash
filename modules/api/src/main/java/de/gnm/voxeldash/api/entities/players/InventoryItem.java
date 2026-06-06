package de.gnm.voxeldash.api.entities.players;

import java.util.List;

public class InventoryItem {

    /**
     * The slot index (vanilla NBT convention, see class docs).
     */
    public int slot;

    /**
     * The namespaced item id (e.g. {@code minecraft:diamond_sword}). For legacy
     * pre-1.13 numeric ids that could not be resolved, this is
     * {@code minecraft:legacy_<numericId>} and the UI falls back to a generic icon.
     */
    public String id;

    /**
     * The stack size.
     */
    public int count = 1;

    /**
     * Durability already used (0 when undamaged or unknown).
     */
    public int damage;

    /**
     * Maximum durability of the item (0 when not damageable or unknown - common
     * for offline reads where the item's max durability is not stored in NBT).
     */
    public int maxDamage;

    /**
     * The custom display name (plain text), or {@code null} when the item has none.
     */
    public String name;

    /**
     * Whether the item carries enchantments (drives the glint affordance in the UI).
     */
    public boolean enchanted;

    /**
     * For player heads: the owner UUID (used to render the head via a head service),
     * or {@code null} for any other item.
     */
    public String headOwner;

    /**
     * Optional lore lines (plain text), or {@code null} when the item has none.
     */
    public List<String> lore;

    public InventoryItem() {
    }

    public InventoryItem(int slot, String id, int count) {
        this.slot = slot;
        this.id = id;
        this.count = count;
    }
}
