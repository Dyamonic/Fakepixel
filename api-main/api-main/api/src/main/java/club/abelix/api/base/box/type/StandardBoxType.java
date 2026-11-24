package club.abelix.api.base.box.type;

import club.abelix.commons.language.Language;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public enum StandardBoxType implements BoxType {

    RANKS(1),
    GAME_COSMETIC(2),
    LOBBY_COSMETIC(3),
    GOLD(4),
    EXPERIENCE(5),
    TITLES(6),
    SKYWARS_PERKS(7),
    SKYWARS_KITS(8),
    SKYWARS_COSMETIC(9);
    ;

    int id;

    @Override
    public @NotNull String getName(@NotNull Language language) {
        return language.getMessage("box_type_" + this.name().toLowerCase());
    }

}
