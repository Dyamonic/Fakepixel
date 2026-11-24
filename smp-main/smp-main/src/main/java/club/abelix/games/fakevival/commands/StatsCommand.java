package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.PlayerLevelManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StatsCommand implements CommandExecutor {
    private final PlayerLevelManager playerLevelManager;

    public StatsCommand(PlayerLevelManager playerLevelManager) {
        this.playerLevelManager = playerLevelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        PlayerLevelManager.PlayerStats stats = playerLevelManager.getStats(player);
        player.sendMessage(CC.color("&6Stats &8> &fLevel: &e" + playerLevelManager.getLevel(player)));
        player.sendMessage(CC.color("&cHP: &f" + whole(stats.hp()) + " &8| &bMana: &f" + whole(stats.mana())));
        player.sendMessage(CC.color("&cDamage: &f+" + one(stats.damage()) + " &8| &9Defense: &f" + one(stats.defense()) + " &8| &aAgility: &f" + one(stats.agility())));
        return true;
    }

    private String whole(double value) {
        return String.valueOf((int) Math.round(value));
    }

    private String one(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }
}
