package club.abelix.core.app.data.group;

import club.abelix.commons.data.rank.service.UserRanksService;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.app.event.gamer.PlayerRedirectEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GroupListener implements Listener {

    UserRanksService userRanksService;

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onJoin(GamerJoinEvent event) {
        Gamer gamer = event.getGamer();
        Language language = gamer.getLanguage();

        LocalDateTime now = LocalDateTime.now();

        int amount = 0;

        for (val rank : gamer.getRanks().asSet()) {
            LocalDateTime expiring = rank.expiration();

            if (expiring != null) {
                long timeUntilExpire = java.time.Duration.between(now, expiring)
                        .toMillis();

                if (timeUntilExpire <= TimeUnit.DAYS.toMillis(3)) {
                    gamer.sendLocalizedMessage(
                            "group_expires",
                            "%rank%", rank.rank().getDisplayName(language),
                            "%time%", Duration.durationWordsMs(language, timeUntilExpire)
                    );

                    amount++;
                }
            }
        }

        if (amount > 0) {
            gamer.sendLocalizedMessage("group_expires_information");
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onRedirect(PlayerRedirectEvent event) {
        val gamer = event.getGamer();
        val expired = userRanksService.checkRankExpire(event.getGamer().getPlayerId());

        if (expired.isEmpty()) return;

        log.info("Player {} has {} expired ranks", gamer.getName(), expired.size());

        for (val userRank : expired) {
            gamer.sendLocalizedMessage("group_expired", "%rank%", userRank.rank().getDisplayName(gamer.getLanguage()));
        }

        gamer.loadRanks();
    }

}
