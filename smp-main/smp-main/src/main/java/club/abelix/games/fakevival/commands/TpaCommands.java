package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.TeleportRequestManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.managers.TeleportRequestManager.TeleportRequest;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TpaCommands implements CommandExecutor {
    private final JavaPlugin plugin;
    private final TeleportRequestManager requestManager;

    public TpaCommands(JavaPlugin plugin, TeleportRequestManager requestManager) {
        this.plugin = plugin;
        this.requestManager = requestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!sender.hasPermission("fakevival.command.tpa")) return CommandUtil.noPermission(plugin, sender);
        if (plugin instanceof Game game && !game.getNetworkService().isOverworld()) {
            player.sendMessage(CC.color(game.getNetworkService().isHub() ? "&cYou can only use teleport requests on overworld servers." : "&cYou cannot use teleport requests inside a dungeon."));
            return true;
        }

        return switch (command.getName().toLowerCase()) {
            case "tpa" -> request(player, args, false);
            case "tpahere" -> request(player, args, true);
            case "tpaccept" -> accept(player, args);
            case "tpacancel" -> cancel(player);
            default -> true;
        };
    }

    private boolean request(Player requester, String[] args, boolean here) {
        if (args.length < 1) {
            requester.sendMessage(CC.color("&cUsage: /" + (here ? "tpahere" : "tpa") + " <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            requester.sendMessage(CC.color("&cThat player is not online."));
            return true;
        }
        if (target.equals(requester)) {
            requester.sendMessage(CC.color("&cYou cannot send a teleport request to yourself."));
            return true;
        }
        requestManager.request(requester, target, here);
        requester.sendMessage(CC.color("&aTeleport request sent to &f" + target.getName() + "&a."));
        if (here) {
            target.sendMessage(CC.color("&f" + requester.getName() + " &awants you to teleport to them. &e/tpaccept " + requester.getName()));
        } else {
            target.sendMessage(CC.color("&f" + requester.getName() + " &awants to teleport to you. &e/tpaccept " + requester.getName()));
        }
        return true;
    }

    private boolean accept(Player target, String[] args) {
        TeleportRequest request = requestManager.getRequest(target, args.length > 0 ? args[0] : null);
        if (request == null) {
            target.sendMessage(CC.color("&cYou do not have a pending teleport request."));
            return true;
        }
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null) {
            target.sendMessage(CC.color("&cThat player is no longer online."));
            requestManager.remove(request);
            return true;
        }
        if (request.here()) {
            target.teleport(requester.getLocation());
            target.sendMessage(CC.color("&aTeleport request accepted."));
            requester.sendMessage(CC.color("&a" + target.getName() + " accepted your teleport request."));
        } else {
            requester.teleport(target.getLocation());
            requester.sendMessage(CC.color("&aTeleport request accepted."));
            target.sendMessage(CC.color("&aAccepted &f" + requester.getName() + "&a's teleport request."));
        }
        requestManager.remove(request);
        return true;
    }

    private boolean cancel(Player requester) {
        if (!requestManager.cancelOutgoing(requester)) {
            requester.sendMessage(CC.color("&cYou do not have any outgoing teleport requests."));
            return true;
        }
        requester.sendMessage(CC.color("&aCancelled your outgoing teleport request."));
        return true;
    }
}
