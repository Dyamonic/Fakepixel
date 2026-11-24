package club.abelix.core.app.screenshare.service;

import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.screenshare.data.ScreenShareData;
import club.abelix.network.packet.play.server.SScreenShareFinishPacket;
import club.abelix.network.packet.play.server.SScreenShareStartPacket;
import club.abelix.network.type.connect.ConnectStatus;
import club.abelix.network.type.mode.Lobby;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class ScreenShareService {

    Map<Gamer, ScreenShareData> adminToSession = new ConcurrentHashMap<>();
    Map<Gamer, ScreenShareData> suspectToSession = new ConcurrentHashMap<>();

    public void start(
            final @NotNull Gamer admin,
            final @NotNull Gamer target
    ) throws CommandTranslationException {
        if (adminToSession.containsKey(admin)) {
            throw new CommandTranslationException("screenshare_start_error_admin_busy");
        }

        if (suspectToSession.containsKey(target)) {
            throw new CommandTranslationException("screenshare_start_error_target_busy");
        }

        val screenShareData = new ScreenShareData(
                target,
                admin
        );

        adminToSession.put(admin, screenShareData);
        suspectToSession.put(target, screenShareData);

        val packet = new SScreenShareStartPacket(
                target.getPlayerId(),
                admin.getPlayerId()
        );

        attemptToSend(screenShareData)
                .thenAccept(server -> {
                    server.sendPacket(packet);
                    screenShareData.setServer(server);

                    log.info("Started screenshare session between admin {} and target {} on server {}",
                            admin.getName(), target.getName(), server.getName());
                })
                .exceptionally(throwable -> {
                    adminToSession.remove(admin);
                    suspectToSession.remove(target);
                    log.error("Failed to start screenshare session", throwable);
                    admin.sendMessage("§cFailed to start screenshare session.");
                    return null;
                });
    }

    public void finishByAdmin(
            Gamer admin
    ) {
        val session = adminToSession.remove(admin);

        if (session != null) {
            finishSession(session);
        }
    }

    public void finishByTarget(
            Gamer target
    ) {
        val session = suspectToSession.remove(target);

        if (session != null) {
            finishSession(session);
        }
    }

    private void finishSession(ScreenShareData data) {
        val admin = data.getAdmin();
        val target = data.getTarget();

        suspectToSession.remove(data.getTarget());
        adminToSession.remove(admin);

        val finishPacket = new SScreenShareFinishPacket(data.getTarget().getPlayerId());

        if (data.getServer() != null) {
            data.getServer().sendPacket(finishPacket);
        }

        log.info("Finished screenshare session between admin {} and target {}",
                admin.getName(), target.getName());

        admin.sendMessage("ScreenShare session has been finished.");
        target.sendMessage("ScreenShare session has been finished.");
    }

    private CompletableFuture<AbstractServer> attemptToSend(ScreenShareData data) throws CommandTranslationException {
        val target = data.getTarget();
        val admin = data.getAdmin();

        val serverToSend = selectSuitableServer();

        if (serverToSend.getOnline() + 2 > serverToSend.getSlots()) {
            return CompletableFuture.failedFuture(new RuntimeException("Server is full"));
        }

        val adminFuture = admin.sendTo(serverToSend)
                .thenCompose(res -> {
                    if (res != ConnectStatus.SUCCESS) {
                        return CompletableFuture.failedFuture(new RuntimeException("Failed to send admin: " + res));
                    }
                    return CompletableFuture.completedFuture(null);
                });

        val targetFuture = target.sendTo(serverToSend)
                .thenCompose(res -> {
                    if (res != ConnectStatus.SUCCESS) {
                        return CompletableFuture.failedFuture(new RuntimeException("Failed to send target: " + res));
                    }
                    return CompletableFuture.completedFuture(null);
                });

        return CompletableFuture.allOf(adminFuture, targetFuture).thenApply(__ -> serverToSend);
    }

    private AbstractServer selectSuitableServer() throws CommandTranslationException {
        return ServerManager.IMP.getServers(Lobby.HUB)
                .stream()
                .findAny()
                .orElseThrow(() -> new CommandTranslationException("screenshare_start_error_no_hub_server"));
    }
}
