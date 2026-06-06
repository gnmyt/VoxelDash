package de.gnm.voxeldash.api.pipes.players;

import de.gnm.voxeldash.api.entities.players.InventoryCapabilities;
import de.gnm.voxeldash.api.entities.players.InventoryItem;
import de.gnm.voxeldash.api.entities.players.InventoryView;
import de.gnm.voxeldash.api.pipes.BasePipe;

import java.util.UUID;

public interface InventoryPipe extends BasePipe {

    /**
     * Reports what this platform can do with inventories.
     */
    InventoryCapabilities getCapabilities();

    /**
     * Reads a player's main inventory (including armor and offhand).
     *
     * @param uuid   the player's UUID
     * @param online whether the player is currently online (read live vs. from disk)
     * @return the inventory snapshot (never null; may be empty)
     */
    InventoryView getInventory(UUID uuid, boolean online);

    /**
     * Reads a player's ender chest.
     *
     * @param uuid   the player's UUID
     * @param online whether the player is currently online
     * @return the ender-chest snapshot (never null; may be empty)
     */
    InventoryView getEnderChest(UUID uuid, boolean online);

    /**
     * Sets (or, when {@code item} is null, clears) a single slot of an online player.
     *
     * @param uuid       the player's UUID (must be online)
     * @param enderChest true to target the ender chest, false for the main inventory
     * @param slot       the slot index (vanilla NBT convention, see {@link InventoryItem})
     * @param item       the item to place, or null to clear the slot
     */
    void setSlot(UUID uuid, boolean enderChest, int slot, InventoryItem item);

    /**
     * Swaps the contents of two slots of an online player's container, preserving
     * the full item (NBT, enchantments, etc.). Used for drag-and-drop in the UI.
     * Both slots are in the same container. Default no-op for platforms/versions
     * without live editing.
     *
     * @param uuid       the player's UUID (must be online)
     * @param enderChest true to target the ender chest, false for the main inventory
     * @param fromSlot   the source slot (vanilla NBT convention)
     * @param toSlot     the destination slot
     */
    default void moveSlot(UUID uuid, boolean enderChest, int fromSlot, int toSlot) {
    }

    /**
     * Gives an item to an online player (first free slot of the main inventory).
     *
     * @param uuid  the player's UUID (must be online)
     * @param id    the namespaced item id
     * @param count the stack size
     */
    void giveItem(UUID uuid, String id, int count);

    /**
     * Clears an online player's container.
     *
     * @param uuid       the player's UUID (must be online)
     * @param enderChest true to clear the ender chest, false for the main inventory
     */
    void clear(UUID uuid, boolean enderChest);
}
