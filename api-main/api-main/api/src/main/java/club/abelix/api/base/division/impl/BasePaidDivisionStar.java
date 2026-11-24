package club.abelix.api.base.division.impl;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.division.DivisionRequirements;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.network.type.mode.Mini;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>BasePaidDivisionStar creation on 19.03.2024 at 18:13</p>
 */

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class BasePaidDivisionStar extends BaseStatDivisionStar {

    private static final String PROPERTY = "Division/Paid/%s/%d";

    int starPrice;

    private String getProperty() {
        return PROPERTY.formatted(CoreUtil.getMaster().getName(), getIdentifier());
    }

    public BasePaidDivisionStar(int identifier, String star, String localizationKey, Mini mini, List<DivisionRequirements> requirementsList, int price) {
        super(identifier, star, localizationKey, mini, requirementsList);

        this.starPrice = price;
    }

    @Override
    public int getPrice() {
        return this.starPrice;
    }

    @Override
    public boolean isPurchased(@NonNull Gamer gamer) {
        return gamer.getProperty(getProperty())  == 1;
    }

    @Override
    public void processPurchase(@NonNull Gamer gamer) {
        val price = getPrice();
        val propertyValue = getProperty();
        val soundApi = FalconAPI.getNmsApi().getSoundApi();

        if (isPurchased(gamer)) {
            gamer.sendLocalizedMessage("division_star_already_have");
            return;
        }

        if (gamer.getGold() < price) {
            gamer.sendLocalizedMessage("purchase_unavailable_cost_message");
            soundApi.playSound(gamer.getPlayer(), YSound.ANVIL_BREAK);
            return;
        }

        gamer.takeGold(price);
        gamer.setProperty(propertyValue, 1);
        soundApi.playSound(gamer.getPlayer(), YSound.LEVEL_UP);
    }
}
