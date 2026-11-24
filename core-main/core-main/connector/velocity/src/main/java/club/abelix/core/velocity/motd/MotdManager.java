package club.abelix.core.velocity.motd;

import club.abelix.commons.language.Language;
import club.abelix.commons.version.GameVersion;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.config.Configuration;
import club.abelix.core.velocity.util.Constants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class MotdManager {

    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    static final long MOTD_ROTATION_INTERVAL_MILLIS = 5_000L;
    static final GameVersion MODERN_MIN_PROTOCOL = GameVersion.MINECRAFT_1_16_4;

    @Getter
    StarshipVelocity starship;

    Configuration motdConfiguration;

    RotatingMotdPool modernPool = new RotatingMotdPool(MOTD_ROTATION_INTERVAL_MILLIS);
    RotatingMotdPool legacyPool = new RotatingMotdPool(MOTD_ROTATION_INTERVAL_MILLIS);

    public void initialize() {
        val pingListener = new PingListener(this, starship.getStatusQueryExecutionService());
        starship.getProxy().getEventManager().register(starship, pingListener);

        reload();
    }

    private List<String> loadAllIterableKeys(@NotNull String baseKey, @NotNull Language language) {
        List<String> result = new ArrayList<>();

        for (int index = 0; ; index++) {
            String key = baseKey + "_" + index;
            if (!language.isMessageTranslated(key)) break;
            result.add(key);
        }

        return result;
    }

    private List<String> loadMaxKeys(@NotNull String baseKey) {
        List<String> maxResult = new ArrayList<>();

        for (Language language : Language.values()) {
            val keys = loadAllIterableKeys(baseKey, language);
            if (keys.size() > maxResult.size()) {
                maxResult = keys;
            }
        }
        return maxResult;
    }

    public void reload() {
        this.legacyPool.getMotdKeys().clear();
        this.modernPool.getMotdKeys().clear();

        this.legacyPool.getMotdKeys().addAll(loadMaxKeys("motd_legacy"));
        this.modernPool.getMotdKeys().addAll(loadMaxKeys("motd_modern"));

        log.info("Legacy pool motd count: {}", this.legacyPool.getMotdKeys().size());
        log.info("Modern pool motd count: {}", this.modernPool.getMotdKeys().size());
    }

    public @NotNull Component getNextMotd(@NotNull Language language, int protocolVersion) {
        val pool = resolvePool(protocolVersion);

        if (pool.isEmpty()) {
            return Component.empty();
        }

        val raw = language.getMessage(
                pool.getCurrentMotd(),
                "%region%", Constants.REGION,
                "%min_version%", GameVersion.MINIMUM_VERSION.getName(),
                "%max_version%", GameVersion.MAXIMUM_VERSION.getMostRecentSupportedVersionName()
        );

        return protocolVersion >= MODERN_MIN_PROTOCOL.getVersion()
                ? MINI_MESSAGE.deserialize(raw)
                : Component.text(raw);
    }

    public @NotNull Component buildMotd(@NotNull Language language, @NotNull String localizationKey) {
        return buildComponent(language, localizationKey);
    }

    private void loadSection(String sectionName, List<String> targetList) {
        val section = motdConfiguration.getSection(sectionName);

        for (val key : section.getKeys()) {
            val localizationKey = section.getString(key);

            if (localizationKey == null || localizationKey.isBlank()) {
                continue;
            }

            targetList.add(localizationKey);
        }
    }

    private RotatingMotdPool resolvePool(int protocolVersion) {
        return protocolVersion >= MODERN_MIN_PROTOCOL.getVersion()
                ? modernPool
                : legacyPool;
    }

    private Component buildComponent(Language language, String localizationKey) {
        return MINI_MESSAGE.deserialize(
                language.getMessage(
                        localizationKey,
                        "%region%", Constants.REGION,
                        "%min_version%", GameVersion.MINIMUM_VERSION.getName(),
                        "%max_version%", GameVersion.MAXIMUM_VERSION.getMostRecentSupportedVersionName()
                )
        );
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static final class RotatingMotdPool {

        @Getter
        final List<String> motdKeys = new ArrayList<>();

        final long rotationIntervalMillis;

        int currentIndex;
        long nextRotationAtMillis;

        private boolean isEmpty() {
            return motdKeys.isEmpty();
        }

        private String getCurrentMotd() {
            return motdKeys.get(getCurrentIndex());
        }

        private int getCurrentIndex() {
            long now = System.currentTimeMillis();

            synchronized (this) {
                if (nextRotationAtMillis == 0L) {
                    nextRotationAtMillis = now + rotationIntervalMillis;
                    return 0;
                }

                if (now >= nextRotationAtMillis) {
                    currentIndex = Math.floorMod(currentIndex + 1, motdKeys.size());
                    nextRotationAtMillis = now + rotationIntervalMillis;
                }

                return currentIndex;
            }
        }
    }
}