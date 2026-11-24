package club.abelix.commons.data.companion;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum CompanionType {
    PANDA(false),
    MP8(false),
    SNOW_MAN(false),
    PENGUIN(false),
    BOXER(false),
    BULBASAUR(false),
    CHIMP(true),
    DIGLET(false),
    GIRAFFE(true),
    GORILLA(true),
    GRUNKLE_STAN(false),
    KOALA(false),
    LION(false),
    MAGIKARP(false),
    PERRY(false),
    POLAR_BEAR(true),
    POLAR_BEAR_CUBE(false),
    PUG(false),
    SUPERMAN(false),
    TIGER(false),
    TURTLE(false),
    ;

    boolean exclusive;

    public static CompanionType getByName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

}
