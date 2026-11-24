package club.abelix.core.app.event.gamer;

import club.abelix.commons.data.rank.UserRank;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Getter;

public class RankChangedEvent extends GamerEvent {

    @Getter
    private final UserRank rank;

    public RankChangedEvent(Gamer gamer, UserRank rank) {
        super(gamer);

        this.rank = rank;
    }
}
