package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ChatFilterListener implements Listener {
    private final JavaPlugin plugin;

    public ChatFilterListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("chat-filter.enabled", true)) return;
        Player player = event.getPlayer();
        String bypass = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("chat-filter.bypass-permission", "fakevival.chatfilter.bypass");
        if (bypass != null && !bypass.isBlank() && player.hasPermission(bypass)) return;

        String matched = matchedTerm(event.getMessage());
        if (matched == null) return;

        event.setCancelled(true);
        player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("chat-filter.messages.blocked", "&cThat message is not allowed.")));
        notifyStaff(player, matched, event.getMessage());
    }

    private static final Set<String> BUILT_IN_SAFE_WORDS = Set.of("idk", "coin", "coins", "shop", "guild", "party", "warp", "rtp", "sell", "buy", "combat", "mining", "fishing", "farming");

    public String matchedTerm(String message) {
        String normalized = normalize(message);
        Set<String> whitelist = new java.util.HashSet<>(BUILT_IN_SAFE_WORDS);
        whitelist.addAll(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("chatfilter.whitelist")
                .stream().map(this::normalize).filter(value -> !value.isBlank()).toList());

        for (String term : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("chat-filter.blocked-words")) {
            String normalizedTerm = normalize(term);
            if (normalizedTerm.isBlank() || whitelist.contains(normalizedTerm)) continue;
            if (normalizedTerm.equals("usd") && (Pattern.compile("(?i)(^|\\W)usd(\\W|$)").matcher(normalized).find() || message.contains("$"))) return term;
            Pattern boundary = Pattern.compile("(?i)(^|\\W)" + Pattern.quote(normalizedTerm) + "(\\W|$)");
            if (boundary.matcher(normalized).find()) return term;
        }
        return null;
    }

    private void notifyStaff(Player player, String term, String message) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("chat-filter.notify-staff.enabled", true)) return;
        String permission = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("chat-filter.notify-staff.permission", "fakevival.staff.chatfilter");
        String alert = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("chat-filter.notify-staff.message",
                "&8[Filter] &f%player% &7matched &c%term%&7: &f%message%")
                .replace("%player%", player.getName())
                .replace("%term%", term)
                .replace("%message%", message);

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (permission == null || permission.isBlank() || online.hasPermission(permission)) {
                    online.sendMessage(CC.color(alert));
                }
            }
        });
    }

    private String normalize(String text) {
        String lower = String.valueOf(text).toLowerCase(Locale.ROOT);
        lower = lower.replace("$/usd", " usd ");
        lower = lower.replace("$", " usd ");
        return lower.replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
}
