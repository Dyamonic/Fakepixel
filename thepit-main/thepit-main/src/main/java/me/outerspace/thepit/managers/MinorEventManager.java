package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.PitGoldFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import me.outerspace.thepit.registry.definition.StoredLocation;
import org.bukkit.inventory.InventoryHolder;

public final class MinorEventManager implements Listener {
    private final ThePitPlugin plugin;
    private final Random random = new Random();
    private final AtomicBoolean answerDispatchQueued = new AtomicBoolean();
    private final Set<UUID> quickMathWinners = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingAnswers = ConcurrentHashMap.newKeySet();
    private volatile Integer quickMathAnswer;
    private volatile long quickMathEndsAt;
    private volatile boolean doubleRewards;
    private volatile long doubleRewardsEndsAt;
    private volatile int activeQuadrant;
    private BukkitTask ticker;
    private String activeEventId;
    private long activeEventEndsAt;
    private long nextScheduledAt;
    private final Map<UUID, Location[]> setupSelections = new ConcurrentHashMap<>();
    private final Map<BlockKey, SavedBlock> ragePitOriginalBlocks = new LinkedHashMap<>();
    private final Set<BlockKey> ragePitBoundary = new java.util.HashSet<>();
    private World ragePitWorld;
    private String ragePitArena;
    private BukkitTask ragePitPlacementTask;
    private boolean ragePitPreparing;
    private final Map<String, Map<BlockKey, SavedBlock>> ragePitPreviews = new LinkedHashMap<>();
    private final Map<String, BukkitTask> ragePitPreviewTasks = new LinkedHashMap<>();
    private GiantCakeSession giantCake;
    private GiantCakeState giantCakeState = GiantCakeState.IDLE;
    private String giantCakeLastFailure = "NONE";

    private static final Set<String> MINOR_IDS = Set.of("double_rewards", "everyone_gets_a_bounty", "king_of_the_hill",
            "dragon_egg", "king_of_the_ladder", "giant_cake");
    private static final Set<String> MAJOR_IDS = Set.of("rage_pit", "team_deathmatch", "beast", "raffle", "robbery",
            "pizza", "squads", "blockhead", "spire");
    private static final Set<String> LOCATION_EVENTS = Set.of("king_of_the_hill", "dragon_egg", "king_of_the_ladder",
            "giant_cake", "rage_pit", "team_deathmatch", "raffle", "pizza", "blockhead", "spire");

    public MinorEventManager(ThePitPlugin plugin) { this.plugin = plugin; }

    public void start() {
        if (ticker != null) return;
        scheduleNext();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        cleanupGiantCake(null, GiantCakeState.COMPLETED);
        cleanupRagePit();
        for (String arena : new ArrayList<>(ragePitPreviews.keySet())) clearRagePitPreview(arena);
        if (ticker != null) ticker.cancel();
        ticker = null;
        quickMathAnswer = null;
        quickMathWinners.clear();
        pendingAnswers.clear();
        setupSelections.clear();
        doubleRewards = false;
        activeEventId = null;
        activeEventEndsAt = 0L;
    }

