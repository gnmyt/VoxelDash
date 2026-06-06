package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashSpigot;
import de.gnm.voxeldash.api.entities.players.InventoryCapabilities;
import de.gnm.voxeldash.api.entities.players.InventoryItem;
import de.gnm.voxeldash.api.entities.players.InventoryView;
import de.gnm.voxeldash.api.helper.OfflinePlayerReader;
import de.gnm.voxeldash.api.pipes.players.InventoryPipe;
import de.gnm.voxeldash.util.BukkitUtil;
import de.gnm.voxeldash.util.ItemCompat;
import de.gnm.voxeldash.util.VersionCompat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.UUID;

public class InventoryPipeImpl implements InventoryPipe {

    private static final int[] ARMOR_SLOTS = {100, 101, 102, 103}; // boots, legs, chest, helmet
    private static final int OFFHAND_SLOT = -106;

    private OfflinePlayerReader reader;

    private OfflinePlayerReader reader() {
        if (reader == null) {
            File serverRoot = VoxelDashSpigot.getInstance().getServer().getWorldContainer().getAbsoluteFile();
            reader = new OfflinePlayerReader(serverRoot);
        }
        return reader;
    }

    @Override
    public InventoryCapabilities getCapabilities() {
        return new InventoryCapabilities(true, true, true, true);
    }

    @Override
    public InventoryView getInventory(UUID uuid, boolean online) {
        if (online) {
            return readOnline(uuid, false);
        }
        return reader().readInventory(uuid);
    }

    @Override
    public InventoryView getEnderChest(UUID uuid, boolean online) {
        if (online) {
            return readOnline(uuid, true);
        }
        return reader().readEnderChest(uuid);
    }

    private InventoryView readOnline(UUID uuid, boolean enderChest) {
        InventoryView view = new InventoryView(enderChest ? "enderchest" : "inventory", true);
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return;
            }
            if (enderChest) {
                Inventory ender = player.getEnderChest();
                ItemStack[] contents = ender.getContents();
                for (int i = 0; i < contents.length; i++) {
                    addItem(view, contents[i], i);
                }
                return;
            }

            PlayerInventory inv = player.getInventory();
            ItemStack[] storage;
            try {
                storage = inv.getStorageContents();
            } catch (Throwable t) {
                storage = inv.getContents();
            }
            for (int i = 0; i < storage.length && i < 36; i++) {
                addItem(view, storage[i], i);
            }

            ItemStack[] armor = inv.getArmorContents();
            for (int i = 0; i < armor.length && i < ARMOR_SLOTS.length; i++) {
                addItem(view, armor[i], ARMOR_SLOTS[i]);
            }

            if (VersionCompat.hasOffhand()) {
                try {
                    addItem(view, inv.getItemInOffHand(), OFFHAND_SLOT);
                } catch (Throwable ignored) {
                }
            }
        });
        return view;
    }

    private void addItem(InventoryView view, ItemStack stack, int slot) {
        InventoryItem item = ItemCompat.toInventoryItem(stack, slot);
        if (item != null) {
            view.items.add(item);
        }
    }

    @Override
    public void setSlot(UUID uuid, boolean enderChest, int slot, InventoryItem item) {
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return;
            }
            ItemStack stack = ItemCompat.toItemStack(item); // null clears the slot
            if (enderChest) {
                Inventory ender = player.getEnderChest();
                if (slot >= 0 && slot < ender.getSize()) {
                    ender.setItem(slot, stack);
                }
                return;
            }
            applyMainSlot(player.getInventory(), slot, stack);
        });
    }

    @Override
    public void moveSlot(UUID uuid, boolean enderChest, int fromSlot, int toSlot) {
        if (fromSlot == toSlot) {
            return;
        }
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return;
            }
            if (enderChest) {
                Inventory ender = player.getEnderChest();
                if (fromSlot < 0 || fromSlot >= ender.getSize() || toSlot < 0 || toSlot >= ender.getSize()) {
                    return;
                }
                ItemStack from = clone(ender.getItem(fromSlot));
                ItemStack to = clone(ender.getItem(toSlot));
                ender.setItem(toSlot, from);
                ender.setItem(fromSlot, to);
            } else {
                PlayerInventory inv = player.getInventory();
                ItemStack from = clone(readMainSlot(inv, fromSlot));
                ItemStack to = clone(readMainSlot(inv, toSlot));
                applyMainSlot(inv, toSlot, from);
                applyMainSlot(inv, fromSlot, to);
            }
        });
    }

    private static ItemStack clone(ItemStack stack) {
        return stack == null ? null : stack.clone();
    }

    private ItemStack readMainSlot(PlayerInventory inv, int slot) {
        if (slot >= 0 && slot < 36) {
            return inv.getItem(slot);
        }
        switch (slot) {
            case 100: return inv.getBoots();
            case 101: return inv.getLeggings();
            case 102: return inv.getChestplate();
            case 103: return inv.getHelmet();
            case OFFHAND_SLOT:
                if (VersionCompat.hasOffhand()) {
                    try {
                        return inv.getItemInOffHand();
                    } catch (Throwable ignored) {
                    }
                }
                return null;
            default:
                return null;
        }
    }

    private void applyMainSlot(PlayerInventory inv, int slot, ItemStack stack) {
        if (slot >= 0 && slot < 36) {
            inv.setItem(slot, stack);
            return;
        }
        switch (slot) {
            case 100: inv.setBoots(stack); break;
            case 101: inv.setLeggings(stack); break;
            case 102: inv.setChestplate(stack); break;
            case 103: inv.setHelmet(stack); break;
            case OFFHAND_SLOT:
                if (VersionCompat.hasOffhand()) {
                    try {
                        inv.setItemInOffHand(stack);
                    } catch (Throwable ignored) {
                    }
                }
                break;
            default:
                // unknown slot; ignore
                break;
        }
    }

    @Override
    public void giveItem(UUID uuid, String id, int count) {
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return;
            }
            InventoryItem dto = new InventoryItem(0, id, count);
            ItemStack stack = ItemCompat.toItemStack(dto);
            if (stack != null) {
                player.getInventory().addItem(stack);
            }
        });
    }

    @Override
    public void clear(UUID uuid, boolean enderChest) {
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return;
            }
            if (enderChest) {
                player.getEnderChest().clear();
            } else {
                player.getInventory().clear();
            }
        });
    }
}
