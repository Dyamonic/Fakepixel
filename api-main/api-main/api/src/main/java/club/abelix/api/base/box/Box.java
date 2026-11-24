package club.abelix.api.base.box;

import club.abelix.api.base.box.reward.BoxReward;
import club.abelix.api.base.box.type.BoxType;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

public interface Box {

    @NotNull
    String getNameKey();

    @NotNull
    BoxType getType();

    @NotNull
    Rarity getRarity();

    @Nullable
    String getMode();

    @Range(from = 0, to = Integer.MAX_VALUE)
    int getCost(@NotNull GameCurrency currency);

    boolean isCraftable();

    boolean isUpgradable();

    boolean isPurchasable(@NotNull GameCurrency currency);

    @NotNull
    @UnmodifiableView
    List<? super BoxReward> getRewards();

    void addReward(@NotNull BoxReward reward);

    @NotNull
    ItemStack getIcon(@NotNull Gamer gamer);

}

