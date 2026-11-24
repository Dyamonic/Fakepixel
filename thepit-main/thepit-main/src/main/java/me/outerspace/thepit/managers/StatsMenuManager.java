package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.NetworkProfileSnapshot;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StatsMenuManager implements Listener {
    private static final List<Integer> INNER_SLOTS = Arrays.asList(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );
    private final ThePitPlugin plugin;
    private final NumberFormat whole = NumberFormat.getIntegerInstance(Locale.US);
    private final Set<String> warnedMaterials = new java.util.HashSet<>();

    public StatsMenuManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        warnedMaterials.clear();
    }

    public void open(Player player) {
        open(player, player);
    }

    public void open(Player viewer, Player target) {
        if (viewer == null || target == null) return;
        if (!plugin.getStatsManager().isStatsMenuUnlocked(target.getUniqueId()) && !me.outerspace.thepit.util.PermissionCompat.has(plugin, viewer, "thepit.stats.bypass")) {
            viewer.sendMessage(CC.color(plugin.registry().messages().getString("messages.stats-locked", "&cThe Statistics menu permanently unlocks at Level 50.")));
            return;
        }
        openMain(viewer, target.getUniqueId(), target.getName());
    }

    public void refreshIfOpen(Player player) {
        if (player == null || !player.isOnline()) return;
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (!(holder instanceof Session session) || !session.viewer.equals(player.getUniqueId())) return;
        if (session.type == MenuType.MAIN) openMain(player, session.target, session.targetName);
        else if (session.type == MenuType.SUMMARY) openSummary(player, session);
        else openCategory(player, session, session.type);
    }

    private void openMain(Player viewer, UUID target, String targetName) {
        Session session = session(viewer, target, targetName, MenuType.MAIN);
        Inventory inventory = create(session, path("main.title", "&8Statistics"));
        set(inventory, session, slot("main.offensive-slot", 10), Action.OFFENSIVE, icon("offensive", Material.DIAMOND_SWORD, "&cOffensive Stats", Arrays.asList("&7View combat and damage dealt.")));
        set(inventory, session, slot("main.defensive-slot", 12), Action.DEFENSIVE, icon("defensive", Material.IRON_CHESTPLATE, "&9Defensive Stats", Arrays.asList("&7View deaths and damage taken.")));
        set(inventory, session, slot("main.performance-slot", 14), Action.PERFORMANCE, icon("performance", Material.EXP_BOTTLE, "&bPerformance Stats", Arrays.asList("&7View ratios and hourly rates.")));
        set(inventory, session, slot("main.miscellaneous-slot", 16), Action.MISCELLANEOUS, icon("miscellaneous", Material.PAPER, "&eMiscellaneous Stats", Arrays.asList("&7View activity statistics.")));
        set(inventory, session, slot("main.summary-slot", 31), Action.SUMMARY, icon("summary", Material.SKULL_ITEM, "&fPlayer Summary", Arrays.asList("&7View progression and passive tiers.")));
        set(inventory, session, slot("main.close-slot", 40), Action.CLOSE, icon("close", Material.BARRIER, "&cClose", new ArrayList<>()));
        viewer.openInventory(inventory);
    }

    private void openCategory(Player viewer, Session previous, MenuType type) {
        Session session = session(viewer, previous.target, previous.targetName, type);
        Inventory inventory = create(session, title(type));
        List<DisplayStat> values = switch (type) {
            case OFFENSIVE -> offensive(previous.target);
            case DEFENSIVE -> defensive(previous.target);
            case PERFORMANCE -> performance(previous.target);
            case MISCELLANEOUS -> miscellaneous(previous.target);
            default -> new ArrayList<>();
        };
        int index = 0;
        for (DisplayStat value : values) {
            if (index >= INNER_SLOTS.size()) break;
            inventory.setItem(INNER_SLOTS.get(index++), statIcon(value));
        }
        set(inventory, session, slot("submenu.back-slot", 40), Action.BACK, icon("back", Material.ARROW, "&aGo Back", new ArrayList<>()));
        viewer.openInventory(inventory);
    }

    private void openSummary(Player viewer, Session previous) {
        Player targetPlayer = Bukkit.getPlayer(previous.target);
        if (targetPlayer == null) {
            viewer.sendMessage(CC.color("&cThat player's loaded profile is no longer available."));
            openMain(viewer, previous.target, previous.targetName);
            return;
        }
        StatsManager stats = plugin.getStatsManager();
        Session session = session(viewer, previous.target, previous.targetName, MenuType.SUMMARY);
        Inventory inventory = create(session, path("summary.title", "&8%player%'s Statistics").replace("%player%", previous.targetName));
        NetworkProfileSnapshot network = plugin.getNetworkProfileCache().cached(previous.target);
        String prefix = network == null ? "" : network.getPrefix();
        String gold = network == null ? "Unavailable" : whole.format(network.getGold());
        List<String> identity = Arrays.asList(
                "&7Player: &r" + prefix + previous.targetName,
                "&7Pit Level: " + stats.getFormattedLevel(previous.target),
                "&7Current XP: &b" + whole.format(stats.getLevelXp(previous.target)),
                "&7XP Needed: &b" + whole.format(Math.max(0, stats.getRequiredXp(previous.target) - stats.getLevelXp(previous.target))),
                "&7Prestige: &d" + (stats.getPrestigeRoman(previous.target).isEmpty() ? "0" : stats.getPrestigeRoman(previous.target)),
                "&7Renown: &e" + whole.format(stats.getRenown(previous.target)),
                "&7Lifetime XP: &b" + whole.format(stats.getLifetimeXp(previous.target))
        );
        List<String> economy = Arrays.asList(
                "&7Pit Gold: &6" + whole.format(stats.getCoins(previous.target, Period.LIFETIME)),
                "&7Abelix Gold: &e" + gold,
                "&7Bounty: &6" + whole.format(stats.getBounty(previous.target)),
                "&7Hours Played: &f" + ratioFormat(hours(previous.target)),
                "&7Fishing Club: &b" + roman(plugin.getFishingManager().clubTier(targetPlayer))
        );
        List<String> combat = Arrays.asList(
                "&7Kills: &f" + whole.format(stats.getKills(previous.target, Period.LIFETIME)),
                "&7Assists: &f" + whole.format(stats.getAssists(previous.target)),
                "&7Deaths: &f" + whole.format(stats.getDeaths(previous.target, Period.LIFETIME)),
                "&7K/D: &f" + ratioFormat(stats.getKd(previous.target, Period.LIFETIME)),
                "&7Current Streak: &f" + whole.format(stats.getKillStreak(previous.target, Period.LIFETIME)),
                "&7Highest Streak: &f" + whole.format(stats.getHighestStreak(previous.target))
        );
        List<String> passives = new ArrayList<>();
        for (me.outerspace.thepit.perk.PassiveDefinition definition : plugin.getPassiveManager().definitions()) {
            int tier = plugin.getPassiveManager().tier(previous.target, definition.id());
            passives.add(definition.displayName() + "&7: &e" + plugin.getPassiveManager().roman(tier));
        }
        inventory.setItem(11, item(Material.SKULL_ITEM, (short) 3, "&f" + previous.targetName, identity));
        inventory.setItem(13, item(Material.GOLD_INGOT, (short) 0, "&6Progression & Economy", economy));
        inventory.setItem(15, item(Material.DIAMOND_SWORD, (short) 0, "&cCombat", combat));
        inventory.setItem(22, item(Material.NETHER_STAR, (short) 0, "&dPassive Upgrades", passives));
        set(inventory, session, slot("submenu.back-slot", 40), Action.BACK, icon("back", Material.ARROW, "&aGo Back", new ArrayList<>()));
        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Session)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Session session = (Session) top.getHolder();
        if (!session.viewer.equals(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.DOUBLE_CLICK
                || event.isShiftClick() || event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) {
            player.updateInventory();
            return;
        }
        Action action = session.actions.get(event.getRawSlot());
        if (action == null) return;
        switch (action) {
            case OFFENSIVE -> openCategory(player, session, MenuType.OFFENSIVE);
            case DEFENSIVE -> openCategory(player, session, MenuType.DEFENSIVE);
            case PERFORMANCE -> openCategory(player, session, MenuType.PERFORMANCE);
            case MISCELLANEOUS -> openCategory(player, session, MenuType.MISCELLANEOUS);
            case SUMMARY -> openSummary(player, session);
            case BACK -> openMain(player, session.target, session.targetName);
            case CLOSE -> player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Session) event.setCancelled(true);
    }

    private List<DisplayStat> offensive(UUID uuid) {
        StatsManager stats = plugin.getStatsManager();
        return Arrays.asList(
                value("kills", "&cKills", stats.getKills(uuid, Period.LIFETIME)),
                value("assists", "&eAssists", stats.getAssists(uuid)),
                value("sword-hits", "&fSword Hits", stats.getLongStat(uuid, StatsManager.SWORD_HITS)),
                value("arrows-shot", "&fArrows Shot", stats.getLongStat(uuid, StatsManager.ARROWS_SHOT)),
                decimal("damage-dealt", "&cDamage Dealt", stats.getDoubleStat(uuid, StatsManager.DAMAGE_DEALT)),
                decimal("melee-damage-dealt", "&cMelee Damage Dealt", stats.getDoubleStat(uuid, StatsManager.MELEE_DAMAGE_DEALT)),
                decimal("bow-damage-dealt", "&cBow Damage Dealt", stats.getDoubleStat(uuid, StatsManager.BOW_DAMAGE_DEALT)),
                value("highest-streak", "&6Highest Streak", stats.getHighestStreak(uuid))
        );
    }

    private List<DisplayStat> defensive(UUID uuid) {
        StatsManager stats = plugin.getStatsManager();
        return Arrays.asList(
                value("deaths", "&cDeaths", stats.getDeaths(uuid, Period.LIFETIME)),
                decimal("damage-taken", "&9Damage Taken", stats.getDoubleStat(uuid, StatsManager.DAMAGE_TAKEN)),
                decimal("melee-damage-taken", "&9Melee Damage Taken", stats.getDoubleStat(uuid, StatsManager.MELEE_DAMAGE_TAKEN)),
                decimal("bow-damage-taken", "&9Bow Damage Taken", stats.getDoubleStat(uuid, StatsManager.BOW_DAMAGE_TAKEN))
        );
    }

    private List<DisplayStat> performance(UUID uuid) {
        StatsManager stats = plugin.getStatsManager();
        double hours = hours(uuid);
        long kills = stats.getKills(uuid, Period.LIFETIME);
        long assists = stats.getAssists(uuid);
        long deaths = stats.getDeaths(uuid, Period.LIFETIME);
        return Arrays.asList(
                value("lifetime-xp", "&bLifetime XP", stats.getLifetimeXp(uuid)),
                ratio("kd", "&cK/D", stats.safeRatio(kills, deaths)),
                ratio("kad", "&eK+A/D", stats.safeRatio(kills + assists, deaths)),
                ratio("damage-ratio", "&9Damage Dealt/Taken", stats.safeRatio(stats.getDoubleStat(uuid, StatsManager.DAMAGE_DEALT), stats.getDoubleStat(uuid, StatsManager.DAMAGE_TAKEN))),
                ratio("hours-played", "&fHours Played", hours),
                ratio("gold-per-hour", "&6Gold per Hour", hours <= 0.0D ? 0.0D : stats.getLongStat(uuid, StatsManager.GOLD_EARNED) / hours),
                ratio("ka-per-hour", "&aKills and Assists per Hour", hours <= 0.0D ? 0.0D : (kills + assists) / hours)
        );
    }

    private List<DisplayStat> miscellaneous(UUID uuid) {
        StatsManager stats = plugin.getStatsManager();
        Map<String, String> values = new LinkedHashMap<>();
        values.put(StatsManager.LEFT_CLICKS, "Left Clicks");
        values.put(StatsManager.GOLD_EARNED, "Gold Earned");
        values.put(StatsManager.SHOP_ITEMS_PURCHASED, "Shop Items Purchased");
        values.put(StatsManager.CHAT_MESSAGES, "Chat Messages");
        values.put(StatsManager.BLOCKS_PLACED, "Blocks Placed");
        values.put(StatsManager.BLOCKS_BROKEN, "Blocks Broken");
        values.put(StatsManager.JUMPS_INTO_PIT, "Jumps into Pit");
        values.put(StatsManager.LAUNCHER_LAUNCHES, "Launcher Launches");
        values.put(StatsManager.GOLDEN_APPLES_EATEN, "Golden Apples Eaten");
        values.put(StatsManager.GOLDEN_HEADS_EATEN, "Golden Heads Eaten");
        values.put(StatsManager.LAVA_BUCKETS_EMPTIED, "Lava Buckets Emptied");
        values.put(StatsManager.FISHING_RODS_LAUNCHED, "Fishing Rods Launched");
        values.put(StatsManager.CONTRACTS_COMPLETED, "Contracts Completed");
        values.put("novice_contracts_completed", "Novice Contracts Completed");
        values.put("big_time_contracts_completed", "Big Time Contracts Completed");
        values.put("contracts_failed", "Contracts Failed");
        values.put("contracts_abandoned", "Contracts Abandoned");
        values.put("contract_vile_earned", "Contract Vile Earned");
        values.put(StatsManager.WHEAT_FARMED, "Wheat Farmed");
        values.put(StatsManager.GOLD_FROM_FARMING, "Gold from Farming");
        values.put(StatsManager.GOLD_FROM_SELLING_FISH, "Gold from Selling Fish");
        values.put(StatsManager.KINGS_QUEST_COMPLETED, "King's Quest Completed");
        values.put(StatsManager.SEWER_TREASURES_FOUND, "Sewer Treasures Found");
        values.put(StatsManager.NIGHT_QUESTS_COMPLETED, "Night Quests Completed");
        values.put(StatsManager.FISHED_ANYTHING, "Fished Anything");
        values.put(StatsManager.FISHED_FISH, "Fished Fish");
        List<DisplayStat> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.entrySet()) result.add(value(entry.getKey().replace('_', '-'), "&e" + entry.getValue(), stats.getLongStat(uuid, entry.getKey())));
        return result;
    }

    private Inventory create(Session session, String title) {
        Inventory inventory = Bukkit.createInventory(session, 45, CC.color(title));
        session.inventory = inventory;
        ItemStack filler = item(Material.STAINED_GLASS_PANE, (short) 15, " ", new ArrayList<>());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        for (int inner : INNER_SLOTS) inventory.setItem(inner, null);
        return inventory;
    }

    private Session session(Player viewer, UUID target, String targetName, MenuType type) {
        return new Session(viewer.getUniqueId(), target, targetName, type);
    }

    private void set(Inventory inventory, Session session, int slot, Action action, ItemStack item) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        session.actions.put(slot, action);
        inventory.setItem(slot, item);
    }

    private ItemStack statIcon(DisplayStat stat) {
        return item(material("statistics.menus.materials." + stat.id, Material.PAPER), (short) 0,
                path("labels." + stat.id, stat.name), Arrays.asList("&7Value: &f" + stat.value));
    }

    private ItemStack icon(String id, Material fallback, String name, List<String> lore) {
        return item(material("statistics.menus.materials." + id, fallback), (short) (id.equals("summary") ? 3 : 0),
                path("labels." + id, name), lore);
    }

    private ItemStack item(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private DisplayStat value(String id, String name, long value) {
        return new DisplayStat(id, name, whole.format(value));
    }

    private DisplayStat decimal(String id, String name, double value) {
        return new DisplayStat(id, name, ratioFormat(Math.max(0.0D, value)));
    }

    private DisplayStat ratio(String id, String name, double value) {
        return new DisplayStat(id, name, ratioFormat(Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D));
    }

    private double hours(UUID uuid) {
        return plugin.getStatsManager().getLongStat(uuid, StatsManager.PLAYTIME_MILLIS) / 3600000.0D;
    }

    private String title(MenuType type) {
        return switch (type) {
            case OFFENSIVE -> path("offensive.title", "&8Offensive Statistics");
            case DEFENSIVE -> path("defensive.title", "&8Defensive Statistics");
            case PERFORMANCE -> path("performance.title", "&8Performance Statistics");
            case MISCELLANEOUS -> path("miscellaneous.title", "&8Miscellaneous Statistics");
            default -> path("main.title", "&8Statistics");
        };
    }

    private int slot(String suffix, int fallback) {
        return plugin.registry().settings().getInt("statistics.menus." + suffix, fallback);
    }

    private String path(String suffix, String fallback) {
        return plugin.registry().settings().getString("statistics.menus." + suffix, fallback);
    }

    private Material material(String path, Material fallback) {
        String configured = plugin.registry().settings().getString(path, fallback.name());
        Material material = Material.matchMaterial(configured);
        if (material == null && "EXPERIENCE_BOTTLE".equalsIgnoreCase(configured)) material = Material.EXP_BOTTLE;
        if (material == null && warnedMaterials.add(path + "=" + configured)) {
            plugin.getLogger().warning("Invalid Statistics menu material '" + configured + "' at " + path + "; using " + fallback.name() + ".");
        }
        return material == null ? fallback : material;
    }

    private String ratioFormat(double value) {
        String pattern = plugin.registry().settings().getString("statistics.formatting.ratio-pattern", "0.00");
        try {
            return new java.text.DecimalFormat(pattern, java.text.DecimalFormatSymbols.getInstance(Locale.US)).format(value);
        } catch (IllegalArgumentException exception) {
            return String.format(Locale.US, "%.2f", value);
        }
    }

    private String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> "0";
        };
    }

    private enum MenuType { MAIN, OFFENSIVE, DEFENSIVE, PERFORMANCE, MISCELLANEOUS, SUMMARY }
    private enum Action { OFFENSIVE, DEFENSIVE, PERFORMANCE, MISCELLANEOUS, SUMMARY, BACK, CLOSE }
    private record DisplayStat(String id, String name, String value) {}

    private static final class Session implements InventoryHolder {
        private final UUID viewer;
        private final UUID target;
        private final String targetName;
        private final MenuType type;
        private final Map<Integer, Action> actions = new LinkedHashMap<>();
        private Inventory inventory;

        private Session(UUID viewer, UUID target, String targetName, MenuType type) {
            this.viewer = viewer;
            this.target = target;
            this.targetName = targetName;
            this.type = type;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }
}
