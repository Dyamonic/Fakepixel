package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.MenuManager;
import club.abelix.games.fakevival.managers.AdventureGuildManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class QuestsCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final MenuManager menuManager;
    private final AdventureGuildManager guild;

    public QuestsCommand(JavaPlugin plugin, MenuManager menuManager,AdventureGuildManager guild) {
        this.plugin = plugin;
        this.menuManager = menuManager;
        this.guild=guild;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.quests")) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("adventure-guild.messages.no-permission", "&cYou do not have permission to use quests.")));
            return true;
        }

        guild.load(player);
        if(args.length>0&&args[0].equalsIgnoreCase("register")){guild.register(player);menuManager.openAdventureGuild(player);return true;}
        if(args.length>0&&args[0].equalsIgnoreCase("rankup")){if(!guild.rankUp(player))player.sendMessage(CC.color("&cYou do not meet the requirements yet."));menuManager.openAdventureGuild(player);return true;}
        if (args.length > 0 && args[0].equalsIgnoreCase("daily")) {
            menuManager.openAdventureGuildQuests(player, "daily", 0);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("weekly")) {
            menuManager.openAdventureGuildQuests(player, "weekly", 0);
            return true;
        }

        menuManager.openAdventureGuild(player);
        return true;
    }
}
