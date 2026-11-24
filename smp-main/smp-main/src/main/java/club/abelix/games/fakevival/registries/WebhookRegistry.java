package club.abelix.games.fakevival.registries;

import java.util.Map;
import java.util.Optional;

public final class WebhookRegistry {
    public static final Map<String, Optional<String>> DEFAULT_WEBHOOK_URLS = Map.of(
            "discord.dupe-detection", Optional.of("https://discord.com/api/webhooks/1525795471153561703/G-ak5y9R6fJttwoTcmaBUTXetWHAcC_STpoD8fmaXjsnIYNuRsqcGgTAF3hEWHvOwf-J"),
            "discord.xray-detection", Optional.of("https://discord.com/api/webhooks/1525795471153561703/G-ak5y9R6fJttwoTcmaBUTXetWHAcC_STpoD8fmaXjsnIYNuRsqcGgTAF3hEWHvOwf-J"),
            "discord.admin-commands", Optional.of("https://discord.com/api/webhooks/1525795471153561703/G-ak5y9R6fJttwoTcmaBUTXetWHAcC_STpoD8fmaXjsnIYNuRsqcGgTAF3hEWHvOwf-J"),
            "discord.trades", Optional.of("https://discord.com/api/webhooks/1525795471153561703/G-ak5y9R6fJttwoTcmaBUTXetWHAcC_STpoD8fmaXjsnIYNuRsqcGgTAF3hEWHvOwf-J"),
            "bazaar.discord.all", Optional.of("https://discord.com/api/webhooks/1525795471153561703/G-ak5y9R6fJttwoTcmaBUTXetWHAcC_STpoD8fmaXjsnIYNuRsqcGgTAF3hEWHvOwf-J"),
            "bazaar.discord.high", Optional.of("https://discord.com/api/webhooks/1525795471153561703/G-ak5y9R6fJttwoTcmaBUTXetWHAcC_STpoD8fmaXjsnIYNuRsqcGgTAF3hEWHvOwf-J"),
            "discord.smp-alerts", Optional.empty()
    );

    public static final Map<String, String> VARIABLE_NAMES = Map.of(
            "discord.dupe-detection", "SMP_DISCORD_DUPE_WEBHOOK",
            "discord.xray-detection", "SMP_DISCORD_XRAY_WEBHOOK",
            "discord.admin-commands", "SMP_DISCORD_ADMIN_COMMAND_WEBHOOK",
            "discord.trades", "SMP_DISCORD_TRADES_WEBHOOK",
            "bazaar.discord.all", "SMP_DISCORD_BAZAAR_ALL_WEBHOOK",
            "bazaar.discord.high", "SMP_DISCORD_BAZAAR_HIGH_WEBHOOK",
            "discord.smp-alerts", "SMP_DISCORD_ALERTS_WEBHOOK"
    );

    private WebhookRegistry() {
    }

    public static Optional<String> defaultWebhookUrl(String key) {
        if (key == null) return Optional.empty();
        return DEFAULT_WEBHOOK_URLS.getOrDefault(key, Optional.empty())
                .filter(value -> !value.isBlank());
    }

    public static Optional<String> variableName(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(VARIABLE_NAMES.get(key))
                .filter(value -> !value.isBlank());
    }
}
