package club.abelix.api.base.gamehistory;

import club.abelix.api.base.gamehistory.model.GameHistoryEntryPlayer;
import club.abelix.api.base.gamehistory.model.GameInfo;
import club.abelix.api.base.gamehistory.model.GamesPage;
import club.abelix.api.base.gamehistory.model.PartyInfo;
import club.abelix.api.base.gamehistory.model.PlayerInfo;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.gamehistory.grpc.GetGamesByPlayerRequest;
import club.abelix.gamehistory.grpc.GameHistoryServiceGrpc;
import club.abelix.gamehistory.grpc.SaveGameRequest;
import club.abelix.gamehistory.grpc.SaveGameResponse;
import club.abelix.network.type.map.GameMap;
import club.abelix.network.type.mode.ServerMode;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.Epoll;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameHistoryService {

    ManagedChannel channel;
    GameHistoryServiceGrpc.GameHistoryServiceFutureStub futureStub;

    public GameHistoryService(String host, int port) {
        EventLoopGroup group = Epoll.isAvailable()
                ? new EpollEventLoopGroup()
                : new NioEventLoopGroup();

        this.channel = NettyChannelBuilder.forAddress(host, port)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .enableRetry()
                .maxRetryAttempts(3)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .eventLoopGroup(group)
                .channelType(Epoll.isAvailable()
                        ? EpollSocketChannel.class
                        : NioSocketChannel.class)
                .build();
        this.futureStub = GameHistoryServiceGrpc.newFutureStub(channel);
    }

    public CompletableFuture<String> saveGame(GameInfo gameInfo) {
        var request = SaveGameRequest.newBuilder()
                .setGame(GameHistoryConverter.toProto(gameInfo))
                .build();

        return sendUnary(
                futureStub.withDeadlineAfter(5, TimeUnit.SECONDS).saveGame(request),
                "save game",
                gameInfo.getGameId()
        ).thenApply(SaveGameResponse::getGameId);
    }

    public CompletableFuture<String> saveGame(ServerMode mode,
                                              String serverName,
                                              GameMap gameMap,
                                              long startTime,
                                              long endTime,
                                              int totalPlayersCount,
                                              boolean privateGame,
                                              List<Gamer> players,
                                              List<Gamer> winners) {
        return saveGame(mode, serverName, gameMap, startTime, endTime,
                totalPlayersCount, privateGame, players, winners, Map.of());
    }

    public CompletableFuture<String> saveGame(ServerMode mode,
                                              String serverName,
                                              GameMap gameMap,
                                              long startTime,
                                              long endTime,
                                              int totalPlayersCount,
                                              boolean privateGame,
                                              List<Gamer> players,
                                              List<Gamer> winners,
                                              Map<Gamer, Map<String, String>> metadata) {
        return saveGame(GameInfo.builder()
                .mode(mode.getName())
                .serverName(serverName)
                .mapName(gameMap.getTechnicalName())
                .startTime(startTime)
                .endTime(endTime)
                .totalPlayersCount(totalPlayersCount)
                .privateGame(privateGame)
                .players(players.stream().map(g -> toPlayerInfo(g, metadata)).toList())
                .winners(winners.stream().map(g -> toPlayerInfo(g, metadata)).toList())
                .build());
    }

    public CompletableFuture<String> saveGame(ServerMode mode,
                                              String serverName,
                                              GameMap gameMap,
                                              long startTime,
                                              long endTime,
                                              int totalPlayersCount,
                                              boolean privateGame,
                                              List<GameHistoryEntryPlayer> entryPlayers) {
        List<PlayerInfo> playerInfos = entryPlayers.stream()
                .map(this::toPlayerInfo)
                .toList();
        List<PlayerInfo> winnerInfos = entryPlayers.stream()
                .filter(GameHistoryEntryPlayer::isWinner)
                .map(this::toPlayerInfo)
                .toList();

        return saveGame(GameInfo.builder()
                .mode(mode.getName())
                .serverName(serverName)
                .mapName(gameMap.getTechnicalName())
                .startTime(startTime)
                .endTime(endTime)
                .totalPlayersCount(totalPlayersCount)
                .privateGame(privateGame)
                .players(playerInfos)
                .winners(winnerInfos)
                .build());
    }

    public CompletableFuture<GamesPage> getGamesByPlayer(Gamer gamer, int page, int size) {
        return getGamesByPlayer(gamer.getPlayerId(), page, size);
    }

    public CompletableFuture<GamesPage> getGamesByPlayer(Gamer gamer) {
        return getGamesByPlayer(gamer.getPlayerId(), 0, 20);
    }

    public CompletableFuture<GamesPage> getGamesByPlayer(long playerId, int page, int size) {
        var request = GetGamesByPlayerRequest.newBuilder()
                .setPlayerId(playerId)
                .setPage(page)
                .setSize(size)
                .build();

        return sendUnary(
                futureStub.withDeadlineAfter(3, TimeUnit.SECONDS).getGamesByPlayer(request),
                "get games for player",
                playerId
        ).thenApply(GameHistoryConverter::toGamesPage);
    }

    public CompletableFuture<GamesPage> getGamesByPlayer(long playerId) {
        return getGamesByPlayer(playerId, 0, 20);
    }

    public CompletableFuture<List<GameInfo>> getGamesList(Gamer gamer, int page, int size) {
        return getGamesByPlayer(gamer, page, size).thenApply(GamesPage::getGames);
    }

    public CompletableFuture<List<GameInfo>> getGamesList(Gamer gamer) {
        return getGamesList(gamer, 0, 20);
    }

    public CompletableFuture<List<GameInfo>> getGamesList(long playerId, int page, int size) {
        return getGamesByPlayer(playerId, page, size).thenApply(GamesPage::getGames);
    }

    public CompletableFuture<List<GameInfo>> getGamesList(long playerId) {
        return getGamesList(playerId, 0, 20);
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Interrupted while shutting down GameHistoryService channel", e);
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private <T> CompletableFuture<T> sendUnary(com.google.common.util.concurrent.ListenableFuture<T> call,
                                               String operation,
                                               Object context) {
        CompletableFuture<T> future = new CompletableFuture<>();

        call.addListener(() -> {
            try {
                future.complete(call.get());
            } catch (java.util.concurrent.CancellationException e) {
                if (!future.isCancelled()) {
                    future.completeExceptionally(new RuntimeException("Cancelled while trying to " + operation, e));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(new RuntimeException("Interrupted while waiting to " + operation, e));
            } catch (java.util.concurrent.ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                if (cause instanceof StatusRuntimeException statusException) {
                    log.error("Failed to {}: {} (status: {})", operation, context, statusException.getStatus(), statusException);
                } else {
                    log.error("Failed to {}: {}", operation, context, cause);
                }
                future.completeExceptionally(new RuntimeException("Failed to " + operation, cause));
            }
        }, Runnable::run);

        future.whenComplete((unused, throwable) -> {
            if (future.isCancelled()) {
                call.cancel(true);
            }
        });

        return future;
    }

    private PlayerInfo toPlayerInfo(Gamer gamer, Map<Gamer, Map<String, String>> metadata) {
        return PlayerInfo.builder()
                .playerId(gamer.getPlayerId())
                .playerName(gamer.getPlayerName())
                .metadata(metadata.getOrDefault(gamer, Map.of()))
                .build();
    }

    private PlayerInfo toPlayerInfo(GameHistoryEntryPlayer entry) {
        Map<String, String> metadata = new java.util.HashMap<>(entry.getMetadata());

        // Команда
        if (entry.isHasTeam()) {
            metadata.put("team_index", String.valueOf(entry.getTeamIndex()));
            metadata.put("team_character", String.valueOf(entry.getTeamCharacter()));
            if (entry.getTeamColorName() != null) {
                metadata.put("team_color", entry.getTeamColorName());
            }
        }

        // Пати
        if (entry.isInParty()) {
            metadata.put("in_party", "true");
            if (entry.getPartyLeaderName() != null) {
                metadata.put("party_leader_name", entry.getPartyLeaderName());
                metadata.put("party_leader_id", String.valueOf(entry.getPartyLeaderId()));
            }
            if (entry.getPartyMemberIds() != null && !entry.getPartyMemberIds().isEmpty()) {
                metadata.put("party_member_ids", entry.getPartyMemberIds().stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + "," + b)
                        .orElse(""));
            }
        }

        // Победитель
        metadata.put("winner", String.valueOf(entry.isWinner()));

        PartyInfo partyInfo = null;
        if (entry.isInParty()) {
            partyInfo = PartyInfo.builder()
                    .partyId(entry.getPartyLeaderName() != null
                            ? "party_" + entry.getPartyLeaderId()
                            : "unknown")
                    .build();
        }

        return PlayerInfo.builder()
                .playerId(entry.getPlayerId())
                .playerName(entry.getPlayerName())
                .party(partyInfo)
                .metadata(metadata)
                .build();
    }
}
