package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Single main-thread entry point for quest/contract/event progress. */
public final class ProgressManager {
    public enum ObjectiveType {
        PLAYER_KILL, ASSIST, DAMAGE_DEALT, PIT_GOLD_EARNED, XP_EARNED, KILLSTREAK_REACHED,
        BOUNTY_RECEIVED, BOUNTY_CLAIMED, QUICK_MATHS_CORRECT, MINOR_EVENT_PARTICIPATION,
        MINOR_EVENT_WIN, MYSTIC_ITEM_FOUND, MYSTIC_ITEM_ENCHANTED, GOLDEN_HEAD_CONSUMED,
        ITEM_PURCHASED, KILLSTREAK_PURCHASED, CONTRACT_COMPLETED, PLAY_TIME
    }

    public static final class ProgressContext {
        private final String targetId;
        private final String uniqueKey;
        private final Map<String, Object> values;

        public ProgressContext(String targetId, String uniqueKey, Map<String, Object> values) {
            this.targetId = targetId == null ? "" : targetId;
            this.uniqueKey = uniqueKey == null ? "" : uniqueKey;
            this.values = values == null ? Collections.emptyMap() : values;
        }
        public String targetId() { return targetId; }
        public String uniqueKey() { return uniqueKey; }
        public Map<String, Object> values() { return values; }
    }

    private final ThePitPlugin plugin;
    private final Map<UUID, String> lastEvents = new ConcurrentHashMap<>();
    private final java.util.Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    public ProgressManager(ThePitPlugin plugin) { this.plugin = plugin; }

    public void recordProgress(Player player, ObjectiveType type, String targetId, long amount, ProgressContext context) {
        if (player == null || type == null || amount <= 0L) return;
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> recordProgress(player, type, targetId, amount, context));
            return;
        }
        if (!player.isOnline() || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) return;
        String key = context == null ? "" : context.uniqueKey();
        if (!key.isEmpty() && !processedKeys.add(player.getUniqueId() + ":" + type.name() + ":" + key)) return;
        lastEvents.put(player.getUniqueId(), type.name() + ":" + (targetId == null ? "" : targetId) + ":+" + amount);

        switch (type) {
            case PLAYER_KILL:
                // Existing kill pipeline already updates both systems; avoid a second increment here.
                break;
            case PIT_GOLD_EARNED:
                if (plugin.getQuestManager() != null) plugin.getQuestManager().handlePitGoldEarned(player, (int) Math.min(Integer.MAX_VALUE, amount));
                break;
            case CONTRACT_COMPLETED:
                if (plugin.getQuestManager() != null) plugin.getQuestManager().handleContractCompleted(player);
                break;
            case GOLDEN_HEAD_CONSUMED:
                if (plugin.getContractManager() != null) plugin.getContractManager().onGoldenApple(player);
                break;
            default:
                // AchievementManager is the broad objective engine in this codebase.
                if (plugin.getAchievementManager() != null) {
                    try {
                        AchievementManager.EventType eventType = AchievementManager.EventType.valueOf(type.name());
                        plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(
                                player.getUniqueId(), eventType, amount,
                                context == null ? Collections.emptyMap() : context.values(), false));
                    } catch (IllegalArgumentException ignored) { }
                }
        }
    }

    public String lastEvent(UUID uuid) { return lastEvents.getOrDefault(uuid, "none"); }
    public void clearSessionKeys(UUID uuid) {
        if (uuid == null) return;
        processedKeys.removeIf(key -> key.startsWith(uuid.toString() + ":"));
    }
}
