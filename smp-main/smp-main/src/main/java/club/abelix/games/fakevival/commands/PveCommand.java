package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.PveManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PveCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final PveManager pveManager;

    public PveCommand(JavaPlugin plugin, PveManager pveManager) {
        this.plugin = plugin;
        this.pveManager = pveManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.command.pve")) return CommandUtil.noPermission(plugin, sender);
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if(plugin instanceof Game game&&!game.getAccessGuard().requireHubOrOverworld(sender,"&cPvE settings cannot be changed inside dungeon servers."))return true;

        if (pveManager.isCombatTagged(player)) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.pve-combat-tagged", "&cYou cannot toggle PvE while combat tagged. Wait &f%seconds%s&c.")
                    .replace("%seconds%", String.valueOf(pveManager.getCombatTagRemainingSeconds(player)))));
            return true;
        }

        boolean enabled = pveManager.togglePve(player);
        String message = enabled
                ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.pve-enabled", "&aPvE mode enabled. Players cannot damage you, and you cannot damage players.")
                : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.pve-disabled", "&cPvE mode disabled. You can now PvP in PvP-enabled worlds.");
        player.sendMessage(CC.color(message));
        return true;
    }
}
