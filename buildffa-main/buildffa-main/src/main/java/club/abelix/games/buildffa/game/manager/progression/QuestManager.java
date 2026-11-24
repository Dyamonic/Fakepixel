package club.abelix.games.buildffa.game.manager.progression;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.dto.dao.QuestProgressDAO;
import club.abelix.games.buildffa.game.dto.dao.impl.QuestProgressDAOImpl;
import club.abelix.network.type.mode.Uncommon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuestManager extends YListener {
    private final BuildFFAGame game;
    private final Logger logger;
    private final List<Quest> dailyQuests = new ArrayList<>();
    private final List<Quest> weeklyQuests = new ArrayList<>();
    private final QuestProgressDAO questProgressDAO = QuestProgressDAOImpl.INSTANCE;
    private final Map<Integer, CompletableFuture<Void>> pendingWrites = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> loadingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastInteractions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> openMenus = new ConcurrentHashMap<>();
    private final Set<String> pendingClaims = ConcurrentHashMap.newKeySet();
    private final Set<Integer> resettingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> questEpochs = new ConcurrentHashMap<>();
    private final Map<String, QuestFailureDiagnostic> diagnostics = new LinkedHashMap<>();
    private final AtomicLong diagnosticSequence = new AtomicLong();
    private String latestDiagnosticId;
    private volatile boolean shuttingDown;
    private String observedDailyKey;
    private String observedWeeklyKey;

    public QuestManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
        this.logger = game.getPlugin().getLogger();
        dailyQuests.add(new Quest("daily_kills_10", "&aDaily Slayer I", "Kill 10 players", "kills", 10, 300, Material.PAPER));
        dailyQuests.add(new Quest("daily_kills", "&aDaily Slayer II", "Kill 25 players", "kills", 25, 650, Material.PAPER));
        dailyQuests.add(new Quest("daily_rating_25", "&aDaily Climber I", "Gain 25 rating", "rating", 25, 300, Material.PAPER));
        dailyQuests.add(new Quest("daily_rating_50", "&aDaily Climber II", "Gain 50 rating", "rating", 50, 550, Material.PAPER));
        dailyQuests.add(new Quest("daily_rating", "&aDaily Climber III", "Gain 100 rating", "rating", 100, 850, Material.PAPER));
        dailyQuests.add(new Quest("daily_streak", "&aDaily Streak I", "Reach a 5 killstreak", "streak", 5, 400, Material.PAPER));
        dailyQuests.add(new Quest("daily_streak_10", "&aDaily Streak II", "Reach a 10 killstreak", "streak", 10, 800, Material.PAPER));
        dailyQuests.add(new Quest("daily_coins_500", "&aDaily Earner I", "Earn 500 gameplay coins", "coins", 500, 350, Material.PAPER));
        dailyQuests.add(new Quest("daily_coins_1000", "&aDaily Earner II", "Earn 1,000 gameplay coins", "coins", 1000, 650, Material.PAPER));
        dailyQuests.add(new Quest("daily_bounty_1", "&aDaily Bounty Hunter", "Claim one bounty", "bounties", 1, 500, Material.PAPER));
        weeklyQuests.add(new Quest("weekly_kills_100", "&6Weekly Slayer I", "Kill 100 players", "kills", 100, 3000, Material.PAPER));
        weeklyQuests.add(new Quest("weekly_kills", "&6Weekly Slayer II", "Kill 250 players", "kills", 250, 6500, Material.PAPER));
        weeklyQuests.add(new Quest("weekly_rating_300", "&6Weekly Climber I", "Gain 300 rating", "rating", 300, 3500, Material.PAPER));
        weeklyQuests.add(new Quest("weekly_rating_600", "&6Weekly Climber II", "Gain 600 rating", "rating", 600, 6000, Material.PAPER));
        weeklyQuests.add(new Quest("weekly_rating", "&6Weekly Climber III", "Gain 1,000 rating", "rating", 1000, 8000, Material.PAPER));
        weeklyQuests.add(new Quest("weekly_streak_10", "&6Weekly Streak I", "Reach a 10 killstreak", "streak", 10, 2500, Material.PAPER));
        weeklyQuests.add(new Quest("weekly_streak", "&6Weekly Streak II", "Reach a 20 killstreak", "streak", 20, 5000, Material.PAPER));
        weeklyQuests.add(new Quest("weekly_coins_10000", "&6Weekly Earner", "Earn 10,000 gameplay coins", "coins", 10000, 4500, Material.PAPER));
        weeklyQuests.add(new Quest("weekly_bounty_5", "&6Weekly Bounty Hunter", "Claim 5 bounties", "bounties", 5, 4000, Material.PAPER));
        observedDailyKey = periodKey("daily");
        observedWeeklyKey = periodKey("weekly");
    }

    public void open(Player player) {
        open(player, true);
    }

    private void open(Player player, boolean debounce) {
        if (shuttingDown || player == null || !player.isOnline()) return;
        UUID playerUuid = player.getUniqueId();
        int playerId = Gamer.getGamer(player).getPlayerId();
        if (resettingPlayers.contains(playerId)) return;
        if (debounce) {
            long now = System.nanoTime();
            Long previousInteraction = lastInteractions.put(playerUuid, now);
            if (previousInteraction != null && now - previousInteraction < 500_000_000L) return;
        }
        if (openMenus.containsKey(playerUuid)) return;
        UUID requestId = UUID.randomUUID();
        if (loadingPlayers.putIfAbsent(playerUuid, requestId) != null) return;
        String dailyKey = periodKey("daily");
        String weeklyKey = periodKey("weekly");
        afterPendingWrites(playerId, () -> loadSnapshot(playerUuid, playerId, dailyKey, weeklyKey))
                .whenComplete((snapshot, throwable) -> runSync(() -> finishOpen(player, playerUuid, requestId, snapshot, throwable)));
    }

    private CompletableFuture<QuestSnapshot> loadSnapshot(
            UUID playerUuid, int playerId, String dailyKey, String weeklyKey) {
        List<Quest> activeDaily = selectQuests(dailyQuests, playerUuid, "daily", dailyKey);
        List<Quest> activeWeekly = selectQuests(weeklyQuests, playerUuid, "weekly", weeklyKey);
        List<CompletableFuture<QuestEntry>> loads = new ArrayList<>();
        for (Quest quest : activeDaily) loads.add(loadQuest(playerUuid, playerId, quest, "DAILY", dailyKey));
        for (Quest quest : activeWeekly) loads.add(loadQuest(playerUuid, playerId, quest, "WEEKLY", weeklyKey));
        CompletableFuture<Map<String, QuestProgressDAO.QuestProgress>> snapshot =
                CompletableFuture.completedFuture(new LinkedHashMap<>());
        for (CompletableFuture<QuestEntry> load : loads) {
            snapshot = snapshot.thenCombine(load, (values, entry) -> {
                values.put(entry.questId, entry.progress);
                return values;
            });
        }
        return snapshot.thenApply(values -> new QuestSnapshot(activeDaily, activeWeekly,
                Collections.unmodifiableMap(new LinkedHashMap<>(values)), dailyKey, weeklyKey));
    }

    private CompletableFuture<QuestEntry> loadQuest(
            UUID playerUuid, int playerId, Quest quest, String periodType, String periodKey) {
        return questProgressDAO.ensureProgress(playerId, quest.id, periodType, periodKey)
                .thenCompose(ignored -> questProgressDAO.getProgress(playerId, quest.id, periodType, periodKey))
                .handle((progress, throwable) -> {
                    if (throwable != null) {
                        reportFailure(playerUuid, quest.id, "load progress using GET_ONE", throwable);
                        throw new QuestOperationException(quest.id, "load progress using GET_ONE", throwable);
                    }
                    if (progress == null) {
                        IllegalStateException failure = new IllegalStateException("Quest progress result was null");
                        reportFailure(playerUuid, quest.id, "decode GET_ONE result", failure);
                        throw new QuestOperationException(quest.id, "decode GET_ONE result", failure);
                    }
                    return new QuestEntry(quest.id, progress);
                });
    }

    private void finishOpen(Player player, UUID playerUuid, UUID requestId,
                            QuestSnapshot snapshot, Throwable throwable) {
        if (!loadingPlayers.remove(playerUuid, requestId)) return;
        if (!isCurrentSession(player, playerUuid)) return;
        if (throwable != null) {
            if (findQuestFailure(throwable) == null) {
                reportFailure(playerUuid, null, "combine quest snapshot", throwable);
            }
            String errorId = captureDiagnostic(playerUuid, throwable);
            player.sendMessage(color("&cCould not load quests. Error ID: &f" + errorId));
            return;
        }
        Inventory inv = Bukkit.createInventory(new QuestHolder(requestId), 54, color("&8BuildFFA Quests"));
        ItemStack filler = named(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        inv.setItem(4, named(Material.DIAMOND_SWORD, (short) 0, "&aBuildFFA Quests", Arrays.asList(
                color("&7Complete daily and weekly quests"), color("&7to earn extra coins."))));
        int[] dailySlots = {10, 11, 12};
        int[] weeklySlots = {14, 15, 16};
        for (int i = 0; i < snapshot.daily.size() && i < dailySlots.length; i++) {
            inv.setItem(dailySlots[i], icon(snapshot.daily.get(i), snapshot.progress, resetRemaining("daily")));
        }
        for (int i = 0; i < snapshot.weekly.size() && i < weeklySlots.length; i++) {
            inv.setItem(weeklySlots[i], icon(snapshot.weekly.get(i), snapshot.progress, resetRemaining("weekly")));
        }
        inv.setItem(39, rotationIcon("&aDaily Rotation", snapshot.dailyKey, resetRemaining("daily")));
        inv.setItem(41, rotationIcon("&6Weekly Rotation", snapshot.weeklyKey, resetRemaining("weekly")));
        inv.setItem(49, named(Material.BARRIER, (short) 0, "&cClose", Collections.singletonList(color("&7Click to close this menu."))));
        player.openInventory(inv);
        openMenus.put(playerUuid, requestId);
    }

    public void handleKill(Player player, int streak) {
        if (player == null) return;
        int playerId = Gamer.getGamer(player).getPlayerId();
        UUID playerUuid = player.getUniqueId();
        updateType(playerUuid, playerId, "daily", "kills", 1, false);
        updateType(playerUuid, playerId, "weekly", "kills", 1, false);
        updateType(playerUuid, playerId, "daily", "streak", streak, true);
        updateType(playerUuid, playerId, "weekly", "streak", streak, true);
    }

    public void handleRating(Gamer gamer, int ratingGained) {
        if (gamer == null || ratingGained <= 0) return;
        int playerId = gamer.getPlayerId();
        UUID playerUuid = gamer.getPlayer().getUniqueId();
        updateType(playerUuid, playerId, "daily", "rating", ratingGained, false);
        updateType(playerUuid, playerId, "weekly", "rating", ratingGained, false);
    }

    public void handleCoinsEarned(Player player, int coins) {
        if (player == null || coins <= 0) return;
        Gamer gamer = Gamer.getGamer(player);
        updateType(player.getUniqueId(), gamer.getPlayerId(), "daily", "coins", coins, false);
        updateType(player.getUniqueId(), gamer.getPlayerId(), "weekly", "coins", coins, false);
    }

    public void handleBountyClaim(Player player) {
        if (player == null) return;
        Gamer gamer = Gamer.getGamer(player);
        updateType(player.getUniqueId(), gamer.getPlayerId(), "daily", "bounties", 1, false);
        updateType(player.getUniqueId(), gamer.getPlayerId(), "weekly", "bounties", 1, false);
    }

    public void checkRotationReset() {
        String dailyKey = periodKey("daily");
        String weeklyKey = periodKey("weekly");
        if (dailyKey.equals(observedDailyKey) && weeklyKey.equals(observedWeeklyKey)) return;
        observedDailyKey = dailyKey;
        observedWeeklyKey = weeklyKey;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (openMenus.containsKey(player.getUniqueId())) refresh(player);
        }
    }

    private ItemStack rotationIcon(String name, String key, String remaining) {
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Rotation: &f" + key));
        lore.add(color("&7Resets in: &f" + remaining));
        return named(Material.WATCH, (short) 0, name, lore);
    }

    private ItemStack icon(Quest quest, Map<String, QuestProgressDAO.QuestProgress> snapshot, String remaining) {
        int progress = progress(snapshot, quest.id);
        boolean complete = progress >= quest.goal;
        boolean claimed = claimed(snapshot, quest.id);
        List<String> lore = new ArrayList<>();
        lore.add(color("&7" + quest.description));
        lore.add(color("&7Progress: &f" + Math.min(progress, quest.goal) + "&7/&f" + quest.goal));
        lore.add(color("&7Reward: &e" + quest.rewardCoins + " coins"));
        lore.add(color("&7Resets in: &f" + remaining));
        lore.add("");
        lore.add(claimed ? color("&aClaimed") : complete ? color("&eClick To Claim") : color("&cNot Completed"));
        lore.add(color("&8quest:" + quest.id));
        return named(quest.material, (short) 0, quest.name, lore);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof QuestHolder)) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (item.getType() == Material.BARRIER) {
            event.getWhoClicked().closeInventory();
            return;
        }
        if (!item.hasItemMeta() || item.getItemMeta().getLore() == null) return;
        String id = null;
        for (String line : item.getItemMeta().getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped.startsWith("quest:")) id = stripped.substring(6);
        }
        if (id != null) claim((Player) event.getWhoClicked(), id);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof QuestHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof QuestHolder)) return;
        QuestHolder holder = (QuestHolder) event.getInventory().getHolder();
        openMenus.remove(event.getPlayer().getUniqueId(), holder.menuId);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayerState(event.getPlayer());
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != game.getPlugin()) return;
        shuttingDown = true;
        loadingPlayers.clear();
        lastInteractions.clear();
        openMenus.clear();
        pendingClaims.clear();
        resettingPlayers.clear();
        questEpochs.clear();
        pendingWrites.clear();
        synchronized (diagnostics) {
            diagnostics.clear();
            latestDiagnosticId = null;
        }
    }

    private void claim(Player player, String id) {
        if (shuttingDown || !player.isOnline()) return;
        Quest quest = getQuest(id);
        if (quest == null) return;
        UUID playerUuid = player.getUniqueId();
        String claimKey = playerUuid + ":" + id;
        if (!pendingClaims.add(claimKey)) return;
        Gamer gamer = Gamer.getGamer(player);
        int playerId = gamer.getPlayerId();
        if (resettingPlayers.contains(playerId)) {
            pendingClaims.remove(claimKey);
            return;
        }
        long questEpoch = questEpochs.getOrDefault(playerUuid, 0L);
        String period = id.startsWith("weekly") ? "weekly" : "daily";
        String key = periodKey(period);
        List<Quest> active = selectQuests("weekly".equals(period) ? weeklyQuests : dailyQuests,
                playerUuid, period, key);
        if (!active.contains(quest)) {
            pendingClaims.remove(claimKey);
            player.sendMessage(color("&cThat quest has expired."));
            refresh(player);
            return;
        }
        int reward = quest.rewardCoins;
        afterPendingWrites(playerId, () -> questProgressDAO.claim(
                playerId, id, period.toUpperCase(Locale.ROOT), key, quest.goal
        )).whenComplete((claimed, throwable) -> runSync(() -> finishClaim(
                player, playerUuid, questEpoch, claimKey, quest, key, reward, claimed, throwable)));
    }

    private void finishClaim(Player player, UUID playerUuid, long questEpoch, String claimKey, Quest quest, String rotationKey, int reward,
                             QuestProgressDAO.ClaimResult result, Throwable throwable) {
        if (questEpochs.getOrDefault(playerUuid, 0L) != questEpoch) {
            pendingClaims.remove(claimKey);
            return;
        }
        if (!isCurrentSession(player, playerUuid)) {
            pendingClaims.remove(claimKey);
            return;
        }
        if (throwable != null) {
            pendingClaims.remove(claimKey);
            String operation = "claim quest for rotation " + rotationKey;
            reportFailure(playerUuid, quest.id, operation, throwable);
            String errorId = captureDiagnostic(playerUuid, new QuestOperationException(quest.id, operation, throwable));
            player.sendMessage(color("&cCould not claim this quest. Error ID: &f" + errorId));
            return;
        }
        if (result == QuestProgressDAO.ClaimResult.INCOMPLETE) {
            pendingClaims.remove(claimKey);
            player.sendMessage(color("&cYou have not completed this quest yet."));
            refresh(player);
            return;
        }
        if (result == QuestProgressDAO.ClaimResult.ALREADY_CLAIMED) {
            pendingClaims.remove(claimKey);
            player.sendMessage(color("&cYou already claimed this quest."));
            refresh(player);
            return;
        }
        if (result != QuestProgressDAO.ClaimResult.CLAIMED) {
            pendingClaims.remove(claimKey);
            IllegalStateException failure = new IllegalStateException("Quest claim returned " + result);
            String operation = "claim quest for rotation " + rotationKey;
            reportFailure(playerUuid, quest.id, operation, failure);
            String errorId = captureDiagnostic(playerUuid, new QuestOperationException(quest.id, operation, failure));
            player.sendMessage(color("&cCould not claim this quest. Error ID: &f" + errorId));
            return;
        }
        Gamer.getGamer(player).addCoins(Uncommon.BUILDFFA, reward);
        pendingClaims.remove(claimKey);
        player.sendMessage(color("&aQuest claimed! You received &e" + quest.rewardCoins + " coins&a."));
        refresh(player);
    }

    private void refresh(Player player) {
        UUID playerUuid = player.getUniqueId();
        openMenus.remove(playerUuid);
        player.closeInventory();
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            if (isCurrentSession(player, playerUuid)) open(player, false);
        });
    }

    private void clearPlayerState(Player player) {
        UUID playerUuid = player.getUniqueId();
        loadingPlayers.remove(playerUuid);
        lastInteractions.remove(playerUuid);
        openMenus.remove(playerUuid);
    }

    private boolean isCurrentSession(Player player, UUID playerUuid) {
        return !shuttingDown && player.isOnline() && Bukkit.getPlayer(playerUuid) == player;
    }

    private void runSync(Runnable action) {
        if (!shuttingDown) Bukkit.getScheduler().runTask(game.getPlugin(), action);
    }

    private <T> CompletableFuture<T> afterPendingWrites(int playerId, Supplier<CompletableFuture<T>> operation) {
        CompletableFuture<Void> pending = pendingWrites.getOrDefault(playerId, CompletableFuture.completedFuture(null));
        return pending.handle((ignored, throwable) -> null).thenCompose(ignored -> operation.get());
    }

    public CompletableFuture<Void> resetPlayerProgress(UUID playerUuid, int playerId) {
        resettingPlayers.add(playerId);
        questEpochs.merge(playerUuid, 1L, Long::sum);
        loadingPlayers.remove(playerUuid);
        lastInteractions.remove(playerUuid);
        openMenus.remove(playerUuid);
        for (String key : new ArrayList<>(pendingClaims)) {
            if (key.startsWith(playerUuid + ":")) pendingClaims.remove(key);
        }
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.getOpenInventory().getTopInventory().getHolder() instanceof QuestHolder) {
            player.closeInventory();
        }
        CompletableFuture<Void> reset = afterPendingWrites(playerId,
                () -> questProgressDAO.resetPlayerProgress(playerId));
        return reset.whenComplete((ignored, throwable) -> resettingPlayers.remove(playerId));
    }

    private void queueWrite(UUID playerUuid, int playerId, String questId,
                            Supplier<CompletableFuture<Void>> write, String description) {
        if (resettingPlayers.contains(playerId)) return;
        CompletableFuture<Void> next = pendingWrites.compute(playerId, (ignored, pending) -> {
            CompletableFuture<Void> previous = pending == null ? CompletableFuture.completedFuture(null) : pending;
            return previous.handle((result, throwable) -> null).thenCompose(result -> write.get());
        });
        next.whenComplete((result, throwable) -> {
            if (throwable != null) reportFailure(playerUuid, questId, description, throwable);
            pendingWrites.remove(playerId, next);
        });
    }

    private void add(UUID playerUuid, int playerId, Quest quest, String period, int amount) {
        String periodType = period.toUpperCase(Locale.ROOT);
        String key = periodKey(period);
        queueWrite(playerUuid, playerId, quest.id,
                () -> questProgressDAO.addProgress(playerId, quest.id, periodType, key, amount, quest.goal),
                "Could not add progress for quest " + quest.id + " and player " + playerId);
    }

    private void setMax(UUID playerUuid, int playerId, Quest quest, String period, int value) {
        String periodType = period.toUpperCase(Locale.ROOT);
        String key = periodKey(period);
        queueWrite(playerUuid, playerId, quest.id,
                () -> questProgressDAO.setMaxProgress(playerId, quest.id, periodType, key, value, quest.goal),
                "Could not update progress for quest " + quest.id + " and player " + playerId);
    }

    private void updateType(UUID playerUuid, int playerId, String period, String type, int value, boolean maximum) {
        String key = periodKey(period);
        List<Quest> pool = "weekly".equals(period) ? weeklyQuests : dailyQuests;
        for (Quest quest : selectQuests(pool, playerUuid, period, key)) {
            if (!quest.type.equals(type)) continue;
            if (maximum) setMax(playerUuid, playerId, quest, period, value);
            else add(playerUuid, playerId, quest, period, value);
        }
    }

    private List<Quest> selectQuests(List<Quest> pool, UUID playerUuid, String period, String key) {
        List<Quest> selected = deterministicSelection(pool, playerUuid, period, key);
        String previousKey = previousPeriodKey(period, key);
        List<Quest> previous = deterministicSelection(pool, playerUuid, period, previousKey);
        if (new HashSet<>(selected).equals(new HashSet<>(previous)) && pool.size() > selected.size()) {
            for (Quest candidate : deterministicOrder(pool, playerUuid, period, key + ":alternate")) {
                if (!selected.contains(candidate)) {
                    selected.set(selected.size() - 1, candidate);
                    break;
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(selected));
    }

    private List<Quest> deterministicSelection(List<Quest> pool, UUID playerUuid, String period, String key) {
        List<Quest> ordered = deterministicOrder(pool, playerUuid, period, key);
        return new ArrayList<>(ordered.subList(0, Math.min(3, ordered.size())));
    }

    private List<Quest> deterministicOrder(List<Quest> pool, UUID playerUuid, String period, String key) {
        List<Quest> ordered = new ArrayList<>(pool);
        ordered.sort(Comparator.comparingLong(quest -> rotationHash(playerUuid, period, key, quest.id)));
        return ordered;
    }

    private long rotationHash(UUID playerUuid, String period, String key, String questId) {
        long hash = 1125899906842597L;
        String value = playerUuid + ":" + period + ":" + key + ":" + questId;
        for (int i = 0; i < value.length(); i++) hash = 31L * hash + value.charAt(i);
        return hash;
    }

    private String previousPeriodKey(String period, String key) {
        if ("daily".equals(period)) return LocalDate.parse(key).minusDays(1).toString();
        LocalDate today = LocalDate.now();
        return weekKey(today.minusWeeks(1));
    }

    private String resetRemaining(String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reset;
        if ("weekly".equals(period)) {
            reset = now.toLocalDate().with(DayOfWeek.MONDAY).plusWeeks(1).atStartOfDay();
        } else {
            reset = now.toLocalDate().plusDays(1).atStartOfDay();
        }
        Duration duration = Duration.between(now, reset);
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        return hours + "h " + minutes + "m";
    }

    private void reportFailure(Object player, String questId, String operation, Throwable throwable) {
        QuestOperationException questFailure = findQuestFailure(throwable);
        if (questFailure != null) {
            questId = questFailure.questId;
            operation = questFailure.operation;
        }
        Throwable cause = unwrap(throwable);
        logger.log(Level.SEVERE, "Quest operation failed [player=" + player + ", quest=" + questId
                + ", operation=" + operation + ", databaseAvailable=" + questProgressDAO.isAvailable() + "]", cause);
    }

    private QuestOperationException findQuestFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof QuestOperationException) return (QuestOperationException) current;
            current = current.getCause();
        }
        return null;
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) current = current.getCause();
        while (current instanceof QuestOperationException && current.getCause() != null) current = current.getCause();
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) current = current.getCause();
        return current;
    }

    public void showDiagnostic(Player player, String requestedId) {
        QuestFailureDiagnostic diagnostic;
        synchronized (diagnostics) {
            String id = requestedId == null ? latestDiagnosticId : requestedId.toUpperCase(Locale.ROOT);
            diagnostic = id == null ? null : diagnostics.get(id);
        }
        if (diagnostic == null) {
            player.sendMessage(color("&cNo matching quest failure is stored."));
            return;
        }
        player.sendMessage(color("&6Quest failure &f" + diagnostic.id));
        player.sendMessage(color("&7Player: &f" + diagnostic.playerUuid));
        player.sendMessage(color("&7Operation: &f" + diagnostic.operation));
        player.sendMessage(color("&7Quest: &f" + diagnostic.questId));
        player.sendMessage(color("&7Exception: &f" + diagnostic.exceptionClass + "&7: &f" + diagnostic.exceptionMessage));
        player.sendMessage(color("&7Root cause: &f" + diagnostic.rootClass + "&7: &f" + diagnostic.rootMessage));
        player.sendMessage(color("&7DAO initialization: &f" + diagnostic.initializationState));
        player.sendMessage(color("&7Database available: &f" + diagnostic.databaseAvailable));
    }

    public void retryInitialization(Player player) {
        QuestProgressDAO.InitializationState before = questProgressDAO.getInitializationState();
        questProgressDAO.retryInitialization().whenComplete((result, throwable) -> runSync(() -> {
            if (!player.isOnline()) return;
            if (throwable != null) {
                String errorId = captureDiagnostic(player.getUniqueId(), new QuestOperationException(
                        null, "retry DAO initialization", throwable
                ));
                reportFailure(player.getUniqueId(), null, "retry DAO initialization", throwable);
                player.sendMessage(color("&cQuest DAO retry failed. Error ID: &f" + errorId));
                return;
            }
            if (before != QuestProgressDAO.InitializationState.FAILED) {
                player.sendMessage(color("&eQuest DAO initialization was not failed. Current state: &f"
                        + questProgressDAO.getInitializationState()));
                return;
            }
            player.sendMessage(color("&aQuest DAO initialization retry completed."));
        }));
    }

    private String captureDiagnostic(UUID playerUuid, Throwable throwable) {
        QuestOperationException questFailure = findQuestFailure(throwable);
        Throwable root = unwrap(throwable);
        String id = "Q" + Long.toString(diagnosticSequence.incrementAndGet(), 36).toUpperCase(Locale.ROOT);
        QuestFailureDiagnostic diagnostic = new QuestFailureDiagnostic(
                id,
                playerUuid,
                questFailure == null ? null : questFailure.operation,
                questFailure == null ? null : questFailure.questId,
                throwable.getClass().getName(),
                sanitize(throwable.getMessage()),
                root.getClass().getName(),
                sanitize(root.getMessage()),
                questProgressDAO.getInitializationState(),
                questProgressDAO.isAvailable(),
                throwable
        );
        synchronized (diagnostics) {
            diagnostics.put(id, diagnostic);
            latestDiagnosticId = id;
            while (diagnostics.size() > 50) diagnostics.remove(diagnostics.keySet().iterator().next());
        }
        return id;
    }

    private String sanitize(String message) {
        if (message == null || message.isEmpty()) return "(none)";
        String sanitized = message
                .replaceAll("(?i)jdbc:[^\\s]+", "[redacted-jdbc]")
                .replaceAll("(?i)(password|passwd|username|user|host|url)\\s*[=:]\\s*[^\\s,;]+", "$1=[redacted]")
                .replaceAll("(?i)for user\\s+['\"][^'\"]+['\"]@['\"][^'\"]+['\"]", "for user [redacted]")
                .replaceAll("(?<![0-9])(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?![0-9])", "[redacted-host]");
        return sanitized.length() > 240 ? sanitized.substring(0, 240) : sanitized;
    }

    private int progress(Map<String, QuestProgressDAO.QuestProgress> snapshot, String id) {
        QuestProgressDAO.QuestProgress progress = snapshot.get(id);
        return progress == null ? 0 : progress.getProgress();
    }

    private boolean claimed(Map<String, QuestProgressDAO.QuestProgress> snapshot, String id) {
        QuestProgressDAO.QuestProgress progress = snapshot.get(id);
        return progress != null && progress.isClaimed();
    }

    private Quest getQuest(String id) {
        for (Quest quest : dailyQuests) if (quest.id.equals(id)) return quest;
        for (Quest quest : weeklyQuests) if (quest.id.equals(id)) return quest;
        return null;
    }

    private String periodKey(String period) {
        return "weekly".equals(period) ? getWeekKey() : LocalDate.now().toString();
    }

    private String getWeekKey() {
        return weekKey(LocalDate.now());
    }

    private String weekKey(LocalDate today) {
        WeekFields weekFields = WeekFields.ISO;
        return today.get(weekFields.weekBasedYear()) + "-W" + today.get(weekFields.weekOfWeekBasedYear());
    }

    private ItemStack named(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static class QuestHolder implements InventoryHolder {
        final UUID menuId;

        QuestHolder(UUID menuId) {
            this.menuId = menuId;
        }

        public Inventory getInventory() { return null; }
    }

    private static class QuestEntry {
        final String questId;
        final QuestProgressDAO.QuestProgress progress;

        QuestEntry(String questId, QuestProgressDAO.QuestProgress progress) {
            this.questId = questId;
            this.progress = progress;
        }
    }

    private static class QuestSnapshot {
        final List<Quest> daily;
        final List<Quest> weekly;
        final Map<String, QuestProgressDAO.QuestProgress> progress;
        final String dailyKey;
        final String weeklyKey;

        QuestSnapshot(List<Quest> daily, List<Quest> weekly,
                      Map<String, QuestProgressDAO.QuestProgress> progress,
                      String dailyKey, String weeklyKey) {
            this.daily = daily;
            this.weekly = weekly;
            this.progress = progress;
            this.dailyKey = dailyKey;
            this.weeklyKey = weeklyKey;
        }
    }

    private static class QuestOperationException extends CompletionException {
        final String questId;
        final String operation;

        QuestOperationException(String questId, String operation, Throwable cause) {
            super(cause);
            this.questId = questId;
            this.operation = operation;
        }
    }

    private static class QuestFailureDiagnostic {
        final String id;
        final UUID playerUuid;
        final String operation;
        final String questId;
        final String exceptionClass;
        final String exceptionMessage;
        final String rootClass;
        final String rootMessage;
        final QuestProgressDAO.InitializationState initializationState;
        final boolean databaseAvailable;
        final Throwable original;

        QuestFailureDiagnostic(String id, UUID playerUuid, String operation, String questId,
                               String exceptionClass, String exceptionMessage, String rootClass, String rootMessage,
                               QuestProgressDAO.InitializationState initializationState, boolean databaseAvailable,
                               Throwable original) {
            this.id = id;
            this.playerUuid = playerUuid;
            this.operation = operation;
            this.questId = questId;
            this.exceptionClass = exceptionClass;
            this.exceptionMessage = exceptionMessage;
            this.rootClass = rootClass;
            this.rootMessage = rootMessage;
            this.initializationState = initializationState;
            this.databaseAvailable = databaseAvailable;
            this.original = original;
        }
    }

    private static class Quest {
        final String id;
        final String name;
        final String description;
        final String type;
        final int goal;
        final int rewardCoins;
        final Material material;

        Quest(String id, String name, String description, String type, int goal, int rewardCoins, Material material) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.goal = goal;
            this.rewardCoins = rewardCoins;
            this.material = material;
        }
    }
}
