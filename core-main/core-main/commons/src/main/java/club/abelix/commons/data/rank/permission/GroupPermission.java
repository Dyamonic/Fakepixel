package club.abelix.commons.data.rank.permission;

import org.jetbrains.annotations.NotNull;

public interface GroupPermission {

    @NotNull String getName();

    boolean isWildcard();

    boolean matches(@NotNull String name);

    boolean isSet();

}
