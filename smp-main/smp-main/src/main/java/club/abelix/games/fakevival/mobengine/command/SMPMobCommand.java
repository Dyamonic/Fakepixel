package club.abelix.games.fakevival.mobengine.command;

import club.abelix.games.fakevival.mobengine.mob.SMPMobManager;
import club.abelix.games.fakevival.mobengine.profile.SMPMobPerformanceProfiler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class SMPMobCommand implements CommandExecutor, TabCompleter {
    private final SMPMobManager manager;
    private final SMPMobPerformanceProfiler profiler;

    public SMPMobCommand(SMPMobManager manager, SMPMobPerformanceProfiler profiler) {
        this.manager = manager;
        this.profiler = profiler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smpmob.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "debug" -> {
                sender.sendMessage(ChatColor.GREEN + "SMPMobEngine running. " + ChatColor.GRAY + profiler.summary());
                return true;
            }
            case "count" -> {
                sender.sendMessage(ChatColor.GREEN + "SMP mobs: " + ChatColor.YELLOW + manager.count());
                return true;
            }
            case "profile" -> {
                sender.sendMessage(ChatColor.AQUA + "Profiler: " + ChatColor.GRAY + profiler.summary());
                return true;
            }
            case "clear" -> {
                manager.clearAll();
                sender.sendMessage(ChatColor.GREEN + "Cleared all SMP mobs.");
                return true;
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this test command.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /smpmob spawn <mobId> <amount>");
                    return true;
                }
                String mobId = args[1];
                int amount = Math.clamp(parseInt(args[2], 1), 1, 500);
                Location base = player.getLocation();
                for (int i = 0; i < amount; i++) {
                    Location loc = base.clone().add((i % 10) - 5, 0, (((double) i / 10) % 10) - 5);
                    manager.spawnSMPMob(mobId, loc);
                }
                sender.sendMessage(ChatColor.GREEN + "Spawned " + ChatColor.YELLOW + amount
                        + ChatColor.GREEN + " SMP mobs with id " + ChatColor.YELLOW + mobId + ChatColor.GREEN + ".");
                return true;
            }
            default -> {
                help(sender);
                return true;
            }
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "/smpmob debug");
        sender.sendMessage(ChatColor.GOLD + "/smpmob count");
        sender.sendMessage(ChatColor.GOLD + "/smpmob profile");
        sender.sendMessage(ChatColor.GOLD + "/smpmob spawn <mobId> <amount>");
        sender.sendMessage(ChatColor.GOLD + "/smpmob clear");
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("debug", "count", "profile", "spawn", "clear");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return List.of("crypt_zombie", "elite_skeleton", "spider", "boss_revenant");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("spawn")) {
            return List.of("10", "50", "100", "500");
        }
        return new ArrayList<>();
    }
}
