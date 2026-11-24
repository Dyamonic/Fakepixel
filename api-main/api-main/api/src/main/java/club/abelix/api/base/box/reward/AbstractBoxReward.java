package club.abelix.api.base.box.reward;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class AbstractBoxReward implements BoxReward {

    @Getter
    Rarity rarity;

    @Getter
    int chance;

    @Override
    public void sendCustomRewardMessage(@NotNull Gamer gamer) {
        // by default doing nothing
    }
}
