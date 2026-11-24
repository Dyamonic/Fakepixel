package club.abelix.api.base.gamersession;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerAsyncLoadEvent;
import club.abelix.api.base.gamer.event.GamerUnloadEvent;
import club.abelix.api.base.listener.YListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: api</p>
 * <p>GamerSessionListener creation on 17.04.2023 at 01:47</p>
 */

public class GamerSessionListener extends YListener {

    public GamerSessionListener(AbstractPlugin plugin) {
        super(plugin);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGamerLoad(GamerAsyncLoadEvent loadEvent) {
        Gamer gamer = loadEvent.getGamer();

        List<GamerSessionProvider<? extends GamerSession>> providerList = GamerSessionManager.INSTANCE.getAllProviders();
        for (GamerSessionProvider<? extends GamerSession> provider : providerList) {
            if (!provider.load(gamer)) {
                loadEvent.setCancelled(true);
                loadEvent.setCancelReason("Failed load gamer session provider! Try later!");
                return;
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGamerUnload(GamerUnloadEvent unloadEvent) {
        Gamer gamer = unloadEvent.getGamer();

        List<GamerSessionProvider<? extends GamerSession>> providerList = GamerSessionManager.INSTANCE.getAllProviders();
        for (GamerSessionProvider<? extends GamerSession> provider : providerList) {
            provider.unload(gamer);
        }
    }
}
