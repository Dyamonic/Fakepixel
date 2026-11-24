package club.abelix.games.fakevival.config.defaults;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public final class DupeDetectionDefaults {
    private DupeDetectionDefaults() {
    }

    public static void apply(ConfigurationSection root) {
        root.set("dupe-detection.enabled", true);
        root.set("dupe-detection.read-only-mode", true);
        root.set("dupe-detection.auto-quarantine", false);
        root.set("dupe-detection.auto-remove-items", false);
        root.set("dupe-detection.cancel-suspicious-clicks", false);
        root.set("dupe-detection.alert-staff", true);
        root.set("dupe-detection.webhook", true);
        root.set("dupe-detection.rate-limit-protection", true);
        root.set("dupe-detection.scan-cooldown-ms", 2000L);
        root.set("dupe-detection.suppression-window-ms", 10000L);
        root.set("dupe-detection.max-scan-requests-per-window", 5);
        root.set("dupe-detection.alert-cooldown-ms", 30000L);
        root.set("dupe-detection.global-scan-interval-seconds", 20);
        root.set("dupe-detection.global-scan-batch-size", 10);
        root.set("dupe-detection.scan-interval-ticks", 400);
        root.set("dupe-detection.event-scan-delay-ticks", 4);
        root.set("dupe-detection.scan-crafting-table", true);
        root.set("dupe-detection.crafting-scan-cooldown-ms", 2000L);
        root.set("dupe-detection.log-suppressed-scans", false);
        root.set("dupe-detection.scan-ender-chest", true);
        root.set("dupe-detection.scan-world-drops", false);
        root.set("dupe-detection.remove-duplicate", false);
        root.set("dupe-detection.fake-menu-chests.enabled", true);
        root.set("dupe-detection.fake-menu-chests.alert", true);
        root.set("dupe-detection.fake-menu-chests.protected-materials", List.of("CHEST", "TRAPPED_CHEST", "ENDER_CHEST", "BARREL", "SHULKER_BOX"));
        root.set("dupe-detection.fake-menu-chests.protected-names", List.of("&6Survival Menu", "&bYour Skills", "&bSkill Leveling", "&bJobs", "&8Shop", "&6Market", "&5Adventure Guild", "&6Auction House", "&dBattlePass", "&8Sell All", "&6SMP Gold Store", "&8Collections", "&8Companions", "&6Playtime Rewards", "&6Guilds", "&8Your Boosters", "&8Your Cosmetics", "&8Equipment", "&8Browse Guilds", "&8Guild Settings", "&8Admin Item Browser", "&6Player Level"));
        root.set("dupe-detection.alert-cooldown-seconds", 30);
        root.set("dupe-detection.track-all-items", false);
        root.set("dupe-detection.track-stacked-items", false);
        root.set("dupe-detection.tracked-materials", List.of());
        root.set("dupe-detection.ignored-materials", List.of("AIR"));
        root.set("dupe.readOnlyMode", true);
        root.set("dupe.autoRemove", false);
        root.set("dupe.cancelSuspiciousClicks", false);
        root.set("dupe.scanCraftingTable", true);
        root.set("dupe.logSuppressedScans", false);
        root.set("dupe.rateLimitProtection", true);
        root.set("dupe.scanCooldownMs", 2000L);
        root.set("dupe.suppressionWindowMs", 10000L);
        root.set("dupe.maxScanRequestsPerWindow", 5);
        root.set("dupe.alertCooldownMs", 30000L);
        root.set("dupe.globalScanIntervalSeconds", 20);
        root.set("dupe.globalScanBatchSize", 10);
    }
}
