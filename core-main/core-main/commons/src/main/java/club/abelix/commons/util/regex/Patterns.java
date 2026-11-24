package club.abelix.commons.util.regex;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Patterns {

    public final Pattern DIGIT_IP = Pattern.compile("(\\d{1,3})(\\.\\d{1,3}){3}");

    public final Pattern WIDE_DIGIT_IP = Pattern.compile("(?iu)([\\dзoо]{1,3})(([^\\dа-я]{1,5}|[а-я ]{1,7})\\d{1,3}){3}");

    public final Pattern VALID_PREFIX_TEXT = Pattern.compile("^(?iu)\\[[a-zа-яё_\\-]{3,10}]\\s$");

    public final Pattern RESTRICTED_PRIVILEGES_NAMES = Pattern.compile("^(?iu)\\[(owner|админ|admin|модер|moder|helper|хелпер|staff|стафф)]\\s$");

    public final Pattern INT_NUMBER_PATTERN = Pattern.compile("^(0|[1-9][0-9]*)$");

    public final Pattern URL = Pattern.compile("(?i)http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*(),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+");

    public final Pattern TIME = Pattern.compile(
            "(?i)" +
                    "(\\d{1,3}(?=ns))?" +
                    "(\\d{1,3}(?=mc))?" +
                    "(\\d{1,3}(?=ms))?" +
                    "(\\d{1,3}(?=s))?" +
                    "(\\d{1,3}(?=m))?" +
                    "(\\d{1,3}(?=h))?" +
                    "(\\d{1,3}(?=d))?" +
                    "(\\d{1,3}(?=w))?"
    );

    private static final Pattern VALID_NICK = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    public final Pattern INTEGER = Pattern.compile("^(-)?\\d+$");

    public final Pattern MOJANG_NICK = Pattern.compile("(?i)[0-9a-z_]{3,16}");

    public final Pattern ENGLISH_LETTERS = Pattern.compile("^[a-zA-Z]+$");

    public boolean isValidInteger(@NonNull String value) {
        return INT_NUMBER_PATTERN.matcher(value).matches();
    }

    public long parseTime(@NonNull String time, @NonNull TimeUnit unitTo) throws IllegalArgumentException {
        Matcher matcher = TIME.matcher(time);

        TObjectIntMap<TimeUnit> values = new TObjectIntHashMap<>();

        while (matcher.find()) {
            for (int i = 1; i <= 7; i++) {
                String value = matcher.group(i);

                if (value == null || value.isEmpty()) {
                    continue;
                }

                TimeUnit unit = TimeUnit.values()[i - 1];
                int intValue = Integer.parseInt(value);

                values.adjustOrPutValue(unit, intValue, intValue);
                break;
            }
        }

        if (values.isEmpty()) {
            throw new IllegalArgumentException("Illegal Date");
        }

        final AtomicLong total = new AtomicLong();

        values.forEachEntry((timeUnit, value) -> {
            total.addAndGet(unitTo.convert(value, timeUnit));

            return true;
        });

        if (total.get() <= 0) {
            throw new IllegalArgumentException("Illegal Date");
        }

        return total.get();
    }

    public boolean isValidNick(@NonNull String name) {
        return VALID_NICK.matcher(name).matches();
    }

    public boolean isMojangNick(@NonNull String name) {
        return MOJANG_NICK.matcher(name).matches();
    }

    public boolean isValidUrl(@NonNull String url) {
        return URL.matcher(url).matches();
    }

    public boolean isInteger(@NonNull String s) {
        return INTEGER.matcher(s).matches();
    }

    public boolean isIPV4(@NonNull String ip) {
        return DIGIT_IP.matcher(ip).matches();
    }

    public boolean isEnglishLetters(@NonNull String input) {
        return ENGLISH_LETTERS.matcher(input).matches();
    }

    public static boolean validateNickName(String name) {
        if (name == null) {
            return false;
        }

        int len = name.length();
        if (len < 3 || len > 16) {
            return false;
        }

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);

            boolean ok =
                    (ch >= 'A' && ch <= 'Z') ||
                            (ch >= 'a' && ch <= 'z') ||
                            (ch >= '0' && ch <= '9') ||
                            ch == '_';

            if (!ok) {
                return false;
            }
        }

        return true;
    }
}
