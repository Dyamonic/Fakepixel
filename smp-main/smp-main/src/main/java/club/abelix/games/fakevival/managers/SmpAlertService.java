package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.network.SMPServerIdentity;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SmpAlertService {
    public enum Type { AUCTION_BID, TRADE_COINS, BANK_WITHDRAW, BAZAAR_SELL }

    private static final long RATE_LIMIT_MS = 10_000L;
    private final JavaPlugin plugin;
    private final DiscordWebhookManager webhooks;
    private final String serverId;
    private final Map<Type, Double> thresholds = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAlerts = new ConcurrentHashMap<>();
    private final Set<UUID> disabledViewers = ConcurrentHashMap.newKeySet();

    public SmpAlertService(JavaPlugin plugin, DiscordWebhookManager webhooks) {
        this.plugin = plugin;
        this.webhooks = webhooks;
        this.serverId = SMPServerIdentity.resolve(plugin).serverId();
        thresholds.put(Type.AUCTION_BID, 1_000_000D);
        thresholds.put(Type.TRADE_COINS, 500_000D);
        thresholds.put(Type.BANK_WITHDRAW, 1_000_000D);
        thresholds.put(Type.BAZAAR_SELL, 750_000D);
        if (!webhooks.isConfigured("discord.smp-alerts")) {
            plugin.getLogger().warning("[SMP] SMP alerts Discord webhook not set. In-game alerts only.");
        }
    }

    public String serverId() { return serverId; }
    public double threshold(Type type) { return thresholds.get(type); }
    public void setThreshold(Type type, double amount) { thresholds.put(type, Math.max(0D, amount)); }
    public Map<Type, Double> thresholds() { return Map.copyOf(thresholds); }

    public boolean toggle(Player player) {
        if (!disabledViewers.add(player.getUniqueId())) {
            disabledViewers.remove(player.getUniqueId());
            return true;
        }
        return false;
    }

    public void alert(Type type, UUID actor, String gameMessage, String discordDescription, String compactDetails) {
        long now = System.currentTimeMillis();
        String key = type.name() + ':' + (actor == null ? "system" : actor);
        Long previous = lastAlerts.put(key, now);
        if (previous != null && now - previous < RATE_LIMIT_MS) return;

        Runnable inGame = () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("fakevival.alerts.staff") && !disabledViewers.contains(player.getUniqueId())) {
                    player.sendMessage(CC.color(gameMessage));
                }
            }
        };
        if (Bukkit.isPrimaryThread()) inGame.run();
        else Bukkit.getScheduler().runTask(plugin, inGame);

        plugin.getLogger().info("[SMP ALERT] " + type + " " + sanitize(compactDetails));
        webhooks.sendSmpAlert(type.name().replace('_', ' '), sanitize(discordDescription));
    }

    private String sanitize(String value) {
        if (value == null) return "";
        return value.replaceAll("(?i)(token|password|webhook|secret)\\s*[=:]\\s*\\S+", "$1=[REDACTED]");
    }
}
