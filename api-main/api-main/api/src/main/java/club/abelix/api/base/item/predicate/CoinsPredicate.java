package club.abelix.api.base.item.predicate;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.language.Language;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Uncommon;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;

public class CoinsPredicate implements GameItemPredicate {

    // Проверка на наличие монет/плазмы у игрока

    @Getter
    private final GameCurrency type;
    @Getter
    private final int cost;
    private Uncommon uncommon;
    private Master master;

    public CoinsPredicate(GameCurrency type, int value) {
        this.type = type;
        this.cost = value;
    }

    public CoinsPredicate(@NonNull Uncommon uncommon, int coins) {
        this(GameCurrency.COINS, coins);

        this.uncommon = uncommon;
    }

    public CoinsPredicate(@NonNull Master master, int coins) {
        this(GameCurrency.COINS, coins);

        this.master = master;
    }

    @Override
    public boolean match(Gamer gamer) {
        return getBalance(gamer) >= getCost();
    }

    @Override
    public List<String> getRequirementLore(GameItem item, Gamer gamer) {
        Language language = gamer.getLanguage();

        int cost = getCost();
        int balance = getBalance(gamer);

        if (cost <= 0) return List.of();

        return Collections.singletonList(
                language.getMessage(
                        "predicate_currency_requirement",
                        "%cost%", type.getWord().get(gamer.getLanguage()).withNumber(cost),
                        "%balance%", type.getWord().get(language).withNumber(balance)
                )
        );
    }

    public List<String> getPurchaseAvailableLore(Gamer gamer) {
        Language language = gamer.getLanguage();
        int cost = getCost();

        return language.getMessageList("predicate_currency_purchase",
                "%cost%", type.getWord().get(language).withNumber(cost));
    }

    @Override
    public int withdraw(@NonNull Gamer gamer) {
        int cost = getCost();

        switch (type) {
            case COINS -> {
                if (uncommon != null) {
                    gamer.takeCoins(uncommon, cost);
                } else if (master != null) {
                    gamer.takeCoins(master, cost);
                }
            }
            case GOLD -> gamer.takeGold(cost);
        }

        return cost;
    }

    private int getBalance(Gamer gamer) {
        if (type == GameCurrency.COINS) {
            if (uncommon != null) {
                return gamer.getCoins(uncommon);
            } else if (master != null) {
                return gamer.getCoins(master);
            }
        }

        return gamer.getGold();
    }

}
