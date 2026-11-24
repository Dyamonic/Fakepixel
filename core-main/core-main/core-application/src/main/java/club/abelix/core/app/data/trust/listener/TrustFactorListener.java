package club.abelix.core.app.data.trust.listener;

import club.abelix.commons.data.trust.TrustFactor;
import club.abelix.commons.data.trust.service.TrustService;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class TrustFactorListener implements Listener {

    private static final int RECALCULATE_THRESHOLD_MINUTES = 30;

    private final TrustService trustService;

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerJoin(GamerJoinEvent event) {
        recalculateIfNeeded(event.getGamer(), "join");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerQuit(PlayerQuitEvent event) {
        recalculateIfNeeded(event.getGamer(), "quit");
    }

    private void recalculateIfNeeded(Gamer gamer, String trigger) {
        trustService.getTrustFactor(gamer.getPlayerId())
            .thenAccept(optional -> {
                if (optional.isEmpty()) {
                    log.debug("[{}] No trust factor for {}, recalculating (first time)", trigger, gamer.getName());
                    doRecalculate(gamer, trigger);
                    return;
                }

                TrustFactor tf = optional.get();
                LocalDateTime threshold = LocalDateTime.now().minusMinutes(RECALCULATE_THRESHOLD_MINUTES);

                if (tf.getLastRecalculated() == null || tf.getLastRecalculated().isBefore(threshold)) {
                    log.debug("[{}] Trust factor for {} is stale (last: {}), recalculating",
                        trigger, gamer.getName(), tf.getLastRecalculated());
                    doRecalculate(gamer, trigger);
                } else {
                    log.debug("[{}] Trust factor for {} is fresh (level={}, score={}), skipping",
                        trigger, gamer.getName(), tf.getLevel(), tf.getScore());
                }
            })
            .exceptionally(t -> {
                log.error("[{}] Error checking trust factor for {}", trigger, gamer.getName(), t);
                return null;
            });
    }

    private void doRecalculate(Gamer gamer, String trigger) {
        trustService.recalculateTrustFactor(gamer.getPlayerId())
            .thenAccept(tf -> log.debug("[{}] Recalculated trust for {}: level={}, score={}, playtime={}h",
                trigger, gamer.getName(), tf.getLevel(), tf.getScore(),
                String.format("%.1f", tf.getPlaytimeHours())))
            .exceptionally(t -> {
                log.error("[{}] Error recalculating trust factor for {}", trigger, gamer.getName(), t);
                return null;
            });
    }
}
