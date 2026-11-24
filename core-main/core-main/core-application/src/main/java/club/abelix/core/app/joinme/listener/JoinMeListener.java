package club.abelix.core.app.joinme.listener;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.arena.ArenaChangeStateEvent;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import club.abelix.core.app.event.gamer.PlayerRedirectEvent;
import club.abelix.core.app.joinme.service.JoinMeService;
import club.abelix.network.common.enums.GameState;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class JoinMeListener implements Listener {

    JoinMeService joinMeService;

    @Subscribe
    public void onGamerQuit(PlayerQuitEvent event) {
        val gamer = event.getGamer();

        if (joinMeService.hasActiveJoinMe(gamer)) {
            joinMeService.stopJoinMe(gamer);
        }
    }

    @Subscribe
    public void onServerChange(PlayerRedirectEvent event) {
        val gamer = event.getGamer();

        if (joinMeService.hasActiveJoinMe(gamer)) {
            joinMeService.stopJoinMe(gamer);
        }
    }

    @Subscribe
    public void onStateChange(ArenaChangeStateEvent event) {
        val newState = event.getNewState();

        if (newState == GameState.STARTING || newState == GameState.ENDING) {
            joinMeService.findByArenaServer(event.getServer()).ifPresent(record -> {
                joinMeService.stopJoinMe(record.getGamer());
                record.getGamer().sendLocalizedMessage("joinme_command_stopped_arena_state");
            });
        }
    }

}
