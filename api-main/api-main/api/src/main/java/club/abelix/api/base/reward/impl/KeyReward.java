package club.abelix.api.base.reward.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.data.cases.CaseType;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Deprecated(forRemoval = true)
@RequiredArgsConstructor
public class KeyReward implements Reward {

    protected final CaseType type;
    protected final int amount;

    public KeyReward(@NonNull CaseType type) {
        this(type, 1);
    }

    @Override
    public boolean give(@NonNull Gamer gamer) {
        gamer.addProperty(type.name(), amount);
        return true;
    }

    @Override
    public String getRewardName(@NotNull Language language) {
        return language.getMessage(
                "reward_case_key_name",
                "%type%", type.getDisplayName(language),
                "%amount%", amount
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.TRIPWIRE_HOOK, amount);
    }
}
