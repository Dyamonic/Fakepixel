package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.JobManager;
import club.abelix.games.fakevival.managers.MenuManager;
import club.abelix.games.fakevival.managers.SkillManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.menus.BankMenu;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.VendorLoreSanitizer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class MenuListener implements Listener {
    private final JavaPlugin plugin;
    private final JobManager jobManager;
    private final SkillManager skillManager;
    private final MenuManager menuManager;
    private final club.abelix.games.fakevival.managers.AuctionHouseManager auctionHouseManager;
    private final club.abelix.games.fakevival.managers.EconomyManager economyManager;
    private final club.abelix.games.fakevival.managers.BazaarManager bazaarManager;
    private final club.abelix.games.fakevival.managers.QuestManager questManager;
    private final club.abelix.games.fakevival.managers.CustomItemManager customItemManager;
    private final club.abelix.games.fakevival.managers.CustomEnchantManager customEnchantManager;
    private final BankMenu bankMenu;
    private final NamespacedKey shopItemKey, shopEntryIdKey;
    private final NamespacedKey bazaarActionKey;
    private final NamespacedKey bazaarItemKey;

    public MenuListener(JavaPlugin plugin, JobManager jobManager, SkillManager skillManager, MenuManager menuManager, club.abelix.games.fakevival.managers.AuctionHouseManager auctionHouseManager, club.abelix.games.fakevival.managers.EconomyManager economyManager, club.abelix.games.fakevival.managers.BazaarManager bazaarManager, club.abelix.games.fakevival.managers.QuestManager questManager, club.abelix.games.fakevival.managers.CustomItemManager customItemManager, club.abelix.games.fakevival.managers.CustomEnchantManager customEnchantManager, BankMenu bankMenu) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.skillManager = skillManager;
        this.menuManager = menuManager;
        this.auctionHouseManager = auctionHouseManager;
        this.economyManager = economyManager;
        this.bazaarManager = bazaarManager;
        this.questManager = questManager;
        this.customItemManager = customItemManager;
        this.customEnchantManager = customEnchantManager;
        this.bankMenu = bankMenu;
        this.shopItemKey = new NamespacedKey(plugin, "shop_item_id");
        this.shopEntryIdKey = new NamespacedKey(plugin, "shop_entry_id");
        this.bazaarActionKey = new NamespacedKey(plugin, "bazaar_menu_action");
        this.bazaarItemKey = new NamespacedKey(plugin, "bazaar_item_id");
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (type.equalsIgnoreCase("bazaar") || type.startsWith("bazaar-category:") || type.startsWith("bazaar-item:")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (type.startsWith("trade:")
                || type.startsWith("anvil-enchant:")
                || type.startsWith("storage-page:")
                || type.startsWith("backpack:")
                || type.startsWith("fishingbag:")
                || type.startsWith("fishingbag-bait:")
                || type.startsWith("fishingbag-drops:")
                || type.equalsIgnoreCase("sellall")
                || type.equalsIgnoreCase("equipment")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if ((type.equalsIgnoreCase("bazaar") || type.startsWith("bazaar-category:") || type.startsWith("bazaar-item:"))
                && (event.getHotbarButton() >= 0 || event.isShiftClick())) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase();

        if (type.startsWith("shop:")) {
            boolean sellInventory = event.isShiftClick() && event.isRightClick();
            boolean buy64 = event.isShiftClick() && event.isLeftClick();
            boolean buy8 = !event.isShiftClick() && event.isRightClick();
            handleShop(player, type.substring("shop:".length()), clicked, name, sellInventory, buy64, buy8);
            return;
        }

        if (type.equalsIgnoreCase("main-menu")) {
            handleMainMenu(player, clicked, name, event.isRightClick());
            return;
        }

        if (type.startsWith("discovered-zones:")) {
            int page = parseTrailingInt(type, 0);
            if (clicked.getType() == Material.BARRIER) menuManager.openMainMenu(player);
            else if (clicked.getType() == Material.ARROW && name.equals("previous page")) menuManager.openDiscoveredZones(player, Math.max(0, page - 1));
            else if (clicked.getType() == Material.ARROW && name.equals("next page")) menuManager.openDiscoveredZones(player, page + 1);
            else {
                String zone = loreValue(clicked, "Discovered Zone: ");
                if (zone != null) menuManager.openZoneDetails(player, zone, page);
            }
            return;
        }

        if (type.startsWith("zone-details:")) {
            String[] parts = type.split(":", 3);
            int page = parts.length >= 3 ? parseInt(parts[2], 0) : 0;
            if (clicked.getType() == Material.BARRIER) {
                menuManager.openDiscoveredZones(player, page);
            } else {
                String zone = loreValue(clicked, "Zone Travel: ");
                if (zone != null && plugin instanceof club.abelix.games.fakevival.Game game) {
                    player.closeInventory();
                    game.getNetworkService().routeZoneWaypoint(player, zone);
                }
            }
            return;
        }

        if (type.startsWith("warps:")) {
            int page = parseTrailingInt(type, 0);
            if (clicked.getType() == Material.BARRIER) menuManager.openMainMenu(player);
            else if (clicked.getType() == Material.ARROW && name.equals("previous page")) menuManager.openWarps(player, Math.max(0, page - 1));
            else if (clicked.getType() == Material.ARROW && name.equals("next page")) menuManager.openWarps(player, page + 1);
            else {
                String action = loreValue(clicked, "Warp Action: ");
                if (action == null) return;
                if (action.equals("hub") || action.equals("spawn")) player.performCommand("warp hub");
                else if (action.equals("overworld")) player.performCommand("warp overworld");
                else if (action.equals("dungeons")) player.performCommand("warp dungeons");
                else if (action.equals("rtp")) player.performCommand("rtp");
                else if (action.startsWith("home:")) player.performCommand("home " + action.substring(5));
                else if (action.startsWith("zone:") && plugin instanceof club.abelix.games.fakevival.Game game) {
                    player.closeInventory();
                    game.getNetworkService().routeZoneWaypoint(player, action.substring(5));
                }
            }
            return;
        }

        if (type.startsWith("event-calendar:")) {
            int page = parseTrailingInt(type, 0);
            if (clicked.getType() == Material.BARRIER) {
                menuManager.openMainMenu(player);
            } else if (clicked.getType() == Material.ARROW && name.equals("previous page")) {
                menuManager.openEventCalendar(player, Math.max(0, page - 1));
            } else if (clicked.getType() == Material.ARROW && name.equals("next page")) {
                menuManager.openEventCalendar(player, page + 1);
            } else {
                String start = loreValue(clicked, "Calendar Start: ");
                if (start != null) {
                    try {
                        menuManager.openEventDetail(player, Long.parseLong(start), page);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return;
        }

        if (type.startsWith("event-shop:")) {
            int page = parseTrailingInt(type, 0);
            if (clicked.getType() == Material.BARRIER) {
                menuManager.openMainMenu(player);
            } else if (clicked.getType() == Material.ARROW && name.equals("previous page")) {
                menuManager.openEventShop(player, Math.max(0, page - 1));
            } else if (clicked.getType() == Material.ARROW && name.equals("next page")) {
                menuManager.openEventShop(player, page + 1);
            } else {
                String reward = loreValue(clicked, "Event Shop Reward: ");
                if (reward != null) {
                    menuManager.purchaseEventShopReward(player, reward);
                    menuManager.openEventShop(player, page);
                }
            }
            return;
        }

        if (type.startsWith("event-detail:")) {
            if (clicked.getType() == Material.BARRIER) {
                String[] parts = type.split(":");
                int page = parts.length >= 3 ? parseInt(parts[2], 0) : 0;
                menuManager.openEventCalendar(player, page);
            }
            return;
        }

        if (type.equalsIgnoreCase("dungeon-help")) {
            if (name.equals("close")) player.closeInventory();
            return;
        }

        if (type.equalsIgnoreCase("dungeons")) {
            if (clicked.getType() == Material.BARRIER) {
                menuManager.openMainMenu(player);
                return;
            }
            String dungeon = loreValue(clicked, "Dungeon: ");
            if (dungeon != null && plugin instanceof club.abelix.games.fakevival.Game game) {
                var access = game.getDungeonManager().getAccessResult(player, dungeon);
                if (!access.exists()) player.sendMessage(club.abelix.games.fakevival.util.CC.color("&cDungeon not found."));
                else if (!access.enabled()) player.sendMessage(club.abelix.games.fakevival.util.CC.color("&cThat dungeon is currently disabled."));
                else if (!access.requirementsMet()) player.sendMessage(club.abelix.games.fakevival.util.CC.color("&cYou do not meet the requirements for this dungeon."));
                else if (game.getNetworkService().identity().serverType() == club.abelix.games.fakevival.network.SMPServerType.DUNGEON) game.getDungeonManager().enterFromMenu(player, dungeon);
                else game.getNetworkService().sendToDungeon(player, club.abelix.games.fakevival.managers.DungeonManager.canonicalDungeonId(dungeon));
            }
            return;
        }

        if (type.equalsIgnoreCase("bazaar")) {
            String action = bazaarAction(clicked);
            if (action == null) return;
            if (action.equals("close")) {
                player.closeInventory();
                return;
            }
            if (action.startsWith("category:")) menuManager.openBazaarCategory(player, action.substring("category:".length()));
            return;
        }

        if (type.startsWith("bazaar-category:")) {
            handleBazaarCategory(player, type, clicked, name, event.isRightClick());
            return;
        }

        if (type.equalsIgnoreCase("adventure-guild")) {
            handleAdventureGuild(player, clicked, name);
            return;
        }

        if (type.startsWith("adventure-guild:")) {
            handleAdventureGuildQuests(player, type, clicked, name);
            return;
        }

        if (type.startsWith("bazaar-item:")) {
            handleBazaarItem(player, type, clicked);
            return;
        }

        if (type.equalsIgnoreCase("auctionhouse") || type.startsWith("auctionhouse:")) {
            handleAuctionHouse(player, type, clicked, name);
            return;
        }

        if (type.equalsIgnoreCase("battlepass") || type.startsWith("battlepass:")) {
            handleBattlePass(player, type, clicked);
            return;
        }

        if (type.equalsIgnoreCase("jobs")) {
            if (name.equals("refresh")) {
                menuManager.openJobs(player);
                return;
            }
            String job = loreValue(clicked, "Job: ");
            if (job != null) handleJobs(player, clicked, job, event.isRightClick());
            return;
        }

        if (type.equalsIgnoreCase("skills")) {
            if (name.equals("refresh")) {
                menuManager.openSkills(player);
                return;
            }
            String skill = loreValue(clicked, "Skill: ");
            if (skill == null) return;
            if (event.isLeftClick()) {
                menuManager.openSkillTree(player, skill);
            } else if (event.isRightClick()) {
                menuManager.openSkillLeveling(player, skill);
            }
            return;
        }

        if (type.startsWith("skill-levels")) {
            if (clicked.getType() == Material.ARROW) menuManager.openSkills(player);
            return;
        }

        if (type.equalsIgnoreCase("mining-tree") || type.startsWith("skill-tree:")) {
            String skill = skillTreeSkill(type);
            int page = skillTreePage(type);
            if (name.equals("back")) {
                menuManager.openSkills(player);
                return;
            }
            if (name.equals("previous page")) {
                menuManager.openSkillTree(player, skill, Math.max(0, page - 1));
                return;
            }
            if (name.equals("next page")) {
                menuManager.openSkillTree(player, skill, page + 1);
                return;
            }
            if (name.equals("refresh")) {
                menuManager.openSkillTree(player, skill, page);
                return;
            }
            String node = loreValue(clicked, "Skill Node: ");
            if (node == null) node = nodeFromName(name);
            if (node == null) node = nodeFromItem(clicked.getType());
            if (node == null) return;
            if (skillManager.upgradeTree(player, skill, node)) {
                player.sendMessage(CC.color("&aYou upgraded &f" + name + "&a."));
                menuManager.openSkillTree(player, skill, page);
            } else {
                player.sendMessage(CC.color("&cYou need a skill point or this upgrade is already maxed."));
            }
        }
    }

    private String skillTreeSkill(String type) {
        if (!type.startsWith("skill-tree:")) return "mining";
        String[] parts = type.split(":");
        return parts.length >= 2 ? parts[1] : "mining";
    }

    private int skillTreePage(String type) {
        if (!type.startsWith("skill-tree:")) return 0;
        String[] parts = type.split(":");
        if (parts.length < 3) return 0;
        return parseInt(parts[2]);
    }


    private void handleShop(Player player, String shopType, ItemStack clicked, String name, boolean sell, boolean bulkBuy, boolean buyEight) {
        String[] shopParts = shopType.split(":", 2);
        String shopId = shopParts[0];
        int page = shopParts.length > 1 ? parseInt(shopParts[1]) : 0;
        if (name.equals("close")) {
            player.closeInventory();
            return;
        }
        if (name.equals("refresh")) {
            menuManager.openShop(player, shopId, page);
            return;
        }
        if (name.equals("previous page")) {
            menuManager.openShop(player, shopId, Math.max(0, page - 1));
            return;
        }
        if (name.equals("next page")) {
            menuManager.openShop(player, shopId, page + 1);
            return;
        }

        String itemId = clicked.getItemMeta().getPersistentDataContainer().get(shopEntryIdKey, PersistentDataType.STRING);
        if (itemId == null) itemId = clicked.getItemMeta().getPersistentDataContainer().get(shopItemKey, PersistentDataType.STRING);
        if (itemId == null) return;
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("shops." + shopId.toLowerCase() + ".items." + itemId);
        if (section == null) return;

        String customItemId = section.getString("custom-item", "");
        String customEnchantId = section.getString("custom-enchant", "");
        if (!meetsShopRequirements(player, section)) return;
        if (!customItemId.isBlank() && !customItemManager.itemExists(customItemId)) {
            player.sendMessage(CC.color("&cInvalid custom item: " + customItemId));
            return;
        }
        Material material;
        try {
            material = !customEnchantId.isBlank()
                    ? Material.ENCHANTED_BOOK
                    : customItemId.isBlank() ? Material.valueOf(section.getString("material", "EMERALD").toUpperCase()) : customItemManager.getMaterial(customItemId);
        } catch (Exception ignored) {
            material = Material.EMERALD;
        }
        if (customItemId.isBlank() && customEnchantId.isBlank() && !canCreateItemStack(material)) {
            player.sendMessage(CC.color("&cThat shop item is not obtainable and cannot be traded."));
            return;
        }
        int amount = Math.max(1, section.getInt("amount", 1));
        double buyPrice = section.getDouble("price", 0.0D);
        if (plugin instanceof club.abelix.games.fakevival.Game game)
            buyPrice = game.getMayorManager().applyMayorModifier(player,
                    club.abelix.games.fakevival.mayor.MayorModifierType.SHOP_PRICE, buyPrice);
        if (buyEight && !sell) { int multiplier = Math.max(1, 8 / amount); amount *= multiplier; buyPrice *= multiplier; }
        if (bulkBuy && !sell && section.getBoolean("bulk-buy-enabled", true)) { int multiplier = Math.max(1, 64 / amount); amount *= multiplier; buyPrice *= multiplier; }
        double sellPrice = section.getDouble("sell-price", Math.max(0.0D, buyPrice * 0.5D));
        if (shopId.equalsIgnoreCase("adventure_guild") && plugin instanceof club.abelix.games.fakevival.Game game) {
            sellPrice *= game.getEventManager().guildSellMultiplier();
        }

        if (economyManager.isEcoFrozen(player)) {
            player.sendMessage(CC.color("&cYour economy is frozen. You cannot use coin shops."));
            return;
        }
        if (sell) {
            if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("shops." + shopId.toLowerCase() + ".sell-enabled", true)) {
                player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("shops." + shopId.toLowerCase() + ".sell-denied-message", "&cThis shop only sells items.")));
                return;
            }
            if (!section.getBoolean("allow-sell", true) || section.getBoolean("quest-only", false)
                    || section.getBoolean("soulbound", false) || itemId.toLowerCase().contains("admin") || itemId.toLowerCase().contains("test")) {
                player.sendMessage(CC.color("&cThat item cannot be sold to this vendor."));
                return;
            }
            if (!customEnchantId.isBlank()) {
                player.sendMessage(CC.color("&cCustom enchant books cannot be sold back to shops."));
                return;
            }
            if (!customItemId.isBlank()) {
                ItemStack heldType = customItemManager.createItem(customItemId, 1);
                String type = customItemManager.getCustomItemType(heldType);
                if (type.equals("FURNITURE") || type.equals("COSMETIC") || type.equals("QUEST")) {
                    player.sendMessage(CC.color("&cThat item type cannot be sold to this vendor."));
                    return;
                }
            }
            SellBatch batch = sellAllMatching(player, material, customItemId, sellPrice / Math.max(1, amount),
                    section.getBoolean("accept-custom-fish", false));
            if (batch.amount() <= 0) {
                player.sendMessage(CC.color("&cYou do not have enough items to sell."));
                return;
            }
            amount = batch.amount(); sellPrice = batch.value();
            economyManager.giveCoins(player.getUniqueId(), sellPrice, "shop sell " + itemId);
            if (shopId.equalsIgnoreCase("adventure_guild") && plugin instanceof club.abelix.games.fakevival.Game game) {
                game.getEventManager().recordGuildSale(player, amount);
            }
            if (customItemId.isBlank())
                player.sendMessage(CC.color("&aSold &f" + amount + "x " + pretty(material.name()) + " &afor &e" + economyManager.format(sellPrice) + " &acoins."));
            else
                player.sendMessage(CC.color("&aSold &f" + amount + "x " + pretty(customItemId) + " &afor &e" + economyManager.format(sellPrice) + " &acoins."));
            return;
        }

        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("shops." + shopId.toLowerCase() + ".buy-enabled", true)
                || !section.getBoolean("allow-buy", true)) {
            player.sendMessage(CC.color("&cThat item is not available for purchase."));
            return;
        }

        ItemStack purchase = !customEnchantId.isBlank()
                ? customEnchantManager.createBook(customEnchantId, Math.max(1, section.getInt("level", 1)))
                : customItemId.isBlank() ? new ItemStack(material, amount) : customItemManager.createItem(customItemId, amount);
        boolean sanitizedPurchase = VendorLoreSanitizer.sanitizeItem(
                purchase, VendorLoreSanitizer.Context.PURCHASED_ITEM);
        if (!customItemId.isBlank() && customItemManager.canRollRandomRarityEffects(purchase)) customItemManager.rerollStats(purchase);
        if (!customItemId.isBlank()) customItemManager.rebuildLoreFor(player, purchase);
        VendorLoreSanitizer.sanitizeItem(purchase, sanitizedPurchase
                ? VendorLoreSanitizer.Context.GENERAL : VendorLoreSanitizer.Context.PURCHASED_ITEM);
        purchase.setAmount(amount);
        if (!canFit(player, purchase)) {
            player.sendMessage(CC.color("&cYou do not have enough inventory space."));
            return;
        }
        if (!economyManager.takeCoins(player.getUniqueId(), buyPrice, "shop buy " + itemId)) {
            player.sendMessage(CC.color("&cYou do not have enough coins."));
            return;
        }
        player.getInventory().addItem(purchase);
        String boughtName = !customEnchantId.isBlank() ? customEnchantId : customItemId.isBlank() ? material.name() : customItemId;
        player.sendMessage(CC.color("&aBought &f" + amount + "x " + pretty(boughtName) + " &afor &e" + economyManager.format(buyPrice) + " &acoins."));
        menuManager.openShop(player, shopId, page);
    }

    private boolean meetsShopRequirements(Player player, ConfigurationSection section) {
        String permission = section.getString("requires.permission", section.getString("permission", ""));
        if (!permission.isBlank() && !player.hasPermission(permission)) {
            player.sendMessage(CC.color("&cYou have not unlocked this vendor item.")); return false;
        }
        String skill = section.getString("requires.skill", "");
        int level = Math.max(0, section.getInt("requires.skill-level", 0));
        if (!skill.isBlank() && level > 0 && skillManager.getLevel(player, skill) < level) {
            player.sendMessage(CC.color("&cRequires " + pretty(skill) + " Level " + level + ".")); return false;
        }
        String quest = section.getString("requires.quest", "");
        if (!quest.isBlank() && plugin instanceof club.abelix.games.fakevival.Game game
                && !game.getDataManager().getData().getBoolean("players." + player.getUniqueId() + ".quests." + quest + ".completed", false)) {
            player.sendMessage(CC.color("&cRequires quest: " + pretty(quest) + ".")); return false;
        }
        return true;
    }

    private boolean canCreateItemStack(Material material) {
        if (material == null || material.isAir()) return false;
        try {
            new ItemStack(material, 1);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private void handleBazaarItem(Player player, String type, ItemStack clicked) {
        String[] parts = type.split(":", 5);
        if (parts.length < 3) return;
        String itemId = parts[1].replace('~', ':');
        String category = parts.length >= 4 ? parts[2] : bazaarManager.getCategory(itemId);
        int page = parts.length >= 4 ? parseInt(parts[3], 0) : 0;
        String action = bazaarAction(clicked);
        if (action == null || action.equals("disabled") || action.equals("loading")) return;
        if (action.equals("back")) {
            menuManager.openBazaarCategory(player, category, page);
            return;
        }
        if (bazaarManager.itemExists(itemId)) {
            player.sendMessage(CC.color("&cThat Bazaar item is no longer available."));
            menuManager.openBazaarCategory(player, category, page);
            return;
        }

        if (action.startsWith("buy:")) {
            int amount = parseInt(action.substring("buy:".length()), 0);
            double cost = bazaarManager.getInstantBuyCost(itemId, amount);
            if (amount <= 0) return;
            if (economyManager.getCoins(player) < cost) {
                player.sendMessage(CC.color("&cYou do not have enough coins."));
                return;
            }
            if (!bazaarManager.hasInventorySpace(player, itemId, amount)) {
                player.sendMessage(CC.color("&cYou need more inventory space."));
                return;
            }
            if (!bazaarManager.instantBuy(player, itemId, amount)) return;
            player.sendMessage(CC.color("&aBought &e" + amount + "x " + bazaarManager.getDisplayName(itemId)
                    + " &afor &6" + economyManager.format(cost) + " coins&a."));
            menuManager.openBazaarItem(player, itemId, category, page);
            return;
        }

        if (action.startsWith("sell:") || action.equals("sell_all")) {
            int available = bazaarManager.countInventory(player, itemId);
            int amount = action.equals("sell_all") ? available : parseInt(action.substring("sell:".length()), 0);
            if (amount <= 0 || available < amount) {
                player.sendMessage(CC.color("&cYou do not have enough of that item."));
                return;
            }
            double value = bazaarManager.getInstantSellValue(itemId, amount);
            if (!bazaarManager.instantSell(player, itemId, amount)) return;
            player.sendMessage(CC.color("&aSold &e" + amount + "x " + bazaarManager.getDisplayName(itemId)
                    + " &afor &6" + economyManager.format(value) + " coins&a."));
            menuManager.openBazaarItem(player, itemId, category, page);
        }
    }

    private void handleBazaarCategory(Player player, String type, ItemStack clicked, String name, boolean rightClick) {
        String[] parts = type.split(":", 3);
        if (parts.length < 3) {
            menuManager.openBazaar(player);
            return;
        }
        String category = parts[1];
        int page = parseInt(parts[2]);

        String action = bazaarAction(clicked);
        if (action == null || action.equals("loading")) return;
        if (action.equals("back")) {
            if (category.startsWith("vanilla_")) menuManager.openBazaarCategory(player, "vanilla"); else menuManager.openBazaar(player);
            return;
        }
        if (action.startsWith("subcategory:") && category.equalsIgnoreCase("vanilla")) {
            menuManager.openBazaarCategory(player, "vanilla_" + action.substring("subcategory:".length()), 0);
            return;
        }
        if (action.equals("previous")) {
            menuManager.openBazaarCategory(player, category, page - 1);
            return;
        }
        if (action.equals("next")) {
            menuManager.openBazaarCategory(player, category, page + 1);
            return;
        }
        if (action.equals("view")) {
            String itemId = clicked.getItemMeta().getPersistentDataContainer().get(bazaarItemKey, PersistentDataType.STRING);
            if (itemId == null) itemId = customItemManager.getItemId(clicked);
            if (itemId != null && itemId.startsWith("vanilla:")) {
                menuManager.openBazaarItem(player, itemId, category, page);
            } else if (itemId != null) menuManager.openBazaarItem(player, itemId, category, page);
        }
    }

    private String bazaarAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(bazaarActionKey, PersistentDataType.STRING);
    }

    private void handleAdventureGuild(Player player, ItemStack clicked, String name) {
        if(name.equals("register as adventurer")){player.performCommand("quests register");return;}
        if(name.equals("rank up")){player.performCommand("quests rankup");return;}
        if (name.equals("close")) {
            player.closeInventory();
            return;
        }
        if (name.equals("monster material shop")) {
            menuManager.openShop(player, "adventure_guild");
            return;
        }
        if (name.equals("daily contracts")) {
            menuManager.openAdventureGuildQuests(player, "daily", 0);
            return;
        }
        if (name.equals("weekly contracts")) {
            menuManager.openAdventureGuildQuests(player, "weekly", 0);
            return;
        }
        String value = loreValue(clicked, "Adventure Quest: ");
        if (value == null) return;
        String[] parts = value.split(":", 2);
        if (parts.length != 2) return;
        String period = parts[0];
        String questId = parts[1];
        handleAdventureQuestClick(player, period, questId, () -> menuManager.openAdventureGuild(player));
    }

    private void handleAdventureGuildQuests(Player player, String type, ItemStack clicked, String name) {
        String[] menuParts = type.split(":");
        String period = menuParts.length >= 2 ? menuParts[1] : "daily";
        int page = 0;
        if (menuParts.length >= 3) {
            try { page = Integer.parseInt(menuParts[2]); } catch (Exception ignored) {}
        }
        final int currentPage = page;

        if (name.equals("close")) {
            player.closeInventory();
            return;
        }
        if (name.equals("back")) {
            menuManager.openAdventureGuild(player);
            return;
        }
        if (name.equals("previous page")) {
            menuManager.openAdventureGuildQuests(player, period, currentPage - 1);
            return;
        }
        if (name.equals("next page")) {
            menuManager.openAdventureGuildQuests(player, period, currentPage + 1);
            return;
        }

        String value = loreValue(clicked, "Adventure Quest: ");
        if (value == null) return;
        String[] parts = value.split(":", 2);
        if (parts.length != 2) return;
        handleAdventureQuestClick(player, parts[0], parts[1], () -> menuManager.openAdventureGuildQuests(player, period, currentPage));
    }

    private void handleAdventureQuestClick(Player player, String period, String questId, Runnable reopen) {
        if (!questManager.isAdventureQuestAccepted(player, period, questId)) {
            questManager.acceptAdventureQuest(player, period, questId);
            if(questManager.isAdventureQuestAccepted(player,period,questId))player.sendMessage(CC.color("&aAccepted &f" + ChatColor.stripColor(CC.color(questManager.getAdventureQuestName(period, questId))) + "&a."));
            reopen.run();
            return;
        }
        if (questManager.isAdventureQuestClaimed(player, period, questId)) {
            player.sendMessage(CC.color("&eYou already claimed this " + period + " quest."));
            return;
        }
        if (questManager.claimAdventureQuest(player, period, questId)) {
            reopen.run();
        } else {
            player.sendMessage(CC.color("&cThat quest is not complete yet."));
        }
    }


    private void handleBattlePass(Player player, String type, ItemStack clicked) {
        if (clicked.getType() == Material.RED_DYE) {
            player.closeInventory();
            return;
        }
        int page = 0;
        if (type.startsWith("battlepass:")) {
            try { page = Integer.parseInt(type.substring("battlepass:".length())); } catch (Exception ignored) {}
        }
        if (clicked.getType() == Material.LIME_DYE) {
            menuManager.openBattlePass(player, page + 1);
        } else if (clicked.getType() == Material.GRAY_DYE) {
            menuManager.openBattlePass(player, Math.max(0, page - 1));
        }
    }

    private void handleMainMenu(Player player, ItemStack clicked, String name, boolean rightClick) {
        if (name.equals("close")) {
            player.closeInventory();
            return;
        }
        String action = loreValue(clicked, "Main Menu Action: ");
        if (action == null) return;
        switch (action.toLowerCase()) {
            case "profile" -> player.performCommand(rightClick ? "equipment" : "playerlevel");
            case "playerlevel" -> player.performCommand("playerlevel");
            case "equipment" -> player.performCommand("equipment");
            case "skills" -> player.performCommand("skills");
            case "jobs" -> player.performCommand("jobs");
            case "quests" -> player.performCommand("quests");
            case "collections" -> player.performCommand("collections");
            case "recipes" -> player.performCommand("recipes");
            case "companions" -> player.performCommand("companions");
            case "storage" -> {
                if (plugin instanceof club.abelix.games.fakevival.Game game) game.getStorageManager().openStorage(player);
                else player.performCommand("enderchest");
            }
            case "bank" -> bankMenu.open(player);
            case "warps" -> menuManager.openWarps(player, 0);
            case "hub" -> player.performCommand("warp hub");
            case "dungeons" -> menuManager.openDungeons(player);
            case "trading" -> player.performCommand("ah");
            case "settings" -> player.performCommand("language");
            case "calendar" -> menuManager.openEventCalendar(player, 0);
            case "event shop" -> menuManager.openEventShop(player, 0);
            case "discovered zones" -> menuManager.openDiscoveredZones(player, 0);
            case "store" -> player.performCommand("store");
            case "bazaar" -> player.performCommand("bazaar");
            case "auctionhouse" -> player.performCommand("ah");
            case "sellall" -> player.performCommand("sellall");
            case "guild" -> player.performCommand("guild");
            case "guildhelp" -> player.performCommand("guild help");
            case "playtime" -> player.performCommand("playtime");
            case "battlepass" -> player.performCommand("battlepass");
            case "cosmetics" -> player.performCommand("cosmetics");
            case "boosters" -> player.performCommand("boosters");
            case "kit" -> player.performCommand("kit");
            case "etable", "enchanttable" -> player.performCommand("etable");
            case "anvil" -> player.performCommand("anvil");
            case "crafting table" -> { player.closeInventory(); player.openWorkbench(null, true); }
            case "goldstore" -> player.performCommand("store");
            case "rtp" -> {
                player.performCommand("rtp");
            }
            case "homes" -> player.performCommand("homes");
            case "spawn" -> player.performCommand("spawn");
            case "pve" -> player.performCommand("pve");
            case "language" -> player.performCommand("language");
            case "refresh" -> menuManager.openMainMenu(player);
            case "close" -> player.closeInventory();
            default -> unavailable(player);
        }
    }

    private int parseTrailingInt(String value, int fallback) {
        int colon = value.lastIndexOf(':');
        return colon < 0 ? fallback : parseInt(value.substring(colon + 1), fallback);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void unavailable(Player player) {
        player.sendMessage(CC.color("&cThis feature is not available yet."));
    }

    private void handleAuctionHouse(Player player, String type, ItemStack clicked, String name) {
        int page = auctionPage(type);
        boolean ownListings = auctionOwnListings(type);
        String category = auctionCategory(type);
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE || clicked.getType() == Material.GOLD_INGOT || clicked.getType() == Material.GOLD_BLOCK) return;
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasLore()) return;
        String selectedCategory = loreValue(clicked, "Auction Category: ");
        if (selectedCategory != null) {
            menuManager.openAuctionHouse(player, 0, ownListings, selectedCategory);
            return;
        }
        String id = null;
        for (String line : Objects.requireNonNull(clicked.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Listing ID: ")) id = clean.substring("Listing ID: ".length()).trim();
        }
        if (id != null) {
            if (auctionHouseManager.getOwner(id) != null && Objects.equals(auctionHouseManager.getOwner(id), player.getUniqueId())) {
                if (auctionHouseManager.reclaim(player, id)) {
                    player.sendMessage(CC.color("&aReclaimed your auction listing."));
                    menuManager.openAuctionHouse(player, page, ownListings, category);
                } else {
                    player.sendMessage(CC.color("&cCould not reclaim this listing. Make sure your inventory has space."));
                }
                return;
            }
            if (auctionHouseManager.buy(player, id)) {
                menuManager.openAuctionHouse(player, page, ownListings, category);
            } else {
                player.sendMessage(CC.color("&cCould not buy this listing. Check your coins and inventory space."));
            }
            return;
        }

        if (name.equals("sell held item")) {
            player.closeInventory();
            player.sendMessage(CC.color("&eHold the item you want to sell and use &f/ah sell <price>&e."));
        } else if (name.equals("your auctions")) {
            menuManager.openAuctionHouse(player, 0, true, category);
        } else if (name.equals("browse all auctions")) {
            menuManager.openAuctionHouse(player, 0, false, category);
        } else if (name.equals("previous page")) {
            menuManager.openAuctionHouse(player, Math.max(0, page - 1), ownListings, category);
        } else if (name.equals("next page")) {
            menuManager.openAuctionHouse(player, page + 1, ownListings, category);
        } else if (name.equals("refresh")) {
            menuManager.openAuctionHouse(player, page, ownListings, category);
        } else if (name.equals("close")) {
            player.closeInventory();
        }
    }

    private int auctionPage(String type) {
        if (!type.startsWith("auctionhouse:")) return 0;
        String[] parts = type.split(":");
        if (parts.length < 2) return 0;
        try {
            return Integer.parseInt(parts[1]);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String auctionCategory(String type) {
        if (!type.startsWith("auctionhouse:")) return "all";
        String[] parts = type.split(":");
        return parts.length >= 4 ? parts[3] : "all";
    }

    private boolean auctionOwnListings(String type) {
        if (!type.startsWith("auctionhouse:")) return false;
        String[] parts = type.split(":");
        return parts.length >= 3 && parts[2].equalsIgnoreCase("mine");
    }

    private int parseInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String loreValue(ItemStack item, String prefix) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith(prefix)) return clean.substring(prefix.length()).trim();
        }
        return null;
    }

    private boolean canFit(Player player, ItemStack item) {
        int remaining = item.getAmount();
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= item.getMaxStackSize();
            } else if (content.isSimilar(item)) {
                remaining -= Math.max(0, content.getMaxStackSize() - content.getAmount());
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    private boolean hasEmptySlot(Player player) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) return true;
        }
        return false;
    }

    private boolean removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material) remaining -= item.getAmount();
            if (remaining <= 0) break;
        }
        if (remaining > 0) return false;

        remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() != material) continue;
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            contents[slot] = item.getAmount() <= 0 ? null : item;
            remaining -= take;
            if (remaining <= 0) break;
        }
        player.getInventory().setStorageContents(contents);
        return true;
    }

    private boolean removeCustomItems(Player player, String customItemId, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (sellableCustomItem(item, customItemId)) remaining -= item.getAmount();
            if (remaining <= 0) break;
        }
        if (remaining > 0) return false;

        remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!sellableCustomItem(item, customItemId)) continue;
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            contents[slot] = item.getAmount() <= 0 ? null : item;
            remaining -= take;
            if (remaining <= 0) break;
        }
        player.getInventory().setStorageContents(contents);
        return true;
    }

    private boolean sellableCustomItem(ItemStack item, String id) {
        if (item == null || !id.equalsIgnoreCase(String.valueOf(customItemManager.getItemId(item)))) return false;
        if (customItemManager.getFlag(item, "soulbound") || customItemManager.getFlag(item, "special_item")
                || customItemManager.getFlag(item, "developer_notebook") || customItemManager.getFlag(item, "preview_item")
                || customItemManager.getFlag(item, "admin_item") || id.equalsIgnoreCase("developer_notebook")) return false;
        String type = customItemManager.getCustomItemType(item);
        return !type.equals("FURNITURE") && !type.equals("QUEST") && !type.contains("ADMIN") && !type.contains("SPECIAL") && !type.contains("PREVIEW");
    }

    private SellBatch sellAllMatching(Player player, Material material, String customId, double unitPrice, boolean acceptAnyCustomFish) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int sold = 0; double value = 0D;
        NamespacedKey fishPriceKey = new NamespacedKey(plugin, "final_sell_price");
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            boolean customFish = item != null && "CUSTOM_FISH".equals(customItemManager.getCustomItemType(item));
            boolean matches = customId.isBlank() ? item != null && item.getType() == material
                    : acceptAnyCustomFish ? customFish && sellableCustomItemIdentity(item) : sellableCustomItem(item, customId);
            if (!matches) continue;
            int count = item.getAmount();
            double each = unitPrice;
            if (!customId.isBlank() && customFish && item.hasItemMeta()) {
                Double fishPrice = item.getItemMeta().getPersistentDataContainer().get(fishPriceKey, PersistentDataType.DOUBLE);
                if (fishPrice != null && Double.isFinite(fishPrice) && fishPrice > 0D) each = fishPrice;
                else {
                    var pdc = item.getItemMeta().getPersistentDataContainer();
                    int length = Math.max(1, pdc.getOrDefault(new NamespacedKey(plugin, "fish_length_cm"), PersistentDataType.INTEGER, 10));
                    String rarity = pdc.getOrDefault(new NamespacedKey(plugin, "fish_rarity"), PersistentDataType.STRING, "COMMON");
                    double rarityRate = switch (rarity.toUpperCase()) { case "UNCOMMON" -> 1.5D; case "RARE" -> 3D;
                        case "EPIC" -> 7D; case "LEGENDARY" -> 15D; case "MYTHIC" -> 30D; default -> 1D; };
                    each = Math.max(unitPrice, length * rarityRate);
                }
            }
            sold += count; value += each * count; contents[slot] = null;
        }
        if (sold > 0) player.getInventory().setStorageContents(contents);
        return new SellBatch(sold, value);
    }

    private record SellBatch(int amount, double value) {}

    private boolean sellableCustomItemIdentity(ItemStack item) {
        String id = customItemManager.getItemId(item);
        return id != null && sellableCustomItem(item, id);
    }

    private String pretty(String text) {
        text = text == null ? "item" : text.toLowerCase().replace('_', ' ');
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void handleJobs(Player player, ItemStack clicked, String job, boolean rightClick) {
        if (jobManager.jobExists(job)) return;

        if (jobManager.hasJob(player, job)) {
            if (rightClick) {
                jobManager.leaveJob(player, job);
                player.sendMessage(CC.color("&aYou left the &f" + job + " &ajob."));
                menuManager.openJobs(player);
            } else {
                player.sendMessage(CC.color("&eYou already joined the &f" + job + " &ejob. Right click to leave."));
            }
        } else {
            jobManager.joinJob(player, job);
            player.sendMessage(CC.color("&aYou joined the &f" + job + " &ajob."));
            menuManager.openJobs(player);
        }
    }

    private String nodeFromName(String name) {
        if (name == null) return null;
        String node = name.toLowerCase().replace(" ", "_").replace("-", "_");
        if (node.equals("back") || node.contains("tree")) return null;
        return node;
    }

    private String nodeFromItem(Material material) {
        return switch (material) {
            case SUGAR -> "speed";
            case EMERALD -> "lucky";
            case DIAMOND -> "double_drops";
            case EXPERIENCE_BOTTLE -> "xp_boost";
            case NETHER_STAR -> "mastery";
            default -> null;
        };
    }
}
