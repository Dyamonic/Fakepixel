package club.abelix.games.fakevival.registries;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class VanillaBazaarRegistry {
    private static final Map<String, Subcategory> SUBCATEGORIES = new LinkedHashMap<>();
    private static final Map<Material, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<String, java.util.LinkedHashSet<Material>> BY_SUBCATEGORY = new LinkedHashMap<>();

    static {
        category("building_blocks", "Building Blocks", Material.BRICKS);
        category("natural_blocks", "Natural Blocks", Material.GRASS_BLOCK);
        category("wood", "Wood", Material.OAK_LOG);
        category("ores_minerals", "Ores & Minerals", Material.DIAMOND);
        category("farming", "Farming", Material.WHEAT);
        category("mob_drops", "Mob Drops", Material.ROTTEN_FLESH);
        category("fishing", "Fishing", Material.COD);
        category("redstone", "Redstone", Material.REDSTONE);
        category("nether", "Nether", Material.NETHERRACK);
        category("end", "End", Material.END_STONE);
        category("utility", "Utility", Material.CHEST);
        category("dyes_flowers", "Dyes & Flowers", Material.POPPY);

        add("building_blocks", "STONE,COBBLESTONE,MOSSY_COBBLESTONE,STONE_BRICKS,MOSSY_STONE_BRICKS,CRACKED_STONE_BRICKS,CHISELED_STONE_BRICKS,GRANITE,POLISHED_GRANITE,DIORITE,POLISHED_DIORITE,ANDESITE,POLISHED_ANDESITE,DEEPSLATE,COBBLED_DEEPSLATE,POLISHED_DEEPSLATE,DEEPSLATE_BRICKS,DEEPSLATE_TILES,TUFF,CALCITE,DRIPSTONE_BLOCK,BRICKS,MUD_BRICKS,SANDSTONE,CUT_SANDSTONE,SMOOTH_SANDSTONE,RED_SANDSTONE,CUT_RED_SANDSTONE,SMOOTH_RED_SANDSTONE,PRISMARINE,PRISMARINE_BRICKS,DARK_PRISMARINE,SEA_LANTERN,QUARTZ_BLOCK,QUARTZ_BRICKS,CHISELED_QUARTZ_BLOCK,SMOOTH_QUARTZ,PURPUR_BLOCK,PURPUR_PILLAR,END_STONE_BRICKS,BLACKSTONE,POLISHED_BLACKSTONE,POLISHED_BLACKSTONE_BRICKS,BASALT,POLISHED_BASALT,SMOOTH_BASALT,NETHERRACK,NETHER_BRICKS,RED_NETHER_BRICKS,END_STONE,OBSIDIAN,CRYING_OBSIDIAN,GLASS,TERRACOTTA");
        add("natural_blocks", "DIRT,COARSE_DIRT,ROOTED_DIRT,GRASS_BLOCK,PODZOL,MYCELIUM,MUD,CLAY,GRAVEL,SAND,RED_SAND,SOUL_SAND,SOUL_SOIL,SNOW_BLOCK,ICE,PACKED_ICE,BLUE_ICE,MOSS_BLOCK,MOSS_CARPET,AZALEA,FLOWERING_AZALEA,BIG_DRIPLEAF,SMALL_DRIPLEAF,VINE,GLOW_LICHEN,CACTUS,SUGAR_CANE,BAMBOO,KELP,DRIED_KELP,SEAGRASS,SEA_PICKLE,SPONGE,WET_SPONGE");
        add("ores_minerals", "COAL,CHARCOAL,RAW_IRON,IRON_INGOT,RAW_GOLD,GOLD_INGOT,RAW_COPPER,COPPER_INGOT,REDSTONE,LAPIS_LAZULI,DIAMOND,EMERALD,QUARTZ,AMETHYST_SHARD,FLINT,GLOWSTONE_DUST,GUNPOWDER,BLAZE_POWDER,NETHERITE_SCRAP,NETHERITE_INGOT,COAL_ORE,DEEPSLATE_COAL_ORE,IRON_ORE,DEEPSLATE_IRON_ORE,COPPER_ORE,DEEPSLATE_COPPER_ORE,GOLD_ORE,DEEPSLATE_GOLD_ORE,REDSTONE_ORE,DEEPSLATE_REDSTONE_ORE,LAPIS_ORE,DEEPSLATE_LAPIS_ORE,DIAMOND_ORE,DEEPSLATE_DIAMOND_ORE,EMERALD_ORE,DEEPSLATE_EMERALD_ORE,NETHER_QUARTZ_ORE,NETHER_GOLD_ORE,ANCIENT_DEBRIS,AMETHYST_BLOCK,BUDDING_AMETHYST");
        add("farming", "WHEAT,WHEAT_SEEDS,CARROT,POTATO,POISONOUS_POTATO,BEETROOT,BEETROOT_SEEDS,PUMPKIN,PUMPKIN_SEEDS,MELON,MELON_SLICE,MELON_SEEDS,SUGAR_CANE,CACTUS,BAMBOO,COCOA_BEANS,NETHER_WART,APPLE,GOLDEN_CARROT,BREAD,COOKIE,CAKE,EGG,MILK_BUCKET,HONEY_BOTTLE,HONEYCOMB,BEEHIVE,BEE_NEST,SWEET_BERRIES,GLOW_BERRIES,CHORUS_FRUIT,CHORUS_FLOWER,BROWN_MUSHROOM,RED_MUSHROOM,MUSHROOM_STEM");
        add("mob_drops", "ROTTEN_FLESH,BONE,STRING,SPIDER_EYE,FERMENTED_SPIDER_EYE,ENDER_PEARL,SLIME_BALL,MAGMA_CREAM,BLAZE_ROD,GHAST_TEAR,PHANTOM_MEMBRANE,LEATHER,RABBIT_HIDE,RABBIT_FOOT,FEATHER,CHICKEN,COOKED_CHICKEN,PORKCHOP,COOKED_PORKCHOP,BEEF,COOKED_BEEF,MUTTON,COOKED_MUTTON,RABBIT,COOKED_RABBIT,INK_SAC,GLOW_INK_SAC,TURTLE_SCUTE,ARMADILLO_SCUTE,PRISMARINE_SHARD,PRISMARINE_CRYSTALS,NAUTILUS_SHELL,SHULKER_SHELL");
        add("fishing", "COD,SALMON,TROPICAL_FISH,PUFFERFISH,COOKED_COD,COOKED_SALMON,LILY_PAD,NAME_TAG,SADDLE");
        add("redstone", "REDSTONE,REDSTONE_TORCH,REPEATER,COMPARATOR,PISTON,STICKY_PISTON,OBSERVER,DISPENSER,DROPPER,HOPPER,TRIPWIRE_HOOK,LEVER,DAYLIGHT_DETECTOR,TARGET,NOTE_BLOCK,JUKEBOX,RAIL,POWERED_RAIL,DETECTOR_RAIL,ACTIVATOR_RAIL,MINECART,CHEST_MINECART,FURNACE_MINECART,HOPPER_MINECART,TNT_MINECART");
        add("nether", "NETHERRACK,SOUL_SAND,SOUL_SOIL,BASALT,BLACKSTONE,GLOWSTONE,NETHER_BRICK,NETHER_BRICKS,RED_NETHER_BRICKS,NETHER_WART,WARPED_WART_BLOCK,NETHER_WART_BLOCK,SHROOMLIGHT,CRIMSON_FUNGUS,WARPED_FUNGUS,CRIMSON_ROOTS,WARPED_ROOTS,WEEPING_VINES,TWISTING_VINES,QUARTZ,MAGMA_BLOCK,CRYING_OBSIDIAN,OBSIDIAN");
        add("end", "END_STONE,END_STONE_BRICKS,CHORUS_FRUIT,CHORUS_FLOWER,POPPED_CHORUS_FRUIT,PURPUR_BLOCK,PURPUR_PILLAR,ENDER_PEARL,END_ROD,DRAGON_BREATH,SHULKER_SHELL");
        add("utility", "TORCH,SOUL_TORCH,LANTERN,SOUL_LANTERN,CHEST,BARREL,FURNACE,BLAST_FURNACE,SMOKER,CRAFTING_TABLE,ANVIL,CHIPPED_ANVIL,DAMAGED_ANVIL,ENCHANTING_TABLE,BREWING_STAND,CAULDRON,COMPOSTER,CAMPFIRE,SOUL_CAMPFIRE,LADDER,SCAFFOLDING,CHAIN,IRON_BARS,BOOKSHELF,CHISELED_BOOKSHELF,BOOK,PAPER,MAP,COMPASS,CLOCK,GLASS_BOTTLE,BUCKET,WATER_BUCKET,LAVA_BUCKET,BOWL,FLOWER_POT");
        add("dyes_flowers", "DANDELION,POPPY,BLUE_ORCHID,ALLIUM,AZURE_BLUET,OXEYE_DAISY,CORNFLOWER,LILY_OF_THE_VALLEY,WITHER_ROSE,SUNFLOWER,LILAC,ROSE_BUSH,PEONY,PITCHER_PLANT,TORCHFLOWER");

        for (Material material : Material.values()) {
            String name = material.name();
            if (name.endsWith("_STAINED_GLASS") || name.matches("(WHITE|ORANGE|MAGENTA|LIGHT_BLUE|YELLOW|LIME|PINK|GRAY|LIGHT_GRAY|CYAN|PURPLE|BLUE|BROWN|GREEN|RED|BLACK)_TERRACOTTA")) add("building_blocks", material);
            if (name.endsWith("_DYE") || name.endsWith("_TULIP")) add("dyes_flowers", material);
            if (isWoodCommodity(name)) add("wood", material);
            if (name.endsWith("_BUTTON") || name.endsWith("_PRESSURE_PLATE")) add("redstone", material);
        }
        add("wood", "CRIMSON_STEM,WARPED_STEM,CRIMSON_HYPHAE,WARPED_HYPHAE,STRIPPED_CRIMSON_STEM,STRIPPED_WARPED_STEM,STRIPPED_CRIMSON_HYPHAE,STRIPPED_WARPED_HYPHAE,CRIMSON_PLANKS,WARPED_PLANKS,MANGROVE_PROPAGULE,STICK");
    }

    private VanillaBazaarRegistry() {}

    public static String key(Material material) { return "vanilla:" + material.name().toLowerCase(Locale.ROOT); }
    public static Optional<Entry> byKey(String key) {
        if (key == null || !key.toLowerCase(Locale.ROOT).startsWith("vanilla:")) return Optional.empty();
        Material material = Material.matchMaterial(key.substring("vanilla:".length()));
        return Optional.ofNullable(ENTRIES.get(material));
    }
    public static Optional<Entry> byMaterial(Material material) { return Optional.ofNullable(ENTRIES.get(material)); }
    public static List<Entry> entries() { return List.copyOf(ENTRIES.values()); }
    public static List<Entry> entries(String subcategory) { return BY_SUBCATEGORY.getOrDefault(subcategory, new java.util.LinkedHashSet<>()).stream().map(ENTRIES::get).toList(); }
    public static List<Subcategory> subcategories() { return List.copyOf(SUBCATEGORIES.values()); }

    private static void category(String id, String name, Material icon) { SUBCATEGORIES.put(id, new Subcategory(id, name, icon)); BY_SUBCATEGORY.put(id, new java.util.LinkedHashSet<>()); }
    private static void add(String category, String names) { for (String name : names.split(",")) { Material material=Material.matchMaterial(name.trim()); if(material!=null)add(category,material); } }
    private static void add(String category, Material material) {
        if (material == null || !material.isItem() || material.isAir() || unsafe(material)) return;
        BY_SUBCATEGORY.computeIfAbsent(category, ignored -> new java.util.LinkedHashSet<>()).add(material);
        ENTRIES.putIfAbsent(material, new Entry(key(material), material, category, buy(material, category), sell(material, category)));
    }
    private static boolean isWoodCommodity(String name) {
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_PLANKS") || name.endsWith("_SAPLING") || name.endsWith("_LEAVES")
                || name.startsWith("STRIPPED_") && (name.endsWith("_LOG") || name.endsWith("_WOOD"));
    }
    private static boolean unsafe(Material material) {
        String n=material.name();
        return n.endsWith("_SPAWN_EGG") || n.contains("COMMAND_BLOCK") || n.contains("MUSIC_DISC") || n.endsWith("_SWORD") || n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE") || n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")
                || List.of("BEDROCK","BARRIER","STRUCTURE_BLOCK","STRUCTURE_VOID","JIGSAW","LIGHT","SPAWNER","TRIAL_SPAWNER","DEBUG_STICK","KNOWLEDGE_BOOK","COMMAND_BLOCK_MINECART","PLAYER_HEAD","ELYTRA","DRAGON_EGG","NETHER_STAR","TOTEM_OF_UNDYING","ENCHANTED_BOOK","WRITABLE_BOOK","WRITTEN_BOOK").contains(n);
    }
    private static double buy(Material m,String c){return switch(m){case DIAMOND->800;case EMERALD->500;case ENDER_PEARL->80;case BLAZE_ROD->120;case NETHERITE_SCRAP->5000;case NETHERITE_INGOT->20000;case ANCIENT_DEBRIS->4000;case DIAMOND_ORE,DEEPSLATE_DIAMOND_ORE->900;case EMERALD_ORE,DEEPSLATE_EMERALD_ORE->600;default->{String n=m.name();if(n.endsWith("_LOG")||n.endsWith("_WOOD")||n.endsWith("_STEM")||n.endsWith("_HYPHAE"))yield 8;if(n.endsWith("_PLANKS"))yield 2;if(c.equals("farming"))yield 6;if(c.equals("mob_drops"))yield 8;if(c.equals("fishing"))yield 10;yield 4;}};}
    private static double sell(Material m,String c){return switch(m){case DIAMOND->250;case EMERALD->150;case ENDER_PEARL->20;case BLAZE_ROD->30;case NETHERITE_SCRAP->1500;case NETHERITE_INGOT->6000;case ANCIENT_DEBRIS->1200;case DIAMOND_ORE,DEEPSLATE_DIAMOND_ORE->275;case EMERALD_ORE,DEEPSLATE_EMERALD_ORE->175;default->{String n=m.name();if(n.endsWith("_LOG")||n.endsWith("_WOOD")||n.endsWith("_STEM")||n.endsWith("_HYPHAE"))yield 2;if(n.endsWith("_PLANKS"))yield 1;if(c.equals("farming")||c.equals("mob_drops"))yield 2;if(c.equals("fishing"))yield 3;yield 1;}};}

    public record Entry(String key, Material material, String subcategory, double buyPrice, double sellPrice) {}
    public record Subcategory(String id, String displayName, Material icon) {}
}
