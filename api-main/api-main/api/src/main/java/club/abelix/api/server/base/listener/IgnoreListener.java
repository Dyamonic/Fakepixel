package club.abelix.api.server.base.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.core.bukkit.event.ServerActiveEvent;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.packet.play.server.SIgnoreListUpdate;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

public class IgnoreListener extends YListener implements PlayServerProcessor {

    public IgnoreListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @Override
    public void process(SIgnoreListUpdate packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getProcessorGamerId());

        if (gamer == null) {
            return;
        }

        int ignoredId = packet.getIgnoredGamerId();

        switch (packet.getAction()) {
            case ADD -> gamer.addIgnoredPlayer(ignoredId);
            case REMOVE -> gamer.removeIgnoredPlayer(ignoredId);
        }
    }

    @EventHandler
    public void onActive(ServerActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

}
