package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.registry.RegistryNode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import me.outerspace.thepit.util.SecureItemTag;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class DefaultLoadoutManager {
    private final ThePitPlugin plugin;
    private final Map<Integer, ItemStack> inventory = new LinkedHashMap<>();
    private boolean armorUnbreakable;

    public DefaultLoadoutManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        inventory.clear();
        RegistryNode root = plugin.registry().settings().getConfigurationSection("default-loadout.inventory");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    if (slot >= 0 && slot < 36) inventory.put(slot, read(root.getConfigurationSection(key), "spawn_slot_" + slot));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        armorUnbreakable = plugin.registry().settings().getBoolean("default-loadout.armor.unbreakable", true);
    }

    public void give(Player player) {
        if (player == null || !player.isOnline()) return;
        PlayerInventory target = player.getInventory();
        for (Map.Entry<Integer, ItemStack> entry : inventory.entrySet()) reconcileProtected(target, entry.getKey(), entry.getValue());
        if (plugin.getRenownShopManager() != null) {
            int tier = plugin.getRenownShopManager().tier(player.getUniqueId(), "armory");
            if (tier > 0) {
                int bonus = tier * plugin.getRenownShopManager().effect("armory", "arrows-per-tier", 8);
                for (Map.Entry<Integer, ItemStack> entry : inventory.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().getType() == Material.ARROW) {
                        ItemStack arrows = target.getItem(entry.getKey());
                        if (sameProtectedId(arrows, entry.getValue())) arrows.setAmount(Math.max(arrows.getAmount(), Math.min(64, entry.getValue().getAmount() + bonus)));
                    }
                }
            }
        }
        int[] eligibleArmor = new int[3];
        int eligibleCount = 0;
        if (canSupplyArmor(target.getChestplate(), "spawn_chestplate")) eligibleArmor[eligibleCount++] = 0;
        if (!SecureItemTag.is(target.getLeggings(), "fresh_aqua_pants") && canSupplyArmor(target.getLeggings(), "spawn_leggings")) eligibleArmor[eligibleCount++] = 1;
        if (canSupplyArmor(target.getBoots(), "spawn_boots")) eligibleArmor[eligibleCount++] = 2;
        int ironSlot = eligibleCount == 0 ? -1 : eligibleArmor[ThreadLocalRandom.current().nextInt(eligibleCount)];
        reconcileArmor(target, 38, starterArmor(ironSlot == 0 ? Material.IRON_CHESTPLATE : Material.CHAINMAIL_CHESTPLATE, "spawn_chestplate"));
        if (!SecureItemTag.is(target.getLeggings(), "fresh_aqua_pants")) reconcileArmor(target, 37,
                starterArmor(ironSlot == 1 ? Material.IRON_LEGGINGS : Material.CHAINMAIL_LEGGINGS, "spawn_leggings"));
        reconcileArmor(target, 36, starterArmor(ironSlot == 2 ? Material.IRON_BOOTS : Material.CHAINMAIL_BOOTS, "spawn_boots"));
        player.updateInventory();
        if (plugin.getPerkManager() != null) plugin.getPerkManager().applySpawnEquipment(player);
        if (plugin.getFishingManager() != null) plugin.getFishingManager().syncAquaLoadout(player);
    }

    private void reconcileProtected(PlayerInventory inventory, int assignedSlot, ItemStack expected) {
        if (expected == null) return;
        String id = SecureItemTag.string(expected, "BuildFFAProtectedId");
        int kept = -1;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!id.isEmpty() && id.equals(SecureItemTag.string(item, "BuildFFAProtectedId"))) {
                if (kept < 0) kept = slot; else inventory.setItem(slot, null);
            }
        }
        ItemStack protectedItem = kept < 0 ? expected.clone() : inventory.getItem(kept).clone();
        if (kept >= 0 && kept != assignedSlot) inventory.setItem(kept, null);
        ItemStack occupying = inventory.getItem(assignedSlot);
        if (isValidUpgradeForExpected(occupying, expected)) return;
        if (occupying != null && occupying.getType() != Material.AIR && !sameProtectedId(occupying, expected)) {
            Map<Integer, ItemStack> overflow = inventory.addItem(occupying.clone());
            if (!overflow.isEmpty()) {
                if (kept >= 0) inventory.setItem(kept, protectedItem);
                return;
            }
        }
        inventory.setItem(assignedSlot, protectedItem);
    }

    private void reconcileArmor(PlayerInventory inventory, int rawSlot, ItemStack expected) {
        if (expected == null) return;
        ItemStack current = rawSlot == 39 ? inventory.getHelmet() : rawSlot == 38 ? inventory.getChestplate() : rawSlot == 37 ? inventory.getLeggings() : inventory.getBoots();
        if (current != null && current.getType() != Material.AIR && !sameProtectedId(current, expected)) return;
        if (rawSlot == 39) inventory.setHelmet(expected.clone());
        else if (rawSlot == 38) inventory.setChestplate(expected.clone());
        else if (rawSlot == 37) inventory.setLeggings(expected.clone());
        else inventory.setBoots(expected.clone());
    }

    public boolean has(Player player) {
        return verify(player).valid();
    }

    public Verification verify(Player player) {
        if (player == null) return new Verification(false, "player is null");
        PlayerInventory target = player.getInventory();
        for (Map.Entry<Integer, ItemStack> entry : inventory.entrySet()) {
            ItemStack current = target.getItem(entry.getKey());
            if (!verifiedInventoryItem(player, current, entry.getValue())) return new Verification(false,
                    "slot " + entry.getKey() + " expected=" + describe(entry.getValue()) + " found=" + describe(current));
            if (current.getAmount() < entry.getValue().getAmount()) return new Verification(false,
                    "slot " + entry.getKey() + " amount expected_at_least=" + entry.getValue().getAmount() + " found=" + current.getAmount());
        }
        boolean safety = plugin.getPerkManager() != null && plugin.getPerkManager().has(player, "safety_first");
        boolean bountyArmor = plugin.getPerkManager() != null && plugin.getPerkManager().has(player, "bounty_hunter")
                && plugin.getPerkManager().value("bounty_hunter").getBoolean("mechanics.require-armor", true);
        boolean marathon = plugin.getPerkManager() != null && plugin.getPerkManager().has(player, "marathon");
        if (safety ? !SecureItemTag.is(target.getHelmet(), "safety_helmet") : present(target.getHelmet()))
            return new Verification(false, "helmet incompatible with safety_first=" + safety + " found=" + describe(target.getHelmet()));
        if (!starterArmor(target.getChestplate(), "spawn_chestplate", Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE))
            return new Verification(false, "chestplate is not verified starter armor found=" + describe(target.getChestplate()));
        boolean aquaLeggings = SecureItemTag.is(target.getLeggings(), "fresh_aqua_pants");
        if (bountyArmor ? !SecureItemTag.is(target.getLeggings(), "bounty_armor")
                : !aquaLeggings && !starterArmor(target.getLeggings(), "spawn_leggings", Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS))
            return new Verification(false, "leggings incompatible with bounty_hunter=" + bountyArmor + " found=" + describe(target.getLeggings()));
        if (marathon ? present(target.getBoots()) : !starterArmor(target.getBoots(), "spawn_boots", Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS))
            return new Verification(false, "boots incompatible with marathon=" + marathon + " found=" + describe(target.getBoots()));
        int iron = ironArmorPieces(target);
        if (!marathon && !bountyArmor && !aquaLeggings && iron != 1)
            return new Verification(false, "starter armor expected exactly one iron piece found=" + iron);
        if ((marathon || bountyArmor || aquaLeggings) && iron > 1)
            return new Verification(false, "perk-adjusted starter armor contains too many iron pieces found=" + iron);
        return new Verification(true, "verified");
    }

    public List<String> equipmentModifiers(Player player) {
        List<String> result = new ArrayList<>();
        if (plugin.getPerkManager() == null || player == null) return result;
        for (String id : new String[]{"safety_first", "bounty_hunter", "barbarian", "marathon"}) {
            if (plugin.getPerkManager().has(player, id)) result.add(id);
        }
        return result;
    }

    private ItemStack starterArmor(Material material, String protectedId) {
        ItemStack item = new ItemStack(material);
        if (armorUnbreakable) makeUnbreakable(item);
        return SecureItemTag.string(item, "BuildFFAProtectedId", protectedId);
    }

    private boolean canSupplyArmor(ItemStack current, String protectedId) {
        return current == null || current.getType() == Material.AIR
                || protectedId.equals(SecureItemTag.string(current, "BuildFFAProtectedId"));
    }

    private boolean starterArmor(ItemStack item, String protectedId, Material chainmail, Material iron) {
        return item != null && (item.getType() == chainmail || item.getType() == iron)
                && protectedId.equals(SecureItemTag.string(item, "BuildFFAProtectedId"));
    }

    private int ironArmorPieces(PlayerInventory inventory) {
        int count = 0;
        if (inventory.getChestplate() != null && inventory.getChestplate().getType() == Material.IRON_CHESTPLATE) count++;
        if (inventory.getLeggings() != null && inventory.getLeggings().getType() == Material.IRON_LEGGINGS) count++;
        if (inventory.getBoots() != null && inventory.getBoots().getType() == Material.IRON_BOOTS) count++;
        return count;
    }

    private ItemStack read(RegistryNode section, String protectedId) {
        if (section == null) return null;
        Material material = Material.matchMaterial(section.getString("material", ""));
        if (material == null || material == Material.AIR) return null;
        int amount = Math.max(1, Math.min(64, section.getInt("amount", 1)));
        short data = (short) section.getInt("data", 0);
        ItemStack item = new ItemStack(material, amount, data);
        if (section.getBoolean("unbreakable", false)) makeUnbreakable(item);
        return SecureItemTag.string(item, "BuildFFAProtectedId", protectedId);
    }

    private void makeUnbreakable(ItemStack item) {
        try {
            ItemMeta meta = item.getItemMeta();
            meta.spigot().setUnbreakable(true);
            item.setItemMeta(meta);
        } catch (Throwable throwable) {
            item.setDurability((short) 0);
        }
    }

    private boolean verifiedInventoryItem(Player player, ItemStack current, ItemStack expected) {
        if (!present(expected)) return !present(current);
        if (!present(current)) return false;
        if (isValidUpgradeForExpected(current, expected)) return true;
        if (!sameProtectedId(current, expected)) return false;
        if (current.getType() == expected.getType()) return true;
        if (plugin.getPerkManager() == null || !plugin.getPerkManager().has(player, "barbarian")) return false;
        return expected.getType() == Material.WOOD_SWORD && current.getType() == Material.WOOD_AXE
                || expected.getType() == Material.STONE_SWORD && current.getType() == Material.STONE_AXE
                || expected.getType() == Material.IRON_SWORD && current.getType() == Material.IRON_AXE
                || expected.getType() == Material.DIAMOND_SWORD && current.getType() == Material.DIAMOND_AXE
                || expected.getType() == Material.GOLD_SWORD && current.getType() == Material.GOLD_AXE;
    }

    private boolean present(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    private String describe(ItemStack item) {
        if (!present(item)) return "empty";
        String protectedId = SecureItemTag.string(item, "BuildFFAProtectedId");
        String pitId = SecureItemTag.string(item, "ThePitItem");
        return item.getType() + "x" + item.getAmount() + " protected=" + (protectedId.isEmpty() ? "none" : protectedId)
                + " pit=" + (pitId.isEmpty() ? "none" : pitId);
    }

    private boolean isValidUpgradeForExpected(ItemStack current, ItemStack expected) {
        if (current == null || expected == null) return false;
        String shopId = SecureItemTag.string(current, "ThePitItem");
        if (expected.getType() == Material.IRON_SWORD) return shopId.equals("pit_shop_diamond-sword");
        if (expected.getType() == Material.CHAINMAIL_CHESTPLATE || expected.getType() == Material.IRON_CHESTPLATE) return shopId.equals("pit_shop_diamond-chestplate");
        if (expected.getType() == Material.CHAINMAIL_LEGGINGS || expected.getType() == Material.IRON_LEGGINGS) return shopId.equals("pit_shop_diamond-leggings");
        if (expected.getType() == Material.CHAINMAIL_BOOTS || expected.getType() == Material.IRON_BOOTS) return shopId.equals("pit_shop_diamond-boots");
        return false;
    }

    public boolean isProtectedSpawnItem(ItemStack item) {
        return item != null && !SecureItemTag.string(item, "BuildFFAProtectedId").isEmpty();
    }

    private boolean sameProtectedId(ItemStack first, ItemStack second) {
        if (first == null || second == null) return false;
        String expected = SecureItemTag.string(second, "BuildFFAProtectedId");
        return !expected.isEmpty() && expected.equals(SecureItemTag.string(first, "BuildFFAProtectedId"));
    }

    public record Verification(boolean valid, String detail) { }

}
