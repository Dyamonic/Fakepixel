package club.abelix.games.fakevival.config;

import club.abelix.games.fakevival.mmo.MMOItemType;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.config.defaults.ExpandedCollectionRecipeDefaults;
import club.abelix.games.fakevival.config.defaults.ShopFoodItemDefaults;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashMap;

public enum StaticConfigDefaults {
    SKILLS,
    JOBS,
    SKILL_TREES,
    PLAYER_LEVELS,
    SHOPS,
    BAZAAR,
    RANKS,
    QUESTS,
    NPC_QUESTS,
    COLLECTIONS,
    STORE,
    CHAT_FILTER,
    COMPANIONS,
    GUILDS,
    ZONES,
    DUNGEONS,
    LANGUAGES,
    PLAYTIME_REWARDS,
    LOOT_BOXES,
    BATTLEPASS,
    HUB;

    public Map<String, Object> data() {
        return StaticRegistryData.data(this);
    }

    public YamlConfiguration load(JavaPlugin plugin) {
        YamlConfiguration config = new YamlConfiguration();
        apply(config, data());
        applyCustomMaterialDefaults(config);
        applyCollectionMaterialDefaults(config);
        applyRecipeMaterialDefaults(config);
        applyExtraMmoItemDefaults(config);
        applyRpgFishingDefaults(config);
        applyRpgItemDefaults(config);
        applyRequestedCustomItemDefaults(config);
        applyExpandedSmpItemDefaults(config);
        applyRequestedFoodItemDefaults(config);
        applyMissingRecipeOutputDefaults(config);
        applyRequestedGearItemDefaults(config);
        applyRequestedSwordItemDefaults(config);
        applyRequestedBowItemDefaults(config);
        applyRequestedCharmItemDefaults(config);
        applyRequestedCombatGearItemDefaults(config);
        applyRequestedDungeonItemDefaults(config);
        applyRequestedToolItemDefaults(config);
        applyRequestedCompanionDefaults(config);
        ExpandedCollectionRecipeDefaults.applyItems(config);
        applyMmoSystemDefaults(config);
        applyEventShopDefaults(config);
        ShopFoodItemDefaults.apply(config);
        normalizeOrdinaryItemRarities(config);
        if (this == SHOPS) applyStreetVendorDefaults(config);
        if (this == SHOPS) club.abelix.games.fakevival.config.defaults.FoodVendorDefaults.apply(config);
        if (this == SHOPS) {
            club.abelix.games.fakevival.config.defaults.NpcShopDefaults.apply(config);
        }
        if (this == ZONES) applyZoneDungeonPerformanceDefaults(config);
        if (this == SHOPS) applyGuildMonsterShopDefaults(config);
        if (this == STORE) {
            applyStorageStoreDefaults(config);
            applyCoinPurseStoreDefaults(config);
        }
        applyZoneBossBalanceDefaults(config);
        return config;
    }

