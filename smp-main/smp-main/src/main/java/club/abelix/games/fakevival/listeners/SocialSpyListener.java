package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.SocialSpyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;

public final class SocialSpyListener implements Listener {
    private final SocialSpyManager socialSpyManager;

    public SocialSpyListener(SocialSpyManager socialSpyManager) {
        this.socialSpyManager = socialSpyManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        socialSpyManager.spy("&8[Spy Chat] &f" + event.getPlayer().getName() + "&7: &f" + event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        socialSpyManager.spy("&8[Spy Command] &f" + event.getPlayer().getName() + "&7: &f" + event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        socialSpyManager.spy("&8[Spy Console] &f" + event.getSender().getName() + "&7: &f/" + event.getCommand());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        socialSpyManager.remove(event.getPlayer());
    }
}
