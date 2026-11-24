package club.abelix.core.velocity.connection;

import club.abelix.commons.language.Language;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketRegistry;
import club.abelix.network.packet.play.both.BUpdatePlayerLanguage;
import club.abelix.network.packet.play.both.BUserRankRefreshPacket;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import java.util.concurrent.CompletableFuture;

@UtilityClass
public class Connection {

    @Setter
    private LocalClientConnection connection;

    public boolean isConnected() {
        return connection != null && connection.isConnected() && connection.getChannel().getProtocol() == PacketRegistry.PLAY;
    }

    private LocalClientConnection getConnection() {
        if (connection == null || !connection.isConnected()) {
            throw new IllegalStateException("Connection wasn't established");
        }

        return connection;
    }

    public void updateLanguage(int playerId, Language newLanguage) {
        sendPacket(new BUpdatePlayerLanguage(playerId, newLanguage.getId()));
    }

    /**
     * Обновить ранг игрока на коре
     */
    public void updateRank(int playerId) {
        sendPacket(new BUserRankRefreshPacket(playerId));
    }

    public void sendPacket(Packet<?> packet) {
        getConnection().sendPacket(packet);
    }

    public static <A extends Packet.Response<?>, B extends Packet.Request<?, A>> CompletableFuture<A> awaitPacket(@NonNull B packet) {
        return connection.awaitPacket(packet);
    }

    public static <A extends Packet.Response<?>, B extends Packet.Request<?, A>> CompletableFuture<A> awaitPacket(@NonNull B packet, long timeout) {
        return connection.awaitPacket(packet, timeout);
    }


}
