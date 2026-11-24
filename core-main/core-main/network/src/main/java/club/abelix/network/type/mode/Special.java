package club.abelix.network.type.mode;

import club.abelix.network.common.locale.UserLocale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum Special implements ServerMode {

    LOGIN("login"),
    LIMBO("limbo"),
    BUILD("build"),
    PROXY("velocity")
    ;

    public static final Special[] VALUES = values();

    private final String displayName;

    public static Optional<Special> findByServerName(
            final @NotNull String serverName
    ) {
        return Arrays.stream(VALUES)
                .filter(s -> s.getDisplayName().equalsIgnoreCase(serverName))
                .findFirst();
    }

    @Override
    public int getTypeId() {
        return 4;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getServerName() {
        return getDisplayName();
    }

    @Override
    public List<String> getDescription(@NonNull UserLocale locale) {
        return locale.getMessageList("special_" + name() + "_description");
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

}
