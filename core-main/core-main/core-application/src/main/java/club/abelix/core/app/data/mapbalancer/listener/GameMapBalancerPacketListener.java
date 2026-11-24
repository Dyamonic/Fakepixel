package club.abelix.core.app.data.mapbalancer.listener;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.mapbalancer.GameMapBalancerManager;
import club.abelix.core.app.data.mapbalancer.GameMapMiniBalancer;
import club.abelix.core.app.event.core.ClientActiveEvent;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.client.CMapBalancerRecovery;
import club.abelix.network.packet.play.client.CMapBalancerRegister;
import club.abelix.network.packet.play.client.CMapBalancerUpdateMap;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SMapBalancerUpdateResponse;
import club.abelix.network.type.map.GameMap;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>GameMapBalancerPacketListener creation on 25.12.2023 at 23:46</p>
 */

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameMapBalancerPacketListener implements PlayClientProcessor, Listener {

    GameMapBalancerManager balancerManager = GameMapBalancerManager.IMP;

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onActive(@NonNull ClientActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    @Override
    public void process(CMapBalancerRegister packet, ChannelProcessorContext ctx) {
        GameMapMiniBalancer balancer = balancerManager.getMini(packet.getMini());
        for (GameMap gameMap : packet.getMaps()) {
            balancer.addMap(gameMap);
        }
    }

    @Override
    public void process(CMapBalancerRecovery packet, ChannelProcessorContext ctx) {
//        GameMapMiniBalancer balancer = balancerManager.getMini(packet.getMini());
//        balancer.startMap(packet.getMap());
    }

    @Override
    public void process(CMapBalancerUpdateMap packet, ChannelProcessorContext ctx) {
        GameMapMiniBalancer balancer = balancerManager.getMini(packet.getMini());

        if (!packet.hasRequestId()) {
            return;
        }

        GameMap newMap = balancer.getBestMap();
        ctx.setResponse(new SMapBalancerUpdateResponse(newMap));
    }
}
