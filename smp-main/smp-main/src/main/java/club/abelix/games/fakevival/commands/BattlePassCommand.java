package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.BattlePassManager;
import club.abelix.games.fakevival.managers.MenuManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BattlePassCommand implements CommandExecutor {
    private final BattlePassManager battlePassManager;
    private final MenuManager menuManager;

    public BattlePassCommand(JavaPlugin plugin, BattlePassManager battlePassManager, MenuManager menuManager) {
        this.battlePassManager = battlePassManager;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(CC.color("&cOnly players can use battlepass."));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("premium")) {
            if (battlePassManager.buyPremium(player)) player.sendMessage(CC.color("&aYou bought the premium battlepass. Free rewards stay unlocked too."));
            else player.sendMessage(CC.color("&cYou already own premium, or you do not have enough Abelix Gold."));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("buy")) {
            int levels = 1;
            if (args.length >= 2) try { levels = Integer.parseInt(args[1]); } catch (Exception ignored) {}
            if (battlePassManager.buyLevels(player, levels)) player.sendMessage(CC.color("&aBought &f" + levels + " &abattlepass level(s)."));
            else player.sendMessage(CC.color("&cYou do not have enough Abelix Gold, or Abelix gold taking is not supported by the hook."));
            return true;
        }
        menuManager.openBattlePass(player);
        return true;
    }
}
