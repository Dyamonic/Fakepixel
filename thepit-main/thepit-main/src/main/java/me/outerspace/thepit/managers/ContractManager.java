package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.PitGoldFormatter;
import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ContractManager implements Listener {
    public enum Difficulty {
        NOVICE, BIG_TIME
    }

    public enum Objective {
        PLAYER_KILLS, KILLS_ON_GRASS, DIAMOND_SWORD_KILLS, LAVA_KILLS, KILLS_ON_OBSIDIAN,
        KILLS_AFTER_BOW_HIT, BOW_FINAL_KILLS, LONG_RANGE_BOW_KILLS, SNEAKING_KILLS, KILLSTREAK,
        CHAIN_KILLS, GROUND_INGOTS, GOLDEN_APPLES_EATEN, BOUNTIES_CLAIMED, BOW_HITS,
        NO_ARMOR_KILLS, FISHED_DIAMOND_SWORDS, CENTER_FIST_KILLS, PLAYER_DAMAGE, ASSISTS
    }

    public enum ContractPurchaseResult {
        SUCCESS,
        INVALID_CONTRACT_TYPE,
        PROFILE_NOT_LOADED,
        LEVEL_REQUIRED,
        PRESTIGE_REQUIRED,
        INSUFFICIENT_GOLD,
        ACTIVE_CONTRACT_EXISTS,
        PENDING_SELECTION_EXISTS,
        DAILY_LIMIT_REACHED,
        NOT_ENOUGH_OBJECTIVES,
        PURCHASE_IN_PROGRESS,
        STORAGE_FAILURE,
        REFUND_FAILURE
    }

    private final ThePitPlugin plugin;
    private final File file;
    private final Random random = new Random();
    private final Map<Difficulty, List<Template>> templates = new EnumMap<>(Difficulty.class);
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Set<UUID> contractPurchaseProcessing = ConcurrentHashMap.newKeySet();
    private final Set<UUID> selectionLocks = ConcurrentHashMap.newKeySet();
    private final Set<UUID> completionLocks = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Shot> shots = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> bowVictims = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Long>> invalidSneaking = new ConcurrentHashMap<>();
    private final Map<UUID, LavaAttribution> lavaAttribution = new ConcurrentHashMap<>();
    private final Set<String> warnings = ConcurrentHashMap.newKeySet();
    private YamlConfiguration config;
    private BukkitTask updater;
    private BukkitTask saver;

    public ContractManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "contracts.yml");
        if (!file.exists()) plugin.saveResource("contracts.yml", false);
        reload();
    }

    public boolean reload() {
        warnings.clear();
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        Map<Difficulty, List<Template>> parsed = new EnumMap<>(Difficulty.class);
        boolean valid = true;
        long durationSeconds = loaded.getLong("contracts.duration-seconds", 0L);
        if (durationSeconds <= 0L) {
            warn("duration", "Rejected Contract definitions: contracts.duration-seconds must be positive.");
            valid = false;
        }
        for (Difficulty difficulty : Difficulty.values()) {
            List<Template> list = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            ConfigurationSection section = loaded.getConfigurationSection("contracts.objectives." + path(difficulty));
            if (section != null) {
                for (String raw : section.getKeys(false)) {
                    String id = normalize(raw);
                    if (id.isEmpty()) {
                        warn("empty-" + difficulty, "Rejected Contract definition '" + raw + "': normalized ID is empty.");
                        valid = false;
                        continue;
                    }
                    if (!ids.add(id)) {
                        warn("duplicate-" + difficulty + "-" + id,
                                "Rejected Contract definition '" + raw + "': duplicate normalized ID '" + id + "'.");
                        valid = false;
                        continue;
                    }
                    Objective objective;
                    try {
                        objective = Objective.valueOf(raw.toUpperCase(Locale.ENGLISH));
                    } catch (Exception exception) {
                        warn("objective-" + raw, "Rejected Contract definition '" + raw + "': invalid objective type.");
                        valid = false;
                        continue;
                    }
                    String base = "contracts.objectives." + path(difficulty) + "." + raw;
                    List<Integer> targets = loaded.getIntegerList(base + ".targets");
                    if (targets.isEmpty() || targets.stream().anyMatch(value -> value <= 0)) {
                        warn("targets-" + raw, "Rejected Contract definition '" + raw + "': target must be positive.");
                        valid = false;
                        continue;
                    }
                    String difficultyPath = "contracts." + path(difficulty);
                    int rewardMinimum = loaded.getInt(base + ".reward-min",
                            loaded.getInt(difficultyPath + ".reward-min", difficulty == Difficulty.NOVICE ? 400 : 1500));
                    int rewardMaximum = loaded.getInt(base + ".reward-max",
                            loaded.getInt(difficultyPath + ".reward-max", difficulty == Difficulty.NOVICE ? 800 : 3000));
                    int enhancedRewardMaximum = loaded.getInt(base + ".enhanced-reward-max",
                            loaded.getInt(difficultyPath + ".enhanced-reward-max", difficulty == Difficulty.NOVICE ? 1000 : 5000));
                    if (rewardMinimum <= 0 || rewardMaximum < rewardMinimum
                            || enhancedRewardMaximum < rewardMaximum) {
                        warn("rewards-" + difficulty + "-" + raw,
                                "Rejected Contract definition '" + raw + "': reward range is invalid.");
                        valid = false;
                        continue;
                    }
                    list.add(new Template(objective, loaded.getBoolean(base + ".enabled", true),
                            Math.max(1, loaded.getInt(base + ".weight", 1)), List.copyOf(targets),
                            loaded.getBoolean(base + ".requires-center-region", false), rewardMinimum,
                            rewardMaximum, enhancedRewardMaximum));
                }
            }
            long enabledUnique = list.stream().filter(Template::enabled).map(Template::objective).distinct().count();
            if (enabledUnique < 3L) {
                warn("count-" + difficulty, "Rejected Contract category " + difficulty
                        + ": only " + enabledUnique + " valid enabled unique definitions; at least 3 are required.");
                valid = false;
            }
            parsed.put(difficulty, List.copyOf(list));
        }
        if (!valid || parsed.values().stream().anyMatch(List::isEmpty)) return false;
        config = loaded;
        templates.clear();
        templates.putAll(parsed);
        if (updater != null) scheduleTasks();
        return true;
    }

    public void start() {
        scheduleTasks();
        for (Player player : Bukkit.getOnlinePlayers()) load(player);
    }

    public void shutdown() {
        if (updater != null) updater.cancel();
        if (saver != null) saver.cancel();
        updater = null;
        saver = null;
        flushDirty();
        profiles.clear();
        loading.clear();
        dirty.clear();
        contractPurchaseProcessing.clear();
        selectionLocks.clear();
        completionLocks.clear();
        clearSessions();
    }

    private void scheduleTasks() {
        if (updater != null) updater.cancel();
        if (saver != null) saver.cancel();
        updater = Bukkit.getScheduler().runTaskTimer(plugin, this::updateContracts, 20L, 20L);
        long period = Math.max(100L, config.getLong("contracts.save-interval-seconds", 10L) * 20L);
        saver = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirty, period, period);
    }

    public void load(Player player) {
        if (player == null || !loading.add(player.getUniqueId())) return;
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String stored = plugin.getStorageManager().getOverride("contracts", uuid.toString()).orElse("");
            Profile profile = decode(uuid, stored);
            Bukkit.getScheduler().runTask(plugin, () -> {
                loading.remove(uuid);
                if (!player.isOnline()) return;
                profiles.put(uuid, profile);
                resetDaily(profile);
                if (profile.pending != null && !profile.pending.charged) profile.pending = null;
                expire(player, profile, true);
                deliverVile(player, profile);
                dirty.add(uuid);
            });
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clearSession(uuid);
        boolean purchaseInProgress = contractPurchaseProcessing.remove(uuid);
        Profile profile = profiles.remove(uuid);
        if (profile != null && profile.active != null) {
            profile.active.chain = 0;
            profile.active.lastKillAt = 0L;
        }
        dirty.remove(uuid);
        if (profile != null && !purchaseInProgress) {
            String encoded = encode(profile);
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> plugin.getStorageManager().putOverride("contracts", uuid.toString(), encoded));
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        clearSession(event.getPlayer().getUniqueId());
    }

    public void open(Player player) {
        plugin.getQuestManager().open(player);
    }

    public void openDifficulty(Player player, Difficulty difficulty) {
        if (player == null || difficulty == null) return;
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(CC.color("&cYour contract profile is still loading."));
            return;
        }
        resetDaily(profile);
        expire(player, profile, true);
        if (profile.pending != null) {
            openChoices(player, profile);
            return;
        }
        purchase(player, difficulty);
    }

    public void status(Player player) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.active == null) {
            player.sendMessage(CC.color("&7You do not have an active contract."));
            return;
        }
        Active active = profile.active;
        player.sendMessage(CC.color("&eCurrent Contract"));
        player.sendMessage(CC.color("&7Objective: &f" + description(active.objective, active.target)));
        player.sendMessage(CC.color("&7Progress: &e" + active.progress + "&7/&e" + active.target));
        player.sendMessage(CC.color("&7Reward: ") + PitGoldFormatter.formatPitGold(active.reward));
        player.sendMessage(CC.color("&7Remaining: &a" + remaining(active)));
    }

    public void refreshActionBar(Player player) {
        Profile profile = player == null ? null : profiles.get(player.getUniqueId());
        Active active = active(profile);
        if (active != null) sendActionBar(player, active, true);
    }

    public void openAbandon(Player player) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.active == null) {
            player.sendMessage(CC.color("&7You do not have an active contract."));
            return;
        }
        AbandonHolder holder = new AbandonHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, CC.color(config.getString("contracts.abandon-menu.title", "&8Abandon Contract?")));
        holder.inventory = inventory;
        fill(inventory);
        put(inventory, config.getInt("contracts.abandon-menu.confirm-slot", 11),
                item(Material.REDSTONE_BLOCK, "&cAbandon Contract", Arrays.asList("&7No reward or refund will be given.", "", "&cClick to confirm.")));
        put(inventory, config.getInt("contracts.abandon-menu.cancel-slot", 15),
                item(Material.EMERALD_BLOCK, "&aKeep Contract", List.of("&7Return to your active contract.")));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ContractHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.owner().equals(player.getUniqueId())
                || event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;
        if (holder instanceof ChoiceHolder choiceHolder) {
            Integer index = choiceHolder.slots.get(event.getRawSlot());
            if (index != null) select(player, index);
        } else if (holder instanceof AbandonHolder) {
            if (event.getRawSlot() == config.getInt("contracts.abandon-menu.confirm-slot", 11)) abandon(player, false);
            else if (event.getRawSlot() == config.getInt("contracts.abandon-menu.cancel-slot", 15)) open(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ContractHolder) event.setCancelled(true);
    }

    private void purchase(Player player, Difficulty difficulty) {
        if (player == null || difficulty == null) {
            if (player != null) sendContractPurchaseFailure(player, ContractPurchaseResult.INVALID_CONTRACT_TYPE, 0);
            return;
        }
        UUID uuid = player.getUniqueId();
        UUID transactionId = UUID.randomUUID();
        if (!contractPurchaseProcessing.add(uuid)) {
            logContractPreflight(player, difficulty, transactionId, ContractPurchaseResult.PURCHASE_IN_PROGRESS,
                    "duplicate pending purchase request", null);
            sendContractPurchaseFailure(player, ContractPurchaseResult.PURCHASE_IN_PROGRESS, 0);
            return;
        }
        Profile profile = profiles.get(uuid);
        try {
            ContractPurchaseResult validation = validateContractPurchase(player, profile, difficulty);
            if (validation != ContractPurchaseResult.SUCCESS) {
                logContractPreflight(player, difficulty, transactionId, validation,
                        "requirement check returned " + validation, null);
                sendContractPurchaseFailure(player, validation, requiredLevel(difficulty));
                if (validation == ContractPurchaseResult.PENDING_SELECTION_EXISTS && profile != null) openChoices(player, profile);
                contractPurchaseProcessing.remove(uuid);
                return;
            }
            List<Offer> choices = generate(player, difficulty);
            if (choices.size() != 3 || choices.stream().map(Offer::objective).distinct().count() != 3L) {
                logContractPreflight(player, difficulty, transactionId, ContractPurchaseResult.NOT_ENOUGH_OBJECTIVES,
                        "three unique offers could not be generated in memory", null);
                sendContractPurchaseFailure(player, ContractPurchaseResult.NOT_ENOUGH_OBJECTIVES, 0);
                contractPurchaseProcessing.remove(uuid);
                return;
            }
            int originalPrice = Math.max(1, config.getInt("contracts." + path(difficulty) + ".base-price",
                    ContractTypeRegistry.definition(difficulty).baseCost()));
            int price = price(uuid, difficulty);
            int originalBalance = plugin.getPitGoldService().balance(uuid);
            if (originalBalance < price) {
                logContractPreflight(player, difficulty, transactionId, ContractPurchaseResult.INSUFFICIENT_GOLD,
                        "cached Pit Gold is below the discounted price", null);
                sendContractPurchaseFailure(player, ContractPurchaseResult.INSUFFICIENT_GOLD, 0);
                contractPurchaseProcessing.remove(uuid);
                return;
            }
            long createdAt = System.currentTimeMillis();
            Pending pending = new Pending(transactionId, difficulty, price, createdAt,
                    createdAt + Math.max(60L, config.getLong("contracts.pending-offer.expires-seconds", 600L)) * 1000L,
                    true, choices);
            String encoded = encode(profile, pending);
            String playerName = player.getName();
            List<PurchaseRepository.Offer> storedOffers = new ArrayList<>();
            for (int index = 0; index < choices.size(); index++) {
                Offer offer = choices.get(index);
                storedOffers.add(new PurchaseRepository.Offer(index,
                        normalize(offer.difficulty.name() + "-" + offer.objective.name()),
                        offer.objective.name(), offer.target, offer.reward, offer.enhanced, offer.vile));
            }
            PurchaseRepository.Request request = new PurchaseRepository.Request(
                    pending.sessionId, uuid, playerName, true, originalBalance,
                    PurchaseRepository.Type.CONTRACT, difficulty.name(), originalPrice, price,
                    ContractPurchaseResult.SUCCESS.name(), encoded, createdAt, pending.expiresAt,
                    List.copyOf(storedOffers));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                PurchaseRepository.Report stored;
                if (plugin.getStatsManager().awaitPendingPitGoldWrites(uuid, 5000L)) {
                    stored = plugin.getPurchaseRepository().purchase(request);
                } else {
                    stored = new PurchaseRepository.Report(pending.sessionId,
                            PurchaseRepository.Status.PERSISTENCE_FAILURE, -1, originalBalance,
                            0, 0, false, false, "timed out waiting for pending Pit Gold writes", null);
                    plugin.getLogger().severe("[Purchase tx=" + pending.sessionId + " player=" + uuid
                            + " type=CONTRACT id=" + difficulty + "] exactFailure=timed out waiting for pending Pit Gold writes");
                }
                Bukkit.getScheduler().runTask(plugin,
                        () -> finishPurchase(player, profile, pending, request, stored));
            });
        } catch (Throwable throwable) {
            contractPurchaseProcessing.remove(uuid);
            logContractPreflight(player, difficulty, transactionId, ContractPurchaseResult.STORAGE_FAILURE,
                    "exception before persistence", throwable);
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Contract purchase failed before persistence for " + uuid, throwable);
            sendContractPurchaseFailure(player, ContractPurchaseResult.STORAGE_FAILURE, 0);
        }
    }

    private void logContractPreflight(Player player, Difficulty difficulty, UUID transactionId,
                                      ContractPurchaseResult result, String reason, Throwable exception) {
        if (player == null || difficulty == null || plugin.getPurchaseRepository() == null) return;
        UUID uuid = player.getUniqueId();
        int original = Math.max(1, config.getInt("contracts." + path(difficulty) + ".base-price",
                ContractTypeRegistry.definition(difficulty).baseCost()));
        int discounted = price(uuid, difficulty);
        PurchaseRepository.Request request = new PurchaseRepository.Request(
                transactionId, uuid, player.getName(),
                profiles.containsKey(uuid) && plugin.getStatsManager().isProfileLoaded(uuid),
                plugin.getPitGoldService().balance(uuid), PurchaseRepository.Type.CONTRACT,
                difficulty.name(), original, discounted, result.name(), "", 0L, 0L,
                Collections.emptyList());
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getPurchaseRepository().rejected(request, result.name(), reason, exception));
    }

    private ContractPurchaseResult validateContractPurchase(Player player, Profile profile, Difficulty difficulty) {
        if (profile == null || loading.contains(player.getUniqueId())
                || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) return ContractPurchaseResult.PROFILE_NOT_LOADED;
        if (!config.getBoolean("contracts." + path(difficulty) + ".enabled", true)) return ContractPurchaseResult.INVALID_CONTRACT_TYPE;
        resetDaily(profile);
        expire(player, profile, true);
        if (profile.active != null) return ContractPurchaseResult.ACTIVE_CONTRACT_EXISTS;
        if (profile.pending != null) return ContractPurchaseResult.PENDING_SELECTION_EXISTS;
        if (profile.dailyCompleted >= dailyLimit(player.getUniqueId())) return ContractPurchaseResult.DAILY_LIMIT_REACHED;
        if (plugin.getStatsManager().getLevel(player.getUniqueId()) < requiredLevel(difficulty)) return ContractPurchaseResult.LEVEL_REQUIRED;
        int requiredPrestige = Math.max(0, config.getInt("contracts." + path(difficulty) + ".required-prestige", 0));
        if (plugin.getStatsManager().getPrestige(player.getUniqueId()) < requiredPrestige) return ContractPurchaseResult.PRESTIGE_REQUIRED;
        String requiredRenown = normalize(config.getString("contracts." + path(difficulty) + ".required-renown-upgrade", ""));
        if (!requiredRenown.isEmpty() && !plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), requiredRenown)) {
            return ContractPurchaseResult.PRESTIGE_REQUIRED;
        }
        if (plugin.getPurchaseRepository() == null || !plugin.getPurchaseRepository().contractAvailable()) {
            return ContractPurchaseResult.STORAGE_FAILURE;
        }
        if (!plugin.getPitGoldService().has(player.getUniqueId(), price(player.getUniqueId(), difficulty))) {
            return ContractPurchaseResult.INSUFFICIENT_GOLD;
        }
        return ContractPurchaseResult.SUCCESS;
    }

    private void finishPurchase(Player player, Profile profile, Pending pending,
                                PurchaseRepository.Request request, PurchaseRepository.Report stored) {
        UUID uuid = player.getUniqueId();
        try {
            if (stored.success()) {
                if (player.isOnline() && profiles.get(uuid) == profile) {
                    profile.pending = pending;
                    plugin.getPitGoldService().applyPersistedBalance(player, stored.updatedCoins());
                    plugin.getPurchaseRepository().cacheUpdated(request, true, "");
                    player.sendMessage(CC.color("&aContract choices generated!"));
                    player.sendMessage(CC.color("&7Choose one of the three contracts."));
                    openChoices(player, profile);
                } else plugin.getPurchaseRepository().cacheUpdated(request, false,
                        "player offline or Contract profile instance changed after commit");
                return;
            }
            if (player.isOnline()) {
                plugin.getPurchaseRepository().cacheUpdated(request, false, stored.failureReason());
                sendContractPurchaseFailure(player, stored.status() == PurchaseRepository.Status.ROLLBACK_FAILURE
                        ? ContractPurchaseResult.REFUND_FAILURE : ContractPurchaseResult.STORAGE_FAILURE, 0);
            }
        } finally {
            contractPurchaseProcessing.remove(uuid);
        }
    }

    private void sendContractPurchaseFailure(Player player, ContractPurchaseResult result, int requiredLevel) {
        if (result == ContractPurchaseResult.PROFILE_NOT_LOADED) player.sendMessage(CC.color("&cYour contract profile is still loading."));
        else if (result == ContractPurchaseResult.INVALID_CONTRACT_TYPE) player.sendMessage(CC.color("&cThat contract type is unavailable."));
        else if (result == ContractPurchaseResult.LEVEL_REQUIRED) player.sendMessage(CC.color("&cYou must reach level " + requiredLevel + " first."));
        else if (result == ContractPurchaseResult.PRESTIGE_REQUIRED) player.sendMessage(CC.color("&cYou do not meet this contract's Prestige or Renown requirement."));
        else if (result == ContractPurchaseResult.INSUFFICIENT_GOLD) player.sendMessage(CC.color("&cYou do not have enough Gold!"));
        else if (result == ContractPurchaseResult.ACTIVE_CONTRACT_EXISTS) player.sendMessage(CC.color("&cYou already have an active contract!"));
        else if (result == ContractPurchaseResult.PENDING_SELECTION_EXISTS) player.sendMessage(CC.color("&cYour contract choices are already waiting to be selected!"));
        else if (result == ContractPurchaseResult.DAILY_LIMIT_REACHED) player.sendMessage(CC.color("&cYou have reached your contract limit for today!"));
        else if (result == ContractPurchaseResult.NOT_ENOUGH_OBJECTIVES) player.sendMessage(CC.color("&cNo valid contract objectives are currently available!"));
        else if (result == ContractPurchaseResult.PURCHASE_IN_PROGRESS) player.sendMessage(CC.color("&cA contract purchase is already being processed."));
        else if (result == ContractPurchaseResult.REFUND_FAILURE) player.sendMessage(CC.color("&cThe contract purchase failed and the refund could not be verified. Contact an administrator."));
        else player.sendMessage(CC.color("&cThe contract purchase failed. Nothing was charged."));
    }

    private void openChoices(Player player, Profile profile) {
        Pending pending = profile.pending;
        if (pending == null) {
            open(player);
            return;
        }
        ChoiceHolder holder = new ChoiceHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, size("contracts.choice-menu.size", 27),
                CC.color(config.getString("contracts.choice-menu.title", "&8Choose a Contract")));
        holder.inventory = inventory;
        fill(inventory);
        List<Integer> slots = config.getIntegerList("contracts.choice-menu.slots");
        if (slots.size() < pending.offers.size()) slots = Arrays.asList(11, 13, 15);
        for (int index = 0; index < pending.offers.size(); index++) {
            int slot = slots.get(index);
            holder.slots.put(slot, index);
            put(inventory, slot, offerItem(pending.offers.get(index)));
        }
        player.openInventory(inventory);
    }

    private void select(Player player, int index) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.active != null || profile.pending == null || !profile.pending.charged
                || index < 0 || index >= profile.pending.offers.size() || !selectionLocks.add(player.getUniqueId())) return;
        try {
            if (System.currentTimeMillis() >= profile.pending.expiresAt) {
                expire(player, profile, true);
                return;
            }
            Offer offer = profile.pending.offers.get(index);
            long now = System.currentTimeMillis();
            profile.active = new Active(offer.objective, offer.target, 0L, offer.reward, offer.difficulty,
                    offer.enhanced, offer.vile, now, now + Math.max(1L, config.getLong("contracts.duration-seconds", 300L)) * 1000L,
                    0L, 0);
            profile.pending = null;
            clearSession(player.getUniqueId());
            dirty.add(player.getUniqueId());
            String encoded = encode(profile);
            UUID uuid = player.getUniqueId();
            Active selected = profile.active;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean saved = plugin.getPurchaseRepository().selectContract(uuid,
                        normalize(offer.difficulty.name() + "-" + offer.objective.name()),
                        offer.difficulty.name(), offer.objective.name(), offer.target, 0L, offer.reward,
                        selected.startedAt, selected.expiresAt, encoded);
                if (saved) dirty.remove(uuid);
            });
            player.closeInventory();
            status(player);
            sendActionBar(player, profile.active, true);
        } finally {
            selectionLocks.remove(player.getUniqueId());
        }
    }

    public void abandon(Player player, boolean administrative) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.active == null || !completionLocks.add(player.getUniqueId())) return;
        try {
            profile.active = null;
            if (plugin.getActionBarManager() != null) plugin.getActionBarManager().clearNormal(player.getUniqueId());
            if (!administrative) {
                plugin.getStatsManager().addLongStat(player.getUniqueId(), "contracts_abandoned", 1L);
                if (config.getBoolean("contracts.statistics.abandoned-contracts-consume-daily", false)) profile.dailyCompleted++;
                message(player, "abandoned", "&cYou abandoned your active contract.");
            }
            clearSession(player.getUniqueId());
            dirty.add(player.getUniqueId());
            persistImmediate(player.getUniqueId());
            player.closeInventory();
        } finally {
            completionLocks.remove(player.getUniqueId());
        }
    }

    public void onKill(Player killer, Player victim, int bounty, int streak) {
        if (!valid(killer) || victim == null || !plugin.getPerkManager().validCombat(killer, victim)) return;
        Profile profile = profiles.get(killer.getUniqueId());
        Active active = active(profile);
        if (active == null) return;
        EntityDamageByEntityEvent finalHit = victim.getLastDamageCause() instanceof EntityDamageByEntityEvent damage ? damage : null;
        boolean bow = finalHit != null && finalHit.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter
                && shooter.getUniqueId().equals(killer.getUniqueId());
        boolean melee = finalHit != null && finalHit.getDamager() instanceof Player attacker && attacker.getUniqueId().equals(killer.getUniqueId());
        Material beneath = killer.getLocation().clone().subtract(0.0D, 1.0D, 0.0D).getBlock().getType();
        boolean diamondSword = plugin.getShopManager().isItem(killer.getItemInHand(), "diamond-sword");
        boolean lava = OptionalLava.owner(lavaAttribution.get(victim.getUniqueId()), killer.getUniqueId());
        boolean priorBow = bowVictims.getOrDefault(killer.getUniqueId(), Collections.emptySet()).contains(victim.getUniqueId());
        Shot finalShot = bow ? shots.get(((Arrow) finalHit.getDamager()).getUniqueId()) : null;
        boolean sneaking = invalidSneaking.getOrDefault(killer.getUniqueId(), Collections.emptyMap())
                .getOrDefault(victim.getUniqueId(), 0L) < System.currentTimeMillis()
                && (bow ? finalShot != null && finalShot.sneaking : killer.isSneaking());
        boolean noArmor = noArmor(killer.getInventory());
        boolean fist = killer.getItemInHand() == null || killer.getItemInHand().getType() == Material.AIR;
        boolean center = inCenter(killer.getLocation()) && inCenter(victim.getLocation());
        double distance = finalShot != null && finalShot.origin.getWorld().equals(victim.getWorld())
                ? finalShot.origin.distance(victim.getLocation()) : 0.0D;
        boolean qualifies = switch (active.objective) {
            case PLAYER_KILLS -> true;
            case KILLS_ON_GRASS -> beneath == Material.GRASS;
            case DIAMOND_SWORD_KILLS -> melee && diamondSword;
            case LAVA_KILLS -> lava;
            case KILLS_ON_OBSIDIAN -> beneath == Material.OBSIDIAN;
            case KILLS_AFTER_BOW_HIT -> priorBow;
            case BOW_FINAL_KILLS -> bow;
            case LONG_RANGE_BOW_KILLS -> bow && distance >= config.getDouble("contracts.long-range-bow-distance", 30.0D);
            case SNEAKING_KILLS -> sneaking;
            case NO_ARMOR_KILLS -> noArmor;
            case CENTER_FIST_KILLS -> melee && fist && center;
            case BOUNTIES_CLAIMED -> bounty > 0;
            default -> false;
        };
        if (active.objective == Objective.KILLSTREAK) setProgress(killer, profile, streak);
        else if (active.objective == Objective.CHAIN_KILLS) {
            long now = System.currentTimeMillis();
            active.chain = now - active.lastKillAt <= Math.max(1L, config.getLong("contracts.chain-window-seconds", 5L)) * 1000L
                    ? active.chain + 1 : 1;
            active.lastKillAt = now;
            setExactProgress(killer, profile, active.chain);
        } else if (qualifies) addProgress(killer, profile, 1L);
        clearVictim(victim.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getProjectile() instanceof Arrow arrow) || !valid(player)) return;
        shots.put(arrow.getUniqueId(), new Shot(player.getUniqueId(), player.getLocation().clone(), player.isSneaking(), System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        boolean bow = false;
        boolean shotSneaking = false;
        if (event.getDamager() instanceof Player player) attacker = player;
        else if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player player) {
            attacker = player;
            bow = true;
            Shot shot = shots.get(arrow.getUniqueId());
            shotSneaking = shot != null && shot.sneaking;
        }
        if (!plugin.getPerkManager().validCombat(attacker, victim)
                || attacker.hasMetadata("BuildFFATestDamage") || victim.hasMetadata("BuildFFATestDamage")) return;
        Profile profile = profiles.get(attacker.getUniqueId());
        Active active = active(profile);
        if (active == null) return;
        if (bow) bowVictims.computeIfAbsent(attacker.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(victim.getUniqueId());
        if (active.objective == Objective.BOW_HITS && bow) addProgress(attacker, profile, 1L);
        if (active.objective == Objective.PLAYER_DAMAGE) addProgress(attacker, profile, Math.max(1L, Math.round(event.getFinalDamage())));
        if (active.objective == Objective.SNEAKING_KILLS && !(bow ? shotSneaking : attacker.isSneaking())) {
            invalidSneaking.computeIfAbsent(attacker.getUniqueId(), ignored -> new ConcurrentHashMap<>()).put(victim.getUniqueId(),
                    System.currentTimeMillis() + Math.max(5L, config.getLong("contracts.sneaking-attempt-timeout-seconds", 30L)) * 1000L);
        }
    }

    public void onAssist(Player player) {
        progress(player, Objective.ASSISTS, 1L);
    }

    public void onGroundIngot(Player player) {
        progress(player, Objective.GROUND_INGOTS, 1L);
    }

    public void onGoldenApple(Player player) {
        progress(player, Objective.GOLDEN_APPLES_EATEN, 1L);
    }

    public void onFishingReward(Player player, String rewardId, Material material) {
        if (material == Material.DIAMOND_SWORD || "diamond-sword".equalsIgnoreCase(rewardId)) {
            progress(player, Objective.FISHED_DIAMOND_SWORDS, 1L);
        }
    }

    public void recordLavaAttribution(Player victim, Player owner, Location source) {
        if (victim == null || owner == null || source == null) return;
        long expires = System.currentTimeMillis() + Math.max(5L, config.getLong("contracts.lava-attribution-seconds", 30L)) * 1000L;
        lavaAttribution.put(victim.getUniqueId(), new LavaAttribution(owner.getUniqueId(), blockKey(source), expires));
    }

    public void clearLavaSource(Location source) {
        if (source == null) return;
        String key = blockKey(source);
        lavaAttribution.entrySet().removeIf(entry -> entry.getValue().source.equals(key));
    }

    public void clearLavaOwner(UUID owner) {
        if (owner != null) lavaAttribution.entrySet().removeIf(entry -> entry.getValue().owner.equals(owner));
    }

    public void clearSessions() {
        shots.clear();
        bowVictims.clear();
        invalidSneaking.clear();
        lavaAttribution.clear();
    }

    private void clearSession(UUID uuid) {
        Profile profile = profiles.get(uuid);
        if (profile != null && profile.active != null) {
            profile.active.chain = 0;
            profile.active.lastKillAt = 0L;
        }
        bowVictims.remove(uuid);
        invalidSneaking.remove(uuid);
        shots.entrySet().removeIf(entry -> entry.getValue().shooter.equals(uuid));
        lavaAttribution.entrySet().removeIf(entry -> entry.getKey().equals(uuid) || entry.getValue().owner.equals(uuid));
    }

    private void clearVictim(UUID victim) {
        lavaAttribution.remove(victim);
        for (Set<UUID> values : bowVictims.values()) values.remove(victim);
        for (Map<UUID, Long> values : invalidSneaking.values()) values.remove(victim);
    }

    private void progress(Player player, Objective objective, long amount) {
        if (!valid(player)) return;
        Profile profile = profiles.get(player.getUniqueId());
        Active active = active(profile);
        if (active != null && active.objective == objective) addProgress(player, profile, amount);
    }

    private void addProgress(Player player, Profile profile, long amount) {
        Active active = active(profile);
        if (active == null || amount <= 0L) return;
        setProgress(player, profile, Math.min(active.target, active.progress + amount));
    }

    private void setProgress(Player player, Profile profile, long value) {
        Active active = active(profile);
        if (active == null) return;
        active.progress = Math.max(active.progress, Math.min(active.target, value));
        dirty.add(player.getUniqueId());
        sendActionBar(player, active, true);
        if (active.progress >= active.target) complete(player, false);
    }

    private void setExactProgress(Player player, Profile profile, long value) {
        Active active = active(profile);
        if (active == null) return;
        active.progress = Math.max(0L, Math.min(active.target, value));
        dirty.add(player.getUniqueId());
        sendActionBar(player, active, true);
        if (active.progress >= active.target) complete(player, false);
    }

    public void resetLife(Player player) {
        if (player == null) return;
        Profile profile = profiles.get(player.getUniqueId());
        if (profile != null && profile.active != null && profile.active.objective == Objective.CHAIN_KILLS) {
            profile.active.progress = 0L;
            profile.active.chain = 0;
            profile.active.lastKillAt = 0L;
            dirty.add(player.getUniqueId());
        }
        clearSession(player.getUniqueId());
    }

    public boolean complete(Player player, boolean administrative) {
        Profile profile = profiles.get(player.getUniqueId());
        Active active = active(profile);
        if (active == null || !completionLocks.add(player.getUniqueId())) return false;
        try {
            if (!administrative && active.progress < active.target) return false;
            if (System.currentTimeMillis() >= active.expiresAt) {
                fail(player, profile, false);
                return false;
            }
            long duration = Math.max(0L, System.currentTimeMillis() - active.startedAt);
            int reward = active.reward;
            int vile = active.vile ? vileAmount(player.getUniqueId()) : 0;
            profile.active = null;
            if (plugin.getActionBarManager() != null) plugin.getActionBarManager().clearNormal(player.getUniqueId());
            profile.dailyCompleted++;
            if (administrative) plugin.getPitGoldService().add(player, reward);
            else plugin.getPitGoldService().award(player, reward, ThePitPlugin.GoldSource.QUEST);
            if (vile > 0) grantVile(player, profile, vile);
            plugin.getStatsManager().addLongStat(player.getUniqueId(), StatsManager.CONTRACTS_COMPLETED, 1L);
            plugin.getStatsManager().addLongStat(player.getUniqueId(), active.difficulty == Difficulty.NOVICE
                    ? "novice_contracts_completed" : "big_time_contracts_completed", 1L);
            plugin.getStatsManager().addLongStat(player.getUniqueId(), "contract_completion_duration_millis", duration);
            if (vile > 0) plugin.getStatsManager().addLongStat(player.getUniqueId(), "contract_vile_earned", vile);
            dirty.add(player.getUniqueId());
            persistImmediate(player.getUniqueId());
            clearSession(player.getUniqueId());
            if (!administrative || config.getBoolean("contracts.admin-completion-awards-achievements", false)) {
                plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(player.getUniqueId(),
                        AchievementManager.EventType.CONTRACT_COMPLETED, 1L, Map.of("duration_millis", duration), false));
            }
            if (!administrative && plugin.getQuestManager() != null) plugin.getQuestManager().handleContractCompleted(player);
            String objective = description(active.objective, active.target);
            player.sendMessage(CC.color("&a&lCONTRACT COMPLETE!"));
            player.sendMessage(CC.color("&7You completed &f" + objective + "&7."));
            player.sendMessage(CC.color("&7Reward: &6" + format(reward) + " Gold"));
            if (vile > 0) player.sendMessage(CC.color("&5Special reward: &d" + vile + " Chunk" + (vile == 1 ? "" : "s") + " of Vile"));
            player.closeInventory();
            return true;
        } finally {
            completionLocks.remove(player.getUniqueId());
        }
    }

    private void fail(Player player, Profile profile, boolean notify) {
        if (profile.active == null) return;
        profile.active = null;
        if (plugin.getActionBarManager() != null) plugin.getActionBarManager().clearNormal(player.getUniqueId());
        plugin.getStatsManager().addLongStat(player.getUniqueId(), "contracts_failed", 1L);
        if (config.getBoolean("contracts.statistics.failed-contracts-consume-daily", false)) profile.dailyCompleted++;
        clearSession(player.getUniqueId());
        dirty.add(player.getUniqueId());
        persistImmediate(player.getUniqueId());
        if (notify) message(player, "failed", "&c&lCONTRACT FAILED! &7You ran out of time.");
    }

    public void clearForPrestige(UUID uuid) {
        Profile profile = profiles.get(uuid);
        if (profile == null) return;
        profile.active = null;
        if (plugin.getActionBarManager() != null) plugin.getActionBarManager().clearNormal(uuid);
        profile.pending = null;
        clearSession(uuid);
        dirty.add(uuid);
        persistImmediate(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return profiles.containsKey(uuid);
    }

    public int validDefinitionCount(Difficulty difficulty) {
        return (int) templates.getOrDefault(difficulty, Collections.emptyList()).stream()
                .filter(Template::enabled).map(Template::objective).distinct().count();
    }

    public boolean definitionsLoaded() {
        return config != null && validDefinitionCount(Difficulty.NOVICE) >= 3
                && validDefinitionCount(Difficulty.BIG_TIME) >= 3;
    }

    public boolean purchasePending(UUID uuid) {
        return contractPurchaseProcessing.contains(uuid);
    }

    public boolean resetDailyAdmin(Player player) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null) return false;
        profile.dailyKey = dailyKey();
        profile.dailyCompleted = 0;
        dirty.add(player.getUniqueId());
        return true;
    }

    public boolean cancelAdmin(Player player) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null) return false;
        profile.active = null;
        if (plugin.getActionBarManager() != null) plugin.getActionBarManager().clearNormal(player.getUniqueId());
        profile.pending = null;
        clearSession(player.getUniqueId());
        dirty.add(player.getUniqueId());
        return true;
    }

    public boolean setProgressAdmin(Player player, long amount) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null || profile.active == null || amount < 0L) return false;
        profile.active.progress = Math.min(profile.active.target, amount);
        dirty.add(player.getUniqueId());
        sendActionBar(player, profile.active, true);
        return true;
    }

    public void debug(CommandSender sender, Player player) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null) {
            sender.sendMessage(CC.color("&cContract profile is not loaded."));
            return;
        }
        resetDaily(profile);
        sender.sendMessage(CC.color("&eContracts for &f" + player.getName()));
        sender.sendMessage(CC.color("&7Daily: &e" + profile.dailyCompleted + "&7/&e" + dailyLimit(player.getUniqueId())
                + " &7Key: &f" + profile.dailyKey));
        sender.sendMessage(CC.color("&7Contractor: &e" + upgradeTier(player.getUniqueId(), "contractor")
                + " &7Scam Artist: &e" + upgradeTier(player.getUniqueId(), "scam-artist")));
        sender.sendMessage(CC.color("&7Pending: &f" + (profile.pending == null ? "none" : profile.pending.difficulty + " choices=" + profile.pending.offers.size())
                + " &7Active: &f" + (profile.active == null ? "none" : profile.active.objective + " " + profile.active.progress + "/" + profile.active.target)));
        sender.sendMessage(CC.color("&7Pending Vile delivery: &d" + profile.pendingVile + " &7Dirty: &f" + dirty.contains(player.getUniqueId())));
    }

    private void updateContracts() {
        long now = System.currentTimeMillis();
        int interval = Math.max(1, config.getInt("contracts.action-bar-update-seconds", 5));
        for (Player player : Bukkit.getOnlinePlayers()) {
            Profile profile = profiles.get(player.getUniqueId());
            if (profile == null) continue;
            expire(player, profile, true);
            Active active = profile.active;
            if (active != null && (now / 1000L) % interval == 0L) sendActionBar(player, active, false);
            deliverVile(player, profile);
        }
        shots.entrySet().removeIf(entry -> now - entry.getValue().createdAt > 60000L);
        lavaAttribution.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
    }

    private void expire(Player player, Profile profile, boolean notify) {
        long now = System.currentTimeMillis();
        if (profile.active != null && profile.active.expiresAt <= now) fail(player, profile, notify);
        Pending pending = profile.pending;
        if (pending != null && pending.expiresAt <= now) {
            profile.pending = null;
            if (pending.charged && config.getBoolean("contracts.pending-offer.refund-on-expire", true)) {
                UUID uuid = player.getUniqueId();
                contractPurchaseProcessing.add(uuid);
                String encoded = encode(profile);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    int balance = plugin.getPurchaseRepository()
                            .refundExpiredContract(uuid, pending.paidPrice, encoded);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        contractPurchaseProcessing.remove(uuid);
                        if (balance >= 0) {
                            plugin.getPitGoldService().applyPersistedBalance(player, balance);
                            dirty.remove(uuid);
                            message(player, "refunded", "&aYour expired contract offer was refunded %amount%&a.",
                                    "%amount%", PitGoldFormatter.formatPitGold(pending.paidPrice));
                        } else {
                            profile.pending = pending;
                            dirty.add(uuid);
                            player.sendMessage(CC.color("&cThe expired Contract refund could not be committed. "
                                    + "Your paid offer remains available; contact an administrator."));
                        }
                    });
                });
                return;
            }
            dirty.add(player.getUniqueId());
            UUID uuid = player.getUniqueId();
            String encoded = encode(profile);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (plugin.getPurchaseRepository().discardPendingContract(uuid, encoded)) dirty.remove(uuid);
            });
        }
    }

    private Active active(Profile profile) {
        if (profile == null || profile.active == null) return null;
        if (profile.active.startedAt > System.currentTimeMillis() || profile.active.expiresAt <= System.currentTimeMillis()) return null;
        return profile.active;
    }

    private boolean valid(Player player) {
        return player != null && profiles.containsKey(player.getUniqueId()) && plugin.getStatsManager().isProfileLoaded(player.getUniqueId())
                && plugin.getArenaManager().isInCurrentPvpArena(player.getLocation())
                && !plugin.getArenaManager().isInCurrentSpawnProtection(player.getLocation());
    }

    private List<Offer> generate(Player player, Difficulty difficulty) {
        List<Template> available = new ArrayList<>();
        for (Template template : templates.getOrDefault(difficulty, Collections.emptyList())) {
            if (!template.enabled || template.requiresCenter && !hasCenterRegion()) continue;
            available.add(template);
        }
        int count = 3;
        if (available.stream().map(template -> template.objective).distinct().count() < count) return Collections.emptyList();
        List<Offer> output = new ArrayList<>();
        Set<Objective> used = new HashSet<>();
        while (output.size() < count) {
            Template template = weighted(available);
            if (!used.add(template.objective)) continue;
            int target = template.targets.get(random.nextInt(template.targets.size()));
            boolean enhanced = random.nextDouble() < Math.max(0.0D, Math.min(1.0D, config.getDouble("contracts.enhanced.chance", 0.05D)));
            if (enhanced) target = Math.max(target + 1, (int) Math.ceil(target * Math.max(1.0D, config.getDouble("contracts.enhanced.target-multiplier", 1.5D))));
            int reward = randomReward(template, enhanced);
            boolean vile = config.getBoolean("contracts.chunks-of-vile.enabled", true)
                    && difficulty.name().equalsIgnoreCase(config.getString("contracts.chunks-of-vile.required-difficulty", "BIG_TIME"))
                    && plugin.getRenownShopManager().tier(player.getUniqueId(),
                    config.getString("contracts.chunks-of-vile.required-renown-upgrade", "heresy")) >= 1
                    && random.nextDouble() < vileChance(player.getUniqueId());
            output.add(new Offer(difficulty, template.objective, target, reward, enhanced, vile));
        }
        return output;
    }

    private Template weighted(List<Template> values) {
        int total = values.stream().mapToInt(template -> template.weight).sum();
        int roll = random.nextInt(Math.max(1, total));
        for (Template value : values) {
            roll -= value.weight;
            if (roll < 0) return value;
        }
        return values.get(values.size() - 1);
    }

    private int randomReward(Template template, boolean enhanced) {
        int maximum = enhanced ? template.enhancedRewardMaximum : template.rewardMaximum;
        return template.rewardMinimum + random.nextInt(maximum - template.rewardMinimum + 1);
    }

    public int price(UUID uuid, Difficulty difficulty) {
        int base = Math.max(1, config.getInt("contracts." + path(difficulty) + ".base-price", ContractTypeRegistry.definition(difficulty).baseCost()));
        int tier = upgradeTier(uuid, "scam-artist");
        double discount = Math.max(0.0D, plugin.getRenownShopManager().effect("scam-artist", "discount-per-tier", 0.05D)) * tier;
        return Math.max(1, (int) Math.floor(base * Math.max(0.0D, 1.0D - discount)));
    }

    public int dailyLimit(UUID uuid) {
        return Math.max(1, config.getInt("contracts.daily-reset.base-limit", 3))
                + upgradeTier(uuid, "contractor") * plugin.getRenownShopManager().effect("contractor", "contracts-per-tier", 1);
    }

    public int completedToday(UUID uuid) {
        Profile profile = profiles.get(uuid);
        if (profile == null) return 0;
        resetDaily(profile);
        return Math.max(0, profile.dailyCompleted);
    }

    public int remainingToday(UUID uuid) {
        return Math.max(0, dailyLimit(uuid) - completedToday(uuid));
    }

    public int upgradeTier(UUID uuid, String id) {
        int tier = 0;
        for (int value = 1; value <= 5; value++) if (plugin.getStatsManager().hasRenownUpgrade(uuid, normalize(id) + "-" + value)) tier = value;
        return tier;
    }

    public Set<String> renownUpgradeIds() {
        Set<String> ids = new LinkedHashSet<>();
        if (config.getBoolean("contracts.renown.contractor.enabled", true)) for (int tier = 1; tier <= 5; tier++) ids.add("contractor-" + tier);
        if (config.getBoolean("contracts.renown.scam-artist.enabled", true)) for (int tier = 1; tier <= 5; tier++) ids.add("scam-artist-" + tier);
        if (config.getBoolean("contracts.renown.heresy.enabled", true)) ids.add("heresy");
        if (config.getBoolean("contracts.renown.do-your-own-carpentry.enabled", true)) ids.add("do-your-own-carpentry");
        return ids;
    }

    public boolean purchaseTier(Player player, String id) {
        String normalized = normalize(id);
        if (normalized.equals("contractor") || normalized.equals("scam-artist")) {
            int current = upgradeTier(player.getUniqueId(), normalized);
            if (current >= 5) return false;
            int next = current + 1;
            int prestige = config.getInt("contracts.renown." + normalized + ".required-prestige", normalized.equals("contractor") ? 2 : 1);
            List<Integer> prices = config.getIntegerList("contracts.renown." + normalized + ".prices");
            long price = prices.size() >= next ? prices.get(next - 1) : 5L;
            return plugin.getStatsManager().purchaseRenownUpgrade(player, normalized + "-" + next, prestige, price);
        }
        if (!normalized.equals("heresy") && !normalized.equals("do-your-own-carpentry")) return false;
        int prestige = config.getInt("contracts.renown." + normalized + ".required-prestige", normalized.equals("heresy") ? 6 : 8);
        long price = config.getLong("contracts.renown." + normalized + ".price", normalized.equals("heresy") ? 10L : 5L);
        return plugin.getStatsManager().purchaseRenownUpgrade(player, normalized, prestige, price);
    }

    public ItemStack renownItem(Player player, String id) {
        String normalized = normalize(id);
        if (normalized.equals("contractor") || normalized.equals("scam-artist")) {
            int current = upgradeTier(player.getUniqueId(), normalized);
            int next = Math.min(5, current + 1);
            List<Integer> prices = config.getIntegerList("contracts.renown." + normalized + ".prices");
            int price = prices.size() >= next ? prices.get(next - 1) : 5;
            int prestige = config.getInt("contracts.renown." + normalized + ".required-prestige", normalized.equals("contractor") ? 2 : 1);
            List<String> lore = new ArrayList<>();
            lore.add(normalized.equals("contractor") ? "&7Raises the daily contract limit." : "&7Reduces contract prices by 5% per tier.");
            lore.add("");
            lore.add("&7Tier: &e" + roman(current));
            lore.add("&7Next cost: &e" + price + " Renown");
            lore.add("&7Requires: &dPrestige " + prestige);
            lore.add("");
            lore.add(current >= 5 ? "&aMaximum tier reached!" : "&eClick to purchase!");
            return item(normalized.equals("contractor") ? Material.PAPER : Material.GOLD_INGOT,
                    normalized.equals("contractor") ? "&aContractor" : "&6Scam Artist", lore);
        }
        boolean owned = plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), normalized);
        int price = config.getInt("contracts.renown." + normalized + ".price", normalized.equals("heresy") ? 10 : 5);
        int prestige = config.getInt("contracts.renown." + normalized + ".required-prestige", normalized.equals("heresy") ? 6 : 8);
        return item(normalized.equals("heresy") ? Material.NETHER_BRICK_ITEM : Material.WOOD,
                normalized.equals("heresy") ? "&5Heresy" : "&6Do Your Own Carpentry",
                Arrays.asList("&7Enables improved Big Time Vile rewards.", "", "&7Cost: &e" + price + " Renown",
                        "&7Requires: &dPrestige " + prestige, "", owned ? "&aPurchased permanently!" : "&eClick to purchase!"));
    }

    public int renownSlot(String id) {
        return config.getInt("contracts.renown." + normalize(id) + ".inventory-slot", -1);
    }

    public int requiredLevel(Difficulty difficulty) {
        return config.getInt("contracts." + path(difficulty) + ".required-level", ContractTypeRegistry.definition(difficulty).minimumLevel());
    }

    private double vileChance(UUID uuid) {
        double chance = config.getDouble("contracts.chunks-of-vile.base-chance", 0.10D);
        if (plugin.getStatsManager().hasRenownUpgrade(uuid, "do-your-own-carpentry")) {
            chance += config.getDouble("contracts.renown.do-your-own-carpentry.vile-chance-bonus", 0.10D);
        }
        return Math.max(0.0D, Math.min(1.0D, chance));
    }

    private int vileAmount(UUID uuid) {
        if (plugin.getStatsManager().hasRenownUpgrade(uuid, "do-your-own-carpentry")) {
            return Math.max(2, config.getInt("contracts.renown.do-your-own-carpentry.vile-amount", 2));
        }
        return Math.max(1, config.getInt("contracts.chunks-of-vile.amount", 1));
    }

    private void grantVile(Player player, Profile profile, int amount) {
        ItemStack vile = SecureItemTag.string(new ItemStack(Material.COAL, amount, (short) 0), "ThePitItem", "chunk_of_vile");
        ItemMeta meta = vile.getItemMeta();
        meta.setDisplayName(CC.color("&5Chunk of Vile"));
        meta.setLore(List.of(CC.color("&7A foul Mystic crafting material.")));
        vile.setItemMeta(meta);
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(vile);
        if (!remaining.isEmpty() && plugin.getPitCommandManager() != null) {
            for (ItemStack item : remaining.values()) plugin.getPitCommandManager().addToStash(player.getUniqueId(), item);
        } else if (!remaining.isEmpty()) profile.pendingVile += remaining.values().stream().mapToInt(ItemStack::getAmount).sum();
    }

    private void deliverVile(Player player, Profile profile) {
        if (profile.pendingVile <= 0) return;
        int amount = Math.min(64, profile.pendingVile);
        ItemStack vile = SecureItemTag.string(new ItemStack(Material.COAL, amount), "ThePitItem", "chunk_of_vile");
        ItemMeta meta = vile.getItemMeta();
        meta.setDisplayName(CC.color("&5Chunk of Vile"));
        vile.setItemMeta(meta);
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(vile);
        int delivered = amount - remaining.values().stream().mapToInt(ItemStack::getAmount).sum();
        if (delivered > 0) {
            profile.pendingVile -= delivered;
            dirty.add(player.getUniqueId());
        }
    }

    private ItemStack offerItem(Offer offer) {
        List<String> lore = new ArrayList<>();
        lore.add("&7" + description(offer.objective, offer.target));
        lore.add("");
        lore.add("&7Reward: " + PitGoldFormatter.formatPitGold(offer.reward));
        if (offer.vile) lore.add("&7Possible reward: &5Chunk of Vile");
        lore.add("&7Time limit: &a5 minutes");
        lore.add("&7Difficulty: " + (offer.difficulty == Difficulty.NOVICE ? "&aNovice" : "&cBig Time"));
        if (offer.enhanced) lore.add(config.getString("contracts.enhanced.name", "&d&lENHANCED!"));
        lore.add("");
        lore.add("&eClick to select!");
        return item(offer.enhanced ? Material.NETHER_STAR : offer.difficulty == Difficulty.NOVICE ? Material.PAPER : Material.ENCHANTED_BOOK,
                offer.difficulty == Difficulty.NOVICE ? "&aNovice Contract" : "&cBig Time Contract", lore);
    }

    private String description(Objective objective, long target) {
        return switch (objective) {
            case PLAYER_KILLS -> "Kill &e" + target + " players&7.";
            case KILLS_ON_GRASS -> "Kill &e" + target + " players &7while standing on grass.";
            case DIAMOND_SWORD_KILLS -> "Kill &e" + target + " players &7with a Diamond Sword.";
            case LAVA_KILLS -> "Kill &e" + target + " players &7using your lava.";
            case KILLS_ON_OBSIDIAN -> "Kill &e" + target + " players &7while standing on obsidian.";
            case KILLS_AFTER_BOW_HIT -> "Kill &e" + target + " players &7after shooting them.";
            case BOW_FINAL_KILLS -> "Kill &e" + target + " players &7with a bow final hit.";
            case LONG_RANGE_BOW_KILLS -> "Earn &e" + target + " long-range bow kills&7.";
            case SNEAKING_KILLS -> "Earn &e" + target + " all-sneaking kills&7.";
            case KILLSTREAK -> "Reach a &e" + target + " killstreak&7.";
            case CHAIN_KILLS -> "Chain &e" + target + " kills&7.";
            case GROUND_INGOTS -> "Collect &e" + target + " world Gold ingots&7.";
            case GOLDEN_APPLES_EATEN -> "Eat &e" + target + " Golden Apples&7.";
            case BOUNTIES_CLAIMED -> "Claim &e" + target + " bounties&7.";
            case BOW_HITS -> "Land &e" + target + " bow hits&7.";
            case NO_ARMOR_KILLS -> "Kill &e" + target + " players &7without armor.";
            case FISHED_DIAMOND_SWORDS -> "Fish &e" + target + " Diamond Sword&7.";
            case CENTER_FIST_KILLS -> "Earn &e" + target + " center fist kills&7.";
            case PLAYER_DAMAGE -> null;
            case ASSISTS -> null;
        };
    }

    private boolean noArmor(PlayerInventory inventory) {
        for (ItemStack armor : inventory.getArmorContents()) if (armor != null && armor.getType() != Material.AIR) return false;
        return true;
    }

    private boolean hasCenterRegion() {
        String arena = plugin.getArenaManager().getCurrentArena();
        return arena != null && config.isConfigurationSection("contracts.center-regions." + arena);
    }

    private boolean inCenter(Location location) {
        String arena = plugin.getArenaManager().getCurrentArena();
        if (arena == null || location == null) return false;
        String base = "contracts.center-regions." + arena;
        if (!config.isConfigurationSection(base)) return false;
        String world = config.getString(base + ".world", location.getWorld().getName());
        if (!location.getWorld().getName().equals(world)) return false;
        double x1 = config.getDouble(base + ".pos1.x");
        double y1 = config.getDouble(base + ".pos1.y");
        double z1 = config.getDouble(base + ".pos1.z");
        double x2 = config.getDouble(base + ".pos2.x");
        double y2 = config.getDouble(base + ".pos2.y");
        double z2 = config.getDouble(base + ".pos2.z");
        return location.getX() >= Math.min(x1, x2) && location.getX() <= Math.max(x1, x2)
                && location.getY() >= Math.min(y1, y2) && location.getY() <= Math.max(y1, y2)
                && location.getZ() >= Math.min(z1, z2) && location.getZ() <= Math.max(z1, z2);
    }

    private void resetDaily(Profile profile) {
        String key = dailyKey();
        if (key.equals(profile.dailyKey)) return;
        profile.dailyKey = key;
        profile.dailyCompleted = 0;
        dirty.add(profile.uuid);
    }

    private String dailyKey() {
        int hour = Math.max(0, Math.min(23, config.getInt("contracts.daily-reset.hour-utc", 4)));
        return Instant.now().minus(hour, ChronoUnit.HOURS).atZone(ZoneOffset.UTC).toLocalDate().toString();
    }

    private void sendActionBar(Player player, Active active, boolean force) {
        if (player == null || active == null || !player.isOnline()) return;
        long now = System.currentTimeMillis();
        if (!force && now - active.lastActionBar < Math.max(1, config.getInt("contracts.action-bar-update-seconds", 5)) * 1000L) return;
        active.lastActionBar = now;
        String message = "&bContract: &f" + description(active.objective, active.target) + " &7(" + active.progress + "/" + active.target + ") &8- &e" + remaining(active);
        if (plugin.getActionBarManager() != null) plugin.getActionBarManager().publishNormal(player, message);
    }

    private String remaining(Active active) {
        long seconds = Math.max(0L, (active.expiresAt - System.currentTimeMillis() + 999L) / 1000L);
        return String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    private void fill(Inventory inventory) {
        Material material = Material.matchMaterial(config.getString("contracts.menu.filler.material", "STAINED_GLASS_PANE"));
        if (material == null) material = Material.STAINED_GLASS_PANE;
        ItemStack filler = new ItemStack(material, 1, (short) config.getInt("contracts.menu.filler.data", 15));
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(CC.color(config.getString("contracts.menu.filler.name", " ")));
        filler.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private void put(Inventory inventory, int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) inventory.setItem(slot, item);
    }

    private int size(String path, int fallback) {
        int value = Math.max(9, Math.min(54, config.getInt(path, fallback)));
        return value - value % 9;
    }

    private void message(Player player, String id, String fallback, String... replacements) {
        String value = config.getString("contracts.messages." + id, fallback);
        for (int index = 0; index + 1 < replacements.length; index += 2) value = value.replace(replacements[index], replacements[index + 1]);
        player.sendMessage(CC.color(value));
    }

    private String path(Difficulty difficulty) {
        return difficulty == Difficulty.NOVICE ? "novice" : "big-time";
    }

    private String format(long amount) {
        return NumberFormat.getIntegerInstance(Locale.US).format(amount);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH).replace('_', '-').replace(' ', '-');
    }

    private String roman(int value) {
        return switch (value) {
            case 0 -> "0";
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }

    private String blockKey(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private void warn(String key, String message) {
        if (warnings.add(key)) plugin.getLogger().warning(message);
    }

    private void flushDirty() {
        for (UUID uuid : new ArrayList<>(dirty)) {
            Profile profile = profiles.get(uuid);
            if (profile != null && persistProfile(uuid, snapshot(profile))) dirty.remove(uuid);
        }
    }

    private void saveAsync(UUID uuid) {
        dirty.add(uuid);
    }

    private void persistImmediate(UUID uuid) {
        Profile profile = profiles.get(uuid);
        if (profile == null) return;
        ContractPersistence state = snapshot(profile);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> {
                    if (persistProfile(uuid, state)) dirty.remove(uuid);
                });
    }

    private ContractPersistence snapshot(Profile profile) {
        Active active = profile.active;
        return new ContractPersistence(encode(profile), profile.dailyKey, profile.dailyCompleted,
                active == null ? "" : normalize(active.difficulty.name() + "-" + active.objective.name()),
                active == null ? "" : active.difficulty.name(),
                active == null ? "" : active.objective.name(),
                active == null ? 0L : active.target,
                active == null ? 0L : active.progress,
                active == null ? 0 : active.reward,
                active == null ? 0L : active.startedAt,
                active == null ? 0L : active.expiresAt);
    }

    private boolean persistProfile(UUID uuid, ContractPersistence state) {
        return plugin.getPurchaseRepository().saveContractState(uuid, state.contractId, state.difficulty,
                state.objective, state.target, state.progress, state.reward, state.startedAt, state.expiresAt,
                state.dayKey, state.dailyCompleted, state.encoded);
    }

    private String encode(Profile profile) {
        return encode(profile, profile.pending);
    }

    private String encode(Profile profile, Pending selectedPending) {
        StringBuilder value = new StringBuilder("v2|").append(profile.dailyKey).append('|').append(profile.dailyCompleted)
                .append('|').append(profile.pendingVile).append('|');
        if (selectedPending != null) {
            value.append(selectedPending.difficulty).append('~').append(selectedPending.paidPrice).append('~')
                    .append(selectedPending.expiresAt).append('~').append(selectedPending.charged ? 1 : 0).append('~');
            for (int index = 0; index < selectedPending.offers.size(); index++) {
                if (index > 0) value.append(';');
                value.append(encodeOffer(selectedPending.offers.get(index)));
            }
            value.append('~').append(selectedPending.sessionId).append('~').append(selectedPending.createdAt).append("~PENDING");
        }
        value.append('|');
        if (profile.active != null) {
            Active active = profile.active;
            value.append(active.difficulty).append('~').append(active.objective).append('~').append(active.target).append('~')
                    .append(active.progress).append('~').append(active.reward).append('~').append(active.enhanced ? 1 : 0)
                    .append('~').append(active.vile ? 1 : 0).append('~').append(active.startedAt).append('~').append(active.expiresAt)
                    .append('~').append(active.lastKillAt).append('~').append(active.chain);
        }
        return value.toString();
    }

    private String encodeOffer(Offer offer) {
        return offer.difficulty + "," + offer.objective + "," + offer.target + "," + offer.reward + ","
                + (offer.enhanced ? 1 : 0) + "," + (offer.vile ? 1 : 0);
    }

    private Profile decode(UUID uuid, String encoded) {
        Profile profile = new Profile(uuid);
        if (encoded == null || encoded.isEmpty()) return profile;
        try {
            String[] sections = encoded.split("\\|", -1);
            profile.dailyKey = sections.length > 1 ? sections[1] : "";
            profile.dailyCompleted = sections.length > 2 ? Integer.parseInt(sections[2]) : 0;
            profile.pendingVile = sections.length > 3 ? Integer.parseInt(sections[3]) : 0;
            if (sections.length > 4 && !sections[4].isEmpty()) {
                String[] pending = sections[4].split("~", -1);
                List<Offer> offers = new ArrayList<>();
                if (pending.length > 4) for (String value : pending[4].split(";")) offers.add(decodeOffer(value));
                UUID sessionId = pending.length > 5 ? UUID.fromString(pending[5]) : UUID.randomUUID();
                long createdAt = pending.length > 6 ? Long.parseLong(pending[6]) : Math.min(System.currentTimeMillis(), Long.parseLong(pending[2]));
                profile.pending = new Pending(sessionId, Difficulty.valueOf(pending[0]), Integer.parseInt(pending[1]),
                        createdAt, Long.parseLong(pending[2]), pending[3].equals("1"), offers);
            }
            if (sections.length > 5 && !sections[5].isEmpty()) {
                String[] active = sections[5].split("~");
                profile.active = new Active(Objective.valueOf(active[1]), Long.parseLong(active[2]), Long.parseLong(active[3]),
                        Integer.parseInt(active[4]), Difficulty.valueOf(active[0]), active[5].equals("1"), active[6].equals("1"),
                        Long.parseLong(active[7]), Long.parseLong(active[8]), Long.parseLong(active[9]), Integer.parseInt(active[10]));
            }
        } catch (Exception exception) {
            warn("profile-decode", "A contract profile could not be decoded and was reset safely.");
            return new Profile(uuid);
        }
        return profile;
    }

    private Offer decodeOffer(String value) {
        String[] parts = value.split(",");
        return new Offer(Difficulty.valueOf(parts[0]), Objective.valueOf(parts[1]), Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]), parts[4].equals("1"), parts[5].equals("1"));
    }

    private static final class Profile {
        private final UUID uuid;
        private String dailyKey = "";
        private int dailyCompleted;
        private int pendingVile;
        private Pending pending;
        private Active active;

        private Profile(UUID uuid) {
            this.uuid = uuid;
        }
    }

    private static final class Pending {
        private final UUID sessionId;
        private final Difficulty difficulty;
        private final int paidPrice;
        private final long createdAt;
        private final long expiresAt;
        private final boolean charged;
        private final List<Offer> offers;

        private Pending(UUID sessionId, Difficulty difficulty, int paidPrice, long createdAt, long expiresAt,
                        boolean charged, List<Offer> offers) {
            this.sessionId = sessionId;
            this.difficulty = difficulty;
            this.paidPrice = paidPrice;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.charged = charged;
            this.offers = List.copyOf(offers);
        }
    }

    private static final class Active {
        private final Objective objective;
        private final long target;
        private long progress;
        private final int reward;
        private final Difficulty difficulty;
        private final boolean enhanced;
        private final boolean vile;
        private final long startedAt;
        private final long expiresAt;
        private long lastKillAt;
        private int chain;
        private long lastActionBar;

        private Active(Objective objective, long target, long progress, int reward, Difficulty difficulty,
                       boolean enhanced, boolean vile, long startedAt, long expiresAt, long lastKillAt, int chain) {
            this.objective = objective;
            this.target = target;
            this.progress = progress;
            this.reward = reward;
            this.difficulty = difficulty;
            this.enhanced = enhanced;
            this.vile = vile;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
            this.lastKillAt = lastKillAt;
            this.chain = chain;
        }
    }

    private record Template(Objective objective, boolean enabled, int weight, List<Integer> targets,
                            boolean requiresCenter, int rewardMinimum, int rewardMaximum,
                            int enhancedRewardMaximum) {
    }

    private record Offer(Difficulty difficulty, Objective objective, int target, int reward, boolean enhanced,
                         boolean vile) {
    }

    private record ContractPersistence(String encoded, String dayKey, int dailyCompleted, String contractId,
                                       String difficulty, String objective, long target, long progress, int reward,
                                       long startedAt, long expiresAt) {
    }

    private record Shot(UUID shooter, Location origin, boolean sneaking, long createdAt) {
    }

    private record LavaAttribution(UUID owner, String source, long expiresAt) {
    }

    private static final class OptionalLava {
        private static boolean owner(LavaAttribution attribution, UUID owner) {
            return attribution != null && attribution.expiresAt > System.currentTimeMillis() && attribution.owner.equals(owner);
        }
    }

    private interface ContractHolder extends InventoryHolder {
        UUID owner();
    }

    private static final class ChoiceHolder implements ContractHolder {
        private final UUID owner;
        private final Map<Integer, Integer> slots = new HashMap<>();
        private Inventory inventory;

        private ChoiceHolder(UUID owner) {
            this.owner = owner;
        }

        public UUID owner() { return owner; }
        public Inventory getInventory() { return inventory; }
    }

    private static final class AbandonHolder implements ContractHolder {
        private final UUID owner;
        private Inventory inventory;

        private AbandonHolder(UUID owner) {
            this.owner = owner;
        }

        public UUID owner() { return owner; }
        public Inventory getInventory() { return inventory; }
    }

}
