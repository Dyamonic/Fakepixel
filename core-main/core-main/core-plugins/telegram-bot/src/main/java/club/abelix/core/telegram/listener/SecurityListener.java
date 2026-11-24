package club.abelix.core.telegram.listener;

import club.abelix.commons.data.auth.AuthSqlHandler;
import club.abelix.commons.data.rank.service.UserRanksService;
import club.abelix.commons.language.Language;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.event.EventPriority;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.app.event.gamer.PlayerRankAddEvent;
import club.abelix.core.app.event.gamer.PlayerRankRemoveEvent;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.core.telegram.user.sql.BotUserSqlHandler;
import club.abelix.network.packet.play.both.BUserRankRefreshPacket;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityListener implements Listener {

    private static final String RANK_PERMISSION = "rank.sensitive";

    UserRanksService userRanksService;

    @Subscribe(priority = EventPriority.LOW, threadMode = ThreadMode.ASYNC)
    public void onPlayerJoin(final GamerJoinEvent event) {
        val gamer = event.getGamer();

        if (check(gamer)) return;
        removeSensitiveRank(gamer);
        gamer.localizedKick("sensitive_role_unavailable_kick");
    }

    @Subscribe(priority = EventPriority.LOW)
    public void onAddRank(final PlayerRankAddEvent event) {
        val gamer = event.getGamer();

        if (check(gamer)) return;
        removeSensitiveRank(gamer);
        gamer.localizedKick("sensitive_role_unavailable_kick");
    }

    private boolean check(@NonNull Gamer gamer) {
        if (!gamer.hasPermission(RANK_PERMISSION)) return true;

        val name = gamer.getName();

        val botUser = BotUserSqlHandler.INSTANCE.getBotUser(name);
        val linkLicense = AuthSqlHandler.INSTANCE.isOnlineMode(name).join();

        return botUser != BotUser.EMPTY_BOT_USER || linkLicense;
    }

    private void removeSensitiveRank(Gamer gamer) {
        val ranks = gamer.getRanks();

        val futures = ranks.asSet().stream()
                .filter(userRank -> userRank.rank().hasPermission(RANK_PERMISSION))
                .map(userRank -> userRanksService.removeUserRank(gamer.getPlayerId(), userRank.id())
                        .thenAccept(removeUserRankResponse -> {

                            ranks.removeRank(userRank.id());

                            ServerManager.IMP.broadcastStaff("command.staffchat",
                                    "sensitive_role_unavailable_staff_anouncment",
                                    "%target%", gamer.getDisplayName(),
                                    "%rank%", userRank.rank().getDisplayName(Language.ENGLISH)
                            );

                            CoreApplication.getInstance().getPluginManager().callEvent(
                                    new PlayerRankRemoveEvent(gamer, userRank));

                        }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).thenRun(() -> {
            gamer.loadRanks();
            gamer.updateDisplayName();
            notifyServers(gamer);
        });
    }

    private void notifyServers(Gamer gamer) {
        val packet = new BUserRankRefreshPacket(gamer.getPlayerId());

        gamer.sendToProxy(packet);
        gamer.sendToServer(packet);
    }
}
