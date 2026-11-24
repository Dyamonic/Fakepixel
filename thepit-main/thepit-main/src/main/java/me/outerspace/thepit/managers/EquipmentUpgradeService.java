package me.outerspace.thepit.managers;

import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class EquipmentUpgradeService {
    private static final String ITEM_TAG = "ThePitItem";

    public EquipmentUpgradeResult apply(Player player, ItemStack purchasedItem) {
        if (player == null || purchasedItem == null || purchasedItem.getType() == Material.AIR) return EquipmentUpgradeResult.INVALID_ITEM;
        String id = SecureItemTag.string(purchasedItem, ITEM_TAG);
        if ("pit_shop_diamond-sword".equals(id)) return applySword(player, purchasedItem);
        if ("pit_shop_diamond-chestplate".equals(id)) return applyChestplate(player, purchasedItem);
        if ("pit_shop_diamond-boots".equals(id)) return applyBoots(player, purchasedItem);
        return EquipmentUpgradeResult.INVALID_ITEM;
    }

    public InventorySnapshot snapshot(Player player) {
        return new InventorySnapshot(player.getInventory());
    }

    private EquipmentUpgradeResult applySword(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        if (containsShopItem(inventory, "pit_shop_diamond-sword")) return EquipmentUpgradeResult.ALREADY_HAS_ITEM;
        int defaultSlot = -1;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (isDefault(current, Material.IRON_SWORD)) {
                defaultSlot = slot;
                break;
            }
        }
        if (defaultSlot >= 0) {
            inventory.setItem(defaultSlot, item.clone());
            return EquipmentUpgradeResult.REPLACED_STANDARD_ITEM;
        }
        int configuredSlot = 0;
        ItemStack current = inventory.getItem(configuredSlot);
        if (current == null || current.getType() == Material.AIR) {
            inventory.setItem(configuredSlot, item.clone());
            return EquipmentUpgradeResult.EQUIPPED;
        }
        int free = inventory.firstEmpty();
        if (free < 0) return EquipmentUpgradeResult.NO_SAFE_INVENTORY_SPACE;
        inventory.setItem(free, current.clone());
        inventory.setItem(configuredSlot, item.clone());
        return EquipmentUpgradeResult.EQUIPPED;
    }

    private EquipmentUpgradeResult applyChestplate(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        ItemStack current = inventory.getChestplate();
        if (isShop(current, "pit_shop_diamond-chestplate")) return EquipmentUpgradeResult.ALREADY_HAS_ITEM;
        if (current == null || current.getType() == Material.AIR || isDefaultArmor(current, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE)) {
            inventory.setChestplate(item.clone());
            return current == null || current.getType() == Material.AIR ? EquipmentUpgradeResult.EQUIPPED : EquipmentUpgradeResult.REPLACED_STANDARD_ITEM;
        }
        int free = inventory.firstEmpty();
        if (free < 0) return EquipmentUpgradeResult.NO_SAFE_INVENTORY_SPACE;
        inventory.setItem(free, item.clone());
        return EquipmentUpgradeResult.PLACED_IN_INVENTORY;
    }

    private EquipmentUpgradeResult applyBoots(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        ItemStack current = inventory.getBoots();
        if (isShop(current, "pit_shop_diamond-boots")) return EquipmentUpgradeResult.ALREADY_HAS_ITEM;
        if (current == null || current.getType() == Material.AIR || isDefaultArmor(current, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS)) {
            inventory.setBoots(item.clone());
            return current == null || current.getType() == Material.AIR ? EquipmentUpgradeResult.EQUIPPED : EquipmentUpgradeResult.REPLACED_STANDARD_ITEM;
        }
        int free = inventory.firstEmpty();
        if (free < 0) return EquipmentUpgradeResult.NO_SAFE_INVENTORY_SPACE;
        inventory.setItem(free, item.clone());
        return EquipmentUpgradeResult.PLACED_IN_INVENTORY;
    }

    private boolean containsShopItem(PlayerInventory inventory, String id) {
        for (ItemStack item : inventory.getContents()) if (isShop(item, id)) return true;
        return isShop(inventory.getHelmet(), id) || isShop(inventory.getChestplate(), id) || isShop(inventory.getLeggings(), id) || isShop(inventory.getBoots(), id);
    }

    private boolean isDefault(ItemStack item, Material material) {
        return item != null && item.getType() == material && !SecureItemTag.string(item, "BuildFFAProtectedId").isEmpty();
    }

    private boolean isDefaultArmor(ItemStack item, Material first, Material second) {
        return item != null && (item.getType() == first || item.getType() == second) && !SecureItemTag.string(item, "BuildFFAProtectedId").isEmpty();
    }

    private boolean isShop(ItemStack item, String id) {
        return item != null && id.equals(SecureItemTag.string(item, ITEM_TAG));
    }

    public static final class InventorySnapshot {
        private final ItemStack[] contents;
        private final ItemStack[] armor;

        private InventorySnapshot(PlayerInventory inventory) {
            contents = cloneItems(inventory.getContents());
            armor = cloneItems(inventory.getArmorContents());
        }

        public void restore(Player player) {
            player.getInventory().setContents(cloneItems(contents));
            player.getInventory().setArmorContents(cloneItems(armor));
            player.updateInventory();
        }

        private static ItemStack[] cloneItems(ItemStack[] source) {
            ItemStack[] result = new ItemStack[source.length];
            for (int index = 0; index < source.length; index++) result[index] = source[index] == null ? null : source[index].clone();
            return result;
        }
    }
}
