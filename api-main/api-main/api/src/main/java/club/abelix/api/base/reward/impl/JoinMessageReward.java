package club.abelix.api.base.reward.impl;


import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.data.joinmessage.JoinMessage;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class JoinMessageReward implements Reward {

    protected final JoinMessage joinMessage;

    @Override
    public boolean give(@NonNull Gamer gamer) {
        if (gamer.getPurchasedJoinMessages().contains(joinMessage)) {
            return false;
        }

        gamer.addPurchasedJoinMessage(joinMessage);

        return true;
    }

    @Override
    public String getRewardName(@NonNull Language language) {
        return language.getMessage(
                "reward_join_message_name",
                "%join_message%", joinMessage.getName(language)
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.PAPER);
    }

}
