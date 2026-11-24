package club.abelix.network.type.mode;

import club.abelix.network.common.locale.UserLocale;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum Master implements ServerMode {

    BW("bw", "BedWars", Lobby.BWLOBBY),
    DUELS("duels", "Duels", Lobby.DUELSLOBBY),
    ARCADE("arcade", "Arcade", Lobby.ARCADELOBBY),
    LW("lw", "LuckyWars", Lobby.LWLOBBY),
    MM("mm", "MurderMystery", Lobby.MMLOBBY),
    EXCLUSIVE("exclusive", "Exclusive Games", Lobby.HUB),
    SW("sw", "SkyWars", Lobby.SWLOBBY, true);

    public static final Master[] VALUES = values();

    private final String serverName;
    private final String displayName;
    private final Lobby lobby;

    private final boolean useQueues;

    private Set<Mini> minis;
    private int miniBitmask;

    @Setter
    @Getter
    private Mini primaryRankedMini;

    @Contract(pure = true)
    Master(@NotNull String serverName, @NotNull String displayName, @NotNull Lobby lobby) {
        this(serverName, displayName, lobby, false);
    }

    @Contract(pure = true)
    Master(@NotNull String serverName, @NotNull String displayName, @NotNull Lobby lobby, boolean useQueues) {
        this.serverName = serverName;
        this.displayName = displayName;
        this.lobby = lobby;
        this.useQueues = useQueues;
    }

    public static @Nullable Master getByName(@NonNull String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    public static @Nullable Master getByPrefix(@NonNull String prefix) {
        for (Master master : VALUES) {
            if (master.getServerName().equals(prefix)) {
                return master;
            }
        }

        return null;
    }

    @Contract(pure = true)
    public String getServerName() {
        return serverName;
    }

    @Contract(pure = true)
    @Override
    public boolean isMaster() {
        return true;
    }

    @Contract(pure = true)
    public boolean isUsingQueues() {
        return useQueues;
    }

    @Contract(pure = true)
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Contract(pure = true)
    public Lobby getLobby() {
        return lobby;
    }

    public void addMini(Mini mini) {
        if (minis != null) {
            minis.add(mini);
        }

        miniBitmask += mini.getMask();
    }

    public boolean hasMini(Mini mini) {
        try {
            return mini.getMaster().getName().equals(this.getName());
        } catch (Exception any) {
            return false;
        }
//        return (miniBitmask & mini.getMask()) != 0;
    }

    public Set<Mini> getMinis() {
        if (minis == null) {
            minis = EnumSet.noneOf(Mini.class);

            for (Mini mini : Mini.VALUES) {
                if (hasMini(mini)) {
                    minis.add(mini);
                }
            }
        }

        return minis;
    }

    @Override
    public List<String> getDescription(@NonNull UserLocale locale) {
        return locale.getMessageList("master_" + name() + "_description");
    }

    @Override
    public int getTypeId() {
        return 0;
    }

    @Override
    public String getName() {
        return name();
    }
}
