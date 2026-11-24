package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Guarded Java-only definitions for the requested batch of 80 inert progression materials. */
public final class RequestedMaterialDefaults80 {
    private RequestedMaterialDefaults80() {}

    private static final String DEFINITIONS = """
rough_stone_core|&7Rough Stone Core|COBBLESTONE|MINING_MATERIAL|COMMON|&7A dense core chipped from rough stone.
dense_cobble_plate|&7Dense Cobble Plate|STONE_PRESSURE_PLATE|MINING_MATERIAL|UNCOMMON|&7A compressed plate of cobblestone.
deep_slate_shard|&8Deep Slate Shard|COBBLED_DEEPSLATE|MINING_MATERIAL|COMMON|&7A sharp shard from the deep caves.
polished_deepslate_core|&8Polished Deepslate Core|POLISHED_DEEPSLATE|MINING_MATERIAL|RARE|&7A polished core of hardened deepslate.
granite_chunk|&cGranite Chunk|GRANITE|MINING_MATERIAL|COMMON|&7A chunk of rough granite.
diorite_chunk|&fDiorite Chunk|DIORITE|MINING_MATERIAL|COMMON|&7A chunk of pale diorite.
andesite_chunk|&7Andesite Chunk|ANDESITE|MINING_MATERIAL|COMMON|&7A chunk of smooth andesite.
tuff_fragment|&8Tuff Fragment|TUFF|MINING_MATERIAL|COMMON|&7A dusty fragment of tuff.
calcite_dust|&fCalcite Dust|CALCITE|MINING_MATERIAL|UNCOMMON|&7Fine dust from white calcite.
dripstone_spike|&6Dripstone Spike|POINTED_DRIPSTONE|MINING_MATERIAL|UNCOMMON|&7A sharp spike from a dripstone cave.
coal_fiber|&8Coal Fiber|COAL|MINING_MATERIAL|COMMON|&7A rough fiber made from coal dust.
iron_rivet|&7Iron Rivet|IRON_NUGGET|MINING_MATERIAL|COMMON|&7A small rivet used in forged equipment.
golden_rivet|&6Golden Rivet|GOLD_NUGGET|MINING_MATERIAL|UNCOMMON|&7A shiny rivet used in rare equipment.
copper_gear|&6Copper Gear|COPPER_INGOT|MINING_MATERIAL|UNCOMMON|&7A small gear made from copper.
diamond_dust|&bDiamond Dust|DIAMOND|MINING_MATERIAL|RARE|&7Fine dust from a shattered diamond.
emerald_dust|&aEmerald Dust|EMERALD|MINING_MATERIAL|RARE|&7Fine dust from a shattered emerald.
lapis_thread|&9Lapis Thread|LAPIS_LAZULI|MAGIC_MATERIAL|UNCOMMON|&7A magical thread dyed with lapis.
redstone_spark|&cRedstone Spark|REDSTONE|MAGIC_MATERIAL|UNCOMMON|&7A tiny spark of unstable redstone power.
quartz_splinter|&fQuartz Splinter|QUARTZ|MAGIC_MATERIAL|UNCOMMON|&7A bright splinter of nether quartz.
amethyst_splinter|&dAmethyst Splinter|AMETHYST_SHARD|MAGIC_MATERIAL|RARE|&7A sharp splinter of magical amethyst.
oak_bark_strip|&6Oak Bark Strip|OAK_LOG|FORAGING_MATERIAL|COMMON|&7A strip of bark from an oak tree.
spruce_bark_strip|&2Spruce Bark Strip|SPRUCE_LOG|FORAGING_MATERIAL|COMMON|&7A strip of bark from a spruce tree.
birch_bark_strip|&fBirch Bark Strip|BIRCH_LOG|FORAGING_MATERIAL|COMMON|&7A strip of bark from a birch tree.
jungle_bark_strip|&aJungle Bark Strip|JUNGLE_LOG|FORAGING_MATERIAL|COMMON|&7A strip of bark from a jungle tree.
acacia_bark_strip|&6Acacia Bark Strip|ACACIA_LOG|FORAGING_MATERIAL|COMMON|&7A strip of bark from an acacia tree.
dark_oak_bark_strip|&8Dark Oak Bark Strip|DARK_OAK_LOG|FORAGING_MATERIAL|COMMON|&7A strip of bark from a dark oak tree.
mangrove_root_fiber|&cMangrove Root Fiber|MANGROVE_ROOTS|FORAGING_MATERIAL|UNCOMMON|&7A tough fiber pulled from mangrove roots.
cherry_blossom_leaf|&dCherry Blossom Leaf|CHERRY_LEAVES|FORAGING_MATERIAL|UNCOMMON|&7A soft leaf from a cherry blossom tree.
crimson_fungus_fiber|&cCrimson Fungus Fiber|CRIMSON_FUNGUS|FORAGING_MATERIAL|UNCOMMON|&7A strange fiber from crimson fungus.
warped_fungus_fiber|&3Warped Fungus Fiber|WARPED_FUNGUS|FORAGING_MATERIAL|UNCOMMON|&7A strange fiber from warped fungus.
leaf_compost|&aLeaf Compost|COMPOSTER|FORAGING_MATERIAL|COMMON|&7A pile of composted leaves.
sapling_resin|&eSapling Resin|OAK_SAPLING|FORAGING_MATERIAL|UNCOMMON|&7Sticky resin from a young sapling.
wheat_bundle|&eWheat Bundle|WHEAT|FARMING_MATERIAL|COMMON|&7A tied bundle of wheat.
carrot_bundle|&6Carrot Bundle|CARROT|FARMING_MATERIAL|COMMON|&7A bundle of fresh carrots.
potato_bundle|&ePotato Bundle|POTATO|FARMING_MATERIAL|COMMON|&7A sack of potatoes.
beetroot_bundle|&cBeetroot Bundle|BEETROOT|FARMING_MATERIAL|COMMON|&7A bundle of beetroots.
sugar_cane_bundle|&aSugar Cane Bundle|SUGAR_CANE|FARMING_MATERIAL|COMMON|&7A bundle of sugar cane.
cactus_needles|&2Cactus Needles|CACTUS|FARMING_MATERIAL|COMMON|&7A bundle of sharp cactus needles.
pumpkin_shell|&6Pumpkin Shell|PUMPKIN|FARMING_MATERIAL|COMMON|&7A tough piece of pumpkin shell.
melon_rind|&aMelon Rind|MELON_SLICE|FARMING_MATERIAL|COMMON|&7A tough piece of melon rind.
cocoa_shell|&6Cocoa Shell|COCOA_BEANS|FARMING_MATERIAL|UNCOMMON|&7A dry shell from cocoa beans.
nether_wart_root|&4Nether Wart Root|NETHER_WART|FARMING_MATERIAL|UNCOMMON|&7A root from a nether wart plant.
honey_glob|&6Honey Glob|HONEY_BOTTLE|FARMING_MATERIAL|UNCOMMON|&7A thick glob of sticky honey.
bee_wing|&eBee Wing|HONEYCOMB|FARMING_MATERIAL|RARE|&7A fragile wing dropped by a bee.
zombie_tooth|&2Zombie Tooth|ROTTEN_FLESH|MONSTER_MATERIAL|COMMON|&7A chipped tooth from a zombie.
skeleton_rib|&fSkeleton Rib|BONE|MONSTER_MATERIAL|COMMON|&7A cracked rib from a skeleton.
spider_leg|&8Spider Leg|STRING|MONSTER_MATERIAL|COMMON|&7A broken leg from a spider.
creeper_powder_clump|&2Creeper Powder Clump|GUNPOWDER|MONSTER_MATERIAL|COMMON|&7A clump of unstable creeper powder.
enderman_eye|&5Enderman Eye|ENDER_PEARL|MONSTER_MATERIAL|RARE|&7A strange eye-like pearl from an enderman.
slime_glob|&aSlime Glob|SLIME_BALL|MONSTER_MATERIAL|COMMON|&7A sticky glob of slime.
magma_glob|&cMagma Glob|MAGMA_CREAM|MONSTER_MATERIAL|UNCOMMON|&7A hot glob of magma cream.
blaze_cinder|&6Blaze Cinder|BLAZE_POWDER|MONSTER_MATERIAL|UNCOMMON|&7A glowing cinder from a blaze.
ghast_wisp|&fGhast Wisp|GHAST_TEAR|MONSTER_MATERIAL|RARE|&7A ghostly wisp from a ghast tear.
phantom_fiber|&9Phantom Fiber|PHANTOM_MEMBRANE|MONSTER_MATERIAL|RARE|&7A thin fiber from a phantom membrane.
cod_scale|&bCod Scale|COD|FISHING_MATERIAL|COMMON|&7A small scale from a cod.
salmon_fin|&cSalmon Fin|SALMON|FISHING_MATERIAL|COMMON|&7A fin from a salmon.
puffer_spine|&ePuffer Spine|PUFFERFISH|FISHING_MATERIAL|UNCOMMON|&7A sharp spine from a pufferfish.
tropical_fin|&dTropical Fin|TROPICAL_FISH|FISHING_MATERIAL|UNCOMMON|&7A colorful fin from a tropical fish.
kelp_thread|&2Kelp Thread|KELP|FISHING_MATERIAL|COMMON|&7A stringy thread from kelp.
dried_kelp_wrap|&2Dried Kelp Wrap|DRIED_KELP|FISHING_MATERIAL|COMMON|&7A wrap made from dried kelp.
prismarine_chip|&3Prismarine Chip|PRISMARINE_SHARD|FISHING_MATERIAL|UNCOMMON|&7A chip of prismarine from the sea.
nautilus_fragment|&fNautilus Fragment|NAUTILUS_SHELL|FISHING_MATERIAL|RARE|&7A broken piece of a nautilus shell.
imperial_chain_link|&7Imperial Chain Link|CHAIN|DUNGEON_MATERIAL|COMMON|&7A chain link from Imperial dungeon armor.
imperial_banner_thread|&fImperial Banner Thread|WHITE_CARPET|DUNGEON_MATERIAL|UNCOMMON|&7Thread torn from an Imperial banner.
imperial_sword_tip|&7Imperial Sword Tip|IRON_NUGGET|DUNGEON_MATERIAL|UNCOMMON|&7The broken tip of an Imperial sword.
imperial_bowstring|&fImperial Bowstring|STRING|DUNGEON_MATERIAL|UNCOMMON|&7A bowstring used by Imperial archers.
imperial_guard_plate|&7Imperial Guard Plate|IRON_INGOT|DUNGEON_MATERIAL|RARE|&7A metal plate from an Imperial guard.
imperial_captain_badge|&6Imperial Captain Badge|GOLD_INGOT|DUNGEON_MATERIAL|EPIC|&7A badge worn by an Imperial captain.
tyrant_blood_drop|&4Tyrant Blood Drop|REDSTONE|DUNGEON_MATERIAL|EPIC|&7A dark drop of blood from the Tyrant.
tyrant_armor_scale|&8Tyrant Armor Scale|NETHERITE_SCRAP|DUNGEON_MATERIAL|LEGENDARY|&7A scale chipped from the Tyrant's armor.
soul_spark|&bSoul Spark|SOUL_TORCH|MAGIC_MATERIAL|RARE|&7A tiny spark of soul energy.
glowstone_core|&eGlowstone Core|GLOWSTONE|MAGIC_MATERIAL|RARE|&7A glowing core of compacted glowstone.
end_stone_dust|&fEnd Stone Dust|END_STONE|MAGIC_MATERIAL|UNCOMMON|&7Dust scraped from End Stone.
purpur_scale|&dPurpur Scale|PURPUR_BLOCK|MAGIC_MATERIAL|RARE|&7A strange scale from purpur stone.
chorus_thread|&5Chorus Thread|CHORUS_FRUIT|MAGIC_MATERIAL|RARE|&7A thread woven with chorus essence.
void_glass|&5Void Glass|BLACK_STAINED_GLASS|MAGIC_MATERIAL|EPIC|&7A shard of glass touched by the void.
nether_ember|&cNether Ember|MAGMA_BLOCK|MAGIC_MATERIAL|UNCOMMON|&7A warm ember from the Nether.
warped_spore|&3Warped Spore|WARPED_FUNGUS|MAGIC_MATERIAL|UNCOMMON|&7A glowing spore from warped fungus.
crimson_spore|&cCrimson Spore|CRIMSON_FUNGUS|MAGIC_MATERIAL|UNCOMMON|&7A glowing spore from crimson fungus.
ancient_spell_page|&dAncient Spell Page|PAPER|MAGIC_MATERIAL|EPIC|&7A torn page from an ancient spellbook.
""";

