package club.abelix.api.base.song.listener;

import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.song.NoteBlockManager;
import club.abelix.api.server.game.event.GameQuitEvent;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

/**
 * @author artem
 */
public class NoteBlockListener extends YListener {

    public NoteBlockListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onQuit(GameQuitEvent event) {
        NoteBlockManager.IMP.stopPlaying(event.getGamer().getPlayer());
    }
}
