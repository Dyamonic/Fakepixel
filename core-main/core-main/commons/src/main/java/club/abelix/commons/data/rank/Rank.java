package club.abelix.commons.data.rank;

import club.abelix.commons.data.rank.permission.GroupPermissions;
import club.abelix.commons.language.Language;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EqualsAndHashCode(of = "id")
@RequiredArgsConstructor
public final class Rank {

    int id;
    int priority;

    String name;
    String displayNameTranslationKey;

    String prefix;
    String suffix;

    GroupPermissions permissions;

    public @NotNull String getDisplayName(@NotNull Language locale) {
        return locale.getMessage(displayNameTranslationKey);
    }

    public boolean hasPermission(@NotNull String permission) {
        return permissions.hasPermission(permission);
    }
}