    public static void apply(ConfigurationSection root) {
        for (Definition definition : definitions()) {
            String base = "custom-items.items." + definition.id();
            if (root.isConfigurationSection(base)) continue;
            String browserCategory = browserCategory(definition.type());
            String bazaarCategory = bazaarCategory(definition.type());
            root.set(base + ".name", definition.name());
            root.set(base + ".material", definition.material());
            root.set(base + ".type", definition.type());
            root.set(base + ".mmo.type", "RECIPE_MATERIAL");
            root.set(base + ".category", browserCategory);
            root.set(base + ".collection.category", bazaarCategory);
            root.set(base + ".recipe-material.category", bazaarCategory);
            root.set(base + ".rarity", definition.rarity());
            root.set(base + ".lore", List.of(definition.lore()));
            root.set(base + ".max-stack-size", 64);
            root.set(base + ".mmo.random-roll", false);
            root.set(base + ".mmo.stats", null);
            root.set(base + ".random-rarity", false);
            root.set(base + ".ability.type", "none");
            root.set(base + ".ability.mana-cost", 0);
            root.set(base + ".consumable.enabled", false);
            root.set(base + ".tradeable", true);
            root.set(base + ".soulbound", false);
            root.set(base + ".auctionable", true);
            root.set(base + ".bazaarable", true);
            root.set(base + ".guild-sellable", true);
            root.set(base + ".sellall-allowed", true);
            root.set(base + ".sell-price", sellPrice(definition.rarity()));
        }
    }

