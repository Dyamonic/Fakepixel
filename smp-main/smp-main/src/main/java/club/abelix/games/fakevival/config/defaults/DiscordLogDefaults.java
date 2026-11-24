package club.abelix.games.fakevival.config.defaults;

import club.abelix.games.fakevival.registries.WebhookRegistry;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public final class DiscordLogDefaults {
    private DiscordLogDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("discord.dupe-detection.enabled", true);
        root.set("discord.dupe-detection.env-var", WebhookRegistry.variableName("discord.dupe-detection").orElse("SMP_DISCORD_DUPE_WEBHOOK"));
        root.set("discord.dupe-detection.username", "Dupe Detection");
        root.set("discord.dupe-detection.embed-color", 15158332);
        root.set("discord.xray-detection.enabled", true);
        root.set("discord.xray-detection.env-var", WebhookRegistry.variableName("discord.xray-detection").orElse("SMP_DISCORD_XRAY_WEBHOOK"));
        root.set("discord.xray-detection.username", "Xray Detection");
        root.set("discord.xray-detection.embed-color", 16753920);
        root.set("discord.admin-commands.enabled", true);
        root.set("discord.admin-commands.env-var", WebhookRegistry.variableName("discord.admin-commands").orElse("SMP_DISCORD_ADMIN_COMMAND_WEBHOOK"));
        root.set("discord.admin-commands.username", "Admin Command");
        root.set("discord.admin-commands.embed-color", 8978312);
        root.set("discord.admin-commands.log-ops", false);
        root.set("discord.admin-commands.log-all-staff-commands", false);
        root.set("discord.admin-commands.log-console", false);
        root.set("discord.admin-commands.debug", false);
        root.set("discord.admin-commands.log-permission", "smp.admin.commandlog");
        root.set("discord.admin-commands.logged-commands", List.of(
                "smp devcontrols", "smp developercontrols", "smp dev", "smp debug", "smp reload",
                "smp save", "smp networkstatus", "smp collections validate", "smp dungeon validate",
                "smp dungeon save", "smp mobs", "smpworld", "smpworlds", "zone", "dungeon",
                "setdungeonspawn", "addmobspawnlocation", "setmobspawnlocation", "setbosslocation",
                "setdungeonarea", "buildmode", "gmc", "gms", "gma", "gmsp", "give",
                "givecustomitem", "itembrowser", "edititem", "enchantadmin"));
        root.set("discord.trades.enabled", true);
        root.set("discord.trades.env-var", WebhookRegistry.variableName("discord.trades").orElse("SMP_DISCORD_TRADES_WEBHOOK"));
        root.set("discord.trades.username", "SMP Trades");
        root.set("discord.trades.embed-color", 5763719);
    }
}
