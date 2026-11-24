package club.abelix.network.type.mode;

import club.abelix.network.common.locale.UserLocale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class Unknown implements ServerMode {

    private final String name;

    @Override
    public int getTypeId() {
        return -1;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getServerName() {
        return name;
    }

    @Override
    public List<String> getDescription(@NonNull UserLocale locale) {
        return List.of("N/A");
    }

    @Override
    public boolean isUnknown() {
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
}
