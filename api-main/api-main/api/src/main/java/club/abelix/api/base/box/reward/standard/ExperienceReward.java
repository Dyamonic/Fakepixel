package club.abelix.api.base.box.reward.standard;

import club.abelix.api.base.box.reward.AbstractBoxReward;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.language.Language;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExperienceReward extends AbstractBoxReward {

    private final int amount;

    public ExperienceReward(Rarity rarity, int amount, int chance) {
        super(rarity, chance);

        this.amount = amount;
    }

    @Override
    public @NotNull String getName(@NotNull Language language) {
        return language.getMessage("box_exp_reward", "%amount%", amount);
    }

    @Override
    public @UnmodifiableView @NotNull List<@NotNull String> getDescription(@NotNull Language language) {
        return List.of();
    }

    @Override
    public boolean apply(@NotNull Gamer gamer) {
        gamer.addExperience(amount);
        return true;
    }

    @Override
    public @NotNull ItemStack getRewardIcon(@NotNull Language language) {
        return ItemUtil.getBuilder(Material.EXP_BOTTLE).build();
    }

    @Override
    public @NotNull EquivalentType getEquivalentType() {
        return new GoldType();
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int getEquivalent(@NotNull Gamer gamer) {
        val random = ThreadLocalRandom.current();
        int minBounds = getRarity().ordinal() * 2;
        int maxBounds = getRarity().ordinal() * 4;

        return random.nextInt(minBounds, maxBounds);
    }
}
