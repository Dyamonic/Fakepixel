package club.abelix.commons.util.exclusive;

import club.abelix.network.type.mode.Mini;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ExclusiveGames creation on 11.10.2023 at 15:50</p>
 */

@UtilityClass
public class ExclusiveGames {

    private final Map<ExclusivePeriod, Mini> MODES = new HashMap<>() {
//        {
//            put(ExclusivePeriod.HALLOWEEN, Mini.HALLOWEEN_SIMULATOR);
//        }
    };

    public static @Nullable Mini get(ExclusivePeriod exclusivePeriod) {
        return MODES.get(exclusivePeriod);
    }

    public static @Nullable Mini getCurrentGame() {
        ExclusivePeriod currentPeriod = ExclusivePeriod.getCurrentExclusivePeriod();

        return MODES.get(currentPeriod);
    }

    public static List<Mini> getCurrentGames() {
        List<ExclusivePeriod> currentPeriods = ExclusivePeriod.getCurrentExclusivePeriods();

        List<Mini> result = new ArrayList<>();

        currentPeriods.forEach(exclusivePeriod -> {
            Mini mode = MODES.get(exclusivePeriod);

            if (mode != null) {
                result.add(mode);
            }
        });

        return result;
    }
}
