package club.abelix.api.base.util.core;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.party.Party;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.play.client.CConnectExact;
import club.abelix.network.packet.play.client.CConnectMap;
import club.abelix.network.packet.play.client.CConnectMode;
import club.abelix.network.packet.play.client.CConnectToPlayer;
import club.abelix.network.packet.play.server.SConnectResponse;
import club.abelix.network.type.connect.ConnectStatus;
import club.abelix.network.type.mode.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.concurrent.CompletableFuture;

@UtilityClass
public class CoreUtil {

    /**
     * Тип сервера
     */
    @Getter
    private ServerType serverType;
    @Getter
    private String serverName;
    @Getter
    private String serverPrefix;

    public void initializeServerData() {
        serverType = StarshipBukkit.getInstance().getServerType();
        serverName = serverType.getServerName();
        serverPrefix = serverType.getPrefix();
    }

    public int getServerNumber() {
        return serverType.getServerNumber();
    }

    public ServerMode getMode() {
        return serverType.getMode();
    }

    public Uncommon getUncommon() {
        return serverType.getUncommon();
    }

    public Master getMaster() {
        ServerMode mode = getMode();

        if (mode instanceof Master) {
            return (Master) mode;
        }

        if (mode instanceof Lobby) {
            return ((Lobby) mode).getMaster();
        }

        return null;
    }

    public Mini getMini() {
        return serverType.getMini();
    }

    public Lobby getLobby() {
        return serverType.getLobby();
    }

    public boolean isHub() {
        return isLobby() && getLobby() == Lobby.HUB;
    }

    public boolean isLobby() {
        return serverType.isLobby();
    }

    public boolean isUncommon() {
        return serverType.isUncommon();
    }

    public boolean isMini() {
        return serverType.isMini();
    }

    public boolean isMaster() {
        return serverType.isMaster();
    }

    private CompletableFuture<ConnectStatus> sendConnectionPacket(@NonNull Packet.Request<?, SConnectResponse> packet,
                                                                  boolean awaitResponse) {
        if (!Connection.isActive()) {
            return CompletableFuture.completedFuture(ConnectStatus.UNKNOWN);
        }

        if (awaitResponse) {
            return Connection.awaitPacket(packet).thenApply(SConnectResponse::getStatus);
        } else {
            Connection.awaitPacket(packet);

            return CompletableFuture.completedFuture(ConnectStatus.SUCCESS);
        }
    }

    /**
     * Подключить игрока к рандомному серверу, начинающемуся на server
     *
     * @param gamer Игрок
     * @param modes Режимы
     */
    public void connectMode(@NonNull Gamer gamer, @NonNull ServerMode... modes) {
        connectMode(gamer, false, modes);
    }

    /**
     * Подключить игрока к серверу
     *
     * @param gamer Игрок
     * @param modes Режимы
     */
    public CompletableFuture<ConnectStatus> connectMode(@NonNull Gamer gamer, boolean awaitResponse,
                                                        @NonNull ServerMode... modes) {

        for (ServerMode mode : modes) {
            if (mode instanceof Mini || mode instanceof Master) {
                Party party = gamer.getParty();

                if (party != null && !party.isLeader(gamer)) {
                    gamer.sendLocalizedMessage("party_join_game_leader_only");
                    return CompletableFuture.completedFuture(ConnectStatus.UNKNOWN);
                }
            }
        }

        return sendConnectionPacket(new CConnectMode(gamer.getPlayerId(), modes), awaitResponse);
    }

    /**
     * Подключить игрока к серверу
     *
     * @param gamer  Игрок
     * @param server Сервер
     */
    public void connectExact(@NonNull Gamer gamer, @NonNull String server) {
        connectExact(gamer, server, false);
    }

    /**
     * Подключить игрока к серверу
     *
     * @param gamer  Игрок
     * @param server Сервер
     */
    public CompletableFuture<ConnectStatus> connectExact(@NonNull Gamer gamer, @NonNull String server,
                                                         boolean awaitResponse) {
        return sendConnectionPacket(new CConnectExact(gamer.getPlayerId(), server), awaitResponse);
    }

    /**
     * Подключить игрока на  определённую карту определённого подрежима
     *
     * @param gamer Игрок
     * @param mini  Подрежим
     * @param map   Карта
     */
    public void connectMap(@NonNull Gamer gamer, @NonNull Mini mini, @NonNull String map) {
        connectMap(gamer, mini, map, false);
    }

    /**
     * Подключить игрока на  определённую карту определённого подрежима
     *
     * @param gamer Игрок
     * @param mini  Подрежим
     * @param map   Карта
     */
    public CompletableFuture<ConnectStatus> connectMap(@NonNull Gamer gamer, @NonNull Mini mini, @NonNull String map,
                                                       boolean awaitResponse) {
        Party party = gamer.getParty();

        if (party != null && !party.isLeader(gamer)) {
            gamer.sendLocalizedMessage("party_join_game_leader_only");
            return CompletableFuture.completedFuture(ConnectStatus.UNKNOWN);
        }

        return sendConnectionPacket(new CConnectMap(gamer.getPlayerId(), mini, map), awaitResponse);
    }

    /**
     * Подключить игрока к серверу другого игрока
     *
     * @param gamer       Игрок
     * @param targetGamer ID другого игрока
     */
    public void connectToPlayer(@NonNull Gamer gamer, int targetGamer) {
        connectToPlayer(gamer, targetGamer, false);
    }

    /**
     * Подключить игрока к серверу другого игрока
     *
     * @param gamer       Игрок
     * @param targetGamer ID другого игрока
     */
    public CompletableFuture<ConnectStatus> connectToPlayer(@NonNull Gamer gamer, int targetGamer,
                                                            boolean awaitResponse) {
        return sendConnectionPacket(new CConnectToPlayer(gamer.getPlayerId(), targetGamer), awaitResponse);
    }

    /**
     * Сгенерировать имя для режима
     *
     * @param serverMode Режим
     */
    public String generateDisplayName(@NonNull ServerMode serverMode) {
        StringBuilder sb = new StringBuilder();

        char prefix = resolvePrefixChar(serverMode);
        sb.append(prefix);

        int number = getServerNumber();
        sb.append(number);

        String suffix = resolveSuffix(serverMode, number);
        sb.append(suffix);

        return sb.toString();
    }

    private char resolvePrefixChar(@NonNull ServerMode mode) {
        if (mode.isLobby() || mode.isGameLobby()) return 'L';
        if (mode.isMaster()) return 'M';
        if (mode.isMini()) return 'm';
        if (mode.isUncommon()) return 'U';

        return mode.getServerName().charAt(0);
    }

    private String resolveSuffix(@NonNull ServerMode mode, int number) {
        if (mode.isLobby()) {
            return "ML";
        }

        Master master = getMaster();
        char letter = (char) ('A' + number - 1);

        if (master == Master.EXCLUSIVE) {
            return "EL" + letter;
        }

        if (master != null) {
            return master.getServerName().toUpperCase() + letter;
        }

        return getMode().getServerName().toUpperCase();
    }
}
