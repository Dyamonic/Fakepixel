package club.abelix.api.server.lobby.reward;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.commons.data.dailyrewards.DailyRewardSqlHandler;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.bukkit.HeartBeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@RequiredArgsConstructor
public class DailyRewardNotifyService {

    private final Map<Integer, Hologram> holograms = new ConcurrentHashMap<>();

    private final Location location;

    public void check(@NotNull final Gamer gamer) {
        DailyRewardSqlHandler.INSTANCE.loadPlayerInfo(gamer.getPlayerId()).thenAccept(dailyRewardEntry -> {
            int currentDay = (int) TimeUtil.getDayUTC();

            if (holograms.containsKey(gamer.getPlayerId())) {
                return;
            }

            if (dailyRewardEntry.getLastDay() < currentDay || dailyRewardEntry.getLastDay() == 0) {
                HeartBeat.runSync(() -> createHologram(gamer));
            }
        });
    }

    public void remove(Gamer gamer) {
        val holo = holograms.remove(gamer.getPlayerId());

        if (holo == null) return;

        holo.remove();

        log.info("Removed notification hologram for gamer: {}", gamer.getName());
    }

    private void createHologram(Gamer gamer) {
        log.info("Created notification hologram for gamer: {}", gamer.getName());
        val hologram = new Hologram(location.clone().add(0, 1.5, 0));

        hologram.setPublicStatus(false);
        hologram.addReceiver(gamer.getPlayer());
        hologram.addTextLine(Lang.of("rewards_npc_hologram_unclaimed").compile());

        holograms.put(gamer.getPlayerId(), hologram);
    }

}
