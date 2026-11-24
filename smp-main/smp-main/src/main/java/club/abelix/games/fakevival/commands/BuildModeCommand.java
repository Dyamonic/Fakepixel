package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.SpawnProtectionManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BuildModeCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final SpawnProtectionManager spawnProtectionManager;

    public BuildModeCommand(JavaPlugin plugin, SpawnProtectionManager spawnProtectionManager) {
        this.plugin = plugin;
        this.spawnProtectionManager = spawnProtectionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender)) return CommandUtil.noPermission(plugin, sender);

        String mode = args.length == 0 ? "toggle" : args[0].toLowerCase(java.util.Locale.ROOT);
        Player target = resolveTarget(sender, args);
        if (target == null) return true;

        if (mode.equals("status")) {
            sender.sendMessage(CC.color("&7Build mode for &f" + target.getName() + "&7: "
                    + (spawnProtectionManager.isBuildMode(target) ? "&aenabled" : "&cdisabled")));
            return true;
        }

        Boolean explicitState = switch (mode) {
            case "on", "enable", "enabled", "true" -> true;
            case "off", "disable", "disabled", "false" -> false;
            case "toggle" -> null;
            default -> {
                sender.sendMessage(CC.color("&cUsage: /buildmode [on|off|toggle|status] [player]"));
                yield null;
            }
        };
        if (!mode.equals("toggle") && explicitState == null) return true;

        boolean enabled = explicitState == null
                ? spawnProtectionManager.toggleBuildMode(target)
                : spawnProtectionManager.setBuildMode(target, explicitState);

        applyGameMode(target, enabled);
        target.sendMessage(CC.color(enabled
                ? "&aBuild mode enabled."
                : "&cBuild mode disabled."));
        if (!sender.equals(target)) {
            sender.sendMessage(CC.color((enabled ? "&aEnabled" : "&cDisabled") + " build mode for &f" + target.getName() + "&7."));
        }
        return true;
    }

    private Player resolveTarget(CommandSender sender, String[] args) {
        String targetName = args.length >= 2 ? args[1] : "";
        if (!targetName.isBlank()) {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) sender.sendMessage(CC.color("&cThat player is not online."));
            return target;
        }
        if (sender instanceof Player player) return player;
        sender.sendMessage(CC.color("&cConsole usage: /buildmode <on|off|toggle|status> <player>"));
        return null;
    }

    private void applyGameMode(Player player, boolean enabled) {
        if (enabled && player.getGameMode() == GameMode.ADVENTURE) {
            player.setGameMode(GameMode.CREATIVE);
            return;
        }
        if (!enabled && spawnProtectionManager.isInside(player.getLocation())
                && club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("spawnprotection.force-adventure", true)
                && player.getGameMode() == GameMode.CREATIVE) {
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    private boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("smp.admin.buildmode") || sender.hasPermission("fakevival.admin.buildmode") || sender.hasPermission("fakevival.devcontrols.admin") || sender.hasPermission("smp.dev");
    }
}
