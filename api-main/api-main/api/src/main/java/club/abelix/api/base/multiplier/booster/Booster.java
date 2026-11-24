package club.abelix.api.base.multiplier.booster;

import club.abelix.api.base.multiplier.Multiplier;
import club.abelix.commons.data.booster.BoosterType;
import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@ToString(callSuper = true)
public class Booster extends Multiplier {

    @Getter
    private final BoosterType boosterType;

    @Getter
    private final LocalDateTime expire;

    public Booster(@NonNull BoosterType boosterType) {
        this(boosterType, LocalDateTime.now().plusMinutes(boosterType.getDuration()));
    }

    public Booster(@NonNull BoosterType boosterType, LocalDateTime expire) {
        super(boosterType.getMultiplierType(), boosterType.getMultiplier());

        this.boosterType = boosterType;
        this.expire = expire;
    }

    public boolean isExpired() {
        return expire != null && expire.isBefore(LocalDateTime.now());
    }

    @Override
    public String getDisplayName(@NotNull Language language) {
        return boosterType.getDisplayName(language);
    }
}
