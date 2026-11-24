package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.ItemStackCodec;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class PlayerInventoryManager {
    private static final String SCOPE = "pit-gameplay-inventory-v1";
    private final ThePitPlugin plugin;
    private final ExecutorService saves = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ThePit-Inventory-Saves");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, Integer> sessions = new HashMap<>();
    private final Map<UUID, BukkitTask> timeouts = new HashMap<>();
    private final Map<UUID, Long> overallDeadlines = new HashMap<>();
    private final Map<UUID, BukkitTask> arenaRetries = new HashMap<>();
    private final Map<UUID, BukkitTask> teleportRetries = new HashMap<>();
    private final Map<UUID, InitializationTeleport> initializationTeleports = new HashMap<>();
    private final Map<UUID, PendingCompletion> pendingCompletions = new HashMap<>();
    private final Map<UUID, String> stages = new HashMap<>();
    private final Map<UUID, JoinDiagnostic> diagnostics = new HashMap<>();
    private final Map<UUID, Long> newestRevision = new ConcurrentHashMap<>();
    private final Map<UUID, Long> persistedRevision = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastFingerprint = new ConcurrentHashMap<>();
    private final Set<UUID> ready = new HashSet<>();
    private final Set<UUID> recoveredThisSession = new HashSet<>();
    private final Set<String> blockedMovementDiagnostics = new HashSet<>();
    private final AtomicLong revisions = new AtomicLong(System.currentTimeMillis());
    private BukkitTask periodicTask;
    private volatile boolean shuttingDown;

    public PlayerInventoryManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long ticks = Math.max(200L, plugin.registry().settings().getLong("inventory-persistence.save-interval-ticks", 1200L));
        periodicTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) captureAndQueue(player, "PERIODIC");
        }, ticks, ticks);
    }

    public void beginJoin(Player player) {
        UUID uuid = player.getUniqueId();
        int session = sessions.getOrDefault(uuid, 0) + 1;
        sessions.put(uuid, session);
        ready.remove(uuid);
        recoveredThisSession.remove(uuid);
        cancelTask(timeouts.remove(uuid));
        cancelTask(arenaRetries.remove(uuid));
        cancelTask(teleportRetries.remove(uuid));
        initializationTeleports.remove(uuid);
        pendingCompletions.remove(uuid);
        diagnostics.put(uuid, new JoinDiagnostic(session));
        long overallTicks = Math.max(200L, plugin.registry().settings().getLong("inventory-persistence.overall-timeout-ticks", 600L));
        overallDeadlines.put(uuid, System.currentTimeMillis() + overallTicks * 50L);
        stage(player, session, "PROFILE_LOADING");
        scheduleStageTimeout(player, session, plugin.registry().settings().getLong("inventory-persistence.load-timeout-ticks", 200L));
    }

    public void loadAfterProfile(Player player, boolean starterEligible, Runnable readyCallback) {
        UUID uuid = player.getUniqueId();
        int session = sessions.getOrDefault(uuid, 0);
        if (session == 0) {
            beginJoin(player);
            session = sessions.getOrDefault(uuid, 0);
        }
        stage(player, session, "PROFILE_READY_INVENTORY_LOADING");
        JoinDiagnostic diagnostic = diagnostic(uuid, session);
        diagnostic.profileLoaded = true;
        debug("Profile load outcome player=" + player.getName() + " uuid=" + uuid + " session=" + session + " success=true existing_profile=" + (!starterEligible));
        scheduleStageTimeout(player, session, plugin.registry().settings().getLong("inventory-persistence.load-timeout-ticks", 200L));
        int activeSession = session;
        saves.execute(() -> {
            StorageManager.StoredValue stored = readStored(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> finishLoad(player, activeSession, starterEligible, stored, readyCallback));
        });
    }

    private StorageManager.StoredValue readStored(UUID uuid) {
        if (plugin.getStorageManager().isPersistentStorageAvailable()) return plugin.getStorageManager().readOverride(SCOPE, uuid.toString());
        if (plugin.getStorageManager().isPersistentStorageConfigured()) return new StorageManager.StoredValue(StorageManager.ReadStatus.FAILED, "",
                "configured persistent storage is unavailable: " + plugin.getStorageManager().lastMigrationFailure());
        try {
            java.util.Optional<String> value = plugin.getStatsManager().readInventoryFallback(uuid);
            return value.isPresent() ? new StorageManager.StoredValue(StorageManager.ReadStatus.FOUND, value.get(), "")
                    : new StorageManager.StoredValue(StorageManager.ReadStatus.MISSING, "", "");
        } catch (Throwable failure) {
            return new StorageManager.StoredValue(StorageManager.ReadStatus.FAILED, "", failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    private void scheduleStageTimeout(Player player, int session, long requestedTicks) {
        UUID uuid = player.getUniqueId();
        cancelTask(timeouts.remove(uuid));
        long stageTicks = Math.max(20L, requestedTicks);
        long overallRemaining = Math.max(1L, (overallDeadlines.getOrDefault(uuid, System.currentTimeMillis() + stageTicks * 50L) - System.currentTimeMillis() + 49L) / 50L);
        long timeoutTicks = Math.min(stageTicks, overallRemaining);
        timeouts.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!active(player, session) || ready.contains(uuid)) return;
            String currentStage = stages.getOrDefault(uuid, "UNKNOWN");
            IllegalStateException failure = new IllegalStateException("initialization exceeded " + timeoutTicks + " ticks at stage " + currentStage);
            if (currentStage.startsWith("WAITING_FOR_INITIALIZATION_TELEPORT") || currentStage.startsWith("WAITING_FOR_ARENA")) failArena(player, session, failure);
            else fail(player, session, "TIMEOUT", failure);
        }, timeoutTicks));
    }

    private void finishLoad(Player player, int session, boolean starterEligible, StorageManager.StoredValue stored, Runnable readyCallback) {
        UUID uuid = player.getUniqueId();
        if (!active(player, session) || ready.contains(uuid)) return;
        JoinDiagnostic diagnostic = diagnostic(uuid, session);
        diagnostic.inventoryResult = stored.status().name();
        stage(player, session, "INVENTORY_RESULT_" + stored.status());
        debug("Inventory load outcome player=" + player.getName() + " uuid=" + uuid + " session=" + session + " status=" + stored.status());
        if (stored.status() == StorageManager.ReadStatus.FAILED) {
            fail(player, session, "INVENTORY_LOAD", new IllegalStateException(stored.failure()));
            return;
        }
        if (!arenaReady()) {
            startArenaTimeout(player, session, diagnostic);
            stage(player, session, "WAITING_FOR_INITIALIZATION_TELEPORT:" + preliminaryArenaReason());
            if (!diagnostic.arenaWaitLogged) {
                diagnostic.arenaWaitLogged = true;
                plugin.getLogger().warning("Join initialization waiting for arena player=" + player.getName() + " uuid=" + uuid
                        + " session=" + session + " " + arenaStatus());
            }
            scheduleArenaRetry(player, session, starterEligible, stored, readyCallback);
            return;
        }
        stage(player, session, "ARENA_READY");
        if (stored.status() == StorageManager.ReadStatus.MISSING) {
            if (!starterEligible) {
                if (!empty(player.getInventory().getContents(), player.getInventory().getArmorContents())) {
                    debug("Kit skipped player=" + player.getName() + " session=" + session + " reason=missing custom snapshot but recoverable Bukkit gameplay inventory is populated counts=" + counts(player.getInventory().getContents(), player.getInventory().getArmorContents()));
                    plugin.getLogger().warning("No custom Pit inventory snapshot exists for existing player " + player.getName() + "; preserving and migrating the sole nonempty Bukkit inventory recovery source.");
                    complete(player, session, "LEGACY_BUKKIT_INVENTORY_RECOVERY", readyCallback);
                    return;
                }
                plugin.getLogger().warning("No custom Pit inventory snapshot or recoverable Bukkit inventory exists for existing player " + player.getName() + "; applying the explicit empty legacy-entry starter rule.");
                debug("Kit eligible player=" + player.getName() + " session=" + session + " reason=existing profile with missing snapshot and confirmed empty Bukkit inventory");
                if (!deliverStarter(player, session)) return;
                complete(player, session, "LEGACY_EMPTY_STARTER_KIT", readyCallback);
                return;
            }
            debug("Inventory missing for new player=" + player.getName() + "; starter kit eligible=true");
            if (!deliverStarter(player, session)) return;
            complete(player, session, "NEW_PLAYER_STARTER_KIT", readyCallback);
            return;
        }
        try {
            InventorySnapshot snapshot = InventorySnapshot.decode(stored.value());
            debug("Saved inventory decoded player=" + player.getName() + " session=" + session + " " + counts(snapshot.contents, snapshot.armor));
            PlayerInventory inventory = player.getInventory();
            inventory.setContents(cloneItems(snapshot.contents));
            inventory.setArmorContents(cloneItems(snapshot.armor));
            inventory.setHeldItemSlot(Math.max(0, Math.min(8, snapshot.heldSlot)));
            player.setItemOnCursor(null);
            player.updateInventory();
            newestRevision.put(uuid, snapshot.revision);
            persistedRevision.put(uuid, snapshot.revision);
            revisions.accumulateAndGet(snapshot.revision, Math::max);
            lastFingerprint.put(uuid, snapshot.fingerprint());
            complete(player, session, "INVENTORY_RESTORED", readyCallback);
            debug("Inventory restored player=" + player.getName() + " revision=" + snapshot.revision + " empty=" + empty(snapshot.contents, snapshot.armor));
            if (empty(snapshot.contents, snapshot.armor)) debug("Kit skipped player=" + player.getName() + " session=" + session + " reason=successfully loaded empty persisted snapshot; legacy format cannot distinguish intentional empty state from interrupted initialization; use /pit recoverkit " + player.getName());
            else debug("Kit skipped player=" + player.getName() + " session=" + session + " reason=successfully restored populated persisted gameplay inventory");
        } catch (Exception failure) {
            fail(player, session, "INVENTORY_DECODE", failure);
        }
    }

    private boolean arenaReady() {
        return plugin.getArenaManager().getCurrentArena() != null && plugin.getArenaManager().getCurrentWorld() != null
                && plugin.getArenaManager().getCurrentSpawn() != null;
    }

    private DestinationReason preliminaryArenaReason() {
        String arena = plugin.getArenaManager().getCurrentArena();
        if (arena == null) return DestinationReason.ARENA_CHANGED;
        org.bukkit.World world = plugin.getArenaManager().getCurrentWorld();
        if (world == null) {
            String expected = plugin.getArenaManager().getArena(arena).map(value -> value.worldName()).orElse(null);
            if (expected != null && plugin.getArenaManager().isWorldLoading(expected)) return DestinationReason.SLIMEWORLD_LOADING;
            return expected == null || Bukkit.getWorld(expected) == null ? DestinationReason.WORLD_NOT_REGISTERED : DestinationReason.WORLD_NULL;
        }
        return plugin.getArenaManager().getCurrentSpawn() == null ? DestinationReason.SPAWN_NULL : DestinationReason.SPAWN_CHUNK_UNLOADED;
    }

    private boolean deliverStarter(Player player, int session) {
        try {
            if (!active(player, session)) {
                debug("Kit skipped player=" + (player == null ? "null" : player.getName()) + " session=" + session + " reason=join session is stale");
                return false;
            }
            JoinDiagnostic diagnostic = diagnostic(player.getUniqueId(), session);
            diagnostic.starterDeliveryRan = true;
            debug("Kit delivery beginning player=" + player.getName() + " session=" + session + " before=" + counts(player.getInventory().getContents(), player.getInventory().getArmorContents()));
            plugin.getDefaultLoadoutManager().give(player);
            diagnostic.equipmentPerks = plugin.getDefaultLoadoutManager().equipmentModifiers(player).toString();
            diagnostic.inventoryAfterDelivery = inventoryDetail(player);
            DefaultLoadoutManager.Verification verification = plugin.getDefaultLoadoutManager().verify(player);
            diagnostic.verification = verification.detail();
            if (!verification.valid()) {
                fail(player, session, "STARTER_KIT_VERIFY", new IllegalStateException("starter equipment verification failed: " + verification.detail()));
                return false;
            }
            debug("Kit delivery completed player=" + player.getName() + " session=" + session + " after=" + counts(player.getInventory().getContents(), player.getInventory().getArmorContents()));
            return true;
        } catch (Throwable failure) {
            JoinDiagnostic diagnostic = diagnostic(player.getUniqueId(), session);
            diagnostic.equipmentPerks = plugin.getDefaultLoadoutManager().equipmentModifiers(player).toString();
            diagnostic.inventoryAfterDelivery = inventoryDetail(player);
            fail(player, session, "STARTER_KIT", failure);
            return false;
        }
    }

    private boolean active(Player player, int session) {
        return player != null && player.isOnline() && sessions.getOrDefault(player.getUniqueId(), 0) == session;
    }

    private void complete(Player player, int session, String decision, Runnable readyCallback) {
        if (!active(player, session) || ready.contains(player.getUniqueId())) return;
        UUID uuid = player.getUniqueId();
        startArenaTimeout(player, session, diagnostic(uuid, session));
        if (pendingCompletions.containsKey(uuid)) return;
        PendingCompletion completion = new PendingCompletion(session, decision, readyCallback, plugin.getArenaManager().getCurrentArena());
        pendingCompletions.put(uuid, completion);
        attemptInitializationTeleport(player, completion);
    }

    private void attemptInitializationTeleport(Player player, PendingCompletion completion) {
        UUID uuid = player.getUniqueId();
        if (!active(player, completion.session) || ready.contains(uuid) || pendingCompletions.get(uuid) != completion) return;
        org.bukkit.Location spawn = plugin.getArenaManager().getCurrentSpawn();
        DestinationReadiness readiness = destinationReadiness(player, completion, spawn);
        if (readiness.reason == DestinationReason.ARENA_CHANGED) {
            diagnostic(uuid, completion.session).spawnChunkLoadRequested = false;
            PendingCompletion replacement = new PendingCompletion(completion.session, completion.decision, completion.readyCallback,
                    plugin.getArenaManager().getCurrentArena());
            pendingCompletions.put(uuid, replacement);
            stage(player, completion.session, "WAITING_FOR_INITIALIZATION_TELEPORT:ARENA_CHANGED");
            scheduleTeleportRetry(player, replacement);
            return;
        }
        if (!readiness.ready) {
            JoinDiagnostic diagnostic = diagnostic(uuid, completion.session);
            if (readiness.reason == DestinationReason.SPAWN_CHUNK_UNLOADED && !diagnostic.spawnChunkLoadRequested) {
                diagnostic.spawnChunkLoadRequested = true;
                plugin.getArenaManager().ensureActiveSpawnChunk();
                readiness = destinationReadiness(player, completion, plugin.getArenaManager().getCurrentSpawn());
                if (readiness.ready) {
                    attemptInitializationTeleport(player, completion);
                    return;
                }
            }
            stage(player, completion.session, "WAITING_FOR_INITIALIZATION_TELEPORT:" + readiness.reason);
            logDestinationWait(player, completion, readiness);
            scheduleTeleportRetry(player, completion);
            return;
        }
        if (!samePosition(player.getLocation(), spawn)) {
            stage(player, completion.session, "WAITING_FOR_INITIALIZATION_TELEPORT:ATTEMPT");
            InitializationTeleport context = new InitializationTeleport(completion.session, spawn.clone());
            initializationTeleports.put(uuid, context);
            boolean teleported;
            try {
                debugTeleport(player, context, "BEGIN", false, "performing exact active-arena spawn teleport");
                teleported = player.teleport(spawn);
                debugTeleport(player, context, "RETURN", !teleported, teleported ? "accepted" : "cancelled by listener or rejected internally");
            } finally {
                initializationTeleports.remove(uuid, context);
            }
            if (!teleported) {
                JoinDiagnostic diagnostic = diagnostic(uuid, completion.session);
                if (!diagnostic.teleportCancellationLogged) {
                    diagnostic.teleportCancellationLogged = true;
                    plugin.getLogger().warning("Join initialization teleport was cancelled or rejected player=" + player.getName()
                            + " uuid=" + uuid + " session=" + completion.session + " from=" + locationDetail(player.getLocation())
                            + " to=" + locationDetail(context.destination) + " from_world_loaded=" + worldLoaded(player.getLocation())
                            + " to_world_loaded=" + worldLoaded(context.destination) + " arena_id=" + plugin.getArenaManager().getCurrentArena()
                            + " cause=PLUGIN online=" + player.isOnline() + " dead=" + player.isDead() + " valid=" + player.isValid()
                            + " stage=" + stages.getOrDefault(uuid, "UNKNOWN")
                            + " thepit_combat_listener=allowed_exact_join_initialization external_or_internal_rejection=true");
                }
                stage(player, completion.session, "WAITING_FOR_INITIALIZATION_TELEPORT:CANCELLED");
                scheduleTeleportRetry(player, completion);
                return;
            }
            if (!samePosition(player.getLocation(), spawn)) {
                DestinationReadiness arrived = destinationReadiness(player, completion, spawn);
                DestinationReason reason = arrived.reason == DestinationReason.READY ? DestinationReason.WORLD_MISMATCH : arrived.reason;
                stage(player, completion.session, "WAITING_FOR_INITIALIZATION_TELEPORT:" + reason);
                logDestinationWait(player, completion, readiness(reason, spawn));
                scheduleTeleportRetry(player, completion);
                return;
            }
        }
        finishCompletion(player, completion);
    }

    private void finishCompletion(Player player, PendingCompletion completion) {
        UUID uuid = player.getUniqueId();
        if (!active(player, completion.session) || pendingCompletions.get(uuid) != completion) return;
        ready.add(uuid);
        try {
            completion.readyCallback.run();
        } catch (Throwable failure) {
            ready.remove(uuid);
            pendingCompletions.remove(uuid, completion);
            fail(player, completion.session, "READY_CALLBACK", failure);
            return;
        }
        if (!active(player, completion.session) || pendingCompletions.get(uuid) != completion) {
            ready.remove(uuid);
            return;
        }
        pendingCompletions.remove(uuid, completion);
        cancelTask(timeouts.remove(uuid));
        cancelTask(arenaRetries.remove(uuid));
        cancelTask(teleportRetries.remove(uuid));
        overallDeadlines.remove(uuid);
        stage(player, completion.session, "COMPLETE:" + completion.decision);
        diagnostics.remove(uuid);
        if (!"INVENTORY_RESTORED".equals(completion.decision)) captureAndQueue(player, completion.decision);
    }

    public boolean isInitializationTeleport(PlayerTeleportEvent event) {
        if (event == null || event.getTo() == null) return false;
        InitializationTeleport context = initializationTeleports.get(event.getPlayer().getUniqueId());
        return context != null && active(event.getPlayer(), context.session) && event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN
                && samePosition(event.getTo(), context.destination);
    }

    public void traceInitializationTeleport(String listener, PlayerTeleportEvent event, String condition) {
        if (event == null) return;
        InitializationTeleport context = initializationTeleports.get(event.getPlayer().getUniqueId());
        if (context == null) return;
        debugTeleport(event.getPlayer(), context, listener, event.isCancelled(), condition + " cause=" + event.getCause());
    }

    public void failProfile(Player player, String reason) {
        if (player == null) return;
        int session = sessions.getOrDefault(player.getUniqueId(), 0);
        if (session != 0) fail(player, session, "PROFILE_LOAD", new IllegalStateException(reason));
    }

    private void fail(Player player, int session, String failedStage, Throwable failure) {
        failAndKick(player, session, failedStage, failure, true);
    }

    private void failArena(Player player, int session, Throwable failure) {
        String current = stages.getOrDefault(player.getUniqueId(), "ARENA_UNKNOWN");
        int separator = current.lastIndexOf(':');
        String reason = separator < 0 ? current : current.substring(separator + 1);
        failAndKick(player, session, "ARENA_DESTINATION_" + reason, failure, false);
    }

    private void failAndKick(Player player, int session, String failedStage, Throwable failure, boolean dataFailure) {
        if (!active(player, session)) return;
        UUID uuid = player.getUniqueId();
        stage(player, session, "FAILED:" + failedStage);
        cancelTask(timeouts.remove(uuid));
        cancelTask(arenaRetries.remove(uuid));
        cancelTask(teleportRetries.remove(uuid));
        initializationTeleports.remove(uuid);
        pendingCompletions.remove(uuid);
        overallDeadlines.remove(uuid);
        ready.remove(uuid);
        JoinDiagnostic diagnostic = diagnostic(uuid, session);
        plugin.getLogger().log(java.util.logging.Level.SEVERE, "Join initialization failed player=" + player.getName()
                + " uuid=" + uuid + " session=" + session + " profile_loaded=" + diagnostic.profileLoaded
                + " inventory_result=" + diagnostic.inventoryResult + " " + arenaStatus()
                + " starter_delivery_ran=" + diagnostic.starterDeliveryRan + " equipment_perks=" + diagnostic.equipmentPerks
                + " inventory_after_delivery=" + diagnostic.inventoryAfterDelivery + " current_inventory=" + inventoryDetail(player)
                + " verification=" + diagnostic.verification
                + " stage=" + failedStage + " plugin_enabled=" + plugin.isEnabled(), failure);
        sessions.put(uuid, session + 1);
        diagnostics.remove(uuid);
        try {
            String message = dataFailure ? "Your The Pit data could not be loaded safely. You have been disconnected to protect your inventory."
                    : "The Pit arena is not ready. Please try again in a moment.";
            player.sendMessage(org.bukkit.ChatColor.RED + message);
            player.kickPlayer(org.bukkit.ChatColor.RED + message);
        } catch (Throwable kickFailure) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not disconnect failed join player=" + player.getName()
                    + " uuid=" + uuid + " session=" + session + " plugin_enabled=" + plugin.isEnabled(), kickFailure);
        }
    }

    private void stage(Player player, int session, String value) {
        stages.put(player.getUniqueId(), value);
        debug("Join initialization player=" + player.getName() + " uuid=" + player.getUniqueId() + " session=" + session
                + " stage=" + value + " arena_ready=" + arenaReady());
    }

    public String blockingReason(UUID uuid) {
        return ready.contains(uuid) ? "READY" : stages.getOrDefault(uuid, "PROFILE_LOADING");
    }

    public void logBlockedMovement(Player player) {
        String reason = blockingReason(player.getUniqueId());
        String key = player.getUniqueId() + ":" + sessions.getOrDefault(player.getUniqueId(), 0) + ":" + reason;
        if (blockedMovementDiagnostics.add(key)) debug("Movement blocked player=" + player.getName() + " uuid=" + player.getUniqueId()
                + " session=" + sessions.getOrDefault(player.getUniqueId(), 0) + " condition=" + reason);
    }

    public boolean isReady(UUID uuid) {
        return ready.contains(uuid);
    }

    public RecoveryResult recoverStarter(Player player) {
        if (player == null || !player.isOnline()) return RecoveryResult.NOT_ONLINE;
        UUID uuid = player.getUniqueId();
        int session = sessions.getOrDefault(uuid, 0);
        if (!ready.contains(uuid) || session == 0) {
            debug("Recovery kit skipped player=" + player.getName() + " session=" + session + " reason=join initialization is not ready stage=" + stages.getOrDefault(uuid, "UNKNOWN"));
            return RecoveryResult.NOT_READY;
        }
        if (!empty(player.getInventory().getContents(), player.getInventory().getArmorContents())) {
            debug("Recovery kit skipped player=" + player.getName() + " session=" + session + " reason=current gameplay inventory is populated counts=" + counts(player.getInventory().getContents(), player.getInventory().getArmorContents()));
            return RecoveryResult.NOT_EMPTY;
        }
        if (!recoveredThisSession.add(uuid)) {
            debug("Recovery kit skipped player=" + player.getName() + " session=" + session + " reason=already recovered in this session");
            return RecoveryResult.ALREADY_RECOVERED;
        }
        if (!deliverStarter(player, session)) {
            recoveredThisSession.remove(uuid);
            return RecoveryResult.FAILED;
        }
        captureAndQueue(player, "ADMIN_EMPTY_INVENTORY_RECOVERY");
        return RecoveryResult.SUCCESS;
    }

    public void onQuit(Player player) {
        if (player == null || shuttingDown) return;
        if (ready.contains(player.getUniqueId())) {
            player.closeInventory();
            captureAndQueue(player, "QUIT");
        }
        ready.remove(player.getUniqueId());
        recoveredThisSession.remove(player.getUniqueId());
        cancelTask(timeouts.remove(player.getUniqueId()));
        cancelTask(arenaRetries.remove(player.getUniqueId()));
        cancelTask(teleportRetries.remove(player.getUniqueId()));
        initializationTeleports.remove(player.getUniqueId());
        pendingCompletions.remove(player.getUniqueId());
        overallDeadlines.remove(player.getUniqueId());
        diagnostics.remove(player.getUniqueId());
        stages.remove(player.getUniqueId());
        blockedMovementDiagnostics.removeIf(value -> value.startsWith(player.getUniqueId() + ":"));
        sessions.put(player.getUniqueId(), sessions.getOrDefault(player.getUniqueId(), 0) + 1);
    }

    public void captureDeathState(Player player) {
        if (player != null && ready.contains(player.getUniqueId())) captureAndQueue(player, "POST_DEATH");
    }

    public void captureAndQueue(Player player, String reason) {
        if (player == null || shuttingDown || !Bukkit.isPrimaryThread() || !ready.contains(player.getUniqueId())) return;
        PlayerInventory inventory = player.getInventory();
        long revision = revisions.incrementAndGet();
        InventorySnapshot snapshot = new InventorySnapshot(revision, inventory.getHeldItemSlot(), cloneItems(inventory.getContents()), cloneItems(inventory.getArmorContents()));
        UUID uuid = player.getUniqueId();
        int fingerprint = snapshot.fingerprint();
        if ("PERIODIC".equals(reason) && lastFingerprint.getOrDefault(uuid, Integer.MIN_VALUE) == fingerprint) return;
        lastFingerprint.put(uuid, fingerprint);
        newestRevision.put(uuid, revision);
        queueSave(uuid, player.getName(), snapshot, reason);
    }

    private void queueSave(UUID uuid, String name, InventorySnapshot snapshot, String reason) {
        saves.execute(() -> {
            synchronized (persistedRevision) {
                long newest = newestRevision.getOrDefault(uuid, snapshot.revision);
                long persisted = persistedRevision.getOrDefault(uuid, 0L);
                if (snapshot.revision < newest || snapshot.revision <= persisted) return;
                boolean saved = plugin.getStorageManager().isPersistentStorageAvailable()
                        ? plugin.getStorageManager().putOverride(SCOPE, uuid.toString(), snapshot.encode())
                        : !plugin.getStorageManager().isPersistentStorageConfigured() && plugin.getStatsManager().writeInventoryFallback(uuid, snapshot.encode());
                if (saved) {
                    persistedRevision.put(uuid, snapshot.revision);
                    debug("Inventory saved player=" + name + " revision=" + snapshot.revision + " reason=" + reason);
                }
                else {
                    lastFingerprint.remove(uuid, snapshot.fingerprint());
                    plugin.getLogger().severe("Inventory save failed player=" + name + " uuid=" + uuid + " revision=" + snapshot.revision + " reason=" + reason);
                }
            }
        });
    }

    public void shutdown() {
        if (periodicTask != null) periodicTask.cancel();
        periodicTask = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!ready.contains(player.getUniqueId())) continue;
            player.closeInventory();
            captureAndQueue(player, "SHUTDOWN");
        }
        shuttingDown = true;
        saves.shutdown();
        try {
            if (!saves.awaitTermination(5L, TimeUnit.SECONDS)) plugin.getLogger().severe("Inventory shutdown save window expired with pending writes.");
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            plugin.getLogger().severe("Inventory shutdown save wait was interrupted.");
        }
        ready.clear();
        recoveredThisSession.clear();
        sessions.clear();
        for (BukkitTask timeout : timeouts.values()) timeout.cancel();
        timeouts.clear();
        for (BukkitTask retry : arenaRetries.values()) retry.cancel();
        arenaRetries.clear();
        for (BukkitTask retry : teleportRetries.values()) retry.cancel();
        teleportRetries.clear();
        initializationTeleports.clear();
        pendingCompletions.clear();
        overallDeadlines.clear();
        stages.clear();
        diagnostics.clear();
        blockedMovementDiagnostics.clear();
        lastFingerprint.clear();
    }

    private static ItemStack[] cloneItems(ItemStack[] items) {
        if (items == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) copy[index] = items[index] == null ? null : items[index].clone();
        return copy;
    }

    private boolean empty(ItemStack[] contents, ItemStack[] armor) {
        for (ItemStack item : contents) if (item != null && item.getType() != org.bukkit.Material.AIR) return false;
        for (ItemStack item : armor) if (item != null && item.getType() != org.bukkit.Material.AIR) return false;
        return true;
    }

    private String counts(ItemStack[] contents, ItemStack[] armor) {
        int gameplayItems = 0;
        int armorItems = 0;
        if (contents != null) for (ItemStack item : contents) if (item != null && item.getType() != org.bukkit.Material.AIR) gameplayItems++;
        if (armor != null) for (ItemStack item : armor) if (item != null && item.getType() != org.bukkit.Material.AIR) armorItems++;
        return "gameplay_slots=" + gameplayItems + " armor_slots=" + armorItems;
    }

    private void scheduleArenaRetry(Player player, int session, boolean starterEligible, StorageManager.StoredValue stored, Runnable readyCallback) {
        UUID uuid = player.getUniqueId();
        if (arenaRetries.containsKey(uuid)) return;
        arenaRetries.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            arenaRetries.remove(uuid);
            finishLoad(player, session, starterEligible, stored, readyCallback);
        }, 5L));
    }

    private void scheduleTeleportRetry(Player player, PendingCompletion completion) {
        UUID uuid = player.getUniqueId();
        if (teleportRetries.containsKey(uuid)) return;
        teleportRetries.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            teleportRetries.remove(uuid);
            attemptInitializationTeleport(player, completion);
        }, 5L));
    }

    private void startArenaTimeout(Player player, int session, JoinDiagnostic diagnostic) {
        if (diagnostic.arenaTimeoutStarted) return;
        diagnostic.arenaTimeoutStarted = true;
        scheduleStageTimeout(player, session, plugin.registry().settings().getLong("inventory-persistence.arena-timeout-ticks", 200L));
    }

    private DestinationReadiness destinationReadiness(Player player, PendingCompletion completion, org.bukkit.Location destination) {
        if (player == null || !player.isOnline() || player.isDead() || !player.isValid()) return readiness(DestinationReason.PLAYER_INVALID, destination);
        String arena = plugin.getArenaManager().getCurrentArena();
        if (completion.arenaId == null || arena == null || !completion.arenaId.equals(arena)) return readiness(DestinationReason.ARENA_CHANGED, destination);
        org.bukkit.World activeWorld = plugin.getArenaManager().getCurrentWorld();
        if (activeWorld == null) {
            String expectedWorld = plugin.getArenaManager().getArena(arena).map(value -> value.worldName()).orElse(null);
            if (expectedWorld != null && plugin.getArenaManager().isWorldLoading(expectedWorld)) return readiness(DestinationReason.SLIMEWORLD_LOADING, destination);
            if (expectedWorld != null && Bukkit.getWorld(expectedWorld) == null) return readiness(DestinationReason.WORLD_NOT_REGISTERED, destination);
            return readiness(DestinationReason.WORLD_NULL, destination);
        }
        if (destination == null) return readiness(DestinationReason.SPAWN_NULL, null);
        if (!Double.isFinite(destination.getX()) || !Double.isFinite(destination.getY()) || !Double.isFinite(destination.getZ()))
            return readiness(DestinationReason.INVALID_COORDINATES, destination);
        if (destination.getWorld() == null) return readiness(DestinationReason.WORLD_NULL, destination);
        org.bukkit.World registered = Bukkit.getWorld(destination.getWorld().getName());
        if (registered == null) return readiness(DestinationReason.WORLD_UNLOADED, destination);
        if (registered != destination.getWorld() || activeWorld != destination.getWorld()) return readiness(DestinationReason.WORLD_MISMATCH, destination);
        int chunkX = destination.getBlockX() >> 4;
        int chunkZ = destination.getBlockZ() >> 4;
        if (!activeWorld.isChunkLoaded(chunkX, chunkZ)) return readiness(DestinationReason.SPAWN_CHUNK_UNLOADED, destination);
        if (destination.getBlockY() < 0 || destination.getBlockY() + 1 >= activeWorld.getMaxHeight()
                || destination.getBlock().getType().isSolid() || destination.clone().add(0.0D, 1.0D, 0.0D).getBlock().getType().isSolid())
            return readiness(DestinationReason.SPAWN_BLOCK_UNSAFE, destination);
        return readiness(DestinationReason.READY, destination);
    }

    private DestinationReadiness readiness(DestinationReason reason, org.bukkit.Location destination) {
        int chunkX = destination == null ? 0 : destination.getBlockX() >> 4;
        int chunkZ = destination == null ? 0 : destination.getBlockZ() >> 4;
        boolean loaded = destination != null && destination.getWorld() != null && destination.getWorld().isChunkLoaded(chunkX, chunkZ);
        return new DestinationReadiness(reason == DestinationReason.READY, reason, chunkX, chunkZ, loaded);
    }

    private void logDestinationWait(Player player, PendingCompletion completion, DestinationReadiness readiness) {
        JoinDiagnostic diagnostic = diagnostic(player.getUniqueId(), completion.session);
        if (readiness.reason.name().equals(diagnostic.lastDestinationReason)) return;
        diagnostic.lastDestinationReason = readiness.reason.name();
        plugin.getLogger().warning("Join initialization destination waiting player=" + player.getName() + " uuid=" + player.getUniqueId()
                + " session=" + completion.session + " reason=" + readiness.reason + " arena_id=" + plugin.getArenaManager().getCurrentArena()
                + " world=" + (plugin.getArenaManager().getCurrentWorld() == null ? "null" : plugin.getArenaManager().getCurrentWorld().getName())
                + " spawn=" + locationDetail(plugin.getArenaManager().getCurrentSpawn()) + " spawn_chunk=" + readiness.chunkX + "," + readiness.chunkZ
                + " chunk_loaded=" + readiness.chunkLoaded + " stage=" + stages.getOrDefault(player.getUniqueId(), "UNKNOWN"));
    }

    private boolean samePosition(org.bukkit.Location first, org.bukkit.Location second) {
        if (first == null || second == null || first.getWorld() == null || first.getWorld() != second.getWorld()) return false;
        return first.distanceSquared(second) <= 0.01D;
    }

    private void debugTeleport(Player player, InitializationTeleport context, String checkpoint, boolean cancelled, String condition) {
        if (!plugin.registry().settings().getBoolean("inventory-persistence.debug", false)) return;
        org.bukkit.Location from = player.getLocation();
        org.bukkit.Location to = context.destination;
        plugin.getLogger().info("Join initialization teleport player=" + player.getName() + " uuid=" + player.getUniqueId()
                + " session=" + context.session + " checkpoint=" + checkpoint + " from=" + locationDetail(from)
                + " to=" + locationDetail(to) + " from_world_loaded=" + worldLoaded(from) + " to_world_loaded=" + worldLoaded(to)
                + " arena_id=" + plugin.getArenaManager().getCurrentArena() + " cause=PLUGIN cancelled=" + cancelled
                + " online=" + player.isOnline() + " dead=" + player.isDead() + " valid=" + player.isValid()
                + " stage=" + stages.getOrDefault(player.getUniqueId(), "UNKNOWN") + " condition=" + condition);
    }

    private String locationDetail(org.bukkit.Location location) {
        if (location == null) return "missing";
        return (location.getWorld() == null ? "missing" : location.getWorld().getName()) + ":"
                + location.getX() + "," + location.getY() + "," + location.getZ();
    }

    private boolean worldLoaded(org.bukkit.Location location) {
        return location != null && location.getWorld() != null && Bukkit.getWorld(location.getWorld().getName()) == location.getWorld();
    }

    private String arenaStatus() {
        String arena = plugin.getArenaManager().getCurrentArena();
        org.bukkit.World world = plugin.getArenaManager().getCurrentWorld();
        org.bukkit.Location spawn = plugin.getArenaManager().getCurrentSpawn();
        String spawnWorld = spawn == null || spawn.getWorld() == null ? "missing" : spawn.getWorld().getName();
        return "arena_id=" + (arena == null ? "missing" : arena) + " world=" + (world == null ? "missing" : world.getName())
                + " spawn=" + (spawn == null ? "missing" : spawnWorld + ":" + spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ());
    }

    private String inventoryDetail(Player player) {
        StringBuilder result = new StringBuilder("contents=[");
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
            if (result.length() > 10) result.append(',');
            result.append(slot).append(':').append(item.getType()).append('x').append(item.getAmount());
        }
        result.append("] armor=[");
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int slot = 0; slot < armor.length; slot++) {
            ItemStack item = armor[slot];
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
            if (result.charAt(result.length() - 1) != '[') result.append(',');
            result.append(slot).append(':').append(item.getType()).append('x').append(item.getAmount());
        }
        return result.append(']').toString();
    }

    private JoinDiagnostic diagnostic(UUID uuid, int session) {
        JoinDiagnostic current = diagnostics.get(uuid);
        if (current == null || current.session != session) {
            current = new JoinDiagnostic(session);
            diagnostics.put(uuid, current);
        }
        return current;
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) task.cancel();
    }

    public enum RecoveryResult { SUCCESS, NOT_ONLINE, NOT_READY, NOT_EMPTY, ALREADY_RECOVERED, FAILED }

    private static final class JoinDiagnostic {
        private final int session;
        private boolean profileLoaded;
        private String inventoryResult = "NOT_LOADED";
        private boolean starterDeliveryRan;
        private boolean arenaWaitLogged;
        private boolean teleportCancellationLogged;
        private boolean arenaTimeoutStarted;
        private boolean spawnChunkLoadRequested;
        private String lastDestinationReason = "";
        private String equipmentPerks = "[]";
        private String inventoryAfterDelivery = "not_run";
        private String verification = "not_run";

        private JoinDiagnostic(int session) {
            this.session = session;
        }
    }

    private static final class PendingCompletion {
        private final int session;
        private final String decision;
        private final Runnable readyCallback;
        private final String arenaId;

        private PendingCompletion(int session, String decision, Runnable readyCallback, String arenaId) {
            this.session = session;
            this.decision = decision;
            this.readyCallback = readyCallback;
            this.arenaId = arenaId;
        }
    }

    private enum DestinationReason { READY, WORLD_NULL, WORLD_NOT_REGISTERED, WORLD_UNLOADED, WORLD_MISMATCH, SPAWN_NULL, INVALID_COORDINATES, SPAWN_CHUNK_UNLOADED, SPAWN_BLOCK_UNSAFE, SLIMEWORLD_LOADING, ARENA_CHANGED, PLAYER_INVALID }

    private static final class DestinationReadiness {
        private final boolean ready;
        private final DestinationReason reason;
        private final int chunkX;
        private final int chunkZ;
        private final boolean chunkLoaded;

        private DestinationReadiness(boolean ready, DestinationReason reason, int chunkX, int chunkZ, boolean chunkLoaded) {
            this.ready = ready;
            this.reason = reason;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.chunkLoaded = chunkLoaded;
        }
    }

    private static final class InitializationTeleport {
        private final int session;
        private final org.bukkit.Location destination;

        private InitializationTeleport(int session, org.bukkit.Location destination) {
            this.session = session;
            this.destination = destination;
        }
    }

    private void debug(String message) {
        if (plugin.registry().settings().getBoolean("inventory-persistence.debug", false)) plugin.getLogger().info(message);
    }

    private static final class InventorySnapshot {
        private final long revision;
        private final int heldSlot;
        private final ItemStack[] contents;
        private final ItemStack[] armor;

        private InventorySnapshot(long revision, int heldSlot, ItemStack[] contents, ItemStack[] armor) {
            this.revision = revision;
            this.heldSlot = heldSlot;
            this.contents = contents;
            this.armor = armor;
        }

        private String encode() {
            return "1\n" + revision + "\n" + heldSlot + "\n" + ItemStackCodec.encode(contents) + "\n" + ItemStackCodec.encode(armor);
        }

        private int fingerprint() {
            int result = 31 * heldSlot + java.util.Arrays.deepHashCode(contents);
            return 31 * result + java.util.Arrays.deepHashCode(armor);
        }

        private static InventorySnapshot decode(String value) throws Exception {
            String[] parts = value == null ? new String[0] : value.split("\\n", 5);
            if (parts.length != 5 || !parts[0].equals("1")) throw new IllegalArgumentException("unsupported inventory snapshot format");
            long revision = Long.parseLong(parts[1]);
            int heldSlot = Integer.parseInt(parts[2]);
            ItemStack[] contents = ItemStackCodec.decodeStrict(parts[3]);
            ItemStack[] armor = ItemStackCodec.decodeStrict(parts[4]);
            if (contents.length != 36 || armor.length != 4) throw new IllegalArgumentException("invalid inventory dimensions " + contents.length + "/" + armor.length);
            return new InventorySnapshot(revision, heldSlot, contents, armor);
        }
    }
}
