package me.outerspace.thepit.registry;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public final class PitRegistry {
    private static final String[] RESOURCES = {
            "settings.yml", "arenas.yml", "abelix-ranks.yml", "messages.yml",
            "scoreboard.yml", "shop.yml", "perks.yml", "quests.yml", "npcs.yml", "database.yml"
    };

    private final ThePitPlugin plugin;
    private volatile DeploymentSettingsRegistry deployment;
    private volatile SettingsRegistry settings;
    private volatile ArenaRegistry arenas;
    private volatile MessageRegistry messages;
    private volatile ScoreboardRegistry scoreboard;
    private volatile TablistRegistry tablist;
    private volatile ShopRegistry shops;
    private volatile PerkRegistry perks;
    private volatile QuestRegistry quests;
    private volatile NpcRegistry npcs;
    private volatile AbelixRankRegistry abelixRanks;

    public PitRegistry(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        plugin.getDataFolder().mkdirs();
        for (String resource : RESOURCES) {
            if (!new File(plugin.getDataFolder(), resource).exists()) plugin.saveResource(resource, false);
        }
        migrateConfiguration();
        return reload();
    }

    private void migrateConfiguration() {
        try {
            migratePrestigeCurrency();
            mergeMissing("settings.yml");
            mergeMissing("scoreboard.yml");
            mergeMissing("perks.yml");
            mergeMissing("messages.yml");
            mergeMissing("shop.yml");
            migrateScoreboard();
            cleanSettings();
            cleanMessages();
            cleanPerks();
            cleanShop();
            cleanArenas();
            Files.deleteIfExists(new File(plugin.getDataFolder(), "kits.yml").toPath());
            Files.deleteIfExists(new File(plugin.getDataFolder(), "ranks.yml").toPath());
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not fully migrate obsolete The Pit configuration: " + exception.getMessage());
        }
    }

    private void mergeMissing(String name) throws Exception {
        File targetFile = new File(plugin.getDataFolder(), name);
        YamlConfiguration target = YamlConfiguration.loadConfiguration(targetFile);
        YamlConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource(name), StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        }
        for (String path : defaults.getKeys(true)) {
            if (!defaults.isConfigurationSection(path) && !target.contains(path)) target.set(path, defaults.get(path));
        }
        target.save(targetFile);
    }

    private void cleanSettings() throws Exception {
        File file = new File(plugin.getDataFolder(), "settings.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if ("normal".equalsIgnoreCase(yaml.getString("combat.knockback.active-profile", ""))
                && yaml.contains("combat.knockback.profiles.normal.horizontal-friction")) {
            yaml.set("combat.knockback.active-profile", "fakepixel");
        }
        for (String path : new String[]{"settings.permanent-kit-effects", "settings.vote-item-slot", "settings.vote-required",
                "settings.vote-cooldown-seconds", "settings.default-kit", "settings.rating", "prestige.gold",
                "prestige.reset.gold-progress"}) yaml.set(path, null);
        yaml.set("prestige.currency", "PIT_GOLD");
        yaml.set("prestige.requirement-mode", "CURRENT_BALANCE");
        yaml.set("prestige.consume-currency", true);
        yaml.set("fishing-club.currency", "PIT_GOLD");
        yaml.set("fishing.aqua-pants.currency", "PIT_GOLD");
        yaml.set("killstreaks.currency", "PIT_GOLD");
        yaml.set("megastreaks.currency", "PIT_GOLD");
        yaml.set("gold.currency", "PIT_GOLD");
        yaml.set("gold.command", null);
        yaml.set("gold.add-command", null);
        yaml.set("gold.placeholder", "%thepit_coins%");
        yaml.set("killstreaks.title", "Killstreaks");
        yaml.set("killstreaks.size", 54);
        yaml.set("killstreaks.choose-title", "Choose a Killstreak");
        yaml.set("killstreaks.choose-size", 54);
        yaml.set("killstreaks.slots.1.inventory-slot", 20);
        yaml.set("killstreaks.slots.2.inventory-slot", 22);
        yaml.set("killstreaks.slots.3.inventory-slot", 24);
        yaml.set("killstreaks.menu.selected-megastreak-slot", 40);
        yaml.set("killstreaks.menu.back-slot", 49);
        yaml.set("killstreaks.menu.megastreaks-slot", null);
        yaml.set("killstreaks.menu.close-slot", null);
        if ("&8Megastreaks".equals(yaml.getString("megastreaks.title"))) {
            yaml.set("megastreaks.title", "Choose a Megastreak");
        }
        yaml.save(file);
    }

    private void migratePrestigeCurrency() throws Exception {
        File file = new File(plugin.getDataFolder(), "settings.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection progression = yaml.getConfigurationSection("prestige.progression");
        if (progression != null) {
            for (String key : progression.getKeys(false)) {
                String oldPath = "prestige.progression." + key + ".gold-required";
                String newPath = "prestige.progression." + key + ".coins-required";
                if (yaml.contains(oldPath) && !yaml.contains(newPath)) yaml.set(newPath, yaml.getLong(oldPath));
                yaml.set(oldPath, null);
            }
        }
        yaml.set("prestige.gold", null);
        yaml.set("prestige.reset.gold-progress", null);
        yaml.set("prestige.currency", "PIT_GOLD");
        yaml.set("prestige.requirement-mode", "CURRENT_BALANCE");
        yaml.set("prestige.consume-currency", true);
        yaml.save(file);
    }

    private void migrateScoreboard() throws Exception {
        File file = new File(plugin.getDataFolder(), "scoreboard.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        java.util.List<String> oldLines = java.util.Arrays.asList(
                "&fLevel: %level_formatted%",
                "&fXP: &b%level_progress%",
                "",
                "&fGold: &6%thepit_abelix_gold%",
                "&fCoins: &e%coins%",
                "",
                "&emc.fakepixel.me"
        );
        java.util.List<String> previousLines = java.util.Arrays.asList(
                "%prestige_line%",
                "&fLevel: %level_formatted%",
                "&fNeeded XP: &b%needed_xp%",
                "",
                "&fGold: &6%thepit_abelix_gold%",
                "&fCoins: &e%coins%",
                "",
                "&fStatus: %status%",
                "%bounty_line%",
                "%streak_line%",
                "%effect_lines%",
                "",
                "&emc.fakepixel.me"
        );
        if (yaml.getStringList("scoreboard.lines").equals(oldLines) || yaml.getStringList("scoreboard.lines").equals(previousLines)) {
            yaml.set("scoreboard.lines", java.util.Arrays.asList(
                    "&7%date%",
                    "",
                    "%prestige_line%",
                    "&fLevel: %level_formatted%",
                    "&fNeeded XP: &b%needed_xp%",
                    "",
                    "&fGold: &6%coins%",
                    "",
                    "&fStatus: %status%",
                    "%bounty_line%",
                    "%streak_line%",
                    "%effect_lines%",
                    "",
                    "&emc.fakepixel.me"
            ));
            yaml.save(file);
        }
    }

    private void cleanMessages() throws Exception {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String path : new String[]{"messages.vote-started", "messages.left-spawn-given-kit", "messages.spawn-rank-needed",
                "messages.enderchest-rank-needed", "messages.prestige-need-ruby3"}) yaml.set(path, null);
        yaml.set("messages.chat-format", "%level_formatted% &r%abelix_prefix%%player%&7: &f%message%");
        yaml.save(file);
    }

    private void cleanPerks() throws Exception {
        File file = new File(plugin.getDataFolder(), "perks.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("menu.megastreaks", null);
        ConfigurationSection perks = yaml.getConfigurationSection("perks");
        if (perks != null) {
            for (String id : perks.getKeys(false)) {
                String base = "perks." + id;
                if (!yaml.contains(base + ".required-level")) yaml.set(base + ".required-level", 1);
                yaml.set(base + ".required-rating", null);
                yaml.set(base + ".required-rating-rank", null);
                yaml.set(base + ".required-rank", null);
            }
        }
        int[] levels = {10, 35, 70, 1};
        int[] prestiges = {0, 0, 0, 1};
        for (int slot = 1; slot <= 4; slot++) {
            String base = "loadout-slots." + slot;
            yaml.set(base + ".required-rank", null);
            yaml.set(base + ".required-rating-rank", null);
            if (!yaml.contains(base + ".required-level")) yaml.set(base + ".required-level", levels[slot - 1]);
            if (!yaml.contains(base + ".required-prestige")) yaml.set(base + ".required-prestige", prestiges[slot - 1]);
        }
        yaml.set("currency", "PIT_GOLD");
        yaml.set("passives.currency", "PIT_GOLD");
        yaml.save(file);
    }

    private void cleanShop() throws Exception {
        File file = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("shop", null);
        yaml.set("item-shop.currency", "PIT_GOLD");
        yaml.save(file);
    }

    private void cleanArenas() throws Exception {
        File file = new File(plugin.getDataFolder(), "arenas.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("npcs.kit", null);
        yaml.set("npcs.kitedit", null);
        if (yaml.contains("npcs.shop.command")) yaml.set("npcs.shop.command", "shop");
        ConfigurationSection boards = yaml.getConfigurationSection("leaderboards");
        if (boards != null) {
            for (String id : boards.getKeys(false)) {
                if ("RATING".equalsIgnoreCase(yaml.getString("leaderboards." + id + ".stat-type", ""))) {
                    yaml.set("leaderboards." + id, null);
                }
            }
        }
        yaml.save(file);
    }

    public synchronized boolean reload() {
        try {
            Map<String, Object> settingsData = YamlRegistryLoader.load(plugin, "settings.yml");
            Map<String, Object> arenasData = YamlRegistryLoader.load(plugin, "arenas.yml");
            Map<String, Object> abelixData = YamlRegistryLoader.load(plugin, "abelix-ranks.yml");
            Map<String, Object> messagesData = YamlRegistryLoader.load(plugin, "messages.yml");
            Map<String, Object> scoreboardData = YamlRegistryLoader.load(plugin, "scoreboard.yml");
            Map<String, Object> shopData = YamlRegistryLoader.load(plugin, "shop.yml");
            Map<String, Object> perksData = YamlRegistryLoader.load(plugin, "perks.yml");
            Map<String, Object> questsData = YamlRegistryLoader.load(plugin, "quests.yml");
            Map<String, Object> npcsData = YamlRegistryLoader.load(plugin, "npcs.yml");
            Map<String, Object> databaseData = YamlRegistryLoader.load(plugin, "database.yml");
            ArenaRegistry nextArenas = new ArenaRegistry(arenasData);
            SettingsRegistry nextSettings = new SettingsRegistry(settingsData);
            MessageRegistry nextMessages = new MessageRegistry(messagesData);
            ScoreboardRegistry nextScoreboard = new ScoreboardRegistry(scoreboardData);
            TablistRegistry nextTablist = new TablistRegistry();
            ShopRegistry nextShops = new ShopRegistry(shopData);
            PerkRegistry nextPerks = new PerkRegistry(perksData);
            QuestRegistry nextQuests = new QuestRegistry(questsData);
            AbelixRankRegistry nextAbelix = new AbelixRankRegistry(abelixData);
            NpcRegistry nextNpcs = new NpcRegistry(npcsData);
            DeploymentSettingsRegistry nextDeployment = new DeploymentSettingsRegistry(databaseData);
            settings=nextSettings;arenas=nextArenas;messages=nextMessages;
            scoreboard=nextScoreboard;tablist=nextTablist;shops=nextShops;perks=nextPerks;quests=nextQuests;
            abelixRanks=nextAbelix;npcs=nextNpcs;deployment=nextDeployment;
            validateTablist(nextTablist);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not reload The Pit YAML registries; previous values remain active: " + exception.getMessage());
            return false;
        }
    }

    private void validateTablist(TablistRegistry registry) {
        for (String key : new String[]{TablistRegistry.TAB_PLAYER_NAME, TablistRegistry.NAMETAG}) {
            for (String placeholder : registry.unknownPlaceholders(key)) plugin.getLogger().warning("Unknown tablist registry placeholder key=" + key + " placeholder=" + placeholder);
        }
        plugin.getLogger().info("Tablist registry loaded");
        plugin.getLogger().info("Player format: " + registry.template(TablistRegistry.TAB_PLAYER_NAME));
        File legacy = new File(plugin.getDataFolder(), "tablist.yml");
        if (legacy.exists()) plugin.getLogger().info("Legacy tablist.yml was preserved but is no longer loaded; tablist formats are registry-backed.");
    }

    public SettingsRegistry settings() { return settings; }
    public ArenaRegistry arenas() { return arenas; }
    public MessageRegistry messages() { return messages; }
    public ScoreboardRegistry scoreboard() { return scoreboard; }
    public TablistRegistry tablist() { return tablist; }
    public ShopRegistry shops() { return shops; }
    public PerkRegistry perks() { return perks; }
    public QuestRegistry quests() { return quests; }
    public NpcRegistry npcs() { return npcs; }
    public AbelixRankRegistry abelixRanks() { return abelixRanks; }
    public DeploymentSettingsRegistry deployment() { return deployment; }
}
