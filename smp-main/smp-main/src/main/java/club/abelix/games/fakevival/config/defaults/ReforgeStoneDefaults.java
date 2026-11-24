package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public final class ReforgeStoneDefaults {
    private static final String STONES = """
            sharp_stone|Sharp Stone|FLINT|sharp|&7A whetstone that gives blades a keen combat edge.
            spicy_stone|Spicy Stone|BLAZE_POWDER|spicy|&7A searing stone that empowers aggressive weapon strikes.
            heroic_stone|Heroic Stone|LAPIS_LAZULI|heroic|&7Arcane resolve hums within this polished blue stone.
            legendary_stone|Legendary Stone|GOLD_INGOT|legendary|&7A storied stone reserved for well-rounded weapons.
            reinforced_stone|Reinforced Stone|IRON_INGOT|reinforced|&7Dense metal layers strengthen armor against heavy blows.
            wise_stone|Wise Stone|AMETHYST_SHARD|wise|&7Stored arcane insight expands the wearer's mana reserves.
            titanic_stone|Titanic Stone|ECHO_SHARD|titanic|&7A weighty relic that grants immense health and defense.
            light_stone|Light Stone|FEATHER|light|&7A weightless stone that favors speed and flowing mana.
            prospector_stone|Prospector Stone|DIAMOND|prospector|&7Mineral veins shimmer across its fortune-touched surface.
            lumberjack_stone|Lumberjack Stone|CHARCOAL|lumberjack|&7The scent of old forests follows this rugged stone.
            harvesting_stone|Harvesting Stone|WHEAT_SEEDS|harvesting|&7A fertile charm made for bountiful farming tools.
            digger_stone|Digger Stone|CLAY_BALL|digger|&7Packed earth within it guides swift and fortunate digging.
            angler_stone|Angler Stone|PRISMARINE_SHARD|angler|&7Ocean currents bless rods reforged with this bright shard.
            steady_stone|Steady Stone|STRING|steady|&7A balanced charm that steadies every cast and reel.
            balanced_stone|Balanced Stone|QUARTZ|balanced|&7Three harmonious energies meet within this accessory stone.
            bloody_stone|Bloody Stone|REDSTONE|bloody|&7A crimson pulse strengthens the fiercest accessories.
            """;

    private ReforgeStoneDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        for (String line : STONES.lines().filter(value -> !value.isBlank()).toList()) {
            String[] parts = line.split("\\|", 5);
            String base = "custom-items.items." + parts[0];
            root.set(base, null);
            root.set(base + ".name", "&b" + parts[1]);
            root.set(base + ".material", parts[2]);
            root.set(base + ".type", "REFORGE_STONE");
            root.set(base + ".mmo.type", "UTILITY");
            root.set(base + ".category", "reforge_stones");
            root.set(base + ".subcategory", "reforge_stones");
            root.set(base + ".item-category", "UTILITY");
            root.set(base + ".item-subcategory", "REFORGE_STONES");
            root.set(base + ".rarity", "RARE");
            root.set(base + ".lore", List.of(parts[4], "&8Use in the SMP Anvil to apply its reforge."));
            root.set(base + ".reforge-id", parts[3]);
            root.set(base + ".reforge-stone", true);
            root.set(base + ".random-stats-allowed", false);
            root.set(base + ".random-rarity-allowed", false);
            root.set(base + ".enchant-allowed", false);
            root.set(base + ".reforge-allowed", false);
            root.set(base + ".mmo.random-roll", false);
            root.set(base + ".random-rarity", false);
            root.set(base + ".fixed-rarity", true);
            root.set(base + ".special-item", false);
            root.set(base + ".developer-notebook", false);
            root.set(base + ".unbreakable", true);
            root.set(base + ".max-stack-size", 64);
            root.set(base + ".consumable.enabled", false);
        }
    }
}
