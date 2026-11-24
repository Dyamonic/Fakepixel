package club.abelix.core.app.data.friend;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import club.abelix.core.app.event.gamer.PlayerVanishEvent;
import lombok.NonNull;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class FriendListener implements Listener {

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        Gamer gamer = event.getGamer();

        FriendManager.INSTANCE.removePlayer(gamer);

        notifyFriendStatus(gamer, false, false);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerVanish(final PlayerVanishEvent event) {
        Gamer gamer = event.getGamer();

        notifyFriendStatus(gamer, !gamer.isVanished(), true);
    }

    private void notifyFriendStatus(@NonNull final Gamer gamer, final boolean online, final boolean ignoreVanish) {
        for (int playerId : gamer.getFriends().toArray()) {
            Gamer friend = Gamer.getGamer(playerId);

            if (friend == null || friend == gamer) {
                continue;
            }

            boolean vanished = gamer.isVanished() && !friend.canSeeVanish();

            if (!ignoreVanish && vanished) {
                continue;
            }

            friend.sendLocalizedMessage(
                    online ? "friend_join_the_game" : "friend_left_the_game",
                    "%name%", gamer.getDisplayName()
            );
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerJoin(final GamerJoinEvent event) {
        Gamer gamer = event.getGamer();

        notifyFriendStatus(gamer, true, false);

        List<Gamer> online = gamer.getOnlineFriends();

        if (online.isEmpty()) {
            return;
        }

        StringBuilder list = new StringBuilder(gamer.getLanguage().getMessage("friends_online"));

        int count = 0;

        for (Gamer target : online) {
            AbstractServer server = target.getServer();

            if (server == null) {
                continue;
            }

            if (count >= 5) {
                list.append("\n").append(
                        gamer.getLanguage().getMessage("friend_online_others_entry", "%count%", online.size() - count)
                );

                break;
            }

            if (target.isVanished() && !gamer.canSeeVanish()) {
                continue;
            }

            list.append("\n").append(gamer.getLanguage().getMessage("friend_online_entry", "%server%", server.getMode().getDisplayName(), "%name%", target.getDisplayName()));

            count++;
        }

        gamer.sendMessage(list.toString());
    }
}
