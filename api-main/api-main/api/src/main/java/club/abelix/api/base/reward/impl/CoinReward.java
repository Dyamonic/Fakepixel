package club.abelix.api.base.reward.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Uncommon;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CoinReward implements Reward {

    protected final int amount;
    protected Uncommon uncommon;
    protected Master master;

    public CoinReward(int amount) {
        this.amount = amount;
    }

    public CoinReward(@NonNull Uncommon uncommon, int amount) {
        this(amount);

        this.uncommon = uncommon;
    }

    public CoinReward(@NonNull Master master, int amount) {
        this(amount);

        this.master = master;
    }

    @Override
    public boolean give(@NonNull Gamer gamer) {
        if (uncommon != null) {
            gamer.addCoins(uncommon, amount, true, null);
        } else if (master != null) {
            gamer.addCoins(master, amount, true, null);
        }

        return true;
    }

    @Override
    public String getRewardName(@NotNull Language language) {
        boolean hasMode = uncommon != null || master != null;

        return language.getMessage(hasMode ? "reward_coin_mode_name" : "reward_coin_name",
                "%mode%", uncommon != null ? uncommon.getDisplayName() : master != null ? master.getDisplayName() :
                        "",
                "%amount%", CommonWords.COIN.get(language).withNumber(amount)
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.DOUBLE_PLANT);
    }
}
