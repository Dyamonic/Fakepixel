package club.abelix.commons.data.nickname;

import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.di.core.DiStaticAccess;
import lombok.NonNull;
import lombok.val;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class NickSqlHandler {

    private static final LocalDateTime INFINITE = LocalDateTime.of(2030, 1, 1, 0, 0);
    private static final String GET_ACTIVE_NICK = "SELECT `Id`, `Nick_Id`, `Group`, `Issue_Time` FROM `NickHistory` WHERE `Id` = ? AND `End_Time` IS NULL;";
    private static final String GET_NICK_AVAILABILITY = "SELECT COUNT(*) AS `count` FROM `NickHistory` WHERE `Nick_Id` = ? AND `End_Time` IS NULL";
    private static final String GET_RANDOM_NICKS = "SELECT `Nick` FROM `NickDatabase` WHERE (`Cooldown` IS NULL OR `Cooldown` < CURRENT_TIMESTAMP()) AND `Public` = 1 ORDER BY RAND() LIMIT ?;";
    private static final String FETCH_NICK_BY_ID = "SELECT `Nick` FROM `NickDatabase` WHERE `Id` = ?";
    private static final String FETCH_ID_BY_NICK = "SELECT `Id`, `Nick` FROM `NickDatabase` WHERE `Nick` = ?";
    private static final String INSERT_NICK = "INSERT INTO `NickDatabase` (`Nick`, `Public`) VALUES (?, 1);";
    private static final String DISABLE_NICK = "UPDATE `NickDatabase` SET `Public` = 0 WHERE `Nick` = ?";
    private static final String CHANGE_PLAYER_NICK = "INSERT INTO `NickHistory` (`Id`, `Nick_Id`, `Group`, `Issue_Time`) VALUES (?, ?, ?, ?);";
    private static final String RESET_PLAYER_NICK = "UPDATE `NickHistory` SET `End_Time` = ? WHERE `Id` = ? AND `End_Time` IS NULL";
    private static final String ACTIVATE_NICK_COOLDOWN = "UPDATE `NickDatabase` SET `Cooldown` = ? WHERE `Id` = ?";
    private static final String GET_NICK_HISTORY = "SELECT * FROM `NickHistory` WHERE `Nick_Id` = ? ORDER BY `Issue_Time` ASC;";
    public static NickSqlHandler INSTANCE = new NickSqlHandler();
    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public CompletableFuture<List<FakePlayerNickname.HistoryEntry>> getNicknameHistory(@NonNull String nick) {
        return fetchNickData(nick, false).thenApply(nickname -> {
            if (nickname == null) {
                return Collections.emptyList();
            }

            return database.executeQuery(GET_NICK_HISTORY, nickname.getNickId()).thenApply(rows -> {
                List<FakePlayerNickname.HistoryEntry> nicknames = new ArrayList<>();

                for (BufferedRow row : rows) {
                    int issuerId = row.getInt("Id").orElseThrow();

                    LocalDateTime issueDate = row.getRequiredTimestamp("Issue_Time");
                    LocalDateTime endDate = row.getTimestamp("End_Time").orElse(null);

                    DiStaticAccess.BEAN_SERVICE.withBean(RanksService.class, rankService -> {
                        val rank = rankService.findRankById(row.getRequiredInt("Group")).orElse(rankService.defaultRank());

                        FakePlayerNickname fakeNick = new FakePlayerNickname(issuerId, nickname, rank);

                        nicknames.add(new FakePlayerNickname.HistoryEntry(fakeNick, issueDate, endDate));
                    });
                }

                return nicknames;
            }).join();
        });
    }

    public CompletableFuture<Set<String>> getRandomNicknames(int count) {
        return database.executeQuery(GET_RANDOM_NICKS, count).thenApply(rows -> {
            Set<String> nicks = new HashSet<>();

            for (BufferedRow row : rows) {
                nicks.add(row.getString("Nick").orElseThrow());
            }

            return nicks;
        });
    }

    public CompletableFuture<FakeNickname> fetchNickData(@NonNull String nick, boolean insertNew) {
        return database.executeQuery(FETCH_ID_BY_NICK, nick).thenApply(rows -> {
            if (rows.isEmpty()) {
                return insertNew ? insertNewNick(nick).join() : null;
            }

            BufferedRow row = rows.getFirst();

            return new FakeNickname(
                    row.getRequiredInt("Id"),
                    row.getRequiredString("Nick")
            );
        });
    }

    public CompletableFuture<FakeNickname> fetchNickData(int nickId) {
        return database.executeQuery(FETCH_NICK_BY_ID, nickId)
                .thenApply(rows -> {
                    if (rows.isEmpty()) return null;

                    BufferedRow row = rows.getFirst();

                    return new FakeNickname(
                            nickId,
                            row.getRequiredString("Nick")
                    );
                });
    }

    public CompletableFuture<FakeNickname> insertNewNick(@NonNull String nick) {
        return database.executeUpdate(INSERT_NICK, nick)
                .thenApply(query -> new FakeNickname(query.getGeneratedKey(), nick));
    }

    public void disableNickname(@NonNull String nick) {
        database.executeVoidUpdate(DISABLE_NICK, nick);
    }

    public void clearPlayerNickname(int nickId, int playerId) {
        LocalDateTime now = LocalDateTime.now();

        database.executeVoidUpdate(ACTIVATE_NICK_COOLDOWN, now.plusDays(2), nickId).join();
        database.executeVoidUpdate(RESET_PLAYER_NICK, now, playerId).join();
    }

    public void clearPlayerNickname(@NonNull FakePlayerNickname playerNick) {
        clearPlayerNickname(playerNick.getNickname().getNickId(), playerNick.getPlayerId());
    }

    public CompletableFuture<FakePlayerNickname> changePlayerNickname(int playerId, @NonNull String nick, @NonNull Rank rank) {
        return fetchNickData(nick, true).thenApply(nickname -> {
            FakePlayerNickname playerNick = new FakePlayerNickname(playerId, nickname, rank);

            database.executeVoidUpdate(ACTIVATE_NICK_COOLDOWN,
                    INFINITE, nickname.getNickId());
            database.executeVoidUpdate(CHANGE_PLAYER_NICK,
                    playerId, nickname.getNickId(), playerNick.getGroup().getId(), LocalDateTime.now());

            return playerNick;
        });
    }

    public CompletableFuture<FakePlayerNickname> getPlayerNickname(int playerId) {
        return database.executeQuery(GET_ACTIVE_NICK, playerId).thenApply(rows -> {
            if (rows.isEmpty()) {
                return null;
            }

            BufferedRow row = rows.getFirst();

            int nickId = row.getRequiredInt("Nick_Id");
            LocalDateTime issueTime = row.getRequiredTimestamp("Issue_Time");

            if (Duration.between(issueTime, LocalDateTime.now()).toDays() > 30) {
                clearPlayerNickname(nickId, playerId);
                return null;
            }

            FakeNickname nickname = fetchNickData(nickId).join();

            if (nickname == null) {
                return null;
            }

            val rankService = DiStaticAccess.BEAN_SERVICE.getBean(RanksService.class).orElseThrow();
            val rank = rankService
                    .findRankById(row.getRequiredInt("Group"))
                    .orElse(rankService.defaultRank());

            return new FakePlayerNickname(playerId, nickname, rank);
        });
    }

    public CompletableFuture<Boolean> checkNicknameAvailability(@NonNull String nick) {
        return fetchNickData(nick, false).thenApply(nickname -> {
            if (nickname == null) {
                return true;
            }

            return database.executeQuery(GET_NICK_AVAILABILITY, nickname.getNickId())
                    .thenApply(rows -> rows.getFirst().getRequiredInt("count") == 0).join();
        });
    }
}