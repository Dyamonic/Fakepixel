package club.abelix.api.base.reward.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Deprecated(forRemoval = true)
@RequiredArgsConstructor
public class MysteryBoxReward implements Reward {
    protected final int quality;
    protected final int amount;

    private static final String STAR = "✰";
    private static final ChatColor color = ChatColor.YELLOW;

    public MysteryBoxReward(int quality) {
        this(quality, 1);
    }

    @Override
    public boolean give(@NonNull Gamer gamer) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gmb give " + gamer.getName() + " " + amount + " " + quality);
        return true;
    }

    @Override
    public String getRewardName(@NotNull Language language) {
        return language.getMessage(
                "reward_mysteryboxes_name",
                "%amount%", amount,
                "%quality%", getQuality(quality)
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.TRIPWIRE_HOOK, amount);
    }

    /**
     * Вернуть качество мистери бокса в виде звёзд
     *
     * @param quality - Качество (МАКС ЗНАЧЕНИЕ 5)
     * @return качество в виде звёзд
     */
    private static String getQuality(@NotNull int quality) {
        if (quality > 5) return "ADMIN TUPOI";

        StringBuffer sb = new StringBuffer();
        sb.append(color);
        for (int i = 0; i < quality; i++) {
            sb.append("✰");
        }
        sb.append(ChatColor.GRAY);
        for (int i = quality; i < 5; i++) {
            sb.append("✰");
        }
        return sb.toString();
    }
}
