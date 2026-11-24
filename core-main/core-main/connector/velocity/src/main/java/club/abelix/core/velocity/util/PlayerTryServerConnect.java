package club.abelix.core.velocity.util;

import club.abelix.core.velocity.gamer.Gamer;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class PlayerTryServerConnect {
    private final Gamer gamer;
    private final List<RegisteredServer> tryServers;
    private final AtomicInteger currentTry = new AtomicInteger(0);
    private final CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

    private void successHandler() {
        resultFuture.complete(true);
    }

    private void failHandler(ConnectionRequestBuilder.Status status, Optional<Component> component) {
        // Если отменил плагин или игрок уже подключен к этому серверу
        if (status == ConnectionRequestBuilder.Status.CONNECTION_CANCELLED
                || status == ConnectionRequestBuilder.Status.ALREADY_CONNECTED) {
            component.ifPresent(gamer::sendMessage);
            resultFuture.complete(false);
            return;
        }

        if (currentTry.get() >= tryServers.size() - 1) {
            component.ifPresent(gamer::sendMessage);
            resultFuture.complete(false);
            return;
        }

        RegisteredServer tryServer = tryServers.get(currentTry.incrementAndGet());
        gamer.sendTo(tryServer, this::successHandler, this::failHandler);
    }

    public CompletableFuture<Boolean> connect() {
        gamer.sendTo(tryServers.get(currentTry.get()), this::successHandler, this::failHandler);
        return resultFuture;
    }
}
