package club.abelix.api.base.multiplier;

import club.abelix.commons.data.booster.MultiplierType;
import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
public abstract class Multiplier {

    @Getter
    private final MultiplierType type;

    @Getter
    protected final double value;

    protected Multiplier(@NonNull MultiplierType type, double value) {
        this.type = type;
        this.value = value;
    }

    public abstract String getDisplayName(@NonNull Language language);
}
