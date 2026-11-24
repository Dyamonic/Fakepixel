package net.fakepixel.housing.scoreboard;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.metadata.HouseMetaCache;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import java.text.SimpleDateFormat;
import java.util.*;

public final class LobbyScoreboard implements ScoreboardProvider, Listener {
    private final HousingPlugin plugin;
    public LobbyScoreboard(HousingPlugin plugin){this.plugin=plugin;}
    public ScoreboardView create(Player player,House ignored){
        HouseMetaCache cache=plugin.getHouseMetaCache();int owned=cache==null?0:cache.getByOwner(player.getUniqueId()).size();
        int cookies=cache==null?0:cache.totalCookies(player.getUniqueId()),weekly=cache==null?0:cache.totalWeeklyCookies(player.getUniqueId());int gold=plugin.getAbelixHook()==null?0:plugin.getAbelixHook().getGold(player);
        String prefix=plugin.getAbelixHook()==null?"":plugin.getAbelixHook().getPrefix(player);if(prefix==null)prefix="";
        List<String> lines=new ArrayList<>();lines.add("&7"+new SimpleDateFormat("M/d/yy h:mm a").format(new Date()));lines.add("");
        lines.add(prefix+"&f"+player.getName());lines.add("&fCookies: &6"+cookies);lines.add("&fWeekly: &6"+weekly);
        lines.add("&fHouses: &a"+owned);lines.add("&fGold: &6"+gold);lines.add("");
        lines.add("&e"+plugin.getStaticConfig().getString("settings.server-address","mc.fakepixel.me"));
        return new ScoreboardView("&6&lHOUSING",lines);
    }
    public List<String> lobbyLines(Player player){return create(player,null).getLines();}
    @EventHandler public void onJoin(PlayerJoinEvent event){apply(event.getPlayer(),7L);}
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event){apply(event.getPlayer(),1L);}
    @EventHandler public void onTeleport(PlayerTeleportEvent event){apply(event.getPlayer(),1L);}
    private void apply(Player player,long delay){if(plugin.getCurrentServer()==null||!plugin.getCurrentServer().isLobby())return;Bukkit.getScheduler().runTaskLater(plugin,()->{if(player.isOnline()&&plugin.getHousingScoreboard()!=null)plugin.getHousingScoreboard().update(player);},delay);}
}
