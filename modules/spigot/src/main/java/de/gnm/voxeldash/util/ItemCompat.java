package de.gnm.voxeldash.util;

import de.gnm.voxeldash.api.entities.players.InventoryItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemCompat {

    private ItemCompat() {
    }

    /**
     * Maps a Bukkit item to the normalized DTO, or null for empty/air slots.
     */
    public static InventoryItem toInventoryItem(ItemStack stack, int slot) {
        if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
            return null;
        }
        InventoryItem item = new InventoryItem();
        item.slot = slot;
        item.id = materialId(stack.getType());
        item.count = stack.getAmount();
        try {
            item.maxDamage = stack.getType().getMaxDurability();
        } catch (Throwable ignored) {
        }
        try {
            item.damage = stack.getDurability();
        } catch (Throwable ignored) {
        }
        item.enchanted = !stack.getEnchantments().isEmpty();
        try {
            if (stack.hasItemMeta()) {
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    if (meta.hasDisplayName()) {
                        item.name = meta.getDisplayName();
                    }
                    if (meta.hasEnchants()) {
                        item.enchanted = true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return item;
    }

    /**
     * The namespaced id for a material. The flattened (1.13+) enum names match
     * the registry ids exactly; on legacy servers this yields a legacy-ish name
     * the UI gracefully falls back from.
     */
    public static String materialId(Material material) {
        return "minecraft:" + material.name().toLowerCase();
    }

    /**
     * Builds a Bukkit item from the DTO, or null when the id cannot be resolved.
     */
    public static ItemStack toItemStack(InventoryItem item) {
        if (item == null || item.id == null) {
            return null;
        }
        Material material = resolveMaterial(item.id);
        if (material == null || material == Material.AIR) {
            return null;
        }
        ItemStack stack = new ItemStack(material, Math.max(1, item.count));
        if (item.damage > 0) {
            try {
                stack.setDurability((short) item.damage);
            } catch (Throwable ignored) {
            }
        }
        if (item.name != null && !item.name.isEmpty()) {
            try {
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(item.name);
                    stack.setItemMeta(meta);
                }
            } catch (Throwable ignored) {
            }
        }
        return stack;
    }

    /**
     * Resolves a (possibly namespaced) item id to a Material across versions.
     */
    public static Material resolveMaterial(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String stripped = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        Material m = Material.matchMaterial(id);
        if (m == null) {
            m = Material.matchMaterial(stripped);
        }
        if (m == null) {
            m = Material.matchMaterial(stripped.toUpperCase());
        }
        return m;
    }
}
