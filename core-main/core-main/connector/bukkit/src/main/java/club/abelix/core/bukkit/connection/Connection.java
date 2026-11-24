package club.abelix.core.bukkit.connection;

import club.abelix.commons.language.Language;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.play.both.BUpdatePlayerLanguage;
import club.abelix.network.packet.play.both.BUserRankRefreshPacket;
import club.abelix.network.packet.play.client.CArenaCreate;
import club.abelix.network.packet.play.client.CArenaSetGameMap;
import club.abelix.network.packet.play.client.CArenaSetState;
import club.abelix.network.packet.play.client.CGetReports;
import club.abelix.network.packet.play.client.CReportAction;
import club.abelix.network.packet.play.client.CReportCheckStateRequest;
import club.abelix.network.packet.play.client.CSetSlots;
import club.abelix.network.packet.play.server.SRemovePrefix;
import club.abelix.network.packet.play.server.SReportCheckStateResponse;
import club.abelix.network.packet.play.server.SReports;
import club.abelix.network.packet.play.server.SUpdatePrefix;
import club.abelix.network.type.map.GameMap;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.report.PlayerReport;
import io.netty.channel.ChannelFuture;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Slf4j
@UtilityClass
public class Connection {

    @Setter
    private LocalClientConnection connection;

    public void registerProcessor(PacketProcessor processor) {
        connection.getChannel().registerProcessor(processor);
    }

    private LocalClientConnection getConnection() {
        if (!isActive()) {
            throw new IllegalStateException("Connection wasn't established");
        }

        return connection;
    }

    public boolean isActive() {
        return connection != null && connection.isConnected();
    }

    /**
     * Обновить язык игрока на коре
     */
    public void updateLanguage(int playerId, Language newLanguage) {
        sendPacket(new BUpdatePlayerLanguage(playerId, newLanguage.getId()));
    }

    /**
     * Обновить префикс игрока на коре
     */
    public void updatePrefix(int playerId, String text, LocalDateTime expire) {
        sendPacket(new SUpdatePrefix(playerId, text, expire));
    }

    public void removePrefix(int playerId) {
        sendPacket(new SRemovePrefix(playerId));
    }

    public CompletableFuture<Boolean> checkReportSendState(int playerId, int targetId) {
        return connection.awaitPacket(new CReportCheckStateRequest(playerId, targetId))
                .thenApply(SReportCheckStateResponse::isAllowed);
    }

    /**
     * Создать арену
     * <br>
     * <p>
     * Этот метод сообщает кору о том, что создана арена
     * </p>
     */
    public void createArena(Mini mini, String name, GameMap map, int slots) {
        sendPacket(new CArenaCreate(mini, name, map, slots));
    }

    /**
     * Установить слоты
     */
    public void setSlots(int slots) {
        sendPacket(new CSetSlots(slots));
    }

    public void startReportSpectating(int playerId, int targetId) {
        sendPacket(new CReportAction(CReportAction.Action.START_SPECTATING, playerId, targetId, null));
    }

    public CompletableFuture<Collection<PlayerReport>> getReports() {
        return connection.awaitPacket(new CGetReports()).thenApply(SReports::getReports);
    }

    public void setArenaState(String name, GameState state) {
        val packet = new CArenaSetState(name, state);
        sendPacketFuture(packet).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("Failed send CArenaSetState({}) packet. Details:", packet, future.cause());
            }
        });
    }

    public void setArenaGameMap(String name, GameMap gameMap) {
        sendPacket(new CArenaSetGameMap(name, gameMap));
    }

    /**
     * Обновить группу игрока на коре
     */
    public void updateRank(int playerId) {
        sendPacket(new BUserRankRefreshPacket(playerId));
    }

    public void sendPacket(Packet<?> packet) {
        getConnection().sendPacket(packet);
    }

    public ChannelFuture sendPacketFuture(Packet<?> packet) {
        return getConnection().sendPacketFuture(packet);
    }

    public static <A extends Packet.Response<?>, B extends Packet.Request<?, A>> CompletableFuture<A> awaitPacket(@NonNull B packet) {
        return connection.awaitPacket(packet);
    }

    public static <A extends Packet.Response<?>, B extends Packet.Request<?, A>> CompletableFuture<A> awaitPacket(@NonNull B packet, long timeout) {
        return connection.awaitPacket(packet, timeout);
    }
}
