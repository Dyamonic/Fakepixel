package club.abelix.api.base.reward.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MysteryDustReward implements Reward {
    protected final int amount;

    public MysteryDustReward(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean give(@NonNull Gamer gamer) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dust add " + gamer.getName() + " " + amount);
        return true;
    }

    @Override
    public String getRewardName(@NotNull Language language) {
        return language.getMessage(
                "reward_mysterydust_name",
                "%amount%", amount
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.TRIPWIRE_HOOK, amount);
    }
}
