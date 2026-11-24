package club.abelix.api.base.util.worldtime;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.World;

@UtilityClass
public class WorldSetTime {
    public static void setAndFreezeTime(@NonNull World world, int setTime) {
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doWeatherCycle", "false");
        world.setWeatherDuration(0);
        world.setFullTime(setTime);

    }

    public static void setAndFreezeTime(@NonNull World world, @NonNull WorldTimeState timeState) {
        WorldSetTime.setAndFreezeTime(world, timeState.getTime());

    }
}

