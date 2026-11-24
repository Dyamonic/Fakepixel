package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SetDungeonLobbyCommand implements CommandExecutor {
    private final Game game;
    public SetDungeonLobbyCommand(Game game){this.game=game;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player))return CommandUtil.playersOnly(sender);
        if(!sender.hasPermission("fakevival.dungeon.admin")&&!sender.hasPermission("fakevival.admin.dungeon"))return CommandUtil.noPermission(game,sender);
        if(!game.getNetworkService().isDungeon()){sender.sendMessage(CC.color("&cYou can only set the dungeon lobby on dungeon servers."));return true;}
        if(!player.getWorld().getName().equalsIgnoreCase("lobby")){sender.sendMessage(CC.color("&cThe dungeon lobby spawn must be set in the lobby world."));return true;}
        game.getNetworkService().saveServerSpawn(player,"dungeon_lobby",success->sender.sendMessage(CC.color(success?"&aDungeon lobby spawn set.":"&cFailed to save the dungeon lobby spawn. Check console.")));
        return true;
    }
}
