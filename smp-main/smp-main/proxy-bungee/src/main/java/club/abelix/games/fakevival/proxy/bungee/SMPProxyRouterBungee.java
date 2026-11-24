package club.abelix.games.fakevival.proxy.bungee;

import club.abelix.games.fakevival.proxy.RouterCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public final class SMPProxyRouterBungee extends Plugin {
    private RouterCore router;
    @Override public void onEnable() {
        router = new RouterCore(new BungeeProxyPlatformAdapter(ProxyServer.getInstance()));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new JoinCommand());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new StatusCommand());
        getLogger().info("SMPProxyRouter enabled. MariaDB routing is active.");
    }
    private final class JoinCommand extends Command {
        private JoinCommand(){super("smpjoin", null);}
        @Override public void execute(CommandSender sender,String[] args){
            if(!(sender instanceof ProxiedPlayer player)){sender.sendMessage(new TextComponent("Players only."));return;}
            if(args.length<1){player.sendMessage(new TextComponent("§cUsage: /smpjoin <hub|overworld|dungeon> [dungeonId]"));return;}
            String type=switch(args[0].toLowerCase()){case "hub"->"HUB";case "overworld","world","survival"->"OVERWORLD";case "dungeon"->"DUNGEON";default->null;};
            if(type==null){player.sendMessage(new TextComponent("§cUnknown server group."));return;}
            String dungeonId=args.length>=2?args[1]:"unknown";
            ProxyServer.getInstance().getScheduler().runAsync(SMPProxyRouterBungee.this,()->{
                try{if(!router.route(player.getUniqueId(),type,dungeonId))player.sendMessage(new TextComponent("§cNo available "+type.toLowerCase()+" server was found."));}
                catch(Exception e){getLogger().warning("Route failed: "+e.getMessage());player.sendMessage(new TextComponent("§cCould not route you right now."));}
            });
        }
    }
    private final class StatusCommand extends Command {
        private StatusCommand(){super("smprouter","fakevival.proxy.admin","smprouterstatus");}
        @Override public void execute(CommandSender sender,String[] args){
            if(!sender.hasPermission("fakevival.proxy.admin")){sender.sendMessage(new TextComponent("§cNo permission."));return;}
            ProxyServer.getInstance().getScheduler().runAsync(SMPProxyRouterBungee.this,()->{
                try{for(String type:new String[]{"HUB","OVERWORLD","DUNGEON"}){sender.sendMessage(new TextComponent("§6"+type+" Servers:"));for(var s:router.database().available(type))sender.sendMessage(new TextComponent("§f"+s.id()+" §a"+s.status()+" §e"+s.online()+"/"+s.max()+" §bTPS "+String.format("%.1f",s.tps())+(type.equals("DUNGEON")?" §dactiveRuns="+s.activeRuns():"")));}}
                catch(Exception e){sender.sendMessage(new TextComponent("§cCould not load router status."));}
            });
        }
    }
}
