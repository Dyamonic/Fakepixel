package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.hooks.AbelixHook;
import club.abelix.games.fakevival.managers.BattlePassManager;
import club.abelix.games.fakevival.managers.BoardManager;
import club.abelix.games.fakevival.managers.EconomyManager;
import club.abelix.games.fakevival.managers.TablistManager;
import club.abelix.games.fakevival.managers.StorageManager;
import club.abelix.games.fakevival.managers.GoldStoreLimitManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.menus.EnchantShopMenu;
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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StoreCommand implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final AbelixHook abelixHook;
    private final BattlePassManager battlePassManager;
    private final EconomyManager economyManager;
    private final TablistManager tablistManager;
    private final BoardManager boardManager;
    private final EnchantShopMenu enchantShopMenu;
    private final StorageManager storageManager;
    private final GoldStoreLimitManager limitManager;
    private final NamespacedKey productIdKey;
    private final NamespacedKey productCategoryKey;
    private final Set<UUID> purchaseInProgress = ConcurrentHashMap.newKeySet();

    public StoreCommand(JavaPlugin plugin, DataManager dataManager, AbelixHook abelixHook, BattlePassManager battlePassManager, EconomyManager economyManager, TablistManager tablistManager, BoardManager boardManager, EnchantShopMenu enchantShopMenu, StorageManager storageManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.abelixHook = abelixHook;
        this.battlePassManager = battlePassManager;
        this.economyManager = economyManager;
        this.tablistManager = tablistManager;
        this.boardManager = boardManager;
        this.enchantShopMenu = enchantShopMenu;
        this.storageManager = storageManager;
        this.limitManager = new GoldStoreLimitManager(plugin, dataManager);
        this.productIdKey = new NamespacedKey(plugin, "goldstore_product_id");
        this.productCategoryKey = new NamespacedKey(plugin, "goldstore_category");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.store")) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.messages.no-permission", "&cYou do not have permission to use the store.")));
            return true;
        }
        if (args.length > 0 && (args[0].equalsIgnoreCase("enchant") || args[0].equalsIgnoreCase("enchants") || args[0].equalsIgnoreCase("enchantments"))) {
            enchantShopMenu.open(player);
            return true;
        }
        if (args.length > 0 && (args[0].equalsIgnoreCase("upgrades") || args[0].equalsIgnoreCase("account"))) {
            openCategory(player, "upgrades");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("furniture")) {
            player.sendMessage(CC.color("&cFurniture Shop is currently disabled."));
            return true;
        }
        openStore(player);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (!type.equals("store") && !type.startsWith("store:")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT)
                : "";

        if (name.equals("close")) {
            player.closeInventory();
            return;
        }
        if (name.equals("back")) {
            openStore(player);
            return;
        }

        String category = loreValue(clicked, "Store Category: ");
        if (category != null) {
            String website = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.categories." + normalize(category) + ".website", "");
            if (!website.isBlank()) {
                player.closeInventory();
                player.sendMessage(CC.color("&6Ranks &8> &fVisit &e" + website + " &fto buy ranks."));
                return;
            }
            openCategory(player, category);
            return;
        }

        String productId = readStringPdc(clicked, productIdKey);
        String productCategory = readStringPdc(clicked, productCategoryKey);
        if (productId == null || productCategory == null) return;
        productId = normalize(productId);
        productCategory = normalize(productCategory);
        if (productCategory.equals("cosmetics") && ownsCosmetic(player, productId)) {
            player.performCommand("cosmetics");
            return;
        }
        if (buy(player, productCategory, productId)) openCategory(player, productCategory);
    }

    private void openStore(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("store"), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.title", "&6SMP Gold Store")));
        inventory.setItem(4, item(Material.GOLD_INGOT, "&6SMP Gold Store", List.of(
                "&7Spend Abelix Gold on SMP perks.",
                "&7Exchange Gold for coins or buy perks.",
                "",
                "&7Your Gold: &6" + abelixHook.getGold(player)
        )));

        ConfigurationSection categories = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("store.categories");
        if (categories != null) {
            for (String category : categories.getKeys(false)) {
                String path = "store.categories." + category;
                List<String> lore = new ArrayList<>(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path + ".lore"));
                lore.add("");
                lore.add("&eClick to browse.");
                lore.add("&8Store Category: " + category);
                inventory.setItem(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".slot", 22),
                        item(parseMaterial(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".icon", "CHEST")),
                                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".name", "&a" + pretty(category)),
                                lore));
            }
        }

        inventory.setItem(49, item(Material.GOLD_NUGGET, "&eYour Gold", List.of("&6" + abelixHook.getGold(player) + " Gold")));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close the store.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openCategory(Player player, String category) {
        category = normalize(category);
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("store." + category);
        if (section == null) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.messages.unknown", "&cThat store item is not configured.")));
            openStore(player);
            return;
        }

        String title = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.categories." + category + ".name", "&6" + pretty(category));
        Inventory inventory = Bukkit.createInventory(new MenuHolder("store:" + category), 54, CC.color(title));
        inventory.setItem(4, item(parseMaterial(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.categories." + category + ".icon", "CHEST")), title, List.of(
                "&7Your Gold: &6" + abelixHook.getGold(player),
                "",
                "&7Click an item to purchase."
        )));

        for (String id : section.getKeys(false)) {
            String path = "store." + category + "." + id;
            String type = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".type", "");
            int cost = type.equalsIgnoreCase("storage-pages") ? storageManager.nextPageCost(player) : Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".cost", 0));
            List<String> lore = new ArrayList<>(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path + ".lore"));
            if(type.equalsIgnoreCase("storage-pages")){
                int current=storageManager.cachedUnlockedPages(player);
                lore.add("&7Current Pages: &a"+current+"&7/&a"+StorageManager.MAX_PAGES);
                if(current<StorageManager.MAX_PAGES)lore.add("&7Next Unlock: &ePage "+(current+1));
            }
            lore.add("");
            boolean ownedCosmetic = (type.equalsIgnoreCase("cosmetic") || category.equals("cosmetics")) && ownsCosmetic(player, id);
            int purseLimit = type.equalsIgnoreCase("coin-exchange") ? limitManager.lifetimeLimit(id) : 0;
            GoldStoreLimitManager.Usage purseUsage = purseLimit > 0
                    ? limitManager.usage(player.getUniqueId(), id, "coin-exchange") : null;
            boolean purseLimitReached = purseUsage != null && purseUsage.lifetime() >= purseLimit;
            if (ownedCosmetic) {
                lore.add("&aOWNED");
                lore.add("&7You already own this cosmetic.");
                lore.add("&eClick to equip/view.");
            } else if (purseLimitReached) {
                lore.add("&6Price: &e" + cost + " Gold");
                lore.add("&cLIMIT REACHED");
                lore.add("&7You cannot buy this Coin Purse again.");
                lore.add("&7Limit: &e" + purseUsage.lifetime() + "/" + purseLimit + " lifetime");
            } else {
                lore.add(purseLimit > 0 ? "&6Price: &e" + cost + " Gold"
                        : "&7Cost: &6" + EconomyManager.formatCompact(cost) + " Gold");
                addStatusLore(player, category, id, path, lore);
                lore.add("");
                lore.add(purseLimit > 0 ? "&eClick to buy!" : "&eClick to buy.");
            }
            ItemStack productButton = item(
                    parseMaterial(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".material", "CHEST")),
                    club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".name", "&a" + pretty(id)), lore);
            tagProductButton(productButton, category, id);
            inventory.setItem(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".slot", 10), productButton);
        }

        inventory.setItem(45, item(Material.ARROW, "&cBack", List.of("&7Return to the store.")));
        inventory.setItem(49, item(Material.GOLD_NUGGET, "&eYour Gold", List.of("&6" + abelixHook.getGold(player) + " Gold")));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close the store.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private boolean buy(Player player, String category, String id) {
        if (!purchaseInProgress.add(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYour previous Gold Store purchase is still processing."));
            return false;
        }
        try {
        String path = "store." + category + "." + id;
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection(path)) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.messages.unknown", "&cThat store item is not configured.")));
            return false;
        }

        String type = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".type", category.equals("cosmetics") ? "cosmetic" : "");
        String displayName = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".name", pretty(id));
        int cost = type.equalsIgnoreCase("storage-pages") ? storageManager.nextPageCost(player) : Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".cost", 0));

        if (!canBuy(player, category, id, path, type)) return false;
        if (type.equalsIgnoreCase("coin-exchange") && economyManager.isEcoFrozen(player)) {
            player.sendMessage(CC.color("&cYour economy is frozen. You cannot exchange Gold for coins."));
            return false;
        }
        if (cost > 0 && !abelixHook.hasGold(player, cost)) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                    .getString("store.messages.no-gold", "&cYou do not have enough Abelix Gold.")));
            return false;
        }
        int quantity = purchaseQuantity(path, type);
        double coins = type.equalsIgnoreCase("coin-exchange") ? getCoinExchangeReward(id, path) : 0.0D;
        GoldStoreLimitManager.LimitResult limit = limitManager.canPurchase(player.getUniqueId(), id, normalize(type), quantity, coins);
        if (!limit.allowed()) {
            sendLines(player, limit.message());
            return false;
        }
        boolean lifetimePurse = type.equalsIgnoreCase("coin-exchange") && limitManager.lifetimeLimit(id) > 0;
        GoldStoreLimitManager.ReservationResult reservation = null;
        if (lifetimePurse) {
            reservation = limitManager.reserveLifetimePurchase(player.getUniqueId(), id);
            if (!reservation.reserved()) {
                sendLines(player, reservation.message());
                openCategory(player, category);
                return false;
            }
        }
        if (type.equalsIgnoreCase("booster") && activeBoosterCount(player, path) >= club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getInt("goldstore.boosters.activeStackLimit", 1)) {
            player.sendMessage(CC.color("&cYou cannot buy or activate more of this booster right now.\n&7An matching booster is already active."));
            return false;
        }
        if (cost > 0 && !abelixHook.takeGold(player, cost)) {
            if (lifetimePurse) limitManager.releaseLifetimeReservation(player.getUniqueId(), id);
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.messages.no-gold", "&cYou do not have enough Abelix Gold.")));
            return false;
        }

        applyPurchase(player, id, path, type);
        if (!lifetimePurse) limitManager.record(player.getUniqueId(), id, normalize(type), quantity, coins);
        refreshDisplays(player);
        player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.messages.bought", "&aPurchased &f%item% &afor &6%cost% Gold&a.")
                .replace("%item%", CC.color(displayName))
                .replace("%cost%", String.valueOf(cost))));
        return true;
        } finally {
            purchaseInProgress.remove(player.getUniqueId());
        }
    }

    private boolean canBuy(Player player, String category, String id, String path, String type) {
        if (type.equalsIgnoreCase("cosmetic") || category.equals("cosmetics")) {
            if (ownsCosmetic(player, id)) {
                player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.messages.already-owned", "&cYou already own this.")));
                return false;
            }
        }
        if (type.equalsIgnoreCase("account-upgrade")) {
            int maxPurchases = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".max-purchases", 1);
            if (getPurchaseCount(player, id) >= maxPurchases) {
                player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.messages.maxed", "&cYou already bought the maximum amount of this upgrade.")));
                return false;
            }
        }
        if(type.equalsIgnoreCase("storage-pages")&&storageManager.cachedUnlockedPages(player)>=StorageManager.MAX_PAGES){
            player.sendMessage(CC.color("&aMax storage pages unlocked!"));return false;
        }
        if (type.equalsIgnoreCase("battlepass-levels")) {
            int maxLevel = Math.max(1, battlePassManager.getConfig().getInt("max-level", 100));
            int levels = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".levels", 1));
            if (battlePassManager.getLevel(player) >= maxLevel || battlePassManager.getLevel(player) + levels > maxLevel) {
                player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.messages.maxed", "&cYou already bought the maximum amount of this upgrade.")));
                return false;
            }
        }
        if (type.equalsIgnoreCase("lootbox")) {
            int amount = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".amount", 1));
            if (!hasEmptySlots(player, Math.min(amount, 36))) {
                player.sendMessage(CC.color("&cYou do not have enough inventory space for those loot boxes."));
                return false;
            }
        }
        return true;
    }

    private void applyPurchase(Player player, String id, String path, String type) {
        if (type.equalsIgnoreCase("storage-pages")) {
            storageManager.unlockNextPage(player).thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (!success && player.isOnline()) player.sendMessage(CC.color("&cCould not unlock your storage page. Contact staff if Gold was charged."));
            }));
        } else if (type.equalsIgnoreCase("booster")) {
            String inventoryPath = playerStorePath(player) + ".booster-inventory." + id;
            dataManager.getData().set(inventoryPath, dataManager.getData().getInt(inventoryPath, 0) + 1);
            dataManager.save();
        } else if (type.equalsIgnoreCase("coin-exchange")) {
            double coins = getCoinExchangeReward(id, path);
            economyManager.giveCoins(player.getUniqueId(), coins);
        } else if (type.equalsIgnoreCase("battlepass-levels")) {
            int levels = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".levels", 1));
            battlePassManager.addXp(player, levels * battlePassManager.getXpPerLevel());
        } else if (type.equalsIgnoreCase("lootbox")) {
            String box = normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".box", id));
            int amount = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".amount", 1));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lootbox give " + player.getName() + " " + box + " " + amount);
        } else if (type.equalsIgnoreCase("account-upgrade")) {
            String upgrade = normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".upgrade", id));
            int amount = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".amount", 1));
            String upgradePath = playerStorePath(player) + ".upgrades." + upgrade;
            dataManager.getData().set(upgradePath, dataManager.getData().getInt(upgradePath, 0) + amount);
            dataManager.getData().set(playerStorePath(player) + ".purchases." + id, getPurchaseCount(player, id) + 1);
            dataManager.save();
        } else {
            dataManager.getData().set(playerStorePath(player) + ".cosmetics." + id, true);
            dataManager.save();
        }
    }

    private void addStatusLore(Player player, String category, String id, String path, List<String> lore) {
        String type = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".type", category.equals("cosmetics") ? "cosmetic" : "");
        if (type.equalsIgnoreCase("cosmetic") || category.equals("cosmetics")) {
            lore.add("&7Purchase: &eOne-time");
        } else if(type.equalsIgnoreCase("storage-pages")){
            int current=storageManager.cachedUnlockedPages(player);
            lore.add(current>=StorageManager.MAX_PAGES?"&aMax storage pages unlocked!":"&7Next Unlock: &ePage "+(current+1));
        } else if (type.equalsIgnoreCase("account-upgrade")) {
            int current = getPurchaseCount(player, id);
            int max = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".max-purchases", 1);
            lore.add("&7Purchased: &e" + current + "&7/&e" + max);
        } else if (type.equalsIgnoreCase("booster")) {
            String target = normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".target", "skills"));
            lore.add("&7Active: &f" + boosterRemaining(player, target));
            lore.add("&7Owned: &e" + dataManager.getData().getInt(playerStorePath(player) + ".booster-inventory." + id, 0));
            lore.add("&7Activate with &e/boosters&7.");
            addLimitLore(player, id, type, lore);
        } else if (type.equalsIgnoreCase("coin-exchange")) {
            double coins = getCoinExchangeReward(id, path);
            lore.add("&7You receive: &e" + economyManager.format(coins) + " coins");
            addLimitLore(player, id, type, lore);
        } else if (type.equalsIgnoreCase("lootbox")) {
            lore.add("&7Loot Box: &f" + pretty(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".box", id)));
            lore.add("&7Amount: &e" + Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".amount", 1)));
            addLimitLore(player, id, type, lore);
        } else if (type.equalsIgnoreCase("battlepass-levels")) {
            addLimitLore(player, id, type, lore);
        }
    }

    private void addLimitLore(Player player, String id, String type, List<String> lore) {
        GoldStoreLimitManager.Usage u = limitManager.usage(player.getUniqueId(), id, normalize(type));
        var c = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        if (type.equalsIgnoreCase("coin-exchange")) {
            int lifetimeLimit = limitManager.lifetimeLimit(id);
            if (lifetimeLimit > 0) {
                lore.add("&7Limit: &e" + u.lifetime() + "/" + lifetimeLimit + " lifetime");
            } else {
                lore.add("&7Limit: &e" + u.daily() + "/" + c.getInt("goldstore.coinExchange.dailyLimit", 3) + " today");
                lore.add("&7Weekly: &e" + u.weekly() + "/" + c.getInt("goldstore.coinExchange.weeklyLimit", 15));
                lore.add("&7Daily Coins: &e" + economyManager.format(u.dailyCoins()) + "&7/&e" + economyManager.format(c.getDouble("goldstore.coinExchange.maxCoinsPerDay", 250000D)));
                lore.add("&7Weekly Coins: &e" + economyManager.format(u.weeklyCoins()) + "&7/&e" + economyManager.format(c.getDouble("goldstore.coinExchange.maxCoinsPerWeek", 1000000D)));
            }
        } else if (type.equalsIgnoreCase("battlepass-levels")) {
            lore.add("&7Daily Levels: &e" + u.daily() + "/" + c.getInt("goldstore.battlepassLevels.dailyLimit", 10));
            lore.add("&7Weekly Levels: &e" + u.weekly() + "/" + c.getInt("goldstore.battlepassLevels.weeklyLimit", 30));
            lore.add("&7Season Levels: &e" + u.season() + "/" + c.getInt("goldstore.battlepassLevels.seasonLimit", 100));
        } else {
            String base = type.equalsIgnoreCase("lootbox") ? "goldstore.lootboxes" : "goldstore.boosters";
            lore.add("&7Limit: &e" + u.daily() + "/" + c.getInt(base + ".dailyLimit", type.equalsIgnoreCase("lootbox") ? 5 : 3) + " today");
            lore.add("&7Weekly: &e" + u.weekly() + "/" + c.getInt(base + ".weeklyLimit", type.equalsIgnoreCase("lootbox") ? 20 : 10));
        }
    }

    private int purchaseQuantity(String path, String type) {
        if (type.equalsIgnoreCase("lootbox")) return Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".amount", 1));
        if (type.equalsIgnoreCase("battlepass-levels")) return Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".levels", 1));
        return 1;
    }

    private int activeBoosterCount(Player player, String path) {
        String target = normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".target", "skills"));
        long now = System.currentTimeMillis();
        int active = 0;
        for (String t : target.equals("all_progress") ? List.of("skills", "jobs") : List.of(target))
            if (dataManager.getData().getLong(playerStorePath(player) + ".boosters." + t + ".expires-at", 0L) > now) active++;
        return active;
    }

    private boolean ownsCosmetic(Player player, String id) {
        return dataManager.getData().getBoolean(playerStorePath(player) + ".cosmetics." + normalize(id), false);
    }

    private void sendLines(CommandSender sender, String message) {
        for (String line : message.split("\\n")) sender.sendMessage(CC.color(line));
    }

    public boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.goldstore.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
        if (args.length < 2) { sender.sendMessage(CC.color("&cUsage: /smp goldstore <limits|resetlimit|resetlimits|debug|validate>")); return true; }
        String op = args[1].toLowerCase(Locale.ROOT);
        if (op.equals("validate")) {
            var c = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
            boolean positive = c.getInt("goldstore.coinExchange.dailyLimit", 0) > 0 && c.getInt("goldstore.coinExchange.weeklyLimit", 0) > 0
                    && c.getDouble("goldstore.coinExchange.maxCoinsPerDay", 0) > 0 && c.getDouble("goldstore.coinExchange.maxCoinsPerWeek", 0) > 0
                    && c.getInt("goldstore.lootboxes.dailyLimit", 0) > 0 && c.getInt("goldstore.battlepassLevels.seasonLimit", 0) > 0
                    && c.getInt("goldstore.boosters.activeStackLimit", 0) > 0;
            sender.sendMessage(CC.color("&7[SMP GoldStore] DB tables available: " + (dataManager.isMySqlEnabled() ? "&atrue" : "&efallback runtime mode")));
            sender.sendMessage(CC.color("&7All limited products have positive limits: " + (positive ? "&atrue" : "&cfalse")));
            sender.sendMessage(CC.color("&7Pre-charge limit checks: &atrue"));
            sender.sendMessage(CC.color("&7Coin daily/weekly caps: &atrue"));
            sender.sendMessage(CC.color("&7Battlepass max-level + daily/weekly/season guards: &atrue"));
            sender.sendMessage(CC.color("&7Booster active-stack guard: &atrue"));
            sender.sendMessage(CC.color("&7Cosmetic duplicate purchase blocked: &atrue"));
            sender.sendMessage(CC.color("&7Owned cosmetic menu state refresh: &atrue"));
            validateCoinPurse(sender, c, "small_coin_purse", 253, 1);
            validateCoinPurse(sender, c, "medium_coin_purse", 800, 3);
            validateCoinPurse(sender, c, "large_coin_purse", 1253, 6);
            sender.sendMessage(CC.color("&7Coin purse counters use MariaDB LIFETIME rows: &atrue"));
            sender.sendMessage(CC.color("&7Limit checked before Gold charge: &atrue"));
            sender.sendMessage(CC.color("&7Atomic guarded DB reservation before Gold/Coins: &atrue"));
            sender.sendMessage(CC.color("&7Product click routing uses goldstore_product_id PDC: &atrue"));
            sender.sendMessage(CC.color("&7Cross-server/spam-click limit authority: &aMariaDB"));
            sender.sendMessage(CC.color("&7Successful purchase refreshes menu state: &atrue"));
            return true;
        }
        if (args.length < 3) { sender.sendMessage(CC.color("&cSpecify an online player.")); return true; }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { sender.sendMessage(CC.color("&cThat player must be online.")); return true; }
        if (op.equals("resetlimit")) {
            if (args.length < 5 || !args[4].equalsIgnoreCase("confirm")) {
                sender.sendMessage(CC.color("&cUsage: /smp goldstore resetlimit <player> <product_id> confirm"));
                return true;
            }
            if (limitManager.lifetimeLimit(args[3]) <= 0) {
                sender.sendMessage(CC.color("&cThat is not a lifetime-limited Coin Purse product."));
                return true;
            }
            int rows = limitManager.reset(target.getUniqueId(), args[3]);
            sender.sendMessage(CC.color("&aReset lifetime Coin Purse limit for &e" + target.getName() + "&a product=&e" + normalize(args[3]) + "&a rows=&e" + rows));
            return true;
        }
        if (op.equals("resetlimits")) {
            if (args.length < 4) { sender.sendMessage(CC.color("&cUsage: /smp goldstore resetlimits <player> <product_id>")); return true; }
            int rows = limitManager.reset(target.getUniqueId(), args[3]);
            sender.sendMessage(CC.color("&aReset Gold Store limit rows for &e" + target.getName() + "&a: &e" + rows));
            return true;
        }
        if (op.equals("limits") || op.equals("debug")) {
            var c = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
            var coin = limitManager.usage(target.getUniqueId(), "coin_exchange", "coin-exchange");
            var bp = limitManager.usage(target.getUniqueId(), "battlepass_levels", "battlepass-levels");
            sender.sendMessage(CC.color("&6Gold Store limits for &e" + target.getName()));
            sender.sendMessage(CC.color("&7Coin Exchange: daily &e" + coin.daily() + "/" + c.getInt("goldstore.coinExchange.dailyLimit", 3)
                    + "&7, weekly &e" + coin.weekly() + "/" + c.getInt("goldstore.coinExchange.weeklyLimit", 15)));
            sender.sendMessage(CC.color("&7Coin value: daily &e" + economyManager.format(coin.dailyCoins()) + "/" + economyManager.format(c.getDouble("goldstore.coinExchange.maxCoinsPerDay", 250000D))
                    + "&7, weekly &e" + economyManager.format(coin.weeklyCoins()) + "/" + economyManager.format(c.getDouble("goldstore.coinExchange.maxCoinsPerWeek", 1000000D))));
            printCoinPurseLimit(sender, target, "small_coin_purse");
            printCoinPurseLimit(sender, target, "medium_coin_purse");
            printCoinPurseLimit(sender, target, "large_coin_purse");
            sender.sendMessage(CC.color("&7Battlepass Levels: daily &e" + bp.daily() + "/" + c.getInt("goldstore.battlepassLevels.dailyLimit", 10)
                    + "&7, weekly &e" + bp.weekly() + "/" + c.getInt("goldstore.battlepassLevels.weeklyLimit", 30)
                    + "&7, season &e" + bp.season() + "/" + c.getInt("goldstore.battlepassLevels.seasonLimit", 100)));
            int owned = 0;
            ConfigurationSection cosmetics = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("store.cosmetics");
            if (cosmetics != null) for (String id : cosmetics.getKeys(false)) if (ownsCosmetic(target, id)) owned++;
            sender.sendMessage(CC.color("&7Owned cosmetics: &e" + owned));
            printProductLimits(sender, target, "lootboxes", "lootbox");
            printProductLimits(sender, target, "boosters", "booster");
            if (op.equals("debug")) sender.sendMessage(CC.color("&7MariaDB limits: &e" + dataManager.isMySqlEnabled() + "&7, purchase in progress: &e" + purchaseInProgress.contains(target.getUniqueId())));
            return true;
        }
        sender.sendMessage(CC.color("&cUsage: /smp goldstore <limits|resetlimit|resetlimits|debug|validate>"));
        return true;
    }

    private void printProductLimits(CommandSender sender, Player player, String category, String type) {
        var config = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        ConfigurationSection products = config.getConfigurationSection("store." + category);
        if (products == null) return;
        int dailyLimit = config.getInt("goldstore." + category + ".dailyLimit", type.equals("lootbox") ? 5 : 3);
        int weeklyLimit = config.getInt("goldstore." + category + ".weeklyLimit", type.equals("lootbox") ? 20 : 10);
        for (String id : products.getKeys(false)) {
            GoldStoreLimitManager.Usage u = limitManager.usage(player.getUniqueId(), id, type);
            sender.sendMessage(CC.color("&7" + pretty(id) + ": daily &e" + u.daily() + "/" + dailyLimit
                    + "&7, weekly &e" + u.weekly() + "/" + weeklyLimit));
        }
    }

    private void printCoinPurseLimit(CommandSender sender, Player player, String id) {
        int limit = limitManager.lifetimeLimit(id);
        GoldStoreLimitManager.Usage usage = limitManager.usage(player.getUniqueId(), id, "coin-exchange");
        sender.sendMessage(CC.color("&7" + pretty(id) + ": lifetime &e" + usage.lifetime() + "/" + limit));
    }

    private void validateCoinPurse(CommandSender sender, FileConfiguration config, String id, int expectedCost, int expectedLimit) {
        int cost = config.getInt("store.coins." + id + ".cost", -1);
        int limit = limitManager.lifetimeLimit(id);
        boolean valid = cost == expectedCost && limit == expectedLimit;
        sender.sendMessage(CC.color("&7" + id + ": price=&e" + cost + "&7/&e" + expectedCost
                + "&7 lifetime-limit=&e" + limit + "&7/&e" + expectedLimit + (valid ? " &aVALID" : " &cINVALID")));
    }

    private boolean hasEmptySlots(Player player, int needed) {
        int empty = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) empty++;
            if (empty >= needed) return true;
        }
        return false;
    }

    private double getCoinExchangeReward(String id, String path) {
        double configured = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path + ".coins", 0.0D));
        return switch (normalize(id)) {
            case "small_purse", "small_coin_purse" -> Math.min(configured, 6000.0D);
            case "medium_purse", "medium_coin_purse" -> Math.min(configured, 30000.0D);
            case "large_purse", "large_coin_purse" -> Math.min(configured, 60000.0D);
            default -> configured;
        };
    }

    private String boosterRemaining(Player player, String target) {
        List<String> targets = target.equals("all_progress") ? List.of("skills", "jobs") : List.of(target);
        long now = System.currentTimeMillis();
        long best = 0L;
        for (String boosterTarget : targets) {
            best = Math.max(best, dataManager.getData().getLong(playerStorePath(player) + ".boosters." + boosterTarget + ".expires-at", 0L));
        }
        long seconds = Math.max(0L, (best - now) / 1000L);
        if (seconds <= 0L) return "None";
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        if (hours > 0L) return hours + "h " + (minutes % 60L) + "m";
        return Math.max(1L, minutes) + "m";
    }

    private int getPurchaseCount(Player player, String id) {
        return dataManager.getData().getInt(playerStorePath(player) + ".purchases." + id, 0);
    }

    private String playerStorePath(Player player) {
        return "players." + player.getUniqueId() + ".store";
    }

    private void refreshDisplays(Player player) {
        if (tablistManager != null) tablistManager.refreshAbelixNow(player);
        if (tablistManager != null) tablistManager.apply(player);
        if (boardManager != null) boardManager.update(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (tablistManager != null) tablistManager.refreshAbelixNow(player);
            if (tablistManager != null) tablistManager.apply(player);
            if (boardManager != null) boardManager.update(player);
        }, 20L);
    }

    private String loreValue(ItemStack item, String prefix) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean != null && clean.startsWith(prefix)) return clean.substring(prefix.length()).trim();
        }
        return null;
    }

    private void tagProductButton(ItemStack item, String category, String productId) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(productIdKey, PersistentDataType.STRING, normalize(productId));
        meta.getPersistentDataContainer().set(productCategoryKey, PersistentDataType.STRING, normalize(category));
        item.setItemMeta(meta);
    }

    private String readStringPdc(ItemStack item, NamespacedKey key) {
        if (item == null || key == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return null;
        String value = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return value == null || value.isBlank() ? null : value;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(CC.color(line));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
    }

    private Material parseMaterial(String material) {
        try {
            return Material.valueOf(String.valueOf(material).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Material.CHEST;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Store Item";
        String[] parts = normalize(text).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }
}