    public boolean startQuickMaths() {
        if (quickMathAnswer != null) return false;
        int a = 1 + random.nextInt(20);
        int b = 1 + random.nextInt(20);
        String symbol;
        int answer;
        switch (random.nextInt(3)) {
            case 0 -> { symbol = "+"; answer = a + b; }
            case 1 -> { if (b > a) { int swap = a; a = b; b = swap; } symbol = "-"; answer = a - b; }
            default -> { a = 2 + random.nextInt(10); b = 2 + random.nextInt(10); symbol = "×"; answer = a * b; }
        }
        quickMathAnswer = answer;
        quickMathEndsAt = System.currentTimeMillis() + 30000L;
        quickMathWinners.clear();
        pendingAnswers.clear();
        Bukkit.broadcastMessage(CC.color("&e&lQUICK MATHS! &7Solve: &b" + a + " " + symbol + " " + b));
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Integer expected = quickMathAnswer;
        if (expected == null || System.currentTimeMillis() >= quickMathEndsAt) return;
        String input = event.getMessage() == null ? "" : event.getMessage().trim();
        if (!input.matches("-?\\d{1,9}")) return;
        int answer;
        try { answer = Integer.parseInt(input); } catch (NumberFormatException ignored) { return; }
        if (answer != expected || quickMathWinners.contains(event.getPlayer().getUniqueId())) return;
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pendingAnswers.add(uuid)) return;
        Bukkit.getScheduler().runTask(plugin, () -> acceptAnswer(uuid, expected));
    }

    private void acceptAnswer(UUID uuid, int expected) {
        pendingAnswers.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline() || quickMathAnswer == null || quickMathAnswer != expected
                || quickMathWinners.size() >= 5 || !quickMathWinners.add(uuid)) return;
        int position = quickMathWinners.size();
        plugin.getStatsManager().addXp(player, 250, StatsManager.XpSource.EVENT);
        plugin.getPitGoldService().award(player, 500, ThePitPlugin.GoldSource.EVENT);
        if (plugin.getProgressManager() != null) {
            String key = "quick-maths:" + quickMathEndsAt + ":" + uuid;
            ProgressManager.ProgressContext context = new ProgressManager.ProgressContext("quick_maths", key,
                    java.util.Map.of("position", position));
            plugin.getProgressManager().recordProgress(player, ProgressManager.ObjectiveType.QUICK_MATHS_CORRECT, "quick_maths", 1, context);
            plugin.getProgressManager().recordProgress(player, ProgressManager.ObjectiveType.MINOR_EVENT_PARTICIPATION, "quick_maths", 1, context);
            plugin.getProgressManager().recordProgress(player, ProgressManager.ObjectiveType.MINOR_EVENT_WIN, "quick_maths", 1, context);
        }
        Bukkit.broadcastMessage(CC.color("&a" + plugin.getNetworkPrefix(player) + player.getName()
                + " &7answered correctly! &8(&e" + position + "&7/5&8)"));
        player.sendMessage(CC.color("&8+&b250 XP"));
        player.sendMessage(CC.color("&8+" + PitGoldFormatter.formatPitGold(500)));
        if (quickMathWinners.size() >= 5) endQuickMaths();
    }

    public boolean startDoubleRewards() {
        if (activeEventId != null || doubleRewards) return false;
        doubleRewards = true;
        activeEventId = "double_rewards";
        activeEventEndsAt = doubleRewardsEndsAt = System.currentTimeMillis() + durationSeconds("double_rewards", 240) * 1000L;
        Bukkit.broadcastMessage(CC.color("&e&lMINOR EVENT! &f2x Rewards"));
        Bukkit.broadcastMessage(CC.color("&7Earn double &bPit XP &7and &6Pit Gold &7from kills and assists!"));
        return true;
    }

    public boolean isDoubleRewardsActive() { return doubleRewards && System.currentTimeMillis() < doubleRewardsEndsAt; }

    public boolean isInsideDoubleRewardsZone(Location location) {
        return isDoubleRewardsActive() && location != null && plugin.getArenaManager().isInCurrentArena(location);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (quickMathAnswer != null && now >= quickMathEndsAt) endQuickMaths();
        if (doubleRewards && now >= doubleRewardsEndsAt) {
            doubleRewards = false;
            activeEventId = null;
            activeEventEndsAt = 0L;
            Bukkit.broadcastMessage(CC.color("&c2x Rewards has ended."));
            scheduleNext();
        }
        if (activeEventId != null && now >= activeEventEndsAt) stopEvent(activeEventId);
        if (activeEventId == null && now >= nextScheduledAt) startScheduled();
    }

    private void particles() {
        Location center = plugin.getArenaManager().getCurrentSpawn();
        if (center == null || center.getWorld() == null) return;
        for (int i = 0; i < 10; i++) {
            double x = center.getX() + (random.nextDouble() * 36.0D + 8.0D) * ((activeQuadrant == 1 || activeQuadrant == 2) ? -1 : 1);
            double z = center.getZ() + (random.nextDouble() * 36.0D + 8.0D) * ((activeQuadrant == 0 || activeQuadrant == 1) ? -1 : 1);
            center.getWorld().playEffect(new Location(center.getWorld(), x, center.getY() + 1.0D, z), Effect.HAPPY_VILLAGER, 0);
        }
    }

    private void endQuickMaths() {
        quickMathAnswer = null;
        quickMathWinners.clear();
        pendingAnswers.clear();
    }

    private String quadrantName(int value) {
        return switch (value) { case 0 -> "Northeast"; case 1 -> "Northwest"; case 2 -> "Southwest"; default -> "Southeast"; };
    }

    public Set<String> eventIds() {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        ids.addAll(MINOR_IDS);
        ids.addAll(MAJOR_IDS);
        return Collections.unmodifiableSet(ids);
    }

    public String activeEventId() { return activeEventId; }

    public long remainingSeconds() { return activeEventId == null ? 0L : Math.max(0L, (activeEventEndsAt - System.currentTimeMillis() + 999L) / 1000L); }

    public String statusLine() {
        return activeEventId == null ? "" : "&e" + displayName(activeEventId) + ": &f" + formatTime(remainingSeconds());
    }

    public String validationFailure(String requestedId) {
        String id = normalizeId(requestedId);
        if (!eventIds().contains(id)) return "unknown stable event ID";
        if (!plugin.registry().settings().getBoolean("events." + id + ".enabled", true)) return "disabled in settings.yml";
        String arena = plugin.getArenaManager().getCurrentArena();
        me.outerspace.thepit.registry.definition.ArenaDefinition definition = plugin.getYamlManager().arena(arena).orElse(null);
        if (definition == null) return "active arena is not defined in arenas.yml";
        if (!definition.enabled()) return "active arena is disabled";
        World runtimeWorld = plugin.getArenaManager().getCurrentWorld();
        if (runtimeWorld == null) return "active arena runtime world is not loaded";
        if (LOCATION_EVENTS.contains(id)) {
            if (id.equals("rage_pit")) {
                PitYamlManager.RagePitDefinition ragePit = plugin.getYamlManager().ragePit(arena);
                if (ragePit == null || !ragePit.valid()) return ragePit == null ? "missing YAML path arenas." + arena + ".rage-pit" : ragePit.error();
            } else {
            Map<String, me.outerspace.thepit.registry.definition.StoredLocation> locations = plugin.getYamlManager().eventLocations(arena, id);
            int minimum = Math.max(1, plugin.registry().settings().getInt("events." + id + ".minimum-locations", defaultMinimumLocations(id)));
            long valid = locations.size();
            if (valid < minimum) return "requires " + minimum + " valid arena YAML location(s), found " + valid;
            }
        }
        int players = eligiblePlayers().size();
        int minimumPlayers = Math.max(0, plugin.registry().settings().getInt("events." + id + ".minimum-players", MAJOR_IDS.contains(id) ? 2 : 1));
        if (players < minimumPlayers) return "requires " + minimumPlayers + " eligible player(s), found " + players;
        return "";
    }

    public String startEvent(String requestedId) {
        String id = normalizeId(requestedId);
        if (ragePitPreparing) return CC.color("&cRage Pit boundary preparation is already active.");
        if (activeEventId != null) return CC.color("&c" + displayName(activeEventId) + " is already active.");
        if (plugin.getMapRotationManager() != null && plugin.getMapRotationManager().isSwitching()) return CC.color("&cAn arena rotation is in progress.");
        String failure = validationFailure(id);
        if (!failure.isEmpty()) return CC.color("&cCannot start " + id + ": " + failure + ".");
        if (id.equals("double_rewards")) return startDoubleRewards() ? CC.color("&aStarted double_rewards.") : CC.color("&cCould not start double_rewards.");
        if (id.equals("rage_pit")) {
            String result = startRagePit();
            if (!result.isEmpty()) return CC.color("&cCannot start rage_pit: " + result + ".");
            return CC.color("&aPreparing &erage_pit&a boundary.");
        }
        if (id.equals("giant_cake")) {
            String result = startGiantCake();
            if (!result.isEmpty()) return CC.color("&cGiant Cake could not start: " + result + ".");
        }
        activeEventId = id;
        activeEventEndsAt = System.currentTimeMillis() + durationSeconds(id, MAJOR_IDS.contains(id) ? 300 : 180) * 1000L;
        if (id.equals("everyone_gets_a_bounty")) assignBounties();
        Bukkit.broadcastMessage(CC.color("&" + (MAJOR_IDS.contains(id) ? "6&lMAJOR" : "e&lMINOR") + " EVENT! &f" + displayName(id)));
        if (id.equals("everyone_gets_a_bounty")) Bukkit.broadcastMessage(CC.color("&7Every eligible player now has a bounty!"));
        return CC.color("&aStarted &e" + id + "&a.");
    }

    public String stopEvent(String requestedId) {
        String id = normalizeId(requestedId);
        if (activeEventId == null && ragePitPreparing) {
            cleanupRagePit();
            scheduleNext();
            return CC.color("&aStopped Rage Pit boundary preparation.");
        }
        if (activeEventId == null) return CC.color("&cNo managed event is active.");
        if (!id.isEmpty() && !id.equals(activeEventId)) return CC.color("&c" + id + " is not active.");
        String stopped = activeEventId;
        if (stopped.equals("rage_pit")) cleanupRagePit();
        if (stopped.equals("giant_cake")) cleanupGiantCake(giantCake == null ? null : giantCake.sessionId, GiantCakeState.COMPLETED);
        activeEventId = null;
        activeEventEndsAt = 0L;
        doubleRewards = false;
        quickMathWinners.clear();
        pendingAnswers.clear();
        scheduleNext();
        Bukkit.broadcastMessage(CC.color("&c" + displayName(stopped) + " has ended."));
        return CC.color("&aStopped &e" + stopped + "&a.");
    }

    private void assignBounties() {
        int minimum = Math.max(1, plugin.registry().settings().getInt("events.everyone_gets_a_bounty.bounty-min", 100));
        int maximum = Math.max(minimum, plugin.registry().settings().getInt("events.everyone_gets_a_bounty.bounty-max", 1000));
        for (Player player : eligiblePlayers()) {
            int value = minimum + random.nextInt(maximum - minimum + 1);
            plugin.getStatsManager().setBountyIfHigher(player, value);
        }
    }

    private List<Player> eligiblePlayers() {
        List<Player> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getArenaManager().isInCurrentArena(player.getLocation()) || player.getGameMode() == GameMode.SPECTATOR
                    || player.hasMetadata("NPC") || player.hasMetadata("vanished") || player.hasMetadata("afk")) continue;
            if (plugin.getArenaManager().isInCurrentSpawnProtection(player.getLocation())) continue;
            result.add(player);
        }
        return result;
    }

    private void startScheduled() {
        if (!plugin.registry().settings().getBoolean("events.schedule.enabled", true)) { scheduleNext(); return; }
        List<String> candidates = new ArrayList<>();
        for (String id : eventIds()) if (validationFailure(id).isEmpty()) candidates.add(id);
        if (!candidates.isEmpty()) {
            String result = startEvent(candidates.get(random.nextInt(candidates.size())));
            if (activeEventId == null) {
                plugin.getLogger().warning("Scheduled event start was skipped: " + org.bukkit.ChatColor.stripColor(result));
                scheduleNext();
            }
        } else scheduleNext();
    }

    private void scheduleNext() {
        long minimum = Math.max(30L, plugin.registry().settings().getLong("events.schedule.minimum-delay-seconds", 600L));
        long maximum = Math.max(minimum, plugin.registry().settings().getLong("events.schedule.maximum-delay-seconds", 1200L));
        nextScheduledAt = System.currentTimeMillis() + (minimum + (maximum == minimum ? 0L : Math.abs(random.nextLong()) % (maximum - minimum + 1L))) * 1000L;
    }

    private int durationSeconds(String id, int fallback) { return Math.max(1, plugin.registry().settings().getInt("events." + id + ".duration-seconds", fallback)); }
    private String normalizeId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH).replace('-', '_');
        return normalized.equals("giantcake") ? "giant_cake" : normalized;
    }
    private int defaultMinimumLocations(String id) { return id.equals("team_deathmatch") ? 2 : id.equals("spire") ? 9 : 1; }
    private String displayName(String id) {
        String[] parts = id.split("_");
        StringBuilder value = new StringBuilder();
        for (String part : parts) value.append(value.length() == 0 ? "" : " ").append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        return value.toString();
    }
    private String formatTime(long seconds) { return String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L); }

    public List<String> eventList() {
        List<String> lines = new ArrayList<>();
        lines.add(CC.color("&6The Pit events"));
        for (String id : eventIds()) {
            String failure = validationFailure(id);
            lines.add(CC.color((failure.isEmpty() ? "&a" : "&c") + id + (failure.isEmpty() ? " &7ready" : " &7" + failure)));
        }
        return lines;
    }

    public List<String> debugEvent(String requestedId) {
        String id = normalizeId(requestedId);
        if (!id.equals("giant_cake")) return Collections.singletonList(CC.color("&cNo detailed diagnostics are registered for " + id + "."));
        String arena = plugin.getArenaManager().getCurrentArena();
        World world = plugin.getArenaManager().getCurrentWorld();
        GiantCakeSession session = giantCake;
        List<String> lines = new ArrayList<>();
        lines.add(CC.color("&eGiant Cake diagnostics"));
        lines.add(CC.color("&7Registered: &f" + eventIds().contains(id)));
        lines.add(CC.color("&7Active arena: &f" + arena));
        lines.add(CC.color("&7Runtime world ready: &f" + (world != null)));
        lines.add(CC.color("&7Configured locations: &f" + plugin.getYamlManager().eventLocations(arena, id).size()));
        lines.add(CC.color("&7Selected location: &f" + (session == null ? "none" : session.locationId)));
        lines.add(CC.color("&7Structure validation: &f" + (validationFailure(id).isEmpty() ? "ready" : validationFailure(id))));
        lines.add(CC.color("&7State: &f" + giantCakeState));
        lines.add(CC.color("&7Blocks placed: &f" + (session == null ? 0 : session.placed)));
        lines.add(CC.color("&7Remaining slices: &f" + (session == null ? 0 : session.slices.size() - session.consumed.size())));
        lines.add(CC.color("&7Hologram: &f" + (session != null && session.hologramId != null)));
        lines.add(CC.color("&7Session: &f" + (session == null ? "none" : session.sessionId)));
        lines.add(CC.color("&7Last failure: &f" + giantCakeLastFailure));
        return lines;
    }

    public List<String> setupCommand(Player player, String arenaValue, String eventValue, String actionValue, String locationValue) {
        List<String> response = new ArrayList<>();
        if (player == null || !me.outerspace.thepit.util.PermissionCompat.has(plugin, player, "thepit.admin.eventsetup")) return Collections.singletonList(CC.color("&cNo permission."));
        String arena = PitYamlManager.normalizeArenaId(arenaValue);
        String event = normalizeId(eventValue);
        String action = actionValue == null ? "" : actionValue.toLowerCase(Locale.ENGLISH);
        if (!plugin.getYamlManager().containsArena(arena)) return Collections.singletonList(CC.color("&cUnknown arena: " + arena + "."));
        if (!eventIds().contains(event)) return Collections.singletonList(CC.color("&cUnknown stable event ID: " + event + "."));
        World runtimeWorld = plugin.getArenaManager().getCurrentWorld();
        if (!arena.equals(plugin.getArenaManager().getCurrentArena()) || runtimeWorld == null || player.getWorld() != runtimeWorld) return Collections.singletonList(CC.color("&cYou must be in the active runtime instance of arena " + arena + "."));
        if (action.equals("listlocations")) {
            response.add(CC.color("&e" + arena + " &7/ &e" + event + " &7locations:"));
            Map<String, StoredLocation> values = plugin.getYamlManager().eventLocations(arena, event);
            if (values.isEmpty()) response.add(CC.color("&7None configured."));
            for (Map.Entry<String, StoredLocation> entry : values.entrySet()) response.add(CC.color("&e" + entry.getKey() + " &7" + entry.getValue().x() + ", " + entry.getValue().y() + ", " + entry.getValue().z()));
            return response;
        }
        if (action.equals("pos1") || action.equals("pos2")) {
            Location[] values = setupSelections.computeIfAbsent(player.getUniqueId(), ignored -> new Location[2]);
            values[action.equals("pos1") ? 0 : 1] = player.getLocation().clone();
            return Collections.singletonList(CC.color("&aSelected " + action + " for &e" + arena + "&a / &e" + event + "&a."));
        }
        if (action.equals("validate")) {
            String failure = validationFailureForArena(arena, event);
            return Collections.singletonList(CC.color(failure.isEmpty() ? "&a" + arena + " / " + event + " is valid." : "&c" + arena + " / " + event + ": " + failure + "."));
        }
        if (action.equals("test")) {
            if (!arena.equals(plugin.getArenaManager().getCurrentArena())) return Collections.singletonList(CC.color("&cTests may only run for the active arena."));
            return Collections.singletonList(startEvent(event));
        }
        String location = PitYamlManager.normalizeStableId(locationValue);
        if ((action.equals("setlocation") || action.equals("removelocation")) && location.isEmpty()) return Collections.singletonList(CC.color("&cA stable location ID is required."));
        if (action.equals("setlocation")) {
            boolean saved = plugin.getYamlManager().saveEventLocation(arena, event, location, player.getLocation());
            return Collections.singletonList(CC.color(saved ? "&aSaved &earenas." + arena + ".events." + event.replace('_', '-') + ".locations." + location + " &ain arenas.yml." : "&cCould not save " + arena + " / " + event + " / " + location + "."));
        }
        if (action.equals("removelocation")) {
            boolean removed = plugin.getYamlManager().removeEventLocation(arena, event, location);
            return Collections.singletonList(CC.color(removed ? "&aRemoved &e" + arena + "&a / &e" + event + "&a / &e" + location + "&a from arenas.yml." : "&cNo matching location was removed."));
        }
        return Collections.singletonList(CC.color("&c/eventsetup <arena> <event-id> <setlocation|removelocation|listlocations|pos1|pos2|validate|test> [location-id]"));
    }

    private String validationFailureForArena(String arena, String id) {
        me.outerspace.thepit.registry.definition.ArenaDefinition definition = plugin.getYamlManager().arena(arena).orElse(null);
        if (definition == null) return "arena is missing";
        if (!definition.enabled()) return "arena is disabled";
        if (!LOCATION_EVENTS.contains(id)) return "";
        if (id.equals("rage_pit")) {
            PitYamlManager.RagePitDefinition ragePit = plugin.getYamlManager().ragePit(arena);
            return ragePit == null ? "missing YAML path arenas." + arena + ".rage-pit" : ragePit.error();
        }
        int minimum = Math.max(1, plugin.registry().settings().getInt("events." + id + ".minimum-locations", defaultMinimumLocations(id)));
        long valid = plugin.getYamlManager().eventLocations(arena, id).size();
        return valid < minimum ? "requires " + minimum + " arena YAML location(s), found " + valid : "";
    }

    private String startGiantCake() {
        giantCakeState = GiantCakeState.VALIDATING;
        String arenaId = plugin.getArenaManager().getCurrentArena();
        World world = plugin.getArenaManager().getCurrentWorld();
        if (arenaId == null || arenaId.isEmpty()) return failGiantCake("NO_ACTIVE_ARENA", "there is no active arena");
        if (world == null) return failGiantCake("RUNTIME_WORLD_UNAVAILABLE", "the active arena runtime world is not loaded");
        if (!Bukkit.isPrimaryThread()) return failGiantCake("NOT_MAIN_THREAD", "structure generation was requested off the main thread");
        Map<String, StoredLocation> locations = plugin.getYamlManager().eventLocations(arenaId, "giant_cake");
        if (locations.isEmpty()) return failGiantCake("NO_LOCATION", "no locations exist at arenas." + arenaId + ".events.giant-cake.locations");
        List<Map.Entry<String, StoredLocation>> choices = new ArrayList<>(locations.entrySet());
        Collections.shuffle(choices, random);
        Map.Entry<String, StoredLocation> selected = null;
        Location origin = null;
        Map<BlockKey, CakeBlock> structure = Collections.emptyMap();
        String invalid = "no configured location is valid";
        for (Map.Entry<String, StoredLocation> choice : choices) {
            Location candidate = new Location(world, choice.getValue().x(), choice.getValue().y(), choice.getValue().z(), choice.getValue().yaw(), choice.getValue().pitch());
            Map<BlockKey, CakeBlock> candidateStructure = cakeStructure(candidate);
            String candidateFailure = validateCakeStructure(world, candidateStructure);
            if (!candidateFailure.isEmpty()) {
                invalid = choice.getKey() + ": " + candidateFailure;
                continue;
            }
            selected = choice;
            origin = candidate;
            structure = candidateStructure;
            break;
        }
        if (selected == null) return failGiantCake("INVALID_STRUCTURE", invalid);
        cleanupGiantCake(null, GiantCakeState.COMPLETED);
        GiantCakeSession session = new GiantCakeSession(UUID.randomUUID(), arenaId, world, selected.getKey(), origin.getBlock().getLocation());
        giantCake = session;
        giantCakeState = GiantCakeState.GENERATING;
        try {
            for (Map.Entry<BlockKey, CakeBlock> entry : structure.entrySet()) {
                BlockKey key = entry.getKey();
                Block block = world.getBlockAt(key.x(), key.y(), key.z());
                session.originals.putIfAbsent(key, new SavedBlock(block.getType(), block.getData()));
                CakeBlock replacement = entry.getValue();
                block.setType(replacement.material());
                block.setData(replacement.data());
                session.placed++;
                if (replacement.slice()) session.slices.add(key);
            }
            if (plugin.getNpcService() == null) throw new IllegalStateException("packet hologram service is unavailable");
            session.hologramId = plugin.getNpcService().createTransientHologram(session.origin.clone().add(0.5D, 4.4D, 0.5D), cakeHologram(session));
            if (session.hologramId == null) throw new IllegalStateException("packet hologram creation failed");
            session.state = GiantCakeState.ACTIVE;
            giantCakeState = GiantCakeState.ACTIVE;
            giantCakeLastFailure = "NONE";
            plugin.getLogger().info("Giant Cake activated session=" + session.sessionId + " arena=" + arenaId + " instance=" + world.getUID()
                    + " world=" + world.getName() + " location=" + selected.getKey() + " origin=" + session.origin.getBlockX() + "," + session.origin.getBlockY() + "," + session.origin.getBlockZ()
                    + " definition=arenas." + arenaId + ".events.giant-cake.locations." + selected.getKey() + " blocks=" + structure.size() + " placed=" + session.placed + " hologram=true state=ACTIVE");
            return "";
        } catch (Throwable failure) {
            plugin.getLogger().log(Level.SEVERE, "Giant Cake failed stage=GENERATING code=GENERATION_FAILED session=" + session.sessionId + " arena=" + arenaId, failure);
            cleanupGiantCake(session.sessionId, GiantCakeState.FAILED);
            giantCakeLastFailure = "GENERATION_FAILED";
            return "structure generation failed and partial blocks were restored";
        }
    }

    private Map<BlockKey, CakeBlock> cakeStructure(Location origin) {
        Map<BlockKey, CakeBlock> blocks = new LinkedHashMap<>();
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) blocks.put(new BlockKey(ox + x, oy, oz + z), new CakeBlock(Material.QUARTZ_BLOCK, (byte) 0, false));
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) blocks.put(new BlockKey(ox + x, oy + 1, oz + z), new CakeBlock(Material.WOOL, (byte) ((Math.abs(x) == 2 || Math.abs(z) == 2) ? 14 : 0), false));
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) blocks.put(new BlockKey(ox + x, oy + 2, oz + z), new CakeBlock(Material.CAKE_BLOCK, (byte) 0, true));
        return blocks;
    }

    private String validateCakeStructure(World world, Map<BlockKey, CakeBlock> structure) {
        if (structure.isEmpty()) return "the generated structure is empty";
        for (BlockKey key : structure.keySet()) {
            if (!world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) return "required chunk " + (key.x() >> 4) + "," + (key.z() >> 4) + " is not loaded";
            Location location = new Location(world, key.x(), key.y(), key.z());
            if (!plugin.getArenaManager().isInCurrentPvpArena(location)) return "structure position " + key.x() + " " + key.y() + " " + key.z() + " is outside the combat arena";
            if (plugin.getArenaManager().isInCurrentSpawnProtection(location)) return "structure overlaps spawn protection";
            if (plugin.getGameListener() != null && plugin.getGameListener().isProtectedFeatureLocation(location)) return "structure overlaps a protected arena feature";
            Block block = world.getBlockAt(key.x(), key.y(), key.z());
            if (block.getState() instanceof InventoryHolder || protectedMaterial(block.getType())) return "structure would overwrite protected block " + block.getType().name();
            if (plugin.getGameListener() != null && plugin.getGameListener().isPlayerPlaced(block)) return "structure overlaps another temporary block";
        }
        return "";
    }

    private boolean protectedMaterial(Material material) {
        return material == Material.BEDROCK || material == Material.PORTAL || material == Material.ENDER_PORTAL
                || material == Material.CHEST || material == Material.TRAPPED_CHEST || material == Material.FURNACE
                || material == Material.BURNING_FURNACE || material == Material.DISPENSER || material == Material.DROPPER
                || material == Material.HOPPER || material == Material.MOB_SPAWNER || material == Material.ENCHANTMENT_TABLE
                || material == Material.ENDER_CHEST || material == Material.ANVIL;
    }

    private String failGiantCake(String code, String reason) {
        giantCakeState = GiantCakeState.FAILED;
        giantCakeLastFailure = code;
        plugin.getLogger().warning("Giant Cake failed stage=VALIDATING code=" + code + " reason=" + reason);
        return reason;
    }

    private List<String> cakeHologram(GiantCakeSession session) {
        return List.of("&e&lGIANT CAKE", "&f" + (session.slices.size() - session.consumed.size()) + " &7slices remaining", "&eClick to eat!");
    }

    private void cleanupGiantCake(UUID expectedSession, GiantCakeState finalState) {
        GiantCakeSession session = giantCake;
        if (session == null || expectedSession != null && !expectedSession.equals(session.sessionId)) return;
        session.state = GiantCakeState.CLEANING;
        giantCakeState = GiantCakeState.CLEANING;
        if (plugin.getNpcService() != null) plugin.getNpcService().removeTransientHologram(session.hologramId);
        boolean restored = true;
        for (Map.Entry<BlockKey, SavedBlock> entry : new ArrayList<>(session.originals.entrySet())) {
            try {
                BlockKey key = entry.getKey();
                Block block = session.world.getBlockAt(key.x(), key.y(), key.z());
                block.setType(entry.getValue().material());
                block.setData(entry.getValue().data());
            } catch (Throwable failure) {
                restored = false;
                plugin.getLogger().log(Level.SEVERE, "Giant Cake restoration failed session=" + session.sessionId + " arena=" + session.arenaId, failure);
            }
        }
        session.originals.clear();
        session.slices.clear();
        session.consumed.clear();
        session.clickCooldowns.clear();
        session.participation.clear();
        giantCake = null;
        giantCakeState = finalState;
        plugin.getLogger().info("Giant Cake cleanup session=" + session.sessionId + " arena=" + session.arenaId + " restored=" + restored + " state=" + finalState);
    }

    private String startRagePit() {
        String arenaId = plugin.getArenaManager().getCurrentArena();
        me.outerspace.thepit.registry.definition.ArenaDefinition arena = plugin.getYamlManager().arena(arenaId).orElse(null);
        if (arena == null) return "unknown active arena ID '" + arenaId + "'";
        World world = Bukkit.getWorld(arena.worldName());
        if (world == null) return "configured world '" + arena.worldName() + "' is not loaded";
        PitYamlManager.RagePitDefinition definition = plugin.getYamlManager().ragePit(arenaId);
        if (definition == null) return "missing YAML path arenas." + arenaId + ".rage-pit";
        if (!definition.valid()) return definition.error();
        if (!world.getName().equals(definition.worldName())) return "active world does not match arenas." + arenaId + ".rage-pit world";
        String validation = validateRagePit(arenaId);
        if (validation.startsWith("§c")) return org.bukkit.ChatColor.stripColor(validation);
        if (ragePitPreparing) return "a Rage Pit boundary is already being prepared";
        clearRagePitPreview(arenaId);
        cleanupRagePit();
        ragePitArena = arenaId;
        ragePitWorld = world;
        ragePitPreparing = true;
        List<PitYamlManager.RagePitBlock> boundary = definition.boundary();
        for (PitYamlManager.RagePitBlock coordinate : boundary) ragePitBoundary.add(new BlockKey(coordinate.x(), coordinate.y(), coordinate.z()));
        int batch = Math.max(50, plugin.registry().settings().getInt("events.rage_pit.wall-placement-batch-size", 500));
        final int[] cursor = {0};
        ragePitPlacementTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                if (!ragePitPreparing || !arenaId.equals(plugin.getArenaManager().getCurrentArena()) || world != plugin.getArenaManager().getCurrentWorld()) {
                    cleanupRagePit();
                    return;
                }
                int end = Math.min(boundary.size(), cursor[0] + batch);
                for (; cursor[0] < end; cursor[0]++) {
                    PitYamlManager.RagePitBlock coordinate = boundary.get(cursor[0]);
                    if (!world.isChunkLoaded(coordinate.x() >> 4, coordinate.z() >> 4)) throw new IllegalStateException("boundary chunk is unloaded at " + (coordinate.x() >> 4) + "," + (coordinate.z() >> 4));
                    Block block = world.getBlockAt(coordinate.x(), coordinate.y(), coordinate.z());
                    if (block.getType().isSolid()) continue;
                    if (!ragePitReplaceable(block.getType())) throw new IllegalStateException("non-replaceable boundary gap at " + coordinate.x() + "," + coordinate.y() + "," + coordinate.z() + " material=" + block.getType());
                    BlockKey key = new BlockKey(coordinate.x(), coordinate.y(), coordinate.z());
                    ragePitOriginalBlocks.putIfAbsent(key, new SavedBlock(block.getType(), block.getData()));
                    block.setType(definition.material());
                    block.setData(definition.data());
                }
                if (cursor[0] < boundary.size()) return;
                ragePitPlacementTask.cancel();
                ragePitPlacementTask = null;
                ragePitPreparing = false;
                activeEventId = "rage_pit";
                activeEventEndsAt = System.currentTimeMillis() + durationSeconds("rage_pit", 300) * 1000L;
                Bukkit.broadcastMessage(CC.color("&6&lMAJOR EVENT! &f" + displayName("rage_pit")));
            } catch (Throwable failure) {
                plugin.getLogger().log(Level.SEVERE, "Rage Pit boundary placement failed for arena '" + arenaId + "'", failure);
                cleanupRagePit();
            }
        }, 1L, 1L);
        return "";
    }

    private void cleanupRagePit() {
        ragePitPreparing = false;
        if (ragePitPlacementTask != null) ragePitPlacementTask.cancel();
        ragePitPlacementTask = null;
        World world = ragePitWorld;
        if (world != null) {
            for (Map.Entry<BlockKey, SavedBlock> entry : new ArrayList<>(ragePitOriginalBlocks.entrySet())) {
                BlockKey key = entry.getKey();
                SavedBlock saved = entry.getValue();
                try {
                    Block block = world.getBlockAt(key.x(), key.y(), key.z());
                    block.setType(saved.material());
                    block.setData(saved.data());
                } catch (Throwable failure) {
                    plugin.getLogger().severe("Rage Pit block restoration failed for arena '" + ragePitArena + "' at " + key.x() + " " + key.y() + " " + key.z() + ": " + failure.getMessage());
                }
            }
        }
        ragePitOriginalBlocks.clear();
        ragePitBoundary.clear();
        ragePitWorld = null;
        ragePitArena = null;
    }

    public String validateRagePit(String arenaId) {
        PitYamlManager.RagePitDefinition definition = plugin.getYamlManager().ragePit(arenaId);
        if (definition == null) return "§cMissing YAML path arenas." + PitYamlManager.normalizeArenaId(arenaId) + ".rage-pit.";
        if (!definition.valid()) return "§c" + definition.error() + ".";
        World world = Bukkit.getWorld(definition.worldName());
        if (world == null) return "§cConfigured world '" + definition.worldName() + "' is not loaded.";
        int gaps = 0, replaceable = 0, solid = 0;
        for (PitYamlManager.RagePitBlock coordinate : definition.boundary()) {
            if (!world.isChunkLoaded(coordinate.x() >> 4, coordinate.z() >> 4)) { gaps++; continue; }
            Material type = world.getBlockAt(coordinate.x(), coordinate.y(), coordinate.z()).getType();
            if (type.isSolid()) solid++; else if (ragePitReplaceable(type)) replaceable++; else gaps++;
        }
        String dimensions = definition.shape() == PitYamlManager.RagePitShape.SQUARE || definition.shape() == PitYamlManager.RagePitShape.RECTANGLE
                ? " x-length=" + Math.abs((int) definition.cornerTwo().x() - (int) definition.cornerOne().x()) + " z-length=" + Math.abs((int) definition.cornerTwo().z() - (int) definition.cornerOne().z())
                : definition.shape() == PitYamlManager.RagePitShape.CIRCLE || definition.shape() == PitYamlManager.RagePitShape.ELLIPSE ? " radius-x=" + definition.radiusX() + " radius-z=" + definition.radiusZ() : "";
        return (gaps == 0 ? "§a" : "§c") + "Rage Pit boundary arena=" + PitYamlManager.normalizeArenaId(arenaId) + " shape=" + definition.shape() + dimensions + " blocks=" + definition.boundary().size() + " replaceable=" + replaceable + " terrain=" + solid + " gaps=" + gaps + ".";
    }

    public String previewRagePit(String arenaId) {
        String id = PitYamlManager.normalizeArenaId(arenaId);
        clearRagePitPreview(id);
        PitYamlManager.RagePitDefinition definition = plugin.getYamlManager().ragePit(id);
        if (definition == null || !definition.valid()) return definition == null ? "§cMissing YAML path arenas." + id + ".rage-pit." : "§c" + definition.error() + ".";
        String validation = validateRagePit(id);
        if (validation.startsWith("§c")) return validation;
        World world = Bukkit.getWorld(definition.worldName());
        if (world == null) return "§cConfigured Rage Pit world is not loaded.";
        Map<BlockKey, SavedBlock> originals = new LinkedHashMap<>();
        ragePitPreviews.put(id, originals);
        List<PitYamlManager.RagePitBlock> boundary = definition.boundary();
        int batch = Math.max(50, plugin.registry().settings().getInt("events.rage_pit.wall-placement-batch-size", 500));
        final int[] cursor = {0};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int end = Math.min(boundary.size(), cursor[0] + batch);
            for (; cursor[0] < end; cursor[0]++) {
                PitYamlManager.RagePitBlock coordinate = boundary.get(cursor[0]);
                if (!world.isChunkLoaded(coordinate.x() >> 4, coordinate.z() >> 4)) continue;
                Block block = world.getBlockAt(coordinate.x(), coordinate.y(), coordinate.z());
                if (block.getType().isSolid() || !ragePitReplaceable(block.getType())) continue;
                BlockKey key = new BlockKey(coordinate.x(), coordinate.y(), coordinate.z());
                originals.putIfAbsent(key, new SavedBlock(block.getType(), block.getData()));
                block.setType(definition.material());
                block.setData(definition.data());
            }
            if (cursor[0] >= boundary.size()) {
                BukkitTask current = ragePitPreviewTasks.remove(id);
                if (current != null) current.cancel();
                long duration = Math.max(20L, plugin.registry().settings().getLong("events.rage_pit.preview-duration-ticks", 600L));
                ragePitPreviewTasks.put(id, Bukkit.getScheduler().runTaskLater(plugin, () -> clearRagePitPreview(id), duration));
            }
        }, 1L, 1L);
        ragePitPreviewTasks.put(id, task);
        return "§aPreparing Rage Pit preview for §e" + id + "§a with §f" + boundary.size() + "§a cached boundary blocks.";
    }

    public String clearRagePitPreview(String arenaId) {
        String id = PitYamlManager.normalizeArenaId(arenaId);
        BukkitTask task = ragePitPreviewTasks.remove(id);
        if (task != null) task.cancel();
        Map<BlockKey, SavedBlock> originals = ragePitPreviews.remove(id);
        PitYamlManager.RagePitDefinition definition = plugin.getYamlManager().ragePit(id);
        World world = definition == null ? null : Bukkit.getWorld(definition.worldName());
        if (originals != null && world != null) for (Map.Entry<BlockKey, SavedBlock> entry : originals.entrySet()) {
            BlockKey key = entry.getKey(); SavedBlock saved = entry.getValue(); Block block = world.getBlockAt(key.x(), key.y(), key.z());
            block.setType(saved.material()); block.setData(saved.data());
        }
        return "§aCleared Rage Pit preview for §e" + id + "§a.";
    }

    private boolean ragePitReplaceable(Material material) {
        return material == Material.AIR || material == Material.WATER || material == Material.STATIONARY_WATER
                || material == Material.LAVA || material == Material.STATIONARY_LAVA || material == Material.LONG_GRASS
                || material == Material.YELLOW_FLOWER || material == Material.RED_ROSE || material == Material.SNOW
                || material == Material.VINE || material == Material.DEAD_BUSH || material == Material.FIRE
                || material == Material.WEB;
    }

    public void arenaCleanup() {
        for (String arena : new ArrayList<>(ragePitPreviews.keySet())) clearRagePitPreview(arena);
        if (activeEventId != null) stopEvent(activeEventId);
        else {
            cleanupRagePit();
            cleanupGiantCake(null, GiantCakeState.COMPLETED);
        }
    }

    public void ragePitReloadCleanup() {
        for (String arena : new ArrayList<>(ragePitPreviews.keySet())) clearRagePitPreview(arena);
        if ("rage_pit".equals(activeEventId) || ragePitPreparing) stopEvent("rage_pit");
        else cleanupRagePit();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGiantCakeInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        GiantCakeSession session = giantCake;
        Block clicked = event.getClickedBlock();
        if (!"giant_cake".equals(activeEventId) || session == null || session.state != GiantCakeState.ACTIVE || clicked == null || clicked.getWorld() != session.world) return;
        BlockKey key = new BlockKey(clicked.getX(), clicked.getY(), clicked.getZ());
        if (!session.slices.contains(key)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR || player.hasMetadata("NPC") || player.hasMetadata("vanished") || player.hasMetadata("afk")) return;
        if (player.getWorld() != session.world || !plugin.getArenaManager().isInCurrentPvpArena(player.getLocation()) || player.getLocation().distanceSquared(clicked.getLocation().add(0.5D, 0.5D, 0.5D)) > 36.0D) return;
        long now = System.currentTimeMillis();
        long cooldown = Math.max(100L, plugin.registry().settings().getLong("events.giant_cake.click-cooldown-ms", 350L));
        Long previous = session.clickCooldowns.put(player.getUniqueId(), now);
        if (previous != null && now - previous < cooldown) return;
        if (!session.consumed.add(key)) return;
        clicked.setType(Material.AIR);
        session.participation.merge(player.getUniqueId(), 1, Integer::sum);
        int gold = Math.max(0, plugin.registry().settings().getInt("events.giant_cake.slice-gold", 25));
        int xp = Math.max(0, plugin.registry().settings().getInt("events.giant_cake.slice-xp", 10));
        double specialChance = Math.max(0.0D, Math.min(1.0D, plugin.registry().settings().getDouble("events.giant_cake.special-piece-chance", 0.08D)));
        if (random.nextDouble() < specialChance) {
            gold += Math.max(0, plugin.registry().settings().getInt("events.giant_cake.special-gold", 100));
            xp += Math.max(0, plugin.registry().settings().getInt("events.giant_cake.special-xp", 50));
        }
        if (gold > 0) plugin.getPitGoldService().award(player, gold, ThePitPlugin.GoldSource.EVENT);
        if (xp > 0) plugin.getStatsManager().addXp(player, xp, StatsManager.XpSource.EVENT);
        if (plugin.getProgressManager() != null) {
            String transaction = "giant-cake:" + session.sessionId + ":" + key.x() + ":" + key.y() + ":" + key.z();
            plugin.getProgressManager().recordProgress(player, ProgressManager.ObjectiveType.MINOR_EVENT_PARTICIPATION, "giant_cake", 1,
                    new ProgressManager.ProgressContext("giant_cake", transaction, Map.of("slices", 1, "gold", gold, "xp", xp)));
        }
        session.hologramId = plugin.getNpcService().replaceTransientHologram(session.hologramId, session.origin.clone().add(0.5D, 4.4D, 0.5D), cakeHologram(session));
        if (session.consumed.size() >= session.slices.size()) stopEvent("giant_cake");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGiantCakeExplosion(EntityExplodeEvent event) {
        GiantCakeSession session = giantCake;
        event.blockList().removeIf(block -> session != null && event.getLocation().getWorld() == session.world && isCakeBlock(session, block)
                || ragePitWorld == block.getWorld() && ragePitBoundary.contains(new BlockKey(block.getX(), block.getY(), block.getZ())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGiantCakePhysics(BlockPhysicsEvent event) {
        GiantCakeSession session = giantCake;
        Block block = event.getBlock();
        if (session != null && isCakeBlock(session, block) || ragePitWorld == block.getWorld() && ragePitBoundary.contains(new BlockKey(block.getX(), block.getY(), block.getZ()))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGiantCakeLiquid(BlockFromToEvent event) {
        GiantCakeSession session = giantCake;
        Block from = event.getBlock(), to = event.getToBlock();
        if (session != null && (isCakeBlock(session, from) || isCakeBlock(session, to))
                || ragePitWorld == from.getWorld() && (ragePitBoundary.contains(new BlockKey(from.getX(), from.getY(), from.getZ())) || ragePitBoundary.contains(new BlockKey(to.getX(), to.getY(), to.getZ())))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGiantCakePistonExtend(BlockPistonExtendEvent event) {
        GiantCakeSession session = giantCake;
        if (event.getBlocks().stream().anyMatch(block -> session != null && isCakeBlock(session, block)
                || ragePitWorld == block.getWorld() && ragePitBoundary.contains(new BlockKey(block.getX(), block.getY(), block.getZ())))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGiantCakePistonRetract(BlockPistonRetractEvent event) {
        GiantCakeSession session = giantCake;
        Block block = event.getRetractLocation().getBlock();
        if (session != null && isCakeBlock(session, block) || ragePitWorld == block.getWorld() && ragePitBoundary.contains(new BlockKey(block.getX(), block.getY(), block.getZ()))) event.setCancelled(true);
    }

    private boolean isCakeBlock(GiantCakeSession session, Block block) {
        return block != null && block.getWorld() == session.world && session.originals.containsKey(new BlockKey(block.getX(), block.getY(), block.getZ()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRagePitBreak(BlockBreakEvent event) {
        GiantCakeSession cake = giantCake;
        if (cake != null && isCakeBlock(cake, event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        if (!("rage_pit".equals(activeEventId) || ragePitPreparing) || ragePitWorld != event.getBlock().getWorld()) return;
        if (ragePitBoundary.contains(new BlockKey(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRagePitPlace(BlockPlaceEvent event) {
        GiantCakeSession cake = giantCake;
        if (cake != null && isCakeBlock(cake, event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        if (!("rage_pit".equals(activeEventId) || ragePitPreparing) || ragePitWorld != event.getBlock().getWorld()) return;
        if (ragePitBoundary.contains(new BlockKey(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()))) event.setCancelled(true);
    }

    @EventHandler
    public void onRagePitWorldUnload(WorldUnloadEvent event) {
        boolean preview = ragePitPreviews.keySet().stream().map(plugin.getYamlManager()::ragePit).anyMatch(definition -> definition != null && event.getWorld().getName().equals(definition.worldName()));
        if (ragePitWorld == event.getWorld() || preview || giantCake != null && giantCake.world == event.getWorld()) arenaCleanup();
    }

    private record BlockKey(int x, int y, int z) {}
    private record SavedBlock(Material material, byte data) {}
    private record CakeBlock(Material material, byte data, boolean slice) {}
    private enum GiantCakeState { IDLE, VALIDATING, GENERATING, ACTIVE, CLEANING, COMPLETED, FAILED }
    private static final class GiantCakeSession {
        private final UUID sessionId;
        private final String arenaId;
        private final World world;
        private final String locationId;
        private final Location origin;
        private final Map<BlockKey, SavedBlock> originals = new LinkedHashMap<>();
        private final Set<BlockKey> slices = ConcurrentHashMap.newKeySet();
        private final Set<BlockKey> consumed = ConcurrentHashMap.newKeySet();
        private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();
        private final Map<UUID, Integer> participation = new ConcurrentHashMap<>();
        private GiantCakeState state = GiantCakeState.VALIDATING;
        private UUID hologramId;
        private int placed;

        private GiantCakeSession(UUID sessionId, String arenaId, World world, String locationId, Location origin) {
            this.sessionId = sessionId;
            this.arenaId = arenaId;
            this.world = world;
            this.locationId = locationId;
            this.origin = origin;
        }
    }
}
