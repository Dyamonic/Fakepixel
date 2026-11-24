package club.abelix.core.bukkit.connection;

import club.abelix.commons.language.Language;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.core.bukkit.event.ConnectionEstablishedEvent;
import club.abelix.core.bukkit.event.CorePingRequestEvent;
import club.abelix.core.bukkit.event.ServerActiveEvent;
import club.abelix.core.bukkit.event.ServerInactiveEvent;
import club.abelix.network.channel.AbstractRemoteServerChannel;
import club.abelix.network.common.enums.ConnectionType;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.PacketRegistry;
import club.abelix.network.packet.handshake.Result;
import club.abelix.network.packet.handshake.packet.HRequest;
import club.abelix.network.packet.handshake.packet.HResponse;
import club.abelix.network.packet.play.client.CServerPingResponse;
import club.abelix.network.packet.play.client.CSetJoinableState;
import club.abelix.network.packet.play.server.SReloadLanguages;
import club.abelix.network.packet.play.server.SServerPingRequest;
import io.netty.channel.socket.SocketChannel;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import w.util.Env;

@Slf4j
public class RemoteServerConnection extends AbstractRemoteServerChannel {

    private final StarshipBukkit core;
    private final String serverName;

    public RemoteServerConnection(StarshipBukkit core, String serverName,
                                  LocalClientConnection local, SocketChannel channel) {
        super(local, channel);

        this.core = core;
        this.serverName = serverName;
    }

    @Override
    public void onServerDisconnect() {
        if (getProtocol() != PacketRegistry.PLAY) {
            Logging.IMP.warn("[HANDSHAKE] Core connection closed before handshake completed.");
        }

        core.getServer().getPluginManager().callEvent(new ServerInactiveEvent());
    }

    @Override
    public void process(@NonNull Throwable throwable) {
        Logging.IMP.stacktrace(throwable, "[HANDSHAKE] Core connection failed while processing network traffic");
    }

    @Override
    public void active() throws Exception {
        try {
            core.getServer().getPluginManager().callEvent(new ServerActiveEvent(this));
        } catch (Throwable t) {
            Logging.IMP.stacktrace(t, "Can't call active event");
        }

        Logging.IMP.info("[HANDSHAKE] Sending handshake packet...");

        sendPacket(
                new HRequest(
                        ConnectionType.SERVER,
                        Env.getString("CORE_TOKEN", "milanskyy_gay"),
                        serverName,
                        25565,
                        core.getServerType().getMode()
                )
        );
    }

    @Override
    public void process(SReloadLanguages packet, ChannelProcessorContext ctx) {
        if (core.getServerType().isSpecial()) {
            return;
        }

        Language.reloadLanguages();
    }

    @Override
    public void process(HResponse packet, ChannelProcessorContext ctx) {
        if (packet.getResult() == Result.FAILED) {
            Logging.IMP.error("[HANDSHAKE] Unable to connect to core: {}", packet.getMessage());
            return;
        }

        Logging.IMP.info("[HANDSHAKE] The connection has been successfully established!");

        upgradeConnection(PacketRegistry.PLAY);
        sendPacket(new CSetJoinableState(true));
        Connection.setConnection(core.getConnection());
        Connection.setSlots(Bukkit.getServer().getMaxPlayers());

        core.getServer().getPluginManager().callEvent(
                new ConnectionEstablishedEvent(this,
                        !this.core.getConnection().isFirstConnection())
        );

        this.core.getConnection().setFirstConnection(false);
    }

    @Override
    public void process(SServerPingRequest packet, ChannelProcessorContext ctx) {
        ctx.setResponse(new CServerPingResponse(packet.getRandom()));

        HeartBeat.callEvent(new CorePingRequestEvent());
    }
}
