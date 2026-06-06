package de.gnm.loader.pipes;

import de.gnm.voxeldash.api.entities.players.InventoryCapabilities;
import de.gnm.voxeldash.api.entities.players.InventoryItem;
import de.gnm.voxeldash.api.entities.players.InventoryView;
import de.gnm.voxeldash.api.helper.OfflinePlayerReader;
import de.gnm.voxeldash.api.pipes.players.InventoryPipe;

import java.io.File;
import java.util.UUID;

public class InventoryPipeImpl implements InventoryPipe {

    private final OfflinePlayerReader reader;

    public InventoryPipeImpl(File serverRoot) {
        this.reader = new OfflinePlayerReader(serverRoot);
    }

    @Override
    public InventoryCapabilities getCapabilities() {
        return new InventoryCapabilities(false, true, true, false);
    }

    @Override
    public InventoryView getInventory(UUID uuid, boolean online) {
        return reader.readInventory(uuid);
    }

    @Override
    public InventoryView getEnderChest(UUID uuid, boolean online) {
        return reader.readEnderChest(uuid);
    }

    @Override
    public void setSlot(UUID uuid, boolean enderChest, int slot, InventoryItem item) {
        // not supported on vanilla
    }

    @Override
    public void giveItem(UUID uuid, String id, int count) {
        // not supported on vanilla
    }

    @Override
    public void clear(UUID uuid, boolean enderChest) {
        // not supported on vanilla
    }
}
