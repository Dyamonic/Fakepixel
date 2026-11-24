package club.abelix.commons.util.time;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;


@UtilityClass
public class CooldownUtil {

    private final TObjectLongMap<String> cooldownMap = new TObjectLongHashMap<>();

    /**
     * Создание и добавление задержки
     */
    public void addCooldown(@NonNull String cooldownName, long duration, @NonNull TimeUnit timeUnit) {
        cooldownMap.put(cooldownName.toLowerCase(),
                System.currentTimeMillis() + timeUnit.toMillis(duration));
    }

    /**
     * Создание и добавление задержки
     */
    public void addCooldown(@NonNull String cooldownName, long durationMils) {
        addCooldown(cooldownName, durationMils, TimeUnit.MILLISECONDS);
    }

    /**
     * Удаление задержки
     */
    public void removeCooldown(@NonNull String cooldownName) {
        cooldownMap.remove(cooldownName.toLowerCase());
    }

    /**
     * Возвращает boolean, говорящий о том, действует ли
     * еще эта задержка.
     */
    public boolean hasCooldown(@NonNull String cooldownName) {
        long cooldown = cooldownMap.get(cooldownName.toLowerCase());

        return System.currentTimeMillis() <= cooldown;
    }

}
