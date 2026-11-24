package club.abelix.api.base.box.reward;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.language.Language;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

public interface BoxReward {

    @NotNull
    Rarity getRarity();

    @NotNull
    String getName(@NotNull Language language);

    @UnmodifiableView
    @NotNull
    List<@NotNull String> getDescription(@NotNull Language language);

    @Range(from = 0, to = 100)
    int getChance();

    boolean apply(@NotNull Gamer gamer);

    @NotNull ItemStack getRewardIcon(@NotNull Language language);

    @NotNull EquivalentType getEquivalentType();

    @Range(from = 0, to = Integer.MAX_VALUE)
    int getEquivalent(@NotNull Gamer gamer);

    void sendCustomRewardMessage(@NotNull Gamer gamer);
}
