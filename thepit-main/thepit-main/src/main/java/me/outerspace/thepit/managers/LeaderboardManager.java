package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.events.PitLevelChangeEvent;
import me.outerspace.thepit.model.NetworkProfileSnapshot;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.model.StatType;
import me.outerspace.thepit.registry.definition.ArenaDefinition;
import me.outerspace.thepit.registry.definition.StoredLocation;
import me.outerspace.thepit.setup.PitSetupSnapshot;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.LocUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class LeaderboardManager implements Listener {
    private final java.util.Set<java.util.UUID> pendingLeaderboardRefreshes = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final ThePitPlugin plugin;
    private final Map<String, BoardData> boards = new LinkedHashMap<>();
    private final Map<String, StoredLocation> activeLocations = new LinkedHashMap<>();
    private final Map<String, StoredLocation> featureLocations = new LinkedHashMap<>();
    private final List<UUID> packetBoards = new ArrayList<>();
    private final Map<UUID, ViewerSession> viewerSessions = new LinkedHashMap<>();
    private final Map<UUID, ViewerSession> featureSessions = new LinkedHashMap<>();
    private final AtomicBoolean activeRefreshRunning = new AtomicBoolean();
    private final ActiveLeaderboardPacketTransport packetTransport;
    private volatile List<StatsManager.ActivePlayerEntry> activeCache = Collections.emptyList();
    private int visibilityTask = -1;
    private int refreshTask = -1;
    private long lastSuccessfulRefresh;
    private long lastRefreshAttempt;
    private long lastQueryErrorLog;
    private long lastPacketErrorLog;
    private volatile long nextDirtyRefresh;
    private volatile boolean activeDirty = true;

    public LeaderboardManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        ActiveLeaderboardPacketTransport created;
        try {
            created = new ActiveLeaderboardPacketTransport();
        } catch (ReflectiveOperationException exception) {
            created = null;
            plugin.getLogger().warning("Active players packet holograms are unavailable: " + exception.getMessage());
        }
        packetTransport = created;
    }

    public void start() {
        load();
        startTasks();
        refreshActivePlayers(true);
    }

    public void reload() {
        stopTasks();
        despawnAll();
        load();
        startTasks();
        spawnAll();
        refreshActivePlayers(true);
    }

    private void startTasks() {
        stopTasks();
        long visibilityTicks = Math.max(10L, configLong("visibility-update-ticks", 20L));
        long refreshTicks = Math.max(20L, configLong("refresh-seconds", 300L) * 20L);
        visibilityTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateVisibility, 1L, visibilityTicks).getTaskId();
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> refreshActivePlayers(false), refreshTicks, refreshTicks).getTaskId();
    }

    private void stopTasks() {
        if (visibilityTask != -1) Bukkit.getScheduler().cancelTask(visibilityTask);
        if (refreshTask != -1) Bukkit.getScheduler().cancelTask(refreshTask);
        visibilityTask = -1;
        refreshTask = -1;
    }

    public void load() {
        boards.clear();
        for (PitSetupSnapshot.Leaderboard value : plugin.getSetupService().snapshot().leaderboards) {
            boards.put(value.stableId, new BoardData(serial(value.location), value.statType, value.period, value.arenaId));
        }
        activeLocations.clear();
        activeLocations.putAll(plugin.getYamlManager().activePlayersLeaderboards());
        featureLocations.clear();
        featureLocations.putAll(plugin.getYamlManager().unlockedFeaturesLeaderboards());
    }

    public boolean create(Location loc, StatType type, Period period) {
        if (loc == null || loc.getWorld() == null) return false;
        String arena = plugin.getArenaManager().getCurrentArena();
        if (arena == null || !plugin.getArenaManager().getArena(arena).map(value -> value.worldName().equals(loc.getWorld().getName())).orElse(false)) return false;
        String id = UUID.randomUUID().toString();
        Location base = loc.clone();
        base.setYaw(0f);
        base.setPitch(0f);
        BoardData board = new BoardData(LocUtil.toString(base), type.name(), period.name(), arena);
        if (!persist(id, board)) return false;
        plugin.getSetupService().reloadFromDatabase();
        boards.put(id, board);
        spawnAll();
        Bukkit.getScheduler().runTaskLater(plugin, this::spawnAll, 10L);
        return true;
    }

    public boolean createActivePlayers(Location location, String requestedArena) {
        if (location == null || location.getWorld() == null) return false;
        String arena = requestedArena == null || requestedArena.trim().isEmpty()
                ? plugin.getArenaManager().getCurrentArena() : ArenaManager.normalize(requestedArena);
        ArenaDefinition definition = arena == null ? null : plugin.getArenaManager().getArena(arena).orElse(null);
        if (definition == null || !definition.worldName().equals(location.getWorld().getName())) return false;
        if (!plugin.getYamlManager().saveActivePlayersLeaderboard(arena, location)) return false;
        load();
        destroyAllViewerSessions();
        refreshActivePlayers(true);
        updateVisibility();
        return true;
    }

    public boolean createUnlockedFeatures(Location location, String requestedArena) {
        if (location == null || location.getWorld() == null) return false;
        String arena = requestedArena == null || requestedArena.trim().isEmpty()
                ? plugin.getArenaManager().getCurrentArena() : ArenaManager.normalize(requestedArena);
        ArenaDefinition definition = arena == null ? null : plugin.getArenaManager().getArena(arena).orElse(null);
        if (definition == null || !definition.worldName().equals(location.getWorld().getName())) return false;
        if (!plugin.getYamlManager().saveUnlockedFeaturesLeaderboard(arena, location)) return false;
        load();
        destroyAllFeatureSessions();
        updateVisibility();
        return true;
    }

    public boolean removeNearest(Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return false;
        String nearestFeature = nearestDisplay(loc, radius, featureLocations);
        String nearestActiveCandidate = nearestDisplay(loc, radius, activeLocations);
        String nearestOld = nearestBoard(loc, radius);
        String nearestActive = nearestActiveCandidate;
        double activeDistance = displayDistance(loc, nearestActive, activeLocations);
        double featureDistance = displayDistance(loc, nearestFeature, featureLocations);
        double oldDistance = Double.MAX_VALUE;
        if (nearestOld != null) {
            Location old = LocUtil.fromString(boards.get(nearestOld).location);
            if (old != null) oldDistance = old.distanceSquared(loc);
        }
        if (nearestFeature != null && featureDistance <= activeDistance && featureDistance <= oldDistance) {
            if (!plugin.getYamlManager().removeUnlockedFeaturesLeaderboard(nearestFeature)) return false;
            featureLocations.remove(nearestFeature);
            destroyAllFeatureSessions();
            return true;
        }
        if (nearestActive != null && activeDistance <= oldDistance) {
            if (!plugin.getYamlManager().removeActivePlayersLeaderboard(nearestActive)) return false;
            activeLocations.remove(nearestActive);
            destroyAllViewerSessions();
            return true;
        }
        if (nearestOld == null || !plugin.getSetupService().repository().deleteLeaderboard(nearestOld)) return false;
        plugin.getSetupService().reloadFromDatabase();
        boards.remove(nearestOld);
        spawnAll();
        return true;
    }

    public boolean removeDisplay(Location location, String displayId, double radius) {
        if (location == null || displayId == null) return false;
        String normalized = displayId.toLowerCase(Locale.ENGLISH).replace("-", "").replace("_", "");
        if (normalized.equals("unlockedfeatures")) {
            String arena = nearestDisplay(location, radius, featureLocations);
            if (arena == null || !plugin.getYamlManager().removeUnlockedFeaturesLeaderboard(arena)) return false;
            featureLocations.remove(arena);
            destroyAllFeatureSessions();
            return true;
        }
        if (normalized.equals("activeplayers")) {
            String arena = nearestDisplay(location, radius, activeLocations);
            if (arena == null || !plugin.getYamlManager().removeActivePlayersLeaderboard(arena)) return false;
            activeLocations.remove(arena);
            destroyAllViewerSessions();
            return true;
        }
        return false;
    }

    public void spawnAll() {
        despawnAll();
        for (String id : boards.keySet()) spawnBoard(id);
        updateVisibility();
    }

    private void spawnBoard(String id) {
        BoardData board = boards.get(id);
        if (board == null) return;
        if (board.arenaId != null && !board.arenaId.equalsIgnoreCase(plugin.getArenaManager().getCurrentArena())) return;
        Location base = LocUtil.fromString(board.location);
        StatType type = StatType.from(board.type);
        Period period = Period.from(board.period);
        if (base == null || type == null || period == null || base.getWorld() == null) return;
        Period displayPeriod = type == StatType.KILLS ? period : Period.LIFETIME;
        base.add(0.5, 0.0, 0.5);
        List<String> lines = new ArrayList<>();
        String typeKey = type.name().toLowerCase(Locale.ENGLISH);
        String typeName = plugin.registry().scoreboard().getString("leaderboards.names." + typeKey, readable(typeKey));
        lines.add(plugin.registry().scoreboard().getString("leaderboards.title", "&b&lThe Pit Top %type%").replace("%type%", typeName));
        lines.add(plugin.registry().scoreboard().getString("leaderboards.period-line", "&7%period% &8(click)").replace("%period%", displayPeriod.name().toLowerCase()));
        List<String> top = plugin.getStatsManager().top(type, displayPeriod, 10);
        String empty = plugin.registry().scoreboard().getString("leaderboards.empty-line", "&7No data");
        for (int i = 0; i < 10; i++) {
            String value = i < top.size() ? top.get(i) : empty;
            lines.add(plugin.registry().scoreboard().getString("leaderboards.entry-line", "%entry%")
                    .replace("%position%", String.valueOf(i + 1))
                    .replace("%entry%", value));
        }
        UUID hologram = plugin.getNpcService().createTransientHologram(base.clone().add(0, 2.5, 0), lines);
        if (hologram != null) packetBoards.add(hologram);
    }

    public void refreshActivePlayers(boolean force) {
        if (!activeEnabled() || activeLocations.isEmpty()) return;
        long now = System.currentTimeMillis();
        long minimum = Math.max(1L, configLong("minimum-refresh-seconds", 30L)) * 1000L;
        if (!force && !activeDirty && now - lastSuccessfulRefresh < configLong("refresh-seconds", 300L) * 1000L) return;
        if (!force && activeDirty && now < nextDirtyRefresh) return;
        if (!force && now - lastRefreshAttempt < minimum) return;
        if (!activeRefreshRunning.compareAndSet(false, true)) return;
        lastRefreshAttempt = now;
        int days = Math.max(1, configInt("active-days", 7));
        int maximum = Math.max(1, configInt("maximum-entries", 10));
        long span;
        try {
            span = Math.multiplyExact(days, 86400000L);
        } catch (ArithmeticException ignored) {
            span = Long.MAX_VALUE;
        }
        long cutoff = Math.max(0L, now - span);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<StatsManager.ActivePlayerEntry> result = plugin.getStatsManager().queryActivePlayers(cutoff, maximum);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    activeCache = Collections.unmodifiableList(new ArrayList<>(result));
                    activeDirty = false;
                    lastSuccessfulRefresh = System.currentTimeMillis();
                    nextDirtyRefresh = lastSuccessfulRefresh + Math.max(1L, configLong("minimum-refresh-seconds", 30L)) * 1000L;
                    activeRefreshRunning.set(false);
                    updateVisibility();
                });
            } catch (Throwable exception) {
                long failureTime = System.currentTimeMillis();
                if (failureTime - lastQueryErrorLog >= 300000L) {
                    lastQueryErrorLog = failureTime;
                    plugin.getLogger().warning("Active players leaderboard refresh failed; keeping the previous result: " + exception.getMessage());
                }
                nextDirtyRefresh = failureTime + Math.max(1L, configLong("refresh-seconds", 300L)) * 1000L;
                activeRefreshRunning.set(false);
            }
        });
    }


    public void refreshLeaderboardsForPlayer(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) return;
        String activeArena = plugin.getArenaManager().getCurrentArena();
        StoredLocation stored = activeArena == null ? null : activeLocations.get(ArenaManager.normalize(activeArena));
        Location anchor = stored == null ? null : stored.resolve();
        double viewDistance = Math.max(1.0D, configDouble("view-distance", 48.0D));
        if (packetTransport != null && activeEnabled() && anchor != null && anchor.getWorld() != null
                && player.getWorld().equals(anchor.getWorld()) && player.getLocation().distanceSquared(anchor) <= viewDistance * viewDistance) {
            destroyViewerSession(player);
            renderViewer(player, ArenaManager.normalize(activeArena));
        }
        StoredLocation featureStored = activeArena == null ? null : featureLocations.get(ArenaManager.normalize(activeArena));
        Location featureAnchor = featureStored == null ? null : featureStored.resolve();
        double featureDistance = Math.max(1.0D, featureConfigDouble("view-distance", 48.0D));
        if (packetTransport != null && featureConfigBoolean("enabled", true) && featureAnchor != null && featureAnchor.getWorld() != null
                && player.getWorld().equals(featureAnchor.getWorld()) && player.getLocation().distanceSquared(featureAnchor) <= featureDistance * featureDistance) {
            destroyFeatureSession(player);
            renderFeatureViewer(player, ArenaManager.normalize(activeArena));
        }
    }

    public void queueLeaderboardRefresh(Player player) {
        if (player == null || !pendingLeaderboardRefreshes.add(player.getUniqueId())) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingLeaderboardRefreshes.remove(player.getUniqueId());
            refreshLeaderboardsForPlayer(player);
        }, 2L);
    }
    public void markActiveLeaderboardDirty() {
        activeDirty = true;
    }

    public void refreshViewerXp(Player player) {
        if (player == null || !player.isOnline()) return;
        Runnable update = () -> {
            ViewerSession session = viewerSessions.get(player.getUniqueId());
            if (session != null) renderViewer(player, session.arenaId);
        };
        if (Bukkit.isPrimaryThread()) update.run();
        else Bukkit.getScheduler().runTask(plugin, update);
    }

    private void updateVisibility() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::updateVisibility);
            return;
        }
        String activeArena = plugin.getArenaManager().getCurrentArena();
        StoredLocation stored = activeArena == null ? null : activeLocations.get(ArenaManager.normalize(activeArena));
        Location anchor = stored == null ? null : stored.resolve();
        double viewDistance = Math.max(1.0D, configDouble("view-distance", 48.0D));
        double maximumDistance = viewDistance * viewDistance;
        StoredLocation featureStored = activeArena == null ? null : featureLocations.get(ArenaManager.normalize(activeArena));
        Location featureAnchor = featureStored == null ? null : featureStored.resolve();
        double featureDistance = Math.max(1.0D, featureConfigDouble("view-distance", 48.0D));
        double maximumFeatureDistance = featureDistance * featureDistance;
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean visible = packetTransport != null && activeEnabled() && anchor != null && anchor.getWorld() != null
                    && player.getWorld().equals(anchor.getWorld()) && player.getLocation().distanceSquared(anchor) <= maximumDistance;
            if (visible) renderViewer(player, ArenaManager.normalize(activeArena));
            else destroyViewerSession(player);
            boolean featuresVisible = packetTransport != null && featureConfigBoolean("enabled", true)
                    && featureAnchor != null && featureAnchor.getWorld() != null
                    && player.getWorld().equals(featureAnchor.getWorld())
                    && player.getLocation().distanceSquared(featureAnchor) <= maximumFeatureDistance;
            if (featuresVisible) renderFeatureViewer(player, ArenaManager.normalize(activeArena));
            else destroyFeatureSession(player);
        }
        for (UUID uuid : new ArrayList<>(viewerSessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) destroyViewerSession(player, uuid);
        }
        for (UUID uuid : new ArrayList<>(featureSessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) destroyFeatureSession(player, uuid);
        }
        if (activeDirty && System.currentTimeMillis() >= nextDirtyRefresh) refreshActivePlayers(false);
    }

    public void refreshFeatureViewer(Player player) {
        if (player == null || !player.isOnline()) return;
        Runnable update = () -> refreshFeatureViewerNow(player);
        if (Bukkit.isPrimaryThread()) update.run();
        else Bukkit.getScheduler().runTask(plugin, update);
    }

    private void refreshFeatureViewerNow(Player player) {
        String activeArena = plugin.getArenaManager().getCurrentArena();
        String arenaId = activeArena == null ? null : ArenaManager.normalize(activeArena);
        StoredLocation stored = arenaId == null ? null : featureLocations.get(arenaId);
        Location anchor = stored == null ? null : stored.resolve();
        double distance = Math.max(1.0D, featureConfigDouble("view-distance", 48.0D));
        boolean visible = packetTransport != null && featureConfigBoolean("enabled", true)
                && anchor != null && anchor.getWorld() != null && player.getWorld().equals(anchor.getWorld())
                && player.getLocation().distanceSquared(anchor) <= distance * distance;
        if (visible) renderFeatureViewer(player, arenaId);
        else destroyFeatureSession(player);
    }

    private void renderFeatureViewer(Player player, String arenaId) {
        StoredLocation stored = featureLocations.get(arenaId);
        Location anchor = stored == null ? null : stored.resolve();
        if (anchor == null || anchor.getWorld() == null || packetTransport == null) {
            destroyFeatureSession(player);
            return;
        }
        List<DesiredLine> desired = featureLines(player, anchor);
        ViewerSession session = featureSessions.computeIfAbsent(player.getUniqueId(), ignored -> new ViewerSession(arenaId));
        if (!session.arenaId.equals(arenaId)) {
            destroyFeatureSession(player);
            session = new ViewerSession(arenaId);
            featureSessions.put(player.getUniqueId(), session);
        }
        reconcile(player, session, desired);
    }

    private List<DesiredLine> featureLines(Player viewer, Location anchor) {
        List<DesiredLine> lines = new ArrayList<>();
        double spacing = Math.max(0.05D, featureConfigDouble("line-spacing", 0.28D));
        double y = anchor.getY();
        addDesired(lines, anchor, y, featureConfigString("title", "&e&lUNLOCKED FEATURES"));
        y -= spacing * 2.0D;
        List<FeatureDefinition> definitions = hologramFeatureDefinitions();
        boolean loaded = plugin.getStatsManager().isProfileLoaded(viewer.getUniqueId());
        int viewerLevel = loaded ? plugin.getStatsManager().getLevel(viewer.getUniqueId()) : 0;
        boolean allUnlocked = !definitions.isEmpty();
        for (FeatureDefinition feature : definitions) {
            boolean unlocked = loaded && viewerLevel >= feature.level;
            allUnlocked &= unlocked;
            String line = unlocked ? "&7[" + thresholdColor(feature.level) + feature.level + "&7] " + feature.display
                    : featureConfigString("locked-line", "&b???");
            addDesired(lines, anchor, y, line);
            y -= spacing;
        }
        boolean hideFinal = featureConfigBoolean("hide-question-mark-at-all-unlocked", false) && allUnlocked;
        if (!hideFinal) {
            y -= spacing;
            List<String> finalLines = plugin.registry().settings().getStringList("unlocked-features.final-lines");
            if (finalLines.isEmpty()) finalLines = Arrays.asList("&7???", "&7Gain levels to unlock more");
            for (String value : finalLines) {
                addDesired(lines, anchor, y, value);
                y -= spacing;
            }
        }
        return lines;
    }

    private List<FeatureDefinition> hologramFeatureDefinitions() {
        List<FeatureDefinition> values = new ArrayList<>();
        for (String id : Arrays.asList("respawn", "pitchat", "upgrades", "enderchest", "contracts", "stats", "trade", "view")) {
            String base = "unlocked-features.features." + id;
            int level = plugin.registry().settings().getInt(base + ".level", defaultFeatureLevel(id));
            String display = plugin.registry().settings().getString(base + ".display", defaultFeatureDisplay(id));
            values.add(new FeatureDefinition(id, Math.max(1, level), display, "", UnlockType.CURRENT_LEVEL));
        }
        return values;
    }

    private void reconcile(Player player, ViewerSession session, List<DesiredLine> desired) {
        int common = Math.min(session.lines.size(), desired.size());
        for (int index = 0; index < common; index++) {
            ActiveLeaderboardPacketTransport.Line current = session.lines.get(index);
            DesiredLine next = desired.get(index);
            if (!sameLocation(current.location(), next.location)) {
                destroyLines(player, Collections.singletonList(current));
                ActiveLeaderboardPacketTransport.Line replacement = createAndSpawn(player, next);
                if (replacement != null) session.lines.set(index, replacement);
            } else if (!current.text().equals(next.text)) {
                try {
                    packetTransport.update(player, current, next.text);
                } catch (ReflectiveOperationException exception) {
                    logPacketFailure(exception);
                }
            }
        }
        if (session.lines.size() > desired.size()) {
            List<ActiveLeaderboardPacketTransport.Line> removed = new ArrayList<>(session.lines.subList(desired.size(), session.lines.size()));
            destroyLines(player, removed);
            session.lines.subList(desired.size(), session.lines.size()).clear();
        }
        for (int index = session.lines.size(); index < desired.size(); index++) {
            ActiveLeaderboardPacketTransport.Line line = createAndSpawn(player, desired.get(index));
            if (line != null) session.lines.add(line);
        }
    }

    private void renderViewer(Player player, String arenaId) {
        StoredLocation stored = activeLocations.get(arenaId);
        Location anchor = stored == null ? null : stored.resolve();
        if (anchor == null || anchor.getWorld() == null || packetTransport == null) {
            destroyViewerSession(player);
            return;
        }
        List<DesiredLine> desired = desiredLines(player, anchor);
        ViewerSession session = viewerSessions.computeIfAbsent(player.getUniqueId(), ignored -> new ViewerSession(arenaId));
        if (!session.arenaId.equals(arenaId)) {
            destroyViewerSession(player);
            session = new ViewerSession(arenaId);
            viewerSessions.put(player.getUniqueId(), session);
        }
        int common = Math.min(session.lines.size(), desired.size());
        for (int index = 0; index < common; index++) {
            ActiveLeaderboardPacketTransport.Line current = session.lines.get(index);
            DesiredLine next = desired.get(index);
            if (!sameLocation(current.location(), next.location)) {
                destroyLines(player, Collections.singletonList(current));
                ActiveLeaderboardPacketTransport.Line replacement = createAndSpawn(player, next);
                if (replacement != null) session.lines.set(index, replacement);
            } else if (!current.text().equals(next.text)) {
                try {
                    packetTransport.update(player, current, next.text);
                } catch (ReflectiveOperationException exception) {
                    logPacketFailure(exception);
                }
            }
        }
        if (session.lines.size() > desired.size()) {
            List<ActiveLeaderboardPacketTransport.Line> removed = new ArrayList<>(session.lines.subList(desired.size(), session.lines.size()));
            destroyLines(player, removed);
            session.lines.subList(desired.size(), session.lines.size()).clear();
        }
        for (int index = session.lines.size(); index < desired.size(); index++) {
            ActiveLeaderboardPacketTransport.Line line = createAndSpawn(player, desired.get(index));
            if (line != null) session.lines.add(line);
        }
    }

    private ActiveLeaderboardPacketTransport.Line createAndSpawn(Player player, DesiredLine desired) {
        try {
            ActiveLeaderboardPacketTransport.Line line = packetTransport.create(desired.location, desired.text);
            packetTransport.spawn(player, line);
            return line;
        } catch (ReflectiveOperationException exception) {
            logPacketFailure(exception);
            return null;
        }
    }

    private List<DesiredLine> desiredLines(Player viewer, Location anchor) {
        List<DesiredLine> lines = new ArrayList<>();
        double spacing = Math.max(0.05D, configDouble("line-spacing", 0.27D));
        double titleGap = Math.max(spacing, configDouble("title-entry-gap", 0.32D));
        double y = anchor.getY();
        addDesired(lines, anchor, y, configString("title", "&b&lTOP ACTIVE PLAYERS"));
        y -= spacing;
        addDesired(lines, anchor, y, configString("subtitle", "&7Pit Level"));
        y -= titleGap;
        List<StatsManager.ActivePlayerEntry> entries = activeCache;
        if (entries.isEmpty()) {
            addDesired(lines, anchor, y, configString("empty-line", "&7No active players"));
            y -= spacing;
        } else {
            String format = configString("entry-format", "%position_color%%position%. %level_display% %player_display% &7- &b%lifetime_xp_short% XP");
            int maximum = Math.min(Math.max(1, configInt("maximum-entries", 10)), entries.size());
            for (int index = 0; index < maximum; index++) {
                StatsManager.ActivePlayerEntry entry = entries.get(index);
                int position = index + 1;
                String line = format
                        .replace("%position_color%", positionColor(position))
                        .replace("%position%", String.valueOf(position))
                        .replace("%level_display%", plugin.getStatsManager().getFormattedLevel(entry.level(), entry.prestige()))
                        .replace("%player_display%", playerDisplay(entry))
                        .replace("%lifetime_xp_short%", formatShort(entry.lifetimeXp()))
                        .replace("%lifetime_xp%", formatWhole(entry.lifetimeXp()));
                addDesired(lines, anchor, y, line);
                y -= spacing;
            }
        }
        y -= spacing;
        String viewerXp = plugin.getStatsManager().isProfileLoaded(viewer.getUniqueId())
                ? formatShort(plugin.getStatsManager().getLifetimeXp(viewer.getUniqueId())) : "Loading...";
        String viewerLine = configString("viewer-line", "&7You have: &b%viewer_lifetime_xp_short% XP")
                .replace("%viewer_lifetime_xp_short%", viewerXp)
                .replace("%viewer_lifetime_xp%", plugin.getStatsManager().isProfileLoaded(viewer.getUniqueId())
                        ? formatWhole(plugin.getStatsManager().getLifetimeXp(viewer.getUniqueId())) : "Loading...");
        addDesired(lines, anchor, y, viewerLine);
        y -= spacing * 2.0D;
        List<String> footer = plugin.registry().scoreboard().getStringList("leaderboards.activeplayers.footer");
        if (footer.isEmpty()) footer = Arrays.asList("&7All-time &ebest &7players!", "&7&oPlayers who logged in this week.");
        for (String value : footer) {
            addDesired(lines, anchor, y, value);
            y -= spacing;
        }
        return lines;
    }

    private void addDesired(List<DesiredLine> lines, Location anchor, double y, String text) {
        if (text == null || text.isEmpty()) return;
        Location location = new Location(anchor.getWorld(), anchor.getX(), y, anchor.getZ(), anchor.getYaw(), anchor.getPitch());
        lines.add(new DesiredLine(location, CC.color(text)));
    }

    private String playerDisplay(StatsManager.ActivePlayerEntry entry) {
        String name = entry.name();
        if (name == null || name.trim().isEmpty()) name = entry.uuid().toString().substring(0, 8);
        String prefix = "";
        NetworkProfileSnapshot cached = plugin.getNetworkProfileCache() == null ? null : plugin.getNetworkProfileCache().cached(entry.uuid());
        if (cached != null) {
            prefix = cached.getPrefix();
            if (prefix == null || prefix.isEmpty()) prefix = cached.getPermissionPrefix();
        }
        if ((prefix == null || prefix.isEmpty()) && entry.prefix() != null && !entry.prefix().contains("%")) prefix = entry.prefix();
        return prefix == null || prefix.isEmpty() ? CC.color("&b" + name) : CC.color(prefix + name);
    }

    private String positionColor(int position) {
        return plugin.registry().scoreboard().getString("leaderboards.activeplayers.position-colors." + position,
                plugin.registry().scoreboard().getString("leaderboards.activeplayers.position-colors.default", "&e"));
    }

    public List<FeatureDefinition> featureDefinitions() {
        List<FeatureDefinition> values = new ArrayList<>();
        List<String> ids = new ArrayList<>(plugin.registry().settings().keys("unlocked-features.features"));
        if (ids.isEmpty()) ids.addAll(Arrays.asList("respawn", "pitchat", "upgrades", "enderchest", "contracts", "stats", "trade", "view"));
        for (String id : ids) {
            String base = "unlocked-features.features." + id;
            int level = plugin.registry().settings().getInt(base + ".level", defaultFeatureLevel(id));
            String display = plugin.registry().settings().getString(base + ".display", defaultFeatureDisplay(id));
            String description = plugin.registry().settings().getString(base + ".description", "");
            String unlockType = plugin.registry().settings().getString(base + ".unlock-type", defaultUnlockType(id));
            boolean enabled = plugin.registry().settings().getBoolean(base + ".enabled", true);
            if (!enabled || !featureSystemAvailable(id)) continue;
            values.add(new FeatureDefinition(id.toLowerCase(Locale.ENGLISH), Math.max(1, level), display, description,
                    unlockType.equalsIgnoreCase("PERMANENT") ? UnlockType.PERMANENT : UnlockType.CURRENT_LEVEL));
        }
        values.sort(Comparator.comparingInt(FeatureDefinition::level));
        return Collections.unmodifiableList(values);
    }

    public FeatureDefinition feature(String id) {
        if (id == null) return null;
        for (FeatureDefinition feature : featureDefinitions()) if (feature.id.equalsIgnoreCase(id)) return feature;
        return null;
    }

    public boolean isFeatureUnlocked(UUID uuid, String id) {
        FeatureDefinition feature = feature(id);
        return feature != null && isFeatureUnlocked(uuid, feature);
    }

    private boolean isFeatureUnlocked(UUID uuid, FeatureDefinition feature) {
        if (uuid == null || feature == null || !plugin.getStatsManager().isProfileLoaded(uuid)) return false;
        if (feature.unlockType == UnlockType.PERMANENT) {
            if (feature.id.equals("stats") && plugin.getStatsManager().isStatsMenuUnlocked(uuid)) return true;
            if (plugin.getStatsManager().getLongStat(uuid, permanentFeatureKey(feature.id)) > 0L) return true;
        }
        return plugin.getStatsManager().getLevel(uuid) >= feature.level;
    }

    public List<FeatureDefinition> unlockPermanentFeatures(Player player, int oldLevel, int newLevel) {
        if (player == null || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) return Collections.emptyList();
        List<FeatureDefinition> unlocked = new ArrayList<>();
        boolean changed = false;
        for (FeatureDefinition feature : featureDefinitions()) {
            if (feature.unlockType != UnlockType.PERMANENT || newLevel < feature.level) continue;
            String key = permanentFeatureKey(feature.id);
            boolean already = feature.id.equals("stats") ? plugin.getStatsManager().isStatsMenuUnlocked(player.getUniqueId())
                    : plugin.getStatsManager().getLongStat(player.getUniqueId(), key) > 0L;
            if (!already && !feature.id.equals("stats")) {
                plugin.getStatsManager().setLongStat(player.getUniqueId(), key, 1L);
                changed = true;
            }
            if (oldLevel < feature.level && newLevel >= feature.level) unlocked.add(feature);
        }
        if (changed) plugin.getStatsManager().flushPlayer(player.getUniqueId(), false);
        refreshFeatureViewer(player);
        return unlocked;
    }

    public void synchronizePermanentFeatures(Player player) {
        if (player == null || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) return;
        int level = plugin.getStatsManager().getLevel(player.getUniqueId());
        if (plugin.getStatsManager().getPrestige(player.getUniqueId()) > 0) level = 120;
        unlockPermanentFeatures(player, level, level);
    }

    public List<FeatureDefinition> crossedFeatures(int oldLevel, int newLevel) {
        if (newLevel <= oldLevel) return Collections.emptyList();
        List<FeatureDefinition> values = new ArrayList<>();
        for (FeatureDefinition feature : featureDefinitions()) {
            if (oldLevel < feature.level && newLevel >= feature.level) values.add(feature);
        }
        return values;
    }

    private boolean featureSystemAvailable(String id) {
        if (!id.equalsIgnoreCase("trade")) return true;
        return plugin.getPitCommandManager() != null;
    }

    private String permanentFeatureKey(String id) {
        return "feature-unlocked-" + id.toLowerCase(Locale.ENGLISH);
    }

    private int defaultFeatureLevel(String id) {
        if (id.equalsIgnoreCase("pitchat")) return 5;
        if (id.equalsIgnoreCase("upgrades")) return 10;
        if (id.equalsIgnoreCase("enderchest")) return 15;
        if (id.equalsIgnoreCase("contracts")) return 30;
        if (id.equalsIgnoreCase("stats")) return 50;
        if (id.equalsIgnoreCase("trade")) return 60;
        if (id.equalsIgnoreCase("view")) return 70;
        return 1;
    }

    private String defaultFeatureDisplay(String id) {
        if (id.equalsIgnoreCase("respawn")) return "&b/respawn";
        if (id.equalsIgnoreCase("pitchat")) return "&b/pitchat";
        if (id.equalsIgnoreCase("upgrades")) return "&bUpgrades";
        if (id.equalsIgnoreCase("enderchest")) return "&bEnder Chest";
        if (id.equalsIgnoreCase("contracts")) return "&bContracts";
        if (id.equalsIgnoreCase("stats")) return "&bStats";
        if (id.equalsIgnoreCase("trade")) return "&b/trade";
        if (id.equalsIgnoreCase("view")) return "&b/view";
        return "&b" + readable(id);
    }

    private String defaultUnlockType(String id) {
        return id.equalsIgnoreCase("enderchest") || id.equalsIgnoreCase("stats") || id.equalsIgnoreCase("trade")
                || id.equalsIgnoreCase("view") ? "PERMANENT" : "CURRENT_LEVEL";
    }

    private String thresholdColor(int level) {
        for (String range : plugin.registry().settings().keys("unlocked-features.threshold-colors")) {
            String[] parts = range.split("-");
            try {
                int minimum = Integer.parseInt(parts[0].replace("+", ""));
                int maximum = parts.length > 1 ? Integer.parseInt(parts[1]) : range.endsWith("+") ? Integer.MAX_VALUE : minimum;
                if (level >= minimum && level <= maximum) return plugin.registry().settings().getString("unlocked-features.threshold-colors." + range, "&7");
            } catch (NumberFormatException ignored) { }
        }
        if (level >= 70) return "&c";
        if (level >= 60) return "&6";
        if (level >= 40) return "&e";
        if (level >= 20) return "&2";
        if (level >= 10) return "&1";
        return "&7";
    }

    public String formatShort(long raw) {
        long value = Math.max(0L, raw);
        long divisor;
        String suffix;
        if (value >= 1000000000000L) {
            divisor = 1000000000000L;
            suffix = "T";
        } else if (value >= 1000000000L) {
            divisor = 1000000000L;
            suffix = "B";
        } else if (value >= 1000000L) {
            divisor = 1000000L;
            suffix = "M";
        } else if (value >= 1000L) {
            divisor = 1000L;
            suffix = "k";
        } else return Long.toString(value);
        BigDecimal compact = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP).stripTrailingZeros();
        return compact.toPlainString() + suffix;
    }

    private String formatWhole(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, value));
    }

    private boolean sameLocation(Location first, Location second) {
        return first.getWorld() != null && second.getWorld() != null && first.getWorld().equals(second.getWorld())
                && Math.abs(first.getX() - second.getX()) < 0.0001D
                && Math.abs(first.getY() - second.getY()) < 0.0001D
                && Math.abs(first.getZ() - second.getZ()) < 0.0001D;
    }

    private void destroyViewerSession(Player player) {
        if (player != null) destroyViewerSession(player, player.getUniqueId());
    }

    private void destroyViewerSession(Player player, UUID uuid) {
        ViewerSession session = viewerSessions.remove(uuid);
        if (session == null || player == null || !player.isOnline()) return;
        destroyLines(player, session.lines);
    }

    private void destroyAllViewerSessions() {
        for (Map.Entry<UUID, ViewerSession> entry : new ArrayList<>(viewerSessions.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) destroyLines(player, entry.getValue().lines);
        }
        viewerSessions.clear();
    }

    private void destroyFeatureSession(Player player) {
        if (player != null) destroyFeatureSession(player, player.getUniqueId());
    }

    private void destroyFeatureSession(Player player, UUID uuid) {
        ViewerSession session = featureSessions.remove(uuid);
        if (session == null || player == null || !player.isOnline()) return;
        destroyLines(player, session.lines);
    }

    private void destroyAllFeatureSessions() {
        for (Map.Entry<UUID, ViewerSession> entry : new ArrayList<>(featureSessions.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) destroyLines(player, entry.getValue().lines);
        }
        featureSessions.clear();
    }

    private void destroyLines(Player player, List<ActiveLeaderboardPacketTransport.Line> lines) {
        if (packetTransport == null || player == null || lines.isEmpty()) return;
        try {
            packetTransport.destroy(player, () -> lines.stream().mapToInt(ActiveLeaderboardPacketTransport.Line::entityId).toArray());
        } catch (ReflectiveOperationException exception) {
            logPacketFailure(exception);
        }
    }

    private void logPacketFailure(Throwable exception) {
        long now = System.currentTimeMillis();
        if (now - lastPacketErrorLog < 300000L) return;
        lastPacketErrorLog = now;
        plugin.getLogger().warning("Active players hologram packet failed: " + exception.getMessage());
    }

    public void list(CommandSender sender) {
        sender.sendMessage(CC.color("&bThe Pit leaderboards:"));
        if (activeLocations.isEmpty() && featureLocations.isEmpty() && boards.isEmpty()) {
            sender.sendMessage(CC.color("&7None configured."));
            return;
        }
        for (Map.Entry<String, BoardData> entry : boards.entrySet()) {
            Location location = LocUtil.fromString(entry.getValue().location);
            if (location == null || location.getWorld() == null) continue;
            sender.sendMessage(CC.color("&7" + entry.getKey() + " &8- &f" + entry.getValue().type.toLowerCase(Locale.ENGLISH)
                    + " &8- &f" + location.getWorld().getName() + " " + formatCoordinate(location.getX()) + ", "
                    + formatCoordinate(location.getY()) + ", " + formatCoordinate(location.getZ())));
        }
        for (Map.Entry<String, StoredLocation> entry : activeLocations.entrySet()) {
            StoredLocation location = entry.getValue();
            sender.sendMessage(CC.color("&7" + entry.getKey() + ":activeplayers &8- &f" + location.worldName() + " "
                    + formatCoordinate(location.x()) + ", " + formatCoordinate(location.y()) + ", " + formatCoordinate(location.z())));
        }
        for (Map.Entry<String, StoredLocation> entry : featureLocations.entrySet()) {
            StoredLocation location = entry.getValue();
            sender.sendMessage(CC.color("&7" + entry.getKey() + ":unlockedfeatures &8- &f" + location.worldName() + " "
                    + formatCoordinate(location.x()) + ", " + formatCoordinate(location.y()) + ", " + formatCoordinate(location.z())));
        }
    }

    public boolean teleport(Player player, String id) {
        if (player == null || id == null) return false;
        BoardData board = boards.get(id);
        if (board != null) {
            Location oldLocation = LocUtil.fromString(board.location);
            if(oldLocation!=null&&oldLocation.getWorld()!=null&&plugin.getCombatLogManager()!=null)plugin.getCombatLogManager().trustTeleport(player,3000L);
            return oldLocation != null && oldLocation.getWorld() != null && player.teleport(oldLocation);
        }
        String arena = ArenaManager.normalize(id.replace(":activeplayers", ""));
        boolean features = id.toLowerCase(Locale.ENGLISH).endsWith(":unlockedfeatures");
        if (features) arena = ArenaManager.normalize(id.substring(0, id.length() - ":unlockedfeatures".length()));
        StoredLocation stored = features ? featureLocations.get(arena) : activeLocations.get(arena);
        Location location = stored == null ? null : stored.resolve();
        if(location!=null&&location.getWorld()!=null&&plugin.getCombatLogManager()!=null)plugin.getCombatLogManager().trustTeleport(player,3000L);
        return location != null && location.getWorld() != null && player.teleport(location);
    }

    public void debug(CommandSender sender, Player viewer) {
        sender.sendMessage(CC.color("&bActive Players leaderboard debug"));
        sender.sendMessage(CC.color("&7Enabled: &f" + activeEnabled()));
        sender.sendMessage(CC.color("&7Configured arenas: &f" + activeLocations.size()));
        sender.sendMessage(CC.color("&7Feature displays: &f" + featureLocations.size()));
        sender.sendMessage(CC.color("&7Cached entries: &f" + activeCache.size()));
        sender.sendMessage(CC.color("&7Refresh running: &f" + activeRefreshRunning.get()));
        sender.sendMessage(CC.color("&7Cache dirty: &f" + activeDirty));
        sender.sendMessage(CC.color("&7Last success: &f" + (lastSuccessfulRefresh <= 0L ? "never" : (System.currentTimeMillis() - lastSuccessfulRefresh) / 1000L + "s ago")));
        if (viewer != null) {
            ViewerSession session = viewerSessions.get(viewer.getUniqueId());
            ViewerSession featureSession = featureSessions.get(viewer.getUniqueId());
            sender.sendMessage(CC.color("&7Viewer: &f" + viewer.getName() + " &7profile-loaded=&f" + plugin.getStatsManager().isProfileLoaded(viewer.getUniqueId())
                    + " &7active-lines=&f" + (session == null ? 0 : session.lines.size())
                    + " &7feature-lines=&f" + (featureSession == null ? 0 : featureSession.lines.size())));
        }
    }

    private String formatCoordinate(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private boolean activeEnabled() {
        return plugin.registry().scoreboard().getBoolean("leaderboards.activeplayers.enabled", true);
    }

    private String configString(String path, String fallback) {
        return plugin.registry().scoreboard().getString("leaderboards.activeplayers." + path, fallback);
    }

    private int configInt(String path, int fallback) {
        return plugin.registry().scoreboard().getInt("leaderboards.activeplayers." + path, fallback);
    }

    private long configLong(String path, long fallback) {
        return plugin.registry().scoreboard().getLong("leaderboards.activeplayers." + path, fallback);
    }

    private double configDouble(String path, double fallback) {
        return plugin.registry().scoreboard().getDouble("leaderboards.activeplayers." + path, fallback);
    }

    private String featureConfigString(String path, String fallback) {
        return plugin.registry().settings().getString("unlocked-features." + path, fallback);
    }

    private boolean featureConfigBoolean(String path, boolean fallback) {
        return plugin.registry().settings().getBoolean("unlocked-features." + path, fallback);
    }

    private double featureConfigDouble(String path, double fallback) {
        return plugin.registry().settings().getDouble("unlocked-features." + path, fallback);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        destroyViewerSession(event.getPlayer());
        destroyFeatureSession(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        destroyViewerSession(event.getPlayer());
        destroyFeatureSession(event.getPlayer());
        Bukkit.getScheduler().runTask(plugin, this::updateVisibility);
    }

    @EventHandler
    public void onLevelChange(PitLevelChangeEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUuid());
        if (player != null && player.isOnline()) refreshFeatureViewer(player);
    }

    private Period next(Period current) {
        if (current == Period.LIFETIME) return Period.MONTHLY;
        if (current == Period.MONTHLY) return Period.WEEKLY;
        if (current == Period.WEEKLY) return Period.DAILY;
        return Period.LIFETIME;
    }

    private String readable(String value) {
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ENGLISH);
    }

    private String nearestBoard(Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return null;
        String nearest = null;
        double best = radius * radius;
        for (Map.Entry<String, BoardData> entry : boards.entrySet()) {
            Location base = LocUtil.fromString(entry.getValue().location);
            if (base == null || base.getWorld() == null || !base.getWorld().equals(loc.getWorld())) continue;
            double distance = base.distanceSquared(loc);
            if (distance <= best) {
                best = distance;
                nearest = entry.getKey();
            }
        }
        return nearest;
    }

    private String nearestDisplay(Location location, double radius, Map<String, StoredLocation> locations) {
        if (location == null || location.getWorld() == null) return null;
        String nearest = null;
        double best = radius * radius;
        for (Map.Entry<String, StoredLocation> entry : locations.entrySet()) {
            Location candidate = entry.getValue().resolve();
            if (candidate == null || candidate.getWorld() == null || !candidate.getWorld().equals(location.getWorld())) continue;
            double distance = candidate.distanceSquared(location);
            if (distance <= best) {
                best = distance;
                nearest = entry.getKey();
            }
        }
        return nearest;
    }

    private double displayDistance(Location location, String arena, Map<String, StoredLocation> locations) {
        StoredLocation stored = arena == null ? null : locations.get(arena);
        Location candidate = stored == null ? null : stored.resolve();
        return candidate == null || candidate.getWorld() == null || !candidate.getWorld().equals(location.getWorld())
                ? Double.MAX_VALUE : candidate.distanceSquared(location);
    }

    public int getSavedBoardCount() {
        return boards.size() + activeLocations.size() + featureLocations.size();
    }

    public int getSpawnedLineCount() {
        int packetLines = viewerSessions.values().stream().mapToInt(value -> value.lines.size()).sum()
                + featureSessions.values().stream().mapToInt(value -> value.lines.size()).sum();
        return packetBoards.size() + packetLines;
    }

    public void refreshAll() {
        spawnAll();
        refreshActivePlayers(true);
    }

    public void despawnAll() {
        destroyAllViewerSessions();
        destroyAllFeatureSessions();
        for (UUID hologram : new ArrayList<>(packetBoards)) plugin.getNpcService().removeTransientHologram(hologram);
        packetBoards.clear();
    }

    public void shutdown() {
        stopTasks();
        despawnAll();
    }

    private boolean persist(String id, BoardData board) {
        Location location = LocUtil.fromString(board.location);
        if (location == null) return false;
        PitSetupSnapshot.Leaderboard value = new PitSetupSnapshot.Leaderboard();
        value.stableId = id;
        value.arenaId = board.arenaId;
        value.location = new PitSetupSnapshot.Location(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        value.statType = board.type;
        value.period = board.period;
        return plugin.getSetupService().repository().upsertLeaderboard(value);
    }

    private String serial(PitSetupSnapshot.Location value) {
        return value.worldName + "," + value.x + "," + value.y + "," + value.z + "," + value.yaw + "," + value.pitch;
    }

    private static final class BoardData {
        private final String location;
        private final String type;
        private final String period;
        private final String arenaId;

        private BoardData(String location, String type, String period, String arenaId) {
            this.location = location;
            this.type = type;
            this.period = period;
            this.arenaId = arenaId;
        }
    }

    private static final class ViewerSession {
        private final String arenaId;
        private final List<ActiveLeaderboardPacketTransport.Line> lines = new ArrayList<>();

        private ViewerSession(String arenaId) {
            this.arenaId = arenaId;
        }
    }

    private record DesiredLine(Location location, String text) {}

    public enum UnlockType { CURRENT_LEVEL, PERMANENT }

    public record FeatureDefinition(String id, int level, String display, String description, UnlockType unlockType) {}
}
