package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.events.PitLevelChangeEvent;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.model.StatType;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StatsManager {
    private final ThePitPlugin plugin;
    private final Map<UUID, PlayerData> profiles = new ConcurrentHashMap<>();
    private final Set<UUID> loadedProfiles = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Map<UUID, Long>> recentKills = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> bounties = new ConcurrentHashMap<>();
    private final KillXpService killXpService;
    private final Set<UUID> bountyLocks = ConcurrentHashMap.newKeySet();
    private final Set<String> bountyClaims = ConcurrentHashMap.newKeySet();
    private final SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat month = new SimpleDateFormat("yyyy-MM");
    private final Random random = new Random();
    private final File prestigeFallbackFile;
    private final YamlConfiguration prestigeFallback;
    private final Set<UUID> dirtyStatistics = ConcurrentHashMap.newKeySet();
    private final Map<UUID, AtomicInteger> pendingProfileWrites = new ConcurrentHashMap<>();
    private final Map<UUID, Object> profileWriteMonitors = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> profileLoadGenerations = new ConcurrentHashMap<>();
    private int flushTask = -1;

    public static final String ASSISTS = "assists";
    public static final String SWORD_HITS = "sword_hits";
    public static final String ARROWS_SHOT = "arrows_shot";
    public static final String DAMAGE_DEALT = "damage_dealt";
    public static final String MELEE_DAMAGE_DEALT = "melee_damage_dealt";
    public static final String BOW_DAMAGE_DEALT = "bow_damage_dealt";
    public static final String DAMAGE_TAKEN = "damage_taken";
    public static final String MELEE_DAMAGE_TAKEN = "melee_damage_taken";
    public static final String BOW_DAMAGE_TAKEN = "bow_damage_taken";
    public static final String HIGHEST_STREAK = "highest_streak";
    public static final String PLAYTIME_MILLIS = "playtime_millis";
    public static final String GOLD_EARNED = "gold_earned";
    public static final String LEFT_CLICKS = "left_clicks";
    public static final String SHOP_ITEMS_PURCHASED = "shop_items_purchased";
    public static final String DIAMOND_ITEMS_PURCHASED = "diamond_items_purchased";
    public static final String GOLD_SPENT_ITEM_SHOP = "gold_spent_item_shop";
    public static final String CHAT_MESSAGES = "chat_messages";
    public static final String BLOCKS_PLACED = "blocks_placed";
    public static final String BLOCKS_BROKEN = "blocks_broken";
    public static final String JUMPS_INTO_PIT = "jumps_into_pit";
    public static final String LAUNCHER_LAUNCHES = "launcher_launches";
    public static final String GOLDEN_APPLES_EATEN = "golden_apples_eaten";
    public static final String GOLDEN_HEADS_EATEN = "golden_heads_eaten";
    public static final String LAVA_BUCKETS_EMPTIED = "lava_buckets_emptied";
    public static final String FISHING_RODS_LAUNCHED = "fishing_rods_launched";
    public static final String CONTRACTS_COMPLETED = "contracts_completed";
    public static final String WHEAT_FARMED = "wheat_farmed";
    public static final String GOLD_FROM_FARMING = "gold_from_farming";
    public static final String GOLD_FROM_SELLING_FISH = "gold_from_selling_fish";
    public static final String KINGS_QUEST_COMPLETED = "kings_quest_completed";
    public static final String SEWER_TREASURES_FOUND = "sewer_treasures_found";
    public static final String NIGHT_QUESTS_COMPLETED = "night_quests_completed";
    public static final String FISHED_ANYTHING = "fished_anything";
    public static final String FISHED_FISH = "fished_fish";
    public static final String STATS_MENU_UNLOCKED = "stats-menu-unlocked";

    public StatsManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        this.killXpService = new KillXpService(plugin);
        prestigeFallbackFile = new File(plugin.getDataFolder(), "prestige-profiles.yml");
        prestigeFallback = YamlConfiguration.loadConfiguration(prestigeFallbackFile);
    }

    public void start() {
        if (flushTask != -1) return;
        long ticks = Math.max(200L, plugin.registry().settings().getLong("statistics.persistence.flush-interval-ticks", 1200L));
        flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirtyNow, ticks, ticks).getTaskId();
    }

    public void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        int generation = profileLoadGenerations.computeIfAbsent(uuid, ignored -> new AtomicInteger()).incrementAndGet();
        profiles.putIfAbsent(uuid, new PlayerData(player.getName()));
        if (!plugin.getStorageManager().isMysql() && !plugin.getStorageManager().isMongo()) {
            PlayerData loaded = loadFallback(uuid, player.getName());
            finishProfileLoad(player, loaded, generation);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData loaded = load(uuid, player.getName());
            finishProfileLoad(player, loaded, generation);
        });
    }

    private void finishProfileLoad(Player player, PlayerData loaded, int generation) {
        AtomicInteger currentGeneration = profileLoadGenerations.get(player.getUniqueId());
        if (currentGeneration == null || currentGeneration.get() != generation) {
            if (plugin.registry().tablist().debugEnabled()) plugin.getLogger().info("Discarded stale profile load player="
                    + player.getName() + " generation=" + generation + " active_generation="
                    + (currentGeneration == null ? "none" : currentGeneration.get()));
            return;
        }
        if (loaded.loadFailed) {
            plugin.getLogger().severe("The Pit profile load failed for " + player.getName() + ". Inventory loading, gameplay entry, and saving remain blocked.");
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getPlayerInventoryManager().failProfile(player, "progression storage returned a failed load"));
            return;
        }
        long now = System.currentTimeMillis();
        loaded.sessionStarted = now;
        loaded.lastPitLogin = now;
        profiles.put(player.getUniqueId(), loaded);
        loadedProfiles.add(player.getUniqueId());
        loadBounty(player.getUniqueId());
        persistLastPitLogin(player.getUniqueId(), player.getName(), now);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            AtomicInteger activeGeneration = profileLoadGenerations.get(player.getUniqueId());
            if (activeGeneration == null || activeGeneration.get() != generation) {
                if (plugin.registry().tablist().debugEnabled()) plugin.getLogger().info("Discarded stale profile callback player="
                        + player.getName() + " generation=" + generation + " active_generation="
                        + (activeGeneration == null ? "none" : activeGeneration.get()));
                return;
            }
            plugin.getPlayerInventoryManager().loadAfterProfile(player, !loaded.existingProfile, () -> {
                AtomicInteger callbackGeneration = profileLoadGenerations.get(player.getUniqueId());
                if (!player.isOnline() || callbackGeneration == null || callbackGeneration.get() != generation) return;
                plugin.getPerkManager().revalidate(player);
                plugin.getBuildScoreboardManager().update(player);
                if (plugin.getTablistManager() != null) plugin.getTablistManager().markRefresh(player, "PIT_PROFILE_AND_INVENTORY_READY");
                if (plugin.getLeaderboardManager() != null) {
                    plugin.getLeaderboardManager().markActiveLeaderboardDirty();
                    plugin.getLeaderboardManager().synchronizePermanentFeatures(player);
                    plugin.getLeaderboardManager().refreshViewerXp(player);
                }
            });
        });
    }

    private void persistLastPitLogin(UUID uuid, String name, long timestamp) {
        Runnable save = () -> {
            if (plugin.getStorageManager().isMongo()) {
                plugin.getStorageManager().maxMongoLong(uuid.toString(), name, Period.LIFETIME.name(), "all", "last_buildffa_login", timestamp);
            } else if (plugin.getStorageManager().isMysql()) {
                try (PreparedStatement statement = plugin.getStorageManager().prepare("INSERT INTO buildffa_stats(uuid,name,period,period_key,last_buildffa_login) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),last_buildffa_login=GREATEST(last_buildffa_login,VALUES(last_buildffa_login))")) {
                    statement.setString(1, uuid.toString());
                    statement.setString(2, name);
                    statement.setString(3, Period.LIFETIME.name());
                    statement.setString(4, "all");
                    statement.setLong(5, timestamp);
                    statement.executeUpdate();
                } catch (Exception exception) {
                    plugin.getLogger().warning("Could not update The Pit login time for " + name + ": " + exception.getMessage());
                }
            } else {
                synchronized (StatsManager.this) {
                    prestigeFallback.set("players." + uuid + ".last-buildffa-login", timestamp);
                    savePrestigeFallback();
                }
            }
        };
        if (Bukkit.isPrimaryThread()) Bukkit.getScheduler().runTaskAsynchronously(plugin, save);
        else save.run();
    }

    public boolean handleKill(Player killer, Player dead) {
        if (!validKill(killer, dead)) return false;
        if (!fullKillXp(killer, dead)) return false;
        PlayerData killerData = profile(killer.getUniqueId(), killer.getName());
        PlayerData deadData = profile(dead.getUniqueId(), dead.getName());
        int victimStreak = deadData.killstreak;
        int nextKillerStreak = killerData.killstreak + 1;
        PitDeathContext deathContext = new PitDeathContext(
                dead.getUniqueId(),
                killer.getUniqueId(),
                deadData.level,
                killerData.level,
                victimStreak,
                nextKillerStreak,
                UUID.randomUUID(),
                dead.getLocation()
        );
        int claimedBounty = getBounty(dead.getUniqueId());
        if (plugin.getKillstreakManager() != null) plugin.getKillstreakManager().cleanup(dead, true);
        int minimumGold = plugin.registry().settings().getInt("settings.coins.kill-min", 2);
        int maximumGold = Math.max(minimumGold, plugin.registry().settings().getInt("settings.coins.kill-max", 8));
        int pitGold = applyCoinMultiplier(killer, minimumGold + random.nextInt(maximumGold - minimumGold + 1));
        if (plugin.getPerkManager() != null) {
            me.outerspace.thepit.perk.PerkManager.Reward reward = plugin.getPerkManager().modifyKillReward(killer, dead, 0, pitGold, nextKillerStreak);
            pitGold = reward.coins();
        }
        pitGold = plugin.getPitGoldService().boostedAmount(killer, pitGold, ThePitPlugin.GoldSource.KILL);
        int killstreakFlatXp = 0;
        if (plugin.getKillstreakManager() != null) {
            KillstreakManager.Reward reward = plugin.getKillstreakManager().handleKill(killer, dead, nextKillerStreak, 0, pitGold);
            killstreakFlatXp = Math.max(0, reward.xp());
            pitGold = reward.gold();
        }
        KillXpTransactionResult xpTransaction = killXpService.grantKillXp(killer, dead, deathContext, killstreakFlatXp);
        if (!xpTransaction.succeeded()) return false;
        int bountyReward = claimBounty(killer, dead);
        killerData.kills = safeIntAdd(killerData.kills, 1);
        killerData.killstreak = nextKillerStreak;
        killerData.coins = Math.max(0, safeIntAdd(killerData.coins, pitGold + bountyReward));
        if (nextKillerStreak > getLongStat(killer.getUniqueId(), HIGHEST_STREAK)) setLongStat(killer.getUniqueId(), HIGHEST_STREAK, nextKillerStreak);
        deadData.deaths = safeIntAdd(deadData.deaths, 1);
        deadData.killstreak = 0;
        killXpService.resetLife(dead.getUniqueId());
        plugin.getPitGoldService().recordEarned(killer, pitGold + bountyReward, ThePitPlugin.GoldSource.KILL);
        applyElGato(killer, nextKillerStreak);
        if (plugin.getShopManager() != null) plugin.getShopManager().onKill(killer);
        if (plugin.getRenownShopManager() != null) {
            int tenacity = plugin.getRenownShopManager().tier(killer.getUniqueId(), "tenacity");
            if (tenacity > 0) {
                double health = plugin.getRenownShopManager().effect("tenacity", "hearts-per-tier", 0.5D) * 2.0D * tenacity;
                killer.setHealth(Math.min(killer.getMaxHealth(), killer.getHealth() + Math.max(0.0D, health)));
            }
        }
        handleKillStreak(killer, nextKillerStreak);
        handleBounty(killer, nextKillerStreak);
        if (plugin.getContractManager() != null) plugin.getContractManager().onKill(killer, dead, claimedBounty, nextKillerStreak);
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(killer.getUniqueId(), AchievementManager.EventType.PLAYER_KILL, 1L, Map.of("victim_bounty", claimedBounty, "strength_max", plugin.getPerkListener() != null && plugin.getPerkListener().isMaximumStrength(killer)), false));
            plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(killer.getUniqueId(), AchievementManager.EventType.STREAK_CHANGED, nextKillerStreak, Collections.emptyMap(), false));
            if (claimedBounty > 0) plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(killer.getUniqueId(), AchievementManager.EventType.BOUNTY_CLAIMED, claimedBounty, Map.of("bounty_hunter", plugin.getPerkManager() != null && plugin.getPerkManager().has(killer, "bounty_hunter")), false));
        }
        int grantedXp = xpTransaction.getGrantedXp();
        plugin.getQuestManager().handleKill(killer, grantedXp, nextKillerStreak);
        if (xpTransaction.getStatus() == KillXpTransactionResult.Status.CONVERTED_TO_GOLD) {
            killer.sendMessage(prefix() + CC.color("You earned &6" + (pitGold + bountyReward + xpTransaction.getConvertedGold()) + " Gold&7."));
        } else {
            killer.sendMessage(prefix() + CC.color("You earned &6" + (pitGold + bountyReward) + " Gold &7and &b+" + grantedXp + " XP&7."));
        }
        persistDelta(killer, 1, 0, pitGold + bountyReward, 1);
        persistDelta(dead, 0, 1, 0, -1);
        plugin.getBuildScoreboardManager().update(killer);
        plugin.getBuildScoreboardManager().update(dead);
        if (plugin.getPerkManager() != null) plugin.getPerkManager().revalidate(killer);
        return true;
    }

    public void handleDeath(Player dead) {
        if (dead == null || !plugin.getArenaManager().isInCurrentArena(dead.getLocation())) return;
        PlayerData data = profile(dead.getUniqueId(), dead.getName());
        data.deaths = safeIntAdd(data.deaths, 1);
        if (plugin.getKillstreakManager() != null) plugin.getKillstreakManager().cleanup(dead, true);
        data.killstreak = 0;
        killXpService.resetLife(dead.getUniqueId());
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().resetLife(dead.getUniqueId());
        persistDelta(dead, 0, 1, 0, -1);
        plugin.getBuildScoreboardManager().update(dead);
    }

    private boolean validKill(Player killer, Player victim) {
        return killer != null && victim != null && killer != victim
                && !killer.getUniqueId().equals(victim.getUniqueId())
                && killer.isOnline() && (victim.isOnline() || plugin.getCombatLogManager() != null && plugin.getCombatLogManager().isProcessingDeath(victim.getUniqueId()))
                && isProfileLoaded(killer.getUniqueId()) && isProfileLoaded(victim.getUniqueId())
                && plugin.getArenaManager().isInCurrentArena(killer.getLocation())
                && plugin.getArenaManager().isInCurrentArena(victim.getLocation());
    }

    private boolean fullKillXp(Player killer, Player victim) {
        long cooldown = Math.max(0L, plugin.registry().settings().getLong("levels.rewards.repeat-kill-full-xp-cooldown-seconds", 60L)) * 1000L;
        long now = System.currentTimeMillis();
        Map<UUID, Long> victims = recentKills.computeIfAbsent(killer.getUniqueId(), ignored -> new ConcurrentHashMap<>());
        long previous = victims.getOrDefault(victim.getUniqueId(), 0L);
        victims.put(victim.getUniqueId(), now);
        return cooldown == 0L || now - previous >= cooldown;
    }

    public int getKills(UUID uuid, Period period) {
        return period == Period.LIFETIME ? profile(uuid, "").kills : getStoredStat(uuid, period, "kills");
    }

    public int getDeaths(UUID uuid, Period period) {
        return period == Period.LIFETIME ? profile(uuid, "").deaths : getStoredStat(uuid, period, "deaths");
    }

    public int getCoins(UUID uuid, Period period) {
        return period == Period.LIFETIME ? profile(uuid, "").coins : getStoredStat(uuid, period, "coins");
    }

    public int getKillStreak(UUID uuid, Period period) {
        return period == Period.LIFETIME ? profile(uuid, "").killstreak : getStoredStat(uuid, period, "killstreak");
    }

    public double getKd(UUID uuid, Period period) {
        int deaths = getDeaths(uuid, period);
        return deaths <= 0 ? getKills(uuid, period) : (double) getKills(uuid, period) / deaths;
    }

    public int getLevel(UUID uuid) {
        return profile(uuid, "").level;
    }

    public int getLevelXp(UUID uuid) {
        return profile(uuid, "").currentXp;
    }

    public long getLifetimeXp(UUID uuid) {
        return profile(uuid, "").lifetimeXp;
    }

    public int getRequiredXp(UUID uuid) {
        PlayerData data = profile(uuid, "");
        return requiredXp(data.level, data.prestige);
    }

    public String getLevelColor(UUID uuid) {
        return levelColor(getLevel(uuid));
    }

    public String getLevelColorFor(int level) {
        return levelColor(level);
    }

    public String getFormattedLevel(UUID uuid) {
        return getFormattedLevel(uuid, getLevel(uuid), LevelRenderContext.TAB_LIST);
    }

    public String getFormattedLevel(UUID uuid, int level) {
        return getFormattedLevel(uuid, level, LevelRenderContext.TAB_LIST);
    }

    public String getFormattedLevel(UUID uuid, LevelRenderContext context) {
        return getFormattedLevel(uuid, getLevel(uuid), context);
    }

    public String getFormattedLevel(UUID uuid, int level, LevelRenderContext context) {
        return getFormattedLevel(level, getPrestige(uuid), context);
    }

    public String getFormattedLevel(int level, int prestige) {
        return getFormattedLevel(level, prestige, LevelRenderContext.TAB_LIST);
    }

    public String getFormattedLevel(int level, int prestige, LevelRenderContext context) {
        String levelColor = levelColor(level);
        String prestigeColor = prestigeBracketColor(prestige);
        String prestigeText = context == LevelRenderContext.SCOREBOARD || prestige <= 0 ? "" : roman(prestige) + "-";
        if (context == LevelRenderContext.NAMETAG) {
            levelColor = levelColor.replace("&l", "").replace("&L", "");
            StringBuilder value = new StringBuilder(prestigeColor).append('[').append(prestigeText);
            if (!levelColor.equalsIgnoreCase(prestigeColor)) value.append(levelColor);
            value.append(level);
            if (!levelColor.equalsIgnoreCase(prestigeColor)) value.append(prestigeColor);
            return CC.color(value.append(']').toString());
        }
        return CC.color(prestigeColor + "[" + prestigeText + levelColor + level + prestigeColor + "]");
    }

    public enum LevelRenderContext { SCOREBOARD, NAMETAG, TAB_LIST, CHAT }

    public String getLevelProgress(UUID uuid) {
        PlayerData data = profile(uuid, "");
        return data.level >= maximumLevel() ? "MAX" : data.currentXp + "/" + requiredXp(data.level, data.prestige);
    }

    public int requiredXp(int level, int prestige) {
        if (level >= maximumLevel()) return 0;
        int base = baseRequirement(level);
        double multiplier = getMultiplierFor(prestige);
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.ceil(base * multiplier)));
    }

    public void addXp(Player player, long amount) {
        addXp(player, amount, XpSource.GAMEPLAY);
    }

    public void addXp(Player player, long amount, XpSource source) {
        if (player == null || amount <= 0L) return;
        XpSource effectiveSource = source == null ? XpSource.GAMEPLAY : source;
        if (effectiveSource.eligibleForPassive() && plugin.getPassiveManager() != null) {
            double multiplier = 1.0D + plugin.getPassiveManager().bonus(player.getUniqueId(), "xp-boost");
            amount = Math.max(0L, (long) Math.floor(amount * multiplier + 0.0000001D));
        }
        if (plugin.getRewardMultiplierService() != null && (effectiveSource == XpSource.KILL || effectiveSource == XpSource.ASSIST)) {
            me.outerspace.thepit.managers.RewardMultiplierService.RewardSource rewardSource = effectiveSource == XpSource.KILL
                    ? me.outerspace.thepit.managers.RewardMultiplierService.RewardSource.KILL_XP
                    : me.outerspace.thepit.managers.RewardMultiplierService.RewardSource.ASSIST_XP;
            amount = Math.max(0L, (long) Math.floor(amount * plugin.getRewardMultiplierService()
                    .getMultiplier(player, rewardSource, player.getLocation()).xp() + 0.0000001D));
        }
        if (amount <= 0L) return;
        PlayerData data = profile(player.getUniqueId(), player.getName());
        int oldLevel = data.level;
        data.lifetimeXp = safeLongAdd(data.lifetimeXp, amount);
        if (plugin.getLeaderboardManager() != null) {
            plugin.getLeaderboardManager().markActiveLeaderboardDirty();
            plugin.getLeaderboardManager().refreshViewerXp(player);
        }
        if (data.level >= maximumLevel()) {
            data.currentXp = safeIntAdd(data.currentXp, amount);
            persistProgress(player.getUniqueId(), player.getName(), data);
            return;
        }
        long availableXp = safeLongAdd(data.currentXp, amount);
        while (data.level < maximumLevel()) {
            int required = requiredXp(data.level, data.prestige);
            if (availableXp < required) break;
            availableXp -= required;
            data.level++;
        }
        data.currentXp = availableXp > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, availableXp);
        if (data.level >= maximumLevel()) data.currentXp = Math.max(0, data.currentXp);
        boolean changed = data.level > oldLevel;
        boolean statisticsUnlocked = changed && oldLevel < 50 && data.level >= 50 && unlockStatsMenu(data, player.getUniqueId());
        if (changed && plugin.getLeaderboardManager() != null) plugin.getLeaderboardManager().unlockPermanentFeatures(player, oldLevel, data.level);
        persistProgress(player.getUniqueId(), player.getName(), data);
        if (changed) fireLevelChange(player, oldLevel, data.level, amount, effectiveSource, statisticsUnlocked);
        else plugin.getBuildScoreboardManager().update(player);
    }

    public boolean setLevel(OfflinePlayer player, int level) {
        if (player == null || level < minimumLevel() || level > maximumLevel()) return false;
        PlayerData data = ensureLoaded(player);
        int oldLevel = data.level;
        data.level = level;
        data.currentXp = 0;
        if (level >= 50) unlockStatsMenu(data, player.getUniqueId());
        Player online = player.getPlayer();
        if (online != null && plugin.getLeaderboardManager() != null) plugin.getLeaderboardManager().unlockPermanentFeatures(online, oldLevel, level);
        persistProgress(player.getUniqueId(), safeName(player), data);
        revalidateOnline(player);
        if (online != null && oldLevel != level) fireLevelChange(online, oldLevel, level, 0L, XpSource.SET_LEVEL, false);
        return true;
    }

    public boolean setXp(OfflinePlayer player, int amount) {
        if (player == null || amount < 0) return false;
        PlayerData data = ensureLoaded(player);
        int oldLevel = data.level;
        data.currentXp = amount;
        normalizeLevel(data);
        if (data.level >= 50) unlockStatsMenu(data, player.getUniqueId());
        Player online = player.getPlayer();
        if (online != null && plugin.getLeaderboardManager() != null) plugin.getLeaderboardManager().unlockPermanentFeatures(online, oldLevel, data.level);
        persistProgress(player.getUniqueId(), safeName(player), data);
        revalidateOnline(player);
        if (online != null && data.level != oldLevel) fireLevelChange(online, oldLevel, data.level, 0L, XpSource.SET_LEVEL, false);
        return true;
    }

    public boolean addOfflineXp(OfflinePlayer player, long amount) {
        if (player == null || amount < 0) return false;
        Player online = player.getPlayer();
        if (online != null) {
            addXp(online, amount, XpSource.ADMINISTRATIVE);
            return true;
        }
        PlayerData data = ensureLoaded(player);
        data.lifetimeXp = safeLongAdd(data.lifetimeXp, amount);
        long availableXp = safeLongAdd(data.currentXp, amount);
        while (data.level < maximumLevel()) {
            int required = requiredXp(data.level, data.prestige);
            if (availableXp < required) break;
            availableXp -= required;
            data.level++;
            if (data.level >= 50) unlockStatsMenu(data, player.getUniqueId());
        }
        data.currentXp = availableXp > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, availableXp);
        persistProgress(player.getUniqueId(), safeName(player), data);
        return true;
    }

    private void normalizeLevel(PlayerData data) {
        while (data.level < maximumLevel()) {
            int required = requiredXp(data.level, data.prestige);
            if (data.currentXp < required) break;
            data.currentXp -= required;
            data.level++;
        }
    }

    private void fireLevelChange(Player player, int oldLevel, int newLevel, long xpAwarded, XpSource source, boolean statisticsUnlocked) {
        Runnable dispatch = () -> Bukkit.getPluginManager().callEvent(new PitLevelChangeEvent(
                player.getUniqueId(), oldLevel, newLevel, xpAwarded, source.name(),
                source.administrative(), source.prestigeReset(), statisticsUnlocked, false
        ));
        if (Bukkit.isPrimaryThread()) dispatch.run();
        else Bukkit.getScheduler().runTask(plugin, dispatch);
    }

    public int getPrestige(UUID uuid) {
        return profile(uuid, "").prestige;
    }

    public int getMaxPrestige() {
        return Math.max(0, plugin.registry().settings().getInt("prestige.maximum", 50));
    }

    public boolean isMaxPrestige(UUID uuid) {
        return getPrestige(uuid) >= getMaxPrestige();
    }

    public double getMultiplier(UUID uuid) {
        return getMultiplierFor(getPrestige(uuid));
    }

    public double getMultiplierFor(int prestige) {
        int value = Math.max(0, Math.min(getMaxPrestige(), prestige));
        return Math.max(0.0D, plugin.registry().settings().getDouble("prestige.progression." + value + ".multiplier", 1.0D));
    }

    public long getPrestigeCoinsRequired(UUID uuid) {
        int prestige = getPrestige(uuid);
        return Math.max(0L, plugin.registry().settings().getLong("prestige.progression." + prestige + ".coins-required", 0L));
    }

    public long getPrestigeCoinsRemaining(UUID uuid) {
        return Math.max(0L, getPrestigeCoinsRequired(uuid) - getCoins(uuid, Period.LIFETIME));
    }

    public String getPrestigeCoinProgress(UUID uuid) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
        return format.format(getCoins(uuid, Period.LIFETIME)) + "/" + format.format(getPrestigeCoinsRequired(uuid));
    }

    public long getRenown(UUID uuid) {
        return profile(uuid, "").renown;
    }

    public long getLastPrestige(UUID uuid) {
        return profile(uuid, "").lastPrestige;
    }

    public boolean isProfileLoaded(UUID uuid) {
        return uuid != null && loadedProfiles.contains(uuid);
    }

    public long getLastPitLogin(UUID uuid) {
        return profile(uuid, "").lastPitLogin;
    }

    public synchronized String getProfileExtension(UUID uuid, String key) {
        return prestigeFallback.getString("players." + uuid + ".extensions." + key, "");
    }

    public synchronized boolean setProfileExtension(UUID uuid, String key, String value) {
        if (uuid == null || key == null || key.trim().isEmpty()) return false;
        prestigeFallback.set("players." + uuid + ".extensions." + key, value == null ? "" : value);
        return savePrestigeFallback();
    }

    public int getRenownReward(int nextPrestige) {
        for (String range : plugin.registry().settings().keys("prestige.renown-rewards")) {
            String[] parts = range.split("-");
            try {
                int minimum = Integer.parseInt(parts[0]);
                int maximum = parts.length == 1 ? minimum : Integer.parseInt(parts[1]);
                if (nextPrestige >= minimum && nextPrestige <= maximum) return Math.max(0, plugin.registry().settings().getInt("prestige.renown-rewards." + range, 0));
            } catch (NumberFormatException ignored) { }
        }
        return 0;
    }

    public String getPrestigeRoman(UUID uuid) {
        return roman(getPrestige(uuid));
    }

    public String getNextPrestigeRoman(UUID uuid) {
        return roman(Math.min(getMaxPrestige(), getPrestige(uuid) + 1));
    }

    public String getPrestigeFormatted(UUID uuid) {
        String roman = getPrestigeRoman(uuid);
        return roman.isEmpty() ? "0" : CC.color(getPrestigeBracketColor(uuid) + roman);
    }

    public String getPrestigeBracketColor(UUID uuid) {
        return prestigeBracketColor(getPrestige(uuid));
    }

    private String prestigeBracketColor(int prestige) {
        for (String range : plugin.registry().settings().keys("prestige.bracket-colors")) {
            String[] parts = range.split("-");
            try {
                int minimum = Integer.parseInt(parts[0]);
                int maximum = parts.length == 1 ? minimum : Integer.parseInt(parts[1]);
                if (prestige >= minimum && prestige <= maximum) return plugin.registry().settings().getString("prestige.bracket-colors." + range, "&7");
            } catch (NumberFormatException ignored) { }
        }
        return "&7";
    }

    public long getPrestigeCooldownRemaining(Player player) {
        if (player == null || me.outerspace.thepit.util.PermissionCompat.has(plugin, player, "thepit.prestige.cooldown.bypass")) return 0L;
        long cooldown = Math.max(0L, plugin.registry().settings().getLong("prestige.cooldown-seconds", 7200L)) * 1000L;
        long elapsed = System.currentTimeMillis() - getLastPrestige(player.getUniqueId());
        return Math.max(0L, cooldown - Math.max(0L, elapsed));
    }

    public String getPrestigeCooldownFormatted(Player player) {
        long remaining = getPrestigeCooldownRemaining(player);
        if (remaining <= 0L) return "Ready";
        long seconds = (remaining + 999L) / 1000L;
        long hours = seconds / 3600L;
        long minutes = seconds % 3600L / 60L;
        long rest = seconds % 60L;
        if (hours > 0L) return hours + "h " + minutes + "m";
        return minutes > 0L ? minutes + "m " + rest + "s" : rest + "s";
    }

    public synchronized void recordPitGold(Player player, long amount) {
        if (player == null || amount <= 0L || !isProfileLoaded(player.getUniqueId())) return;
        addLongStat(player.getUniqueId(), GOLD_EARNED, amount);
    }

    public enum XpSource {
        GAMEPLAY(true, false, false),
        KILL(true, false, false),
        ASSIST(true, false, false),
        QUEST(true, false, false),
        FISHING(true, false, false),
        EVENT(true, false, false),
        KILLSTREAK(true, false, false),
        EL_GATO(true, false, false),
        ADMINISTRATIVE(false, true, false),
        MIGRATION(false, false, false),
        LOAD(false, false, false),
        SET_LEVEL(false, true, false),
        PRESTIGE_RESET(false, false, true);

        private final boolean passive;
        private final boolean administrative;
        private final boolean prestigeReset;

        XpSource(boolean passive, boolean administrative, boolean prestigeReset) {
            this.passive = passive;
            this.administrative = administrative;
            this.prestigeReset = prestigeReset;
        }

        public boolean eligibleForPassive() {
            return passive;
        }

        public boolean administrative() {
            return administrative;
        }

        public boolean prestigeReset() {
            return prestigeReset;
        }
    }

    public long getLongStat(UUID uuid, String id) {
        PlayerData data = profile(uuid, "");
        if (PLAYTIME_MILLIS.equals(id)) {
            long current = data.longStats.getOrDefault(id, 0L);
            return data.sessionStarted <= 0L ? current : safeLongAdd(current, Math.max(0L, System.currentTimeMillis() - data.sessionStarted));
        }
        return data.longStats.getOrDefault(id, 0L);
    }

    public double getDoubleStat(UUID uuid, String id) {
        return profile(uuid, "").doubleStats.getOrDefault(id, 0.0D);
    }

    public void addLongStat(UUID uuid, String id, long amount) {
        if (uuid == null || id == null || amount == 0L) return;
        PlayerData data = profile(uuid, "");
        data.longStats.put(id, safeLongAdd(data.longStats.getOrDefault(id, 0L), amount));
        dirtyStatistics.add(uuid);
    }

    public void setLongStat(UUID uuid, String id, long value) {
        if (uuid == null || id == null) return;
        profile(uuid, "").longStats.put(id, Math.max(0L, value));
        dirtyStatistics.add(uuid);
    }

    public void addDoubleStat(UUID uuid, String id, double amount) {
        if (uuid == null || id == null || !Double.isFinite(amount) || amount <= 0.0D) return;
        PlayerData data = profile(uuid, "");
        double value = data.doubleStats.getOrDefault(id, 0.0D) + amount;
        data.doubleStats.put(id, Double.isFinite(value) ? value : Double.MAX_VALUE);
        dirtyStatistics.add(uuid);
    }

    public void recordAssist(Player player) {
        if (player == null) return;
        addLongStat(player.getUniqueId(), ASSISTS, 1L);
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new AchievementManager.ProgressEvent(player.getUniqueId(), AchievementManager.EventType.PLAYER_ASSIST,
                        1L, Collections.emptyMap(), false));
    }

    public long getAssists(UUID uuid) {
        return getLongStat(uuid, ASSISTS);
    }

    public long getHighestStreak(UUID uuid) {
        return getLongStat(uuid, HIGHEST_STREAK);
    }

    public double safeRatio(double numerator, double denominator) {
        String rule = plugin.registry().settings().getString("statistics.performance.safe-zero-rule", "NUMERATOR");
        if (denominator <= 0.0D) return "ZERO".equalsIgnoreCase(rule) ? 0.0D : Math.max(0.0D, numerator);
        double value = numerator / denominator;
        return Double.isFinite(value) ? value : 0.0D;
    }

    public boolean isStatsMenuUnlocked(UUID uuid) {
        PlayerData data = profile(uuid, "");
        return data.longStats.getOrDefault(STATS_MENU_UNLOCKED, 0L) > 0L || data.level >= 50 || data.prestige > 0;
    }

    private boolean unlockStatsMenu(PlayerData data, UUID uuid) {
        if (data.longStats.getOrDefault(STATS_MENU_UNLOCKED, 0L) > 0L) return false;
        data.longStats.put(STATS_MENU_UNLOCKED, 1L);
        dirtyStatistics.add(uuid);
        return true;
    }

    private void inferStatsUnlock(UUID uuid, PlayerData data) {
        if ((data.level >= 50 || data.prestige > 0) && data.longStats.getOrDefault(STATS_MENU_UNLOCKED, 0L) == 0L) {
            data.longStats.put(STATS_MENU_UNLOCKED, 1L);
            dirtyStatistics.add(uuid);
        }
        if (data.killstreak > data.longStats.getOrDefault(HIGHEST_STREAK, 0L)) {
            data.longStats.put(HIGHEST_STREAK, (long) data.killstreak);
            dirtyStatistics.add(uuid);
        }
    }

    public boolean hasRenownUpgrade(UUID uuid, String id) {
        String normalized = normalizeUpgrade(id);
        for (String value : profile(uuid, "").renownUpgrades.split(",")) {
            if (normalizeUpgrade(value).equals(normalized)) return true;
        }
        return false;
    }

    public synchronized boolean purchaseRenownUpgrade(Player player, String id, int requiredPrestige, long price) {
        if (player == null || getPrestige(player.getUniqueId()) < requiredPrestige || price < 0L) return false;
        PlayerData data = profile(player.getUniqueId(), player.getName());
        String normalized = normalizeUpgrade(id);
        if (hasRenownUpgrade(player.getUniqueId(), normalized) || data.renown < price) return false;
        String previous = data.renownUpgrades;
        long previousRenown = data.renown;
        data.renown -= price;
        data.renownUpgrades = previous.isEmpty() ? normalized : previous + "," + normalized;
        persistPrestigeFields(player.getUniqueId(), player.getName(), data);
        if (data.renown < 0L) {
            data.renown = previousRenown;
            data.renownUpgrades = previous;
            return false;
        }
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new AchievementManager.ProgressEvent(player.getUniqueId(), AchievementManager.EventType.RENOWN_UNLOCKED,
                        1L, Map.of("id", normalized), false));
        return true;
    }

    public RenownPurchaseResult persistRenownPurchase(UUID uuid, String name, int requiredPrestige, long price,
                                                      String previousToken, String nextToken) {
        if (uuid == null || nextToken == null || price < 0L || !awaitProfileWrites(uuid, 10000L)) {
            return new RenownPurchaseResult(RenownPurchaseStatus.STORAGE_FAILURE, 0L, "");
        }
        String normalizedPrevious = normalizeUpgrade(previousToken);
        String normalizedNext = normalizeUpgrade(nextToken);
        if (normalizedNext.isEmpty()) return new RenownPurchaseResult(RenownPurchaseStatus.STORAGE_FAILURE, 0L, "");
        if (plugin.getStorageManager().isMysql()) {
            synchronized (plugin.getStorageManager()) {
                Connection connection = null;
                boolean previousAutoCommit = true;
                try {
                    connection = plugin.getStorageManager().connection();
                    previousAutoCommit = connection.getAutoCommit();
                    connection.setAutoCommit(false);
                    int prestige;
                    long renown;
                    String upgrades;
                    try (PreparedStatement statement = connection.prepareStatement("SELECT prestige,renown,renown_upgrades FROM buildffa_stats WHERE uuid=? AND period=? AND period_key=? FOR UPDATE")) {
                        statement.setString(1, uuid.toString());
                        statement.setString(2, Period.LIFETIME.name());
                        statement.setString(3, "all");
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                connection.rollback();
                                return new RenownPurchaseResult(RenownPurchaseStatus.PROFILE_NOT_LOADED, 0L, "");
                            }
                            prestige = result.getInt("prestige");
                            renown = Math.max(0L, result.getLong("renown"));
                            upgrades = result.getString("renown_upgrades");
                        }
                    }
                    upgrades = upgrades == null ? "" : upgrades;
                    RenownPurchaseStatus validation = validateRenownPurchase(prestige, renown, upgrades,
                            requiredPrestige, price, normalizedPrevious, normalizedNext);
                    if (validation != RenownPurchaseStatus.SUCCESS) {
                        connection.rollback();
                        return new RenownPurchaseResult(validation, renown, upgrades);
                    }
                    long balance = renown - price;
                    String updated = appendUpgrade(upgrades, normalizedNext);
                    try (PreparedStatement statement = connection.prepareStatement("UPDATE buildffa_stats SET renown=?,renown_upgrades=? WHERE uuid=? AND period=? AND period_key=?")) {
                        statement.setLong(1, balance);
                        statement.setString(2, updated);
                        statement.setString(3, uuid.toString());
                        statement.setString(4, Period.LIFETIME.name());
                        statement.setString(5, "all");
                        if (statement.executeUpdate() != 1) {
                            connection.rollback();
                            return new RenownPurchaseResult(RenownPurchaseStatus.STORAGE_FAILURE, renown, upgrades);
                        }
                    }
                    connection.commit();
                    return new RenownPurchaseResult(RenownPurchaseStatus.SUCCESS, balance, updated);
                } catch (Exception exception) {
                    try { if (connection != null) connection.rollback(); } catch (Exception ignored) { }
                    plugin.getLogger().warning("Renown purchase transaction failed for " + uuid + ": " + exception.getMessage());
                    return new RenownPurchaseResult(RenownPurchaseStatus.STORAGE_FAILURE, 0L, "");
                } finally {
                    try { if (connection != null) connection.setAutoCommit(previousAutoCommit); } catch (Exception ignored) { }
                }
            }
        }
        if (plugin.getStorageManager().isMongo()) {
            int prestige = plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "prestige", 0);
            long renown = Math.max(0L, plugin.getStorageManager().getMongoLong(uuid.toString(), Period.LIFETIME.name(), "all", "renown", 0L));
            String upgrades = plugin.getStorageManager().getMongoString(uuid.toString(), Period.LIFETIME.name(), "all", "renown_upgrades", "");
            RenownPurchaseStatus validation = validateRenownPurchase(prestige, renown, upgrades,
                    requiredPrestige, price, normalizedPrevious, normalizedNext);
            if (validation != RenownPurchaseStatus.SUCCESS) return new RenownPurchaseResult(validation, renown, upgrades);
            long balance = renown - price;
            String updated = appendUpgrade(upgrades, normalizedNext);
            Map<String, Object> expected = new java.util.LinkedHashMap<>();
            expected.put("prestige", prestige);
            expected.put("renown", renown);
            expected.put("renown_upgrades", upgrades);
            Map<String, Object> fields = new java.util.LinkedHashMap<>();
            fields.put("renown", balance);
            fields.put("renown_upgrades", updated);
            if (!plugin.getStorageManager().setMongoFieldsConditional(uuid.toString(), name, Period.LIFETIME.name(), "all", expected, fields)) {
                return new RenownPurchaseResult(RenownPurchaseStatus.CONFLICT, renown, upgrades);
            }
            return new RenownPurchaseResult(RenownPurchaseStatus.SUCCESS, balance, updated);
        }
        synchronized (this) {
            PlayerData data = profile(uuid, name);
            RenownPurchaseStatus validation = validateRenownPurchase(data.prestige, data.renown, data.renownUpgrades,
                    requiredPrestige, price, normalizedPrevious, normalizedNext);
            if (validation != RenownPurchaseStatus.SUCCESS) {
                return new RenownPurchaseResult(validation, data.renown, data.renownUpgrades);
            }
            long previousRenown = data.renown;
            String previousUpgrades = data.renownUpgrades;
            data.renown -= price;
            data.renownUpgrades = appendUpgrade(data.renownUpgrades, normalizedNext);
            if (!saveFallbackState(uuid, prestigeState(uuid), null)) {
                data.renown = previousRenown;
                data.renownUpgrades = previousUpgrades;
                return new RenownPurchaseResult(RenownPurchaseStatus.STORAGE_FAILURE, previousRenown, previousUpgrades);
            }
            return new RenownPurchaseResult(RenownPurchaseStatus.SUCCESS, data.renown, data.renownUpgrades);
        }
    }

    public synchronized void applyRenownPurchase(UUID uuid, RenownPurchaseResult result) {
        if (uuid == null || result == null || result.status() != RenownPurchaseStatus.SUCCESS) return;
        PlayerData data = profile(uuid, "");
        data.renown = Math.max(0L, result.balance());
        data.renownUpgrades = result.upgrades() == null ? "" : result.upgrades();
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new AchievementManager.ProgressEvent(uuid, AchievementManager.EventType.RENOWN_UNLOCKED,
                        1L, Collections.emptyMap(), false));
    }

    public synchronized void synchronizeRenownPurchase(UUID uuid, RenownPurchaseResult result) {
        if (uuid == null || result == null || result.upgrades() == null
                || result.status() == RenownPurchaseStatus.STORAGE_FAILURE
                || result.status() == RenownPurchaseStatus.CONFLICT
                || result.status() == RenownPurchaseStatus.PROFILE_NOT_LOADED) return;
        PlayerData data = profile(uuid, "");
        data.renown = Math.max(0L, result.balance());
        data.renownUpgrades = result.upgrades();
    }

    private RenownPurchaseStatus validateRenownPurchase(int prestige, long renown, String upgrades,
                                                        int requiredPrestige, long price,
                                                        String previousToken, String nextToken) {
        if (prestige < requiredPrestige) return RenownPurchaseStatus.PRESTIGE_LOCKED;
        if (containsUpgrade(upgrades, nextToken)) return RenownPurchaseStatus.ALREADY_PURCHASED;
        if (!previousToken.isEmpty() && !containsUpgrade(upgrades, previousToken)) return RenownPurchaseStatus.TIER_MISMATCH;
        if (renown < price || renown - price < 0L) return RenownPurchaseStatus.INSUFFICIENT_RENOWN;
        return RenownPurchaseStatus.SUCCESS;
    }

    private boolean containsUpgrade(String upgrades, String token) {
        if (token == null || token.isEmpty()) return false;
        for (String value : (upgrades == null ? "" : upgrades).split(",")) {
            if (normalizeUpgrade(value).equals(token)) return true;
        }
        return false;
    }

    private String appendUpgrade(String upgrades, String token) {
        String value = upgrades == null ? "" : upgrades.trim();
        return value.isEmpty() ? token : value + "," + token;
    }

    public synchronized boolean grantRenownUpgrade(Player player, String id) {
        if (player == null || id == null || id.trim().isEmpty()) return false;
        PlayerData data = profile(player.getUniqueId(), player.getName());
        String normalized = normalizeUpgrade(id);
        if (hasRenownUpgrade(player.getUniqueId(), normalized)) return true;
        data.renownUpgrades = data.renownUpgrades.isEmpty() ? normalized : data.renownUpgrades + "," + normalized;
        persistPrestigeFields(player.getUniqueId(), player.getName(), data);
        return true;
    }

    public synchronized boolean removeRenownUpgrade(Player player, String id) {
        if (player == null || id == null || id.trim().isEmpty()) return false;
        PlayerData data = profile(player.getUniqueId(), player.getName());
        String normalized = normalizeUpgrade(id);
        List<String> retained = new ArrayList<>();
        boolean removed = false;
        for (String value : data.renownUpgrades.split(",")) {
            if (normalizeUpgrade(value).equals(normalized)) removed = true;
            else if (!value.trim().isEmpty()) retained.add(normalizeUpgrade(value));
        }
        if (!removed) return false;
        data.renownUpgrades = String.join(",", retained);
        persistPrestigeFields(player.getUniqueId(), player.getName(), data);
        return true;
    }

    public boolean renownToggleEnabled(UUID uuid, String id, boolean fallback) {
        if (uuid == null || id == null) return fallback;
        return !hasRenownUpgrade(uuid, "toggle-" + normalizeUpgrade(id) + "-off");
    }

    public synchronized boolean setRenownToggle(Player player, String id, boolean enabled) {
        if (player == null || id == null) return false;
        String token = "toggle-" + normalizeUpgrade(id) + "-off";
        return enabled ? !hasRenownUpgrade(player.getUniqueId(), token) || removeRenownUpgrade(player, token)
                : hasRenownUpgrade(player.getUniqueId(), token) || grantRenownUpgrade(player, token);
    }

    public int getPassiveTier(UUID uuid, String id) {
        return parsePassiveData(profile(uuid, "").temporaryPassiveUpgrades).getOrDefault(normalizePassive(id), 0);
    }

    public String passiveData(UUID uuid) {
        return profile(uuid, "").temporaryPassiveUpgrades;
    }

    public String withPassiveTier(String raw, String id, int tier) {
        Map<String, Integer> values = parsePassiveData(raw);
        values.put(normalizePassive(id), Math.max(0, Math.min(6, tier)));
        return serializePassiveData(values);
    }

    public void applyPassiveData(UUID uuid, String data) {
        profile(uuid, "").temporaryPassiveUpgrades = data == null ? "" : data;
    }

    public boolean persistPassiveTransaction(UUID uuid, String name, String expected, String updated, int expectedCoins, int price) {
        if (price < 0 || expectedCoins < price) return false;
        if (!awaitProfileWrites(uuid, 10000L)) return false;
        int updatedCoins = expectedCoins - price;
        if (plugin.getStorageManager().isMongo()) {
            Map<String, Object> expectedFields = new LinkedHashMap<>();
            expectedFields.put("temporary_passive_upgrades", expected);
            expectedFields.put("coins", expectedCoins);
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("temporary_passive_upgrades", updated);
            fields.put("coins", updatedCoins);
            return plugin.getStorageManager().setMongoFieldsConditional(uuid.toString(), name, Period.LIFETIME.name(), "all", expectedFields, fields);
        }
        if (plugin.getStorageManager().isMysql()) {
            try (PreparedStatement statement = plugin.getStorageManager().prepare("UPDATE buildffa_stats SET temporary_passive_upgrades=?,coins=? WHERE uuid=? AND period=? AND period_key=? AND COALESCE(temporary_passive_upgrades,'')=? AND coins=?")) {
                statement.setString(1, updated);
                statement.setInt(2, updatedCoins);
                statement.setString(3, uuid.toString());
                statement.setString(4, Period.LIFETIME.name());
                statement.setString(5, "all");
                statement.setString(6, expected == null ? "" : expected);
                statement.setInt(7, expectedCoins);
                return statement.executeUpdate() == 1;
            } catch (Exception exception) {
                plugin.getLogger().warning("Passive transaction failed for " + uuid + ": " + exception.getMessage());
                return false;
            }
        }
        PlayerData data = profile(uuid, name);
        String previous = data.temporaryPassiveUpgrades;
        int previousCoins = data.coins;
        data.temporaryPassiveUpgrades = updated;
        data.coins = updatedCoins;
        boolean saved = saveFallbackState(uuid, prestigeState(uuid), null);
        data.temporaryPassiveUpgrades = previous;
        data.coins = previousCoins;
        return saved;
    }

    public void applyPassivePurchase(UUID uuid, String data, int price) {
        PlayerData profile = profile(uuid, "");
        profile.temporaryPassiveUpgrades = data == null ? "" : data;
        profile.coins = Math.max(0, profile.coins - Math.max(0, price));
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) refreshPrestigeMenu(player);
    }

    private Map<String, Integer> parsePassiveData(String raw) {
        Map<String, Integer> values = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return values;
        for (String entry : raw.split(",")) {
            int cut = entry.indexOf('=');
            if (cut <= 0) continue;
            try {
                values.put(normalizePassive(entry.substring(0, cut)), Math.max(0, Math.min(6, Integer.parseInt(entry.substring(cut + 1)))));
            } catch (NumberFormatException ignored) { }
        }
        return values;
    }

    private String serializePassiveData(Map<String, Integer> values) {
        List<String> output = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : values.entrySet()) output.add(entry.getKey() + "=" + entry.getValue());
        return String.join(",", output);
    }

    private String normalizePassive(String id) {
        return id == null ? "" : id.trim().toLowerCase(java.util.Locale.ENGLISH).replace('_', '-').replace(' ', '-');
    }

    private String normalizeUpgrade(String id) {
        return normalizePassive(id);
    }

    private void applyElGato(Player killer, int streak) {
        if (plugin.getPassiveManager() == null) return;
        int tier = plugin.getPassiveManager().tier(killer.getUniqueId(), "el-gato");
        if (streak < 1 || streak > tier) return;
        me.outerspace.thepit.perk.PassiveDefinition definition = plugin.getPassiveManager().definition("el-gato");
        if (definition == null) return;
        if (definition.bonusGold() > 0) plugin.awardPitGold(killer, definition.bonusGold(), ThePitPlugin.GoldSource.EL_GATO);
        if (definition.bonusXp() > 0) addXp(killer, definition.bonusXp(), XpSource.EL_GATO);
    }

    public boolean resetPrestigeCooldown(OfflinePlayer player) {
        if (player == null) return false;
        PlayerData data = ensureLoaded(player);
        data.lastPrestige = 0L;
        persistPrestigeFields(player.getUniqueId(), safeName(player), data);
        return true;
    }

    public synchronized boolean changeRenown(OfflinePlayer player, long amount, RenownMode mode) {
        if (player == null || amount < 0L) return false;
        PlayerData data = ensureLoaded(player);
        long next;
        if (mode == RenownMode.SET) next = amount;
        else if (mode == RenownMode.TAKE) {
            if (data.renown < amount) return false;
            next = data.renown - amount;
        } else next = safeLongAdd(data.renown, amount);
        if (next < 0L) return false;
        data.renown = next;
        persistPrestigeFields(player.getUniqueId(), safeName(player), data);
        return true;
    }

    public PrestigeState prestigeState(UUID uuid) {
        PlayerData data = profile(uuid, "");
        return new PrestigeState(data.prestige, data.level, data.currentXp, data.lifetimeXp,
                data.renown, data.lastPrestige, data.renownUpgrades, data.temporaryPassiveUpgrades, data.coins, isProfileLoaded(uuid));
    }

    public boolean persistPrestigeTransaction(UUID uuid, String name, PrestigeState expected, int nextPrestige,
                                              long nextRenown, long timestamp, long requiredCoins,
                                              boolean consumeCurrency, boolean bypassCooldown, String perkData) {
        if (!awaitProfileWrites(uuid, 10000L)) return false;
        PrestigeState current = prestigeState(uuid);
        if (current.prestige() != expected.prestige() || current.level() != expected.level()
                || current.coins() != expected.coins() || current.renown() != expected.renown()
                || current.lastPrestige() != expected.lastPrestige()) return false;
        long cooldownMillis = Math.max(0L, plugin.registry().settings().getLong("prestige.cooldown-seconds", 7200L)) * 1000L;
        if (expected.level() != 120 || nextPrestige != expected.prestige() + 1
                || expected.prestige() >= getMaxPrestige() || requiredCoins < 0L
                || expected.coins() < requiredCoins
                || !bypassCooldown && timestamp - expected.lastPrestige() < cooldownMillis) return false;
        boolean resetKillstreak = plugin.registry().settings().getBoolean("prestige.reset.killstreak", true);
        int nextLevel = 1;
        int nextXp = 0;
        int nextCoins = consumeCurrency ? expected.coins() - (int) requiredCoins : expected.coins();
        String nextPassiveUpgrades = shouldResetPassives() ? "" : expected.temporaryPassiveUpgrades();
        if (plugin.getStorageManager().isMysql()) {
            synchronized (plugin.getStorageManager()) {
                Connection connection = null;
                boolean previousAutoCommit = true;
                try {
                    connection = plugin.getStorageManager().connection();
                    previousAutoCommit = connection.getAutoCommit();
                    connection.setAutoCommit(false);
                    try (PreparedStatement statement = connection.prepareStatement("SELECT prestige,level,coins,renown,last_prestige FROM buildffa_stats WHERE uuid=? AND period=? AND period_key=? FOR UPDATE")) {
                        statement.setString(1, uuid.toString());
                        statement.setString(2, Period.LIFETIME.name());
                        statement.setString(3, "all");
                        try (ResultSet result = statement.executeQuery()) {
                            if (result.next() && (result.getInt("prestige") != expected.prestige()
                                    || result.getInt("level") != expected.level()
                                    || result.getInt("coins") != expected.coins()
                                    || result.getLong("renown") != expected.renown()
                                    || result.getLong("last_prestige") != expected.lastPrestige())) {
                                connection.rollback();
                                return false;
                            }
                        }
                    }
                    String resetFields = resetKillstreak ? ",killstreak=0" : "";
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO buildffa_stats(uuid,name,period,period_key,prestige,level,current_xp,lifetime_xp,coins,renown,last_prestige,renown_upgrades,temporary_passive_upgrades,killstreak,prestige_migrated,pit_migrated) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,0,1,1) ON DUPLICATE KEY UPDATE name=VALUES(name),prestige=VALUES(prestige),level=VALUES(level),current_xp=VALUES(current_xp),coins=VALUES(coins),renown=VALUES(renown),last_prestige=VALUES(last_prestige),renown_upgrades=VALUES(renown_upgrades),temporary_passive_upgrades=VALUES(temporary_passive_upgrades),prestige_migrated=1,pit_migrated=1" + resetFields)) {
                        statement.setString(1, uuid.toString());
                        statement.setString(2, name);
                        statement.setString(3, Period.LIFETIME.name());
                        statement.setString(4, "all");
                        statement.setInt(5, nextPrestige);
                        statement.setInt(6, nextLevel);
                        statement.setInt(7, nextXp);
                        statement.setLong(8, expected.lifetimeXp());
                        statement.setInt(9, nextCoins);
                        statement.setLong(10, nextRenown);
                        statement.setLong(11, timestamp);
                        statement.setString(12, expected.renownUpgrades());
                        statement.setString(13, nextPassiveUpgrades);
                        statement.executeUpdate();
                    }
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO buildffa_perk_profiles(uuid,profile_data) VALUES(?,?) ON DUPLICATE KEY UPDATE profile_data=VALUES(profile_data)")) {
                        statement.setString(1, uuid.toString());
                        statement.setString(2, perkData);
                        statement.executeUpdate();
                    }
                    connection.commit();
                    return true;
                } catch (Exception exception) {
                    try { if (connection != null) connection.rollback(); } catch (Exception ignored) { }
                    plugin.getLogger().warning("Prestige transaction failed for " + uuid + ": " + exception.getMessage());
                    return false;
                } finally {
                    try { if (connection != null) connection.setAutoCommit(previousAutoCommit); } catch (Exception ignored) { }
                }
            }
        }
        if (plugin.getStorageManager().isMongo()) {
            Map<String, Object> fields = new java.util.LinkedHashMap<>();
            fields.put("prestige", nextPrestige);
            fields.put("level", nextLevel);
            fields.put("current_xp", nextXp);
            fields.put("coins", nextCoins);
            fields.put("renown", nextRenown);
            fields.put("last_prestige", timestamp);
            fields.put("renown_upgrades", expected.renownUpgrades());
            fields.put("temporary_passive_upgrades", nextPassiveUpgrades);
            if (resetKillstreak) fields.put("killstreak", 0);
            fields.put("prestige_migrated", 1);
            fields.put("pit_migrated", 1);
            fields.put("perk_profile_data", perkData);
            Map<String, Object> expectedFields = new java.util.LinkedHashMap<>();
            expectedFields.put("prestige", expected.prestige());
            expectedFields.put("level", expected.level());
            expectedFields.put("coins", expected.coins());
            expectedFields.put("renown", expected.renown());
            expectedFields.put("last_prestige", expected.lastPrestige());
            return plugin.getStorageManager().setMongoFieldsConditional(uuid.toString(), name, Period.LIFETIME.name(), "all", expectedFields, fields);
        }
        PrestigeState next = new PrestigeState(nextPrestige, nextLevel, nextXp, expected.lifetimeXp(),
                nextRenown, timestamp, expected.renownUpgrades(), nextPassiveUpgrades, nextCoins, true);
        return saveFallbackState(uuid, next, perkData);
    }

    public synchronized String fallbackPerkData(UUID uuid) {
        return prestigeFallback.getString("players." + uuid + ".perk-data", "");
    }

    public synchronized boolean persistFallbackPerkData(UUID uuid, String data) {
        prestigeFallback.set("players." + uuid + ".perk-data", data == null ? "" : data);
        return savePrestigeFallback();
    }

    public void applyPrestigeCommit(UUID uuid, int prestige, long renown, long timestamp, long requiredCoins, boolean consumeCurrency) {
        PlayerData data = profile(uuid, "");
        profileLoadGenerations.computeIfAbsent(uuid, ignored -> new AtomicInteger()).incrementAndGet();
        Player activePlayer = Bukkit.getPlayer(uuid);
        if (activePlayer != null && plugin.getKillstreakManager() != null) plugin.getKillstreakManager().cleanup(activePlayer, false);
        int oldLevel = data.level;
        long oldRenown = data.renown;
        data.prestige = prestige;
        data.level = 1;
        data.currentXp = 0;
        if (consumeCurrency) data.coins = Math.max(0, data.coins - (int) requiredCoins);
        data.renown = renown;
        data.lastPrestige = timestamp;
        if (shouldResetPassives()) data.temporaryPassiveUpgrades = "";
        if (plugin.registry().settings().getBoolean("prestige.reset.killstreak", true)) data.killstreak = 0;
        if (plugin.registry().settings().getBoolean("prestige.reset.bounty", true)) bounties.remove(uuid);
        if (plugin.getContractManager() != null) plugin.getContractManager().clearForPrestige(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && data.level != oldLevel) fireLevelChange(player, oldLevel, data.level, 0L, XpSource.PRESTIGE_RESET, false);
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(uuid,
                    AchievementManager.EventType.PRESTIGE_CHANGED, prestige, Collections.emptyMap(), false));
            if (renown > oldRenown) plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(uuid,
                    AchievementManager.EventType.RENOWN_EARNED, renown - oldRenown, Collections.emptyMap(), false));
        }
    }

    public enum RenownMode { GIVE, TAKE, SET }

    public enum RenownPurchaseStatus {
        SUCCESS,
        PROFILE_NOT_LOADED,
        PRESTIGE_LOCKED,
        INSUFFICIENT_RENOWN,
        ALREADY_PURCHASED,
        TIER_MISMATCH,
        CONFLICT,
        STORAGE_FAILURE
    }

    public record RenownPurchaseResult(RenownPurchaseStatus status, long balance, String upgrades) {}

    public record PrestigeState(int prestige, int level, int currentXp, long lifetimeXp,
                                long renown, long lastPrestige, String renownUpgrades, String temporaryPassiveUpgrades,
                                int coins, boolean loaded) {}

    public String getPrestigeIcon(UUID uuid) {
        return getPrestige(uuid) <= 0 ? "" : getPrestigeFormatted(uuid);
    }

    public boolean canPrestige(Player player) {
        if (player == null || !isProfileLoaded(player.getUniqueId())) return false;
        PlayerData data = profile(player.getUniqueId(), player.getName());
        return data.level == 120 && data.coins >= getPrestigeCoinsRequired(player.getUniqueId())
                && !isMaxPrestige(player.getUniqueId()) && getPrestigeCooldownRemaining(player) <= 0L;
    }

    public boolean setPrestige(OfflinePlayer player, int prestige) {
        if (player == null || prestige < 0 || prestige > getMaxPrestige()) return false;
        PlayerData data = ensureLoaded(player);
        data.prestige = prestige;
        persistProgress(player.getUniqueId(), safeName(player), data);
        revalidateOnline(player);
        return true;
    }

    public boolean takeCoins(Player player, int amount) {
        if (player == null || amount < 0) return false;
        if (plugin.getPrestigeManager().isProcessing(player.getUniqueId())) return false;
        PlayerData data = profile(player.getUniqueId(), player.getName());
        if (data.coins < amount) return false;
        data.coins -= amount;
        persistDelta(player, 0, 0, -amount, 0);
        refreshPrestigeMenu(player);
        return true;
    }

    public void addCoins(Player player, int amount) {
        if (player == null || amount == 0) return;
        PlayerData data = profile(player.getUniqueId(), player.getName());
        data.coins = Math.max(0, safeIntAdd(data.coins, amount));
        persistDelta(player, 0, 0, amount, 0);
        refreshPrestigeMenu(player);
    }

    public void setCoins(Player player, int amount) {
        if (player == null || amount < 0) return;
        PlayerData data = profile(player.getUniqueId(), player.getName());
        int delta = amount - data.coins;
        data.coins = amount;
        persistDelta(player, 0, 0, delta, 0);
        refreshPrestigeMenu(player);
    }

    public void applyPersistedCoins(Player player, int amount) {
        if (player == null || amount < 0 || !isProfileLoaded(player.getUniqueId())) return;
        profile(player.getUniqueId(), player.getName()).coins = amount;
        refreshPrestigeMenu(player);
        plugin.getBuildScoreboardManager().update(player);
    }

    private void refreshPrestigeMenu(Player player) {
        Runnable refresh = () -> plugin.getPrestigeManager().refreshIfOpen(player);
        if (Bukkit.isPrimaryThread()) refresh.run();
        else Bukkit.getScheduler().runTask(plugin, refresh);
    }

    public void persistFallbackProfile(UUID uuid) {
        if (plugin.getStorageManager().isMysql() || plugin.getStorageManager().isMongo()) return;
        PrestigeState state = prestigeState(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveFallbackState(uuid, state, null));
    }

    public int addRewardCoins(Player player, int baseAmount) {
        int amount = applyCoinMultiplier(player, Math.max(0, baseAmount));
        addCoins(player, amount);
        return amount;
    }

    public void addKillstreakProgress(Player player, int amount) {
        if (player == null || amount <= 0) return;
        PlayerData data = profile(player.getUniqueId(), player.getName());
        data.killstreak = safeIntAdd(data.killstreak, amount);
        if (data.killstreak > getHighestStreak(player.getUniqueId())) setLongStat(player.getUniqueId(), HIGHEST_STREAK, data.killstreak);
        persistDelta(player, 0, 0, 0, amount);
    }

    public int getBounty(UUID uuid) {
        return bounties.getOrDefault(uuid, 0);
    }

    public boolean setBountyIfHigher(Player player, int requestedValue) {
        if (player == null || requestedValue <= 0) return false;
        UUID uuid = player.getUniqueId();
        int current = getBounty(uuid);
        if (requestedValue <= current) return false;
        bounties.put(uuid, requestedValue);
        persistBounty(uuid, requestedValue);
        if (plugin.getTablistManager() != null) plugin.getTablistManager().refreshBounty(player);
        if (plugin.getBuildScoreboardManager() != null) plugin.getBuildScoreboardManager().update(player);
        return true;
    }

    public boolean resetPlayer(OfflinePlayer player) {
        return player != null && resetPlayer(player.getUniqueId());
    }

    public boolean resetPlayer(UUID uuid) {
        if (uuid == null) return false;
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && plugin.getKillstreakManager() != null) plugin.getKillstreakManager().cleanup(online, false);
        profiles.put(uuid, new PlayerData(""));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getStorageManager().isMongo()) plugin.getStorageManager().deleteMongoPlayer(uuid.toString());
            if (plugin.getStorageManager().isMysql()) {
                try (PreparedStatement statement = plugin.getStorageManager().prepare("DELETE FROM buildffa_stats WHERE uuid=?")) {
                    statement.setString(1, uuid.toString());
                    statement.executeUpdate();
                } catch (Exception exception) {
                    plugin.getLogger().warning("MySQL stat reset failed: " + exception.getMessage());
                }
                try (PreparedStatement statement = plugin.getStorageManager().prepare("DELETE FROM buildffa_player_statistics WHERE uuid=?")) {
                    statement.setString(1, uuid.toString());
                    statement.executeUpdate();
                } catch (Exception exception) {
                    plugin.getLogger().warning("MySQL extended stat reset failed: " + exception.getMessage());
                }
            }
        });
        return true;
    }

    public List<String> top(StatType type, Period period, int limit) {
        if (type != StatType.KILLS && type != StatType.LEVEL && type != StatType.XP && type != StatType.PRESTIGE) return Collections.emptyList();
        Period effectivePeriod = type == StatType.KILLS ? period : Period.LIFETIME;
        if (plugin.getStorageManager().isMongo()) return topMongo(type, effectivePeriod, limit);
        if (plugin.getStorageManager().isMysql()) return topMysql(type, effectivePeriod, limit);
        List<Map.Entry<String, Long>> values = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> entry : profiles.entrySet()) {
            PlayerData data = entry.getValue();
            values.add(new AbstractMap.SimpleEntry<>(entry.getKey() + "|" + data.name + "|" + data.level + "|" + data.prestige, leaderboardValue(type, data)));
        }
        values.sort(Comparator.comparingLong((Map.Entry<String, Long> value) -> value.getValue()).reversed());
        return formatTop(type, values, limit);
    }

    public String topLine(StatType type, Period period, int position) {
        List<String> lines = top(type, period, position);
        return position >= 1 && lines.size() >= position ? lines.get(position - 1) : "&7" + position + ". &fNobody &8- &70";
    }

    private PlayerData load(UUID uuid, String name) {
        PlayerData data = new PlayerData(name);
        if (plugin.getStorageManager().isMongo()) {
            data.kills = plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "kills", 0);
            data.deaths = plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "deaths", 0);
            data.coins = plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "coins", 0);
            data.killstreak = plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "killstreak", 0);
            data.prestige = plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "prestige", 0);
            data.prestige = Math.max(0, Math.min(getMaxPrestige(), data.prestige));
            data.level = plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "level", minimumLevel());
            data.currentXp = plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "current_xp", 0);
            data.lifetimeXp = plugin.getStorageManager().getMongoLong(uuid.toString(), Period.LIFETIME.name(), "all", "lifetime_xp", 0L);
            data.renown = Math.max(0L, plugin.getStorageManager().getMongoLong(uuid.toString(), Period.LIFETIME.name(), "all", "renown", 0L));
            data.lastPrestige = Math.max(0L, plugin.getStorageManager().getMongoLong(uuid.toString(), Period.LIFETIME.name(), "all", "last_prestige", 0L));
            data.lastPitLogin = Math.max(0L, plugin.getStorageManager().getMongoLong(uuid.toString(), Period.LIFETIME.name(), "all", "last_buildffa_login", 0L));
            data.existingProfile = data.lastPitLogin > 0L || data.level != minimumLevel() || data.prestige > 0
                    || data.currentXp > 0 || data.lifetimeXp > 0L || data.kills > 0 || data.deaths > 0 || data.coins > 0;
            data.renownUpgrades = plugin.getStorageManager().getMongoString(uuid.toString(), Period.LIFETIME.name(), "all", "renown_upgrades", "");
            data.temporaryPassiveUpgrades = plugin.getStorageManager().getMongoString(uuid.toString(), Period.LIFETIME.name(), "all", "temporary_passive_upgrades", "");
            loadExtendedMongo(uuid, data);
            int migrated = plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "pit_migrated", 0);
            if (migrated == 0) migrate(uuid, name, data);
            migratePrestige(uuid, name, data, plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "prestige_migrated", 0));
            inferStatsUnlock(uuid, data);
            return data;
        }
        if (plugin.getStorageManager().isMysql()) {
            try (PreparedStatement statement = plugin.getStorageManager().prepare("SELECT kills,deaths,coins,killstreak,prestige,level,current_xp,lifetime_xp,renown,last_prestige,last_buildffa_login,renown_upgrades,temporary_passive_upgrades,prestige_migrated,pit_migrated,rating FROM buildffa_stats WHERE uuid=? AND period=? AND period_key=?")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, Period.LIFETIME.name());
                statement.setString(3, "all");
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        data.existingProfile = true;
                        data.kills = result.getInt("kills");
                        data.deaths = result.getInt("deaths");
                        data.coins = result.getInt("coins");
                        data.killstreak = result.getInt("killstreak");
                        data.prestige = Math.max(0, Math.min(getMaxPrestige(), result.getInt("prestige")));
                        data.level = Math.max(minimumLevel(), result.getInt("level"));
                        data.currentXp = result.getInt("current_xp");
                        data.lifetimeXp = result.getLong("lifetime_xp");
                        data.renown = Math.max(0L, result.getLong("renown"));
                        data.lastPrestige = Math.max(0L, result.getLong("last_prestige"));
                        data.lastPitLogin = Math.max(0L, result.getLong("last_buildffa_login"));
                        data.renownUpgrades = result.getString("renown_upgrades") == null ? "" : result.getString("renown_upgrades");
                        data.temporaryPassiveUpgrades = result.getString("temporary_passive_upgrades") == null ? "" : result.getString("temporary_passive_upgrades");
                        loadExtendedMysql(uuid, data);
                        if (result.getInt("pit_migrated") == 0) {
                            migrateFromLegacyValue(data, result.getInt("rating"));
                            persistProgressNow(uuid, name, data);
                        }
                        migratePrestige(uuid, name, data, result.getInt("prestige_migrated"));
                        inferStatsUnlock(uuid, data);
                        return data;
                    }
                }
            } catch (Exception exception) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Could not load The Pit player progression for " + name + " (" + uuid + ").", exception);
                data.loadFailed = true;
                return data;
            }
            persistProgressNow(uuid, name, data);
            migratePrestige(uuid, name, data, 0);
        }
        return data;
    }

    private PlayerData loadFallback(UUID uuid, String name) {
        PlayerData data = new PlayerData(name);
        String raw = prestigeFallback.getString("players." + uuid + ".state", "");
        if (raw.isEmpty()) return data;
        data.existingProfile = true;
        String[] values = raw.split("\\|", -1);
        try {
            if (values.length > 0) data.prestige = Math.max(0, Math.min(getMaxPrestige(), Integer.parseInt(values[0])));
            if (values.length > 1) data.level = Math.max(minimumLevel(), Math.min(maximumLevel(), Integer.parseInt(values[1])));
            if (values.length > 2) data.currentXp = Math.max(0, Integer.parseInt(values[2]));
            if (values.length > 3) data.lifetimeXp = Math.max(0L, Long.parseLong(values[3]));
            if (values.length > 5) data.renown = Math.max(0L, Long.parseLong(values[5]));
            if (values.length > 6) data.lastPrestige = Math.max(0L, Long.parseLong(values[6]));
            if (values.length > 7) data.renownUpgrades = values[7];
            if (values.length > 8) data.temporaryPassiveUpgrades = values[8];
            if (values.length > 9) data.coins = Math.max(0, Integer.parseInt(values[9]));
        } catch (NumberFormatException ignored) { }
        deserializeExtended(prestigeFallback.getString("players." + uuid + ".statistics", ""), data);
        data.lastPitLogin = Math.max(0L, prestigeFallback.getLong("players." + uuid + ".last-buildffa-login", 0L));
        if (prestigeFallback.getBoolean("players." + uuid + ".stats-menu-unlocked", false)) data.longStats.put(STATS_MENU_UNLOCKED, 1L);
        inferStatsUnlock(uuid, data);
        return data;
    }

    private synchronized boolean saveFallbackState(UUID uuid, PrestigeState state, String perkData) {
        prestigeFallback.set("players." + uuid + ".state", serializeFallback(state));
        if (perkData != null) prestigeFallback.set("players." + uuid + ".perk-data", perkData);
        PlayerData data = profiles.get(uuid);
        if (data != null) {
            prestigeFallback.set("players." + uuid + ".statistics", serializeExtended(data));
            prestigeFallback.set("players." + uuid + ".stats-menu-unlocked", data.longStats.getOrDefault(STATS_MENU_UNLOCKED, 0L) > 0L);
        }
        return savePrestigeFallback();
    }

    private String serializeFallback(PrestigeState state) {
        return state.prestige() + "|" + state.level() + "|" + state.currentXp() + "|" + state.lifetimeXp() + "|"
                + "0|" + state.renown() + "|" + state.lastPrestige() + "|" + state.renownUpgrades() + "|" + state.temporaryPassiveUpgrades() + "|" + state.coins();
    }

    private boolean savePrestigeFallback() {
        try {
            prestigeFallbackFile.getParentFile().mkdirs();
            File temporary = new File(prestigeFallbackFile.getParentFile(), prestigeFallbackFile.getName() + ".tmp");
            prestigeFallback.save(temporary);
            try { Files.move(temporary.toPath(), prestigeFallbackFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary.toPath(), prestigeFallbackFile.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not save prestige fallback profiles: " + exception.getMessage());
            return false;
        }
    }

    public synchronized Optional<String> readInventoryFallback(UUID uuid) {
        String path = "players." + uuid + ".gameplay-inventory";
        return prestigeFallback.contains(path) ? Optional.ofNullable(prestigeFallback.getString(path, "")) : Optional.empty();
    }

    public synchronized boolean writeInventoryFallback(UUID uuid, String value) {
        prestigeFallback.set("players." + uuid + ".gameplay-inventory", value);
        return savePrestigeFallback();
    }

    private void migrate(UUID uuid, String name, PlayerData data) {
        int legacy = plugin.getStorageManager().getMongoInt(uuid.toString(), Period.LIFETIME.name(), "all", "rating", 0);
        migrateFromLegacyValue(data, legacy);
        plugin.getStorageManager().setMongoStat(uuid.toString(), name, Period.LIFETIME.name(), "all", "level", data.level);
        plugin.getStorageManager().setMongoStat(uuid.toString(), name, Period.LIFETIME.name(), "all", "current_xp", data.currentXp);
        plugin.getStorageManager().setMongoLong(uuid.toString(), name, Period.LIFETIME.name(), "all", "lifetime_xp", data.lifetimeXp);
        plugin.getStorageManager().setMongoStat(uuid.toString(), name, Period.LIFETIME.name(), "all", "pit_migrated", 1);
    }

    private void migrateFromLegacyValue(PlayerData data, int legacyValue) {
        data.level = minimumLevel();
        data.currentXp = 0;
        data.lifetimeXp = 0L;
        if (!plugin.registry().settings().getBoolean("levels.migration.convert-rating-to-xp", false)) return;
        double rate = Math.max(0.0D, plugin.registry().settings().getDouble("levels.migration.xp-per-rating", 1.0D));
        long converted = Math.max(0L, Math.round(legacyValue * rate));
        data.lifetimeXp = converted;
        data.currentXp = converted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) converted;
        normalizeLevel(data);
    }

    private void migratePrestige(UUID uuid, String name, PlayerData data, int migrated) {
        if (migrated != 0) return;
        data.prestige = Math.max(0, Math.min(getMaxPrestige(), data.prestige));
        if (plugin.registry().settings().getBoolean("prestige.migration.award-historical-renown", false) && data.renown == 0L) {
            for (int value = 1; value <= data.prestige; value++) data.renown = safeLongAdd(data.renown, getRenownReward(value));
        }
        persistPrestigeFieldsNow(uuid, name, data, 1);
    }

    private void persistDelta(Player player, int kills, int deaths, int coins, int streak) {
        UUID playerUuid = player.getUniqueId();
        String uuid = playerUuid.toString();
        String name = player.getName();
        String prefix = plugin.getNetworkPrefix(player);
        pendingProfileWrites.computeIfAbsent(playerUuid, ignored -> new AtomicInteger()).incrementAndGet();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (!plugin.getStorageManager().isMysql() && !plugin.getStorageManager().isMongo()) {
                    if (!plugin.getPrestigeManager().isProcessing(playerUuid)) saveFallbackState(playerUuid, prestigeState(playerUuid), null);
                }
                for (Period period : Period.values()) {
                    String key = periodKey(period);
                    if (plugin.getStorageManager().isMongo()) {
                        plugin.getStorageManager().addMongoStats(uuid, name, prefix, period.name(), key, kills, deaths, coins, streak);
                    } else if (plugin.getStorageManager().isMysql()) {
                        try (PreparedStatement statement = plugin.getStorageManager().prepare("INSERT INTO buildffa_stats(uuid,name,prefix,period,period_key,kills,deaths,coins,killstreak) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),prefix=VALUES(prefix),kills=kills+VALUES(kills),deaths=deaths+VALUES(deaths),coins=GREATEST(0,coins+VALUES(coins)),killstreak=IF(? < 0,0,killstreak+?)")) {
                            statement.setString(1, uuid);
                            statement.setString(2, name);
                            statement.setString(3, prefix);
                            statement.setString(4, period.name());
                            statement.setString(5, key);
                            statement.setInt(6, kills);
                            statement.setInt(7, deaths);
                            statement.setInt(8, coins);
                            statement.setInt(9, Math.max(0, streak));
                            statement.setInt(10, streak);
                            statement.setInt(11, Math.max(0, streak));
                            statement.executeUpdate();
                        } catch (Exception exception) {
                            plugin.getLogger().warning("MySQL stat update failed: " + exception.getMessage());
                        }
                    }
                }
            } finally {
                AtomicInteger pending = pendingProfileWrites.get(playerUuid);
                if (pending != null && pending.decrementAndGet() <= 0) {
                    pendingProfileWrites.remove(playerUuid, pending);
                    Object monitor = profileWriteMonitors.get(playerUuid);
                    if (monitor != null) synchronized (monitor) { monitor.notifyAll(); }
                }
            }
        });
    }

    private boolean awaitProfileWrites(UUID uuid, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + Math.max(0L, timeoutMillis);
        Object monitor = profileWriteMonitors.computeIfAbsent(uuid, ignored -> new Object());
        try {
            synchronized (monitor) {
                while (pendingProfileWrites.getOrDefault(uuid, new AtomicInteger()).get() > 0) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0L) return false;
                    monitor.wait(Math.min(remaining, 250L));
                }
            }
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            profileWriteMonitors.remove(uuid, monitor);
        }
    }

    public boolean awaitPendingPitGoldWrites(UUID uuid, long timeoutMillis) {
        return uuid != null && awaitProfileWrites(uuid, timeoutMillis);
    }

    private void persistProgress(UUID uuid, String name, PlayerData data) {
        int level = data.level;
        int xp = data.currentXp;
        long lifetime = data.lifetimeXp;
        int prestige = data.prestige;
        if (!plugin.getStorageManager().isMysql() && !plugin.getStorageManager().isMongo()) {
            PrestigeState state = prestigeState(uuid);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveFallbackState(uuid, state, null));
        } else Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> persistProgressNow(uuid, name, level, xp, lifetime, prestige));
    }

    private void persistProgressNow(UUID uuid, String name, PlayerData data) {
        persistProgressNow(uuid, name, data.level, data.currentXp, data.lifetimeXp, data.prestige);
    }

    private void persistProgressNow(UUID uuid, String name, int level, int xp, long lifetime, int prestige) {
        if (plugin.getStorageManager().isMongo()) {
            plugin.getStorageManager().setMongoStat(uuid.toString(), name, Period.LIFETIME.name(), "all", "level", level);
            plugin.getStorageManager().setMongoStat(uuid.toString(), name, Period.LIFETIME.name(), "all", "current_xp", xp);
            plugin.getStorageManager().setMongoLong(uuid.toString(), name, Period.LIFETIME.name(), "all", "lifetime_xp", lifetime);
            plugin.getStorageManager().setMongoStat(uuid.toString(), name, Period.LIFETIME.name(), "all", "prestige", prestige);
            plugin.getStorageManager().setMongoStat(uuid.toString(), name, Period.LIFETIME.name(), "all", "pit_migrated", 1);
        } else if (plugin.getStorageManager().isMysql()) {
            try (PreparedStatement statement = plugin.getStorageManager().prepare("INSERT INTO buildffa_stats(uuid,name,period,period_key,level,current_xp,lifetime_xp,prestige,pit_migrated) VALUES(?,?,?,?,?,?,?,?,1) ON DUPLICATE KEY UPDATE name=VALUES(name),level=VALUES(level),current_xp=VALUES(current_xp),lifetime_xp=VALUES(lifetime_xp),prestige=VALUES(prestige),pit_migrated=1")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.setString(3, Period.LIFETIME.name());
                statement.setString(4, "all");
                statement.setInt(5, level);
                statement.setInt(6, xp);
                statement.setLong(7, lifetime);
                statement.setInt(8, prestige);
                statement.executeUpdate();
            } catch (Exception exception) {
                plugin.getLogger().warning("MySQL progression update failed: " + exception.getMessage());
            }
        }
    }

    private void persistPrestigeFields(UUID uuid, String name, PlayerData data) {
        PrestigeState state = new PrestigeState(data.prestige, data.level, data.currentXp, data.lifetimeXp,
                data.renown, data.lastPrestige, data.renownUpgrades, data.temporaryPassiveUpgrades, data.coins, true);
        if (!plugin.getStorageManager().isMysql() && !plugin.getStorageManager().isMongo()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveFallbackState(uuid, state, null));
        } else Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> persistPrestigeFieldsNow(uuid, name, state, 1));
    }

    private void persistPrestigeFieldsNow(UUID uuid, String name, PlayerData data, int migrated) {
        PrestigeState state = new PrestigeState(data.prestige, data.level, data.currentXp, data.lifetimeXp,
                data.renown, data.lastPrestige, data.renownUpgrades, data.temporaryPassiveUpgrades, data.coins, true);
        persistPrestigeFieldsNow(uuid, name, state, migrated);
    }

    private void persistPrestigeFieldsNow(UUID uuid, String name, PrestigeState state, int migrated) {
        if (plugin.getStorageManager().isMongo()) {
            Map<String, Object> fields = new java.util.LinkedHashMap<>();
            fields.put("prestige", state.prestige());
            fields.put("renown", state.renown());
            fields.put("last_prestige", state.lastPrestige());
            fields.put("renown_upgrades", state.renownUpgrades());
            fields.put("temporary_passive_upgrades", state.temporaryPassiveUpgrades());
            fields.put("prestige_migrated", migrated);
            plugin.getStorageManager().setMongoFields(uuid.toString(), name, Period.LIFETIME.name(), "all", fields);
        } else if (plugin.getStorageManager().isMysql()) {
            try (PreparedStatement statement = plugin.getStorageManager().prepare("INSERT INTO buildffa_stats(uuid,name,period,period_key,prestige,renown,last_prestige,renown_upgrades,temporary_passive_upgrades,prestige_migrated) VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),prestige=VALUES(prestige),renown=VALUES(renown),last_prestige=VALUES(last_prestige),renown_upgrades=VALUES(renown_upgrades),temporary_passive_upgrades=VALUES(temporary_passive_upgrades),prestige_migrated=VALUES(prestige_migrated)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.setString(3, Period.LIFETIME.name());
                statement.setString(4, "all");
                statement.setInt(5, state.prestige());
                statement.setLong(6, state.renown());
                statement.setLong(7, state.lastPrestige());
                statement.setString(8, state.renownUpgrades());
                statement.setString(9, state.temporaryPassiveUpgrades());
                statement.setInt(10, migrated);
                statement.executeUpdate();
            } catch (Exception exception) {
                plugin.getLogger().warning("MySQL prestige progress update failed: " + exception.getMessage());
            }
        }
    }

    private void loadExtendedMysql(UUID uuid, PlayerData data) {
        try (PreparedStatement statement = plugin.getStorageManager().prepare("SELECT statistic_id,long_value,double_value FROM buildffa_player_statistics WHERE uuid=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String id = result.getString("statistic_id");
                    long longValue = result.getLong("long_value");
                    double doubleValue = result.getDouble("double_value");
                    if (doubleValue != 0.0D) data.doubleStats.put(id, doubleValue);
                    if (longValue != 0L) data.longStats.put(id, longValue);
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not load extended statistics for " + uuid + ": " + exception.getMessage());
        }
    }

    private void loadExtendedMongo(UUID uuid, PlayerData data) {
        deserializeExtended(plugin.getStorageManager().getMongoString(uuid.toString(), Period.LIFETIME.name(), "all", "pit_statistics", ""), data);
    }

    private String serializeExtended(PlayerData data) {
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, Long> entry : data.longStats.entrySet()) {
            if (entry.getValue() != 0L) values.add(entry.getKey() + ":L:" + entry.getValue());
        }
        for (Map.Entry<String, Double> entry : data.doubleStats.entrySet()) {
            if (entry.getValue() != 0.0D) values.add(entry.getKey() + ":D:" + entry.getValue());
        }
        return String.join(",", values);
    }

    private void deserializeExtended(String raw, PlayerData data) {
        if (raw == null || raw.isEmpty()) return;
        for (String value : raw.split(",")) {
            String[] parts = value.split(":", 3);
            if (parts.length != 3) continue;
            try {
                if ("L".equals(parts[1])) data.longStats.put(parts[0], Math.max(0L, Long.parseLong(parts[2])));
                else if ("D".equals(parts[1])) data.doubleStats.put(parts[0], Math.max(0.0D, Double.parseDouble(parts[2])));
            } catch (NumberFormatException ignored) { }
        }
    }

    public void flushPlayer(UUID uuid, boolean unload) {
        PlayerData data = profiles.get(uuid);
        if (data == null) return;
        capturePlaytime(data);
        dirtyStatistics.remove(uuid);
        String name = data.name == null || data.name.isEmpty() ? uuid.toString().substring(0, 16) : data.name;
        Map<String, Long> longs = new LinkedHashMap<>(data.longStats);
        Map<String, Double> doubles = new LinkedHashMap<>(data.doubleStats);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> persistExtended(uuid, name, longs, doubles));
        if (unload) {
            profiles.remove(uuid);
            loadedProfiles.remove(uuid);
            recentKills.remove(uuid);
            bounties.remove(uuid);
        }
    }

    private void capturePlaytime(PlayerData data) {
        if (data.sessionStarted <= 0L) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - data.sessionStarted);
        data.longStats.put(PLAYTIME_MILLIS, safeLongAdd(data.longStats.getOrDefault(PLAYTIME_MILLIS, 0L), elapsed));
        data.sessionStarted = System.currentTimeMillis();
    }

    private void flushDirtyNow() {
        for (UUID uuid : new ArrayList<>(dirtyStatistics)) {
            PlayerData data = profiles.get(uuid);
            if (data == null || !dirtyStatistics.remove(uuid)) continue;
            capturePlaytime(data);
            String name = data.name == null || data.name.isEmpty() ? uuid.toString().substring(0, 16) : data.name;
            persistExtended(uuid, name, new LinkedHashMap<>(data.longStats), new LinkedHashMap<>(data.doubleStats));
        }
    }

    private void persistExtended(UUID uuid, String name, Map<String, Long> longs, Map<String, Double> doubles) {
        if (plugin.getStorageManager().isMongo()) {
            PlayerData snapshot = new PlayerData(name);
            snapshot.longStats.putAll(longs);
            snapshot.doubleStats.putAll(doubles);
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("pit_statistics", serializeExtended(snapshot));
            plugin.getStorageManager().setMongoFields(uuid.toString(), name, Period.LIFETIME.name(), "all", fields);
            return;
        }
        if (plugin.getStorageManager().isMysql()) {
            Set<String> ids = new java.util.LinkedHashSet<>();
            ids.addAll(longs.keySet());
            ids.addAll(doubles.keySet());
            for (String id : ids) {
                try (PreparedStatement statement = plugin.getStorageManager().prepare("INSERT INTO buildffa_player_statistics(uuid,statistic_id,long_value,double_value) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE long_value=VALUES(long_value),double_value=VALUES(double_value)")) {
                    statement.setString(1, uuid.toString());
                    statement.setString(2, id);
                    statement.setLong(3, longs.getOrDefault(id, 0L));
                    statement.setDouble(4, doubles.getOrDefault(id, 0.0D));
                    statement.executeUpdate();
                } catch (Exception exception) {
                    plugin.getLogger().warning("Could not persist statistic '" + id + "' for " + uuid + ": " + exception.getMessage());
                }
            }
            return;
        }
        synchronized (this) {
            PlayerData snapshot = new PlayerData(name);
            snapshot.longStats.putAll(longs);
            snapshot.doubleStats.putAll(doubles);
            prestigeFallback.set("players." + uuid + ".statistics", serializeExtended(snapshot));
            savePrestigeFallback();
        }
    }

    private int getStoredStat(UUID uuid, Period period, String field) {
        if (plugin.getStorageManager().isMongo()) return plugin.getStorageManager().getMongoInt(uuid.toString(), period.name(), periodKey(period), field, 0);
        if (!plugin.getStorageManager().isMysql()) return 0;
        try (PreparedStatement statement = plugin.getStorageManager().prepare("SELECT " + field + " FROM buildffa_stats WHERE uuid=? AND period=? AND period_key=?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, period.name());
            statement.setString(3, periodKey(period));
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(field) : 0;
            }
        } catch (Exception ignored) {
            return 0;
        }
    }

    private List<String> topMongo(StatType type, Period period, int limit) {
        String field = leaderboardField(type);
        List<Object> documents = plugin.getStorageManager().topMongo(period.name(), periodKey(period), field, limit);
        List<Map.Entry<String, Long>> values = new ArrayList<>();
        for (Object document : documents) {
            String uuid = plugin.getStorageManager().getDocString(document, "uuid", "");
            String name = plugin.getStorageManager().getDocString(document, "name", "Unknown");
            int level = period == Period.LIFETIME ? plugin.getStorageManager().getDocInt(document, "level", 1)
                    : plugin.getStorageManager().getMongoInt(uuid, Period.LIFETIME.name(), "all", "level", 1);
            int prestige = period == Period.LIFETIME ? plugin.getStorageManager().getDocInt(document, "prestige", 0)
                    : plugin.getStorageManager().getMongoInt(uuid, Period.LIFETIME.name(), "all", "prestige", 0);
            values.add(new AbstractMap.SimpleEntry<>(uuid + "|" + name + "|" + level + "|" + prestige, plugin.getStorageManager().getDocLong(document, field, 0L)));
        }
        return formatTop(type, values, limit);
    }

    private List<String> topMysql(StatType type, Period period, int limit) {
        String field = leaderboardField(type);
        List<Map.Entry<String, Long>> values = new ArrayList<>();
        try (PreparedStatement statement = plugin.getStorageManager().prepare("SELECT s.uuid,s.name,s." + field + " AS leaderboard_value,COALESCE(l.level,1) AS level,COALESCE(l.prestige,0) AS prestige FROM buildffa_stats s LEFT JOIN buildffa_stats l ON l.uuid=s.uuid AND l.period='LIFETIME' AND l.period_key='all' WHERE s.period=? AND s.period_key=? ORDER BY s." + field + " DESC LIMIT ?")) {
            statement.setString(1, period.name());
            statement.setString(2, periodKey(period));
            statement.setInt(3, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) values.add(new AbstractMap.SimpleEntry<>(result.getString("uuid") + "|" + result.getString("name") + "|" + result.getInt("level") + "|" + result.getInt("prestige"), result.getLong("leaderboard_value")));
            }
        } catch (Exception ignored) {
        }
        return formatTop(type, values, limit);
    }

    public List<ActivePlayerEntry> queryActivePlayers(long cutoff, int limit) {
        int maximum = Math.max(1, Math.min(100, limit));
        List<ActivePlayerEntry> values = new ArrayList<>();
        if (plugin.getStorageManager().isMongo()) {
            for (Object document : plugin.getStorageManager().activePlayersMongo(cutoff, maximum)) {
                String rawUuid = plugin.getStorageManager().getDocString(document, "uuid", "");
                UUID uuid;
                try {
                    uuid = UUID.fromString(rawUuid);
                } catch (Exception ignored) {
                    continue;
                }
                values.add(new ActivePlayerEntry(uuid,
                        plugin.getStorageManager().getDocString(document, "name", ""),
                        Math.max(0L, plugin.getStorageManager().getDocLong(document, "lifetime_xp", 0L)),
                        Math.max(0, plugin.getStorageManager().getDocInt(document, "prestige", 0)),
                        Math.max(1, plugin.getStorageManager().getDocInt(document, "level", 1)),
                        plugin.getStorageManager().getDocString(document, "prefix", "")));
            }
            return values;
        }
        if (plugin.getStorageManager().isMysql()) {
            try (PreparedStatement statement = plugin.getStorageManager().prepare("SELECT uuid,name,lifetime_xp,prestige,level,prefix FROM buildffa_stats WHERE period='LIFETIME' AND period_key='all' AND last_buildffa_login>=? ORDER BY lifetime_xp DESC,uuid ASC LIMIT ?")) {
                statement.setLong(1, cutoff);
                statement.setInt(2, maximum);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(result.getString("uuid"));
                        } catch (Exception ignored) {
                            continue;
                        }
                        values.add(new ActivePlayerEntry(uuid, result.getString("name"), Math.max(0L, result.getLong("lifetime_xp")),
                                Math.max(0, result.getInt("prestige")), Math.max(1, result.getInt("level")),
                                result.getString("prefix") == null ? "" : result.getString("prefix")));
                    }
                }
            } catch (Exception exception) {
                throw new IllegalStateException(exception.getMessage(), exception);
            }
            return values;
        }
        synchronized (this) {
            ConfigurationSection root = prestigeFallback.getConfigurationSection("players");
            if (root != null) {
                for (String rawUuid : root.getKeys(false)) {
                    long login = prestigeFallback.getLong("players." + rawUuid + ".last-buildffa-login", 0L);
                    if (login < cutoff) continue;
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(rawUuid);
                    } catch (Exception ignored) {
                        continue;
                    }
                    PlayerData data = profiles.get(uuid);
                    if (data == null) data = loadFallback(uuid, "");
                    values.add(new ActivePlayerEntry(uuid, data.name, Math.max(0L, data.lifetimeXp),
                            Math.max(0, data.prestige), Math.max(1, data.level), ""));
                }
            }
        }
        values.sort(Comparator.comparingLong(ActivePlayerEntry::lifetimeXp).reversed().thenComparing(value -> value.uuid().toString()));
        return values.size() > maximum ? new ArrayList<>(values.subList(0, maximum)) : values;
    }

    private List<String> formatTop(StatType type, List<Map.Entry<String, Long>> values, int limit) {
        List<String> result = new ArrayList<>();
        int position = 1;
        for (Map.Entry<String, Long> entry : values) {
            if (position > limit) break;
            String[] identity = entry.getKey().split("\\|", 4);
            UUID uuid;
            try {
                uuid = UUID.fromString(identity[0]);
            } catch (Exception ignored) {
                uuid = null;
            }
            int levelValue;
            try {
                levelValue = identity.length > 2 ? Integer.parseInt(identity[2]) : uuid == null ? 1 : getLevel(uuid);
            } catch (NumberFormatException ignored) {
                levelValue = 1;
            }
            int prestigeValue;
            try {
                prestigeValue = identity.length > 3 ? Integer.parseInt(identity[3]) : uuid == null ? 0 : getPrestige(uuid);
            } catch (NumberFormatException ignored) {
                prestigeValue = 0;
            }
            String bracket = prestigeBracketColor(prestigeValue);
            String level = CC.color(bracket + "[" + levelColor(levelValue) + levelValue + bracket + "]");
            String name = identity.length > 1 ? identity[1] : "Unknown";
            result.add(CC.color("&7" + position + ". " + level + " &f" + name + " &8- " + leaderboardDisplay(type, entry.getValue(), levelValue, prestigeValue)));
            position++;
        }
        return result;
    }

    private long leaderboardValue(StatType type, PlayerData data) {
        if (type == StatType.LEVEL) return data.level;
        if (type == StatType.XP) return data.lifetimeXp;
        if (type == StatType.PRESTIGE) return data.prestige;
        return data.kills;
    }

    private String leaderboardField(StatType type) {
        if (type == StatType.LEVEL) return "level";
        if (type == StatType.XP) return "lifetime_xp";
        if (type == StatType.PRESTIGE) return "prestige";
        return "kills";
    }

    private String leaderboardDisplay(StatType type, long value, int level, int prestige) {
        NumberFormat whole = NumberFormat.getIntegerInstance(Locale.US);
        if (type == StatType.LEVEL) {
            String prestigeText = prestige <= 0 ? "" : " &7(Prestige &d" + roman(prestige) + "&7)";
            return "&bLevel " + whole.format(level) + prestigeText;
        }
        if (type == StatType.XP) return "&b" + whole.format(value) + " XP";
        if (type == StatType.PRESTIGE) return prestige <= 0 ? "&70 &8(0)" : "&d" + roman(prestige) + " &8(" + whole.format(prestige) + ")";
        return "&a" + whole.format(value);
    }

    private void handleKillStreak(Player killer, int streak) {
        List<Integer> milestones = plugin.registry().settings().getIntegerList("killstreaks.milestones");
        if (!milestones.contains(streak)) return;
        plugin.getPitCommandManager().broadcast("killstreaks", CC.color(plugin.replaceNetworkPlaceholders(killer, null,
                plugin.registry().messages().getString("messages.killstreak", "%player% is on a %streak% killstreak!"))
                .replace("%streak%", String.valueOf(streak))));
        int xp = Math.max(0, plugin.registry().settings().getInt("levels.rewards.killstreak-milestone-xp", 0));
        if (xp > 0) addXp(killer, xp, XpSource.KILLSTREAK);
    }

    private void handleBounty(Player killer, int streak) {
        int after = Math.max(6, plugin.registry().settings().getInt("bounties.after-streak", 6));
        if (streak < after) return;
        double chance = Math.max(0.0D, Math.min(1.0D,
                plugin.registry().settings().getDouble("bounties.bump-chance", 0.10D)));
        if (random.nextDouble() >= chance) return;
        int maximumBump = Math.max(50, plugin.registry().settings().getInt("bounties.maximum-bump", 300));
        int increments = Math.max(1, maximumBump / 50);
        int bump = (1 + random.nextInt(increments)) * 50;
        UUID uuid = killer.getUniqueId();
        int value = Math.max(0, getBounty(uuid)) + bump;
        bounties.put(uuid, value);
        persistBounty(uuid, value);
        if (plugin.getTablistManager() != null) plugin.getTablistManager().refreshBounty(killer);
        if (plugin.getProgressManager() != null) plugin.getProgressManager().recordProgress(killer,
                ProgressManager.ObjectiveType.BOUNTY_RECEIVED, "bounty", bump,
                new ProgressManager.ProgressContext("bounty", "bounty-bump:" + uuid + ":" + streak,
                        java.util.Map.of("bounty", value)));
        plugin.getPitCommandManager().broadcast("bounties", CC.color(plugin.replaceNetworkPlaceholders(killer, null,
                plugin.registry().messages().getString("messages.bounty-placed", "A bounty of %bounty% was placed on %player%."))
                .replace("%bounty%", plugin.getPitGoldService().formatPitGold(value))));
    }

    private int claimBounty(Player killer, Player dead) {
        if (killer == null || dead == null || killer.equals(dead)) return 0;
        UUID victimId = dead.getUniqueId();
        String claimId = victimId + ":" + dead.getTicksLived() + ":" + System.currentTimeMillis() / 50L;
        if (!bountyClaims.add(claimId) || !bountyLocks.add(victimId)) return 0;
        try {
            int value = bounties.getOrDefault(victimId, 0);
            if (value <= 0) return 0;
            if (!plugin.getStorageManager().putOverride("pit-bounty", victimId.toString(), "0")) return 0;
            bounties.put(victimId, 0);
            if (plugin.getShopManager() != null) value = plugin.getShopManager().bountyReward(killer, value);
            if (plugin.getTablistManager() != null) plugin.getTablistManager().refreshBounty(dead);
            if (plugin.getProgressManager() != null) plugin.getProgressManager().recordProgress(killer,
                    ProgressManager.ObjectiveType.BOUNTY_CLAIMED, "bounty", value,
                    new ProgressManager.ProgressContext("bounty", claimId, java.util.Map.of("victim", victimId.toString())));
            plugin.getPitCommandManager().broadcast("bounties", CC.color("&6&lBOUNTY CLAIMED! &7"
                    + plugin.getPlayerNameFormatter().decoratedName(killer, plugin.getNetworkProfileCache().get(killer), false) + " &7claimed "
                    + plugin.getPitGoldService().formatPitGold(value) + " &7from "
                    + plugin.getPlayerNameFormatter().decoratedName(dead, plugin.getNetworkProfileCache().get(dead), false) + "&7!"));
            return value;
        } finally {
            bountyLocks.remove(victimId);
            Bukkit.getScheduler().runTaskLater(plugin, () -> bountyClaims.remove(claimId), 200L);
        }
    }

    private void loadBounty(UUID uuid) {
        String raw = plugin.getStorageManager().getOverride("pit-bounty", uuid.toString()).orElse("0");
        try { bounties.put(uuid, Math.max(0, Integer.parseInt(raw))); }
        catch (NumberFormatException ignored) { bounties.put(uuid, 0); }
    }

    private void persistBounty(UUID uuid, int value) {
        plugin.getStorageManager().putOverride("pit-bounty", uuid.toString(), String.valueOf(Math.max(0, value)));
    }

    private int baseRequirement(int level) {
        for (String range : plugin.registry().settings().keys("levels.xp-requirements")) {
            String[] parts = range.split("-");
            try {
                int minimum = Integer.parseInt(parts[0]);
                int maximum = parts.length == 1 ? minimum : Integer.parseInt(parts[1]);
                if (level >= minimum && level <= maximum) return Math.max(1, plugin.registry().settings().getInt("levels.xp-requirements." + range, 1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }

    private String levelColor(int level) {
        for (String range : plugin.registry().settings().keys("levels.colors")) {
            String[] parts = range.split("-");
            try {
                int minimum = Integer.parseInt(parts[0]);
                int maximum = parts.length == 1 ? minimum : Integer.parseInt(parts[1]);
                if (level >= minimum && level <= maximum) return plugin.registry().settings().getString("levels.colors." + range, "&7");
            } catch (NumberFormatException ignored) {
            }
        }
        return "&7";
    }

    private int minimumLevel() {
        return Math.max(1, plugin.registry().settings().getInt("levels.minimum", 1));
    }

    private int maximumLevel() {
        return Math.max(minimumLevel(), plugin.registry().settings().getInt("levels.maximum", 120));
    }

    private int getKillXp() {
        return Math.max(0, plugin.registry().settings().getInt("levels.rewards.kill-xp", 10));
    }

    private boolean shouldResetPassives() {
        return plugin.registry().perks().getBoolean("passives.reset-on-prestige", true)
                && plugin.registry().settings().getBoolean("prestige.reset.temporary-passive-upgrades", true);
    }

    private int applyCoinMultiplier(Player player, int base) {
        return Math.max(0, base);
    }

    private synchronized String periodKey(Period period) {
        Date now = new Date();
        return switch (period) {
            case DAILY -> day.format(now);
            case WEEKLY -> {
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                yield calendar.get(java.util.Calendar.YEAR) + "-W" + calendar.get(java.util.Calendar.WEEK_OF_YEAR);
            }
            case MONTHLY -> month.format(now);
            default -> "all";
        };
    }

    private PlayerData profile(UUID uuid, String name) {
        PlayerData data = profiles.computeIfAbsent(uuid, ignored -> new PlayerData(name));
        if (name != null && !name.isEmpty()) data.name = name;
        return data;
    }

    public void loadOffline(OfflinePlayer player) {
        if (player != null) ensureLoaded(player);
    }

    private PlayerData ensureLoaded(OfflinePlayer player) {
        PlayerData existing = profiles.get(player.getUniqueId());
        if (existing != null) return existing;
        PlayerData loaded = load(player.getUniqueId(), safeName(player));
        profiles.put(player.getUniqueId(), loaded);
        loadedProfiles.add(player.getUniqueId());
        loadBounty(player.getUniqueId());
        return loaded;
    }

    private void revalidateOnline(OfflinePlayer offline) {
        Player player = offline.getPlayer();
        if (player != null && plugin.getPerkManager() != null) plugin.getPerkManager().revalidate(player);
    }

    private String safeName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString().substring(0, 16) : player.getName();
    }

    private int safeIntAdd(int value, long amount) {
        long result = value + amount;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : result < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) result;
    }

    private long safeLongAdd(long value, long amount) {
        if (amount > 0L && value > Long.MAX_VALUE - amount) return Long.MAX_VALUE;
        return value + amount;
    }

    private String prefix() {
        return CC.color(plugin.registry().messages().getString("messages.prefix", "&bThe Pit &8» &7"));
    }

    private String roman(int value) {
        if (value <= 0) return "";
        int number = Math.min(50, value);
        int[] values = {50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            while (number >= values[index]) {
                result.append(numerals[index]);
                number -= values[index];
            }
        }
        return result.toString();
    }

    public void save() {
        if (flushTask != -1) {
            Bukkit.getScheduler().cancelTask(flushTask);
            flushTask = -1;
        }
        for (Map.Entry<UUID, PlayerData> entry : new ArrayList<>(profiles.entrySet())) {
            UUID uuid = entry.getKey();
            PlayerData data = entry.getValue();
            String name = data.name == null || data.name.isEmpty() ? uuid.toString().substring(0, 16) : data.name;
            capturePlaytime(data);
            if (!plugin.getStorageManager().isMysql() && !plugin.getStorageManager().isMongo()) {
                saveFallbackState(uuid, prestigeState(uuid), null);
            } else {
                persistProgressNow(uuid, name, data);
                persistPrestigeFieldsNow(uuid, name, data, 1);
            }
            persistExtended(uuid, name, new LinkedHashMap<>(data.longStats), new LinkedHashMap<>(data.doubleStats));
        }
    }

    private static final class PlayerData {
        private String name;
        private int kills;
        private int deaths;
        private int coins;
        private int killstreak;
        private int prestige;
        private int level = 1;
        private int currentXp;
        private long lifetimeXp;
        private long renown;
        private long lastPrestige;
        private long lastPitLogin;
        private String renownUpgrades = "";
        private String temporaryPassiveUpgrades = "";
        private final Map<String, Long> longStats = new ConcurrentHashMap<>();
        private final Map<String, Double> doubleStats = new ConcurrentHashMap<>();
        private long sessionStarted;
        private boolean existingProfile;
        private boolean loadFailed;

        private PlayerData(String name) {
            this.name = name == null ? "" : name;
        }
    }

    public record ActivePlayerEntry(UUID uuid, String name, long lifetimeXp, int prestige, int level, String prefix) {}
}
