package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.SmpAlertService;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AlertsCommand implements CommandExecutor {
    private final SmpAlertService alerts;
    public AlertsCommand(SmpAlertService alerts) { this.alerts = alerts; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player) || !sender.hasPermission("fakevival.alerts.staff")) {
            sender.sendMessage(CC.color("&cYou do not have permission to use SMP alerts.")); return true;
        }
        boolean enabled = alerts.toggle(player);
        sender.sendMessage(CC.color(enabled ? "&aSMP alerts enabled." : "&cSMP alerts disabled."));
        return true;
    }
}
