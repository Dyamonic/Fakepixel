package club.abelix.api.base.box;

import club.abelix.api.base.box.reward.BoxReward;
import club.abelix.api.base.box.type.BoxType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.network.type.mode.ServerMode;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Objects;

@ToString(exclude = "rewards")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public abstract class AbstractBox implements Box {

    @Getter
    @EqualsAndHashCode.Include
    String nameKey;

    @Getter
    @EqualsAndHashCode.Include
    BoxType type;

    int goldCost;
    int coinsCost;

    @Getter
    @EqualsAndHashCode.Include
    Rarity rarity;

    ServerMode mode;

    @Getter
    List<? super BoxReward> rewards;

    @Override
    public void addReward(@NotNull BoxReward reward) {
        rewards.add(reward);
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int getCost(
            @NotNull GameCurrency currency
    ) {
        return currency.equals(GameCurrency.GOLD) ? goldCost : coinsCost;
    }

    @Override
    public @Nullable String getMode() {
        return mode == null ? null : mode.getName();
    }

    @Override
    public boolean isPurchasable(@NotNull GameCurrency currency) {
        return true;
    }
}