package club.abelix.api.server.game.listener.redirect;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.event.GamerUnloadEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.game.manager.GameRedirectLobbyManager;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>GamerRedirectListener creation on 01.02.2024 at 01:00</p>
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GamerRedirectToLobbyListener extends YListener {

    GameRedirectLobbyManager manager = GameRedirectLobbyManager.INSTANCE;


    public GamerRedirectToLobbyListener(@NotNull AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onGamerUnload(final GamerUnloadEvent event) {
        val gamer = event.getGamer();
        if (!manager.isWaitSend(gamer)) {
            return;
        }

        manager.remove(gamer, true);
    }
}
