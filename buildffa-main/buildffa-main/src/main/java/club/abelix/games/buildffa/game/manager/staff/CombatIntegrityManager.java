package club.abelix.games.buildffa.game.manager.staff;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.event.GameDeathEvent;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import java.util.*;

public final class CombatIntegrityManager extends YListener {
    private final BuildFFAGame game;
    private final Map<UUID, State> states = new HashMap<>();
    private final Set<UUID> viewers = new HashSet<>();
    private final Map<String, Long> alerts = new HashMap<>();
    private BukkitTask task;
    private long tick, lastTick, uncertainUntil;
    private final java.util.concurrent.ConcurrentMap<UUID, java.util.concurrent.atomic.AtomicLong> movements = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean enabled;
    private volatile long generation;

    CombatIntegrityManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
    }

    void enable() {
        enabled = true;
        generation++;
        task = Bukkit.getScheduler().runTaskTimer(game.getPlugin(), this::sample, 1L, 1L);
    }

    void disable() {
        enabled = false;
        generation++;
        if (task != null) task.cancel();
        states.clear();
        movements.clear();
        viewers.clear();
        alerts.clear();
    }

    private int ping(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return -1;
        }
    }

    private boolean eligible(Player p) {
        return !game.isFrozen(p) && p.isOnline() && !p.isDead() && p.getGameMode() == GameMode.SURVIVAL
                && !p.getAllowFlight() && !p.isInsideVehicle() && !game.isSpectating(p)
                && !game.isBuildMode(p) && !game.getRespawningPlayers().contains(p.getUniqueId())
                && game.getEventManager().isArenaParticipant(p) && !game.isLobbyZone(p.getLocation());
    }

    private void sample() {
        tick++;
        long now = System.nanoTime();
        if (lastTick == 0 || now - lastTick > 100_000_000L) {
            uncertainUntil = tick + 40;
            states.clear();
        }
        lastTick = now;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!eligible(p)) { states.remove(p.getUniqueId()); continue; }
            State s = states.computeIfAbsent(p.getUniqueId(), id -> new State());
            Location pos = p.getLocation();
            if (!s.history.isEmpty()) {
                Location previous = s.history.peekLast().position;
                if (!previous.getWorld().equals(pos.getWorld()) || previous.distanceSquared(pos) > 4) {
                    reset(p); continue;
                }
            }
            s.history.addLast(new Frame(tick, pos.clone(), p.getEyeHeight()));
            while (!s.history.isEmpty() && s.history.peekFirst().tick < tick - 12) s.history.removeFirst();
            if (s.pending != null) evaluate(p, s);
        }
    }

    double reach(Player attacker, Entity target) throws ReflectiveOperationException {
        double current = ReachDistance.measure(attacker.getEyeLocation(), target);
        int latency = ping(attacker);
        State a = states.get(attacker.getUniqueId());
        State v = states.get(target.getUniqueId());
        if (tick < uncertainUntil || latency < 0 || latency > 250 || a == null || v == null
                || a.history.size() < 10 || v.history.size() < 10 || tick < a.exemptUntil || tick < v.exemptUntil)
            return Double.NaN;
        long oldest = tick - (int) Math.ceil(latency / 50.0) - 3;
        double minimum = current;
        Location targetNow = target.getLocation();
        for (Frame eye : a.history) {
            if (eye.tick < tick - 2) continue;
            for (Frame box : v.history) {
                if (box.tick < oldest) continue;
                Location translatedEye = eye.position.clone().add(0, eye.eyeHeight, 0)
                        .add(targetNow.getX() - box.position.getX(), targetNow.getY() - box.position.getY(),
                                targetNow.getZ() - box.position.getZ());
                minimum = Math.min(minimum, ReachDistance.measure(translatedEye, target));
            }
        }
        return minimum;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void hit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)
                || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || event.getFinalDamage() <= 0) return;
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        State s = states.get(victim.getUniqueId());
        if (s != null && eligible(attacker) && eligible(victim)) { s.hitTick = tick; s.hitOrigin = victim.getLocation().clone(); }
    }

    void movementPacket(PacketEvent event) {
        com.comphenix.protocol.PacketType type = event.getPacketType();
        if (event.isCancelled() || !enabled) return;
        if (type == com.comphenix.protocol.PacketType.Play.Client.FLYING || type == com.comphenix.protocol.PacketType.Play.Client.POSITION
                || type == com.comphenix.protocol.PacketType.Play.Client.LOOK || type == com.comphenix.protocol.PacketType.Play.Client.POSITION_LOOK)
            movements.computeIfAbsent(event.getPlayer().getUniqueId(), id -> new java.util.concurrent.atomic.AtomicLong()).incrementAndGet();
    }

    void velocityPacket(PacketEvent event) {
        if (!enabled || event.isCancelled() || event.getPacket().getIntegers().read(0) != event.getPlayer().getEntityId()) return;
        UUID id = event.getPlayer().getUniqueId();
        Vector sent = new Vector(event.getPacket().getIntegers().read(1) / 8000.0,
                event.getPacket().getIntegers().read(2) / 8000.0, event.getPacket().getIntegers().read(3) / 8000.0);
        long token = generation;
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            if (!enabled || token != generation) return;
            Player p = Bukkit.getPlayer(id);
            State s = states.get(id);
            if (p == null || s == null) return;
            if (s.pending != null) { s.pending = null; s.exemptUntil = tick + 20; return; }
            int latency = ping(p);
            if (!eligible(p) || game.hasPermission(p, "buildffa.antiknockback.bypass") || s.hitOrigin == null || s.hitTick < tick - 2 || tick < s.knockbackUntil || tick < s.exemptUntil || tick < uncertainUntil
                    || latency < 0 || latency > 200 || !clear(p) || sent.clone().setY(0).length() < 0.15) return;
            s.pending = new Impulse(tick, s.hitOrigin.clone(), sent, latency);
            s.pending.startPackets = movements.getOrDefault(id, new java.util.concurrent.atomic.AtomicLong()).get();
            s.hitTick = Long.MIN_VALUE;
        });
    }

    private boolean clear(Player p) {
        if (!game.getEventManager().getActiveEventIds().isEmpty() || !p.getActivePotionEffects().isEmpty()) return false;
        Location l = p.getLocation();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 0; y <= 2; y++) {
            Material m = l.clone().add(x, y, z).getBlock().getType();
            if (m != Material.AIR) return false;
        }
        return true;
    }

    private void evaluate(Player p, State s) {
        Impulse i = s.pending;
        if (!eligible(p) || !clear(p) || tick < uncertainUntil || tick < s.exemptUntil
                || tick < s.knockbackUntil || ping(p) < 0 || Math.abs(ping(p) - i.ping) > 50) { s.pending = null; return; }
        Vector displacement = p.getLocation().toVector().subtract(i.origin.toVector()).setY(0);
        Vector direction = i.velocity.clone().setY(0).normalize();
        i.observed = Math.max(i.observed, displacement.dot(direction));
        if (tick - i.tick < 10 + (int) Math.ceil(i.ping / 50.0)) return;
        s.pending = null;
        if (movements.getOrDefault(p.getUniqueId(), new java.util.concurrent.atomic.AtomicLong()).get() - i.startPackets < 6) return;
        double expected = i.velocity.clone().setY(0).length();
        if (i.observed >= expected * 0.15) { s.violations = 0; return; }
        if (tick - s.lastViolation > 600) s.violations = 0;
        s.lastViolation = tick;
        if (++s.violations >= 3) alert(p, "Anti-Knockback", i.observed, expected * 0.15, "detected; no correction");
    }

    public void executeAlerts(Player staff, String[] args) {
        if (!game.hasPermission(staff, "buildffa.staff")) { message(staff, "buildffa_alerts_no_permission", "&cYou do not have permission to use staff alerts."); return; }
        if (args.length != 2 || !args[0].equalsIgnoreCase("alerts")
                || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            message(staff, "buildffa_alerts_usage", "&cUsage: /bffa alerts <on|off>"); return;
        }
        boolean on = args[1].equalsIgnoreCase("on");
        if (on) viewers.add(staff.getUniqueId()); else viewers.remove(staff.getUniqueId());
        message(staff, on ? "buildffa_alerts_enabled" : "buildffa_alerts_disabled", on ? "&aStaff alerts enabled." : "&eStaff alerts disabled.");
    }

    void alert(Player player, String check, double measured, double limit, String action) {
        String key = player.getUniqueId() + ":" + check;
        long now = System.nanoTime();
        if (now - alerts.getOrDefault(key, 0L) < 3_000_000_000L) return;
        alerts.put(key, now);
        for (UUID id : viewers) {
            Player staff = Bukkit.getPlayer(id);
            if (staff != null && game.hasPermission(staff, "buildffa.staff")) message(staff, "buildffa_combat_alert",
                    "&8[&cBuildFFA&8] &f%player% &7%check%: measured=%measured%, limit=%limit%; %action%",
                    "%player%", player.getName(), "%check%", check, "%measured%", String.format(Locale.ROOT, "%.3f", measured),
                    "%limit%", String.format(Locale.ROOT, "%.3f", limit), "%action%", action);
        }
    }

    private void message(Player p, String key, String fallback, String... replacements) {
        Gamer gamer = Gamer.getGamer(p);
        String text = gamer == null ? fallback : gamer.getLanguage().getMessage(key);
        if (text == null || text.equals(key) || text.startsWith("No translation (")) text = fallback;
        for (int i = 0; i < replacements.length; i += 2) text = text.replace(replacements[i], replacements[i + 1]);
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
    }

    private void reset(Player p) {
        State s = new State(); s.exemptUntil = tick + 40; states.put(p.getUniqueId(), s);
        alerts.keySet().removeIf(key -> key.startsWith(p.getUniqueId().toString()));
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void ability(PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        State s = states.get(e.getPlayer().getUniqueId());
        if (s != null) { s.pending = null; s.knockbackUntil = tick + 40; }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void otherDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && (!(e instanceof EntityDamageByEntityEvent)
                || !(((EntityDamageByEntityEvent) e).getDamager() instanceof Player)
                || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK)) reset((Player) e.getEntity());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent e) { reset(e.getPlayer()); }
    @EventHandler public void world(PlayerChangedWorldEvent e) { reset(e.getPlayer()); }
    @EventHandler public void respawn(PlayerRespawnEvent e) { reset(e.getPlayer()); }
    @EventHandler public void death(PlayerDeathEvent e) { reset(e.getEntity()); }
    @EventHandler public void gameDeath(GameDeathEvent e) { reset(e.getVictim()); }
    @EventHandler public void quit(PlayerQuitEvent e) { reset(e.getPlayer()); states.remove(e.getPlayer().getUniqueId()); movements.remove(e.getPlayer().getUniqueId()); viewers.remove(e.getPlayer().getUniqueId()); }

    private static final class State {
        final Deque<Frame> history = new ArrayDeque<>();
        long exemptUntil, knockbackUntil, hitTick = Long.MIN_VALUE, lastViolation;
        int violations; Location hitOrigin;
        Impulse pending;
    }
    private static final class Frame {
        final long tick; final Location position; final double eyeHeight;
        Frame(long tick, Location position, double eyeHeight) { this.tick = tick; this.position = position; this.eyeHeight = eyeHeight; }
    }
    private static final class Impulse {
        final long tick; final Location origin; final Vector velocity; final int ping;
        double observed; long startPackets;
        Impulse(long tick, Location origin, Vector velocity, int ping) { this.tick = tick; this.origin = origin; this.velocity = velocity; this.ping = ping; }
    }
}
