package club.abelix.commons.data.trust;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustFactor {

    private int playerId;

    private TrustLevel level;

    /**
     * Trust score (0-100+)
     * Вычисляется на основе различных факторов
     */
    private int score;

    /**
     * Общее время игры в секундах (из Sessions)
     */
    private long totalPlaytimeSeconds;

    /**
     * Есть ли у игрока донат (ранг/префикс/суффикс)
     */
    private int donationEffort;

    private int mutesCount;

    private int bansCount;

    private LocalDateTime lastRecalculated;

    private LocalDateTime createdAt;

    /** Принудительно выставленный уровень, null = вычисляется автоматически */
    @Nullable
    private TrustLevel forcedLevel;

    public boolean isForcedLevel() {
        return forcedLevel != null;
    }

    public boolean requiresModeration() {
        return level.isRequiresModeration();
    }

    public double getPlaytimeHours() {
        return totalPlaytimeSeconds / 3600.0;
    }

    public long getPlaytimeMinutes() {
        return totalPlaytimeSeconds / 60;
    }

    public boolean isHasDonation() {
        return donationEffort > 0;
    }
    public boolean isStaff() {
        return donationEffort == -1;
    }
}
