package club.abelix.core.telegram.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;

@UtilityClass
public final class PasswordErrorUtil {

    private final int MAX_ERRORS = 3;

    private final Cache<Long, Integer> PASSWORD_ERRORS = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public boolean addError(long telegramId) {
        int errors = getPasswordErrors(telegramId) + 1;

        PASSWORD_ERRORS.put(telegramId, errors);

        return errors >= MAX_ERRORS;
    }

    private int getPasswordErrors(long telegramId) {
        PASSWORD_ERRORS.cleanUp();

        return PASSWORD_ERRORS.get(telegramId, key -> 0);
    }

    public void resetErrors(long telegramId) {
        PASSWORD_ERRORS.invalidate(telegramId);
    }


}
