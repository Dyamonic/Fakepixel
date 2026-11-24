package club.abelix.games.fakevival.mobengine.profile;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public final class SMPMobPerformanceProfiler {
    private int totalMobs;
    private int tickedMobs;
    private int fullAiMobs;
    private int pathfinds;
    private int rotationBatchSize;
    private long lastTickNanos;
    private int visibleMobs;
    private int nameplateUpdatesPerTick;
    private int particlesPerSecond;
    private int sleepingMobs;
    private int fullRenderMobs;
    private int reducedRenderMobs;
    private int hiddenFarMobs;
    private int spawnQueueSize;
    private int maxVisibleForPlayer;
    private String visibleByPlayerSummary = "none";

    public void recordTick(int totalMobs, int tickedMobs, int fullAiMobs, int pathfinds, int rotationBatchSize, long nanos) {
        this.totalMobs = totalMobs;
        this.tickedMobs = tickedMobs;
        this.fullAiMobs = fullAiMobs;
        this.pathfinds = pathfinds;
        this.rotationBatchSize = rotationBatchSize;
        this.lastTickNanos = nanos;
    }

    public String summary() {
        return "total=" + totalMobs
                + ", ticked=" + tickedMobs
                + ", fullAI=" + fullAiMobs
                + ", pathfinds=" + pathfinds
                + ", batch=" + rotationBatchSize
                + ", aiMs=" + String.format("%.3f", lastTickNanos / 1_000_000.0)
                + ", visible=" + visibleMobs
                + ", maxVisiblePlayer=" + maxVisibleForPlayer
                + ", nameTags/t=" + nameplateUpdatesPerTick
                + ", particles/s=" + particlesPerSecond
                + ", sleeping=" + sleepingMobs
                + ", fullRender=" + fullRenderMobs
                + ", reducedRender=" + reducedRenderMobs
                + ", hiddenFar=" + hiddenFarMobs
                + ", spawnQueue=" + spawnQueueSize
                + ", visiblePlayers=" + visibleByPlayerSummary;
    }

    public void recordClientLoad(int visibleMobs, int nameplateUpdates, int particlesPerSecond, int sleepingMobs,
                                 int fullRenderMobs, int reducedRenderMobs, int hiddenFarMobs, int spawnQueueSize,
                                 Map<UUID, Integer> visibleByPlayer) {
        this.visibleMobs = visibleMobs;
        this.nameplateUpdatesPerTick = nameplateUpdates;
        this.particlesPerSecond = particlesPerSecond;
        this.sleepingMobs = sleepingMobs;
        this.fullRenderMobs = fullRenderMobs;
        this.reducedRenderMobs = reducedRenderMobs;
        this.hiddenFarMobs = hiddenFarMobs;
        this.spawnQueueSize = spawnQueueSize;
        this.maxVisibleForPlayer = visibleByPlayer.values().stream().max(Comparator.naturalOrder()).orElse(0);
        this.visibleByPlayerSummary = visibleByPlayer.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(6)
                .map(entry -> playerName(entry.getKey()) + ":" + entry.getValue())
                .reduce((left, right) -> left + "|" + right)
                .orElse("none");
    }

    private String playerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player == null ? uuid.toString().substring(0, 8) : player.getName();
    }
}
