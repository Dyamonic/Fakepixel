package club.abelix.commons.data.rank.permission;

import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class FullGroupPermissions extends ManagedGroupPermissions {

    Set<GroupPermission> permissions;

    public FullGroupPermissions(final @NotNull Set<GroupPermission> permissions) {
        super(
                Sets.union(
                        permissions,
                        Set.of(Permissions.fullPermission())
                )
        );

        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "FULL";
    }

    @Override
    public @Nullable GroupPermission getPermission(final @NotNull String name) {
        return super.getPermission(name);
    }

    @Override
    public boolean hasPermission(final @NotNull String name) {
        return true;
    }

    @Override
    public @NotNull Set<@NotNull GroupPermission> asSet() {
        return permissions;
    }
}
