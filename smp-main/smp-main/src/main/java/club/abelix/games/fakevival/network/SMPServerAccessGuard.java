package club.abelix.games.fakevival.network;

import club.abelix.games.fakevival.util.CC;
import org.bukkit.command.CommandSender;

public final class SMPServerAccessGuard {
    public enum ServerGroupRule { OVERWORLD, HUB, DUNGEON, HUB_OR_OVERWORLD, NOT_DUNGEON }
    private final SMPNetworkService router;
    public SMPServerAccessGuard(SMPNetworkService router){this.router=router;}
    public boolean isAllowed(ServerGroupRule rule){return switch(rule){case OVERWORLD->router.isOverworld();case HUB->router.isHub();case DUNGEON->router.isDungeon();case HUB_OR_OVERWORLD->router.isHub()||router.isOverworld();case NOT_DUNGEON->!router.isDungeon();};}
    public boolean requireOverworld(CommandSender sender,String message){return require(sender,ServerGroupRule.OVERWORLD,message);}
    public boolean requireHub(CommandSender sender,String message){return require(sender,ServerGroupRule.HUB,message);}
    public boolean requireDungeon(CommandSender sender,String message){return require(sender,ServerGroupRule.DUNGEON,message);}
    public boolean requireHubOrOverworld(CommandSender sender,String message){return require(sender,ServerGroupRule.HUB_OR_OVERWORLD,message);}
    public boolean denyDungeon(CommandSender sender,String message){return require(sender,ServerGroupRule.NOT_DUNGEON,message);}
    private boolean require(CommandSender sender,ServerGroupRule rule,String message){if(isAllowed(rule))return true;if(sender!=null)sender.sendMessage(CC.color(message));return false;}
}
