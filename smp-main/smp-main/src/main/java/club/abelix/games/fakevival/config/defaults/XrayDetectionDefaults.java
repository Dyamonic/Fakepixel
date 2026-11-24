package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class XrayDetectionDefaults {
    private XrayDetectionDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("xray-detection.enabled", true);
        root.set("xray-detection.window-seconds", 180);
        root.set("xray-detection.alert-threshold", 10);
        root.set("xray-detection.alert-cooldown-seconds", 120);
        root.set("xray-detection.max-y", 32);
        root.set("xray-detection.max-light-level", 5);
        root.set("xray-detection.staff-permission", "fakevival.staff.xray");
        root.set("xray-detection.staff-message", "&c[Xray] &f%player% &7mined &e%count% &7valuable ores in &e%seconds%s&7. &8%location% &7- &f%summary%");
        root.set("xray-detection.tracked-blocks", List.of("DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE", "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE", "ANCIENT_DEBRIS", "GOLD_ORE", "DEEPSLATE_GOLD_ORE", "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE", "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE"));
    }
}
