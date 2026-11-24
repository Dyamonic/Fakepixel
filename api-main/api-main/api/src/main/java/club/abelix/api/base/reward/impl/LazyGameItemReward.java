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
import lombok.val;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class LazyGameItemReward implements Reward {
    
    @Getter
    protected final Supplier<GameItem> lazyGameItem;
    

    @Override
    public String getRewardName(@NotNull Language language) {
        val item = lazyGameItem.get();
        return language.getMessage(
                "reward_game_item_name",
                "%name%", item.getDisplayName(language),
                "%category%", getGameCategoryName(language)
        );
    }

    @Override
    public boolean give(@NotNull Gamer gamer) {
        val item = lazyGameItem.get();
        int id = item.getId();

        if (gamer.hasPurchase(id)) {
            return false;
        }

        gamer.addPurchase(id, 0, GameCurrency.GOLD);
        return true;
    }

    @Override
    public ItemStack getRewardIcon(@NotNull Language language) {
        val item = lazyGameItem.get();
        return item.getIcon(language);
    }

    protected final String getGameCategoryName(@NonNull Language language) {
        val item = lazyGameItem.get();
        return item.getCategory().getWord()
                .get(language)
                .withCase(WordCase.NOMINATIVE)
                .firstUp()
                .toString();
    }
}

