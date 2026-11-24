package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.KitManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KitCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final KitManager kitManager;

    public KitCommand(JavaPlugin plugin, KitManager kitManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sendList(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("claim")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /kit claim <kit>"));
                return true;
            }
            return claim(sender, args[1]);
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CC.color("&cOnly players can create kits from an inventory."));
                return true;
            }
            if (!hasAdmin(sender)) return noPermission(sender);
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /kit create <name> [delay]"));
                return true;
            }
            String kitName = kitManager.normalize(args[1]);
            if (kitName.isBlank()) {
                sender.sendMessage(CC.color("&cKit names can only use letters, numbers, underscores, and dashes."));
                return true;
            }
            long delay = args.length >= 3 ? parseDelay(args[2]) : 86400L;
            if (delay < 0L) {
                sender.sendMessage(CC.color("&cInvalid delay. Examples: 30m, 12h, 7d, none"));
                return true;
            }
            kitManager.createKit(player, kitName, delay);
            sender.sendMessage(CC.color("&aCreated kit &f" + kitName + " &awith delay &e" + formatDuration(delay) + "&a."));
            return true;
        }

        if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove")) {
            if (!hasAdmin(sender)) return noPermission(sender);
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /kit delete <name>"));
                return true;
            }
            sender.sendMessage(CC.color(kitManager.deleteKit(args[1]) ? "&aDeleted kit &f" + args[1] + "&a." : "&cThat kit does not exist."));
            return true;
        }

        if (args[0].equalsIgnoreCase("setdelay")) {
            if (!hasAdmin(sender)) return noPermission(sender);
            if (args.length < 3) {
                sender.sendMessage(CC.color("&cUsage: /kit setdelay <name> <delay>"));
                return true;
            }
            long delay = parseDelay(args[2]);
            if (delay < 0L) {
                sender.sendMessage(CC.color("&cInvalid delay. Examples: 30m, 12h, 7d, none"));
                return true;
            }
            sender.sendMessage(CC.color(kitManager.setDelay(args[1], delay) ? "&aUpdated kit delay to &e" + formatDuration(delay) + "&a." : "&cThat kit does not exist."));
            return true;
        }

        if (args[0].equalsIgnoreCase("setpermission") || args[0].equalsIgnoreCase("setperm")) {
            if (!hasAdmin(sender)) return noPermission(sender);
            if (args.length < 3) {
                sender.sendMessage(CC.color("&cUsage: /kit setpermission <name> <permission|none>"));
                return true;
            }
            String permission = args[2].equalsIgnoreCase("none") ? "" : args[2];
            sender.sendMessage(CC.color(kitManager.setPermission(args[1], permission) ? "&aUpdated kit permission." : "&cThat kit does not exist."));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!hasAdmin(sender)) return noPermission(sender);
            if (args.length < 3) {
                sender.sendMessage(CC.color("&cUsage: /kit give <player> <kit>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(CC.color("&cThat player is not online."));
                return true;
            }
            if (kitManager.exists(args[2])) {
                sender.sendMessage(CC.color("&cThat kit does not exist."));
                return true;
            }
            int itemCount = kitManager.claim(target, args[2]);
            sender.sendMessage(CC.color("&aGave kit &f" + args[2] + " &ato &f" + target.getName() + "&a."));
            target.sendMessage(CC.color("&aYou received kit &f" + args[2] + " &7(" + itemCount + " items)&a."));
            return true;
        }

        if (args[0].equalsIgnoreCase("resetcooldown") || args[0].equalsIgnoreCase("resetcd")) {
            if (!hasAdmin(sender)) return noPermission(sender);
            if (args.length < 3) {
                sender.sendMessage(CC.color("&cUsage: /kit resetcooldown <player> <kit>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(CC.color("&cThat player is not online."));
                return true;
            }
            sender.sendMessage(CC.color(kitManager.resetCooldown(target, args[2]) ? "&aReset kit cooldown." : "&cThat kit does not exist."));
            return true;
        }

        return claim(sender, args[0]);
    }

    private boolean claim(CommandSender sender, String kitName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(CC.color("&cUsage: /kit claim <kit>"));
            return true;
        }
        if (!sender.hasPermission("fakevival.command.kit")) return noPermission(sender);
        if (kitManager.exists(kitName)) {
            sender.sendMessage(CC.color("&cThat kit does not exist. Use &f/kits &cto view kits."));
            return true;
        }
        if (!kitManager.canUseKit(player, kitName)) {
            sender.sendMessage(CC.color("&cYou do not have permission to use that kit."));
            return true;
        }
        long remaining = kitManager.getRemainingSeconds(player, kitName);
        if (remaining > 0L) {
            sender.sendMessage(CC.color("&cYou can claim that kit again in &f" + formatDuration(remaining) + "&c."));
            return true;
        }
        int itemCount = kitManager.claim(player, kitName);
        if (itemCount <= 0) {
            sender.sendMessage(CC.color("&cThat kit has no items."));
            return true;
        }
        sender.sendMessage(CC.color("&aClaimed kit &f" + kitManager.getDisplayName(kitName) + " &7(" + itemCount + " items)&a."));
        return true;
    }

    private void sendList(CommandSender sender) {
        sender.sendMessage(CC.color("&6&lKits"));
        List<String> kitNames = kitManager.getKitNames();
        if (kitNames.isEmpty()) {
            sender.sendMessage(CC.color("&7No kits have been created yet."));
            return;
        }
        for (String kitName : kitNames) {
            String delay = formatDuration(kitManager.getDelaySeconds(kitName));
            String line = "&e" + kitName + " &7- &f" + delay + " &8(/kit " + kitName + ")";
            sender.sendMessage(CC.color(line));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(CC.color("&6&lKit Commands"));
        sender.sendMessage(CC.color("&e/kit &7- List kits"));
        sender.sendMessage(CC.color("&e/kit <kit> &7- Claim a kit"));
        sender.sendMessage(CC.color("&e/kit claim <kit> &7- Claim a kit"));
        if (hasAdmin(sender)) {
            sender.sendMessage(CC.color("&e/kit create <name> [delay] &7- Save your inventory as a kit"));
            sender.sendMessage(CC.color("&e/kit delete <name> &7- Delete a kit"));
            sender.sendMessage(CC.color("&e/kit setdelay <name> <delay> &7- Change cooldown"));
            sender.sendMessage(CC.color("&e/kit setpermission <name> <permission|none> &7- Require a permission"));
            sender.sendMessage(CC.color("&e/kit give <player> <kit> &7- Give a kit now"));
            sender.sendMessage(CC.color("&e/kit resetcooldown <player> <kit> &7- Reset a player's cooldown"));
        }
    }

    private boolean hasAdmin(CommandSender sender) {
        return sender.hasPermission("fakevival.admin.kit");
    }

    private boolean noPermission(CommandSender sender) {
        sender.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.no-permission", "&cNo permission.")));
        return true;
    }

    private long parseDelay(String value) {
        String input = value.toLowerCase(Locale.ROOT).trim();
        if (input.equals("none") || input.equals("0") || input.equals("never")) return 0L;
        long multiplier = 1L;
        if (input.endsWith("s")) {
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m")) {
            input = input.substring(0, input.length() - 1);
            multiplier = 60L;
        } else if (input.endsWith("h")) {
            input = input.substring(0, input.length() - 1);
            multiplier = 3600L;
        } else if (input.endsWith("d")) {
            input = input.substring(0, input.length() - 1);
            multiplier = 86400L;
        } else if (input.endsWith("w")) {
            input = input.substring(0, input.length() - 1);
            multiplier = 604800L;
        }
        try {
            return Math.max(0L, Long.parseLong(input) * multiplier);
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0L) return "No cooldown";
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;
        if (days > 0L) return days + "d " + hours + "h";
        if (hours > 0L) return hours + "h " + minutes + "m";
        if (minutes > 0L) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("list");
            completions.add("claim");
            completions.add("help");
            completions.addAll(kitManager.getKitNames());
            if (hasAdmin(sender)) {
                completions.add("create");
                completions.add("delete");
                completions.add("setdelay");
                completions.add("setpermission");
                completions.add("give");
                completions.add("resetcooldown");
            }
            return filter(completions, args[0]);
        }
        if (args.length == 2 && List.of("claim", "delete", "setdelay", "setpermission").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(kitManager.getKitNames(), args[1]);
        }
        if (args.length == 2 && List.of("give", "resetcooldown").contains(args[0].toLowerCase(Locale.ROOT))) {
            for (Player player : Bukkit.getOnlinePlayers()) completions.add(player.getName());
            return filter(completions, args[1]);
        }
        if (args.length == 3 && List.of("give", "resetcooldown").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(kitManager.getKitNames(), args[2]);
        }
        return completions;
    }

    private List<String> filter(List<String> options, String input) {
        String lowered = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowered)) result.add(option);
        }
        return result;
    }
}
