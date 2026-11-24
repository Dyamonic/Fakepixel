package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.network.SMPServerType;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class EventManager implements Listener {
    private static final String DATA_PATH = "event-system";
    private static final DateTimeFormatter CALENDAR_TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a");
    private static final Set<Material> MINING_BLOCKS = EnumSet.of(
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
            Material.ANDESITE, Material.DIORITE, Material.GRANITE, Material.TUFF, Material.CALCITE,
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE
    );
    private static final Set<Material> FARMING_BLOCKS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.PUMPKIN,
            Material.MELON, Material.SUGAR_CANE, Material.CACTUS, Material.NETHER_WART
    );

    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final SkillManager skillManager;
    private final EconomyManager economyManager;
    private final CustomItemManager customItemManager;
    private final ZoneManager zoneManager;
    private final Map<UUID, Long> progress = new HashMap<>();
    private final Map<UUID, Long> networkScores = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pendingScoreWrites = new ConcurrentHashMap<>();
    private final Set<UUID> participants = new HashSet<>();
    private final LinkedHashSet<BlockKey> playerPlacedBlocks = new LinkedHashSet<>();
    private final Map<BlockKey, Material> treasureBlocks = new HashMap<>();
    private final Deque<SmpEventType> upcoming = new ArrayDeque<>();
    private BukkitTask task;
    private BukkitTask networkRefreshTask;
    private final AtomicBoolean networkRefreshInFlight = new AtomicBoolean(false);
    private volatile long dbLastRefresh;
    private volatile long dbLastSuccess;
    private volatile long nextDbWarningAt;
    private volatile long lastNetworkUpdatedAt;
    private volatile long localNetworkWriteAt;
    private volatile String lastCreatedBy = "";
    private volatile Set<SMPServerType> activeAllowedScopes = Set.of();
    private EventState state = EventState.WAITING;
    private SmpEventType current;
    private SmpEventType next;
    private SmpEventType previous;
    private long startAt;
    private long endAt;
    private long nextAt;
    private String eventLocation = "";

    public EventManager(JavaPlugin plugin, DataManager dataManager, SkillManager skillManager,
                        EconomyManager economyManager, CustomItemManager customItemManager, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.skillManager = skillManager;
        this.economyManager = economyManager;
        this.customItemManager = customItemManager;
        this.zoneManager = zoneManager;
    }

    public void start() {
        stopTask();
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("events.enabled", true)) return;
        restore();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        refreshNetworkState();
        networkRefreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshNetworkState, 100L, 100L);
    }

    public void stop() {
        if (isOverworld()) persist();
        flushNetworkScores();
        stopTask();
        cleanupTreasures();
    }

    private void stopTask() {
        if (task != null) task.cancel();
        if (networkRefreshTask != null) networkRefreshTask.cancel();
        task = null;
        networkRefreshTask = null;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (!isNetworkCoordinator()) {
            if (current != null && state == EventState.STARTING && now >= startAt) activateFollower();
            if (current != null && state == EventState.ACTIVE && now >= endAt) expireLocalNetworkEvent();
            return;
        }
        if (current == null) {
            state = EventState.WAITING;
            if (next == null) next = pollUpcoming(previous);
            if (nextAt <= 0L) {
                nextAt = now + randomDowntimeMillis();
                persistNextNetworkState();
            }
            if (now >= nextAt && next != null) begin(next, false);
            return;
        }
        if (state == EventState.STARTING && now >= startAt) activate();
        if (state == EventState.ACTIVE && now >= endAt) finish(true);
    }

    private boolean isNetworkCoordinator() {
        if (!isOverworld()) return false;
        String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getString("events.network-coordinator-server", "smpworld-1");
        return configured == null || configured.isBlank() || serverId().equalsIgnoreCase(configured.trim());
    }

    private void activateFollower() {
        if (current == null) return;
        state = EventState.ACTIVE;
        announceFollowerActivation();
    }

    private void announceFollowerActivation() {
        Bukkit.broadcastMessage(CC.color("&6&lEVENT STARTED! &e" + current.displayName
                + " &7- &f" + current.description));
    }

    public void reloadNetworkState() {
        refreshNetworkState();
    }

    private void refreshNetworkState() {
        dbLastRefresh = System.currentTimeMillis();
        flushNetworkScores();
        if (!dataManager.isMySqlEnabled() || !networkRefreshInFlight.compareAndSet(false, true)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                NetworkEventSnapshot snapshot = loadNetworkSnapshot();
                Bukkit.getScheduler().runTask(plugin, () -> applyNetworkSnapshot(snapshot));
            } catch (Exception exception) {
                long now = System.currentTimeMillis();
                if (now >= nextDbWarningAt) {
                    nextDbWarningAt = now + 60_000L;
                    plugin.getLogger().warning("[SMP Events] Network event refresh failed; keeping cached state. Reason: " + exception.getMessage());
                }
            } finally {
                networkRefreshInFlight.set(false);
            }
        });
    }

    private NetworkEventSnapshot loadNetworkSnapshot() throws Exception {
        try (var connection = dataManager.getConnection()) {
            SmpEventType type = null;
            EventState networkState = EventState.WAITING;
            long starts = 0L, ends = 0L, updated = System.currentTimeMillis();
            Set<SMPServerType> scopes = Set.of();
            String createdBy = "";
            Map<UUID, Long> scores = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(
                     "SELECT event_id,state,starts_at,ends_at,allowed_server_types,created_by,updated_at " +
                             "FROM smp_network_events WHERE state IN ('STARTING','ACTIVE') AND ends_at>? " +
                             "ORDER BY updated_at DESC LIMIT 1")) {
                statement.setLong(1, System.currentTimeMillis());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        type = SmpEventType.parse(result.getString("event_id"));
                        networkState = parseState(result.getString("state"));
                        starts = result.getLong("starts_at");
                        ends = result.getLong("ends_at");
                        scopes = parseScopes(result.getString("allowed_server_types"));
                        createdBy = result.getString("created_by");
                        updated = result.getLong("updated_at");
                    }
                }
            }
            if (type != null) try (PreparedStatement score = connection.prepareStatement(
                    "SELECT player_uuid,SUM(score) total_score FROM smp_event_scores WHERE event_id=? GROUP BY player_uuid")) {
                score.setString(1, type.id);
                try (ResultSet rows = score.executeQuery()) {
                    while (rows.next()) try {
                        scores.put(UUID.fromString(rows.getString("player_uuid")), Math.round(rows.getDouble("total_score")));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            SmpEventType scheduled = null;
            long scheduledAt = 0L;
            try (PreparedStatement schedule = connection.prepareStatement(
                    "SELECT event_id,starts_at FROM smp_network_events WHERE state='SCHEDULED' AND starts_at>? " +
                            "ORDER BY starts_at ASC LIMIT 1")) {
                schedule.setLong(1, System.currentTimeMillis());
                try (ResultSet row = schedule.executeQuery()) {
                    if (row.next()) {
                        scheduled = SmpEventType.parse(row.getString("event_id"));
                        scheduledAt = row.getLong("starts_at");
                    }
                }
            }
            return new NetworkEventSnapshot(type, networkState, starts, ends, scopes, createdBy, updated,
                    Map.copyOf(scores), scheduled, scheduledAt);
        }
    }

    private void applyNetworkSnapshot(NetworkEventSnapshot snapshot) {
        dbLastSuccess = System.currentTimeMillis();
        if (snapshot.nextType() != null) {
            next = snapshot.nextType();
            nextAt = snapshot.nextAt();
        }
        if (snapshot.updatedAt() < lastNetworkUpdatedAt) return;
        if (snapshot.type() == null) {
            if (System.currentTimeMillis() - localNetworkWriteAt < 10_000L) return;
            if (current != null) expireLocalNetworkEvent();
            networkScores.clear();
            return;
        }
        lastNetworkUpdatedAt = snapshot.updatedAt();
        boolean changedEvent = current != snapshot.type();
        EventState previousState = state;
        current = snapshot.type();
        startAt = snapshot.startsAt();
        endAt = snapshot.endsAt();
        state = snapshot.state() == EventState.STARTING && System.currentTimeMillis() >= startAt
                ? EventState.ACTIVE : snapshot.state();
        lastCreatedBy = snapshot.createdBy();
        activeAllowedScopes = snapshot.scopes().isEmpty() ? current.allowedServerTypes() : snapshot.scopes();
        if (changedEvent) networkScores.clear();
        snapshot.scores().forEach((uuid, score) -> networkScores.merge(uuid, score, Math::max));
        if (!isNetworkCoordinator() && state == EventState.ACTIVE
                && (changedEvent || previousState != EventState.ACTIVE)) announceFollowerActivation();
    }

    private Set<SMPServerType> parseScopes(String value) {
        if (value == null || value.isBlank()) return Set.of();
        EnumSet<SMPServerType> scopes = EnumSet.noneOf(SMPServerType.class);
        for (String token : value.split(",")) {
            SMPServerType parsed = SMPServerType.parse(token);
            if (parsed != SMPServerType.UNKNOWN) scopes.add(parsed);
        }
        return Set.copyOf(scopes);
    }

    private void persistNetworkState() {
        if (current == null || !dataManager.isMySqlEnabled()) return;
        SmpEventType event = current;
        EventState eventState = state;
        long starts = startAt, ends = endAt, updated = System.currentTimeMillis();
        String scopes = event.allowedServerTypes().stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse("");
        String actor = lastCreatedBy == null ? "" : lastCreatedBy;
        localNetworkWriteAt = updated;
        lastNetworkUpdatedAt = updated;
        dataManager.runDatabaseAsync("network-event-state", connection -> {
            try (PreparedStatement finishOthers = connection.prepareStatement(
                    "UPDATE smp_network_events SET state='FINISHED',updated_at=? WHERE event_id<>? AND state IN ('STARTING','ACTIVE')")) {
                finishOthers.setLong(1, updated); finishOthers.setString(2, event.id); finishOthers.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO smp_network_events(event_id,display_name,state,starts_at,ends_at,allowed_server_types,created_by,updated_at) " +
                            "VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),state=VALUES(state)," +
                            "starts_at=VALUES(starts_at),ends_at=VALUES(ends_at),allowed_server_types=VALUES(allowed_server_types)," +
                            "created_by=VALUES(created_by),updated_at=VALUES(updated_at)")) {
                statement.setString(1, event.id); statement.setString(2, event.displayName); statement.setString(3, eventState.name());
                statement.setLong(4, starts); statement.setLong(5, ends); statement.setString(6, scopes);
                statement.setString(7, actor); statement.setLong(8, updated); statement.executeUpdate();
            }
        });
    }

    private void finishNetworkState(SmpEventType event) {
        if (event == null || !dataManager.isMySqlEnabled()) return;
        long updated = System.currentTimeMillis();
        localNetworkWriteAt = updated;
        dataManager.runDatabaseAsync("network-event-finish", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE smp_network_events SET state='FINISHED',updated_at=? WHERE event_id=?")) {
                statement.setLong(1, updated); statement.setString(2, event.id); statement.executeUpdate();
            }
        });
    }

    private void persistNextNetworkState() {
        if (next == null || nextAt <= 0L || !dataManager.isMySqlEnabled() || !isNetworkCoordinator()) return;
        SmpEventType scheduled = next;
        long scheduledAt = nextAt, updated = System.currentTimeMillis();
        String scopes = scheduled.allowedServerTypes().stream().map(Enum::name).sorted()
                .reduce((a, b) -> a + "," + b).orElse("");
        dataManager.runDatabaseAsync("network-event-schedule", connection -> {
            try (PreparedStatement clear = connection.prepareStatement(
                    "UPDATE smp_network_events SET state='FINISHED',updated_at=? WHERE state='SCHEDULED' AND event_id<>?")) {
                clear.setLong(1, updated); clear.setString(2, scheduled.id); clear.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO smp_network_events(event_id,display_name,state,starts_at,ends_at,allowed_server_types,created_by,updated_at) " +
                            "VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),state=VALUES(state)," +
                            "starts_at=VALUES(starts_at),ends_at=VALUES(ends_at),allowed_server_types=VALUES(allowed_server_types)," +
                            "created_by=VALUES(created_by),updated_at=VALUES(updated_at)")) {
                statement.setString(1, scheduled.id); statement.setString(2, scheduled.displayName);
                statement.setString(3, "SCHEDULED"); statement.setLong(4, scheduledAt);
                statement.setLong(5, scheduledAt + scheduled.durationMinutes * 60_000L);
                statement.setString(6, scopes); statement.setString(7, "scheduler");
                statement.setLong(8, updated); statement.executeUpdate();
            }
        });
    }

    private void flushNetworkScores() {
        if (pendingScoreWrites.isEmpty() || current == null || !dataManager.isMySqlEnabled()) return;
        SmpEventType event = current;
        Map<UUID, Long> batch = new HashMap<>();
        pendingScoreWrites.forEach((uuid, score) -> {
            Long removed = pendingScoreWrites.remove(uuid);
            if (removed != null && removed > 0L) batch.put(uuid, removed);
        });
        if (batch.isEmpty()) return;
        String currentServer = serverId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (var connection = dataManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO smp_event_scores(event_id,player_uuid,server_id,score,updated_at) VALUES(?,?,?,?,?) " +
                                 "ON DUPLICATE KEY UPDATE score=score+VALUES(score),updated_at=VALUES(updated_at)")) {
                for (Map.Entry<UUID, Long> entry : batch.entrySet()) {
                    statement.setString(1, event.id); statement.setString(2, entry.getKey().toString());
                    statement.setString(3, currentServer); statement.setDouble(4, entry.getValue());
                    statement.setLong(5, System.currentTimeMillis()); statement.addBatch();
                }
                statement.executeBatch();
            } catch (SQLException exception) {
                batch.forEach((uuid, score) -> pendingScoreWrites.merge(uuid, score, Long::sum));
                long now = System.currentTimeMillis();
                if (now >= nextDbWarningAt) {
                    nextDbWarningAt = now + 60_000L;
                    plugin.getLogger().warning("[SMP Events] Score flush failed; points remain queued. Reason: " + exception.getMessage());
                }
            }
        });
    }

    private void expireLocalNetworkEvent() {
        current = null;
        activeAllowedScopes = Set.of();
        state = EventState.WAITING;
        startAt = 0L;
        endAt = 0L;
        eventLocation = "";
    }

    public boolean startEvent(String input) {
        return startEvent(input, "console");
    }

    public boolean startEvent(String input, String createdBy) {
        SmpEventType type = SmpEventType.parse(input);
        if (type == null || type.allowedServerTypes().isEmpty()) return false;
        if (current != null) finish(false);
        lastCreatedBy = createdBy == null ? "" : createdBy;
        begin(type, true);
        return true;
    }

    public boolean stopEvent() {
        if (current == null) return false;
        finish(true);
        return true;
    }

    public void forceNext() {
        if (current != null) finish(false);
        next = pollUpcoming(previous);
        nextAt = System.currentTimeMillis() + 10_000L;
        state = EventState.WAITING;
        persistNextNetworkState();
        if (isOverworld()) persist();
    }

    private boolean isOverworld(){return serverType() == SMPServerType.OVERWORLD;}

    public SMPServerType serverType() {
        if (!(plugin instanceof club.abelix.games.fakevival.Game game)) return SMPServerType.OVERWORLD;
        String id = game.getNetworkService().getCurrentServerId().toLowerCase(Locale.ROOT);
        if (id.startsWith("smpdun-")) return SMPServerType.DUNGEON;
        if (id.startsWith("smpworld-")) return SMPServerType.OVERWORLD;
        if (id.startsWith("smp-")) return SMPServerType.HUB;
        return game.getNetworkService().identity().serverType();
    }

    public String serverId() {
        return plugin instanceof club.abelix.games.fakevival.Game game
                ? game.getNetworkService().getCurrentServerId() : "local-test";
    }

    public boolean canDisplayEvent(SmpEventType event) {
        return event != null;
    }

    public boolean canProgressEvent(Player player, String eventId) {
        return canProgressEvent(player, SmpEventType.parse(eventId));
    }

    public boolean canProgressEvent(Player player, SmpEventType event) {
        if (player == null || event == null) return false;
        return canParticipateOnThisServer(event);
    }

    public boolean canParticipateOnThisServer(SmpEventType event) {
        Set<SMPServerType> scopes = event == current && !activeAllowedScopes.isEmpty()
                ? activeAllowedScopes : event == null ? Set.of() : event.allowedServerTypes();
        if (event == null || !scopes.contains(serverType())) return false;
        if (serverType() != SMPServerType.HUB) return true;
        if (!event.safeInHub()) return false;
        if (event.requiresZone() && !zoneManager.hasConfiguredZones()) return false;
        return event != SmpEventType.BOSS_INVASION && event != SmpEventType.BOSS_HOUR
                || zoneManager.hasEventBossZone();
    }

    public boolean isActionAllowedForEvent(SmpEventType event, EventActionType action) {
        if (event == null || action == null) return false;
        return switch (action) {
            case FISHING -> Set.of(SmpEventType.FISHING_FESTIVAL, SmpEventType.SEA_CREATURE_SWARM,
                    SmpEventType.TREASURE_TIDE, SmpEventType.FROSTFALL, SmpEventType.RECIPE_HUNT,
                    SmpEventType.COLLECTION_BOOST).contains(event);
            case MOB_KILL -> Set.of(SmpEventType.MOB_HUNT, SmpEventType.MONSTER_MAYHEM,
                    SmpEventType.LUCKY_DROPS, SmpEventType.RECIPE_HUNT, SmpEventType.RIFT_INVASION,
                    SmpEventType.FROSTFALL, SmpEventType.INFERNAL_NIGHT, SmpEventType.COIN_STORM,
                    SmpEventType.COLLECTION_BOOST, SmpEventType.DUNGEON_SURGE).contains(event);
            case BOSS_KILL, BOSS_DAMAGE -> Set.of(SmpEventType.BOSS_INVASION, SmpEventType.BOSS_HOUR,
                    SmpEventType.MOB_HUNT, SmpEventType.DUNGEON_SURGE, SmpEventType.COIN_STORM).contains(event);
            case EXPLORATION -> event == SmpEventType.EXPLORERS_CALL || event == SmpEventType.COLLECTION_BOOST;
            case MINING -> Set.of(SmpEventType.MINING_FIESTA, SmpEventType.MINING_RUSH,
                    SmpEventType.GEMSTONE_GLOW, SmpEventType.RECIPE_HUNT, SmpEventType.COLLECTION_BOOST).contains(event);
            case FARMING -> Set.of(SmpEventType.FARMING_CONTEST, SmpEventType.CROP_FRENZY,
                    SmpEventType.RECIPE_HUNT, SmpEventType.COLLECTION_BOOST).contains(event);
            case FORAGING -> Set.of(SmpEventType.WOODCUTTING_RUSH, SmpEventType.RECIPE_HUNT,
                    SmpEventType.COLLECTION_BOOST).contains(event);
            case DUNGEON_MOB -> event.allowedServerTypes().contains(SMPServerType.DUNGEON)
                    && isActionAllowedForEvent(event, EventActionType.MOB_KILL);
            case DUNGEON_BOSS -> event.allowedServerTypes().contains(SMPServerType.DUNGEON)
                    && isActionAllowedForEvent(event, EventActionType.BOSS_KILL);
            case DUNGEON_CLEAR -> event == SmpEventType.DUNGEON_SURGE || event == SmpEventType.COLLECTION_BOOST;
        };
    }

    private void begin(SmpEventType type, boolean admin) {
        cleanupTreasures();
        current = type;
        activeAllowedScopes = type.allowedServerTypes();
        upcoming.removeFirstOccurrence(type);
        state = EventState.STARTING;
        startAt = System.currentTimeMillis() + (admin ? 5_000L : 10_000L);
        endAt = startAt + type.durationMinutes * 60_000L;
        next = pollUpcoming(type);
        nextAt = endAt + randomDowntimeMillis();
        progress.clear();
        networkScores.clear();
        pendingScoreWrites.clear();
        participants.clear();
        eventLocation = "";
        Bukkit.broadcastMessage(CC.color("&e&lEVENT! &a" + type.displayName + " &fstarts in &e" + (admin ? 5 : 10) + " seconds&7!"));
        persistNetworkState();
        persistNextNetworkState();
        if (isOverworld()) persist();
    }

    private void activate() {
        if (current == null) return;
        state = EventState.ACTIVE;
        if (current == SmpEventType.BOSS_INVASION && isOverworld()) {
            eventLocation = zoneManager.startEventBossInvasion();
            if (eventLocation == null) {
                Bukkit.broadcastMessage(CC.color("&cBoss Invasion was skipped because no loaded dungeon boss zone has players nearby."));
                finish(false);
                return;
            }
        } else if (current == SmpEventType.TREASURE_HUNT) {
            spawnTreasures();
        }
        Bukkit.broadcastMessage(CC.color("&6&lEVENT STARTED! &e" + current.displayName
                + (eventLocation.isBlank() ? "" : " &fin &f" + eventLocation)
                + " &7- &f" + current.description));
        persistNetworkState();
        if (isOverworld()) persist();
    }

    private void finish(boolean rewards) {
        if (current == null) return;
        flushNetworkScores();
        state = EventState.ENDING;
        SmpEventType finished = current;
        if (rewards) rewardParticipants(finished);
        cleanupTreasures();
        Bukkit.broadcastMessage(CC.color("&e" + finished.displayName + " &fhas ended."
                + (participants.isEmpty() ? "" : " &a" + participants.size() + " players participated!")));
        previous = finished;
        current = null;
        activeAllowedScopes = Set.of();
        state = EventState.FINISHED;
        startAt = 0L;
        endAt = 0L;
        eventLocation = "";
        progress.clear();
        participants.clear();
        if (next == null) next = pollUpcoming(finished);
        if (nextAt <= System.currentTimeMillis()) nextAt = System.currentTimeMillis() + randomDowntimeMillis();
        persistNextNetworkState();
        clearPersistedActive();
        finishNetworkState(finished);
        if (isOverworld()) persist();
    }

    private void rewardParticipants(SmpEventType finished) {
        List<Map.Entry<UUID, Long>> ranking = progress.entrySet().stream()
                .filter(entry -> entry.getValue() > 0L)
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .toList();
        for (int index = 0; index < ranking.size(); index++) {
            UUID uuid = ranking.get(index).getKey();
            long score = ranking.get(index).getValue();
            int tokens = tokenReward(finished, score, index);
            if (plugin instanceof club.abelix.games.fakevival.Game game) tokens = (int) Math.round(
                    game.getMayorManager().applyMayorModifier(Bukkit.getPlayer(uuid),
                            club.abelix.games.fakevival.mayor.MayorModifierType.EVENT_TOKENS, tokens));
            double coins = (index == 0 ? 1_000.0D : index == 1 ? 650.0D : index == 2 ? 400.0D : 100.0D)
                    + Math.min(500.0D, score * 2.0D);
            if (plugin instanceof club.abelix.games.fakevival.Game game) coins = game.getMayorManager().applyMayorModifier(
                    Bukkit.getPlayer(uuid), club.abelix.games.fakevival.mayor.MayorModifierType.REWARDS, coins);
            economyManager.giveCoins(uuid, coins, "event:" + finished.id);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (tokens > 0) giveCustomItem(player, "event_token", tokens);
                String skill = finished.rewardSkill;
                if (skill != null) skillManager.addXp(player, skill, 50 + Math.min(250, (int) score));
                player.sendMessage(CC.color("&6Event Reward: &e" + economyManager.format(coins) + " coins"
                        + (tokens > 0 ? "&7, &e" + tokens + " Event Token" + (tokens == 1 ? "" : "s") : "")));
            } else if (tokens > 0) {
                dataManager.getData().set(DATA_PATH + ".pending-tokens." + uuid,
                        dataManager.getData().getInt(DATA_PATH + ".pending-tokens." + uuid, 0) + tokens);
            }
        }
    }

    private int tokenReward(SmpEventType type, long score, int placement) {
        long target = Math.max(1L, milestone(type));
        if (type == SmpEventType.BOSS_INVASION) {
            int contribution = (int) Math.min(15L, Math.max(0L, (score * 15L) / target));
            int placementBonus = placement == 0 ? 5 : placement == 1 ? 3 : placement == 2 ? 2 : 0;
            return Math.clamp(5 + contribution + placementBonus, 5, 25);
        }
        double ratio = score / (double) target;
        int participation = ratio < 0.25D ? 1 : ratio < 0.75D ? 2 : ratio < 1.5D ? 4 : ratio < 3.0D ? 7 : 10;
        int placementBonus = ratio >= 0.5D ? (placement == 0 ? 5 : placement == 1 ? 3 : placement == 2 ? 2 : 0) : 0;
        return Math.clamp(participation + placementBonus, 1, 20);
    }

    private long milestone(SmpEventType type) {
        return switch (type) {
            case FARMING_CONTEST -> 100L;
            case MINING_FIESTA -> 150L;
            case FISHING_FESTIVAL -> 50L;
            case MOB_HUNT -> 75L;
            case BOSS_INVASION -> 5_000L;
            default -> 25L;
        };
    }

    public double skillXpMultiplier(Player player, String skill) {
        if (state != EventState.ACTIVE || current == null || skill == null) return 1.0D;
        if (!canProgressEvent(player, current)) return 1.0D;
        String normalized = skill.toLowerCase(Locale.ROOT);
        if (current == SmpEventType.DOUBLE_SKILL_XP) {
            addProgress(player, 1L);
            return 2.0D;
        }
        if (current == SmpEventType.MINING_FIESTA && normalized.equals("mining")) return 1.25D;
        if (current == SmpEventType.FISHING_FESTIVAL && normalized.equals("fishing")) return 1.25D;
        if (current == SmpEventType.MOB_HUNT && normalized.equals("combat")) return 1.25D;
        if (current == SmpEventType.WOODCUTTING_RUSH && (normalized.equals("woodcutting") || normalized.equals("foraging"))) return 1.25D;
        if (current == SmpEventType.CROP_FRENZY && normalized.equals("farming")) return 1.25D;
        if (current == SmpEventType.MONSTER_MAYHEM && normalized.equals("combat")) return 1.25D;
        if (current == SmpEventType.SEA_CREATURE_SWARM && normalized.equals("fishing")) return 1.15D;
        if (current == SmpEventType.MINING_RUSH && normalized.equals("mining")) return 1.25D;
        if (current == SmpEventType.GEMSTONE_GLOW && normalized.equals("mining")) return 1.15D;
        if (current == SmpEventType.SKILL_WEEKEND) {
            addProgress(player, 1L);
            return 1.20D;
        }
        if (current == SmpEventType.PET_TRAINING && normalized.equals("taming")) {
            addProgress(player, 1L);
            return 1.25D;
        }
        return 1.0D;
    }

    public double statBonus(Player player, MMOStatType stat) {
        if (state != EventState.ACTIVE || current == null) return 0.0D;
        if (!canProgressEvent(player, current)) return 0.0D;
        if (current == SmpEventType.MINING_FIESTA && stat == MMOStatType.MINING_FORTUNE) return 25.0D;
        if (current == SmpEventType.FISHING_FESTIVAL && stat == MMOStatType.SEA_CREATURE_CHANCE) return 10.0D;
        if (current == SmpEventType.FISHING_FESTIVAL && stat == MMOStatType.TREASURE_CHANCE) return 5.0D;
        if (current == SmpEventType.SEA_CREATURE_SWARM && stat == MMOStatType.SEA_CREATURE_CHANCE) return 15.0D;
        if (current == SmpEventType.TREASURE_TIDE && stat == MMOStatType.TREASURE_CHANCE) return 10.0D;
        if (current == SmpEventType.MINING_RUSH && stat == MMOStatType.MINING_FORTUNE) return 25.0D;
        if (current == SmpEventType.LUCKY_DROPS && stat == MMOStatType.MAGIC_FIND) return 10.0D;
        return 0.0D;
    }

    public double activityCoinMultiplier() {
        return state == EventState.ACTIVE && current == SmpEventType.COIN_STORM
                && canParticipateOnThisServer(current) ? 1.15D : 1.0D;
    }

    public double guildSellMultiplier() {
        return state == EventState.ACTIVE && current == SmpEventType.MERCHANT_DAY
                && canParticipateOnThisServer(current) ? 1.10D : 1.0D;
    }

    public double dungeonRespawnMultiplier() {
        return state == EventState.ACTIVE && current == SmpEventType.DUNGEON_SURGE
                && canParticipateOnThisServer(current) ? 0.75D : 1.0D;
    }

    public double bossRespawnMultiplier() {
        return state == EventState.ACTIVE && current == SmpEventType.BOSS_HOUR
                && canParticipateOnThisServer(current) ? 0.75D : 1.0D;
    }

    public void recordZoneDiscovery(Player player) {
        if (player == null || state != EventState.ACTIVE || current != SmpEventType.EXPLORERS_CALL) return;
        if (!canParticipateOnThisServer(current) || !isActionAllowedForEvent(current, EventActionType.EXPLORATION)) return;
        addProgress(player, 25L);
        economyManager.giveCoins(player.getUniqueId(), 50.0D, "event:explorers_call");
    }

    public void recordGuildSale(Player player, int amount) {
        if (player == null || state != EventState.ACTIVE || current != SmpEventType.MERCHANT_DAY) return;
        addProgress(player, Math.max(1, amount));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isOverworld()) return;
        Block block = event.getBlock();
        BlockKey key = BlockKey.of(block);
        boolean placed = playerPlacedBlocks.remove(key);
        if (state != EventState.ACTIVE || current == null || placed) return;
        if ((current == SmpEventType.MINING_FIESTA || current == SmpEventType.MINING_RUSH || current == SmpEventType.GEMSTONE_GLOW)
                && MINING_BLOCKS.contains(block.getType())) {
            addProgress(event.getPlayer(), 1L);
            double chance = current == SmpEventType.GEMSTONE_GLOW ? 7.0D : 4.0D;
            if (ThreadLocalRandom.current().nextDouble(100.0D) < chance) giveRandomMaterial(event.getPlayer(),
                    "mithril_dust", "titanium_shard", "ruby_fragment", "dense_iron_fragment", "lapis_focus_lens");
        } else if ((current == SmpEventType.FARMING_CONTEST || current == SmpEventType.CROP_FRENZY) && validCrop(block)) {
            addProgress(event.getPlayer(), 1L);
            if (current == SmpEventType.CROP_FRENZY && ThreadLocalRandom.current().nextDouble(100.0D) < 5.0D) {
                giveRandomMaterial(event.getPlayer(), "golden_wheat_bundle", "carrot_seed_core", "potato_sprout", "beetroot_extract", "melon_rind");
            }
        } else if (current == SmpEventType.WOODCUTTING_RUSH && isLog(block.getType())) {
            addProgress(event.getPlayer(), 1L);
            if (ThreadLocalRandom.current().nextDouble(100.0D) < 5.0D) giveRandomMaterial(event.getPlayer(),
                    "oak_bark_strip", "birch_bark_strip", "spruce_pinecone", "jungle_vine_fiber", "living_root");
        } else if (current == SmpEventType.RECIPE_HUNT && (MINING_BLOCKS.contains(block.getType()) || validCrop(block) || isLog(block.getType()))) {
            addProgress(event.getPlayer(), 1L);
            if (ThreadLocalRandom.current().nextDouble(100.0D) < 2.0D) giveCustomItem(event.getPlayer(), "recipe_fragment_alpha", 1);
        } else if (current == SmpEventType.COLLECTION_BOOST && (MINING_BLOCKS.contains(block.getType()) || validCrop(block) || isLog(block.getType()))) {
            addProgress(event.getPlayer(), 1L);
            if (ThreadLocalRandom.current().nextDouble(100.0D) < 4.0D) giveRandomMaterial(event.getPlayer(),
                    "rough_coal_chunk", "oak_bark_strip", "golden_wheat_bundle", "zombie_fang", "river_fish_scale");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTreasureBreak(BlockBreakEvent event) {
        if (!isOverworld()) return;
        if (treasureBlocks.containsKey(BlockKey.of(event.getBlock()))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isOverworld()) return;
        BlockKey key = BlockKey.of(event.getBlock());
        playerPlacedBlocks.add(key);
        if (playerPlacedBlocks.size() > 50_000) playerPlacedBlocks.remove(playerPlacedBlocks.getFirst());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (state != EventState.ACTIVE || current == null || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        recordFishingCatch(event.getPlayer());
    }

    public void recordFishingCatch(Player player) {
        if (player == null || state != EventState.ACTIVE || current == null) return;
        if (!canProgressEvent(player, current)) return;
        if (!isActionAllowedForEvent(current, EventActionType.FISHING)) return;
        addProgress(player, 5L);
        if (current == SmpEventType.RECIPE_HUNT && ThreadLocalRandom.current().nextDouble(100.0D) < 3.0D) {
            giveCustomItem(player, "recipe_fragment_alpha", 1);
        } else if (ThreadLocalRandom.current().nextDouble(100.0D) < (current == SmpEventType.TREASURE_TIDE ? 10.0D : 6.0D)) giveRandomMaterial(player,
                "angler_scale", "deepwater_pearl", "sea_serpent_scale", "kraken_ink", "prismarine_rod_core");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(EntityDeathEvent event) {
        if (state != EventState.ACTIVE || current == null) return;
        Player killer = event.getEntity().getKiller();
        boolean dungeonMob = plugin instanceof club.abelix.games.fakevival.Game game
                && game.getDungeonManager() != null && game.getDungeonManager().isDungeonEntity(event.getEntity());
        if (killer == null || (!zoneManager.isZoneMob(event.getEntity()) && !dungeonMob)) return;
        if (!canProgressEvent(killer, current)) return;
        boolean boss = zoneManager.isZoneBoss(event.getEntity()) || (dungeonMob
                && plugin instanceof club.abelix.games.fakevival.Game game
                && game.getDungeonManager().isDungeonBossEntity(event.getEntity()));
        EventActionType action = dungeonMob ? (boss ? EventActionType.DUNGEON_BOSS : EventActionType.DUNGEON_MOB)
                : (boss ? EventActionType.BOSS_KILL : EventActionType.MOB_KILL);
        if (!isActionAllowedForEvent(current, action)) return;
        addProgress(killer, boss ? 50L : 5L);
        if (current == SmpEventType.RECIPE_HUNT && ThreadLocalRandom.current().nextDouble(100.0D) < 4.0D) giveCustomItem(killer, "recipe_fragment_alpha", 1);
        else if (current == SmpEventType.RIFT_INVASION && ThreadLocalRandom.current().nextDouble(100.0D) < 10.0D) giveRandomMaterial(killer, "void_claw", "ender_residue", "shadow_dust");
        else if (current == SmpEventType.FROSTFALL && ThreadLocalRandom.current().nextDouble(100.0D) < 10.0D) giveRandomMaterial(killer, "frozen_weapon_core", "frozen_bone");
        else if (current == SmpEventType.INFERNAL_NIGHT && ThreadLocalRandom.current().nextDouble(100.0D) < 10.0D) giveRandomMaterial(killer, "blaze_cinder", "magma_scale", "blaze_heat_core");
        else if ((!boss || current == SmpEventType.BOSS_HOUR) && ThreadLocalRandom.current().nextDouble(100.0D) < (current == SmpEventType.LUCKY_DROPS ? 10.0D : 8.0D)) giveRandomMaterial(killer,
                    "hardened_bone_shard", "spider_silk_bundle", "creeper_powder_core", "shadow_fang", "ancient_bone");
    }

    private boolean isLog(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (state != EventState.ACTIVE || current != SmpEventType.BOSS_INVASION
                || !(event.getEntity() instanceof LivingEntity living) || !zoneManager.isZoneBoss(living)) return;
        if (!isActionAllowedForEvent(current, EventActionType.BOSS_DAMAGE)) return;
        Player player = attackingPlayer(event);
        if (player != null && canProgressEvent(player, current)) addProgress(player, Math.max(1L, Math.round(event.getFinalDamage())));
    }

    public void recordDungeonClear(Player player, long score) {
        if (player == null || current == null || state != EventState.ACTIVE) return;
        if (!canParticipateOnThisServer(current) || !isActionAllowedForEvent(current, EventActionType.DUNGEON_CLEAR)) return;
        addProgress(player, Math.max(25L, score));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTreasureOpen(PlayerInteractEvent event) {
        if (!isOverworld()) return;
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || !treasureBlocks.containsKey(BlockKey.of(block))) return;
        event.setCancelled(true);
        BlockKey key = BlockKey.of(block);
        treasureBlocks.remove(key);
        block.setType(Material.AIR, false);
        addProgress(event.getPlayer(), 15L);
        double coins = ThreadLocalRandom.current().nextDouble(50.0D, 151.0D);
        economyManager.giveCoins(event.getPlayer().getUniqueId(), coins, "event:treasure_hunt");
        if (ThreadLocalRandom.current().nextDouble(100.0D) < 5.0D) giveCustomItem(event.getPlayer(), "event_token", 1);
        else giveRandomMaterial(event.getPlayer(), "ancient_gear_fragment", "gold_dust", "deepwater_pearl",
                "recipe_fragment_alpha", "storm_bait", "ancient_ore_chunk");
        event.getPlayer().sendMessage(CC.color("&6Treasure found! &e+" + economyManager.format(coins) + " coins"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (participants.contains(event.getPlayer().getUniqueId())) {
            flushNetworkScores();
            if (isOverworld()) persist();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String path = DATA_PATH + ".pending-tokens." + event.getPlayer().getUniqueId();
        int pending = dataManager.getData().getInt(path, 0);
        if (pending <= 0) return;
        giveCustomItem(event.getPlayer(), "event_token", pending);
        dataManager.getData().set(path, null);
        dataManager.save();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (current == SmpEventType.TREASURE_HUNT && state == EventState.ACTIVE) return;
        boolean changed = false;
        for (BlockKey key : new HashSet<>(treasureBlocks.keySet())) {
            if (!key.worldId.equals(event.getWorld().getUID()) || key.x >> 4 != event.getChunk().getX() || key.z >> 4 != event.getChunk().getZ()) continue;
            Block block = event.getWorld().getBlockAt(key.x, key.y, key.z);
            if (block.getType() == Material.CHEST) block.setType(Material.AIR, false);
            treasureBlocks.remove(key);
            changed = true;
        }
        if (changed) persistTreasureLocations();
    }

    private boolean validCrop(Block block) {
        if (!FARMING_BLOCKS.contains(block.getType())) return false;
        return !(block.getBlockData() instanceof Ageable ageable) || ageable.getAge() >= ageable.getMaximumAge();
    }

    private Player attackingPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }

    private void addProgress(Player player, long amount) {
        if (player == null || amount <= 0L || state != EventState.ACTIVE || current == null) return;
        if (!canProgressEvent(player, current)) return;
        UUID uuid = player.getUniqueId();
        participants.add(uuid);
        progress.merge(uuid, amount, Long::sum);
        networkScores.merge(uuid, amount, Long::sum);
        pendingScoreWrites.merge(uuid, amount, Long::sum);
    }

    private void giveRandomMaterial(Player player, String... ids) {
        if (ids.length == 0) return;
        String id = ids[ThreadLocalRandom.current().nextInt(ids.length)];
        giveCustomItem(player, id, 1);
    }

    private void giveCustomItem(Player player, String id, int amount) {
        if (player == null || !customItemManager.itemExists(id)) return;
        ItemStack item = customItemManager.createItem(id, amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(value -> player.getWorld().dropItemNaturally(player.getLocation(), value));
    }

    private void spawnTreasures() {
        if (!isOverworld()) return;
        int target = Math.min(12, Math.max(4, Bukkit.getOnlinePlayers().size() * 2));
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) return;
        int attempts = 0;
        while (treasureBlocks.size() < target && attempts++ < target * 12) {
            Player player = players.get(ThreadLocalRandom.current().nextInt(players.size()));
            World world = player.getWorld();
            int x = player.getLocation().getBlockX() + ThreadLocalRandom.current().nextInt(-24, 25);
            int z = player.getLocation().getBlockZ() + ThreadLocalRandom.current().nextInt(-24, 25);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            Block ground = world.getHighestBlockAt(x, z);
            Block block = world.getBlockAt(x, ground.getY() + 1, z);
            if (!block.getType().isAir()) continue;
            BlockKey key = BlockKey.of(block);
            treasureBlocks.put(key, Material.AIR);
            block.setType(Material.CHEST, false);
        }
        persistTreasureLocations();
    }

    private void cleanupTreasures() {
        for (BlockKey key : new HashSet<>(treasureBlocks.keySet())) {
            World world = Bukkit.getWorld(key.worldId);
            if (world == null || !world.isChunkLoaded(key.x >> 4, key.z >> 4)) continue;
            Block block = world.getBlockAt(key.x, key.y, key.z);
            if (block.getType() == Material.CHEST) block.setType(treasureBlocks.getOrDefault(key, Material.AIR), false);
            treasureBlocks.remove(key);
        }
        if (treasureBlocks.isEmpty()) dataManager.getData().set(DATA_PATH + ".treasures", null);
        else persistTreasureLocations();
    }

    private void persistTreasureLocations() {
        List<String> values = treasureBlocks.keySet().stream()
                .map(key -> key.worldId + ";" + key.x + ";" + key.y + ";" + key.z)
                .toList();
        dataManager.getData().set(DATA_PATH + ".treasures", values);
    }

    private void restoreTreasures() {
        for (String value : dataManager.getData().getStringList(DATA_PATH + ".treasures")) {
            String[] parts = value.split(";");
            if (parts.length != 4) continue;
            try {
                BlockKey key = new BlockKey(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                treasureBlocks.put(key, Material.AIR);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private SmpEventType pollUpcoming(SmpEventType excluded) {
        ensureUpcoming(excluded);
        while (!upcoming.isEmpty()) {
            SmpEventType candidate = upcoming.removeFirst();
            if (candidate != excluded && available(candidate)) return candidate;
        }
        return null;
    }

    private void ensureUpcoming(SmpEventType excluded) {
        int targetSize = (int) java.util.Arrays.stream(SmpEventType.values())
                .filter(type -> type != excluded && available(type)).count();
        while (upcoming.size() < targetSize) {
            List<SmpEventType> choices = new ArrayList<>();
            for (SmpEventType type : SmpEventType.values()) {
                if (type != excluded && available(type) && !upcoming.contains(type)) choices.add(type);
            }
            if (choices.isEmpty()) return;
            Collections.shuffle(choices);
            boolean added = false;
            for (SmpEventType type : choices) {
                if (upcoming.size() >= targetSize) break;
                SmpEventType tail = upcoming.peekLast();
                if (tail != type) {
                    upcoming.addLast(type);
                    added = true;
                }
            }
            if (!added) return;
        }
    }

    private boolean available(SmpEventType type) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getBoolean("events.types." + type.id + ".enabled", true)) return false;
        if (type == SmpEventType.FISHING_FESTIVAL || type == SmpEventType.SEA_CREATURE_SWARM
                || type == SmpEventType.TREASURE_TIDE) {
            return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("skills.enabled", true);
        }
        if (type == SmpEventType.BOSS_INVASION) return zoneManager.hasEventBossZone();
        if (type == SmpEventType.BOSS_HOUR) return zoneManager.hasEventBossZone();
        if (type == SmpEventType.PET_TRAINING) {
            return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("companions.enabled", true);
        }
        if (type == SmpEventType.RIFT_INVASION || type == SmpEventType.DUNGEON_SURGE
                || type == SmpEventType.EXPLORERS_CALL) return zoneManager.hasConfiguredZones();
        if (type == SmpEventType.TREASURE_HUNT) return !Bukkit.getOnlinePlayers().isEmpty();
        return true;
    }

    private long randomDowntimeMillis() {
        long min = Math.max(1L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("events.min-downtime-minutes", 30L));
        long max = Math.max(min, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("events.max-downtime-minutes", 60L));
        return ThreadLocalRandom.current().nextLong(min, max + 1L) * 60_000L;
    }

    private void restore() {
        String type = dataManager.getData().getString(DATA_PATH + ".active.type", "");
        current = SmpEventType.parse(type);
        startAt = dataManager.getData().getLong(DATA_PATH + ".active.start", 0L);
        endAt = dataManager.getData().getLong(DATA_PATH + ".active.end", 0L);
        state = parseState(dataManager.getData().getString(DATA_PATH + ".active.state", "WAITING"));
        previous = SmpEventType.parse(dataManager.getData().getString(DATA_PATH + ".previous", ""));
        upcoming.clear();
        for (String id : dataManager.getData().getStringList(DATA_PATH + ".upcoming")) {
            SmpEventType scheduled = SmpEventType.parse(id);
            if (scheduled != null && available(scheduled)) upcoming.addLast(scheduled);
        }
        next = SmpEventType.parse(dataManager.getData().getString(DATA_PATH + ".next.type", ""));
        nextAt = dataManager.getData().getLong(DATA_PATH + ".next.at", 0L);
        ConfigurationSection savedProgress = dataManager.getData().getConfigurationSection(DATA_PATH + ".active.progress");
        if (savedProgress != null) {
            for (String key : savedProgress.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long value = Math.max(0L, savedProgress.getLong(key));
                    progress.put(uuid, value);
                    if (value > 0L) participants.add(uuid);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        restoreTreasures();
        long now = System.currentTimeMillis();
        if (current != null && endAt > now) {
            state = now < startAt ? EventState.STARTING : EventState.ACTIVE;
        } else if (current != null) {
            current = null;
            state = EventState.WAITING;
            cleanupTreasures();
        }
        if (next == null) next = pollUpcoming(previous);
        if (nextAt <= now) nextAt = now + randomDowntimeMillis();
    }

    private EventState parseState(String value) {
        try {
            return EventState.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return EventState.WAITING;
        }
    }

    private void persist() {
        dataManager.getData().set(DATA_PATH + ".previous", previous == null ? null : previous.id);
        dataManager.getData().set(DATA_PATH + ".next.type", next == null ? null : next.id);
        dataManager.getData().set(DATA_PATH + ".next.at", nextAt);
        dataManager.getData().set(DATA_PATH + ".upcoming", upcoming.stream().map(type -> type.id).toList());
        if (current != null) {
            dataManager.getData().set(DATA_PATH + ".active.type", current.id);
            dataManager.getData().set(DATA_PATH + ".active.state", state.name());
            dataManager.getData().set(DATA_PATH + ".active.start", startAt);
            dataManager.getData().set(DATA_PATH + ".active.end", endAt);
            dataManager.getData().set(DATA_PATH + ".active.progress", null);
            for (Map.Entry<UUID, Long> entry : progress.entrySet()) {
                dataManager.getData().set(DATA_PATH + ".active.progress." + entry.getKey(), entry.getValue());
            }
            persistTreasureLocations();
        }
        dataManager.save();
    }

    private void clearPersistedActive() {
        dataManager.getData().set(DATA_PATH + ".active", null);
        dataManager.getData().set(DATA_PATH + ".treasures", null);
    }

    public String placeholder(Player player, String placeholder) {
        String key = placeholder == null ? "" : placeholder.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "name" -> current == null ? "None" : current.displayName;
            case "name_colored" -> current == null ? "&cNone" : "&a" + current.displayName;
            case "status", "state" -> current == null ? EventState.WAITING.name() : state.name();
            case "time_left", "time_until_end" -> current == null ? "0s" : formatDuration(Math.max(0L, endAt - System.currentTimeMillis()));
            case "next", "next_name" -> next == null ? "None" : next.displayName;
            case "next_time" -> current != null ? "After event" : formatDuration(Math.max(0L, nextAt - System.currentTimeMillis()));
            case "next_minecraft_days" -> formatMinecraftDays(Math.max(0L, nextAt - System.currentTimeMillis()));
            case "duration" -> (current != null ? current : next) == null ? "--" : (current != null ? current : next).durationMinutes + " minutes";
            case "progress" -> progressPercent() + "%";
            case "score", "player_score" -> String.valueOf(player == null ? 0L
                    : Math.max(progress.getOrDefault(player.getUniqueId(), 0L), networkScores.getOrDefault(player.getUniqueId(), 0L)));
            case "server_scope" -> current == null ? "None" : (activeAllowedScopes.isEmpty()
                    ? current.allowedServerTypes() : activeAllowedScopes).stream()
                    .map(Enum::name).sorted().reduce((a, b) -> a + ", " + b).orElse("None");
            case "line_1" -> current == null ? "&eEvent: &cNone" : "&eEvent: &a" + current.displayName;
            case "line_2" -> current == null ? "&fNext: &b" + (next == null ? "None" : next.displayName)
                    : "&fCurrent: &e" + (player == null ? 0L : Math.max(progress.getOrDefault(player.getUniqueId(), 0L), networkScores.getOrDefault(player.getUniqueId(), 0L)));
            case "line_3" -> current == null ? "&fStarts In: &e" + formatDuration(Math.max(0L, nextAt - System.currentTimeMillis()))
                    : "&fEnds In: &e" + formatDuration(Math.max(0L, endAt - System.currentTimeMillis()));
            default -> "";
        };
    }

    private int progressPercent() {
        if (current == null) return 0;
        long total = progress.values().stream().mapToLong(Long::longValue).sum();
        long goal = switch (current) {
            case MINING_FIESTA -> 1_500L;
            case FISHING_FESTIVAL -> 500L;
            case FARMING_CONTEST -> 1_000L;
            case MOB_HUNT -> 750L;
            case BOSS_INVASION -> 10_000L;
            case DOUBLE_SKILL_XP -> 1_000L;
            case TREASURE_HUNT -> 180L;
            default -> 1_000L;
        };
        return Math.clamp((int) Math.round(total * 100.0D / goal), 0, 100);
    }

    public List<String> statusLines() {
        return statusLines(null);
    }

    public List<String> statusLines(Player player) {
        SmpEventType active = current;
        return List.of(
                "&eActive Event: &a" + placeholder(player, "name"),
                "&fState: &e" + placeholder(player, "state"),
                "&fServer Scope: &b" + placeholder(player, "server_scope"),
                "&fThis Server: &e" + serverId() + " &7(" + serverType().name() + ")",
                "&fCan Participate Here: " + (canParticipateOnThisServer(active) ? "&atrue" : "&cfalse"),
                "&fTime Left: &e" + placeholder(player, "time_left"),
                "&fPlayer Score: &e" + placeholder(player, "player_score"),
                "&fNext: &b" + placeholder(player, "next_name"),
                "&fStarts In: &e" + placeholder(player, "next_time")
        );
    }

    public List<String> debugLines(Player player) {
        return List.of(
                "&7server_id: &e" + serverId(),
                "&7server_type: &e" + serverType(),
                "&7active_event_id: &e" + (current == null ? "none" : current.id),
                "&7active_event_state: &e" + (current == null ? EventState.WAITING : state),
                "&7allowed_server_types: &e" + placeholder(player, "server_scope"),
                "&7safe_in_hub: &e" + (current != null && current.safeInHub()),
                "&7tablist_event_text: &e" + CC.color(placeholder(player, "line_1") + " &8| " + placeholder(player, "line_3")),
                "&7next_event: &e" + placeholder(player, "next_name"),
                "&7db_last_refresh: &e" + (dbLastRefresh == 0L ? "never" : new java.util.Date(dbLastRefresh)),
                "&7db_last_success: &e" + (dbLastSuccess == 0L ? "never" : new java.util.Date(dbLastSuccess))
        );
    }

    public NetworkValidationReport validateNetworkEvents() {
        int missingScopes = 0, destructiveHub = 0, invalidRequirements = 0;
        for (SmpEventType event : SmpEventType.values()) {
            if (event.allowedServerTypes().isEmpty()) missingScopes++;
            if (event.allowedServerTypes().contains(SMPServerType.HUB) && (!event.safeInHub()
                    || Set.of(EventCategory.BLOCK_BREAKING, EventCategory.BLOCK_PLACING, EventCategory.MINING,
                    EventCategory.FARMING, EventCategory.FORAGING).contains(event.category()))) destructiveHub++;
            if (event.requiresDungeon() && !event.allowedServerTypes().contains(SMPServerType.DUNGEON)) invalidRequirements++;
        }
        boolean placeholders = !placeholder(null, "state").isBlank() && !placeholder(null, "name").isBlank()
                && !placeholder(null, "name_colored").isBlank()
                && !placeholder(null, "time_left").isBlank() && !placeholder(null, "next_name").isBlank()
                && !placeholder(null, "next_time").isBlank() && !placeholder(null, "player_score").isBlank()
                && !placeholder(null, "server_scope").isBlank();
        boolean timerHealthy = current == null || endAt > startAt;
        boolean cacheLoaded = !dataManager.isMySqlEnabled() || dbLastSuccess > 0L;
        return new NetworkValidationReport(missingScopes, destructiveHub, invalidRequirements, placeholders,
                timerHealthy, cacheLoaded, dataManager.isMySqlEnabled(), true);
    }

    public List<CalendarEntry> calendarEntries() {
        long now = System.currentTimeMillis();
        List<CalendarEntry> entries = new ArrayList<>();
        if (current != null) {
            boolean active = state == EventState.ACTIVE;
            entries.add(calendarEntry(current, active ? "Active" : "Starting", active, startAt, endAt, now));
        }
        long scheduledAt = nextAt > now ? nextAt : now + randomDowntimePreviewMillis();
        if (next != null) {
            entries.add(calendarEntry(next, "Next", false, scheduledAt,
                    scheduledAt + next.durationMinutes * 60_000L, now));
            scheduledAt += next.durationMinutes * 60_000L + randomDowntimePreviewMillis();
        }
        for (SmpEventType type : upcoming) {
            if (!available(type)) continue;
            entries.add(calendarEntry(type, "Scheduled", false, scheduledAt,
                    scheduledAt + type.durationMinutes * 60_000L, now));
            scheduledAt += type.durationMinutes * 60_000L + randomDowntimePreviewMillis();
        }
        return List.copyOf(entries);
    }

    private CalendarEntry calendarEntry(SmpEventType type, String status, boolean active,
                                        long scheduledStart, long scheduledEnd, long now) {
        long countdown = Math.max(0L, (active ? scheduledEnd : scheduledStart) - now);
        return new CalendarEntry(type.id, type.displayName, type.icon, status, active,
                scheduledStart, scheduledEnd, type.durationMinutes + " minutes",
                CALENDAR_TIME.format(Instant.ofEpochMilli(scheduledStart).atZone(ZoneId.systemDefault())),
                formatDetailedDuration(countdown), formatMinecraftDays(countdown), type.description,
                type.bonusLines(), type.rewardLines(), type.participationLines());
    }

    private long randomDowntimePreviewMillis() {
        long min = Math.max(1L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("events.min-downtime-minutes", 30L));
        long max = Math.max(min, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("events.max-downtime-minutes", 60L));
        return ((min + max) / 2L) * 60_000L;
    }

    private String formatMinecraftDays(long millis) {
        double minutes = Math.max(1.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble("events.minecraft-day-real-minutes", 20.0D));
        double days = millis / (minutes * 60_000.0D);
        if (days < 0.05D) return "Today";
        return String.format(Locale.ROOT, "%.1f", days);
    }

    private String formatDetailedDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remaining = seconds % 60L;
        if (hours > 0L) return hours + "h " + minutes + "m " + remaining + "s";
        if (minutes > 0L) return minutes + "m " + remaining + "s";
        return remaining + "s";
    }

    public record CalendarEntry(String id, String name, Material icon, String status, boolean active,
                                long startAt, long endAt, String duration, String startTime,
                                String realTimeUntil, String minecraftDays, String description,
                                List<String> bonuses, List<String> rewards, List<String> participation) {
    }

    private record NetworkEventSnapshot(SmpEventType type, EventState state, long startsAt, long endsAt,
                                        Set<SMPServerType> scopes, String createdBy, long updatedAt,
                                        Map<UUID, Long> scores, SmpEventType nextType, long nextAt) {}

    public record NetworkValidationReport(int missingScopes, int destructiveHubEvents, int invalidRequirements,
                                          boolean placeholdersResolve, boolean timerHealthy, boolean cacheLoaded,
                                          boolean scoreDatabaseAvailable, boolean noWorldMutationOnHub) {}

    private String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remaining = seconds % 60L;
        if (hours > 0L) return hours + "h" + (minutes > 0L ? " " + minutes + "m" : "");
        if (minutes > 0L) return minutes + "m" + (remaining > 0L ? " " + remaining + "s" : "");
        return remaining + "s";
    }

    public enum EventState {
        WAITING, STARTING, ACTIVE, ENDING, FINISHED
    }

    public enum EventCategory {
        COMBAT, FISHING, BOSS_KILL, MOB_KILL, DUNGEON_BOSS, COMMUNITY,
        MINING, FARMING, FORAGING, BLOCK_BREAKING, BLOCK_PLACING, RESOURCE_GATHERING
    }

    public enum EventActionType {
        FISHING, MOB_KILL, BOSS_KILL, BOSS_DAMAGE, EXPLORATION, MINING, FARMING, FORAGING,
        DUNGEON_MOB, DUNGEON_BOSS, DUNGEON_CLEAR
    }

    public enum SmpEventType {
        MINING_FIESTA("mining_fiesta", "Mining Fiesta", Material.DIAMOND_PICKAXE, 20, "Mine ores and stone for boosted XP, Fortune, and materials.", "mining"),
        FISHING_FESTIVAL("fishing_festival", "Fishing Festival", Material.FISHING_ROD, 20, "Fish for boosted XP, sea creatures, treasure, and rare materials.", "fishing"),
        FARMING_CONTEST("farming_contest", "Farming Contest", Material.WHEAT, 15, "Harvest valid crops and compete for the highest score.", "farming"),
        MOB_HUNT("mob_hunt", "Mob Hunt", Material.IRON_SWORD, 20, "Defeat zone and dungeon mobs for bonus combat XP and materials.", "combat"),
        BOSS_INVASION("boss_invasion", "Boss Invasion", Material.NETHER_STAR, 30, "Contribute damage against an invading zone boss.", "combat"),
        DOUBLE_SKILL_XP("double_skill_xp", "Double Skill XP", Material.EXPERIENCE_BOTTLE, 15, "All skill XP is doubled for the event.", null),
        TREASURE_HUNT("treasure_hunt", "Treasure Hunt", Material.CHEST, 20, "Find limited treasure chests near active wilderness players.", null),
        WOODCUTTING_RUSH("woodcutting_rush", "Woodcutting Rush", Material.DIAMOND_AXE, 15, "Chop natural logs for boosted Foraging XP and materials.", "woodcutting"),
        CROP_FRENZY("crop_frenzy", "Crop Frenzy", Material.GOLDEN_HOE, 15, "Harvest crops for boosted Farming XP and crop materials.", "farming"),
        MONSTER_MAYHEM("monster_mayhem", "Monster Mayhem", Material.ROTTEN_FLESH, 20, "Defeat zone and dungeon monsters for combat materials.", "combat"),
        DUNGEON_SURGE("dungeon_surge", "Dungeon Surge", Material.SPAWNER, 20, "Dungeon enemies recover faster and drop more materials.", "combat"),
        BOSS_HOUR("boss_hour", "Boss Hour", Material.WITHER_SKELETON_SKULL, 30, "Hunt zone bosses for improved material rewards.", "combat"),
        SEA_CREATURE_SWARM("sea_creature_swarm", "Sea Creature Swarm", Material.NAUTILUS_SHELL, 20, "Fish during a surge of sea creatures.", "fishing"),
        TREASURE_TIDE("treasure_tide", "Treasure Tide", Material.HEART_OF_THE_SEA, 20, "Fishing treasure and rare materials are more common.", "fishing"),
        MINING_RUSH("mining_rush", "Mining Rush", Material.IRON_PICKAXE, 15, "Mine natural blocks with boosted XP and Fortune.", "mining"),
        GEMSTONE_GLOW("gemstone_glow", "Gemstone Glow", Material.AMETHYST_SHARD, 15, "Rare gems glow brighter in natural mining areas.", "mining"),
        SKILL_WEEKEND("skill_weekend", "Skill Weekend", Material.EXPERIENCE_BOTTLE, 30, "All normal skill activities grant twenty percent more XP.", null),
        LUCKY_DROPS("lucky_drops", "Lucky Drops", Material.RABBIT_FOOT, 20, "Magic Find and rare material chances are improved.", null),
        COIN_STORM("coin_storm", "Coin Storm", Material.GOLD_INGOT, 15, "Normal mob and activity coin rewards are increased.", null),
        RECIPE_HUNT("recipe_hunt", "Recipe Hunt", Material.PAPER, 20, "Find recipe fragments through normal SMP activities.", null),
        COLLECTION_BOOST("collection_boost", "Collection Boost", Material.BOOK, 20, "Progress collections through valid gathering activities.", null),
        PET_TRAINING("pet_training", "Pet Training", Material.BONE, 20, "Train pets and companions with boosted Taming XP.", "taming"),
        RIFT_INVASION("rift_invasion", "Rift Invasion", Material.ENDER_EYE, 25, "Rift energy empowers zone mobs and void material drops.", "combat"),
        FROSTFALL("frostfall", "Frostfall", Material.BLUE_ICE, 20, "Frost enemies and fishing rewards yield extra frozen materials.", null),
        INFERNAL_NIGHT("infernal_night", "Infernal Night", Material.BLAZE_POWDER, 20, "Fire and Nether monster materials are empowered.", "combat"),
        MERCHANT_DAY("merchant_day", "Merchant Day", Material.EMERALD, 15, "Guild merchants pay ten percent more for eligible goods.", null),
        EXPLORERS_CALL("explorers_call", "Explorer's Call", Material.COMPASS, 20, "Discover zones and explore the wilderness for rewards.", null);

        private final String id;
        private final String displayName;
        private final Material icon;
        private final int durationMinutes;
        private final String description;
        private final String rewardSkill;

        SmpEventType(String id, String displayName, Material icon, int durationMinutes, String description, String rewardSkill) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.durationMinutes = durationMinutes;
            this.description = description;
            this.rewardSkill = rewardSkill;
        }

        private List<String> bonusLines() {
            return switch (this) {
                case MINING_FIESTA -> List.of("&a+25% Mining XP", "&a+25 Mining Fortune", "&aIncreased rare mining materials");
                case FISHING_FESTIVAL -> List.of("&a+25% Fishing XP", "&a+10% Sea Creature Chance", "&a+5% Treasure Chance");
                case FARMING_CONTEST -> List.of("&aCrop harvest leaderboard", "&aMilestone rewards");
                case MOB_HUNT -> List.of("&a+25% Combat XP", "&aExtra dungeon materials");
                case BOSS_INVASION -> List.of("&aShared zone boss fight", "&aContribution scoring");
                case DOUBLE_SKILL_XP -> List.of("&a2x all Skill XP");
                case TREASURE_HUNT -> List.of("&aLimited wilderness treasures", "&aLoaded chunks only");
                case WOODCUTTING_RUSH -> List.of("&a+25% Foraging XP", "&aExtra foraging material chance");
                case CROP_FRENZY -> List.of("&a+25% Farming XP", "&aExtra crop material chance");
                case MONSTER_MAYHEM -> List.of("&a+25% Combat XP", "&aIncreased monster materials");
                case DUNGEON_SURGE -> List.of("&a25% faster dungeon respawns", "&aIncreased dungeon materials");
                case BOSS_HOUR -> List.of("&a25% faster boss respawns", "&aImproved boss materials");
                case SEA_CREATURE_SWARM -> List.of("&a+15% Sea Creature Chance", "&a+15% Fishing XP");
                case TREASURE_TIDE -> List.of("&a+10% Treasure Chance", "&aImproved fishing materials");
                case MINING_RUSH -> List.of("&a+25% Mining XP", "&a+25 Mining Fortune");
                case GEMSTONE_GLOW -> List.of("&a+15% Mining XP", "&aIncreased gemstone materials");
                case SKILL_WEEKEND -> List.of("&a+20% all Skill XP", "&aUses normal booster stacking rules");
                case LUCKY_DROPS -> List.of("&a+10 Magic Find", "&aSlightly improved rare materials");
                case COIN_STORM -> List.of("&a+15% activity coins", "&aDoes not affect player markets");
                case RECIPE_HUNT -> List.of("&aIncreased recipe fragments", "&aGathering, fishing, and mob hooks");
                case COLLECTION_BOOST -> List.of("&aExtra collection opportunities", "&aOnly valid natural activities count");
                case PET_TRAINING -> List.of("&a+25% Taming XP", "&aCompanion training progress");
                case RIFT_INVASION -> List.of("&aVoid material drops", "&aUses existing zone mob caps");
                case FROSTFALL -> List.of("&aImproved frost materials", "&aFishing and frost mob bonuses");
                case INFERNAL_NIGHT -> List.of("&aImproved fire materials", "&aBlaze and magma rewards");
                case MERCHANT_DAY -> List.of("&a+10% Guild sell prices", "&aPlayer markets are unchanged");
                case EXPLORERS_CALL -> List.of("&aZone discovery rewards", "&aWilderness exploration progress");
            };
        }

        private List<String> rewardLines() {
            String tokenRange = this == BOSS_INVASION ? "&7Token Reward: &e5-25 Event Tokens" : "&7Token Reward: &e1-20 Event Tokens";
            return List.of("&eCoins and Skill XP", "&eEvent Tokens", tokenRange, "&eCollection and recipe materials");
        }

        private List<String> participationLines() {
            return switch (this) {
                case MINING_FIESTA -> List.of("&7Break natural ores or stone.", "&7Player-placed blocks do not count.");
                case FISHING_FESTIVAL -> List.of("&7Catch fish with the existing fishing system.");
                case FARMING_CONTEST -> List.of("&7Harvest fully grown valid crops.");
                case MOB_HUNT -> List.of("&7Defeat tagged zone or dungeon mobs.");
                case BOSS_INVASION -> List.of("&7Damage the active invasion boss.");
                case DOUBLE_SKILL_XP -> List.of("&7Earn XP from normal skill activities.");
                case TREASURE_HUNT -> List.of("&7Right-click event treasure chests.");
                case WOODCUTTING_RUSH -> List.of("&7Chop natural logs.");
                case CROP_FRENZY -> List.of("&7Harvest fully-grown crops.");
                case MONSTER_MAYHEM, DUNGEON_SURGE, BOSS_HOUR, LUCKY_DROPS, RIFT_INVASION, FROSTFALL, INFERNAL_NIGHT -> List.of("&7Defeat tagged zone or dungeon mobs.");
                case SEA_CREATURE_SWARM, TREASURE_TIDE -> List.of("&7Catch fish through the fishing system.");
                case MINING_RUSH, GEMSTONE_GLOW -> List.of("&7Mine natural ore or stone.");
                case SKILL_WEEKEND -> List.of("&7Earn XP from normal skill activities.");
                case COIN_STORM -> List.of("&7Earn normal activity or mob coins.");
                case RECIPE_HUNT -> List.of("&7Gather, fish, or defeat tagged mobs.");
                case COLLECTION_BOOST -> List.of("&7Perform valid collection activities.");
                case PET_TRAINING -> List.of("&7Train an active companion.");
                case MERCHANT_DAY -> List.of("&7Sell eligible materials to Guild shops.");
                case EXPLORERS_CALL -> List.of("&7Discover new configured zones.");
            };
        }

        public static SmpEventType parse(String value) {
            if (value == null || value.isBlank()) return null;
            String normalized = value.toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            for (SmpEventType type : values()) if (type.id.equals(normalized) || type.name().equalsIgnoreCase(normalized)) return type;
            return null;
        }

        public EventCategory category() {
            return switch (this) {
                case MINING_FIESTA, MINING_RUSH, GEMSTONE_GLOW -> EventCategory.MINING;
                case FARMING_CONTEST, CROP_FRENZY -> EventCategory.FARMING;
                case WOODCUTTING_RUSH -> EventCategory.FORAGING;
                case FISHING_FESTIVAL, SEA_CREATURE_SWARM, TREASURE_TIDE -> EventCategory.FISHING;
                case BOSS_INVASION, BOSS_HOUR -> EventCategory.BOSS_KILL;
                case DUNGEON_SURGE -> EventCategory.DUNGEON_BOSS;
                case MOB_HUNT, MONSTER_MAYHEM, RIFT_INVASION, INFERNAL_NIGHT -> EventCategory.MOB_KILL;
                case MERCHANT_DAY, PET_TRAINING -> EventCategory.COMMUNITY;
                case TREASURE_HUNT -> EventCategory.BLOCK_BREAKING;
                case DOUBLE_SKILL_XP, SKILL_WEEKEND, LUCKY_DROPS, COIN_STORM, RECIPE_HUNT,
                     COLLECTION_BOOST, FROSTFALL, EXPLORERS_CALL -> EventCategory.RESOURCE_GATHERING;
            };
        }

        public boolean allowsDungeonParticipation() {
            return this == DUNGEON_SURGE || this == MOB_HUNT || this == MONSTER_MAYHEM
                    || this == BOSS_HOUR || this == BOSS_INVASION || this == RIFT_INVASION
                    || this == INFERNAL_NIGHT;
        }

        public Set<SMPServerType> allowedServerTypes() {
            EnumSet<SMPServerType> scopes = EnumSet.of(SMPServerType.OVERWORLD);
            switch (this) {
                case FISHING_FESTIVAL, SEA_CREATURE_SWARM, TREASURE_TIDE, DOUBLE_SKILL_XP,
                     SKILL_WEEKEND, LUCKY_DROPS, COIN_STORM, RECIPE_HUNT, COLLECTION_BOOST,
                     PET_TRAINING, EXPLORERS_CALL -> scopes.add(SMPServerType.HUB);
                case MOB_HUNT, MONSTER_MAYHEM, BOSS_HOUR, BOSS_INVASION, RIFT_INVASION,
                     FROSTFALL, INFERNAL_NIGHT -> {
                    scopes.add(SMPServerType.HUB);
                    scopes.add(SMPServerType.DUNGEON);
                }
                case DUNGEON_SURGE -> {
                    scopes.clear();
                    scopes.add(SMPServerType.DUNGEON);
                }
                default -> { }
            }
            if (Set.of(DOUBLE_SKILL_XP, SKILL_WEEKEND, LUCKY_DROPS, COIN_STORM,
                    RECIPE_HUNT, COLLECTION_BOOST, FROSTFALL).contains(this)) scopes.add(SMPServerType.DUNGEON);
            return Set.copyOf(scopes);
        }

        public boolean safeInHub() {
            return allowedServerTypes().contains(SMPServerType.HUB)
                    && category() != EventCategory.BLOCK_BREAKING
                    && category() != EventCategory.BLOCK_PLACING
                    && category() != EventCategory.MINING
                    && category() != EventCategory.FARMING
                    && category() != EventCategory.FORAGING;
        }

        public boolean requiresZone() {
            return Set.of(MOB_HUNT, MONSTER_MAYHEM, BOSS_INVASION, BOSS_HOUR,
                    RIFT_INVASION, INFERNAL_NIGHT, EXPLORERS_CALL).contains(this);
        }

        public boolean requiresDungeon() {
            return this == DUNGEON_SURGE;
        }

        public String scoreType() {
            return category().name();
        }

        public String eventId() { return id; }

        public String eventDisplayName() { return displayName; }

        public long durationSeconds() { return durationMinutes * 60L; }
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        private static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }
}
