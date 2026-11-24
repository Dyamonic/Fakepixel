package club.abelix.core.plugin.forum.listener;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.*;
import club.abelix.core.plugin.forum.manager.ForumSqlManager;
import club.abelix.core.plugin.forum.util.XenApi;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ForumListener implements Listener {

    XenApi xenApi;
    ForumSqlManager forumSqlManager;

    @Subscribe
    public void onJoin(final @NonNull GamerJoinEvent event) {
        val gamer = event.getGamer();

        if (!xenApi.canRegister(gamer)) {
            return;
        }

        forumSqlManager.loadLinkedId(gamer).thenAccept(forumId -> {
            if (forumId > 0) {
                xenApi.setGroup(forumId, gamer);
                forumSqlManager.addIdToCache(gamer, forumId);
            } else {
                gamer.sendLocalizedMessage("forum_not_linked");
            }
        });
    }


    @Subscribe
    public void onPlayerAddRank(final @NonNull PlayerRankAddEvent event) {
        onPlayerRankChange(event);
    }

    @Subscribe
    public void onPlayerRemoveRank(final @NonNull PlayerRankRemoveEvent event) {
        onPlayerRankChange(event);
    }

    private void onPlayerRankChange(final @NonNull RankChangedEvent event) {
        val gamer = event.getGamer();

        forumSqlManager.getOrLoadForumId(gamer).thenAccept(forumId -> {
            if (forumId > 0) {
                xenApi.setGroup(forumId, gamer);
            }
        });
    }

    @Subscribe
    public void onQuit(final @NonNull PlayerQuitEvent event) {
        forumSqlManager.removeLinkedId(event.getGamer());
    }
}
