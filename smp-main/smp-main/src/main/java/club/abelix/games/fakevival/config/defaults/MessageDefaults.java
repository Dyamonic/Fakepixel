package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class MessageDefaults {
    private MessageDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("messages.pve-enabled", "&aPvE mode enabled. Players cannot damage you, and you cannot damage players.");
        root.set("messages.pve-disabled", "&cPvE mode disabled. You can now PvP in PvP-enabled worlds.");
        root.set("messages.pve-combat-tagged", "&cYou cannot toggle PvE while combat tagged. Wait &f%seconds%s&c.");
        root.set("messages.pve-spawn-deny", "&cPvP is disabled in spawn.");
        root.set("messages.pve-world-deny", "&cPvP is disabled in this world.");
        root.set("messages.pve-attacker-deny", "&cYou are in PvE mode. Type &f/pve &cto enable PvP first.");
        root.set("messages.pve-victim-deny", "&cThat player is in PvE mode.");
        root.set("messages.no-permission", "&cYou do not have permission.");
        root.set("messages.skill-level-up", "&aYour %skill% skill is now level %level%!");
        root.set("messages.job-level-up", "&aYour %job% job is now level %level%!");
        root.set("messages.spawnprotection-deny", "&cYou cannot build or break blocks in spawn protection.");
        root.set("messages.rtp-teleported", "&aTeleported to a random safe location.");
        root.set("messages.rtp-cooldown", "&cYou can use RTP again in %seconds%s.");
    }
}
