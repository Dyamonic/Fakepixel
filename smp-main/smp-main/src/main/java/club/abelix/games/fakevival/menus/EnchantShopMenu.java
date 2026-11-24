package club.abelix.games.fakevival.menus;

import club.abelix.games.fakevival.managers.CustomEnchantManager;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.managers.EconomyManager;
import club.abelix.games.fakevival.config.RuntimeSettings;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class EnchantShopMenu implements Listener {
    private static final int[] DISPLAY_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
    private final JavaPlugin plugin;
    private final CustomEnchantManager enchants;
    private final CustomItemManager customItems;
    private final EconomyManager economy;
    private final NamespacedKey enchantKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey actionKey;
    private final Map<String, List<CustomEnchantManager.CustomEnchant>> categoryCache = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Integer>> selectedLevels = new HashMap<>();

    public EnchantShopMenu(JavaPlugin plugin, CustomEnchantManager enchants, CustomItemManager customItems, EconomyManager economy) {
        this.plugin = plugin;
        this.enchants = enchants;
        this.customItems = customItems;
        this.economy = economy;
        this.enchantKey = new NamespacedKey(plugin, "enchant_shop_enchant");
        this.categoryKey = new NamespacedKey(plugin, "enchant_shop_category");
        this.actionKey = new NamespacedKey(plugin, "enchant_shop_action");
        cacheCategories();
    }

    public void open(Player player) {
        Inventory menu = Bukkit.createInventory(new MenuHolder("enchant-shop:categories"), 54, CC.color("&8Enchant Shop"));
        menu.setItem(4, item(Material.ENCHANTING_TABLE, "&d&lEnchant Shop", List.of(
                "&7Hold the item you want to enchant", "&7in your main hand, then browse a category.", "",
                "&7Purse: &6" + economy.format(economy.getCoins(player)) + " Coins")));
        category(menu, 10, "weapons", "&cWeapons", Material.DIAMOND_SWORD);
        category(menu, 12, "bows", "&aBows", Material.BOW);
        category(menu, 14, "armor", "&9Armor", Material.DIAMOND_CHESTPLATE);
        category(menu, 16, "tools", "&6Tools", Material.DIAMOND_PICKAXE);
        category(menu, 28, "fishing", "&3Fishing Rods", Material.FISHING_ROD);
        category(menu, 30, "magic", "&dWands/Staffs", Material.BLAZE_ROD);
        category(menu, 32, "utility", "&eUtility", Material.ANVIL);
        category(menu, 34, "special", "&5Special/Custom", Material.ENCHANTED_BOOK);
        menu.setItem(49, action(Material.BARRIER, "&cClose", "close", List.of("&7Close the enchant shop.")));
        fill(menu);
        player.openInventory(menu);
    }

    private void openCategory(Player player, String category, int requestedPage) {
        List<CustomEnchantManager.CustomEnchant> entries = categoryCache.getOrDefault(category, List.of());
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) DISPLAY_SLOTS.length));
        int page = Math.clamp(requestedPage, 0, pages - 1);
        Inventory menu = Bukkit.createInventory(new MenuHolder("enchant-shop:list:" + category + ":" + page), 54, CC.color("&8Enchant Shop"));
        ItemStack held = player.getInventory().getItemInMainHand();
        int start = page * DISPLAY_SLOTS.length;
        for (int i = 0; i < DISPLAY_SLOTS.length && start + i < entries.size(); i++) {
            menu.setItem(DISPLAY_SLOTS[i], preview(player, held, entries.get(start + i), category));
        }
        if (entries.isEmpty()) menu.setItem(22, item(Material.BARRIER, "&cNo Enchants", List.of("&7No registered enchants are in this category.")));
        if (page > 0) menu.setItem(45, action(Material.ARROW, "&ePrevious Page", "previous", List.of("&7Page " + page + " of " + pages)));
        menu.setItem(49, action(Material.ARROW, "&eBack", "back", List.of("&7Return to enchant categories.")));
        if (page + 1 < pages) menu.setItem(53, action(Material.ARROW, "&eNext Page", "next", List.of("&7Page " + (page + 2) + " of " + pages)));
        fill(menu);
        player.openInventory(menu);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder) || !holder.type().startsWith("enchant-shop:")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            if (action.equals("close")) player.closeInventory();
            else if (action.equals("back")) open(player);
            else {
                View view = parse(holder.type());
                if (view != null) openCategory(player, view.category(), view.page() + (action.equals("next") ? 1 : -1));
            }
            return;
        }
        String category = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
        if (category != null) {
            openCategory(player, category, 0);
            return;
        }
        String enchantId = meta.getPersistentDataContainer().get(enchantKey, PersistentDataType.STRING);
        if (enchantId == null) return;
        CustomEnchantManager.CustomEnchant enchant = enchants.get(enchantId);
        View view = parse(holder.type());
        if (enchant == null || view == null) return;
        if (event.isRightClick()) {
            int current = selectedLevel(player, enchant);
            selectedLevels.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                    .put(enchant.id(), current >= enchant.maxLevel() ? 1 : current + 1);
            openCategory(player, view.category(), view.page());
            return;
        }
        if (event.isLeftClick()) apply(player, enchant, view);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder holder && holder.type().startsWith("enchant-shop:")) event.setCancelled(true);
    }

    private void apply(Player player, CustomEnchantManager.CustomEnchant enchant, View view) {
        ItemStack target = player.getInventory().getItemInMainHand();
        if (target == null || target.getType().isAir()) {
            player.sendMessage(CC.color("&cHold the item you want to enchant.")); return;
        }
        int level = selectedLevel(player, enchant);
        if (!enchants.canApply(target, enchant.id())) {
            player.sendMessage(CC.color("&cThat item cannot receive this enchant.")); return;
        }
        String conflict = enchants.conflict(target, enchant.id());
        if (conflict != null) {
            player.sendMessage(CC.color("&cThis enchant conflicts with " + enchants.plainName(conflict) + ".")); return;
        }
        List<String> missing = missingRequirements(player, enchant.id(), level);
        if (!missing.isEmpty()) {
            player.sendMessage(CC.color("&cMissing Requirements:")); missing.forEach(line -> player.sendMessage(CC.color("&c✖ " + line))); return;
        }
        double cost = cost(target, enchant, level);
        if (economy.isEcoFrozen(player) || !economy.takeCoins(player.getUniqueId(), cost, "enchant-shop:" + enchant.id() + ":" + level)) {
            player.sendMessage(CC.color("&cYou do not have enough coins.")); return;
        }
        boolean applied = false;
        if (customItems.getItemId(target) == null) {
            Enchantment vanilla = Enchantment.getByKey(NamespacedKey.minecraft(enchant.id()));
            if (vanilla != null && vanilla.canEnchantItem(target) && level <= vanilla.getMaxLevel()
                    && target.getEnchantments().keySet().stream().noneMatch(vanilla::conflictsWith)) {
                target.addEnchantment(vanilla, level); applied = true;
            }
        }
        if (!applied) {
            CustomEnchantManager.ApplyResult result = enchants.applyEnchant(target, enchant.id(), level);
            if (!result.success()) {
                economy.giveCoins(player.getUniqueId(), cost, "enchant-shop-refund:" + enchant.id());
                player.sendMessage(CC.color(result.message())); return;
            }
            if (customItems.getItemId(target) != null) customItems.rebuildLoreFor(player, target);
        }
        player.getInventory().setItemInMainHand(target);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.1F);
        player.sendMessage(CC.color("&aApplied &e" + enchant.name() + " " + enchants.roman(level)
                + " &ato your held item for &6" + economy.format(cost) + " coins&a."));
        openCategory(player, view.category(), view.page());
    }

    private ItemStack preview(Player player, ItemStack held, CustomEnchantManager.CustomEnchant enchant, String category) {
        int level = selectedLevel(player, enchant);
        double cost = cost(held, enchant, level);
        List<String> lore = new ArrayList<>(List.of(
                "&9" + enchant.name() + " " + enchants.roman(level),
                "&7" + enchants.description(enchant.id()),
                "",
                "&7Applies to: &e" + validTypes(enchant.type(), category),
                "&7Max Level: &e" + enchant.maxLevel(),
                "&7Selected Level: &a" + level,
                "&7Cost: &6" + economy.format(cost) + " Coins", ""));
        if (held == null || held.getType().isAir()) lore.add("&cHold a valid item to enchant.");
        else if (!enchants.canApply(held, enchant.id())) lore.add("&cThis enchant cannot be applied to that item.");
        for (String missing : missingRequirements(player, enchant.id(), level)) lore.add("&c✖ " + missing);
        lore.addAll(List.of("", "&eLeft Click to buy/apply!", "&bRight Click to change level!"));
        ItemStack item = item(enchants.displayMaterial(enchant.id()), enchant.color() + enchant.name(), lore);
        ItemMeta meta = item.getItemMeta(); meta.getPersistentDataContainer().set(enchantKey, PersistentDataType.STRING, enchant.id()); item.setItemMeta(meta);
        return item;
    }

    private double cost(ItemStack item, CustomEnchantManager.CustomEnchant enchant, int level) {
        String rarity = RuntimeSettings.config(plugin).getString("enchant-shop.enchants." + enchant.id() + ".rarity", inferredRarity(enchant));
        double base = switch (rarity.toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> 1_000; case "RARE" -> 2_500; case "EPIC" -> 5_000;
            case "LEGENDARY" -> 10_000; case "MYTHIC" -> 25_000; default -> 500;
        };
        double enchantMultiplier = switch (rarity.toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> 1.15; case "RARE" -> 1.35; case "EPIC" -> 1.65;
            case "LEGENDARY" -> 2.0; case "MYTHIC" -> 2.75; default -> 1.0;
        };
        String itemRarity = item == null ? "COMMON" : customItems.getCustomItemRarity(item);
        double itemMultiplier = switch (itemRarity.toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> 1.1; case "RARE" -> 1.25; case "EPIC" -> 1.5;
            case "LEGENDARY" -> 1.9; case "MYTHIC" -> 2.4; default -> 1.0;
        };
        return Math.ceil(base * level * enchantMultiplier * itemMultiplier);
    }

    private List<String> missingRequirements(Player player, String id, int level) {
        String base = "enchant-shop.enchants." + id + ".requires";
        List<String> missing = new ArrayList<>();
        String permission = RuntimeSettings.config(plugin).getString(base + ".permission", "");
        if (!permission.isBlank() && !player.hasPermission(permission)) missing.add("Permission: " + permission);
        int playerLevel = RuntimeSettings.config(plugin).getInt(base + ".player-level", 0);
        int currentPlayerLevel = plugin instanceof Game game ? game.getPlayerLevelManager().getLevel(player) : player.getLevel();
        if (playerLevel > 0 && currentPlayerLevel < playerLevel) missing.add("PlayerLevel " + playerLevel);
        String skill = RuntimeSettings.config(plugin).getString(base + ".skill", "");
        int skillLevel = RuntimeSettings.config(plugin).getInt(base + ".skill-level", 0);
        if (!skill.isBlank() && skillLevel > 0 && plugin instanceof Game game
                && game.getSkillManager().getLevel(player, skill) < skillLevel) {
            missing.add(pretty(skill) + " Level " + skillLevel);
        }
        int minimumLevel = RuntimeSettings.config(plugin).getInt(base + ".selected-level", 0);
        if (minimumLevel > level) missing.add("Selected Enchant Level " + minimumLevel);
        return missing;
    }

    private int selectedLevel(Player player, CustomEnchantManager.CustomEnchant enchant) {
        return Math.clamp(selectedLevels.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .getOrDefault(enchant.id(), 1), 1, enchant.maxLevel());
    }

    private void cacheCategories() {
        for (String category : List.of("weapons","bows","armor","tools","fishing","magic","utility","special")) categoryCache.put(category, new ArrayList<>());
        for (CustomEnchantManager.CustomEnchant enchant : enchants.all()) {
            String category = switch (enchant.type()) {
                case "weapon" -> "weapons"; case "bow" -> "bows"; case "armor" -> "armor";
                case "tool" -> "tools"; case "fishing" -> "fishing"; case "all" -> "utility";
                case "magic" -> "magic"; case "accessory", "utility" -> "utility"; default -> "special";
            };
            categoryCache.get(category).add(enchant);
        }
        categoryCache.values().forEach(list -> list.sort(Comparator.comparing(CustomEnchantManager.CustomEnchant::name)));
        categoryCache.replaceAll((key, value) -> List.copyOf(value));
    }

    private void category(Inventory inventory, int slot, String id, String name, Material material) {
        ItemStack item = item(material, name, List.of("&7Browse " + name.replaceAll("&.", "") + " enchants.", "",
                "&7Available: &e" + categoryCache.getOrDefault(id, List.of()).size(), "&eClick to browse!"));
        ItemMeta meta = item.getItemMeta(); meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, id); item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private ItemStack action(Material material, String name, String action, List<String> lore) {
        ItemStack item = item(material, name, lore); ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action); item.setItemMeta(meta); return item;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name)); meta.setLore(CC.colorList(lore)); meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta); return item;
    }

    private void fill(Inventory inventory) {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) if (inventory.getItem(slot) == null) inventory.setItem(slot, pane);
    }

    private String validTypes(String type, String category) {
        return switch (type) { case "weapon" -> "Swords, Daggers, Katanas, Axes"; case "bow" -> "Bows, Crossbows";
            case "armor" -> "Armor, Shields"; case "tool" -> "Pickaxes, Axes, Hoes, Tools";
            case "fishing" -> "Fishing Rods"; case "magic" -> "Wands, Staffs"; case "accessory" -> "Accessories";
            case "utility" -> "Supported utility equipment"; default -> "Most equipment"; };
    }

    private String inferredRarity(CustomEnchantManager.CustomEnchant enchant) {
        if (!enchant.color().equals("&9")) return enchant.maxLevel() <= 1 ? "LEGENDARY" : enchant.maxLevel() <= 3 ? "EPIC" : "RARE";
        return enchant.maxLevel() <= 1 ? "RARE" : "COMMON";
    }

    private String pretty(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).replace('-', '_').split("_");
        List<String> result = new ArrayList<>();
        for (String word : words) if (!word.isBlank()) result.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
        return String.join(" ", result);
    }

    private View parse(String holder) {
        String[] split = holder.split(":");
        if (split.length != 4 || !split[1].equals("list")) return null;
        try { return new View(split[2], Integer.parseInt(split[3])); } catch (NumberFormatException ignored) { return null; }
    }

    private record View(String category, int page) {}
}
