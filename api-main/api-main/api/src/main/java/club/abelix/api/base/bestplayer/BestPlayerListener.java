package club.abelix.api.base.bestplayer;

import club.abelix.api.base.listener.YListener;
import club.abelix.core.bukkit.event.ServerActiveEvent;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.both.BBestPlayerDonate;
import club.abelix.network.packet.play.both.BBestPlayerUpdate;
import club.abelix.network.packet.play.processor.PlayProcessor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.event.EventHandler;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class BestPlayerListener extends YListener implements PlayProcessor {

    BestPlayerManager manager;

    @EventHandler
    public void onActive(ServerActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    public void process(BBestPlayerUpdate packet, ChannelProcessorContext ctx) {
        Logging.IMP.info("[BestPlayer] " + packet.toString());
        manager.load();
    }

    public void process(BBestPlayerDonate packet, ChannelProcessorContext ctx) {
        Logging.IMP.info("[BestPlayer] " + packet.toString());
        manager.getCurrentBestPlayer().reloadDonations();
    }

}
