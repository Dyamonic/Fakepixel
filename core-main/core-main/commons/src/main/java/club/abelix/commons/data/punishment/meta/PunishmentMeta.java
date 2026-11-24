package club.abelix.commons.data.punishment.meta;

import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.DurationField;
import club.abelix.commons.util.time.DurationFormatter;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * Общая метадата для всех наказаний
 */
@AllArgsConstructor
public abstract class PunishmentMeta {

    public static final long PERMANENT_TIME = -1;

    /**
     * Уникальный ID наказания
     */
    private final int id;

    /**
     * Ник администратора, выдавшего наказание
     * Ник администратора, снявшего наказание
     */
    private final String adminName, removedPunishmentName;

    /**
     * Причина выдачи наказания
     */
    private final String reason;

    /**
     * Дата выдачи наказания
     */
    private final long issueDate;

    /**
     * Время истечения наказания
     */
    private final long expirationDate;

    public String getReason(Language language) {
        String[] words = reason.split(" ");

        for (int i = 0; i < words.length; i++) {
            String key = "rule_" + words[i];

            if (language.isMessageTranslated(key)) {
                words[i] = language.getMessage(key);
            }
        }

        return String.join(" ", words);
    }

    public String getUntranslatedReason() {
        return reason;
    }

    public int getId() {
        return id;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getRemovedPunishmentName() {
        return removedPunishmentName;
    }

    public long getExpirationDate() {
        return expirationDate;
    }

    public boolean isExpired() {
        return !isPermanent() && expirationDate < System.currentTimeMillis();
    }

    public boolean isPermanent() {
        return expirationDate == PERMANENT_TIME;
    }

    public String getExpirationString(@NonNull Language language) {
        if (isPermanent()) {
            return language.getMessage("eternity");
        }

        Duration.FormatParams params = new Duration.FormatParams();

        long leftTime = expirationDate - System.currentTimeMillis();

        params.sourceUnit = TimeUnit.MILLISECONDS;

        params.max = DurationField.DAY;
        params.min = leftTime > TimeUnit.HOURS.toMillis(1) ?
                DurationField.MIN :
                DurationField.SEC;

        params.locale = language == Language.RUSSIAN ?
                DurationFormatter.RU_SHORT :
                DurationFormatter.EN_SHORT;

        return Duration.format(expirationDate - System.currentTimeMillis(), params);
    }

    public long getIssueDate() {
        return issueDate;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        if (this == object) {
            return true;
        }

        PunishmentMeta meta = (PunishmentMeta) object;

        return id == meta.getId();
    }

    @Override
    public int hashCode() {
        return id;
    }
}
