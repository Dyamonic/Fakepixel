package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.network.SMPServerType;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WarpCommand implements CommandExecutor {
    private final Game game;
    public WarpCommand(Game game){this.game=game;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player))return CommandUtil.playersOnly(sender);
        if(args.length!=1){player.sendMessage(CC.color("&cUsage: /warp <hub|overworld|dungeon|dungeons>"));return true;}
        SMPServerType type=switch(args[0].toLowerCase()){case "hub"->SMPServerType.HUB;case "overworld","survival"->SMPServerType.OVERWORLD;case "dungeon","dungeons"->SMPServerType.DUNGEON;default->SMPServerType.UNKNOWN;};
        if(type==SMPServerType.UNKNOWN){player.sendMessage(CC.color("&cUnknown server warp. Use hub, overworld, or dungeon."));return true;}
        game.getNetworkService().travelWarp(player,type,type==SMPServerType.DUNGEON?"dungeon":type.name().toLowerCase());
        return true;
    }
}
