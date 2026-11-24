package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.MenuManager;
import club.abelix.games.fakevival.managers.SkillManager;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CC;

public final class SkillsCommand implements CommandExecutor {
    private final MenuManager menuManager;
    private final JavaPlugin plugin;

    public SkillsCommand(JavaPlugin plugin, SkillManager skillManager, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if(plugin instanceof Game game&&!game.getNetworkService().isOverworld()){player.sendMessage(CC.color(game.getNetworkService().isDungeon()?"&cQuests and jobs cannot be used inside dungeons.":"&cQuests and jobs can only be used on overworld servers."));return true;}
        menuManager.openSkills(player);
        return true;
    }
}
