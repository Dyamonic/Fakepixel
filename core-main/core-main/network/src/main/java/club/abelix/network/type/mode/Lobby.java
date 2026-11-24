package club.abelix.network.type.mode;

import club.abelix.network.common.locale.UserLocale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public enum Lobby implements ServerMode {

    HUB("hub"),
    BWLOBBY("bwlobby"),
    DUELSLOBBY("duelslobby"),
    ARCADELOBBY("arcadelobby"),
    LWLOBBY("lwlobby"),
    MMLOBBY("mmlobby"),
    SWLOBBY("swlobby");

    public static final Lobby[] VALUES = values();

    @Getter
    private final String displayName;

    private Master master;

    public static Lobby getByName(@NonNull String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Master getMaster() {
        // У хаба нет мастера
        if (master == null && this != HUB) {
            for (Master master : Master.values()) {
                if (master.getLobby() == this) {
                    this.master = master;

                    break;
                }
            }
        }

        return master;
    }

    @Override
    public int getTypeId() {
        return 1;
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
    public boolean isGameLobby() {
        return master != null;
    }

    @Override
    public boolean isLobby() {
        return true;
    }

    @Override
    public List<String> getDescription(@NonNull UserLocale locale) {
        return locale.getMessageList("lobby_" + name() + "_description");
    }
}
