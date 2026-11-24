package club.abelix.commons.util.time;

import club.abelix.commons.language.Language;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@UtilityClass
public final class PlayerCooldownUtil {

    private final Cache<Integer, List<CooldownRecord>> cooldownMap = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    public CooldownRecord addCooldown(final int gamerId,
                            final @NonNull String cooldownName,
                            final long value,
                            final @NonNull TimeUnit unit) {
        var oldList = cooldownMap.getIfPresent(gamerId);

        if (oldList == null) {
            oldList = new ArrayList<>();
        }

        val record = new CooldownRecord(cooldownName.toLowerCase(), System.currentTimeMillis() + unit.toMillis(value));

        oldList.add(record);
        cooldownMap.put(gamerId, oldList);

        return record;
    }

    /**
     * Удаление задержки
     */
    public void removeCooldown(final @NonNull String cooldownName) {
        cooldownMap.cleanUp();

        cooldownMap.asMap().forEach((key, value) -> {
            value.removeIf(record -> record.key().equals(cooldownName));
        });
    }

    /**
     * Удаление задержки у конкретного игрока
     */
    public void removeCooldown(final int gamerId, final @NonNull String cooldownName) {
        cooldownMap.cleanUp();

        val cooldowns = cooldownMap.getIfPresent(gamerId);

        if (cooldowns == null) {
            return;
        }

        cooldowns.removeIf(record -> record.key().equals(cooldownName));
    }

    public CooldownRecord getCooldown(final int gamer, final @NonNull String cooldownName) {
        cooldownMap.cleanUp();

        val cooldowns = cooldownMap.getIfPresent(gamer);

        if (cooldowns == null) {
            return null;
        }

        return cooldowns.stream().filter(record -> record.key().equalsIgnoreCase(cooldownName))
                .filter(record -> !record.isExpired())
                .findFirst()
                .orElse(null);
    }

    /**
     * Возвращает boolean, говорящий о том, действует ли
     * еще эта задержка.
     */
    public boolean hasCooldown(final int gamer, final @NonNull String cooldownName) {
        cooldownMap.cleanUp();

        val cooldowns = cooldownMap.getIfPresent(gamer);

        if (cooldowns == null) {
            return false;
        }

        val cooldown = cooldowns.stream().filter(record -> record.key().equalsIgnoreCase(cooldownName))
                .filter(record -> !record.isExpired())
                .findFirst();

        if (cooldown.isEmpty()) {
            return false;
        }

        return System.currentTimeMillis() <= cooldown.map(CooldownRecord::value).orElse(0L);
    }

    public record CooldownRecord(String key, long value) {
        public long remaining() {
            return value - System.currentTimeMillis();
        }

        public String remainingFormatted() {
            return remainingFormatted(Language.defaultLanguage());
        }

        public String remainingFormatted(final @NonNull Language language) {
            return Duration.durationWordsMs(language, remaining() + 1000);
        }

        public boolean isExpired() {
            return remaining() <= 0;
        }
    }

}
