package club.abelix.commons.data.setting.media;

import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@RequiredArgsConstructor
public enum SocialMediaType {

    /**
     * Ссылка на профиль игрока в ВКонтакте
     */
    VK(Pattern.compile("(?i)(https?://)?(www\\.)?vk\\.com/([a-z0-9]+)"), 32),

    /**
     * Тэг игрока в Discord
     */
    DISCORD(Pattern.compile("(?i)^(.*)#(\\d{4})$"), 32),

    /**
     * Логин игрока в Telegram
     */
    TELEGRAM(Pattern.compile("(?i)[a-z0-9_]{5,}"), 32),

    /**
     * Канал игрока на YouTube
     */
    YOUTUBE(Pattern.compile("(?i)(https?://)?(www\\.)?youtube\\.com/(channel|user)/([a-zA-Z0-9_-]+)"), 120),

    /**
     * Ссылка на профиль игрока на Twitch
     */
    TWITCH(Pattern.compile("(?i)(https?://)?(www\\.)?twitch\\.tv/([a-zA-Z0-9_-]+)"), 32),

    /**
     * Ссылка на профиль игрока в TikTok
     */
    TIKTOK(Pattern.compile("(?i)(https?://)?(www\\.)?tiktok\\.com/@([a-zA-Z0-9_-]+)"), 32),

    /**
     * Ссылка на профиль игрока в Instagram
     */
    INSTAGRAM(Pattern.compile("(?i)(https?://)?(www\\.)?instagram\\.com/([a-zA-Z0-9._]+)"), 32),

    /**
     * Ссылка на профиль игрока в Twitter
     */
    TWITTER(Pattern.compile("(?i)(https?://)?(www\\.)?twitter\\.com/([a-zA-Z0-9_-]+)"), 32),

    /**
     * Ссылка на профиль игрока на сайте fakepixel.fun
     */
    FAKEPIXEL_FORUM(Pattern.compile("(?i)(https?://)?(www\\.)?fakepixel\\.fun/profile/([a-zA-Z0-9_-]+)"), 32);;


    private static final SocialMediaType[] VALUES = values();
    private final Pattern regex;
    private final int maxLength;

    public static SocialMediaType getById(final int id) {
        if (id < 0 || id >= VALUES.length) {
            return null;
        }

        return VALUES[id];
    }

    /**
     * Проверить введенное сообщение на соответствие регулярке итд
     * данной социальной сети
     *
     * @param message - сообщение для проверки
     * @return - результат парсинга
     */
    public String parse(@NonNull String message, @NonNull Matcher matcher) {
        return message;
    }

    public final String parse(@NonNull String message) {
        if (message.length() > maxLength) return null;

        Matcher matcher = getRegex().matcher(message);
        return matcher.matches() ? parse(message, matcher) : null;
    }

    public String getName(@NonNull Language language) {
        return language.getMessage("social_" + name() + "_name");
    }

    public String getUsage(@NonNull Language language) {
        return language.getMessage("social_" + name() + "_usage");
    }
}
