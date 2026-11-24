package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.hooks.AbelixHook;
import me.outerspace.thepit.registry.AbelixRankRegistry;
import me.outerspace.thepit.registry.definition.AbelixRankDefinition;
import me.outerspace.thepit.model.NetworkProfileSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import me.outerspace.thepit.registry.RegistryNode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AbelixRankPermissionManager implements Listener {
    private static final String ROOT = "rank-detection";
    private static final List<String> CATEGORIES = Arrays.asList("admin-staff", "staff", "player-ranks");
    private static final List<String> DEFAULT_PLACEHOLDERS = Arrays.asList(
            "%abelix_rank%", "%abelix_group%", "%fakepixel_rank%", "%fakepixel_group%");
    private static final List<String> RANK_EVENT_CLASSES = Arrays.asList(
            "club.abelix.api.bukkit.event.gamer.GamerRankChangeEvent",
            "club.abelix.api.bukkit.event.gamer.GamerGroupChangeEvent",
            "club.abelix.api.event.gamer.GamerRankChangeEvent",
            "club.abelix.api.event.gamer.GamerGroupChangeEvent",
            "club.abelix.api.base.gamer.event.GamerRankChangeEvent",
            "club.abelix.api.base.gamer.event.GamerGroupChangeEvent",
            "me.abelix.api.event.gamer.GamerRankChangeEvent",
            "me.abelix.api.event.gamer.GamerGroupChangeEvent",
            "net.fakepixel.api.event.gamer.GamerRankChangeEvent",
            "net.fakepixel.api.event.gamer.GamerGroupChangeEvent",
            "club.abelix.api.bukkit.event.gamer.GamerGoldChangeEvent",
            "club.abelix.api.event.gamer.GamerGoldChangeEvent",
            "club.abelix.api.base.gamer.event.GamerGoldChangeEvent",
            "me.abelix.api.event.gamer.GamerGoldChangeEvent",
            "net.fakepixel.api.event.gamer.GamerGoldChangeEvent");

    private final ThePitPlugin plugin;
    private final AbelixHook abelixHook;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final Map<UUID, AppliedRank> applied = new HashMap<>();
    private final Map<String, RankDefinition> ranks = new HashMap<>();
    private final Set<String> loggedUnknownRanks = new HashSet<>();
    private BukkitTask refreshTask;
    private boolean listenersRegistered;
    private boolean enabled;

    public AbelixRankPermissionManager(ThePitPlugin plugin, AbelixHook abelixHook) {
        this.plugin = plugin;
        this.abelixHook = abelixHook;
    }

    public void start() {
        if (!listenersRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registerAbelixRankEvents();
            listenersRegistered = true;
        }
        reload();
        Bukkit.getScheduler().runTaskLater(plugin, this::reapplyWildcardRanks, 40L);
    }

    public void reload() {
        runSync(() -> {
            if (refreshTask != null) {
                refreshTask.cancel();
                refreshTask = null;
            }
            loadRanks();
            enabled = plugin.registry().abelixRanks().getBoolean(ROOT + ".enabled", true);
            if (!enabled) {
                removeAllAttachments();
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) refreshNow(player, true);
            long interval = Math.max(1L, plugin.registry().abelixRanks().getLong(ROOT + ".refresh-ticks", 200L));
            refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshOnlinePlayers, interval, interval);
        });
    }

    public void shutdown() {
        runSync(() -> {
            enabled = false;
            if (refreshTask != null) refreshTask.cancel();
            refreshTask = null;
            removeAllAttachments();
            HandlerList.unregisterAll(this);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer(), false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) refreshNow(event.getPlayer(), false);
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        removeAttachment(event.getPlayer());
    }

    public void refresh(Player player, boolean force) {
        if (player == null) return;
        runSync(() -> refreshNow(player, force));
    }

    public void sendDebug(CommandSender sender, Player player) {
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or is not online.");
            return;
        }
        runSync(() -> {
            refreshNow(player, false);
            Detection detection = detect(player);
            AppliedRank current = applied.get(player.getUniqueId());
            RankDefinition matched = ranks.get(detection.normalizedRank);
            if (matched == null) matched = ranks.get("DEFAULT");
            sender.sendMessage(ChatColor.AQUA + "The Pit Abelix rank debug for " + ChatColor.WHITE + player.getName());
            sender.sendMessage(ChatColor.GRAY + "Raw Abelix rank: " + ChatColor.WHITE + display(detection.rawRank));
            sender.sendMessage(ChatColor.GRAY + "Normalized rank: " + ChatColor.WHITE + detection.normalizedRank);
            sender.sendMessage(ChatColor.GRAY + "Detection method: " + ChatColor.WHITE + detection.method);
            sender.sendMessage(ChatColor.GRAY + "Matched category: " + ChatColor.WHITE + (matched == null ? "player-ranks.DEFAULT (implicit)" : matched.category + "." + matched.name));
            Collection<String> permissions = current == null ? Collections.<String>emptyList() : current.permissions;
            sender.sendMessage(ChatColor.GRAY + "Applied permissions (" + permissions.size() + "): " + ChatColor.WHITE
                    + (permissions.isEmpty() ? "(none)" : join(permissions)));
        });
    }

    private void refreshOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) refreshNow(player, false);
    }

    private void refreshNow(Player player, boolean force) {
        if (!enabled || player == null || !player.isOnline()) return;
        Detection detection = detect(player);
        RankDefinition definition = ranks.get(detection.normalizedRank);
        if (definition == null) {
            if (!"DEFAULT".equals(detection.normalizedRank) && loggedUnknownRanks.add(detection.normalizedRank)) {
                plugin.getLogger().warning("Unknown Abelix rank '" + detection.rawRank + "' (normalized: "
                        + detection.normalizedRank + "); applying DEFAULT permissions.");
            }
            definition = ranks.get("DEFAULT");
        }
        if (definition == null) definition = new RankDefinition("DEFAULT", "player-ranks", Collections.<String>emptyList());

        AppliedRank old = applied.get(player.getUniqueId());
        if (!force && old != null && old.rank.equals(definition.name)) {
            old.rawRank = detection.rawRank;
            old.normalizedRank = detection.normalizedRank;
            old.method = detection.method;
            return;
        }

        removeAttachment(player);
        Set<String> expanded = expandPermissions(definition.permissions);
        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String permission : expanded) attachment.setPermission(permission, true);
        attachments.put(player.getUniqueId(), attachment);
        applied.put(player.getUniqueId(), new AppliedRank(definition.name, detection.rawRank,
                detection.normalizedRank, detection.method, expanded, hasWildcard(definition.permissions)));
        player.recalculatePermissions();
        if (plugin.getNetworkProfileCache() != null) plugin.getNetworkProfileCache().invalidate(player);
    }

    private Detection detect(Player player) {
        NetworkProfileSnapshot profile = plugin.getNetworkProfileCache() == null
                ? plugin.resolveNetworkProfile(player) : plugin.getNetworkProfileCache().get(player);
        if (profile.getGroup() != null && !profile.getGroup().trim().isEmpty() && !profile.getGroup().equalsIgnoreCase("default")) {
            return detection(profile.getGroup(), "Cached Abelix profile group");
        }

        List<String> placeholders = plugin.registry().abelixRanks().getStringList(ROOT + ".rank-placeholders");
        if (placeholders == null || placeholders.isEmpty()) {
            placeholders = plugin.registry().abelixRanks().getStringList("abelix.rank-placeholders");
        }
        if (placeholders == null || placeholders.isEmpty()) placeholders = DEFAULT_PLACEHOLDERS;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            for (String placeholder : placeholders) {
                if (placeholder == null || placeholder.trim().isEmpty()) continue;
                String value = plugin.applyPlaceholders(player, placeholder);
                if (resolved(value, placeholder)) return detection(value.trim(), "PlaceholderAPI " + placeholder);
            }
        }

        String prefix = profile.getPrefix();
        String prefixRank = rankFromPrefix(prefix);
        if (prefixRank != null) return detection(prefixRank, "Abelix colored prefix (final fallback)");
        return detection(null, "No Abelix API, placeholder, or prefix rank resolved");
    }

    private Detection detection(String raw, String method) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) normalized = "DEFAULT";
        return new Detection(raw, normalized, method);
    }

    public static String normalize(String rank) {
        return AbelixRankRegistry.normalize(rank);
    }

    private String rankFromPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) return null;
        String normalizedPrefix = normalize(prefix.replace('[', ' ').replace(']', ' '));
        List<String> names = new ArrayList<>(ranks.keySet());
        names.remove("DEFAULT");
        names.sort(Comparator.comparingInt(String::length).reversed());
        for (String name : names) {
            if (normalizedPrefix.equals(name) || normalizedPrefix.startsWith(name + "_")
                    || normalizedPrefix.endsWith("_" + name) || normalizedPrefix.contains("_" + name + "_")) return name;
        }
        return null;
    }

    private boolean resolved(String value, String placeholder) {
        if (value == null) return false;
        String trimmed = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', value)).trim();
        return !trimmed.isEmpty() && !trimmed.equalsIgnoreCase(placeholder)
                && !(trimmed.startsWith("%") && trimmed.endsWith("%"));
    }

    private void loadRanks() {
        ranks.clear();
        for (AbelixRankDefinition definition : plugin.registry().abelixRanks().all().values()) {
            String category = definition.category().name().toLowerCase(Locale.ENGLISH).replace("player_rank", "player-ranks").replace("admin_staff", "admin-staff");
            ranks.put(definition.normalizedName(), new RankDefinition(definition.normalizedName(), category, definition.permissions()));
        }
        if (!ranks.containsKey("DEFAULT")) {
            ranks.put("DEFAULT", new RankDefinition("DEFAULT", "player-ranks", Collections.<String>emptyList()));
        }
    }

    private Set<String> expandPermissions(List<String> configured) {
        Set<String> expanded = new LinkedHashSet<>();
        if (configured == null) return expanded;
        Collection<Permission> registered = Bukkit.getPluginManager().getPermissions();
        for (String entry : configured) {
            if (entry == null) continue;
            String permission = entry.trim().toLowerCase(Locale.ENGLISH);
            if (permission.isEmpty()) continue;
            if (permission.equals("*")) {
                for (Permission known : registered) expanded.add(known.getName().toLowerCase(Locale.ENGLISH));
            } else if (permission.endsWith(".*")) {
                String prefix = permission.substring(0, permission.length() - 1);
                for (Permission known : registered) {
                    String name = known.getName().toLowerCase(Locale.ENGLISH);
                    if (name.startsWith(prefix)) expanded.add(name);
                }
            } else {
                expanded.add(permission);
            }
        }
        return expanded;
    }

    private void reapplyWildcardRanks() {
        if (!enabled) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            AppliedRank state = applied.get(player.getUniqueId());
            if (state != null && state.wildcard) refreshNow(player, true);
        }
    }

    private boolean hasWildcard(List<String> permissions) {
        if (permissions == null) return false;
        for (String permission : permissions) {
            if (permission == null) continue;
            String lower = permission.toLowerCase(Locale.ENGLISH);
            if (lower.equals("*") || lower.endsWith(".*")) return true;
        }
        return false;
    }

    private void removeAttachment(Player player) {
        UUID uuid = player.getUniqueId();
        PermissionAttachment attachment = attachments.remove(uuid);
        applied.remove(uuid);
        if (attachment != null) {
            try { player.removeAttachment(attachment); } catch (IllegalArgumentException ignored) { }
            player.recalculatePermissions();
        }
    }

    private void removeAllAttachments() {
        for (UUID uuid : new ArrayList<>(attachments.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) removeAttachment(player);
            else {
                attachments.remove(uuid);
                applied.remove(uuid);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void registerAbelixRankEvents() {
        for (String className : RANK_EVENT_CLASSES) {
            try {
                Class<?> type = Class.forName(className);
                if (!Event.class.isAssignableFrom(type)) continue;
                Bukkit.getPluginManager().registerEvent((Class<? extends Event>) type, this, EventPriority.MONITOR,
                        new EventExecutor() {
                            @Override public void execute(Listener listener, Event event) {
                                Player player = playerFromEvent(event);
                                if (player != null) {
                                    if (plugin.getNetworkProfileCache() != null) plugin.getNetworkProfileCache().invalidate(player);
                                    refresh(player, false);
                                } else runSync(() -> {
                                    if (plugin.getNetworkProfileCache() != null) plugin.getNetworkProfileCache().clear();
                                    refreshOnlinePlayers();
                                });
                            }
                        }, plugin, true);
                plugin.getLogger().info("Abelix rank-change refresh enabled using " + className + ".");
            } catch (Throwable ignored) { }
        }
    }

    private Player playerFromEvent(Object event) {
        Object value = invoke(event, "getPlayer", "getBukkitPlayer");
        if (value instanceof Player) return (Player) value;
        Object gamer = invoke(event, "getGamer", "getProfile", "getPlayerData");
        value = invoke(gamer, "getPlayer", "getBukkitPlayer");
        if (value instanceof Player) return (Player) value;
        value = invoke(event, "getUuid", "getUniqueId");
        if (value instanceof UUID) return Bukkit.getPlayer((UUID) value);
        value = invoke(gamer, "getUuid", "getUniqueId");
        return value instanceof UUID ? Bukkit.getPlayer((UUID) value) : null;
    }

    private Object invoke(Object target, String... names) {
        if (target == null) return null;
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) task.run();
        else Bukkit.getScheduler().runTask(plugin, task);
    }

    private String display(String value) { return value == null || value.trim().isEmpty() ? "(missing)" : value; }

    private String join(Collection<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append(", ");
            out.append(value);
        }
        return out.toString();
    }

    private static final class RankDefinition {
        private final String name;
        private final String category;
        private final List<String> permissions;
        private RankDefinition(String name, String category, List<String> permissions) {
            this.name = name;
            this.category = category;
            this.permissions = permissions == null ? Collections.<String>emptyList() : new ArrayList<>(permissions);
        }
    }

    private static final class Detection {
        private final String rawRank;
        private final String normalizedRank;
        private final String method;
        private Detection(String rawRank, String normalizedRank, String method) {
            this.rawRank = rawRank;
            this.normalizedRank = normalizedRank;
            this.method = method;
        }
    }

    private static final class AppliedRank {
        private final String rank;
        private String rawRank;
        private String normalizedRank;
        private String method;
        private final Set<String> permissions;
        private final boolean wildcard;
        private AppliedRank(String rank, String rawRank, String normalizedRank, String method,
                            Set<String> permissions, boolean wildcard) {
            this.rank = rank;
            this.rawRank = rawRank;
            this.normalizedRank = normalizedRank;
            this.method = method;
            this.permissions = new LinkedHashSet<>(permissions);
            this.wildcard = wildcard;
        }
    }
}
