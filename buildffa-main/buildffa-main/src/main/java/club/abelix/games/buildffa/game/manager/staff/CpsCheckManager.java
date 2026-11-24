package club.abelix.games.buildffa.game.manager.staff;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.event.GameDeathEvent;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static club.abelix.api.base.util.chat.MessageSender.sendActionBar;

public final class CpsCheckManager extends YListener {
    private static final long CPS_WINDOW_NANOS = 1_000_000_000L;
    private static final long ACTION_PAIR_NANOS = 50_000_000L;
    private static final long MAX_TICK_NANOS = 100_000_000L;
    private static final long BURST_NANOS = 5_000_000L;
    private static final int PENALTY_TICKS = 5 * 20;
    private static final int PENALTY_AMPLIFIER = 14;
    private final BuildFFAGame game;
    private final CombatIntegrityManager combat;
    private final Map<UUID, MonitorSession> sessions = new HashMap<>();
    private final Map<UUID, ClickHistory> histories = new HashMap<>();
    private final Map<UUID, PenaltyEffect> penalties = new HashMap<>();
    private long lastTickAt;
    private final ConcurrentMap<Player, ClickHistory> packetHistories = new ConcurrentHashMap<>();
    private final Map<UUID, MonitorSession> reachSessions = new HashMap<>();
    private final Map<UUID, ReachHistory> reachHistories = new HashMap<>();
    private final Map<EntityDamageByEntityEvent, HitSample> pendingHits = new WeakHashMap<>();
    private final Set<EntityDamageByEntityEvent> measuredHits = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<EntityDamageByEntityEvent> rejectedHits = Collections.newSetFromMap(new WeakHashMap<>());
    private boolean loggedReachFailure;
    private PacketListener packetListener;
    private BukkitTask displayTask;
    private volatile boolean enabled;
    private volatile long generation;

