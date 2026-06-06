package de.gnm.voxeldash.api.entities.players;

public class InventoryCapabilities {

    /**
     * Whether inventories of currently online players can be read from the live server.
     */
    public boolean viewOnline;

    /**
     * Whether inventories of offline players can be read from NBT on disk.
     */
    public boolean viewOffline;

    /**
     * Whether the ender chest can be read.
     */
    public boolean viewEnderChest;

    /**
     * Whether inventories of online players can be edited (give/remove/move/clear).
     * Offline editing is never supported.
     */
    public boolean editOnline;

    public InventoryCapabilities() {
    }

    public InventoryCapabilities(boolean viewOnline, boolean viewOffline, boolean viewEnderChest, boolean editOnline) {
        this.viewOnline = viewOnline;
        this.viewOffline = viewOffline;
        this.viewEnderChest = viewEnderChest;
        this.editOnline = editOnline;
    }
}
