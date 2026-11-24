package club.abelix.core.telegram.bans.meta;

import club.abelix.commons.data.punishment.meta.PunishmentMeta;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.DurationField;
import club.abelix.commons.util.time.DurationFormatter;
import lombok.Getter;
import lombok.NonNull;

import java.util.concurrent.TimeUnit;

@Getter
public final class TelegramBanMeta extends PunishmentMeta {

    private final long telegramId;

    public TelegramBanMeta(int id, long telegramId, String reason, long issueDate, long expirationDate) {
        super(id, null, null, reason, issueDate, expirationDate);

        this.telegramId = telegramId;
    }

    @Override
    public String getExpirationString(@NonNull Language language) {
        if (isPermanent()) {
            return language.getMessage("eternity");
        }

        Duration.FormatParams params = new Duration.FormatParams();

        long leftTime = getExpirationDate() - System.currentTimeMillis();

        params.sourceUnit = TimeUnit.MILLISECONDS;

        params.max = DurationField.DAY;
        params.min = leftTime > TimeUnit.HOURS.toMillis(1) ?
                DurationField.MIN :
                DurationField.SEC;

        params.locale = language == Language.RUSSIAN ?
                DurationFormatter.RU_ACCUSATIVE :
                DurationFormatter.EN;

        return Duration.format(getExpirationDate() - System.currentTimeMillis(), params);
    }

}
