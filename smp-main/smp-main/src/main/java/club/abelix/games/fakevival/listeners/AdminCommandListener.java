package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.config.RuntimeSettings;
import club.abelix.games.fakevival.managers.DiscordWebhookManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AdminCommandListener implements Listener {
    private static final Set<String> SENSITIVE = Set.of("password", "token", "secret", "apikey", "api-key", "webhook", "mysql", "mariadb", "database");
    private static final Set<String> ADMIN_PERMISSION_MARKERS = Set.of("admin", "staff", "developer", "devcontrols", "debug", "smpworld", "zone.admin", "dungeon.admin", "minecraft.command");
    private static final Set<String> STAFF_PERMISSIONS = Set.of("fakevival.admin", "fakevival.devcontrols.admin", "fakevival.debug.admin", "fakevival.zone.admin", "fakevival.dungeon.admin", "fakevival.smpworld.admin", "minecraft.command.gamemode");

    private final JavaPlugin plugin;
    private final DiscordWebhookManager webhooks;

    public AdminCommandListener(JavaPlugin plugin, DiscordWebhookManager webhooks) {
        this.plugin = plugin;
        this.webhooks = webhooks;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String normalized = normalize(event.getMessage());
        if (!shouldSendAdminCommandWebhook(event.getPlayer(), normalized)) {
            if (RuntimeSettings.config(plugin).getBoolean("discord.admin-commands.debug", false))
                plugin.getLogger().info("[SMP] Admin webhook skipped non-admin command: /" + normalized);
            return;
        }
        String audited = redact(normalized);
        String root = normalized.split(" ", 2)[0];
        if (Set.of("maxiteminhand", "maxitem", "maxhand").contains(root) && plugin instanceof Game game) {
            String itemId = game.getCustomItemManager().getItemId(event.getPlayer().getInventory().getItemInMainHand());
            audited += " custom_item_id=" + (itemId == null ? "none" : itemId);
        }
        webhooks.sendAdminCommandUsage(event.getPlayer().getName(), audited, resolveServerId());
    }

    public boolean shouldSendAdminCommandWebhook(CommandSender sender, String commandLine) {
        if (!RuntimeSettings.config(plugin).getBoolean("discord.admin-commands.enabled", true)) return false;
        if (!(sender instanceof Player) && !RuntimeSettings.config(plugin).getBoolean("discord.admin-commands.log-console", false)) return false;
        String normalized = normalize(commandLine);
        if (normalized.isBlank()) return false;
        for (String entry : RuntimeSettings.config(plugin).getStringList("discord.admin-commands.logged-commands")) {
            String allowed = normalize(entry);
            if (!allowed.isBlank() && (normalized.equals(allowed) || normalized.startsWith(allowed + " "))) return true;
        }
        if (requiresAdminPermission(normalized)) return true;
        return RuntimeSettings.config(plugin).getBoolean("discord.admin-commands.log-all-staff-commands", false)
                && STAFF_PERMISSIONS.stream().anyMatch(sender::hasPermission);
    }

    private boolean requiresAdminPermission(String commandLine) {
        String root = commandLine.split(" ", 2)[0];
        if (Set.of("maxiteminhand", "maxitem", "maxhand").contains(root)) return true;
        PluginCommand command = Bukkit.getPluginCommand(root);
        if (command == null || command.getPermission() == null) return false;
        String permission = command.getPermission().toLowerCase(Locale.ROOT);
        return ADMIN_PERMISSION_MARKERS.stream().anyMatch(permission::contains);
    }

    private String redact(String commandLine) {
        String[] parts = commandLine.split(" ");
        for (int i = 0; i < parts.length; i++) {
            String token = parts[i].toLowerCase(Locale.ROOT);
            if (SENSITIVE.stream().anyMatch(token::contains)) {
                for (int j = i + 1; j < parts.length; j++) parts[j] = "[REDACTED]";
                break;
            }
        }
        return String.join(" ", parts);
    }

    private String normalize(String commandLine) {
        if (commandLine == null) return "";
        String value = commandLine.trim().toLowerCase(Locale.ROOT);
        while (value.startsWith("/")) value = value.substring(1);
        value = value.replaceAll("\\s+", " ");
        int space = value.indexOf(' ');
        String root = space < 0 ? value : value.substring(0, space);
        int namespace = root.indexOf(':');
        if (namespace >= 0) root = root.substring(namespace + 1);
        return root + (space < 0 ? "" : value.substring(space));
    }

    private String resolveServerId() {
        String property = System.getProperty("SMP_SERVER_ID");
        if (property != null && !property.isBlank()) return property;
        String environment = System.getenv("SMP_SERVER_ID");
        if (environment != null && !environment.isBlank()) return environment;
        return Bukkit.getServer().getName();
    }
}
