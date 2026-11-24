package club.abelix.api.base.reward.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.data.suffix.Suffix;
import club.abelix.commons.data.suffix.SuffixSqlHandler;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class SuffixReward implements Reward {

    protected final Suffix suffix;

    public SuffixReward(int suffixId) {
        this.suffix = SuffixSqlHandler.INSTANCE.getSuffixById(suffixId);
    }

    @Override
    public boolean give(@NonNull Gamer gamer) {
        if (gamer.isSuffixPurchased(suffix)) {
            return false;
        }

        gamer.addPurchasedSuffix(suffix.getId());

        return true;
    }

    @Override
    public String getRewardName(@NonNull Language language) {
        return language.getMessage(
                "reward_suffix_name",
                "%suffix%", suffix.getDisplaySuffix()
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.NAME_TAG);
    }

}
