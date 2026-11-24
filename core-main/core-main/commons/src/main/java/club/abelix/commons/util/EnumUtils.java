package club.abelix.commons.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class EnumUtils {

    public <E extends Enum<E>> boolean isInEnum(
            final @NotNull Class<E> enumClass,
            final @NotNull String value
    ) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

}
