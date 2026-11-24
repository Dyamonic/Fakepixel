package club.abelix.commons.data.rank.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

public interface GroupPermissions {

    @Nullable GroupPermission getPermission(@NotNull String name);

    boolean hasPermission(@NotNull String name);

    @Unmodifiable
    @NotNull Set<@NotNull GroupPermission> asSet();
}
