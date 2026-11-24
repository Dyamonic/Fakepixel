package club.abelix.core.plugin.forum.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;


@Slf4j
@UtilityClass
public final class TempMailUtil {

    private final String TEMP_MAIL_LIST_URL = "https://raw.githubusercontent.com/disposable-email-domains/disposable-email-domains/main/disposable_email_blocklist.conf";

    private Set<String> TEMP_MAIL_DOMAINS;

    public boolean isTempMail(final @NonNull String email) {
        val index = email.lastIndexOf('@');
        if (index == -1 || index == email.length() - 1) {
            return true;
        }

        val domain = email.substring(index + 1).toLowerCase();

        return TEMP_MAIL_DOMAINS.contains(domain);
    }

    public void loadTempMailDomains() {
        val result = new HashSet<String>();

        try {
            val client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            val request = HttpRequest.newBuilder()
                    .uri(URI.create(TEMP_MAIL_LIST_URL))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            val response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                for (var line : response.body().split("\\R")) {
                    line = line.trim().toLowerCase();

                    if (!line.isEmpty() && !line.startsWith("#")) {
                        result.add(line);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[TempMailUtil] Failed to load temp mail domains", e);
        }

        log.info("[TempMailUtil] Loaded {} temp mail domains", result.size());

        TEMP_MAIL_DOMAINS = result;
    }
}
