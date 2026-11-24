package club.abelix.games.buildffa.game.manager.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.games.buildffa.game.event.GameDeathEvent;
import club.abelix.games.buildffa.game.event.GameMapChangeEvent;
import club.abelix.games.buildffa.game.gui.event.CandyHuntLeaderboardGui;
import club.abelix.games.buildffa.game.gui.event.StandardEventLeaderboardGui;
import club.abelix.games.buildffa.game.manager.event.bossbar.LegacyEventBossBar;
import club.abelix.network.type.mode.Uncommon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Date;
import java.util.logging.Level;

public final class BuildFFAEventManager extends YListener {
    private static final String CANDY_BASKET_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTZiNTlmOWNjNjBlY2Q0ODMyYjkzMDI5MzA0ODE5ZDc2YTBhZDU4YTc1ZmZiMjNiZGZlOGIxZjMyNjlhMDJjYiJ9fX0=";
    public static final String EVENT_MANAGER_PERMISSION = BuildFFAGame.EVENT_MANAGER_PERMISSION;
    private static final long ASSIST_WINDOW_MILLIS = 10_000L;
    private static final double MIN_ASSIST_DAMAGE = 4.0D;
    private static final int ASSIST_COINS = 15;
    private static final int ASSIST_RATING = 7;
    private static final int HOT_POTATO_COINS = 25;
    private static final int HOT_POTATO_RATING = 6;
    private static final long TARGET_TIMEOUT_MILLIS = 60_000L;
    private static final long HOT_POTATO_REWARD_MILLIS = 15_000L;
    private static final long MODIFIER_INTERVAL_MILLIS = 45_000L;
    private static final long BOSS_BAR_UPDATE_TICKS = 1L;
    private static final long BOSS_BAR_RETRY_MILLIS = 2_000L;
    private static final long BOSS_BAR_FAILURE_LOG_MILLIS = 30_000L;
    private static final long INITIAL_DELAY_MIN_MILLIS = 5L * 60L * 1000L;
    private static final long INITIAL_DELAY_MAX_MILLIS = 8L * 60L * 1000L;
    private static final long INTER_EVENT_DELAY_MIN_MILLIS = 8L * 60L * 1000L;
    private static final long INTER_EVENT_DELAY_MAX_MILLIS = 12L * 60L * 1000L;
    private static final long AUTOMATIC_COUNTDOWN_MILLIS = 60L * 1000L;
    private static final long AUTOMATIC_SECOND_WARNING_MILLIS = 30L * 1000L;
    private static final long AUTOMATIC_FINAL_WARNING_MILLIS = 10L * 1000L;
    private static final long AUTOMATIC_RETRY_MILLIS = 60L * 1000L;
    private static final long AUTOMATIC_SCHEDULER_TICKS = 20L;
    private static final int[] EVENT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24};
    private static final int AUTOMATIC_STATUS_SLOT = 27;
    private static final int PLAYER_ACTIVE_EVENT_SLOT = 4;
    private static final int[] PLAYER_UPCOMING_EVENT_SLOTS = {10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int PLAYER_AUTOMATIC_STATUS_SLOT = 48;
    private static final int PLAYER_REFRESH_SLOT = 49;
    private static final int PLAYER_EVENT_CLOSE_SLOT = 53;
    private static final int UPCOMING_EVENT_COUNT = 9;
    private static final int START_SLOT = 30;
    private static final int STOP_SLOT = 32;
    private static final int CLOSE_SLOT = 35;
    private static final int EVENT_STATS_ACTIVE_SLOT = 4;
    private static final int[] HISTORY_SLOTS = {20, 21, 22, 23, 24};
    private static final int[] LEADERBOARD_SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
    private static final int EVENT_STATS_BACK_SLOT = 48;
    private static final int EVENT_STATS_CLOSE_SLOT = 53;

    private final BuildFFAGame game;
    private final KingOfTheHillPlatform hill;
    private String hillUnavailable;
    private String hillStatus = "empty";
    private UUID hillHolder;
    private int hillRemovalWarnings;
    private UUID lastRewardedHillSession;
    private final Map<UUID, List<HillReward>> pendingHillRewards = new HashMap<>();
    private final Random random = new Random();
    private final Map<UUID, Map<UUID, Contribution>> contributions = new HashMap<>();
    private final Map<UUID, UUID> revengeContracts = new HashMap<>();
    private final Map<UUID, Map<PotionEffectType, PotionEffect>> replacedEffects = new HashMap<>();
    private final Map<UUID, Set<Integer>> streakRewardedMilestones = new HashMap<>();
    private final Set<GameDeathEvent> processedDeaths = Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private final Map<UUID, LegacyEventBossBar> bossBars = new HashMap<>();
    private final Map<UUID, Long> bossBarRetryAfter = new HashMap<>();
    private final Map<UUID, String> bossBarFailureSignatures = new HashMap<>();
    private final Map<UUID, Long> bossBarFailureLogAfter = new HashMap<>();
    private final Map<EventType, Boolean> administrativeEnabled = new HashMap<>();
    private final LinkedList<ScheduledEvent> upcomingEvents = new LinkedList<>();
    private final Map<UUID, EventType> selections = new HashMap<>();
    private final LinkedList<EventSessionSnapshot> history = new LinkedList<>();
    private final Map<UUID, Integer> activeSessionKills = new HashMap<>();
    private final Map<UUID, PlayerIdentity> activeSessionPlayers = new HashMap<>();
    private UUID lastRecordedSession;
    private UUID activeSession;
    private volatile long inventoryRevision;
    private boolean transferringArena;
    private boolean endingEvent;
    private final Map<UUID, Integer> transferredRouletteWeapons = new HashMap<>();
    private long transferredHotPotatoReward;
    private EventType activeEvent;
    private long startTimestamp;
    private long endTimestamp;
    private long activeDurationMillis;
    private long automaticSchedulePausedAt;
    private boolean activeEventAutomatic;
    private BukkitTask task;
    private BukkitTask bossBarTask;
    private final Set<UUID> pendingBossBarRemovals = new HashSet<>();
    private boolean loggedBossBarTaskFailure;
    private final LinkedList<LeaderboardDiagnostic> leaderboardDiagnostics = new LinkedList<>();
    private final LinkedList<LeaderboardDiagnostic> leaderboardErrors = new LinkedList<>();
    private UUID goldenTarget;
    private UUID previousGoldenTarget;
    private long targetExpiresAt;
    private UUID hotPotatoHolder;
    private UUID previousHotPotatoHolder;
    private String hotPotatoHolderDisplayName;
    private String previousHotPotatoHolderDisplayName;
    private long nextHotPotatoReward;
    private boolean hotPotatoWaitingAnnounced;
    private Modifier activeModifier;
    private long nextModifierChange;
    private long nextSelectionAt;
    private EventType selectedAutomaticEvent;
    private long automaticStartAt;
    private boolean countdownActive;
    private EventType lastCompletedEvent;
    private long schedulerGeneration;
    private int automaticAnnouncements;
    private BukkitTask automaticSchedulerTask;
    private boolean shuttingDown;
    private final Map<UUID, EventScore> eventScores = new HashMap<>();
    private final Set<GameDeathEvent> creditedCandyDeaths = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private final Map<UUID, Long> candyInteractions = new HashMap<>();
    private final Set<UUID> eventDeaths = new HashSet<>();
    private final Set<UUID> resettingPlayers = new HashSet<>();
    private final Map<UUID, Integer> rouletteWeapons = new HashMap<>();
    private final Map<UUID, Integer> rouletteSlots = new HashMap<>();
    private final Map<UUID, Integer> candySlots = new HashMap<>();
    private final Map<UUID, org.bukkit.entity.Entity> rouletteProjectiles = new HashMap<>();
    private EventSaveFilter eventSaveFilter;
    private final Map<UUID, Map<PotionEffectType, OwnedEffect>> ownedEffects = new HashMap<>();
    private UUID juggernaut;
    private UUID pendingJuggernaut;
    private long juggernautSelectedAt;
    private double juggernautMaxHealth;
    private final RouletteWeapon[] roulettePool = RouletteWeapon.values();

    public BuildFFAEventManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
        hill = new KingOfTheHillPlatform(game);
        for (EventType type : EventType.values()) {
            administrativeEnabled.put(type, game.getFfaConfig().isEventEnabled(type.id, type.defaultEnabled));
        }
        for (org.bukkit.World world : Bukkit.getWorlds()) for (org.bukkit.entity.Entity entity : world.getEntities())
            if (entity instanceof Item && EventItems.isTemporary(((Item) entity).getItemStack())) entity.remove();
        for (Player player : Bukkit.getOnlinePlayers()) removeTemporaryItems(player);
    }

    public void startAutomaticScheduler() {
        if (shuttingDown || activeEvent != null) return;
        scheduleAutomaticSelection(randomDelay(INITIAL_DELAY_MIN_MILLIS, INITIAL_DELAY_MAX_MILLIS));
    }

    public void open(Player player) {
        if (!hasEventManagerPermission(player)) {
            sendEventManagerNoPermission(player);
            return;
        }
        Inventory current = player.getOpenInventory().getTopInventory();
        if (current != null && current.getHolder() instanceof EventManagerHolder) {
            renderManager(player, current);
            return;
        }
        Inventory inventory = Bukkit.createInventory(new EventManagerHolder(player.getUniqueId()), 36,
                message(Gamer.getGamer(player), "buildffa_event_manager_title", "§8BuildFFA Event Manager"));
        renderManager(player, inventory);
        player.openInventory(inventory);
    }

    public void openPlayerMenu(Player player) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> openPlayerMenu(player));
            return;
        }
        Inventory current = player.getOpenInventory().getTopInventory();
        if (current != null && current.getHolder() instanceof PlayerEventHolder) {
            renderPlayerMenu(player, current);
            return;
        }
        Gamer gamer = Gamer.getGamer(player);
        Inventory inventory = Bukkit.createInventory(new PlayerEventHolder(player.getUniqueId()), 54,
                playerMenuMessage(gamer, "buildffa_event_player_menu_title", "§8BuildFFA Events"));
        renderPlayerMenu(player, inventory);
        player.openInventory(inventory);
    }

    private void renderPlayerMenu(Player player, Inventory inventory) {
        Gamer gamer = Gamer.getGamer(player);
        ItemStack filler = item(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        if (activeEvent != null) {
            inventory.setItem(PLAYER_ACTIVE_EVENT_SLOT, playerCalendarEventItem(gamer, activeEvent,
                    startTimestamp, activeDurationMillis, true));
        } else inventory.setItem(PLAYER_ACTIVE_EVENT_SLOT, playerCalendarStatusItem(gamer,
                "buildffa_event_player_no_active_name", "§7No active event",
                "buildffa_event_player_no_active_lore", "§7There is no event active right now."));
        int eventIndex = 0;
        for (ScheduledEvent scheduled : upcomingEvents) {
            if (eventIndex >= PLAYER_UPCOMING_EVENT_SLOTS.length) break;
            inventory.setItem(PLAYER_UPCOMING_EVENT_SLOTS[eventIndex++], playerCalendarEventItem(gamer,
                    scheduled.eventType, effectiveScheduledStartTime(scheduled, System.currentTimeMillis()),
                    scheduled.plannedDuration, false));
        }
        if (eventIndex == 0) inventory.setItem(PLAYER_UPCOMING_EVENT_SLOTS[10], emptyScheduleItem(gamer));
        inventory.setItem(PLAYER_AUTOMATIC_STATUS_SLOT, automaticStatusItem(gamer));
        inventory.setItem(PLAYER_REFRESH_SLOT, playerCalendarStatusItem(gamer,
                "buildffa_event_player_refresh_name", "§eCalendar refreshes automatically",
                "buildffa_event_player_refresh_lore", "§7Event times update every second."));
        inventory.setItem(PLAYER_EVENT_CLOSE_SLOT, item(Material.BARRIER, (short) 0,
                playerMenuMessage(gamer, "buildffa_event_player_close", "§cClose"), Collections.emptyList()));
    }

    private ItemStack emptyScheduleItem(Gamer gamer) {
        boolean anyEnabled = false;
        for (EventType type : EventType.values()) if (isAdministrativelyEnabled(type)) anyEnabled = true;
        return anyEnabled ? playerCalendarStatusItem(gamer,
                "buildffa_event_player_schedule_rebuilding_name", "§eSchedule rebuilding",
                "buildffa_event_player_schedule_rebuilding_lore", "§7Upcoming events are being scheduled.")
                : playerCalendarStatusItem(gamer,
                "buildffa_event_player_no_enabled_name", "§cAutomatic events disabled",
                "buildffa_event_player_no_enabled_lore", "§7No events are administratively enabled.");
    }

    private ItemStack playerCalendarStatusItem(Gamer gamer, String nameKey, String nameFallback,
                                                String loreKey, String loreFallback) {
        return item(Material.PAPER, (short) 0, playerMenuMessage(gamer, nameKey, nameFallback),
                messageList(gamer, loreKey, Collections.singletonList(loreFallback)));
    }

    private ItemStack playerCalendarEventItem(Gamer gamer, EventType type, long scheduledStartTime,
                                               long durationMillis, boolean active) {
        List<String> description = displayedEventDescriptions(gamer, type);
        String firstLine = description.isEmpty() ? "" : description.get(0);
        String secondLine = description.size() < 2 ? "" : description.get(1);
        String eventName = eventName(gamer, type);
        String starts = active ? "" : formatEventDuration(Math.max(0L, scheduledStartTime - System.currentTimeMillis()));
        String lasts = formatEventDuration(durationMillis);
        String remaining = active ? formatEventDuration(remainingMillis()) : "";
        String name = "§e" + eventName;
        List<String> lore = new ArrayList<>();
        if (active) {
            addPlayerEventLore(lore, gamer, "buildffa_event_player_active_now", "§aActive now",
                    eventName, starts, lasts, remaining, firstLine, secondLine);
            addPlayerEventLore(lore, gamer, "buildffa_event_player_time_remaining", "§7Time remaining: §e%event_time_remaining%",
                    eventName, starts, lasts, remaining, firstLine, secondLine);
        } else {
            addPlayerEventLore(lore, gamer, "buildffa_event_player_starts_in", "§7Starts in: §e%event_duration_starts%",
                    eventName, starts, lasts, remaining, firstLine, secondLine);
        }
        addPlayerEventLore(lore, gamer, "buildffa_event_player_lasts_for", "§7Event lasts for: §e%event_duration_lasts%",
                eventName, starts, lasts, remaining, firstLine, secondLine);
        lore.add("");
        addPlayerEventLore(lore, gamer, "buildffa_event_player_description_1", "§7%event_description_1%",
                eventName, starts, lasts, remaining, firstLine, secondLine);
        addPlayerEventLore(lore, gamer, "buildffa_event_player_description_2", "§7%event_description_2%",
                eventName, starts, lasts, remaining, firstLine, secondLine);
        if (type == EventType.KING_OF_THE_HILL) lore.add(availabilityReason(gamer, type));
        return item(Material.WOOL, active ? (short) 1 : type == EventType.KING_OF_THE_HILL ? woolData(type) : (short) 3, name, lore);
    }

    private void addPlayerEventLore(List<String> lore, Gamer gamer, String key, String fallback, String eventName,
                                    String starts, String lasts, String remaining, String firstLine, String secondLine) {
        String row = replacePlayerEventPlaceholders(playerMenuMessage(gamer, key, fallback), eventName, starts, lasts,
                remaining, firstLine, secondLine);
        lore.add(row);
    }

    private String replacePlayerEventPlaceholders(String value, String eventName, String starts, String lasts,
                                                  String remaining, String firstLine, String secondLine) {
        return value.replace("%event_name%", eventName)
                .replace("%event_duration_starts%", starts)
                .replace("%event_duration_lasts%", lasts)
                .replace("%event_time_remaining%", remaining)
                .replace("%event_description_1%", firstLine)
                .replace("%event_description_2%", secondLine);
    }

    private void renderManager(Player player, Inventory inventory) {
        Gamer gamer = Gamer.getGamer(player);
        ItemStack filler = item(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        EventType[] events = EventType.values();
        for (int index = 0; index < events.length; index++) {
            inventory.setItem(EVENT_SLOTS[index], eventItem(gamer, events[index], selections.get(player.getUniqueId()) == events[index]));
        }
        EventType selected = selections.get(player.getUniqueId());
        inventory.setItem(AUTOMATIC_STATUS_SLOT, automaticStatusItem(gamer));
        inventory.setItem(START_SLOT, controlItem(gamer, selected, true));
        inventory.setItem(STOP_SLOT, controlItem(gamer, selected, false));
        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, (short) 0,
                message(gamer, "buildffa_event_manager_close", "§cClose"),
                messageList(gamer, "buildffa_event_manager_close_lore", Collections.singletonList("§7Click to close this menu."))));
    }

    private ItemStack automaticStatusItem(Gamer gamer) {
        List<String> lore = new ArrayList<>();
        boolean anyEnabled = false;
        for (EventType type : EventType.values()) if (isAdministrativelyEnabled(type)) anyEnabled = true;
        lore.add(anyEnabled
                ? message(gamer, "buildffa_event_automatic_status_enabled", "§aAutomatic events: Enabled")
                : message(gamer, "buildffa_event_automatic_status_disabled", "§cAutomatic events: Disabled"));
        long now = System.currentTimeMillis();
        ScheduledEvent nextScheduled = upcomingEvents.peekFirst();
        if (activeEvent != null) {
            String remaining = formatEventDuration(Math.max(0L, endTimestamp - now));
            lore.add(message(gamer, "buildffa_event_automatic_status_active",
                    "§7Active event: §e%event_name% §7(%event_time_remaining%)")
                    .replace("%event_name%", eventName(gamer, activeEvent))
                    .replace("%event_time_remaining%", remaining)
                    .replace("%event_time%", remaining));
            if (!activeEventAutomatic) lore.add(message(gamer, "buildffa_event_automatic_status_paused",
                    "§7Automatic schedule: §ePaused while an event is active")
                    .replace("%event_name%", eventName(gamer, activeEvent))
                    .replace("%event_time_remaining%", remaining));
        } else if (nextScheduled != null) {
            lore.add(message(gamer, "buildffa_event_automatic_status_upcoming", "§7Upcoming event: §e%event_name%")
                    .replace("%event_name%", eventName(gamer, nextScheduled.eventType)));
            long remainingMillis = Math.max(0L, effectiveScheduledStartTime(nextScheduled, now) - now);
            lore.add(message(gamer, "buildffa_event_automatic_status_starts", "%event_starts_in%")
                    .replace("%event_starts_in%", formatEventDuration(remainingMillis)));
        } else {
            lore.add(message(gamer, "buildffa_event_automatic_status_unavailable", "§7Unavailable"));
        }
        return item(Material.WATCH, (short) 0,
                message(gamer, "buildffa_event_automatic_status_name", "§eAutomatic Event Scheduler"), lore);
    }

    private ItemStack eventItem(Gamer gamer, EventType type, boolean selected) {
        EventState state = state(type);
        short data = woolData(type);
        String stateKey = state == EventState.ENABLED ? "buildffa_event_state_enabled"
                : state == EventState.UNAVAILABLE ? "buildffa_event_king_of_the_hill_state_unavailable"
                : state == EventState.DISABLED ? "buildffa_event_state_disabled"
                : "buildffa_event_state_active";
        String stateText = message(gamer, stateKey,
                state == EventState.ACTIVE ? "§6Active" : state == EventState.ENABLED ? "§aEnabled"
                        : state == EventState.UNAVAILABLE ? "§cUnavailable" : "§cDisabled");
        String canStart = availabilityReason(gamer, type);
        String duration = state == EventState.ACTIVE ? formatLoreDuration(remainingMillis())
                : formatLoreDuration(type.durationMillis);
        List<String> lore = type == EventType.CANDY_HUNT || type == EventType.JUGGERNAUT || type == EventType.WEAPON_ROULETTE
                ? new ArrayList<>(eventDescriptions(gamer, type)) : wrapLore(eventDescriptions(gamer, type), 5);
        lore.add("");
        lore.add(message(gamer, "buildffa_event_manager_state", "§7State: %state%").replace("%state%", stateText));
        lore.add(message(gamer, "buildffa_event_manager_duration", "§7Duration: §e%duration%").replace("%duration%", duration));
        lore.add(canStart);
        lore.add("");
        if (selected) lore.add(message(gamer, "buildffa_event_selected", "§aSelected"));
        else lore.add(message(gamer, "buildffa_event_click_select", "§eClick to select this event!"));
        return item(Material.WOOL, data, message(gamer, type.nameKey, "§e" + type.displayName), lore);
    }

    private short woolData(EventType type) {
        if (activeEvent == type) return 1;
        return canStart(type) ? (short) 5 : (short) 14;
    }

    private ItemStack controlItem(Gamer gamer, EventType selected, boolean start) {
        String nameKey = start ? "buildffa_event_start_button_name" : "buildffa_event_stop_button_name";
        String name = message(gamer, nameKey, start ? "§a§lStart Event" : "§c§lStop Event");
        if (selected == null) {
            String loreKey = start ? "buildffa_event_start_button_no_selection" : "buildffa_event_stop_button_no_selection";
            List<String> fallback = Arrays.asList("§7Select an event before " + (start ? "starting." : "stopping."), "", "§cNo event selected.");
            return item(Material.WOOL, (short) 14, name, wrapLore(messageList(gamer, loreKey, fallback), 5));
        }
        String loreKey = start ? "buildffa_event_start_button_lore" : "buildffa_event_stop_button_lore";
        List<String> fallback = Arrays.asList("§7" + (start ? "Start" : "Stop") + " the selected BuildFFA event.", "",
                "§7Selected: §e%event_name%", "", "§eClick to " + (start ? "start!" : "stop!"));
        List<String> lore = replace(wrapLore(messageList(gamer, loreKey, fallback), 5), "%event_name%", eventName(gamer, selected));
        if (start) lore.add(availabilityReason(gamer, selected));
        return item(Material.WOOL, start ? woolData(selected) : (short) 14, name, lore);
    }

    public boolean start(EventType type, Player actor) {
        return start(type, actor, actor == null);
    }

    public boolean start(EventType type, Player actor, long durationMillis) {
        return start(type, actor, false);
    }

    private boolean start(EventType type, Player actor, boolean automatic) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Events must start on the server thread");
        if (!canStart(type)) {
            if (actor != null && type == EventType.KING_OF_THE_HILL) actor.sendMessage(availabilityReason(Gamer.getGamer(actor), type));
            return false;
        }
        if (!automatic) pauseAutomaticSchedule();
        activeEvent = type;
        activeEventAutomatic = automatic;
        activeSession = UUID.randomUUID();
        inventoryRevision++;
        startTimestamp = System.currentTimeMillis();
        activeDurationMillis = type.durationMillis;
        endTimestamp = startTimestamp + activeDurationMillis;
        activeSessionKills.clear();
        activeSessionPlayers.clear();
        for (Player player : eligiblePlayers()) recordParticipant(player);
        UUID session = activeSession;
        try {
            initializeEvent(type);
            task = Bukkit.getScheduler().runTaskTimer(game.getPlugin(), () -> tick(session), 20L, 20L);
        } catch (RuntimeException exception) {
            activeSession = null;
            inventoryRevision++;
            activeEvent = null;
            activeEventAutomatic = false;
            activeDurationMillis = 0L;
            if (task != null) task.cancel();
            task = null;
            removeBossBars();
            cleanupEvent();
            if (actor != null && type == EventType.KING_OF_THE_HILL) actor.sendMessage(hillUnavailableReason(Gamer.getGamer(actor), "placement"));
            game.getPlugin().getLogger().log(Level.SEVERE, "Could not start BuildFFA event " + type.id, exception);
            if (!automatic) resumeAutomaticSchedule(System.currentTimeMillis());
            else if (!shuttingDown) refillUpcomingEvents(System.currentTimeMillis() + AUTOMATIC_RETRY_MILLIS + AUTOMATIC_COUNTDOWN_MILLIS);
            return false;
        }
        broadcastEventStarted(type);
        refreshManagerMenus();
        startBossBars(session, startTimestamp);
        return true;
    }

    private void startBossBars(UUID session, long now) {
        if (bossBarTask != null) bossBarTask.cancel();
        bossBarTask = null;
        bossBarRetryAfter.clear();
        bossBarFailureSignatures.clear();
        bossBarFailureLogAfter.clear();
        loggedBossBarTaskFailure = false;
        try {
            bossBarTask = Bukkit.getScheduler().runTaskTimer(game.getPlugin(), () -> tickBossBars(session),
                    BOSS_BAR_UPDATE_TICKS, BOSS_BAR_UPDATE_TICKS);
            updateBossBars(now);
        } catch (RuntimeException exception) {
            game.getPlugin().getLogger().log(Level.WARNING, "Could not initialize BuildFFA event boss bars", exception);
        }
    }

    public void stop(boolean expired) {
        stop(expired, true);
    }

    private void stop(boolean expired, boolean scheduleNext) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Events must stop on the server thread");
        if (activeEvent == null || endingEvent) return;
        endingEvent = true;
        EventType stopped = activeEvent;
        boolean stoppedAutomatically = activeEventAutomatic;
        UUID stoppedSession = activeSession;
        long stoppedAt = System.currentTimeMillis();
        releaseJuggernaut();
        if (expired) rewardEventResults(stopped);
        recordSession(stoppedSession, stopped, stoppedAt, expired);
        activeSession = null;
        activeEvent = null;
        inventoryRevision++;
        activeEventAutomatic = false;
        if (task != null) task.cancel();
        task = null;
        if (bossBarTask != null) bossBarTask.cancel();
        bossBarTask = null;
        removeBossBars();
        cleanupEvent();
        endingEvent = false;
        lastCompletedEvent = stopped;
        if (expired) {
            broadcastEventExpired(stopped);
        } else {
            broadcastEvent("buildffa_event_stopped", "§cBuildFFA event stopped: §f%event%", stopped);
        }
        refreshManagerMenus();
        if (scheduleNext && !shuttingDown) {
            if (!stoppedAutomatically) resumeAutomaticSchedule(stoppedAt);
            refillUpcomingEvents(stoppedAt + INTER_EVENT_DELAY_MIN_MILLIS + AUTOMATIC_COUNTDOWN_MILLIS);
            ensureAutomaticSchedulerTask();
        }
        refreshManagerMenus();
    }

    private void scheduleAutomaticSelection(long delayMillis) {
        cancelAutomaticScheduler();
        if (shuttingDown || activeEvent != null) return;
        refillUpcomingEvents(System.currentTimeMillis() + delayMillis + AUTOMATIC_COUNTDOWN_MILLIS);
        ensureAutomaticSchedulerTask();
        refreshManagerMenus();
    }

    private void tickAutomaticScheduler(long generation) {
        if (shuttingDown || generation != schedulerGeneration) return;
        long now = System.currentTimeMillis();
        removeDisabledScheduledEvents();
        refillUpcomingEvents(now + AUTOMATIC_RETRY_MILLIS + AUTOMATIC_COUNTDOWN_MILLIS);
        syncAutomaticHead(now);
        if (activeEvent != null || upcomingEvents.isEmpty()) {
            refreshManagerMenus();
            return;
        }
        ScheduledEvent scheduled = upcomingEvents.getFirst();
        long remaining = scheduled.scheduledStartTime - now;
        if (remaining > 0L) {
            if (remaining <= AUTOMATIC_COUNTDOWN_MILLIS) announceAutomaticWarning(scheduled.eventType, remaining * 1_000_000L);
            refreshManagerMenus();
            return;
        }
        upcomingEvents.removeFirst();
        if (!canStart(scheduled.eventType)) {
            if (countdownActive) broadcastAutomaticCancellation(scheduled.eventType);
            resetAutomaticHeadState();
            refillUpcomingEvents(now + AUTOMATIC_RETRY_MILLIS + AUTOMATIC_COUNTDOWN_MILLIS);
            refreshManagerMenus();
            return;
        }
        resetAutomaticHeadState();
        refillUpcomingEvents(now + scheduled.plannedDuration + INTER_EVENT_DELAY_MIN_MILLIS + AUTOMATIC_COUNTDOWN_MILLIS);
        if (!start(scheduled.eventType, null, true) && activeEvent == null) {
            refillUpcomingEvents(now + AUTOMATIC_RETRY_MILLIS + AUTOMATIC_COUNTDOWN_MILLIS);
        }
        refreshManagerMenus();
    }

    private EventType selectScheduledEvent(EventType previous) {
        List<EventType> enabled = new ArrayList<>();
        for (EventType type : EventType.values()) {
            if (isAdministrativelyEnabled(type) && activeEvent != type) enabled.add(type);
        }
        if (enabled.size() > 1 && previous != null) enabled.remove(previous);
        return enabled.isEmpty() ? null : enabled.get(random.nextInt(enabled.size()));
    }

    private void refillUpcomingEvents(long earliestStartTime) {
        long now = System.currentTimeMillis();
        long startTime = automaticSchedulePausedAt == 0L ? earliestStartTime
                : earliestStartTime - Math.max(0L, now - automaticSchedulePausedAt);
        EventType previous = lastCompletedEvent;
        if (!upcomingEvents.isEmpty()) {
            ScheduledEvent last = upcomingEvents.getLast();
            previous = last.eventType;
            startTime = Math.max(startTime, last.scheduledStartTime + last.plannedDuration
                    + randomDelay(INTER_EVENT_DELAY_MIN_MILLIS, INTER_EVENT_DELAY_MAX_MILLIS)
                    + AUTOMATIC_COUNTDOWN_MILLIS);
        }
        while (upcomingEvents.size() < UPCOMING_EVENT_COUNT) {
            EventType selected = selectScheduledEvent(previous);
            if (selected == null) break;
            upcomingEvents.add(new ScheduledEvent(selected, startTime, selected.durationMillis));
            previous = selected;
            startTime += selected.durationMillis
                    + randomDelay(INTER_EVENT_DELAY_MIN_MILLIS, INTER_EVENT_DELAY_MAX_MILLIS)
                    + AUTOMATIC_COUNTDOWN_MILLIS;
        }
        syncAutomaticHead(now);
    }

    private void removeDisabledScheduledEvents() {
        int enabledCount = 0;
        for (EventType type : EventType.values()) if (isAdministrativelyEnabled(type)) enabledCount++;
        EventType previous = null;
        Iterator<ScheduledEvent> iterator = upcomingEvents.iterator();
        while (iterator.hasNext()) {
            ScheduledEvent scheduled = iterator.next();
            if (!isAdministrativelyEnabled(scheduled.eventType)
                    || enabledCount > 1 && scheduled.eventType == previous) {
                iterator.remove();
                continue;
            }
            previous = scheduled.eventType;
        }
    }

    private void pauseAutomaticSchedule() {
        if (automaticSchedulePausedAt == 0L) automaticSchedulePausedAt = System.currentTimeMillis();
    }

    private void resumeAutomaticSchedule(long now) {
        if (automaticSchedulePausedAt == 0L) return;
        long pausedFor = Math.max(0L, now - automaticSchedulePausedAt);
        for (ScheduledEvent scheduled : upcomingEvents) scheduled.scheduledStartTime += pausedFor;
        automaticSchedulePausedAt = 0L;
        syncAutomaticHead(now);
    }

    private void ensureAutomaticSchedulerTask() {
        if (automaticSchedulerTask != null || shuttingDown) return;
        long generation = schedulerGeneration;
        automaticSchedulerTask = Bukkit.getScheduler().runTaskTimer(game.getPlugin(),
                () -> tickAutomaticScheduler(generation), AUTOMATIC_SCHEDULER_TICKS, AUTOMATIC_SCHEDULER_TICKS);
    }

    private void syncAutomaticHead(long now) {
        ScheduledEvent next = upcomingEvents.peekFirst();
        selectedAutomaticEvent = next == null ? null : next.eventType;
        automaticStartAt = next == null ? 0L : effectiveScheduledStartTime(next, now);
        nextSelectionAt = automaticStartAt;
        boolean wasCountingDown = countdownActive;
        countdownActive = next != null && automaticStartAt - now <= AUTOMATIC_COUNTDOWN_MILLIS;
        if (!countdownActive || !wasCountingDown) automaticAnnouncements = 0;
    }

    private long effectiveScheduledStartTime(ScheduledEvent scheduled, long now) {
        return automaticSchedulePausedAt == 0L ? scheduled.scheduledStartTime
                : scheduled.scheduledStartTime + Math.max(0L, now - automaticSchedulePausedAt);
    }

    private void resetAutomaticHeadState() {
        selectedAutomaticEvent = null;
        automaticStartAt = 0L;
        nextSelectionAt = 0L;
        countdownActive = false;
        automaticAnnouncements = 0;
    }

    private void announceAutomaticWarning(EventType type, long remainingNanos) {
        long remainingMillis = nanosToMillis(remainingNanos);
        if ((automaticAnnouncements & 1) == 0) {
            automaticAnnouncements |= 1;
            broadcastAutomaticAnnouncement(type, formatEventDuration(AUTOMATIC_COUNTDOWN_MILLIS));
        }
        if (remainingMillis <= AUTOMATIC_SECOND_WARNING_MILLIS && (automaticAnnouncements & 2) == 0) {
            automaticAnnouncements |= 2;
            broadcastAutomaticAnnouncement(type, formatEventDuration(AUTOMATIC_SECOND_WARNING_MILLIS));
        }
        if (remainingMillis <= AUTOMATIC_FINAL_WARNING_MILLIS && (automaticAnnouncements & 4) == 0) {
            automaticAnnouncements |= 4;
            broadcastAutomaticAnnouncement(type, formatEventDuration(AUTOMATIC_FINAL_WARNING_MILLIS));
        }
    }

    private void cancelAutomaticScheduler() {
        schedulerGeneration++;
        if (automaticSchedulerTask != null) automaticSchedulerTask.cancel();
        automaticSchedulerTask = null;
        upcomingEvents.clear();
        automaticSchedulePausedAt = 0L;
        resetAutomaticHeadState();
    }

    private long randomDelay(long minimum, long maximum) {
        return minimum + (long) (random.nextDouble() * (maximum - minimum + 1L));
    }

    private long nanosToMillis(long nanos) {
        return Math.max(0L, (nanos + 999_999L) / 1_000_000L);
    }

    private String hillUnavailableReason(Gamer gamer, String reason) {
        String detail = message(gamer, "buildffa_event_king_of_the_hill_unavailable_" + reason,
                reason.equals("missing") ? "Missing arena hill configuration." : reason.equals("bounds") ? "Radius must be 1-12 and capture height 2-6."
                : reason.equals("world") ? "The arena world is unavailable." : reason.equals("spawn") ? "The hill overlaps spawn protection."
                : reason.equals("unsafe_replacement") ? "The platform intersects a tile entity or inventory that cannot be safely restored." : reason.equals("obstructed") ? "The platform or standing space is obstructed." : reason.equals("placement") ? "Platform placement failed."
                : "The hill is outside the permitted build area or world height, or below the death level.");
        return color(message(gamer, "buildffa_event_king_of_the_hill_unavailable", "&cKing of the Hill unavailable: %reason%").replace("%reason%", detail));
    }

    private void tickHill() {
        if (!hill.isPlaced() || hillUnavailable != null) return;
        List<Player> occupants = new ArrayList<>();
        for (Player player : eligiblePlayers()) {
            score(player);
            if (hill.contains(player)) occupants.add(player);
        }
        hillHolder = occupants.size() == 1 ? occupants.get(0).getUniqueId() : null;
        hillStatus = occupants.isEmpty() ? "empty" : occupants.size() == 1 ? "capturing" : "contested";
        if (occupants.size() == 1) score(occupants.get(0)).capturePoints++;
    }
    private int nextHillRemovalWarning(long now) {
        long remaining = endTimestamp - now;
        if (remaining <= 0L || remaining > 5000L) return 0;
        int seconds = (int) ((remaining + 999L) / 1000L);
        int warning = 1 << seconds;
        if ((hillRemovalWarnings & warning) != 0) return 0;
        hillRemovalWarnings |= warning;
        return seconds;
    }

    private void warnHillRemoval(long now) {
        if (activeEvent != EventType.KING_OF_THE_HILL || activeSession == null || endingEvent
                || shuttingDown || transferringArena || !hill.isPlaced()) return;
        int seconds = nextHillRemovalWarning(now);
        if (seconds == 0) return;
        for (Player player : eligiblePlayers()) {
            Gamer gamer = Gamer.getGamer(player);
            if (gamer == null) continue;
            try {
                gamer.sendMessage(color(message(gamer, "buildffa_event_king_of_the_hill_removal_warning",
                        "&c[EVENT] &eThe hill platform disappears in &c%seconds% seconds&e! Get to safety!")
                        .replace("%seconds%", String.valueOf(seconds))));
            } catch (RuntimeException failure) {
                game.getPlugin().getLogger().log(Level.WARNING,
                        "Could not send King of the Hill removal chat countdown to " + player.getUniqueId(), failure);
            }
        }
    }
    private void tick(UUID session) {
        if (activeSession == null || !activeSession.equals(session) || activeEvent == null) return;
        long now = System.currentTimeMillis();
        if (now >= endTimestamp) {
            stop(true);
            return;
        }
        warnHillRemoval(now);
        refreshManagerMenus();
        if (transferringArena) return;
        if (activeEvent == EventType.RANDOM_MODIFIER) tickModifier(now);
        if (activeEvent == EventType.GOLDEN_KILL) tickGoldenTarget(now);
        if (activeEvent == EventType.HOT_POTATO) tickHotPotato(now);
        if (activeEvent == EventType.KING_OF_THE_HILL) tickHill();
        tickNewEvents(now);
    }

    private void tickBossBars(UUID session) {
        retryBossBarRemovals();
        if (activeSession == null || !activeSession.equals(session) || activeEvent == null) return;
        long now = System.currentTimeMillis();
        if (now >= endTimestamp) {
            stop(true);
            return;
        }
        try {
            warnHillRemoval(now);
            if (!transferringArena) updateBossBars(now);
        } catch (RuntimeException exception) {
            if (!loggedBossBarTaskFailure) {
                loggedBossBarTaskFailure = true;
                game.getPlugin().getLogger().log(Level.WARNING, "Could not update BuildFFA event boss bars", exception);
            }
        }
    }

    private void initializeEvent(EventType type) {
        hillRemovalWarnings = 0;
        if (type == EventType.KING_OF_THE_HILL) {
            hillUnavailable = hill.place();
            if (hillUnavailable != null) throw new IllegalStateException(hillUnavailable);
            hillStatus = "empty";
            hillHolder = null;
        }
        creditedCandyDeaths.clear();
        eventScores.clear();
        eventDeaths.clear();
        if (type == EventType.CANDY_HUNT || type == EventType.JUGGERNAUT || type == EventType.WEAPON_ROULETTE)
            eventSaveFilter = new EventSaveFilter(this::saveWithoutTemporaryState);
        if (type == EventType.JUGGERNAUT) selectJuggernaut();
        if (type == EventType.WEAPON_ROULETTE) for (Player player : eligiblePlayers()) giveRouletteWeapon(player);
        if (type == EventType.RANDOM_MODIFIER) rotateModifier();
        if (type == EventType.GOLDEN_KILL) selectGoldenTarget();
        if (type == EventType.HOT_POTATO) selectHotPotatoHolder();
    }

    private void cleanupEvent() {
        hillRemovalWarnings = 0;
        hill.restore();
        hillUnavailable = null;
        hillHolder = null;
        transferringArena = false;
        transferredRouletteWeapons.clear();
        transferredHotPotatoReward = 0L;
        creditedCandyDeaths.clear();
        releaseJuggernaut();
        pendingJuggernaut = null;
        for (Player player : Bukkit.getOnlinePlayers()) clearEventPlayer(player, true);
        eventScores.clear();
        eventDeaths.clear();
        resettingPlayers.clear();
        candyInteractions.clear();
        rouletteWeapons.clear();
        rouletteSlots.clear();
        candySlots.clear();
        for (org.bukkit.entity.Entity projectile : rouletteProjectiles.values()) projectile.remove();
        rouletteProjectiles.clear();
        ownedEffects.clear();
        if (eventSaveFilter != null) eventSaveFilter.close();
        eventSaveFilter = null;
        contributions.clear();
        revengeContracts.clear();
        streakRewardedMilestones.clear();
        clearTrackedEffects();
        game.getBlockTracker().setDecayMultiplier(1.0D);
        goldenTarget = null;
        previousGoldenTarget = null;
        hotPotatoHolder = null;
        previousHotPotatoHolder = null;
        hotPotatoHolderDisplayName = null;
        previousHotPotatoHolderDisplayName = null;
        hotPotatoWaitingAnnounced = false;
        activeModifier = null;
    }

    public long getInventoryRevision() {
        return inventoryRevision;
    }

    public boolean isActive(EventType type) {
        return activeEvent == type;
    }

    public int getRewardMultiplier() {
        return isActive(EventType.DOUBLE_REWARDS) ? 2 : 1;
    }

    public int getStreakRatingBonus(int normalBonus) {
        return normalBonus;
    }

    public EventBonus prepareKillBonus(Player killer, Player victim, int streak) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Event kill bonuses must be prepared on the server thread");
        if (activeEvent == null || !isEligible(killer) || !isDeathParticipant(victim) || killer.equals(victim)) return EventBonus.NONE;
        switch (activeEvent) {
            case DOUBLE_REWARDS: return EventBonus.DOUBLE_REWARDS;
            case GOLDEN_KILL: return victim.getUniqueId().equals(goldenTarget) ? EventBonus.GOLDEN_KILL : EventBonus.NONE;
            case JUGGERNAUT: return victim.getUniqueId().equals(juggernaut) ? EventBonus.JUGGERNAUT : EventBonus.NONE;
            case WEAPON_ROULETTE: return EventBonus.WEAPON_ROULETTE;
            case STREAK_SURGE: return handleStreakMilestone(killer, streak) ? EventBonus.STREAK_SURGE : EventBonus.NONE;
            case RANDOM_MODIFIER: return EventBonus.RANDOM_MODIFIER;
            case REVENGE_CONTRACT: return victim.getUniqueId().equals(revengeContracts.get(killer.getUniqueId()))
                    ? EventBonus.REVENGE_CONTRACT : EventBonus.NONE;
            case KNOCKBACK_CHAOS: return EventBonus.KNOCKBACK_CHAOS;
            case COMBO_HOUR: return EventBonus.COMBO_HOUR;
            default: return EventBonus.NONE;
        }
    }

    public boolean handleStreakMilestone(Player player, int streak) {
        if (!isActive(EventType.STREAK_SURGE) || !isEligible(player) || streak < 5 || streak % 5 != 0) return false;
        Set<Integer> milestones = streakRewardedMilestones.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());
        if (!milestones.add(streak)) return false;
        if (streak >= 20) applyTrackedEffect(player, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 8 * 20, 0));
        else if (streak >= 15) applyTrackedEffect(player, new PotionEffect(PotionEffectType.ABSORPTION, 10 * 20, 0));
        else if (streak >= 10) applyTrackedEffect(player, new PotionEffect(PotionEffectType.REGENERATION, 6 * 20, 0));
        else applyTrackedEffect(player, new PotionEffect(PotionEffectType.SPEED, 8 * 20, 0));
        return true;
    }

    public void recordDamage(Player attacker, Player victim, double damage) {
        if (!isActive(EventType.ASSIST_RUSH) || attacker.equals(victim) || !isEligible(attacker) || !isEligible(victim)) return;
        Map<UUID, Contribution> victimContributions = contributions.computeIfAbsent(victim.getUniqueId(), ignored -> new HashMap<>());
        Contribution contribution = victimContributions.computeIfAbsent(attacker.getUniqueId(), ignored -> new Contribution());
        contribution.damage += Math.max(0.0D, damage);
        contribution.lastHit = System.currentTimeMillis();
    }

    @EventHandler
    public void onDeath(GameDeathEvent event) {
        if (!Bukkit.isPrimaryThread()) {
            UUID session = activeSession;
            long revision = inventoryRevision;
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
                if (revision == inventoryRevision && !transferringArena && session != null && session.equals(activeSession)) onDeath(event);
            });
            return;
        }
        Player victim = event.getVictim();
        if (!processedDeaths.add(event)) return;
        boolean eligibleVictim = isDeathParticipant(victim);
        if (activeEvent != null && !eventDeaths.add(victim.getUniqueId())) return;
        UUID deathSession = activeSession;
        long deathRevision = inventoryRevision;
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            if (deathRevision == inventoryRevision && deathSession != null && deathSession.equals(activeSession)) eventDeaths.remove(victim.getUniqueId());
        });
        resettingPlayers.add(victim.getUniqueId());
        clearEventPlayer(victim, false);
        clearStreakEffects(event.getVictim());
        if (!event.isProgressionEligible() || event.getKiller() == null || event.getBaseKillRating() <= 0
                || !eligibleVictim || !isEligible(event.getKiller()) || event.getKiller().equals(victim)
                || !event.getDeathLocation().getWorld().equals(game.getWorldConfig().world())
                || game.isLobbyZone(event.getDeathLocation())) {
            contributions.remove(event.getVictim().getUniqueId());
            handleInvalidTargetDeath(event.getVictim());
            return;
        }
        if (activeEvent != null && activeSession != null) {
            recordParticipant(event.getVictim());
            recordKill(event.getKiller());
        }
        if (isActive(EventType.ASSIST_RUSH)) rewardAssists(event);
        if (isActive(EventType.REVENGE_CONTRACT) && eligibleVictim && isEligible(event.getKiller())
                && !event.getKiller().equals(victim)) handleRevenge(event);
        if (isActive(EventType.GOLDEN_KILL)) handleGoldenKill(event);
        if (isActive(EventType.HOT_POTATO)) handleHotPotatoDeath(event);
        if (isActive(EventType.STREAK_SURGE) && event.getEventBonusCoins() > 0)
            sendKillBonusMessage(event, "buildffa_event_streak_surge_reward", "§aStreak milestone! §e+%coins% coins §b+%rating% rating");
        if (eligibleVictim && isEligible(event.getKiller()) && !event.getKiller().equals(victim)
                && event.getDeathLocation().getWorld().equals(game.getWorldConfig().world())
                && !game.isLobbyZone(event.getDeathLocation())) {
            if (isActive(EventType.CANDY_HUNT)) awardCandy(event);
            if (isActive(EventType.WEAPON_ROULETTE)) {
                EventScore score = score(event.getKiller());
                score.rouletteKills++;
                int weapon = rouletteWeapons.getOrDefault(event.getKiller().getUniqueId(), -1);
                score.killsPerWeapon.put(weapon, score.killsPerWeapon.getOrDefault(weapon, 0) + 1);
                giveRouletteWeapon(event.getKiller());
            }
            if (isActive(EventType.JUGGERNAUT) && victim.getUniqueId().equals(pendingJuggernaut)) {
                Player killer = event.getKiller();
                score(killer).juggernautKills++;
                sendNewEventMessage(killer, "buildffa_event_juggernaut_killed", "%killer%", formattedPlayerName(killer),
                        "%juggernaut%", formattedPlayerName(victim), "%coins%", String.valueOf(event.getEventBonusCoins()),
                        "%rating%", String.valueOf(event.getEventBonusRating()));
                pendingJuggernaut = killer.getUniqueId();
            }
        }
        contributions.remove(event.getVictim().getUniqueId());
    }

    private void rewardAssists(GameDeathEvent event) {
        Map<UUID, Contribution> values = contributions.remove(event.getVictim().getUniqueId());
        if (values == null) return;
        long cutoff = System.currentTimeMillis() - ASSIST_WINDOW_MILLIS;
        for (Map.Entry<UUID, Contribution> entry : values.entrySet()) {
            if (entry.getKey().equals(event.getKiller().getUniqueId()) || entry.getKey().equals(event.getVictim().getUniqueId())) continue;
            Contribution contribution = entry.getValue();
            if (contribution.lastHit < cutoff || contribution.damage < MIN_ASSIST_DAMAGE) continue;
            Player assistant = Bukkit.getPlayer(entry.getKey());
            if (!isEligible(assistant)) continue;
            rewardEventBonus(assistant, ASSIST_COINS, ASSIST_RATING, "buildffa_event_assist_reward",
                    "§aAssist! §e+%coins% coins §b+%rating% rating");
        }
    }

    private void handleRevenge(GameDeathEvent event) {
        UUID killerId = event.getKiller().getUniqueId();
        UUID victimId = event.getVictim().getUniqueId();
        if (victimId.equals(revengeContracts.get(killerId))) {
            sendKillBonusMessage(event, "buildffa_event_revenge_reward", "§aRevenge claimed! §e+%coins% coins §b+%rating% rating");
            revengeContracts.remove(killerId);
        }
        revengeContracts.put(victimId, killerId);
    }

    private void handleGoldenKill(GameDeathEvent event) {
        if (goldenTarget == null || !goldenTarget.equals(event.getVictim().getUniqueId())) return;
        sendKillBonusMessage(event, "buildffa_event_golden_reward", "§6Golden Kill! §e+%coins% coins §b+%rating% rating");
        previousGoldenTarget = goldenTarget;
        goldenTarget = null;
        selectGoldenTarget();
    }

    private void handleHotPotatoDeath(GameDeathEvent event) {
        if (hotPotatoHolder == null || !hotPotatoHolder.equals(event.getVictim().getUniqueId())) return;
        UUID previous = hotPotatoHolder;
        clearHotPotatoHolder(previous);
        Player killer = event.getKiller();
        if (killer != null && !killer.getUniqueId().equals(previous) && isEligible(killer)) {
            assignHotPotatoHolder(killer, previous, killer, true);
            return;
        }
        selectHotPotatoHolder(previous);
    }

    private void handleInvalidTargetDeath(Player victim) {
        if (isActive(EventType.GOLDEN_KILL) && victim.getUniqueId().equals(goldenTarget)) {
            previousGoldenTarget = goldenTarget;
            goldenTarget = null;
            selectGoldenTarget();
        }
        if (isActive(EventType.HOT_POTATO) && victim.getUniqueId().equals(hotPotatoHolder)) {
            UUID previous = hotPotatoHolder;
            clearHotPotatoHolder(previous);
            selectHotPotatoHolder(previous);
        }
    }

    private void sendKillBonusMessage(GameDeathEvent event, String key, String fallback) {
        if (event.getEventBonusCoins() <= 0 && event.getEventBonusRating() <= 0) return;
        Gamer gamer = Gamer.getGamer(event.getKiller());
        gamer.sendMessage(color(message(gamer, key, fallback)
                .replace("%coins%", String.valueOf(event.getEventBonusCoins()))
                .replace("%rating%", String.valueOf(event.getEventBonusRating()))));
    }

    private void rewardEventBonus(Player player, int coins, int rating, String key, String fallback) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Event bonuses must be awarded on the server thread");
        Gamer gamer = Gamer.getGamer(player);
        gamer.addCoins(Uncommon.BUILDFFA, coins);
        game.getQuestManager().handleCoinsEarned(player, coins);
        game.getDivisionListener().awardEventRating(gamer, rating);
        gamer.sendMessage(color(message(gamer, key, fallback).replace("%coins%", String.valueOf(coins))
                .replace("%rating%", String.valueOf(rating))));
    }

    private void tickGoldenTarget(long now) {
        Player target = goldenTarget == null ? null : Bukkit.getPlayer(goldenTarget);
        if (target == null || !isEligible(target) || now >= targetExpiresAt) {
            previousGoldenTarget = goldenTarget;
            goldenTarget = null;
            selectGoldenTarget();
        }
    }

    private void selectGoldenTarget() {
        List<Player> eligible = eligiblePlayers();
        if (eligible.isEmpty()) return;
        Player selected = selectDifferent(eligible, previousGoldenTarget);
        if (!isEligible(selected)) return;
        goldenTarget = selected.getUniqueId();
        targetExpiresAt = System.currentTimeMillis() + TARGET_TIMEOUT_MILLIS;
        announceTarget("buildffa_event_golden_target", "§6Golden Target: §f%player%", selected);
    }

    private void tickHotPotato(long now) {
        UUID holderId = hotPotatoHolder;
        Player holder = holderId == null ? null : Bukkit.getPlayer(holderId);
        if (holder == null || !isEligible(holder)) {
            clearHotPotatoHolder(holderId);
            selectHotPotatoHolder(holderId);
            return;
        }
        if (now >= nextHotPotatoReward) {
            if (!holderId.equals(hotPotatoHolder) || !holder.isOnline() || !isEligible(holder)) return;
            nextHotPotatoReward = now + HOT_POTATO_REWARD_MILLIS;
            rewardEventBonus(holder, HOT_POTATO_COINS, HOT_POTATO_RATING, "buildffa_event_hot_potato_reward",
                    "§eHot Potato reward: §6+%coins% coins §b+%rating% rating");
        }
    }

    private void selectHotPotatoHolder() {
        selectHotPotatoHolder(null);
    }

    private void selectHotPotatoHolder(UUID excluded) {
        if (!isActive(EventType.HOT_POTATO)) return;
        List<Player> eligible = eligiblePlayers();
        eligible.removeIf(player -> player.getUniqueId().equals(excluded));
        if (eligible.isEmpty()) {
            announceHotPotatoWaiting();
            return;
        }
        Player selected = selectDifferent(eligible, previousHotPotatoHolder);
        assignHotPotatoHolder(selected, previousHotPotatoHolder, null, false);
    }

    private void clearHotPotatoHolder(UUID previous) {
        if (previous != null) {
            previousHotPotatoHolder = previous;
            if (previous.equals(hotPotatoHolder)) {
                Player previousPlayer = Bukkit.getPlayer(previous);
                String currentDisplayName = formattedPlayerName(previousPlayer);
                previousHotPotatoHolderDisplayName = currentDisplayName.isEmpty()
                        ? hotPotatoHolderDisplayName : currentDisplayName;
            }
        }
        hotPotatoHolder = null;
        hotPotatoHolderDisplayName = null;
        nextHotPotatoReward = 0L;
    }

    private void assignHotPotatoHolder(Player selected, UUID previous, Player killer, boolean transferred) {
        if (!isActive(EventType.HOT_POTATO) || selected == null || !isEligible(selected)) return;
        hotPotatoHolder = selected.getUniqueId();
        hotPotatoHolderDisplayName = formattedPlayerName(selected);
        nextHotPotatoReward = transferredHotPotatoReward > 0L ? transferredHotPotatoReward
                : System.currentTimeMillis() + HOT_POTATO_REWARD_MILLIS;
        transferredHotPotatoReward = 0L;
        hotPotatoWaitingAnnounced = false;
        announceHotPotatoHolder(selected, previous, killer, transferred);
    }

    private void announceHotPotatoHolder(Player selected, UUID previous, Player killer, boolean transferred) {
        String key = transferred ? "buildffa_event_hot_potato_transferred" : "buildffa_event_hot_potato_assigned";
        String fallback = transferred ? "§cHot Potato transferred from §f%previous_holder% §cto §f%new_holder%§c!"
                : "§cHot Potato: §f%new_holder%";
        String newHolderName = restoreHotPotatoSentenceColor(formattedPlayerName(selected));
        String previousName = restoreHotPotatoSentenceColor(previous == null ? "" : previousHotPotatoHolderDisplayName);
        String killerName = restoreHotPotatoSentenceColor(killer == null ? "" : formattedPlayerName(killer));
        for (Gamer recipient : Gamer.getGamers()) {
            if (!recipient.isOnline()) continue;
            recipient.sendMessage(message(recipient, key, fallback)
                    .replace("%player%", newHolderName)
                    .replace("%holder%", newHolderName)
                    .replace("%previous_holder%", previousName)
                    .replace("%previous_player%", previousName)
                    .replace("%old_holder%", previousName)
                    .replace("%old_player%", previousName)
                    .replace("%new_holder%", newHolderName)
                    .replace("%new_player%", newHolderName)
                    .replace("%killer%", killerName)
                    .replace("%event_name%", eventName(recipient, EventType.HOT_POTATO))
                    .replace("%event_time_remaining%", formatDuration(remainingMillis())));
        }
    }

    private void announceHotPotatoWaiting() {
        if (hotPotatoWaitingAnnounced) return;
        hotPotatoWaitingAnnounced = true;
        for (Gamer recipient : Gamer.getGamers()) {
            if (!recipient.isOnline()) continue;
            recipient.sendMessage(message(recipient, "buildffa_event_hot_potato_waiting_for_holder",
                    "§eHot Potato is waiting for an eligible holder.")
                    .replace("%event_name%", eventName(recipient, EventType.HOT_POTATO))
                    .replace("%event_time_remaining%", formatDuration(remainingMillis())));
        }
    }

    private String formattedPlayerName(Player player) {
        if (player == null) return "";
        Gamer gamer = Gamer.getGamer(player);
        String displayName = gamer == null ? null : gamer.getDisplayName();
        return displayName == null ? "" : displayName;
    }

    private String restoreHotPotatoSentenceColor(String displayName) {
        return (displayName == null ? "" : displayName) + "§c";
    }

    private Player selectDifferent(List<Player> eligible, UUID previous) {
        if (eligible.isEmpty()) return null;
        List<Player> alternatives = new ArrayList<>();
        for (Player player : eligible) if (previous == null || !previous.equals(player.getUniqueId())) alternatives.add(player);
        List<Player> pool = alternatives.isEmpty() ? eligible : alternatives;
        return pool.get(random.nextInt(pool.size()));
    }

    private void announceTarget(String key, String fallback, Player target) {
        for (Gamer recipient : Gamer.getGamers()) {
            if (!recipient.isOnline()) continue;
            recipient.sendMessage(message(recipient, key, fallback).replace("%player%", Gamer.getGamer(target).getDisplayName()));
        }
    }

    private void tickModifier(long now) {
        if (now >= nextModifierChange) rotateModifier();
        if (activeModifier == Modifier.SPEED) for (Player player : eligiblePlayers()) applyTrackedEffect(player, new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
        if (activeModifier == Modifier.JUMP) for (Player player : eligiblePlayers()) applyTrackedEffect(player, new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 0));
        if (activeModifier == Modifier.PROJECTILE_RESTOCK) restockProjectiles();
    }

    private void rotateModifier() {
        Modifier previous = activeModifier;
        clearModifier();
        Modifier[] values = Modifier.values();
        Modifier next = values[random.nextInt(values.length)];
        if (values.length > 1 && next == previous) next = values[(next.ordinal() + 1) % values.length];
        activeModifier = next;
        nextModifierChange = System.currentTimeMillis() + MODIFIER_INTERVAL_MILLIS;
        if (next == Modifier.SPEED) for (Player player : eligiblePlayers()) applyTrackedEffect(player, new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
        if (next == Modifier.JUMP) for (Player player : eligiblePlayers()) applyTrackedEffect(player, new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 0));
        if (next == Modifier.FAST_BLOCK_DECAY) game.getBlockTracker().setDecayMultiplier(0.5D);
        broadcast("buildffa_event_modifier_changed", "§eRandom Modifier: §f%modifier%", "%modifier%", next.displayName);
    }

    private void clearModifier() {
        if (activeModifier == Modifier.SPEED) clearTrackedEffect(PotionEffectType.SPEED);
        if (activeModifier == Modifier.JUMP) clearTrackedEffect(PotionEffectType.JUMP);
        if (activeModifier == Modifier.FAST_BLOCK_DECAY) game.getBlockTracker().setDecayMultiplier(1.0D);
    }

    private void restockProjectiles() {
        for (Player player : eligiblePlayers()) {
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack == null || !isAllowedProjectile(player, stack.getType()) || stack.getAmount() >= 16) continue;
                stack.setAmount(Math.min(16, stack.getAmount() + 1));
            }
        }
    }

    private boolean isAllowedProjectile(Player player, Material material) {
        if (material != Material.ARROW && material != Material.SNOW_BALL && material != Material.EGG) return false;
        for (ItemStack kitItem : game.getSelectedKit(Gamer.getGamer(player)).getKitItems().keySet()) {
            if (kitItem.getType() == material) return true;
        }
        return false;
    }

    private void applyTrackedEffect(Player player, PotionEffect effect) {
        Map<PotionEffectType, PotionEffect> playerEffects = replacedEffects.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        if (!playerEffects.containsKey(effect.getType())) playerEffects.put(effect.getType(), findEffect(player, effect.getType()));
        player.addPotionEffect(effect, true);
    }

    private PotionEffect findEffect(Player player, PotionEffectType type) {
        for (PotionEffect effect : player.getActivePotionEffects()) if (effect.getType().equals(type)) return effect;
        return null;
    }

    private void clearTrackedEffect(PotionEffectType type) {
        for (UUID playerId : new ArrayList<>(replacedEffects.keySet())) restoreEffect(playerId, type);
    }

    private void clearStreakEffects(Player player) {
        if (!isActive(EventType.STREAK_SURGE)) return;
        streakRewardedMilestones.remove(player.getUniqueId());
        for (PotionEffectType type : Arrays.asList(PotionEffectType.SPEED, PotionEffectType.REGENERATION,
                PotionEffectType.ABSORPTION, PotionEffectType.DAMAGE_RESISTANCE)) restoreEffect(player.getUniqueId(), type);
    }

    private void restoreEffect(UUID playerId, PotionEffectType type) {
        Map<PotionEffectType, PotionEffect> effects = replacedEffects.get(playerId);
        if (effects == null || !effects.containsKey(type)) return;
        Player player = Bukkit.getPlayer(playerId);
        PotionEffect previous = effects.remove(type);
        if (player != null) {
            player.removePotionEffect(type);
            if (previous != null) player.addPotionEffect(previous, true);
        }
        if (effects.isEmpty()) replacedEffects.remove(playerId);
    }

    private void clearTrackedEffects() {
        for (UUID playerId : new ArrayList<>(replacedEffects.keySet())) {
            Map<PotionEffectType, PotionEffect> effects = replacedEffects.get(playerId);
            if (effects == null) continue;
            for (PotionEffectType type : new ArrayList<>(effects.keySet())) restoreEffect(playerId, type);
        }
    }

    private boolean isEligible(Player player) {
        return isEligible(player, false);
    }

    private boolean isEligible(Player player, boolean allowDead) {
        if (player == null || !player.isOnline() || (!allowDead && player.isDead()) || player.hasMetadata("NPC") || game.isSpectating(player) || game.isBuildMode(player)) return false;
        if (transferringArena || game.getWorldConfig() == null || resettingPlayers.contains(player.getUniqueId())
                || !player.getWorld().equals(game.getWorldConfig().world())
                || Bukkit.getWorld(player.getWorld().getUID()) != player.getWorld()) return false;
        Gamer gamer = Gamer.getGamer(player);
        if (gamer == null) return false;
        if (gamer.getPlayerId() <= 0 || gamer.isVanished() || player.getGameMode() != GameMode.SURVIVAL
                || game.isLobbyZone(player.getLocation())) return false;
        try {
            return BuildFFAGamer.get(gamer) != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private List<Player> eligiblePlayers() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) if (isEligible(player)) players.add(player);
        return players;
    }

    public boolean isHillPlatform(org.bukkit.block.Block block) {
        return hill.owns(block);
    }

    public boolean isArenaParticipant(Player player) {
        return isEligible(player);
    }

    public boolean isDeathParticipant(Player player) {
        return isEligible(player, true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        removeTemporaryItems(player);
        UUID session = activeSession;
        long revision = inventoryRevision;
        if (session == null || activeEvent == null) {
            removeBossBar(player.getUniqueId());
            return;
        }
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            if (revision != inventoryRevision || transferringArena) return;
            if (!player.isOnline() || activeSession == null || !activeSession.equals(session) || activeEvent == null) {
                removeBossBar(player.getUniqueId());
                return;
            }
            if (isEligible(player)) recordParticipant(player);
            reconcileBossBar(player, System.currentTimeMillis());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        game.invalidateInventoryRequest(event.getPlayer());
        clearEventPlayer(event.getPlayer(), false);
        if (event.getPlayer().getUniqueId().equals(juggernaut)) releaseJuggernaut();
        UUID playerId = event.getPlayer().getUniqueId();
        LegacyEventBossBar bar = bossBars.remove(playerId);
        if (bar != null) bar.discard();
        pendingBossBarRemovals.remove(playerId);
        bossBarRetryAfter.remove(playerId);
        bossBarFailureSignatures.remove(playerId);
        bossBarFailureLogAfter.remove(playerId);
        contributions.remove(playerId);
        for (Map<UUID, Contribution> values : contributions.values()) values.remove(playerId);
        revengeContracts.remove(playerId);
        revengeContracts.values().removeIf(playerId::equals);
        if (playerId.equals(goldenTarget)) {
            previousGoldenTarget = goldenTarget;
            goldenTarget = null;
        }
        if (playerId.equals(hotPotatoHolder)) {
            clearHotPotatoHolder(playerId);
            selectHotPotatoHolder(playerId);
        }
        Map<PotionEffectType, PotionEffect> effects = replacedEffects.get(playerId);
        if (effects != null) for (PotionEffectType type : new ArrayList<>(effects.keySet())) restoreEffect(playerId, type);
        streakRewardedMilestones.remove(playerId);
        selections.remove(playerId);
        resettingPlayers.remove(playerId);
        eventDeaths.remove(playerId);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        beforeInventoryReset(player);
        LegacyEventBossBar bar = bossBars.get(player.getUniqueId());
        if (bar != null) bar.respawn();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossBarMove(org.bukkit.event.player.PlayerMoveEvent event) {
        LegacyEventBossBar bar = bossBars.get(event.getPlayer().getUniqueId());
        if (bar == null || pendingBossBarRemovals.contains(event.getPlayer().getUniqueId())) return;
        try {
            if (!bar.updatePosition(event.getTo()))
                logBossBarFailure(event.getPlayer(), bar.getLastOperation(), bar.getLastFailure(), System.currentTimeMillis());
        } catch (RuntimeException exception) {
            logBossBarFailure(event.getPlayer(), "movement", exception, System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        UUID session = activeSession;
        long revision = inventoryRevision;
        if (session == null || activeEvent == null || event.isCancelled()) return;
        Player player = event.getPlayer();
        Location destination = event.getTo();
        if (destination != null && (!destination.getWorld().equals(game.getWorldConfig().world()) || game.isLobbyZone(destination))) {
            clearEventPlayer(player, false);
            game.invalidateInventoryRequest(player);
        }
        if (event.getTo() != null && !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            LegacyEventBossBar bar = bossBars.get(player.getUniqueId());
            if (bar != null) bar.respawn();
            if (!transferringArena && isActive(EventType.HOT_POTATO) && player.getUniqueId().equals(hotPotatoHolder)) {
                clearHotPotatoHolder(player.getUniqueId());
                selectHotPotatoHolder(player.getUniqueId());
            }
        }
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            if (revision != inventoryRevision || transferringArena || !player.isOnline() || activeSession == null || !activeSession.equals(session) || activeEvent == null) return;
            reconcileBossBar(player, System.currentTimeMillis());
        });
    }

    @EventHandler
    public void onMapChange(GameMapChangeEvent event) {
        if (event.getGame() != game) return;
        beginArenaTransfer(event.getFrom() == null ? null : event.getFrom().world());
    }

    private void beginArenaTransfer(org.bukkit.World oldWorld) {
        hill.restore();
        hillHolder = null;
        hillStatus = "suspended";
        inventoryRevision++;
        transferringArena = true;
        transferredRouletteWeapons.putAll(rouletteWeapons);
        if (hotPotatoHolder != null && transferredHotPotatoReward == 0L)
            transferredHotPotatoReward = nextHotPotatoReward;
        for (Player player : Bukkit.getOnlinePlayers()) {
            game.invalidateInventoryRequest(player);
            clearEventPlayer(player, false);
            resettingPlayers.add(player.getUniqueId());
        }
        clearTrackedEffects();
        contributions.clear();
        eventDeaths.clear();
        creditedCandyDeaths.clear();
        removeBossBars();
        if (oldWorld != null) cleanupArenaWorld(oldWorld);
    }

    private void cleanupArenaWorld(org.bukkit.World world) {
        for (UUID id : new ArrayList<>(rouletteProjectiles.keySet())) {
            org.bukkit.entity.Entity projectile = rouletteProjectiles.get(id);
            if (projectile.getWorld().equals(world)) {
                projectile.remove();
                rouletteProjectiles.remove(id);
            }
        }
        for (org.bukkit.entity.Entity entity : world.getEntities())
            if (entity instanceof Item && EventItems.isTemporary(((Item) entity).getItemStack())) entity.remove();
        game.getBlockTracker().clearWorld(world);
    }

    public void completeArenaTransfer() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Arena transfer must run on the server thread");
        if (shuttingDown || game.getWorldConfig() == null
                || Bukkit.getWorld(game.getWorldConfig().world().getUID()) != game.getWorldConfig().world()) return;
        transferringArena = false;
        if (activeEvent == EventType.KING_OF_THE_HILL) {
            try { hillUnavailable = hill.place(); }
            catch (RuntimeException failure) {
                hill.restore();
                hillUnavailable = "placement";
                game.getPlugin().getLogger().log(Level.SEVERE, "Could not transfer hill platform", failure);
            }
            hillStatus = hillUnavailable == null ? "empty" : "suspended";
            if (hillUnavailable != null) for (Player player : Bukkit.getOnlinePlayers()) {
                Gamer gamer = Gamer.getGamer(player);
                if (gamer != null) player.sendMessage(hillUnavailableReason(gamer, hillUnavailable));
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) onInventoryReady(player);
        if (activeSession == null || activeEvent == null) return;
        if (activeEvent == EventType.KING_OF_THE_HILL) {
            if (System.currentTimeMillis() >= endTimestamp) stop(true);
        } else tick(activeSession);
        if (activeEvent == null) return;
        updateBossBars(System.currentTimeMillis());
        refreshManagerMenus();
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != game.getPlugin()) return;
        shuttingDown = true;
        cancelAutomaticScheduler();
        if (activeEvent != null) stop(false, false);
        else {
            if (task != null) task.cancel();
            task = null;
            if (bossBarTask != null) bossBarTask.cancel();
            bossBarTask = null;
            removeBossBars();
        }
        if (!pendingHillRewards.isEmpty()) game.getPlugin().getLogger().warning("Unpaid King of the Hill rewards for " + pendingHillRewards.size() + " offline players cannot survive shutdown; no persistent event reward storage exists.");
        selections.clear();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        InventoryHolder inventoryHolder = inventory.getHolder();
        if (!(inventoryHolder instanceof EventManagerHolder) && !(inventoryHolder instanceof EventStatsHolder)
                && !(inventoryHolder instanceof PlayerEventHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();
        if (inventoryHolder instanceof PlayerEventHolder) {
            PlayerEventHolder holder = (PlayerEventHolder) inventoryHolder;
            if (!holder.viewer.equals(player.getUniqueId())) return;
            if (rawSlot == PLAYER_EVENT_CLOSE_SLOT) player.closeInventory();
            return;
        }
        if (rawSlot < 0 || rawSlot >= inventory.getSize()) return;
        if (inventoryHolder instanceof EventStatsHolder) {
            EventStatsHolder statsHolder = (EventStatsHolder) inventoryHolder;
            if (!statsHolder.viewer.equals(player.getUniqueId())) return;
            if (!hasEventStatsPermission(player)) {
                Gamer gamer = Gamer.getGamer(player);
                gamer.sendMessage(message(gamer, "buildffa_event_stats_no_permission", "§cYou do not have permission to view event statistics."));
                player.closeInventory();
                return;
            }
            if (rawSlot == EVENT_STATS_CLOSE_SLOT) {
                player.closeInventory();
                return;
            }
            for (int index = 0; index < HISTORY_SLOTS.length && index < statsHolder.sessions.size(); index++) {
                if (rawSlot != HISTORY_SLOTS[index]) continue;
                EventSessionSnapshot snapshot = findSnapshot(statsHolder.sessions.get(index));
                if (snapshot != null) openEventLeaderboard(player, snapshot);
                else {
                    Gamer gamer = Gamer.getGamer(player);
                    gamer.sendMessage(message(gamer, "buildffa_event_stats_record_unavailable", "&cThis event record is no longer available."));
                    renderStatsHistory(player, inventory);
                }
                return;
            }
            return;
        }
        EventManagerHolder holder = (EventManagerHolder) inventoryHolder;
        if (!holder.viewer.equals(player.getUniqueId())) return;
        Gamer gamer = Gamer.getGamer(player);
        if (!hasEventManagerPermission(player)) {
            sendEventManagerNoPermission(player);
            player.closeInventory();
            return;
        }
        for (int index = 0; index < EVENT_SLOTS.length; index++) {
            if (rawSlot != EVENT_SLOTS[index]) continue;
            selections.put(player.getUniqueId(), EventType.values()[index]);
            renderManager(player, inventory);
            return;
        }
        if (rawSlot == START_SLOT) {
            EventType selected = selections.get(player.getUniqueId());
            if (selected == null) {
                gamer.sendMessage(message(gamer, "buildffa_event_no_selection", "§cSelect an event before using this button."));
                return;
            }
            if (!canStart(selected)) {
                gamer.sendMessage(availabilityReason(gamer, selected));
                renderManager(player, inventory);
                return;
            }
            if (!start(selected, player)) {
                gamer.sendMessage(canStart(selected)
                        ? message(gamer, "buildffa_event_start_setup_failed", "§cThe event could not initialize. See the server log for details.")
                        : availabilityReason(gamer, selected));
                renderManager(player, inventory);
                return;
            }
            selections.remove(player.getUniqueId());
            refreshManagerMenus();
            return;
        }
        if (rawSlot == STOP_SLOT) {
            EventType selected = selections.get(player.getUniqueId());
            if (selected == null) gamer.sendMessage(message(gamer, "buildffa_event_select_first", "§cSelect an event first."));
            else if (activeEvent != selected) gamer.sendMessage(message(gamer, "buildffa_event_not_active", "§cThe selected event is not currently active."));
            else stopEvent(gamer, selected.id);
            renderManager(player, inventory);
            return;
        }
        if (rawSlot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof EventManagerHolder || holder instanceof EventStatsHolder
                || holder instanceof PlayerEventHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof EventManagerHolder)) return;
        EventManagerHolder holder = (EventManagerHolder) event.getInventory().getHolder();
        if (holder.viewer.equals(event.getPlayer().getUniqueId())) selections.remove(event.getPlayer().getUniqueId());
    }

    private EventState state(EventType type) {
        if (activeEvent == type) return EventState.ACTIVE;
        if (!isAdministrativelyEnabled(type)) return EventState.DISABLED;
        if (type == EventType.KING_OF_THE_HILL && hill.validate() != null) return EventState.UNAVAILABLE;
        return EventState.ENABLED;
    }

    private boolean canStart(EventType type) {
        return type != null && startRequirement(type) == StartRequirement.READY;
    }

    public boolean hasEventManagerPermission(Player player) {
        return game.hasPermission(player, EVENT_MANAGER_PERMISSION);
    }

    public boolean hasEventStatsPermission(Player player) {
        return game.hasPermission(player, BuildFFAGame.EVENT_STATS_PERMISSION);
    }

    private void sendEventManagerNoPermission(Player player) {
        Gamer gamer = Gamer.getGamer(player);
        if (gamer != null) gamer.sendMessage(message(gamer, "buildffa_event_manager_no_permission",
                "§cYou do not have permission to use the Event Manager."));
    }

    private boolean isAdministrativelyEnabled(EventType type) {
        return Boolean.TRUE.equals(administrativeEnabled.get(type));
    }

    private StartRequirement startRequirement(EventType type) {
        if (activeEvent != null) return StartRequirement.EVENT_ACTIVE;
        if (!isAdministrativelyEnabled(type)) return StartRequirement.DISABLED;
        if (type == EventType.KING_OF_THE_HILL && hill.validate() != null) return StartRequirement.UNAVAILABLE;
        return StartRequirement.READY;
    }

    private String availabilityReason(Gamer gamer, EventType type) {
        if (type == EventType.KING_OF_THE_HILL && isAdministrativelyEnabled(type)) {
            String reason = activeEvent == type ? hillUnavailable : hill.validate();
            if (reason != null) return hillUnavailableReason(gamer, reason);
        }
        StartRequirement requirement = startRequirement(type);
        String key;
        String fallback;
        switch (requirement) {
            case EVENT_ACTIVE:
                key = activeEvent == type ? "buildffa_event_active_reason" : "buildffa_event_blocked_reason";
                fallback = activeEvent == type ? "§6This event is currently active."
                        : "§cAnother event is active: §e%event_name%§c.";
                break;
            case DISABLED:
                key = "buildffa_event_disabled_reason";
                fallback = "§cThis event has been disabled by an administrator.";
                break;
            case UNAVAILABLE:
                return hillUnavailableReason(gamer, hill.validate());
            case READY:
                key = "buildffa_event_can_start";
                fallback = "§aReady to start.";
                break;
            default:
                throw new IllegalStateException("Unknown start requirement: " + requirement);
        }
        String name = eventName(gamer, requirement == StartRequirement.EVENT_ACTIVE ? activeEvent : type);
        Object[] values = {"%event_name%", name};
        String translated = gamer.getLanguage().getMessage(key, values);
        String reason = missingTranslation(translated, key) ? fallback : translated;
        for (int index = 0; index < values.length; index += 2)
            reason = reason.replace(String.valueOf(values[index]), String.valueOf(values[index + 1]));
        return reason;
    }

    public void openStats(Player player) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> openStats(player));
            return;
        }
        Gamer gamer = Gamer.getGamer(player);
        if (!hasEventStatsPermission(player)) {
            gamer.sendMessage(message(gamer, "buildffa_event_stats_no_permission", "§cYou do not have permission to view event statistics."));
            return;
        }
        Inventory inventory = Bukkit.createInventory(new EventStatsHolder(player.getUniqueId()), 54,
                message(gamer, "buildffa_event_stats_title", "Event History"));
        renderStatsHistory(player, inventory);
        player.openInventory(inventory);
    }

    private void renderStatsHistory(Player player, Inventory inventory) {
        Gamer gamer = Gamer.getGamer(player);
        EventStatsHolder holder = (EventStatsHolder) inventory.getHolder();
        holder.sessions.clear();
        ItemStack filler = item(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        if (activeEvent != null) inventory.setItem(EVENT_STATS_ACTIVE_SLOT, activeStatsItem(gamer));
        if (history.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, (short) 0,
                    message(gamer, "buildffa_event_stats_no_history", "§cNo completed events have been recorded yet."),
                    Collections.emptyList()));
        } else {
            int index = 0;
            for (EventSessionSnapshot snapshot : history) {
                if (index >= HISTORY_SLOTS.length) break;
                inventory.setItem(HISTORY_SLOTS[index++], historyItem(gamer, snapshot));
                holder.sessions.add(snapshot.sessionId);
            }
        }
        inventory.setItem(EVENT_STATS_CLOSE_SLOT, item(Material.BARRIER, (short) 0,
                message(gamer, "buildffa_event_stats_close_name", "§cClose"), Collections.emptyList()));
    }

    private ItemStack activeStatsItem(Gamer gamer) {
        String localizedEventName = eventName(gamer, activeEvent);
        String name = message(gamer, "buildffa_event_stats_active_name", "§aActive Event: §f%event_name%")
                .replace("%event_name%", localizedEventName);
        List<String> lore = messageList(gamer, "buildffa_event_stats_active_lore", Arrays.asList(
                "§7Time remaining: §e%event_time_remaining%",
                "§7Duration: §e%event_duration%",
                "§7Participants: §e%participant_count%"));
        lore = replace(lore, "%event_time_remaining%", formatEventDuration(remainingMillis()));
        lore = replace(lore, "%event_duration%", formatEventDuration(activeDurationMillis));
        lore = replace(lore, "%participant_count%", String.valueOf(activeSessionPlayers.size()));
        lore = replace(lore, "%event_name%", localizedEventName);
        return item(Material.NETHER_STAR, (short) 0, name, lore);
    }

    private ItemStack historyItem(Gamer gamer, EventSessionSnapshot snapshot) {
        EventType type = resolveEvent(snapshot.eventId);
        String localizedEventName = type == null ? safeText(snapshot.eventId) : safeText(eventName(gamer, type));
        String startTime = new SimpleDateFormat("yyyy-MM-dd h:mm a").format(new Date(snapshot.startedAt));
        String duration = formatHistoryDuration(snapshot.durationMillis);
        String kills = String.valueOf(Math.max(0, snapshot.totalKills));
        String result = message(gamer, snapshot.completed ? "buildffa_event_stats_result_completed"
                : "buildffa_event_stats_result_stopped", snapshot.completed ? "§aCompleted" : "§cStopped");
        String name = message(gamer, "buildffa_event_stats_history_name", "§r%event_name%")
                .replace("%event_name%", localizedEventName);
        List<String> lore = messageList(gamer, "buildffa_event_stats_history_lore", Arrays.asList(
                "§7Ended: §e%event_ended_ago%",
                "§7Duration: §e%event_duration%",
                "§7Participants: §e%participant_count%",
                "",
                "§eClick to view leaderboard!"));
        lore = replace(lore, "%event_name%", localizedEventName);
        lore = replace(lore, "%event_ended_ago%", formatEndedAgo(snapshot.endedAt));
        lore = replace(lore, "%event_duration%", duration);
        lore = replace(lore, "%participant_count%", String.valueOf(snapshot.players.size()));
        lore = replace(lore, "%start_time%", startTime);
        lore = replace(lore, "%duration%", duration);
        lore = replace(lore, "%kills%", kills);
        lore = replace(lore, "%result%", safeText(result));
        return item(Material.BOOK, (short) 0, name, lore);
    }

    private void openEventLeaderboard(Player player, EventSessionSnapshot snapshot) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> openEventLeaderboard(player, snapshot));
            return;
        }
        if (!player.isOnline() || !hasEventStatsPermission(player)) return;
        EventType type = resolveEvent(snapshot.eventId);
        StandardEventLeaderboardGui gui = null;
        try {
            Gamer gamer = Gamer.getGamer(player);
            String eventName = type == null ? safeText(snapshot.eventId) : safeText(eventName(gamer, type));
            java.util.function.Consumer<StandardEventLeaderboardGui> renderer = menu -> renderEventLeaderboard(player, snapshot, menu);
            if (type == EventType.CANDY_HUNT)
                gui = new CandyHuntLeaderboardGui(player, eventName, renderer);
            else gui = new StandardEventLeaderboardGui(player, eventName, renderer);
            gui.onFailure((menu, exception) -> {
                debugEventLeaderboard(player, snapshot, menu, "framework refresh", "buildffa_event_stats_leaderboard_title", exception);
                try {
                    addLeaderboardFailure(Gamer.getGamer(player), menu, 4);
                } catch (RuntimeException indicatorFailure) {
                    debugEventLeaderboard(player, snapshot, menu, "refresh failure indicator", "buildffa_event_stats_loading_failed", indicatorFailure);
                }
            });
            gui.reopen();
            StandardEventLeaderboardGui opened = gui;
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> verifyEventLeaderboard(player, snapshot, opened));
        } catch (RuntimeException exception) {
            debugEventLeaderboard(player, snapshot, gui, "opening", "buildffa_event_stats_leaderboard_title", exception);
            try {
                player.sendMessage(color(message(Gamer.getGamer(player), "buildffa_event_stats_loading_failed", "&cCould not load this event leaderboard.")));
            } catch (RuntimeException notificationFailure) {
                debugEventLeaderboard(player, snapshot, gui, "opening failure notification", "buildffa_event_stats_loading_failed", notificationFailure);
            }
        }
    }

    private void renderEventLeaderboard(Player player, EventSessionSnapshot snapshot, StandardEventLeaderboardGui gui) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
                if (player.isOnline() && isEventLeaderboardOpen(player, gui)
                        && hasEventStatsPermission(player)) gui.refreshLeaderboard();
            });
            return;
        }
        gui.resetRenderedHeadCount();
        String key = gui.playerLoreKey();
        String operation = "background";
        try {
            Gamer gamer = Gamer.getGamer(player);
            Set<Integer> reservedSlots = new HashSet<>();
            for (int slot : LEADERBOARD_SLOTS) reservedSlots.add(slot);
            reservedSlots.add(EVENT_STATS_BACK_SLOT);
            reservedSlots.add(EVENT_STATS_CLOSE_SLOT);
            reservedSlots.add(4);
            gui.prepareBackground(item(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList()), reservedSlots);
            operation = "Back button";
            key = "buildffa_event_stats_back_name";
            addLeaderboardBack(player, gamer, gui);
            operation = "Close button";
            key = "buildffa_event_stats_close_name";
            addLeaderboardClose(player, gamer, gui);
            operation = "participant statistics";
            key = gui.playerLoreKey();
            List<LeaderboardEntry> leaderboard = leaderboard(snapshot);
            if (leaderboard.isEmpty()) gui.add(item(Material.BARRIER, (short) 0,
                    message(gamer, "buildffa_event_stats_no_participants", "&cNo participants were recorded."),
                    Collections.emptyList()), 22, event -> event.setCancelled(true));
            boolean failed = false;
            for (int position = 0; position < leaderboard.size() && position < LEADERBOARD_SLOTS.length; position++) {
                gui.beginLoreTrace(position + 1, LEADERBOARD_SLOTS[position]);
                try {
                    gui.addPlayerItem(leaderboardItem(gamer, leaderboard.get(position), position + 1, gui, snapshot, player), LEADERBOARD_SLOTS[position]);
                } catch (RuntimeException exception) {
                    failed = true;
                    debugEventLeaderboard(player, snapshot, gui, "participant " + leaderboard.get(position).identity.playerId, key, exception);
                    addLeaderboardFailure(gamer, gui, LEADERBOARD_SLOTS[position]);
                }
            }
            if (failed) addLeaderboardFailure(gamer, gui, 4);
            else gui.add(item(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList()), 4, event -> event.setCancelled(true));
            debugEventLeaderboard(player, snapshot, gui, failed ? "partial render" : "render complete", key, null);
        } catch (RuntimeException exception) {
            game.getPlugin().getLogger().log(Level.SEVERE, "Could not render BuildFFA event leaderboard " + snapshot.sessionId, exception);
            debugEventLeaderboard(player, snapshot, gui, operation, key, exception);
            try {
                addLeaderboardFailure(Gamer.getGamer(player), gui, 4);
            } catch (RuntimeException indicatorFailure) {
                debugEventLeaderboard(player, snapshot, gui, "failure indicator", "buildffa_event_stats_loading_failed", indicatorFailure);
            }
            try {
                addLeaderboardBack(player, Gamer.getGamer(player), gui);
            } catch (RuntimeException backFailure) {
                debugEventLeaderboard(player, snapshot, gui, "Back recovery", "buildffa_event_stats_back_name", backFailure);
            }
            try {
                addLeaderboardClose(player, Gamer.getGamer(player), gui);
            } catch (RuntimeException closeFailure) {
                debugEventLeaderboard(player, snapshot, gui, "Close recovery", "buildffa_event_stats_close_name", closeFailure);
            }
        }
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> captureDisplayedLeaderboardLore(player, snapshot, gui));
    }

    private void captureDisplayedLeaderboardLore(Player player, EventSessionSnapshot snapshot, StandardEventLeaderboardGui gui) {
        if (!player.isOnline() || !isEventLeaderboardOpen(player, gui)) return;
        try {
            Inventory displayed = player.getOpenInventory().getTopInventory();
            for (int slot : LEADERBOARD_SLOTS) {
                ItemStack head = displayed.getItem(slot);
                ItemMeta meta = head != null && head.getType() == Material.SKULL_ITEM && head.getDurability() == 3
                        ? head.getItemMeta() : null;
                gui.recordDisplayedLore(slot, meta == null ? null : meta.getLore());
            }
            debugEventLeaderboard(player, snapshot, gui, "lore stage comparison", gui.playerLoreKey(), null);
        } catch (RuntimeException exception) {
            debugEventLeaderboard(player, snapshot, gui, "lore stage capture", gui.playerLoreKey(), exception);
        }
    }

    private void addLeaderboardBack(Player player, Gamer gamer, StandardEventLeaderboardGui gui) {
        gui.add(item(Material.ARROW, (short) 0,
                message(gamer, "buildffa_event_stats_back_name", "&eBack"), Collections.emptyList()), EVENT_STATS_BACK_SLOT, event -> {
            event.setCancelled(true);
            if (!hasEventStatsPermission(player)) player.closeInventory();
            else openStats(player);
        });
    }

    private void addLeaderboardClose(Player player, Gamer gamer, StandardEventLeaderboardGui gui) {
        gui.add(item(Material.BARRIER, (short) 0,
                message(gamer, "buildffa_event_stats_close_name", "&cClose"), Collections.emptyList()), EVENT_STATS_CLOSE_SLOT, event -> {
            event.setCancelled(true);
            player.closeInventory();
        });
    }

    private void addLeaderboardFailure(Gamer gamer, StandardEventLeaderboardGui gui, int slot) {
        String name;
        try {
            name = message(gamer, "buildffa_event_stats_loading_failed", "&cCould not load this event leaderboard.");
        } catch (RuntimeException exception) {
            name = "&cCould not load this event leaderboard.";
        }
        gui.add(item(Material.BARRIER, (short) 0, name, Collections.emptyList()), slot, event -> event.setCancelled(true));
    }

    private ItemStack leaderboardItem(Gamer gamer, LeaderboardEntry entry, int position, StandardEventLeaderboardGui gui,
                                      EventSessionSnapshot snapshot, Player viewer) {
        String playerName = safeText(leaderboardDisplayName(entry.identity));
        String formattedPosition = String.valueOf(position);
        Object[] replacements = {"%position%", formattedPosition, "%player%", playerName};
        String nameKey = "buildffa_event_stats_player_name";
        String name = gamer.getLanguage().getMessage(nameKey, replacements);
        if (missingTranslation(name, nameKey)) name = "§6#%position% §e%player%";
        List<String> lore = gui.playerLore(entry.kills, entry.score.candyCollected, entry.eventsPlayed);
        if (snapshot.eventId.equals(EventType.KING_OF_THE_HILL.id)) lore.add(0, color(message(gamer, "buildffa_event_king_of_the_hill_points", "&7Capture points: &e%points%").replace("%points%", String.valueOf(entry.score.capturePoints))));
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        try {
            if (!safeText(entry.identity.playerName).isEmpty() && meta.setOwner(entry.identity.playerName)) {
                if (!head.setItemMeta(meta)) throw new IllegalStateException("Could not apply player skin");
                meta = (SkullMeta) head.getItemMeta();
            } else meta = (SkullMeta) new ItemStack(Material.SKULL_ITEM, 1, (short) 3).getItemMeta();
        } catch (RuntimeException exception) {
            game.getPlugin().getLogger().log(Level.WARNING, "Could not load leaderboard skin for " + entry.identity.playerId, exception);
            debugEventLeaderboard(viewer, snapshot, gui, "skin fallback " + entry.identity.playerId, nameKey, exception);
            head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            meta = (SkullMeta) head.getItemMeta();
        }
        meta.setDisplayName(color(name.replace("%position%", formattedPosition).replace("%player%", playerName)));
        meta.setLore(lore);
        if (!head.setItemMeta(meta) || !lore.equals(head.getItemMeta().getLore() == null
                ? Collections.emptyList() : head.getItemMeta().getLore())) {
            game.getPlugin().getLogger().severe("Could not apply localized leaderboard lore");
            throw new IllegalStateException("Leaderboard item lost its localized lore");
        }
        return head;
    }

    private void verifyEventLeaderboard(Player player, EventSessionSnapshot snapshot, StandardEventLeaderboardGui gui) {
        if (!player.isOnline()) return;
        Inventory displayed = player.getOpenInventory().getTopInventory();
        if (!isEventLeaderboardOpen(player, gui)) return;
        try {
            int heads = 0;
            boolean metadataValid = true;
            for (int slot : LEADERBOARD_SLOTS) {
                ItemStack head = displayed.getItem(slot);
                if (head == null || head.getType() != Material.SKULL_ITEM || head.getDurability() != 3) {
                    gui.recordDisplayedLore(slot, null);
                    continue;
                }
                heads++;
                ItemMeta meta = head.getItemMeta();
                gui.recordDisplayedLore(slot, meta == null ? null : meta.getLore());
                metadataValid &= meta != null && meta.hasDisplayName() && meta.hasLore();
                if (meta != null && meta.hasDisplayName())
                    metadataValid &= !meta.getDisplayName().contains("%position%") && !meta.getDisplayName().contains("%player%");
                if (meta != null && meta.hasLore()) {
                    metadataValid &= meta.getLore().size() >= (gui instanceof CandyHuntLeaderboardGui ? 3 : 2);
                    for (String row : meta.getLore()) metadataValid &= !row.contains("%kills%")
                            && !row.contains("%events_played%") && !row.contains("%event_candy_hunt_collected%");
                }
            }
            ItemStack back = displayed.getItem(EVENT_STATS_BACK_SLOT);
            ItemStack close = displayed.getItem(EVENT_STATS_CLOSE_SLOT);
            boolean navigationValid = back != null && back.getType() == Material.ARROW
                    && close != null && close.getType() == Material.BARRIER;
            String operation = "displayed heads=" + heads + ", navigation=" + navigationValid + ", metadata=" + metadataValid;
            if (displayed.getSize() != 54 || heads != gui.renderedHeadCount() || !navigationValid || !metadataValid) {
                debugEventLeaderboard(player, snapshot, gui, operation, gui.playerLoreKey(),
                        new IllegalStateException("Displayed leaderboard differs from rendered entries"));
                addLeaderboardFailure(Gamer.getGamer(player), gui, 4);
                gui.reopen();
            } else debugEventLeaderboard(player, snapshot, gui, operation, gui.playerLoreKey(), null);
        } catch (RuntimeException exception) {
            debugEventLeaderboard(player, snapshot, gui, "display verification", gui.playerLoreKey(), exception);
        }
    }

    private boolean isEventLeaderboardOpen(Player player, StandardEventLeaderboardGui gui) {
        Inventory displayed = player.getOpenInventory().getTopInventory();
        return displayed.getHolder() == gui || displayed.equals(gui.getInventory());
    }

    private void debugEventLeaderboard(Player player, EventSessionSnapshot snapshot, StandardEventLeaderboardGui gui,
                                       String operation, String key, Throwable exception) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> debugEventLeaderboard(player, snapshot, gui, operation, key, exception));
            return;
        }
        try {
            String guiClass = gui == null ? snapshot.eventId.equals(EventType.CANDY_HUNT.id)
                    ? "CandyHuntLeaderboardGui" : "StandardEventLeaderboardGui" : gui.getClass().getSimpleName();
            String loreKey = snapshot.eventId.equals(EventType.CANDY_HUNT.id)
                    ? "buildffa_event_stats_player_candy_hunt_lore" : "buildffa_event_stats_player_lore";
            Throwable root = exception;
            for (int depth = 0; root != null && root.getCause() != null && root.getCause() != root && depth < 16; depth++)
                root = root.getCause();
            String cause = exception == null ? "none" : exception.getClass().getSimpleName();
            if (root != null) {
                cause += " -> " + root.getClass().getSimpleName();
                StackTraceElement[] trace = root.getStackTrace();
                if (trace.length > 0) cause += " at " + trace[0];
            }
            int rendered = gui == null ? 0 : gui.renderedHeadCount();
            String loreRows = gui == null ? "[]" : gui.renderedLoreRowCounts().toString();
            List<String> loreTrace = gui == null ? Collections.emptyList() : gui.loreDiagnosticLines();
            String signature = loreTrace.hashCode() + "|" + operation + '|' + key + '|' + rendered + '|' + loreRows + '|' + cause;
            if (gui != null && !gui.reportDiagnostic(signature)) return;
            List<String> diagnosticLines = new ArrayList<>(Arrays.asList(
                    "&6[BuildFFA event leaderboard] &7session=&f" + snapshot.sessionId
                            + " &7gui=&f" + guiClass + " &7participants=&f" + snapshot.players.size() + " &7rendered_heads=&f" + rendered,
                    "&7stage=&f" + operation + " &7key=&f" + key
                            + " &7name_key=&fbuildffa_event_stats_player_name &7lore_key=&f" + loreKey,
                    "&7viewer=&f" + player.getUniqueId() + " &7time=&f" + new Date()
                            + " &7lore_rows=&f" + loreRows + " &7cause=&f" + cause
            ));
            diagnosticLines.addAll(loreTrace);
            LeaderboardDiagnostic diagnostic = new LeaderboardDiagnostic(player.getUniqueId(), diagnosticLines.toArray(new String[0]));
            leaderboardDiagnostics.addLast(diagnostic);
            while (leaderboardDiagnostics.size() > 64) leaderboardDiagnostics.removeFirst();
            if (exception != null) {
                leaderboardErrors.addLast(diagnostic);
                while (leaderboardErrors.size() > 16) leaderboardErrors.removeFirst();
            }
        } catch (RuntimeException diagnosticFailure) {
            game.getPlugin().getLogger().warning("Could not record event leaderboard diagnostic: " + diagnosticFailure.getClass().getName());
        }
    }

    public void showDebug(Player player) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> showDebug(player));
            return;
        }
        if (!player.isOnline()) return;
        if (!game.hasPermission(player, "buildffa.debug")) {
            player.sendMessage(color(message(Gamer.getGamer(player), "buildffa_debug_no_permission",
                    "&cYou do not have permission to use BuildFFA diagnostics.")));
            return;
        }
        for (String line : hill.diagnostics()) player.sendMessage(line);
        if (hillUnavailable != null) player.sendMessage("Hill capture suspension reason: " + hillUnavailable);
        LeaderboardDiagnostic latest = null;
        Iterator<LeaderboardDiagnostic> diagnostics = leaderboardDiagnostics.descendingIterator();
        while (diagnostics.hasNext()) {
            LeaderboardDiagnostic candidate = diagnostics.next();
            if (candidate.viewer.equals(player.getUniqueId())) {
                latest = candidate;
                break;
            }
        }
        if (latest == null && !leaderboardDiagnostics.isEmpty()) latest = leaderboardDiagnostics.getLast();
        player.sendMessage(color("&6BuildFFA diagnostics &7(latest leaderboard result; recent server rendering errors)"));
        if (latest == null) player.sendMessage(color("&7No event leaderboard diagnostics have been recorded. Open a selected event in /eventstats first."));
        else for (String line : latest.lines) player.sendMessage(color(line));
        player.sendMessage(color("&6Recent rendering errors: &f" + leaderboardErrors.size() + " &7(maximum 16, newest first)"));
        Iterator<LeaderboardDiagnostic> errors = leaderboardErrors.descendingIterator();
        while (errors.hasNext()) {
            for (String line : errors.next().lines) player.sendMessage(color(line));
        }
    }

    private static final class LeaderboardDiagnostic {
        private final UUID viewer;
        private final String[] lines;

        private LeaderboardDiagnostic(UUID viewer, String[] lines) {
            this.viewer = viewer;
            this.lines = lines;
        }
    }

    private void recordKill(Player player) {
        UUID playerId = player.getUniqueId();
        activeSessionKills.put(playerId, activeSessionKills.getOrDefault(playerId, 0) + 1);
        recordParticipant(player);
    }

    private EventScore score(Player player) {
        recordParticipant(player);
        return eventScores.computeIfAbsent(player.getUniqueId(), ignored -> new EventScore());
    }

    private void sendNewEventMessage(Player player, String key, Object... replacements) {
        Gamer gamer = Gamer.getGamer(player);
        if (gamer == null) return;
        EventScore score = eventScores.getOrDefault(player.getUniqueId(), new EventScore());
        Map<String, Object> resolved = new LinkedHashMap<>();
        resolved.put("%player%", formattedPlayerName(player));
        resolved.put("%event_name%", eventName(gamer, activeEvent));
        resolved.put("%event_time_remaining%", formatDuration(remainingMillis()));
        resolved.put("%event_duration_lasts%", formatEventDuration(activeDurationMillis));
        resolved.put("%points%", String.valueOf(score.capturePoints));
        resolved.put("%candy_collected%", String.valueOf(score.candyCollected));
        resolved.put("%candy_consumed%", String.valueOf(score.candyConsumed));
        resolved.put("%candy_retained%", String.valueOf(score.retained()));
        for (int index = 0; index + 1 < replacements.length; index += 2)
            resolved.put(String.valueOf(replacements[index]), replacements[index + 1]);
        List<Object> values = new ArrayList<>();
        resolved.forEach((placeholder, replacement) -> {
            values.add(placeholder);
            values.add(replacement);
        });
        String value = gamer.getLanguage().getMessage(key, values.toArray());
        if (!missingTranslation(value, key)) gamer.sendMessage(color(value));
    }

    private void tickNewEvents(long now) {
        if (eventSaveFilter != null && !eventSaveFilter.isAttached()) {
            eventSaveFilter.close();
            eventSaveFilter = new EventSaveFilter(this::saveWithoutTemporaryState);
        }
        rouletteProjectiles.values().removeIf(projectile -> !projectile.isValid());
        for (UUID id : new ArrayList<>(ownedEffects.keySet())) {
            Player player = Bukkit.getPlayer(id);
            Map<PotionEffectType, OwnedEffect> effects = ownedEffects.get(id);
            if (player == null) {
                ownedEffects.remove(id);
                continue;
            }
            for (PotionEffectType type : new ArrayList<>(effects.keySet())) {
                OwnedEffect effect = effects.get(type);
                long effectNow = player.getTicksLived();
                if (effectNow < effect.until) continue;
                PotionEffect current = findEffect(player, type);
                if (current == null && effect.previous != null && effect.previousUntil > effectNow)
                    player.addPotionEffect(new PotionEffect(type, (int) (effect.previousUntil - effectNow),
                            effect.previous.getAmplifier(), effect.previous.isAmbient(), effect.previous.hasParticles()), true);
                effects.remove(type);
            }
            if (effects.isEmpty()) ownedEffects.remove(id);
        }
        if (isActive(EventType.JUGGERNAUT)) {
            Player holder = juggernaut == null ? null : Bukkit.getPlayer(juggernaut);
            if (!isEligible(holder)) releaseJuggernaut();
            if (juggernaut == null) {
                Player next = pendingJuggernaut == null ? null : Bukkit.getPlayer(pendingJuggernaut);
                pendingJuggernaut = null;
                if (isEligible(next)) assignJuggernaut(next);
                else selectJuggernaut();
            } else {
                applyOwnedEffect(holder, new PotionEffect(PotionEffectType.SPEED, 60, 0));
                applyOwnedEffect(holder, new PotionEffect(PotionEffectType.JUMP, 60, 0));
                holder.getWorld().playEffect(holder.getLocation().add(0, 2, 0), org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
            }
        }
        if (isActive(EventType.WEAPON_ROULETTE) || isActive(EventType.CANDY_HUNT)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!isEligible(player)) {
                    clearEventPlayer(player, false);
                    continue;
                }
                if (isActive(EventType.WEAPON_ROULETTE) && !rouletteWeapons.containsKey(player.getUniqueId()))
                    giveRouletteWeapon(player);
                if (isActive(EventType.CANDY_HUNT)) syncCandyItem(player);
            }
        }
    }

    private void selectJuggernaut() {
        List<Player> players = eligiblePlayers();
        if (!players.isEmpty()) assignJuggernaut(players.get(random.nextInt(players.size())));
    }

    private void assignJuggernaut(Player player) {
        if (!isEligible(player) || player.getUniqueId().equals(juggernaut)) return;
        releaseJuggernaut();
        juggernaut = player.getUniqueId();
        juggernautMaxHealth = player.getMaxHealth();
        juggernautSelectedAt = System.currentTimeMillis();
        score(player).timesSelected++;
        JuggernautHealth.add(player);
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 8.0D));
        applyOwnedEffect(player, new PotionEffect(PotionEffectType.SPEED, 60, 0));
        applyOwnedEffect(player, new PotionEffect(PotionEffectType.JUMP, 60, 0));
        for (Player viewer : Bukkit.getOnlinePlayers()) if (isBuildFFAPlayer(viewer))
            sendNewEventMessage(viewer, "buildffa_event_juggernaut_selected", "%player%", formattedPlayerName(player),
                    "%juggernaut%", formattedPlayerName(player));
        refreshManagerMenus();
    }

    private void releaseJuggernaut() {
        if (juggernaut == null) return;
        UUID previous = juggernaut;
        EventScore score = eventScores.get(previous);
        if (score != null) score.timeAsJuggernaut += Math.max(0L, System.currentTimeMillis() - juggernautSelectedAt);
        Player player = Bukkit.getPlayer(previous);
        if (player != null) {
            JuggernautHealth.remove(player);
            restoreOwnedEffects(player);
        }
        juggernaut = null;
        juggernautSelectedAt = 0L;
    }

    private void applyOwnedEffect(Player player, PotionEffect effect) {
        long now = player.getTicksLived();
        PotionEffect current = findEffect(player, effect.getType());
        if (current != null && (current.getAmplifier() > effect.getAmplifier()
                || current.getAmplifier() == effect.getAmplifier() && current.getDuration() >= effect.getDuration())) return;
        Map<PotionEffectType, OwnedEffect> effects = ownedEffects.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        OwnedEffect existing = effects.get(effect.getType());
        PotionEffect previous = current;
        long previousUntil = current == null ? now : now + current.getDuration();
        if (existing != null && existing.owns(current, now)) {
            previous = existing.previous;
            previousUntil = existing.previousUntil;
        }
        player.addPotionEffect(effect, true);
        effects.put(effect.getType(), new OwnedEffect(previous, previousUntil, effect, now + effect.getDuration()));
    }

    private void restoreOwnedEffects(Player player) {
        Map<PotionEffectType, OwnedEffect> effects = ownedEffects.remove(player.getUniqueId());
        if (effects == null) return;
        long now = player.getTicksLived();
        for (Map.Entry<PotionEffectType, OwnedEffect> entry : effects.entrySet()) {
            OwnedEffect effect = entry.getValue();
            PotionEffect current = findEffect(player, entry.getKey());
            if (!effect.owns(current, now) && !(current == null && now >= effect.until)) continue;
            player.removePotionEffect(entry.getKey());
            if (effect.previous != null && effect.previousUntil > now)
                player.addPotionEffect(new PotionEffect(entry.getKey(), (int) (effect.previousUntil - now),
                        effect.previous.getAmplifier(), effect.previous.isAmbient(), effect.previous.hasParticles()), true);
        }
    }

    private boolean combatWeapon(ItemStack stack) {
        if (stack == null) return false;
        return stack.getType().name().endsWith("_SWORD") || stack.getType().name().endsWith("_AXE")
                || stack.getType() == Material.BOW;
    }

    private void giveRouletteWeapon(Player player) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Roulette equipment must be given on the server thread");
        if (!isActive(EventType.WEAPON_ROULETTE) || !isEligible(player)) return;
        UUID id = player.getUniqueId();
        int previous = rouletteWeapons.getOrDefault(id, -1);
        Integer transferred = transferredRouletteWeapons.get(id);
        int selected = transferred == null ? random.nextInt(roulettePool.length - (previous >= 0 ? 1 : 0)) : transferred;
        if (transferred == null && previous >= 0 && selected >= previous) selected++;
        if (roulettePool[selected].template.getType() == Material.BOW) {
            int available = 0;
            for (int index = 0; index < 36; index++) {
                ItemStack stack = player.getInventory().getItem(index);
                if (stack == null || stack.getType() == Material.AIR || combatWeapon(stack) || EventItems.isKind(stack, "roulette") || EventItems.isKind(stack, "arrows")) available++;
            }
            if (available < 2) {
                List<Integer> choices = new ArrayList<>();
                for (int index = 0; index < roulettePool.length; index++) if (roulettePool[index].template.getType() != Material.BOW && index != previous) choices.add(index);
                selected = choices.get(random.nextInt(choices.size()));
            }
        }
        ItemStack weapon = roulettePool[selected].template.clone();
        weapon = club.abelix.api.base.util.item.ItemUtil.getBuilder(weapon).setUnbreakable().build();
        weapon = EventItems.mark(weapon, "roulette", activeSession, id);
        ItemStack arrows = weapon.getType() == Material.BOW ? EventItems.mark(new ItemStack(Material.ARROW, 16), "arrows", activeSession, id) : null;
        int slot = rouletteSlots.getOrDefault(id, -1);
        for (int index = 0; index < 36; index++) {
            ItemStack stack = player.getInventory().getItem(index);
            if (combatWeapon(stack)) {
                if (slot < 0) slot = index;
                player.getInventory().setItem(index, null);
            } else if (EventItems.isKind(stack, "roulette") || EventItems.isKind(stack, "arrows")) player.getInventory().setItem(index, null);
        }
        if (slot < 0 || slot >= 36 || player.getInventory().getItem(slot) != null) slot = player.getInventory().firstEmpty();
        if (slot < 0 || slot >= 36) return;
        String weaponName = message(Gamer.getGamer(player), "buildffa_event_weapon_" + roulettePool[selected].id, weapon.getType().name());
        ItemMeta meta = weapon.getItemMeta();
        meta.setDisplayName(color(weaponName));
        weapon.setItemMeta(meta);
        player.getInventory().setItem(slot, weapon);
        rouletteWeapons.put(id, selected);
        rouletteSlots.put(id, slot);
        transferredRouletteWeapons.remove(id);
        if (transferred == null) score(player).weaponsReceived++;
        if (roulettePool[selected].template.getType() == Material.BOW) {
            int arrowSlot = player.getInventory().firstEmpty();
            if (arrowSlot >= 0 && arrowSlot < 36)
                player.getInventory().setItem(arrowSlot, arrows);
        }
        if (transferred == null) sendNewEventMessage(player, "buildffa_event_weapon_roulette_received", "%weapon%", weaponName,
                "%kills%", String.valueOf(score(player).rouletteKills));
        player.updateInventory();
    }

    public void onInventoryReady(Player player) {
        if (!Bukkit.isPrimaryThread()) {
            long revision = inventoryRevision;
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
                if (revision == inventoryRevision) onInventoryReady(player);
            });
            return;
        }
        payHillRewards(player);
        resettingPlayers.remove(player.getUniqueId());
        if (!isEligible(player)) return;
        if (isActive(EventType.WEAPON_ROULETTE) && !rouletteWeapons.containsKey(player.getUniqueId())) giveRouletteWeapon(player);
        if (isActive(EventType.CANDY_HUNT)) syncCandyItem(player);
        if (activeEvent != null) reconcileBossBar(player, System.currentTimeMillis());
    }

    public void beforeInventoryReset(Player player) {
        clearEventPlayer(player, false);
        resettingPlayers.add(player.getUniqueId());
    }

    public void beforeServerTransfer(Player player) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Transfer cleanup must run on the server thread");
        beforeInventoryReset(player);
        game.invalidateInventoryRequest(player);
    }

    private void saveWithoutTemporaryState(Player player, Runnable save) {
        UUID session = activeSession;
        Map<Integer, ItemStack> temporary = new HashMap<>();
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (EventItems.isTemporary(stack)) {
                temporary.put(slot, stack);
                player.getInventory().setItem(slot, null);
            }
        }
        ItemStack cursor = player.getItemOnCursor();
        if (EventItems.isTemporary(cursor)) player.setItemOnCursor(null);
        Map<Integer, ItemStack> ender = new HashMap<>();
        for (int slot = 0; slot < player.getEnderChest().getSize(); slot++) {
            ItemStack stack = player.getEnderChest().getItem(slot);
            if (EventItems.isTemporary(stack)) {
                ender.put(slot, stack);
                player.getEnderChest().setItem(slot, null);
            }
        }
        double health = player.getHealth();
        boolean holder = player.getUniqueId().equals(juggernaut);
        long started = player.getTicksLived();
        Map<PotionEffectType, PotionEffect> installed = new HashMap<>();
        Map<PotionEffectType, OwnedEffect> owned = ownedEffects.getOrDefault(player.getUniqueId(), Collections.emptyMap());
        try {
            if (holder) {
                JuggernautHealth.remove(player);
            }
            for (Map.Entry<PotionEffectType, OwnedEffect> entry : owned.entrySet()) {
                PotionEffect current = findEffect(player, entry.getKey());
                OwnedEffect effect = entry.getValue();
                if (!effect.owns(current, started)) continue;
                installed.put(entry.getKey(), current);
                player.removePotionEffect(entry.getKey());
                if (effect.previous != null && effect.previousUntil > started)
                    player.addPotionEffect(new PotionEffect(entry.getKey(), (int) (effect.previousUntil - started),
                            effect.previous.getAmplifier(), effect.previous.isAmbient(), effect.previous.hasParticles()), true);
            }
            save.run();
        } finally {
            if (session != null && session.equals(activeSession)) {
                temporary.forEach((slot, stack) -> {
                    ItemStack current = player.getInventory().getItem(slot);
                    if (current == null || current.getType() == Material.AIR) player.getInventory().setItem(slot, stack);
                });
                ItemStack currentCursor = player.getItemOnCursor();
                if (EventItems.isTemporary(cursor) && (currentCursor == null || currentCursor.getType() == Material.AIR)) player.setItemOnCursor(cursor);
                ender.forEach((slot, stack) -> {
                    ItemStack current = player.getEnderChest().getItem(slot);
                    if (current == null || current.getType() == Material.AIR) player.getEnderChest().setItem(slot, stack);
                });
                if (holder && player.getUniqueId().equals(juggernaut)) {
                    JuggernautHealth.add(player);
                    player.setHealth(Math.min(health, player.getMaxHealth()));
                }
                for (Map.Entry<PotionEffectType, PotionEffect> entry : installed.entrySet()) {
                    PotionEffect effect = entry.getValue();
                    int duration = effect.getDuration() - (int) (player.getTicksLived() - started);
                    player.removePotionEffect(entry.getKey());
                    if (duration > 0) player.addPotionEffect(new PotionEffect(entry.getKey(), duration,
                            effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()), true);
                }
                if (!temporary.isEmpty()) player.updateInventory();
            }
        }
    }

    private void clearEventPlayer(Player player, boolean restoreLoadout) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof CandyMenuHolder) player.closeInventory();
        UUID id = player.getUniqueId();
        boolean hadWeapon = rouletteWeapons.remove(id) != null;
        rouletteSlots.remove(id);
        candySlots.remove(id);
        candyInteractions.remove(id);
        removeTemporaryItems(player);
        for (UUID projectileId : new ArrayList<>(rouletteProjectiles.keySet())) {
            org.bukkit.entity.Entity projectile = rouletteProjectiles.get(projectileId);
            if (projectile instanceof org.bukkit.entity.Projectile && ((org.bukkit.entity.Projectile) projectile).getShooter() instanceof Player
                    && id.equals(((Player) ((org.bukkit.entity.Projectile) projectile).getShooter()).getUniqueId())) {
                projectile.remove();
                rouletteProjectiles.remove(projectileId);
            }
        }
        restoreOwnedEffects(player);
        if (id.equals(juggernaut)) {
            pendingJuggernaut = id;
            releaseJuggernaut();
        }
        if (restoreLoadout && hadWeapon && player.isOnline() && isEligible(player)) {
            game.getSelectedKit(Gamer.getGamer(player)).restoreCombatItems(Gamer.getGamer(player), game);
        }
    }

    private void removeTemporaryItems(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++)
            if (EventItems.isTemporary(player.getInventory().getItem(slot))) player.getInventory().setItem(slot, null);
        if (EventItems.isTemporary(player.getItemOnCursor())) player.setItemOnCursor(null);
        for (int slot = 0; slot < player.getEnderChest().getSize(); slot++)
            if (EventItems.isTemporary(player.getEnderChest().getItem(slot))) player.getEnderChest().setItem(slot, null);
    }

    private void awardCandy(GameDeathEvent event) {
        if (!creditedCandyDeaths.add(event)) return;
        Player player = event.getKiller();
        EventScore score = score(player);
        int count = Math.min(1 + random.nextInt(3), Integer.MAX_VALUE - score.candyCollected);
        long now = System.currentTimeMillis();
        for (int index = 0; index < count; index++) {
            CandyType type = CandyType.values()[random.nextInt(CandyType.values().length)];
            score.collectCandy(type);
            score.retainedReachedAt.putIfAbsent(score.retained(), now);
        }
        score.reachedAt = score.retainedReachedAt.getOrDefault(score.retained(), Long.MAX_VALUE);
        refreshCandy(player);
        player.sendMessage(candyText(player, "buildffa_event_candy_collected", candyValues(player, null, count)));
    }

    private Object[] candyValues(Player player, CandyType type, int amount) {
        EventScore score = score(player);
        Gamer gamer = Gamer.getGamer(player);
        String name = candyText(player, candyKey(type, "name"), new Object[0]);
        String[] effects = {"", ""};
        String[] durations = {"", ""};
        if (type != null) for (int index = 0; index < type.effects.length; index++) {
            String key = "buildffa_event_candy_effect_" + type.effectIds[index];
            effects[index] = message(gamer, key, candyFallback(key));
            durations[index] = String.valueOf(type.effects[index].getDuration() / 20);
        }
        String effect = effects[0] + (effects[1].isEmpty() ? "" : ", " + effects[1]);
        return new Object[]{"%candy_type%", name, "%amount%", String.valueOf(amount),
                "%candy_collected%", String.valueOf(score.candyCollected),
                "%candy_consumed%", String.valueOf(score.candyConsumed), "%candy_retained%", String.valueOf(score.retained()),
                "%effect%", effect, "%duration%", type == null ? "" : type.duration,
                "%effect_1%", effects[0], "%duration_1%", durations[0],
                "%effect_2%", effects[1], "%duration_2%", durations[1]};
    }

    private String candyFallback(String key) {
        switch (key) {
            case "buildffa_event_candy_basket_name": return "&dCandy Basket";
            case "buildffa_event_candy_chocolate_name": return "&6Chocolate";
            case "buildffa_event_candy_corn_name": return "&eCandy Corn";
            case "buildffa_event_candy_caramel_apple_name": return "&cCaramel Apple";
            case "buildffa_event_candy_menu_title": return "&8Candy Hunt";
            case "buildffa_event_candy_consume_warning": return "&cConsuming candy removes one retained event point.";
            case "buildffa_event_candy_collected": return "&dCollected %amount% candy. &7Retained: %candy_retained%.";
            case "buildffa_event_candy_consumed": return "&dConsumed %candy_type%: %effect% (%duration% seconds). &7Retained: %candy_retained%.";
            case "buildffa_event_candy_none_available": return "&cYou have no %candy_type% available.";
            case "buildffa_event_candy_effect_jump_one": return "Jump Boost I";
            case "buildffa_event_candy_effect_speed": return "Speed II";
            case "buildffa_event_candy_effect_regeneration_two": return "Regeneration II";
            case "buildffa_event_candy_effect_jump": return "Jump Boost II";
            default: return "";
        }
    }

    private String candyFormat(String text, Object[] values) {
        for (int index = 0; index + 1 < values.length; index += 2)
            text = text.replace(String.valueOf(values[index]), String.valueOf(values[index + 1]));
        return color(text.replaceAll("%[a-z0-9_]+%", ""));
    }

    private String candyText(Player player, String key, Object[] values) {
        String text = Gamer.getGamer(player).getLanguage().getMessage(key, values);
        if (missingTranslation(text, key) || text.trim().isEmpty()
                || key.endsWith("_name") && "Head".equalsIgnoreCase(ChatColor.stripColor(color(text)).trim()))
            text = candyFallback(key);
        if (key.endsWith("_name")) return candyFormat(splitCandyLore(text).get(0), values);
        return candyFormat(text, values).replaceAll("(?i)<br\\s*/?\\s*>|\\r?\\n|\\\\n", " ");
    }

    private static List<String> splitCandyLore(String text) {
        return Arrays.asList(text.split("(?i)<br\\s*/?\\s*>|\\r?\\n|\\\\n", -1));
    }

    private String candyKey(CandyType type, String suffix) {
        String id = type == null ? "basket" : type == CandyType.CANDY_CORN ? "corn" : type.id;
        return "buildffa_event_candy_" + id + "_" + suffix;
    }

    private List<String> candyLore(Player player, CandyType type, Object[] values) {
        String key = candyKey(type, "lore");
        List<String> lines = messageList(Gamer.getGamer(player), key, Collections.emptyList());
        if (lines.isEmpty()) {
            String single = message(Gamer.getGamer(player), key, "");
            if (!single.trim().isEmpty()) lines = Collections.singletonList(single);
        }
        if (lines.isEmpty()) {
            if (type == null) lines = Arrays.asList("&7Collected: &f%candy_collected%", "&7Consumed: &f%candy_consumed%",
                    "&7Retained: &f%candy_retained%", "&eRight-click to choose candy.");
            else lines = Arrays.asList("&7Available: &f%amount%", "&7Effect: &f%effect%", "&7Duration: &f%duration% seconds");
        }
        List<String> lore = new ArrayList<>();
        for (String line : lines) if (line != null) for (String row : splitCandyLore(line)) {
            if (type != null && type.effects.length < 2 && (row.contains("%effect_2%") || row.contains("%duration_2%"))) continue;
            lore.add(candyFormat(row, values));
        }
        if (type == null) {
            String name = Gamer.getGamer(player).getLanguage().getMessage(candyKey(null, "name"), values);
            if (!missingTranslation(name, candyKey(null, "name"))) {
                List<String> nameRows = splitCandyLore(candyFormat(name, values));
                for (int index = 1; index < nameRows.size(); index++)
                    if (!nameRows.get(index).trim().isEmpty() && !lore.contains(nameRows.get(index))) lore.add(nameRows.get(index));
            }
        }
        if (type != null) lore.add(candyText(player, "buildffa_event_candy_consume_warning", values));
        return lore;
    }

    private ItemStack candyItem(Player player, CandyType type, int amount) {
        String id = type == null ? "basket" : type.id;
        Object[] values = candyValues(player, type, amount);
        String texture = type == null ? CANDY_BASKET_TEXTURE : type.texture;
        try {
            ItemStack head = EventItems.candyHead(texture, id, activeSession, player.getUniqueId());
            ItemMeta meta = head.getItemMeta();
            meta.setDisplayName(candyText(player, candyKey(type, "name"), values));
            meta.setLore(candyLore(player, type, values));
            if (!head.setItemMeta(meta)) throw new IllegalStateException("Cannot decorate Candy Hunt skull");
            head.setAmount(Math.max(1, Math.min(64, amount)));
            head = EventItems.applyCandyTexture(head, texture);
            return head;
        } catch (RuntimeException exception) {
            game.getPlugin().getLogger().log(Level.SEVERE, "Could not create textured Candy Hunt " + id, exception);
            throw exception;
        }
    }

    private void syncCandyItem(Player player) {
        if (!isActive(EventType.CANDY_HUNT)) return;
        UUID id = player.getUniqueId();
        Integer slot = null;
        for (int index = 0; index < player.getInventory().getSize(); index++) {
            if (!EventItems.matchesCandy(player.getInventory().getItem(index), "basket", activeSession, id)) continue;
            if (slot == null && index < 36) slot = index;
            else player.getInventory().setItem(index, null);
        }
        if (slot == null) {
            int empty = player.getInventory().firstEmpty();
            if (empty < 0 || empty >= 36) return;
            slot = empty;
        }
        candySlots.put(id, slot);
        player.getInventory().setItem(slot, candyItem(player, null, score(player).retained()));
        try {
            EventItems.verifyCandyHead(player.getInventory().getItem(slot), CANDY_BASKET_TEXTURE);
        } catch (RuntimeException exception) {
            game.getPlugin().getLogger().log(Level.SEVERE, "Candy Hunt basket profile was lost during inventory refresh", exception);
            throw exception;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCandyInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!EventItems.matches(event.getItem(), "candy_hunt", activeSession, player.getUniqueId())) return;
        event.setCancelled(true);
        if (!EventItems.matchesCandy(event.getItem(), "basket", activeSession, player.getUniqueId())) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isActive(EventType.CANDY_HUNT) || !isEligible(player)) return;
        Inventory menu = Bukkit.createInventory(new CandyMenuHolder(player.getUniqueId(), activeSession), 9,
                candyText(player, "buildffa_event_candy_menu_title", candyValues(player, null, score(player).retained())));
        renderCandyMenu(player, menu);
        player.openInventory(menu);
    }

    private void renderCandyMenu(Player player, Inventory menu) {
        for (CandyType type : CandyType.values())
            menu.setItem(2 + type.ordinal() * 2, candyItem(player, type, score(player).candyByType.getOrDefault(type, 0)));
    }

    private void refreshCandy(Player player) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Candy inventories must be refreshed on the server thread");
        syncCandyItem(player);
        Inventory menu = player.getOpenInventory().getTopInventory();
        if (menu.getHolder() instanceof CandyMenuHolder) {
            CandyMenuHolder holder = (CandyMenuHolder) menu.getHolder();
            if (holder.owner.equals(player.getUniqueId()) && holder.session.equals(activeSession)) renderCandyMenu(player, menu);
        }
        updateBossBar(player, System.currentTimeMillis());
        refreshManagerMenus();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCandyMenuClick(InventoryClickEvent event) {
        Inventory menu = event.getView().getTopInventory();
        if (!(menu.getHolder() instanceof CandyMenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        CandyMenuHolder holder = (CandyMenuHolder) menu.getHolder();
        if (!player.getUniqueId().equals(holder.owner) || !holder.session.equals(activeSession)
                || !isActive(EventType.CANDY_HUNT) || !isEligible(player)) return;
        if (event.isShiftClick() || !(event.getClick() == org.bukkit.event.inventory.ClickType.LEFT
                || event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT)) return;
        CandyType type = null;
        for (CandyType candidate : CandyType.values())
            if (EventItems.matchesCandy(event.getCurrentItem(), candidate.id, activeSession, holder.owner)) type = candidate;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= menu.getSize() || type == null) return;
        long now = System.currentTimeMillis();
        if (now - candyInteractions.getOrDefault(holder.owner, 0L) < 350L) return;
        EventScore score = score(player);
        int available = Math.max(0, score.candyByType.getOrDefault(type, 0));
        if (!score.consumeCandy(type)) {
            player.sendMessage(candyText(player, "buildffa_event_candy_none_available", candyValues(player, type, available)));
            return;
        }
        candyInteractions.put(holder.owner, now);
        score.retainedReachedAt.putIfAbsent(score.retained(), now);
        score.reachedAt = score.retainedReachedAt.get(score.retained());
        for (PotionEffect effect : type.effects) applyOwnedEffect(player, effect);
        refreshCandy(player);
        player.sendMessage(candyText(player, "buildffa_event_candy_consumed", candyValues(player, type, available - 1)));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEventItemDrop(PlayerDropItemEvent event) {
        if (EventItems.isTemporary(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRouletteBow(EntityShootBowEvent event) {
        if (!EventItems.isTemporary(event.getBow())) return;
        if (!(event.getEntity() instanceof Player)) {
            event.setCancelled(true);
            return;
        }
        Player player = (Player) event.getEntity();
        UUID id = player.getUniqueId();
        int temporarySlot = -1;
        int firstArrowSlot = -1;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack != null && stack.getType() == Material.ARROW && firstArrowSlot < 0) firstArrowSlot = slot;
            if (EventItems.matches(stack, "arrows", activeSession, id) && stack.getAmount() > 0) temporarySlot = slot;
        }
        if (!isActive(EventType.WEAPON_ROULETTE) || !isEligible(player)
                || !EventItems.matches(event.getBow(), "roulette", activeSession, id) || temporarySlot < 0) {
            event.setCancelled(true);
            return;
        }
        if (firstArrowSlot >= 0 && firstArrowSlot < temporarySlot) {
            ItemStack ordinary = player.getInventory().getItem(firstArrowSlot);
            player.getInventory().setItem(firstArrowSlot, player.getInventory().getItem(temporarySlot));
            player.getInventory().setItem(temporarySlot, ordinary);
        }
        try {
            EventItems.preventArrowPickup(event.getProjectile());
            rouletteProjectiles.put(event.getProjectile().getUniqueId(), event.getProjectile());
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            game.getPlugin().getLogger().log(Level.WARNING, "Could not protect roulette arrow", exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEventItemPlace(BlockPlaceEvent event) {
        if (EventItems.isTemporary(event.getItemInHand())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEventHopperPickup(InventoryPickupItemEvent event) {
        if (EventItems.isTemporary(event.getItem().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEventGameMode(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() != GameMode.SURVIVAL) {
            clearEventPlayer(event.getPlayer(), false);
            game.invalidateInventoryRequest(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void protectEventInventory(InventoryClickEvent event) {
        boolean protectedItem = EventItems.isTemporary(event.getCurrentItem()) || EventItems.isTemporary(event.getCursor());
        if (event.getWhoClicked() instanceof Player && event.getHotbarButton() >= 0)
            protectedItem |= EventItems.isTemporary(((Player) event.getWhoClicked()).getInventory().getItem(event.getHotbarButton()));
        if (protectedItem) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void protectEventCraft(org.bukkit.event.inventory.CraftItemEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix())
            if (EventItems.isTemporary(ingredient)) {
                event.setCancelled(true);
                return;
            }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void protectEventSale(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().split("\\s+", 2)[0].toLowerCase(java.util.Locale.ROOT);
        command = command.substring(command.lastIndexOf(':') + 1).replace("/", "");
        if (!(command.equals("sell") || command.equals("sellall") || command.equals("trade")
                || command.equals("auction") || command.equals("ah"))) return;
        for (ItemStack stack : event.getPlayer().getInventory().getContents())
            if (EventItems.isTemporary(stack)) {
                event.setCancelled(true);
                return;
            }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void protectEventDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CandyMenuHolder
                || EventItems.isTemporary(event.getOldCursor())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEventInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) removeStaleEventItems((Player) event.getPlayer());
    }

    private void removeStaleEventItems(Player player) {
        UUID id = player.getUniqueId();
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (EventItems.isTemporary(stack) && !(EventItems.matches(stack, "roulette", activeSession, id)
                    || EventItems.matches(stack, "arrows", activeSession, id) || EventItems.matchesCandy(stack, "basket", activeSession, id)))
                player.getInventory().setItem(slot, null);
        }
        for (int slot = 0; slot < player.getEnderChest().getSize(); slot++)
            if (EventItems.isTemporary(player.getEnderChest().getItem(slot))) player.getEnderChest().setItem(slot, null);
    }

    @EventHandler
    public void onEventChunkLoad(ChunkLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities())
            if (entity instanceof Item && EventItems.isTemporary(((Item) entity).getItemStack())) entity.remove();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEventWorldSave(WorldSaveEvent event) {
        for (UUID id : new ArrayList<>(rouletteProjectiles.keySet())) {
            org.bukkit.entity.Entity projectile = rouletteProjectiles.get(id);
            if (projectile.getWorld().equals(event.getWorld())) {
                projectile.remove();
                rouletteProjectiles.remove(id);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEventChunkUnload(ChunkUnloadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item && EventItems.isTemporary(((Item) entity).getItemStack())) {
                entity.remove();
            }
            if (rouletteProjectiles.remove(entity.getUniqueId()) != null) entity.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVanillaEventDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(EventItems::isTemporary);
        beforeInventoryReset(event.getEntity());
        removeBossBar(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEventWorldUnload(WorldUnloadEvent event) {
        if (game.getWorldConfig() != null && event.getWorld().equals(game.getWorldConfig().world()))
            beginArenaTransfer(event.getWorld());
        else cleanupArenaWorld(event.getWorld());
    }

    @EventHandler
    public void onEventWorldChanged(PlayerChangedWorldEvent event) {
        removeBossBar(event.getPlayer().getUniqueId());
        clearEventPlayer(event.getPlayer(), false);
    }

    private List<UUID> rankedEventPlayers(EventType type) {
        List<UUID> players = new ArrayList<>(eventScores.keySet());
        Comparator<UUID> comparator;
        if (type == EventType.KING_OF_THE_HILL) comparator = Comparator.comparingInt((UUID id) -> eventScores.get(id).capturePoints).reversed();
        else if (type == EventType.CANDY_HUNT) comparator = Comparator.comparingInt((UUID id) -> eventScores.get(id).retained()).reversed()
                .thenComparing(Comparator.comparingInt((UUID id) -> eventScores.get(id).candyCollected).reversed())
                .thenComparingLong(id -> eventScores.get(id).reachedAt);
        else if (type == EventType.JUGGERNAUT) comparator = Comparator.comparingInt((UUID id) -> eventScores.get(id).juggernautKills).reversed()
                .thenComparing(Comparator.comparingLong((UUID id) -> eventScores.get(id).timeAsJuggernaut).reversed());
        else comparator = Comparator.comparingInt((UUID id) -> eventScores.get(id).rouletteKills).reversed();
        players.sort(comparator.thenComparing(UUID::toString));
        return players;
    }

    private void payHillRewards(Player player) {
        if (player == null || !player.isOnline()) return;
        Gamer gamer = Gamer.getGamer(player);
        if (gamer == null) return;
        List<HillReward> rewards = pendingHillRewards.remove(player.getUniqueId());
        if (rewards == null) return;
        for (HillReward reward : rewards) {
            if (reward.coins > 0) {
                gamer.addCoins(Uncommon.BUILDFFA, reward.coins);
                game.getQuestManager().handleCoinsEarned(player, reward.coins);
            }
            if (reward.rating > 0) game.getDivisionListener().awardEventRating(gamer, reward.rating);
            String text = message(gamer, "buildffa_event_king_of_the_hill_results",
                    "&6%event_name%: #%position% | %points% points | %kills% kills | +%coins% coins, +%rating% rating");
            gamer.sendMessage(color(text.replace("%event_name%", eventName(gamer, EventType.KING_OF_THE_HILL))
                    .replace("%player%", formattedPlayerName(player)).replace("%position%", String.valueOf(reward.position))
                    .replace("%points%", String.valueOf(reward.points)).replace("%kills%", String.valueOf(reward.kills))
                    .replace("%coins%", String.valueOf(reward.coins)).replace("%rating%", String.valueOf(reward.rating))));
        }
    }

    private static final class HillReward {
        final int position, points, kills, coins, rating;
        HillReward(int position, int points, int kills, int coins, int rating) {
            this.position = position;
            this.points = points;
            this.kills = kills;
            this.coins = coins;
            this.rating = rating;
        }
    }
    private void rewardEventResults(EventType type) {
        if (type != EventType.CANDY_HUNT && type != EventType.JUGGERNAUT && type != EventType.WEAPON_ROULETTE && type != EventType.KING_OF_THE_HILL) return;
        List<UUID> ranked = rankedEventPlayers(type);
        if (type == EventType.KING_OF_THE_HILL) {
            if (activeSession == null || activeSession.equals(lastRewardedHillSession)) return;
            lastRewardedHillSession = activeSession;
            for (int index = 0; index < ranked.size(); index++) {
                UUID id = ranked.get(index);
                int points = eventScores.get(id).capturePoints;
                int coins = points == 0 ? 0 : index == 0 ? 300 : index == 1 ? 200 : index == 2 ? 150 : 25;
                int rating = points == 0 ? 0 : index == 0 ? 30 : index == 1 ? 20 : index == 2 ? 15 : 2;
                pendingHillRewards.computeIfAbsent(id, ignored -> new ArrayList<>()).add(new HillReward(index + 1,
                        points, activeSessionKills.getOrDefault(id, 0), coins, rating));
                payHillRewards(Bukkit.getPlayer(id));
            }
            return;
        }
        for (int index = 0; index < ranked.size(); index++) {
            Player player = Bukkit.getPlayer(ranked.get(index));
            if (player == null || !player.isOnline()) continue;
            EventScore score = eventScores.get(ranked.get(index));
            int coins = 0;
            int rating = 0;
            if (type == EventType.CANDY_HUNT && score.retained() > 0) {
                if (index == 0) { coins = 300; rating = 30; }
                else if (index == 1) { coins = 200; rating = 20; }
                else if (index == 2) { coins = 150; rating = 15; }
                else if (index < 10) { coins = 75; rating = 7; }
                else if (score.retained() >= 5) { coins = 25; rating = 2; }
            }

            Gamer gamer = Gamer.getGamer(player);
            if (gamer == null) continue;
            if (coins > 0) {
                gamer.addCoins(Uncommon.BUILDFFA, coins);
                game.getQuestManager().handleCoinsEarned(player, coins);
            }
            if (rating > 0) {
                game.getDivisionListener().awardEventRating(gamer, rating);
            }
            String key = type == EventType.CANDY_HUNT ? "buildffa_event_candy_results"
                    : type == EventType.JUGGERNAUT ? "buildffa_event_juggernaut_results" : "buildffa_event_weapon_roulette_results";
            sendNewEventMessage(player, key, "%player%", formattedPlayerName(player), "%position%", String.valueOf(index + 1),
                    "%coins%", String.valueOf(coins), "%rating%", String.valueOf(rating), "%kills%",
                    String.valueOf(type == EventType.JUGGERNAUT ? score.juggernautKills : type == EventType.CANDY_HUNT
                            ? activeSessionKills.getOrDefault(player.getUniqueId(), 0) : score.rouletteKills),
                    "%candy_collected%", String.valueOf(score.candyCollected), "%candy_consumed%", String.valueOf(score.candyConsumed),
                    "%candy_retained%", String.valueOf(score.retained()));
        }
    }

    private void recordParticipant(Player player) {
        if (player == null) return;
        Gamer gamer = Gamer.getGamer(player);
        String displayName = gamer == null ? player.getName() : gamer.getDisplayName();
        activeSessionPlayers.put(player.getUniqueId(), new PlayerIdentity(player.getUniqueId(), player.getName(), displayName));
    }

    private void recordSession(UUID session, EventType type, long stoppedAt, boolean completed) {
        if (session == null || session.equals(lastRecordedSession)) return;
        lastRecordedSession = session;
        Map<UUID, Integer> kills = new HashMap<>(activeSessionKills);
        Map<UUID, PlayerIdentity> players = new HashMap<>(activeSessionPlayers);
        int totalKills = 0;
        for (int count : kills.values()) totalKills += count;
        long endedAt = completed ? endTimestamp : stoppedAt;
        history.addFirst(new EventSessionSnapshot(session, type.id, startTimestamp, endedAt,
                Math.max(0L, endedAt - startTimestamp), completed, totalKills, kills, players, copyEventScores()));
        while (history.size() > 5) history.removeLast();
        activeSessionKills.clear();
        activeSessionPlayers.clear();
    }

    private List<LeaderboardEntry> leaderboard(EventSessionSnapshot snapshot) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, PlayerIdentity> participant : snapshot.players.entrySet()) {
            int kills = snapshot.kills.getOrDefault(participant.getKey(), 0);
            LeaderboardEntry entry = new LeaderboardEntry(participant.getValue());
            entry.kills = kills;
            entry.eventsPlayed = eventsPlayed(participant.getKey());
            entry.score = snapshot.scores.getOrDefault(participant.getKey(), new EventScore());
            entries.add(entry);
        }
        Comparator<LeaderboardEntry> ranking = Comparator.comparingInt((LeaderboardEntry entry) -> entry.kills).reversed();
        if (snapshot.eventId.equals(EventType.CANDY_HUNT.id)) ranking = Comparator.comparingInt((LeaderboardEntry entry) -> entry.score.retained()).reversed()
                .thenComparing(Comparator.comparingInt((LeaderboardEntry entry) -> entry.score.candyCollected).reversed())
                .thenComparingLong(entry -> entry.score.reachedAt);
        if (snapshot.eventId.equals(EventType.JUGGERNAUT.id)) ranking = Comparator.comparingInt((LeaderboardEntry entry) -> entry.score.juggernautKills).reversed()
                .thenComparing(Comparator.comparingLong((LeaderboardEntry entry) -> entry.score.timeAsJuggernaut).reversed());
        if (snapshot.eventId.equals(EventType.WEAPON_ROULETTE.id)) ranking = Comparator.comparingInt((LeaderboardEntry entry) -> entry.score.rouletteKills).reversed();
        if (snapshot.eventId.equals(EventType.KING_OF_THE_HILL.id)) ranking = Comparator.comparingInt((LeaderboardEntry entry) -> entry.score.capturePoints).reversed();
        boolean newEvent = snapshot.eventId.equals(EventType.KING_OF_THE_HILL.id) || snapshot.eventId.equals(EventType.CANDY_HUNT.id) || snapshot.eventId.equals(EventType.JUGGERNAUT.id)
                || snapshot.eventId.equals(EventType.WEAPON_ROULETTE.id);
        if (!newEvent) ranking = ranking.thenComparing(entry -> ChatColor.stripColor(safeText(entry.identity.displayName)), String.CASE_INSENSITIVE_ORDER);
        entries.sort(ranking.thenComparing(entry -> entry.identity.playerId.toString()));
        if (entries.size() > 10) return new ArrayList<>(entries.subList(0, 10));
        return entries;
    }

    private Map<UUID, EventScore> copyEventScores() {
        Map<UUID, EventScore> copy = new HashMap<>();
        eventScores.forEach((id, score) -> copy.put(id, new EventScore(score)));
        return copy;
    }

    private int eventsPlayed(UUID playerId) {
        int count = 0;
        for (EventSessionSnapshot snapshot : history) if (snapshot.players.containsKey(playerId)) count++;
        return count;
    }

    private String leaderboardDisplayName(PlayerIdentity identity) {
        Player player = Bukkit.getPlayer(identity.playerId);
        String current = formattedPlayerName(player);
        if (!current.isEmpty()) return current;
        String storedDisplayName = safeText(identity.displayName);
        return storedDisplayName.isEmpty() ? safeText(identity.playerName) : storedDisplayName;
    }

    private EventSessionSnapshot findSnapshot(UUID sessionId) {
        for (EventSessionSnapshot snapshot : history) if (snapshot.sessionId.equals(sessionId)) return snapshot;
        return null;
    }

    private String ordinal(int position) {
        if (position == 1) return "1st";
        if (position == 2) return "2nd";
        if (position == 3) return "3rd";
        return position + "th";
    }

    private String formatEndedAgo(long endedAt) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - endedAt) / 1000L);
        if (seconds < 60L) return seconds + "s ago";
        long minutes = seconds / 60L;
        if (minutes < 60L) return minutes + "m ago";
        long hours = minutes / 60L;
        if (hours < 24L) return hours + "h ago";
        return (hours / 24L) + "d ago";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void refreshManagerMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory == null) continue;
            InventoryHolder holder = inventory.getHolder();
            if (holder instanceof EventManagerHolder
                    && ((EventManagerHolder) holder).viewer.equals(player.getUniqueId())) {
                renderManager(player, inventory);
            } else if (holder instanceof PlayerEventHolder
                    && ((PlayerEventHolder) holder).viewer.equals(player.getUniqueId())) {
                renderPlayerMenu(player, inventory);
            } else if (holder instanceof EventStatsHolder
                    && ((EventStatsHolder) holder).viewer.equals(player.getUniqueId())) {
                renderStatsHistory(player, inventory);
            } else if (holder instanceof StandardEventLeaderboardGui) {
                if (hasEventStatsPermission(player)) ((StandardEventLeaderboardGui) holder).refreshLeaderboard();
                else player.closeInventory();
            }
        }
    }

    private List<String> wrapLore(List<String> source, int visibleWordsPerLine) {
        List<String> wrapped = new ArrayList<>();
        for (String paragraph : source) {
            if (paragraph == null || paragraph.trim().isEmpty()) {
                wrapped.add("");
                continue;
            }
            String[] words = paragraph.trim().split("\\s+");
            StringBuilder line = new StringBuilder();
            int visibleWords = 0;
            for (String word : words) {
                boolean visible = !ChatColor.stripColor(word).isEmpty();
                if (visible && visibleWords >= visibleWordsPerLine) {
                    String completed = line.toString();
                    wrapped.add(completed);
                    line = new StringBuilder(ChatColor.getLastColors(completed));
                    visibleWords = 0;
                }
                if (visible) {
                    if (!ChatColor.stripColor(line.toString()).isEmpty()) line.append(' ');
                    line.append(word);
                    visibleWords++;
                } else {
                    line.append(word);
                }
            }
            if (!line.isEmpty()) wrapped.add(line.toString());
        }
        return wrapped;
    }

    private List<String> replace(List<String> source, String placeholder, String value) {
        List<String> replaced = new ArrayList<>();
        for (String line : source) replaced.add(line.replace(placeholder, value == null ? "" : value));
        return replaced;
    }

    private String formatHistoryDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainder = seconds % 60L;
        return hours > 0L ? hours + "h " + minutes + "m " + remainder + "s" : minutes + "m " + remainder + "s";
    }

    private String formatLoreDuration(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        return (seconds / 60L) + "m " + (seconds % 60L) + "s";
    }

    public List<String> getEventIds() {
        List<String> ids = new ArrayList<>();
        for (EventType type : EventType.values()) ids.add(type.id);
        return ids;
    }

    public List<String> getEnabledEventIds() {
        List<String> ids = new ArrayList<>();
        for (EventType type : EventType.values()) if (isAdministrativelyEnabled(type)) ids.add(type.id);
        return ids;
    }

    public List<String> getDisabledEventIds() {
        List<String> ids = new ArrayList<>();
        for (EventType type : EventType.values()) if (!isAdministrativelyEnabled(type)) ids.add(type.id);
        return ids;
    }

    public List<String> getActiveEventIds() {
        return activeEvent == null ? Collections.emptyList() : Collections.singletonList(activeEvent.id);
    }

    public EventType resolveEvent(String id) {
        if (id == null) return null;
        String normalized = id.trim().toLowerCase().replace(' ', '_').replace('-', '_');
        for (EventType type : EventType.values()) if (type.id.equals(normalized)) return type;
        return null;
    }

    public void setEventEnabled(Gamer gamer, String id, boolean enabled) {
        EventType type = resolveEvent(id);
        if (type == null) {
            gamer.sendMessage(message(gamer, "buildffa_event_unknown", "§cUnknown event: §f%event%§c.")
                    .replace("%event%", id == null ? "" : id));
            return;
        }
        if (isAdministrativelyEnabled(type) == enabled) {
            String key = enabled ? "buildffa_event_already_enabled" : "buildffa_event_already_disabled";
            String fallback = enabled ? "§c%event_name% is already enabled." : "§c%event_name% is already disabled.";
            gamer.sendMessage(message(gamer, key, fallback).replace("%event_name%", eventName(gamer, type)));
            return;
        }
        if (!game.getFfaConfig().saveEventEnabled(type.id, enabled)) {
            gamer.sendMessage(message(gamer, "buildffa_event_availability_save_failed",
                    "§cCould not save the event availability setting."));
            return;
        }
        administrativeEnabled.put(type, enabled);
        if (!enabled && selectedAutomaticEvent == type && countdownActive) broadcastAutomaticCancellation(type);
        removeDisabledScheduledEvents();
        refillUpcomingEvents(System.currentTimeMillis() + AUTOMATIC_RETRY_MILLIS + AUTOMATIC_COUNTDOWN_MILLIS);
        ensureAutomaticSchedulerTask();
        refreshManagerMenus();
        String key = enabled ? "buildffa_event_enabled" : "buildffa_event_disabled";
        String fallback = enabled ? "§aEnabled event §b%event_name%§a." : "§cDisabled event §b%event_name%§c.";
        gamer.sendMessage(message(gamer, key, fallback).replace("%event_name%", eventName(gamer, type)));
    }

    public void stopEvent(Gamer gamer, String id) {
        EventType requested = resolveEvent(id);
        if (requested == null) {
            gamer.sendMessage(message(gamer, "buildffa_event_unknown", "§cUnknown event: §f%event%§c.")
                    .replace("%event%", id == null ? "" : id));
            return;
        }
        if (activeEvent == null) {
            gamer.sendMessage(message(gamer, "buildffa_event_stop_none_active", "§cThere is no active BuildFFA event."));
            return;
        }
        if (activeEvent != requested) {
            gamer.sendMessage(message(gamer, "buildffa_event_stop_not_active", "§cThat event is not active. Currently active: §6%event_name%§c.")
                    .replace("%event_name%", eventName(gamer, activeEvent)));
            return;
        }
        String stoppedName = eventName(gamer, requested);
        stop(false);
        gamer.sendMessage(message(gamer, "buildffa_event_stop_success", "§fStopped the §6%event_name% §fevent.")
                .replace("%event_name%", stoppedName));
    }

    public void sendEventList(Gamer gamer) {
        gamer.sendMessage(message(gamer, "buildffa_event_list_header", "§6§lBuildFFA Events"));
        gamer.sendMessage(message(gamer, "buildffa_event_automatic_status_enabled", "§aAutomatic events: Enabled"));
        long now = System.currentTimeMillis();
        if (activeEvent != null) {
            String remaining = formatEventDuration(Math.max(0L, endTimestamp - now));
            gamer.sendMessage(message(gamer, "buildffa_event_automatic_status_active",
                    "§7Active event: §e%event_name% §7(%event_time_remaining%)")
                    .replace("%event_name%", eventName(gamer, activeEvent))
                    .replace("%event_time_remaining%", remaining)
                    .replace("%event_time%", remaining));
            if (!activeEventAutomatic) gamer.sendMessage(message(gamer, "buildffa_event_automatic_status_paused",
                    "§7Automatic schedule: §ePaused while an event is active")
                    .replace("%event_name%", eventName(gamer, activeEvent))
                    .replace("%event_time_remaining%", remaining));
        } else if (selectedAutomaticEvent != null) {
            gamer.sendMessage(message(gamer, "buildffa_event_automatic_status_upcoming", "§7Upcoming event: §e%event_name%")
                    .replace("%event_name%", eventName(gamer, selectedAutomaticEvent)));
            ScheduledEvent nextScheduled = upcomingEvents.peekFirst();
            if (nextScheduled != null) {
                long remainingMillis = Math.max(0L, effectiveScheduledStartTime(nextScheduled, now) - now);
                gamer.sendMessage(message(gamer, "buildffa_event_automatic_status_starts", "%event_starts_in%")
                        .replace("%event_starts_in%", formatEventDuration(remainingMillis)));
            } else gamer.sendMessage(message(gamer, "buildffa_event_automatic_status_unavailable", "§7Unavailable"));
        } else {
            gamer.sendMessage(message(gamer, "buildffa_event_automatic_status_unavailable", "§7Unavailable"));
        }
        for (EventType type : EventType.values()) {
            EventState eventState = state(type);
            String key;
            String fallback;
            if (eventState == EventState.ACTIVE) {
                key = "buildffa_event_list_active";
                fallback = "§8- §6%event_name% §7(%event_id%) §aACTIVE §7- §f%event_time% remaining";
            } else if (eventState == EventState.DISABLED) {
                key = "buildffa_event_list_disabled";
                fallback = "§8- §6%event_name% §7(%event_id%) §cDISABLED";
            } else {
                key = "buildffa_event_list_enabled";
                fallback = "§8- §6%event_name% §7(%event_id%) §aENABLED";
            }
            gamer.sendMessage(message(gamer, key, fallback)
                    .replace("%event_name%", eventName(gamer, type))
                    .replace("%event_id%", type.id)
                    .replace("%event_time%", formatDuration(remainingMillis())));
        }
    }

    public void sendUsage(Gamer gamer) {
        gamer.sendMessage(message(gamer, "buildffa_event_manager_usage", "§cUsage: /eventmanager [list|enable <event>|disable <event>|stop <event>]"));
    }

    public void sendStopUsage(Gamer gamer) {
        gamer.sendMessage(message(gamer, "buildffa_event_stop_usage", "§cUsage: /eventmanager stop <event>"));
    }

    public void sendAvailabilityUsage(Gamer gamer, boolean enabled) {
        String key = enabled ? "buildffa_event_enable_usage" : "buildffa_event_disable_usage";
        String fallback = enabled ? "§cUsage: /eventmanager enable <eventname>"
                : "§cUsage: /eventmanager disable <eventname>";
        gamer.sendMessage(message(gamer, key, fallback));
    }

    private void updateBossBars(long now) {
        if (activeEvent == null) {
            removeBossBars();
            return;
        }
        long remaining = Math.max(0L, endTimestamp - now);
        if (remaining <= 0L) {
            removeBossBars();
            return;
        }
        Set<UUID> viewers = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isBuildFFAPlayer(player)) continue;
            viewers.add(player.getUniqueId());
            Long retryAfter = bossBarRetryAfter.get(player.getUniqueId());
            if (retryAfter != null && now < retryAfter) continue;
            reconcileBossBar(player, now);
        }
        for (UUID playerId : new HashSet<>(bossBars.keySet())) {
            if (!viewers.contains(playerId)) removeBossBar(playerId);
        }
    }

    private void updateBossBar(Player player, long now) {
        if (player.isDead() || pendingBossBarRemovals.contains(player.getUniqueId())) return;
        EventType event = activeEvent;
        if (event == null) {
            removeBossBar(player.getUniqueId());
            return;
        }
        LegacyEventBossBar bar = bossBars.get(player.getUniqueId());
        if (bar == null && !isBuildFFAPlayer(player)) return;
        long remaining = Math.max(0L, endTimestamp - now);
        if (remaining <= 0L) {
            removeBossBar(player.getUniqueId());
            return;
        }
        float progress = (float) Math.clamp((double) remaining / activeDurationMillis, 0.0D, 1.0D);
        Gamer gamer = Gamer.getGamer(player);
        EventScore currentScore = eventScores.getOrDefault(player.getUniqueId(), new EventScore());
        String currentJuggernaut = juggernaut == null ? message(gamer, "buildffa_event_juggernaut_waiting", "")
                : formattedPlayerName(Bukkit.getPlayer(juggernaut));
        String title = gamer.getLanguage().getMessage("buildffa_event_bossbar",
                "%event_name%", eventName(gamer, event), "%player%", formattedPlayerName(player),
                "%event_time%", formatBossBarDuration(remaining), "%event_time_remaining%", formatBossBarDuration(remaining),
                "%event_duration_lasts%", formatEventDuration(activeDurationMillis), "%juggernaut%", currentJuggernaut,
                "%candy_collected%", String.valueOf(currentScore.candyCollected), "%candy_consumed%", String.valueOf(currentScore.candyConsumed),
                "%candy_retained%", String.valueOf(currentScore.retained()), "%kills%", String.valueOf(currentScore.rouletteKills));
        if (missingTranslation(title, "buildffa_event_bossbar"))
            title = "§e§l" + eventName(gamer, event) + " §7- §f" + formatBossBarDuration(remaining);
        if (event == EventType.CANDY_HUNT || event == EventType.JUGGERNAUT || event == EventType.WEAPON_ROULETTE) {
            EventScore score = eventScores.getOrDefault(player.getUniqueId(), new EventScore());
            String key = "buildffa_event_" + event.id + "_bossbar";
            String juggernautName = juggernaut == null ? message(gamer, "buildffa_event_juggernaut_waiting", "")
                    : formattedPlayerName(Bukkit.getPlayer(juggernaut));
            String localized = gamer.getLanguage().getMessage(key,
                    "%player%", formattedPlayerName(player),
                    "%event_name%", eventName(gamer, event), "%event_time_remaining%", formatBossBarDuration(remaining),
                    "%event_time%", formatBossBarDuration(remaining), "%candy_collected%", String.valueOf(score.candyCollected),
                    "%candy_consumed%", String.valueOf(score.candyConsumed), "%candy_retained%", String.valueOf(score.retained()),
                    "%juggernaut%", juggernautName,
                    "%kills%", String.valueOf(score.rouletteKills));
            if (!missingTranslation(localized, key)) title = color(localized);
        }
        if (event == EventType.KING_OF_THE_HILL) {
            String status = message(gamer, "buildffa_event_king_of_the_hill_status_" + hillStatus,
                    hillStatus.equals("capturing") ? "Capturing: %holder%" : hillStatus.equals("contested") ? "Contested" : hillStatus.equals("suspended") ? "Capture suspended" : "Empty")
                    .replace("%holder%", formattedPlayerName(hillHolder == null ? null : Bukkit.getPlayer(hillHolder)))
                    .replace("%reason%", "");
            title = color(message(gamer, "buildffa_event_king_of_the_hill_bossbar", "&6%event_name% &7| %capture_status% &7| &e%points% points &7| &f%event_time_remaining%")
                    .replace("%event_name%", eventName(gamer, event)).replace("%capture_status%", status)
                    .replace("%points%", String.valueOf(currentScore.capturePoints)).replace("%event_time_remaining%", formatBossBarDuration(remaining)));
        }
        if (bar == null) {
            bar = new LegacyEventBossBar(player);
            bossBars.put(player.getUniqueId(), bar);
        }
        if (bar.update(title, progress)) {
            bossBarRetryAfter.remove(player.getUniqueId());
            bossBarFailureSignatures.remove(player.getUniqueId());
            bossBarFailureLogAfter.remove(player.getUniqueId());
            return;
        }
        String operation = bar.getLastOperation();
        Throwable failure = bar.getLastFailure();
        bossBarRetryAfter.put(player.getUniqueId(), now + BOSS_BAR_RETRY_MILLIS);
        logBossBarFailure(player, operation, failure, now);
    }

    private void reconcileBossBar(Player player, long now) {
        try {
            updateBossBar(player, now);
        } catch (RuntimeException exception) {
            bossBarRetryAfter.put(player.getUniqueId(), now + BOSS_BAR_RETRY_MILLIS);
            logBossBarFailure(player, "viewer reconciliation", exception, now);
        }
    }

    private boolean isBuildFFAPlayer(Player player) {
        if (player == null || !player.isOnline()) return false;
        Gamer gamer = Gamer.getGamer(player);
        if (gamer == null || gamer.getPlayerId() <= 0) return false;
        try {
            return BuildFFAGamer.get(gamer) != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void removeBossBar(UUID playerId) {
        LegacyEventBossBar bar = bossBars.get(playerId);
        if (bar == null) {
            pendingBossBarRemovals.remove(playerId);
            return;
        }
        pendingBossBarRemovals.add(playerId);
        try {
            if (bar.remove()) {
                bossBars.remove(playerId);
                pendingBossBarRemovals.remove(playerId);
                bossBarRetryAfter.remove(playerId);
                bossBarFailureSignatures.remove(playerId);
                bossBarFailureLogAfter.remove(playerId);
            } else {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) logBossBarFailure(player, bar.getLastOperation(), bar.getLastFailure(), System.currentTimeMillis());
            }
        } catch (RuntimeException exception) {
            game.getPlugin().getLogger().log(Level.WARNING, "Could not remove BuildFFA event boss bar", exception);
        }
    }

    private void retryBossBarRemovals() {
        for (UUID playerId : new HashSet<>(pendingBossBarRemovals)) removeBossBar(playerId);
    }

    private void removeBossBars() {
        for (UUID playerId : new HashSet<>(bossBars.keySet())) removeBossBar(playerId);
        if (!pendingBossBarRemovals.isEmpty() && bossBarTask == null && !shuttingDown) {
            try {
                bossBarTask = Bukkit.getScheduler().runTaskTimer(game.getPlugin(), () -> {
                    retryBossBarRemovals();
                    if (pendingBossBarRemovals.isEmpty() && activeEvent == null && bossBarTask != null) {
                        bossBarTask.cancel();
                        bossBarTask = null;
                    }
                }, BOSS_BAR_UPDATE_TICKS, BOSS_BAR_UPDATE_TICKS);
            } catch (RuntimeException exception) {
                game.getPlugin().getLogger().log(Level.WARNING, "Could not schedule BuildFFA boss bar cleanup", exception);
            }
        }
    }

    private void logBossBarFailure(Player player, String operation, Throwable failure, long now) {
        Throwable root = failure;
        while (root != null && root.getCause() != null && root.getCause() != root) root = root.getCause();
        String operationName = operation == null ? "unknown operation" : operation;
        String exceptionName = failure == null ? "no exception" : failure.getClass().getName();
        String exceptionMessage = failure == null ? "no message" : String.valueOf(failure.getMessage());
        String rootDetail = root == null ? "none" : root.getClass().getName() + ": " + String.valueOf(root.getMessage());
        String signature = operationName + '|' + exceptionName + '|' + exceptionMessage + '|' + rootDetail;
        UUID playerId = player.getUniqueId();
        Long logAfter = bossBarFailureLogAfter.get(playerId);
        if (signature.equals(bossBarFailureSignatures.get(playerId)) && logAfter != null && now < logAfter) return;
        bossBarFailureSignatures.put(playerId, signature);
        bossBarFailureLogAfter.put(playerId, now + BOSS_BAR_FAILURE_LOG_MILLIS);
        String detail = "BuildFFA event boss bar failed for player " + playerId + " during " + operationName
                + " with " + exceptionName + ": " + exceptionMessage + "; root cause " + rootDetail;
        if (failure == null) game.getPlugin().getLogger().warning(detail);
        else game.getPlugin().getLogger().log(Level.WARNING, detail, failure);
    }

    private String eventName(Gamer gamer, EventType type) {
        return type == null ? "" : message(gamer, type.nameKey, type.displayName);
    }

    private long remainingMillis() {
        return activeEvent == null ? 0L : Math.max(0L, endTimestamp - System.currentTimeMillis());
    }

    private void broadcast(String key, String fallback, String placeholder, String value) {
        for (Gamer gamer : Gamer.getGamers()) {
            if (!gamer.isOnline()) continue;
            gamer.sendMessage(message(gamer, key, fallback).replace(placeholder, value));
        }
    }

    private void broadcastEvent(String key, String fallback, EventType type) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isBuildFFAPlayer(player)) continue;
            Gamer gamer = Gamer.getGamer(player);
            String localizedEventName = message(gamer, type.nameKey, type.displayName);
            gamer.sendMessage(message(gamer, key, fallback)
                    .replace("%event_name%", localizedEventName)
                    .replace("%event%", localizedEventName));
        }
    }

    private void broadcastAutomaticAnnouncement(EventType type, String time) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isBuildFFAPlayer(player)) continue;
            Gamer gamer = Gamer.getGamer(player);
            gamer.sendMessage(message(gamer, "buildffa_event_automatic_announcement",
                    "§e[EVENT] §b%event_name% §awill start in §e%event_time%§a!")
                    .replace("%event_name%", eventName(gamer, type))
                    .replace("%event_time%", time));
        }
    }

    private void broadcastAutomaticCancellation(EventType type) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isBuildFFAPlayer(player)) continue;
            Gamer gamer = Gamer.getGamer(player);
            gamer.sendMessage(message(gamer, "buildffa_event_automatic_cancelled",
                    "§cThe upcoming §b%event_name% §cevent was cancelled.")
                    .replace("%event_name%", eventName(gamer, type)));
        }
    }

    private void broadcastEventStarted(EventType type) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Gamer recipient = Gamer.getGamer(player);
            if (recipient == null) continue;
            String localizedEventName = message(recipient, type.nameKey, type.displayName);
            List<String> descriptions = displayedEventDescriptions(recipient, type);
            String announcement = recipient.getLanguage().getMessage("buildffa_event_started",
                    "%event_name%", localizedEventName, "%event_duration_lasts%", formatEventDuration(type.durationMillis),
                    "%event_time_remaining%", formatDuration(type.durationMillis),
                    "%event_description_1%", descriptions.isEmpty() ? "" : descriptions.get(0),
                    "%event_description_2%", descriptions.size() < 2 ? "" : descriptions.get(1));
            if (announcement == null || announcement.startsWith("No translation (")) {
                announcement = "§e[EVENT] §b" + localizedEventName + " §ahas started!";
            }
            player.sendMessage(announcement);
            sendEventAnnouncement(player, recipient, type, localizedEventName);
        }
    }

    private void sendEventAnnouncement(Player player, Gamer recipient, EventType type, String localizedEventName) {
        List<String> description = displayedEventDescriptions(recipient, type);
        String firstLine = description.isEmpty() ? "" : description.get(0);
        String secondLine = description.size() < 2 ? "" : description.get(1);
        String duration = formatEventDuration(type.durationMillis);
        sendEventAnnouncementRow(player, recipient, "buildffa_event_announcement_border",
                localizedEventName, duration, firstLine, secondLine);
        sendEventAnnouncementRow(player, recipient, "buildffa_event_announcement_header",
                localizedEventName, duration, firstLine, secondLine);
        if (!firstLine.isEmpty()) {
            sendEventAnnouncementRow(player, recipient, "buildffa_event_announcement_description_1",
                    localizedEventName, duration, firstLine, secondLine);
        }
        if (!secondLine.isEmpty()) {
            sendEventAnnouncementRow(player, recipient, "buildffa_event_announcement_description_2",
                    localizedEventName, duration, firstLine, secondLine);
        }
        sendEventAnnouncementRow(player, recipient, "buildffa_event_announcement_border",
                localizedEventName, duration, firstLine, secondLine);
    }

    private void sendEventAnnouncementRow(Player player, Gamer recipient, String key, String eventName,
        String duration, String firstLine, String secondLine) {
        String row = recipient.getLanguage().getMessage(key, "%event_name%", eventName,
                "%event_duration%", duration, "%event_duration_lasts%", duration,
                "%event_description_1%", firstLine, "%event_description_2%", secondLine);
        if (row == null || row.equals(key) || row.startsWith("No translation (")) return;
        String trimmed = row.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) return;
        player.sendMessage(color(row.replace("%event_name%", eventName)
                .replace("%event_duration%", duration)
                .replace("%event_description_1%", firstLine)
                .replace("%event_description_2%", secondLine)));
    }

    private List<String> balancedDescription(List<String> source) {
        List<String> words = new ArrayList<>();
        for (String line : source) {
            String plain = ChatColor.stripColor(color(line)).trim().replaceAll("\\s+,", ",");
            if (plain.isEmpty()) continue;
            words.addAll(Arrays.asList(plain.split("\\s+")));
        }
        if (words.isEmpty()) return Collections.emptyList();
        String complete = String.join(" ", words);
        if (complete.length() <= 42 || words.size() == 1) return Collections.singletonList(complete);
        int bestSplit = 1;
        int bestDifference = Integer.MAX_VALUE;
        for (int split = 1; split < words.size(); split++) {
            int firstLength = joinedLength(words, 0, split);
            int secondLength = joinedLength(words, split, words.size());
            int difference = Math.abs(firstLength - secondLength);
            if (difference < bestDifference) {
                bestDifference = difference;
                bestSplit = split;
            }
        }
        return Arrays.asList(String.join(" ", words.subList(0, bestSplit)),
                String.join(" ", words.subList(bestSplit, words.size())));
    }

    private int joinedLength(List<String> words, int from, int to) {
        int length = Math.max(0, to - from - 1);
        for (int index = from; index < to; index++) length += words.get(index).length();
        return length;
    }

    private void broadcastEventExpired(EventType type) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Gamer recipient = Gamer.getGamer(player);
            if (recipient == null) continue;
            String localizedEventName = message(recipient, type.nameKey, type.displayName);
            String announcement = recipient.getLanguage().getMessage("buildffa_event_expired",
                    "%event_name%", localizedEventName);
            if (announcement == null || announcement.startsWith("No translation (")) {
                announcement = "§e[EVENT] §b" + localizedEventName + " §ahas ended!";
            }
            player.sendMessage(announcement);
        }
    }

    private String message(Gamer gamer, String key, String fallback) {
        String value = gamer.getLanguage().getMessage(key);
        return missingTranslation(value, key) ? fallback : value;
    }

    private String playerMenuMessage(Gamer gamer, String key, String fallback) {
        String value = gamer.getLanguage().getMessage(key);
        if (missingTranslation(value, key)) return fallback;
        String trimmed = value.trim();
        return trimmed.startsWith("[") && trimmed.endsWith("]") ? fallback : value;
    }

    private List<String> messageList(Gamer gamer, String key, List<String> fallback) {
        List<String> value = gamer.getLanguage().getMessageList(key);
        return value == null || value.isEmpty() || value.get(0).equals(key)
                || missingTranslation(value.get(0), key) ? fallback : value;
    }

    private boolean missingTranslation(String value, String key) {
        return value == null || value.equals(key) || value.contains("No translation (" + key + ")")
                || value.startsWith("No translation (");
    }

    private List<String> eventDescriptions(Gamer gamer, EventType type) {
        if (type == EventType.KING_OF_THE_HILL) {
            List<String> lines = messageList(gamer, type.descriptionKey,
                    Arrays.asList("Bridge to the diamond hill using your kit blocks.", "Earn one point per second alone; contested hills award no points."));
            List<String> populated = new ArrayList<>();
            for (String line : lines) populated.add(line.replace("%event_name%", eventName(gamer, type))
                    .replace("%event_duration_lasts%", formatEventDuration(type.durationMillis))
                    .replace("%participation_coins%", "25").replace("%participation_rating%", "2")
                    .replace("%first_coins%", "300").replace("%first_rating%", "30")
                    .replace("%second_coins%", "200").replace("%second_rating%", "20")
                    .replace("%third_coins%", "150").replace("%third_rating%", "15"));
            return populated;
        }
        if (type != EventType.CANDY_HUNT && type != EventType.JUGGERNAUT && type != EventType.WEAPON_ROULETTE)
            return messageList(gamer, type.descriptionKey, Collections.singletonList(type.displayName));
        List<String> lines = new ArrayList<>();
        for (int line = 1; line <= 2; line++) {
            String key = type.descriptionKey + "_" + line;
            String value = gamer.getLanguage().getMessage(key, "%event_name%", eventName(gamer, type),
                    "%event_duration_lasts%", formatEventDuration(type.durationMillis));
            if (!missingTranslation(value, key)) lines.add(value);
        }
        return lines;
    }

    private List<String> displayedEventDescriptions(Gamer gamer, EventType type) {
        List<String> descriptions = eventDescriptions(gamer, type);
        return type == EventType.CANDY_HUNT || type == EventType.JUGGERNAUT || type == EventType.WEAPON_ROULETTE || type == EventType.KING_OF_THE_HILL
                ? descriptions : balancedDescription(descriptions);
    }

    public String formatDuration(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainder = seconds % 60L;
        return hours > 0L ? String.format("%02d:%02d:%02d", hours, minutes, remainder)
                : String.format("%02d:%02d", minutes, remainder);
    }

    private String formatBossBarDuration(long millis) {
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        long minutes = seconds / 60L;
        long remainder = seconds % 60L;
        return minutes > 0L ? minutes + "m " + remainder + "s" : seconds + "s";
    }

    private String formatEventDuration(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        long minutes = seconds / 60L;
        long remainder = seconds % 60L;
        if (minutes == 0L) return remainder + "s";
        if (remainder == 0L) return minutes + "m";
        return minutes + "m " + remainder + "s";
    }

    private ItemStack item(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) coloredLore.add(color(line));
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    public enum EventType {
        DOUBLE_REWARDS("double_rewards", "Double Rewards", true, 480L * 1000L),
        STREAK_SURGE("streak_surge", "Streak Surge", true, 600L * 1000L),
        ASSIST_RUSH("assist_rush", "Assist Rush", true, 480L * 1000L),
        RANDOM_MODIFIER("random_modifier", "Random Modifier", true, 600L * 1000L),
        REVENGE_CONTRACT("revenge_contract", "Revenge Contract", true, 480L * 1000L),
        KNOCKBACK_CHAOS("knockback_chaos", "Knockback Chaos", false, 600L * 1000L),
        COMBO_HOUR("combo_hour", "Combo Hour", false, 480L * 1000L),
        GOLDEN_KILL("golden_kill", "Golden Kill", true, 600L * 1000L),
        HOT_POTATO("hot_potato", "Hot Potato", true, 480L * 1000L),
        CANDY_HUNT("candy_hunt", "Candy Hunt", true, 480L * 1000L),
        JUGGERNAUT("juggernaut", "Juggernaut", true, 480L * 1000L),
        WEAPON_ROULETTE("weapon_roulette", "Weapon Roulette", true, 480L * 1000L),
        KING_OF_THE_HILL("king_of_the_hill", "King of the Hill", true, 480L * 1000L);

        final String id;
        final String nameKey;
        final String descriptionKey;
        final String displayName;
        final boolean defaultEnabled;
        final long durationMillis;

        EventType(String id, String displayName, boolean defaultEnabled, long durationMillis) {
            this.id = id;
            this.nameKey = "buildffa_event_" + id + "_name";
            this.descriptionKey = "buildffa_event_" + id + "_description";
            this.displayName = displayName;
            this.defaultEnabled = defaultEnabled;
            this.durationMillis = durationMillis;
        }
    }

    private enum EventState {
        ENABLED,
        ACTIVE,
        UNAVAILABLE,
        DISABLED
    }

    private enum StartRequirement {
        READY,
        EVENT_ACTIVE,
        UNAVAILABLE,
        DISABLED
    }

    private enum Modifier {
        SPEED("Speed"),
        JUMP("Jump Boost"),
        FAST_BLOCK_DECAY("Fast Block Decay"),
        PROJECTILE_RESTOCK("Projectile Restocking");

        final String displayName;

        Modifier(String displayName) {
            this.displayName = displayName;
        }
    }

    private static class ScheduledEvent {
        final EventType eventType;
        long scheduledStartTime;
        final long plannedDuration;

        ScheduledEvent(EventType eventType, long scheduledStartTime, long plannedDuration) {
            this.eventType = eventType;
            this.scheduledStartTime = scheduledStartTime;
            this.plannedDuration = plannedDuration;
        }
    }

    private static class Contribution {
        double damage;
        long lastHit;
    }

    private static class EventManagerHolder implements InventoryHolder {
        final UUID viewer;

        EventManagerHolder(UUID viewer) {
            this.viewer = viewer;
        }

        public Inventory getInventory() {
            return null;
        }
    }

    private static class EventStatsHolder implements InventoryHolder {
        final UUID viewer;
        final List<UUID> sessions = new ArrayList<>();

        EventStatsHolder(UUID viewer) {
            this.viewer = viewer;
        }

        public Inventory getInventory() {
            return null;
        }
    }

    private static class PlayerEventHolder implements InventoryHolder {
        final UUID viewer;

        PlayerEventHolder(UUID viewer) {
            this.viewer = viewer;
        }

        public Inventory getInventory() {
            return null;
        }
    }

    private static class PlayerIdentity {
        final UUID playerId;
        final String playerName;
        final String displayName;

        PlayerIdentity(UUID playerId, String playerName, String displayName) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.displayName = displayName;
        }
    }

    private static class EventSessionSnapshot {
        final Map<UUID, EventScore> scores;
        final UUID sessionId;
        final String eventId;
        final long startedAt;
        final long endedAt;
        final long durationMillis;
        final boolean completed;
        final int totalKills;
        final Map<UUID, Integer> kills;
        final Map<UUID, PlayerIdentity> players;

        EventSessionSnapshot(UUID sessionId, String eventId, long startedAt, long endedAt, long durationMillis, boolean completed,
                             int totalKills, Map<UUID, Integer> kills, Map<UUID, PlayerIdentity> players, Map<UUID, EventScore> scores) {
            this.scores = scores;
            this.sessionId = sessionId;
            this.eventId = eventId;
            this.startedAt = startedAt;
            this.endedAt = endedAt;
            this.durationMillis = durationMillis;
            this.completed = completed;
            this.totalKills = totalKills;
            this.kills = kills;
            this.players = players;
        }
    }

    private static class LeaderboardEntry {
        EventScore score;
        final PlayerIdentity identity;
        int kills;
        int eventsPlayed;

        LeaderboardEntry(PlayerIdentity identity) {
            this.identity = identity;
        }
    }

    private static final class EventScore {
        final Map<Integer, Long> retainedReachedAt = new HashMap<>();
        final Map<CandyType, Integer> candyByType = new HashMap<>();
        int capturePoints;
        int candyCollected;
        int candyConsumed;
        long reachedAt = Long.MAX_VALUE;
        int juggernautKills;
        int timesSelected;
        long timeAsJuggernaut;
        int rouletteKills;
        int weaponsReceived;
        final Map<Integer, Integer> killsPerWeapon = new HashMap<>();

        EventScore() {
        }

        EventScore(EventScore other) {
            retainedReachedAt.putAll(other.retainedReachedAt);
            candyByType.putAll(other.candyByType);
            capturePoints = other.capturePoints;
            candyCollected = other.candyCollected;
            candyConsumed = other.candyConsumed;
            reachedAt = other.reachedAt;
            juggernautKills = other.juggernautKills;
            timesSelected = other.timesSelected;
            timeAsJuggernaut = other.timeAsJuggernaut;
            rouletteKills = other.rouletteKills;
            weaponsReceived = other.weaponsReceived;
            killsPerWeapon.putAll(other.killsPerWeapon);
        }

        int retained() {
            return Math.max(0, candyCollected - candyConsumed);
        }

        boolean collectCandy(CandyType type) {
            if (candyCollected == Integer.MAX_VALUE) return false;
            candyCollected++;
            candyByType.put(type, Math.max(0, candyByType.getOrDefault(type, 0)) + 1);
            return true;
        }

        boolean consumeCandy(CandyType type) {
            int available = Math.max(0, candyByType.getOrDefault(type, 0));
            if (available == 0 || retained() == 0) return false;
            candyByType.put(type, available - 1);
            candyConsumed = Math.min(candyCollected, candyConsumed + 1);
            return true;
        }
    }

    public enum EventBonus {
        NONE(0, 0),
        DOUBLE_REWARDS(25, 7),
        GOLDEN_KILL(150, 15),
        JUGGERNAUT(50, 12),
        WEAPON_ROULETTE(15, 7),
        STREAK_SURGE(20, 8),
        RANDOM_MODIFIER(25, 8),
        REVENGE_CONTRACT(35, 8),
        KNOCKBACK_CHAOS(15, 5),
        COMBO_HOUR(15, 5);

        private final int coins;
        private final int rating;

        EventBonus(int coins, int rating) {
            this.coins = coins;
            this.rating = rating;
        }

        public int getCoins() {
            return coins;
        }

        public int getRating() {
            return rating;
        }
    }

    private enum RouletteWeapon {
        WOOD_SWORD("wood_sword", Material.WOOD_SWORD, 1, 0, 0),
        STONE_SWORD("stone_sword", Material.STONE_SWORD, 1, 0, 0),
        GOLD_SWORD("gold_sword", Material.GOLD_SWORD, 1, 0, 0),
        IRON_AXE("iron_axe", Material.IRON_AXE, 1, 0, 0),
        BOW("bow", Material.BOW, 0, 0, 3),
        KNOCKBACK_SWORD("knockback_sword", Material.WOOD_SWORD, 1, 1, 0),
        WOOD_PICKAXE("wood_pickaxe", Material.WOOD_PICKAXE, 2, 0, 0),
        STONE_PICKAXE("stone_pickaxe", Material.STONE_PICKAXE, 2, 0, 0),
        IRON_PICKAXE("iron_pickaxe", Material.IRON_PICKAXE, 2, 0, 0),
        GOLD_PICKAXE("gold_pickaxe", Material.GOLD_PICKAXE, 2, 0, 0),
        DIAMOND_PICKAXE("diamond_pickaxe", Material.DIAMOND_PICKAXE, 2, 0, 0),
        WOOD_HOE("wood_hoe", Material.WOOD_HOE, 2, 0, 0),
        STONE_HOE("stone_hoe", Material.STONE_HOE, 2, 0, 0),
        IRON_HOE("iron_hoe", Material.IRON_HOE, 2, 0, 0),
        GOLD_HOE("gold_hoe", Material.GOLD_HOE, 2, 0, 0),
        DIAMOND_HOE("diamond_hoe", Material.DIAMOND_HOE, 2, 0, 0),
        WOOD_SHOVEL("wood_shovel", Material.WOOD_SPADE, 2, 0, 0),
        STONE_SHOVEL("stone_shovel", Material.STONE_SPADE, 2, 0, 0),
        IRON_SHOVEL("iron_shovel", Material.IRON_SPADE, 2, 0, 0),
        GOLD_SHOVEL("gold_shovel", Material.GOLD_SPADE, 2, 0, 0),
        DIAMOND_SHOVEL("diamond_shovel", Material.DIAMOND_SPADE, 2, 0, 0),
        RAW_FISH("raw_fish", Material.RAW_FISH, 0, 1, 0),
        DIAMOND_SWORD("diamond_sword", Material.DIAMOND_SWORD, 1, 0, 0),
        POPPY("poppy", Material.RED_ROSE, 3, 0, 0);

        final String id;
        final ItemStack template;

        RouletteWeapon(String id, Material material, int sharpness, int knockback, int power) {
            this.id = id;
            template = new ItemStack(material, 1, (short) 0);
            if (sharpness > 0) template.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, sharpness);
            if (knockback > 0) template.addUnsafeEnchantment(Enchantment.KNOCKBACK, knockback);
            if (power > 0) template.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, power);
            if (material == Material.BOW) template.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
        }
    }
    private enum CandyType {
        CHOCOLATE("chocolate",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2MxYjM1MzVkYjNhYmJmMTVlOTA4NzNmNTQyMzNjZDdjMGZmYWE1NDk0ZWJmZDhlOThiMGRiZDZhMTQzYjY2YyJ9fX0=",
                "8", new String[]{"jump_one"}, new PotionEffect(PotionEffectType.JUMP, 160, 0)),
        CANDY_CORN("candy_corn",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmMzYTE0MWY2NWQ0MTAxMzU1ZTNlZjBlZDg4ZGEwODhlNmQwMDNhYjZlYzkxMDY1NDhlYTEzMTI4OTExZjMzNCJ9fX0=",
                "12", new String[]{"speed"}, new PotionEffect(PotionEffectType.SPEED, 240, 1)),
        CARAMEL_APPLE("caramel_apple",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWZhNThiNGE0MzMwOWM0YWJiNGVkMzU1NDQ4NmZkMWJiOTUyY2MyNmFmMDlkZTZiYmRmNDYxZDMwNDkzMGU4MyJ9fX0=",
                "6", new String[]{"regeneration_two"}, new PotionEffect(PotionEffectType.REGENERATION, 120, 1));

        final String id;
        final String texture;
        final String duration;
        final String[] effectIds;
        final PotionEffect[] effects;

        CandyType(String id, String texture, String duration, String[] effectIds, PotionEffect... effects) {
            this.id = id;
            this.texture = texture;
            this.duration = duration;
            this.effectIds = effectIds;
            this.effects = effects;
        }
    }

    private static final class CandyMenuHolder implements InventoryHolder {
        final UUID owner;
        final UUID session;

        CandyMenuHolder(UUID owner, UUID session) {
            this.owner = owner;
            this.session = session;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class OwnedEffect {
        final PotionEffect previous;
        final long previousUntil;
        final int amplifier;
        final boolean ambient;
        final boolean particles;
        final long until;

        OwnedEffect(PotionEffect previous, long previousUntil, PotionEffect effect, long until) {
            this.previous = previous;
            this.previousUntil = previousUntil;
            this.amplifier = effect.getAmplifier();
            this.ambient = effect.isAmbient();
            this.particles = effect.hasParticles();
            this.until = until;
        }

        boolean owns(PotionEffect current, long now) {
            return current != null && current.getAmplifier() == amplifier
                    && current.isAmbient() == ambient && current.hasParticles() == particles
                    && Math.abs(current.getDuration() - Math.max(0L, until - now)) <= 2L;
        }
    }
}
