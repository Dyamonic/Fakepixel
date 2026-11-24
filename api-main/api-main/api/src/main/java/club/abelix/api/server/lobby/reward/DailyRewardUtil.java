package club.abelix.api.server.lobby.reward;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.commons.data.dailyrewards.DailyRewardSqlHandler;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.time.TimeUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Location;

import java.util.HashMap;

/**
 * @author fleyer (Fleyer001)
 * @since 25.06.2024 at 10:54</p>
 * For AG-Api project.
 */

@Log4j2
@UtilityClass
public class DailyRewardUtil {

    HashMap<Gamer, Hologram> hologramHashMap = new HashMap<>();

    void test(Gamer gamer, Location location){

    }
}
