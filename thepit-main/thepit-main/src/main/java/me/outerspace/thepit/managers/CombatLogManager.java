package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CombatLogManager implements Listener {
    private static final Set<String> BLOCKED_COMMANDS = Set.of("spawn","respawn","hub","lobby","trade","offer","prestige","server","switchserver","play");
    private final ThePitPlugin plugin;
    private final Map<UUID,CombatSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID,Long> trustedTeleports = new ConcurrentHashMap<>();
    private final Map<UUID,Long> trustedDisconnects = new ConcurrentHashMap<>();
    private final Map<UUID,String> results = new ConcurrentHashMap<>();
    private volatile boolean shuttingDown;
    private BukkitTask task;

    public CombatLogManager(ThePitPlugin plugin){this.plugin=plugin;}

    public void start(){if(task!=null)task.cancel();task=Bukkit.getScheduler().runTaskTimer(plugin,this::expire,10L,10L);}
    public void shutdown(){shuttingDown=true;trustAllDisconnects(30000L);if(task!=null)task.cancel();task=null;sessions.clear();trustedTeleports.clear();}
    public boolean isTagged(UUID uuid){CombatSession session=active(uuid);return session!=null;}
    public boolean isProcessingDeath(UUID uuid){CombatSession session=sessions.get(uuid);return session!=null&&session.processing.get()&&!session.processed.get();}
    public long remainingSeconds(UUID uuid){CombatSession session=active(uuid);return session==null?0L:Math.max(1L,(session.expiresAt-System.currentTimeMillis()+999L)/1000L);}
    public String display(UUID uuid){long seconds=remainingSeconds(uuid);return seconds<=0L?"":CC.color("&cCombat: &f"+seconds+"s");}
    public void clear(UUID uuid){sessions.remove(uuid);Player player=Bukkit.getPlayer(uuid);if(player!=null)plugin.getBuildScoreboardManager().update(player);}
    public void clearAllTrusted(){sessions.clear();for(Player player:Bukkit.getOnlinePlayers())plugin.getBuildScoreboardManager().update(player);}
    public void trustTeleport(Player player,long millis){if(player!=null)trustedTeleports.put(player.getUniqueId(),System.currentTimeMillis()+Math.max(1L,millis));}
    public void trustDisconnect(Player player,long millis){if(player!=null)trustedDisconnects.put(player.getUniqueId(),System.currentTimeMillis()+Math.max(1L,millis));}
    public void trustAllDisconnects(long millis){long until=System.currentTimeMillis()+Math.max(1L,millis);for(Player player:Bukkit.getOnlinePlayers())trustedDisconnects.put(player.getUniqueId(),until);}
    public boolean blocked(Player player){CombatSession session=active(player.getUniqueId());if(session==null)return false;player.sendMessage(CC.color("&cYou cannot do that while in combat! &7(&f"+remainingSeconds(player.getUniqueId())+"s&7)"));return true;}

    public List<String> status(Player player){CombatSession session=active(player.getUniqueId());List<String> lines=new ArrayList<>();lines.add(CC.color("&6Combat log status for &f"+player.getName()));lines.add(CC.color("&7Tagged: "+(session==null?"&cno":"&ayes")));lines.add(CC.color("&7Remaining: &f"+remainingSeconds(player.getUniqueId())+"s"));lines.add(CC.color("&7Last attacker: &f"+(session==null||session.attacker==null?"none":session.attacker)));lines.add(CC.color("&7Arena instance: &f"+(session==null?"none":session.instanceId)));lines.add(CC.color("&7Session: &f"+(session==null?"none":session.sessionId)));lines.add(CC.color("&7Pending death: &f"+(session!=null&&session.processing.get())));lines.add(CC.color("&7Trusted transfer: &f"+trusted(player.getUniqueId(),trustedDisconnects)));lines.add(CC.color("&7Last result: &f"+results.getOrDefault(player.getUniqueId(),"none")));return lines;}

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onDamage(EntityDamageByEntityEvent event){if(!(event.getEntity() instanceof Player victim)||event.getFinalDamage()<=0.0D)return;Player attacker=attacker(event.getDamager());if(attacker==null||attacker.equals(victim)||attacker.getGameMode()==GameMode.SPECTATOR||victim.getGameMode()==GameMode.SPECTATOR)return;if(attacker.hasMetadata("BuildFFATestDamage")||victim.hasMetadata("BuildFFATestDamage"))return;if(!plugin.getPerkManager().validCombat(attacker,victim))return;if(plugin.getArenaManager().isInCurrentSpawnProtection(attacker.getLocation())||plugin.getArenaManager().isInCurrentSpawnProtection(victim.getLocation()))return;tag(attacker,victim,event.getFinalDamage());}

    @EventHandler(priority=EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event){Player victim=event.getPlayer();CombatSession session=active(victim.getUniqueId());if(session==null||shuttingDown||trusted(victim.getUniqueId(),trustedDisconnects)||!plugin.getStatsManager().isProfileLoaded(victim.getUniqueId())){sessions.remove(victim.getUniqueId());return;}if(!session.processing.compareAndSet(false,true))return;long remaining=Math.max(1L,(session.expiresAt-System.currentTimeMillis()+999L)/1000L);Player killer=session.attacker==null?null:Bukkit.getPlayer(session.attacker);boolean credited=killer!=null&&killer.isOnline()&&!killer.equals(victim)&&sameInstance(killer,session);String policy=plugin.getMinorEventManager()!=null&&plugin.getMinorEventManager().activeEventId()!=null?"EVENT":"NORMAL";double health=victim.getHealth();if(health<=0.0D||victim.isDead()){session.processed.set(true);results.put(victim.getUniqueId(),"ALREADY_DEAD");return;}victim.setMetadata("BuildFFADeathCause",new FixedMetadataValue(plugin,"COMBAT_LOG"));if(credited)victim.damage(Math.max(1000.0D,health+victim.getMaxHealth()),killer);else victim.setHealth(0.0D);if(!victim.isDead()&&victim.getHealth()>0.0D){victim.removeMetadata("BuildFFADeathCause",plugin);session.processing.set(false);results.put(victim.getUniqueId(),"DEATH_CANCELLED");return;}session.processed.set(true);results.put(victim.getUniqueId(),"COMMITTED");plugin.getLogger().info("combat_log session="+session.sessionId+" victim="+victim.getUniqueId()+" killer="+(credited?killer.getUniqueId():"none")+" arena="+session.arenaId+" instance="+session.instanceId+" remaining="+remaining+" policy="+policy+" result=COMMITTED consequences=death_pipeline");}

    @EventHandler(priority=EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event){CombatSession session=sessions.remove(event.getEntity().getUniqueId());if(session!=null){session.processed.set(true);results.put(event.getEntity().getUniqueId(),session.processing.get()?"COMMITTED":"NORMAL_DEATH");}plugin.getBuildScoreboardManager().update(event.getEntity());}

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event){String raw=event.getMessage().substring(1).trim();if(raw.isEmpty())return;String command=raw.split("\\s+",2)[0].toLowerCase(Locale.ENGLISH);int colon=command.indexOf(':');if(colon>=0)command=command.substring(colon+1);if(BLOCKED_COMMANDS.contains(command)&&blocked(event.getPlayer()))event.setCancelled(true);}

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onTeleport(PlayerTeleportEvent event){if(plugin.getPlayerInventoryManager().isInitializationTeleport(event)){plugin.getPlayerInventoryManager().traceInitializationTeleport("CombatLogManager",event,"allowed exact JOIN_INITIALIZATION destination");return;}if(!isTagged(event.getPlayer().getUniqueId())||trusted(event.getPlayer().getUniqueId(),trustedTeleports)){plugin.getPlayerInventoryManager().traceInitializationTeleport("CombatLogManager",event,"not combat-blocked");return;}if(event.getCause()==PlayerTeleportEvent.TeleportCause.ENDER_PEARL){plugin.getPlayerInventoryManager().traceInitializationTeleport("CombatLogManager",event,"ender pearl allowed");return;}event.setCancelled(true);plugin.getPlayerInventoryManager().traceInitializationTeleport("CombatLogManager",event,"cancelled active combat-tag teleport");blocked(event.getPlayer());}

    @EventHandler
    public void onJoin(PlayerJoinEvent event){trustedDisconnects.remove(event.getPlayer().getUniqueId());trustedTeleports.remove(event.getPlayer().getUniqueId());sessions.remove(event.getPlayer().getUniqueId());}

    private void tag(Player attacker,Player victim,double damage){long now=System.currentTimeMillis(),duration=Math.max(1L,plugin.registry().perks().getLong("assists.combat-timeout-seconds",15L))*1000L;String arena=plugin.getArenaManager().getCurrentArena();String instance=arena+":"+attacker.getWorld().getUID();CombatSession attackerSession=renew(attacker.getUniqueId(),victim.getUniqueId(),arena,instance,now,duration);CombatSession victimSession=renew(victim.getUniqueId(),attacker.getUniqueId(),arena,instance,now,duration);attackerSession.damage.merge(victim.getUniqueId(),damage,Double::sum);victimSession.damage.merge(attacker.getUniqueId(),damage,Double::sum);plugin.getBuildScoreboardManager().update(attacker);plugin.getBuildScoreboardManager().update(victim);}
    private CombatSession renew(UUID player,UUID attacker,String arena,String instance,long now,long duration){CombatSession current=sessions.get(player);if(current==null||current.expiresAt<=now||!current.instanceId.equals(instance)||current.processed.get()){current=new CombatSession(UUID.randomUUID(),player,attacker,arena,instance,now,now+duration,now);sessions.put(player,current);}else{current.attacker=attacker;current.expiresAt=now+duration;current.lastDamageAt=now;}return current;}
    private CombatSession active(UUID uuid){CombatSession session=sessions.get(uuid);if(session==null)return null;if(session.expiresAt<=System.currentTimeMillis()||session.processed.get()){sessions.remove(uuid,session);return null;}return session;}
    private void expire(){long now=System.currentTimeMillis();for(Map.Entry<UUID,CombatSession> entry:sessions.entrySet())if(entry.getValue().expiresAt<=now&&!entry.getValue().processing.get()){sessions.remove(entry.getKey(),entry.getValue());Player player=Bukkit.getPlayer(entry.getKey());if(player!=null)plugin.getBuildScoreboardManager().update(player);}trustedTeleports.entrySet().removeIf(entry->entry.getValue()<=now);trustedDisconnects.entrySet().removeIf(entry->entry.getValue()<=now);}
    private boolean trusted(UUID uuid,Map<UUID,Long> values){Long until=values.get(uuid);if(until==null)return false;if(until<=System.currentTimeMillis()){values.remove(uuid,until);return false;}return true;}
    private boolean sameInstance(Player player,CombatSession session){return session.arenaId.equals(plugin.getArenaManager().getCurrentArena())&&session.instanceId.equals(session.arenaId+":"+player.getWorld().getUID());}
    private Player attacker(Entity entity){if(entity instanceof Player)return(Player)entity;if(entity instanceof Projectile projectile&&projectile.getShooter() instanceof Player)return(Player)projectile.getShooter();return null;}

    private static final class CombatSession{private final UUID sessionId;private final UUID player;private volatile UUID attacker;private final String arenaId;private final String instanceId;private final long startedAt;private volatile long expiresAt;private volatile long lastDamageAt;private final Map<UUID,Double> damage=new ConcurrentHashMap<>();private final AtomicBoolean processing=new AtomicBoolean();private final AtomicBoolean processed=new AtomicBoolean();private CombatSession(UUID sessionId,UUID player,UUID attacker,String arenaId,String instanceId,long startedAt,long expiresAt,long lastDamageAt){this.sessionId=sessionId;this.player=player;this.attacker=attacker;this.arenaId=arenaId;this.instanceId=instanceId;this.startedAt=startedAt;this.expiresAt=expiresAt;this.lastDamageAt=lastDamageAt;}}
}
