package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.players.InventoryCapabilities;
import de.gnm.voxeldash.api.entities.players.InventoryItem;
import de.gnm.voxeldash.api.entities.players.InventoryView;
import de.gnm.voxeldash.api.helper.OfflinePlayerReader;
import de.gnm.voxeldash.api.pipes.players.InventoryPipe;
import de.gnm.voxeldash.util.ForgeUtil;

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
        boolean live = ForgeUtil.compat().inventorySupported();
        return new InventoryCapabilities(live, true, true, live);
    }

    @Override
    public InventoryView getInventory(UUID uuid, boolean online) {
        if (online) {
            return ForgeUtil.compat().readInventory(uuid);
        }
        return reader().readInventory(uuid);
    }

    @Override
    public InventoryView getEnderChest(UUID uuid, boolean online) {
        if (online) {
            return ForgeUtil.compat().readEnderChest(uuid);
        }
        return reader().readEnderChest(uuid);
    }

    @Override
    public void setSlot(UUID uuid, boolean enderChest, int slot, InventoryItem item) {
        ForgeUtil.compat().setSlot(uuid, enderChest, slot, item);
    }

    @Override
    public void moveSlot(UUID uuid, boolean enderChest, int fromSlot, int toSlot) {
        ForgeUtil.compat().moveSlot(uuid, enderChest, fromSlot, toSlot);
    }

    @Override
    public void giveItem(UUID uuid, String id, int count) {
        ForgeUtil.compat().giveItem(uuid, id, count);
    }

    @Override
    public void clear(UUID uuid, boolean enderChest) {
        ForgeUtil.compat().clearInventory(uuid, enderChest);
    }
}
