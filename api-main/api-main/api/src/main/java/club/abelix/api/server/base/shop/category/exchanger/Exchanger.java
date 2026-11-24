package club.abelix.api.server.base.shop.category.exchanger;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.val;

/**
 * @author - suuft (aka: Valeriy Shushpanov) ( vk.com/suuft )
 * Created at 28/01/2023 21:18
 */
@UtilityClass
public class Exchanger {

    @Setter
    private static int goldsToCoins = 1000; // 1 (gold) = 10 (coins)

    // not cool, but works (quote by @krashe85: "we write business code")
    public void convertGoldToCoins(@NonNull Gamer gamer, int golds) {
        val result = getResult(golds);
        gamer.takeGold(golds);

        if (CoreUtil.isMaster() || CoreUtil.isLobby()) {
            gamer.addCoins(CoreUtil.getMaster(), result, false, "Обмен золота");
        } else {
            gamer.addCoins(CoreUtil.getUncommon(), result, false, "Обмен золота");
        }

        gamer.sendLocalizedMessage("golds_to_coins_convert_successful", "%golds%", golds, "%coins%", result);
        FalconAPI.getNmsApi().getSoundApi().playSound(gamer.getPlayer(), YSound.VILLAGER_YES);
    }

    public int getResult(int golds) {
        return golds * goldsToCoins;
    }
}
