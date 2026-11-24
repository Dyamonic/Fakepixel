package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class RewardMultiplierService {
    public enum RewardSource { KILL_GOLD, KILL_XP, ASSIST_GOLD, ASSIST_XP, OTHER }
    public record RewardMultiplier(double gold, double xp) {
        public static final RewardMultiplier NORMAL = new RewardMultiplier(1.0D, 1.0D);
    }

    private final ThePitPlugin plugin;
    public RewardMultiplierService(ThePitPlugin plugin) { this.plugin = plugin; }

    public RewardMultiplier getMultiplier(Player player, RewardSource source, Location location) {
        MinorEventManager events = plugin.getMinorEventManager();
        if (events == null || !events.isDoubleRewardsActive() || !events.isInsideDoubleRewardsZone(location)) return RewardMultiplier.NORMAL;
        return switch (source) {
            case KILL_GOLD, ASSIST_GOLD -> new RewardMultiplier(2.0D, 1.0D);
            case KILL_XP, ASSIST_XP -> new RewardMultiplier(1.0D, 2.0D);
            default -> RewardMultiplier.NORMAL;
        };
    }
}
