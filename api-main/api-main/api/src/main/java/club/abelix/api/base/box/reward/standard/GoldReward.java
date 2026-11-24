package club.abelix.api.base.box.reward.standard;

import club.abelix.api.base.box.reward.AbstractBoxReward;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.language.Language;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

public class GoldReward extends AbstractBoxReward {

    private final int amount;

    public GoldReward(
            Rarity rarity,
            int amount,
            int chance

    ) {
        super(rarity, chance);

        this.amount = amount;
    }

    @Override
    public @NotNull String getName(@NotNull Language language) {
        return language.getMessage("box_reward_gold", "%amount%", amount);
    }

    @Override
    public @UnmodifiableView @NotNull List<@NotNull String> getDescription(@NotNull Language language) {
        return List.of();
    }

    @Override
    public boolean apply(@NotNull Gamer gamer) {
        gamer.addGold(amount);
        return true;
    }

    @Override
    public @NotNull ItemStack getRewardIcon(@NotNull Language language) {
        return ItemUtil.getBuilder(Material.GOLD_INGOT).build();
    }

    @Override
    public @NotNull EquivalentType getEquivalentType() {
        return new GoldType();
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int getEquivalent(@NotNull Gamer gamer) {
        return amount;
    }
}
