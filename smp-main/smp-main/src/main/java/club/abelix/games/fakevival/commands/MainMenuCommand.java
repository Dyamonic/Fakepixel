package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.MenuManager;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MainMenuCommand implements CommandExecutor {
    private final MenuManager menuManager;

    public MainMenuCommand(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        menuManager.openMainMenu(player);
        return true;
    }
}
