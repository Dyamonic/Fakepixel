package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.config.DeploymentConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DiscordWebhookManager {
    private static final int ADMIN_COMMAND_COLOR = 8978312;

    private final JavaPlugin plugin;
    private final Set<String> warnedMissingWebhooks = ConcurrentHashMap.newKeySet();
    private final java.util.Map<String, Long> nextFailureLogAt = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public DiscordWebhookManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendDupeAlert(String title, String description) {
        sendEmbed("discord.dupe-detection", title, description, "Fakevival Dupe Detection", 15158332);
    }

    public void sendAdminCommandLog(String title, String description) {
        sendEmbed("discord.admin-commands", title, description, "SMP Admin Commands", 3447003);
    }

    public void sendAdminCommandUsage(String playerName, String commandLine, String serverId) {
        String command = commandLine == null || commandLine.isBlank() ? "[REDACTED]" : commandLine;
        String server = serverId == null || serverId.isBlank() ? "unknown" : serverId;
        sendDescriptionOnlyEmbed("discord.admin-commands",
                "**" + playerName + "** used command `" + inlineCode(command) + "` [" + server + "]",
                ADMIN_COMMAND_COLOR);
    }

    public void sendXrayAlert(String title, String description) {
        sendEmbed("discord.xray-detection", title, description, "Xray Detection", 16753920);
    }

    public void sendTradeLog(String title, String description) {
        sendEmbed("discord.trades", title, description, "SMP Trades", 5763719);
    }

    public void sendBazaarAllLog(String title, String description) {
        sendEmbed("bazaar.discord.all", title, description, "SMP Bazaar", 15844367);
    }

    public void sendBazaarHighLog(String title, String description) {
        sendEmbed("bazaar.discord.high", title, description, "SMP Bazaar High Value", 16753920);
    }

    public void sendSmpAlert(String title, String description) {
        sendEmbed("discord.smp-alerts", title, description, "SMP ALERT", 16753920);
    }

    public boolean isConfigured(String configPath) {
        return DeploymentConfig.webhook(configPath).filter(value -> !value.isBlank()).isPresent();
    }

    private void sendEmbed(String configPath, String title, String description, String defaultUsername, int defaultColor) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(configPath + ".enabled", true)) return;
        String url = DeploymentConfig.webhook(configPath).orElse("");
        if (url.isBlank()) {
            DeploymentConfig.webhookEnvName(configPath).ifPresent(envName -> {
                if (warnedMissingWebhooks.add(configPath)) {
                    plugin.getLogger().warning("Discord webhook logger '" + configPath + "' is disabled because env var "
                            + envName + " is not set.");
                }
            });
            return;
        }

        String username = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(configPath + ".username", defaultUsername);
        int color = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(configPath + ".embed-color", defaultColor);
        String payload = "{"
                + "\"username\":\"" + escape(username) + "\","
                + "\"embeds\":[{"
                + "\"title\":\"" + escape(title) + "\","
                + "\"description\":\"" + escape(description) + "\","
                + "\"color\":" + color
                + "}]"
                + "}";

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid Discord webhook URL for " + configPath + ".");
            return;
        }

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenAccept(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                warnFailure(configPath, "HTTP " + response.statusCode());
            }
        }).exceptionally(error -> {
            warnFailure(configPath, error.getClass().getSimpleName());
            return null;
        });
    }

    private void sendDescriptionOnlyEmbed(String configPath, String description, int color) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(configPath + ".enabled", true)) return;
        String url = DeploymentConfig.webhook(configPath).orElse("");
        if (url.isBlank()) {
            DeploymentConfig.webhookEnvName(configPath).ifPresent(envName -> {
                if (warnedMissingWebhooks.add(configPath)) {
                    plugin.getLogger().warning("Discord webhook logger '" + configPath + "' is disabled because env var "
                            + envName + " is not set.");
                }
            });
            return;
        }

        String payload = "{"
                + "\"embeds\":[{"
                + "\"description\":\"" + escape(description) + "\","
                + "\"color\":" + color
                + "}]"
                + "}";
        sendPayload(configPath, url, payload);
    }

    private void sendPayload(String configPath, String url, String payload) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid Discord webhook URL for " + configPath + ".");
            return;
        }

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenAccept(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                warnFailure(configPath, "HTTP " + response.statusCode());
            }
        }).exceptionally(error -> {
            warnFailure(configPath, error.getClass().getSimpleName());
            return null;
        });
    }

    private String inlineCode(String value) {
        return value == null ? "" : value.replace("`", "'");
    }

    private void warnFailure(String configPath, String reason) {
        long now = System.currentTimeMillis();
        if (nextFailureLogAt.getOrDefault(configPath, 0L) > now) return;
        nextFailureLogAt.put(configPath, now + 60_000L);
        plugin.getLogger().warning("Discord webhook " + configPath + " failed (" + reason + ").");
    }

    private String escape(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
