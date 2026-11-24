package club.abelix.core.velocity.metrics.listener;
import club.abelix.core.velocity.metrics.VelocityMetricsStorage;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerNettyDisconnectMetricListener {

    VelocityMetricsStorage storage;

    @Subscribe(priority = Short.MIN_VALUE)
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        try {
            Object connection = getFieldValue(player, "connection"); // или "mcConnection"
            if (connection == null) return;

            Channel channel = (Channel) getFieldValue(connection, "channel");
            if (channel == null) return;

            channel.pipeline().addFirst("disconnect-metrics-handler", new DisconnectMetricsHandler(player.getUsername()));

            log.info("Disconnect metric success inject {}", player.getUsername());
        } catch (Exception ex) {
            log.error("Disconnect metric failed inject {}. Reason: {}", player.getUsername(), ex);
        }
    }

    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    @ChannelHandler.Sharable
    private class DisconnectMetricsHandler extends ChannelInboundHandlerAdapter {

        private final String playerName;

        public DisconnectMetricsHandler(String playerName) {
            this.playerName = playerName;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            String causeName = cause.getClass().getSimpleName().toLowerCase();

            if (causeName.contains("connectionreset") || causeName.contains("nativeioexception") || causeName.contains("resetbypeer")) {
                storage.getResetByPeerDisconnect().incrementAndGet();
            } else if (causeName.contains("timeout") || causeName.contains("readtimeout") || causeName.contains("writetimeout")) {
                storage.getTimeoutDisconnect().incrementAndGet();
            } else if (causeName.contains("closedchannel") || causeName.contains("premature") || causeName.contains("eof")) {
                storage.getClosedChannelsDisconnect().incrementAndGet();
            } else {
                storage.getOtherNettyErrorsDisconnect().incrementAndGet();
            }

            log.warn("Netty exception for {}: {} - {}", playerName, cause.getClass().getSimpleName(), cause.getMessage());

            ctx.fireExceptionCaught(cause);
        }
    }
}
