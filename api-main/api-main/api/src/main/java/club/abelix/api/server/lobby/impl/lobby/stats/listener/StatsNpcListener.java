package club.abelix.api.server.lobby.impl.lobby.stats.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.api.base.gamer.event.GamerUnloadEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.lobby.impl.lobby.stats.manager.StatsNpcManager;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.event.EventHandler;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StatsNpcListener extends YListener {


    StatsNpcManager manager;

    public StatsNpcListener(AbstractPlugin plugin, StatsNpcManager manager) {
        super(plugin);
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(GamerJoinEvent event) {
        manager.createNpc(event.getGamer());
    }

    @EventHandler
    public void onPlayerDisconnect(GamerUnloadEvent event) {
        manager.deleteNpc(event.getGamer());
    }
}