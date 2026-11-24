package net.fakepixel.housing.menu;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.group.HouseGroups;
import net.fakepixel.housing.hook.AbelixHook;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import net.fakepixel.housing.jukebox.JukeboxService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import net.fakepixel.housing.skull.SkullPackRegistry;
import net.fakepixel.housing.skull.SkullPackDefinition;
import net.fakepixel.housing.skull.SkullPackHeadDefinition;
import net.fakepixel.housing.theme.ThemeRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class HousingMenu implements Listener {
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final AbelixHook abelix;
    private final Map<UUID, Integer> blockMenuPages = new HashMap<>();
    private final Map<UUID, String> browseChannels = new HashMap<>();
    private final Map<UUID, Integer> browsePages = new HashMap<>();
    private final Map<UUID, String[]> browseRowChannels = new HashMap<>();
    private final Map<UUID, List<UUID>> recentBrowseVisits = new HashMap<>();
    private final Map<UUID, Integer> selectingBrowseRow = new HashMap<>();
    private final Map<UUID, UUID> playerOptionTargets = new HashMap<>();
    private final Map<UUID, VisitingMessageEdit> visitingMessageEdits = new HashMap<>();
    private final Map<UUID, ScoreboardEdit> scoreboardEdits = new HashMap<>();
    private final Map<UUID, Integer> selectedScoreboardLine = new HashMap<>();
    private final Map<UUID, TeleportPadRename> teleportPadRenames = new HashMap<>();
    private final Map<UUID, Integer> themePages = new HashMap<>();
    private final Set<UUID> applyingThemes = new HashSet<>();
    private final Map<String, ThemeBlockCache> themeTemplateCache = new LinkedHashMap<String, ThemeBlockCache>(16, 0.75f, true);
    private final Map<UUID, Integer> visibilityLimits = new HashMap<>();

    private final String mainTitle = ChatColor.DARK_GRAY + "Housing";
    private final String ownerTitle = "Housing Menu";
    private final String browseTitle = "Browse Houses";
    private final String channelsTitle = "Channels";
    private final String listTitle = ChatColor.DARK_GRAY + "Your Houses";
    private final String randomTitle = ChatColor.DARK_GRAY + "Random House";
    private final String leaderboardTitle = ChatColor.DARK_GRAY + "Cookie Leaderboard";
    private final String settingsTitle = "House Settings";
    private final String playersTitle = "Players";
    private final String managePlayerTitle = "Player Options";
    private final String blocksTitle = "Blocks";
    private final String plantsTitle = "Plants";
    private final String pvpTitlePrefix = "PVP (Page ";
    private final String itemsTitlePrefix = "Items (Page ";
    private static final String ITEMS_MENU_PERMISSION_MESSAGE = "&cYou need build permission in this house to use the Items Menu.";
    private final String carpenterTitle = "Carpenter Menu";
    private final String visibilityModeTitle = "Visibility";
    private final String visitingRulesTitle = "Visiting Rules";
    private final String visitingMessagesTitle = "Visiting Messages";
    private final String scoreboardEditorTitle = "Scoreboard Editor";
    private final String scoreboardLinesTitle = "Scoreboard Lines";
    private final String teleportPadsTitle = "Teleport Pads";
    private final String teleportPadTitle = "Teleport Pad";
    private final String teleportPadRelinkTitle = "Relink Teleport Pad";
    private final String jukeboxTitle = "House Jukebox";
    private final String weatherTitle = "Weather Selector";
    private final String biomeSkyTitle = "Biomes & Skies";
    private final String themesTitle = "Themes";
    private final String plotUpgradeTitle = "Plot Size Upgrade";
    private final String pvpDamageTitle = "PvP + Damage Settings";
    private final String pvpLayoutTitle = "Edit PvP Layout";
    private final String carpenterRemoveTitle = "Remove Carpenter?";
    private final String skullPacksTitle = "Skull Packs";
    private final String skullPackTitlePrefix = "Skulls: ";

    public HousingMenu(HousingPlugin plugin, HouseManager houses, AbelixHook abelix) {
        this.plugin = plugin;
        this.houses = houses;
        this.abelix = abelix;
        loadBrowseChannelSelections();
        loadSkullPacks();
        loadSkullPackUnlocks();
        loadThemes();
        loadThemeUnlocks();
    }

    public void open(Player player) {
        if (plugin.getCurrentServer() != null && plugin.getCurrentServer().isInstance()) {
            House current = houses.getHouseByWorld(player.getWorld().getName());
            if (current != null) openOwnerMenu(player);
            else if (plugin.getClusterService() != null) plugin.getClusterService().sendPlayerToLobby(player, "Lobby Housing menu requested on instance", "");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, mainTitle);
        inv.setItem(10, item(legacyMat("DARK_OAK_DOOR_ITEM"), "&aYour House", "&7Open your own housing plot.", "&7Gold: &6" + abelix.getGold(player), "&7Rank: &f" + abelix.getRankName(player)));
        inv.setItem(11, item(net.fakepixel.housing.util.Compat.mat("GRASS_BLOCK"), "&aBrowse Houses", "&7Browse public houses.", "&eClick to open!"));
        inv.setItem(12, item(net.fakepixel.housing.util.Compat.mat("COMPASS"), "&bRandom House", "&7Join a random public house.", "&eClick to enter!"));
        inv.setItem(13, item(net.fakepixel.housing.util.Compat.mat("BOOK"), "&eHouses List", "&7View your housing list.", "&eClick to open!"));
        inv.setItem(14, item(net.fakepixel.housing.util.Compat.mat("COOKIE"), "&6Cookie Leaderboard", "&7Top houses by cookies.", "&eClick to open!"));
        inv.setItem(15, item(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"), "&bHouse Info", "&7Prefix: &r" + plugin.color(abelix.getPrefix(player)) + player.getName(), "&7Rank Priority: &f" + abelix.getRankPriority(player)));
        inv.setItem(16, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cClose", "&7Close this menu."));
        player.openInventory(inv);
    }

    private void openScoreboardEditor(Player player, House house) {
        if (house == null) {
            player.closeInventory();
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, scoreboardEditorTitle);
        inv.setItem(4, item(net.fakepixel.housing.util.Compat.mat("EMPTY_MAP"),
                "&aScoreboard",
                "&7Title: &r" + house.getScoreboardTitle(),
                "&7Enabled: " + enabledText(house.isScoreboardEnabled()),
                "&7Lines: &f" + house.getScoreboardLines().size() + "&7/&f15",
                "&7Update: &f" + house.getScoreboardUpdateTicks() + " ticks",
                "&7Placeholders: " + enabledText(house.isScoreboardPlaceholdersEnabled())));
        inv.setItem(10, toggleSettingItem("&aToggle Scoreboard", house.isScoreboardEnabled(), "Show this house's scoreboard."));
        inv.setItem(11, item(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"), "&aEdit Title", "&7Current: &r" + house.getScoreboardTitle(), "", "&eClick to edit!"));
        inv.setItem(12, item(net.fakepixel.housing.util.Compat.mat("PAPER"), "&aEdit Lines", "&7View and edit all lines.", "", "&eClick to view!"));
        inv.setItem(13, item(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aAdd Line", "&7Add a scoreboard line.", "", "&eClick to add!"));
        inv.setItem(14, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cRemove Line", "&7Removes selected line, or last line.", "", "&eClick to remove!"));
        inv.setItem(15, item(net.fakepixel.housing.util.Compat.mat("HOPPER"), "&aMove Line Up", "&7Moves selected line up.", "", "&eClick to move!"));
        inv.setItem(16, item(net.fakepixel.housing.util.Compat.mat("HOPPER"), "&aMove Line Down", "&7Moves selected line down.", "", "&eClick to move!"));
        inv.setItem(20, item(net.fakepixel.housing.util.Compat.mat("REDSTONE"), "&aUpdate Interval", "&7Current: &f" + house.getScoreboardUpdateTicks() + " ticks", "", "&eClick to edit!"));
        inv.setItem(21, toggleSettingItem("&aPlaceholders", house.isScoreboardPlaceholdersEnabled(), "Replace scoreboard placeholders."));
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("BOOK"), "&ePreview Scoreboard", "&7Refresh your current scoreboard now.", "", "&eClick to preview!"));
        inv.setItem(23, item(net.fakepixel.housing.util.Compat.mat("TNT"), "&cReset to Default", "&7Restore the default scoreboard.", "", "&eClick to reset!"));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to House Settings."));
        player.openInventory(inv);
    }

    private void openScoreboardLines(Player player, House house) {
        if (house == null) {
            player.closeInventory();
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, scoreboardLinesTitle);
        int selected = selectedScoreboardLine.getOrDefault(player.getUniqueId(), -1);
        for (int i = 0; i < house.getScoreboardLines().size() && i < 15; i++) {
            String line = house.getScoreboardLines().get(i);
            inv.setItem(i, item(i == selected ? net.fakepixel.housing.util.Compat.limeDye() : new ItemStack(net.fakepixel.housing.util.Compat.mat("PAPER")),
                    (i == selected ? "&a" : "&e") + "Line " + (i + 1),
                    "&7Current: &r" + (line == null || line.isEmpty() ? "&8<blank>" : line),
                    "",
                    "&eClick to select/edit!"));
        }
        inv.setItem(45, item(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aAdd Line"));
        inv.setItem(46, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cRemove Line"));
        inv.setItem(47, item(net.fakepixel.housing.util.Compat.mat("HOPPER"), "&aMove Line Up"));
        inv.setItem(48, item(net.fakepixel.housing.util.Compat.mat("HOPPER"), "&aMove Line Down"));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to Scoreboard Editor."));
        player.openInventory(inv);
    }

    public void openOwnerMenu(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) house = houses.getHouse(player.getUniqueId());
        if (house == null || (!house.canManage(player.getUniqueId()) && !canEditHouseSettings(player, house))) {
            player.sendMessage(plugin.color("&cOnly the owner/co-owner or groups with settings permission can open the housing menu."));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, ownerTitle);

        // Row 1 - exact visible layout.
        inv.setItem(0, item(net.fakepixel.housing.util.Compat.mat("STICK"), "&dPro Tools", "&7Select an area then use &6/set <block>", "&c&lIt''s NOT double // it's only one /!", "", "&6Alias: &7/selector", "", "&eClick to get the region selector!"));
        inv.setItem(2, item(net.fakepixel.housing.util.Compat.mat("BRICK"), "&aBlocks", "&7Browse blocks you have unlocked.", "", "&eClick to browse!"));
        inv.setItem(3, item(legacyItem("DOUBLE_PLANT", 4), "&aPlants", "&7Browse plants you have unlocked.", "", "&eClick to browse!"));
        inv.setItem(4, item(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aItems", "&7View guests and house players.", "", "", "&eClick to browse!"));
        inv.setItem(5, skull("&aSkull Packs", "&eClick to open!"));
        inv.setItem(6, item(net.fakepixel.housing.util.Compat.mat("RED_BED"), "&aFurniture", "&7Browse furniture you have unlocked", "&7Unlocked: &c0/0 &8(0%)", "", "&cUnavailable!"));

        // Row 2.
        inv.setItem(11, item(net.fakepixel.housing.util.Compat.mat("IRON_CHESTPLATE"), "&aPVP", "&7Browse pvp you have unlocked", "&7Unlocked: &a51/51 &8(100%)", "", "&eClick to browse!"));
        inv.setItem(12, item(legacyItem("DOUBLE_PLANT", 5), "&aWeather Selector", "&7Allows you to select the", "&7weather for your home.", "", "&6Alias: &7/housing weather", "&eClick to select!"));
        inv.setItem(13, item(net.fakepixel.housing.util.Compat.mat("DEAD_BUSH"), "&aBiomes & Skies", "&7Allows you to select", "&7different skies and biomes", "&7for your home.", "", "&6Alias: &7/setbiome <biome>", "&eClick to select!"));
        inv.setItem(14, item(net.fakepixel.housing.util.Compat.mat("PAINTING"), "&aThemes", "&7Alllows you to select", "&7a theme. Themes contain a mix", "&7of different features including", "&7weather, time, items and skys", "", "&eClick to select!"));

        // Row 3.
        inv.setItem(36, item(legacyMat("SPRUCE_DOOR_ITEM"), "&aTravel to someone else's house", "&7Hop into the hot air balloon and", "&7visit other peoples's houses.", "", "&eClick to view houses you can join!"));
        inv.setItem(39, skull("&aRewards", "&7Pick tags for this house.", "", "&eClick to open!"));
        inv.setItem(41, dyeVisibilityMode());
        inv.setItem(44, item(house.isBuildMode() ? net.fakepixel.housing.util.Compat.mat("IRON_PICKAXE") : net.fakepixel.housing.util.Compat.mat("EMERALD"),
                plugin.houseModeName(house),
                "&7Toggle this house between",
                "&7Build Mode and Social Mode.",
                "",
                "&7Current: &f" + ChatColor.stripColor(plugin.houseModeName(house)),
                "&eClick to toggle!"));

        // Row 4.
        inv.setItem(45, item(net.fakepixel.housing.util.Compat.mat("COMPASS"), "&aGame Menu", "&7Choose any Fakepixel lobby to warp", "&7to.", "", "&eClick to open!"));
        inv.setItem(47, skull("&aVisiting Rules", "&7Allows you to select who", "&7can visit your home.", "&7Current: " + ruleColor(house.getVisitingRule()) + house.getVisitingRuleDisplay(), "", "&6Alias: &7/visitingrule <rule>", "&eClick to edit Visiting Rules!"));
        inv.setItem(48, item(net.fakepixel.housing.util.Compat.mat("COMPARATOR"), "&aHouse Settings", "&7Allows you to edit and control", "&7various house settings.", "", "&eClick to edit your settings!"));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("FILLED_MAP"), "&aPermissions and Groups", "&7Allows you to edit and control", "&7groups and their permissions", "", "&eClick to edit permissions!"));
        inv.setItem(50, item(net.fakepixel.housing.util.Compat.mat("WRITABLE_BOOK"), "&aPlayer Listing", "&7List all players currently in your home.", "", "&7Alias: /housing players", "&eClick to view Player Listing"));
        inv.setItem(51, item(legacyMat("CAULDRON_ITEM"), "&aClear inventory", "&7Clicking this clears your inventory.", "&7You can use the &dHousing Menu &7or", "&aCarpenter Menu &7to browse through and", "&7access your list of unlocked blocks", "", "&eClick to clear your inventory"));
        inv.setItem(53, item(net.fakepixel.housing.util.Compat.mat("JUKEBOX"), "&aJukebox", "&7Allows you to select the", "&7music playing in your home", "&7as well as control it.", "", "&aVIP Only!", "", "&eClick to open!"));

        player.openInventory(inv);
    }


    public void openGuestMenu(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) {
            open(player);
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, ownerTitle);
        inv.setItem(11, item(legacyMat("DARK_OAK_DOOR_ITEM"), "&aLeave House", "&7Return to the housing lobby.", "", "&eClick to leave!"));
        inv.setItem(12, item(net.fakepixel.housing.util.Compat.mat("OAK_TRAPDOOR"), "&eVisiting Rules", "&7Current: " + ruleColor(house.getVisitingRule()) + house.getVisitingRuleDisplay(), "&7Only the owner can change this."));
        inv.setItem(13, item(net.fakepixel.housing.util.Compat.mat("COMPASS"), "&bVisit Another House", "&7Browse other public houses.", "", "&eClick to browse!"));
        inv.setItem(14, item(net.fakepixel.housing.util.Compat.mat("ANVIL"), "&7Players", "&7View players in this house.", "", "&eClick to open!"));
        inv.setItem(15, item(net.fakepixel.housing.util.Compat.mat("CHEST"), "&6House Browser", "&7Browse public houses.", "", "&eClick to open!"));
        inv.setItem(22, dyeVisibilityMode());
        player.openInventory(inv);
    }

    private void openVisibilityMode(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, visibilityModeTitle);
        int selected = visibilityLimits.getOrDefault(player.getUniqueId(), 100);
        inv.setItem(10, visibilityLimitItem(0, selected));
        inv.setItem(11, visibilityLimitItem(20, selected));
        inv.setItem(12, visibilityLimitItem(50, selected));
        inv.setItem(13, visibilityLimitItem(100, selected));
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cClose", "&7Close this menu."));
        player.openInventory(inv);
    }

    private void openVisitingRules(Player player, House house) {
        Inventory inv = Bukkit.createInventory(null, 27, visitingRulesTitle);
        String rule = house.getVisitingRule();
        inv.setItem(10, visitingRuleItem("friends", "Friend", "&7Enables your friends to visit.", rule));
        inv.setItem(11, visitingRuleItem("party", "Party", "&7Enables your party to visit.", rule));
        inv.setItem(12, visitingRuleItem("public", "Public", "&7Enables anyone to visit.", rule));
        inv.setItem(13, visitingRuleItem("private", "Private", "&7Blocks anyone from visiting.", rule));
        inv.setItem(14, visitingRuleItem("guild", "Guild", "&7Enables your guild to visit.", rule));
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&aMain Menu", "&7Go back to the Housing menu."));
        player.openInventory(inv);
    }

    public void openBrowse(Player player, int page) {
        openBrowseMain(player);
    }

    private void loadSkullPacks() { }

    private void loadSkullPackUnlocks() { }

    private void saveSkullPackUnlocks() { }

    private void openSkullPacks(Player player) {
        if (!requireItemsMenuBuildPermission(player)) return;
        Inventory inv = Bukkit.createInventory(null, 27, skullPacksTitle);
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16};
        List<SkullPack> packs = skullPacks();
        for (int i = 0; i < packs.size() && i < slots.length; i++) {
            inv.setItem(slots[i], skullPackItem(player, packs.get(i)));
        }
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to Housing Menu."));
        player.openInventory(inv);
    }

    private void openSkullPack(Player player, SkullPack pack) {
        if (!requireItemsMenuBuildPermission(player)) return;
        Inventory inv = Bukkit.createInventory(null, 54, skullPackTitlePrefix + ChatColor.stripColor(plugin.color(pack.displayName)));
        List<SkullEntry> skulls = skullsForPack(pack);
        for (int i = 0; i < skulls.size() && i < 45; i++) {
            inv.setItem(i, skullItem(pack, skulls.get(i)));
        }
        if (skulls.isEmpty()) {
            inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cNo Skulls", "&7This compiled skull pack is empty."));
        }
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to Skull Packs."));
        player.openInventory(inv);
    }

    private ItemStack skullItem(SkullPack pack, SkullEntry skull) {
        ItemStack item = customSkull(skull.displayName, skull.texture);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(plugin.color("&eClick to receive!"));
            lore.add(plugin.color("&8Skull Pack: " + pack.key));
            lore.add(plugin.color("&8Skull: " + skull.id));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack skullPackItem(Player player, SkullPack pack) {
        boolean unlocked = isSkullPackUnlocked(player, pack);
        List<String> lore = new ArrayList<>();
        if (unlocked) {
            lore.add("&aUnlocked");
            if ("FREE".equals(pack.unlockType)) lore.add("&aFree");
            else if ("GOLD".equals(pack.unlockType)) lore.add("&6Cost: " + pack.price + " Gold");
            lore.add("");
            lore.add("&eClick to open!");
        } else {
            lore.add("&cLocked");
            if ("GOLD".equals(pack.unlockType)) {
                lore.add("&6Cost: " + pack.price + " Gold");
                lore.add("&7Gold: &6" + abelix.getGold(player));
                lore.add("");
                lore.add("&eClick to unlock!");
            } else {
                lore.add("&cUnavailable.");
            }
        }
        lore.add("&8Skull Pack: " + pack.key);
        return item(materialFromName(pack.icon), pack.displayName, lore.toArray(new String[0]));
    }

    private List<SkullPack> skullPacks() {
        List<SkullPack> packs = new ArrayList<>();
        for (SkullPackDefinition definition : SkullPackRegistry.all())
            packs.add(new SkullPack(definition.getId(), definition.getDisplayName(), definition.getIcon(), definition.getUnlockType(), definition.getPrice()));
        return packs;
    }

    private List<SkullEntry> skullsForPack(SkullPack pack) {
        List<SkullEntry> skulls = new ArrayList<>();
        SkullPackDefinition definition = SkullPackRegistry.get(pack.key);
        if (definition != null) for (SkullPackHeadDefinition head : definition.getHeads())
            if (!head.getTexture().trim().isEmpty()) skulls.add(new SkullEntry(head.getId(), head.getDisplayName(), head.getTexture()));
        return skulls;
    }

    private SkullPack skullPackFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (!clean.startsWith("Skull Pack: ")) continue;
            String key = clean.substring("Skull Pack: ".length());
            for (SkullPack pack : skullPacks()) {
                if (pack.key.equalsIgnoreCase(key)) return pack;
            }
        }
        return null;
    }

    private boolean isSkullPackUnlocked(Player player, SkullPack pack) {
        if ("FREE".equals(pack.unlockType)) return true;
        if (!"GOLD".equals(pack.unlockType)) return false;
        try { return plugin.getHousingRepository() != null && plugin.getHousingRepository().hasUnlock(player.getUniqueId(), "skull_pack", pack.key); }
        catch (Exception ex) { plugin.getLogger().warning("Could not read skull unlock: " + ex.getMessage()); return false; }
    }

    private void unlockSkullPack(Player player, SkullPack pack) {
        try { if (plugin.getHousingRepository() != null) plugin.getHousingRepository().saveUnlock(player.getUniqueId(), "skull_pack", pack.key); }
        catch (Exception ex) { plugin.getLogger().warning("Could not save skull unlock: " + ex.getMessage()); }
    }

    private void handleSkullPackClick(Player player, SkullPack pack) {
        if (isSkullPackUnlocked(player, pack)) {
            openSkullPack(player, pack);
            return;
        }
        if (!"GOLD".equals(pack.unlockType)) {
            player.sendMessage(plugin.color("&cThat skull pack is locked."));
            openSkullPacks(player);
            return;
        }
        if (plugin.getHousingRepository() == null) { player.sendMessage(plugin.color("&cPersistent housing data is unavailable.")); return; }
        if (!abelix.takeGold(player, pack.price)) {
            player.sendMessage(plugin.color("&cYou do not have enough Gold to unlock that skull pack."));
            openSkullPacks(player);
            return;
        }
        unlockSkullPack(player, pack);
        player.sendMessage(plugin.color("&aUnlocked " + pack.displayName + "&a for &6" + pack.price + " Gold&a."));
        openSkullPack(player, pack);
    }

    private SkullEntry skullFromItem(SkullPack pack, ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (!clean.startsWith("Skull: ")) continue;
            String id = clean.substring("Skull: ".length());
            for (SkullEntry skull : skullsForPack(pack)) {
                if (skull.id.equalsIgnoreCase(id)) return skull;
            }
        }
        return null;
    }

    private void giveSkull(Player player, SkullEntry skull) {
        if (!requireItemsMenuBuildPermission(player)) return;
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(customSkull(skull.displayName, skull.texture));
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        player.sendMessage(plugin.color("&aYou received " + skull.displayName + "&a."));
    }

    private ItemStack customSkull(String displayName, String texture) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(plugin.color(displayName));
        if (texture != null && !texture.trim().isEmpty()) {
            try {
                Object profile = Class.forName("com.mojang.authlib.GameProfile")
                        .getConstructor(UUID.class, String.class)
                        .newInstance(UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8)), null);
                Object property = Class.forName("com.mojang.authlib.properties.Property")
                        .getConstructor(String.class, String.class)
                        .newInstance("textures", texture.trim());
                Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
                Method put = properties.getClass().getMethod("put", Object.class, Object.class);
                put.invoke(properties, "textures", property);
                Field field = meta.getClass().getDeclaredField("profile");
                field.setAccessible(true);
                field.set(meta, profile);
            } catch (ReflectiveOperationException ex) {
                plugin.getLogger().warning("Could not apply skull texture: " + ex.getMessage());
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    private Material materialFromName(String name) {
        Material material = Material.matchMaterial(name == null ? "" : name);
        return material == null ? net.fakepixel.housing.util.Compat.mat(name == null ? "PLAYER_HEAD" : name) : material;
    }

    private static class SkullPack {
        private final String key;
        private final String displayName;
        private final String icon;
        private final String unlockType;
        private final int price;

        private SkullPack(String key, String displayName, String icon, String unlockType, int price) {
            this.key = key;
            this.displayName = displayName;
            this.icon = icon;
            this.unlockType = unlockType == null ? "FREE" : unlockType.trim().toUpperCase(java.util.Locale.ENGLISH);
            this.price = price;
        }
    }

    private static class SkullEntry {
        private final String id;
        private final String displayName;
        private final String texture;

        private SkullEntry(String id, String displayName, String texture) {
            this.id = id;
            this.displayName = displayName;
            this.texture = texture;
        }
    }

    public void openBrowse(Player player, String channel, int page) {
        setBrowseRowChannel(player, 0, channel);
        openBrowseMain(player);
    }

    public void openBrowseMain(Player player) {
        String[] rows = getBrowseRows(player);
        Inventory inv = Bukkit.createInventory(null, 45, browseTitle);

        for (int row = 0; row < 5; row++) {
            String channel = rows[row];
            List<House> channelHouses = housesForChannel(channel, player);
            inv.setItem(row * 9, browseRowSelectorItem(row, channel, channelHouses.size()));

            int base = row * 9 + 1;
            for (int i = 0; i < 8 && i < channelHouses.size(); i++) {
                House house = channelHouses.get(i);
                inv.setItem(base + i, houseItem(house,
                        "&eClick to visit!"));
            }

            if (channelHouses.isEmpty()) {
                inv.setItem(base, item(net.fakepixel.housing.util.Compat.mat("BARRIER"),
                        "&cNo Houses",
                        "&7No public houses are currently",
                        "&7listed in &b" + prettyBrowseChannel(channel) + "&7."));
            }
        }

        player.openInventory(inv);
    }

    public void openChannels(Player player) {
        int row = selectingBrowseRow.getOrDefault(player.getUniqueId(), 0);
        if (row < 0 || row > 4) row = 0;
        String current = getBrowseRows(player)[row];
        Inventory inv = Bukkit.createInventory(null, 54, channelsTitle);
        List<BrowseChannel> channels = browseChannelList();
        for (int i = 0; i < channels.size() && i < 45; i++) {
            BrowseChannel ch = channels.get(i);
            inv.setItem(i, browseChannelItem(ch, ch.key.equals(current)));
        }
        inv.setItem(48, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to Browse Houses."));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&aMain Menu", "&7Go back to the Housing menu."));
        inv.setItem(50, item(net.fakepixel.housing.util.Compat.mat("CHEST"), "&aEditing Row " + (row + 1), "&7Current: &b" + prettyBrowseChannel(current), "", "&eChoose a channel above."));
        player.openInventory(inv);
    }

    public void openBrowseResults(Player player, String channel, int page) {
        setBrowseRowChannel(player, 0, channel);
        openBrowseMain(player);
    }

    private String[] getBrowseRows(Player player) {
        UUID uuid = player.getUniqueId();
        String[] rows = browseRowChannels.get(uuid);
        if (rows == null || rows.length != 5) {
            rows = defaultBrowseRows();
            browseRowChannels.put(uuid, rows);
        }
        for (int i = 0; i < rows.length; i++) rows[i] = normalizeBrowseChannel(rows[i]);
        return rows;
    }

    private String[] defaultBrowseRows() {
        return new String[]{"most-guests", "weekly-cookies", "recommended", "new-houses", "random"};
    }

    private void setBrowseRowChannel(Player player, int row, String channel) {
        if (row < 0 || row > 4) return;
        String[] rows = getBrowseRows(player);
        rows[row] = normalizeBrowseChannel(channel);
        browseRowChannels.put(player.getUniqueId(), rows);
        saveBrowseChannelSelections(player);
    }

    private ItemStack browseRowSelectorItem(int row, String channel, int publicHouses) {
        BrowseChannel ch = browseChannel(channel);
        List<String> lore = new ArrayList<>();
        lore.add(plugin.color("&7Shows houses from this channel."));
        lore.add(plugin.color("&7Houses found: &f" + publicHouses));
        lore.add("");
        lore.add(plugin.color("&eClick to change this channel!"));
        lore.add(plugin.color("&8Browse Row: " + row));
        ItemStack stack = new ItemStack(ch == null || ch.icon == null ? Material.CHEST : ch.icon);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color("&bChannel: " + prettyBrowseChannel(channel)));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Integer browseRowFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Browse Row: ")) {
                try { return Integer.parseInt(clean.substring("Browse Row: ".length()).trim()); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private List<House> housesForChannel(String channel) {
        return housesForChannel(channel, null);
    }

    private List<House> housesForChannel(String channel, Player viewer) {
        String clean = normalizeBrowseChannel(channel);

        if (clean == null || clean.isEmpty()) {
            return new ArrayList<>();
        }

        List<House> publicHouses = new ArrayList<>(houses.getPublicHouses());
        List<House> list = new ArrayList<>();

        switch (clean) {
            case "most-guests":
                list.addAll(publicHouses);
                list.sort((a, b) -> Integer.compare(houses.getGuests(b), houses.getGuests(a)));
                return list;

            case "weekly-cookies":
                list.addAll(publicHouses);
                list.sort((a, b) -> Integer.compare(b.getWeeklyCookies(), a.getWeeklyCookies()));
                return list;

            case "new-houses":
                list.addAll(publicHouses);
                list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                return list;

            case "random":
                list.addAll(publicHouses);
                java.util.Collections.shuffle(list);
                return list;

            case "revisit":
                return recentlyVisitedHouses(viewer);

            case "mvp++":
                for (House house : publicHouses) {
                    if (ownerHasMvpPlusPlus(house)) list.add(house);
                }
                list.sort((a, b) -> Integer.compare(houses.getGuests(b), houses.getGuests(a)));
                return list;

            case "staff+yt":
                for (House house : publicHouses) {
                    if (ownerIsStaffOrYoutube(house)) list.add(house);
                }
                list.sort((a, b) -> Integer.compare(houses.getGuests(b), houses.getGuests(a)));
                return list;

            case "recommended":
            case "social":
            case "friends":
            case "guild":
            case "own-language":
            case "more-language-channels":
                return list;

            default:
                if (isTagChannel(clean)) {
                    String tag = tagNameFromChannel(clean);
                    for (House house : publicHouses) {
                        if (house.hasTag(tag)) list.add(house);
                    }
                    list.sort((a, b) -> Integer.compare(houses.getGuests(b), houses.getGuests(a)));
                    return list;
                }

                return list;
        }
    }

    public void recordRecentVisit(Player player, House house) {
        if (player == null || house == null) return;
        if (house.getOwner().equals(player.getUniqueId())) return;
        List<UUID> recent = recentBrowseVisits.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        recent.remove(house.getOwner());
        recent.add(0, house.getOwner());
        while (recent.size() > 45) recent.remove(recent.size() - 1);
        saveBrowseChannelSelections(player);
    }

    private List<House> recentlyVisitedHouses(Player viewer) {
        List<House> list = new ArrayList<>();
        if (viewer == null) return list;
        List<UUID> recent = recentBrowseVisits.get(viewer.getUniqueId());
        if (recent == null || recent.isEmpty()) return list;
        for (UUID owner : recent) {
            House house = houses.getHouse(owner);
            if (house != null && house.isPublicHouse()) list.add(house);
        }
        return list;
    }

    private boolean ownerHasMvpPlusPlus(House house) {
        Player owner = Bukkit.getPlayer(house.getOwner());
        if (owner == null) return false;
        String rank = cleanRankText(abelix.getRankName(owner));
        String prefix = cleanRankText(ChatColor.stripColor(plugin.color(abelix.getPrefix(owner))));
        return rank.equals("mvp++") || rank.equals("mvpplusplus") || rank.equals("mvp-plus-plus")
                || prefix.contains("mvp++") || prefix.contains("mvp-plus-plus");
    }

    private boolean ownerIsStaffOrYoutube(House house) {
        Player owner = Bukkit.getPlayer(house.getOwner());
        if (owner == null) return false;

        String rank = cleanRankText(abelix.getRankName(owner));
        String prefix = cleanRankText(ChatColor.stripColor(plugin.color(abelix.getPrefix(owner))));

        return isStaffOrYoutubeRank(rank) || isStaffOrYoutubeRank(prefix);
    }

    private boolean isStaffOrYoutubeRank(String value) {
        if (value == null || value.isEmpty()) return false;
        return value.equals("yt") || value.equals("youtube") || value.equals("youtuber")
                || value.equals("helper") || value.equals("mod") || value.equals("moderator")
                || value.equals("admin") || value.equals("owner") || value.equals("staff")
                || value.contains("[yt]") || value.contains("[youtube]") || value.contains("[youtuber]")
                || value.contains("[helper]") || value.contains("[mod]") || value.contains("[admin]")
                || value.contains("[owner]") || value.contains("[staff]");
    }

    private String cleanRankText(String value) {
        if (value == null) return "";
        return value.toLowerCase(java.util.Locale.ENGLISH)
                .replace("§", "")
                .replace("&", "")
                .replace(" ", "")
                .replace("_", "-");
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ENGLISH);
    }

    private boolean isTagChannel(String clean) {
        return clean.endsWith("-tag") || clean.equals("roleplay") || clean.equals("parkour") || clean.equals("building") || clean.equals("chatting") || clean.equals("maze") || clean.equals("competition") || clean.equals("event") || clean.equals("guild-hangout") || clean.equals("minigame");
    }

    private String tagNameFromChannel(String clean) {
        clean = clean.replace("-tag", "");
        if (clean.equals("roleplay")) return "roleplay";
        return clean;
    }

    private ItemStack browseChannelItem(BrowseChannel channel, boolean selected) {
        return browseChannelItem(channel, selected, -1);
    }

    private ItemStack browseChannelItem(BrowseChannel channel, boolean selected, int publicHouses) {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.color(channel.description));
        if (publicHouses >= 0) lore.add(plugin.color("&7Houses found: &f" + publicHouses));
        lore.add("");
        lore.add(plugin.color(selected ? "&aSelected!" : "&eClick to use this channel!"));
        lore.add(plugin.color("&8Browse Channel: " + channel.key));
        ItemStack stack = new ItemStack(channel.icon == null ? Material.CHEST : channel.icon);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color((selected ? "&a" : "&b") + "Channel: " + channel.name));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String browseChannelFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Browse Channel: ")) return clean.substring("Browse Channel: ".length()).trim();
        }
        return null;
    }

    private static class BrowseChannel {
        final String key; final String name; final Material icon; final String description;
        BrowseChannel(String key, String name, Material icon, String description) {
            this.key = key; this.name = name; this.icon = icon; this.description = description;
        }
    }

    private List<BrowseChannel> browseChannelList() {
        List<BrowseChannel> list = new ArrayList<>();
        list.add(new BrowseChannel("most-guests", "Most Guests", net.fakepixel.housing.util.Compat.mat("EMERALD"), "&7View houses with the most guests."));
        list.add(new BrowseChannel("weekly-cookies", "Weekly Cookies", net.fakepixel.housing.util.Compat.mat("COOKIE"), "&7View houses with the most cookies."));
        list.add(new BrowseChannel("recommended", "Recommended", net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&7View recommended houses."));
        list.add(new BrowseChannel("mvp++", "MVP++", net.fakepixel.housing.util.Compat.mat("GOLD_INGOT"), "&7View houses owned by MVP++ players."));
        list.add(new BrowseChannel("social", "Social", net.fakepixel.housing.util.Compat.mat("ENDER_CHEST"), "&7View social houses."));
        list.add(new BrowseChannel("friends", "Friends", net.fakepixel.housing.util.Compat.mat("SKULL_ITEM"), "&7View houses owned by friends."));
        list.add(new BrowseChannel("guild", "Guild", net.fakepixel.housing.util.Compat.mat("BANNER"), "&7View houses from your guild."));
        list.add(new BrowseChannel("new-houses", "New Houses", net.fakepixel.housing.util.Compat.mat("GRASS_BLOCK"), "&7View new houses of players who recently joined Player Housing."));
        list.add(new BrowseChannel("revisit", "Revisit", net.fakepixel.housing.util.Compat.mat("COMPASS"), "&7View houses you recently visited."));
        list.add(new BrowseChannel("random", "Random", net.fakepixel.housing.util.Compat.mat("ENDER_PEARL"), "&7View random public houses."));
        list.add(new BrowseChannel("staff+yt", "Staff+YT", net.fakepixel.housing.util.Compat.mat("DIAMOND"), "&7View houses owned by staff members and youtubers."));
        list.add(new BrowseChannel("own-language", "Own Language", net.fakepixel.housing.util.Compat.mat("PAPER"), "&7View houses using your language."));
        list.add(new BrowseChannel("more-language-channels", "More Language Channels", net.fakepixel.housing.util.Compat.mat("BOOK"), "&7Browse language channels."));
        list.add(new BrowseChannel("roleplay", "Roleplay (RP) Tag", net.fakepixel.housing.util.Compat.mat("NAME_TAG"), "&7View houses tagged with Roleplay by their owner."));
        list.add(new BrowseChannel("parkour", "Parkour Tag", net.fakepixel.housing.util.Compat.mat("FEATHER"), "&7View houses tagged with Parkour by their owner."));
        list.add(new BrowseChannel("building", "Building Tag", net.fakepixel.housing.util.Compat.mat("BRICK"), "&7View houses tagged with Building by their owner."));
        list.add(new BrowseChannel("chatting", "Chatting Tag", net.fakepixel.housing.util.Compat.mat("PAPER"), "&7View houses tagged with Chatting by their owner."));
        list.add(new BrowseChannel("maze", "Maze Tag", net.fakepixel.housing.util.Compat.mat("IRON_FENCE"), "&7View houses tagged with Maze by their owner."));
        list.add(new BrowseChannel("competition", "Competition Tag", net.fakepixel.housing.util.Compat.mat("GOLD_BLOCK"), "&7View houses tagged with Competition by their owner."));
        list.add(new BrowseChannel("event", "Event Tag", net.fakepixel.housing.util.Compat.mat("FIREWORK"), "&7View houses tagged with Event by their owner."));
        list.add(new BrowseChannel("guild-hangout", "Guild Hangout Tag", net.fakepixel.housing.util.Compat.mat("ANVIL"), "&7View houses tagged with Guild Hangout."));
        list.add(new BrowseChannel("minigame", "Minigame Tag", net.fakepixel.housing.util.Compat.mat("SLIME_BALL"), "&7View houses tagged with Minigame."));
        return list;
    }

    private BrowseChannel browseChannel(String channel) {
        String clean = normalizeBrowseChannel(channel);
        for (BrowseChannel ch : browseChannelList()) if (ch.key.equals(clean)) return ch;
        return null;
    }

    private String normalizeBrowseChannel(String channel) {
        if (channel == null) return "";
        String clean = channel.trim().toLowerCase(java.util.Locale.ENGLISH).replace("_", "-").replace(" ", "-");
        if (clean.equals("roleplay") || clean.equals("rp") || clean.equals("roleplay-rp")) return "roleplay";
        if (clean.equals("parkour")) return "parkour";
        if (clean.equals("new") || clean.equals("newhouses")) return "new-houses";
        if (clean.equals("ownlanguage") || clean.equals("own-lang") || clean.equals("language")) return "own-language";
        if (clean.equals("morelanguages") || clean.equals("more-language") || clean.equals("language-channels")) return "more-language-channels";
        if (clean.equals("mvpplusplus") || clean.equals("mvp-plus-plus")) return "mvp++";
        if (clean.equals("staffyt") || clean.equals("staff-youtube") || clean.equals("staff+youtube") || clean.equals("staff-yt")) return "staff+yt";
        if (clean.equals("guildhangout")) return "guild-hangout";
        return clean;
    }

    private String prettyBrowseChannel(String channel) {
        String normalized = normalizeBrowseChannel(channel);
        for (BrowseChannel ch : browseChannelList()) if (ch.key.equals(normalized)) return ch.name;
        if (normalized == null || normalized.isEmpty()) return "Unknown";
        String clean = normalized.replace('-', ' ');
        return clean.substring(0, 1).toUpperCase(java.util.Locale.ENGLISH) + clean.substring(1);
    }

    private void loadBrowseChannelSelections() { }

    private void saveBrowseChannelSelections(Player player) {
        String[] rows = getBrowseRows(player);
        List<String> recent = new ArrayList<>();
        List<UUID> recentUuids = recentBrowseVisits.get(player.getUniqueId());
        if (recentUuids != null) for (UUID uuid : recentUuids) recent.add(uuid.toString());
        try {
            if (plugin.getHousingRepository() != null) plugin.getHousingRepository().saveBrowseData(player.getUniqueId(), String.join(",", rows) + "|" + String.join(",", recent));
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not save browse preferences: " + ex.getMessage());
        }
    }
    public void openList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, listTitle);
        int[] slots = new int[]{10, 11, 12, 13, 14, 15};
        int max = Math.min(houses.maxPlots(), slots.length);
        for (int plot = 1; plot <= max; plot++) {
            inv.setItem(slots[plot - 1], plotItem(player, plot));
        }
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("CHEST"), "&aMain Menu", "&7Go back."));
        player.openInventory(inv);
    }

    private ItemStack plotItem(Player player, int plotId) {
        House house = houses.getHouse(player.getUniqueId(), plotId);
        boolean owned = houses.isPlotOwned(player.getUniqueId(), plotId);
        if (owned) {
            if (house == null) {
                return item(net.fakepixel.housing.util.Compat.mat("GRASS"),
                        "&aHouse #" + plotId,
                        "&7Unlocked.",
                        plotId == 1 ? "&7Owned by default." : "&7Purchased plot.",
                        "",
                        "&eClick to create/load!",
                        "&8House Plot: " + plotId);
            }
            List<String> lore = new ArrayList<>();
            lore.add("&7Unlocked plot.");
            lore.add("&7Players: &a" + houses.getGuests(house));
            lore.add("&7Cookies: &a" + house.getCookies());
            lore.add("");
            lore.add("&eClick to enter!");
            lore.add("&eRight click to toggle public/private!");
            lore.add("&8Owner UUID: " + house.getOwner().toString());
            lore.add("&8House Plot: " + plotId);
            return item(net.fakepixel.housing.util.Compat.mat("GRASS_BLOCK"), "&a" + house.getName(), lore.toArray(new String[0]));
        }
        int price = houses.plotPrice(plotId);
        return item(net.fakepixel.housing.util.Compat.mat("COAL_BLOCK"),
                "&cHouse #" + plotId,
                "&7Locked plot.",
                "&7Cost: &6" + price + " Gold",
                "",
                "&eClick to purchase!",
                "&8House Plot: " + plotId);
    }

    public void openRandom(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, randomTitle);
        inv.setItem(13, item(net.fakepixel.housing.util.Compat.mat("COMPASS"), "&bRandom Public House", "&7Finds a random public house.", "&eClick to enter!"));
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("CHEST"), "&aMain Menu", "&7Go back."));
        player.openInventory(inv);
    }

    public void openLeaderboard(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, leaderboardTitle);
        List<House> list = houses.getTopCookieHouses(45);
        for (int i = 0; i < list.size(); i++) {
            House house = list.get(i);
            inv.setItem(i, houseItem(house, "&7Rank: &e#" + (i + 1), "&eClick to enter!"));
        }
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("CHEST"), "&aMain Menu", "&7Go back."));
        player.openInventory(inv);
    }

    private void openSimple(Player player, String title, ItemStack... items) {
        Inventory inv = Bukkit.createInventory(null, 36, title);
        for (int i = 0; i < items.length && i < 27; i++) inv.setItem(i, items[i]);
        inv.setItem(31, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to Housing Menu."));
        player.openInventory(inv);
    }

    private void openSettings(Player player, House house) {
        Inventory inv = Bukkit.createInventory(null, 54, settingsTitle);

        inv.setItem(10, item(net.fakepixel.housing.util.Compat.mat("CLOCK"),
                "&aTime Selector",
                "&7Allows you to select the",
                "&7time for your home.",
                "",
                "&6Alias: &7/time <time>",
                "&eClick to select!"));
        inv.setItem(11, item(net.fakepixel.housing.util.Compat.mat("GRASS_BLOCK"),
                "&aBiome Selector",
                "&7Allows you to select the",
                "&7biome for your home.",
                "",
                "&6Alias: &7/biome <biome>",
                "&eClick to select!"));
        inv.setItem(12, item(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"),
                "&aSet House Name",
                "",
                "&6Alias: &7/housing name <name>",
                "&eClick to change!"));
        PlotTier nextPlot = nextPlotTier(house.getPlotSize());
        inv.setItem(13, item(net.fakepixel.housing.util.Compat.mat("IRON_PICKAXE"),
                nextPlot == null ? "&aPlot Size Upgrade: &eMaxed" : "&aPlot Size Upgrade: &e" + nextPlot.size + "x" + nextPlot.size,
                "&7Expand your allowed build area",
                "&7without resetting your house.",
                "",
                "&7Current Plot Size: &f" + house.getPlotSize() + "x" + house.getPlotSize(),
                nextPlot == null ? "&7Next Upgrade: &cMaxed" : "&7Next Upgrade: &f" + nextPlot.size + "x" + nextPlot.size,
                nextPlot == null ? "&7Upgrade Cost: &cNone" : plotCostLine(player, nextPlot),
                nextPlot == null ? "&aAlready unlocked!" : plotStatusLine(player, nextPlot),
                "",
                "&eClick to view upgrades!"));
        inv.setItem(14, item(net.fakepixel.housing.util.Compat.mat("PAPER"),
                "&aHouse Language",
                "&7Select your home's language tags. These",
                "&7tags allow other players to find your",
                "&7house based on the language in your home.",
                "&7You can have &a3 &7languages at once.",
                "",
                "&6Alias: &7/houselang <lang>",
                "&eClick to select!"));
        inv.setItem(15, item(net.fakepixel.housing.util.Compat.mat("BOOK"),
                "&aHouse Tags",
                "&7Select your home's tags. These tags",
                "&7allow other players to find your house",
                "&7based on the content in your home.",
                "&7You can have &a3 &7tags at once.",
                "",
                "&6Alias: &7/housetag <tag>",
                "&eClick to select!"));
        inv.setItem(16, item(net.fakepixel.housing.util.Compat.mat("COOKIE"),
                "&aCookie Jar",
                "&7Cookies are used as a rating system",
                "&7and reset on a weekly basis! You can",
                "&7give out a maximum of &e10 &7cookie packs to",
                "&7other players, so choose carefully.",
                "",
                "&7You can give out another &e10 &7cookie packs!",
                "",
                "&eYour Cookies: &a" + house.getCookies()));
        inv.setItem(19, item(net.fakepixel.housing.util.Compat.mat("GOLD_INGOT"),
                "&aMax Players: &b30",
                "&7Change the maximum number of",
                "&7players allowed in your house at",
                "&7one time.",
                "",
                "&eLeft Click to cycle forward!",
                "&eRight Click to cycle backwards!"));
        inv.setItem(20, item(net.fakepixel.housing.util.Compat.mat("PAPER"),
                "&aJoin/Leave Messages",
                "&7Join Message: " + enabledText(house.isJoinMessageEnabled()),
                "&7Leave Message: " + enabledText(house.isLeaveMessageEnabled()),
                "&7Announce Joins: " + enabledText(house.isAnnounceJoinsEnabled()),
                "&7Announce Leaves: " + enabledText(house.isAnnounceLeavesEnabled()),
                "",
                "&eClick to edit!"));
        inv.setItem(21, item(net.fakepixel.housing.util.Compat.mat("PAPER"),
                "&aDoor/Fence Auto-Reset: &fOff",
                "&7Whether doors and fences will auto",
                "&7close once opened by a guest.",
                "",
                "&eClick to toggle!"));
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("PAPER"),
                "&aStatuses: &fOff",
                "&7Whether to show players' status",
                "&7above their heads.",
                "",
                "&eClick to toggle!"));
        inv.setItem(23, item(net.fakepixel.housing.util.Compat.mat("FEATHER"),
                "&aParkour Announce: &fALL",
                "&7Change Announce Mode for Parkour.",
                "&7This can be one of &fOFF&7, &fRECORD&7, &fALL&7.",
                "",
                "&7Current mode: &fALL",
                "&7Every time a player finishes parkour",
                "&7their time will be broadcasted in your",
                "&7home!",
                "",
                "&6Alias: &7/parkour announce <mode>",
                "",
                "&eLeft Click to cycle forward!",
                "&eRight Click to cycle backwards!"));
        inv.setItem(24, item(net.fakepixel.housing.util.Compat.mat("IRON_CHESTPLATE"),
                "&aPvP + Damage Settings",
                "&7PvP: " + enabledText(house.isPvpEnabled()),
                "&7Fall Damage: " + enabledText(house.isFallDamageEnabled()),
                "&7Fire Damage: " + enabledText(house.isFireDamageEnabled()),
                "&7Hunger: " + enabledText(house.isHungerEnabled()),
                "",
                "&eClick to view!"));
        inv.setItem(25, item(net.fakepixel.housing.util.Compat.mat("ENDER_PEARL"),
                "&aTeleport Pads",
                "&7View and manage every",
                "&7Teleport Pad in this house.",
                "",
                "&7Pads: &f" + house.getTeleportPads().size(),
                "&eClick to view!"));
        inv.setItem(26, item(net.fakepixel.housing.util.Compat.mat("EMPTY_MAP"),
                "&aScoreboard Editor",
                "&7Customize the scoreboard shown",
                "&7inside this house.",
                "",
                "&7Scoreboard: " + enabledText(house.isScoreboardEnabled()),
                "&7Lines: &f" + house.getScoreboardLines().size(),
                "&eClick to edit!"));
        inv.setItem(28, item(net.fakepixel.housing.util.Compat.mat("COMMAND"),
                "&aCommands",
                "&7Create and edit custom",
                "&7commands for this house.",
                "",
                "&7Commands: &f" + house.getCustomCommands().size(),
                "&eClick to edit!"));
        inv.setItem(29, item(net.fakepixel.housing.util.Compat.mat("REDSTONE"),
                "&aFunctions",
                "&7Create reusable groups",
                "&7of safe house actions.",
                "",
                "&7Functions: &f" + house.getFunctions().size(),
                "&eClick to edit!"));
        inv.setItem(40, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&aMain Menu"));
        inv.setItem(41, item(net.fakepixel.housing.util.Compat.mat("TNT"),
                "&aReset House",
                "&7Clicking this resets your house.",
                "&c&lWARNING: Cannot be reverted!",
                "",
                "&eClick to reset house!"));

        player.openInventory(inv);
    }

    public void openSettingsMenu(Player player, House house) {
        if (house == null || !canEditHouseSettings(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        openSettings(player, house);
    }

    private void openJukebox(Player player, House house) {
        if (house == null) {
            player.closeInventory();
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, jukeboxTitle);
        int slot = 0;
        for (JukeboxService.Song song : plugin.getJukeboxService().songs()) {
            if (slot >= 36) break;
            inv.setItem(slot++, jukeboxSongItem(house, song));
        }
        inv.setItem(45, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cStop Music", "&7Stop and clear the selected", "&7music for this house.", "", "&eClick to stop!"));
        inv.setItem(46, item(net.fakepixel.housing.util.Compat.mat("ENDER_PEARL"), "&aShuffle / Random Song", "&7Pick a random song now and", "&7shuffle music for visitors.", "", "&eClick to shuffle!"));
        inv.setItem(47, item(house.isJukeboxEnabled() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(),
                "&aMusic: " + enabledText(house.isJukeboxEnabled()), "&7Toggle house music playback.", "", "&eClick to toggle!"));
        inv.setItem(48, item(net.fakepixel.housing.util.Compat.mat("REDSTONE"), "&cVolume -", "&7Current: &f" + house.getJukeboxVolume() + "%", "", "&eClick to lower!"));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to House Settings."));
        inv.setItem(50, item(net.fakepixel.housing.util.Compat.mat("GLOWSTONE_DUST"), "&aVolume +", "&7Current: &f" + house.getJukeboxVolume() + "%", "", "&eClick to raise!"));
        inv.setItem(53, item(net.fakepixel.housing.util.Compat.mat("WOOD_DOOR"), "&aClose"));
        player.openInventory(inv);
    }

    private ItemStack jukeboxSongItem(House house, JukeboxService.Song song) {
        boolean selected = song.id().equals(house.getJukeboxSong());
        return item(song.material(),
                (selected ? "&b" : "&a") + ChatColor.stripColor(plugin.color(song.displayName())),
                "&7Select this record for",
                "&7this house's jukebox.",
                "",
                selected ? "&aCurrently selected!" : "&eClick to select!",
                "&8Jukebox Song: " + song.id());
    }

    private String jukeboxSongName(House house) {
        JukeboxService.Song song = plugin.getJukeboxService().song(house.getJukeboxSong());
        if (song == null) return house.isJukeboxShuffle() ? "Shuffle" : "None";
        return ChatColor.stripColor(plugin.color(song.displayName()));
    }

    private JukeboxService.Song jukeboxSongFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Jukebox Song: ")) return plugin.getJukeboxService().song(clean.substring("Jukebox Song: ".length()));
        }
        return null;
    }

    private String houseSettingsAction(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return "";
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("House Settings Action: ")) return clean.substring("House Settings Action: ".length()).trim().toLowerCase(java.util.Locale.ENGLISH);
        }
        return "";
    }

    private void openPvpDamageSettings(Player player, House house) {
        if (house == null) {
            player.closeInventory();
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, pvpDamageTitle);
        inv.setItem(10, toggleSettingItem("&aPvP", house.isPvpEnabled(), "Allow players to damage each other."));
        inv.setItem(11, toggleSettingItem("&aFall Damage", house.isFallDamageEnabled(), "Allow fall damage inside this house."));
        inv.setItem(12, toggleSettingItem("&aFire Damage", house.isFireDamageEnabled(), "Allow fire, lava and burning damage."));
        inv.setItem(13, toggleSettingItem("&aHunger", house.isHungerEnabled(), "Allow food level changes."));
        inv.setItem(14, toggleSettingItem("&aInstant Respawn", house.isInstantRespawnEnabled(), "Respawn players immediately after death."));
        inv.setItem(15, toggleSettingItem("&aKill/Death Messages", house.isKillDeathMessagesEnabled(), "Broadcast death messages in this house."));
        inv.setItem(31, item(net.fakepixel.housing.util.Compat.mat("CHEST"),
                "&aEdit PvP Layout",
                "&7Set the inventory players receive",
                "&7when PvP is enabled.",
                "",
                "&eClick to edit!"));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to House Settings."));
        player.openInventory(inv);
    }

    private void openVisitingMessages(Player player, House house) {
        if (house == null) {
            player.closeInventory();
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 36, visitingMessagesTitle);
        inv.setItem(10, toggleSettingItem("&aJoin Message", house.isJoinMessageEnabled(), "Send a welcome message to the visitor."));
        inv.setItem(11, item(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"),
                "&aCustom Join Message",
                "&7Current:",
                "&f" + house.getJoinMessage(),
                "",
                "&eClick to edit!"));
        inv.setItem(12, toggleSettingItem("&aLeave Message", house.isLeaveMessageEnabled(), "Send a leave message to the player leaving."));
        inv.setItem(13, item(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"),
                "&aCustom Leave Message",
                "&7Current:",
                "&f" + house.getLeaveMessage(),
                "",
                "&eClick to edit!"));
        inv.setItem(14, toggleSettingItem("&aAnnounce Joins", house.isAnnounceJoinsEnabled(), "Tell players already inside when someone joins."));
        inv.setItem(15, toggleSettingItem("&aAnnounce Leaves", house.isAnnounceLeavesEnabled(), "Tell players inside when someone leaves."));
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("BOOK"),
                "&aPlaceholders",
                "&7%player%",
                "&7%owner%",
                "&7%house_name%",
                "&7%guests%",
                "&7%cookies%",
                "&7%weekly_cookies%",
                "&7%housing_group%"));
        inv.setItem(31, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to House Settings."));
        player.openInventory(inv);
    }

    private void openTeleportPads(Player player, House house) {
        if (house == null) {
            player.closeInventory();
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, teleportPadsTitle);
        int slot = 0;
        for (House.TeleportPad pad : house.getTeleportPads().values()) {
            if (slot >= 45) break;
            inv.setItem(slot++, teleportPadListItem(house, pad));
        }
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to House Settings."));
        player.openInventory(inv);
    }

    private void openTeleportPad(Player player, House house, House.TeleportPad pad) {
        if (house == null || pad == null) {
            openTeleportPads(player, house);
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 36, teleportPadTitle);
        inv.setItem(4, teleportPadListItem(house, pad));
        inv.setItem(10, item(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"), "&aRename Pad", "&7Change this pad's saved name.", "", "&eClick to rename!"));
        inv.setItem(11, item(net.fakepixel.housing.util.Compat.mat("ENDER_PEARL"), "&aRelink Pad", "&7Choose another pad in this", "&7house to link to.", "", "&eClick to relink!"));
        inv.setItem(12, item(net.fakepixel.housing.util.Compat.mat("TNT"), "&cDelete Pad", "&7Remove this pad from house data", "&7and unlink connected pads.", "", "&eClick to delete!"));
        inv.setItem(14, item(pad.isParticles() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(),
                "&aParticles: " + enabledText(pad.isParticles()), "&7Toggle teleport particles.", "", "&eClick to toggle!"));
        inv.setItem(15, item(pad.isSound() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(),
                "&aSound: " + enabledText(pad.isSound()), "&7Toggle teleport sound.", "", "&eClick to toggle!"));
        inv.setItem(31, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to Teleport Pads."));
        inv.setItem(35, teleportPadToken(pad.getId()));
        player.openInventory(inv);
    }

    private void openTeleportPadRelink(Player player, House house, House.TeleportPad source) {
        if (house == null || source == null) {
            openTeleportPads(player, house);
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, teleportPadRelinkTitle);
        int slot = 0;
        for (House.TeleportPad pad : house.getTeleportPads().values()) {
            if (pad.getId().equals(source.getId())) continue;
            if (slot >= 45) break;
            inv.setItem(slot++, teleportPadListItem(house, pad));
        }
        inv.setItem(48, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cUnlink Pad", "&7Remove this pad's link."));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to Teleport Pad."));
        inv.setItem(53, teleportPadToken(source.getId()));
        player.openInventory(inv);
    }

    private ItemStack teleportPadListItem(House house, House.TeleportPad pad) {
        House.TeleportPad linked = house.getTeleportPad(pad.getLinkedPadId());
        return item(net.fakepixel.housing.util.Compat.mat("ENDER_PEARL"),
                "&a" + pad.getName(),
                "&7Location: &f" + pad.getLocation(),
                "&7Linked Pad: &f" + (linked == null ? "None" : linked.getName()),
                "&7Particles: " + enabledText(pad.isParticles()),
                "&7Sound: " + enabledText(pad.isSound()),
                "",
                "&eClick to manage!",
                "&8Teleport Pad ID: " + pad.getId());
    }

    private ItemStack teleportPadToken(String id) {
        return item(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Teleport Pad", "&8Teleport Pad ID: " + id);
    }

    private ItemStack toggleSettingItem(String name, boolean enabled, String description) {
        return item(enabled ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(),
                name + ": " + (enabled ? "&aEnabled" : "&cDisabled"),
                "&7" + description,
                "",
                "&7Current: " + enabledText(enabled),
                "&eClick to toggle!");
    }

    private String enabledText(boolean enabled) {
        return enabled ? "&aEnabled" : "&cDisabled";
    }

    private void openPvpLayoutEditor(Player player, House house) {
        if (house == null) {
            player.closeInventory();
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, pvpLayoutTitle);
        ItemStack[] layout = house.getPvpLayout();
        for (int i = 0; i < layout.length && i < 36; i++) {
            if (layout[i] != null) inv.setItem(i, layout[i].clone());
        }
        ItemStack filler = item(legacyItem("STAINED_GLASS_PANE", 7), "&8Layout Controls");
        for (int slot = 36; slot < 45; slot++) inv.setItem(slot, filler);
        inv.setItem(45, item(net.fakepixel.housing.util.Compat.mat("EMERALD_BLOCK"), "&aSave Layout", "&7Save the items above as", "&7the default PvP layout."));
        inv.setItem(48, item(net.fakepixel.housing.util.Compat.mat("TNT"), "&cReset Layout", "&7Clear the saved PvP layout."));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to PvP + Damage Settings."));
        inv.setItem(53, item(net.fakepixel.housing.util.Compat.mat("ANVIL"), "&aApply Layout", "&7Apply the saved PvP layout", "&7to your inventory now."));
        player.openInventory(inv);
    }

    private void openPlotUpgrade(Player player, House house) {
        if (house == null) {
            player.closeInventory();
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, plotUpgradeTitle);
        int current = house.getPlotSize();
        PlotTier next = nextPlotTier(current);

        inv.setItem(4, item(net.fakepixel.housing.util.Compat.mat("FILLED_MAP"),
                "&aPlot Size",
                "&7Current Plot Size: &f" + current + "x" + current,
                next == null ? "&7Next Upgrade: &cMaxed" : "&7Next Upgrade: &f" + next.size + "x" + next.size,
                next == null ? "&7Upgrade Cost: &cNone" : plotCostLine(player, next),
                next == null ? "&aYour house already has the highest plot size." : plotStatusLine(player, next)));

        int[] slots = new int[]{10, 11, 12, 13, 14, 15};
        PlotTier[] tiers = plotTiers();
        for (int i = 0; i < tiers.length && i < slots.length; i++) {
            inv.setItem(slots[i], plotTierItem(player, house, tiers[i], next));
        }
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to House Settings."));
        player.openInventory(inv);
    }

    private ItemStack plotTierItem(Player player, House house, PlotTier tier, PlotTier next) {
        boolean unlocked = house.getPlotSize() >= tier.size;
        boolean purchasable = next != null && next.size == tier.size;
        boolean locked = isPlotTierLocked(player, tier);
        String cost = tier.cost <= 0 ? "Default" : (hasFreePlotBypass(player, tier) ? "Free" : tier.cost + " Gold");
        return item(unlocked ? net.fakepixel.housing.util.Compat.limeDye() : new ItemStack(net.fakepixel.housing.util.Compat.mat("IRON_PICKAXE")),
                (unlocked ? "&a" : locked ? "&c" : purchasable ? "&e" : "&7") + tier.displayName,
                "&7Current Plot Size: &f" + house.getPlotSize() + "x" + house.getPlotSize(),
                next == null ? "&7Next Upgrade: &cMaxed" : "&7Next Upgrade: &f" + next.size + "x" + next.size,
                "&7Upgrade Cost: &6" + cost,
                "&7Can Afford: " + (canAffordPlot(player, tier) ? "&aYes" : "&cNo"),
                "&7Already Unlocked: " + (unlocked ? "&aYes" : "&cNo"),
                "",
                locked ? "&cRequires Housing+" : (unlocked ? "&aAlready unlocked!" : (purchasable ? "&eClick to upgrade!" : "&7Unlock previous tiers first.")));
    }

    private void openPlayers(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !house.canManage(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, playersTitle);
        int slot = 0;
        for (Player target : player.getWorld().getPlayers()) {
            if (slot >= 45) break;
            inv.setItem(slot++, playerItem(house, target));
        }
        inv.setItem(45, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cKick Guests", "&7Kick every guest from this house."));
        inv.setItem(46, item(net.fakepixel.housing.util.Compat.mat("TNT"), "&cKick All", "&7Kick everyone except you from this house."));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to Housing Menu."));
        player.openInventory(inv);
    }

    public void openPlayerManageMenu(Player manager, Player target) {
        House house = houses.getHouseByWorld(manager.getWorld().getName());
        if (house == null || target == null || !target.getWorld().getName().equals(house.getWorldName()) || !canOpenPlayerOptions(manager, house)) {
            manager.sendMessage(plugin.msg("no-permission"));
            return;
        }
        playerOptionTargets.put(manager.getUniqueId(), target.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 36, managePlayerTitle);
        String group = house.getGroup(target.getUniqueId());
        inv.setItem(4, playerItem(house, target));
        inv.setItem(10, playerOptionItem(house, manager, target, "teleport", net.fakepixel.housing.util.Compat.mat("ENDER_PEARL"),
                "&aTeleport to Player", "&7Teleport to &e" + target.getName() + "&7."));
        inv.setItem(11, playerOptionItem(house, manager, target, "kick", net.fakepixel.housing.util.Compat.mat("IRON_DOOR"),
                "&cKick Player", "&7Remove this player from the house."));
        inv.setItem(12, playerOptionItem(house, manager, target, "ban", net.fakepixel.housing.util.Compat.mat("BARRIER"),
                "&4Ban Player", "&7Ban and remove this player from the house."));
        inv.setItem(13, playerOptionItem(house, manager, target, "mute", house.isMuted(target.getUniqueId()) ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(),
                house.isMuted(target.getUniqueId()) ? "&aUnmute Player" : "&eMute Player",
                house.isMuted(target.getUniqueId()) ? "&7Allow this player to chat again." : "&7Prevent this player from chatting."));
        inv.setItem(14, playerOptionItem(house, manager, target, "manage", net.fakepixel.housing.util.Compat.mat("NAME_TAG"),
                "&aChange Group", "&7Current: " + house.groupSuffix(group), "&7Next: " + house.groupSuffix(nextGroup(group))));
        inv.setItem(15, item(net.fakepixel.housing.util.Compat.mat("BOOK"),
                "&aView Player Info",
                "&7Name: &f" + target.getName(),
                "&7Group: " + house.groupSuffix(group),
                "&7Muted: " + (house.isMuted(target.getUniqueId()) ? "&cYes" : "&aNo"),
                "&7Banned: " + (house.isBanned(target.getUniqueId()) ? "&cYes" : "&aNo"),
                "",
                "&eClick to view info!"));
        inv.setItem(31, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to Players."));
        inv.setItem(35, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cClose", "&7Close this menu."));
        manager.openInventory(inv);
    }


    public void openCarpenterMenu(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || (!house.canManage(player.getUniqueId()) && !canRemoveCarpenter(player, house))) {
            player.closeInventory();
            player.sendMessage(plugin.color("&cOnly the owner/co-owner can use the Carpenter."));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, carpenterTitle);

        inv.setItem(10, item(net.fakepixel.housing.util.Compat.mat("BRICKS"),
                "&aBlocks",
                "&7Browse blocks you have unlocked.",
                "",
                "&eClick to browse!"));
        inv.setItem(11, item(net.fakepixel.housing.util.Compat.mat("ROSE_BUSH"),
                "&aPlants",
                "&7Browse plants you have unlocked.",
                "",
                "&eClick to browse!"));
        inv.setItem(14, item(net.fakepixel.housing.util.Compat.mat("EMERALD"),
                "&aItems",
                "&7Browse items you have unlocked.",
                "",
                "&aVIP Items: &a6/6 &8(100%)",
                "",
                "&eClick to browse!"));
        inv.setItem(15, item(net.fakepixel.housing.util.Compat.mat("RED_BED"),
                "&aFurniture",
                "&7Browse furniture you have unlocked.",
                "",
                "&7Unlock more by spending &6Gold &7at the",
                "&7In-Game Store!",
                "",
                "&7Unlocked: &c0/0 &8(0%)",
                "",
                "&cUnavailable!"));

        inv.setItem(18, item(legacyMat("DARK_OAK_DOOR_ITEM"),
                "&aTravel to someone else's house",
                "&7Hop into the hot air balloon and",
                "&7visit other people's houses.",
                "",
                "&eClick to view houses you can join!"));
        inv.setItem(19, item(net.fakepixel.housing.util.Compat.mat("COMPASS"),
                "&aGame Menu",
                "&7Choose any Fakepixel lobby to warp to.",
                "",
                "&eClick to open!"));
        inv.setItem(20, item(net.fakepixel.housing.util.Compat.mat("BLACK_CONCRETE"),
                "&aVisiting Rules",
                "&7Allows you to select who",
                "&7can visit your home.",
                "",
                "&6Alias: &7/visitingrule <rule>",
                "&eClick to edit Visiting Rules!"));
        inv.setItem(21, item(net.fakepixel.housing.util.Compat.mat("REDSTONE_TORCH"),
                "&aHouse Settings",
                "&7Allows you to edit and control",
                "&7various settings for your house.",
                "",
                "&eClick to view!"));
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("FILLED_MAP"),
                "&aPermissions and Groups",
                "&7Allows you to edit and control",
                "&7groups and their permissions.",
                "",
                "&eClick to edit!"));
        inv.setItem(23, item(net.fakepixel.housing.util.Compat.mat("WRITABLE_BOOK"),
                "&aPlayer Listing",
                "&7List all players currently in this house.",
                "",
                "&6Alias: &7/housing players",
                "&eClick to view Player Listing!"));
        inv.setItem(24, item(net.fakepixel.housing.util.Compat.mat("JUKEBOX"),
                "&aJukebox",
                "&7Allows you to select the",
                "&7music playing in your home,",
                "&7as well as control it.",
                "",
                "&aVIP Only!",
                "",
                "&eClick to select!"));

        inv.setItem(40, item(net.fakepixel.housing.util.Compat.mat("BARRIER"),
                "&cRemove Carpenter",
                "&7Remove the Carpenter NPC",
                "&7from your house.",
                "",
                "&eClick to remove!"));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"),
                "&aMain Menu",
                "&7Go back to the Housing Menu."));

        player.openInventory(inv);
    }

    private void openRemoveCarpenterConfirm(Player player, House house) {
        if (house == null) {
            player.closeInventory();
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, carpenterRemoveTitle);
        inv.setItem(11, item(net.fakepixel.housing.util.Compat.limeDye(),
                "&aConfirm remove",
                "&7Remove the Carpenter NPC",
                "&7from your house.",
                "",
                "&eClick to confirm!"));
        inv.setItem(15, item(net.fakepixel.housing.util.Compat.redDye(),
                "&cCancel",
                "&7Return to the Carpenter menu.",
                "",
                "&eClick to cancel!"));
        player.openInventory(inv);
    }



    private void openWeather(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !house.canManage(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage(plugin.color("&cOnly the owner/co-owner can change the weather."));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, weatherTitle);
        String current = house.getWeather();
        inv.setItem(11, weatherItem("sunny", "&aSunny", net.fakepixel.housing.util.Compat.mat("SUNFLOWER"), current));
        inv.setItem(12, weatherItem("raining", "&aRaining", net.fakepixel.housing.util.Compat.mat("WATER_BUCKET"), current));
        inv.setItem(14, weatherItem("storming", "&aStorming", net.fakepixel.housing.util.Compat.mat("FIRE_CHARGE"), current));
        inv.setItem(15, weatherItem("thunder", "&aThunder", net.fakepixel.housing.util.Compat.mat("BLAZE_ROD"), current));
        inv.setItem(20, weatherItem("sunny", "&aSunny", net.fakepixel.housing.util.Compat.limeDye(), current));
        inv.setItem(21, item(net.fakepixel.housing.util.Compat.mat("CLOCK"), "&7Weather Status", "&7Current: &f" + prettyWeather(current)));
        inv.setItem(23, weatherItem("raining", "&aRaining", net.fakepixel.housing.util.Compat.grayDye(), current));
        inv.setItem(24, weatherItem("storming", "&aStorming", net.fakepixel.housing.util.Compat.grayDye(), current));
        inv.setItem(31, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&aMain Menu", "&7Go back to the Housing Menu."));
        player.openInventory(inv);
    }

    private ItemStack weatherItem(String weather, String name, Material material, String current) {
        return weatherItem(weather, name, new ItemStack(material), current);
    }

    private ItemStack weatherItem(String weather, String name, ItemStack stack, String current) {
        boolean selected = weather.equalsIgnoreCase(current);
        return item(stack, name,
                selected ? "&aCurrently selected!" : "&7Change your house weather.",
                "",
                "&6Alias: &7/weather <weather>",
                selected ? "&7Already active." : "&eClick to select!");
    }

    private String prettyWeather(String weather) {
        if (weather == null) return "Sunny";
        switch (weather.toLowerCase(java.util.Locale.ENGLISH)) {
            case "raining": return "Raining";
            case "storming": return "Storming";
            case "thunder": return "Thunder";
            default: return "Sunny";
        }
    }

    private void setHouseWeather(Player player, String weather) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !house.canManage(player.getUniqueId())) return;
        house.setWeather(weather);
        applyHouseWeather(house);
        houses.saveAll();
        player.sendMessage(plugin.color("&aSet house weather to &e" + prettyWeather(house.getWeather()) + "&a."));
        openWeather(player);
    }

    public void applyHouseWeather(House house) {
        if (house == null) return;
        org.bukkit.World world = Bukkit.getWorld(house.getWorldName());
        if (world == null) return;
        String weather = house.getWeather();
        if ("raining".equalsIgnoreCase(weather)) {
            world.setStorm(true);
            world.setThundering(false);
        } else if ("storming".equalsIgnoreCase(weather)) {
            world.setStorm(true);
            world.setThundering(false);
        } else if ("thunder".equalsIgnoreCase(weather)) {
            world.setStorm(true);
            world.setThundering(true);
        } else {
            world.setStorm(false);
            world.setThundering(false);
        }
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setThunderDuration(Integer.MAX_VALUE);
    }

    public void applyHouseTime(House house) {
        if (house == null || house.getTimeTicks() < 0L) return;
        org.bukkit.World world = Bukkit.getWorld(house.getWorldName());
        if (world != null) world.setTime(house.getTimeTicks());
    }

    private void openBiomesAndSkies(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !house.canManage(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage(plugin.color("&cOnly the owner/co-owner can change biomes and skies."));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, biomeSkyTitle);
        String currentBiome = house.getBiome();
        String currentSky = house.getSky();

        inv.setItem(0, biomeSkyItem("jungle", "&aJungle", net.fakepixel.housing.util.Compat.mat("LEAVES"), currentBiome, true));
        inv.setItem(1, biomeSkyItem("roofed-forest", "&aRoofed Forest", net.fakepixel.housing.util.Compat.mat("LEAVES"), currentBiome, true));
        inv.setItem(2, biomeSkyItem("forest", "&aForest", net.fakepixel.housing.util.Compat.mat("LOG"), currentBiome, true));
        inv.setItem(3, biomeSkyItem("mesa", "&eMesa", net.fakepixel.housing.util.Compat.mat("SAND"), currentBiome, true));
        inv.setItem(4, biomeSkyItem("cold-taiga", "&eCold Taiga", net.fakepixel.housing.util.Compat.mat("SNOW_BLOCK"), currentBiome, true));
        inv.setItem(5, biomeSkyItem("hell", "&cHell", net.fakepixel.housing.util.Compat.mat("NETHERRACK"), currentBiome, true));
        inv.setItem(6, biomeSkyItem("sky", "&eSky", net.fakepixel.housing.util.Compat.mat("END_STONE"), currentBiome, true));
        inv.setItem(7, biomeSkyItem("taiga", "&aTaiga", net.fakepixel.housing.util.Compat.mat("SAPLING"), currentBiome, true));
        inv.setItem(8, biomeSkyItem("desert", "&eDesert", net.fakepixel.housing.util.Compat.mat("SAND"), currentBiome, true));

        inv.setItem(9, biomeSkyItem("plains", "&aPlains", net.fakepixel.housing.util.Compat.mat("GRASS"), currentBiome, true));
        inv.setItem(10, biomeSkyItem("forest", "&aForest", net.fakepixel.housing.util.Compat.mat("SAPLING"), currentBiome, true));
        inv.setItem(11, biomeSkyItem("taiga", "&aTaiga", net.fakepixel.housing.util.Compat.mat("SNOW_BALL"), currentBiome, true));
        inv.setItem(12, biomeSkyItem("desert", "&eDesert", net.fakepixel.housing.util.Compat.mat("CACTUS"), currentBiome, true));
        inv.setItem(13, biomeSkyItem("jungle", "&aJungle", net.fakepixel.housing.util.Compat.mat("VINE"), currentBiome, true));
        inv.setItem(14, biomeSkyItem("mesa", "&eMesa", net.fakepixel.housing.util.Compat.mat("STAINED_CLAY"), currentBiome, true));
        inv.setItem(15, biomeSkyItem("cold-taiga", "&eCold Taiga", net.fakepixel.housing.util.Compat.mat("SNOW"), currentBiome, true));

        inv.setItem(21, skyItem("overworld", "&aOverworld", legacyItem("STAINED_GLASS_PANE", 5), currentSky));
        inv.setItem(22, skyItem("nether", "&cNether", legacyItem("STAINED_GLASS_PANE", 14), currentSky));
        inv.setItem(23, skyItem("end", "&dThe End", legacyItem("STAINED_GLASS_PANE", 2), currentSky));

        inv.setItem(31, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&aMain Menu", "&7Go back to the Housing Menu."));
        player.openInventory(inv);
    }

    private ItemStack biomeSkyItem(String key, String name, Material material, String current, boolean biome) {
        boolean selected = key.equalsIgnoreCase(current);
        return item(material, name,
                selected ? "&aCurrently selected!" : "",
                biome ? "&6Alias: &7/setbiome <biome>" : "",
                selected ? "&7Already active." : "&eClick to select!");
    }

    private ItemStack skyItem(String key, String name, ItemStack stack, String current) {
        boolean selected = key.equalsIgnoreCase(current);
        return item(stack, name,
                selected ? "&aCurrently selected!" : "&7Change your house sky.",
                selected ? "&7Already active." : "&eClick to select!");
    }

    private String biomeBySlot(int slot) {
        switch (slot) {
            case 0: case 13: return "jungle";
            case 1: return "roofed-forest";
            case 2: case 10: return "forest";
            case 3: case 14: return "mesa";
            case 4: case 15: return "cold-taiga";
            case 5: return "hell";
            case 6: return "sky";
            case 7: case 11: return "taiga";
            case 8: case 12: return "desert";
            case 9: return "plains";
            default: return null;
        }
    }

    private String skyBySlot(int slot) {
        switch (slot) {
            case 21: return "overworld";
            case 22: return "nether";
            case 23: return "end";
            default: return null;
        }
    }

    private void setHouseBiome(Player player, String biome) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !house.canManage(player.getUniqueId())) return;
        house.setBiome(biome);
        applyHouseBiomeSky(house);
        houses.saveAll();
        player.sendMessage(plugin.color("&aSet house biome to &e" + prettyBiome(house.getBiome()) + "&a."));
        openBiomesAndSkies(player);
    }

    private void setHouseSky(Player player, String sky) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !house.canManage(player.getUniqueId())) return;
        house.setSky(sky);
        applyHouseBiomeSky(house);
        houses.saveAll();
        player.sendMessage(plugin.color("&aSet house sky to &e" + prettySky(house.getSky()) + "&a."));
        openBiomesAndSkies(player);
    }

    public void applyHouseBiomeSky(House house) {
        if (house == null) return;
        org.bukkit.World world = Bukkit.getWorld(house.getWorldName());
        if (world == null) return;
        applyBiomeToHousePlot(world, house.getBiome());
    }

    @SuppressWarnings("deprecation")
    private void applyBiomeToHousePlot(org.bukkit.World world, String biomeKey) {
        Object biome = findBiome(biomeKey);
        if (biome == null) return;
        House house = houses.getHouseByWorld(world.getName());
        HouseManager.BorderBounds bounds = houses.borderBounds(house);
        int minX = bounds.minX;
        int maxX = bounds.maxX;
        int minZ = bounds.minZ;
        int maxZ = bounds.maxZ;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) setBiomeCompat(world, x, z, biome);
        }
        for (int cx = (minX >> 4); cx <= (maxX >> 4); cx++) {
            for (int cz = (minZ >> 4); cz <= (maxZ >> 4); cz++) {
                try { world.refreshChunk(cx, cz); } catch (Throwable ignored) {}
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object findBiome(String key) {
        try {
            Class enumClass = Class.forName("org.bukkit.block.Biome");
            for (String name : biomeNames(key)) {
                try { return Enum.valueOf(enumClass, name); } catch (IllegalArgumentException ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private String[] biomeNames(String key) {
        if (key == null) return new String[] {"PLAINS"};
        switch (key.toLowerCase(java.util.Locale.ENGLISH)) {
            case "jungle": return new String[] {"JUNGLE"};
            case "roofed-forest": return new String[] {"ROOFED_FOREST", "DARK_FOREST"};
            case "forest": return new String[] {"FOREST"};
            case "mesa": return new String[] {"MESA", "BADLANDS"};
            case "cold-taiga": return new String[] {"COLD_TAIGA", "SNOWY_TAIGA", "TAIGA_COLD"};
            case "hell": return new String[] {"HELL", "NETHER", "NETHER_WASTES"};
            case "sky": return new String[] {"SKY", "THE_END", "END_HIGHLANDS"};
            case "taiga": return new String[] {"TAIGA"};
            case "desert": return new String[] {"DESERT"};
            default: return new String[] {"PLAINS"};
        }
    }

    private void setBiomeCompat(org.bukkit.World world, int x, int z, Object biome) {
        try {
            Class<?> biomeClass = Class.forName("org.bukkit.block.Biome");
            try {
                java.lang.reflect.Method method = world.getClass().getMethod("setBiome", int.class, int.class, biomeClass);
                method.invoke(world, x, z, biome);
                return;
            } catch (NoSuchMethodException ignored) {}
            try {
                java.lang.reflect.Method method = world.getClass().getMethod("setBiome", int.class, int.class, int.class, biomeClass);
                method.invoke(world, x, 64, z, biome);
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable ignored) {}
    }

    private String prettyBiome(String biome) {
        if (biome == null) return "Plains";
        switch (biome.toLowerCase(java.util.Locale.ENGLISH)) {
            case "jungle": return "Jungle";
            case "roofed-forest": return "Roofed Forest";
            case "forest": return "Forest";
            case "mesa": return "Mesa";
            case "cold-taiga": return "Cold Taiga";
            case "hell": return "Hell";
            case "sky": return "Sky";
            case "taiga": return "Taiga";
            case "desert": return "Desert";
            default: return "Plains";
        }
    }

    private String prettySky(String sky) {
        if (sky == null) return "Overworld";
        switch (sky.toLowerCase(java.util.Locale.ENGLISH)) {
            case "nether": return "Nether";
            case "end": return "The End";
            default: return "Overworld";
        }
    }


    private void loadThemes() { }

    private void loadThemeUnlocks() { }

    private void saveThemeUnlocks() { }

    private List<ThemeDefinition> themes() {
        List<ThemeDefinition> themes = new ArrayList<>();
        for (net.fakepixel.housing.theme.ThemeDefinition child : ThemeRegistry.all()) {
            themes.add(new ThemeDefinition(
                    child.getId(), child.getDisplayName(), child.getIcon(), child.getUnlockType(), child.getPrice(),
                    child.getPermission(), child.getTemplate().getFolderName(), child.getBiome(), child.getSky(), child.getWeather(), child.getLore()
            ));
        }
        return themes;
    }

    private ThemeDefinition themeBySlot(Player player, int slot) {
        if (slot < 0 || slot >= themeItemsPerPage()) return null;
        int page = safeThemePage(player);
        int index = page * themeItemsPerPage() + slot;
        List<ThemeDefinition> themes = themes();
        return index >= 0 && index < themes.size() ? themes.get(index) : null;
    }

    private int themeItemsPerPage() {
        return 18;
    }

    private int maxThemePage() {
        int total = themes().size();
        return Math.max(0, (total - 1) / themeItemsPerPage());
    }

    private int safeThemePage(Player player) {
        int page = themePages.getOrDefault(player.getUniqueId(), 0);
        int max = maxThemePage();
        page = Math.max(0, Math.min(page, max));
        themePages.put(player.getUniqueId(), page);
        return page;
    }

    private boolean isThemeUnlocked(Player player, ThemeDefinition theme) {
        if (theme == null) return false;
        if ("FREE".equals(theme.unlockType)) return true;
        if ("PERMISSION".equals(theme.unlockType)) return theme.permission != null && !theme.permission.trim().isEmpty() && player.hasPermission(theme.permission.trim());
        if (!"GOLD".equals(theme.unlockType)) return false;
        try { return plugin.getHousingRepository() != null && plugin.getHousingRepository().hasUnlock(player.getUniqueId(), "theme", theme.id); }
        catch (Exception ex) { plugin.getLogger().warning("Could not read theme unlock: " + ex.getMessage()); return false; }
    }

    private void unlockTheme(Player player, ThemeDefinition theme) {
        try { if (plugin.getHousingRepository() != null) plugin.getHousingRepository().saveUnlock(player.getUniqueId(), "theme", theme.id); }
        catch (Exception ex) { plugin.getLogger().warning("Could not save theme unlock: " + ex.getMessage()); }
    }

    private void handleThemeClick(Player player, ThemeDefinition theme) {
        if (theme == null) return;
        if (isThemeUnlocked(player, theme)) {
            setHouseTheme(player, theme);
            return;
        }
        if (!"GOLD".equals(theme.unlockType)) {
            player.sendMessage(plugin.color("&cThat theme is locked."));
            openThemes(player);
            return;
        }
        if (plugin.getHousingRepository() == null) { player.sendMessage(plugin.color("&cPersistent housing data is unavailable.")); return; }
        if (!themeTemplateExists(theme)) {
            player.sendMessage(plugin.color("&cThat theme template is missing. Add &e" + themeStorageFolder() + "/" + templateFolderName(theme) + ".slime &cand reload."));
            openThemes(player);
            return;
        }
        if (!abelix.takeGold(player, theme.price)) {
            player.sendMessage(plugin.color("&cYou do not have enough Gold to unlock that theme."));
            openThemes(player);
            return;
        }
        unlockTheme(player, theme);
        player.sendMessage(plugin.color("&aUnlocked &e" + ChatColor.stripColor(plugin.color(theme.displayName)) + "&a!"));
        setHouseTheme(player, theme);
    }

    private void openThemes(Player player) {
        openThemes(player, safeThemePage(player));
    }

    private void openThemes(Player player, int page) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !house.canManage(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage(plugin.color("&cOnly the owner/co-owner can change themes."));
            return;
        }
        int maxPage = maxThemePage();
        int safePage = Math.max(0, Math.min(page, maxPage));
        themePages.put(player.getUniqueId(), safePage);
        Inventory inv = Bukkit.createInventory(null, 27, themesTitle);
        String current = house.getTheme();
        List<ThemeDefinition> themes = themes();
        int start = safePage * themeItemsPerPage();
        int end = Math.min(themes.size(), start + themeItemsPerPage());
        for (int index = start; index < end; index++) {
            inv.setItem(index - start, themeItem(player, themes.get(index), current));
        }
        if (safePage > 0) inv.setItem(18, item(net.fakepixel.housing.util.Compat.grayDye(), "&7Previous Page", "&7Page &f" + safePage + " &7of &f" + (maxPage + 1), "", "&eClick to go back!"));
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&aMain Menu", "&7Go back to the Housing Menu."));
        inv.setItem(23, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cClose", "&7Close this menu."));
        if (safePage < maxPage) inv.setItem(26, item(net.fakepixel.housing.util.Compat.limeDye(), "&aNext Page", "&7Page &f" + (safePage + 2) + " &7of &f" + (maxPage + 1), "", "&eClick to continue!"));
        player.openInventory(inv);
    }

    private ItemStack themeItem(Player player, ThemeDefinition theme, String current) {
        boolean selected = theme.id.equalsIgnoreCase(current);
        boolean unlocked = isThemeUnlocked(player, theme);
        List<String> lore = new ArrayList<>(theme.lore);
        lore.add("&7Biome: &f" + prettyBiome(theme.biome));
        lore.add("&7Sky: &f" + prettySky(theme.sky));
        lore.add("&7Weather: &f" + prettyWeather(theme.weather));
        lore.add("");
        lore.add("&c&lWARNING! &cSetting a theme will reset");
        lore.add("&cSome of your plot!");
        lore.add("");
        if (selected) lore.add("&aCurrently selected!");
        else if (unlocked) lore.add("&eClick to select!");
        else if ("GOLD".equals(theme.unlockType)) {
            lore.add("&7Cost: &6" + theme.price + " Gold");
            lore.add("&eClick to unlock!");
        } else if ("PERMISSION".equals(theme.unlockType)) {
            lore.add("&cRequires permission.");
        } else {
            lore.add("&cLocked.");
        }
        return item(iconStack(theme.icon), theme.displayName, lore.toArray(new String[0]));
    }

    private ItemStack iconStack(String icon) {
        Material material = org.bukkit.Material.matchMaterial(icon == null ? "" : icon.trim().toUpperCase(java.util.Locale.ENGLISH));
        if (material == null) material = net.fakepixel.housing.util.Compat.mat(icon == null ? "GRASS" : icon.trim().toUpperCase(java.util.Locale.ENGLISH));
        return new ItemStack(material == null ? net.fakepixel.housing.util.Compat.mat("GRASS_BLOCK") : material);
    }

    private void setHouseTheme(Player player, ThemeDefinition theme) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !house.canManage(player.getUniqueId())) return;
        if (applyingThemes.contains(player.getUniqueId())) {
            player.sendMessage(plugin.color("&eA theme is already applying. Please wait."));
            return;
        }
        if (applyingThemes.size() >= net.fakepixel.housing.cluster.HousingPerformanceRegistry.MAX_ACTIVE_THEME_PASTES) {
            player.sendMessage(plugin.color("&eThis instance is already applying the maximum number of themes. Please try again shortly."));
            return;
        }
        if (isSelectedTheme(house, theme)) {
            player.sendMessage(plugin.color("&eThis theme is already selected."));
            openThemes(player);
            return;
        }
        if (!themeTemplateExists(theme)) {
            player.sendMessage(plugin.color("&cThat theme template is missing. Add &e" + themeStorageFolder() + "/" + templateFolderName(theme) + ".slime &cand reload."));
            openThemes(player);
            return;
        }
        applyingThemes.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(plugin.color("&eApplying theme &6" + ChatColor.stripColor(plugin.color(theme.displayName)) + "&e..."));
        try {
            startThemeOutsideBorderApply(player, house, theme);
        } catch (Exception ex) {
            player.sendMessage(plugin.color("&cCould not apply theme: " + ex.getMessage()));
            plugin.getLogger().warning("Could not apply housing theme " + theme.id + " for " + player.getName() + ": " + ex.getMessage());
            applyingThemes.remove(player.getUniqueId());
            houses.updateVisitItems(player);
            openThemes(player);
        }
    }

    public boolean isThemeApplying(Player player) {
        return player != null && applyingThemes.contains(player.getUniqueId());
    }

    private void applyThemeSettings(House house, ThemeDefinition theme) {
        house.setSky(theme.sky);
        house.setWeather(theme.weather);
        house.setBiome(theme.biome);
    }

    private boolean isSelectedTheme(House house, ThemeDefinition theme) {
        return house != null && theme != null && house.getTheme() != null && house.getTheme().equalsIgnoreCase(theme.id);
    }

    private ThemeDefinition themeById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        for (ThemeDefinition theme : themes()) {
            if (theme.id.equalsIgnoreCase(id.trim())) return theme;
        }
        return null;
    }

    private String prettyTheme(String theme) {
        if (theme == null) return "Default";
        switch (theme.toLowerCase(java.util.Locale.ENGLISH)) {
            case "desert": return "Desert";
            case "mesa": return "Mesa";
            case "jungle": return "Jungle";
            case "forest": return "Forest";
            case "snow": return "Snow";
            case "air": return "Air";
            case "basic": return "Basic";
            case "scifi": return "Sci Fi";
            default: return "Default";
        }
    }

    private boolean themeTemplateExists(ThemeDefinition theme) {
        java.io.File folder = new java.io.File(themeStorageFolder());
        String name = templateFolderName(theme);
        return new java.io.File(folder, name + ".slime").exists()
                || new java.io.File(folder, name + ".slime.gz").exists()
                || new java.io.File(folder, name + ".swm").exists()
                || new java.io.File(folder, name + ".dat").exists();
    }

    private String templateFolderName(ThemeDefinition theme) {
        if (theme == null) return "default";
        String raw = theme.slimeworld == null || theme.slimeworld.trim().isEmpty() ? theme.id : theme.slimeworld.trim();
        return cleanThemeWorldName(raw);
    }

    private String cleanThemeWorldName(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "default";
        int slash = Math.max(raw.lastIndexOf('/'), raw.lastIndexOf('\\'));
        if (slash >= 0) raw = raw.substring(slash + 1);
        int dot = raw.lastIndexOf('.');
        if (dot > 0) raw = raw.substring(0, dot);
        String clean = raw.trim();
        if (clean.endsWith(".slime")) clean = clean.substring(0, clean.length() - ".slime".length());
        if (clean.endsWith(".swm")) clean = clean.substring(0, clean.length() - ".swm".length());
        if (clean.endsWith(".dat")) clean = clean.substring(0, clean.length() - ".dat".length());
        if (clean.endsWith(".slime.gz")) clean = clean.substring(0, clean.length() - ".slime.gz".length());
        return clean.replaceAll("[^A-Za-z0-9_-]", "");
    }

    private String themeStorageFolder() {
        return plugin.getStaticConfig().getString("slimeworldmanager.storage-folder", "slime_worlds");
    }

    private void startThemeOutsideBorderApply(Player player, House house, ThemeDefinition theme) {
        org.bukkit.World target = Bukkit.getWorld(house.getWorldName());
        if (target == null) throw new IllegalStateException("House world is not loaded.");
        if (!(plugin.getWorldService() instanceof net.fakepixel.housing.world.SlimeWorldManagerService)) {
            throw new IllegalStateException("Theme files require SlimeWorldManager/AdvancedSlimePaper.");
        }
        net.fakepixel.housing.world.SlimeWorldManagerService service = (net.fakepixel.housing.world.SlimeWorldManagerService) plugin.getWorldService();
        ThemeCopyBounds bounds = themeCopyBounds(house);
        String templateName = templateFolderName(theme);
        java.io.File templateFile = themeTemplateFile(templateName);
        long lastModified = templateFile == null ? 0L : templateFile.lastModified();
        String cacheKey = themeCacheKey(templateName, bounds);
        ThemeBlockCache cache = cachedThemeBlocks(cacheKey, lastModified);

        if (cache != null) {
            houses.clearPlotBorder(target);
            new ThemeApplyTask(player.getUniqueId(), house, theme, null, target, service, bounds, cache, cacheKey, lastModified).runTaskTimer(plugin, 1L, 1L);
            return;
        }
        org.bukkit.World source = service.loadThemeWorld(templateName);
        houses.clearPlotBorder(target);
        new ThemeApplyTask(player.getUniqueId(), house, theme, source, target, service, bounds, null, cacheKey, lastModified).runTaskTimer(plugin, 1L, 1L);
    }

    @SuppressWarnings("deprecation")
    private boolean copyThemeBlock(org.bukkit.World source, org.bukkit.World target, int x, int y, int z, ThemeCopyBounds bounds) {
        org.bukkit.block.Block sourceBlock = source.getBlockAt(x, y, z);
        if (bounds.ignoreAir && sourceBlock.getType() == net.fakepixel.housing.util.Compat.mat("AIR")) return false;
        org.bukkit.block.Block targetBlock = target.getBlockAt(x, y, z);
        if (bounds.onlyChangeDifferentBlocks && sourceBlock.getTypeId() == targetBlock.getTypeId() && sourceBlock.getData() == targetBlock.getData()) return false;
        targetBlock.setTypeIdAndData(sourceBlock.getTypeId(), sourceBlock.getData(), false);
        copyBlockState(sourceBlock, targetBlock);
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean clearThemeBlock(org.bukkit.World target, int x, int y, int z) {
        org.bukkit.block.Block targetBlock = target.getBlockAt(x, y, z);
        if (targetBlock.getType() == net.fakepixel.housing.util.Compat.mat("AIR")) return false;
        targetBlock.setTypeIdAndData(net.fakepixel.housing.util.Compat.mat("AIR").getId(), (byte) 0, false);
        return true;
    }

    private ThemeCopyBounds themeCopyBounds(House house) {
        int rawMinX = plugin.getStaticConfig().getInt("theme-apply.min-x", plugin.getStaticConfig().getInt("theme-copy.min-x", plugin.getStaticConfig().getInt("template-copy.min-x", -128)));
        int rawMaxX = plugin.getStaticConfig().getInt("theme-apply.max-x", plugin.getStaticConfig().getInt("theme-copy.max-x", plugin.getStaticConfig().getInt("template-copy.max-x", 128)));
        int rawMinZ = plugin.getStaticConfig().getInt("theme-apply.min-z", plugin.getStaticConfig().getInt("theme-copy.min-z", plugin.getStaticConfig().getInt("template-copy.min-z", -128)));
        int rawMaxZ = plugin.getStaticConfig().getInt("theme-apply.max-z", plugin.getStaticConfig().getInt("theme-copy.max-z", plugin.getStaticConfig().getInt("template-copy.max-z", 128)));
        int minY = Math.max(0, plugin.getStaticConfig().getInt("theme-apply.min-y", plugin.getStaticConfig().getInt("theme-copy.min-y", plugin.getStaticConfig().getInt("template-copy.min-y", 0))));
        int maxY = Math.min(255, plugin.getStaticConfig().getInt("theme-apply.max-y", Math.min(160, plugin.getStaticConfig().getInt("template-copy.max-y", 160))));
        int batchSize = net.fakepixel.housing.cluster.HousingPerformanceRegistry.THEME_BLOCKS_PER_TICK;
        int scanLimit = Math.max(batchSize, plugin.getStaticConfig().getInt("theme-apply.max-scanned-blocks-per-tick", Math.max(5000, batchSize * 100)));
        boolean useTemplateCache = plugin.getStaticConfig().getBoolean("theme-apply.use-template-cache", true) && plugin.getStaticConfig().getBoolean("theme-apply.cache-theme-blocks", true);
        boolean alignFromPlotCenter = plugin.getStaticConfig().getBoolean("theme-apply.align-from-plot-center", plugin.getStaticConfig().getBoolean("theme-apply.align-template-origin-to-house-origin", true));
        boolean pasteByChunks = plugin.getStaticConfig().getBoolean("theme-apply.paste-by-chunks", true);
        int chunkBatchSize = Math.max(1, plugin.getStaticConfig().getInt("theme-apply.chunk-batch-size", 8));
        int batchRadius = Math.max(1, plugin.getStaticConfig().getInt("theme-apply.batch-radius", 8));
        boolean updateLightingAfter = plugin.getStaticConfig().getBoolean("theme-apply.update-lighting-after", true);
        boolean redrawBorderAfterApply = plugin.getStaticConfig().getBoolean("theme-apply.redraw-border-after-apply", true);
        boolean skipProtectedPlot = plugin.getStaticConfig().getBoolean("theme-apply.skip-protected-plot", true);
        boolean skipRedBorder = plugin.getStaticConfig().getBoolean("theme-apply.skip-red-border", true);
        boolean skipRedBorderBlocksOnly = plugin.getStaticConfig().getBoolean("theme-apply.skip-red-border-blocks-only", true);
        boolean protectPlayerBuildBlocks = plugin.getStaticConfig().getBoolean("theme-apply.protect-player-build-blocks", true);
        boolean useFixedProtectedBuildRegion = plugin.getStaticConfig().getBoolean("theme-apply.use-fixed-protected-build-region", true);
        boolean useUpgradedPlotSizeAsThemeMask = plugin.getStaticConfig().getBoolean("theme-apply.use-upgraded-plot-size-as-theme-mask", false);
        boolean protectEntireBorderBox = plugin.getStaticConfig().getBoolean("theme-apply.protect-entire-border-box", false);
        boolean pasteThemeInsideBorderArea = plugin.getStaticConfig().getBoolean("theme-apply.paste-theme-inside-border-area", true);
        boolean alignTemplateOriginToHouseOrigin = plugin.getStaticConfig().getBoolean("theme-apply.align-template-origin-to-house-origin", alignFromPlotCenter);
        boolean clearOldThemeBeforeApply = plugin.getStaticConfig().getBoolean("theme-apply.clear-old-theme-before-apply", true);
        boolean pasteFullTemplateRegion = plugin.getStaticConfig().getBoolean("theme-apply.paste-full-template-region", true);
        boolean onlyChangeDifferentBlocks = plugin.getStaticConfig().getBoolean("theme-apply.only-change-different-blocks", false);
        boolean ignoreAir = plugin.getStaticConfig().getBoolean("theme-apply.ignore-air", false);
        HouseManager.BorderBounds bounds = houses.borderBounds(house);
        int borderY = houses.borderY(house);
        org.bukkit.Location houseOrigin = house.getOrigin();
        int houseOriginX = houseOrigin.getBlockX();
        int houseOriginZ = houseOrigin.getBlockZ();
        HouseManager.BorderBounds themeProtectBounds = useUpgradedPlotSizeAsThemeMask
                ? bounds
                : (useFixedProtectedBuildRegion ? fixedProtectedBuildBounds() : bounds);
        List<HouseManager.BorderBounds> borderSkipBounds = houses.knownBorderBounds(house);
        if (!borderSkipBounds.contains(bounds)) borderSkipBounds.add(bounds);
        int templateOriginX = plugin.getStaticConfig().getInt("theme-apply.template-origin-x", 0);
        int templateOriginZ = plugin.getStaticConfig().getInt("theme-apply.template-origin-z", 0);
        int minX = alignFromPlotCenter ? houseOriginX + rawMinX - templateOriginX : rawMinX;
        int maxX = alignFromPlotCenter ? houseOriginX + rawMaxX - templateOriginX : rawMaxX;
        int minZ = alignFromPlotCenter ? houseOriginZ + rawMinZ - templateOriginZ : rawMinZ;
        int maxZ = alignFromPlotCenter ? houseOriginZ + rawMaxZ - templateOriginZ : rawMaxZ;
        long totalPositions = ((long) Math.max(0, Math.max(minX, maxX) - Math.min(minX, maxX) + 1))
                * Math.max(0, maxY - minY + 1)
                * Math.max(0, Math.max(minZ, maxZ) - Math.min(minZ, maxZ) + 1);
        return new ThemeCopyBounds(Math.min(minX, maxX), Math.max(minX, maxX), minY, maxY, Math.min(minZ, maxZ), Math.max(minZ, maxZ), bounds, borderSkipBounds, themeProtectBounds, borderY, houseOriginX, houseOriginZ, templateOriginX, templateOriginZ, batchSize, scanLimit, useTemplateCache, alignFromPlotCenter, pasteByChunks, chunkBatchSize, batchRadius, updateLightingAfter, redrawBorderAfterApply, skipProtectedPlot, skipRedBorder, skipRedBorderBlocksOnly, protectPlayerBuildBlocks, protectEntireBorderBox, pasteThemeInsideBorderArea, alignTemplateOriginToHouseOrigin, clearOldThemeBeforeApply, pasteFullTemplateRegion, onlyChangeDifferentBlocks, ignoreAir, totalPositions);
    }

    private java.io.File themeTemplateFile(String templateName) {
        java.io.File folder = new java.io.File(themeStorageFolder());
        String[] extensions = {".slime", ".slime.gz", ".swm", ".dat"};
        for (String extension : extensions) {
            java.io.File file = new java.io.File(folder, templateName + extension);
            if (file.exists()) return file;
        }
        return null;
    }

    private String themeCacheKey(String templateName, ThemeCopyBounds bounds) {
        return "tracked-v2|" + templateName + "|"
                + bounds.minX + ":" + bounds.maxX + ":" + bounds.minY + ":" + bounds.maxY + ":" + bounds.minZ + ":" + bounds.maxZ + "|"
                + bounds.border.minX + ":" + bounds.border.maxX + ":" + bounds.border.minZ + ":" + bounds.border.maxZ + ":" + bounds.borderY + ":"
                + borderBoundsCacheKey(bounds.borderSkipBounds) + ":"
                + bounds.themeProtectBounds.minX + ":" + bounds.themeProtectBounds.maxX + ":" + bounds.themeProtectBounds.minZ + ":" + bounds.themeProtectBounds.maxZ + ":"
                + bounds.houseOriginX + ":" + bounds.houseOriginZ + ":" + bounds.templateOriginX + ":" + bounds.templateOriginZ + "|"
                + bounds.alignFromPlotCenter + ":" + bounds.skipProtectedPlot + ":" + bounds.skipRedBorder + ":" + bounds.skipRedBorderBlocksOnly + ":"
                + bounds.protectPlayerBuildBlocks + ":" + bounds.protectEntireBorderBox + ":" + bounds.pasteThemeInsideBorderArea + ":"
                + bounds.alignTemplateOriginToHouseOrigin + ":" + bounds.clearOldThemeBeforeApply + ":" + bounds.pasteFullTemplateRegion + ":"
                + bounds.onlyChangeDifferentBlocks + ":" + bounds.ignoreAir;
    }

    private ThemeBlockCache cachedThemeBlocks(String cacheKey, long lastModified) {
        if (!plugin.getStaticConfig().getBoolean("theme-apply.use-template-cache", true) || !plugin.getStaticConfig().getBoolean("theme-apply.cache-theme-blocks", true)) return null;
        ThemeBlockCache cache = themeTemplateCache.get(cacheKey);
        if (cache == null) return null;
        if (cache.lastModified != lastModified) {
            themeTemplateCache.remove(cacheKey);
            return null;
        }
        return cache;
    }

    private String borderBoundsCacheKey(List<HouseManager.BorderBounds> borderBounds) {
        StringBuilder builder = new StringBuilder();
        for (HouseManager.BorderBounds bounds : borderBounds) {
            if (builder.length() > 0) builder.append(';');
            builder.append(bounds.minX).append(',').append(bounds.maxX).append(',').append(bounds.minZ).append(',').append(bounds.maxZ);
        }
        return builder.toString();
    }

    private void storeThemeBlockCache(String cacheKey, ThemeBlockCache cache) {
        if (!plugin.getStaticConfig().getBoolean("theme-apply.use-template-cache", true) || !plugin.getStaticConfig().getBoolean("theme-apply.cache-theme-blocks", true)) return;
        themeTemplateCache.put(cacheKey, cache);
        int maxCached = Math.max(1, plugin.getStaticConfig().getInt("theme-apply.max-cached-themes", 3));
        while (themeTemplateCache.size() > maxCached) {
            String oldest = themeTemplateCache.keySet().iterator().next();
            themeTemplateCache.remove(oldest);
        }
    }

    private boolean shouldSkipThemeBlock(int x, int y, int z, ThemeCopyBounds bounds) {
        if (bounds.skipRedBorder && isProtectedBorderBlock(x, y, z, bounds)) return true;
        return false;
    }

    private boolean isInsideBuildArea(int x, int z, ThemeCopyBounds bounds) {
        return x > bounds.themeProtectBounds.minX && x < bounds.themeProtectBounds.maxX && z > bounds.themeProtectBounds.minZ && z < bounds.themeProtectBounds.maxZ;
    }

    private boolean isProtectedBorderBlock(int x, int y, int z, ThemeCopyBounds bounds) {
        if (!isOnKnownBorderRing(x, z, bounds)) return false;
        return !bounds.skipRedBorderBlocksOnly || y == bounds.borderY;
    }

    private boolean isOnKnownBorderRing(int x, int z, ThemeCopyBounds bounds) {
        for (HouseManager.BorderBounds border : bounds.borderSkipBounds) {
            boolean onBorderRing = (x >= border.minX && x <= border.maxX && (z == border.minZ || z == border.maxZ))
                    || (z >= border.minZ && z <= border.maxZ && (x == border.minX || x == border.maxX));
            if (onBorderRing) return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean isTemplateBorderBlock(org.bukkit.block.Block sourceBlock, int x, int y, int z, ThemeCopyBounds bounds) {
        if (!bounds.skipRedBorder || !isOnKnownBorderRing(x, z, bounds)) return false;
        return isRedWool(sourceBlock);
    }

    @SuppressWarnings("deprecation")
    private boolean isRedWool(org.bukkit.block.Block block) {
        if (block == null) return false;
        if (block.getType() == net.fakepixel.housing.util.Compat.mat("RED_WOOL")) return true;
        return block.getType() == org.bukkit.Material.matchMaterial("WOOL") && block.getData() == 14;
    }

    private HouseManager.BorderBounds fixedProtectedBuildBounds() {
        int x1 = plugin.getStaticConfig().getInt("theme-apply.protected-build-region.pos1.x", -32);
        int z1 = plugin.getStaticConfig().getInt("theme-apply.protected-build-region.pos1.z", 25);
        int x2 = plugin.getStaticConfig().getInt("theme-apply.protected-build-region.pos2.x", 20);
        int z2 = plugin.getStaticConfig().getInt("theme-apply.protected-build-region.pos2.z", -27);
        return new HouseManager.BorderBounds(Math.min(x1, x2), Math.max(x1, x2), Math.min(z1, z2), Math.max(z1, z2));
    }

    private final class ThemeApplyTask extends org.bukkit.scheduler.BukkitRunnable {
        private final UUID playerId;
        private final House house;
        private final ThemeDefinition theme;
        private final org.bukkit.World source;
        private final org.bukkit.World target;
        private final net.fakepixel.housing.world.SlimeWorldManagerService service;
        private final ThemeCopyBounds bounds;
        private final String cacheKey;
        private final long templateLastModified;
        private int x;
        private int z;
        private int y;
        private long scannedTotal;
        private long changedTotal;
        private int ticks;
        private int applyIndex;
        private ThemeBlockBuilder cacheBuilder;
        private ThemeBlockCache cache;
        private boolean sourceCleaned;

        private ThemeApplyTask(UUID playerId, House house, ThemeDefinition theme, org.bukkit.World source, org.bukkit.World target, net.fakepixel.housing.world.SlimeWorldManagerService service, ThemeCopyBounds bounds, ThemeBlockCache cache, String cacheKey, long templateLastModified) {
            this.playerId = playerId;
            this.house = house;
            this.theme = theme;
            this.source = source;
            this.target = target;
            this.service = service;
            this.bounds = bounds;
            this.cache = cache;
            this.cacheKey = cacheKey;
            this.templateLastModified = templateLastModified;
            this.x = bounds.minX;
            this.z = bounds.minZ;
            this.y = bounds.minY;
            this.cacheBuilder = cache == null ? new ThemeBlockBuilder() : null;
        }

        @Override
        public void run() {
            try {
                if (cache == null) buildCacheTick();
                else applyCacheTick();
                ticks++;
                if (ticks % 20 == 0) sendThemeProgress();
            } catch (Throwable throwable) {
                fail(throwable);
                cancel();
            }
        }

        @SuppressWarnings("deprecation")
        private void buildCacheTick() {
            if (source == null) {
                fail(new IllegalStateException("Theme source world is not loaded."));
                cancel();
                return;
            }
            int changedThisTick = 0;
            int scanned = 0;
            while (changedThisTick < bounds.blocksPerTick && scanned < bounds.maxScannedPerTick) {
                if (x > bounds.maxX) {
                    cache = cacheBuilder.build(templateLastModified);
                    if (bounds.useTemplateCache) storeThemeBlockCache(cacheKey, cache);
                    cacheBuilder = null;
                    finish();
                    cancel();
                    return;
                }
                if (!shouldSkipThemeBlock(x, y, z, bounds)) {
                    org.bukkit.block.Block sourceBlock = source.getBlockAt(sourceThemeX(x, bounds), y, sourceThemeZ(z, bounds));
                    if (isTemplateBorderBlock(sourceBlock, x, y, z, bounds)) {
                        scanned++;
                        scannedTotal++;
                        advance();
                        continue;
                    }
                    boolean sourceAir = sourceBlock.getType() == net.fakepixel.housing.util.Compat.mat("AIR");
                    if (!bounds.ignoreAir || !sourceAir) {
                        org.bukkit.block.Block targetBlock = target.getBlockAt(x, y, z);
                        boolean targetAir = targetBlock.getType() == net.fakepixel.housing.util.Compat.mat("AIR");
                        cacheBuilder.add(x, y, z, sourceBlock.getTypeId(), sourceBlock.getData());
                        if (!(sourceAir && targetAir) && !(bounds.protectPlayerBuildBlocks && isPlayerBuildBlock(targetBlock, x, y, z, house))) {
                            targetBlock.setTypeIdAndData(sourceBlock.getTypeId(), sourceBlock.getData(), false);
                            changedThisTick++;
                            changedTotal++;
                        }
                    }
                }
                scanned++;
                scannedTotal++;
                advance();
            }
        }

        @SuppressWarnings("deprecation")
        private void applyCacheTick() {
            if (cache == null || applyIndex >= cache.size()) {
                finish();
                cancel();
                return;
            }
            int changedThisTick = 0;
            int processed = 0;
            int chunks = 0;
            int activeChunk = Integer.MIN_VALUE;
            while (applyIndex < cache.size() && changedThisTick < bounds.blocksPerTick && processed < bounds.maxScannedPerTick) {
                int packed = cache.coords[applyIndex];
                int chunkKey = themeChunkKey(packed);
                if (bounds.pasteByChunks && chunkKey != activeChunk) {
                    if (chunks >= bounds.chunkBatchSize) break;
                    activeChunk = chunkKey;
                    chunks++;
                }
                int blockX = unpackSigned12(packed >> 20);
                int blockZ = unpackSigned12((packed >> 8) & 0xFFF);
                int blockY = packed & 0xFF;
                int typeData = cache.typeData[applyIndex];
                int typeId = typeData >> 8;
                byte data = (byte) (typeData & 0xFF);
                org.bukkit.block.Block targetBlock = target.getBlockAt(blockX, blockY, blockZ);
                boolean sourceAir = typeId == net.fakepixel.housing.util.Compat.mat("AIR").getId();
                boolean targetAir = targetBlock.getType() == net.fakepixel.housing.util.Compat.mat("AIR");
                if (sourceAir && targetAir) {
                    applyIndex++;
                    processed++;
                    scannedTotal++;
                    continue;
                }
                if (bounds.protectPlayerBuildBlocks && isPlayerBuildBlock(targetBlock, blockX, blockY, blockZ, house)) {
                    applyIndex++;
                    processed++;
                    scannedTotal++;
                    continue;
                }
                if (!bounds.onlyChangeDifferentBlocks || targetBlock.getTypeId() != typeId || targetBlock.getData() != data) {
                    targetBlock.setTypeIdAndData(typeId, data, false);
                    changedThisTick++;
                    changedTotal++;
                }
                applyIndex++;
                processed++;
                scannedTotal++;
            }
        }

        @SuppressWarnings("deprecation")
        private boolean isPlayerBuildBlock(org.bukkit.block.Block targetBlock, int blockX, int blockY, int blockZ, House house) {
            return house != null && house.isPlayerEditedBlock(blockX, blockY, blockZ);
        }

        private void advance() {
            y++;
            if (y <= bounds.maxY) return;
            y = bounds.minY;
            z++;
            if (z <= bounds.maxZ) return;
            z = bounds.minZ;
            x++;
        }

        private void finish() {
            try {
                house.setTheme(theme.id);
                applyThemeSettings(house, theme);
                applyHouseWeather(house);
                applyHouseBiomeSky(house);
                houses.prepareHouseWorld(target);
                saveWorldBestEffort(target);
                if (bounds.redrawBorderAfterApply) houses.ensureBorder(target);
                houses.saveAll();
                if (bounds.updateLightingAfter) refreshThemeChunks();
                for (Player viewer : target.getPlayers()) houses.refreshBorderVisibility(viewer, house);
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(plugin.color("&aTheme applied!"));
                    openThemes(player);
                    houses.updateVisitItems(player);
                }
            } finally {
                applyingThemes.remove(playerId);
                cleanupSource();
            }
        }

        private void fail(Throwable throwable) {
            try {
                if (bounds.redrawBorderAfterApply) houses.ensureBorder(target);
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(plugin.color("&cCould not apply theme: " + throwable.getMessage()));
                    openThemes(player);
                    houses.updateVisitItems(player);
                }
                plugin.getLogger().warning("Could not apply housing theme " + theme.id + ": " + throwable.getMessage());
            } finally {
                applyingThemes.remove(playerId);
                cleanupSource();
            }
        }

        private void cleanupSource() {
            if (sourceCleaned) return;
            sourceCleaned = true;
            if (source != null) service.cleanupThemeWorld(source);
        }

        @SuppressWarnings("deprecation")
        private void refreshThemeChunks() {
            int minChunkX = bounds.minX >> 4;
            int maxChunkX = bounds.maxX >> 4;
            int minChunkZ = bounds.minZ >> 4;
            int maxChunkZ = bounds.maxZ >> 4;
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    try { target.refreshChunk(cx, cz); } catch (Throwable ignored) {}
                }
            }
        }

        private void sendThemeProgress() {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) return;
            long totalWork = cache == null ? bounds.totalPositions : cache.size();
            int percent = totalWork <= 0L ? 100 : (int) Math.min(100L, (scannedTotal * 100L) / totalWork);
            String status = "&eApplying theme... &a";
            if (!sendActionBar(player, status + percent + "%")) {
                if (ticks % 100 == 0) player.sendMessage(plugin.color(status + percent + "%"));
            }
        }
    }

    private int sourceThemeX(int targetX, ThemeCopyBounds bounds) {
        if (!bounds.alignTemplateOriginToHouseOrigin) return targetX;
        return targetX - bounds.houseOriginX + bounds.templateOriginX;
    }

    private int sourceThemeZ(int targetZ, ThemeCopyBounds bounds) {
        if (!bounds.alignTemplateOriginToHouseOrigin) return targetZ;
        return targetZ - bounds.houseOriginZ + bounds.templateOriginZ;
    }

    private boolean sendActionBar(Player player, String message) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Class<?> serializer = Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent$ChatSerializer");
            Object component = serializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + plugin.color(message).replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
            Object packet = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutChat").getConstructor(Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent"), byte.class).newInstance(component, (byte) 2);
            connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server.v1_8_R3.Packet")).invoke(connection, packet);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class ThemeCopyBounds {
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;
        private final HouseManager.BorderBounds border;
        private final List<HouseManager.BorderBounds> borderSkipBounds;
        private final HouseManager.BorderBounds themeProtectBounds;
        private final int borderY;
        private final int houseOriginX;
        private final int houseOriginZ;
        private final int templateOriginX;
        private final int templateOriginZ;
        private final int blocksPerTick;
        private final int maxScannedPerTick;
        private final boolean useTemplateCache;
        private final boolean alignFromPlotCenter;
        private final boolean pasteByChunks;
        private final int chunkBatchSize;
        private final boolean updateLightingAfter;
        private final boolean redrawBorderAfterApply;
        private final boolean skipProtectedPlot;
        private final boolean skipRedBorder;
        private final boolean skipRedBorderBlocksOnly;
        private final boolean protectPlayerBuildBlocks;
        private final boolean protectEntireBorderBox;
        private final boolean pasteThemeInsideBorderArea;
        private final boolean alignTemplateOriginToHouseOrigin;
        private final boolean clearOldThemeBeforeApply;
        private final boolean pasteFullTemplateRegion;
        private final boolean onlyChangeDifferentBlocks;
        private final boolean ignoreAir;
        private final long totalPositions;

        private ThemeCopyBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, HouseManager.BorderBounds border, List<HouseManager.BorderBounds> borderSkipBounds, HouseManager.BorderBounds themeProtectBounds, int borderY, int houseOriginX, int houseOriginZ, int templateOriginX, int templateOriginZ, int blocksPerTick, int maxScannedPerTick, boolean useTemplateCache, boolean alignFromPlotCenter, boolean pasteByChunks, int chunkBatchSize, int batchRadius, boolean updateLightingAfter, boolean redrawBorderAfterApply, boolean skipProtectedPlot, boolean skipRedBorder, boolean skipRedBorderBlocksOnly, boolean protectPlayerBuildBlocks, boolean protectEntireBorderBox, boolean pasteThemeInsideBorderArea, boolean alignTemplateOriginToHouseOrigin, boolean clearOldThemeBeforeApply, boolean pasteFullTemplateRegion, boolean onlyChangeDifferentBlocks, boolean ignoreAir, long totalPositions) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.border = border;
            this.borderSkipBounds = borderSkipBounds == null ? new ArrayList<HouseManager.BorderBounds>() : borderSkipBounds;
            this.themeProtectBounds = themeProtectBounds;
            this.borderY = borderY;
            this.houseOriginX = houseOriginX;
            this.houseOriginZ = houseOriginZ;
            this.templateOriginX = templateOriginX;
            this.templateOriginZ = templateOriginZ;
            this.blocksPerTick = blocksPerTick;
            this.maxScannedPerTick = maxScannedPerTick;
            this.useTemplateCache = useTemplateCache;
            this.alignFromPlotCenter = alignFromPlotCenter;
            this.pasteByChunks = pasteByChunks;
            this.chunkBatchSize = chunkBatchSize;
            this.updateLightingAfter = updateLightingAfter;
            this.redrawBorderAfterApply = redrawBorderAfterApply;
            this.skipProtectedPlot = skipProtectedPlot;
            this.skipRedBorder = skipRedBorder;
            this.skipRedBorderBlocksOnly = skipRedBorderBlocksOnly;
            this.protectPlayerBuildBlocks = protectPlayerBuildBlocks;
            this.protectEntireBorderBox = protectEntireBorderBox;
            this.pasteThemeInsideBorderArea = pasteThemeInsideBorderArea;
            this.alignTemplateOriginToHouseOrigin = alignTemplateOriginToHouseOrigin;
            this.clearOldThemeBeforeApply = clearOldThemeBeforeApply;
            this.pasteFullTemplateRegion = pasteFullTemplateRegion;
            this.onlyChangeDifferentBlocks = onlyChangeDifferentBlocks;
            this.ignoreAir = ignoreAir;
            this.totalPositions = totalPositions;
        }
    }

    private static final class ThemeBlockCache {
        private final int[] coords;
        private final int[] typeData;
        private final long lastModified;

        private ThemeBlockCache(int[] coords, int[] typeData, long lastModified) {
            this.coords = coords;
            this.typeData = typeData;
            this.lastModified = lastModified;
        }

        private int size() {
            return coords.length;
        }
    }

    private static final class ThemeBlockBuilder {
        private int[] coords = new int[8192];
        private int[] typeData = new int[8192];
        private int size;

        private void add(int x, int y, int z, int typeId, byte data) {
            ensureCapacity(size + 1);
            coords[size] = packThemeCoord(x, y, z);
            typeData[size] = (typeId << 8) | (data & 0xFF);
            size++;
        }

        private ThemeBlockCache build(long lastModified) {
            int[] builtCoords = Arrays.copyOf(coords, size);
            int[] builtTypeData = Arrays.copyOf(typeData, size);
            sortThemeBlocksByChunk(builtCoords, builtTypeData, 0, size - 1);
            return new ThemeBlockCache(builtCoords, builtTypeData, lastModified);
        }

        private void ensureCapacity(int required) {
            if (required <= coords.length) return;
            int next = Math.max(required, coords.length * 2);
            coords = Arrays.copyOf(coords, next);
            typeData = Arrays.copyOf(typeData, next);
        }
    }

    private static int packThemeCoord(int x, int y, int z) {
        return ((x & 0xFFF) << 20) | ((z & 0xFFF) << 8) | (y & 0xFF);
    }

    private static void sortThemeBlocksByChunk(int[] coords, int[] typeData, int left, int right) {
        if (left >= right) return;
        int i = left;
        int j = right;
        int pivot = coords[(left + right) >>> 1];
        while (i <= j) {
            while (compareThemeCoord(coords[i], pivot) < 0) i++;
            while (compareThemeCoord(coords[j], pivot) > 0) j--;
            if (i <= j) {
                int coord = coords[i];
                coords[i] = coords[j];
                coords[j] = coord;
                int data = typeData[i];
                typeData[i] = typeData[j];
                typeData[j] = data;
                i++;
                j--;
            }
        }
        if (left < j) sortThemeBlocksByChunk(coords, typeData, left, j);
        if (i < right) sortThemeBlocksByChunk(coords, typeData, i, right);
    }

    private static int compareThemeCoord(int first, int second) {
        int firstChunk = themeChunkKey(first);
        int secondChunk = themeChunkKey(second);
        if (firstChunk != secondChunk) return firstChunk < secondChunk ? -1 : 1;
        int firstY = first & 0xFF;
        int secondY = second & 0xFF;
        if (firstY != secondY) return firstY - secondY;
        return first - second;
    }

    private static int themeChunkKey(int packed) {
        int x = unpackSigned12(packed >> 20) >> 4;
        int z = unpackSigned12((packed >> 8) & 0xFFF) >> 4;
        return ((x & 0xFFFF) << 16) | (z & 0xFFFF);
    }

    private static int unpackSigned12(int value) {
        value &= 0xFFF;
        return value >= 0x800 ? value - 0x1000 : value;
    }

    private void copyBlockState(org.bukkit.block.Block sourceBlock, org.bukkit.block.Block targetBlock) {
        org.bukkit.block.BlockState sourceState = sourceBlock.getState();
        org.bukkit.block.BlockState targetState = targetBlock.getState();
        if (sourceState instanceof org.bukkit.inventory.InventoryHolder && targetState instanceof org.bukkit.inventory.InventoryHolder) {
            ((org.bukkit.inventory.InventoryHolder) targetState).getInventory().setContents(((org.bukkit.inventory.InventoryHolder) sourceState).getInventory().getContents());
        }
        if (sourceState instanceof org.bukkit.block.Sign && targetState instanceof org.bukkit.block.Sign) {
            org.bukkit.block.Sign sourceSign = (org.bukkit.block.Sign) sourceState;
            org.bukkit.block.Sign targetSign = (org.bukkit.block.Sign) targetState;
            for (int i = 0; i < 4; i++) targetSign.setLine(i, sourceSign.getLine(i));
            targetSign.update(true, false);
        }
    }

    private void copyDirectory(java.io.File source, java.io.File target) throws java.io.IOException {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) throw new java.io.IOException("Could not create " + target.getPath());
            java.io.File[] children = source.listFiles();
            if (children != null) for (java.io.File child : children) copyDirectory(child, new java.io.File(target, child.getName()));
            return;
        }
        java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private void cleanCopiedWorldFolder(java.io.File worldFolder) {
        deleteDirectory(new java.io.File(worldFolder, "uid.dat"));
        deleteDirectory(new java.io.File(worldFolder, "session.lock"));
        deleteDirectory(new java.io.File(worldFolder, "playerdata"));
    }

    private void deleteDirectory(java.io.File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            if (children != null) for (java.io.File child : children) deleteDirectory(child);
        }
        if (!file.delete()) plugin.getLogger().warning("Could not delete " + file.getPath());
    }

    private void saveWorldBestEffort(org.bukkit.World world) {
        try { world.save(); } catch (Throwable ignored) {}
        org.bukkit.plugin.Plugin asp = Bukkit.getPluginManager().getPlugin("AdvancedSlimePaper");
        if (asp == null) asp = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
        if (asp == null) return;
        invokeAny(asp, "saveWorld", world);
        invokeAny(asp, "saveWorld", world.getName());
    }

    private void invokeAny(Object target, String name, Object... args) {
        if (target == null) return;
        for (java.lang.reflect.Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) continue;
            try {
                method.setAccessible(true);
                method.invoke(target, args);
                return;
            } catch (Throwable ignored) {}
        }
    }


    private void openBlocks(Player player, int page) {
        if (!requireItemsMenuBuildPermission(player)) return;
        int maxPage = blockPages().size() - 1;
        int safePage = Math.max(0, Math.min(page, maxPage));
        blockMenuPages.put(player.getUniqueId(), safePage);

        Inventory inv = Bukkit.createInventory(null, 54, blocksTitle);
        List<ItemStack> blocks = blockPages().get(safePage);
        for (int i = 0; i < blocks.size() && i < 36; i++) {
            ItemStack block = blocks.get(i);
            if (block == null || block.getType() == net.fakepixel.housing.util.Compat.mat("AIR")) continue;
            inv.setItem(i, blockItem(block));
        }

        inv.setItem(48, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&7Previous Page", "&7Go to the previous blocks page."));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&aMain Menu", "&7Go back to the Housing Menu."));
        inv.setItem(50, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aNext Page", "&7Go to the next blocks page."));
        player.openInventory(inv);
    }

    private boolean isBlocksTitle(String title) {
        return title != null && title.equals(blocksTitle);
    }

    private int currentBlocksPage(Player player) {
        Integer page = blockMenuPages.get(player.getUniqueId());
        if (page == null) return 0;
        return Math.max(0, Math.min(page, blockPages().size() - 1));
    }

    private ItemStack blockItem(ItemStack stack) {
        if (stack == null || stack.getType() == net.fakepixel.housing.util.Compat.mat("AIR")) return emptyStack();
        String name = stack.getType().name().toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
        StringBuilder pretty = new StringBuilder();
        for (String part : name.split(" ")) {
            if (part.isEmpty()) continue;
            if (pretty.length() > 0) pretty.append(' ');
            pretty.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return item(stack.clone(), "&a" + pretty, "&7Click to get a stack of 64.");
    }

    private ItemStack blockItem(Material material) {
        return blockItem(new ItemStack(material));
    }

    private ItemStack pvpItem(Material material) {
        String name = material.name().toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ');
        StringBuilder pretty = new StringBuilder();
        for (String part : name.split(" ")) {
            if (part.isEmpty()) continue;
            if (pretty.length() > 0) pretty.append(' ');
            pretty.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return item(material, "&a" + pretty, "&7Click to get 1 item.");
    }

    private boolean isBuildBlock(ItemStack stack) {
        if (stack == null || stack.getType() == net.fakepixel.housing.util.Compat.mat("AIR")) return false;
        Material material = stack.getType();
        for (List<ItemStack> page : blockPages()) {
            for (ItemStack item : page) {
                if (item != null && item.getType() == material) return true;
            }
        }
        return false;
    }

    private void openPlants(Player player) {
        if (!requireItemsMenuBuildPermission(player)) return;
        Inventory inv = Bukkit.createInventory(null, 54, plantsTitle);
        List<ItemStack> plants = plantItems();
        for (int i = 0; i < plants.size() && i < 45; i++) {
            inv.setItem(i, blockItem(plants.get(i)));
        }
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&aMain Menu", "&7Go back to the Housing Menu."));
        inv.setItem(50, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Go back to the Housing Menu."));
        player.openInventory(inv);
    }

    @SuppressWarnings("deprecation")
    private boolean isPlantItem(ItemStack stack) {
        if (stack == null || stack.getType() == net.fakepixel.housing.util.Compat.mat("AIR")) return false;
        for (ItemStack plant : plantItems()) {
            if (plant.getType() == stack.getType() && plant.getDurability() == stack.getDurability()) return true;
        }
        return false;
    }

    private List<ItemStack> plantItems() {
        return Arrays.asList(
                legacyItem("LEAVES", 0), legacyItem("LEAVES", 1), legacyItem("LEAVES", 2), legacyItem("LEAVES", 3), legacyItem("LEAVES_2", 0), legacyItem("LEAVES_2", 1),
                legacyItem("LONG_GRASS", 1), legacyItem("LONG_GRASS", 2), legacyItem("DOUBLE_PLANT", 2), legacyItem("DOUBLE_PLANT", 3), stack("CACTUS"), stack("DEAD_BUSH"), stack("VINE"), stack("WATER_LILY"),
                stack("YELLOW_FLOWER"), legacyItem("RED_ROSE", 0), legacyItem("RED_ROSE", 1), legacyItem("RED_ROSE", 2), legacyItem("RED_ROSE", 3), legacyItem("RED_ROSE", 4), legacyItem("RED_ROSE", 5), legacyItem("RED_ROSE", 6), legacyItem("RED_ROSE", 7),
                legacyItem("RED_ROSE", 8), legacyItem("DOUBLE_PLANT", 0), legacyItem("DOUBLE_PLANT", 1), legacyItem("DOUBLE_PLANT", 4), legacyItem("DOUBLE_PLANT", 5), stack("SUGAR_CANE"),
                stack("BROWN_MUSHROOM"), stack("RED_MUSHROOM"), stack("NETHER_WARTS"), stack("SEEDS"), stack("PUMPKIN_SEEDS"), stack("MELON_SEEDS"),
                legacyItem("DIRT", 0), legacyItem("DIRT", 1), legacyItem("DIRT", 2), stack("GRASS"), stack("SOIL"), stack("MYCEL")
        );
    }





    private void openPvp(Player player, int page) {
        int maxPage = pvpPages().size() - 1;
        int safePage = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(null, 54, pvpTitle(safePage));
        List<Material> pvp = pvpPages().get(safePage);
        for (int i = 0; i < pvp.size() && i < 45; i++) {
            Material material = pvp.get(i);
            inv.setItem(i, pvpItem(material));
        }
        if (safePage > 0) {
            inv.setItem(48, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aPrevious Page", "&7Go to page &e" + safePage + "&7."));
        }
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&aMain Menu", "&7Go back to the Housing Menu."));
        if (safePage < maxPage) {
            inv.setItem(50, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aNext Page", "&7Go to page &e" + (safePage + 2) + "&7."));
        }
        player.openInventory(inv);
    }

    private String pvpTitle(int page) {
        return pvpTitlePrefix + (page + 1) + "/" + pvpPages().size() + ")";
    }

    private boolean isPvpTitle(String title) {
        return title != null && title.startsWith(pvpTitlePrefix);
    }

    private int pageFromPvpTitle(String title) {
        try {
            int start = title.indexOf("Page ") + 5;
            int slash = title.indexOf('/', start);
            return Math.max(0, Integer.parseInt(title.substring(start, slash)) - 1);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean isPvpItem(Material material) {
        for (List<Material> page : pvpPages()) {
            if (page.contains(material)) return true;
        }
        return false;
    }

    private List<List<Material>> pvpPages() {
        List<List<Material>> pages = new ArrayList<>();
        pages.add(materialList(
                "WOODEN_SWORD", "STONE_SWORD", "IRON_SWORD", "GOLDEN_SWORD", "DIAMOND_SWORD",
                "WOODEN_AXE", "STONE_AXE", "IRON_AXE", "GOLDEN_AXE", "DIAMOND_AXE",
                "BOW", "CROSSBOW", "ARROW", "SPECTRAL_ARROW", "TIPPED_ARROW",
                "FISHING_ROD", "STICK", "BLAZE_ROD", "TRIDENT",
                "SLIME_BALL", "ENDER_PEARL", "EGG", "SNOWBALL",
                "GOLDEN_APPLE", "ENCHANTED_GOLDEN_APPLE", "CARROT", "GOLDEN_CARROT", "BREAD", "COOKED_BEEF",
                "COOKED_CHICKEN", "COOKED_COD", "COOKED_SALMON", "PORKCHOP", "COOKED_PORKCHOP", "MUTTON", "COOKED_MUTTON"
        ));
        pages.add(materialList(
                "LEATHER_HELMET", "CHAINMAIL_HELMET", "IRON_HELMET", "GOLDEN_HELMET", "DIAMOND_HELMET", "NETHERITE_HELMET",
                "LEATHER_CHESTPLATE", "CHAINMAIL_CHESTPLATE", "IRON_CHESTPLATE", "GOLDEN_CHESTPLATE", "DIAMOND_CHESTPLATE", "NETHERITE_CHESTPLATE",
                "LEATHER_LEGGINGS", "CHAINMAIL_LEGGINGS", "IRON_LEGGINGS", "GOLDEN_LEGGINGS", "DIAMOND_LEGGINGS", "NETHERITE_LEGGINGS",
                "LEATHER_BOOTS", "CHAINMAIL_BOOTS", "IRON_BOOTS", "GOLDEN_BOOTS", "DIAMOND_BOOTS", "NETHERITE_BOOTS"
        ));
        return pages;
    }

    private void giveSingle(Player player, ItemStack clicked) {
        if (clicked == null || clicked.getType() == net.fakepixel.housing.util.Compat.mat("AIR")) return;
        ItemStack give = clicked.clone();
        give.setAmount(1);
        ItemMeta meta = give.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(null);
            meta.setLore(null);
            give.setItemMeta(meta);
        }
        player.getInventory().addItem(give).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.sendMessage(plugin.color("&aAdded &e1 " + clicked.getType().name().toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ') + "&a to your inventory."));
    }


    private void openItems(Player player) {
        openItems(player, 0);
    }

    private void openItems(Player player, int page) {
        if (!requireItemsMenuBuildPermission(player)) return;
        List<List<ItemStack>> pages = itemPages();
        int safePage = Math.max(0, Math.min(page, pages.size() - 1));
        Inventory inv = Bukkit.createInventory(null, 54, itemsTitle(safePage));
        List<ItemStack> items = pages.get(safePage);
        for (int i = 0; i < items.size() && i < 45; i++) inv.setItem(i, items.get(i));
        inv.setItem(47, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&7Previous Page", "&7Go to the previous page."));
        inv.setItem(48, item(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Page &f" + (safePage + 1) + "&7/&f" + pages.size()));
        inv.setItem(49, item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), "&aHousing Menu", "&7Go back to the Housing Menu."));
        inv.setItem(50, item(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aNext Page", "&7Go to the next page."));
        player.openInventory(inv);
    }

    private String itemsTitle() {
        return itemsTitle(0);
    }

    private String itemsTitle(int page) {
        return itemsTitlePrefix + (page + 1) + ")";
    }

    private boolean isItemsTitle(String title) {
        return title != null && title.startsWith(itemsTitlePrefix);
    }

    private int pageFromItemsTitle(String title) {
        try {
            int start = title.indexOf(itemsTitlePrefix) + itemsTitlePrefix.length();
            int end = title.indexOf(")", start);
            return Math.max(0, Integer.parseInt(title.substring(start, end)) - 1);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private List<List<ItemStack>> itemPages() {
        List<ItemStack> items = new ArrayList<>();
        items.add(specialHousingItem(legacyMat("GOLD_PLATE"), "&aParkour Start Pad", "start", "&7Place this where players start", "&7your parkour."));
        items.add(specialHousingItem(legacyMat("IRON_PLATE"), "&eParkour Checkpoint Pad", "checkpoint", "&7Place this where players save", "&7a checkpoint."));
        items.add(specialHousingItem(legacyMat("STONE_PLATE"), "&cParkour Finish Pad", "finish", "&7Place this where players finish", "&7your parkour."));
        items.add(parkourLeaderboardItem());
        items.add(teleportPadItem());
        if (plugin.getHologramService() != null) items.add(plugin.getHologramService().item());
        if (plugin.getHouseNpcService() != null) items.add(plugin.getHouseNpcService().item());
        if (plugin.getActionButtonService() != null) items.add(plugin.getActionButtonService().item());
        if (plugin.getActionPadService() != null) items.add(plugin.getActionPadService().item());
        addItems(items,
                "SIGN", "LADDER", "CHEST", "ENDER_CHEST", "ANVIL", "ENCHANTMENT_TABLE", "BREWING_STAND_ITEM", "CAULDRON_ITEM", "FURNACE", "WORKBENCH",
                "NOTE_BLOCK", "MINECART", "BOAT", "WATER_BUCKET", "LAVA_BUCKET");
        List<List<ItemStack>> pages = new ArrayList<>();
        for (int i = 0; i < items.size(); i += 45) pages.add(new ArrayList<>(items.subList(i, Math.min(items.size(), i + 45))));
        if (pages.isEmpty()) pages.add(new ArrayList<>());
        return pages;
    }

    private void addItems(List<ItemStack> items, String... materialNames) {
        for (String name : materialNames) {
            Material material = Material.matchMaterial(name);
            if (material == null) material = net.fakepixel.housing.util.Compat.mat(name);
            if (material == net.fakepixel.housing.util.Compat.mat("STONE") && !"STONE".equalsIgnoreCase(name)) continue;
            if (material != null && material != net.fakepixel.housing.util.Compat.mat("AIR")) items.add(new ItemStack(material));
        }
    }

    private ItemStack specialHousingItem(Material material, String name, String parkourType, String... lore) {
        List<String> lines = new ArrayList<>(Arrays.asList(lore));
        lines.add("");
        lines.add("&7Special Housing item.");
        lines.add("&eClick to receive 1!");
        lines.add("&8Housing Special: parkour-" + parkourType);
        return item(material, name, lines.toArray(new String[0]));
    }

    private boolean isSpecialHousingItem(ItemStack item) {
        return parkourType(item) != null
                || isParkourLeaderboardItem(item)
                || isTeleportPadItem(item)
                || (plugin.getHologramService() != null && plugin.getHologramService().isHologramItem(item))
                || (plugin.getHouseNpcService() != null && plugin.getHouseNpcService().isNpcItem(item))
                || (plugin.getActionButtonService() != null && plugin.getActionButtonService().isActionButtonItem(item))
                || (plugin.getActionPadService() != null && plugin.getActionPadService().isActionPadItem(item));
    }

    public String parkourType(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Housing Special: parkour-")) return clean.substring("Housing Special: parkour-".length()).toLowerCase(java.util.Locale.ENGLISH);
        }
        return null;
    }

    public boolean isTeleportPadItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            if ("Housing Special: teleport-pad".equals(ChatColor.stripColor(line))) return true;
        }
        return false;
    }

    public boolean isParkourLeaderboardItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            if ("Housing Special: parkour-leaderboard".equals(ChatColor.stripColor(line))) return true;
        }
        return false;
    }

    private ItemStack parkourLeaderboardItem() {
        return item(net.fakepixel.housing.util.Compat.mat("SIGN"),
                "&eParkour Leaderboard",
                "&7Place this to show the",
                "&7top parkour times in this house.",
                "",
                "&7Special Housing item.",
                "&eClick to receive 1!",
                "&8Housing Special: parkour-leaderboard");
    }

    private ItemStack teleportPadItem() {
        return item(legacyMat("ENDER_PORTAL_FRAME"),
                "&bTeleport Pad",
                "&7Place this to create a",
                "&7Teleport Pad in your house.",
                "",
                "&7Special Housing item.",
                "&eClick to receive 1!",
                "&8Housing Special: teleport-pad");
    }

    private void giveHousingItem(Player player, ItemStack clicked) {
        if (clicked == null || clicked.getType() == net.fakepixel.housing.util.Compat.mat("AIR")) return;
        if (isSpecialHousingItem(clicked)) giveSpecial(player, clicked);
        else giveStack(player, clicked);
    }

    private void giveSpecial(Player player, ItemStack clicked) {
        ItemStack give = clicked.clone();
        give.setAmount(1);
        player.getInventory().addItem(give).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.sendMessage(plugin.color("&aAdded &e1 special Housing item&a to your inventory."));
    }

    private boolean requireItemsMenuBuildPermission(Player player) {
        House house = player == null ? null : houses.getHouseByWorld(player.getWorld().getName());
        if (house != null && plugin.canBuildInHouseMode(player, house)) return true;
        if (player != null) {
            player.closeInventory();
            player.sendMessage(plugin.color(ITEMS_MENU_PERMISSION_MESSAGE));
        }
        return false;
    }

    private List<Material> materialList(String... names) {
        List<Material> materials = new ArrayList<>();
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material != null && material != net.fakepixel.housing.util.Compat.mat("AIR") && !materials.contains(material)) materials.add(material);
        }
        return materials;
    }

    private void giveStack(Player player, Material material) {
        giveStack(player, new ItemStack(material));
    }

    private void giveStack(Player player, ItemStack clicked) {
        if (clicked == null || clicked.getType() == net.fakepixel.housing.util.Compat.mat("AIR")) return;
        ItemStack give = clicked.clone();
        give.setAmount(64);
        ItemMeta meta = give.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(null);
            meta.setLore(null);
            give.setItemMeta(meta);
        }
        player.getInventory().addItem(give).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.sendMessage(plugin.color("&aAdded &e64 " + clicked.getType().name().toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ') + "&a to your inventory."));
    }

    private List<List<ItemStack>> blockPages() {
        List<List<ItemStack>> pages = new ArrayList<>();

        pages.add(Arrays.asList(
                legacyItem("WOOD", 0), legacyItem("WOOD", 1), legacyItem("WOOD", 2), legacyItem("WOOD", 3), legacyItem("WOOD", 4), legacyItem("WOOD", 5), stack("WOOD_DOOR"), stack("BIRCH_DOOR_ITEM"), stack("ACACIA_DOOR_ITEM"),
                legacyItem("LOG", 0), legacyItem("LOG", 1), legacyItem("LOG", 2), legacyItem("LOG", 3), legacyItem("LOG_2", 0), legacyItem("LOG_2", 1), stack("TRAP_DOOR"), stack("JUNGLE_DOOR_ITEM"), stack("DARK_OAK_DOOR_ITEM"),
                legacyItem("WOOD_STAIRS", 0), stack("SPRUCE_WOOD_STAIRS"), stack("BIRCH_WOOD_STAIRS"), stack("JUNGLE_WOOD_STAIRS"), stack("ACACIA_STAIRS"), stack("DARK_OAK_STAIRS"), stack("FENCE"), stack("SPRUCE_FENCE"), stack("BIRCH_FENCE"),
                stack("FENCE_GATE"), stack("SPRUCE_FENCE_GATE"), stack("BIRCH_FENCE_GATE"), stack("JUNGLE_FENCE_GATE"), stack("ACACIA_FENCE_GATE"), stack("DARK_OAK_FENCE_GATE"), stack("WOOD_BUTTON"), stack("WOOD_PLATE"), emptyStack(),
                legacyItem("WOOD_STEP", 0), legacyItem("WOOD_STEP", 1), legacyItem("WOOD_STEP", 2), legacyItem("WOOD_STEP", 3), legacyItem("WOOD_STEP", 4), legacyItem("WOOD_STEP", 5), stack("JUNGLE_FENCE"), stack("DARK_OAK_FENCE"), stack("ACACIA_FENCE")
        ));

        pages.add(Arrays.asList(
                stack("STONE"), stack("DIRT"), stack("HARD_CLAY"), stack("COBBLESTONE"), stack("MOSSY_COBBLESTONE"), stack("SMOOTH_BRICK"), legacyItem("SMOOTH_BRICK", 1), legacyItem("SMOOTH_BRICK", 2), legacyItem("SMOOTH_BRICK", 3),
                stack("STEP"), stack("DOUBLE_STEP"), stack("GRAVEL"), stack("SAND"), stack("GRASS"), stack("MYCEL"), stack("BRICK"), stack("STONE_STAIRS"), stack("COBBLE_WALL"),
                stack("SANDSTONE"), legacyItem("SANDSTONE", 1), legacyItem("SANDSTONE", 2), stack("SMOOTH_STAIRS"), stack("QUARTZ_STAIRS"), stack("WOOD_STAIRS"), stack("BRICK_STAIRS"), stack("SMOOTH_BRICK"), stack("HARD_CLAY"),
                legacyItem("SAND", 1), legacyItem("RED_SANDSTONE", 0), legacyItem("RED_SANDSTONE", 1), legacyItem("RED_SANDSTONE", 2), legacyItem("RED_SANDSTONE_STAIRS", 0), legacyItem("STONE_SLAB2", 0), stack("IRON_DOOR"), stack("IRON_TRAPDOOR"), stack("STONE_BUTTON")
        ));

        pages.add(Arrays.asList(
                stack("QUARTZ_BLOCK"), stack("QUARTZ_BLOCK", 1), stack("QUARTZ_BLOCK", 2), stack("NETHERRACK"), stack("SOUL_SAND"), stack("NETHER_BRICK"), stack("NETHER_FENCE"), stack("NETHER_BRICK_STAIRS"), stack("NETHER_WART_BLOCK"),
                stack("HAY_BLOCK"), stack("SPONGE"), stack("NETHER_WARTS"), stack("REDSTONE_ORE"), stack("LAPIS_ORE"), stack("DIAMOND_ORE"), stack("EMERALD_ORE"), stack("IRON_ORE"), stack("GOLD_ORE"),
                stack("PRISMARINE"), stack("SEA_LANTERN"), stack("PACKED_ICE"), stack("SLIME_BLOCK"), stack("GLOWSTONE"), stack("SANDSTONE"), stack("ENDER_STONE"), stack("BEACON"), stack("DIAMOND_BLOCK"),
                stack("COAL_BLOCK"), stack("OBSIDIAN"), stack("LAPIS_BLOCK"), stack("GOLD_BLOCK"), stack("EMERALD_BLOCK"), stack("REDSTONE_BLOCK"), stack("DIAMOND_BLOCK"), stack("PUMPKIN"), stack("JACK_O_LANTERN")
        ));

        pages.add(Arrays.asList(
                legacyItem("WOOL", 14), legacyItem("WOOL", 10), legacyItem("WOOL", 6), legacyItem("WOOL", 4), legacyItem("WOOL", 1), legacyItem("WOOL", 13), legacyItem("WOOL", 5), legacyItem("WOOL", 8), legacyItem("WOOL", 15),
                legacyItem("STAINED_CLAY", 2), legacyItem("STAINED_CLAY", 10), legacyItem("STAINED_CLAY", 6), legacyItem("STAINED_CLAY", 1), legacyItem("STAINED_CLAY", 4), legacyItem("STAINED_CLAY", 5), legacyItem("STAINED_CLAY", 9), legacyItem("STAINED_CLAY", 3), emptyStack(),
                legacyItem("STAINED_GLASS", 10), legacyItem("STAINED_GLASS", 2), legacyItem("STAINED_GLASS", 6), legacyItem("STAINED_GLASS", 14), legacyItem("STAINED_GLASS", 1), legacyItem("STAINED_GLASS", 4), legacyItem("STAINED_GLASS", 5), legacyItem("STAINED_GLASS", 9), legacyItem("STAINED_GLASS", 3),
                legacyItem("CARPET", 10), legacyItem("CARPET", 2), legacyItem("CARPET", 6), legacyItem("CARPET", 14), legacyItem("CARPET", 1), legacyItem("CARPET", 4), legacyItem("CARPET", 5), legacyItem("CARPET", 9), legacyItem("CARPET", 3)
        ));

        pages.add(Arrays.asList(
                legacyItem("WOOL", 15), legacyItem("WOOL", 11), legacyItem("STAINED_CLAY", 12), legacyItem("STAINED_CLAY", 15), legacyItem("STAINED_CLAY", 7), legacyItem("STAINED_CLAY", 14), legacyItem("STAINED_CLAY", 0), legacyItem("STAINED_CLAY", 1), legacyItem("STAINED_CLAY", 8),
                stack("GLASS"), legacyItem("STAINED_GLASS", 3), legacyItem("STAINED_GLASS", 11), legacyItem("STAINED_GLASS", 12), legacyItem("STAINED_GLASS", 7), legacyItem("STAINED_GLASS", 0), legacyItem("STAINED_GLASS", 8), stack("THIN_GLASS"), emptyStack(),
                legacyItem("STAINED_GLASS", 3), legacyItem("STAINED_GLASS", 11), legacyItem("STAINED_GLASS", 12), legacyItem("STAINED_GLASS", 15), legacyItem("STAINED_GLASS", 8), legacyItem("STAINED_GLASS", 0), legacyItem("STAINED_GLASS", 0), emptyStack(), emptyStack(),
                legacyItem("CARPET", 3), legacyItem("CARPET", 11), legacyItem("CARPET", 12), legacyItem("CARPET", 15), legacyItem("CARPET", 8), legacyItem("CARPET", 0), legacyItem("CARPET", 0), emptyStack(), emptyStack()
        ));

        pages.add(Arrays.asList(
                stack("CHEST"), stack("TRAPPED_CHEST"), stack("LOG", 0), stack("CRAFTING_TABLE"), stack("FURNACE"), stack("DISPENSER"), stack("DROPPER"), stack("HOPPER"), stack("ANVIL"),
                stack("JUKEBOX"), stack("NOTE_BLOCK"), stack("LOG", 1), stack("ENCHANTING_TABLE"), stack("ENDER_CHEST"), stack("BEACON"), stack("TNT"), stack("RED_BED"), stack("COBWEB"),
                stack("DAYLIGHT_DETECTOR"), stack("WOOD_STEP", 0), stack("STEP", 0), stack("IRON_PLATE"), stack("GOLD_PLATE"), stack("GOLD_BLOCK"), emptyStack(), emptyStack(), emptyStack(),
                stack("RAIL"), stack("POWERED_RAIL"), stack("DETECTOR_RAIL"), stack("ACTIVATOR_RAIL"), emptyStack(), stack("LEVER"), stack("REDSTONE_TORCH"), stack("TORCH"), emptyStack()
        ));

        return pages;
    }

    private ItemStack stack(String material) {
        return new ItemStack(legacyMat(material));
    }

    @SuppressWarnings("deprecation")
    private ItemStack stack(String material, int data) {
        return new ItemStack(legacyMat(material), 1, (short) data);
    }

    private ItemStack emptyStack() {
        return new ItemStack(net.fakepixel.housing.util.Compat.mat("AIR"));
    }

    private PlotTier[] plotTiers() {
        List<PlotTier> tiers = new ArrayList<>();
        org.bukkit.configuration.ConfigurationSection section = plugin.getStaticConfig().getConfigurationSection("plot-sizes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int size = Integer.parseInt(key);
                    String path = "plot-sizes." + key + ".";
                    tiers.add(new PlotTier(size,
                            plugin.getStaticConfig().getString(path + "name", size + "x" + size),
                            Math.max(0, plugin.getStaticConfig().getInt(path + "price", 0)),
                            plugin.getStaticConfig().getString(path + "permission", "")));
                } catch (NumberFormatException ignored) { }
            }
        }
        tiers.sort((left, right) -> Integer.compare(left.size, right.size));
        return tiers.toArray(new PlotTier[0]);
    }

    private PlotTier nextPlotTier(int currentSize) {
        for (PlotTier tier : plotTiers()) {
            if (tier.size > currentSize) return tier;
        }
        return null;
    }

    private PlotTier plotTierFromSlot(int slot) {
        int[] slots = new int[]{10, 11, 12, 13, 14, 15};
        PlotTier[] tiers = plotTiers();
        for (int i = 0; i < slots.length; i++) {
            if (slot == slots[i]) return tiers[i];
        }
        return null;
    }

    private boolean canAffordPlot(Player player, PlotTier tier) {
        if (tier == null || tier.cost <= 0) return true;
        if (isPlotTierLocked(player, tier)) return false;
        if (hasFreePlotBypass(player, tier)) return true;
        return abelix.getGold(player) >= tier.cost;
    }

    private boolean isPlotTierLocked(Player player, PlotTier tier) {
        return tier != null && tier.size == 255 && !hasPlotAccess(player, tier);
    }

    private boolean hasPlotAccess(Player player, PlotTier tier) {
        return tier.permission == null || tier.permission.trim().isEmpty() || player.hasPermission(tier.permission) || player.hasPermission("housing.admin");
    }

    private boolean hasFreePlotBypass(Player player, PlotTier tier) {
        return tier != null && tier.permission != null && player.hasPermission(tier.permission);
    }

    private String plotCostLine(Player player, PlotTier tier) {
        if (isPlotTierLocked(player, tier)) return "&7Upgrade Cost: &cRequires " + tier.permission;
        if (hasFreePlotBypass(player, tier)) return "&7Upgrade Cost: &aFree";
        return "&7Upgrade Cost: &6" + tier.cost + " Gold";
    }

    private String plotStatusLine(Player player, PlotTier tier) {
        if (isPlotTierLocked(player, tier)) return "&cRequires " + tier.permission;
        return canAffordPlot(player, tier) ? "&aYou can afford this upgrade." : "&cYou cannot afford this upgrade.";
    }

    private void upgradePlot(Player player, House house, PlotTier tier) {
        if (!house.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(plugin.color("&cOnly the house owner can buy plot size upgrades."));
            openPlotUpgrade(player, house);
            return;
        }
        if (house.getPlotSize() >= tier.size) {
            player.sendMessage(plugin.color("&aThat plot size is already unlocked for this house."));
            openPlotUpgrade(player, house);
            return;
        }
        PlotTier next = nextPlotTier(house.getPlotSize());
        if (next == null) {
            player.sendMessage(plugin.color("&aYour house already has the highest plot size."));
            openPlotUpgrade(player, house);
            return;
        }
        if (next.size != tier.size) {
            player.sendMessage(plugin.color("&cUnlock the next plot size first."));
            openPlotUpgrade(player, house);
            return;
        }
        if (isPlotTierLocked(player, tier)) {
            player.sendMessage(plugin.color("&cRequires " + tier.permission));
            openPlotUpgrade(player, house);
            return;
        }
        boolean permissionFree = hasFreePlotBypass(player, tier);
        if (!permissionFree && !abelix.takeGold(player, tier.cost)) {
            player.sendMessage(plugin.color("&cYou do not have enough Gold for that plot size upgrade."));
            openPlotUpgrade(player, house);
            return;
        }
        houses.setPlotSize(house, tier.size);
        player.sendMessage(plugin.color("&aPlot size upgraded to &e" + tier.size + "x" + tier.size + "&a."));
        openPlotUpgrade(player, house);
    }

    private static class PlotTier {
        private final int size;
        private final String displayName;
        private final int cost;
        private final String permission;

        private PlotTier(int size, String displayName, int cost, String permission) {
            this.size = size;
            this.displayName = displayName;
            this.cost = cost;
            this.permission = permission;
        }
    }

    private void openBuild(Player player) {
        if (plugin.getProToolsService() != null) plugin.getProToolsService().open(player);
    }

    private boolean canEditHouseSettings(Player player, House house) {
        if (player == null || house == null) return false;
        return house.getOwner().equals(player.getUniqueId()) || house.hasGroupPermission(player.getUniqueId(), "settings");
    }

    private boolean canToggleHouseMode(Player player, House house) {
        return canEditHouseSettings(player, house);
    }

    private boolean canRemoveCarpenter(Player player, House house) {
        if (player == null || house == null) return false;
        return house.getOwner().equals(player.getUniqueId()) || player.hasPermission("housing.carpenter.remove");
    }

    private boolean canEditHousingObjects(Player player, House house) {
        return plugin.canEditHousingObjects(player, house);
    }

    private void toggleHouseMode(Player player, House house) {
        if (!canToggleHouseMode(player, house)) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        house.toggleMode();
        houses.saveAll();
        plugin.sendHouseModeChanged(player, house);
        if (plugin.getHousingScoreboard() != null) plugin.getHousingScoreboard().refreshHouse(house);
        if (ownerTitle.equals(player.getOpenInventory().getTitle())) openOwnerMenu(player);
    }

    private void togglePvpDamageSetting(Player player, House house, int slot) {
        if (!canEditHouseSettings(player, house)) {
            player.sendMessage(plugin.color("&cYou cannot edit this house's settings."));
            openPvpDamageSettings(player, house);
            return;
        }
        if (slot == 10) house.setPvpEnabled(!house.isPvpEnabled());
        else if (slot == 11) house.setFallDamageEnabled(!house.isFallDamageEnabled());
        else if (slot == 12) house.setFireDamageEnabled(!house.isFireDamageEnabled());
        else if (slot == 13) house.setHungerEnabled(!house.isHungerEnabled());
        else if (slot == 14) house.setInstantRespawnEnabled(!house.isInstantRespawnEnabled());
        else if (slot == 15) house.setKillDeathMessagesEnabled(!house.isKillDeathMessagesEnabled());
        else return;
        houses.saveAll();
        if (slot == 10) {
            if (house.isPvpEnabled()) houses.applyPvpLayoutToHouse(house);
            else {
                org.bukkit.World world = Bukkit.getWorld(house.getWorldName());
                if (world != null) for (Player inside : world.getPlayers()) houses.updateVisitItems(inside);
            }
        }
        player.sendMessage(plugin.color("&aUpdated PvP + Damage Settings."));
        openPvpDamageSettings(player, house);
    }

    private void toggleVisitingMessageSetting(Player player, House house, int slot) {
        if (!canEditHouseSettings(player, house)) {
            player.sendMessage(plugin.color("&cYou cannot edit this house's settings."));
            openVisitingMessages(player, house);
            return;
        }
        if (slot == 10) house.setJoinMessageEnabled(!house.isJoinMessageEnabled());
        else if (slot == 12) house.setLeaveMessageEnabled(!house.isLeaveMessageEnabled());
        else if (slot == 14) house.setAnnounceJoinsEnabled(!house.isAnnounceJoinsEnabled());
        else if (slot == 15) house.setAnnounceLeavesEnabled(!house.isAnnounceLeavesEnabled());
        else return;
        houses.saveAll();
        player.sendMessage(plugin.color("&aUpdated Visiting Messages."));
        openVisitingMessages(player, house);
    }

    private void handleScoreboardEditorClick(Player player, House house, int slot) {
        if (!canEditHouseSettings(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (slot == 10) house.setScoreboardEnabled(!house.isScoreboardEnabled());
        else if (slot == 11) { beginScoreboardEdit(player, house, "title", -1); return; }
        else if (slot == 12) { openScoreboardLines(player, house); return; }
        else if (slot == 13) { beginScoreboardEdit(player, house, "add", -1); return; }
        else if (slot == 14) removeSelectedScoreboardLine(player, house);
        else if (slot == 15) moveSelectedScoreboardLine(player, house, -1);
        else if (slot == 16) moveSelectedScoreboardLine(player, house, 1);
        else if (slot == 20) { beginScoreboardEdit(player, house, "interval", -1); return; }
        else if (slot == 21) house.setScoreboardPlaceholdersEnabled(!house.isScoreboardPlaceholdersEnabled());
        else if (slot == 22) {
            refreshHouseScoreboard(house);
            player.sendMessage(plugin.color("&aPreview refreshed."));
            openScoreboardEditor(player, house);
            return;
        } else if (slot == 23) {
            house.resetScoreboard();
        } else if (slot == 49) {
            openSettings(player, house);
            return;
        } else return;
        saveScoreboard(player, house);
        openScoreboardEditor(player, house);
    }

    private void handleScoreboardLinesClick(Player player, House house, int slot) {
        if (!canEditHouseSettings(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (slot >= 0 && slot < 15 && slot < house.getScoreboardLines().size()) {
            selectedScoreboardLine.put(player.getUniqueId(), slot);
            beginScoreboardEdit(player, house, "line", slot);
            return;
        }
        if (slot == 45) { beginScoreboardEdit(player, house, "add", -1); return; }
        if (slot == 46) removeSelectedScoreboardLine(player, house);
        else if (slot == 47) moveSelectedScoreboardLine(player, house, -1);
        else if (slot == 48) moveSelectedScoreboardLine(player, house, 1);
        else if (slot == 49) { openScoreboardEditor(player, house); return; }
        else return;
        saveScoreboard(player, house);
        openScoreboardLines(player, house);
    }

    private void removeSelectedScoreboardLine(Player player, House house) {
        int selected = selectedScoreboardLine.getOrDefault(player.getUniqueId(), house.getScoreboardLines().size() - 1);
        house.removeScoreboardLine(selected);
        if (selected >= house.getScoreboardLines().size()) selected = house.getScoreboardLines().size() - 1;
        if (selected < 0) selectedScoreboardLine.remove(player.getUniqueId());
        else selectedScoreboardLine.put(player.getUniqueId(), selected);
    }

    private void moveSelectedScoreboardLine(Player player, House house, int direction) {
        int selected = selectedScoreboardLine.getOrDefault(player.getUniqueId(), -1);
        house.moveScoreboardLine(selected, direction);
        int target = selected + direction;
        if (target >= 0 && target < house.getScoreboardLines().size()) selectedScoreboardLine.put(player.getUniqueId(), target);
    }

    private void beginScoreboardEdit(Player player, House house, String type, int line) {
        if (!canEditHouseSettings(player, house)) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        scoreboardEdits.put(player.getUniqueId(), new ScoreboardEdit(house.getWorldName(), type, line));
        player.closeInventory();
        if ("title".equals(type)) player.sendMessage(plugin.color("&eType the new scoreboard title in chat. Type &ccancel &eto cancel."));
        else if ("interval".equals(type)) player.sendMessage(plugin.color("&eType the update interval in ticks. Type &ccancel &eto cancel."));
        else if ("add".equals(type)) player.sendMessage(plugin.color("&eType the new scoreboard line in chat. Type &ccancel &eto cancel."));
        else player.sendMessage(plugin.color("&eType the new scoreboard line in chat. Type &ccancel &eto cancel."));
    }

    private void saveScoreboard(Player player, House house) {
        houses.saveAll();
        refreshHouseScoreboard(house);
        player.sendMessage(plugin.color("&aSaved scoreboard settings."));
    }

    private void refreshHouseScoreboard(House house) {
        if (plugin.getHousingScoreboard() != null) plugin.getHousingScoreboard().refreshHouse(house);
    }

    private void beginVisitingMessageEdit(Player player, House house, String type) {
        if (!canEditHouseSettings(player, house)) {
            player.sendMessage(plugin.color("&cYou cannot edit this house's settings."));
            return;
        }
        visitingMessageEdits.put(player.getUniqueId(), new VisitingMessageEdit(house.getWorldName(), type));
        player.closeInventory();
        player.sendMessage(plugin.color("&aType the new " + ("join".equals(type) ? "join" : "leave") + " message in chat."));
        player.sendMessage(plugin.color("&7Use placeholders: &e%player% %owner% %house_name% %guests% %cookies% %weekly_cookies% %housing_group%"));
        player.sendMessage(plugin.color("&7Type &ccancel &7to cancel."));
    }

    private void beginTeleportPadRename(Player player, House house, House.TeleportPad pad) {
        if (!canEditHousingObjects(player, house) || pad == null) {
            player.sendMessage(plugin.color("&cYou cannot edit teleport pads here."));
            return;
        }
        teleportPadRenames.put(player.getUniqueId(), new TeleportPadRename(house.getWorldName(), pad.getId()));
        player.closeInventory();
        player.sendMessage(plugin.color("&aType the new Teleport Pad name in chat. Type &ccancel &ato cancel."));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVisitingMessageChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ScoreboardEdit scoreboardEdit = scoreboardEdits.remove(player.getUniqueId());
        if (scoreboardEdit != null) {
            event.setCancelled(true);
            String message = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> {
                House house = houses.getHouseByWorld(scoreboardEdit.worldName);
                if (house == null || !canEditHouseSettings(player, house)) {
                    player.sendMessage(plugin.color("&cYou cannot edit that house's scoreboard."));
                    return;
                }
                if ("cancel".equalsIgnoreCase(message.trim())) {
                    player.sendMessage(plugin.color("&cScoreboard edit cancelled."));
                    openScoreboardEditor(player, house);
                    return;
                }
                if ("title".equals(scoreboardEdit.type)) house.setScoreboardTitle(message);
                else if ("interval".equals(scoreboardEdit.type)) {
                    try { house.setScoreboardUpdateTicks(Integer.parseInt(message.trim())); } catch (NumberFormatException ignored) {}
                } else if ("add".equals(scoreboardEdit.type)) {
                    if (!house.addScoreboardLine(message)) player.sendMessage(plugin.color("&cScoreboards can only have 15 lines."));
                    else selectedScoreboardLine.put(player.getUniqueId(), house.getScoreboardLines().size() - 1);
                } else if ("line".equals(scoreboardEdit.type)) {
                    house.setScoreboardLine(scoreboardEdit.line, message);
                    selectedScoreboardLine.put(player.getUniqueId(), scoreboardEdit.line);
                }
                houses.saveAll();
                refreshHouseScoreboard(house);
                player.sendMessage(plugin.color("&aSaved scoreboard " + scoreboardEdit.type + "."));
                openScoreboardEditor(player, house);
            });
            return;
        }
        TeleportPadRename rename = teleportPadRenames.remove(player.getUniqueId());
        if (rename != null) {
            event.setCancelled(true);
            String message = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> {
                House house = houses.getHouseByWorld(rename.worldName);
                House.TeleportPad pad = house == null ? null : house.getTeleportPad(rename.padId);
                if (house == null || pad == null || !canEditHousingObjects(player, house)) {
                    player.sendMessage(plugin.color("&cYou cannot rename that Teleport Pad."));
                    return;
                }
                if ("cancel".equalsIgnoreCase(message.trim())) {
                    player.sendMessage(plugin.color("&cTeleport Pad rename cancelled."));
                    openTeleportPad(player, house, pad);
                    return;
                }
                pad.setName(message.trim());
                houses.saveAll();
                player.sendMessage(plugin.color("&aRenamed Teleport Pad to &e" + pad.getName() + "&a."));
                openTeleportPad(player, house, pad);
            });
            return;
        }
        VisitingMessageEdit edit = visitingMessageEdits.remove(player.getUniqueId());
        if (edit == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> {
            House house = houses.getHouseByWorld(edit.worldName);
            if (house == null || !canEditHouseSettings(player, house)) {
                player.sendMessage(plugin.color("&cYou cannot edit that house's messages."));
                return;
            }
            if ("cancel".equalsIgnoreCase(message.trim())) {
                player.sendMessage(plugin.color("&cMessage edit cancelled."));
                openVisitingMessages(player, house);
                return;
            }
            if ("join".equals(edit.type)) house.setJoinMessage(message);
            else house.setLeaveMessage(message);
            houses.saveAll();
            player.sendMessage(plugin.color("&aSaved the " + edit.type + " message."));
            openVisitingMessages(player, house);
        });
    }

    private void handlePvpLayoutClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            event.setCancelled(true);
            return;
        }
        Player player = (Player) event.getWhoClicked();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) house = houses.getHouse(player.getUniqueId());
        if (house == null || !canEditHouseSettings(player, house)) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(plugin.color("&cYou cannot edit this house's PvP layout."));
            return;
        }
        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot >= 0 && rawSlot < 36) return;
        if (rawSlot >= topSize) return;
        event.setCancelled(true);
        if (rawSlot == 45) {
            savePvpLayout(player, house, event.getView().getTopInventory());
        } else if (rawSlot == 48) {
            house.clearPvpLayout();
            houses.saveAll();
            player.sendMessage(plugin.color("&aReset the PvP layout."));
            openPvpLayoutEditor(player, house);
        } else if (rawSlot == 49) {
            openPvpDamageSettings(player, house);
        } else if (rawSlot == 53) {
            houses.applyPvpLayout(player, house);
            player.sendMessage(plugin.color(house.hasPvpLayout() ? "&aApplied the saved PvP layout." : "&cNo PvP layout is saved yet."));
        }
    }

    private void savePvpLayout(Player player, House house, Inventory inventory) {
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = inventory.getItem(i);
            contents[i] = item == null ? null : item.clone();
        }
        house.setPvpLayout(contents);
        houses.saveAll();
        player.sendMessage(plugin.color("&aSaved the PvP layout."));
        openPvpLayoutEditor(player, house);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!pvpLayoutTitle.equals(event.getView().getTitle())) return;
        if (!(event.getWhoClicked() instanceof Player)) {
            event.setCancelled(true);
            return;
        }
        Player player = (Player) event.getWhoClicked();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) house = houses.getHouse(player.getUniqueId());
        if (house == null || !canEditHouseSettings(player, house)) {
            event.setCancelled(true);
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot >= 36 && rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        event.getView().getTitle();
        String title = event.getView().getTitle();
        String socialTitle = "Social Menu";
        String visibilityTitle = "Visiting Rules";
        String placeholderTitle = "Housing Category";
        String browseResultsTitle = "Player Housing";
        String buildTitle = "Build Blocks";
        boolean isKnown = title.equals(mainTitle) || title.equals(ownerTitle) || title.equals(browseTitle) || title.equals(channelsTitle) || title.equals(browseResultsTitle) || title.equals(listTitle) || title.equals(randomTitle) || title.equals(leaderboardTitle)
                || title.equals(settingsTitle) || title.equals(scoreboardEditorTitle) || title.equals(scoreboardLinesTitle) || title.equals(plotUpgradeTitle) || title.equals(pvpDamageTitle) || title.equals(pvpLayoutTitle) || title.equals(visitingMessagesTitle) || title.equals(teleportPadsTitle) || title.equals(teleportPadTitle) || title.equals(teleportPadRelinkTitle) || title.equals(jukeboxTitle) || title.equals(playersTitle) || title.equals(carpenterTitle) || title.equals(carpenterRemoveTitle) || title.equals(visibilityModeTitle) || title.equals(visitingRulesTitle) || title.equals(weatherTitle) || title.equals(biomeSkyTitle) || title.equals(themesTitle) || title.equals(socialTitle) || title.equals(visibilityTitle) || title.equals(buildTitle) || title.equals(plantsTitle) || title.equals(placeholderTitle) || isBlocksTitle(title) || isPvpTitle(title) || isItemsTitle(title);
        if (!isKnown && plugin.getCustomCommandService() != null) isKnown = plugin.getCustomCommandService().isMenuTitle(title);
        if (!isKnown && plugin.getFunctionService() != null) isKnown = plugin.getFunctionService().isMenuTitle(title);
        isKnown = isKnown || title.equals(managePlayerTitle);
        isKnown = isKnown || title.equals(skullPacksTitle) || title.startsWith(skullPackTitlePrefix);
        if (!isKnown) return;
        if (title.equals(pvpLayoutTitle)) {
            handlePvpLayoutClick(event);
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == net.fakepixel.housing.util.Compat.mat("AIR")) return;

        if (title.equals(ownerTitle) && event.getInventory().getSize() == 27) {
            if (event.getSlot() == 11) { player.closeInventory(); houses.leave(player); return; }
            if (event.getSlot() == 13 || event.getSlot() == 15) { openBrowse(player, 0); return; }
            if (event.getSlot() == 14) { openPlayers(player); return; }
            if (event.getSlot() == 22) { openVisibilityMode(player); return; }
            return;
        }

        if (title.equals(visibilityModeTitle)) {
            int slot = event.getRawSlot();
            if (slot == 22) { player.closeInventory(); return; }
            int limit = visibilityLimitFromSlot(slot);
            if (limit >= 0) {
                visibilityLimits.put(player.getUniqueId(), limit);
                applyVisibilityLimit(player, limit);
                player.sendMessage(plugin.color("&aVisibility updated to &e" + limit + "&a players."));
                openVisibilityMode(player);
            }
            return;
        }

        if (title.equals(visitingRulesTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null || !house.canManage(player.getUniqueId())) { player.closeInventory(); player.sendMessage(plugin.msg("no-permission")); return; }
            if (event.getSlot() == 22) { openOwnerMenu(player); return; }
            String rule = visitingRuleFromSlot(event.getRawSlot());
            if (rule != null) {
                house.setVisitingRule(rule);
                houses.saveAll();
                player.sendMessage(plugin.color("&aVisiting Rules set to " + ruleColor(rule) + house.getVisitingRuleDisplay() + "&a."));
                openVisitingRules(player, house);
            }
            return;
        }

        if (title.equals(managePlayerTitle)) {
            handleManagePlayerClick(player, event.getSlot());
            return;
        }

        if (title.equals(mainTitle)) {
            if (event.getSlot() == 10) { player.closeInventory(); houses.sendToOwnHouse(player); }
            else if (event.getSlot() == 11) openBrowse(player, 0);
            else if (event.getSlot() == 12) openRandom(player);
            else if (event.getSlot() == 13) openList(player);
            else if (event.getSlot() == 14) openLeaderboard(player);
            else if (event.getSlot() == 16) player.closeInventory();
            return;
        }

        if (title.equals(carpenterTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null || (!house.canManage(player.getUniqueId()) && !canRemoveCarpenter(player, house))) { player.closeInventory(); player.sendMessage(plugin.msg("no-permission")); return; }
            int slot = event.getSlot();
            if (!house.canManage(player.getUniqueId()) && slot != 40) { player.sendMessage(plugin.msg("no-permission")); return; }
            if (slot == 10) { openBlocks(player, 0); return; }
            if (slot == 11) { openPlants(player); return; }
            if (slot == 14) { openItems(player); return; }
            if (slot == 18) { openBrowse(player, 0); return; }
            if (slot == 19) { player.closeInventory(); houses.leave(player); return; }
            if (slot == 49) { openOwnerMenu(player); return; }
            if (slot == 20) { openVisitingRules(player, house); return; }
            if (slot == 21) { openSettings(player, house); return; }
            if (slot == 22) { plugin.getGroupsMenu().open(player); return; }
            if (slot == 23) { openPlayers(player); return; }
            if (slot == 24) { player.sendMessage(plugin.color("&cJukebox is VIP only.")); return; }
            if (slot == 15) { player.sendMessage(plugin.color("&cFurniture is not available yet.")); return; }
            if (slot == 40) {
                if (!canRemoveCarpenter(player, house)) {
                    player.sendMessage(plugin.msg("no-permission"));
                    return;
                }
                openRemoveCarpenterConfirm(player, house);
                return;
            }
            return;
        }

        if (title.equals(carpenterRemoveTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null || !canRemoveCarpenter(player, house)) { player.closeInventory(); player.sendMessage(plugin.msg("no-permission")); return; }
            int slot = event.getSlot();
            if (slot == 11) {
                if (plugin.getHousingNpcService() != null) plugin.getHousingNpcService().removeCarpenter(house);
                else { house.setCarpenterRemoved(true); houses.saveAll(); }
                player.closeInventory();
                player.sendMessage(plugin.color("&aRemoved the Carpenter NPC from your house."));
                return;
            }
            if (slot == 15) {
                openCarpenterMenu(player);
                return;
            }
            return;
        }

        if (title.equals(ownerTitle)) {
            if (event.getRawSlot() == 36) {
                player.closeInventory();
                openBrowse(player, 0);
                return;
            }
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null || (!house.canManage(player.getUniqueId()) && !canEditHouseSettings(player, house))) { player.closeInventory(); player.sendMessage(plugin.msg("no-permission")); return; }
            int slot = event.getSlot();
            if (!house.canManage(player.getUniqueId())) {
                if (slot == 44) toggleHouseMode(player, house);
                else if (slot == 48) openSettings(player, house);
                else player.sendMessage(plugin.msg("no-permission"));
                return;
            }
            if (slot == 0) { openBuild(player); return; }
            if (slot == 2) { openBlocks(player, 0); return; }
            if (slot == 3) { openPlants(player); return; }
            if (slot == 4) { openItems(player); return; }
            if (slot == 11) { openPvp(player, 0); return; }
            if (slot == 12) { openWeather(player); return; }
            if (slot == 13) { openBiomesAndSkies(player); return; }
            if (slot == 14) { openThemes(player); return; }
            if (slot == 29 || slot == 50) { openPlayers(player); return; }
            if (slot == 48) { openSettings(player, house); return; }
            if (slot == 5) { openSkullPacks(player); return; }
            if (slot == 27) { openBrowse(player, 0); return; }
            if (slot == 6) { player.sendMessage(plugin.color("&cFurniture is not available yet.")); return; }
            if (slot == 45) { player.closeInventory(); houses.leave(player); return; }
            if (slot == 10 || slot == 31 || slot == 49) { plugin.getGroupsMenu().open(player); return; }
            if (slot == 23 || slot == 47) { openVisitingRules(player, house); return; }
            if (slot == 33) { player.closeInventory(); houses.resetHouse(player, house); return; }
            if (slot == 35) { openBuild(player); return; }
            if (slot == 41) { openVisibilityMode(player); return; }
            if (slot == 44) { toggleHouseMode(player, house); return; }
            if (slot == 53) { openJukebox(player, house); return; }
            if (slot == 21 || slot == 26 || slot == 30 || slot == 32) {
                openSimple(player, placeholderTitle, item(event.getCurrentItem().getType(), event.getCurrentItem().getItemMeta().getDisplayName(), "&7This category is ready to be hooked", "&7into your housing features."));
                return;
            }
            return;
        }

        if (title.equals(skullPacksTitle)) {
            if (event.getSlot() == 22) { openOwnerMenu(player); return; }
            SkullPack pack = skullPackFromItem(event.getCurrentItem());
            if (pack != null) handleSkullPackClick(player, pack);
            return;
        }

        if (title.startsWith(skullPackTitlePrefix)) {
            if (event.getSlot() == 49) { openSkullPacks(player); return; }
            SkullPack pack = skullPackFromItem(event.getCurrentItem());
            if (pack == null || !isSkullPackUnlocked(player, pack)) return;
            SkullEntry skull = skullFromItem(pack, event.getCurrentItem());
            if (skull != null) giveSkull(player, skull);
            return;
        }


        if (isBlocksTitle(title)) {
            if (!requireItemsMenuBuildPermission(player)) return;
            int page = currentBlocksPage(player);
            int maxPage = blockPages().size() - 1;
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
            if (slot == 48) {
                openBlocks(player, page <= 0 ? maxPage : page - 1);
                return;
            }
            if (slot == 49) { openOwnerMenu(player); return; }
            if (slot == 50) {
                openBlocks(player, page >= maxPage ? 0 : page + 1);
                return;
            }
            if (slot < 36 && isBuildBlock(event.getCurrentItem())) {
                giveStack(player, event.getCurrentItem());
            }
            return;
        }

        if (title.equals(plantsTitle)) {
            if (!requireItemsMenuBuildPermission(player)) return;
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
            if (slot == 49 || slot == 50) { openOwnerMenu(player); return; }
            if (slot < 45 && isPlantItem(event.getCurrentItem())) {
                giveStack(player, event.getCurrentItem());
            }
            return;
        }




        if (isPvpTitle(title)) {
            int page = pageFromPvpTitle(title);
            int maxPage = pvpPages().size() - 1;
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
            if (slot == 48) {
                if (page > 0) openPvp(player, page - 1);
                return;
            }
            if (slot == 49) { openOwnerMenu(player); return; }
            if (slot == 50) {
                if (page < maxPage) openPvp(player, page + 1);
                return;
            }
            Material material = event.getCurrentItem().getType();
            if (slot < 45 && isPvpItem(material)) {
                giveSingle(player, event.getCurrentItem());
            }
            return;
        }

        if (isItemsTitle(title)) {
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
            ItemStack clicked = event.getCurrentItem();
            if (!requireItemsMenuBuildPermission(player)) return;
            int page = pageFromItemsTitle(title);
            int maxPage = itemPages().size() - 1;
            if (slot == 45) { openItems(player, page <= 0 ? maxPage : page - 1); return; }
            if (slot == 49) { openOwnerMenu(player); return; }
            if (slot == 50) { player.closeInventory(); return; }
            if (slot == 53) { openItems(player, page >= maxPage ? 0 : page + 1); return; }
            if (slot < 45) {
                giveHousingItem(player, clicked);
            }
            return;
        }


        if (title.equals(themesTitle)) {
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
            if (slot == 18) { openThemes(player, safeThemePage(player) - 1); return; }
            if (slot == 22) { openOwnerMenu(player); return; }
            if (slot == 23) { player.closeInventory(); return; }
            if (slot == 26) { openThemes(player, safeThemePage(player) + 1); return; }
            ThemeDefinition theme = themeBySlot(player, slot);
            if (theme != null) { handleThemeClick(player, theme); return; }
            return;
        }


        if (title.equals(biomeSkyTitle)) {
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
            if (slot == 31) { openOwnerMenu(player); return; }
            String biome = biomeBySlot(slot);
            if (biome != null) { setHouseBiome(player, biome); return; }
            String sky = skyBySlot(slot);
            if (sky != null) { setHouseSky(player, sky); return; }
            return;
        }

        if (title.equals(weatherTitle)) {
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
            if (slot == 31) { openOwnerMenu(player); return; }
            if (slot == 11 || slot == 20) { setHouseWeather(player, "sunny"); return; }
            if (slot == 12 || slot == 23) { setHouseWeather(player, "raining"); return; }
            if (slot == 14 || slot == 24) { setHouseWeather(player, "storming"); return; }
            if (slot == 15) { setHouseWeather(player, "thunder"); return; }
            return;
        }

        if (title.equals(plotUpgradeTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null) { player.closeInventory(); return; }
            if (event.getSlot() == 22) { openSettings(player, house); return; }
            PlotTier tier = plotTierFromSlot(event.getRawSlot());
            if (tier != null) upgradePlot(player, house, tier);
            return;
        }

        if (title.equals(pvpDamageTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null) { player.closeInventory(); return; }
            int slot = event.getRawSlot();
            if (slot == 49) { openSettings(player, house); return; }
            if (slot == 31) { openPvpLayoutEditor(player, house); return; }
            togglePvpDamageSetting(player, house, slot);
            return;
        }

        if (title.equals(visitingMessagesTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null) { player.closeInventory(); return; }
            int slot = event.getRawSlot();
            if (slot == 31) { openSettings(player, house); return; }
            if (slot == 11) { beginVisitingMessageEdit(player, house, "join"); return; }
            if (slot == 13) { beginVisitingMessageEdit(player, house, "leave"); return; }
            toggleVisitingMessageSetting(player, house, slot);
            return;
        }

        if (title.equals(teleportPadsTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null || !canEditHousingObjects(player, house)) { player.closeInventory(); player.sendMessage(plugin.msg("no-permission")); return; }
            if (event.getRawSlot() == 49) { openSettings(player, house); return; }
            House.TeleportPad pad = teleportPadFromItem(house, event.getCurrentItem());
            if (pad != null) openTeleportPad(player, house, pad);
            return;
        }

        if (title.equals(teleportPadTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null || !canEditHousingObjects(player, house)) { player.closeInventory(); player.sendMessage(plugin.msg("no-permission")); return; }
            House.TeleportPad pad = teleportPadFromItem(house, event.getView().getTopInventory().getItem(35));
            if (pad == null) { openTeleportPads(player, house); return; }
            int slot = event.getRawSlot();
            if (slot == 10) { beginTeleportPadRename(player, house, pad); return; }
            if (slot == 11) { openTeleportPadRelink(player, house, pad); return; }
            if (slot == 12) { house.removeTeleportPad(pad.getId()); houses.saveAll(); player.sendMessage(plugin.color("&cDeleted Teleport Pad.")); openTeleportPads(player, house); return; }
            if (slot == 14) { pad.setParticles(!pad.isParticles()); houses.saveAll(); openTeleportPad(player, house, pad); return; }
            if (slot == 15) { pad.setSound(!pad.isSound()); houses.saveAll(); openTeleportPad(player, house, pad); return; }
            if (slot == 31) { openTeleportPads(player, house); return; }
            return;
        }

        if (title.equals(teleportPadRelinkTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null || !canEditHousingObjects(player, house)) { player.closeInventory(); player.sendMessage(plugin.msg("no-permission")); return; }
            House.TeleportPad source = teleportPadFromItem(house, event.getView().getTopInventory().getItem(53));
            if (source == null) { openTeleportPads(player, house); return; }
            int slot = event.getRawSlot();
            if (slot == 48) { house.unlinkTeleportPad(source.getId()); houses.saveAll(); openTeleportPad(player, house, source); return; }
            if (slot == 49) { openTeleportPad(player, house, source); return; }
            House.TeleportPad target = teleportPadFromItem(house, event.getCurrentItem());
            if (target != null && !target.getId().equals(source.getId())) {
                house.linkTeleportPads(source.getId(), target.getId());
                houses.saveAll();
                player.sendMessage(plugin.color("&aLinked &e" + source.getName() + " &ato &e" + target.getName() + "&a."));
                openTeleportPad(player, house, source);
            }
            return;
        }

        if (title.equals(jukeboxTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null || !canEditHouseSettings(player, house)) { player.closeInventory(); player.sendMessage(plugin.msg("no-permission")); return; }
            int slot = event.getRawSlot();
            if (slot == 45) {
                house.setJukeboxSong("");
                house.setJukeboxShuffle(false);
                house.setJukeboxEnabled(false);
                houses.saveAll();
                plugin.getJukeboxService().stopHouseMusic(house);
                openJukebox(player, house);
                return;
            }
            if (slot == 46) {
                JukeboxService.Song song = plugin.getJukeboxService().randomSong();
                house.setJukeboxSong(song.id());
                house.setJukeboxShuffle(true);
                house.setJukeboxEnabled(true);
                houses.saveAll();
                plugin.getJukeboxService().restartHouseMusic(house);
                openJukebox(player, house);
                return;
            }
            if (slot == 47) {
                house.setJukeboxEnabled(!house.isJukeboxEnabled());
                houses.saveAll();
                if (house.isJukeboxEnabled()) plugin.getJukeboxService().restartHouseMusic(house);
                else plugin.getJukeboxService().stopHouseMusic(house);
                openJukebox(player, house);
                return;
            }
            if (slot == 48) {
                house.setJukeboxVolume(house.getJukeboxVolume() - 10);
                houses.saveAll();
                plugin.getJukeboxService().restartHouseMusic(house);
                openJukebox(player, house);
                return;
            }
            if (slot == 49) { openSettings(player, house); return; }
            if (slot == 50) {
                house.setJukeboxVolume(house.getJukeboxVolume() + 10);
                houses.saveAll();
                plugin.getJukeboxService().restartHouseMusic(house);
                openJukebox(player, house);
                return;
            }
            if (slot == 53) { player.closeInventory(); return; }
            JukeboxService.Song song = jukeboxSongFromItem(event.getCurrentItem());
            if (song != null) {
                house.setJukeboxSong(song.id());
                house.setJukeboxShuffle(false);
                house.setJukeboxEnabled(true);
                houses.saveAll();
                plugin.getJukeboxService().restartHouseMusic(house);
                player.sendMessage(plugin.color("&aSelected house music: " + song.displayName() + "&a."));
                openJukebox(player, house);
            }
            return;
        }

        if (title.equals(settingsTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null || !canEditHouseSettings(player, house)) { player.closeInventory(); return; }
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
            if (slot == 40) { openOwnerMenu(player); return; }
            if (slot == 13) { openPlotUpgrade(player, house); return; }
            if (slot == 41) { player.closeInventory(); houses.resetHouse(player, house); return; }
            if (slot == 12) { player.closeInventory(); player.sendMessage(plugin.color("&eUse &6/housing name <name> &eto change your house name.")); return; }
            if (slot == 10) { player.closeInventory(); player.sendMessage(plugin.color("&eUse &6/time <time> &eto change your house time.")); return; }
            if (slot == 14) { player.closeInventory(); player.sendMessage(plugin.color("&eUse &6/houselang <lang> &eto edit house languages.")); return; }
            if (slot == 15) { player.closeInventory(); player.sendMessage(plugin.color("&eUse &6/housetag <tag> &eto edit house tags.")); return; }
            if (slot == 20) { openVisitingMessages(player, house); return; }
            if (slot == 21 || slot == 22) { player.sendMessage(plugin.color("&aSetting toggled.")); openSettings(player, house); return; }
            if (slot == 24) { openPvpDamageSettings(player, house); return; }
            if (slot == 25) { openTeleportPads(player, house); return; }
            if (slot == 26) { openScoreboardEditor(player, house); return; }
            if (slot == 28 && plugin.getCustomCommandService() != null) { plugin.getCustomCommandService().open(player, house); return; }
            if (slot == 29 && plugin.getFunctionService() != null) { plugin.getFunctionService().open(player, house); return; }
            return;
        }

        if (title.equals(scoreboardEditorTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null || !canEditHouseSettings(player, house)) { player.closeInventory(); return; }
            handleScoreboardEditorClick(player, house, event.getRawSlot());
            return;
        }

        if (title.equals(scoreboardLinesTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null || !canEditHouseSettings(player, house)) { player.closeInventory(); return; }
            handleScoreboardLinesClick(player, house, event.getRawSlot());
            return;
        }

        if (title.equals(playersTitle)) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null || !house.canManage(player.getUniqueId())) { player.closeInventory(); return; }
            if (event.getSlot() == 49) { openOwnerMenu(player); return; }
            if (event.getSlot() == 45) { player.closeInventory(); houses.kickGuests(player, house); return; }
            if (event.getSlot() == 46) { player.closeInventory(); houses.kickAll(player, house); return; }
            Player target = playerFromItem(event.getCurrentItem());
            if (target != null) openPlayerManageMenu(player, target);
            return;
        }

        if (title.equals(placeholderTitle) || title.equals(buildTitle) || title.equals(socialTitle) || title.equals(visibilityTitle)) {
            if (event.getSlot() == 31) openOwnerMenu(player);
            if (title.equals(buildTitle) && event.getSlot() == 0) { player.closeInventory(); player.performCommand("wand"); }
            if (title.equals(buildTitle) && event.getSlot() == 4) { player.closeInventory(); player.performCommand("gmc"); }
            return;
        }

        if (title.equals(randomTitle)) {
            if (event.getSlot() == 13) {
                House random = houses.getRandomPublicHouse();
                player.closeInventory();
                if (random == null) player.sendMessage(plugin.color("&cThere are no public houses to join."));
                else houses.visit(player, random);
            } else if (event.getSlot() == 22) open(player);
            return;
        }

        if (title.equals(listTitle)) {
            if (event.getSlot() == 22) { open(player); return; }
            Integer plotId = plotIdFromItem(event.getCurrentItem());
            if (plotId != null) {
                House own = houses.getHouse(player.getUniqueId(), plotId);
                if (own != null && event.getClick() == ClickType.RIGHT) {
                    own.setPublicHouse(!own.isPublicHouse());
                    houses.saveAll();
                    openList(player);
                    return;
                }
                if (!houses.isPlotOwned(player.getUniqueId(), plotId)) {
                    int price = houses.plotPrice(plotId);
                    if (!houses.purchasePlot(player, plotId)) {
                        player.sendMessage(plugin.color("&cYou need &6" + price + " Gold &cto unlock House #" + plotId + "."));
                        return;
                    }
                    player.sendMessage(plugin.color("&aUnlocked &eHouse #" + plotId + " &afor &6" + price + " Gold&a."));
                    openList(player);
                    return;
                }
                player.closeInventory();
                houses.sendToHousePlot(player, plotId);
            }
            return;
        }

        if (title.equals(browseTitle)) {
            Integer row = browseRowFromItem(event.getCurrentItem());
            if (row != null) {
                selectingBrowseRow.put(player.getUniqueId(), row);
                openChannels(player);
                return;
            }
        }
        if (title.equals(channelsTitle)) {
            if (event.getSlot() == 48) { openBrowseMain(player); return; }
            if (event.getSlot() == 49) { open(player); return; }
            String channel = browseChannelFromItem(event.getCurrentItem());
            if (channel != null) {
                int row = selectingBrowseRow.getOrDefault(player.getUniqueId(), 0);
                setBrowseRowChannel(player, row, channel);
                openBrowseMain(player);
                return;
            }
        }
        if (title.equals(browseResultsTitle)) {
            openBrowseMain(player);
            return;
        }
        House clicked = houseFromItem(event.getCurrentItem());
        if (clicked != null) {
            if (event.getClick() == ClickType.RIGHT && clicked.getOwner().equals(player.getUniqueId())) {
                openList(player);
                return;
            }
            player.closeInventory();
            houses.visit(player, clicked);
        }
    }

    private void handleManagePlayerClick(Player manager, int slot) {
        House house = houses.getHouseByWorld(manager.getWorld().getName());
        if (house == null || !canOpenPlayerOptions(manager, house)) { manager.closeInventory(); return; }
        Player target = targetFromOpenManageMenu(manager);
        if (target == null || !target.getWorld().getName().equals(house.getWorldName())) {
            manager.closeInventory();
            manager.sendMessage(plugin.color("&cThat player is no longer in this house."));
            return;
        }
        if (slot == 31) { openPlayers(manager); return; }
        if (slot == 35) { manager.closeInventory(); return; }
        if (slot == 10) {
            if (!canUsePlayerOption(manager, house, "teleport")) {
                manager.sendMessage(plugin.color("&cYour group cannot teleport to players."));
                openPlayerManageMenu(manager, target);
                return;
            }
            manager.closeInventory();
            manager.teleport(target);
            manager.sendMessage(plugin.color("&aTeleported to &e" + target.getName() + "&a."));
            return;
        }
        if (slot == 14) {
            if (!canUsePlayerOption(manager, house, "manage") || !canManageTarget(manager, target, house)) {
                manager.sendMessage(plugin.color("&cYou cannot change that player's group."));
                openPlayerManageMenu(manager, target);
                return;
            }
            String next = nextGroup(house.getGroup(target.getUniqueId()));
            house.setGroup(target.getUniqueId(), next);
            houses.saveAll();
            houses.updateVisitItems(target);
            if (plugin.getNametagService() != null) plugin.getNametagService().update(target);
            manager.sendMessage(plugin.color("&aSet &e" + target.getName() + " &ato " + house.groupSuffix(next) + "&a."));
            openPlayerManageMenu(manager, target);
            return;
        }
        if (slot == 11) {
            if (!canUsePlayerOption(manager, house, "kick") || !canManageTarget(manager, target, house)) {
                manager.sendMessage(plugin.color("&cYou cannot kick that player."));
                openPlayerManageMenu(manager, target);
                return;
            }
            if (houses.isPunishmentProtected(target)) {
                manager.sendMessage(plugin.color("&cYou cannot punish staff members."));
                openPlayerManageMenu(manager, target);
                return;
            }
            manager.closeInventory();
            houses.kick(manager, target, house);
            return;
        }
        if (slot == 13) {
            if (!canUsePlayerOption(manager, house, "mute") || !canManageTarget(manager, target, house)) {
                manager.sendMessage(plugin.color("&cYou cannot mute that player."));
                openPlayerManageMenu(manager, target);
                return;
            }
            if (house.isMuted(target.getUniqueId())) {
                house.getMuted().remove(target.getUniqueId());
                manager.sendMessage(plugin.color("&aUnmuted &e" + target.getName() + "&a."));
            } else {
                if (houses.isPunishmentProtected(target)) {
                    manager.sendMessage(plugin.color("&cYou cannot punish staff members."));
                    openPlayerManageMenu(manager, target);
                    return;
                }
                house.getMuted().add(target.getUniqueId());
                manager.sendMessage(plugin.color("&cMuted &e" + target.getName() + "&c."));
            }
            houses.saveAll();
            openPlayerManageMenu(manager, target);
            return;
        }
        if (slot == 12) {
            if (!canUsePlayerOption(manager, house, "ban") || !canManageTarget(manager, target, house)) {
                manager.sendMessage(plugin.color("&cYou cannot ban that player."));
                openPlayerManageMenu(manager, target);
                return;
            }
            if (houses.isPunishmentProtected(target)) {
                manager.sendMessage(plugin.color("&cYou cannot punish staff members."));
                openPlayerManageMenu(manager, target);
                return;
            }
            manager.closeInventory();
            house.getBanned().add(target.getUniqueId());
            houses.saveAll();
            houses.leave(target);
            target.sendMessage(plugin.color("&cYou were banned from &e" + house.getName() + "&c."));
            manager.sendMessage(plugin.color("&cBanned &e" + target.getName() + "&c."));
            return;
        }
        if (slot == 15) {
            manager.sendMessage(plugin.color("&6Player Info &8> &e" + target.getName()
                    + " &7Group: " + house.groupSuffix(house.getGroup(target.getUniqueId()))
                    + " &7Muted: " + (house.isMuted(target.getUniqueId()) ? "&cYes" : "&aNo")
                    + " &7Banned: " + (house.isBanned(target.getUniqueId()) ? "&cYes" : "&aNo")));
        }
    }

    private Player targetFromOpenManageMenu(Player manager) {
        UUID targetId = playerOptionTargets.get(manager.getUniqueId());
        return targetId == null ? null : Bukkit.getPlayer(targetId);
    }

    public boolean canOpenPlayerOptions(Player manager, House house) {
        if (manager == null || house == null) return false;
        if (house.getOwner().equals(manager.getUniqueId())) return true;
        if (HouseGroups.power(house.getGroup(manager.getUniqueId())) < HouseGroups.power(HouseGroups.CO_OWNER)) return false;
        return canUsePlayerOption(manager, house, "teleport")
                || canUsePlayerOption(manager, house, "kick")
                || canUsePlayerOption(manager, house, "ban")
                || canUsePlayerOption(manager, house, "mute")
                || canUsePlayerOption(manager, house, "manage");
    }

    private boolean canUsePlayerOption(Player manager, House house, String permission) {
        return manager != null && house != null
                && (house.getOwner().equals(manager.getUniqueId()) || house.hasGroupPermission(manager.getUniqueId(), permission));
    }

    private ItemStack playerOptionItem(House house, Player manager, Player target, String permission, Material material, String name, String... lore) {
        return playerOptionItem(house, manager, target, permission, new ItemStack(material), name, lore);
    }

    private ItemStack playerOptionItem(House house, Player manager, Player target, String permission, ItemStack icon, String name, String... lore) {
        List<String> lines = new ArrayList<>(Arrays.asList(lore));
        boolean canUse = canUsePlayerOption(manager, house, permission);
        boolean punishAction = permission.equals("kick") || permission.equals("ban") || permission.equals("mute") || permission.equals("manage");
        if (punishAction && !canManageTarget(manager, target, house)) canUse = false;
        if ((permission.equals("kick") || permission.equals("ban") || permission.equals("mute")) && houses.isPunishmentProtected(target)) canUse = false;
        lines.add("");
        lines.add(canUse ? "&eClick to use!" : "&cNo permission.");
        return item(icon, canUse ? name : "&7" + ChatColor.stripColor(plugin.color(name)), lines.toArray(new String[0]));
    }

    private boolean canManageTarget(Player manager, Player target, House house) {
        if (manager == null || target == null || house == null) return false;
        if (target.getUniqueId().equals(house.getOwner())) return false;
        if (manager.getUniqueId().equals(house.getOwner())) return true;
        return HouseGroups.power(house.getGroup(manager.getUniqueId())) > HouseGroups.power(house.getGroup(target.getUniqueId()));
    }

    private String nextGroup(String current) {
        String group = HouseGroups.normalize(current);
        if (HouseGroups.GUEST.equals(group)) return HouseGroups.RESIDENT;
        if (HouseGroups.RESIDENT.equals(group)) return HouseGroups.CO_OWNER;
        return HouseGroups.GUEST;
    }

    private Player playerFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (!clean.startsWith("UUID: ")) continue;
            try { return Bukkit.getPlayer(UUID.fromString(clean.substring("UUID: ".length()))); } catch (Exception ignored) {}
        }
        return null;
    }

    private House.TeleportPad teleportPadFromItem(House house, ItemStack item) {
        if (house == null || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (!clean.startsWith("Teleport Pad ID: ")) continue;
            return house.getTeleportPad(clean.substring("Teleport Pad ID: ".length()));
        }
        return null;
    }

    private House houseFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        java.util.UUID owner = null;
        int plotId = 1;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Owner UUID: ")) {
                try { owner = java.util.UUID.fromString(clean.substring("Owner UUID: ".length())); } catch (Exception ignored) {}
            }
            if (clean.startsWith("House Plot: ")) {
                try { plotId = Integer.parseInt(clean.substring("House Plot: ".length())); } catch (Exception ignored) {}
            }
        }
        return owner == null ? null : houses.getHouse(owner, plotId);
    }

    private Integer plotIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (!clean.startsWith("House Plot: ")) continue;
            try { return Integer.parseInt(clean.substring("House Plot: ".length())); } catch (Exception ignored) { return null; }
        }
        return null;
    }

    private String prefixOf(House house) {
        Player owner = Bukkit.getPlayer(house.getOwner());
        return owner == null ? "" : abelix.getPrefix(owner);
    }

    private ItemStack houseItem(House house, String... extraLore) {
        List<String> lore = new ArrayList<>();
        lore.add("&7House owner:");
        lore.add("&r" + plugin.color(prefixOf(house)) + house.getOwnerName());
        lore.add("");
        lore.add("&7Players: &a" + houses.getGuests(house));
        lore.add("");
        lore.add("&7Cookies: &a" + house.getCookies());
        lore.add("");
        lore.addAll(Arrays.asList(extraLore));
        lore.add("&8Owner UUID: " + house.getOwner().toString());
        lore.add("&8House Plot: " + house.getPlotId());
        return item(net.fakepixel.housing.util.Compat.mat("GRASS_BLOCK"), "&a" + house.getName(), lore.toArray(new String[0]));
    }

    private ItemStack playerItem(House house, Player player) {
        return item(net.fakepixel.housing.util.Compat.mat("PLAYER_HEAD"),
                "&a" + player.getName(),
                "&7Group: " + house.groupSuffix(house.getGroup(player.getUniqueId())),
                "&7Muted: " + (house.isMuted(player.getUniqueId()) ? "&cYes" : "&aNo"),
                "&7Banned: " + (house.isBanned(player.getUniqueId()) ? "&cYes" : "&aNo"),
                "",
                "&eClick to manage.",
                "&8UUID: " + player.getUniqueId());
    }

    private ItemStack targetToken(Player target) {
        return item(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Target: &f" + target.getName(), "&8UUID: " + target.getUniqueId());
    }

    private ItemStack skull(String name, String... lore) {
        ItemStack stack = new ItemStack(net.fakepixel.housing.util.Compat.mat("PLAYER_HEAD"));
        return apply(stack, name, lore);
    }



    private ItemStack visibilityLimitItem(int limit, int selected) {
        String name = limit == 0 ? "&aVisibility 0" : "&aVisibility " + limit;
        List<String> lore = new ArrayList<>();
        if (selected == limit) lore.add("&aSelected!");
        else lore.add("&eClick to select!");
        return apply(selected == limit ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), name, lore.toArray(new String[0]));
    }

    private int visibilityLimitFromSlot(int slot) {
        if (slot == 10) return 0;
        if (slot == 11) return 20;
        if (slot == 12) return 50;
        if (slot == 13) return 100;
        return -1;
    }

    private void applyVisibilityLimit(Player viewer, int limit) {
        if (viewer == null || viewer.getWorld() == null) return;
        int shown = 0;
        for (Player other : viewer.getWorld().getPlayers()) {
            if (other.equals(viewer)) continue;
            if (shown < limit) {
                try { viewer.showPlayer(other); } catch (Throwable ignored) { }
                shown++;
            } else {
                try { viewer.hidePlayer(other); } catch (Throwable ignored) { }
            }
        }
    }

    private ItemStack visitingRuleItem(String rule, String display, String description, String selectedRule) {
        boolean selected = rule.equals(selectedRule);
        ItemStack stack = selected ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye();
        return apply(stack, ruleColor(rule) + display,
                description,
                "",
                "&6Alias: &7/visitingrule <rule>",
                selected ? "&aSelected!" : ("private".equals(rule) ? "&cClick to disable!" : "&eClick to enable!"));
    }

    private String visitingRuleFromSlot(int slot) {
        if (slot == 10) return "friends";
        if (slot == 11) return "party";
        if (slot == 12) return "public";
        if (slot == 13) return "private";
        if (slot == 14) return "guild";
        return null;
    }

    private String ruleColor(String rule) {
        if ("private".equals(rule)) return "&c";
        if ("friends".equals(rule)) return "&a";
        if ("party".equals(rule)) return "&d";
        if ("guild".equals(rule)) return "&b";
        return "&a";
    }

    private ItemStack dyeVisibilityMode() {
        ItemStack stack = net.fakepixel.housing.util.Compat.grayDye();
        return apply(stack, "&aVisibility Mode &7(Right Click)",
                "&7This item is used to decide how many",
                "&7players you can see!",
                "",
                "&eRight Click to open Visibility Menu!");
    }

    private ItemStack dye(String... lore) {
        ItemStack stack = net.fakepixel.housing.util.Compat.grayDye();
        return apply(stack, "&aVisibility", lore);
    }

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat);
        return apply(stack, name, lore);
    }

    private ItemStack item(ItemStack stack, String name, String... lore) {
        return apply(stack, name, lore);
    }

    private Material legacyMat(String name) {
        Material material = Material.matchMaterial(name);
        return material == null ? net.fakepixel.housing.util.Compat.mat(name) : material;
    }

    @SuppressWarnings("deprecation")
    private ItemStack legacyItem(String name, int data) {
        return new ItemStack(legacyMat(name), 1, (short) data);
    }

    private ItemStack apply(ItemStack stack, String name, String... lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(plugin.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(plugin.color(line));
        meta.setLore(colored);
        stack.setItemMeta(meta);
        return stack;
    }

    private static class VisitingMessageEdit {
        private final String worldName;
        private final String type;

        private VisitingMessageEdit(String worldName, String type) {
            this.worldName = worldName;
            this.type = type;
        }
    }

    private static class ScoreboardEdit {
        private final String worldName;
        private final String type;
        private final int line;

        private ScoreboardEdit(String worldName, String type, int line) {
            this.worldName = worldName;
            this.type = type;
            this.line = line;
        }
    }

    private static class TeleportPadRename {
        private final String worldName;
        private final String padId;

        private TeleportPadRename(String worldName, String padId) {
            this.worldName = worldName;
            this.padId = padId;
        }
    }

    private static class ThemeDefinition {
        private final String id;
        private final String displayName;
        private final String icon;
        private final String unlockType;
        private final int price;
        private final String permission;
        private final String slimeworld;
        private final String biome;
        private final String sky;
        private final String weather;
        private final List<String> lore;

        private ThemeDefinition(String id, String displayName, String icon, String unlockType, int price, String permission, String slimeworld, String biome, String sky, String weather, List<String> lore) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.unlockType = unlockType;
            this.price = price;
            this.permission = permission;
            this.slimeworld = slimeworld;
            this.biome = biome;
            this.sky = sky;
            this.weather = weather;
            this.lore = lore;
        }
    }
}
