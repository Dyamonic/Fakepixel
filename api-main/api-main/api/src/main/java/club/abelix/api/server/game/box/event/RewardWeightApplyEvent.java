package club.abelix.api.server.game.box.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RewardWeightApplyEvent extends GameEvent {

    Gamer gamer;
    int weight;

    public RewardWeightApplyEvent(
            Game game,
            Gamer gamer,
            int weight
    ) {
        super(game);

        this.gamer = gamer;
        this.weight = weight;
    }

}
