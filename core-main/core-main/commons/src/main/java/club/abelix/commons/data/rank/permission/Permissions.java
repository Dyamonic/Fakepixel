package club.abelix.commons.data.rank.permission;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@UtilityClass
public class Permissions {

    public @NotNull GroupPermission from(final @NonNull String text) {
        if (text.equals("*")) {
            return FullPermission.INSTANCE;
        }

        final boolean set;
        final String name;

        if (text.charAt(0) == '-') {
            set = false;
            name = text.substring(1);
        } else {
            set = true;
            name = text;
        }

        val params = name.split("\\.");

        boolean isLastWildcard = false;
        int wildcards = 0;

        for (val param : params) {
            if (param.equals("*")) {
                isLastWildcard = true;
                wildcards++;
            } else {
                isLastWildcard = false;
            }
        }

        if (isLastWildcard && wildcards == 1) {
            return new FastWildcardPermission(name, set, name.substring(0, name.length() - 2));
        } else if (wildcards > 0) {
            return new SlowWildcardPermission(name, set, params);
        } else {
            return new SimplePermission(name, set);
        }
    }

    public boolean hasFullPermissions(final @NotNull Set<@NotNull GroupPermission> permissions) {
        return permissions.contains(FullPermission.INSTANCE);
    }

    public @NotNull GroupPermission fullPermission() {
        return FullPermission.INSTANCE;
    }

    @EqualsAndHashCode
    @FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class AbstractPermission implements GroupPermission {
        @Getter
        String name;

        @Getter
        boolean set;

        @Override
        public String toString() {
            return name + " = " + set;
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class FullPermission extends AbstractPermission {

        private static final GroupPermission INSTANCE = new FullPermission();

        private FullPermission() {
            super("*", true);
        }

        @Override
        public boolean isWildcard() {
            return true;
        }

        @Override
        public boolean matches(final @NonNull String name) {
            return true;
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class SlowWildcardPermission extends AbstractPermission {

        String[] wildcard;

        private SlowWildcardPermission(final String name, final boolean set,
                                       final String[] wildcard) {
            super(name, set);

            this.wildcard = wildcard;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean isWildcard() {
            return true;
        }

        @Override
        public boolean matches(final @NonNull String name) {
            val path = name.split("\\.");

            if (wildcard.length != path.length)
                return false;

            for (int i = 0; i < wildcard.length; i++) {
                val value = wildcard[i];

                if (!value.equals("*") && !path[i].equals(wildcard[i])) {
                    return false;
                }
            }

            return true;
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class FastWildcardPermission extends AbstractPermission {

        String wildcard;

        private FastWildcardPermission(final String name, final boolean set,
                                       final String wildcard) {
            super(name, set);

            this.wildcard = wildcard;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean isWildcard() {
            return true;
        }

        @Override
        public boolean matches(final @NonNull String name) {
            return name.startsWith(wildcard);
        }

    }

    private static final class SimplePermission extends AbstractPermission {

        private SimplePermission(final String name, final boolean set) {
            super(name, set);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean isWildcard() {
            return false;
        }

        @Override
        public boolean matches(final @NonNull String name) {
            return this.name.equals(name);
        }

    }

}
