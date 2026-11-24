package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.rank.UserRank;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.UserRanksService;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.packet.play.server.SRefreshRanksPacket;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@NamedCommand("refreshranks")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RefreshRanksCommand {

    RanksService ranksService;
    UserRanksService userRanksService;

    @Executor
    @WithPermission("command.refreshranks")
    public void execute(CoreExecutionSession session) {
        val startTime = System.currentTimeMillis();

        session.sendMessage(ChatColor.DARK_GRAY + "Refreshing ranks... This operation may take some time and temporarily increase server load.");

        ranksService.refreshRanks().whenComplete((res, ex) -> {
            if (ex != null) {
                session.sendMessage(ChatColor.RED + "Refresh attempt canceled!");
                session.sendMessage(ChatColor.RED + "Refresh ranks completed with error: " + ex.getMessage());
                return;
            }

            ranksService.load();

            ServerManager.IMP.sendToProxies(new SRefreshRanksPacket());
            ServerManager.IMP.sendToServers(new SRefreshRanksPacket());

            userRanksService.refreshUsersRanks(Gamer.getGamers()).join();

            session.sendMessage(ChatColor.GREEN + "Refresh ranks completed successfully in " + (System.currentTimeMillis() - startTime) + "ms");
        });
    }

}
