package club.abelix.core.app.screenshare.listener;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import club.abelix.core.app.screenshare.service.ScreenShareService;
import lombok.RequiredArgsConstructor;
import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class ScreenShareListener implements Listener {

    private final ScreenShareService service;

    @Subscribe
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        service.finishByAdmin(event.getGamer());
        service.finishByTarget(event.getGamer());
    }
}
