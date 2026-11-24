package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class SpawnProtectionDefaults {
    private SpawnProtectionDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("spawnprotection.enabled", true);
        root.set("spawnprotection.spawn-world", "hub");
        root.set("spawnprotection.difficulty", "EASY");
        root.set("spawnprotection.random-tick-speed", 0);
        root.set("spawnprotection.allow-mob-spawning", false);
        root.set("spawnprotection.force-adventure", true);
        root.set("spawnprotection.disable-fall-damage", true);
        root.set("spawnprotection.disable-pvp", true);
        root.set("spawnprotection.disable-farmland-trample", true);
        root.set("spawnprotection.disable-fire-tick", true);
        root.set("spawnprotection.disable-fire-spread", true);
        root.set("spawnprotection.disable-leaf-decay", true);
        root.set("spawnprotection.disable-bed-sleep", true);
        root.set("spawnprotection.disable-container-access", true);
        root.set("spawnprotection.disable-hunger", true);
        root.set("spawnprotection.disable-block-breaking-placing", true);
        root.set("spawnprotection.disable-mob-griefing", true);
        root.set("spawnprotection.disable-enderman-griefing", true);
        root.set("spawnprotection.disable-enderdragon-griefing", true);
        root.set("spawnprotection.disable-enderdragon-fireball", true);
        root.set("spawnprotection.disable-enderdragon-fireball-explosion", true);
        root.set("spawnprotection.enable-keepinventory", true);
        root.set("spawnprotection.disable-other-griefing", true);
        root.set("spawnprotection.advance_weather.type", "clear");
        root.set("spawnprotection.advance_weather.duration", 12000);
        root.set("spawnprotection.time", "day");
        root.set("spawnprotection.hunger-vanilla-worlds", List.of("world", "world_the_end", "world_the_nether"));
    }
}
