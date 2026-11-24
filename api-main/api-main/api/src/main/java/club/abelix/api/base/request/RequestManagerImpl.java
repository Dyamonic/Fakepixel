package club.abelix.api.base.request;

import club.abelix.api.base.gamer.event.GamerUnloadEvent;
import club.abelix.commons.util.request.RequestManager;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * @author WhileInside
 */
public class RequestManagerImpl extends RequestManager implements Listener {

    public RequestManagerImpl(final @NonNull Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void injectManager(final @NonNull Plugin plugin) {
        setImplementation(new RequestManagerImpl(plugin));
    }

    @EventHandler
    public void onQuit(final GamerUnloadEvent event) {
        handleQuit(event.getGamer().getPlayerId());
    }
}
