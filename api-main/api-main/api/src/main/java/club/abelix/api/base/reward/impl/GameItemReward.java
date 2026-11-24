package club.abelix.api.base.reward.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.word.WordCase;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class GameItemReward implements Reward {

    @Getter
    protected final GameItem gameItem;

    @Override
    public String getRewardName(@NotNull Language language) {
        return language.getMessage(
                "reward_game_item_name",
                "%name%", gameItem.getDisplayName(language),
                "%category%", getGameCategoryName(language)
        );
    }

    @Override
    public boolean give(@NotNull Gamer gamer) {
        int id = gameItem.getId();

        if (gamer.hasPurchase(id)) {
            return false;
        }

        gamer.addPurchase(id, 0, GameCurrency.GOLD);
        return true;
    }

    @Override
    public ItemStack getRewardIcon(@NotNull Language language) {
        return gameItem.getIcon(language);
    }

    protected final String getGameCategoryName(@NonNull Language language) {
        return gameItem.getCategory().getWord()
                .get(language)
                .withCase(WordCase.NOMINATIVE)
                .firstUp()
                .toString();
    }
}
