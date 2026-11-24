package club.abelix.games.fakevival.network;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.managers.PlayerDataSyncManager;
import club.abelix.games.fakevival.managers.HomeManager;
import club.abelix.games.fakevival.managers.DungeonManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SMPNetworkService implements Listener {
    private final Game plugin;
    private final DataManager dataManager;
    private final PlayerDataSyncManager playerDataSync;
    private final SMPServerIdentity identity;
    private final SMPNetworkRepository repository;
    private BukkitTask heartbeatTask;
    private BukkitTask presenceTask;
    private volatile SMPServerStatus status = SMPServerStatus.STARTING;
    private final AtomicInteger overworldFallbackIndex = new AtomicInteger();

    public SMPNetworkService(Game plugin, DataManager dataManager, PlayerDataSyncManager playerDataSync) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.playerDataSync = playerDataSync;
        this.identity = SMPServerIdentity.resolve(plugin);
        this.repository = new SMPNetworkRepository(dataManager);
    }

    public void start() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (!dataManager.isMySqlEnabled()) {
            plugin.getLogger().warning("[SMP] Network routing registry disabled because MariaDB is unavailable.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repository.ensureTables();
                status = SMPServerStatus.ONLINE;
                heartbeat();
                validateCurrentServerSetup();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    heartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::heartbeat, 200L, 200L);
                    presenceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPresence, 20L, 400L);
                });
                plugin.getLogger().info("[SMP] Network identity: " + identity.serverId() + " (" + identity.serverType() + ")");
            } catch (Exception e) {
                plugin.getLogger().severe("[SMP] Network table setup failed: " + e.getMessage());
            }
        });
    }

    public void stop() {
        status = SMPServerStatus.STOPPING;
        if (heartbeatTask != null) heartbeatTask.cancel();
        if (presenceTask != null) presenceTask.cancel();
        if (dataManager.isMySqlEnabled()) {
            try { repository.markStatus(identity.serverId(), SMPServerStatus.OFFLINE); }
            catch (Exception ignored) { }
        }
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, "BungeeCord");
    }

    private void heartbeat() {
        if (!dataManager.isMySqlEnabled()) return;
        try {
            double tps = Math.min(20.0D, Bukkit.getTPS()[0]);
            int activeRuns = identity.serverType() == SMPServerType.DUNGEON && plugin.getDungeonManager() != null
                    ? plugin.getDungeonManager().activeRunCount() : 0;
            repository.heartbeat(identity, Bukkit.getOnlinePlayers().size(), Bukkit.getMaxPlayers(), tps, activeRuns, status);
        } catch (Exception e) {
            plugin.getLogger().warning("[SMP] Heartbeat failed: " + e.getMessage());
        }
    }

    private void validateCurrentServerSetup() {
        try {
            if (identity.serverType() == SMPServerType.OVERWORLD) {
                Optional<SMPNetworkRepository.ServerSpawn> spawn = repository.findSpawn("overworld", identity);
                Optional<SMPNetworkRepository.RtpLocation> rtp = repository.findRtpLocation(identity.serverId());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    spawn.ifPresent(value -> warnIfLocalWorldMissing("Overworld spawn", value.worldName()));
                    rtp.ifPresent(value -> warnIfLocalWorldMissing("RTP location", value.worldName()));
                });
            } else if (identity.serverType() == SMPServerType.DUNGEON) {
                Optional<SMPNetworkRepository.ServerSpawn> lobby = repository.findSpawn("dungeon_lobby", identity);
                Bukkit.getScheduler().runTask(plugin, () -> lobby.ifPresent(value -> warnIfLocalWorldMissing("Dungeon lobby spawn", value.worldName())));
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[SMP] Could not validate local " + identity.serverType() + " spawn setup: " + exception.getMessage());
        }
    }

    private void warnIfLocalWorldMissing(String setupName, String worldName) {
        if (worldName != null && !worldName.isBlank() && findLoadedWorld(worldName) == null) {
            plugin.getLogger().warning("[SMP] " + setupName + " references unloaded world '" + worldName + "'.");
        }
    }

    private World findLoadedWorld(String name) {
        if (name == null || name.isBlank()) return null;
        World exact = Bukkit.getWorld(name);
        if (exact != null) return exact;
        for (World world : Bukkit.getWorlds()) if (world.getName().equalsIgnoreCase(name)) return world;
        return null;
    }

    public void sendToHub(Player player) { route(player, SMPServerType.HUB, null); }
    public void sendToHub(Player player, Runnable unavailable) {
        if (!dataManager.isMySqlEnabled()) { if (unavailable != null) unavailable.run(); return; }
        CompletableFuture.supplyAsync(() -> selectServerNow(player, SMPServerType.HUB)).thenAccept(selected -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (selected.isEmpty()) { if (unavailable != null) unavailable.run(); return; }
            flushThenConnect(player, selected.get());
        }));
    }
    public void sendToOverworld(Player player) { route(player, SMPServerType.OVERWORLD, null); }
    public void sendToDungeon(Player player, String dungeonId) { route(player, SMPServerType.DUNGEON, dungeonId); }

    public void routeHome(Player player, HomeManager.HomeRecord home) {
        if (player == null || home == null || !home.isOverworld() || !dataManager.isMySqlEnabled()) {
            if (player != null) player.sendMessage("§cCould not find your home.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean available = repository.servers().stream().anyMatch(server -> server.serverId().equalsIgnoreCase(home.serverId())
                        && server.type() == SMPServerType.OVERWORLD && server.available());
                if (!available) { Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cYour home server is offline. Try again later.")); return; }
                long now = System.currentTimeMillis(); String teleportId = "home_" + player.getUniqueId();
                try (var connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO `smp_pending_cross_server_teleports` (`teleport_id`,`uuid`,`target_server_id`,`target_world`,`x`,`y`,`z`,`yaw`,`pitch`,`reason`,`created_at`,`expires_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `target_server_id`=VALUES(`target_server_id`),`target_world`=VALUES(`target_world`),`x`=VALUES(`x`),`y`=VALUES(`y`),`z`=VALUES(`z`),`yaw`=VALUES(`yaw`),`pitch`=VALUES(`pitch`),`reason`=VALUES(`reason`),`created_at`=VALUES(`created_at`),`expires_at`=VALUES(`expires_at`)")) {
                    statement.setString(1, teleportId); statement.setString(2, player.getUniqueId().toString()); statement.setString(3, home.serverId()); statement.setString(4, home.world()); statement.setDouble(5, home.x()); statement.setDouble(6, home.y()); statement.setDouble(7, home.z()); statement.setFloat(8, home.yaw()); statement.setFloat(9, home.pitch()); statement.setString(10, "HOME"); statement.setLong(11, now); statement.setLong(12, now + 30_000L); statement.executeUpdate();
                }
                Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) { player.sendMessage("§aSending you to your home on §e" + home.serverId() + "§a..."); flushThenConnect(player, home.serverId()); } });
            } catch (Exception exception) { Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cCould not save teleport request. Try again.")); }
        });
    }

    public String getCurrentServerId() { return identity.serverId(); }
    public SMPServerType getCurrentServerType() { return identity.serverType(); }
    public boolean isHub() { return identity.serverType() == SMPServerType.HUB; }
    public boolean isOverworld() { return identity.serverType() == SMPServerType.OVERWORLD; }
    public boolean isDungeon() { return identity.serverType() == SMPServerType.DUNGEON; }
    public boolean canRunHubZoneSystems() { return isHub(); }
    public boolean canRunDungeonInstances() { return isDungeon(); }
    public void ensureDungeonLobbyLoaded(){if(isDungeon()&&Bukkit.getWorld("lobby")==null&&loadExistingWorld("lobby")==null)plugin.getLogger().severe("[SMP] DUNGEON server requires an existing lobby world folder.");}

    public void travelWarp(Player player,SMPServerType targetType,String warp) {
        plugin.getLogger().info("Reserved warp requested: player="+player.getName()+", target="+warp+", currentServer="+identity.serverId()+", currentType="+identity.serverType());
        if(targetType==SMPServerType.UNKNOWN){player.sendMessage("§cCould not send you there. Please try again.");return;}
        if(identity.serverType()==targetType){player.sendMessage("§aTeleporting you to the spawn...");teleportToServerSpawn(player,warp,true);return;}
        if(!dataManager.isMySqlEnabled()){player.sendMessage("§cCould not send you there. Please try again.");return;}
        selectServerForWarp(player,targetType).thenAccept(server->{
            if(server.isEmpty()){Bukkit.getScheduler().runTask(plugin,()->player.sendMessage("§cCould not send you there. Please try again."));return;}
            plugin.getLogger().info("Selected target server for warp "+warp+": "+server.get());
            Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{
                repository.queueWarp(player.getUniqueId(),targetType,server.get(),warp);
                plugin.getLogger().info("Queued pending warp: player="+player.getUniqueId()+", targetWarp="+warp+", targetServer="+server.get());
                repository.updateAssignment(player.getUniqueId(),targetType,server.get());
                Bukkit.getScheduler().runTask(plugin,()->{if(!player.isOnline())return;player.sendMessage(switch(targetType){case HUB->"§aSending you to the SMP hub...";case OVERWORLD->"§aSending you to the overworld...";case DUNGEON->"§aSending you to the dungeon lobby...";case UNKNOWN->"§cCould not send you there. Please try again.";});plugin.getLogger().info("Sending player to server: "+server.get());flushThenConnect(player,server.get());});
            }catch(Exception e){Bukkit.getScheduler().runTask(plugin,()->player.sendMessage("§cCould not send you there. Please try again."));}});
        });
    }

    public void saveServerSpawn(Player player,String key,java.util.function.Consumer<Boolean> completion){Location l=player.getLocation().clone();UUID updatedBy=player.getUniqueId();Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{boolean success;try{repository.saveSpawn(key,identity,l.getWorld().getName(),l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch(),updatedBy);success=true;}catch(Exception e){plugin.getLogger().severe("[SMP] Failed to save server spawn "+key+": "+e.getMessage());success=false;}boolean result=success;Bukkit.getScheduler().runTask(plugin,()->completion.accept(result));});}

    public void saveRtpLocation(Player player,int minRadius,int maxRadius,java.util.function.Consumer<Boolean> completion){Location l=player.getLocation().clone();UUID updatedBy=player.getUniqueId();Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{boolean success;try{repository.saveRtpLocation(identity.serverId(),l.getWorld().getName(),l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch(),minRadius,maxRadius,updatedBy);success=true;}catch(Exception e){plugin.getLogger().severe("[SMP] Failed to save RTP location for "+identity.serverId()+": "+e.getMessage());success=false;}boolean result=success;Bukkit.getScheduler().runTask(plugin,()->completion.accept(result));});}
    public CompletableFuture<Optional<SMPNetworkRepository.RtpLocation>> findRtpLocation(){if(!dataManager.isMySqlEnabled())return CompletableFuture.completedFuture(Optional.empty());return CompletableFuture.supplyAsync(()->{try{return repository.findRtpLocation(identity.serverId());}catch(Exception e){plugin.getLogger().warning("[SMP] Failed to load RTP location for "+identity.serverId()+": "+e.getMessage());return Optional.empty();}});}

    public void teleportToServerSpawn(Player player,String warp,boolean reportFailure){String key=warp.equalsIgnoreCase("dungeon")?"dungeon_lobby":warp.toLowerCase();Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{Optional<SMPNetworkRepository.ServerSpawn> spawn=repository.findSpawn(key,identity);Bukkit.getScheduler().runTask(plugin,()->teleportResolvedSpawn(player,spawn.orElse(null),warp,reportFailure));}catch(Exception e){Bukkit.getScheduler().runTask(plugin,()->teleportResolvedSpawn(player,null,warp,reportFailure));}});}
    private void teleportResolvedSpawn(Player player,SMPNetworkRepository.ServerSpawn stored,String warp,boolean reportFailure){if(!player.isOnline())return;Location target=null;if(stored!=null){World world=Bukkit.getWorld(stored.worldName());if(world==null&&warp.equalsIgnoreCase("dungeon")&&stored.worldName().equalsIgnoreCase("lobby"))world=loadExistingWorld("lobby");if(world!=null)target=new Location(world,stored.x(),stored.y(),stored.z(),stored.yaw(),stored.pitch());}if(target==null&&warp.equalsIgnoreCase("dungeon")){World lobby=Bukkit.getWorld("lobby");if(lobby==null)lobby=loadExistingWorld("lobby");if(lobby!=null)target=lobby.getSpawnLocation();}if(target==null)target=plugin.getSpawnManager().getSpawn();if(target!=null&&target.getWorld()!=null)player.teleport(target);else if(reportFailure)player.sendMessage("§cCould not send you there. Please try again.");}
    private World loadExistingWorld(String name){java.nio.file.Path root=plugin.getServer().getWorldContainer().toPath().normalize(),folder=root.resolve(name).normalize();if(!folder.startsWith(root)||!java.nio.file.Files.isDirectory(folder)||!java.nio.file.Files.isRegularFile(folder.resolve("level.dat")))return null;return Bukkit.createWorld(new WorldCreator(name));}

    public void routeRtp(Player player) {
        if (!isHub()) return;
        List<String> group = getServersForType(SMPServerType.OVERWORLD);
        plugin.getLogger().info("[SMP] RTP selecting overworld server from group OVERWORLD: " + group);
        selectOverworldServer(player).thenAccept(server -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (server.isEmpty()) { player.sendMessage("§cNo available overworld server was found."); return; }
            plugin.getLogger().info("[SMP] RTP selected overworld server: " + server.get());
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    repository.queueRtp(player.getUniqueId(), identity.serverId(), server.get());
                    Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) { player.sendMessage("§aSending you to survival for random teleport..."); flushThenConnect(player, server.get()); } });
                } catch (Exception e) { Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cServer transfer is not available on this server.")); }
            });
        }));
    }

    public void routeZoneWaypoint(Player player,String zoneId) {
        if (isHub()) {
            var result=plugin.getZoneManager().travelToZone(player,zoneId);
            player.sendMessage(club.abelix.games.fakevival.util.CC.color(result.message()));
            return;
        }
        if (!dataManager.isMySqlEnabled()) { player.sendMessage("§cThat waypoint is unavailable right now."); return; }
        selectServer(player,SMPServerType.HUB).thenAccept(server->{
            if(server.isEmpty()){Bukkit.getScheduler().runTask(plugin,()->player.sendMessage("§cThat waypoint is unavailable right now."));return;}
            Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{
                repository.queueZoneWarp(player.getUniqueId(),zoneId);
                repository.updateAssignment(player.getUniqueId(),SMPServerType.HUB,server.get());
                Bukkit.getScheduler().runTask(plugin,()->{if(!player.isOnline())return;player.sendMessage("§aSending you to the hub for that waypoint...");flushThenConnect(player,server.get());});
            }catch(Exception e){Bukkit.getScheduler().runTask(plugin,()->player.sendMessage("§cThat waypoint is unavailable right now."));}});
        });
    }

    public void staffTeleport(Player staff, String targetName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Optional<SMPNetworkRepository.PlayerPresence> found=repository.findPresence(targetName);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!staff.isOnline()) return;
                    if (found.isEmpty()) { staff.sendMessage("§cThat player is not online."); return; }
                    var target=found.get();
                    if (target.serverId().equals(identity.serverId())) {
                        Player local=Bukkit.getPlayer(target.uuid());
                        if(local==null) staff.sendMessage("§cThat player is no longer online."); else { staff.teleport(local); staff.sendMessage("§aTeleported to §e"+local.getName()+"§a."); }
                        return;
                    }
                    Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{repository.queueStaffTp(staff.getUniqueId(),target);Bukkit.getScheduler().runTask(plugin,()->{if(staff.isOnline()){staff.sendMessage("§aSending you to §e"+target.serverId()+" §ato teleport to §e"+target.name()+"§a...");flushThenConnect(staff,target.serverId());}});}catch(Exception e){Bukkit.getScheduler().runTask(plugin,()->staff.sendMessage("§cServer transfer is not available on this server."));}});
                });
            } catch (Exception e) { Bukkit.getScheduler().runTask(plugin, () -> staff.sendMessage("§cThat player is not online.")); }
        });
    }

    public boolean transferPlayer(Player player,String targetServer){ if(targetServer==null||targetServer.isBlank())return false;flushThenConnect(player,targetServer);return true; }
    public Optional<SMPNetworkRepository.PlayerPresence> findPlayerPresence(String value){try{return repository.findPresence(value);}catch(Exception e){return Optional.empty();}}

    private CompletableFuture<Optional<String>> selectServer(Player player,SMPServerType type){return CompletableFuture.supplyAsync(()->selectServerNow(player,type));}
    private CompletableFuture<Optional<String>> selectServerForWarp(Player player,SMPServerType type){return switch(type){case HUB->selectHubServer(player);case OVERWORLD->selectOverworldServer(player);case DUNGEON->selectDungeonServer(player);case UNKNOWN->CompletableFuture.completedFuture(Optional.empty());};}
    private CompletableFuture<Optional<String>> selectHubServer(Player player){return selectServer(player,SMPServerType.HUB);}
    private CompletableFuture<Optional<String>> selectOverworldServer(Player player){return selectServer(player,SMPServerType.OVERWORLD);}
    private CompletableFuture<Optional<String>> selectDungeonServer(Player player){return selectServer(player,SMPServerType.DUNGEON);}

    public List<String> getServersForType(SMPServerType type) {
        return SMPServerGroups.getServersForType(type);
    }

    private Optional<String> selectServerNow(Player player, SMPServerType type) {
        List<String> fallback = getServersForType(type);
        if (fallback.isEmpty()) {
            plugin.getLogger().warning("[SMP] Server selection failed for " + type + ": group empty; no fallback configured.");
            return Optional.empty();
        }
        try {
            List<SMPNetworkRepository.ServerRecord> group = repository.servers().stream()
                    .filter(server -> server.type() == type && isServerGroup(server.serverId(), type))
                    .toList();
            if (type == SMPServerType.OVERWORLD) {
                Optional<String> assigned = repository.assignedOverworld(player.getUniqueId());
                Optional<SMPNetworkRepository.ServerRecord> match = assigned.flatMap(id -> group.stream()
                        .filter(server -> server.serverId().equals(SMPServerGroups.canonicalId(id)) && server.available())
                        .findFirst());
                if (match.isPresent()) return Optional.of(match.get().serverId());
            }
            Optional<String> available = select(group, type).map(SMPNetworkRepository.ServerRecord::serverId);
            if (available.isPresent()) return available;
            if (!group.isEmpty()) {
                plugin.getLogger().warning("[SMP] Server selection failed for " + type + ": all servers offline or full.");
                return Optional.empty();
            }
            plugin.getLogger().info("[SMP] No status rows for " + type + "; using configured round-robin fallback.");
            return fallback(type, fallback);
        } catch (Exception e) {
            plugin.getLogger().warning("[SMP] Server status table missing or unavailable for " + type + ": " + e.getMessage() + "; using configured fallback.");
            return fallback(type, fallback);
        }
    }

    private Optional<String> fallback(SMPServerType type, List<String> servers) {
        if (servers.isEmpty()) {
            plugin.getLogger().warning("[SMP] Server selection failed for " + type + ": no fallback configured.");
            return Optional.empty();
        }
        int index = type == SMPServerType.OVERWORLD
                ? Math.floorMod(overworldFallbackIndex.getAndIncrement(), servers.size()) : 0;
        return Optional.of(servers.get(index));
    }

    private void route(Player player, SMPServerType targetType, String dungeonId) {
        if (!dataManager.isMySqlEnabled()) {
            player.sendMessage("§cServer transfer is not available on this server.");
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            try {
                Optional<String> selected = targetType==SMPServerType.DUNGEON&&dungeonId!=null
                        ? findRejoinServer(player.getUniqueId(),dungeonId).or(()->selectServerNow(player,targetType))
                        : selectServerNow(player, targetType);
                if (selected.isEmpty()) return new RouteResult(null, "§cNo available " + targetType.name().toLowerCase() + " server was found.");
                String server = selected.get();
                if (targetType == SMPServerType.DUNGEON) repository.reserveDungeon(player.getUniqueId(), dungeonId == null ? "unknown" : dungeonId, server);
                repository.updateAssignment(player.getUniqueId(), targetType, server);
                return new RouteResult(server, null);
            } catch (Exception e) {
                plugin.getLogger().warning("[SMP] Route selection failed: " + e.getMessage());
                return new RouteResult(null, "§cServer transfer is not available on this server.");
            }
        }).thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (result.serverId() == null) { player.sendMessage(result.error()); return; }
            flushThenConnect(player, result.serverId());
        }));
    }

    private Optional<String> findRejoinServer(UUID uuid,String dungeonId)throws SQLException{try(var c=dataManager.getConnection();var s=c.prepareStatement("SELECT r.server_id FROM smp_dungeon_runs r JOIN smp_dungeon_run_players p ON p.run_id=r.run_id WHERE p.uuid=? AND r.dungeon_id=? AND r.state IN ('ACTIVE','EMPTY_WAITING_FOR_REJOIN') AND (r.expires_at=0 OR r.expires_at>?) ORDER BY r.created_at DESC LIMIT 1")){s.setString(1,uuid.toString());s.setString(2,DungeonManager.canonicalDungeonId(dungeonId));s.setLong(3,System.currentTimeMillis());try(var r=s.executeQuery()){if(r.next()){if(playerServerAvailable(r.getString(1))){plugin.getLogger().info("[SMP] Routing dungeon rejoin to existing server "+r.getString(1));return Optional.of(r.getString(1));}}}}return Optional.empty();}
    private boolean playerServerAvailable(String serverId)throws SQLException{return repository.servers().stream().anyMatch(server->server.serverId().equalsIgnoreCase(serverId)&&server.available());}

    private Optional<SMPNetworkRepository.ServerRecord> select(List<SMPNetworkRepository.ServerRecord> servers, SMPServerType type) {
        Comparator<SMPNetworkRepository.ServerRecord> comparator;
        if (type == SMPServerType.DUNGEON) comparator = Comparator.comparingInt(SMPNetworkRepository.ServerRecord::activeDungeonRuns).thenComparingInt(SMPNetworkRepository.ServerRecord::onlinePlayers);
        else if (type == SMPServerType.HUB) comparator = Comparator.<SMPNetworkRepository.ServerRecord>comparingInt(s -> s.serverId().equalsIgnoreCase("smp-1") && s.onlinePlayers() < Math.max(1, s.maxPlayers() * 3 / 4) ? 0 : 1).thenComparingInt(SMPNetworkRepository.ServerRecord::onlinePlayers);
        else comparator = Comparator.comparingInt(SMPNetworkRepository.ServerRecord::onlinePlayers);
        return servers.stream().filter(s -> s.type() == type && isServerGroup(s.serverId(),type) && s.available()).min(comparator);
    }

    private boolean isServerGroup(String serverId,SMPServerType type){return SMPServerGroups.contains(type,serverId);}

    private void flushThenConnect(Player player, String serverId) {
        dataManager.save();
        plugin.getStorageManager().saveOpenPage(player).whenComplete((ignored, storageError) ->
                Bukkit.getScheduler().runTask(plugin, () -> continueTransferAfterStorage(player, serverId, storageError)));
    }

    private void continueTransferAfterStorage(Player player, String serverId, Throwable storageError) {
        if (!player.isOnline()) return;
        if (storageError != null) {
            player.sendMessage("§cCould not save your storage. Try again in a moment.");
            return;
        }
        if (playerDataSync != null && playerDataSync.isEnabled()) {
            playerDataSync.prepareTransfer(player,serverId, saved -> {
                if (!player.isOnline()) return;
                if (!saved) { player.sendMessage("§cCould not save your data. Try again in a moment."); return; }
                connect(player, serverId);
            });
        } else connect(player, serverId);
    }

    private void connect(Player player, String serverId) {
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
            out.writeUTF("Connect"); out.writeUTF(serverId);
            player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (Exception e) {
            player.sendMessage("§cServer transfer is not available on this server.");
        }
    }

    private void updateAllPresence(){for(Player player:Bukkit.getOnlinePlayers())updatePresence(player);}
    private void updatePresence(Player player){var l=player.getLocation();UUID uuid=player.getUniqueId();String name=player.getName();String world=l.getWorld()==null?null:l.getWorld().getName();double x=l.getX(),y=l.getY(),z=l.getZ();Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{repository.updatePresence(uuid,name,identity,world,x,y,z);}catch(Exception e){plugin.getLogger().warning("[SMP] Presence update failed: "+e.getMessage());}});}

    @EventHandler public void onJoin(PlayerJoinEvent event){
        Player player=event.getPlayer();
        if(!dataManager.isMySqlEnabled()){if(isDungeon())Bukkit.getScheduler().runTaskLater(plugin,()->teleportToServerSpawn(player,"dungeon",true),30L);return;}
        updatePresence(player);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            try{
                boolean rtp=isOverworld()&&repository.consumeRtp(player.getUniqueId(),identity.serverId());
                Optional<SMPNetworkRepository.PendingStaffTp> staff=repository.consumeStaffTp(player.getUniqueId(),identity.serverId());
                Optional<String> dungeon=isDungeon()?repository.consumeDungeonJoin(player.getUniqueId(),identity.serverId()):Optional.empty();
                Optional<SMPNetworkRepository.PendingWarp> warp=repository.consumeWarp(player.getUniqueId(),identity.serverId());
                Optional<SMPNetworkRepository.PendingZoneWarp> zoneWarp=isHub()?repository.consumeZoneWarp(player.getUniqueId()):Optional.empty();
                Optional<PendingHomeTeleport> homeTeleport=consumePendingHome(player.getUniqueId());
                Bukkit.getScheduler().runTaskLater(plugin,()->{
                    if(!player.isOnline())return;
                    if(dungeon.isPresent()){plugin.getDungeonManager().enterFromMenu(player,dungeon.get());return;}
                    if(homeTeleport.isPresent()){teleportPendingHome(player,homeTeleport.get());return;}
                    if(staff.isPresent()){var p=staff.get();Player target=Bukkit.getPlayer(p.targetUuid());if(target==null)player.sendMessage("§cThat player is no longer online.");else{player.teleport(target);player.sendMessage("§aTeleported to §e"+target.getName()+"§a.");}return;}
                    if(rtp){player.sendMessage("§aFinding a safe random location...");player.performCommand("rtp");return;}
                    if(zoneWarp.isPresent()){var result=plugin.getZoneManager().travelToZone(player,zoneWarp.get().zoneId());player.sendMessage(club.abelix.games.fakevival.util.CC.color(result.message()));return;}
                    if(warp.isPresent()){teleportToServerSpawn(player,warp.get().targetWarp(),true);return;}
                    if(isDungeon())teleportToServerSpawn(player,"dungeon",true);
                },30L);
            }catch(Exception e){plugin.getLogger().warning("[SMP] Pending transfer consume failed: "+e.getMessage());}
        });
    }
    @EventHandler public void onQuit(PlayerQuitEvent event){UUID uuid=event.getPlayer().getUniqueId();if(dataManager.isMySqlEnabled())Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{repository.removePresence(uuid,identity.serverId());}catch(Exception ignored){}});}

    private Optional<PendingHomeTeleport> consumePendingHome(UUID uuid) throws Exception {
        try (var connection=dataManager.getConnection(); PreparedStatement statement=connection.prepareStatement("SELECT `teleport_id`,`target_world`,`x`,`y`,`z`,`yaw`,`pitch`,`expires_at` FROM `smp_pending_cross_server_teleports` WHERE `uuid`=? AND `target_server_id`=? AND `reason`='HOME' ORDER BY `created_at` DESC LIMIT 1")) {
            statement.setString(1,uuid.toString());statement.setString(2,identity.serverId());
            try(ResultSet result=statement.executeQuery()){if(!result.next())return Optional.empty();String id=result.getString("teleport_id");long expires=result.getLong("expires_at");if(expires<System.currentTimeMillis()){deletePendingHome(id);return Optional.empty();}return Optional.of(new PendingHomeTeleport(id,result.getString("target_world"),result.getDouble("x"),result.getDouble("y"),result.getDouble("z"),result.getFloat("yaw"),result.getFloat("pitch")));}
        }
    }

    private void teleportPendingHome(Player player,PendingHomeTeleport pending){World world=findLoadedWorld(pending.world());if(world==null){player.sendMessage("§cYour home world is not loaded right now.");Location spawn=plugin.getSpawnManager().getSpawn();if(spawn!=null&&spawn.getWorld()!=null)player.teleport(spawn);}else player.teleport(new Location(world,pending.x(),pending.y(),pending.z(),pending.yaw(),pending.pitch()));Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{deletePendingHome(pending.id());}catch(Exception ignored){}});}
    private void deletePendingHome(String id)throws Exception{try(var connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("DELETE FROM `smp_pending_cross_server_teleports` WHERE `teleport_id`=?")){statement.setString(1,id);statement.executeUpdate();}}

    public SMPServerIdentity identity() { return identity; }
    public SMPNetworkRepository repository() { return repository; }
    private record RouteResult(String serverId, String error) {}
    private record PendingHomeTeleport(String id,String world,double x,double y,double z,float yaw,float pitch) {}
}
