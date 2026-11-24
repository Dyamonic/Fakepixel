package club.abelix.api.base.box.reward.standard;

import club.abelix.api.base.box.reward.AbstractBoxReward;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.GameItemCategory;
import club.abelix.api.base.item.predicate.CoinsPredicate;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.CoinType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.language.Language;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameItemReward extends AbstractBoxReward implements club.abelix.api.base.box.reward.gameitem.GameItemReward {

    @Getter
    GameItem gameItem;

    public GameItemReward(
            Rarity rarity,
            GameItem gameItem,
            int chance
    ) {
        super(
                rarity,
                chance
        );

        this.gameItem = gameItem;
    }

    @Override
    public @NotNull String getName(@NotNull Language language) {
        return gameItem.getDisplayName(language);
    }

    @Override
    public @UnmodifiableView @NotNull List<@NotNull String> getDescription(@NotNull Language language) {
        return Collections.singletonList(gameItem.getDisplayName(language));
    }

    @Override
    public boolean apply(@NotNull Gamer gamer) {
        int id = gameItem.getId();

        if (gamer.hasPurchase(id)) {
            return false;
        }

        gamer.addPurchase(id, 0, GameCurrency.OTHER);
        return true;
    }

    @Override
    public @NotNull ItemStack getRewardIcon(@NonNull Language language) {
        return gameItem.getIcon(language);
    }

    @Override
    public int getEquivalent(@NotNull Gamer gamer) {
        CoinsPredicate coins = gameItem.getCoinsPredicate();

        if (coins == null) {
            return 0;
        }

        int cost = coins.getCost();

        return (int) (cost * 0.2);
    }

    @Override
    public @NotNull EquivalentType getEquivalentType() {
        CoinsPredicate coins = gameItem.getCoinsPredicate();

        return coins == null || coins.getType() == GameCurrency.COINS ?
                new CoinType() :
                new GoldType();
    }

    @Override
    public void sendCustomRewardMessage(@NotNull Gamer gamer) {}

    @Override
    public @NotNull GameItemCategory getCategory() {
        return gameItem.getCategory();
    }
}
