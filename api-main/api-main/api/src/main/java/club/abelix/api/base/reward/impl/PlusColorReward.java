package club.abelix.api.base.reward.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.data.permission.prefix.defaults.RankPrefixModifier;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class PlusColorReward implements Reward {

    protected final RankPrefixModifier prefixModifier;

    @Override
    public boolean give(@NonNull Gamer gamer) {
        return true;
    }

    @Override
    public String getRewardName(@NonNull Language language) {
        return language.getMessage(
                "reward_plus_color_name",
                "%plus_color%", language.getMessage("plus_color_%s_name"
                        .formatted(prefixModifier.getColor().getName()))
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.PAPER);
    }

}
