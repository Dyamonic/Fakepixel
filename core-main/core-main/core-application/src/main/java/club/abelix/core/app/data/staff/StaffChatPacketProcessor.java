package club.abelix.core.app.data.staff;

import club.abelix.commons.language.LanguageContext;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.event.core.ClientActiveEvent;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.client.CBroadcastStaff;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import lombok.NonNull;
import org.greenrobot.eventbus.Subscribe;

/**
 * Слушает смешной пакет на броадкаст в стаффчат
 * {@link CBroadcastStaff}
 */
public class StaffChatPacketProcessor implements PlayClientProcessor, Listener {

    @Subscribe
    public void onActive(@NonNull ClientActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    @Override
    public void process(@NonNull CBroadcastStaff packet, @NonNull ChannelProcessorContext ctx) {
        // TODO: Refract
        ServerManager.IMP.broadcastStaff("command.staffchat",
                new LanguageContext(language -> packet.getMessage()));
    }
}
