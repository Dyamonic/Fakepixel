package club.abelix.core.velocity.util;

import club.abelix.commons.util.random.RandomUtil;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.packet.play.client.CGetBestServer;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Special;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@UtilityClass
public class ServerUtil {

    public RegisteredServer getRandomLimbo() {
        return getRandomServer(Special.LIMBO);
    }

    public RegisteredServer getRandomServer(@NonNull ServerMode mode) {
        return getRandomServer(Collections.singletonList(mode), Collections.emptySet());
    }

    public RegisteredServer getRandomServer(@NonNull List<ServerMode> modes, @NonNull Set<RegisteredServer> excludes) {
        List<RegisteredServer> serversByPrefix = new ArrayList<>();
        var proxyServer = StarshipVelocity.getInstance().getProxy();

        for (ServerMode mode : modes) {
            for (RegisteredServer server : proxyServer.getAllServers()) {
                if (matchesMode(server, mode)
                        && !excludes.contains(server)) {
                    serversByPrefix.add(server);
                }
            }
        }

        return serversByPrefix.isEmpty() ? null : RandomUtil.getRandomElement(serversByPrefix);
    }

    public RegisteredServer getRandomServer(@NonNull ServerMode mode, @NonNull Set<RegisteredServer> excludes) {
        return getRandomServer(Collections.singletonList(mode), excludes);
    }

    public List<RegisteredServer> getServerList(
            @NonNull ServerMode mode,
            @NonNull Set<RegisteredServer> excludes
    ) {
        Set<String> names = excludes.stream()
                .map(rs -> rs.getServerInfo().getName())
                .collect(Collectors.toUnmodifiableSet());

        var proxy = StarshipVelocity.getInstance().getProxy();

        return proxy.getAllServers().stream()
                .filter(s -> matchesMode(s, mode))
                .filter(s -> !names.contains(s.getServerInfo().getName()))
                .collect(Collectors.toList());
    }

    private boolean matchesMode(RegisteredServer server, ServerMode mode) {
        String serverName = server.getServerInfo().getName();
        String modeName = mode.getServerName();

        return serverName.equalsIgnoreCase(modeName)
                || serverName.regionMatches(
                true,
                0,
                modeName + "-",
                0,
                modeName.length() + 1
        );
    }

    public List<RegisteredServer> getServerList(@NonNull ServerMode mode) {
        return getServerList(mode, Collections.emptySet());
    }

    public CompletableFuture<Optional<String>> getBestServer(@NonNull Gamer gamer, @NonNull ServerMode mode, boolean max) {
        return Connection.awaitPacket(new CGetBestServer(gamer.getName(), gamer.getAddress(), mode, max))
                .thenApply(result -> Optional.ofNullable(result.getServer()))
                .exceptionally(cause -> Optional.empty());
    }
}