    public CpsCheckManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
        this.combat = new CombatIntegrityManager(game);
    }

    public void enable() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("CPS monitoring must be enabled on the server thread");
        if (enabled) return;
        enabled = true;
        combat.enable();
        long listenerGeneration = ++generation;
        Plugin plugin = game.getPlugin();
        packetListener = new PacketAdapter(plugin, ListenerPriority.MONITOR,
                PacketType.Play.Server.ENTITY_VELOCITY, PacketType.Play.Client.ARM_ANIMATION, PacketType.Play.Client.BLOCK_PLACE,
                PacketType.Play.Client.BLOCK_DIG, PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.FLYING, PacketType.Play.Client.POSITION,
                PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK) {
            @Override
            public void onPacketSending(PacketEvent event) { combat.velocityPacket(event); }
            @Override
            public void onPacketReceiving(PacketEvent event) {
                combat.movementPacket(event);
                if (!enabled || listenerGeneration != generation || event.isCancelled()) return;
                Player player = event.getPlayer();
                if (player == null) return;
                ClickHistory history = packetHistories.get(player);
                if (history == null) return;
                long receivedAt = System.nanoTime();
                PacketType type = event.getPacketType();
                if (type != PacketType.Play.Client.BLOCK_PLACE) history.observePacket(type, receivedAt);
                if (type == PacketType.Play.Client.ARM_ANIMATION) history.swing(receivedAt);
                else if (type == PacketType.Play.Client.BLOCK_PLACE)
                    history.use(receivedAt, event.getPacket().getIntegers().read(0) != 255);
                else if (type == PacketType.Play.Client.USE_ENTITY) {
                    if (event.getPacket().getEntityUseActions().read(0) == EnumWrappers.EntityUseAction.ATTACK)
                        history.attack(receivedAt);
                } else if (type == PacketType.Play.Client.BLOCK_DIG) {
                    EnumWrappers.PlayerDigType digging = event.getPacket().getPlayerDigTypes().read(0);
                    if (digging == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) history.startMining(receivedAt);
                    else if (digging == EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK) history.stopMining();
                    else if (digging == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) history.finishMining(receivedAt);
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);
        displayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateDisplays, 1L, 1L);
    }

    public void toggle(Player staff, String targetName) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> toggle(staff, targetName));
            return;
        }
        if (!enabled || !staff.isOnline()) return;
        if (!hasPermission(staff)) {
            send(staff, "buildffa_cpscheck_no_permission", "&cYou do not have permission to check CPS.");
            return;
        }
        Player target = findOnlinePlayer(targetName);
        if (target == null) {
            send(staff, "buildffa_cpscheck_player_not_found", "&cThat player is not online.");
            return;
        }
        MonitorSession current = sessions.get(staff.getUniqueId());
        if (current != null && current.targetId.equals(target.getUniqueId())) {
            stop(staff, true);
            return;
        }
        if (current != null) removeSession(staff.getUniqueId());
        sessions.put(staff.getUniqueId(), new MonitorSession(target.getUniqueId()));
        ClickHistory history = histories.computeIfAbsent(target.getUniqueId(), ignored -> new ClickHistory());
        sampleBlockTarget(target, history);
        packetHistories.put(target, history);
        long[] counts = history.counts(System.nanoTime());
        send(staff, "buildffa_cpscheck_started", "&aStarted monitoring &f%player%&a.", displayName(target),
                counts[0], counts[1]);
        send(staff, "buildffa_cpscheck_measurement_limits",
                "&7Left measures non-mining swings/attacks, not verified physical clicks. Block-directed swings are excluded. Right measures use packets; held use can repeat.");
        sendStaffDisplay(staff, System.nanoTime());
    }

    public void stop(Player staff, boolean notify) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> stop(staff, notify));
            return;
        }
        MonitorSession removed = removeSession(staff.getUniqueId());
        if (removed != null && staff.isOnline()) sendStaffDisplay(staff, System.nanoTime());
        if (notify) send(staff, "buildffa_cpscheck_stopped",
                removed == null ? "&cYou do not have an active CPS check." : "&eStopped the CPS check.");
    }

    public boolean isMonitoring(Player staff) {
        return sessions.containsKey(staff.getUniqueId()) || reachSessions.containsKey(staff.getUniqueId());
    }

    public boolean hasPermission(Player player) {
        return game.hasPermission(player, BuildFFAGame.CPS_CHECK_PERMISSION);
    }

    public void sendUsage(Player player) {
        send(player, "buildffa_cpscheck_usage", "&cUsage: /cpscheck <player|stop>");
    }

    public Player findOnlinePlayer(String name) {
        if (name == null) return null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    private void updateDisplays() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Staff displays must be updated on the server thread");
        if (!enabled) return;
        long now = System.nanoTime();
        boolean reliableTick = lastTickAt != 0 && now - lastTickAt > 0 && now - lastTickAt <= MAX_TICK_NANOS;
        lastTickAt = now;
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            updatePenaltyEffect(player);
            boolean eligible = isPenaltyEligible(player);
            ClickHistory history = histories.get(id);
            if (eligible || history != null) {
                if (history == null) {
                    history = new ClickHistory();
                    histories.put(id, history);
                    sampleBlockTarget(player, history);
                    packetHistories.put(player, history);
                }
                int result = history.penaltyWindow(now, eligible, reliableTick);
                if (result > 0) applyPenalty(player, result == 2);
                if (!eligible) removeUnusedHistory(id);
            }
        }
        for (Map.Entry<Player, ClickHistory> entry : packetHistories.entrySet()) {
            if (entry.getKey().isOnline()) sampleBlockTarget(entry.getKey(), entry.getValue());
        }
        updateDisplays(null, now);
    }

    private boolean isPenaltyEligible(Player player) {
        return !game.isFrozen(player) && game.getEventManager() != null && game.getEventManager().isArenaParticipant(player)
                && !game.getRespawningPlayers().contains(player.getUniqueId())
                && !game.hasPermission(player, "buildffa.cpslimit.bypass");
    }

    private PotionEffect fatigue(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects())
            if (effect.getType().equals(PotionEffectType.SLOW_DIGGING)) return effect;
        return null;
    }

    private void applyPenalty(Player player, boolean consecutive) {
        PotionEffect current = fatigue(player);
        if (current != null && current.getAmplifier() > PENALTY_AMPLIFIER) return;
        PenaltyEffect owned = penalties.get(player.getUniqueId());
        long ticks = player.getTicksLived();
        boolean ours = owned != null && owned.owns(current, ticks);
        long remaining = current == null ? 0 : current.getDuration();
        int duration = (int) Math.min(Integer.MAX_VALUE,
                consecutive ? remaining + PENALTY_TICKS : Math.max(remaining, PENALTY_TICKS));
        if (current != null && current.getAmplifier() == PENALTY_AMPLIFIER && current.getDuration() >= duration) return;
        PotionEffect previous = ours ? owned.previous : current;
        long previousUntil = ours ? owned.previousUntil : ticks + remaining;
        PotionEffect effect = new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, PENALTY_AMPLIFIER);
        if (player.addPotionEffect(effect, true))
            penalties.put(player.getUniqueId(), new PenaltyEffect(previous, previousUntil, ticks + duration));
    }

    private void updatePenaltyEffect(Player player) {
        PenaltyEffect owned = penalties.get(player.getUniqueId());
        if (owned != null && !owned.owns(fatigue(player), player.getTicksLived()))
            penalties.remove(player.getUniqueId());
    }

    public void clearCps(Player player) {
        histories.remove(player.getUniqueId());
        packetHistories.remove(player);
        PenaltyEffect owned = penalties.remove(player.getUniqueId());
        long ticks = player.getTicksLived();
        if (owned != null && owned.owns(fatigue(player), ticks)) {
            player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
            if (owned.previous != null && owned.previousUntil > ticks)
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,
                        (int) (owned.previousUntil - ticks), owned.previous.getAmplifier(),
                        owned.previous.isAmbient(), owned.previous.hasParticles()), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameDeath(GameDeathEvent event) {
        clearCps(event.getVictim());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVanillaDeath(PlayerDeathEvent event) {
        clearCps(event.getEntity());
    }

    private void sampleBlockTarget(Player player, ClickHistory history) {
        history.blockTarget(player.getTargetBlock((Set<Material>) null, 5).getType() != Material.AIR);
    }

    private void updateDisplays(UUID changedTarget, long now) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Staff displays must be updated on the server thread");
        if (!enabled) return;
        Set<UUID> viewers = new HashSet<>(sessions.keySet());
        viewers.addAll(reachSessions.keySet());
        for (UUID staffId : viewers) {
            MonitorSession cps = sessions.get(staffId);
            MonitorSession reach = reachSessions.get(staffId);
            if (changedTarget != null && (cps == null || !changedTarget.equals(cps.targetId))
                    && (reach == null || !changedTarget.equals(reach.targetId))) continue;
            Player staff = Bukkit.getPlayer(staffId);
            if (staff == null || !staff.isOnline()) {
                removeSession(staffId);
                removeReachSession(staffId);
                continue;
            }
            sendStaffDisplay(staff, now);
        }
    }

    private void sendStaffDisplay(Player staff, long now) {
        String cpsText = "";
        String reachText = "";
        MonitorSession cps = sessions.get(staff.getUniqueId());
        if (cps != null) {
            Player target = Bukkit.getPlayer(cps.targetId);
            if (!hasPermission(staff) || target == null || !target.isOnline()) removeSession(staff.getUniqueId());
            else {
                ClickHistory history = histories.get(cps.targetId);
                long[] counts = history == null ? new long[]{0, 0} : history.counts(now);
                cpsText = format(staff, "buildffa_cpscheck_display",
                        "&bCPS Check: &f%player% &8| &7Left: &e%left_cps% &8| &7Right: &e%right_cps%",
                        displayName(target), counts[0], counts[1]);
                cpsText += format(staff, "buildffa_cpscheck_activity_label",
                        " &8[Left: non-mining actions/s; Right: use packets/s]", "", 0, 0);
            }
        }
        MonitorSession reach = reachSessions.get(staff.getUniqueId());
        if (reach != null) {
            Player target = Bukkit.getPlayer(reach.targetId);
            if (!hasReachPermission(staff)) {
                removeReachSession(staff.getUniqueId());
                sendReach(staff, "buildffa_reachcheck_no_permission", "&cYou do not have permission to check reach.", "", null);
            } else if (target == null || !target.isOnline()) {
                removeReachSession(staff.getUniqueId());
                sendReach(staff, "buildffa_reachcheck_target_disconnected", "&eReach check ended: &f%player% &eis offline.", reach.targetName, null);
            } else {
                ReachHistory history = reachHistories.get(reach.targetId);
                reachText = history.latestDistance == null
                        ? reachFormat(staff, "buildffa_reachcheck_waiting", "&bServer hit distance: &f%player% &8| &7Waiting for a melee hit", displayName(target), null)
                        : reachFormat(staff, "buildffa_reachcheck_display", "&bServer hit distance: &f%player% &8| &e%distance% blocks", displayName(target), history.latestDistance);
                reachText += " \u00a78| \u00a77Rejected >5: \u00a7c" + history.violations;
                if (history.lastRejectedDistance != null)
                    reachText += String.format(Locale.ROOT, " \u00a78(%.2f blocks)", history.lastRejectedDistance);
            }
        }
        sendActionBar(staff, cpsText.isEmpty() ? reachText : reachText.isEmpty() ? cpsText : cpsText + " \u00a78| " + reachText);
    }

    private MonitorSession removeSession(UUID staffId) {
        MonitorSession removed = sessions.remove(staffId);
        if (removed != null) removeUnusedHistory(removed.targetId);
        return removed;
    }

    private void removeUnusedHistory(UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (enabled && target != null && isPenaltyEligible(target)) return;
        for (MonitorSession session : sessions.values()) if (session.targetId.equals(targetId)) return;
        ClickHistory removed = histories.remove(targetId);
        if (removed != null) packetHistories.values().removeIf(history -> history == removed);
    }

    private void send(Player staff, String key, String fallback) {
        send(staff, key, fallback, "", 0, 0);
    }

    private void send(Player staff, String key, String fallback, String playerName, long leftCps, long rightCps) {
        staff.sendMessage(format(staff, key, fallback, playerName, leftCps, rightCps));
    }

    private String format(Player staff, String key, String fallback, String playerName, long leftCps, long rightCps) {
        Gamer gamer = Gamer.getGamer(staff);
        String value = gamer == null ? fallback : gamer.getLanguage().getMessage(key);
        if (value == null || value.equals(key) || value.startsWith("No translation (")) value = fallback;
        return ChatColor.translateAlternateColorCodes('&', value
                .replace("%player%", playerName)
                .replace("%cps_player%", playerName)
                .replace("%left_cps%", String.valueOf(leftCps))
                .replace("%right_cps%", String.valueOf(rightCps)));
    }

    private String displayName(Player player) {
        Gamer gamer = Gamer.getGamer(player);
        String displayName = gamer == null ? null : gamer.getDisplayName();
        return displayName == null ? player.getName() : displayName;
    }

    private void disable() {
        if (!enabled) return;
        enabled = false;
        combat.disable();
        generation++;
        if (displayTask != null) displayTask.cancel();
        displayTask = null;
        Set<UUID> viewers = new HashSet<>(sessions.keySet());
        viewers.addAll(reachSessions.keySet());
        for (UUID staffId : viewers) {
            Player staff = Bukkit.getPlayer(staffId);
            if (staff != null && staff.isOnline()) sendActionBar(staff, "");
        }
        sessions.clear();
        for (Player player : Bukkit.getOnlinePlayers()) clearCps(player);
        penalties.clear();
        lastTickAt = 0;
        histories.clear();
        packetHistories.clear();
        reachSessions.clear();
        reachHistories.clear();
        pendingHits.clear();
        measuredHits.clear();
        rejectedHits.clear();
        if (packetListener != null) ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);
        packetListener = null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearCps(event.getPlayer());
        UUID playerId = event.getPlayer().getUniqueId();
        Set<UUID> viewers = new HashSet<>(sessions.keySet());
        viewers.addAll(reachSessions.keySet());
        removeSession(playerId);
        removeReachSession(playerId);
        for (UUID viewer : viewers) {
            MonitorSession cps = sessions.get(viewer);
            MonitorSession reach = reachSessions.get(viewer);
            boolean changed = false;
            if (cps != null && cps.targetId.equals(playerId)) {
                removeSession(viewer);
                changed = true;
            }
            if (reach != null && reach.targetId.equals(playerId)) {
                removeReachSession(viewer);
                changed = true;
                Player staff = Bukkit.getPlayer(viewer);
                if (staff != null && staff.isOnline() && hasReachPermission(staff))
                    sendReach(staff, "buildffa_reachcheck_target_disconnected", "&eReach check ended: &f%player% &eis offline.", reach.targetName, null);
            }
            Player staff = Bukkit.getPlayer(viewer);
            if (changed && staff != null && staff.isOnline() && !viewer.equals(playerId)) sendStaffDisplay(staff, System.nanoTime());
        }
        histories.remove(playerId);
        packetHistories.remove(event.getPlayer());
        reachHistories.remove(playerId);
        pendingHits.values().removeIf(sample -> sample.attackerId.equals(playerId) || sample.victimId.equals(playerId));
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == game.getPlugin()) disable();
    }

    public CombatIntegrityManager getCombatIntegrity() { return combat; }

    public boolean hasReachPermission(Player staff) {
        return game.hasPermission(staff, BuildFFAGame.REACH_CHECK_PERMISSION);
    }

    public void executeReach(Player staff, String[] arguments) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> executeReach(staff, arguments));
            return;
        }
        if (!staff.isOnline()) return;
        if (!hasReachPermission(staff)) {
            stopReach(staff, false);
            sendReachNoPermission(staff);
            return;
        }
        if (arguments.length != 1) sendReachUsage(staff);
        else if (arguments[0].equalsIgnoreCase("stop")) stopReach(staff, true);
        else startReach(staff, arguments[0]);
    }

    public void sendReachNoPermission(Player staff) {
        sendReach(staff, "buildffa_reachcheck_no_permission", "&cYou do not have permission to check reach.", "", null);
    }

    public void sendReachUsage(Player staff) {
        sendReach(staff, "buildffa_reachcheck_usage", "&cUsage: /reachcheck <player|stop>", "", null);
    }

    public void startReach(Player staff, String targetName) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> startReach(staff, targetName));
            return;
        }
        if (!enabled || !staff.isOnline()) return;
        if (!hasReachPermission(staff)) {
            sendReach(staff, "buildffa_reachcheck_no_permission", "&cYou do not have permission to check reach.", "", null);
            return;
        }
        Player target = findOnlinePlayer(targetName);
        if (target == null) {
            sendReach(staff, "buildffa_reachcheck_player_not_found", "&c%player% is not online.", targetName, null);
            return;
        }
        MonitorSession existing = reachSessions.get(staff.getUniqueId());
        if (existing == null || !existing.targetId.equals(target.getUniqueId())) {
            removeReachSession(staff.getUniqueId());
            reachSessions.put(staff.getUniqueId(), new MonitorSession(target.getUniqueId(), displayName(target)));
            reachHistories.computeIfAbsent(target.getUniqueId(), ignored -> new ReachHistory());
        }
        sendReach(staff, "buildffa_reachcheck_started", "&aMonitoring server-observed melee distance for &f%player%&a.", displayName(target), null);
        sendReach(staff, "buildffa_reachcheck_limits",
                "&7Survival reach uses verified history and latency allowances. Shorter hits are not proof of legitimate reach.", "", null);
        sendStaffDisplay(staff, System.nanoTime());
    }

    public void stopReach(Player staff, boolean notify) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> stopReach(staff, notify));
            return;
        }
        MonitorSession removed = removeReachSession(staff.getUniqueId());
        if (staff.isOnline()) sendStaffDisplay(staff, System.nanoTime());
        if (notify) sendReach(staff, removed == null ? "buildffa_reachcheck_none" : "buildffa_reachcheck_stopped",
                removed == null ? "&cYou do not have an active reach check." : "&eStopped the reach check.", "", null);
    }

    private MonitorSession removeReachSession(UUID staffId) {
        return reachSessions.remove(staffId);
    }

    private boolean melee(EntityDamageByEntityEvent event) {
        return enabled && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity
                && !game.isFrozen((Player) event.getDamager())
                && (!(event.getEntity() instanceof Player) || !game.isFrozen((Player) event.getEntity()))
                && !event.getDamager().equals(event.getEntity()) && !game.isSpectating((Player) event.getDamager())
                && (!(event.getEntity() instanceof Player) || !game.isSpectating((Player) event.getEntity()))
                && event.getDamager().getWorld().equals(event.getEntity().getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void captureMelee(EntityDamageByEntityEvent event) {
        validateMelee(event);
    }

    public boolean validateMelee(EntityDamageByEntityEvent event) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Melee validation must run on the server thread");
        if (rejectedHits.contains(event)) {
            event.setCancelled(true);
            return false;
        }
        if (event.isCancelled()) return false;
        if (!melee(event)) return true;
        Player attacker = (Player) event.getDamager();
        if (game.getEventManager() == null || !game.getEventManager().isArenaParticipant(attacker)
                || game.getRespawningPlayers().contains(attacker.getUniqueId())
                || game.isBuildMode(attacker)
                || game.hasPermission(attacker, "buildffa.reachlimit.bypass")
                || game.isLobbyZone(event.getEntity().getLocation())) return true;
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (!game.getEventManager().isArenaParticipant(victim)
                    || game.getRespawningPlayers().contains(victim.getUniqueId())
                    || game.isBuildMode(victim)) return true;
        }
        if (pendingHits.containsKey(event)) return true;
        try {
            if (ReachDistance.teleportPending(attacker)
                    || event.getEntity() instanceof Player && ReachDistance.teleportPending((Player) event.getEntity())) return true;
            double distance = combat.reach(attacker, event.getEntity());
            ReachHistory history = reachHistories.computeIfAbsent(attacker.getUniqueId(), ignored -> new ReachHistory());
            if (ReachDistance.exceedsLimit(distance)) {
                event.setCancelled(true);
                rejectedHits.add(event);
                recordReachViolation(attacker, event.getEntity().getUniqueId(), history, distance);
                combat.alert(attacker, "Reach", distance, 3.1D, "hit cancelled");
                return false;
            }
            pendingHits.put(event, new HitSample(attacker.getUniqueId(), event.getEntity().getUniqueId(), history, distance));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!loggedReachFailure) {
                loggedReachFailure = true;
                game.getPlugin().getLogger().log(Level.WARNING, "Could not validate current server melee reach; skipping reach enforcement for unreadable hits", exception);
            }
        }
        return true;
    }

    private void recordReachViolation(Player attacker, UUID victimId, ReachHistory history, double distance) {
        long now = System.nanoTime();
        history.violations++;
        history.lastRejectedDistance = distance;
        if (history.recentViolations == 0 || now - history.violationWindowStart >= 60_000_000_000L) {
            history.violationWindowStart = now;
            history.recentViolations = 0;
        }
        history.recentViolations++;
        if (history.recentViolations >= 3 && (history.lastViolationLog == 0 || now - history.lastViolationLog >= 30_000_000_000L)) {
            history.lastViolationLog = now;
            game.getPlugin().getLogger().warning(String.format(Locale.ROOT,
                    "Reach cap: %s (%s), target %s, %.3f blocks; %d rejected in the current 60s window, %d this connection. Verified position history; the Survival limit is not a legitimacy verdict.",
                    attacker.getName(), attacker.getUniqueId(), victimId, distance, history.recentViolations, history.violations));
        }
        updateDisplays(attacker.getUniqueId(), now);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSuccessfulMelee(EntityDamageByEntityEvent event) {
        recordSuccessfulMelee(event);
    }

    public void recordSuccessfulMelee(EntityDamageByEntityEvent event) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Melee measurements must run on the server thread");
        if (event.isCancelled() || event.getFinalDamage() <= 0.0D || !melee(event)
                || game.isLobbyZone(event.getEntity().getLocation())) {
            pendingHits.remove(event);
            return;
        }
        HitSample sample = pendingHits.get(event);
        if (sample == null || reachHistories.get(sample.attackerId) != sample.history || !measuredHits.add(event)) return;
        sample.history.latestDistance = sample.distance;
        updateDisplays(sample.attackerId, System.nanoTime());
    }

    private void sendReach(Player staff, String key, String fallback, String playerName, Double distance) {
        staff.sendMessage(reachFormat(staff, key, fallback, playerName, distance));
    }

    private String reachFormat(Player staff, String key, String fallback, String playerName, Double distance) {
        Gamer gamer = Gamer.getGamer(staff);
        String value = gamer == null ? fallback : gamer.getLanguage().getMessage(key);
        if (value == null || value.equals(key) || value.startsWith("No translation (")) value = fallback;
        return ChatColor.translateAlternateColorCodes('&', value.replace("%player%", playerName == null ? "" : playerName)
                .replace("%distance%", String.format(Locale.ROOT, "%.2f", distance == null ? 0.0D : distance)));
    }

    private static final class ReachHistory {
        Double latestDistance;
        Double lastRejectedDistance;
        long violations;
        int recentViolations;
        long violationWindowStart;
        long lastViolationLog;
    }

    private static final class HitSample {
        final UUID attackerId;
        final UUID victimId;
        final ReachHistory history;
        final double distance;

        HitSample(UUID attackerId, UUID victimId, ReachHistory history, double distance) {
            this.attackerId = attackerId;
            this.victimId = victimId;
            this.history = history;
            this.distance = distance;
        }
    }

    private static final class MonitorSession {
        final UUID targetId;
        final String targetName;

        MonitorSession(UUID targetId) {
            this(targetId, "");
        }

        MonitorSession(UUID targetId, String targetName) {
            this.targetId = targetId;
            this.targetName = targetName;
        }
    }

    private static final class ClickHistory {
        final RollingWindow leftClicks = new RollingWindow();
        final RollingWindow rightClicks = new RollingWindow();
        boolean mining;
        boolean targetingBlock;
        boolean unpairedSwing;
        boolean countedSwing;
        long swingAt;
        boolean followingSwing;
        long actionAt;
        long windowStart;
        long unreliableUntil;
        boolean qualifying;
        final Map<PacketType, Long> lastPackets = new HashMap<>();
        long lastMovement;
        long placementAt;
        boolean placementUse;

        synchronized void observePacket(PacketType type, long now) {
            boolean movement = type == PacketType.Play.Client.FLYING || type == PacketType.Play.Client.POSITION
                    || type == PacketType.Play.Client.LOOK || type == PacketType.Play.Client.POSITION_LOOK;
            Long previous = movement ? (lastMovement == 0 ? null : lastMovement) : lastPackets.put(type, now);
            if (movement) lastMovement = now;
            if (previous != null && (now - previous < BURST_NANOS
                    || movement && now - previous > 150_000_000L))
                unreliableUntil = now + CPS_WINDOW_NANOS;
        }

        synchronized int penaltyWindow(long now, boolean eligible, boolean reliableTick) {
            if (!reliableTick) unreliableUntil = now + CPS_WINDOW_NANOS;
            if (!eligible || now < unreliableUntil || windowStart == 0) {
                windowStart = eligible ? (Math.floorDiv(now, RollingWindow.BUCKET_NANOS) + 1)
                        * RollingWindow.BUCKET_NANOS : 0;
                qualifying = false;
                return 0;
            }
            long end = windowStart + CPS_WINDOW_NANOS;
            if (now < end + ACTION_PAIR_NANOS) return 0;
            boolean exceeds = leftClicks.countRange(windowStart, end) > 20
                    || rightClicks.countRange(windowStart, end) > 20;
            int result = exceeds ? (qualifying ? 2 : 1) : 0;
            qualifying = exceeds;
            windowStart = end;
            return result;
        }

        synchronized void swing(long now) {
            if (followingSwing && now - actionAt >= 0 && now - actionAt < ACTION_PAIR_NANOS) {
                followingSwing = false;
                return;
            }
            followingSwing = false;
            countedSwing = !mining && !targetingBlock;
            if (countedSwing) leftClicks.add(now);
            swingAt = now;
            unpairedSwing = true;
        }

        synchronized void blockTarget(boolean targeted) {
            targetingBlock = targeted;
            if (!targeted) mining = false;
        }

        synchronized void startMining(long now) {
            discardSwing(now);
            mining = true;
            followingSwing = false;
        }

        synchronized void stopMining() {
            mining = false;
        }

        synchronized void finishMining(long now) {
            mining = false;
            followingSwing = true;
            actionAt = now;
        }

        synchronized void attack(long now) {
            boolean paired = discardSwing(now);
            leftClicks.add(now);
            mining = false;
            followingSwing = !paired;
            actionAt = now;
        }

        synchronized void use(long now, boolean blockPlacement) {
            if (!blockPlacement && placementUse && now - placementAt >= 0 && now - placementAt < ACTION_PAIR_NANOS) {
                placementUse = false;
                return;
            }
            placementUse = blockPlacement;
            placementAt = now;
            observePacket(PacketType.Play.Client.BLOCK_PLACE, now);
            rightClicks.add(now);
            if (blockPlacement) discardSwing(now);
            followingSwing = blockPlacement;
            actionAt = now;
        }

        private boolean discardSwing(long now) {
            boolean paired = unpairedSwing && now - swingAt >= 0 && now - swingAt < ACTION_PAIR_NANOS;
            if (paired && countedSwing) leftClicks.remove(swingAt);
            unpairedSwing = false;
            return paired;
        }

        synchronized long[] counts(long now) {
            return new long[]{leftClicks.count(now), rightClicks.count(now)};
        }
    }

    private static final class RollingWindow {
        private static final long BUCKET_NANOS = 1_000_000L;
        private static final int BUCKETS = (int) (2 * CPS_WINDOW_NANOS / BUCKET_NANOS);
        final long[] timestamps = new long[BUCKETS];
        final long[] samples = new long[BUCKETS];

        void add(long now) {
            long bucket = Math.floorDiv(now, BUCKET_NANOS);
            int index = (int) Math.floorMod(bucket, BUCKETS);
            if (timestamps[index] != bucket) {
                timestamps[index] = bucket;
                samples[index] = 0;
            }
            samples[index]++;
        }

        void remove(long now) {
            long bucket = Math.floorDiv(now, BUCKET_NANOS);
            int index = (int) Math.floorMod(bucket, BUCKETS);
            if (timestamps[index] == bucket && samples[index] > 0) samples[index]--;
        }

        long count(long now) {
            long bucket = Math.floorDiv(now, BUCKET_NANOS);
            long total = 0;
            for (int index = 0; index < BUCKETS; index++) {
                long age = bucket - timestamps[index];
                if (age >= 0 && age < CPS_WINDOW_NANOS / BUCKET_NANOS) total += samples[index];
                else if (age >= BUCKETS) samples[index] = 0;
            }
            return total;
        }

        long countRange(long start, long end) {
            long first = Math.floorDiv(start, BUCKET_NANOS);
            long last = Math.floorDiv(end, BUCKET_NANOS);
            long total = 0;
            for (long bucket = first; bucket < last; bucket++) {
                int index = (int) Math.floorMod(bucket, BUCKETS);
                if (timestamps[index] == bucket) total += samples[index];
            }
            return total;
        }
    }

    private static final class PenaltyEffect {
        final PotionEffect previous;
        final long previousUntil;
        final long until;

        PenaltyEffect(PotionEffect previous, long previousUntil, long until) {
            this.previous = previous;
            this.previousUntil = previousUntil;
            this.until = until;
        }

        boolean owns(PotionEffect current, long ticks) {
            return current != null && current.getAmplifier() == PENALTY_AMPLIFIER
                    && !current.isAmbient() && current.hasParticles()
                    && Math.abs(current.getDuration() - Math.max(0L, until - ticks)) <= 2L;
        }
    }
}
