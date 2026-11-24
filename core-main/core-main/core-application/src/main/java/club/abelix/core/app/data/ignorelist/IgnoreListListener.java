package club.abelix.core.app.data.ignorelist;

import club.abelix.core.app.api.event.EventPriority;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.GamerLoadEvent;
import org.greenrobot.eventbus.Subscribe;

public class IgnoreListListener implements Listener {

    @Subscribe(priority = EventPriority.HIGHEST)
    public void onPlayerLoad(GamerLoadEvent event) {
        event.getGamer().loadIgnoreList();
    }

}
