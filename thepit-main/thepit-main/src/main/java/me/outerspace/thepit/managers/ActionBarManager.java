package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.NetworkProfileSnapshot;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ActionBarManager implements Listener {
    private final ThePitPlugin plugin;
    private final Map<UUID, TargetState> targets = new HashMap<>();
    private final Map<UUID, KilledState> killed = new HashMap<>();
    private final Map<UUID, String> normal = new HashMap<>();
    private final Map<UUID, String> lastRendered = new HashMap<>();
    private final Map<UUID, Long> lastSent = new HashMap<>();
    private BukkitTask task;
    private PacketSender sender;
    private AbsorptionReader absorptionReader;
    private boolean senderFailed;
    private boolean absorptionReaderFailed;
    private long tick;

    public ActionBarManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        shutdown();
        try {
            sender = new PacketSender();
        } catch (Throwable throwable) {
            senderFailed = true;
            plugin.getLogger().warning("The Pit action bars are unavailable on this server implementation.");
        }
        try {
            absorptionReader = new AbsorptionReader();
        } catch (Throwable throwable) {
            absorptionReaderFailed = true;
            plugin.getLogger().warning("The Pit absorption hearts are unavailable on this server implementation.");
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 1L, 1L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        task = null;
        for (UUID uuid : lastRendered.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) send(player, "");
        }
        targets.clear();
        killed.clear();
        normal.clear();
        lastRendered.clear();
        lastSent.clear();
    }

    public void publishNormal(Player player, String message) {
        if (player == null) return;
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> publishNormal(player, message));
            return;
        }
        if (message == null || message.isEmpty()) normal.remove(player.getUniqueId());
        else normal.put(player.getUniqueId(), CC.color(message));
        render(player, true);
    }

    public void clearNormal(UUID uuid) {
        if (uuid == null) return;
        normal.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) render(player, true);
    }

    public void confirmKill(Player killerPlayer, Player victim) {
        if (killerPlayer == null || victim == null || killerPlayer.equals(victim)) return;
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> confirmKill(killerPlayer, victim));
            return;
        }
        UUID killerId = killerPlayer.getUniqueId();
        TargetState active = targets.get(killerId);
        if (active == null || !active.target.equals(victim.getUniqueId())) return;
        clearTargetReferences(victim.getUniqueId(), killerId);
        long duration = Math.max(1L, settingLong("killed-display-ticks", 40L));
        killed.put(killerId, new KilledState(victim.getUniqueId(), tick + duration, active.arena, active.viewerWorld));
        render(killerPlayer, true);
    }

    public void clearCombatStates() {
        targets.clear();
        killed.clear();
        refreshTracked();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!enabled() || !(event.getEntity() instanceof Player victim) || event.getFinalDamage() <= 0.0D) return;
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null || attacker.equals(victim) || !plugin.getPerkManager().validCombat(attacker, victim)) return;
        String arena = plugin.getArenaManager().getCurrentArena();
        if (arena == null) return;
        killed.remove(attacker.getUniqueId());
        targets.put(attacker.getUniqueId(), new TargetState(victim.getUniqueId(),
                tick + Math.max(1L, settingLong("display-ticks", 100L)), arena,
                attacker.getWorld().getUID(), victim.getWorld().getUID()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        targets.remove(uuid);
        killed.remove(uuid);
        normal.remove(uuid);
        lastRendered.remove(uuid);
        lastSent.remove(uuid);
        clearTargetReferences(uuid, null);
        clearKilledReferences(uuid);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        targets.remove(uuid);
        killed.remove(uuid);
        clearTargetReferences(uuid, null);
        clearKilledReferences(uuid);
        render(event.getPlayer(), true);
    }

    private void update() {
        tick++;
        if (!enabled() && (!targets.isEmpty() || !killed.isEmpty())) {
            targets.clear();
            killed.clear();
            refreshTracked();
        }
        Iterator<Map.Entry<UUID, TargetState>> targetIterator = targets.entrySet().iterator();
        while (targetIterator.hasNext()) {
            Map.Entry<UUID, TargetState> entry = targetIterator.next();
            Player viewer = Bukkit.getPlayer(entry.getKey());
            Player target = Bukkit.getPlayer(entry.getValue().target);
            if (tick >= entry.getValue().expiresAt || !validTargetState(viewer, target, entry.getValue())) {
                targetIterator.remove();
                if (viewer != null) render(viewer, true);
            }
        }
        Iterator<Map.Entry<UUID, KilledState>> killedIterator = killed.entrySet().iterator();
        while (killedIterator.hasNext()) {
            Map.Entry<UUID, KilledState> entry = killedIterator.next();
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (tick >= entry.getValue().expiresAt || !validKilledState(viewer, entry.getValue())) {
                killedIterator.remove();
                if (viewer != null) render(viewer, true);
            }
        }
        for (UUID uuid : trackedViewers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) render(player, false);
        }
    }

    private void render(Player viewer, boolean force) {
        if (viewer == null || !viewer.isOnline()) return;
        String message = desired(viewer);
        UUID uuid = viewer.getUniqueId();
        String previous = lastRendered.get(uuid);
        long previousTick = lastSent.getOrDefault(uuid, Long.MIN_VALUE / 2L);
        boolean keepAlive = tick - previousTick >= 40L && !message.isEmpty();
        if (!force && message.equals(previous) && !keepAlive) return;
        if (message.equals(previous) && force && !keepAlive) return;
        send(viewer, message);
        if (message.isEmpty()) lastRendered.remove(uuid);
        else lastRendered.put(uuid, message);
        lastSent.put(uuid, tick);
    }

    private String desired(Player viewer) {
        KilledState killedState = killed.get(viewer.getUniqueId());
        if (killedState != null) {
            Player victim = Bukkit.getPlayer(killedState.target);
            return format(victim, plugin.registry().settings().getString(path("killed-format"),
                    "%abelix_prefix%%player% &aKilled!"), "");
        }
        TargetState state = targets.get(viewer.getUniqueId());
        if (state != null) {
            Player target = Bukkit.getPlayer(state.target);
            if (target != null) {
                String hearts = renderHearts(target);
                return format(target, plugin.registry().settings().getString(path("format"),
                        "%abelix_prefix%%player% %health_hearts%"), hearts);
            }
        }
        return normal.getOrDefault(viewer.getUniqueId(), "");
    }

    private String format(Player target, String configured, String hearts) {
        String name = target == null || target.getName() == null ? "" : target.getName();
        String prefix = "";
        if (target != null && plugin.getNetworkProfileCache() != null) {
            NetworkProfileSnapshot snapshot = plugin.getNetworkProfileCache().cached(target.getUniqueId());
            if (snapshot != null) {
                prefix = snapshot.getPrefix();
                if (prefix == null || prefix.isEmpty()) prefix = snapshot.getPermissionPrefix();
            }
        }
        if (prefix == null) prefix = "";
        return CC.color(configured
                .replace("%abelix_prefix%", prefix)
                .replace("%player%", name)
                .replace("%health_hearts%", hearts));
    }

    private String renderHearts(Player target) {
        int baseHearts = Math.max(0, settingInt("base-hearts", 10));
        int maximumRendered = Math.max(1, settingInt("maximum-rendered-hearts", 30));
        double maximumHealth = Math.max(0.0D, finite(target.getMaxHealth(), 20.0D));
        double health = Math.max(0.0D, Math.min(maximumHealth, finite(target.getHealth(), 0.0D)));
        double absorption = Math.max(0.0D, finite(absorption(target), 0.0D));
        String normalFull = setting("normal-full-heart", "&c❤");
        String bonusFull = setting("permanent-bonus-heart", "&6❤");
        String absorptionFull = setting("absorption-heart", "&e❤");
        String empty = setting("empty-heart", "&8❤");
        String half = setting("half-heart", "♥");
        return renderHearts(health, maximumHealth, absorption, baseHearts, maximumRendered,
                normalFull, bonusFull, absorptionFull, empty, half);
    }

    private static String renderHearts(double health, double maximumHealth, double absorption,
                                       int baseHearts, int maximumRendered, String normalFull,
                                       String bonusFull, String absorptionFull, String empty, String half) {
        maximumHealth = Math.max(0.0D, finiteValue(maximumHealth, 20.0D));
        health = Math.max(0.0D, Math.min(maximumHealth, finiteValue(health, 0.0D)));
        absorption = Math.max(0.0D, finiteValue(absorption, 0.0D));
        baseHearts = Math.max(0, baseHearts);
        maximumRendered = Math.max(1, maximumRendered);
        int permanentSlots = Math.min(maximumRendered, (int) Math.ceil(maximumHealth / 2.0D));
        int absorptionSlots = Math.min(maximumRendered - permanentSlots, (int) Math.ceil(absorption / 2.0D));
        StringBuilder out = new StringBuilder();
        double remainingHealth = health;
        for (int slot = 0; slot < permanentSlots; slot++) {
            String full = slot < baseHearts ? normalFull : bonusFull;
            appendHeart(out, remainingHealth, full, empty, half);
            remainingHealth = Math.max(0.0D, remainingHealth - 2.0D);
        }
        double remainingAbsorption = absorption;
        for (int slot = 0; slot < absorptionSlots; slot++) {
            appendHeart(out, remainingAbsorption, absorptionFull, empty, half);
            remainingAbsorption = Math.max(0.0D, remainingAbsorption - 2.0D);
        }
        return CC.color(out.toString());
    }

    private static void appendHeart(StringBuilder out, double points, String full, String empty, String half) {
        if (points >= 2.0D - 0.0001D) out.append(full);
        else if (points > 0.0001D) out.append(ChatColor.getLastColors(CC.color(full))).append(half);
        else out.append(empty);
    }

    private boolean validTargetState(Player viewer, Player target, TargetState state) {
        if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()
                || viewer.isDead() || target.isDead() || viewer.equals(target)) return false;
        if (!state.viewerWorld.equals(viewer.getWorld().getUID()) || !state.targetWorld.equals(target.getWorld().getUID())
                || viewer.getWorld() != target.getWorld()) return false;
        if (!state.arena.equals(plugin.getArenaManager().getCurrentArena())) return false;
        return plugin.getPerkManager().validCombat(viewer, target);
    }

    private boolean validKilledState(Player viewer, KilledState state) {
        if (viewer == null || !viewer.isOnline() || viewer.isDead()) return false;
        if (!state.viewerWorld.equals(viewer.getWorld().getUID())) return false;
        return state.arena.equals(plugin.getArenaManager().getCurrentArena());
    }

    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (!(damager instanceof Projectile)) return null;
        Object shooter = ((Projectile) damager).getShooter();
        return shooter instanceof Player ? (Player) shooter : null;
    }

    private void clearTargetReferences(UUID target, UUID exceptViewer) {
        Iterator<Map.Entry<UUID, TargetState>> iterator = targets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TargetState> entry = iterator.next();
            if (entry.getValue().target.equals(target) && (exceptViewer == null || !entry.getKey().equals(exceptViewer))) {
                UUID viewer = entry.getKey();
                iterator.remove();
                Player player = Bukkit.getPlayer(viewer);
                if (player != null) render(player, true);
            }
        }
    }

    private void clearKilledReferences(UUID target) {
        Iterator<Map.Entry<UUID, KilledState>> iterator = killed.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, KilledState> entry = iterator.next();
            if (!entry.getValue().target.equals(target)) continue;
            UUID viewer = entry.getKey();
            iterator.remove();
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) render(player, true);
        }
    }

    private void refreshTracked() {
        for (UUID uuid : trackedViewers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) render(player, true);
        }
    }

    private java.util.Set<UUID> trackedViewers() {
        java.util.Set<UUID> viewers = new java.util.HashSet<>(normal.keySet());
        viewers.addAll(targets.keySet());
        viewers.addAll(killed.keySet());
        viewers.addAll(lastRendered.keySet());
        return viewers;
    }

    private void send(Player player, String message) {
        try {
            if (sender == null && !senderFailed) sender = new PacketSender();
            if (sender != null) sender.send(player, message);
        } catch (Throwable throwable) {
            senderFailed = true;
            sender = null;
            plugin.getLogger().warning("The Pit action bars are unavailable on this server implementation.");
        }
    }

    private boolean enabled() {
        return plugin.registry().settings().getBoolean(path("enabled"), true);
    }

    private String path(String key) {
        return "action-bar.target-health." + key;
    }

    private String setting(String key, String fallback) {
        return plugin.registry().settings().getString(path(key), fallback);
    }

    private int settingInt(String key, int fallback) {
        return plugin.registry().settings().getInt(path(key), fallback);
    }

    private long settingLong(String key, long fallback) {
        return plugin.registry().settings().getLong(path(key), fallback);
    }

    private double finite(double value, double fallback) {
        return Double.isNaN(value) || Double.isInfinite(value) ? fallback : value;
    }

    private static double finiteValue(double value, double fallback) {
        return Double.isNaN(value) || Double.isInfinite(value) ? fallback : value;
    }

    private double absorption(Player player) {
        try {
            if (absorptionReader == null && !absorptionReaderFailed) absorptionReader = new AbsorptionReader();
            return absorptionReader == null ? 0.0D : absorptionReader.read(player);
        } catch (Throwable throwable) {
            absorptionReaderFailed = true;
            absorptionReader = null;
            return 0.0D;
        }
    }

    private record TargetState(UUID target, long expiresAt, String arena, UUID viewerWorld, UUID targetWorld) {}

    private record KilledState(UUID target, long expiresAt, String arena, UUID viewerWorld) {}

    private static final class PacketSender {
        private final Method getHandle;
        private final Field connection;
        private final Method sendPacket;
        private final Method parse;
        private final Constructor<?> packet;

        private PacketSender() throws Exception {
            String craft = Bukkit.getServer().getClass().getPackage().getName();
            String version = craft.substring(craft.lastIndexOf('.') + 1);
            String nms = "net.minecraft.server." + version + ".";
            Class<?> craftPlayer = Class.forName(craft + ".entity.CraftPlayer");
            Class<?> entityPlayer = Class.forName(nms + "EntityPlayer");
            Class<?> playerConnection = Class.forName(nms + "PlayerConnection");
            Class<?> packetType = Class.forName(nms + "Packet");
            Class<?> component = Class.forName(nms + "IChatBaseComponent");
            Class<?> serializer = Class.forName(nms + "IChatBaseComponent$ChatSerializer");
            Class<?> chatPacket = Class.forName(nms + "PacketPlayOutChat");
            getHandle = craftPlayer.getMethod("getHandle");
            connection = entityPlayer.getField("playerConnection");
            sendPacket = playerConnection.getMethod("sendPacket", packetType);
            parse = serializer.getMethod("a", String.class);
            packet = chatPacket.getConstructor(component, byte.class);
        }

        private void send(Player player, String text) throws Exception {
            Object handle = getHandle.invoke(player);
            Object component = parse.invoke(null, "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
            sendPacket.invoke(connection.get(handle), packet.newInstance(component, (byte) 2));
        }
    }

    private static final class AbsorptionReader {
        private final Method getHandle;
        private final Method getAbsorptionHearts;

        private AbsorptionReader() throws Exception {
            String craft = Bukkit.getServer().getClass().getPackage().getName();
            String version = craft.substring(craft.lastIndexOf('.') + 1);
            Class<?> craftPlayer = Class.forName(craft + ".entity.CraftPlayer");
            Class<?> entityLiving = Class.forName("net.minecraft.server." + version + ".EntityLiving");
            getHandle = craftPlayer.getMethod("getHandle");
            getAbsorptionHearts = entityLiving.getMethod("getAbsorptionHearts");
        }

        private double read(Player player) throws Exception {
            Object value = getAbsorptionHearts.invoke(getHandle.invoke(player));
            return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
        }
    }
}
