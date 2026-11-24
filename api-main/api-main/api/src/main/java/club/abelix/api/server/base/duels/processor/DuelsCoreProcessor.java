package club.abelix.api.server.base.duels.processor;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.base.duels.gui.DuelsGuiFactory;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.event.ServerActiveEvent;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.packet.play.server.SDuelOpenGui;
import org.bukkit.event.EventHandler;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>DuelsCoreListener creation on 17.01.2024 at 13:53</p>
 */

public class DuelsCoreProcessor extends YListener implements PlayServerProcessor {

    public DuelsCoreProcessor(AbstractPlugin plugin) {
        super(plugin);
    }

    @Override
    public void process(SDuelOpenGui packet, ChannelProcessorContext ctx) {
        Gamer sender = Gamer.getGamer(packet.getSenderPlayerId());
        Gamer receiver = Gamer.getOrCreate(packet.getReceiverPlayerId(),
                offline -> {
                    offline.loadRanks();
                    offline.loadPrefixAndUpdateName();
                });

        if (sender == null || receiver == null) {
            Logging.IMP.warn("[DuelGui] Failed start gui because sender or receiver null!");
            return;
        }


//        if (!StarshipBukkit.getInstance().getServerType().isLobby()) {
//            sender.sendLocalizedMessage("duel_gui_only_lobby");
//            return;
//        }

        HeartBeat.runSync(() ->
                DiBukkitPlatform.withBean(
                        DuelsGuiFactory.class,
                        factory -> factory.openDuelsRequestGui(sender, receiver)
                )
        );
    }

    @EventHandler
    public void onActive(ServerActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }
}
