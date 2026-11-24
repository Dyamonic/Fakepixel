package club.abelix.api.server.lobby.mysterybox.content;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.predicate.CoinsPredicate;
import club.abelix.api.base.reward.impl.GameItemReward;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.CoinType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.word.WordCase;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class GameItemContent extends GameItemReward implements CaseContent {

    @Getter
    private final Rarity rarity;

    public GameItemContent(@NonNull Rarity rarity, @NonNull GameItem gameItem) {
        super(gameItem);

        this.rarity = rarity;
    }

    @Override
    public String getRewardName(@NotNull Language language) {
        String categoryWord = gameItem.getCategory().getWord()
                .get(language)
                .withCase(WordCase.NOMINATIVE)
                .firstUp()
                .toString();

        return language.getMessage(
                "case_content_game_item",
                "%rarity%", getRarity().getColor(),
                "%category%", categoryWord,
                "%name%", gameItem.getDisplayName(language)
        );
    }

    @Override
    public int getDuplicateEquivalent(@NonNull Gamer gamer) {
        CoinsPredicate coins = gameItem.getCoinsPredicate();

        if (coins == null) {
            return 0;
        }

        int cost = coins.getCost();

        return (int) (cost * 0.2);
    }

    @Override
    public EquivalentType getEquivalentType() {
        CoinsPredicate coins = gameItem.getCoinsPredicate();

        return coins == null || coins.getType() == GameCurrency.COINS ?
                CoinType.INSTANCE :
                GoldType.INSTANCE;
    }

}
