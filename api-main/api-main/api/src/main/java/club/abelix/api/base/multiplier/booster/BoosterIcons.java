package club.abelix.api.base.multiplier.booster;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.booster.BoosterType;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

@UtilityClass
public class BoosterIcons {

    public ItemStack getIcon(@NonNull BoosterType boosterType) {
        val playerApi = FalconAPI.getNmsApi().getPlayerApi();

        PotionType potionType = switch (boosterType) {
            case BOOSTER_COINS_1 -> PotionType.INVISIBILITY;
            case BOOSTER_COINS_2 -> PotionType.FIRE_RESISTANCE;
            case BOOSTER_EXPERIENCE_1 -> PotionType.NIGHT_VISION;
            case BOOSTER_EXPERIENCE_2 -> PotionType.JUMP;
            case BOOSTER_EXPERIENCE_3 -> PotionType.WATER_BREATHING;
            default -> PotionType.SPEED;
        };

        ItemStack itemStack = playerApi.getPotion(potionType, false, false, false, 1);

        return ItemUtil.getBuilder(itemStack)
                .addItemFlags()
                .build();
    }

}
