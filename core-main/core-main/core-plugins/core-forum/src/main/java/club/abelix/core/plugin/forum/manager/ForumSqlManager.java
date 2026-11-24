package club.abelix.core.plugin.forum.manager;

import club.abelix.commons.language.Language;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.commons.mysql.modern.util.TableBuilder;
import club.abelix.core.app.data.gamer.Gamer;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import w.util.Env;

import java.util.AbstractCollection;
import java.util.concurrent.CompletableFuture;

public class ForumSqlManager {

    public static final String SELECT_ID_BY_MID = "SELECT `id` FROM `xf_server_link` WHERE `minecraft_id` = ? LIMIT 1";

    public static final String INSERT_LINK = "INSERT INTO `xf_server_link` (`id`, `minecraft_id`) VALUES (?, ?);";
    public static final String CHANGE_LANGUAGE = "UPDATE `xf_user` SET `language_id` = ? WHERE `user_id` = ?;";

    public static final String CHECK_EMAIL_USAGE = "SELECT 1 FROM `xf_user` WHERE `email` = ? LIMIT 1";

    private final TIntIntMap forumIdMap = new TIntIntHashMap();

    private final Database database;

    public ForumSqlManager() {
        database = new MySQL(new DatabaseCredentials(
                Env.getString("FORUM_MYSQL_DATABASE", "xenforo"),
                Env.getString("FORUM_MYSQL_USER", "xenforo"),
                Env.getString("FORUM_MYSQL_PASSWORD", "xenforo"),
                Env.getString("FORUM_MYSQL_HOST", "xenforo"),
                Env.getInt("FORUM_MYSQL_PORT", 3306),
                Env.getInt("FORUM_MYSQL_MAX_POOL_SIZE", 8)
        ));

        setupTable();
    }

    @SneakyThrows
    public CompletableFuture<Integer> getOrLoadForumId(final @NonNull Gamer gamer) {
        val cachedId = forumIdMap.get(gamer.getPlayerId());

        if (cachedId != 0) {
            return CompletableFuture.completedFuture(cachedId);
        }

        return loadLinkedId(gamer);
    }

    @SneakyThrows
    public CompletableFuture<Integer> loadLinkedId(final @NonNull Gamer gamer) {
        return database.executeQuery(SELECT_ID_BY_MID, gamer.getPlayerId()).thenApply(rows -> !rows.isEmpty()
                ? rows.getFirst().getRequiredInt("id")
                : 0
        );
    }

    public void addIdToCache(final @NonNull Gamer gamer, final int id) {
        forumIdMap.put(gamer.getPlayerId(), id);
    }

    public int getForumId(final @NonNull Gamer gamer) {
        return forumIdMap.get(gamer.getPlayerId());
    }

    public void removeLinkedId(final @NonNull Gamer gamer) {
        forumIdMap.remove(gamer.getPlayerId());
    }

    public CompletableFuture<Boolean> checkEmailAvailability(final @NonNull String email) {
        return database.executeQuery(CHECK_EMAIL_USAGE, email)
                .thenApply(AbstractCollection::isEmpty);
    }

    public void linkGameName(final @NonNull Gamer gamer) {
        val forumId = getForumId(gamer);

        if (forumId < 1) {
            return;
        }

        database.executeVoidUpdate(INSERT_LINK, forumId, gamer.getPlayerId());
        changeLanguage(forumId, gamer.getLanguage() == Language.RUSSIAN ? 4 : 1);
    }

    public void changeLanguage(final int forumId, final int languageId) {
        database.executeVoidUpdate(CHANGE_LANGUAGE, languageId, forumId);
    }

    private void setupTable() {
        database.executeVoidUpdate(new TableBuilder("xf_server_link")
                .addLine("id int(10) unsigned NOT NULL")
                .addLine("minecraft_id int(10) NOT NULL")
                .addLine("PRIMARY KEY (id)")
                .addLine("CONSTRAINT `FK1_Link_Id` FOREIGN KEY (`id`) REFERENCES `xf_user` (`user_id`) ON DELETE CASCADE")
                .toString());
    }
}
