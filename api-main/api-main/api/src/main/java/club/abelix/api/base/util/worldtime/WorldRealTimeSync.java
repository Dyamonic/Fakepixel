package club.abelix.api.base.util.worldtime;

import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.bukkit.World;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class WorldRealTimeSync {

    private static final Map<String, SynchronizedWorld> SYNCHRONIZED_WORLDS = new HashMap<>();

    static {
        HeartBeat.runTimerSync(() -> SYNCHRONIZED_WORLDS.values().forEach(SynchronizedWorld::updateTime), 0, 20);
    }

    public void sync(@NonNull World world, @NonNull ZoneId zone) {
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doWeatherCycle", "false");
        world.setWeatherDuration(0);

        SYNCHRONIZED_WORLDS.put(world.getName(), new SynchronizedWorld(world, zone));
    }

    public void stop(@NonNull World world) {
        stop(world.getName());
    }

    public void stop(@NonNull String worldName) {
        SYNCHRONIZED_WORLDS.remove(worldName);
    }

    public void updateTime(@NonNull World world, @NonNull ZoneId zone) {
        LocalTime lTime = LocalTime.now(zone);

        long time = lTime.toSecondOfDay();

        long converted = (long) ((double) time / TimeUnit.DAYS.toSeconds(1) * 24_000) + 18_000 - 1_000;

        world.setFullTime(converted % 24_000);
    }

    @RequiredArgsConstructor
    public static class SynchronizedWorld {

        private final World world;
        private final ZoneId zone;

        void updateTime() {
            WorldRealTimeSync.updateTime(world, zone);
        }

    }

}
