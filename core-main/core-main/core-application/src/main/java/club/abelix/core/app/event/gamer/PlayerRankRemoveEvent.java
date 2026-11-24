package club.abelix.core.app.event.gamer;

import club.abelix.commons.data.rank.UserRank;
import club.abelix.core.app.data.gamer.Gamer;

public class PlayerRankRemoveEvent extends RankChangedEvent {

    public PlayerRankRemoveEvent(Gamer gamer, UserRank rank) {
        super(gamer, rank);
    }
}
