package club.abelix.api.base.reward.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ExperienceReward implements Reward {

    protected final int experience;

    @Override
    public boolean give(@NotNull Gamer gamer) {
        gamer.addExperience(experience);
        return true;
    }

    @Override
    public String getRewardName(@NotNull Language language) {
        return language.getMessage("reward_experience_name",
                "%exp%", Leveling.formatGrouped(experience)
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.EXP_BOTTLE);
    }
}
