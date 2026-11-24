package club.abelix.api.server.lobby.mysterybox.content;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.predicate.CoinsPredicate;
import club.abelix.api.base.reward.impl.LazyGameItemReward;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.CoinType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.word.WordCase;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class lazyGameItemContent extends LazyGameItemReward implements CaseContent {

    @Getter
    private final Rarity rarity;

    public lazyGameItemContent(@NonNull Rarity rarity, @NonNull Supplier<GameItem> gameItem) {
        super(gameItem);

        this.rarity = rarity;
    }

    @Override
    public String getRewardName(@NotNull Language language) {
        val item = lazyGameItem.get();

        String categoryWord = item.getCategory().getWord()
                .get(language)
                .withCase(WordCase.NOMINATIVE)
                .firstUp()
                .toString();

        return language.getMessage(
                "case_content_game_item",
                "%rarity%", getRarity().getColor(),
                "%category%", categoryWord,
                "%name%", item.getDisplayName(language)
        );
    }

    @Override
    public int getDuplicateEquivalent(@NonNull Gamer gamer) {
        val item = lazyGameItem.get();
        CoinsPredicate coins = item.getCoinsPredicate();

        if (coins == null) {
            return 0;
        }

        int cost = coins.getCost();

        return (int) (cost * 0.2);
    }

    @Override
    public EquivalentType getEquivalentType() {
        CoinsPredicate coins = lazyGameItem.get().getCoinsPredicate();

        return coins == null || coins.getType() == GameCurrency.COINS ?
                CoinType.INSTANCE :
                GoldType.INSTANCE;
    }

}

