package club.abelix.commons.util.gitlab;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import w.util.Env;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * @author milansky
 */
@Slf4j
@UtilityClass
public final class GitLab {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final int PROJECT_ID = Env.getInt("LOCALIZATION_PROJECT_ID", -1);
    private static final String BRANCH = Env.getString("LOCALIZATION_BRANCH", "main");
    private static final String GITLAB_HOST = "git.abelix.club", LOCALIZATION_KEY = Env.getString("LOCALIZATION_KEY", "");

    @SneakyThrows
    public InputStream downloadZip() {
        if (PROJECT_ID == -1 || LOCALIZATION_KEY.isEmpty())
            throw new IllegalStateException("Unable to download localization: LOCALIZATION_PROJECT_ID or LOCALIZATION_KEY is not set");

        val formatted = "https://" + GITLAB_HOST + "/api/v4/projects/" + PROJECT_ID +
                "/repository/archive.zip?access_token=" + LOCALIZATION_KEY + "&sha=" + BRANCH;

        log.info("Downloading localization from GitLab: {}", formatted);

        val request = HttpRequest.newBuilder()
                .uri(URI.create(formatted))
                .GET()
                .build();

        return CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
    }

}
