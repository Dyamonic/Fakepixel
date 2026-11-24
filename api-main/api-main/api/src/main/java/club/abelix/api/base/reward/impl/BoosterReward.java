package club.abelix.api.base.reward.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.data.booster.BoosterType;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class BoosterReward implements Reward {

    protected final BoosterType type;
    protected final int amount;

    public BoosterReward(@NonNull BoosterType type) {
        this(type, 1);
    }

    @Override
    public boolean give(@NonNull Gamer gamer) {
        gamer.addProperty(type.name(), amount);
        return true;
    }

    @Override
    public String getRewardName(@NonNull @NotNull Language language) {
        return language.getMessage(
                "reward_booster_name",
                "%name%", type.getDisplayName(language),
                "%amount%", amount
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.GLASS_BOTTLE);
    }
}