    private static void normalizeOrdinaryItemRarities(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) return;
        Set<String> ordinary = Set.of(
                "MATERIAL", "RECIPE_MATERIAL", "MINING_MATERIAL", "FORAGING_MATERIAL", "FARMING_MATERIAL",
                "COMBAT_MATERIAL", "MONSTER_MATERIAL", "FISHING_MATERIAL", "DUNGEON_MATERIAL", "MAGIC_MATERIAL",
                "COOKING_INGREDIENT", "CUSTOM_FOOD", "FOOD", "CUSTOM_FISH", "FISH", "FISHING_ITEM",
                "FISHING_BAIT", "BAIT", "FISHING_GEAR_MATERIAL", "EVENT_TOKEN", "TOKEN", "EVENT_MATERIAL",
                "NORMAL_EVENT_ITEM", "EVENT_ITEM", "EVENT_COLLECTIBLE", "VANILLA_MATERIAL");
        for (String id : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(id);
            if (item == null) continue;
            String type = item.getString("type", item.getString("mmo.type", "")).toUpperCase(Locale.ROOT);
            if (!ordinary.contains(type) || Set.of("developer_notebook", "dctr_space_helmet").contains(id.toLowerCase(Locale.ROOT))) continue;
            String rarity = item.getString("rarity", "");
            if (rarity.isBlank() || rarity.equalsIgnoreCase("SPECIAL")) item.set("rarity", defaultOrdinaryRarity(type));
            item.set("special-item", false);
            item.set("developer-notebook", false);
            item.set("random-stats-allowed", false);
            item.set("random-rarity-allowed", false);
            item.set("mmo.random-roll", false);
            item.set("reforge-allowed", false);
            item.set("enchant-allowed", false);
        }
    }

    private static String defaultOrdinaryRarity(String type) {
        return switch (type) {
            case "COMBAT_MATERIAL", "MONSTER_MATERIAL", "FISHING_MATERIAL", "DUNGEON_MATERIAL", "MAGIC_MATERIAL",
                    "FISHING_ITEM", "FISHING_GEAR_MATERIAL", "EVENT_TOKEN", "TOKEN", "EVENT_MATERIAL" -> "UNCOMMON";
            case "EVENT_ITEM", "NORMAL_EVENT_ITEM", "EVENT_COLLECTIBLE" -> "RARE";
            default -> "COMMON";
        };
    }

    private static void applyStorageStoreDefaults(YamlConfiguration config) {
        config.set("store.categories.upgrades.name", "&6Account Upgrades");
        config.set("store.categories.upgrades.icon", "GOLD_BLOCK");
        config.set("store.categories.upgrades.lore", List.of("&7Buy permanent account upgrades.", "&7Unlock additional personal storage."));
        config.set("store.upgrades.storage_pages.slot", 18);
        config.set("store.upgrades.storage_pages.material", "ENDER_CHEST");
        config.set("store.upgrades.storage_pages.name", "&eStorage Pages");
        config.set("store.upgrades.storage_pages.cost", 250);
        config.set("store.upgrades.storage_pages.type", "storage-pages");
        config.set("store.upgrades.storage_pages.lore", List.of("&7Unlock more Ender Chest storage pages."));
    }

    private static void applyCoinPurseStoreDefaults(YamlConfiguration config) {
        config.set("store.coins.small_purse", null);
        config.set("store.coins.medium_purse", null);
        config.set("store.coins.large_purse", null);
        coinPurse(config, "small_coin_purse", 11, "GOLD_NUGGET", "&eSmall Coin Purse", 253, 6000, 1);
        coinPurse(config, "medium_coin_purse", 13, "GOLD_INGOT", "&6Medium Coin Purse", 800, 30000, 3);
        coinPurse(config, "large_coin_purse", 15, "GOLD_BLOCK", "&6Large Coin Purse", 1253, 60000, 6);
    }

    private static void coinPurse(YamlConfiguration config, String id, int slot, String material, String name,
                                  int cost, int coins, int lifetimeLimit) {
        String path = "store.coins." + id;
        config.set(path + ".slot", slot);
        config.set(path + ".material", material);
        config.set(path + ".name", name);
        config.set(path + ".cost", cost);
        config.set(path + ".type", "coin-exchange");
        config.set(path + ".coins", coins);
        config.set(path + ".lifetime-limit", lifetimeLimit);
        config.set(path + ".lore", List.of("&7Adds coins directly to your purse."));
    }

    private static void applyMissingRecipeOutputDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");

        ConfigurationSection bread = ensureItem(items, "enchanted_bread");
        bread.set("name", "&aEnchanted Bread");
        bread.set("material", "BREAD");
        bread.set("type", "FOOD");
        bread.set("rarity", "UNCOMMON");
        bread.set("category", "food");
        bread.set("lore", List.of("&7A magical loaf used in recipes and survival meals."));
        bread.set("max-stack-size", 64);
        bread.set("mmo.type", "FOOD");
        bread.set("mmo.random-roll", false);
        bread.set("ability.type", "none");
        bread.set("ability.mana-cost", 0);
        bread.set("consumable.enabled", true);
        bread.set("consumable.heal", 10);
        bread.set("food-effect-id", "enchanted_bread");

        ConfigurationSection compactor = ensureItem(items, "super_compactor_3000");
        compactor.set("name", "&9Super Compactor 3000");
        compactor.set("material", "HOPPER");
        compactor.set("type", "UTILITY");
        compactor.set("rarity", "RARE");
        compactor.set("category", "utility");
        compactor.set("lore", List.of("&7A powerful utility item used for compacting materials."));
        compactor.set("max-stack-size", 64);
        compactor.set("mmo.type", "UTILITY");
        compactor.set("mmo.random-roll", false);
        compactor.set("ability.type", "none");
        compactor.set("ability.mana-cost", 0);
    }

    private static void applyStreetVendorDefaults(YamlConfiguration config) {
        vendor(config, "general", "&eGeneral Merchant", "CHEST", "Everyday food, ammunition, tools, and utility supplies.",
                "bread,m:BREAD,8,16", "torches,m:TORCH,16,24", "arrows,m:ARROW,16,32", "stone_tools,m:STONE_PICKAXE,1,75", "coal,m:COAL,8,32");
        vendor(config, "blacksmith", "&8Blacksmith", "ANVIL", "Starter weapons, armor, and basic smithing supplies.",
                "iron_sword,m:IRON_SWORD,1,350", "starter_bow,m:BOW,1,300", "chainmail_chest,m:CHAINMAIL_CHESTPLATE,1,450", "stonebreaker_pick,c:stonebreaker_pick,1,500", "iron_ingots,m:IRON_INGOT,8,160");
        vendor(config, "alchemist", "&dAlchemist", "BREWING_STAND", "Potions and practical alchemy ingredients.",
                "healing_potion,c:lesser_healing_potion,1,180", "mana_potion,c:dungeon_mana_potion,1,250", "strength_potion,c:revenant_strength_potion,1,325", "speed_potion,c:shadow_speed_potion,1,325", "redstone,m:REDSTONE,8,80");
        vendor(config, "farmer", "&aFarmer", "HAY_BLOCK", "Food, seeds, and entry-level farming supplies.",
                "bread,m:BREAD,8,16", "cookies,m:COOKIE,8,32", "apple_pie,c:apple_pie,1,90", "hearty_stew,c:mushroom_stew_plus,1,160", "wheat_seeds,m:WHEAT_SEEDS,16,32", "farmers_hoe,c:farmers_hoe,1,450");
        vendor(config, "fisherman", "&3Fisherman", "FISHING_ROD", "Rods, bait, fish, and supplies for longer voyages.",
                "fishing_rod,m:FISHING_ROD,1,175", "chum_bait,c:chum_bait,8,120", "nightcrawler_bait,c:nightcrawler_bait,4,180", "treasure_bait,c:treasure_bait,1,650", "cooked_cod,m:COOKED_COD,8,48");
        vendor(config, "miner", "&bMiner", "IRON_PICKAXE", "Mining tools, fuel, and low-tier underground provisions.",
                "iron_pickaxe,m:IRON_PICKAXE,1,400", "stonebreaker_pick,c:stonebreaker_pick,1,550", "coal,m:COAL,16,64", "torches,m:TORCH,16,24", "miners_bread,c:miners_bread,1,140");
        vendor(config, "lumberjack", "&2Lumberjack", "IRON_AXE", "Axes, saplings, logs, and foraging provisions.",
                "iron_axe,m:IRON_AXE,1,400", "timber_cleaver,c:timber_cleaver,1,550", "oak_logs,m:OAK_LOG,16,64", "oak_saplings,m:OAK_SAPLING,8,40", "lumberjack_stew,c:lumberjack_stew,1,140");
        vendor(config, "dungeon_vendor", "&5Dungeon Vendor", "ENDER_CHEST", "Low-tier dungeon consumables and utility fragments.",
                "dungeon_healing,c:dungeon_healing_potion,1,300", "dungeon_mana,c:dungeon_mana_potion,1,300", "key_fragment,c:ancient_key_fragment,1,900", "combat_ration,c:combat_ration,1,400");
        vendor(config,"general_cooking_shop","&aGeneral Cooking Shop","CAMPFIRE","Buy basic cooking ingredients and kitchen supplies.","flour,c:flour,1,8","dough,c:dough,1,18","bread_dough,c:bread_dough,1,24","sugar_powder,c:sugar_powder,1,10","salt,c:salt,1,6","pepper,c:pepper,1,8","spice_mix,c:spice_mix,1,35","hot_spice_mix,c:hot_spice_mix,1,90","butter,c:butter,1,30","cream,c:cream,1,35","cheese,c:cheese,1,55","honey_glaze,c:honey_glaze,1,75");
        vendor(config,"bakers_pantry","&eBaker's Pantry","BREAD","Buy baking ingredients for bread, cookies, pies, and cakes.","flour,c:flour,1,8","enchanted_flour,c:enchanted_flour,1,1200","dough,c:dough,1,18","enchanted_dough,c:enchanted_dough,1,1800","sweet_dough,c:sweet_dough,1,32","cookie_dough,c:cookie_dough,1,55","cake_batter,c:cake_batter,1,120","pie_crust,c:pie_crust,1,45","frosting,c:frosting,1,65","chocolate_mix,c:chocolate_mix,1,80","berry_jam,c:berry_jam,1,60","apple_slices,c:apple_slices,1,20");
        vendor(config,"fishermans_kitchen","&bFisherman's Kitchen","COD","Buy prepared seafood cooking ingredients.","fish_fillet,c:fish_fillet,1,45","salmon_fillet,c:salmon_fillet,1,65","cooked_fish_fillet,c:cooked_fish_fillet,1,80","grilled_salmon_fillet,c:grilled_salmon_fillet,1,110","seaweed_wrap,c:seaweed_wrap,1,35","pufferfish_spice,c:pufferfish_spice,1,150","salt,c:salt,1,6","pepper,c:pepper,1,8","spice_mix,c:spice_mix,1,35","hot_spice_mix,c:hot_spice_mix,1,90");
        vendor(config,"butchers_counter","&cButcher's Counter","COOKED_BEEF","Buy sliced meats and meat cooking ingredients.","raw_beef_slice,c:raw_beef_slice,1,55","raw_pork_slice,c:raw_pork_slice,1,50","raw_chicken_slice,c:raw_chicken_slice,1,45","raw_mutton_slice,c:raw_mutton_slice,1,55","raw_rabbit_slice,c:raw_rabbit_slice,1,65","cooked_meat_slice,c:cooked_meat_slice,1,90","salt,c:salt,1,6","pepper,c:pepper,1,8","spice_mix,c:spice_mix,1,35","hot_spice_mix,c:hot_spice_mix,1,90","golden_sauce,c:golden_sauce,1,250");
        vendor(config,"farm_kitchen_supplies","&aFarm Kitchen Supplies","WHEAT","Buy sliced vegetables, fruit, and farming ingredients.","carrot_slices,c:carrot_slices,1,18","potato_slices,c:potato_slices,1,18","beetroot_slices,c:beetroot_slices,1,18","pumpkin_puree,c:pumpkin_puree,1,35","melon_chunks,c:melon_chunks,1,28","melon_juice,c:melon_juice,1,60","mushroom_slice,c:mushroom_slice,1,20","cooked_mushroom_mix,c:cooked_mushroom_mix,1,55","healing_herbs,c:healing_herbs,1,75","egg_mix,c:egg_mix,1,40","golden_sauce,c:golden_sauce,1,250","mana_syrup,c:mana_syrup,1,400");
        for(String id:List.of("general_cooking_shop","bakers_pantry","fishermans_kitchen","butchers_counter","farm_kitchen_supplies")){config.set("shops."+id+".buy-enabled",true);config.set("shops."+id+".sell-enabled",false);config.set("npcs.shops."+id+".shop",id);config.set("npcs.shops."+id+".open-delay-ticks",10);config.set("npcs.shops."+id+".sound","ENTITY_VILLAGER_TRADE");}
        setVendorIdentity(config, "event_vendor", "&dEvent Vendor", "SUNFLOWER", "Limited event rewards use the existing Event Token shop.");
        setVendorIdentity(config, "furniture", "&6Furniture Vendor", "ARMOR_STAND", "Furniture, armor-stand decorations, and installed decorative models.");
        config.set("shops.furniture.items", null);
        config.set("shops.miner.items.stonebreaker_pick.requires.skill", "mining");
        config.set("shops.miner.items.stonebreaker_pick.requires.skill-level", 3);
        config.set("shops.fisherman.items.treasure_bait.requires.skill", "fishing");
        config.set("shops.fisherman.items.treasure_bait.requires.skill-level", 5);
        for (String id : List.of("general", "blacksmith", "alchemist", "farmer", "fisherman", "miner", "lumberjack", "dungeon_vendor", "event_vendor", "furniture")) {
            String npc = "npcs.shops." + id;
            config.set(npc + ".shop", id);
            config.set(npc + ".open-delay-ticks", 10);
            config.set(npc + ".sound", "ENTITY_VILLAGER_TRADE");
        }
        config.set("npcs.shops.shopkeeper.shop", "general");
        config.set("npcs.shops.forge.shop", "miner");
        config.set("npcs.shops.foraging.shop", "lumberjack");
        config.set("npcs.shops.furniture.messages", List.of("&6Furniture Dealer &8> &fLocally built vanilla models are in stock."));
    }

    private static void vendor(YamlConfiguration config, String id, String name, String icon, String description, String... entries) {
        setVendorIdentity(config, id, name, icon, description);
        config.set("shops." + id + ".items", null);
        for (String entry : entries) {
            String[] part = entry.split(",", 4); if (part.length != 4) continue;
            String path = "shops." + id + ".items." + part[0];
            if (part[1].startsWith("c:")) config.set(path + ".custom-item", part[1].substring(2));
            else config.set(path + ".material", part[1].substring(2));
            config.set(path + ".amount", parseInt(part[2], 1)); config.set(path + ".price", parseInt(part[3], 1));
            config.set(path + ".sell-price", 0); config.set(path + ".allow-sell", false);
        }
    }

    private static void setVendorIdentity(YamlConfiguration config, String id, String name, String icon, String description) {
        config.set("shops." + id + ".name", name); config.set("shops." + id + ".icon", icon);
        config.set("shops." + id + ".description", List.of("&7" + description));
    }

    private static final String REQUESTED_FOOD_ITEMS = """
            apple_pie|&aApple Pie|PUMPKIN_PIE|COMMON|40|0|0|0||Restore 40 HP instantly.
            sweet_berry_tart|&cSweet Berry Tart|SWEET_BERRIES|COMMON|30|0|0|20|SPEED=5|Restore 30 HP and gain 5 Speed for 20 seconds.
            golden_pancakes|&6Golden Pancakes|HONEYCOMB|UNCOMMON|75|0|0|30|STRENGTH=10|Restore 75 HP and gain 10 Strength for 30 seconds.
            mushroom_stew_plus|&6Hearty Mushroom Stew|MUSHROOM_STEW|UNCOMMON|100|0|0|0||Restore 100 HP instantly.
            miners_bread|&7Miner's Bread|BREAD|UNCOMMON|0|0|0|60|MINING_FORTUNE=10|Gain 10 Mining Fortune for 60 seconds.
            farmers_breakfast|&aFarmer's Breakfast|COOKED_PORKCHOP|UNCOMMON|0|0|0|60|FARMING_FORTUNE=10|Gain 10 Farming Fortune for 60 seconds.
            lumberjack_stew|&2Lumberjack Stew|RABBIT_STEW|UNCOMMON|0|0|0|60|FORAGING_FORTUNE=10|Gain 10 Foraging Fortune for 60 seconds.
            fishermans_supper|&3Fisherman's Supper|COOKED_COD|UNCOMMON|0|0|0|60|FISHING_FORTUNE=10|Gain 10 Fishing Fortune for 60 seconds.
            combat_ration|&cCombat Ration|COOKED_BEEF|RARE|0|0|0|60|STRENGTH=20;DEFENSE=20|Gain 20 Strength and 20 Defense for 60 seconds.
            mana_cookie|&bMana Cookie|COOKIE|RARE|0|100|0|0||Restore 100 Mana instantly.
            enchanted_carrot_cake|&6Enchanted Carrot Cake|CARROT|RARE|0|0|0|45|SPEED=15|Gain 15 Speed and Jump Boost for 45 seconds.
            crystal_candy|&dCrystal Candy|AMETHYST_SHARD|RARE|0|0|0|60|MANA=50;ABILITY_DAMAGE=10|Gain 50 Mana and 10% Ability Damage for 60 seconds.
            dragonfruit_slice|&cDragonfruit Slice|MELON_SLICE|EPIC|0|0|0|90|STRENGTH=35;CRIT_DAMAGE=25|Gain 35 Strength and 25% Crit Damage for 90 seconds.
            voidberry|&5Voidberry|GLOW_BERRIES|EPIC|0|0|0|90|CRIT_CHANCE=5;CRIT_DAMAGE=35|Gain 5% Crit Chance and 35% Crit Damage for 90 seconds.
            golden_fish_fillet|&6Golden Fish Fillet|COOKED_SALMON|EPIC|0|0|0|90|FISHING_SPEED=20;SEA_CREATURE_CHANCE=5|Gain 20 Fishing Speed and 5% Sea Creature Chance for 90 seconds.
            deep_miner_chili|&6Deep Miner Chili|BEETROOT_SOUP|EPIC|0|0|0|90|MINING_SPEED=75;MINING_FORTUNE=25|Gain 75 Mining Speed and 25 Mining Fortune for 90 seconds.
            harvest_feast|&aHarvest Feast|CAKE|EPIC|0|0|0|120|FARMING_FORTUNE=35;HEALTH=100|Gain 35 Farming Fortune and 100 Health for 120 seconds.
            ancient_honey_bottle|&eAncient Honey Bottle|HONEY_BOTTLE|EPIC|200|0|0|0||Restore 200 HP and clear poison.
            star_cookie|&dStar Cookie|COOKIE|LEGENDARY|0|0|1800|600|XP_BOOST=10|Gain 10% Skill XP for 10 minutes.
            feast_of_the_hero|&6Feast of the Hero|GOLDEN_CARROT|LEGENDARY|0|0|600|300|HEALTH=250;STRENGTH=50;DEFENSE=50;MANA=100|Gain 250 Health, 50 Strength, 50 Defense, and 100 Mana for 5 minutes.
            """;

    private static void applyRequestedFoodItemDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        for (String line : REQUESTED_FOOD_ITEMS.strip().split("\\R")) {
            String[] p = line.trim().split("\\|", -1);
            if (p.length < 10) continue;
            String id = normalize(p[0]);
            ConfigurationSection item = ensureItem(items, id);
            item.set("name", p[1]);
            item.set("material", p[2]);
            item.set("type", "FOOD");
            item.set("rarity", p[3]);
            item.set("category", "food");
            item.set("lore", List.of("&7Effect: &a" + p[9]));
            item.set("mmo", null);
            item.set("mmo.type", "FOOD");
            item.set("mmo.random-roll", false);
            item.set("ability.type", "none");
            item.set("ability.mana-cost", 0);
            item.set("consumable.enabled", true);
            item.set("consumable.heal", parseInt(p[4], 0));
            item.set("consumable.mana", parseInt(p[5], 0));
            item.set("consumable.cooldown-seconds", parseInt(p[6], 0));
            int duration = parseInt(p[7], 0);
            item.set("food-effect-id", id);
            item.set("food-duration-seconds", duration);
            for (String statEntry : p[8].split(";")) {
                String[] stat = statEntry.split("=", 2);
                if (stat.length != 2) continue;
                item.set("consumable.stat-buffs." + stat[0] + ".amount", parseInt(stat[1], 0));
                item.set("consumable.stat-buffs." + stat[0] + ".duration-seconds", Math.max(1, duration));
            }
            if (id.equals("enchanted_carrot_cake")) {
                item.set("consumable.effects.JUMP_BOOST.duration-seconds", 45);
                item.set("consumable.effects.JUMP_BOOST.amplifier", 0);
            }
            if (id.equals("ancient_honey_bottle")) item.set("consumable.clear-effects", List.of("POISON"));
            item.set("consumable.message", "&aYou consumed " + p[1] + "&a.");
            item.set("max-stack-size", p[2].endsWith("STEW") || p[2].endsWith("SOUP") || p[2].equals("CAKE") ? 1 : 64);
            item.set("sellall-allowed", false);
        }
    }

    private static void applyEventShopDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        applyEventShopItem(items, "event_token", "&eEvent Token", "SUNFLOWER", "TOKEN", "UNCOMMON",
                List.of("&7Earned by participating in scheduled SMP events.", "&7Currency for the Event Shop."), 64);
        applyEventShopItem(items, "event_loot_box", "&aEvent Loot Box", "ENDER_CHEST", "LOOT_BOX", "RARE",
                List.of("&7Contains event materials, bait, and utility rewards."), 64);
        applyEventShopItem(items, "event_cosmetic_token", "&dEvent Cosmetic Token", "AMETHYST_SHARD", "TOKEN", "RARE",
                List.of("&7A token reserved for event cosmetics and future unlocks."), 64);
        applyEventShopItem(items, "developer_notebook", "&cDeveloper's Notebook", "BOOK", "SPECIAL", "SPECIAL",
                List.of(
                        "&7Every page rewrites itself",
                        "&7whenever a new bug is discovered.",
                        "&7Some pages are best left unread.",
                        "",
                        "&c&lSPECIAL"
                ), 1);
        ConfigurationSection developerNotebook = items.getConfigurationSection("developer_notebook");
        if (developerNotebook != null) {
            developerNotebook.set("category", "special");
            developerNotebook.set("tradeable", false);
            developerNotebook.set("auctionable", false);
            developerNotebook.set("bazaarable", false);
            developerNotebook.set("sellable", false);
            developerNotebook.set("reforge-allowed", false);
            developerNotebook.set("enchant-allowed", false);
            developerNotebook.set("sellall-allowed", false);
            developerNotebook.set("guild-sellable", false);
            developerNotebook.set("unbreakable", true);
            developerNotebook.set("mmo.random-roll", false);
            developerNotebook.set("random-stats-allowed", false);
            developerNotebook.set("random-rarity-allowed", false);
            developerNotebook.set("ability.type", "none");
            developerNotebook.set("ability.mana-cost", 0);
        }

        config.set("loot-boxes.boxes.event.name", "&aEvent Loot Box");
        config.set("loot-boxes.boxes.event.material", "ENDER_CHEST");
        config.set("loot-boxes.boxes.event.custom-item", "event_loot_box");
        config.set("loot-boxes.boxes.event.lore", List.of("&7Contains event materials, bait,", "&7and useful crafting rewards."));
        setEventBoxReward(config, "recipe_fragments", "&eRecipe Fragments", "uncommon", 30.0D, "custom-item:recipe_fragment_alpha");
        setEventBoxReward(config, "monster_material", "&cMonster Material", "uncommon", 24.0D, "custom-item:monster_heart");
        setEventBoxReward(config, "mining_material", "&bMining Material", "rare", 18.0D, "custom-item:mithril_dust");
        setEventBoxReward(config, "fishing_material", "&3Fishing Material", "rare", 15.0D, "custom-item:deepwater_pearl");
        setEventBoxReward(config, "treasure_bait", "&6Treasure Bait", "epic", 10.0D, "custom-item:treasure_bait");
        setEventBoxReward(config, "key_fragment", "&6Ancient Key Fragment", "epic", 3.0D, "custom-item:ancient_key_fragment");
    }

    private static void applyEventShopItem(ConfigurationSection items, String id, String name, String material,
                                           String type, String rarity, List<String> lore, int stackSize) {
        ConfigurationSection item = ensureItem(items, id);
        item.set("name", name);
        item.set("material", material);
        item.set("category", "materials");
        item.set("type", type);
        item.set("rarity", rarity);
        item.set("lore", lore);
        item.set("mmo.type", type);
        item.set("mmo.random-roll", false);
        item.set("ability.type", "none");
        item.set("ability.mana-cost", 0);
        item.set("consumable.enabled", false);
        item.set("max-stack-size", stackSize);
        item.set("sell-price", 0);
        item.set("sellall-allowed", false);
        item.set("guild-sellable", false);
    }

    private static void setEventBoxReward(YamlConfiguration config, String id, String display, String rarity,
                                          double chance, String reward) {
        String path = "loot-boxes.boxes.event.rewards." + id;
        config.set(path + ".display", display);
        config.set(path + ".rarity", rarity);
        config.set(path + ".chance", chance);
        config.set(path + ".reward", reward);
    }

    private static void applyGuildMonsterShopDefaults(YamlConfiguration config) {
        String entries = """
                imperial_cloth|18
                imperial_medal|65
                imperial_bone|15
                imperial_steel_fragment|70
                imperial_arrowhead|20
                imperial_scout_badge|75
                imperial_officer_seal|250
                ancient_dungeon_essence|750
                cracked_monster_core|60
                dungeon_hide|18
                dungeon_fang|25
                dungeon_claw|35
                cursed_fabric|180
                undead_ash|12
                rotten_monster_flesh|10
                enchanted_monster_bone|65
                shadow_dust|200
                royal_scrap|250
                broken_guard_hilt|70
                monster_soul_shard|650
                goblin_ear|10
                orc_tooth|45
                shadow_fang|180
                cursed_eye|750
                ancient_bone|200
                blood_essence|220
                monster_heart|800
                void_claw|900
                zombie_fang|10
                hardened_bone_shard|12
                spider_silk_bundle|12
                venom_gland|45
                creeper_powder_core|55
                ender_residue|65
                blaze_cinder|200
                slime_core|15
                magma_scale|220
                phantom_wing_fragment|250
                """;
        for (String line : entries.strip().split("\\R")) {
            String[] parts = line.split("\\|", 2);
            String path = "shops.adventure_guild.items." + parts[0];
            double sell = Double.parseDouble(parts[1]);
            config.set(path + ".custom-item", parts[0]);
            config.set(path + ".amount", 1);
            config.set(path + ".sell-price", sell);
            config.set(path + ".price", sell * 4.0D);
        }
    }

    private static void applyZoneDungeonPerformanceDefaults(YamlConfiguration config) {
        config.set("zones.spawn-scan-interval-ticks", 20L);
        config.set("zones.dungeon-max-mobs", 80);
        config.set("zones.dungeon-soft-cap", 80);
        config.set("zones.dungeon-spawn-batch-size", 5);
        config.set("zones.dungeon-mob-respawn-min-seconds", 20L);
        config.set("zones.dungeon-mob-respawn-max-seconds", 45L);
        config.set("zones.dungeon-mob-health-base", 400.0D);
        config.set("zones.dungeon-mob-health-per-level", 120.0D);
        config.set("zones.dungeon-mob-health-multiplier", 1.0D);
        config.set("zones.dungeon-mob-damage-base", 15.0D);
        config.set("zones.dungeon-mob-damage-per-level", 3.0D);
        config.set("zones.dungeon-mob-defense-base", 10.0D);
        config.set("zones.dungeon-mob-defense-per-level", 2.0D);
        config.set("zones.dungeon-mob-leash-radius", 32.0D);
        config.set("zones.default-boss-health", 10000.0D);
        config.set("zones.boss-health-per-level", 350.0D);
        config.set("zones.boss-health-scale", 1.0D);
        config.set("zones.elite-boss-health-scale", 1.5D);
        config.set("zones.final-boss-health-scale", 2.25D);
        config.set("zones.default-boss-defense", 25.0D);
        config.set("zones.boss-defense-per-level", 3.0D);
        config.set("zones.elite-boss-defense-multiplier", 1.15D);
        config.set("zones.final-boss-defense-multiplier", 1.35D);
        config.set("zones.boss-damage-base", 12.0D);
        config.set("zones.boss-damage-per-level", 1.5D);
        config.set("zones.boss-regen-damage-pause-seconds", 8.0D);
        config.set("zones.boss-regen-percent", 0.15D);
        config.set("zones.elite-boss-regen-percent", 0.25D);
        config.set("zones.final-boss-regen-percent", 0.35D);
        config.set("zones.max-safe-bukkit-boss-health", 1024.0D);
        config.set("zones.mob-name-format", "&8[&7Lv.%level%&8] &c%name% &a%health%&7/&a%max_health%&c❤");
        config.set("zones.debug", false);
    }

    private static final String CUSTOM_MATERIAL_ITEMS = """
            royal_crest|&6Royal Crest|GOLD_NUGGET|RARE|&7A royal emblem carried by~&7guardians of the lost kingdom.~&7Used to craft royal equipment.
            shadow_cloth|&5Shadow Cloth|BLACK_DYE|RARE|&7A piece of fabric soaked~&7in dark sanctuary magic.~&7Used to craft assassin equipment.
            crystal_fragment|&bCrystal Fragment|AMETHYST_SHARD|UNCOMMON|&7A sharp mining crystal fragment.~&7Used to craft crystal equipment.
            frozen_bone|&bFrozen Bone|BONE|UNCOMMON|&7A bone preserved in ancient frost.~&7Used to craft frozen equipment.
            toxic_vial|&aToxic Vial|HONEY_BOTTLE|RARE|&7A sealed vial of dangerous toxin.~&7Used to craft plague equipment.
            ancient_dungeon_essence|&5Ancient Dungeon Essence|ECHO_SHARD|EPIC|&7A condensed fragment of ancient dungeon power.~&7Used for high-level player level requirements~&7and dungeon progression.
            royal_crest_fragment|&6Royal Crest Fragment|GOLD_NUGGET|RARE|&7A fragment used for player level ascension.
            prestige_core|&dPrestige Core|NETHER_STAR|EPIC|&7A condensed core used for prestige player levels.
            ascended_crystal|&bAscended Crystal|END_CRYSTAL|LEGENDARY|&7A stable crystal used for ascended player levels.
            mythic_essence|&5Mythic Essence|ECHO_SHARD|MYTHIC|&7Rare essence used for high prestige levels.
            rainbow_core|&cRainbow Core|HEART_OF_THE_SEA|SPECIAL|&7The ultimate material for rainbow player levels.
            """;

    private static final String COLLECTION_MATERIAL_ITEMS = """
            rough_coal_chunk|&8Rough Coal Chunk|COAL|COMMON|mining|10|mining|COAL_ORE,DEEPSLATE_COAL_ORE|8.0
            polished_coal_core|&8Polished Coal Core|COAL_BLOCK|UNCOMMON|mining|45|mining|COAL_ORE,DEEPSLATE_COAL_ORE|2.0
            iron_nugget_cluster|&fIron Nugget Cluster|IRON_NUGGET|COMMON|mining|12|mining|IRON_ORE,DEEPSLATE_IRON_ORE|8.0
            dense_iron_fragment|&fDense Iron Fragment|RAW_IRON|UNCOMMON|mining|55|mining|IRON_ORE,DEEPSLATE_IRON_ORE|2.0
            refined_iron_plate|&fRefined Iron Plate|IRON_INGOT|RARE|mining|220|mining|IRON_ORE,DEEPSLATE_IRON_ORE|0.5
            copper_wire_bundle|&6Copper Wire Bundle|COPPER_INGOT|COMMON|mining|10|mining|COPPER_ORE,DEEPSLATE_COPPER_ORE|8.0
            charged_copper_coil|&6Charged Copper Coil|COPPER_BLOCK|UNCOMMON|mining|50|mining|COPPER_ORE,DEEPSLATE_COPPER_ORE|2.0
            gold_dust|&6Gold Dust|GOLD_NUGGET|COMMON|mining|15|mining|GOLD_ORE,DEEPSLATE_GOLD_ORE,NETHER_GOLD_ORE|7.0
            royal_gold_plate|&6Royal Gold Plate|GOLD_INGOT|RARE|mining|300|mining|GOLD_ORE,DEEPSLATE_GOLD_ORE,NETHER_GOLD_ORE|0.45
            lapis_powder|&9Lapis Powder|LAPIS_LAZULI|COMMON|mining|10|mining|LAPIS_ORE,DEEPSLATE_LAPIS_ORE|8.0
            enchanted_lapis_shard|&9Enchanted Lapis Shard|LAPIS_BLOCK|UNCOMMON|mining|60|mining|LAPIS_ORE,DEEPSLATE_LAPIS_ORE|1.8
            redstone_spark|&cRedstone Spark|REDSTONE|COMMON|mining|8|mining|REDSTONE_ORE,DEEPSLATE_REDSTONE_ORE|8.0
            unstable_redstone_core|&cUnstable Redstone Core|REDSTONE_BLOCK|RARE|mining|250|mining|REDSTONE_ORE,DEEPSLATE_REDSTONE_ORE|0.5
            emerald_chip|&aEmerald Chip|EMERALD|UNCOMMON|mining|75|mining|EMERALD_ORE,DEEPSLATE_EMERALD_ORE|2.0
            flawless_emerald|&aFlawless Emerald|EMERALD_BLOCK|EPIC|mining|900|mining|EMERALD_ORE,DEEPSLATE_EMERALD_ORE|0.12
            oak_bark_strip|&6Oak Bark Strip|OAK_LOG|COMMON|foraging|8|foraging|OAK_LOG,OAK_WOOD|8.0
            enchanted_oak_resin|&6Enchanted Oak Resin|OAK_SAPLING|UNCOMMON|foraging|40|foraging|OAK_LOG,OAK_WOOD|2.0
            birch_bark_strip|&fBirch Bark Strip|BIRCH_LOG|COMMON|foraging|8|foraging|BIRCH_LOG,BIRCH_WOOD|8.0
            pale_birch_sap|&fPale Birch Sap|BIRCH_SAPLING|UNCOMMON|foraging|40|foraging|BIRCH_LOG,BIRCH_WOOD|2.0
            spruce_pinecone|&2Spruce Pinecone|SPRUCE_SAPLING|COMMON|foraging|10|foraging|SPRUCE_LOG,SPRUCE_WOOD|8.0
            hardened_spruce_resin|&2Hardened Spruce Resin|SPRUCE_LOG|RARE|foraging|180|foraging|SPRUCE_LOG,SPRUCE_WOOD|0.55
            jungle_vine_fiber|&aJungle Vine Fiber|VINE|COMMON|foraging|10|foraging|JUNGLE_LOG,JUNGLE_WOOD|8.0
            jungle_heartwood|&aJungle Heartwood|JUNGLE_LOG|RARE|foraging|200|foraging|JUNGLE_LOG,JUNGLE_WOOD|0.55
            acacia_thorn|&6Acacia Thorn|ACACIA_SAPLING|COMMON|foraging|10|foraging|ACACIA_LOG,ACACIA_WOOD|8.0
            sunbaked_acacia_bark|&6Sunbaked Acacia Bark|ACACIA_LOG|UNCOMMON|foraging|50|foraging|ACACIA_LOG,ACACIA_WOOD|2.0
            dark_oak_root|&8Dark Oak Root|DARK_OAK_SAPLING|COMMON|foraging|12|foraging|DARK_OAK_LOG,DARK_OAK_WOOD|7.0
            ancient_dark_oak_core|&8Ancient Dark Oak Core|DARK_OAK_LOG|EPIC|foraging|800|foraging|DARK_OAK_LOG,DARK_OAK_WOOD|0.12
            mangrove_root_fiber|&cMangrove Root Fiber|MANGROVE_ROOTS|COMMON|foraging|12|foraging|MANGROVE_LOG,MANGROVE_WOOD|7.0
            mangrove_sap_cluster|&cMangrove Sap Cluster|MANGROVE_PROPAGULE|UNCOMMON|foraging|60|foraging|MANGROVE_LOG,MANGROVE_WOOD|1.8
            cherry_blossom_petal|&dCherry Blossom Petal|PINK_PETALS|COMMON|foraging|15|foraging|CHERRY_LOG,CHERRY_WOOD|7.0
            golden_wheat_bundle|&eGolden Wheat Bundle|WHEAT|UNCOMMON|farming|35|farming|WHEAT|3.0
            dense_hay_fiber|&eDense Hay Fiber|HAY_BLOCK|RARE|farming|160|farming|WHEAT|0.6
            carrot_seed_core|&6Carrot Seed Core|CARROT|COMMON|farming|8|farming|CARROTS|8.0
            sweet_carrot_bundle|&6Sweet Carrot Bundle|CARROT|UNCOMMON|farming|35|farming|CARROTS|2.5
            potato_sprout|&ePotato Sprout|POTATO|COMMON|farming|8|farming|POTATOES|8.0
            sturdy_potato_sack|&eSturdy Potato Sack|POTATO|UNCOMMON|farming|35|farming|POTATOES|2.5
            beetroot_extract|&cBeetroot Extract|BEETROOT|COMMON|farming|10|farming|BEETROOTS|8.0
            rich_beetroot_paste|&cRich Beetroot Paste|BEETROOT_SOUP|RARE|farming|180|farming|BEETROOTS|0.55
            pumpkin_pulp|&6Pumpkin Pulp|PUMPKIN|COMMON|farming|10|farming|PUMPKIN|7.0
            glowing_pumpkin_seed|&6Glowing Pumpkin Seed|PUMPKIN_SEEDS|RARE|farming|200|farming|PUMPKIN|0.5
            melon_rind|&aMelon Rind|MELON_SLICE|COMMON|farming|8|farming|MELON|8.0
            juicy_melon_core|&aJuicy Melon Core|MELON|UNCOMMON|farming|40|farming|MELON|2.2
            sugar_cane_fiber|&fSugar Cane Fiber|SUGAR_CANE|COMMON|farming|8|farming|SUGAR_CANE|8.0
            compressed_sugar_crystal|&fCompressed Sugar Crystal|SUGAR|RARE|farming|175|farming|SUGAR_CANE|0.55
            nether_wart_spore|&4Nether Wart Spore|NETHER_WART|UNCOMMON|farming|55|farming|NETHER_WART|2.0
            zombie_fang|&2Zombie Fang|ROTTEN_FLESH|COMMON|combat|10|combat|ZOMBIE,HUSK,DROWNED,ZOMBIE_VILLAGER|8.0
            hardened_bone_shard|&fHardened Bone Shard|BONE|COMMON|combat|12|combat|SKELETON,STRAY,WITHER_SKELETON|8.0
            spider_silk_bundle|&fSpider Silk Bundle|STRING|COMMON|combat|10|combat|SPIDER,CAVE_SPIDER|8.0
            venom_gland|&2Venom Gland|SPIDER_EYE|UNCOMMON|combat|55|combat|SPIDER,CAVE_SPIDER|2.0
            creeper_powder_core|&aCreeper Powder Core|GUNPOWDER|UNCOMMON|combat|60|combat|CREEPER|2.0
            ender_residue|&5Ender Residue|ENDER_PEARL|UNCOMMON|combat|75|combat|ENDERMAN,ENDERMITE|1.8
            blaze_cinder|&6Blaze Cinder|BLAZE_POWDER|RARE|combat|220|combat|BLAZE|0.55
            slime_core|&aSlime Core|SLIME_BALL|COMMON|combat|12|combat|SLIME|8.0
            magma_scale|&cMagma Scale|MAGMA_CREAM|RARE|combat|240|combat|MAGMA_CUBE|0.5
            phantom_wing_fragment|&9Phantom Wing Fragment|PHANTOM_MEMBRANE|RARE|combat|275|combat|PHANTOM|0.45
            river_fish_scale|&bRiver Fish Scale|COD|COMMON|fishing|10|fishing|COD|20.0
            salmon_fin|&cSalmon Fin|SALMON|COMMON|fishing|12|fishing|SALMON|14.0
            puffer_spike|&ePuffer Spike|PUFFERFISH|UNCOMMON|fishing|50|fishing|PUFFERFISH|6.0
            tropical_fish_gem|&dTropical Fish Gem|TROPICAL_FISH|RARE|fishing|225|fishing|TROPICAL_FISH|2.0
            ancient_pearl_scale|&3Ancient Pearl Scale|PRISMARINE_CRYSTALS|EPIC|fishing|850|fishing|PRISMARINE_CRYSTALS|0.35
            """;

    private static final String RECIPE_MATERIAL_ITEMS = """
            compressed_coal_plate|&8Compressed Coal Plate|COAL_BLOCK|UNCOMMON|mining|45|Fuel-based weapon and tool recipes.
            heated_iron_core|&fHeated Iron Core|IRON_INGOT|UNCOMMON|mining|50|Early weapon and armor recipes.
            reinforced_iron_frame|&fReinforced Iron Frame|IRON_BLOCK|RARE|mining|220|Reinforced armor and defensive gear recipes.
            copper_gear|&6Copper Gear|COPPER_INGOT|COMMON|mining|12|Machinery and tool recipes.
            charged_copper_engine|&6Charged Copper Engine|LIGHTNING_ROD|RARE|mining|260|Electric and storm tool recipes.
            golden_mechanism|&6Golden Mechanism|GOLD_INGOT|UNCOMMON|mining|65|Accessory and precision tool recipes.
            royal_gold_core|&6Royal Gold Core|GOLD_BLOCK|EPIC|mining|950|Royal gear recipes.
            lapis_focus_lens|&9Lapis Focus Lens|LAPIS_LAZULI|UNCOMMON|mining|55|Magic weapon recipes.
            redstone_circuit|&cRedstone Circuit|REDSTONE|COMMON|mining|10|Utility and powered tool recipes.
            unstable_power_cell|&cUnstable Power Cell|REDSTONE_BLOCK|RARE|mining|240|High-tier utility recipes.
            emerald_binding|&aEmerald Binding|EMERALD|RARE|mining|280|Accessory and trading gear recipes.
            diamond_cutting_edge|&bDiamond Cutting Edge|DIAMOND|RARE|mining|320|Sword and advanced tool recipes.
            flawless_diamond_core|&bFlawless Diamond Core|DIAMOND_BLOCK|EPIC|mining|1200|High-tier weapon and armor recipes.
            amethyst_focus_crystal|&dAmethyst Focus Crystal|AMETHYST_SHARD|RARE|mining|300|Magic, fishing, and utility recipes.
            netherite_reinforcement|&8Netherite Reinforcement|NETHERITE_SCRAP|EPIC|mining|1400|Late-game gear recipes.
            oak_handle|&6Oak Handle|STICK|COMMON|foraging|8|Sword and tool handle recipes.
            reinforced_oak_handle|&6Reinforced Oak Handle|OAK_PLANKS|UNCOMMON|foraging|35|Stronger weapon and tool handles.
            birch_grip|&fBirch Grip|BIRCH_PLANKS|COMMON|foraging|8|Bow and tool recipes.
            spruce_shaft|&2Spruce Shaft|SPRUCE_PLANKS|COMMON|foraging|10|Spear and bow recipes.
            jungle_wrapping|&aJungle Wrapping|VINE|UNCOMMON|foraging|40|Weapon grip recipes.
            acacia_bow_limb|&6Acacia Bow Limb|ACACIA_PLANKS|UNCOMMON|foraging|45|Bow recipes.
            dark_oak_guard|&8Dark Oak Guard|DARK_OAK_PLANKS|RARE|foraging|180|Sword and greatsword recipes.
            mangrove_binding|&cMangrove Binding|MANGROVE_ROOTS|UNCOMMON|foraging|50|Armor and tool binding recipes.
            cherry_wood_inlay|&dCherry Wood Inlay|CHERRY_PLANKS|RARE|foraging|210|Decorative gear and rod recipes.
            bamboo_reinforcement|&aBamboo Reinforcement|BAMBOO|COMMON|foraging|12|Light weapon and tool recipes.
            woven_wheat_cloth|&eWoven Wheat Cloth|WHEAT|COMMON|farming|10|Armor and utility recipes.
            dense_hay_padding|&eDense Hay Padding|HAY_BLOCK|UNCOMMON|farming|40|Armor padding recipes.
            carrot_oil|&6Carrot Oil|CARROT|UNCOMMON|farming|45|Gear infusion recipes.
            potato_starch_glue|&ePotato Starch Glue|POTATO|COMMON|farming|8|Binding recipes.
            beetroot_dye_extract|&cBeetroot Dye Extract|BEETROOT|COMMON|farming|10|Decorative and cosmetic recipes.
            pumpkin_shell_plate|&6Pumpkin Shell Plate|PUMPKIN|UNCOMMON|farming|50|Armor recipes.
            melon_gel|&aMelon Gel|MELON_SLICE|UNCOMMON|farming|45|Restorative gear recipes.
            sugar_crystal_dust|&fSugar Crystal Dust|SUGAR|COMMON|farming|8|Speed and light gear recipes.
            cactus_spike_bundle|&2Cactus Spike Bundle|CACTUS|UNCOMMON|farming|55|Thorn and bleed weapon recipes.
            nether_wart_compound|&4Nether Wart Compound|NETHER_WART|RARE|farming|230|Magic and curse recipes.
            bone_hilt|&fBone Hilt|BONE|COMMON|combat|12|Weapon recipes.
            hardened_bone_frame|&fHardened Bone Frame|BONE_BLOCK|UNCOMMON|combat|55|Armor and weapon recipes.
            spider_silk_thread|&fSpider Silk Thread|STRING|COMMON|combat|10|Bow and armor recipes.
            venom_coating|&2Venom Coating|SPIDER_EYE|RARE|combat|220|Poison weapon recipes.
            creeper_blast_core|&aCreeper Blast Core|GUNPOWDER|RARE|combat|250|Explosive weapon recipes.
            ender_binding_powder|&5Ender Binding Powder|ENDER_PEARL|RARE|combat|300|Teleport and void recipes.
            blaze_heat_core|&6Blaze Heat Core|BLAZE_ROD|RARE|combat|280|Fire weapon recipes.
            slime_flex_plate|&aSlime Flex Plate|SLIME_BALL|UNCOMMON|combat|55|Mobility gear and utility recipes.
            magma_guard_plate|&cMagma Guard Plate|MAGMA_CREAM|RARE|combat|300|Fire armor recipes.
            phantom_silk_membrane|&9Phantom Silk Membrane|PHANTOM_MEMBRANE|EPIC|combat|900|Mobility gear recipes.
            fishbone_needle|&fFishbone Needle|COD|COMMON|fishing|10|Fishing gear recipes.
            salmon_oil_vial|&cSalmon Oil Vial|SALMON|UNCOMMON|fishing|45|Rod and fishing armor recipes.
            puffer_spine_hook|&ePuffer Spine Hook|PUFFERFISH|UNCOMMON|fishing|60|Fishing rod hook recipes.
            tropical_scale_inlay|&dTropical Scale Inlay|TROPICAL_FISH|RARE|fishing|220|Decorative fishing gear recipes.
            prismarine_rod_core|&3Prismarine Rod Core|PRISMARINE_SHARD|RARE|fishing|260|Fishing rod recipes.
            crystal_hook|&bCrystal Hook|PRISMARINE_CRYSTALS|EPIC|fishing|850|High-tier fishing rod recipes.
            coral_binding|&dCoral Binding|TUBE_CORAL|UNCOMMON|fishing|55|Fishing armor and rod recipes.
            kelp_woven_line|&2Kelp Woven Line|KELP|COMMON|fishing|8|Fishing line recipes.
            dungeon_steel_plate|&8Dungeon Steel Plate|IRON_INGOT|RARE|dungeon|300|Dungeon gear recipes.
            cursed_handle|&5Cursed Handle|STICK|EPIC|dungeon|900|Cursed weapon recipes.
            ancient_gear_fragment|&6Ancient Gear Fragment|CLOCK|RARE|dungeon|350|Utility and dungeon recipes.
            royal_sword_guard|&6Royal Sword Guard|GOLD_INGOT|EPIC|dungeon|1000|Royal sword recipes.
            shadow_blade_core|&8Shadow Blade Core|ECHO_SHARD|EPIC|dungeon|1100|Shadow weapon recipes.
            frozen_weapon_core|&bFrozen Weapon Core|BLUE_ICE|EPIC|dungeon|950|Frost weapon recipes.
            mythic_crafting_core|&dMythic Crafting Core|NETHER_STAR|MYTHIC|dungeon|5000|Late-game recipe catalyst.
            """;

    private static final String RPG_ITEMS = """
            mithril_dust|&bMithril Dust|PRISMARINE_CRYSTALS|MATERIAL|RARE|materials|Fine metallic dust recovered from deep crystal seams.|||180
            mithril_plate|&bMithril Plate|LIGHT_BLUE_DYE|RECIPE_MATERIAL|EPIC|materials|A light plate used in advanced equipment recipes.|||800
            titanium_shard|&fTitanium Shard|QUARTZ|MATERIAL|RARE|materials|A pale shard with exceptional structural strength.|||200
            titanium_core|&fTitanium Core|IRON_BLOCK|RECIPE_MATERIAL|EPIC|materials|A dense core prepared for elite crafting.|||900
            ruby_fragment|&cRuby Fragment|RED_DYE|MATERIAL|RARE|materials|A warm gemstone fragment used by master crafters.|||180
            sapphire_fragment|&9Sapphire Fragment|BLUE_DYE|MATERIAL|RARE|materials|A cool gemstone fragment with a clear blue sheen.|||180
            topaz_fragment|&eTopaz Fragment|YELLOW_DYE|MATERIAL|RARE|materials|A bright gemstone fragment carrying stored light.|||180
            opal_fragment|&fOpal Fragment|WHITE_DYE|MATERIAL|RARE|materials|A pale gemstone fragment that shifts in the light.|||180
            ancient_ore_chunk|&8Ancient Ore Chunk|RAW_IRON|MATERIAL|UNCOMMON|materials|An unrefined piece of ore from an older age.|||45
            molten_ingot|&6Molten Ingot|COPPER_INGOT|RECIPE_MATERIAL|RARE|materials|A heat-treated ingot ready for specialized forging.|||220
            living_root|&2Living Root|HANGING_ROOTS|MATERIAL|UNCOMMON|materials|A root that still carries the pulse of the forest.|||40
            elderwood_plank|&6Elderwood Plank|OAK_PLANKS|RECIPE_MATERIAL|RARE|materials|Seasoned elderwood prepared for durable equipment.|||190
            spirit_bark|&aSpirit Bark|MANGROVE_LOG|MATERIAL|EPIC|materials|Ancient bark infused with quiet forest energy.|||750
            enchanted_leaf_bundle|&aEnchanted Leaf Bundle|OAK_LEAVES|MATERIAL|UNCOMMON|materials|A bundle of leaves preserved by natural magic.|||35
            thorned_vine|&2Thorned Vine|VINE|RECIPE_MATERIAL|UNCOMMON|materials|A tough vine used to bind dangerous equipment.|||45
            rich_soil_clump|&6Rich Soil Clump|DIRT|MATERIAL|COMMON|materials|Dark fertile soil gathered from a productive field.|||8
            fertile_essence|&aFertile Essence|GREEN_DYE|RECIPE_MATERIAL|RARE|materials|Concentrated growth energy for farming equipment.|||180
            sunflower_oil|&eSunflower Oil|SUNFLOWER|RECIPE_MATERIAL|UNCOMMON|materials|Pressed oil used in light utility crafting.|||40
            golden_seed|&eGolden Seed|WHEAT_SEEDS|MATERIAL|RARE|materials|A rare seed selected from an exceptional harvest.|||160
            harvest_crystal|&aHarvest Crystal|EMERALD|RECIPE_MATERIAL|EPIC|materials|A crystal that resonates with cultivated land.|||700
            goblin_ear|&2Goblin Ear|RABBIT_HIDE|MATERIAL|COMMON|materials|A rough trophy carried by monster hunters.|||10
            orc_tooth|&7Orc Tooth|BONE|MATERIAL|UNCOMMON|materials|A heavy tooth recovered from a powerful brute.|||45
            shadow_fang|&8Shadow Fang|FLINT|MATERIAL|RARE|materials|A dark fang that absorbs nearby light.|||190
            cursed_eye|&5Cursed Eye|ENDER_EYE|RECIPE_MATERIAL|EPIC|materials|An unsettling eye used in forbidden crafting.|||850
            ancient_bone|&fAncient Bone|BONE|MATERIAL|RARE|materials|A fossilized bone hardened by centuries.|||170
            blood_essence|&4Blood Essence|REDSTONE|RECIPE_MATERIAL|RARE|materials|A sealed essence used in crimson equipment recipes.|||220
            monster_heart|&cMonster Heart|FERMENTED_SPIDER_EYE|RECIPE_MATERIAL|EPIC|materials|A preserved heart pulsing with monster energy.|||900
            void_claw|&5Void Claw|ECHO_SHARD|MATERIAL|EPIC|materials|A claw marked by unstable void resonance.|||950
            angler_scale|&3Angler Scale|PRISMARINE_SHARD|MATERIAL|UNCOMMON|fishing|A sturdy scale collected by experienced anglers.|||45
            deepwater_pearl|&bDeepwater Pearl|ENDER_PEARL|RECIPE_MATERIAL|RARE|fishing|A polished pearl used in deepwater equipment.|||250
            sunken_coin|&6Sunken Coin|GOLD_NUGGET|MATERIAL|UNCOMMON|fishing|An old coin recovered from beneath the waves.|||55
            sea_serpent_scale|&3Sea Serpent Scale|CYAN_DYE|MATERIAL|EPIC|fishing|A flexible scale from a legendary sea creature.|||850
            kraken_ink|&8Kraken Ink|INK_SAC|RECIPE_MATERIAL|EPIC|fishing|Dense ink used to mark high-tier sea equipment.|||900
            storm_bait|&bStorm Bait|COD|FISHING_BAIT|RARE|fishing|Charged bait favored during violent weather.|||120
            copper_dagger|&6Copper Dagger|IRON_SWORD|DAGGER|UNCOMMON|daggers|A light dagger balanced for rapid close combat.|DAMAGE=20-32;STRENGTH=4-8;CRIT_CHANCE=2-5||180
            elderwood_sword|&6Elderwood Sword|WOODEN_SWORD|SWORD|RARE|swords|A living wooden blade reinforced with elderwood.|DAMAGE=34-50;STRENGTH=8-14;CRIT_DAMAGE=8-16||420
            ruby_blade|&cRuby Blade|DIAMOND_SWORD|SWORD|EPIC|swords|A jeweled blade that holds a steady crimson edge.|DAMAGE=68-92;STRENGTH=20-34;CRIT_CHANCE=5-9;CRIT_DAMAGE=18-30||1200
            shadow_katana|&8Shadow Katana|NETHERITE_SWORD|KATANA|EPIC|katanas|A dark katana forged around a shadow blade core.|DAMAGE=80-115;STRENGTH=28-48;CRIT_CHANCE=8-14;CRIT_DAMAGE=25-45||1450
            titanium_greatsword|&fTitanium Greatsword|IRON_SWORD|GREATSWORD|EPIC|swords|A massive titanium blade built for deliberate strikes.|DAMAGE=100-138;STRENGTH=38-60;CRIT_DAMAGE=28-45||1500
            molten_axe|&6Molten Axe|NETHERITE_AXE|AXE_WEAPON|RARE|swords|A heavy axe forged from heat-treated metal.|DAMAGE=72-102;STRENGTH=26-42;CRIT_DAMAGE=15-28||850
            void_spear|&5Void Spear|TRIDENT|SPEAR|EPIC|swords|A long spear shaped around a fragment of the void.|DAMAGE=86-122;STRENGTH=25-44;CRIT_CHANCE=5-10||1400
            storm_wand|&bStorm Wand|BLAZE_ROD|WAND|RARE|staffs_wands|A focused wand that channels controlled storm energy.|DAMAGE=55-78;MANA=65-100;CRIT_DAMAGE=14-25||800
            mithril_pickaxe|&bMithril Pickaxe|DIAMOND_PICKAXE|PICKAXE|EPIC|tools|A precise pickaxe built from refined mithril.|MINING_SPEED=170-240;MINING_FORTUNE=35-60;BREAKING_POWER=5-6||1300
            titanium_drill|&fTitanium Drill|NETHERITE_PICKAXE|DRILL|EPIC|tools|A reinforced drill designed for the hardest deposits.|MINING_SPEED=220-310;MINING_FORTUNE=45-75;BREAKING_POWER=6-7||1500
            elderwood_axe|&6Elderwood Axe|DIAMOND_AXE|FORAGING_AXE|RARE|tools|A forestry axe made from seasoned elderwood.|DAMAGE=48-68;FORAGING_FORTUNE=30-55;SPEED=3-6||750
            harvest_hoe|&aHarvest Hoe|DIAMOND_HOE|FARMING_HOE|RARE|farming_equipment|A balanced hoe prepared for long harvest sessions.|FARMING_FORTUNE=40-70;REPLANT_CHANCE=6-14;FARMING_XP_BOOST=3-6||700
            deepwater_rod|&3Deepwater Rod|FISHING_ROD|FISHING_ROD|RARE|fishing|A reinforced rod intended for dangerous deep water.|FISHING_SPEED=28-48;FISHING_FORTUNE=30-55;SEA_CREATURE_CHANCE=3-7;TREASURE_CHANCE=2-5||850
            goblin_helmet|&2Goblin Helmet|LEATHER_HELMET|HELMET|UNCOMMON|armor|Crude goblin armor reinforced for survival.|HEALTH=18-30;DEFENSE=8-16;SPEED=1-3||180
            goblin_chestplate|&2Goblin Chestplate|LEATHER_CHESTPLATE|CHESTPLATE|UNCOMMON|armor|Crude goblin armor reinforced for survival.|HEALTH=32-50;DEFENSE=15-28||260
            goblin_leggings|&2Goblin Leggings|LEATHER_LEGGINGS|LEGGINGS|UNCOMMON|armor|Crude goblin armor reinforced for survival.|HEALTH=26-42;DEFENSE=12-23||230
            goblin_boots|&2Goblin Boots|LEATHER_BOOTS|BOOTS|UNCOMMON|armor|Crude goblin armor reinforced for survival.|HEALTH=16-28;DEFENSE=7-14;SPEED=2-4||170
            titanium_helmet|&fTitanium Helmet|IRON_HELMET|HELMET|RARE|armor|A dense helmet formed from a titanium core.|HEALTH=45-70;DEFENSE=30-50;TRUE_DEFENSE=2-5||700
            titanium_chestplate|&fTitanium Chestplate|IRON_CHESTPLATE|CHESTPLATE|RARE|armor|A dense chestplate formed from a titanium core.|HEALTH=80-120;DEFENSE=55-85;TRUE_DEFENSE=3-7||950
            minor_power_ring|&aMinor Power Ring|GOLD_NUGGET|RING|UNCOMMON|rings|A simple ring carrying a small passive combat focus.|STRENGTH=5;CRIT_DAMAGE=8||120
            sea_charm|&3Sea Charm|PRISMARINE_CRYSTALS|CHARM|RARE|fishing|A passive charm carrying the rhythm of the sea.|FISHING_FORTUNE=12;SEA_CREATURE_CHANCE=2||300
            hunter_necklace|&cHunter Necklace|RABBIT_FOOT|NECKLACE|RARE|necklaces|A passive necklace worn by experienced monster hunters.|STRENGTH=6;CRIT_DAMAGE=10;MAGIC_FIND=2||320
            dungeon_entry_token|&5Dungeon Entry Token|ECHO_SHARD|TOKEN|RARE|materials|A marked token recognized at dungeon entrances.|||200
            dungeon_key_imperial|&6Imperial Dungeon Key|TRIPWIRE_HOOK|KEY|RARE|materials|A PDC-backed key for the Imperial Dungeon.|||500
            dungeon_key_frozen|&bFrozen Catacombs Key|TRIPWIRE_HOOK|KEY|RARE|materials|A PDC-backed key for the Frozen Catacombs.|||500
            dungeon_key_shadow|&5Shadow Crypt Key|TRIPWIRE_HOOK|KEY|EPIC|materials|A PDC-backed key for the Shadow Crypt.|||750
            dungeon_key_toxic|&aToxic Depths Key|TRIPWIRE_HOOK|KEY|RARE|materials|A PDC-backed key for the Toxic Depths.|||600
            dungeon_key_crystal|&bCrystal Mines Key|TRIPWIRE_HOOK|KEY|RARE|materials|A PDC-backed key for the Crystal Mines.|||600
            dungeon_key_ancient|&6Ancient Ruins Key|TRIPWIRE_HOOK|KEY|EPIC|materials|A PDC-backed key for the Ancient Ruins.|||800
            boss_summon_token|&cBoss Summon Token|NETHER_STAR|TOKEN|EPIC|materials|A dangerous token prepared for boss encounters.|||1000
            recipe_fragment_alpha|&eRecipe Fragment Alpha|PAPER|RECIPE_FRAGMENT|UNCOMMON|materials|The first fragment of an incomplete crafting record.|||50
            ancient_key_fragment|&6Ancient Key Fragment|TRIPWIRE_HOOK|KEY_FRAGMENT|RARE|materials|A broken section of an ancient mechanism.|||220
            event_token|&eEvent Token|SUNFLOWER|TOKEN|UNCOMMON|materials|A participation token earned from rotating SMP events.|||50
            """;

    private static final String FISHING_MATERIAL_ITEMS = """
            raw_river_fish|Raw River Fish|COD|COMMON|5
            silver_fish_scale|Silver Fish Scale|PRISMARINE_SHARD|UNCOMMON|20
            golden_fish_scale|Golden Fish Scale|GOLD_NUGGET|RARE|75
            deep_sea_fin|Deep Sea Fin|PRISMARINE_CRYSTALS|RARE|150
            ancient_fish_bone|Ancient Fish Bone|BONE|EPIC|350
            glowing_roe|Glowing Roe|GLOW_INK_SAC|UNCOMMON|60
            pearl_fragment|Pearl Fragment|ENDER_PEARL|RARE|250
            coral_fragment|Coral Fragment|TUBE_CORAL|UNCOMMON|45
            seaweed_bundle|Seaweed Bundle|KELP|COMMON|8
            enchanted_kelp|Enchanted Kelp|DRIED_KELP_BLOCK|UNCOMMON|50
            treasure_shell|Treasure Shell|NAUTILUS_SHELL|EPIC|500
            royal_fish_scale|Royal Fish Scale|GOLD_NUGGET|EPIC|600
            voidwater_scale|Voidwater Scale|ECHO_SHARD|EPIC|750
            stormscale_fin|Stormscale Fin|LIGHTNING_ROD|LEGENDARY|1000
            crystal_fish_shard|Crystal Fish Shard|AMETHYST_SHARD|RARE|400
            """;

    private static final String FISHING_BAIT_ITEMS = """
            simple_bait|Simple Bait|STRING|COMMON|A basic bait that slightly shortens fishing wait time.
            worm_bait|Worm Bait|FERMENTED_SPIDER_EYE|UNCOMMON|A lively bait that improves normal fish catches.
            spicy_bait|Fishing Bait|BLAZE_POWDER|UNCOMMON|A hot bait that improves the chance of catching custom fish.
            deep_sea_bait|Deep Sea Bait|PRISMARINE_SHARD|RARE|A dense bait that improves rare fish catches.
            treasure_bait|Treasure Bait|GOLD_NUGGET|RARE|A glittering bait that attracts treasure.
            monster_bait|Treasure Bait|ROTTEN_FLESH|EPIC|A rare bait that strongly improves treasure chance.
            lucky_bait|Lucky Bait|RABBIT_FOOT|RARE|A lucky bait with a small Magic Find bonus.
            crystal_bait|Crystal Bait|AMETHYST_SHARD|EPIC|Unlocks improved crystal fishing drops in crystal zones.
            royal_bait|Royal Bait|GOLD_INGOT|EPIC|Improves the chance of rare royal materials.
            void_bait|Void Bait|ECHO_SHARD|LEGENDARY|Unlocks void fishing loot while fishing in a void zone.
            """;

    private static final String FISHING_GEAR_ITEMS = """
            beginner_rod|Beginner Rod|FISHING_ROD|FISHING_ROD|COMMON|0|FISHING_SPEED=5;FISHING_FORTUNE=5|A simple rod for learning the waters.
            river_rod|River Rod|FISHING_ROD|FISHING_ROD|UNCOMMON|3|FISHING_SPEED=10;FISHING_FORTUNE=12;FISHING_XP_BOOST=3|A balanced rod made for rivers and lakes.
            reinforced_rod|Reinforced Rod|FISHING_ROD|FISHING_ROD|UNCOMMON|6|FISHING_SPEED=15;FISHING_FORTUNE=18;SEA_CREATURE_CHANCE=2|A reinforced rod that can handle stronger catches.
            deep_sea_rod|Deep Sea Rod|FISHING_ROD|FISHING_ROD|RARE|10|FISHING_SPEED=22;FISHING_FORTUNE=25;SEA_CREATURE_CHANCE=4;TREASURE_CHANCE=3|A rod built for dangerous deep water.
            captains_trident|Captain's Trident|FISHING_ROD|FISHING_ROD|EPIC|12|FISHING_SPEED=26;FISHING_FORTUNE=30;SEA_CREATURE_CHANCE=6;TREASURE_CHANCE=4|A captain's trident refitted as a powerful fishing rod.
            crystal_rod|Crystal Rod|FISHING_ROD|FISHING_ROD|EPIC|15|FISHING_SPEED=30;FISHING_FORTUNE=38;TREASURE_CHANCE=6;MAGIC_FIND=3|A crystal-lined rod that resonates with rare shards.
            royal_fishing_rod|Royal Fishing Rod|FISHING_ROD|FISHING_ROD|LEGENDARY|20|FISHING_SPEED=36;FISHING_FORTUNE=48;SEA_CREATURE_CHANCE=7;TREASURE_CHANCE=8;FISHING_XP_BOOST=8|A royal rod made for expert anglers.
            voidwater_rod|Voidwater Rod|FISHING_ROD|FISHING_ROD|LEGENDARY|25|FISHING_SPEED=40;FISHING_FORTUNE=55;SEA_CREATURE_CHANCE=10;TREASURE_CHANCE=9;MAGIC_FIND=5|A rod able to draw life from voidwater.
            stormcaller_rod|Stormcaller Rod|FISHING_ROD|FISHING_ROD|LEGENDARY|30|FISHING_SPEED=46;FISHING_FORTUNE=65;SEA_CREATURE_CHANCE=12;TREASURE_CHANCE=10;FISHING_XP_BOOST=10|A charged rod that calls creatures from stormy seas.
            mythic_angler_rod|Mythic Angler Rod|FISHING_ROD|FISHING_ROD|MYTHIC|35|FISHING_SPEED=55;FISHING_FORTUNE=80;SEA_CREATURE_CHANCE=15;TREASURE_CHANCE=13;FISHING_XP_BOOST=15;MAGIC_FIND=8|The finest rod known to the anglers' guild.
            fishing_armor_helmet|Fishing Armor Helmet|TURTLE_HELMET|ARMOR|RARE|10|HEALTH=35;DEFENSE=18;FISHING_FORTUNE=8;SEA_CREATURE_CHANCE=1|A practical helmet for long fishing trips.
            fishing_armor_chestplate|Fishing Armor Chestplate|LEATHER_CHESTPLATE|ARMOR|RARE|10|HEALTH=60;DEFENSE=30;FISHING_FORTUNE=12;TREASURE_CHANCE=1|A water-resistant angler chestplate.
            fishing_armor_leggings|Fishing Armor Leggings|LEATHER_LEGGINGS|ARMOR|RARE|10|HEALTH=50;DEFENSE=25;FISHING_FORTUNE=10;SEA_CREATURE_CHANCE=1|Flexible leggings made for anglers.
            fishing_armor_boots|Fishing Armor Boots|LEATHER_BOOTS|ARMOR|RARE|10|HEALTH=30;DEFENSE=15;FISHING_FORTUNE=7;TREASURE_CHANCE=1|Weighted boots for slippery shores.
            sea_charm|Sea Charm|PRISMARINE_CRYSTALS|ACCESSORY|UNCOMMON|5|FISHING_FORTUNE=8;SEA_CREATURE_CHANCE=1|A passive charm carrying the rhythm of the sea.
            angler_necklace|Angler Necklace|NAUTILUS_SHELL|NECKLACE|RARE|12|FISHING_FORTUNE=15;FISHING_XP_BOOST=5;HEALTH=20|A necklace worn by experienced anglers.
            treasure_ring|Treasure Ring|GOLD_NUGGET|RING|EPIC|18|TREASURE_CHANCE=5;MAGIC_FIND=4;FISHING_FORTUNE=12|A passive ring that guides its owner toward treasure.
            pearl_trinket|Pearl Trinket|ENDER_PEARL|TRINKET|EPIC|22|TREASURE_CHANCE=4;MAGIC_FIND=6;SEA_CREATURE_CHANCE=2|A polished pearl that improves fishing luck.
            """;

    private static final String EXTRA_MMO_ITEMS = """
            royal_spear|Royal Spear|TRIDENT|WEAPON|LEGENDARY|dash_attack|Royal Thrust|75|A royal spear that has been passed down by generations until it finally found its owner.|DAMAGE=105-135;STRENGTH=35-55;CRIT_CHANCE=5-9;CRIT_DAMAGE=20-35;MANA=50-90
            sakura_katana|Sakura Katana|DIAMOND_SWORD|KATANA|RARE|dash_attack|Petal Dash|35|A curved blade that cuts forward with a petal dash.|DAMAGE=48-65;STRENGTH=18-28;CRIT_CHANCE=4-8;CRIT_DAMAGE=15-25;MANA=10-25
            shadow_katana|Shadow Katana|NETHERITE_SWORD|KATANA|EPIC|shadow_step|Shadow Cut|60|A dark katana used by assassins of the forgotten sanctum.|DAMAGE=80-115;STRENGTH=28-48;CRIT_CHANCE=8-14;CRIT_DAMAGE=25-45
            bloodfang_dagger|Bloodfang Dagger|IRON_SWORD|DAGGER|EPIC|vampire_bite|Bleeding Strike|40|A dagger carved from the fang of a crimson beast.|DAMAGE=55-80;STRENGTH=20-38;CRIT_CHANCE=12-20;CRIT_DAMAGE=20-40;LIFESTEAL=1-3
            ember_blade|Ember Blade|GOLDEN_SWORD|SWORD|RARE|flame_burst|Ember Slash|35|A heated blade forged from Ember Cavern fragments.|DAMAGE=45-70;STRENGTH=20-35;CRIT_DAMAGE=10-25
            frost_bow|Frost Bow|BOW|BOW|RARE|frost_nova|Frost Shot|45|A bow that chills the air around its arrows.|DAMAGE=35-55;STRENGTH=15-30;CRIT_CHANCE=5-10;CRIT_DAMAGE=20-35
            harvester_bow|Harvester Bow|BOW|BOW|EPIC|soul_reaper|Soul Arrow|70|A haunted bow that hungers for lost souls.|DAMAGE=55-85;STRENGTH=25-45;CRIT_CHANCE=8-14;CRIT_DAMAGE=30-50;MAGIC_FIND=2-5
            storm_staff|Storm Staff|BLAZE_ROD|STAFF|LEGENDARY|thunder_strike|Lightning Call|100|A staff that calls storm energy from the peaks.|DAMAGE=70-100;STRENGTH=15-30;MANA=120-180;CRIT_DAMAGE=15-30
            plague_wand|Plague Wand|STICK|WAND|EPIC|venom_cloud|Plague Cloud|85|A wand carrying unstable plague magic.|DAMAGE=45-70;MANA=80-130;MAGIC_FIND=1-4
            celestial_blade|Celestial Blade|NETHERITE_SWORD|SWORD|MYTHIC|holy_smite|Celestial Slash|120|A blade blessed by the light of the Celestial Keep.|DAMAGE=130-180;STRENGTH=50-80;CRIT_CHANCE=8-15;CRIT_DAMAGE=35-65;MANA=80-140
            miner_helmet|Miner Helmet|IRON_HELMET|ARMOR|UNCOMMON|||0|A sturdy helmet worn by deep mine workers.|HEALTH=25-45;DEFENSE=15-30;MINING_SPEED=20-40
            miner_chestplate|Miner Chestplate|IRON_CHESTPLATE|ARMOR|UNCOMMON|||0|A reinforced chestplate for long mining trips.|HEALTH=40-70;DEFENSE=25-45;MINING_FORTUNE=5-12
            miner_leggings|Miner Leggings|IRON_LEGGINGS|ARMOR|UNCOMMON|||0|Leggings built to survive falling stone.|HEALTH=35-60;DEFENSE=20-40;MINING_SPEED=15-35
            miner_boots|Miner Boots|IRON_BOOTS|ARMOR|UNCOMMON|||0|Heavy boots with a surprising grip on stone.|HEALTH=20-40;DEFENSE=15-30;SPEED=2-5
            obsidian_shield|Obsidian Shield|SHIELD|SHIELD|EPIC|||0|A shield formed from compressed obsidian plates.|HEALTH=120-180;DEFENSE=80-140;TRUE_DEFENSE=5-12
            deepstone_pickaxe|Deepstone Pickaxe|DIAMOND_PICKAXE|TOOL|RARE|||0|A pickaxe strong enough to crack deepstone.|MINING_SPEED=120-180;MINING_FORTUNE=15-35;BREAKING_POWER=4-5
            crystal_hammer|Crystal Hammer|IRON_PICKAXE|TOOL|EPIC|earthquake_slam|Crystal Slam|65|A mining hammer powered by crystal energy.|DAMAGE=40-65;MINING_SPEED=100-160;MINING_FORTUNE=25-50;BREAKING_POWER=5-6
            treant_axe|Treant Axe|DIAMOND_AXE|TOOL|RARE|||0|An axe grown from ancient living roots.|DAMAGE=55-80;STRENGTH=20-35;FORAGING_FORTUNE=20-45
            farmers_hoe|Farmer's Hoe|IRON_HOE|FARMING_EQUIPMENT|COMMON|||0|A simple hoe made for early farming.|FARMING_FORTUNE=5-12;REPLANT_CHANCE=1-3
            harvesting_hoe|Harvesting Hoe|DIAMOND_HOE|FARMING_EQUIPMENT|RARE|||0|A farming hoe made for large harvests.|FARMING_FORTUNE=25-55;REPLANT_CHANCE=5-15
            royal_harvest_hoe|Royal Harvest Hoe|NETHERITE_HOE|FARMING_EQUIPMENT|LEGENDARY|farmburst|Royal Harvest|80|A royal hoe that turns harvests into riches.|FARMING_FORTUNE=80-140;REPLANT_CHANCE=15-30;FARMING_XP_BOOST=5-10
            wheat_reaper|Wheat Reaper|GOLDEN_HOE|FARMING_EQUIPMENT|RARE|||0|A light hoe made for cutting wheat quickly.|FARMING_FORTUNE=35-70;DOUBLE_DROP_CHANCE=5-12
            carrot_digger|Carrot Digger|IRON_SHOVEL|FARMING_EQUIPMENT|UNCOMMON|||0|A tool shaped for pulling carrots cleanly.|FARMING_FORTUNE=20-45;REPLANT_CHANCE=4-10
            potato_spade|Potato Spade|DIAMOND_SHOVEL|FARMING_EQUIPMENT|RARE|||0|A sturdy spade for potato fields.|FARMING_FORTUNE=30-65;DOUBLE_DROP_CHANCE=4-10
            beetroot_sickle|Beetroot Sickle|SHEARS|FARMING_EQUIPMENT|UNCOMMON|||0|A curved sickle used for beetroot harvests.|FARMING_FORTUNE=18-40;FARMING_XP_BOOST=2-5
            pumpkin_cleaver|Pumpkin Cleaver|IRON_AXE|FARMING_EQUIPMENT|RARE|||0|A heavy axe for splitting pumpkins.|FARMING_FORTUNE=35-75;HARVEST_RANGE=1-2
            melon_splitter|Melon Splitter|DIAMOND_AXE|FARMING_EQUIPMENT|RARE|||0|An axe designed to crack melons cleanly.|FARMING_FORTUNE=40-80;DOUBLE_DROP_CHANCE=5-12
            sugar_cane_cutter|Sugar Cane Cutter|SHEARS|FARMING_EQUIPMENT|RARE|||0|A sharp cutter for sugar cane farms.|FARMING_FORTUNE=45-90;SPEED=2-5
            cactus_knife|Cactus Knife|STONE_SWORD|FARMING_EQUIPMENT|UNCOMMON|||0|A rough blade used to harvest cactus safely.|FARMING_FORTUNE=20-45;DEFENSE=5-15
            cocoa_clipper|Cocoa Clipper|SHEARS|FARMING_EQUIPMENT|RARE|||0|Small clippers for cocoa bean farms.|FARMING_FORTUNE=35-70;FARMING_XP_BOOST=2-6
            nether_wart_trowel|Nether Wart Trowel|NETHERITE_SHOVEL|FARMING_EQUIPMENT|EPIC|||0|A dark farming tool used for nether wart.|FARMING_FORTUNE=60-110;REPLANT_CHANCE=8-18;MAGIC_FIND=1-3
            mushroom_harvester|Mushroom Harvester|BOWL|FARMING_EQUIPMENT|UNCOMMON|||0|A strange tool used by mushroom farmers.|FARMING_FORTUNE=18-38;DOUBLE_DROP_CHANCE=2-6
            bamboo_cutter|Bamboo Cutter|BAMBOO|FARMING_EQUIPMENT|UNCOMMON|||0|A flexible cutter for bamboo fields.|FARMING_FORTUNE=20-45;SPEED=3-7
            berry_pruner|Berry Pruner|SHEARS|FARMING_EQUIPMENT|UNCOMMON|||0|A careful tool for trimming berry bushes.|FARMING_FORTUNE=18-42;HEALTH=10-25
            pitcher_crop_spade|Pitcher Crop Spade|BRUSH|FARMING_EQUIPMENT|RARE|||0|A delicate tool for rare pitcher crops.|FARMING_FORTUNE=35-70;FARMING_XP_BOOST=3-7
            torchflower_sickle|Torchflower Sickle|GOLDEN_HOE|FARMING_EQUIPMENT|RARE|||0|A warm sickle used around torchflowers.|FARMING_FORTUNE=35-75;CROP_GROWTH=2-5
            sprinkler|Basic Sprinkler|WATER_BUCKET|FARMING_EQUIPMENT|UNCOMMON|farmburst|Sprinkler|0|A small sprinkler that helps nearby crops grow.|CROP_GROWTH=1-2
            advanced_sprinkler|Advanced Sprinkler|DISPENSER|FARMING_EQUIPMENT|RARE|farmburst|Advanced Sprinkler|0|A stronger sprinkler for larger farms.|CROP_GROWTH=2-4;HARVEST_RANGE=1
            enchanted_sprinkler|Enchanted Sprinkler|CONDUIT|FARMING_EQUIPMENT|EPIC|farmburst|Enchanted Sprinkler|0|A magical sprinkler that waters crops with enchanted mist.|CROP_GROWTH=3-6;HARVEST_RANGE=2;FARMING_XP_BOOST=3-7
            farmers_gloves|Farmer's Gloves|LEATHER|FARMING_EQUIPMENT|UNCOMMON|||0|Gloves made for quick replanting.|FARMING_FORTUNE=12-30;REPLANT_CHANCE=3-8
            harvester_belt|Harvester Belt|LEATHER_HORSE_ARMOR|FARMING_EQUIPMENT|RARE|||0|A belt that carries tools for long harvests.|FARMING_FORTUNE=25-55;FARMING_XP_BOOST=2-5
            compost_charm|Compost Charm|COMPOSTER|TRINKET|RARE|||0|A charm that improves soil quality around crops.|CROP_GROWTH=2-5;DOUBLE_DROP_CHANCE=2-6
            golden_seed_pouch|Golden Seed Pouch|WHEAT_SEEDS|TRINKET|EPIC|||0|A pouch filled with seeds that never seem to run out.|REPLANT_CHANCE=10-25;FARMING_FORTUNE=40-85;FARMING_XP_BOOST=4-8
            royal_ring|Royal Ring|GOLD_NUGGET|RING|EPIC|||0|A ring once worn by a royal guard.|HEALTH=30-60;MANA=30-70;STRENGTH=5-12
            void_ring|Void Ring|ENDER_PEARL|RING|LEGENDARY|||0|A ring pulsing with unstable void energy.|MANA=80-140;CRIT_CHANCE=3-7;CRIT_DAMAGE=10-25;MAGIC_FIND=2-6
            guardian_necklace|Guardian Necklace|AMETHYST_SHARD|NECKLACE|RARE|||0|A necklace used by protectors of the lost kingdom.|HEALTH=50-100;DEFENSE=25-60;VITALITY=2-6
            celestial_necklace|Celestial Necklace|NETHER_STAR|NECKLACE|MYTHIC|||0|A necklace infused with celestial light.|HEALTH=120-220;MANA=120-220;DEFENSE=50-100;MAGIC_FIND=4-10
            lucky_trinket|Lucky Trinket|RABBIT_FOOT|TRINKET|RARE|||0|A small charm that seems to bend luck.|MAGIC_FIND=5-12;PET_LUCK=3-8;XP_BOOST=2-5
            rift_trinket|Rift Trinket|ECHO_SHARD|TRINKET|EPIC|||0|A trinket recovered from a cracked rift.|MANA=70-130;SPEED=3-7;CRIT_DAMAGE=8-18
            deep_sea_rod|Deep Sea Rod|FISHING_ROD|FISHING_ROD|RARE|||0|A fishing rod built for dangerous waters.|FISHING_SPEED=20-45;SEA_CREATURE_CHANCE=3-8;TREASURE_CHANCE=2-6
            captains_trident|Captain's Trident|TRIDENT|FISHING_ROD|EPIC|gravity_pull|Tidal Pull|70|A trident once carried by a drowned captain.|DAMAGE=70-100;FISHING_SPEED=25-55;SEA_CREATURE_CHANCE=5-12;TREASURE_CHANCE=4-9
            ember_wolf_companion|Ember Wolf Companion|WOLF_SPAWN_EGG|COMPANION|EPIC|||0|A fiery companion spirit from the Ember Caverns.|STRENGTH=10-25;CRIT_DAMAGE=10-30;MAGIC_FIND=1-4
            royal_feast|Royal Feast|COOKED_BEEF|FOOD|RARE|||0|A meal prepared for royal warriors before battle.|HEALTH_RESTORE=150-250;MANA_RESTORE=30-60
            mana_tonic|Mana Tonic|POTION|POTION|UNCOMMON|||0|A small tonic that restores magical focus.|MANA_RESTORE=80-150;XP_BOOST=1-3
            """;

    @SuppressWarnings("unchecked")
    private static void apply(ConfigurationSection section, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                apply(section.createSection(entry.getKey()), (Map<String, Object>) nested);
            } else if (value instanceof List<?> list) {
                section.set(entry.getKey(), list);
            } else {
                section.set(entry.getKey(), value);
            }
        }
    }

    private static void applyCustomMaterialDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        for (String line : CUSTOM_MATERIAL_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 5) continue;
            ConfigurationSection item = items.getConfigurationSection(parts[0]);
            if (item == null) item = items.createSection(parts[0]);
            item.set("name", parts[1]);
            item.set("material", parts[2]);
            item.set("category", "materials");
            item.set("type", "MATERIAL");
            item.set("rarity", parts[3]);
            item.set("lore", splitLore(parts[4]));
            item.set("mmo.type", "MATERIAL");
            if (parts[0].equals("ancient_dungeon_essence")) {
                item.set("category", "dungeon_materials");
                item.set("type", "DUNGEON_MATERIAL");
                item.set("mmo.type", "DUNGEON_MATERIAL");
                item.set("mmo.random-roll", false);
                item.set("ability.type", "none");
                item.set("ability.mana-cost", 0);
                item.set("consumable.enabled", false);
                item.set("max-stack-size", 64);
                item.set("sell-price", 750.0D);
                item.set("guild-sellable", true);
                item.set("tradeable", true);
            }
        }
    }

    private static void applyCollectionMaterialDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        for (String line : COLLECTION_MATERIAL_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 9) continue;
            String id = normalize(parts[0]);
            ConfigurationSection item = ensureItem(items, id);
            item.set("name", parts[1]);
            item.set("material", parts[2]);
            item.set("category", "materials");
            item.set("type", "COLLECTION_MATERIAL");
            item.set("rarity", parts[3]);
            item.set("lore", List.of("&7A stackable " + prettyFishingName(parts[4]) + " collection material.", "&8Used for collection crafting and trade."));
            item.set("mmo.type", "MATERIAL");
            item.set("mmo.random-roll", false);
            item.set("ability.type", "none");
            item.set("ability.mana-cost", 0);
            item.set("unbreakable", false);
            item.set("max-stack-size", 64);
            item.set("sell-price", parseInt(parts[5], 0));
            item.set("collection.category", parts[4]);
            item.set("collection.drop.activity", parts[6]);
            item.set("collection.drop.sources", java.util.Arrays.stream(parts[7].split(",")).map(String::trim).filter(value -> !value.isBlank()).toList());
            item.set("collection.drop.chance", Double.parseDouble(parts[8]));

            String collectionPath = "collections.items." + id;
            config.set(collectionPath + ".name", parts[1] + " Collection");
            config.set(collectionPath + ".category", parts[4]);
            config.set(collectionPath + ".icon", parts[2]);
            config.set(collectionPath + ".materials", List.of());
            config.set(collectionPath + ".unlocks.tier_1.amount", 25);
            config.set(collectionPath + ".unlocks.tier_1.reward-type", "perk");
            config.set(collectionPath + ".unlocks.tier_1.reward-value", id + "_mastery");
            config.set(collectionPath + ".unlocks.tier_1.name", "&aCollection Material Mastery");
        }
    }

    private static void applyRecipeMaterialDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        for (String line : RECIPE_MATERIAL_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 7) continue;
            String id = normalize(parts[0]);
            ConfigurationSection item = ensureItem(items, id);
            item.set("name", parts[1]);
            item.set("material", parts[2]);
            item.set("category", "materials");
            item.set("type", "RECIPE_MATERIAL");
            item.set("rarity", parts[3]);
            item.set("lore", wrapLore(parts[6]));
            item.set("mmo.type", "MATERIAL");
            item.set("mmo.random-roll", false);
            item.set("ability.type", "none");
            item.set("ability.mana-cost", 0);
            item.set("consumable.enabled", false);
            item.set("unbreakable", false);
            item.set("max-stack-size", 64);
            item.set("sell-price", parseInt(parts[5], 0));
            item.set("recipe-material.category", parts[4]);
        }
    }

    private static void applyRpgItemDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        for (String line : RPG_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 10) continue;
            String id = normalize(parts[0]);
            MMOItemType type = MMOItemType.parse(parts[3]);
            ConfigurationSection item = ensureItem(items, id);
            item.set("name", parts[1]);
            item.set("material", parts[2]);
            item.set("type", parts[3]);
            item.set("rarity", parts[4]);
            item.set("category", parts[5]);
            item.set("lore", wrapLore(parts[6]));
            item.set("ability", null);
            item.set("ability.type", "none");
            item.set("ability.mana-cost", 0);
            item.set("consumable.enabled", false);
            item.set("mmo", null);
            item.set("mmo.type", parts[3]);
            item.set("mmo.random-roll", type.canRollRandomRarityEffects() && !parts[7].isBlank());
            if (!parts[7].isBlank()) applyStatRanges(item, parts[7]);
            boolean gear = type.canRollRandomRarityEffects();
            item.set("unbreakable", gear);
            item.set("max-stack-size", gear ? null : 64);
            item.set("sell-price", parseInt(parts[9], 0));
        }
    }

    private static final String REQUESTED_CUSTOM_ITEMS = """
            star_fragment|&dStar Fragment|AMETHYST_SHARD|RECIPE_MATERIAL|EPIC|materials|A fragment of condensed starlight used in event and cosmetic recipes.|||0
            moonlit_cloth|&fMoonlit Cloth|WHITE_WOOL|RECIPE_MATERIAL|RARE|materials|Soft cloth used in armor and cosmetic recipes.|||0
            sun_core|&eSun Core|YELLOW_DYE|RECIPE_MATERIAL|EPIC|materials|A radiant core used in fire and light recipes.|||0
            frost_crystal|&bFrost Crystal|BLUE_ICE|MATERIAL|RARE|materials|A stable crystal recovered while mining frozen caverns.|||120
            glacial_shard|&bGlacial Shard|ICE|RECIPE_MATERIAL|UNCOMMON|materials|A cold shard prepared for frost recipes.|||0
            ember_shard|&6Ember Shard|BLAZE_POWDER|MATERIAL|UNCOMMON|materials|A hot combat material recovered from fiery enemies.|||45
            infernal_core|&cInfernal Core|MAGMA_CREAM|RECIPE_MATERIAL|EPIC|materials|A volatile core used in fire weapon recipes.|||0
            storm_essence|&bStorm Essence|LIGHTNING_ROD|RECIPE_MATERIAL|RARE|materials|Captured storm energy used in lightning recipes.|||0
            ancient_thread|&6Ancient Thread|STRING|RECIPE_MATERIAL|RARE|materials|Durable thread used in bow and armor recipes.|||0
            royal_gemstone|&dRoyal Gemstone|AMETHYST_CLUSTER|RECIPE_MATERIAL|EPIC|materials|A flawless gemstone reserved for royal gear recipes.|||0
            frostbite_dagger|&bFrostbite Dagger|IRON_SWORD|DAGGER|RARE|daggers|A swift frozen dagger with a keen critical edge.|DAMAGE=34-46;CRIT_CHANCE=12-18||650
            ember_blade|&6Ember Blade|GOLDEN_SWORD|SWORD|RARE|swords|A heated blade balanced for fast attacks.|DAMAGE=48-66;STRENGTH=16-26||800
            royal_greatsword|&6Royal Greatsword|DIAMOND_SWORD|GREATSWORD|EPIC|swords|A massive royal blade built for deliberate, powerful strikes.|DAMAGE=105-140;STRENGTH=42-62||1600
            storm_spear|&bStorm Spear|TRIDENT|SPEAR|EPIC|swords|A charged spear that rewards precise critical strikes.|DAMAGE=82-112;CRIT_DAMAGE=28-45||1450
            moon_staff|&fMoon Staff|END_ROD|STAFF|RARE|staffs_wands|A moonlit focus that increases its wielder's mana reserves.|MANA=90-135;DAMAGE=42-58||900
            crystal_pickaxe|&dCrystal Pickaxe|DIAMOND_PICKAXE|PICKAXE|RARE|tools|A precise crystal tool for profitable mining.|MINING_SPEED=155-215;MINING_FORTUNE=30-48||1000
            ember_drill|&6Ember Drill|NETHERITE_PICKAXE|DRILL|EPIC|tools|A powerful heated drill for the hardest deposits.|MINING_SPEED=230-310;BREAKING_POWER=6-7||1700
            forest_cutter|&2Forest Cutter|DIAMOND_AXE|FORAGING_AXE|RARE|tools|A balanced forestry axe made for rapid gathering.|FORAGING_FORTUNE=34-54;SPEED=4-7||900
            sunlit_hoe|&eSunlit Hoe|DIAMOND_HOE|FARMING_HOE|RARE|farming_equipment|A radiant hoe that encourages healthy crop growth.|FARMING_FORTUNE=42-68;CROP_GROWTH=8-14||850
            pearl_fishing_rod|&3Pearl Fishing Rod|FISHING_ROD|FISHING_ROD|RARE|fishing|A pearl-lined rod tuned for fast and fortunate catches.|FISHING_SPEED=30-45;FISHING_FORTUNE=32-52;TREASURE_CHANCE=3-6||950
            frost_helmet|&bFrost Helmet|DIAMOND_HELMET|HELMET|RARE|armor|A frozen helmet offering sturdy protection.|HEALTH=44-66;DEFENSE=28-44||750
            frost_chestplate|&bFrost Chestplate|DIAMOND_CHESTPLATE|CHESTPLATE|RARE|armor|A frozen chestplate offering sturdy protection.|HEALTH=82-118;DEFENSE=52-78||1050
            frost_leggings|&bFrost Leggings|DIAMOND_LEGGINGS|LEGGINGS|RARE|armor|Frozen leggings offering sturdy protection.|HEALTH=66-96;DEFENSE=42-64||950
            frost_boots|&bFrost Boots|DIAMOND_BOOTS|BOOTS|RARE|armor|Frozen boots that protect without sacrificing mobility.|HEALTH=38-58;DEFENSE=24-38;SPEED=3-6||700
            storm_ring|&bStorm Ring|LIGHT_BLUE_DYE|RING|RARE|rings|A passive ring carrying quiet storm energy.|MANA=35;CRIT_CHANCE=5||400
            ember_charm|&6Ember Charm|ORANGE_DYE|CHARM|RARE|trinkets|A passive charm that strengthens its bearer.|STRENGTH=9||400
            moon_necklace|&fMoon Necklace|QUARTZ|NECKLACE|EPIC|necklaces|A passive necklace infused with calm lunar energy.|MANA=55;HEALTH=40||700
            event_crate_key|&eEvent Crate Key|TRIPWIRE_HOOK|KEY|RARE|materials|A key awarded by SMP events for future event crates.|||0
            furniture_token|&aFurniture Token|EMERALD|TOKEN|UNCOMMON|materials|A token reserved for furniture shops and event rewards.|||0
            cosmic_bait|&dCosmic Bait|TROPICAL_FISH|FISHING_BAIT|EPIC|fishing|Cosmic bait that attracts treasure and unusual sea creatures.|||0
            """;

    private static final String EXPANDED_SMP_ITEMS = """
            ember_fang_sword|&6Ember Fang Sword|GOLDEN_SWORD|SWORD|RARE|swords|A heated blade forged from scorched fang fragments.|DAMAGE=65;STRENGTH=25;CRIT_DAMAGE=10|EMBER_SLASH
            frostbite_longsword|&bFrostbite Longsword|DIAMOND_SWORD|SWORD|EPIC|swords|A cold blade that slows enemies with icy cuts.|DAMAGE=90;STRENGTH=30;CRIT_CHANCE=5|FROSTBITE_STRIKE
            royal_guard_blade|&eRoyal Guard Blade|IRON_SWORD|SWORD|RARE|swords|A ceremonial sword carried by palace defenders.|DAMAGE=75;STRENGTH=20;DEFENSE=15|GUARDIAN_CUT
            voidsteel_saber|&5Voidsteel Saber|NETHERITE_SWORD|SWORD|LEGENDARY|swords|A saber folded with dark voidsteel.|DAMAGE=145;STRENGTH=55;CRIT_DAMAGE=35|VOID_REND
            sunforged_broadsword|&6Sunforged Broadsword|GOLDEN_SWORD|SWORD|EPIC|swords|A broad blade burning with sunlight.|DAMAGE=110;STRENGTH=45;HEALTH=25|SOLAR_CLEAVE
            shadow_sting_dagger|&8Shadow Sting Dagger|STONE_SWORD|DAGGER|RARE|daggers|A quick dagger made for silent strikes.|DAMAGE=45;CRIT_CHANCE=12;CRIT_DAMAGE=25|SHADOW_STING
            venom_tip_dagger|&aVenom Tip Dagger|IRON_SWORD|DAGGER|EPIC|daggers|A dagger coated with toxic dungeon venom.|DAMAGE=60;CRIT_CHANCE=10;STRENGTH=15|VENOM_CUT
            glassfang_dagger|&fGlassfang Dagger|DIAMOND_SWORD|DAGGER|RARE|daggers|A fragile-looking dagger with a razor edge.|DAMAGE=50;CRIT_CHANCE=15;CRIT_DAMAGE=20|PIERCING_FANG
            crimson_needle|&cCrimson Needle|GOLDEN_SWORD|DAGGER|LEGENDARY|daggers|A tiny blade that strikes like a beast fang.|DAMAGE=85;CRIT_CHANCE=18;CRIT_DAMAGE=45|BLOOD_NEEDLE
            nightfall_shiv|&5Nightfall Shiv|NETHERITE_SWORD|DAGGER|EPIC|daggers|A shiv that vanishes into the night.|DAMAGE=70;CRIT_CHANCE=16;SPEED=10|NIGHTFALL_DASH
            sakura_katana|&dSakura Katana|IRON_SWORD|KATANA|RARE|katanas|A light katana decorated with pink petals.|DAMAGE=70;STRENGTH=20;CRIT_CHANCE=7|SAKURA_SLICE
            stormcut_katana|&9Stormcut Katana|DIAMOND_SWORD|KATANA|EPIC|katanas|A katana that hums with storm energy.|DAMAGE=105;STRENGTH=35;CRIT_DAMAGE=20|STORMCUT
            oni_katana|&4Oni Katana|NETHERITE_SWORD|KATANA|LEGENDARY|katanas|A cursed katana from a forgotten dungeon boss.|DAMAGE=155;STRENGTH=60;CRIT_DAMAGE=40|ONI_RAGE
            moonlit_katana|&bMoonlit Katana|DIAMOND_SWORD|KATANA|EPIC|katanas|A blade that glows under moonlight.|DAMAGE=115;STRENGTH=30;MANA=50|MOONLIGHT_CUT
            bamboo_edge_katana|&aBamboo Edge Katana|IRON_SWORD|KATANA|UNCOMMON|katanas|A beginner katana with a flexible edge.|DAMAGE=45;STRENGTH=15;SPEED=5|BAMBOO_SWEEP
            windpiercer_bow|&aWindpiercer Bow|BOW|BOW|RARE|bows|A bow that fires arrows through strong winds.|DAMAGE=60;CRIT_CHANCE=8;CRIT_DAMAGE=15|WINDPIERCE
            emberstring_bow|&6Emberstring Bow|BOW|BOW|EPIC|bows|Its string glows with warm ember light.|DAMAGE=85;STRENGTH=25;CRIT_DAMAGE=25|EMBER_ARROW
            frozen_hunter_bow|&bFrozen Hunter Bow|BOW|BOW|EPIC|bows|A hunter bow from cold dungeon ruins.|DAMAGE=95;CRIT_CHANCE=10;FISHING_FORTUNE=5|FROZEN_ARROW
            royal_crossbow|&eRoyal Crossbow|CROSSBOW|CROSSBOW|RARE|bows|A precise crossbow used by royal scouts.|DAMAGE=80;STRENGTH=20;DEFENSE=10|ROYAL_BOLT
            voidshot_bow|&5Voidshot Bow|BOW|BOW|LEGENDARY|bows|A bow that shoots arrows into the void and back.|DAMAGE=135;CRIT_CHANCE=15;CRIT_DAMAGE=40|VOIDSHOT
            spark_wand|&eSpark Wand|BLAZE_ROD|WAND|UNCOMMON|staffs_wands|A small wand that releases sparks.|DAMAGE=25;MANA=40;ABILITY_DAMAGE=5|SPARK_BOLT
            frost_wand|&bFrost Wand|STICK|WAND|RARE|staffs_wands|A wand chilled by ancient ice.|DAMAGE=40;MANA=75;ABILITY_DAMAGE=15|FROST_BOLT
            toxic_wand|&2Toxic Wand|BLAZE_ROD|WAND|RARE|staffs_wands|A wand that leaks poisonous magic.|DAMAGE=45;MANA=70;ABILITY_DAMAGE=15|TOXIC_BURST
            void_wand|&5Void Wand|END_ROD|WAND|EPIC|staffs_wands|A wand that bends space around its target.|DAMAGE=65;MANA=120;ABILITY_DAMAGE=30|VOID_PULSE
            sunbeam_wand|&6Sunbeam Wand|BLAZE_ROD|WAND|EPIC|staffs_wands|A wand that fires concentrated sunlight.|DAMAGE=70;MANA=110;ABILITY_DAMAGE=25|SUNBEAM
            apprentice_staff|&aApprentice Staff|STICK|STAFF|UNCOMMON|staffs_wands|A beginner staff for new mages.|DAMAGE=35;MANA=60;ABILITY_DAMAGE=10|MAGIC_MISSILE
            stormcaller_staff|&9Stormcaller Staff|LIGHTNING_ROD|STAFF|EPIC|staffs_wands|A staff that calls sparks from storm clouds.|DAMAGE=90;MANA=160;ABILITY_DAMAGE=35|STORMCALL
            necrotic_staff|&8Necrotic Staff|BONE|STAFF|LEGENDARY|staffs_wands|A staff carved from cursed dungeon bone.|DAMAGE=110;MANA=220;ABILITY_DAMAGE=50|NECROTIC_WAVE
            crystal_staff|&dCrystal Staff|AMETHYST_SHARD|STAFF|EPIC|staffs_wands|A staff focused through a glowing crystal.|DAMAGE=85;MANA=170;ABILITY_DAMAGE=30|CRYSTAL_BEAM
            infernal_staff|&cInfernal Staff|BLAZE_ROD|STAFF|LEGENDARY|staffs_wands|A staff burning with infernal magic.|DAMAGE=130;MANA=200;ABILITY_DAMAGE=55|INFERNAL_NOVA
            lesser_healing_potion|&cLesser Healing Potion|POTION|POTION|COMMON|potions|Restores a small amount of health.||
            greater_healing_potion|&cGreater Healing Potion|POTION|POTION|RARE|potions|Restores a large amount of health.||
            lesser_mana_potion|&bLesser Mana Potion|POTION|POTION|COMMON|potions|Restores a small amount of mana.||
            greater_mana_potion|&bGreater Mana Potion|POTION|POTION|RARE|potions|Restores a large amount of mana.||
            iron_skin_potion|&7Iron Skin Potion|POTION|POTION|UNCOMMON|potions|Temporarily hardens your skin.||
            swift_step_potion|&aSwift Step Potion|POTION|POTION|UNCOMMON|potions|Makes your steps lighter for a short time.||
            honey_bread|&6Honey Bread|BREAD|FOOD|COMMON|food|Fresh bread glazed with sweet honey.||
            spiced_mushroom_soup|&cSpiced Mushroom Soup|MUSHROOM_STEW|FOOD|UNCOMMON|food|A warm soup with a small combat kick.||
            fishermans_stew|&bFisherman's Stew|RABBIT_STEW|FOOD|UNCOMMON|food|A hearty stew popular with fishermen.||
            miners_pie|&6Miner's Pie|PUMPKIN_PIE|FOOD|UNCOMMON|food|A dense pie made for long mining trips.||
            forest_berry_tart|&dForest Berry Tart|SWEET_BERRIES|FOOD|RARE|food|A berry treat from deep forest camps.||
            royal_feast_plate|&eRoyal Feast Plate|COOKED_BEEF|FOOD|EPIC|food|A feast worthy of royal guards.||
            miners_luck_charm|&6Miner's Luck Charm|RAW_GOLD|CHARM|RARE|charms|A charm carried by deep cave miners.|MINING_FORTUNE=15;DEFENSE=5|
            farmers_blessing_charm|&aFarmer's Blessing Charm|WHEAT|CHARM|RARE|charms|A charm blessed for better harvests.|FARMING_FORTUNE=15;HEALTH=10|
            fishermans_pearl_charm|&bFisherman's Pearl Charm|ENDER_PEARL|CHARM|EPIC|charms|A pearl charm used by expert fishers.|FISHING_FORTUNE=20;SEA_CREATURE_CHANCE=3|
            shadow_luck_charm|&5Shadow Luck Charm|AMETHYST_SHARD|CHARM|EPIC|charms|A charm that twists luck in dark places.|MAGIC_FIND=10;CRIT_DAMAGE=10|
            warriors_focus_charm|&cWarrior's Focus Charm|REDSTONE|CHARM|RARE|charms|A charm used to focus during combat.|STRENGTH=15;CRIT_CHANCE=3|
            ancient_guard_charm|&eAncient Guard Charm|SHIELD|CHARM|LEGENDARY|charms|A charm once carried by an ancient defender.|DEFENSE=35;HEALTH=50|
            totem_of_minor_vigor|&aTotem of Minor Vigor|TOTEM_OF_UNDYING|TOTEM|UNCOMMON|totems|A small totem that strengthens the body.|HEALTH=25;DEFENSE=5|
            totem_of_mana_flow|&bTotem of Mana Flow|TOTEM_OF_UNDYING|TOTEM|RARE|totems|A totem that helps mana flow smoothly.|MANA=75;MANA_REGEN=5|
            totem_of_battle_fury|&cTotem of Battle Fury|TOTEM_OF_UNDYING|TOTEM|EPIC|totems|A totem carved for battle.|STRENGTH=25;CRIT_DAMAGE=20|
            totem_of_deep_mines|&6Totem of Deep Mines|TOTEM_OF_UNDYING|TOTEM|EPIC|totems|A totem carried through dangerous mines.|MINING_FORTUNE=25;DEFENSE=15|
            totem_of_ancient_light|&eTotem of Ancient Light|TOTEM_OF_UNDYING|TOTEM|LEGENDARY|totems|A glowing totem filled with old light.|HEALTH=75;DEFENSE=30;MAGIC_FIND=5|
            totem_of_void_resistance|&5Totem of Void Resistance|TOTEM_OF_UNDYING|TOTEM|LEGENDARY|totems|A totem used to resist void magic.|DEFENSE=45;MANA=100;ABILITY_DAMAGE=10|
            bronze_adventurer_necklace|&6Bronze Adventurer Necklace|GOLD_NUGGET|NECKLACE|UNCOMMON|necklaces|A simple necklace for new adventurers.|HEALTH=20;STRENGTH=5|
            crystal_heart_necklace|&dCrystal Heart Necklace|AMETHYST_SHARD|NECKLACE|RARE|necklaces|A necklace pulsing with crystal energy.|HEALTH=45;MANA=40|
            hunters_fang_necklace|&cHunter's Fang Necklace|RABBIT_FOOT|NECKLACE|RARE|necklaces|A necklace strung with beast fangs.|CRIT_CHANCE=5;CRIT_DAMAGE=20|
            royal_sun_necklace|&eRoyal Sun Necklace|SUNFLOWER|NECKLACE|EPIC|necklaces|A golden necklace worn by royal mages.|HEALTH=60;DEFENSE=20;MANA=40|
            shadow_chain_necklace|&5Shadow Chain Necklace|CHAIN|NECKLACE|EPIC|necklaces|A chain necklace forged in shadow.|STRENGTH=20;CRIT_DAMAGE=25;MAGIC_FIND=5|
            ascended_star_necklace|&dAscended Star Necklace|NETHER_STAR|NECKLACE|LEGENDARY|necklaces|A necklace made from a fallen star.|HEALTH=100;MANA=100;STRENGTH=25;CRIT_DAMAGE=30|
            """;

    private static void applyExpandedSmpItemDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        for (String line : EXPANDED_SMP_ITEMS.strip().split("\\R")) {
            String[] p = line.trim().split("\\|", -1);
            if (p.length < 9) continue;
            String id = normalize(p[0]);
            MMOItemType type = MMOItemType.parse(p[3]);
            ConfigurationSection item = ensureItem(items, id);
            item.set("name", p[1]); item.set("material", p[2]); item.set("type", p[3]);
            item.set("rarity", p[4]); item.set("category", p[5]); item.set("lore", wrapLore(p[6]));
            item.set("mmo", null); item.set("mmo.type", p[3]);
            boolean rollable = type.canRollRandomRarityEffects() && type != MMOItemType.FOOD && type != MMOItemType.POTION;
            item.set("mmo.random-roll", rollable); if (!p[7].isBlank()) applyStatRanges(item, p[7]);
            item.set("ability.type", p[8].isBlank() ? "none" : p[8].toLowerCase(Locale.ROOT));
            item.set("ability.mana-cost", 0);
            item.set("consumable.enabled", type == MMOItemType.FOOD || type == MMOItemType.POTION);
            item.set("unbreakable", rollable); item.set("max-stack-size", rollable ? null : 64);
        }
        consumable(items, "lesser_healing_potion", 40, 0, 0, 0, "");
        consumable(items, "greater_healing_potion", 150, 0, 0, 0, "");
        consumable(items, "lesser_mana_potion", 0, 50, 0, 0, "");
        consumable(items, "greater_mana_potion", 0, 175, 0, 0, "");
        consumable(items, "iron_skin_potion", 0, 0, 0, 180, "DEFENSE=50");
        consumable(items, "swift_step_potion", 0, 0, 0, 180, "SPEED=20");
        consumable(items, "honey_bread", 20, 0, 6, 0, "");
        consumable(items, "spiced_mushroom_soup", 45, 0, 8, 60, "STRENGTH=5");
        consumable(items, "fishermans_stew", 35, 0, 8, 90, "FISHING_FORTUNE=5");
        consumable(items, "miners_pie", 40, 0, 8, 90, "MINING_FORTUNE=5");
        consumable(items, "forest_berry_tart", 60, 0, 5, 90, "FORAGING_FORTUNE=8");
        consumable(items, "royal_feast_plate", 150, 0, 10, 300, "HEALTH=50");
    }

    private static void consumable(ConfigurationSection items, String id, int heal, int mana, int food, int duration, String buffs) {
        ConfigurationSection item = items.getConfigurationSection(id);
        if (item == null) return;
        item.set("consumable.heal", heal); item.set("consumable.mana", mana);
        item.set("consumable.food", food); item.set("consumable.saturation", food > 0 ? 3.0D : 0.0D);
        for (String entry : buffs.split(";")) {
            String[] stat = entry.split("=", 2); if (stat.length != 2) continue;
            item.set("consumable.stat-buffs." + stat[0] + ".amount", parseInt(stat[1], 0));
            item.set("consumable.stat-buffs." + stat[0] + ".duration-seconds", Math.max(1, duration));
        }
    }

    private static void applyRequestedCustomItemDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        for (String line : REQUESTED_CUSTOM_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 10) continue;
            String id = normalize(parts[0]);
            MMOItemType type = MMOItemType.parse(parts[3]);
            ConfigurationSection item = ensureItem(items, id);
            item.set("name", parts[1]);
            item.set("material", parts[2]);
            item.set("type", parts[3]);
            item.set("rarity", parts[4]);
            item.set("category", parts[5]);
            item.set("lore", wrapLore(parts[6]));
            item.set("ability", null);
            item.set("ability.type", "none");
            item.set("ability.mana-cost", 0);
            item.set("consumable.enabled", false);
            item.set("mmo", null);
            item.set("mmo.type", parts[3]);
            item.set("mmo.random-roll", type.canRollRandomRarityEffects() && !parts[7].isBlank());
            if (!parts[7].isBlank()) applyStatRanges(item, parts[7]);
            boolean randomGear = type.canRollRandomRarityEffects();
            item.set("unbreakable", randomGear);
            item.set("max-stack-size", randomGear ? null : 64);
            item.set("sell-price", parseInt(parts[9], 0));
            item.set("sellall-allowed", !Set.of("event_crate_key", "furniture_token", "cosmic_bait").contains(id));
            item.set("guild-sellable", !Set.of("event_crate_key", "furniture_token", "cosmic_bait").contains(id));
        }

        items.set("star_fragment.recipe-material.category", "event_cosmetic");
        items.set("moonlit_cloth.recipe-material.category", "armor_cosmetic");
        items.set("sun_core.recipe-material.category", "fire_light");
        items.set("glacial_shard.recipe-material.category", "frost");
        items.set("infernal_core.recipe-material.category", "fire_weapons");
        items.set("storm_essence.recipe-material.category", "lightning");
        items.set("ancient_thread.recipe-material.category", "bows_armor");
        items.set("royal_gemstone.recipe-material.category", "royal_gear");
        items.set("frost_crystal.collection.category", "mining");
        items.set("ember_shard.collection.category", "combat");

        addRequestedCollection(config, "frost_crystal", "&bFrost Crystal Collection", "mining", "BLUE_ICE");
        addRequestedCollection(config, "ember_shard", "&6Ember Shard Collection", "combat", "BLAZE_POWDER");
    }

    private static void addRequestedCollection(YamlConfiguration config, String id, String name, String category, String icon) {
        String path = "collections.items." + id;
        config.set(path + ".name", name);
        config.set(path + ".category", category);
        config.set(path + ".icon", icon);
        config.set(path + ".materials", List.of());
    }

    private static final String REQUESTED_GEAR_ITEMS = """
            frostguard_helmet|&bFrostguard Helmet|DIAMOND_HELMET|HELMET|RARE|armor|A frost-forged helmet built for veteran defenders.|HEALTH=80;DEFENSE=45||combat|10
            frostguard_chestplate|&bFrostguard Chestplate|DIAMOND_CHESTPLATE|CHESTPLATE|RARE|armor|A frost-forged chestplate built for veteran defenders.|HEALTH=140;DEFENSE=75||combat|10
            frostguard_leggings|&bFrostguard Leggings|DIAMOND_LEGGINGS|LEGGINGS|RARE|armor|Frost-forged leggings built for veteran defenders.|HEALTH=120;DEFENSE=60||combat|10
            frostguard_boots|&bFrostguard Boots|DIAMOND_BOOTS|BOOTS|RARE|armor|Frost-forged boots that remain light on their feet.|HEALTH=70;DEFENSE=35;SPEED=5||combat|10
            shadow_hood|&8Shadow Hood|LEATHER_HELMET|HELMET|EPIC|armor|A silent hood favored by experienced assassins.|HEALTH=60;DEFENSE=25;CRIT_CHANCE=5;CRIT_DAMAGE=15||combat|18
            shadow_tunic|&8Shadow Tunic|LEATHER_CHESTPLATE|CHESTPLATE|EPIC|armor|A light tunic woven from light-drinking fibers.|HEALTH=110;DEFENSE=40;CRIT_DAMAGE=25||combat|18
            shadow_leggings|&8Shadow Leggings|LEATHER_LEGGINGS|LEGGINGS|EPIC|armor|Flexible shadow armor made for rapid movement.|HEALTH=90;DEFENSE=35;SPEED=8||combat|18
            shadow_boots|&8Shadow Boots|LEATHER_BOOTS|BOOTS|EPIC|armor|Nearly weightless boots made for silent pursuit.|HEALTH=55;DEFENSE=20;SPEED=12||combat|18
            ember_knight_helmet|&6Ember Knight Helmet|NETHERITE_HELMET|HELMET|EPIC|armor|A heated netherite helmet worn by ember knights.|HEALTH=120;DEFENSE=55;STRENGTH=10||combat|22
            ember_knight_chestplate|&6Ember Knight Chestplate|NETHERITE_CHESTPLATE|CHESTPLATE|EPIC|armor|A massive heated chestplate worn by ember knights.|HEALTH=210;DEFENSE=95;STRENGTH=20||combat|22
            ember_knight_leggings|&6Ember Knight Leggings|NETHERITE_LEGGINGS|LEGGINGS|EPIC|armor|Reinforced heated leggings worn by ember knights.|HEALTH=170;DEFENSE=80;STRENGTH=15||combat|22
            ember_knight_boots|&6Ember Knight Boots|NETHERITE_BOOTS|BOOTS|EPIC|armor|Heatproof boots designed for aggressive advances.|HEALTH=100;DEFENSE=45;SPEED=6||combat|22
            glacial_longsword|&bGlacial Longsword|DIAMOND_SWORD|SWORD|RARE|swords|A long frozen blade with a precise critical edge.|DAMAGE=95;STRENGTH=35;CRIT_DAMAGE=20||combat|12
            ember_cutlass|&6Ember Cutlass|GOLDEN_SWORD|SWORD|RARE|swords|A fast heated cutlass made for close combat.|DAMAGE=80;STRENGTH=45||combat|14
            royal_blade|&6Royal Blade|DIAMOND_SWORD|SWORD|EPIC|swords|An expertly balanced blade bearing a royal crest.|DAMAGE=135;STRENGTH=60;CRIT_DAMAGE=35||combat|20
            void_reaver|&5Void Reaver|NETHERITE_SWORD|SWORD|LEGENDARY|swords|A heavy reaver that tears unstable paths through the void.|DAMAGE=190;STRENGTH=85;CRIT_CHANCE=5;CRIT_DAMAGE=50||combat|30
            quickfang_dagger|&aQuickfang Dagger|IRON_SWORD|DAGGER|UNCOMMON|daggers|A compact dagger built for relentless rapid strikes.|DAMAGE=45;CRIT_CHANCE=8||combat|5
            venom_dagger|&2Venom Dagger|STONE_SWORD|DAGGER|RARE|daggers|A sharpened stone dagger coated in potent venom.|DAMAGE=70;CRIT_CHANCE=10;CRIT_DAMAGE=20||combat|13
            assassin_dagger|&8Assassin Dagger|NETHERITE_SWORD|DAGGER|EPIC|daggers|A master assassin's blade made for decisive critical attacks.|DAMAGE=110;CRIT_CHANCE=15;CRIT_DAMAGE=45;SPEED=8||combat|24
            wind_katana|&fWind Katana|IRON_SWORD|KATANA|RARE|katanas|A light katana that moves with the wind.|DAMAGE=85;STRENGTH=25;SPEED=10||combat|11
            blood_katana|&4Blood Katana|DIAMOND_SWORD|KATANA|EPIC|katanas|A crimson katana honed for devastating critical cuts.|DAMAGE=130;STRENGTH=45;CRIT_DAMAGE=40||combat|21
            moonlight_katana|&fMoonlight Katana|NETHERITE_SWORD|KATANA|LEGENDARY|katanas|A legendary katana illuminated by cold moonlight.|DAMAGE=175;STRENGTH=65;CRIT_CHANCE=8;CRIT_DAMAGE=60||combat|32
            hunter_shortbow|&aHunter Shortbow|BOW|BOW|UNCOMMON|bows|A compact bow suitable for aspiring monster hunters.|DAMAGE=55;CRIT_CHANCE=5||combat|6
            storm_bow|&bStorm Bow|BOW|BOW|RARE|bows|A charged bow that releases arrows with a thunderous snap.|DAMAGE=100;STRENGTH=25;CRIT_DAMAGE=25||combat|15
            dragonbone_bow|&6Dragonbone Bow|BOW|BOW|EPIC|bows|A powerful bow reinforced with ancient dragonbone.|DAMAGE=145;STRENGTH=50;CRIT_CHANCE=5;CRIT_DAMAGE=45||combat|25
            crystal_pickaxe|&dCrystal Pickaxe|DIAMOND_PICKAXE|PICKAXE|RARE|tools|A precise crystal tool for profitable mining.|MINING_SPEED=150;MINING_FORTUNE=25||mining|12
            molten_drill|&6Molten Drill|NETHERITE_PICKAXE|DRILL|EPIC|tools|A molten drill capable of cutting hardened deposits.|MINING_SPEED=260;MINING_FORTUNE=45;BREAKING_POWER=2||mining|22
            royal_fishing_rod|&6Royal Fishing Rod|FISHING_ROD|FISHING_ROD|EPIC|fishing|A royal rod made for experienced treasure anglers.|FISHING_SPEED=35;FISHING_FORTUNE=45;TREASURE_CHANCE=10||fishing|20
            arcane_wand|&9Arcane Wand|BLAZE_ROD|WAND|RARE|staffs_wands|An arcane focus that amplifies spellcasting power.|DAMAGE=65;MANA=120;ABILITY_DAMAGE=20||combat|15
            celestial_staff|&dCelestial Staff|END_ROD|STAFF|LEGENDARY|staffs_wands|A celestial focus holding immense magical potential.|DAMAGE=130;MANA=300;ABILITY_DAMAGE=55;INTELLIGENCE=80||combat|30
            """;

    private static void applyRequestedGearItemDefaults(YamlConfiguration config) {
        applyGearItemRows(config, REQUESTED_GEAR_ITEMS);
    }

    private static final String REQUESTED_SWORD_ITEMS = """
            copper_shortblade|&6Copper Shortblade|IRON_SWORD|SWORD|COMMON|swords|A compact copper-toned blade made for new fighters.|DAMAGE=35;STRENGTH=8||combat|2
            ironbound_sword|&fIronbound Sword|IRON_SWORD|SWORD|COMMON|swords|A dependable iron sword reinforced along its spine.|DAMAGE=45;STRENGTH=12||combat|4
            silver_knight_blade|&fSilver Knight Blade|IRON_SWORD|SWORD|UNCOMMON|swords|A polished knightly blade that offers modest protection.|DAMAGE=65;STRENGTH=20;DEFENSE=10||combat|8
            forest_guard_sword|&2Forest Guard Sword|IRON_SWORD|SWORD|UNCOMMON|swords|A swift woodland guard blade balanced for patrols.|DAMAGE=72;STRENGTH=24;SPEED=3||combat|9
            bandit_cutlass|&7Bandit Cutlass|GOLDEN_SWORD|SWORD|UNCOMMON|swords|A quick curved cutlass favored by opportunistic bandits.|DAMAGE=78;STRENGTH=18;CRIT_CHANCE=4||combat|10
            crimson_edge|&cCrimson Edge|DIAMOND_SWORD|SWORD|RARE|swords|A crimson-edged sword honed for punishing critical strikes.|DAMAGE=105;STRENGTH=35;CRIT_DAMAGE=20||combat|14
            glacier_saber|&bGlacier Saber|DIAMOND_SWORD|SWORD|RARE|swords|A frozen saber that reinforces its wielder against attacks.|DAMAGE=112;STRENGTH=30;DEFENSE=25||combat|16
            venomfang_sword|&2Venomfang Sword|DIAMOND_SWORD|SWORD|RARE|swords|A fast venom-colored fang made for precise openings.|DAMAGE=120;STRENGTH=34;CRIT_CHANCE=6||combat|17
            stormsteel_blade|&bStormsteel Blade|DIAMOND_SWORD|SWORD|RARE|swords|A storm-tempered blade with a violently charged edge.|DAMAGE=128;STRENGTH=42;CRIT_DAMAGE=28||combat|18
            royal_paladin_sword|&6Royal Paladin Sword|GOLDEN_SWORD|SWORD|EPIC|swords|A radiant royal sword that fortifies its sworn bearer.|DAMAGE=145;STRENGTH=55;DEFENSE=35;HEALTH=80||combat|22
            emberheart_blade|&6Emberheart Blade|NETHERITE_SWORD|SWORD|EPIC|swords|A netherite blade forged around an enduring ember heart.|DAMAGE=160;STRENGTH=70;CRIT_DAMAGE=35||combat|24
            frostmourne_saber|&bFrostmourne Saber|NETHERITE_SWORD|SWORD|EPIC|swords|A cold saber carrying defensive frost and stored mana.|DAMAGE=168;STRENGTH=62;DEFENSE=45;MANA=50||combat|25
            shadowbane_sword|&8Shadowbane Sword|NETHERITE_SWORD|SWORD|EPIC|swords|A rapid shadow blade designed for lethal critical attacks.|DAMAGE=175;STRENGTH=65;CRIT_CHANCE=8;CRIT_DAMAGE=45||combat|26
            dragonfire_blade|&cDragonfire Blade|NETHERITE_SWORD|SWORD|LEGENDARY|swords|A heavy legendary blade tempered in dragon fire.|DAMAGE=215;STRENGTH=95;CRIT_DAMAGE=65||combat|32
            celestial_longsword|&dCelestial Longsword|NETHERITE_SWORD|SWORD|LEGENDARY|swords|A celestial weapon that amplifies mana-driven attacks.|DAMAGE=225;STRENGTH=85;MANA=120;ABILITY_DAMAGE=35||combat|34
            voidlord_blade|&5Voidlord Blade|NETHERITE_SWORD|SWORD|LEGENDARY|swords|A deliberate void blade with overwhelming critical power.|DAMAGE=245;STRENGTH=110;CRIT_CHANCE=10;CRIT_DAMAGE=80||combat|36
            ancient_warblade|&6Ancient Warblade|NETHERITE_SWORD|SWORD|LEGENDARY|swords|An immense ancient warblade that strengthens and protects.|DAMAGE=260;STRENGTH=120;DEFENSE=50;HEALTH=150||combat|38
            starforged_sword|&dStarforged Sword|NETHERITE_SWORD|SWORD|MYTHIC|swords|A mythic sword forged from condensed stellar metal.|DAMAGE=310;STRENGTH=150;CRIT_CHANCE=12;CRIT_DAMAGE=100;MANA=150||combat|45
            abyssal_greatsword|&5Abyssal Greatsword|NETHERITE_SWORD|GREATSWORD|MYTHIC|swords|A colossal abyssal weapon built for devastating measured blows.|DAMAGE=360;STRENGTH=180;CRIT_DAMAGE=120||combat|50
            admin_test_sword|&cAdmin Test Sword|NETHERITE_SWORD|SWORD|SPECIAL|swords|An administrator testing weapon excluded from automatic drops.|DAMAGE=500;STRENGTH=250;CRIT_CHANCE=25;CRIT_DAMAGE=250||combat|0
            """;

    private static void applyRequestedSwordItemDefaults(YamlConfiguration config) {
        applyGearItemRows(config, REQUESTED_SWORD_ITEMS);
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items != null) {
            items.set("admin_test_sword.admin-only", true);
            items.set("admin_test_sword.automatic-drop-eligible", false);
        }
    }

    private static final String REQUESTED_BOW_ITEMS = """
            oak_hunter_bow|&2Oak Hunter Bow|BOW|BOW|COMMON|bows|A simple oak bow built for beginning hunters.|DAMAGE=35;CRIT_CHANCE=2||archery|2
            reinforced_shortbow|&fReinforced Shortbow|BOW|SHORTBOW|COMMON|bows|A compact reinforced bow suited to quick engagements.|DAMAGE=45;STRENGTH=8||archery|4
            silverstring_bow|&fSilverstring Bow|BOW|BOW|UNCOMMON|bows|A polished bow strung with resilient silver thread.|DAMAGE=65;STRENGTH=15;CRIT_CHANCE=3||archery|8
            forest_ranger_bow|&2Forest Ranger Bow|BOW|BOW|UNCOMMON|bows|A mobile woodland bow favored by experienced rangers.|DAMAGE=72;STRENGTH=18;SPEED=3||archery|9
            bandit_shortbow|&7Bandit Shortbow|BOW|SHORTBOW|UNCOMMON|bows|A compact bandit bow designed for opportunistic shots.|DAMAGE=78;STRENGTH=16;CRIT_CHANCE=5||archery|10
            crimson_longbow|&cCrimson Longbow|BOW|BOW|RARE|bows|A crimson longbow that rewards carefully placed arrows.|DAMAGE=105;STRENGTH=30;CRIT_DAMAGE=20||archery|14
            glacier_bow|&bGlacier Bow|BOW|BOW|RARE|bows|A frozen bow that lends its wielder defensive resilience.|DAMAGE=112;STRENGTH=26;DEFENSE=15||archery|16
            venomstring_bow|&2Venomstring Bow|BOW|BOW|RARE|bows|A venom-colored bow made for precise critical openings.|DAMAGE=120;STRENGTH=32;CRIT_CHANCE=6||archery|17
            stormpiercer_bow|&bStormpiercer Bow|BOW|BOW|RARE|bows|A charged bow whose arrows pierce with storm force.|DAMAGE=128;STRENGTH=38;CRIT_DAMAGE=28||archery|18
            royal_marksman_bow|&6Royal Marksman Bow|BOW|BOW|EPIC|bows|An ornate royal bow made for elite marksmen.|DAMAGE=145;STRENGTH=50;CRIT_CHANCE=6;CRIT_DAMAGE=30||archery|22
            emberheart_bow|&6Emberheart Bow|BOW|BOW|EPIC|bows|A powerful bow built around an enduring ember heart.|DAMAGE=160;STRENGTH=65;CRIT_DAMAGE=40||archery|24
            frostmourne_longbow|&bFrostmourne Longbow|BOW|BOW|EPIC|bows|A cold longbow carrying mana through every drawn arrow.|DAMAGE=168;STRENGTH=60;MANA=50;ABILITY_DAMAGE=15||archery|25
            shadowbane_shortbow|&8Shadowbane Shortbow|BOW|SHORTBOW|EPIC|bows|A compact shadow bow made for lethal critical volleys.|DAMAGE=175;STRENGTH=62;CRIT_CHANCE=9;CRIT_DAMAGE=45||archery|26
            dragonfire_bow|&cDragonfire Bow|BOW|BOW|LEGENDARY|bows|A legendary bow tempered by the heat of dragon fire.|DAMAGE=215;STRENGTH=90;CRIT_DAMAGE=65||archery|32
            celestial_longbow|&dCelestial Longbow|BOW|BOW|LEGENDARY|bows|A celestial longbow that amplifies mana-driven attacks.|DAMAGE=225;STRENGTH=85;MANA=120;ABILITY_DAMAGE=35||archery|34
            voidlord_bow|&5Voidlord Bow|BOW|BOW|LEGENDARY|bows|A deliberate void bow with overwhelming critical power.|DAMAGE=245;STRENGTH=105;CRIT_CHANCE=10;CRIT_DAMAGE=80||archery|36
            ancient_warbow|&6Ancient Warbow|BOW|BOW|LEGENDARY|bows|An immense ancient warbow that strengthens and protects.|DAMAGE=260;STRENGTH=115;DEFENSE=35;HEALTH=100||archery|38
            starforged_bow|&dStarforged Bow|BOW|BOW|MYTHIC|bows|A mythic bow forged from condensed stellar metal.|DAMAGE=310;STRENGTH=145;CRIT_CHANCE=12;CRIT_DAMAGE=100;MANA=150||archery|45
            abyssal_greatbow|&5Abyssal Greatbow|BOW|LONGBOW|MYTHIC|bows|A colossal abyssal bow built for devastating distant shots.|DAMAGE=360;STRENGTH=175;CRIT_DAMAGE=120||archery|50
            admin_test_bow|&cAdmin Test Bow|BOW|BOW|SPECIAL|bows|An administrator testing bow excluded from automatic drops.|DAMAGE=500;STRENGTH=250;CRIT_CHANCE=25;CRIT_DAMAGE=250||archery|0
            """;

    private static void applyRequestedBowItemDefaults(YamlConfiguration config) {
        applyGearItemRows(config, REQUESTED_BOW_ITEMS);
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items != null) {
            items.set("admin_test_bow.admin-only", true);
            items.set("admin_test_bow.automatic-drop-eligible", false);
        }
    }

    private static final String REQUESTED_CHARM_ITEMS = """
            tiny_health_charm|&aTiny Health Charm|LIME_DYE|CHARM|COMMON|charms|A tiny charm carrying a trace of restorative vitality.|HEALTH=25||combat|0
            minor_defense_charm|&7Minor Defense Charm|GRAY_DYE|CHARM|COMMON|charms|A modest ward that hardens its equipped bearer.|DEFENSE=8||combat|0
            sparking_mana_charm|&bSparking Mana Charm|LIGHT_BLUE_DYE|CHARM|COMMON|charms|A small focus crackling with stored mana.|MANA=20||combat|0
            swiftstep_charm|&fSwiftstep Charm|FEATHER|CHARM|UNCOMMON|charms|A featherlight charm that quickens every step.|SPEED=3||combat|5
            miners_luck_charm|&6Miner's Luck Charm|RAW_GOLD|CHARM|UNCOMMON|charms|A raw-gold charm said to guide miners toward richer deposits.|MINING_FORTUNE=8||mining|5
            farmers_luck_charm|&aFarmer's Luck Charm|WHEAT|CHARM|UNCOMMON|charms|A field charm that improves the yield of careful farmers.|FARMING_FORTUNE=8||farming|5
            fishermans_luck_charm|&3Fisherman's Luck Charm|PRISMARINE_SHARD|CHARM|UNCOMMON|charms|A sea-worn charm that favors patient anglers.|FISHING_FORTUNE=8||fishing|5
            lumberjacks_luck_charm|&2Lumberjack's Luck Charm|OAK_SAPLING|CHARM|UNCOMMON|charms|A living charm that improves woodland harvests.|FORAGING_FORTUNE=8||foraging|5
            archers_focus_charm|&eArcher's Focus Charm|ARROW|CHARM|RARE|charms|A precise focus carried by disciplined archers.|CRIT_CHANCE=3;CRIT_DAMAGE=10||archery|10
            warriors_strength_charm|&cWarrior's Strength Charm|RED_DYE|CHARM|RARE|charms|A crimson charm that reinforces martial strength.|STRENGTH=18||combat|12
            guardian_charm|&9Guardian Charm|LAPIS_LAZULI|CHARM|RARE|charms|A protective lapis ward that strengthens body and armor.|DEFENSE=25;HEALTH=60||combat|14
            alchemists_charm|&dAlchemist's Charm|BREWING_STAND|CHARM|RARE|charms|An alchemical focus that improves study and magical reserves.|ALCHEMY_XP_BOOST=5;MANA=40||alchemy|10
            enchanters_charm|&5Enchanter's Charm|ENCHANTING_TABLE|CHARM|RARE|charms|An arcane charm that rewards dedicated enchanting practice.|ENCHANTING_XP_BOOST=5;INTELLIGENCE=25||enchanting|10
            repairmasters_charm|&7Repairmaster's Charm|ANVIL|CHARM|RARE|charms|A dense forge charm used by master repairers.|REPAIR_XP_BOOST=5;DEFENSE=15||repair|10
            smelters_charm|&6Smelter's Charm|BLAST_FURNACE|CHARM|RARE|charms|A warm industrial charm that improves smelting and mining.|SMELTING_XP_BOOST=5;MINING_FORTUNE=12||smelting|10
            sea_creature_charm|&3Sea Creature Charm|HEART_OF_THE_SEA|CHARM|EPIC|charms|A deep-sea charm that draws unusual creatures closer.|SEA_CREATURE_CHANCE=5;FISHING_FORTUNE=20||fishing|18
            treasure_hunter_charm|&eTreasure Hunter Charm|GOLD_NUGGET|CHARM|EPIC|charms|A gilded charm drawn toward hidden valuables.|TREASURE_CHANCE=8;MAGIC_FIND=5||excavation|18
            shadow_crit_charm|&8Shadow Crit Charm|BLACK_DYE|CHARM|EPIC|charms|A shadowed focus that sharpens critical attacks.|CRIT_CHANCE=6;CRIT_DAMAGE=35||combat|24
            dragonheart_charm|&cDragonheart Charm|DRAGON_BREATH|CHARM|LEGENDARY|charms|A legendary charm carrying the vitality of a dragon.|HEALTH=250;STRENGTH=35;CRIT_DAMAGE=35||combat|32
            celestial_balance_charm|&dCelestial Balance Charm|NETHER_STAR|CHARM|MYTHIC|charms|A mythic charm balancing every major combat discipline.|HEALTH=300;DEFENSE=80;MANA=200;STRENGTH=50;CRIT_CHANCE=5;CRIT_DAMAGE=50||combat|40
            """;

    private static void applyRequestedCharmItemDefaults(YamlConfiguration config) {
        applyGearItemRows(config, REQUESTED_CHARM_ITEMS);
    }

    private static final String REQUESTED_TOOL_ITEMS = """
            copper_pickaxe|&6Copper Pickaxe|IRON_PICKAXE|PICKAXE|COMMON|pickaxes|A practical copper-colored pickaxe for new miners.|MINING_SPEED=45;MINING_FORTUNE=5;BREAKING_POWER=2|none|mining|2
            deepstone_pickaxe|&8Deepstone Pickaxe|STONE_PICKAXE|PICKAXE|UNCOMMON|pickaxes|A dense pickaxe made to pulse through ordinary stone.|MINING_SPEED=75;MINING_FORTUNE=10;BREAKING_POWER=3|stone_pulse|mining|6
            silver_pickaxe|&fSilver Pickaxe|IRON_PICKAXE|PICKAXE|UNCOMMON|pickaxes|A bright pickaxe that resonates with nearby ore.|MINING_SPEED=95;MINING_FORTUNE=15;BREAKING_POWER=3|prospect|mining|8
            crystalbound_pickaxe|&dCrystalbound Pickaxe|DIAMOND_PICKAXE|PICKAXE|RARE|pickaxes|A crystal-bound pickaxe tuned to hidden ore deposits.|MINING_SPEED=150;MINING_FORTUNE=30;BREAKING_POWER=4|prospect|mining|14
            molten_core_pickaxe|&6Molten Core Pickaxe|NETHERITE_PICKAXE|PICKAXE|EPIC|pickaxes|A molten pickaxe capable of bursting entire ore veins.|MINING_SPEED=230;MINING_FORTUNE=45;BREAKING_POWER=5|vein_burst|mining|22
            void_miner_pickaxe|&5Void Miner Pickaxe|NETHERITE_PICKAXE|PICKAXE|LEGENDARY|pickaxes|A void-forged pickaxe for the richest deep deposits.|MINING_SPEED=340;MINING_FORTUNE=80;TREASURE_CHANCE=8;BREAKING_POWER=6|vein_burst|mining|34
            royal_drill|&6Royal Drill|NETHERITE_PICKAXE|DRILL|EPIC|drills|A royal drill that sends controlled pulses through stone.|MINING_SPEED=280;MINING_FORTUNE=55;BREAKING_POWER=5|stone_pulse|mining|26
            astral_drill|&dAstral Drill|NETHERITE_PICKAXE|DRILL|LEGENDARY|drills|An astral drill able to reveal deposits through solid rock.|MINING_SPEED=420;MINING_FORTUNE=95;TREASURE_CHANCE=12;BREAKING_POWER=7|prospect|mining|40
            dirtbreaker_shovel|&7Dirtbreaker Shovel|IRON_SHOVEL|SHOVEL|COMMON|shovels|A dependable shovel for beginning excavators.|EXCAVATION_FORTUNE=5;SPEED=2|none|excavation|2
            sandstorm_shovel|&eSandstorm Shovel|GOLDEN_SHOVEL|SHOVEL|UNCOMMON|shovels|A light shovel that clears soft ground in a sweep.|EXCAVATION_FORTUNE=15;TREASURE_CHANCE=3;SPEED=4|excavate|excavation|8
            claymore_shovel|&bClaymore Shovel|DIAMOND_SHOVEL|SHOVEL|RARE|shovels|A hardened shovel sensitive to buried valuables.|EXCAVATION_FORTUNE=30;TREASURE_CHANCE=5|treasure_sense|excavation|14
            gravekeeper_shovel|&8Gravekeeper Shovel|NETHERITE_SHOVEL|SHOVEL|EPIC|shovels|A gravekeeper's tool that can shake hostile ground.|EXCAVATION_FORTUNE=55;TREASURE_CHANCE=8;DAMAGE=40|earthquake|excavation|24
            void_digger_shovel|&5Void Digger Shovel|NETHERITE_SHOVEL|SHOVEL|LEGENDARY|shovels|A swift void tool that ruptures the earth around its wielder.|EXCAVATION_FORTUNE=90;TREASURE_CHANCE=15;SPEED=10|earthquake|excavation|36
            harvest_hoe|&aHarvest Hoe|IRON_HOE|FARMING_HOE|COMMON|hoes|A reliable hoe for a farmer's first fields.|FARMING_FORTUNE=8|none|farming|2
            wheatfield_hoe|&eWheatfield Hoe|GOLDEN_HOE|FARMING_HOE|UNCOMMON|hoes|A quick hoe made to sweep mature wheatfields.|FARMING_FORTUNE=18;SPEED=3|harvest_sweep|farming|7
            pumpkin_king_hoe|&6Pumpkin King Hoe|DIAMOND_HOE|FARMING_HOE|RARE|hoes|A decorated harvesting tool favored by crop royalty.|FARMING_FORTUNE=35;TREASURE_CHANCE=4|harvest_sweep|farming|14
            sunbloom_hoe|&eSunbloom Hoe|DIAMOND_HOE|FARMING_HOE|EPIC|hoes|A radiant hoe that encourages nearby crops to grow.|FARMING_FORTUNE=60;SPEED=8|growth_aura|farming|23
            verdant_reaper_hoe|&2Verdant Reaper Hoe|NETHERITE_HOE|FARMING_HOE|LEGENDARY|hoes|A legendary reaper carrying the vitality of ancient fields.|FARMING_FORTUNE=100;TREASURE_CHANCE=10;SPEED=12|growth_aura|farming|35
            oak_cutter_axe|&2Oak Cutter Axe|IRON_AXE|FORAGING_AXE|COMMON|axes|A sturdy axe made for young oak groves.|FORAGING_FORTUNE=8|none|foraging|2
            birchwood_axe|&fBirchwood Axe|IRON_AXE|FORAGING_AXE|UNCOMMON|axes|A balanced axe capable of cleaving connected timber.|FORAGING_FORTUNE=18;SPEED=3|timber_cleave|foraging|7
            jungle_cleaver_axe|&aJungle Cleaver Axe|DIAMOND_AXE|FORAGING_AXE|RARE|axes|A broad axe designed for dense jungle trunks.|FORAGING_FORTUNE=35;SPEED=5|timber_cleave|foraging|14
            elderwood_axe|&6Elderwood Axe|DIAMOND_AXE|FORAGING_AXE|EPIC|axes|An elderwood axe that scatters dense foliage.|FORAGING_FORTUNE=60;TREASURE_CHANCE=5;SPEED=8|leaf_storm|foraging|24
            worldroot_axe|&2Worldroot Axe|NETHERITE_AXE|FORAGING_AXE|LEGENDARY|axes|A worldroot blade that binds hostile creatures in place.|FORAGING_FORTUNE=100;TREASURE_CHANCE=12;SPEED=12|root_snare|foraging|36
            excavators_mattock|&6Excavator's Mattock|DIAMOND_SHOVEL|SHOVEL|RARE|shovels|A versatile mattock made for searching buried ground.|EXCAVATION_FORTUNE=40;MINING_SPEED=50;TREASURE_CHANCE=5|treasure_sense|excavation|18
            miners_mattock|&7Miner's Mattock|DIAMOND_PICKAXE|PICKAXE|RARE|pickaxes|A miner's mattock that clears stone with a focused pulse.|MINING_SPEED=170;MINING_FORTUNE=35;EXCAVATION_FORTUNE=15;BREAKING_POWER=4|stone_pulse|mining|18
            farmers_scythe|&aFarmer's Scythe|NETHERITE_HOE|FARMING_HOE|EPIC|hoes|An expert scythe made for sweeping mature fields.|FARMING_FORTUNE=75;SPEED=10|harvest_sweep|farming|28
            lumberjack_saw|&6Lumberjack Saw|NETHERITE_AXE|FORAGING_AXE|EPIC|axes|A powerful saw that rapidly cleaves connected logs.|FORAGING_FORTUNE=75;SPEED=9|timber_cleave|foraging|28
            treasure_spade|&eTreasure Spade|GOLDEN_SHOVEL|SHOVEL|EPIC|shovels|A gilded spade drawn toward buried treasure.|EXCAVATION_FORTUNE=65;TREASURE_CHANCE=15|treasure_sense|excavation|26
            geode_breaker|&dGeode Breaker|NETHERITE_PICKAXE|PICKAXE|EPIC|pickaxes|A reinforced pickaxe that bursts crystal deposits apart.|MINING_SPEED=300;MINING_FORTUNE=70;TREASURE_CHANCE=8;BREAKING_POWER=6|vein_burst|mining|30
            mythic_alloy_tool|&dMythic Alloy Tool|NETHERITE_PICKAXE|MULTI_TOOL|MYTHIC|multi_tools|A peerless alloy tool adaptable to every gathering craft.|MINING_SPEED=450;MINING_FORTUNE=100;FARMING_FORTUNE=70;FORAGING_FORTUNE=70;EXCAVATION_FORTUNE=70;TREASURE_CHANCE=15;SPEED=15;BREAKING_POWER=7|prospect|mining|45
            """;

    private static void applyRequestedToolItemDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        for (String line : REQUESTED_TOOL_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 11) continue;
            ConfigurationSection item = ensureItem(items, normalize(parts[0]));
            item.set("name", parts[1]);
            item.set("material", parts[2]);
            item.set("type", parts[3]);
            item.set("rarity", parts[4]);
            item.set("category", parts[5]);
            item.set("lore", wrapLore(parts[6]));
            item.set("mmo.type", parts[3].equals("FARMING_HOE") ? "FARMING_EQUIPMENT" : "TOOL");
            item.set("mmo.random-roll", false);
            item.set("mmo.stats", null);
            applyStatRanges(item, parts[7]);
            item.set("mmo.requirements." + normalize(parts[9]), parseInt(parts[10], 0));
            item.set("ability.type", parts[8]);
            item.set("ability.mana-cost", 0);
            item.set("ability.cooldown-seconds", toolCooldown(parts[8]));
            item.set("unbreakable", true);
            item.set("max-stack-size", 1);
        }
    }

    private static int toolCooldown(String ability) {
        return switch (normalize(ability)) {
            case "prospect", "harvest_sweep" -> 20;
            case "vein_burst", "leaf_storm" -> 30;
            case "stone_pulse" -> 15;
            case "treasure_sense", "growth_aura" -> 45;
            case "earthquake", "timber_cleave" -> 25;
            case "excavate" -> 12;
            case "root_snare" -> 35;
            default -> 0;
        };
    }

    private static final String REQUESTED_COMPANIONS = """
            tiny_slime_companion|&aTiny Slime|SLIME|SLIME_BALL|COMMON|cute|small
            baby_zombie_companion|&2Baby Zombie|ZOMBIE|ROTTEN_FLESH|COMMON|monster|baby
            mini_skeleton_companion|&fMini Skeleton|SKELETON|BONE|COMMON|monster|small
            forest_fox_companion|&6Forest Fox|FOX|SWEET_BERRIES|UNCOMMON|animal|red
            snow_fox_companion|&bSnow Fox|FOX|SNOWBALL|UNCOMMON|animal|snow
            tiny_wolf_companion|&7Tiny Wolf|WOLF|BONE|UNCOMMON|animal|small
            golden_chicken_companion|&6Golden Chicken|CHICKEN|GOLD_NUGGET|UNCOMMON|farm|small
            mushroom_cow_companion|&cMushroom Cow|MOOSHROOM|RED_MUSHROOM|RARE|farm|small
            crystal_sheep_companion|&dCrystal Sheep|SHEEP|AMETHYST_SHARD|RARE|magical|pink
            emerald_turtle_companion|&aEmerald Turtle|TURTLE|SCUTE|RARE|animal|small
            baby_panda_companion|&fBaby Panda|PANDA|BAMBOO|RARE|animal|baby
            jungle_parrot_companion|&aJungle Parrot|PARROT|FEATHER|RARE|flying|green
            blue_parrot_companion|&9Blue Parrot|PARROT|LAPIS_LAZULI|RARE|flying|blue
            bee_companion|&eBee|BEE|HONEYCOMB|UNCOMMON|flying|small
            glow_squid_companion|&bGlow Squid|GLOW_SQUID|GLOW_INK_SAC|EPIC|aquatic|small
            axolotl_companion|&dAxolotl|AXOLOTL|TROPICAL_FISH|RARE|aquatic|lucy
            blue_axolotl_companion|&bBlue Axolotl|AXOLOTL|HEART_OF_THE_SEA|LEGENDARY|aquatic|blue
            mini_blaze_companion|&6Mini Blaze|BLAZE|BLAZE_POWDER|EPIC|nether|small
            baby_piglin_companion|&6Baby Piglin|PIGLIN|GOLD_INGOT|EPIC|nether|baby
            tiny_hoglin_companion|&cTiny Hoglin|HOGLIN|CRIMSON_FUNGUS|EPIC|nether|baby
            endermite_companion|&5Endermite|ENDERMITE|ENDER_PEARL|RARE|end|small
            mini_enderman_companion|&5Mini Enderman|ENDERMAN|ENDER_EYE|EPIC|end|small
            shulker_companion|&dPocket Shulker|SHULKER|SHULKER_SHELL|EPIC|end|small
            phantom_companion|&9Tiny Phantom|PHANTOM|PHANTOM_MEMBRANE|EPIC|flying|small
            allay_companion|&bAllay|ALLAY|AMETHYST_SHARD|RARE|magical|normal
            vex_companion|&8Tiny Vex|VEX|IRON_SWORD|EPIC|magical|small
            snow_golem_companion|&fSnow Golem|SNOW_GOLEM|CARVED_PUMPKIN|RARE|utility|small
            iron_golem_companion|&7Tiny Iron Golem|IRON_GOLEM|IRON_BLOCK|LEGENDARY|utility|small
            warden_sprite_companion|&3Warden Sprite|WARDEN|ECHO_SHARD|MYTHIC|deep_dark|small
            dragon_whelp_companion|&5Dragon Whelp|PHANTOM|DRAGON_BREATH|MYTHIC|end|small
            """;

    private static void applyRequestedCompanionDefaults(YamlConfiguration config) {
        ConfigurationSection types = config.getConfigurationSection("companions.types");
        if (types == null) types = config.createSection("companions.types");
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        int slot = 100;
        for (String line : REQUESTED_COMPANIONS.strip().split("\\R")) {
            String[] p = line.trim().split("\\|", -1);
            if (p.length < 7) continue;
            String id = normalize(p[0]);
            ConfigurationSection companion = ensureItem(types, id);
            companion.set("slot", slot++);
            companion.set("entity", p[2]);
            companion.set("icon", p[3]);
            companion.set("name", p[1]);
            companion.set("rarity", p[4]);
            companion.set("category", p[5]);
            companion.set("variant", p[6]);
            companion.set("cosmetic-only", true);
            companion.set("max-level", 1);
            companion.set("base-health", 20.0D);
            companion.set("base-defense", 0.0D);
            companion.set("base-damage", 0.0D);

            ConfigurationSection item = ensureItem(items, id);
            item.set("name", p[1]);
            item.set("material", p[3]);
            item.set("type", "COMPANION");
            item.set("rarity", p[4]);
            item.set("category", "companions");
            item.set("companion.id", id);
            item.set("companion.rarity", p[4]);
            item.set("companion.category", p[5]);
            item.set("lore", List.of("&7A cosmetic companion that follows you.", "&7Category: &e" + prettyFishingName(p[5]),
                    "&7Rarity: " + rarityColor(p[4]) + p[4], "", "&eRight Click to summon!"));
            item.set("max-stack-size", 1);
        }
    }

    private static void applyGearItemRows(YamlConfiguration config, String rows) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        for (String line : rows.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 11) continue;
            String id = normalize(parts[0]);
            ConfigurationSection item = ensureItem(items, id);
            item.set("name", parts[1]);
            item.set("material", parts[2]);
            item.set("type", parts[3]);
            item.set("rarity", parts[4]);
            item.set("category", parts[5]);
            item.set("lore", wrapLore(parts[6]));
            item.set("ability", null);
            item.set("ability.type", "none");
            item.set("ability.mana-cost", 0);
            item.set("consumable.enabled", false);
            item.set("mmo", null);
            item.set("mmo.type", parts[3]);
            item.set("mmo.random-roll", false);
            applyStatRanges(item, parts[7]);
            item.set("mmo.requirements." + normalize(parts[9]), parseInt(parts[10], 0));
            item.set("unbreakable", true);
            item.set("max-stack-size", 1);
        }
    }

    private static final String REQUESTED_COMBAT_GEAR_ITEMS = """
            crystal_guard_helmet|&dCrystal Guard Helmet|DIAMOND_HELMET|HELMET|RARE|armor|A crystalline helmet carrying a reserve of arcane energy.|HEALTH=95;DEFENSE=50;MANA=25||combat|12
            crystal_guard_chestplate|&dCrystal Guard Chestplate|DIAMOND_CHESTPLATE|CHESTPLATE|RARE|armor|A crystalline chestplate carrying a reserve of arcane energy.|HEALTH=160;DEFENSE=85;MANA=40||combat|12
            crystal_guard_leggings|&dCrystal Guard Leggings|DIAMOND_LEGGINGS|LEGGINGS|RARE|armor|Crystalline leggings carrying a reserve of arcane energy.|HEALTH=135;DEFENSE=70;MANA=35||combat|12
            crystal_guard_boots|&dCrystal Guard Boots|DIAMOND_BOOTS|BOOTS|RARE|armor|Crystalline boots made for mobile defenders.|HEALTH=80;DEFENSE=40;SPEED=6||combat|12
            royal_knight_helmet|&6Royal Knight Helmet|GOLDEN_HELMET|HELMET|EPIC|armor|An ornate helmet worn by the royal guard.|HEALTH=130;DEFENSE=60;STRENGTH=12||combat|20
            royal_knight_chestplate|&6Royal Knight Chestplate|GOLDEN_CHESTPLATE|CHESTPLATE|EPIC|armor|An ornate chestplate worn by the royal guard.|HEALTH=230;DEFENSE=105;STRENGTH=25||combat|20
            royal_knight_leggings|&6Royal Knight Leggings|GOLDEN_LEGGINGS|LEGGINGS|EPIC|armor|Ornate leggings worn by the royal guard.|HEALTH=185;DEFENSE=85;STRENGTH=18||combat|20
            royal_knight_boots|&6Royal Knight Boots|GOLDEN_BOOTS|BOOTS|EPIC|armor|Ornate boots made for decisive royal advances.|HEALTH=110;DEFENSE=50;SPEED=7||combat|20
            voidwalker_mask|&5Voidwalker Mask|NETHERITE_HELMET|HELMET|LEGENDARY|armor|A dark mask that sharpens its wearer's sight beyond the void.|HEALTH=150;DEFENSE=70;CRIT_CHANCE=6;CRIT_DAMAGE=25||combat|30
            voidwalker_robes|&5Voidwalker Robes|NETHERITE_CHESTPLATE|CHESTPLATE|LEGENDARY|armor|Heavy void-touched robes holding immense magical power.|HEALTH=260;DEFENSE=115;CRIT_DAMAGE=45;MANA=80||combat|30
            voidwalker_leggings|&5Voidwalker Leggings|NETHERITE_LEGGINGS|LEGGINGS|LEGENDARY|armor|Void-touched leggings that bend around rapid movement.|HEALTH=210;DEFENSE=95;CRIT_DAMAGE=35;SPEED=8||combat|30
            voidwalker_boots|&5Voidwalker Boots|NETHERITE_BOOTS|BOOTS|LEGENDARY|armor|Void-touched boots that cross ground unnaturally quickly.|HEALTH=130;DEFENSE=60;SPEED=15||combat|30
            silver_fang_sword|&fSilver Fang Sword|IRON_SWORD|SWORD|UNCOMMON|swords|A dependable silver-edged sword for developing warriors.|DAMAGE=55;STRENGTH=20||combat|5
            crystal_edge|&dCrystal Edge|DIAMOND_SWORD|SWORD|RARE|swords|A crystalline blade with a precisely honed edge.|DAMAGE=105;STRENGTH=35;CRIT_DAMAGE=25||combat|14
            infernal_saber|&cInfernal Saber|NETHERITE_SWORD|SWORD|EPIC|swords|A fast saber forged in relentless infernal heat.|DAMAGE=150;STRENGTH=70;CRIT_DAMAGE=35||combat|24
            astral_greatblade|&dAstral Greatblade|NETHERITE_SWORD|SWORD|LEGENDARY|swords|A colossal astral blade that rewards measured attacks.|DAMAGE=220;STRENGTH=100;CRIT_CHANCE=8;CRIT_DAMAGE=70||combat|35
            rusted_dagger|&7Rusted Dagger|STONE_SWORD|DAGGER|COMMON|daggers|A worn but exceptionally quick beginner's dagger.|DAMAGE=25;CRIT_CHANCE=4||combat|1
            silverleaf_dagger|&aSilverleaf Dagger|IRON_SWORD|DAGGER|UNCOMMON|daggers|A light leaf-shaped dagger made for agile fighters.|DAMAGE=50;CRIT_CHANCE=8;SPEED=4||combat|7
            nightbite_dagger|&8Nightbite Dagger|DIAMOND_SWORD|DAGGER|RARE|daggers|A dark dagger designed for sudden critical strikes.|DAMAGE=85;CRIT_CHANCE=12;CRIT_DAMAGE=30||combat|16
            voidfang_dagger|&5Voidfang Dagger|NETHERITE_SWORD|DAGGER|EPIC|daggers|A void-touched fang made for elite assassins.|DAMAGE=125;CRIT_CHANCE=18;CRIT_DAMAGE=55;SPEED=10||combat|26
            oakwind_katana|&2Oakwind Katana|IRON_SWORD|KATANA|UNCOMMON|katanas|A flexible katana that moves like a forest breeze.|DAMAGE=60;STRENGTH=15;SPEED=6||combat|8
            frostwind_katana|&bFrostwind Katana|DIAMOND_SWORD|KATANA|RARE|katanas|A frozen katana that cuts with winter wind.|DAMAGE=105;STRENGTH=35;SPEED=10||combat|17
            emberstorm_katana|&6Emberstorm Katana|NETHERITE_SWORD|KATANA|EPIC|katanas|A heated katana surrounded by a violent emberstorm.|DAMAGE=155;STRENGTH=60;CRIT_DAMAGE=45;SPEED=12||combat|27
            starfall_katana|&dStarfall Katana|NETHERITE_SWORD|KATANA|LEGENDARY|katanas|A legendary katana that falls upon enemies like a star.|DAMAGE=200;STRENGTH=80;CRIT_CHANCE=10;CRIT_DAMAGE=65;SPEED=15||combat|36
            training_bow|&7Training Bow|BOW|BOW|COMMON|bows|A simple bow for learning ranged combat.|DAMAGE=30||combat|1
            forest_hunter_bow|&2Forest Hunter Bow|BOW|BOW|UNCOMMON|bows|A practical bow favored by woodland hunters.|DAMAGE=65;CRIT_CHANCE=5||combat|8
            crystal_longbow|&dCrystal Longbow|BOW|BOW|RARE|bows|A crystal-reinforced longbow with exceptional range.|DAMAGE=115;STRENGTH=30;CRIT_DAMAGE=30||combat|18
            infernal_shortbow|&cInfernal Shortbow|BOW|BOW|EPIC|bows|A compact infernal bow that launches searing arrows.|DAMAGE=155;STRENGTH=55;CRIT_CHANCE=8;CRIT_DAMAGE=45||combat|28
            voidpiercer_bow|&5Voidpiercer Bow|BOW|BOW|LEGENDARY|bows|A legendary bow whose arrows pierce the edge of the void.|DAMAGE=210;STRENGTH=80;CRIT_CHANCE=10;CRIT_DAMAGE=70||combat|38
            royal_crossbow|&6Royal Crossbow|CROSSBOW|BOW|EPIC|bows|A decorated royal crossbow with immense stopping power.|DAMAGE=145;STRENGTH=50;CRIT_DAMAGE=40||combat|24
            """;

    private static void applyRequestedCombatGearItemDefaults(YamlConfiguration config) {
        applyGearItemRows(config, REQUESTED_COMBAT_GEAR_ITEMS);
    }

    private static final String REQUESTED_DUNGEON_GEAR_ITEMS = """
            crypt_guard_helmet|&8Crypt Guard Helmet|IRON_HELMET|HELMET|RARE|dungeon_armor|A battered guard helmet recovered from the crypt.|HEALTH=120;DEFENSE=60||combat|15
            crypt_guard_chestplate|&8Crypt Guard Chestplate|IRON_CHESTPLATE|CHESTPLATE|RARE|dungeon_armor|A battered guard chestplate recovered from the crypt.|HEALTH=210;DEFENSE=105||combat|15
            crypt_guard_leggings|&8Crypt Guard Leggings|IRON_LEGGINGS|LEGGINGS|RARE|dungeon_armor|Battered guard leggings recovered from the crypt.|HEALTH=170;DEFENSE=85||combat|15
            crypt_guard_boots|&8Crypt Guard Boots|IRON_BOOTS|BOOTS|RARE|dungeon_armor|Battered guard boots that retain surprising mobility.|HEALTH=100;DEFENSE=45;SPEED=4||combat|15
            frost_revenant_helmet|&bFrost Revenant Helmet|DIAMOND_HELMET|HELMET|EPIC|dungeon_armor|A frozen helmet carrying revenant magic.|HEALTH=160;DEFENSE=75;MANA=40||combat|22
            frost_revenant_chestplate|&bFrost Revenant Chestplate|DIAMOND_CHESTPLATE|CHESTPLATE|EPIC|dungeon_armor|A frozen chestplate carrying revenant magic.|HEALTH=280;DEFENSE=130;MANA=75||combat|22
            frost_revenant_leggings|&bFrost Revenant Leggings|DIAMOND_LEGGINGS|LEGGINGS|EPIC|dungeon_armor|Frozen leggings carrying revenant magic.|HEALTH=230;DEFENSE=105;MANA=55||combat|22
            frost_revenant_boots|&bFrost Revenant Boots|DIAMOND_BOOTS|BOOTS|EPIC|dungeon_armor|Frozen revenant boots made for relentless pursuit.|HEALTH=130;DEFENSE=60;SPEED=8||combat|22
            shadow_catacomb_mask|&5Shadow Catacomb Mask|NETHERITE_HELMET|HELMET|LEGENDARY|dungeon_armor|A forbidden mask recovered from the deepest catacombs.|HEALTH=190;DEFENSE=85;CRIT_CHANCE=6;CRIT_DAMAGE=30||combat|32
            shadow_catacomb_robes|&5Shadow Catacomb Robes|NETHERITE_CHESTPLATE|CHESTPLATE|LEGENDARY|dungeon_armor|Void-soaked robes from the deepest catacombs.|HEALTH=330;DEFENSE=150;CRIT_DAMAGE=55;MANA=100||combat|32
            shadow_catacomb_leggings|&5Shadow Catacomb Leggings|NETHERITE_LEGGINGS|LEGGINGS|LEGENDARY|dungeon_armor|Void-soaked leggings from the deepest catacombs.|HEALTH=270;DEFENSE=125;CRIT_DAMAGE=40;SPEED=10||combat|32
            shadow_catacomb_boots|&5Shadow Catacomb Boots|NETHERITE_BOOTS|BOOTS|LEGENDARY|dungeon_armor|Void-soaked boots that glide over catacomb stone.|HEALTH=160;DEFENSE=70;SPEED=18||combat|32
            crypt_cleaver|&8Crypt Cleaver|IRON_SWORD|SWORD|RARE|dungeon_weapons|A heavy crypt blade made for breaking undead ranks.|DAMAGE=115;STRENGTH=45||combat|16
            revenant_frostblade|&bRevenant Frostblade|DIAMOND_SWORD|SWORD|EPIC|dungeon_weapons|A frozen revenant blade with a deadly edge.|DAMAGE=155;STRENGTH=65;CRIT_DAMAGE=35||combat|23
            catacomb_executioner|&5Catacomb Executioner|NETHERITE_SWORD|SWORD|LEGENDARY|dungeon_weapons|A deliberate executioner's blade from the lowest crypt.|DAMAGE=240;STRENGTH=115;CRIT_CHANCE=8;CRIT_DAMAGE=80||combat|34
            bonepiercer_dagger|&fBonepiercer Dagger|IRON_SWORD|DAGGER|RARE|dungeon_weapons|A narrow dagger designed to split ancient bone.|DAMAGE=90;CRIT_CHANCE=12;CRIT_DAMAGE=25||combat|17
            shadowstep_dagger|&5Shadowstep Dagger|NETHERITE_SWORD|DAGGER|EPIC|dungeon_weapons|A catacomb dagger that seems to skip through shadows.|DAMAGE=140;CRIT_CHANCE=18;CRIT_DAMAGE=60;SPEED=12||combat|27
            crypt_hunter_bow|&8Crypt Hunter Bow|BOW|BOW|RARE|dungeon_bows|A rugged bow used by hunters of the undead.|DAMAGE=120;STRENGTH=35;CRIT_CHANCE=5||combat|18
            frost_revenant_bow|&bFrost Revenant Bow|BOW|BOW|EPIC|dungeon_bows|A frozen bow strung with revenant sinew.|DAMAGE=165;STRENGTH=55;CRIT_DAMAGE=45||combat|25
            shadow_catacomb_bow|&5Shadow Catacomb Bow|BOW|BOW|LEGENDARY|dungeon_bows|A legendary bow recovered from a sealed catacomb vault.|DAMAGE=230;STRENGTH=90;CRIT_CHANCE=10;CRIT_DAMAGE=80||combat|35
            crypt_mage_wand|&8Crypt Mage Wand|BLAZE_ROD|WAND|RARE|dungeon_wands_staffs|A brittle focus used by the crypt's forgotten mages.|DAMAGE=85;MANA=150;ABILITY_DAMAGE=25||combat|16
            frostcaller_wand|&bFrostcaller Wand|BLAZE_ROD|WAND|EPIC|dungeon_wands_staffs|A revenant focus that calls power from frozen graves.|DAMAGE=120;MANA=240;ABILITY_DAMAGE=45||combat|24
            void_catacomb_wand|&5Void Catacomb Wand|BLAZE_ROD|WAND|LEGENDARY|dungeon_wands_staffs|A void focus sealed within the deepest catacomb.|DAMAGE=165;MANA=360;ABILITY_DAMAGE=70;INTELLIGENCE=100||combat|34
            necromancer_staff|&8Necromancer Staff|END_ROD|STAFF|EPIC|dungeon_wands_staffs|A forbidden staff humming with necromantic knowledge.|DAMAGE=140;MANA=300;ABILITY_DAMAGE=55;INTELLIGENCE=80||combat|28
            frost_revenant_staff|&bFrost Revenant Staff|END_ROD|STAFF|LEGENDARY|dungeon_wands_staffs|A legendary staff holding the will of a frost revenant.|DAMAGE=190;MANA=420;ABILITY_DAMAGE=85;INTELLIGENCE=120||combat|36
            """;

    private static void applyRequestedDungeonItemDefaults(YamlConfiguration config) {
        applyGearItemRows(config, REQUESTED_DUNGEON_GEAR_ITEMS);
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) return;
        for (String line : REQUESTED_DUNGEON_GEAR_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 11) continue;
            String id = normalize(parts[0]);
            items.set(id + ".dungeon.loot-eligible", true);
            items.set(id + ".dungeon.source", "dungeon_mobs_recipes_bosses");
            items.set(id + ".dungeon.minimum-drop-rank", parts[4].equals("RARE") ? "elite" : "boss");
        }
        dungeonPotion(items, "dungeon_healing_potion", "&aDungeon Healing Potion", "UNCOMMON", 20,
                350, 0, Map.of(), "Restores 350 HP instantly.");
        dungeonPotion(items, "dungeon_mana_potion", "&bDungeon Mana Potion", "UNCOMMON", 20,
                0, 150, Map.of(), "Restores 150 Mana instantly.");
        dungeonPotion(items, "revenant_strength_potion", "&cRevenant Strength Potion", "RARE", 60,
                0, 0, Map.of(MMOStatType.STRENGTH, new double[]{40, 45}), "Grants 40 Strength for 45 seconds.");
        dungeonPotion(items, "shadow_speed_potion", "&8Shadow Speed Potion", "RARE", 60,
                0, 0, Map.of(MMOStatType.SPEED, new double[]{35, 45}), "Grants 35 Speed for 45 seconds.");
        Map<MMOStatType, double[]> resistance = new LinkedHashMap<>();
        resistance.put(MMOStatType.DEFENSE, new double[]{100, 60});
        resistance.put(MMOStatType.TRUE_DEFENSE, new double[]{25, 60});
        dungeonPotion(items, "catacomb_resistance_potion", "&5Catacomb Resistance Potion", "EPIC", 90,
                0, 0, resistance, "Grants 100 Defense and 25 True Defense for 60 seconds.");
    }

    private static void dungeonPotion(ConfigurationSection items, String id, String name, String rarity, int cooldown,
                                      double heal, double mana, Map<MMOStatType, double[]> buffs, String description) {
        ConfigurationSection item = ensureItem(items, id);
        item.set("name", name);
        item.set("material", "POTION");
        item.set("type", "POTION");
        item.set("rarity", rarity);
        item.set("category", "dungeon_potions");
        item.set("lore", wrapLore(description));
        item.set("mmo", null);
        item.set("mmo.type", "POTION");
        item.set("mmo.random-roll", false);
        item.set("ability", null);
        item.set("ability.type", "none");
        item.set("ability.mana-cost", 0);
        item.set("consumable.enabled", true);
        item.set("consumable.heal", heal);
        item.set("consumable.mana", mana);
        item.set("consumable.cooldown-seconds", cooldown);
        item.set("consumable.message", "&aYou consumed " + name + "&a.");
        item.set("unbreakable", false);
        item.set("max-stack-size", 1);
        item.set("dungeon.loot-eligible", true);
        item.set("dungeon.source", "dungeon_mobs_chests_recipes");
        for (Map.Entry<MMOStatType, double[]> entry : buffs.entrySet()) {
            String path = "consumable.stat-buffs." + entry.getKey().name();
            item.set(path + ".amount", entry.getValue()[0]);
            item.set(path + ".duration-seconds", (long) entry.getValue()[1]);
        }
    }

    private static void applyExtraMmoItemDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");
        for (String line : EXTRA_MMO_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 10) continue;
            ConfigurationSection item = items.getConfigurationSection(parts[0]);
            if (item == null) item = items.createSection(parts[0]);
            item.set("name", rarityColor(parts[4]) + parts[1]);
            item.set("material", parts[2]);
            item.set("category", parts[3].toLowerCase(Locale.ROOT));
            item.set("rarity", parts[4]);
            item.set("lore", wrapLore(parts[8]));
            item.set("mmo.type", parts[3]);
            item.set("mmo.random-roll", true);
            if (isMmoGearType(parts[3])) item.set("unbreakable", true);
            applyStatRanges(item, parts[9]);
            boolean consumable = parts[3].equalsIgnoreCase("FOOD") || parts[3].equalsIgnoreCase("POTION") || parts[3].equalsIgnoreCase("CONSUMABLE");
            if (consumable) {
                item.set("consumable.enabled", true);
                item.set("consumable.message", "&aYou consumed &f" + parts[1] + "&a.");
                item.set("ability.type", "none");
                item.set("ability.mana-cost", 0);
            } else if (!parts[5].isBlank()) {
                item.set("ability.type", parts[5]);
                item.set("ability.name", parts[6].isBlank() ? parts[5] : parts[6]);
                item.set("ability.mana-cost", parseInt(parts[7], 0));
            } else {
                item.set("ability.type", "none");
                item.set("ability.mana-cost", 0);
            }
            applyFarmingAbilityDefaults(parts[0], item);
        }
    }

    private static void applyFarmingAbilityDefaults(String id, ConfigurationSection item) {
        switch (normalize(id)) {
            case "sprinkler" -> {
                item.set("ability.radius", 3);
                item.set("ability.growth-amount", 1);
                item.set("ability.max-crops", 8);
                item.set("ability.cooldown-seconds", 20);
            }
            case "advanced_sprinkler" -> {
                item.set("ability.radius", 5);
                item.set("ability.growth-amount", 2);
                item.set("ability.max-crops", 16);
                item.set("ability.cooldown-seconds", 15);
            }
            case "enchanted_sprinkler" -> {
                item.set("ability.radius", 7);
                item.set("ability.growth-amount", 2);
                item.set("ability.max-crops", 28);
                item.set("ability.cooldown-seconds", 10);
            }
            case "royal_harvest_hoe" -> {
                item.set("ability.radius", 5);
                item.set("ability.growth-amount", 1);
                item.set("ability.max-crops", 18);
                item.set("ability.cooldown-seconds", 20);
            }
            default -> {
            }
        }
    }

    private static void applyStatRanges(ConfigurationSection item, String stats) {
        for (String entry : stats.split(";")) {
            String[] pair = entry.split("=", 2);
            if (pair.length != 2) continue;
            String path = "mmo.stats." + pair[0].trim().toUpperCase(Locale.ROOT).replace(" ", "_");
            String value = pair[1].trim();
            try {
                item.set(path, value.contains("-") || value.contains("..") ? value : Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
                item.set(path, value);
            }
        }
    }

    private static List<String> wrapLore(String text) {
        if (text == null || text.isBlank()) return List.of();
        String[] words = text.split("\\s+");
        List<String> lines = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (!current.isEmpty() && current.length() + word.length() + 1 > 34) {
                lines.add("&7" + current);
                current.setLength(0);
            }
            if (!current.isEmpty()) current.append(' ');
            current.append(word);
        }
        if (!current.isEmpty()) lines.add("&7" + current);
        return lines;
    }

    private static List<String> splitLore(String text) {
        if (text == null || text.isBlank()) return List.of();
        return java.util.Arrays.stream(text.split("~"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private static String rarityColor(String rarity) {
        return switch (rarity == null ? "" : rarity.toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> "&a";
            case "RARE" -> "&9";
            case "EPIC" -> "&5";
            case "LEGENDARY" -> "&6";
            case "MYTHIC" -> "&d";
            case "SPECIAL" -> "&c";
            default -> "&f";
        };
    }

    private static boolean isMmoGearType(String type) {
        return switch (type == null ? "" : type.toUpperCase(Locale.ROOT)) {
            case "WEAPON", "SWORD", "KATANA", "DAGGER", "BOW", "STAFF", "WAND", "ARMOR", "TOOL", "SHIELD", "FISHING_ROD", "FARMING_EQUIPMENT" -> true;
            default -> false;
        };
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private static void applyRpgFishingDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) items = config.createSection("custom-items.items");

        for (String line : FISHING_MATERIAL_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 5) continue;
            ConfigurationSection item = ensureItem(items, parts[0]);
            item.set("name", rarityColor(parts[3]) + parts[1]);
            item.set("material", parts[2]);
            item.set("category", "fishing");
            item.set("type", "MATERIAL");
            item.set("rarity", parts[3]);
            item.set("lore", List.of("&7A stackable fishing material.", "&7Used by fishing recipes and the guild."));
            item.set("mmo.type", "MATERIAL");
            item.set("mmo.random-roll", false);
            item.set("ability.type", "none");
            item.set("ability.mana-cost", 0);
            item.set("sell-price", parseInt(parts[4], 0));
        }

        for (String line : FISHING_BAIT_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 5) continue;
            ConfigurationSection item = ensureItem(items, parts[0]);
            item.set("name", rarityColor(parts[3]) + parts[1]);
            item.set("material", parts[2]);
            item.set("category", "fishing");
            item.set("type", "FISHING_BAIT");
            item.set("rarity", parts[3]);
            item.set("lore", wrapLore(parts[4]));
            item.set("mmo.type", "FISHING_BAIT");
            item.set("mmo.random-roll", false);
            item.set("consumable.enabled", false);
            item.set("ability.type", "none");
            item.set("ability.mana-cost", 0);
        }

        for (String line : FISHING_GEAR_ITEMS.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 8) continue;
            ConfigurationSection item = ensureItem(items, parts[0]);
            item.set("name", rarityColor(parts[4]) + parts[1]);
            item.set("material", parts[2]);
            item.set("category", parts[3].equalsIgnoreCase("ARMOR") ? "armor" : "fishing");
            item.set("type", parts[3]);
            item.set("rarity", parts[4]);
            item.set("lore", wrapLore(parts[7]));
            item.set("mmo.type", parts[3]);
            item.set("mmo.random-roll", false);
            item.set("mmo.requirements.fishing", parseInt(parts[5], 0));
            item.set("ability.type", "none");
            item.set("ability.mana-cost", 0);
            item.set("unbreakable", true);
            applyStatRanges(item, parts[6]);
        }

        config.set("collections.categories.fishing.name", "Fishing Collection");
        config.set("collections.categories.fishing.icon", "FISHING_ROD");
        for (String id : List.of("raw_river_fish", "silver_fish_scale", "golden_fish_scale", "deep_sea_fin",
                "pearl_fragment", "coral_fragment", "enchanted_kelp", "treasure_shell", "voidwater_scale", "stormscale_fin")) {
            String path = "collections.items." + id;
            config.set(path + ".name", prettyFishingName(id) + " Collection");
            config.set(path + ".category", "fishing");
            config.set(path + ".icon", items.getString(id + ".material", "FISHING_ROD"));
            config.set(path + ".materials", List.of());
        }
    }

    private static ConfigurationSection ensureItem(ConfigurationSection items, String id) {
        ConfigurationSection item = items.getConfigurationSection(id);
        return item == null ? items.createSection(id) : item;
    }

    private static String prettyFishingName(String id) {
        StringBuilder result = new StringBuilder();
        for (String part : normalize(id).split("_")) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private static void applyMmoSystemDefaults(YamlConfiguration config) {
        ConfigurationSection items = config.getConfigurationSection("custom-items.items");
        if (items == null) return;
        for (String id : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(id);
            if (item == null) continue;
            Material material = material(item.getString("material", "PAPER"));
            String category = item.getString("category", "");
            String name = item.getString("name", id);
            String lower = (id + " " + category + " " + name + " " + material.name()).toLowerCase(Locale.ROOT);
            String existingMmoType = item.getString("mmo.type", item.getString("type", "")).toUpperCase(Locale.ROOT);
            boolean configuredMelee = List.of("WEAPON", "SWORD", "KATANA", "DAGGER", "SPEAR", "AXE", "STAFF", "WAND").contains(existingMmoType);
            if (item.isConfigurationSection("mmo.stats")) continue;
            if (isArmor(material)) {
                item.set("mmo.type", "ARMOR");
                item.set("mmo.random-roll", true);
                item.set("unbreakable", true);
                armorStats(item, material, tier(lower));
            } else if (material.name().endsWith("_PICKAXE") || lower.contains("drill")) {
                item.set("mmo.type", "TOOL");
                item.set("mmo.random-roll", true);
                item.set("unbreakable", true);
                miningStats(item, material, tier(lower));
            } else if (material.name().endsWith("_HOE") || lower.contains("harvest") || lower.contains("replant")) {
                item.set("mmo.type", "FARMING_EQUIPMENT");
                item.set("mmo.random-roll", true);
                item.set("unbreakable", true);
                farmingStats(item, tier(lower));
            } else if (material == Material.FISHING_ROD || lower.contains("rod")) {
                item.set("mmo.type", "FISHING_ROD");
                item.set("mmo.random-roll", true);
                item.set("unbreakable", true);
                fishingStats(item, tier(lower));
            } else if (isWeapon(material)) {
                item.set("mmo.type", weaponType(material, lower));
                item.set("mmo.random-roll", true);
                item.set("unbreakable", true);
                combatStats(item, material, tier(lower));
            } else if (lower.contains("ring") || lower.contains("talisman") || lower.contains("artifact") || lower.contains("accessor")) {
                item.set("mmo.type", lower.contains("ring") ? "RING" : "TRINKET");
                item.set("mmo.random-roll", true);
                utilityStats(item, tier(lower));
            } else if (lower.contains("xp") || lower.contains("luck") || lower.contains("magic")) {
                item.set("mmo.type", "UTILITY");
                utilityStats(item, tier(lower));
            }
        }
    }

    private static void applyZoneBossBalanceDefaults(YamlConfiguration config) {
        config.set("zones.default-boss-health", 15_000.0D);
        config.set("zones.boss-health-per-level", 500.0D);
        config.set("zones.boss-health-scale", 1.5D);
        config.set("zones.elite-boss-health-scale", 2.5D);
        config.set("zones.final-boss-health-scale", 3.5D);
        config.set("zones.default-boss-defense", 50.0D);
        config.set("zones.boss-defense-per-level", 5.0D);
        config.set("zones.elite-boss-defense-multiplier", 1.25D);
        config.set("zones.final-boss-defense-multiplier", 1.5D);
        config.set("zones.boss-respawn-seconds", 120L);
        config.set("zones.boss-minimum-damage-percent", 10.0D);
        config.set("zones.boss-regen-damage-pause-seconds", 8.0D);
        config.set("zones.boss-regen-percent", 0.25D);
        config.set("zones.elite-boss-regen-percent", 0.5D);
        config.set("zones.final-boss-regen-percent", 0.75D);
    }

    private static void combatStats(ConfigurationSection item, Material material, int tier) {
        int base = switch (material) {
            case MACE, NETHERITE_SWORD, NETHERITE_AXE, TRIDENT -> 55;
            case DIAMOND_SWORD, DIAMOND_AXE -> 35;
            case IRON_SWORD, IRON_AXE -> 20;
            case BOW, CROSSBOW -> 25;
            default -> 15;
        };
        set(item, "DAMAGE", Math.max(1, base + tier * 18));
        set(item, "STRENGTH", 10 + tier * 12);
        set(item, "CRIT_CHANCE", 3 + tier * 2);
        set(item, "CRIT_DAMAGE", 10 + tier * 8);
        set(item, "MANA", 10 + tier * 8);
        if (tier >= 4) set(item, "FEROCITY", tier - 2);
        if (tier >= 5) set(item, "LIFESTEAL", 1 + tier);
    }

    private static void armorStats(ConfigurationSection item, Material material, int tier) {
        boolean chest = material.name().endsWith("_CHESTPLATE");
        boolean legs = material.name().endsWith("_LEGGINGS");
        int piece = chest ? 2 : legs ? 2 : 1;
        set(item, "HEALTH", piece * (15 + tier * 18));
        set(item, "MANA", piece * (8 + tier * 10));
        set(item, "DEFENSE", piece * (8 + tier * 12));
        if (tier >= 3) set(item, "TRUE_DEFENSE", tier);
        if (material.name().endsWith("_BOOTS")) set(item, "SPEED", 2 + tier);
        if (tier >= 4) set(item, "VITALITY", 2 + tier);
    }

    private static void miningStats(ConfigurationSection item, Material material, int tier) {
        int base = material.name().startsWith("NETHERITE") ? 220 : material.name().startsWith("DIAMOND") ? 150 : 80;
        set(item, "MINING_SPEED", base + tier * 90);
        set(item, "MINING_FORTUNE", 10 + tier * 12);
        set(item, "BREAKING_POWER", Math.min(10, 3 + tier));
    }

    private static void farmingStats(ConfigurationSection item, int tier) {
        set(item, "FARMING_FORTUNE", 8 + tier * 12);
        set(item, "REPLANT_CHANCE", Math.min(75, 8 + tier * 9));
    }

    private static void fishingStats(ConfigurationSection item, int tier) {
        set(item, "FISHING_SPEED", 5 + tier * 8);
        set(item, "SEA_CREATURE_CHANCE", 2 + tier * 2);
        set(item, "TREASURE_CHANCE", 3 + tier * 3);
    }

    private static void utilityStats(ConfigurationSection item, int tier) {
        set(item, "HEALTH", 5 + tier * 8);
        set(item, "MANA", 15 + tier * 12);
        set(item, "MAGIC_FIND", tier * 2);
        set(item, "PET_LUCK", tier);
        set(item, "XP_BOOST", tier * 2);
    }

    private static int tier(String text) {
        if (text.contains("inferno") || text.contains("shadow") || text.contains("storm") || text.contains("crystal") || text.contains("sunsteel")) return 5;
        if (text.contains("frost") || text.contains("tide") || text.contains("grove")) return 4;
        if (text.contains("dune") || text.contains("reef") || text.contains("tidecaller")) return 3;
        if (text.contains("angler") || text.contains("harvester") || text.contains("replanter")) return 2;
        return 1;
    }

    private static boolean isWeapon(Material material) {
        return material.name().endsWith("_SWORD") || material.name().endsWith("_AXE") || material == Material.BOW
                || material == Material.CROSSBOW || material == Material.TRIDENT || material == Material.MACE;
    }

    private static String weaponType(Material material, String lower) {
        if (lower.contains("katana")) return "KATANA";
        if (lower.contains("dagger")) return "DAGGER";
        if (lower.contains("staff")) return "STAFF";
        if (lower.contains("wand")) return "WAND";
        if (material == Material.BOW || material == Material.CROSSBOW) return "BOW";
        if (material.name().endsWith("_SWORD")) return "SWORD";
        return "WEAPON";
    }

    private static boolean isArmor(Material material) {
        return material.name().endsWith("_HELMET") || material.name().endsWith("_CHESTPLATE")
                || material.name().endsWith("_LEGGINGS") || material.name().endsWith("_BOOTS");
    }

    private static Material material(String value) {
        try {
            return Material.valueOf(String.valueOf(value).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Material.PAPER;
        }
    }

    private static void set(ConfigurationSection item, String stat, double value) {
        item.set("mmo.stats." + stat, value);
    }
}
