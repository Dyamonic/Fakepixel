package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerEvent;
import club.abelix.commons.data.stat.StatsField;
import lombok.Getter;

@Getter
public class GameStatsChangeEvent extends GamerEvent {

    private final StatsField statsField;
    private final int delta, totalStatValue;

    public GameStatsChangeEvent(Gamer gamer, StatsField field, int delta, int total) {
        super(true, gamer);

        this.statsField = field;
        this.delta = delta;
        this.totalStatValue = total;
    }
}
