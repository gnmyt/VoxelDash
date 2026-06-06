package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.players.InventoryCapabilities;
import de.gnm.voxeldash.api.entities.players.InventoryItem;
import de.gnm.voxeldash.api.entities.players.InventoryView;
import de.gnm.voxeldash.api.helper.OfflinePlayerReader;
import de.gnm.voxeldash.api.pipes.players.InventoryPipe;
import de.gnm.voxeldash.util.FabricUtil;

import java.io.File;
import java.util.UUID;

public class InventoryPipeImpl implements InventoryPipe {

    private OfflinePlayerReader reader;

    private OfflinePlayerReader reader() {
        if (reader == null) {
            reader = new OfflinePlayerReader(new File(System.getProperty("user.dir")));
        }
        return reader;
    }

    @Override
    public InventoryCapabilities getCapabilities() {
        boolean live = FabricUtil.compat().inventorySupported();
        return new InventoryCapabilities(live, true, true, live);
    }

    @Override
    public InventoryView getInventory(UUID uuid, boolean online) {
        if (online) {
            return FabricUtil.compat().readInventory(uuid);
        }
        return reader().readInventory(uuid);
    }

    @Override
    public InventoryView getEnderChest(UUID uuid, boolean online) {
        if (online) {
            return FabricUtil.compat().readEnderChest(uuid);
        }
        return reader().readEnderChest(uuid);
    }

    @Override
    public void setSlot(UUID uuid, boolean enderChest, int slot, InventoryItem item) {
        FabricUtil.compat().setSlot(uuid, enderChest, slot, item);
    }

    @Override
    public void moveSlot(UUID uuid, boolean enderChest, int fromSlot, int toSlot) {
        FabricUtil.compat().moveSlot(uuid, enderChest, fromSlot, toSlot);
    }

    @Override
    public void giveItem(UUID uuid, String id, int count) {
        FabricUtil.compat().giveItem(uuid, id, count);
    }

    @Override
    public void clear(UUID uuid, boolean enderChest) {
        FabricUtil.compat().clearInventory(uuid, enderChest);
    }
}
