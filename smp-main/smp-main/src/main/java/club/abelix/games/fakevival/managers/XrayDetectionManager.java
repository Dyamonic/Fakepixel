package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class XrayDetectionManager {
    private final JavaPlugin plugin;
    private final DiscordWebhookManager discordWebhookManager;
    private final SocialSpyManager socialSpyManager;
    private final Map<UUID, Deque<OreBreak>> recentBreaks = new HashMap<>();
    private final Map<UUID, Long> alertCooldowns = new HashMap<>();

    public XrayDetectionManager(JavaPlugin plugin, DiscordWebhookManager discordWebhookManager) {
        this(plugin, discordWebhookManager, null);
    }

    public XrayDetectionManager(JavaPlugin plugin, DiscordWebhookManager discordWebhookManager, SocialSpyManager socialSpyManager) {
        this.plugin = plugin;
        this.discordWebhookManager = discordWebhookManager;
        this.socialSpyManager = socialSpyManager;
    }

    public boolean isEnabled() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("xray-detection.enabled", true);
    }

    public void handleOreBreak(Player player, Block block) {
        if (!isEnabled() || player.hasPermission("fakevival.admin.xray.bypass")) return;
        if (!isTrackedOre(block.getType())) return;
        if (block.getY() > club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("xray-detection.max-y", 32)) return;
        if (block.getLightLevel() > club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("xray-detection.max-light-level", 5)) return;

        long now = System.currentTimeMillis();
        long windowMillis = Duration.ofSeconds(Math.max(10L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("xray-detection.window-seconds", 180L))).toMillis();
        Deque<OreBreak> breaks = recentBreaks.computeIfAbsent(player.getUniqueId(), uuid -> new ArrayDeque<>());
        breaks.addLast(new OreBreak(now, block.getType(), block.getLocation()));
        trimOldBreaks(breaks, now - windowMillis);

        int threshold = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("xray-detection.alert-threshold", 10));
        if (breaks.size() < threshold || isCoolingDown(player.getUniqueId(), now)) return;

        long cooldownMillis = Duration.ofSeconds(Math.max(1L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("xray-detection.alert-cooldown-seconds", 120L))).toMillis();
        alertCooldowns.put(player.getUniqueId(), now + cooldownMillis);
        alert(player, breaks);
    }

    private void trimOldBreaks(Deque<OreBreak> breaks, long earliestMillis) {
        while (!breaks.isEmpty() && breaks.peekFirst().timeMillis() < earliestMillis) breaks.removeFirst();
    }

    private boolean isCoolingDown(UUID uuid, long now) {
        long until = alertCooldowns.getOrDefault(uuid, 0L);
        if (now <= until) return true;
        alertCooldowns.remove(uuid);
        return false;
    }

    private boolean isTrackedOre(Material material) {
        for (String configured : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("xray-detection.tracked-blocks")) {
            if (configured.equalsIgnoreCase(material.name())) return true;
        }
        return false;
    }

    private void alert(Player player, Deque<OreBreak> breaks) {
        OreBreak latest = breaks.peekLast();
        if (latest == null) return;
        String permission = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("xray-detection.staff-permission", "fakevival.staff.xray");
        String summary = summarize(breaks);
        Location location = latest.location();
        String locationText = location.getWorld().getName()
                + " " + location.getBlockX()
                + ", " + location.getBlockY()
                + ", " + location.getBlockZ();

        String staffMessage = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("xray-detection.staff-message",
                        "&c[Xray] &f%player% &7mined &e%count% &7valuable ores in &e%seconds%s&7. &8(%location%)")
                .replace("%player%", player.getName())
                .replace("%count%", String.valueOf(breaks.size()))
                .replace("%seconds%", String.valueOf(Math.max(10L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("xray-detection.window-seconds", 180L))))
                .replace("%location%", locationText)
                .replace("%summary%", summary);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission)) staff.sendMessage(CC.color(staffMessage));
        }
        if (socialSpyManager != null) socialSpyManager.spy("&8[Spy Alert] " + staffMessage);

        String description = "**Player:** " + player.getName() + "\n"
                + "**Ore Count:** " + breaks.size() + "\n"
                + "**Window:** " + club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("xray-detection.window-seconds", 180L) + " seconds\n"
                + "**Latest Location:** " + locationText + "\n"
                + "**Breakdown:** " + summary;
        discordWebhookManager.sendXrayAlert("Possible xray mining pattern", description);
    }

    private String summarize(Deque<OreBreak> breaks) {
        Map<Material, Integer> counts = new HashMap<>();
        for (OreBreak oreBreak : breaks) counts.merge(oreBreak.material(), 1, Integer::sum);
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
            if (!builder.isEmpty()) builder.append(", ");
            builder.append(pretty(entry.getKey())).append(" x").append(entry.getValue());
        }
        return builder.toString();
    }

    private String pretty(Material material) {
        String text = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private record OreBreak(long timeMillis, Material material, Location location) {
    }
}
