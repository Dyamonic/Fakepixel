package club.abelix.core.votifer.manager;

import club.abelix.commons.data.vote.VotePlatform;
import club.abelix.commons.data.vote.VoteSqlHandler;
import club.abelix.commons.util.random.RandomUtil;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.app.api.config.Configuration;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.packet.play.server.SPlayerVote;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import club.abelix.core.votifer.VotifierCorePlugin;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static club.abelix.commons.data.vote.VoteSqlHandler.EXPERIENCE_FOR_COMBO_VOTE;
import static club.abelix.commons.data.vote.VoteSqlHandler.EXPERIENCE_FOR_VOTE;

@Slf4j
@RequiredArgsConstructor
public class VoteManager {

    private final VotifierCorePlugin votifierCorePlugin;

    /**
     * Сообщения о покупке клаудов и привилегии
     */
    private List<String> monitoringVoteMessages;

    private int messageBroadcastInterval;

    /**
     * Количество проголосовавших за последние N минут
     */
    private int votesCount;

    private ScheduledExecutorService autoMessageExecutor;

    public void reloadConfiguration() {
        votifierCorePlugin.loadConfig();

        Configuration configuration = votifierCorePlugin.getConfig();

        messageBroadcastInterval = configuration.getInt("Interval", 2);
        monitoringVoteMessages = configuration.getStringList("vote_messages");
    }

    public void runAutoMessageTask() {
        if (autoMessageExecutor != null) {
            autoMessageExecutor.shutdown();
            autoMessageExecutor = null;
        }

        autoMessageExecutor = Executors.newSingleThreadScheduledExecutor();

        autoMessageExecutor.scheduleAtFixedRate(() -> {
            if (votesCount > 0) {
                String message = RandomUtil.getRandomElement(monitoringVoteMessages)
                        .replace("%votes%", String.valueOf(votesCount))
                        .replace("%players_format%", "поддержал" + TimeUtil.getEnding(votesCount, "", "и", "и"))
                        .replace("%players_string%", TimeUtil.getEnding(votesCount, "игрок", "игрока", "игроков"))
                        .replace("%votes_format%", "отдал" + TimeUtil.getEnding(votesCount, "", "и", "и"))
                        .replace("%votes_string%", TimeUtil.getEnding(votesCount, "голос", "голоса", "голосов"));

                ServerManager.IMP.broadcastChat(message);
            }

            votesCount = 0;
        }, messageBroadcastInterval, messageBroadcastInterval, TimeUnit.MINUTES);
    }

    public void processVote(@NonNull VotePlatform platform, @NonNull String playerName) {
        Gamer player = Gamer.getOfflineGamer(playerName, Gamer::loadProperties);

        if (player == null) {
            return;
        }

        VoteSqlHandler.INSTANCE.getPlayerVotes(player.getPlayerId()).thenAccept(voteData -> {
            // игрок уже проголосовал за этот мониторинг. игнорим
            if (voteData.isVotedToday(platform)) {
                log.warn("Player {} tried to get reward for already voted monitoring (platform {})",
                        player.getName(), platform.name());
                return;
            }

            LocalDateTime timestamp = LocalDateTime.now();
            ZonedDateTime zonedTimestamp = timestamp.atZone(ZoneId.systemDefault());

            // добавляем голос в базу данных и отправляем на сервер,
            // если прогосовавший игрок онлайн
            VoteSqlHandler.INSTANCE.addPlayerVote(player.getPlayerId(), platform, timestamp);
            voteData.addVote(platform, timestamp);

            if (player.isOnline()) {
                player.getServer().sendPacket(new SPlayerVote(
                        player.getPlayerId(),
                        platform.platformId(),
                        zonedTimestamp.toInstant().toEpochMilli()
                ));

                player.sendLocalizedMessage("vote_received", "%platform%", platform.name());
                player.sendLocalizedMessage("vote_experience_reward",
                        "%experience%", EXPERIENCE_FOR_VOTE);
            }

            player.addProperty("Votes", 1);
            player.addProperty("Experience", EXPERIENCE_FOR_VOTE);

            int currentDay = (int) TimeUtil.getDayUTC();

            // игрок проголосовал на всех мониторингах за сегодня,
            // и последний комбо-голос он получал тоже не сегодня,
            // значит он может получить комбо-голос
            if (voteData.isVotedOnAllPlatforms()
                    && currentDay != player.getProperty("LastComboVote")) {

                player.addProperty("ComboVotes", 1);
                player.setProperty("LastComboVote", currentDay);

                player.addProperty("Experience", EXPERIENCE_FOR_COMBO_VOTE);

                if (player.isOnline()) {
                    player.sendLocalizedMessage("vote_combo");
                    player.sendLocalizedMessage("vote_combo_experience_reward",
                            "%experience%", EXPERIENCE_FOR_VOTE);
                }
            }

            votesCount++;
        });
    }

}
