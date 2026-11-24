package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.managers.EconomyManager;
import club.abelix.games.fakevival.managers.BazaarManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.mmo.MMOItemValueService;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SellAllCommand implements CommandExecutor, Listener {
    private static final List<String> CUSTOM_SELL_CATEGORIES = List.of(
            "swords", "daggers", "katanas", "tools", "bows", "armor", "shields",
            "food", "rings", "necklaces", "trinkets", "potions", "staffs", "wands",
            "staffs_wands", "fishing", "rare_drops"
    );
    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private final CustomItemManager customItemManager;
    private final BazaarManager bazaarManager;
    private final MMOItemValueService itemValueService;

    public SellAllCommand(JavaPlugin plugin, EconomyManager economyManager, CustomItemManager customItemManager, BazaarManager bazaarManager, MMOItemValueService itemValueService) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.customItemManager = customItemManager;
        this.bazaarManager = bazaarManager;
        this.itemValueService = itemValueService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.sellall")) {
            player.sendMessage(CC.color("&cYou do not have permission to use sell all."));
            return true;
        }
        open(player);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (!type.equalsIgnoreCase("sellall")) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < event.getInventory().getSize() && isDepositSlot(rawSlot)) {
            return;
        }
        if (event.isShiftClick() && rawSlot >= event.getInventory().getSize()) {
            event.setCancelled(true);
            moveToDeposit(event.getInventory(), event.getCurrentItem());
            if (event.getCurrentItem() != null && event.getCurrentItem().getAmount() <= 0) event.setCurrentItem(null);
            return;
        }
        if (rawSlot >= event.getInventory().getSize()) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().hasDisplayName()
                ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT)
                : "";
        if (name.equals("close")) {
            player.closeInventory();
            return;
        }
        if (name.equals("sell deposited items")) {
            sellDeposited(player, event.getInventory());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.equalsIgnoreCase("sellall")) return;
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize() && !isDepositSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.equalsIgnoreCase("sellall")) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        returnDeposited(player, event.getInventory());
    }

    private void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("sellall"), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("menus.sellall.title", "&8Sell All")));
        for (int slot = 45; slot < 54; slot++) inventory.setItem(slot, item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()));
        inventory.setItem(45, item(Material.BOOK, "&bHow SellAll Works", List.of(
                "&7Place items in the empty slots above.",
                "&7Shop/custom prices are used first.",
                "&7Bazaar sell prices are used as fallback.",
                "&7Unsold items are returned when closed."
        )));
        inventory.setItem(49, item(Material.EMERALD, "&aSell Deposited Items", List.of(
                "&7Sells only items placed in this menu.",
                "&7Click after depositing items.",
                "",
                "&eClick to sell."
        )));
        inventory.setItem(51, item(Material.GOLD_INGOT, "&eYour Purse", List.of("&f" + economyManager.format(economyManager.getCoins(player)) + " coins")));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        player.openInventory(inventory);
    }

    private double estimate(Player player, String shopFilter) {
        SellResult result = calculate(player, shopFilter, false);
        return result.coins;
    }

    private void sellDeposited(Player player, Inventory inventory) {
        if (economyManager.isEcoFrozen(player)) {
            player.sendMessage(CC.color("&cYour economy is frozen. You cannot sell items."));
            return;
        }
        SellResult result = calculateDeposited(inventory, true);
        if (result.items <= 0 || result.coins <= 0.0D) {
            player.sendMessage(CC.color("&cNo sellable deposited items were found."));
            return;
        }
        economyManager.giveCoins(player.getUniqueId(), result.coins, "sellall deposited");
        bazaarManager.alertBazaarSell(player, "sell_inventory", result.items, result.coins);
        player.sendMessage(CC.color("&aSold &f" + result.items + " &aitems for &e" + economyManager.format(result.coins) + " &acoins."));
        player.closeInventory();
    }

    private void sell(Player player, String shopFilter) {
        if (economyManager.isEcoFrozen(player)) {
            player.sendMessage(CC.color("&cYour economy is frozen. You cannot sell items."));
            return;
        }
        SellResult result = calculate(player, shopFilter, true);
        if (result.items <= 0 || result.coins <= 0.0D) {
            player.sendMessage(CC.color("&cYou do not have any sellable items."));
            return;
        }
        economyManager.giveCoins(player.getUniqueId(), result.coins, "sellall " + (shopFilter == null ? "all" : shopFilter));
        bazaarManager.alertBazaarSell(player, "sell_inventory", result.items, result.coins);
        player.sendMessage(CC.color("&aSold &f" + result.items + " &aitems for &e" + economyManager.format(result.coins) + " &acoins."));
    }

    private SellResult calculate(Player player, String shopFilter, boolean remove) {
        List<Sellable> sellables = getSellables(shopFilter);
        SellResult result = new SellResult();
        ItemStack[] contents = player.getInventory().getStorageContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType().isAir()) continue;
            Sellable sellable = priceFor(stack, sellables);
            if (sellable == null) continue;

            int amount = stack.getAmount();
            result.items += amount;
            result.coins += amount * sellable.unitPrice;
            if (remove) {
                stack.setAmount(0);
                contents[slot] = null;
            }
        }

        if (remove) player.getInventory().setStorageContents(contents);
        return result;
    }

    private SellResult calculateDeposited(Inventory inventory, boolean remove) {
        List<Sellable> sellables = getSellables("all");
        SellResult result = new SellResult();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!isDepositSlot(slot)) continue;
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;
            Sellable sellable = priceFor(stack, sellables);
            if (sellable == null) continue;
            int amount = stack.getAmount();
            result.items += amount;
            result.coins += amount * sellable.unitPrice;
            if (remove) inventory.setItem(slot, null);
        }
        return result;
    }

    private List<Sellable> getSellables(String shopFilter) {
        Map<String, Sellable> best = new LinkedHashMap<>();
        ConfigurationSection shops = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("shops");
        if (shops == null) return List.of();

        for (String shop : shops.getKeys(false)) {
            if (!shopFilter.equalsIgnoreCase("all") && !shop.equalsIgnoreCase(shopFilter)) continue;
            ConfigurationSection items = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("shops." + shop + ".items");
            if (items == null) continue;
            for (String id : items.getKeys(false)) {
                String path = "shops." + shop + ".items." + id;
                double sellPrice = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path + ".sell-price", -1.0D);
                if (sellPrice <= 0.0D) continue;
                int amount = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".amount", 1));
                double unitPrice = sellPrice / amount;
                String customId = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".custom-item", "");
                Sellable sellable;
                if (customId != null && !customId.isBlank()) {
                    sellable = new Sellable("custom:" + customId.toLowerCase(Locale.ROOT), null, customId, unitPrice);
                } else {
                    Material material = parseMaterial(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".material", "AIR"));
                    if (material == Material.AIR) continue;
                    sellable = new Sellable("material:" + material.name(), material, null, unitPrice);
                }
                Sellable previous = best.get(sellable.key);
                if (previous == null || sellable.unitPrice > previous.unitPrice) best.put(sellable.key, sellable);
            }
        }
        if (shopFilter.equalsIgnoreCase("all") || CUSTOM_SELL_CATEGORIES.stream().anyMatch(category -> category.equalsIgnoreCase(shopFilter))) {
            addCustomCategorySellables(best, shopFilter);
        }
        return new ArrayList<>(best.values());
    }

    private Sellable matchSellable(ItemStack stack, List<Sellable> sellables) {
        String customId = customItemManager == null ? null : customItemManager.getItemId(stack);
        for (Sellable sellable : sellables) {
            if (sellable.customId != null) {
                if (customId != null && sellable.customId.equalsIgnoreCase(customId)) return sellable;
            } else if (customId == null && sellable.material == stack.getType()) {
                return sellable;
            }
        }
        return null;
    }

    private Sellable priceFor(ItemStack stack, List<Sellable> sellables) {
        String customId = customItemManager == null ? null : customItemManager.getItemId(stack);
        if (customItemManager != null && "CUSTOM_FISH".equals(customItemManager.getCustomItemType(stack))) {
            if (!stack.hasItemMeta()) return null;
            var pdc = stack.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(new NamespacedKey(plugin, "caught_at"), PersistentDataType.LONG)
                    || !pdc.has(new NamespacedKey(plugin, "caught_by"), PersistentDataType.STRING)) return null;
            Double finalPrice = pdc.get(new NamespacedKey(plugin, "final_sell_price"), PersistentDataType.DOUBLE);
            if (finalPrice != null && finalPrice > 0.0D)
                return new Sellable("custom-fish:" + normalize(customId), null, customId, finalPrice);
        }
        if (customId != null && !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getBoolean("custom-items.items." + normalize(customId) + ".sellall-allowed", true)) return null;
        Sellable calculated = mmoSellable(stack);
        if (calculated != null) return calculated;
        Sellable configured = matchSellable(stack, sellables);
        if (configured != null) return configured;
        return bazaarSellable(stack);
    }

    private Sellable mmoSellable(ItemStack stack) {
        if (itemValueService == null || stack == null || stack.getType().isAir()) return null;
        String customId = customItemManager == null ? null : customItemManager.getItemId(stack);
        if (customId == null || customId.isBlank()) return null;
        double configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble("custom-items.items." + normalize(customId) + ".sell-price", -1.0D);
        double unitPrice = configured >= 0.0D ? configured : itemValueService.getSellValue(stack);
        if (unitPrice <= 0.0D) return null;
        return new Sellable("mmo-value:" + normalize(customId), null, customId, unitPrice);
    }

    private Sellable bazaarSellable(ItemStack stack) {
        if (bazaarManager == null || stack == null || stack.getType().isAir()) return null;
        if (customItemManager != null && customItemManager.getItemId(stack) != null) return null;
        if (!bazaarManager.isSafeVanillaBazaarItem(stack)) return null;
        Sellable best = null;
        for (String itemId : bazaarManager.getItemIds()) {
            if (!itemId.startsWith("vanilla:")) continue;
            if (bazaarManager.getMaterial(itemId) != stack.getType()) continue;
            double sellPrice = bazaarManager.getSellPrice(itemId);
            if (sellPrice <= 0.0D) continue;
            Sellable candidate = new Sellable("bazaar:" + normalize(itemId), stack.getType(), null, sellPrice);
            if (best == null || candidate.unitPrice > best.unitPrice) best = candidate;
        }
        return best;
    }

    private boolean isDepositSlot(int slot) {
        return slot >= 0 && slot < 45;
    }

    private void returnDeposited(Player player, Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!isDepositSlot(slot)) continue;
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            inventory.setItem(slot, null);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    private void moveToDeposit(Inventory inventory, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        ItemStack moving = item.clone();
        for (int slot = 0; slot < 45 && moving.getAmount() > 0; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType().isAir()) continue;
            if (!current.isSimilar(moving)) continue;
            int room = current.getMaxStackSize() - current.getAmount();
            if (room <= 0) continue;
            int moved = Math.min(room, moving.getAmount());
            current.setAmount(current.getAmount() + moved);
            moving.setAmount(moving.getAmount() - moved);
        }
        for (int slot = 0; slot < 45 && moving.getAmount() > 0; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current != null && !current.getType().isAir()) continue;
            int moved = Math.min(moving.getMaxStackSize(), moving.getAmount());
            ItemStack placed = moving.clone();
            placed.setAmount(moved);
            inventory.setItem(slot, placed);
            moving.setAmount(moving.getAmount() - moved);
        }
        item.setAmount(moving.getAmount());
    }

    private void addCustomCategorySellables(Map<String, Sellable> best, String shopFilter) {
        ConfigurationSection items = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("custom-items.items");
        if (items == null) return;
        Set<String> rareDrops = rareDropIds();
        for (String customId : items.getKeys(false)) {
            String category = normalize(items.getString(customId + ".category", categoryFor(customId)));
            boolean rareDrop = rareDrops.contains(normalize(customId));
            if (!CUSTOM_SELL_CATEGORIES.contains(category) && !rareDrop) continue;
            if (!shopFilter.equalsIgnoreCase("all")
                    && !shopFilter.equalsIgnoreCase(category)
                    && !(rareDrop && shopFilter.equalsIgnoreCase("rare_drops"))) continue;
            double unitPrice = configuredCustomSellPrice(customId);
            if (unitPrice <= 0.0D) continue;
            Sellable sellable = new Sellable("custom:" + normalize(customId), null, customId, unitPrice);
            Sellable previous = best.get(sellable.key);
            if (previous == null || sellable.unitPrice > previous.unitPrice) best.put(sellable.key, sellable);
        }
    }

    private double configuredCustomSellPrice(String customId) {
        ConfigurationSection shops = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("shops");
        if (shops == null) return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("sellall.custom-items.default-sell-price", 25.0D);
        double best = 0.0D;
        for (String shop : shops.getKeys(false)) {
            ConfigurationSection items = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("shops." + shop + ".items");
            if (items == null) continue;
            for (String id : items.getKeys(false)) {
                String path = "shops." + shop + ".items." + id;
                if (!customId.equalsIgnoreCase(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".custom-item", ""))) continue;
                double sellPrice = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path + ".sell-price", -1.0D);
                if (sellPrice <= 0.0D) continue;
                int amount = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".amount", 1));
                best = Math.max(best, sellPrice / amount);
            }
        }
        return best > 0.0D ? best : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("sellall.custom-items.default-sell-price", 25.0D);
    }

    private Set<String> rareDropIds() {
        Set<String> ids = new HashSet<>();
        addRareDropIds(ids, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getMapList("zones.mob-rewards.rare-drops"));
        addRareDropIds(ids, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getMapList("zones.mob-rewards.boss.rare-drops"));
        ConfigurationSection zones = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("zones.list");
        if (zones != null) {
            for (String zone : zones.getKeys(false)) {
                addRareDropIds(ids, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getMapList("zones.list." + zone + ".rare-drops"));
                addRareDropIds(ids, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getMapList("zones.list." + zone + ".boss.rare-drops"));
            }
        }
        return ids;
    }

    private void addRareDropIds(Set<String> ids, List<Map<?, ?>> drops) {
        for (Map<?, ?> drop : drops) {
            Object custom = drop.get("custom-item");
            if (custom == null) custom = drop.get("custom_item");
            if (custom != null && !String.valueOf(custom).isBlank()) ids.add(normalize(String.valueOf(custom)));
        }
    }

    private boolean isSellAllMenuShop(String shop) {
        return CUSTOM_SELL_CATEGORIES.contains(normalize(shop))
                || List.of("general", "blacksmith", "forge", "bakery", "jeweler", "potioner").contains(normalize(shop));
    }

    private boolean hasSellableInventoryValue(Player player, String shop) {
        return estimate(player, shop) > 0.0D;
    }

    private String loreValue(ItemStack item, String prefix) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean != null && clean.startsWith(prefix)) return clean.substring(prefix.length()).trim();
        }
        return null;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material == null ? Material.CHEST : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            meta.setLore(CC.colorList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }

    private Material parseMaterial(String material) {
        try {
            return Material.valueOf(String.valueOf(material).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Material.CHEST;
        }
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Shop";
        String[] parts = text.toLowerCase(Locale.ROOT).replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private String categoryFor(String id) {
        String lower = normalize(id);
        Material material = customItemManager == null ? Material.PAPER : customItemManager.getMaterial(id);
        String type = material.name();
        if (lower.contains("dagger")) return "daggers";
        if (lower.contains("katana")) return "katanas";
        if (lower.contains("staff") || lower.contains("wand") || lower.contains("focus") || lower.contains("scepter")) return "staffs_wands";
        if (lower.contains("fishing") || lower.contains("bait") || lower.contains("tackle") || lower.contains("fish")) return "fishing";
        if (lower.contains("ring")) return "rings";
        if (lower.contains("necklace") || lower.contains("amulet")) return "necklaces";
        if (lower.contains("potion") || type.equals("POTION") || type.equals("SPLASH_POTION")) return "potions";
        if (lower.contains("charm") || lower.contains("totem") || lower.contains("compass") || lower.contains("rune")) return "trinkets";
        if (type.endsWith("_SWORD") || type.equals("TRIDENT") || type.equals("MACE")) return "swords";
        if (type.equals("BOW") || type.equals("CROSSBOW")) return "bows";
        if (type.equals("FISHING_ROD")) return "fishing";
        if (type.endsWith("_PICKAXE") || type.endsWith("_AXE") || type.endsWith("_SHOVEL") || type.endsWith("_HOE")) return "tools";
        if (type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE") || type.endsWith("_LEGGINGS") || type.endsWith("_BOOTS")) return "armor";
        if (type.equals("SHIELD")) return "shields";
        if (type.equals("COOKIE") || type.equals("BREAD") || type.contains("CARROT") || type.contains("APPLE")) return "food";
        return "trinkets";
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private record Sellable(String key, Material material, String customId, double unitPrice) {}

    private static final class SellResult {
        private int items;
        private double coins;
    }
}
