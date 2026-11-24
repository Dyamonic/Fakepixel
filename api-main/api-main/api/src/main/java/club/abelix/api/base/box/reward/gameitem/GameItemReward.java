package club.abelix.api.base.box.reward.gameitem;

import club.abelix.api.base.box.reward.BoxReward;
import club.abelix.api.base.item.GameItemCategory;
import org.jetbrains.annotations.NotNull;

public interface GameItemReward extends BoxReward {

    @NotNull GameItemCategory getCategory();

}