    public static List<Definition> definitions() {
        return DEFINITIONS.lines().filter(line -> !line.isBlank()).map(line -> {
            String[] parts = line.split("\\|", 6);
            return new Definition(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
        }).toList();
    }

    public static Set<String> materialIds() {
        Set<String> ids = new LinkedHashSet<>();
        definitions().forEach(definition -> ids.add(definition.id()));
        return Set.copyOf(ids);
    }

    public static String browserCategory(String type) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "MINING_MATERIAL" -> "mining_materials";
            case "FORAGING_MATERIAL" -> "foraging_materials";
            case "FARMING_MATERIAL" -> "farming_materials";
            case "MONSTER_MATERIAL" -> "combat_materials";
            case "FISHING_MATERIAL" -> "fishing_materials";
            case "DUNGEON_MATERIAL" -> "dungeon_materials";
            case "MAGIC_MATERIAL" -> "magic_materials";
            default -> "materials";
        };
    }

    private static String bazaarCategory(String type) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "MINING_MATERIAL" -> "mining";
            case "FORAGING_MATERIAL" -> "foraging";
            case "FARMING_MATERIAL" -> "farming";
            case "MONSTER_MATERIAL" -> "combat";
            case "FISHING_MATERIAL" -> "fishing";
            case "DUNGEON_MATERIAL" -> "dungeon";
            default -> "misc";
        };
    }

    private static int sellPrice(String rarity) {
        return switch (rarity.toUpperCase(Locale.ROOT)) {
            case "LEGENDARY" -> 75;
            case "EPIC" -> 30;
            case "RARE" -> 12;
            case "UNCOMMON" -> 5;
            default -> 2;
        };
    }

    public record Definition(String id, String name, String material, String type, String rarity, String lore) {}
}
