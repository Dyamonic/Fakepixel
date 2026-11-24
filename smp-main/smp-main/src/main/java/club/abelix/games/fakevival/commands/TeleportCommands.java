package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TeleportCommands implements CommandExecutor {
    private final JavaPlugin plugin;

    public TeleportCommands(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.admin.teleport")) return CommandUtil.noPermission(plugin, sender);
        return switch (command.getName().toLowerCase()) {
            case "tp" -> tp(sender, args);
            case "tphere" -> tphere(sender, args);
            case "tpall" -> tpall(sender, args);
            default -> true;
        };
    }

    private boolean tp(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                if (plugin instanceof Game game) game.getNetworkService().staffTeleport(player,args[0]);
                else sender.sendMessage(CC.color("&cThat player is not online."));
                return true;
            }
            player.teleport(target.getLocation());
            sender.sendMessage(CC.color("&aTeleported to &f" + target.getName() + "&a."));
            return true;
        }
        if (args.length == 2) {
            Player player = Bukkit.getPlayerExact(args[0]);
            Player target = Bukkit.getPlayerExact(args[1]);
            if (player == null || target == null) {
                sender.sendMessage(CC.color("&cOne of those players is not online."));
                return true;
            }
            player.teleport(target.getLocation());
            sender.sendMessage(CC.color("&aTeleported &f" + player.getName() + " &ato &f" + target.getName() + "&a."));
            return true;
        }
        sender.sendMessage(CC.color("&cUsage: /tp <player> [target]"));
        return true;
    }

    private boolean tphere(CommandSender sender, String[] args) {
        if (blockedRequestServer(sender)) return true;
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /tphere <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cThat player is not online."));
            return true;
        }
        target.teleport(player.getLocation());
        sender.sendMessage(CC.color("&aTeleported &f" + target.getName() + " &ato you."));
        return true;
    }

    private boolean tpall(CommandSender sender, String[] args) {
        if (blockedRequestServer(sender)) return true;
        Player target = args.length > 0 ? Bukkit.getPlayerExact(args[0]) : sender instanceof Player player ? player : null;
        if (target == null) {
            sender.sendMessage(CC.color(args.length > 0 ? "&cThat player is not online." : "&cUsage: /tpall <player>"));
            return true;
        }
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(target)) continue;
            online.teleport(target.getLocation());
            count++;
        }
        sender.sendMessage(CC.color("&aTeleported &f" + count + " &aplayers to &f" + target.getName() + "&a."));
        return true;
    }

    private boolean blockedRequestServer(CommandSender sender) {
        if (!(plugin instanceof Game game) || game.getNetworkService().isOverworld()) return false;
        sender.sendMessage(CC.color(game.getNetworkService().isHub() ? "&cYou can only use teleport requests on overworld servers." : "&cYou cannot use teleport requests inside a dungeon."));
        return true;
    }
}
