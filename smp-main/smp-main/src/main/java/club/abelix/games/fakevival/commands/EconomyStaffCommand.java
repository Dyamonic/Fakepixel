package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.EconomyAuditManager;
import club.abelix.games.fakevival.managers.EconomyManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public final class EconomyStaffCommand implements CommandExecutor {
    private static final int HISTORY_LIMIT = 10;

    private final JavaPlugin plugin;
    private final EconomyAuditManager auditManager;
    private final EconomyManager economyManager;

    public EconomyStaffCommand(JavaPlugin plugin, EconomyAuditManager auditManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.auditManager = auditManager;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.staff.economy")) {
            sender.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        String name = command.getName().toLowerCase();
        if (name.equals("rollbacktrade")) return rollback(sender, args);
        if (name.equals("freezeeco")) return freeze(sender, args);
        return history(sender, name, args);
    }

    private boolean history(CommandSender sender, String command, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /" + command + " <player>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = target.getUniqueId();
        List<String> rows = switch (command) {
            case "econhistory" -> auditManager.economyHistory(uuid, HISTORY_LIMIT);
            case "tradehistory" -> auditManager.tradeHistory(uuid, HISTORY_LIMIT);
            case "ahhistory" -> auditManager.auctionHistory(uuid, HISTORY_LIMIT);
            case "itemhistory" -> auditManager.itemHistory(uuid, HISTORY_LIMIT);
            default -> List.of();
        };

        sender.sendMessage(CC.color("&6" + command + " &7for &f" + auditManager.playerName(uuid)));
        if (rows.isEmpty()) {
            sender.sendMessage(CC.color("&7No history found."));
            return true;
        }
        for (String row : rows) sender.sendMessage(CC.color(row));
        return true;
    }

    private boolean rollback(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /rollbacktrade <tradeid>"));
            return true;
        }

        EconomyAuditManager.RollbackResult result = auditManager.rollbackTrade(args[0], economyManager);
        switch (result) {
            case SUCCESS -> sender.sendMessage(CC.color("&aRolled back trade &f" + args[0] + "&a."));
            case NOT_FOUND -> sender.sendMessage(CC.color("&cTrade ID not found."));
            case ALREADY_ROLLED_BACK -> sender.sendMessage(CC.color("&cThat trade was already rolled back."));
            case PLAYERS_OFFLINE -> sender.sendMessage(CC.color("&cBoth trade players must be online to rollback item transfers."));
            case INSUFFICIENT_COINS -> sender.sendMessage(CC.color("&cRollback failed. A player no longer has the coins needed."));
            case MISSING_ITEMS -> sender.sendMessage(CC.color("&cRollback failed. A player no longer has the recorded items."));
        }
        return true;
    }

    private boolean freeze(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /freezeeco <player>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = target.getUniqueId();
        boolean frozen = !auditManager.isFrozen(uuid);
        auditManager.setFrozen(uuid, frozen);
        auditManager.logEconomy(uuid, frozen ? "freeze" : "unfreeze", 0.0D, economyManager.getCoins(uuid), "Changed by " + sender.getName());
        sender.sendMessage(CC.color((frozen ? "&cFrozen" : "&aUnfrozen") + " &f" + auditManager.playerName(uuid) + "&7's economy."));
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(CC.color(frozen
                    ? "&cYour economy has been frozen by staff."
                    : "&aYour economy freeze has been lifted."));
        }
        return true;
    }
}
