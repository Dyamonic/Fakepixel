package club.abelix.commons.data.rank.permission;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ManagedGroupPermissions implements GroupPermissions {

    Set<GroupPermission> permissions;

    public static @NotNull GroupPermissions create(final @NotNull Set<@NotNull GroupPermission> permissions) {
        return Permissions.hasFullPermissions(permissions)
                ? new FullGroupPermissions(permissions)
                : new ManagedGroupPermissions(permissions);
    }

    @Override
    public String toString() {
        return permissions.toString();
    }

    @Override
    public @Nullable GroupPermission getPermission(final @NotNull String name) {
        for (val permission : permissions) {
            if (permission.matches(name)) {
                return permission;
            }
        }

        return null;
    }

    @Override
    public boolean hasPermission(final @NotNull String name) {
        for (val permission : permissions) {
            if (permission.matches(name)) {
                return permission.isSet();
            }
        }

        return false;
    }

    @Override
    public @NotNull Set<@NotNull GroupPermission> asSet() {
        return permissions;
    }
}
