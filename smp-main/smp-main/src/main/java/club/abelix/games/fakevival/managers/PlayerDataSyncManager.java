package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.mmo.MMOStatService;
import club.abelix.games.fakevival.registries.PlayerDataSyncRegistry;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class PlayerDataSyncManager implements Listener {
    private static final int SAVE_VERSION_START = 1;

    private final Game plugin;
    private final DataManager dataManager;
    private final ManaActionBarManager manaManager;
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> loadingPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> savingPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> warnedCorruptPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> transferringPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> transferPreparingPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> leavingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID,String> lockTokens = new ConcurrentHashMap<>();
    private final Map<UUID,Long> lastLoadTimes = new ConcurrentHashMap<>();
    private final Map<UUID,Long> lastSaveTimes = new ConcurrentHashMap<>();
    private final Map<UUID,Integer> loadedVersions = new ConcurrentHashMap<>();
    private final Map<UUID,Integer> loadedSnapshotItemCounts = new ConcurrentHashMap<>();
    private final Map<UUID,Long> loadedAt = new ConcurrentHashMap<>();
    private final Map<UUID,Long> lastInventoryMutationTimes = new ConcurrentHashMap<>();
    private final Map<UUID,String> lastSaveResults = new ConcurrentHashMap<>();
    private final Map<UUID,PendingSave> queuedSaves = new ConcurrentHashMap<>();
    private BukkitTask autosaveTask;
    private BukkitTask lockHeartbeatTask;
    private boolean enabled;
    private String serverId = PlayerDataSyncRegistry.SERVER_ID_FALLBACK;
    private int autosaveSeconds = PlayerDataSyncRegistry.AUTOSAVE_SECONDS;
    private int lockTtlSeconds = PlayerDataSyncRegistry.LOCK_TTL_SECONDS;
    private int heartbeatIntervalSeconds = PlayerDataSyncRegistry.HEARTBEAT_INTERVAL_SECONDS;
    private int staleLockGraceSeconds = PlayerDataSyncRegistry.STALE_LOCK_GRACE_SECONDS;
    private final String serverInstanceId=UUID.randomUUID().toString();
    private volatile boolean serverIdentityUnique=true;
    private volatile long lastServerHeartbeat;

    public PlayerDataSyncManager(Game plugin, DataManager dataManager, ManaActionBarManager manaManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.manaManager = manaManager;
    }

    public void start() {
        stop();
        enabled = PlayerDataSyncRegistry.booleanValue("SMP_PLAYERDATA_SYNC_ENABLED", PlayerDataSyncRegistry.ENABLED)
                && dataManager.isMySqlEnabled();
        if (!enabled) {
            plugin.getLogger().info("[PlayerDataSync] Disabled" + (dataManager.isMySqlEnabled() ? "." : " because MariaDB is disabled."));
            return;
        }
        serverId = resolveServerId();
        autosaveSeconds = Math.max(30, PlayerDataSyncRegistry.intValue("SMP_PLAYERDATA_SYNC_AUTOSAVE_SECONDS", PlayerDataSyncRegistry.AUTOSAVE_SECONDS));
        lockTtlSeconds = Math.max(30, PlayerDataSyncRegistry.intValue("SMP_PLAYERDATA_SYNC_LOCK_TTL_SECONDS", PlayerDataSyncRegistry.LOCK_TTL_SECONDS));
        heartbeatIntervalSeconds=Math.max(5,PlayerDataSyncRegistry.intValue("SMP_PLAYERDATA_SYNC_HEARTBEAT_SECONDS",PlayerDataSyncRegistry.HEARTBEAT_INTERVAL_SECONDS));
        staleLockGraceSeconds=Math.max(lockTtlSeconds,PlayerDataSyncRegistry.intValue("SMP_PLAYERDATA_SYNC_STALE_GRACE_SECONDS",PlayerDataSyncRegistry.STALE_LOCK_GRACE_SECONDS));
        String serverType=plugin.getNetworkService().isHub()?"HUB":plugin.getNetworkService().isDungeon()?"DUNGEON":plugin.getNetworkService().isOverworld()?"OVERWORLD":"UNKNOWN";
        plugin.getLogger().info("[SMP PlayerDataSync] Server ID="+serverId+" type="+serverType);
        if(serverId.isBlank()||PlayerDataSyncRegistry.SERVER_ID_FALLBACK.equalsIgnoreCase(serverId))plugin.getLogger().severe("[SMP PlayerDataSync] WARNING: SMP_SERVER_ID is missing or duplicate. Player locks will not work correctly.");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ensureTable();
                serverIdentityUnique=claimServerIdentity();
                if(!serverIdentityUnique)plugin.getLogger().severe("[SMP PlayerDataSync] WARNING: SMP_SERVER_ID is missing or duplicate. Player locks will not work correctly.");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!plugin.isEnabled()) return;
                    autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::autosaveOnlinePlayers,
                            autosaveSeconds * 20L, autosaveSeconds * 20L);
                    lockHeartbeatTask=Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,this::heartbeatOwnedLocks,heartbeatIntervalSeconds*20L,heartbeatIntervalSeconds*20L);
                    plugin.getLogger().info("[PlayerDataSync] Enabled with serverId=" + serverId + ", autosave=" + autosaveSeconds + "s");
                });
            } catch (SQLException exception) {
                enabled = false;
                plugin.getLogger().warning("[PlayerDataSync] Disabled because table setup failed: " + exception.getMessage());
            }
        });
    }

    public void stop() {
        if (autosaveTask != null) autosaveTask.cancel();
        autosaveTask = null;
        if(lockHeartbeatTask!=null)lockHeartbeatTask.cancel();
        lockHeartbeatTask=null;
    }

    public void shutdownSaveOnlinePlayers() {
        if (!enabled) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if(!isPlayerDataLoaded(player.getUniqueId()))continue;
            PlayerDataSnapshot snapshot = capture(player);
            saveBlocking(snapshot, true);
        }
        releaseServerIdentity();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean ownsCurrentSessionLock(UUID uuid){return !enabled||(uuid!=null&&isPlayerDataLoaded(uuid)&&lockTokens.containsKey(uuid));}
    public boolean isPlayerDataLoaded(UUID uuid) { return !enabled || uuid != null && loadedPlayers.contains(uuid)&&!loadingPlayers.contains(uuid)&&!transferPreparingPlayers.contains(uuid)&&!transferringPlayers.contains(uuid); }
    public boolean isPlayerDataLoading(UUID uuid) { return enabled && uuid != null && loadingPlayers.contains(uuid); }
    public boolean isTransferPending(UUID uuid) { return enabled && uuid != null && (transferPreparingPlayers.contains(uuid)||transferringPlayers.contains(uuid)); }

    public String statusLine() {
        if (!enabled) return "&ePlayerdata sync is disabled.";
        return "&aPlayerdata sync enabled &7serverId=&f" + serverId + " &7autosave=&f" + autosaveSeconds + "s &7loaded=&f" + loadedPlayers.size();
    }

    public void saveCommand(Player player, Consumer<Boolean> callback) {
        if (!enabled) {
            callback.accept(false);
            return;
        }
        if (!isPlayerDataLoaded(player.getUniqueId())) { callback.accept(false); return; }
        PlayerDataSnapshot snapshot = capture(player);
        saveAsync(snapshot, false, callback);
    }

    public void prepareTransfer(Player player,String targetServerId,Consumer<Boolean> callback){
        if(!enabled){callback.accept(true);return;}
        UUID uuid=player.getUniqueId();
        if(!isPlayerDataLoaded(uuid)||isPlayerDataLoading(uuid)){callback.accept(false);return;}
        if(!transferPreparingPlayers.add(uuid)){callback.accept(false);return;}
        PlayerDataSnapshot snapshot=capture(player);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            if(!beginTransferHandoff(uuid,targetServerId)){
                transferPreparingPlayers.remove(uuid);Bukkit.getScheduler().runTask(plugin,()->callback.accept(false));
                return;
            }
            Bukkit.getScheduler().runTask(plugin,()->saveAsync(snapshot,false,saved->{
                if(!saved){transferPreparingPlayers.remove(uuid);markHandoffFailedAsync(uuid);callback.accept(false);return;}
                Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                    int savedVersion=loadedVersions.getOrDefault(uuid,0);
                    boolean marked=markTransferReady(uuid,snapshot.lockToken(),targetServerId,savedVersion);
                    if(marked){
                        transferPreparingPlayers.remove(uuid);
                        transferringPlayers.add(uuid);
                        Bukkit.getScheduler().runTaskLater(plugin,()->{
                            Player stillHere=Bukkit.getPlayer(uuid);
                            if(stillHere==null||!stillHere.isOnline()||!transferringPlayers.remove(uuid))return;
                            Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{expireHandoff(uuid,"EXPIRED");acquireLock(uuid,stillHere.getName());}catch(SQLException exception){plugin.getLogger().warning("[PlayerDataSync] Could not resume lock after cancelled transfer: "+exception.getMessage());}});
                        },200L);
                    } else {transferPreparingPlayers.remove(uuid);markHandoffFailed(uuid);}
                    Bukkit.getScheduler().runTask(plugin,()->{Player local=Bukkit.getPlayer(uuid);if(local==null||!local.isOnline()){clearSessionState(uuid,true);leavingPlayers.remove(uuid);}callback.accept(marked);});
                });
            }));
        });
    }

    public void loadCommand(Player player, Consumer<Boolean> callback) {
        if (!enabled) {
            callback.accept(false);
            return;
        }
        loadAsync(player, true, callback);
    }

    public void unlockCommand(Player player, Consumer<Boolean> callback) {
        if (!enabled) {
            callback.accept(false);
            return;
        }
        // Active sessions must never be force-unlocked through the legacy API.
        if(player!=null&&player.isOnline()){callback.accept(false);return;}
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean result = releaseAnyLock(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        UUID uuid=event.getPlayer().getUniqueId();
        if(!savingPlayers.contains(uuid)&&!leavingPlayers.contains(uuid))clearSessionState(uuid, false);
        loadAsync(event.getPlayer(), false, ignored -> {
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handleLeave(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        handleLeave(event.getPlayer());
    }

    private void handleLeave(Player player) {
        if (!enabled || player == null) return;
        UUID uuid=player.getUniqueId();
        if(!leavingPlayers.add(uuid))return;
        if (!loadedPlayers.contains(uuid)) {
            loadingPlayers.remove(uuid);
            String token=lockTokens.remove(uuid);
            if(token!=null&&!token.isBlank())Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{releaseLock(uuid,token);}catch(SQLException ignored){}});
            leavingPlayers.remove(uuid);
            return;
        }
        if(transferPreparingPlayers.contains(uuid)){loadingPlayers.remove(uuid);return;}
        if(transferringPlayers.remove(uuid)){
            clearSessionState(uuid,true);leavingPlayers.remove(uuid);return;
        }
        PlayerDataSnapshot snapshot = capture(player);
        loadingPlayers.remove(uuid);
        saveAsync(snapshot, true, ignored -> {clearPreviousSessionAfterSave(uuid);leavingPlayers.remove(uuid);});
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void blockInventoryClickWhileLoading(InventoryClickEvent event){if(event.getWhoClicked() instanceof Player player&&!inventoryReady(player.getUniqueId()))event.setCancelled(true);}
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void blockInventoryDragWhileLoading(InventoryDragEvent event){if(event.getWhoClicked() instanceof Player player&&!inventoryReady(player.getUniqueId()))event.setCancelled(true);}
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void blockInventoryOpenWhileLoading(InventoryOpenEvent event){if(event.getPlayer() instanceof Player player&&!inventoryReady(player.getUniqueId()))event.setCancelled(true);}
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void blockDropWhileLoading(PlayerDropItemEvent event){if(!inventoryReady(event.getPlayer().getUniqueId()))event.setCancelled(true);}
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void blockPickupWhileLoading(EntityPickupItemEvent event){if(event.getEntity() instanceof Player player&&!inventoryReady(player.getUniqueId()))event.setCancelled(true);}
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void blockConsumeWhileLoading(PlayerItemConsumeEvent event){if(!inventoryReady(event.getPlayer().getUniqueId()))event.setCancelled(true);}
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void blockSwapWhileLoading(PlayerSwapHandItemsEvent event){if(!inventoryReady(event.getPlayer().getUniqueId()))event.setCancelled(true);}
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void blockInteractionWhileLoading(PlayerInteractEvent event){if(!inventoryReady(event.getPlayer().getUniqueId()))event.setCancelled(true);}
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void blockEntityInteractionWhileLoading(PlayerInteractEntityEvent event){if(!inventoryReady(event.getPlayer().getUniqueId()))event.setCancelled(true);}
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void protectLoadingPlayer(EntityDamageEvent event){if(event.getEntity() instanceof Player player&&!inventoryReady(player.getUniqueId()))event.setCancelled(true);}
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void blockLoadingPlayerDamage(EntityDamageByEntityEvent event){Player attacker=event.getDamager() instanceof Player player?player:event.getDamager() instanceof org.bukkit.entity.Projectile projectile&&projectile.getShooter() instanceof Player player?player:null;if(attacker!=null&&!inventoryReady(attacker.getUniqueId()))event.setCancelled(true);}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void trackInventoryClick(InventoryClickEvent event){if(event.getWhoClicked() instanceof Player player&&isPlayerDataLoaded(player.getUniqueId()))lastInventoryMutationTimes.put(player.getUniqueId(),System.currentTimeMillis());}
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void trackInventoryDrag(InventoryDragEvent event){if(event.getWhoClicked() instanceof Player player&&isPlayerDataLoaded(player.getUniqueId()))lastInventoryMutationTimes.put(player.getUniqueId(),System.currentTimeMillis());}
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void trackDrop(PlayerDropItemEvent event){if(isPlayerDataLoaded(event.getPlayer().getUniqueId()))lastInventoryMutationTimes.put(event.getPlayer().getUniqueId(),System.currentTimeMillis());}
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void trackPickup(EntityPickupItemEvent event){if(event.getEntity() instanceof Player player&&isPlayerDataLoaded(player.getUniqueId()))lastInventoryMutationTimes.put(player.getUniqueId(),System.currentTimeMillis());}
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void trackConsume(PlayerItemConsumeEvent event){if(isPlayerDataLoaded(event.getPlayer().getUniqueId()))lastInventoryMutationTimes.put(event.getPlayer().getUniqueId(),System.currentTimeMillis());}
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void trackSwap(PlayerSwapHandItemsEvent event){if(isPlayerDataLoaded(event.getPlayer().getUniqueId()))lastInventoryMutationTimes.put(event.getPlayer().getUniqueId(),System.currentTimeMillis());}
    @EventHandler(priority = EventPriority.MONITOR)
    public void trackDeathInventory(PlayerDeathEvent event){if(isPlayerDataLoaded(event.getEntity().getUniqueId()))lastInventoryMutationTimes.put(event.getEntity().getUniqueId(),System.currentTimeMillis());}

    private void autosaveOnlinePlayers() {
        if (!enabled) return;
        List<PlayerDataSnapshot> snapshots = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerDataLoaded(player.getUniqueId())) snapshots.add(capture(player));
        }
        for (PlayerDataSnapshot snapshot : snapshots) saveAsync(snapshot, false, ignored -> {
        });
    }

    private void loadAsync(Player player, boolean forceApply, Consumer<Boolean> callback) {
        UUID uuid = player.getUniqueId();
        if (!loadingPlayers.add(uuid)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            LoadResult result;
            try {
                long loadDeadline=System.currentTimeMillis()+PlayerDataSyncRegistry.JOIN_LOCK_RETRIES*PlayerDataSyncRegistry.JOIN_RETRY_DELAY_MILLIS;
                while((savingPlayers.contains(uuid)||leavingPlayers.contains(uuid))&&System.currentTimeMillis()<loadDeadline){
                    try{Thread.sleep(50L);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}
                }
                LockAttempt attempt=acquireLockWithRetry(uuid,player.getName(),loadDeadline);
                if (!attempt.acquired()) {
                    plugin.getLogger().warning("[PlayerDataSync] Playerdata for " + player.getName() + " remains locked by "+attempt.owner()+" after retries.");
                    result = LoadResult.lockedResult();
                } else {
                    result = loadCommittedSnapshot(uuid,loadDeadline);
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("[PlayerDataSync] Could not load synced playerdata for " + player.getName() + ": " + exception.getMessage());
                result = LoadResult.failure();
            }
            LoadResult finalResult = result;
            Bukkit.getScheduler().runTask(plugin, () -> {
                loadingPlayers.remove(uuid);
                if (!player.isOnline()) {
                    releaseFailedLoadLock(uuid);
                    callback.accept(false);
                    return;
                }
                if (finalResult.locked()) {
                    releaseFailedLoadLock(uuid);
                    player.kickPlayer(CC.color("&cYour player data is still syncing. Please rejoin in a few seconds."));
                    callback.accept(false);
                    return;
                }
                if(finalResult.failed()){
                    releaseFailedLoadLock(uuid);
                    player.kickPlayer(CC.color("&cYour player data is still syncing. Please rejoin in a few seconds."));
                    callback.accept(false);
                    return;
                }
                if (finalResult.snapshot() == null) {
                    loadedPlayers.add(uuid);
                    long now=System.currentTimeMillis();lastLoadTimes.put(uuid,now);loadedAt.put(uuid,now);
                    loadedVersions.put(uuid,0);loadedSnapshotItemCounts.put(uuid,countCurrentItems(player));
                    plugin.getSkillManager().loadSkillData(player);
                    completeHandoffAsync(uuid,0);
                    callback.accept(true);
                    return;
                }
                boolean applied = apply(player, finalResult.snapshot());
                if (applied) {
                    loadedPlayers.add(uuid);
                    long now=System.currentTimeMillis();lastLoadTimes.put(uuid,now);loadedAt.put(uuid,now);
                    loadedVersions.put(uuid,finalResult.snapshot().saveVersion());
                    loadedSnapshotItemCounts.put(uuid,countCurrentItems(player));
                    plugin.getSkillManager().loadSkillData(player);
                    completeHandoffAsync(uuid,finalResult.snapshot().saveVersion());
                    plugin.getLogger().info("[PlayerDataSync] Loaded data for " + player.getName() + " version=" + finalResult.snapshot().saveVersion());
                }
                else {
                    releaseFailedLoadLock(uuid);
                    player.kickPlayer(CC.color("&cYour player data is still syncing. Please rejoin in a few seconds."));
                }
                callback.accept(applied);
            });
        });
    }

    private LoadResult loadSnapshot(UUID uuid) throws SQLException {
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM `" + PlayerDataSyncRegistry.TABLE + "` WHERE `uuid`=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return LoadResult.empty();
                if (result.getInt("save_version") <= 0) return LoadResult.empty();
                return LoadResult.loaded(PlayerDataSnapshot.from(result));
            }
        }
    }

    private LoadResult loadCommittedSnapshot(UUID uuid,long deadline)throws SQLException{
        TransferHandoff handoff=readHandoff(uuid);
        int required=handoff!=null&&"READY".equals(handoff.state())&&handoff.expiresAt()>=System.currentTimeMillis()?handoff.savedVersion():0;
        do{
            LoadResult result=loadSnapshot(uuid);
            if(required<=0||result.snapshot()!=null&&result.snapshot().saveVersion()>=required)return result;
            try{Thread.sleep(PlayerDataSyncRegistry.JOIN_RETRY_DELAY_MILLIS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}
        }while(System.currentTimeMillis()<deadline);
        return LoadResult.failure();
    }

    private boolean apply(Player player, PlayerDataSnapshot snapshot) {
        try {
            ItemStack[] contents = decodeRequired(snapshot.inventory(), PlayerDataSyncRegistry.SYNC_INVENTORY, snapshot.uuid());
            ItemStack[] armor = decodeRequired(snapshot.armor(), PlayerDataSyncRegistry.SYNC_ARMOR, snapshot.uuid());
            ItemStack[] offhand = decodeRequired(snapshot.offhand(), PlayerDataSyncRegistry.SYNC_OFFHAND, snapshot.uuid());
            ItemStack[] ender = decodeRequired(snapshot.enderChest(), PlayerDataSyncRegistry.SYNC_ENDER_CHEST, snapshot.uuid());
            if (PlayerDataSyncRegistry.SYNC_INVENTORY && contents != null) player.getInventory().setContents(fit(contents, player.getInventory().getSize()));
            if (PlayerDataSyncRegistry.SYNC_ARMOR && armor != null) player.getInventory().setArmorContents(fit(armor, player.getInventory().getArmorContents().length));
            if (PlayerDataSyncRegistry.SYNC_OFFHAND && offhand != null) player.getInventory().setItemInOffHand(offhand.length == 0 ? null : offhand[0]);
            if (PlayerDataSyncRegistry.SYNC_ENDER_CHEST && ender != null) player.getEnderChest().setContents(fit(ender, player.getEnderChest().getSize()));
            if (PlayerDataSyncRegistry.SYNC_EXP) {
                player.setLevel(Math.max(0, snapshot.expLevel()));
                player.setExp((float) Math.clamp(snapshot.expProgress(), 0.0D, 1.0D));
                player.setTotalExperience(Math.max(0, snapshot.totalExp()));
            }
            if (PlayerDataSyncRegistry.SYNC_HEALTH) {
                player.setHealth(Math.clamp(snapshot.health(), 0.0D, player.getMaxHealth()));
                MMOStatService mmo = plugin.getMmoStatService();
                if (mmo != null) mmo.setMmoHealth(player, snapshot.customHealth());
            }
            if (PlayerDataSyncRegistry.SYNC_FOOD) {
                player.setFoodLevel(Math.clamp(snapshot.foodLevel(), 0, 20));
                player.setSaturation((float) Math.max(0.0D, snapshot.saturation()));
            }
            if (PlayerDataSyncRegistry.SYNC_EFFECTS) applyEffects(player, snapshot.effects());
            if (PlayerDataSyncRegistry.SYNC_GAMEMODE && notBlank(snapshot.gameMode())) {
                try {
                    player.setGameMode(GameMode.valueOf(snapshot.gameMode()));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (PlayerDataSyncRegistry.SYNC_LOCATION && notBlank(snapshot.worldName())) applyLocation(player, snapshot);
            manaManager.setMana(player, (int) Math.round(snapshot.mana()));
            player.updateInventory();
            manaManager.sendNow(player);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("[PlayerDataSync] Could not load synced playerdata for " + player.getName() + ". Keeping current data: " + exception.getMessage());
            player.sendMessage(CC.color("&cCould not load synced playerdata for " + player.getName() + ". Keeping current data."));
            return false;
        }
    }

    private void applyLocation(Player player, PlayerDataSnapshot snapshot) {
        World world = Bukkit.getWorld(snapshot.worldName());
        if (world == null) {
            player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            return;
        }
        player.teleport(new Location(world, snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), snapshot.pitch()));
    }

    private PlayerDataSnapshot capture(Player player) {
        MMOStatService mmo = plugin.getMmoStatService();
        Location location = player.getLocation();
        return new PlayerDataSnapshot(
                player.getUniqueId(),
                player.getName(),
                serverId,
                lockTokens.getOrDefault(player.getUniqueId(),""),
                location.getWorld() == null ? "" : location.getWorld().getName(),
                safeSerialize(PlayerDataSyncRegistry.SYNC_INVENTORY ? player.getInventory().getContents() : new ItemStack[0]),
                safeSerialize(PlayerDataSyncRegistry.SYNC_ARMOR ? player.getInventory().getArmorContents() : new ItemStack[0]),
                safeSerialize(PlayerDataSyncRegistry.SYNC_OFFHAND ? new ItemStack[]{player.getInventory().getItemInOffHand()} : new ItemStack[0]),
                safeSerialize(PlayerDataSyncRegistry.SYNC_ENDER_CHEST ? player.getEnderChest().getContents() : new ItemStack[0]),
                PlayerDataSyncRegistry.SYNC_EFFECTS ? serializeEffects(player.getActivePotionEffects()) : "",
                player.getGameMode().name(),
                player.getLevel(),
                player.getExp(),
                player.getTotalExperience(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                mmo == null ? player.getHealth() : mmo.currentMmoHealth(player),
                mmo == null ? player.getMaxHealth() : mmo.maxMmoHealth(player),
                manaManager.getMana(player),
                manaManager.getMaxMana(player),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                loadedVersions.getOrDefault(player.getUniqueId(),0),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    private void saveAsync(PlayerDataSnapshot snapshot, boolean releaseLock, Consumer<Boolean> callback) {
        if(!enabled){callback.accept(false);return;}
        UUID uuid=snapshot.uuid();
        if(!loadedPlayers.contains(uuid)){lastSaveResults.put(uuid,"BLOCKED_NOT_LOADED");callback.accept(false);return;}
        PlayerDataSnapshot effectiveSnapshot=snapshot.withSaveVersion(loadedVersions.getOrDefault(uuid,snapshot.saveVersion()));
        if (!savingPlayers.add(uuid)) {
            PendingSave pending=new PendingSave(effectiveSnapshot,releaseLock,callback);
            queuedSaves.compute(uuid,(ignored,current)->current!=null&&current.releaseLock()?current:pending);
            return;
        }
        if(effectiveSnapshot.lockToken()==null||effectiveSnapshot.lockToken().isBlank()){
            savingPlayers.remove(uuid);lastSaveResults.put(uuid,"BLOCKED_NO_TOKEN");
            callback.accept(false);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean saved = saveBlocking(effectiveSnapshot, releaseLock);
            savingPlayers.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(saved));
            PendingSave queued=queuedSaves.remove(uuid);
            if(queued!=null)saveAsync(queued.snapshot(),queued.releaseLock(),queued.callback());
        });
    }

    private boolean saveBlocking(PlayerDataSnapshot snapshot, boolean releaseLock) {
        if(!loadedPlayers.contains(snapshot.uuid())){lastSaveResults.put(snapshot.uuid(),"BLOCKED_NOT_LOADED");return false;}
        int loadedItems=loadedSnapshotItemCounts.getOrDefault(snapshot.uuid(),0),currentItems=countSnapshotItems(snapshot);
        if(loadedItems>0&&currentItems==0&&lastInventoryMutationTimes.getOrDefault(snapshot.uuid(),0L)<loadedAt.getOrDefault(snapshot.uuid(),Long.MAX_VALUE)){
            lastSaveResults.put(snapshot.uuid(),"BLOCKED_SUSPICIOUS_EMPTY");
            plugin.getLogger().warning("[SMP PlayerDataSync] Blocked suspicious empty save player="+snapshot.username()+" loadedItems="+loadedItems+" currentItems="+currentItems+" server="+serverId+" reason=data_not_loaded_or_no_mutation");
            return false;
        }
        try {
            int version = saveSnapshot(snapshot);
            plugin.getLogger().info("[PlayerDataSync] Saved data for " + snapshot.username() + " version=" + version);
            lastSaveTimes.put(snapshot.uuid(),System.currentTimeMillis());
            loadedVersions.put(snapshot.uuid(),version);lastSaveResults.put(snapshot.uuid(),"SAVED_VERSION_"+version);
            if (releaseLock) releaseLock(snapshot.uuid(),snapshot.lockToken());
            return true;
        } catch (LockOwnershipLostException ignored) {
            lastSaveResults.put(snapshot.uuid(),"STALE_TOKEN_OR_VERSION");
            return false;
        } catch (SQLException exception) {
            lastSaveResults.put(snapshot.uuid(),"SQL_ERROR");
            plugin.getLogger().warning("[PlayerDataSync] Could not save synced playerdata for " + snapshot.username() + ": " + exception.getMessage());
            return false;
        }
    }

    private int saveSnapshot(PlayerDataSnapshot snapshot) throws SQLException {
        long now = System.currentTimeMillis();
        long lockUntil = now + lockTtlSeconds * 1000L;
        if(snapshot.lockToken()==null||snapshot.lockToken().isBlank())throw new LockOwnershipLostException("No local lock token for this player session");
        try (Connection connection = dataManager.getConnection()) {
            int expectedVersion=Math.max(0,snapshot.saveVersion());
            int nextVersion = Math.max(SAVE_VERSION_START, expectedVersion + 1);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE `" + PlayerDataSyncRegistry.TABLE + "` SET " +
                            "`username`=?, `server_id`=?, `world_name`=?, `inventory`=?, `armor`=?, `offhand`=?, " +
                            "`ender_chest`=?, `effects`=?, `gamemode`=?, `exp_level`=?, `exp_progress`=?, `total_exp`=?, " +
                            "`health`=?, `food_level`=?, `saturation`=?, `custom_health`=?, `custom_max_health`=?, " +
                            "`mana`=?, `max_mana`=?, `last_save`=?, `last_load`=?, `save_version`=?, `dirty`=?, `locked_by`=?, " +
                            "`lock_until`=?, `heartbeat_at`=?, `expires_at`=?, `transfer_state`='ACTIVE', `target_server_id`='', `x`=?, `y`=?, `z`=?, `yaw`=?, `pitch`=? " +
                             "WHERE `uuid`=? AND `locked_by`=? AND `lock_token`=? AND `save_version`=? AND `expires_at`>=?")) {
                int i = 1;
                statement.setString(i++, snapshot.username());
                statement.setString(i++, serverId);
                statement.setString(i++, snapshot.worldName());
                statement.setString(i++, snapshot.inventory());
                statement.setString(i++, snapshot.armor());
                statement.setString(i++, snapshot.offhand());
                statement.setString(i++, snapshot.enderChest());
                statement.setString(i++, snapshot.effects());
                statement.setString(i++, snapshot.gameMode());
                statement.setInt(i++, snapshot.expLevel());
                statement.setDouble(i++, snapshot.expProgress());
                statement.setInt(i++, snapshot.totalExp());
                statement.setDouble(i++, snapshot.health());
                statement.setInt(i++, snapshot.foodLevel());
                statement.setFloat(i++, snapshot.saturation());
                statement.setDouble(i++, snapshot.customHealth());
                statement.setDouble(i++, snapshot.customMaxHealth());
                statement.setDouble(i++, snapshot.mana());
                statement.setDouble(i++, snapshot.maxMana());
                statement.setLong(i++, now);
                statement.setLong(i++, snapshot.lastLoad());
                statement.setInt(i++, nextVersion);
                statement.setBoolean(i++, false);
                statement.setString(i++, serverId);
                statement.setLong(i++, lockUntil);
                statement.setLong(i++,now);
                statement.setLong(i++,lockUntil);
                statement.setDouble(i++, snapshot.x());
                statement.setDouble(i++, snapshot.y());
                statement.setDouble(i++, snapshot.z());
                statement.setFloat(i++, snapshot.yaw());
                statement.setFloat(i++, snapshot.pitch());
                statement.setString(i++, snapshot.uuid().toString());
                statement.setString(i++, serverId);
                statement.setString(i++,snapshot.lockToken());
                statement.setInt(i++,expectedVersion);
                statement.setLong(i++,now-staleLockGraceSeconds*1000L);
                if (statement.executeUpdate() <= 0) {
                    LockInfo info=readLock(connection,snapshot.uuid());
                    boolean stale=info!=null&&info.expiresAt()<now;
                    boolean onlineHere=loadedPlayers.contains(snapshot.uuid());
                    String log="[SMP PlayerDataSync] Save skipped player="+snapshot.username()+" reason=lock_owned_by_other currentServer="+serverId+" owner="+(info==null?"none":info.owner())+" onlineHere="+onlineHere+" stale="+stale;
                    if(onlineHere)plugin.getLogger().warning(log);else plugin.getLogger().fine(log);
                    throw new LockOwnershipLostException("Playerdata lock token is no longer owned by this session");
                }
            }
            return nextVersion;
        }
    }

    private LockAttempt acquireLockWithRetry(UUID uuid,String playerName,long deadline)throws SQLException{
        LockAttempt attempt=null;
        SQLException lastFailure=null;
        for(int retry=0;retry<PlayerDataSyncRegistry.JOIN_LOCK_RETRIES&&System.currentTimeMillis()<deadline;retry++){
            try{attempt=acquireLock(uuid,playerName);}catch(SQLException exception){lastFailure=exception;}
            if(attempt!=null&&attempt.acquired())return attempt;
            try{Thread.sleep(PlayerDataSyncRegistry.JOIN_RETRY_DELAY_MILLIS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}
        }
        if(attempt==null&&lastFailure!=null)throw lastFailure;
        return attempt==null?new LockAttempt(false,"unknown",false):attempt;
    }

    private LockAttempt acquireLock(UUID uuid,String playerName)throws SQLException{
        long now=System.currentTimeMillis(),expires=now+lockTtlSeconds*1000L;
        String token=lockTokens.computeIfAbsent(uuid,ignored->UUID.randomUUID().toString());
        try(Connection connection=dataManager.getConnection()){
            connection.setAutoCommit(false);
            try{
                LockInfo old=readLockForUpdate(connection,uuid);
                TransferHandoff handoff=readHandoff(connection,uuid,true);
                boolean absent=old==null,free=absent||old.owner()==null||old.owner().isBlank();
                boolean stale=!absent&&old.expiresAt()<now;
                boolean ours=!absent&&serverId.equals(old.owner())&&token.equals(old.token());
                boolean readyHandoff=!absent&&handoff!=null&&handoff.expiresAt()>=now&&"READY".equals(handoff.state())
                        && "TRANSFERRING".equalsIgnoreCase(old.transferState())
                        && (handoff.targetServer()==null||handoff.targetServer().isBlank()||serverId.equalsIgnoreCase(handoff.targetServer()));
                if(absent){try(PreparedStatement insert=connection.prepareStatement("INSERT INTO `"+PlayerDataSyncRegistry.TABLE+"` (`uuid`,`server_id`,`last_load`,`save_version`,`dirty`,`locked_by`,`lock_until`,`lock_token`,`locked_at`,`heartbeat_at`,`expires_at`,`transfer_state`,`target_server_id`,`transfer_started_at`) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)")){insert.setString(1,uuid.toString());insert.setString(2,serverId);insert.setLong(3,now);insert.setInt(4,0);insert.setBoolean(5,false);insert.setString(6,serverId);insert.setLong(7,expires);insert.setString(8,token);insert.setLong(9,now);insert.setLong(10,now);insert.setLong(11,expires);insert.setString(12,"ACTIVE");insert.setString(13,"");insert.setLong(14,0L);insert.executeUpdate();}}
                else if(free||stale||ours||readyHandoff||(serverId.equals(old.owner())&&(old.token()==null||old.token().isBlank()))){try(PreparedStatement update=connection.prepareStatement("UPDATE `"+PlayerDataSyncRegistry.TABLE+"` SET locked_by=?,lock_until=?,lock_token=?,locked_at=?,heartbeat_at=?,expires_at=?,transfer_state='ACTIVE',target_server_id='',transfer_started_at=0,last_load=? WHERE uuid=?")){update.setString(1,serverId);update.setLong(2,expires);update.setString(3,token);update.setLong(4,now);update.setLong(5,now);update.setLong(6,expires);update.setLong(7,now);update.setString(8,uuid.toString());update.executeUpdate();}}
                else{connection.rollback();return new LockAttempt(false,old.owner(),false);}
                connection.commit();
                if(stale&&!serverId.equals(old.owner()))plugin.getLogger().info("[SMP PlayerDataSync] Took over stale lock for "+playerName+" oldServer="+old.owner()+" newServer="+serverId);
                return new LockAttempt(true,serverId,stale);
            }catch(SQLException exception){connection.rollback();throw exception;}finally{connection.setAutoCommit(true);}
        }
    }

    private int currentVersion(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT `save_version` FROM `" + PlayerDataSyncRegistry.TABLE + "` WHERE `uuid`=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Math.max(0, result.getInt("save_version")) : 0;
            }
        }
    }

    private LockInfo readLockForUpdate(Connection connection,UUID uuid)throws SQLException{return readLock(connection,uuid," FOR UPDATE");}
    private LockInfo readLock(Connection connection,UUID uuid)throws SQLException{return readLock(connection,uuid,"");}
    private LockInfo readLock(Connection connection,UUID uuid,String suffix)throws SQLException{try(PreparedStatement statement=connection.prepareStatement("SELECT locked_by,lock_token,locked_at,heartbeat_at,expires_at,transfer_state,target_server_id,transfer_started_at FROM `"+PlayerDataSyncRegistry.TABLE+"` WHERE uuid=?"+suffix)){statement.setString(1,uuid.toString());try(ResultSet result=statement.executeQuery()){return result.next()?new LockInfo(result.getString(1),result.getString(2),result.getLong(3),result.getLong(4),result.getLong(5),result.getString(6),result.getString(7),result.getLong(8)):null;}}}

    private void releaseLock(UUID uuid,String token) throws SQLException {
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE `" + PlayerDataSyncRegistry.TABLE + "` SET `locked_by`='', `lock_until`=0,`lock_token`='',`expires_at`=0,`transfer_state`='OFFLINE',`target_server_id`='' WHERE `uuid`=? AND `locked_by`=? AND `lock_token`=?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, serverId);
            statement.setString(3,token);
            statement.executeUpdate();
        }
        lockTokens.remove(uuid,token);
    }

    private boolean releaseAnyLock(UUID uuid) {
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE `" + PlayerDataSyncRegistry.TABLE + "` SET `locked_by`='', `lock_until`=0,`lock_token`='',`expires_at`=0,`transfer_state`='ADMIN_UNLOCKED',`target_server_id`='' WHERE `uuid`=?")) {
            statement.setString(1, uuid.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            plugin.getLogger().warning("[PlayerDataSync] Could not unlock playerdata: " + exception.getMessage());
            return false;
        }
    }

    private boolean beginTransferHandoff(UUID uuid,String targetServer){long now=System.currentTimeMillis(),expires=now+Math.max(30,lockTtlSeconds)*1000L;try(Connection connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("INSERT INTO smp_player_transfer_handoff(player_uuid,source_server,target_server,state,saved_version,started_at,completed_at,expires_at) VALUES(?,?,?,'SAVING',0,?,NULL,?) ON DUPLICATE KEY UPDATE source_server=VALUES(source_server),target_server=VALUES(target_server),state='SAVING',saved_version=0,started_at=VALUES(started_at),completed_at=NULL,expires_at=VALUES(expires_at)")){statement.setString(1,uuid.toString());statement.setString(2,serverId);statement.setString(3,targetServer==null?"":targetServer);statement.setLong(4,now);statement.setLong(5,expires);statement.executeUpdate();return true;}catch(SQLException exception){plugin.getLogger().warning("[PlayerDataSync] Could not begin transfer handoff for "+uuid+": "+exception.getMessage());return false;}}

    private boolean markTransferReady(UUID uuid,String token,String targetServer,int savedVersion){long now=System.currentTimeMillis(),expires=now+PlayerDataSyncRegistry.TRANSFER_LOCK_SECONDS*1000L;try(Connection connection=dataManager.getConnection()){connection.setAutoCommit(false);try{int main;try(PreparedStatement statement=connection.prepareStatement("UPDATE `"+PlayerDataSyncRegistry.TABLE+"` SET transfer_state='TRANSFERRING',target_server_id=?,transfer_started_at=?,heartbeat_at=?,expires_at=?,lock_until=? WHERE uuid=? AND locked_by=? AND lock_token=? AND save_version=?")){statement.setString(1,targetServer==null?"":targetServer);statement.setLong(2,now);statement.setLong(3,now);statement.setLong(4,expires);statement.setLong(5,expires);statement.setString(6,uuid.toString());statement.setString(7,serverId);statement.setString(8,token);statement.setInt(9,savedVersion);main=statement.executeUpdate();}int handoff;try(PreparedStatement statement=connection.prepareStatement("UPDATE smp_player_transfer_handoff SET state='READY',saved_version=?,expires_at=? WHERE player_uuid=? AND source_server=? AND state='SAVING'")){statement.setInt(1,savedVersion);statement.setLong(2,now+Math.max(30,lockTtlSeconds)*1000L);statement.setString(3,uuid.toString());statement.setString(4,serverId);handoff=statement.executeUpdate();}if(main!=1||handoff!=1){connection.rollback();return false;}connection.commit();return true;}catch(SQLException exception){connection.rollback();throw exception;}finally{connection.setAutoCommit(true);}}catch(SQLException exception){plugin.getLogger().warning("[PlayerDataSync] Could not mark transfer READY for "+uuid+": "+exception.getMessage());return false;}}

    private void markHandoffFailedAsync(UUID uuid){Bukkit.getScheduler().runTaskAsynchronously(plugin,()->markHandoffFailed(uuid));}
    private void markHandoffFailed(UUID uuid){expireHandoff(uuid,"FAILED");}
    private void expireHandoff(UUID uuid,String state){try(Connection connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE smp_player_transfer_handoff SET state=?,completed_at=? WHERE player_uuid=?")){statement.setString(1,state);statement.setLong(2,System.currentTimeMillis());statement.setString(3,uuid.toString());statement.executeUpdate();}catch(SQLException exception){plugin.getLogger().warning("[PlayerDataSync] Could not update transfer handoff for "+uuid+": "+exception.getMessage());}}
    private void completeHandoffAsync(UUID uuid,int version){Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try(Connection connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE smp_player_transfer_handoff SET state='LOADED',completed_at=? WHERE player_uuid=? AND state='READY' AND saved_version<=?")){statement.setLong(1,System.currentTimeMillis());statement.setString(2,uuid.toString());statement.setInt(3,version);statement.executeUpdate();}catch(SQLException exception){plugin.getLogger().warning("[PlayerDataSync] Could not complete transfer handoff for "+uuid+": "+exception.getMessage());}});}
    private TransferHandoff readHandoff(UUID uuid)throws SQLException{try(Connection connection=dataManager.getConnection()){return readHandoff(connection,uuid,false);}}
    private TransferHandoff readHandoff(Connection connection,UUID uuid,boolean forUpdate)throws SQLException{try(PreparedStatement statement=connection.prepareStatement("SELECT source_server,target_server,state,saved_version,started_at,completed_at,expires_at FROM smp_player_transfer_handoff WHERE player_uuid=?"+(forUpdate?" FOR UPDATE":""))){statement.setString(1,uuid.toString());try(ResultSet result=statement.executeQuery()){return result.next()?new TransferHandoff(result.getString(1),result.getString(2),result.getString(3),result.getInt(4),result.getLong(5),result.getLong(6),result.getLong(7)):null;}}}

    private void heartbeatOwnedLocks(){
        if(!enabled)return;
        long now=System.currentTimeMillis(),expires=now+lockTtlSeconds*1000L;
        heartbeatServerIdentity(now);
        for(var entry:new ArrayList<>(lockTokens.entrySet())){
            if(transferringPlayers.contains(entry.getKey()))continue;
            try(Connection connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE `"+PlayerDataSyncRegistry.TABLE+"` SET heartbeat_at=?,expires_at=?,lock_until=? WHERE uuid=? AND locked_by=? AND lock_token=?")){statement.setLong(1,now);statement.setLong(2,expires);statement.setLong(3,expires);statement.setString(4,entry.getKey().toString());statement.setString(5,serverId);statement.setString(6,entry.getValue());if(statement.executeUpdate()==0)lockTokens.remove(entry.getKey(),entry.getValue());}catch(SQLException exception){plugin.getLogger().warning("[PlayerDataSync] Lock heartbeat failed: "+exception.getMessage());break;}
        }
    }

    private void ensureTable() throws SQLException {
        try (Connection connection = dataManager.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + PlayerDataSyncRegistry.TABLE + "` (" +
                    "`uuid` VARCHAR(36) PRIMARY KEY, " +
                    "`username` VARCHAR(16), " +
                    "`server_id` VARCHAR(64), " +
                    "`world_name` VARCHAR(128), " +
                    "`inventory` LONGTEXT, " +
                    "`armor` LONGTEXT, " +
                    "`offhand` LONGTEXT, " +
                    "`ender_chest` LONGTEXT, " +
                    "`effects` LONGTEXT, " +
                    "`gamemode` VARCHAR(32), " +
                    "`exp_level` INT, " +
                    "`exp_progress` DOUBLE, " +
                    "`total_exp` INT, " +
                    "`health` DOUBLE, " +
                    "`food_level` INT, " +
                    "`saturation` FLOAT, " +
                    "`custom_health` DOUBLE, " +
                    "`custom_max_health` DOUBLE, " +
                    "`mana` DOUBLE, " +
                    "`max_mana` DOUBLE, " +
                    "`last_save` BIGINT, " +
                    "`last_load` BIGINT, " +
                    "`save_version` INT, " +
                    "`dirty` BOOLEAN, " +
                    "`locked_by` VARCHAR(64), " +
                    "`lock_until` BIGINT, " +
                    "`lock_token` VARCHAR(64), " +
                    "`locked_at` BIGINT, " +
                    "`heartbeat_at` BIGINT, " +
                    "`expires_at` BIGINT, " +
                    "`transfer_state` VARCHAR(32), " +
                    "`target_server_id` VARCHAR(64), " +
                    "`transfer_started_at` BIGINT, " +
                    "`x` DOUBLE, " +
                    "`y` DOUBLE, " +
                    "`z` DOUBLE, " +
                    "`yaw` FLOAT, " +
                    "`pitch` FLOAT, " +
                    "`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");
            statement.executeUpdate("ALTER TABLE `"+PlayerDataSyncRegistry.TABLE+"` ADD COLUMN IF NOT EXISTS `lock_token` VARCHAR(64) NULL");
            statement.executeUpdate("ALTER TABLE `"+PlayerDataSyncRegistry.TABLE+"` ADD COLUMN IF NOT EXISTS `locked_at` BIGINT NOT NULL DEFAULT 0");
            statement.executeUpdate("ALTER TABLE `"+PlayerDataSyncRegistry.TABLE+"` ADD COLUMN IF NOT EXISTS `heartbeat_at` BIGINT NOT NULL DEFAULT 0");
            statement.executeUpdate("ALTER TABLE `"+PlayerDataSyncRegistry.TABLE+"` ADD COLUMN IF NOT EXISTS `expires_at` BIGINT NOT NULL DEFAULT 0");
            statement.executeUpdate("ALTER TABLE `"+PlayerDataSyncRegistry.TABLE+"` ADD COLUMN IF NOT EXISTS `transfer_state` VARCHAR(32) NOT NULL DEFAULT 'OFFLINE'");
            statement.executeUpdate("ALTER TABLE `"+PlayerDataSyncRegistry.TABLE+"` ADD COLUMN IF NOT EXISTS `target_server_id` VARCHAR(64) NOT NULL DEFAULT ''");
            statement.executeUpdate("ALTER TABLE `"+PlayerDataSyncRegistry.TABLE+"` ADD COLUMN IF NOT EXISTS `transfer_started_at` BIGINT NOT NULL DEFAULT 0");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS smp_playerdata_servers (server_id VARCHAR(64) PRIMARY KEY,instance_id VARCHAR(64) NOT NULL,server_type VARCHAR(32) NOT NULL,heartbeat_at BIGINT NOT NULL,started_at BIGINT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS smp_player_transfer_handoff (player_uuid VARCHAR(36) PRIMARY KEY,source_server VARCHAR(64) NOT NULL,target_server VARCHAR(64) NULL,state VARCHAR(32) NOT NULL,saved_version BIGINT NOT NULL DEFAULT 0,started_at BIGINT NOT NULL,completed_at BIGINT NULL,expires_at BIGINT NOT NULL)");
        }
    }

    private boolean claimServerIdentity()throws SQLException{long now=System.currentTimeMillis();String type=plugin.getNetworkService().isHub()?"HUB":plugin.getNetworkService().isDungeon()?"DUNGEON":"OVERWORLD";try(Connection connection=dataManager.getConnection()){connection.setAutoCommit(false);try{String oldInstance=null;long oldHeartbeat=0;try(PreparedStatement select=connection.prepareStatement("SELECT instance_id,heartbeat_at FROM smp_playerdata_servers WHERE server_id=? FOR UPDATE")){select.setString(1,serverId);try(ResultSet result=select.executeQuery()){if(result.next()){oldInstance=result.getString(1);oldHeartbeat=result.getLong(2);}}}boolean duplicate=oldInstance!=null&&!oldInstance.equals(serverInstanceId)&&oldHeartbeat>=now-staleLockGraceSeconds*1000L;if(!duplicate){try(PreparedStatement replace=connection.prepareStatement("INSERT INTO smp_playerdata_servers(server_id,instance_id,server_type,heartbeat_at,started_at) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE instance_id=VALUES(instance_id),server_type=VALUES(server_type),heartbeat_at=VALUES(heartbeat_at),started_at=VALUES(started_at)")){replace.setString(1,serverId);replace.setString(2,serverInstanceId);replace.setString(3,type);replace.setLong(4,now);replace.setLong(5,now);replace.executeUpdate();}lastServerHeartbeat=now;}connection.commit();return !duplicate;}catch(SQLException exception){connection.rollback();throw exception;}finally{connection.setAutoCommit(true);}}}
    private void heartbeatServerIdentity(long now){try(Connection connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE smp_playerdata_servers SET heartbeat_at=? WHERE server_id=? AND instance_id=?")){statement.setLong(1,now);statement.setString(2,serverId);statement.setString(3,serverInstanceId);serverIdentityUnique=statement.executeUpdate()>0;if(serverIdentityUnique)lastServerHeartbeat=now;}catch(SQLException exception){serverIdentityUnique=false;plugin.getLogger().warning("[PlayerDataSync] Server identity heartbeat failed: "+exception.getMessage());}}
    private void releaseServerIdentity(){try(Connection connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("DELETE FROM smp_playerdata_servers WHERE server_id=? AND instance_id=?")){statement.setString(1,serverId);statement.setString(2,serverInstanceId);statement.executeUpdate();}catch(SQLException ignored){}}

    private String safeSerialize(ItemStack[] items) {
        try {
            return serializeItems(items);
        } catch (Exception exception) {
            plugin.getLogger().warning("[PlayerDataSync] Failed to serialize playerdata inventory snapshot: " + exception.getMessage());
            return "";
        }
    }

    private int countCurrentItems(Player player){return countItems(player.getInventory().getContents())+countItems(player.getInventory().getArmorContents())+countItems(new ItemStack[]{player.getInventory().getItemInOffHand()})+countItems(player.getEnderChest().getContents());}
    private int countSnapshotItems(PlayerDataSnapshot snapshot){return countEncoded(snapshot.inventory(),snapshot.uuid())+countEncoded(snapshot.armor(),snapshot.uuid())+countEncoded(snapshot.offhand(),snapshot.uuid())+countEncoded(snapshot.enderChest(),snapshot.uuid());}
    private int countEncoded(String encoded,UUID uuid){if(!notBlank(encoded))return 0;ItemStack[] items=deserializeItems(encoded,uuid);return countItems(items);}
    private int countItems(ItemStack[] items){if(items==null)return 0;int count=0;for(ItemStack item:items)if(item!=null&&!item.getType().isAir()&&item.getAmount()>0)count++;return count;}

    private boolean inventoryReady(UUID uuid){return isPlayerDataLoaded(uuid)&&!isTransferPending(uuid);}
    private void clearSessionState(UUID uuid,boolean removeToken){loadedPlayers.remove(uuid);loadingPlayers.remove(uuid);transferPreparingPlayers.remove(uuid);transferringPlayers.remove(uuid);loadedVersions.remove(uuid);loadedSnapshotItemCounts.remove(uuid);loadedAt.remove(uuid);lastInventoryMutationTimes.remove(uuid);queuedSaves.remove(uuid);if(removeToken)lockTokens.remove(uuid);}
    private void clearPreviousSessionAfterSave(UUID uuid){boolean loading=loadingPlayers.contains(uuid);clearSessionState(uuid,true);if(loading)loadingPlayers.add(uuid);}
    private void releaseFailedLoadLock(UUID uuid){String token=lockTokens.get(uuid);if(token==null||token.isBlank())return;Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{releaseLock(uuid,token);}catch(SQLException exception){plugin.getLogger().warning("[PlayerDataSync] Could not release failed-load lock for "+uuid+": "+exception.getMessage());}});}

    private String serializeItems(ItemStack[] items) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeInt(items.length);
            for (ItemStack item : items) output.writeObject(item);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private ItemStack[] deserializeItems(String encoded, UUID uuid) {
        try {
            byte[] raw = Base64.getDecoder().decode(encoded);
            try (BukkitObjectInputStream input = new BukkitObjectInputStream(new ByteArrayInputStream(raw))) {
                int length = Math.max(0, Math.min(200, input.readInt()));
                ItemStack[] items = new ItemStack[length];
                for (int slot = 0; slot < length; slot++) {
                    Object object = input.readObject();
                    items[slot] = object instanceof ItemStack item ? item : null;
                }
                return items;
            }
        } catch (Exception exception) {
            if (warnedCorruptPlayers.add(uuid)) {
                plugin.getLogger().warning("[PlayerDataSync] Failed to deserialize data for " + uuid + "; keeping current inventory");
            }
            return null;
        }
    }

    private ItemStack[] decodeRequired(String encoded, boolean enabled, UUID uuid) {
        if (!enabled) return null;
        if (!notBlank(encoded)) throw new IllegalStateException("Missing synchronized inventory payload");
        ItemStack[] decoded = deserializeItems(encoded, uuid);
        if (decoded == null) throw new IllegalStateException("Invalid synchronized inventory payload");
        return decoded;
    }

    private ItemStack[] fit(ItemStack[] source, int size) {
        ItemStack[] target = new ItemStack[size];
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
        return target;
    }

    private String serializeEffects(Collection<PotionEffect> effects) {
        List<String> parts = new ArrayList<>();
        for (PotionEffect effect : effects) {
            parts.add(effect.getType().getKey() + "," + effect.getDuration() + "," + effect.getAmplifier() + ","
                    + effect.isAmbient() + "," + effect.hasParticles() + "," + effect.hasIcon());
        }
        return String.join(";", parts);
    }

    private void applyEffects(Player player, String encoded) {
        for (PotionEffect effect : new HashSet<>(player.getActivePotionEffects())) player.removePotionEffect(effect.getType());
        if (encoded == null || encoded.isBlank()) return;
        for (String part : encoded.split(";")) {
            String[] fields = part.split(",");
            if (fields.length < 6) continue;
            NamespacedKey key = NamespacedKey.fromString(fields[0]);
            PotionEffectType type = key == null ? null : Registry.EFFECT.get(key);
            if (type == null) continue;
            try {
                player.addPotionEffect(new PotionEffect(type, Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
                        Boolean.parseBoolean(fields[3]), Boolean.parseBoolean(fields[4]), Boolean.parseBoolean(fields[5])));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String resolveServerId() {
        String value = PlayerDataSyncRegistry.stringValue("SMP_SERVER_ID", "");
        if (value != null && !value.isBlank()) return value.substring(0, Math.min(64, value.length()));
        String name = Bukkit.getServer().getName();
        if (name != null && !name.isBlank()) return name.substring(0, Math.min(64, name.length()));
        return PlayerDataSyncRegistry.SERVER_ID_FALLBACK;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.playerdatasync.admin")) {
            sender.sendMessage(CC.color("&cYou do not have permission to use this command."));
            return true;
        }
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            sender.sendMessage(CC.color(statusLine()));
            return true;
        }
        if (!enabled) {
            sender.sendMessage(CC.color("&ePlayerdata sync is disabled."));
            return true;
        }
        if(args[1].equalsIgnoreCase("locks")){listLocks(sender);return true;}
        if(args[1].equalsIgnoreCase("validate")){validateLocks(sender);return true;}
        if (args.length < 3) {
            sender.sendMessage(CC.color("&cUsage: /smp playerdata <lock|unlock|debug|transfer|locks|validate|save|load> [player]"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "save" -> {if(target==null){sender.sendMessage(CC.color("&cThat player must be online."));return true;}saveCommand(target, success -> sender.sendMessage(CC.color(success
                    ? "&aSaved synced playerdata for " + target.getName() + "."
                    : "&cCould not save synced playerdata for " + target.getName() + ".")));}
            case "load" -> {if(target==null){sender.sendMessage(CC.color("&cThat player must be online."));return true;}loadCommand(target, success -> sender.sendMessage(CC.color(success
                    ? "&aLoaded synced playerdata for " + target.getName() + "."
                    : "&cCould not load synced playerdata for " + target.getName() + ". Keeping current data.")));}
            case "debug" -> debugLock(sender,args[2]);
            case "transfer" -> debugLock(sender,args[2]);
            case "lock" -> {if(target==null){sender.sendMessage(CC.color("&cThat player must be online on this server."));return true;}Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{LockAttempt result=acquireLock(target.getUniqueId(),target.getName());reply(sender,result.acquired()?"&aLock acquired for "+target.getName()+" token="+lockTokens.get(target.getUniqueId()):"&cLock is actively owned by "+result.owner());}catch(SQLException exception){reply(sender,"&cCould not acquire lock: "+exception.getMessage());}});}
            case "unlock" -> {if(args.length<4||!args[3].equalsIgnoreCase("confirm")){sender.sendMessage(CC.color("&cConfirm with /smp playerdata unlock "+args[2]+" confirm"));return true;}safeAdminUnlock(sender,args[2]);}
            default -> sender.sendMessage(CC.color("&cUsage: /smp playerdata <lock|unlock|debug|transfer|locks|validate|save|load> [player]"));
        }
        return true;
    }

    private void reply(CommandSender sender,String message){Bukkit.getScheduler().runTask(plugin,()->sender.sendMessage(CC.color(message)));}

    private UUID findPlayerUuid(Connection connection,String name)throws SQLException{Player online=Bukkit.getPlayerExact(name);if(online!=null)return online.getUniqueId();try(PreparedStatement statement=connection.prepareStatement("SELECT uuid FROM `"+PlayerDataSyncRegistry.TABLE+"` WHERE LOWER(username)=LOWER(?) LIMIT 1")){statement.setString(1,name);try(ResultSet result=statement.executeQuery()){if(!result.next())return null;try{return UUID.fromString(result.getString(1));}catch(Exception ignored){return null;}}}}

    private void debugLock(CommandSender sender,String name){Player local=Bukkit.getPlayerExact(name);int currentItems=local==null?0:countCurrentItems(local);Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try(Connection connection=dataManager.getConnection()){UUID uuid=findPlayerUuid(connection,name);if(uuid==null){reply(sender,"&cPlayerdata row not found.");return;}LockInfo info=readLock(connection,uuid);TransferHandoff handoff=readHandoff(connection,uuid,false);long now=System.currentTimeMillis();boolean online=local!=null&&local.isOnline();int dbVersion=currentVersion(connection,uuid);List<String> lines=List.of("&7Player: &e"+name,"&7Online Here: &e"+online,"&7This Server: &e"+serverId,"&7PlayerData Loaded: &e"+isPlayerDataLoaded(uuid),"&7PlayerData Loading: &e"+isPlayerDataLoading(uuid),"&7Loaded Version: &e"+loadedVersions.getOrDefault(uuid,0),"&7DB Version: &e"+dbVersion,"&7Lock Owner: &e"+(info==null?"none":info.owner()),"&7Lock Token Matches: &e"+(info!=null&&lockTokens.getOrDefault(uuid,"").equals(info.token())),"&7Transfer State: &e"+(handoff==null?(info==null?"none":info.transferState()):handoff.state()),"&7Transfer Source: &e"+(handoff==null?"none":handoff.sourceServer()),"&7Transfer Target: &e"+(handoff==null?"none":handoff.targetServer()),"&7Transfer Saved Version: &e"+(handoff==null?0:handoff.savedVersion()),"&7Loaded Snapshot Items: &e"+loadedSnapshotItemCounts.getOrDefault(uuid,0),"&7Current Snapshot Items: &e"+currentItems,"&7Last Inventory Mutation: &e"+lastInventoryMutationTimes.getOrDefault(uuid,0L),"&7Last Load: &e"+lastLoadTimes.getOrDefault(uuid,0L),"&7Last Save: &e"+lastSaveTimes.getOrDefault(uuid,0L),"&7Queued Saves: &e"+((savingPlayers.contains(uuid)?1:0)+(queuedSaves.containsKey(uuid)?1:0)),"&7Last Save Result: &e"+lastSaveResults.getOrDefault(uuid,"NONE"),"&7Expires At: &e"+(info==null?0:info.expiresAt()),"&7Is Stale: &e"+(info==null||info.expiresAt()<now));Bukkit.getScheduler().runTask(plugin,()->lines.forEach(line->sender.sendMessage(CC.color(line))));}catch(SQLException exception){reply(sender,"&cCould not inspect lock: "+exception.getMessage());}});}

    private void listLocks(CommandSender sender){Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{List<String> lines=new ArrayList<>();try(Connection connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("SELECT username,locked_by,heartbeat_at,expires_at,transfer_state FROM `"+PlayerDataSyncRegistry.TABLE+"` WHERE locked_by IS NOT NULL AND locked_by<>'' ORDER BY expires_at LIMIT 50");ResultSet result=statement.executeQuery()){long now=System.currentTimeMillis();while(result.next())lines.add("&7- &e"+result.getString(1)+" &7owner=&f"+result.getString(2)+" &7stale="+(result.getLong(4)<now)+" &7state=&f"+result.getString(5));}catch(SQLException exception){lines.add("&cCould not list locks: "+exception.getMessage());}Bukkit.getScheduler().runTask(plugin,()->{sender.sendMessage(CC.color("&7Active playerdata locks: &e"+lines.size()));lines.forEach(line->sender.sendMessage(CC.color(line)));});});}

    private void safeAdminUnlock(CommandSender sender,String name){Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try(Connection connection=dataManager.getConnection()){UUID uuid=findPlayerUuid(connection,name);if(uuid==null){reply(sender,"&cPlayerdata row not found.");return;}LockInfo info=readLock(connection,uuid);long now=System.currentTimeMillis();boolean stale=info==null||info.expiresAt()<now;boolean present=false;try(PreparedStatement presence=connection.prepareStatement("SELECT 1 FROM smp_player_presence WHERE player_uuid=? AND updated_at>=? LIMIT 1")){presence.setString(1,uuid.toString());presence.setLong(2,now-staleLockGraceSeconds*1000L);try(ResultSet result=presence.executeQuery()){present=result.next();}}if((Bukkit.getPlayer(uuid)!=null||present)&&!stale){reply(sender,"&cRefusing to unlock an active network player.");return;}boolean unlocked=releaseAnyLock(uuid);reply(sender,unlocked?"&aUnlocked stale/offline playerdata for "+name+".":"&cNo lock was released.");}catch(SQLException exception){reply(sender,"&cCould not validate unlock safety: "+exception.getMessage());}});}

    private void validateLocks(CommandSender sender){boolean validId=serverId!=null&&!serverId.isBlank()&&!PlayerDataSyncRegistry.SERVER_ID_FALLBACK.equalsIgnoreCase(serverId);boolean heartbeatHealthy=serverIdentityUnique&&System.currentTimeMillis()-lastServerHeartbeat<heartbeatIntervalSeconds*3000L;sender.sendMessage(CC.color("&7[SMP PlayerDataSync] Validation:"));sender.sendMessage(CC.color("&7SMP_SERVER_ID present/unique: "+(validId&&serverIdentityUnique?"&atrue":"&cfalse")));sender.sendMessage(CC.color("&7SMP_SERVER_TYPE present: &atrue"));sender.sendMessage(CC.color("&7Current server heartbeat updating: "+(heartbeatHealthy?"&atrue":"&cfalse")));sender.sendMessage(CC.color("&7Join pending-load barrier: &atrue &7(20 retries x 500ms)"));sender.sendMessage(CC.color("&7Autosave disabled while loading: &atrue&7, inventory mutations blocked: &atrue"));sender.sendMessage(CC.color("&7Dupe scans skip while loading: &atrue&7, requirement checks pending: &atrue"));sender.sendMessage(CC.color("&7Token + atomic version guarded saves: &atrue&7, stale async overwrite: &cblocked"));sender.sendMessage(CC.color("&7Suspicious empty snapshot protection: &atrue"));sender.sendMessage(CC.color("&7Transfer handoff SAVING -> READY -> LOADED: &atrue"));sender.sendMessage(CC.color("&7Target waits for READY/latest version: &atrue&7, fast switch/relog protection: &atrue"));sender.sendMessage(CC.color("&7Lock TTL: &e"+lockTtlSeconds+"s&7, heartbeat: &e"+heartbeatIntervalSeconds+"s&7, stale grace: &e"+staleLockGraceSeconds+"s"));sender.sendMessage(CC.color("&7Duplicate active lock rows possible: &cfalse &7(primary key per UUID)"));}

    private record LoadResult(PlayerDataSnapshot snapshot, boolean locked, boolean failed) {
        private static LoadResult empty() {
            return new LoadResult(null, false, false);
        }

        private static LoadResult loaded(PlayerDataSnapshot snapshot) {
            return new LoadResult(snapshot, false, false);
        }

        private static LoadResult lockedResult() {
            return new LoadResult(null, true, false);
        }

        private static LoadResult failure() {
            return new LoadResult(null, false, true);
        }
    }

    private record PendingSave(PlayerDataSnapshot snapshot,boolean releaseLock,Consumer<Boolean> callback){}
    private static final class LockOwnershipLostException extends SQLException{private LockOwnershipLostException(String message){super(message);}}
    private record LockAttempt(boolean acquired,String owner,boolean stale){}
    private record LockInfo(String owner,String token,long lockedAt,long heartbeatAt,long expiresAt,String transferState,String targetServerId,long transferStartedAt){}
    private record TransferHandoff(String sourceServer,String targetServer,String state,int savedVersion,long startedAt,long completedAt,long expiresAt){}

    private record PlayerDataSnapshot(UUID uuid, String username, String serverId, String lockToken, String worldName, String inventory,
                                      String armor, String offhand, String enderChest, String effects, String gameMode,
                                      int expLevel, double expProgress, int totalExp, double health, int foodLevel,
                                      float saturation, double customHealth, double customMaxHealth, double mana,
                                      double maxMana, long lastSave, long lastLoad, int saveVersion, double x,
                                      double y, double z, float yaw, float pitch) {
        private PlayerDataSnapshot withSaveVersion(int version){return new PlayerDataSnapshot(uuid,username,serverId,lockToken,worldName,inventory,armor,offhand,enderChest,effects,gameMode,expLevel,expProgress,totalExp,health,foodLevel,saturation,customHealth,customMaxHealth,mana,maxMana,lastSave,lastLoad,version,x,y,z,yaw,pitch);}
        private static PlayerDataSnapshot from(ResultSet result) throws SQLException {
            return new PlayerDataSnapshot(
                    UUID.fromString(result.getString("uuid")),
                    result.getString("username"),
                    result.getString("server_id"),
                    "",
                    result.getString("world_name"),
                    result.getString("inventory"),
                    result.getString("armor"),
                    result.getString("offhand"),
                    result.getString("ender_chest"),
                    result.getString("effects"),
                    result.getString("gamemode"),
                    result.getInt("exp_level"),
                    result.getDouble("exp_progress"),
                    result.getInt("total_exp"),
                    result.getDouble("health"),
                    result.getInt("food_level"),
                    result.getFloat("saturation"),
                    result.getDouble("custom_health"),
                    result.getDouble("custom_max_health"),
                    result.getDouble("mana"),
                    result.getDouble("max_mana"),
                    result.getLong("last_save"),
                    result.getLong("last_load"),
                    result.getInt("save_version"),
                    result.getDouble("x"),
                    result.getDouble("y"),
                    result.getDouble("z"),
                    result.getFloat("yaw"),
                    result.getFloat("pitch")
            );
        }
    }
}
