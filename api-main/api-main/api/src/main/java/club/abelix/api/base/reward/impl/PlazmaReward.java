package club.abelix.api.base.reward.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.word.CommonWords;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class PlazmaReward implements Reward {

    protected final int amount;

    @Override
    public boolean give(@NonNull Gamer gamer) {
        gamer.addGold(amount);
        return true;
    }

    @Override
    public String getRewardName(@NotNull Language language) {
        return language.getMessage("reward_gold_name",
                "%amount%", CommonWords.PLAZMA.get(language).withNumber(amount)
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.TRIPWIRE_HOOK, amount);
    }
}
