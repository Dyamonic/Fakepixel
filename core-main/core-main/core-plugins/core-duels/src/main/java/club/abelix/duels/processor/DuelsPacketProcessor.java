package club.abelix.duels.processor;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.core.ClientActiveEvent;
import club.abelix.duels.DuelsCorePlugin;
import club.abelix.duels.mode.DuelMode;
import club.abelix.duels.request.DuelRequestManager;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.client.CDuelRequest;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import org.greenrobot.eventbus.Subscribe;

public class DuelsPacketProcessor implements PlayClientProcessor, Listener {

    @Subscribe
    public void onActive(ClientActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    @Override
    public void process(CDuelRequest packet, ChannelProcessorContext ctx) {
        Gamer sender = Gamer.getGamer(packet.getSenderId());
        Gamer target = Gamer.getGamer(packet.getTargetId());
        DuelMode duelMode = null;

        for (DuelMode itterDuelMode : DuelsCorePlugin.AVAILABLE_DUEL_MODES.values()) {
            if (itterDuelMode.mini() == packet.getDuelMode()) {
                duelMode = itterDuelMode;
                break;
            }
        }

        if (duelMode == null) {
            sender.sendLocalizedMessage("duel_command_invalid_mode");
            return;
        }

        DuelRequestManager.send(sender, target, duelMode);
    }
}
