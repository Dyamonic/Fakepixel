package club.abelix.api.base.joinmessage.custom;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;

public class JoinMessageCreateListener extends YListener {

    private final Set<Gamer> pendingCreation = new HashSet<>();

    public JoinMessageCreateListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());

        if (!pendingCreation.contains(gamer)) {
            return;
        }


    }

}
