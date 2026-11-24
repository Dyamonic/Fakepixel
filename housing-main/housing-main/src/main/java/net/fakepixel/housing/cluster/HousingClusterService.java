package net.fakepixel.housing.cluster;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;

public final class HousingClusterService implements Listener {
    private final HousingPlugin plugin; private final HousingServerDefinition server; private final HousingClusterRepository repository;
    private final Map<String,Long> emptySince=new HashMap<>(); private BukkitTask heartbeat,maintenance,browse;
    private final Map<String,Long> leaseExpiresAt=new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<String> ownershipLost=java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String,Boolean>());
    private final Set<String> evacuatingHouses=java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String,Boolean>());
    private final Set<UUID> routingPlayers=java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<UUID,Boolean>());
    private final Set<UUID> loadingPlayers=java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<UUID,Boolean>());
    private final Set<UUID> previousInvisible=java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<UUID,Boolean>());
    private final Set<UUID> previousBlind=java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<UUID,Boolean>());
    public HousingClusterService(HousingPlugin plugin,HousingServerDefinition server,HousingClusterRepository repository){this.plugin=plugin;this.server=server;this.repository=repository;}
    public void start(){
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin,"BungeeCord");
        Bukkit.getPluginManager().registerEvents(this,plugin);
        if(server.isInstance()) heartbeat=Bukkit.getScheduler().runTaskTimer(plugin,()->{final int players=Bukkit.getOnlinePlayers().size();final Collection<String> ids=loadedIds();Bukkit.getScheduler().runTaskAsynchronously(plugin,()->heartbeat(players,ids));},1L,HousingPerformanceRegistry.HEARTBEAT_SECONDS*20L);
        if(server.isInstance()) maintenance=Bukkit.getScheduler().runTaskTimer(plugin,this::maintainEmptyHouses,20L,20L);
        if(server.isLobby()) browse=Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,()->plugin.getHouseManager().refreshBrowseCache(),1L,HousingPerformanceRegistry.BROWSE_CACHE_REFRESH_SECONDS*20L);
        plugin.getLogger().info("Housing server role="+server.getRole()+" id="+server.getId());
    }
    public boolean ensureLocal(Player player,House house){return ensureLocal(player,house,PendingRouteAction.LOAD_HOUSE);}
    public boolean ensureLocal(Player player,House house,PendingRouteAction action){
        if(server.isLobby()){return routePlayer(player,house,action);}
        try{
            HouseRoute current=repository.find(id(house),System.currentTimeMillis());
            if(current==null && loadedIds().size()>=HousingPerformanceRegistry.MAX_LOADED_HOUSES){player.sendMessage(plugin.color("&cThis Housing instance is at capacity."));return false;}
            HouseRoute route=repository.route(id(house),server,System.currentTimeMillis());
            if(route==null){unavailable(player);return false;}
            if(server.getId().equals(route.getInstanceId())){confirmLease(id(house));return true;}
            if(!isRegisteredInstance(route.getServerName())){plugin.getLogger().warning("Refusing to route house "+id(house)+" to unregistered instance id "+route.getServerName());unavailable(player);return false;}
            connect(player,route.getServerName());return false;
        }catch(Exception ex){plugin.getLogger().warning("Could not acquire house lease: "+ex.getMessage());player.sendMessage(plugin.color("&cHousing routing is temporarily unavailable."));return false;}
    }
    private boolean routePlayer(Player player,House house,PendingRouteAction action){try{HouseRoute route=repository.route(id(house),server,System.currentTimeMillis());if(route==null){unavailable(player);return false;}if(!isRegisteredInstance(route.getServerName())){plugin.getLogger().warning("Refusing to route house "+id(house)+" to unregistered instance id "+route.getServerName());unavailable(player);return false;}repository.savePendingRoute(player.getUniqueId(),action,id(house),route.getServerName(),System.currentTimeMillis());plugin.getLogger().info("Housing lobby routing "+player.getName()+" action="+action+" house="+id(house)+" target="+route.getServerName());connect(player,route.getServerName());}catch(Exception ex){plugin.getLogger().warning("Could not route house: "+ex.getMessage());unavailable(player);}return false;}
    private boolean isRegisteredInstance(String serverId){HousingServerDefinition target=HousingServerRegistry.get(serverId);return target!=null&&target.isInstance();}
    private void unavailable(Player player){player.sendMessage(plugin.color("&cNo housing instances are available right now. Please try again later."));}
    private boolean connect(Player player,String targetServer)throws Exception{if(HousingServerRegistry.get(targetServer)==null)throw new IllegalArgumentException("Unregistered proxy server id "+targetServer);if(server.getId().equals(targetServer))return false;if(!routingPlayers.add(player.getUniqueId()))return false;try{plugin.getLogger().info("Sending BungeeCord Connect plugin message using proxy server id "+targetServer+" for "+player.getName());ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeUTF("Connect");out.writeUTF(targetServer);player.sendPluginMessage(plugin,"BungeeCord",bytes.toByteArray());return true;}catch(Exception ex){routingPlayers.remove(player.getUniqueId());throw ex;}}
    @EventHandler(priority=EventPriority.LOWEST)
    public void onInstanceJoin(PlayerJoinEvent event){
        if(!server.isInstance())return;
        beginLoading(event.getPlayer());
        final UUID playerId=event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{final PendingHouseRoute pending=repository.findPendingRoute(playerId,System.currentTimeMillis());final House routedHouse=pending!=null&&server.getId().equals(pending.getTargetServer())?plugin.getHousingRepository().loadHouse(pending.getHouseId()):null;Bukkit.getScheduler().runTask(plugin,()->completeJoin(playerId,pending,routedHouse));}catch(Exception ex){plugin.getLogger().warning("Could not read pending Housing route for "+playerId+": "+ex.getMessage());Bukkit.getScheduler().runTask(plugin,()->returnToLobby(playerId,"Pending Housing route lookup failed."));}});
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onMove(PlayerMoveEvent event){if(!loadingPlayers.contains(event.getPlayer().getUniqueId())||event.getTo()==null)return;if(event.getFrom().getX()!=event.getTo().getX()||event.getFrom().getY()!=event.getTo().getY()||event.getFrom().getZ()!=event.getTo().getZ())event.setTo(event.getFrom());}
    @EventHandler public void onQuit(PlayerQuitEvent event){UUID id=event.getPlayer().getUniqueId();routingPlayers.remove(id);loadingPlayers.remove(id);previousInvisible.remove(id);previousBlind.remove(id);}
    public boolean isLoadingHouse(Player player){return player!=null&&loadingPlayers.contains(player.getUniqueId());}
    private void beginLoading(Player player){UUID id=player.getUniqueId();if(!loadingPlayers.add(id))return;if(player.hasPotionEffect(PotionEffectType.INVISIBILITY))previousInvisible.add(id);if(player.hasPotionEffect(PotionEffectType.BLINDNESS))previousBlind.add(id);player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,20*120,1),true);player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,20*120,1),true);player.getInventory().clear();player.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);player.closeInventory();player.sendMessage(plugin.color("&eLoading your house..."));if(plugin.getHousingScoreboard()!=null)plugin.getHousingScoreboard().update(player);}
    private void finishLoading(Player player){UUID id=player.getUniqueId();loadingPlayers.remove(id);if(!previousInvisible.remove(id))player.removePotionEffect(PotionEffectType.INVISIBILITY);if(!previousBlind.remove(id))player.removePotionEffect(PotionEffectType.BLINDNESS);if(plugin.getHousingScoreboard()!=null)plugin.getHousingScoreboard().update(player);}
    private void completeJoin(UUID playerId,PendingHouseRoute pending,House routedHouse){
        Player player=Bukkit.getPlayer(playerId);if(player==null||!player.isOnline())return;
        if(pending==null){Bukkit.getScheduler().runTaskLater(plugin,()->sendPlayerToLobby(player,"No pending Housing route was found.","&cYou were not routed to a house, returning you to Housing."),7L);return;}
        if(!server.getId().equals(pending.getTargetServer())){returnToLobby(playerId,"Pending Housing route targets "+pending.getTargetServer()+", not "+server.getId()+".");return;}
        House house=routedHouse==null?plugin.getHouseManager().getHouseById(pending.getHouseId()):routedHouse;
        if(house==null){returnToLobby(playerId,"House metadata "+pending.getHouseId()+" was not found.");return;}
        plugin.getHouseManager().registerHouseMetadata(house);
        try{HouseRoute lease=repository.route(pending.getHouseId(),server,System.currentTimeMillis());if(lease==null||!server.getId().equals(lease.getInstanceId())){if(lease!=null&&isRegisteredInstance(lease.getServerName())){repository.savePendingRoute(playerId,pending.getAction(),pending.getHouseId(),lease.getServerName(),System.currentTimeMillis());connect(player,lease.getServerName());}else returnToLobby(playerId,"Could not acquire the house lock.");return;}confirmLease(pending.getHouseId());plugin.getLogger().info("Consuming pending Housing route player="+player.getName()+" action="+pending.getAction()+" house="+pending.getHouseId()+" server="+server.getId());if(!plugin.getHouseManager().completePendingRoute(player,house,pending.getAction()))throw new IllegalStateException("House teleport did not complete");repository.markHouseLoaded(pending.getHouseId(),server.getId(),System.currentTimeMillis());repository.deletePendingRoute(playerId,server.getId());finishLoading(player);}catch(Exception ex){plugin.getLogger().warning("Could not complete pending Housing route for "+player.getName()+": "+ex.getMessage());returnToLobby(playerId,"Could not load the routed house.");}
    }
    private void returnToLobby(UUID playerId,String reason){Player player=Bukkit.getPlayer(playerId);if(player==null||!player.isOnline()||routingPlayers.contains(playerId)||server.isLobby())return;plugin.getLogger().warning("Returning "+player.getName()+" to housing: "+reason);try{if(connect(player,HousingServerRegistry.lobby().getId()))player.sendMessage(plugin.color("&cCould not load your house. Returning you to Housing."));}catch(Exception ex){routingPlayers.remove(playerId);player.kickPlayer(plugin.color("&cHousing routing is unavailable. Please reconnect."));}}
    public boolean sendPlayerToLobby(Player player,String reason,String message){if(player==null||!player.isOnline())return false;if(server.isLobby()){if(message!=null&&!message.isEmpty())player.sendMessage(plugin.color("&eYou are already in Housing."));return false;}if(routingPlayers.contains(player.getUniqueId()))return false;plugin.getLogger().info("Sending player back to housing player="+player.getName()+" current_server="+server.getId()+" reason="+reason);try{boolean sent=connect(player,HousingServerRegistry.lobby().getId());if(sent&&message!=null&&!message.isEmpty())player.sendMessage(plugin.color(message));return sent;}catch(Exception ex){routingPlayers.remove(player.getUniqueId());plugin.getLogger().warning("Could not send "+player.getName()+" to housing: "+ex.getMessage());return false;}}
    public boolean acquireHouseLease(House house){if(!server.isInstance()||house==null)return false;try{HouseRoute route=repository.route(id(house),server,System.currentTimeMillis());if(route==null||!server.getId().equals(route.getInstanceId()))return false;confirmLease(id(house));return true;}catch(Exception ex){plugin.getLogger().warning("Could not acquire maintenance lease house_id="+id(house)+" server_id="+server.getId()+": "+ex.getMessage());return false;}}
    public void releaseHouseLease(House house){if(house==null)return;String houseId=id(house);try{repository.release(houseId,server.getId());}catch(Exception ex){plugin.getLogger().warning("Could not release house lease house_id="+houseId+" server_id="+server.getId()+": "+ex.getMessage());}leaseExpiresAt.remove(houseId);ownershipLost.remove(houseId);evacuatingHouses.remove(houseId);}
    private void heartbeat(int players,Collection<String> loaded){long now=System.currentTimeMillis();try{repository.heartbeat(server,players,loaded,now);}catch(Exception ex){plugin.getLogger().warning("Housing instance heartbeat failed server_id="+server.getId()+" now="+now+": "+ex.getMessage());}for(String houseId:loaded)refreshLease(houseId,now);}
    private void confirmLease(String houseId)throws Exception{HouseLease lease=repository.refreshLease(houseId,server.getId(),System.currentTimeMillis());if(!server.getId().equals(lease.getLockedBy()))throw new IllegalStateException("House lock is owned by "+lease.getLockedBy());leaseExpiresAt.put(houseId,lease.getExpiresAt());ownershipLost.remove(houseId);evacuatingHouses.remove(houseId);}
    private void refreshLease(String houseId,long now){try{HouseLease lease=repository.refreshLease(houseId,server.getId(),now);if(server.getId().equals(lease.getLockedBy())){leaseExpiresAt.put(houseId,lease.getExpiresAt());ownershipLost.remove(houseId);}else{ownershipLost.add(houseId);plugin.getLogger().severe("Housing lease refresh rejected house_id="+houseId+" server_id="+server.getId()+" locked_by="+lease.getLockedBy()+" expires_at="+lease.getExpiresAt()+" now="+now);}}catch(Exception ex){HouseLease diagnostic=null;try{diagnostic=repository.getLease(houseId);}catch(Exception ignored){}String lockedBy=diagnostic==null?"unknown":diagnostic.getLockedBy();Long known=leaseExpiresAt.get(houseId);long expires=diagnostic==null?(known==null?0L:known):diagnostic.getExpiresAt();if(diagnostic!=null&&server.getId().equals(lockedBy)){leaseExpiresAt.put(houseId,expires);}else if(diagnostic!=null){ownershipLost.add(houseId);}plugin.getLogger().warning("Housing lease refresh failed house_id="+houseId+" server_id="+server.getId()+" locked_by="+lockedBy+" expires_at="+expires+" now="+now+": "+ex.getMessage());}}
    private Collection<String> loadedIds(){List<String> ids=new ArrayList<>();if(plugin.getHouseManager()!=null)for(House h:plugin.getHouseManager().getHouses())if(Bukkit.getWorld(h.getWorldName())!=null)ids.add(id(h));return ids;}
    private void maintainEmptyHouses(){long now=System.currentTimeMillis();for(House house:plugin.getHouseManager().getHouses()){String houseId=id(house);World world=Bukkit.getWorld(house.getWorldName());if(world==null){emptySince.remove(house.getWorldName());leaseExpiresAt.remove(houseId);ownershipLost.remove(houseId);evacuatingHouses.remove(houseId);continue;}Long expires=leaseExpiresAt.get(houseId);if(ownershipLost.contains(houseId)||(expires!=null&&now>=expires)){evacuateAndUnload(house,world);continue;}if(!world.getPlayers().isEmpty()){emptySince.remove(house.getWorldName());continue;}Long since=emptySince.get(house.getWorldName());if(since==null){emptySince.put(house.getWorldName(),now);continue;}if(now-since<HousingPerformanceRegistry.EMPTY_HOUSE_UNLOAD_SECONDS*1000L)continue;saveAndUnload(house,world);}}
    private void evacuateAndUnload(House house,World world){String houseId=id(house);boolean first=evacuatingHouses.add(houseId);if(first)plugin.getLogger().severe("Housing lease lost; unloading house_id="+houseId+" server_id="+server.getId()+" expires_at="+leaseExpiresAt.get(houseId)+" now="+System.currentTimeMillis());for(Player player:new ArrayList<>(world.getPlayers())){if(routingPlayers.contains(player.getUniqueId()))continue;try{if(connect(player,HousingServerRegistry.lobby().getId()))player.sendMessage(plugin.color("&cThis Housing instance lost its lease! Returning you to the lobby."));}catch(Exception ex){plugin.getLogger().warning("Could not return "+player.getName()+" to housing: "+ex.getMessage());}}saveAndUnload(house,world);}
    private void saveAndUnload(House house,World world){String houseId=id(house);try{plugin.getHousingRepository().saveHouse(house);world.save();if(Bukkit.unloadWorld(world,true)){repository.release(houseId,server.getId());emptySince.remove(house.getWorldName());leaseExpiresAt.remove(houseId);ownershipLost.remove(houseId);evacuatingHouses.remove(houseId);}}catch(Exception ex){plugin.getLogger().warning("Could not save/unload "+house.getWorldName()+": "+ex.getMessage());emptySince.put(house.getWorldName(),System.currentTimeMillis());}}
    public void stop(){if(heartbeat!=null)heartbeat.cancel();if(maintenance!=null)maintenance.cancel();if(browse!=null)browse.cancel();if(server.isInstance())try{heartbeat(Bukkit.getOnlinePlayers().size(),loadedIds());repository.offline(server.getId());}catch(Exception ex){plugin.getLogger().warning("Could not mark Housing instance offline: "+ex.getMessage());}}
    public static String id(House house){return house.getOwner()+"#"+house.getPlotId();}
}
