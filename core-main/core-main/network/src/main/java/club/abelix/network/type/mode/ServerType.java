package club.abelix.network.type.mode;

import club.abelix.network.util.ModeUtil;
import club.abelix.network.util.ParseUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ServerType {

    private static final char SERVER_PARAM_SEPARATOR = '-';

    /**
     * Полное имя сервера
     */
    @NotNull
    private final String serverName;

    /**
     * Префикс сервера, первое значение перед знаком '-'
     */
    @NotNull
    private final String prefix;

    /**
     * Номер сервера, если есть gameNumber не ноль, то это третье значение, иначе идёт сразу же после префикса
     */
    private final int serverNumber;

    /**
     * Номер арены, может отсутствовать, но обычно идёт после префикса
     */
    private final int gameNumber;

    /**
     * Режим сервера, может быть кастомным, так что результат поиска режима по
     * префиксу может отличаться от этого режима
     */
    @NotNull
    private final ServerMode mode;

    public ServerType(@NotNull String serverName, @NotNull String prefix, int serverNumber, int gameNumber,
                      @NotNull ServerMode mode) {
        this.serverName = serverName;
        this.prefix = prefix;
        this.serverNumber = serverNumber;
        this.gameNumber = gameNumber;
        this.mode = mode;
    }

    public ServerType(@NotNull String serverName, @Nullable ServerMode mode) {
        this.serverName = serverName;

        int separator = serverName.indexOf(SERVER_PARAM_SEPARATOR);

        // Номер сервера и арены отсутствует
        if (separator == -1) {
            this.prefix = serverName;

            this.serverNumber = 0;
            this.gameNumber = 0;

            this.mode = mode == null
                    ? ModeUtil.getServerMode(serverName)
                    : mode;
            return;
        }

        this.prefix = serverName.substring(0, separator);

        this.mode = mode == null
                ? ModeUtil.getServerMode(prefix)
                : mode;

        char afterSeparator = serverName.charAt(separator + 1);

        if (afterSeparator >= 'A' && afterSeparator <= 'Z') {
            serverNumber = afterSeparator - 'A' + 1;
            gameNumber = ParseUtils.parseInt(serverName, separator + 2, serverName.length());
        } else {
            serverNumber = ParseUtils.parseInt(serverName, separator + 1, serverName.length());
            gameNumber = 0;
        }
    }

    public ServerType(String serverName) {
        this(serverName, null);
    }

    public Uncommon getUncommon() {
        return isUncommon() ? (Uncommon) mode : null;
    }

    public Master getMaster() {
        return isMaster() ? (Master) mode : null;
    }

    public Mini getMini() {
        return isMini() ? (Mini) mode : null;
    }

    public Lobby getLobby() {
        return isLobby() ? (Lobby) mode : null;
    }

    public boolean isLobby() {
        return mode instanceof Lobby;
    }

    public boolean isUncommon() {
        return mode instanceof Uncommon;
    }

    public boolean isMini() {
        return mode instanceof Mini;
    }

    public boolean isMaster() {
        return mode instanceof Master;
    }

    public boolean isSpecial() {
        return mode instanceof Special;
    }

    @NotNull
    public String getServerName() {
        return serverName;
    }


    /**
     * Имя сервера с которым будет зарегистрирован сервер в коре
     */
    @NotNull
    public String getRegistrationServerName() {
        return serverNumber == 0
                ? mode.getServerName()
                : mode.getServerName() + "-" + serverNumber;
    }

    @NotNull
    public String getPrefix() {
        return prefix;
    }

    public int getServerNumber() {
        return serverNumber;
    }

    public int getGameNumber() {
        return gameNumber;
    }

    @NotNull
    public ServerMode getMode() {
        return mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerType)) return false;

        ServerType that = (ServerType) o;

        if (serverNumber != that.serverNumber) return false;
        if (gameNumber != that.gameNumber) return false;
        if (!serverName.equals(that.serverName)) return false;
        if (!prefix.equals(that.prefix)) return false;

        return mode.equals(that.mode);
    }

    @Override
    public int hashCode() {
        int result = serverName.hashCode();
        result = 31 * result + prefix.hashCode();
        result = 31 * result + serverNumber;
        result = 31 * result + gameNumber;
        result = 31 * result + mode.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServerType{" +
                "serverName='" + serverName + '\'' +
                ", prefix='" + prefix + '\'' +
                ", serverNumber=" + serverNumber +
                ", gameNumber=" + gameNumber +
                ", mode=" + mode +
                '}';
    }
}
