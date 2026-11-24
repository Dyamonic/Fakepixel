package club.abelix.api.server.lobby.tutorial;

import club.abelix.api.base.camera.CameraDestroyReason;
import club.abelix.api.base.camera.follow.FollowCamera;
import club.abelix.core.bukkit.HeartBeat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;

public class LobbyTutorialCamera extends FollowCamera {

    private final Iterator<LobbyTutorialSegment> iterator;
    private BukkitTask task;

    public LobbyTutorialCamera(LobbyTutorial tutorial, Player player) {
        super(player, player.getLocation(), tutorial.getProperties());

        this.iterator = tutorial.getSegments().iterator();
    }

    @Override
    public void onDestroy() {
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void onFollow() {
        if (!iterator.hasNext()) {
            destroy(CameraDestroyReason.CUSTOM);
            return;
        }

        LobbyTutorialSegment segment = iterator.next();

        if (task != null) {
            task.cancel();
            task = null;
        }

        LobbyTutorialTitle title = segment.getTitle();

        if (title != null) {
            gamer.sendLocalizedTitle(title.getFadeIn(), title.getStay(), title.getFadeOut(),
                    title.getTitle(), title.getSubtitle());
        }

        String message = segment.getMessage();

        if (message != null) {
            gamer.sendLocalizedMessage(message);
        }

        task = HeartBeat.runLaterSync(this::follow, segment.getDuration());
    }
}
