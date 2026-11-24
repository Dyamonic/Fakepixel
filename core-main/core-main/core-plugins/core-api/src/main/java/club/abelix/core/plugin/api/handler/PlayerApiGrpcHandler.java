package club.abelix.core.plugin.api.handler;

import club.abelix.commons.data.gamehistory.GameHistoryEntry;
import club.abelix.commons.data.gamehistory.GameHistorySqlHandler;
import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.commons.data.rank.UserRank;
import club.abelix.core.api.proto.v1.AddPlayerGoldByIdRequest;
import club.abelix.core.api.proto.v1.AddPlayerGoldByNameRequest;
import club.abelix.core.api.proto.v1.AddPlayerGoldResponse;
import club.abelix.core.api.proto.v1.AddPlayerRankByIdRequest;
import club.abelix.core.api.proto.v1.AddPlayerRankByNameRequest;
import club.abelix.core.api.proto.v1.AddPlayerRankResponse;
import club.abelix.core.api.proto.v1.BanPlayerRequest;
import club.abelix.core.api.proto.v1.BanPlayerResponse;
import club.abelix.core.api.proto.v1.CheckPlayerRankByIdRequest;
import club.abelix.core.api.proto.v1.CheckPlayerRankByNameRequest;
import club.abelix.core.api.proto.v1.CheckPlayerRankResponse;
import club.abelix.core.api.proto.v1.GameHistoryRecordProto;
import club.abelix.core.api.proto.v1.GetGameHistoryResponse;
import club.abelix.core.api.proto.v1.GetPlayerCoinsByIdRequest;
import club.abelix.core.api.proto.v1.GetPlayerCoinsByNameRequest;
import club.abelix.core.api.proto.v1.GetPlayerCoinsResponse;
import club.abelix.core.api.proto.v1.GetPlayerCurrentServerByIdRequest;
import club.abelix.core.api.proto.v1.GetPlayerCurrentServerByNameRequest;
import club.abelix.core.api.proto.v1.GetPlayerCurrentServerResponse;
import club.abelix.core.api.proto.v1.GetPlayerFriendsByIdRequest;
import club.abelix.core.api.proto.v1.GetPlayerFriendsByNameRequest;
import club.abelix.core.api.proto.v1.GetPlayerFriendsResponse;
import club.abelix.core.api.proto.v1.GetPlayerGameHistoryByIdRequest;
import club.abelix.core.api.proto.v1.GetPlayerGameHistoryByNameRequest;
import club.abelix.core.api.proto.v1.GetPlayerPartyByIdRequest;
import club.abelix.core.api.proto.v1.GetPlayerPartyByNameRequest;
import club.abelix.core.api.proto.v1.GetPlayerPartyResponse;
import club.abelix.core.api.proto.v1.GetPlayerSessionByIdRequest;
import club.abelix.core.api.proto.v1.GetPlayerSessionByNameRequest;
import club.abelix.core.api.proto.v1.GetPlayerSessionResponse;
import club.abelix.core.api.proto.v1.GetPlayerSkinByIdRequest;
import club.abelix.core.api.proto.v1.GetPlayerSkinByNameRequest;
import club.abelix.core.api.proto.v1.GetPlayerSkinResponse;
import club.abelix.core.api.proto.v1.GetPlayerStatsByIdRequest;
import club.abelix.core.api.proto.v1.GetPlayerStatsByNameRequest;
import club.abelix.core.api.proto.v1.GetPlayerStatsResponse;
import club.abelix.core.api.proto.v1.GivePlayerBoosterByNameRequest;
import club.abelix.core.api.proto.v1.GivePlayerBoosterByNameResponse;
import club.abelix.core.api.proto.v1.GivePlayerJoinMessageByNameRequest;
import club.abelix.core.api.proto.v1.GivePlayerJoinMessageByNameResponse;
import club.abelix.core.api.proto.v1.GivePlayerTitleByNameRequest;
import club.abelix.core.api.proto.v1.GivePlayerTitleByNameResponse;
import club.abelix.core.api.proto.v1.GiveSkyBlockGemsByPlayerNameRequest;
import club.abelix.core.api.proto.v1.GiveSkyBlockGemsByPlayerNameResponse;
import club.abelix.core.api.proto.v1.KickPlayerRequest;
import club.abelix.core.api.proto.v1.KickPlayerResponse;
import club.abelix.core.api.proto.v1.MutePlayerRequest;
import club.abelix.core.api.proto.v1.MutePlayerResponse;
import club.abelix.core.api.proto.v1.PasswordResetRequest;
import club.abelix.core.api.proto.v1.PasswordResetResponse;
import club.abelix.core.api.proto.v1.PlayerApiGrpc;
import club.abelix.core.api.proto.v1.PlayerInfoByIdRequest;
import club.abelix.core.api.proto.v1.PlayerInfoByNameRequest;
import club.abelix.core.api.proto.v1.PlayerInfoResponse;
import club.abelix.core.api.proto.v1.PlayerSkinProto;
import club.abelix.core.api.proto.v1.RankInfoProto;
import club.abelix.core.api.proto.v1.SendPlayerMessageRequest;
import club.abelix.core.api.proto.v1.SendPlayerMessageResponse;
import club.abelix.core.api.proto.v1.SetPlayerGoldRequest;
import club.abelix.core.api.proto.v1.SetPlayerGoldResponse;
import club.abelix.core.api.proto.v1.TakePlayerRankRequest;
import club.abelix.core.api.proto.v1.TakePlayerRankResponse;
import club.abelix.core.api.proto.v1.UnbanPlayerByNameRequest;
import club.abelix.core.api.proto.v1.UnbanPlayerResponse;
import club.abelix.core.api.proto.v1.UnmutePlayerByNameRequest;
import club.abelix.core.api.proto.v1.UnmutePlayerResponse;
import club.abelix.core.api.proto.v1.UserRankType;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.plugin.api.service.PlayerApiService;
import club.abelix.network.packet.play.server.SGiveSkyblockGems;
import club.abelix.network.type.mode.Uncommon;
import club.abelix.ranks.commons.util.TimestampConverter;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PlayerApiGrpcHandler extends PlayerApiGrpc.PlayerApiImplBase {

    PlayerApiService playerApiService;

    @Override
    public void getPlayerInfoById(PlayerInfoByIdRequest request, StreamObserver<PlayerInfoResponse> responseObserver) {
        handleWithGamer(request.getUserId(), responseObserver, this::buildPlayerInfo);
    }

    @Override
    public void getPlayerInfoByName(PlayerInfoByNameRequest request, StreamObserver<PlayerInfoResponse> responseObserver) {
        val gamer = Gamer.getOfflineGamer(request.getUsername(), Gamer::load);

        if (gamer == null) {
            respondError(
                    responseObserver,
                    Status.NOT_FOUND,
                    "Gamer not found",
                    Status.NOT_FOUND.getCause()
            );
        } else {
            try {
                val response = buildPlayerInfo(gamer);

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                responseObserver.onError(e);
            }
        }
    }

    @Override
    public void checkPlayerRankById(CheckPlayerRankByIdRequest request, StreamObserver<CheckPlayerRankResponse> responseObserver) {
        handleWithGamer(request.getUserId(), responseObserver, gamer -> {
            val hasRank = gamer.getRanks().stream()
                    .anyMatch(r -> r.rank().getId() == request.getRankId());
            return CheckPlayerRankResponse.newBuilder().setHasRank(hasRank).build();
        });
    }

    @Override
    public void checkPlayerRankByName(CheckPlayerRankByNameRequest request, StreamObserver<CheckPlayerRankResponse> responseObserver) {
        handleWithGamer(request.getUsername(), responseObserver, gamer -> {
            val hasRank = gamer.getRanks().stream()
                    .anyMatch(r -> r.rank().getId() == request.getRankId());
            return CheckPlayerRankResponse.newBuilder().setHasRank(hasRank).build();
        });
    }

    @Override
    public void getPlayerCoinsById(GetPlayerCoinsByIdRequest request, StreamObserver<GetPlayerCoinsResponse> responseObserver) {
        handleWithGamer(request.getUserId(), responseObserver, gamer -> {
            val coins = request.getMode().isEmpty()
                    ? gamer.getCoins()
                    : gamer.getCoins(request.getMode());
            return GetPlayerCoinsResponse.newBuilder().setCoins(coins).build();
        });
    }

    @Override
    public void getPlayerCoinsByName(GetPlayerCoinsByNameRequest request, StreamObserver<GetPlayerCoinsResponse> responseObserver) {
        handleWithGamer(request.getName(), responseObserver, gamer -> {
            val coins = request.getMode().isEmpty()
                    ? gamer.getCoins()
                    : gamer.getCoins(request.getMode());
            return GetPlayerCoinsResponse.newBuilder().setCoins(coins).build();
        });
    }

    @Override
    public void addPlayerGoldById(AddPlayerGoldByIdRequest request, StreamObserver<AddPlayerGoldResponse> responseObserver) {
        handleWithGamer(request.getUserId(), responseObserver, gamer -> {
            gamer.addGold(request.getAmount());
            return AddPlayerGoldResponse.newBuilder().build();
        });
    }

    @Override
    public void addPlayerGoldByName(AddPlayerGoldByNameRequest request, StreamObserver<AddPlayerGoldResponse> responseObserver) {
        handleWithGamer(request.getUserName(), responseObserver, gamer -> {
            gamer.addGold(request.getAmount());
            return AddPlayerGoldResponse.newBuilder().build();
        });
    }

    @Override
    public void setPlayerGold(SetPlayerGoldRequest request, StreamObserver<SetPlayerGoldResponse> responseObserver) {
        handleWithGamer(request.getUserId(), responseObserver, gamer -> {
            gamer.takeGold(gamer.getGold());
            gamer.addGold(request.getAmount());
            return SetPlayerGoldResponse.newBuilder().build();
        });
    }

    @Override
    public void getPlayerSkinById(GetPlayerSkinByIdRequest request, StreamObserver<GetPlayerSkinResponse> responseObserver) {
        handleWithGamer(request.getUserId(), responseObserver, gamer -> {
            val skin = gamer.getSkin();
            return GetPlayerSkinResponse.newBuilder()
                    .setName(skin.getName())
                    .setUuid(skin.getUuid())
                    .setValue(skin.getValue())
                    .setSignature(skin.getSignature())
                    .build();
        });
    }

    @Override
    public void getPlayerSkinByName(GetPlayerSkinByNameRequest request, StreamObserver<GetPlayerSkinResponse> responseObserver) {
        handleWithGamer(request.getUsername(), responseObserver, gamer -> {
            val skin = gamer.getSkin();
            return GetPlayerSkinResponse.newBuilder()
                    .setName(skin.getName())
                    .setUuid(skin.getUuid())
                    .setValue(skin.getValue())
                    .setSignature(skin.getSignature())
                    .build();
        });
    }

    @Override
    public void getPlayerCurrentServerById(GetPlayerCurrentServerByIdRequest request, StreamObserver<GetPlayerCurrentServerResponse> responseObserver) {
        handleServiceCall(() -> playerApiService.getPlayerCurrentServer(request.getUserId()),
                serverName -> GetPlayerCurrentServerResponse.newBuilder().setServerName(serverName).build(),
                responseObserver);
    }

    @Override
    public void getPlayerCurrentServerByName(GetPlayerCurrentServerByNameRequest request, StreamObserver<GetPlayerCurrentServerResponse> responseObserver) {
        handleServiceCall(() -> playerApiService.getPlayerCurrentServer(request.getUsername()),
                serverName -> GetPlayerCurrentServerResponse.newBuilder().setServerName(serverName).build(),
                responseObserver);
    }

    @Override
    public void sendPlayerMessage(SendPlayerMessageRequest request, StreamObserver<SendPlayerMessageResponse> responseObserver) {
        val success = playerApiService.sendMessage(request.getUserId(), request.getMessage(), request.getLocalized());
        respond(responseObserver, SendPlayerMessageResponse.newBuilder().setSuccess(success).build());
    }

    @Override
    public void kickPlayer(KickPlayerRequest request, StreamObserver<KickPlayerResponse> responseObserver) {
        handlePunishment(() -> {
            playerApiService.kickPlayer(request.getUserId(), request.getReason(), request.getLocalized());
            return KickPlayerResponse.newBuilder().build();
        }, responseObserver);
    }

    @Override
    public void mutePlayer(MutePlayerRequest request, StreamObserver<MutePlayerResponse> responseObserver) {
        handlePunishment(() -> {
            playerApiService.mutePlayer(request.getUserId(), request.getReason(), request.getLocalized(), request.getDuration());
            return MutePlayerResponse.newBuilder().build();
        }, responseObserver);
    }

    @Override
    public void banPlayer(BanPlayerRequest request, StreamObserver<BanPlayerResponse> responseObserver) {
        handlePunishment(() -> {
            playerApiService.banPlayer(request.getUserId(), request.getReason(), request.getLocalized(), request.getDuration());
            return BanPlayerResponse.newBuilder().build();
        }, responseObserver);
    }

    @Override
    public void unmutePlayerByName(UnmutePlayerByNameRequest request, StreamObserver<UnmutePlayerResponse> responseObserver) {
        handlePunishment(() -> {
            playerApiService.unmute(request.getUserName());
            return UnmutePlayerResponse.newBuilder().build();
        }, responseObserver);
    }

    @Override
    public void unbanPlayerByName(UnbanPlayerByNameRequest request, StreamObserver<UnbanPlayerResponse> responseObserver) {
        handlePunishment(() -> {
            playerApiService.unban(request.getUserName());
            return UnbanPlayerResponse.newBuilder().build();
        }, responseObserver);
    }


    @Override
    public void addPlayerRankById(AddPlayerRankByIdRequest request, StreamObserver<AddPlayerRankResponse> responseObserver) {
        handleAsyncOperation(
                () -> playerApiService.givePlayerRank(request.getUserId(), request.getRankId(), request.getDuration()),
                () -> AddPlayerRankResponse.newBuilder().build(),
                responseObserver
        );
    }

    @Override
    public void addPlayerRankByName(AddPlayerRankByNameRequest request, StreamObserver<AddPlayerRankResponse> responseObserver) {
        handleAsyncOperation(
                () -> playerApiService.givePlayerRank(request.getUserName(), request.getRankId(), request.getDuration()),
                () -> AddPlayerRankResponse.newBuilder().build(),
                responseObserver
        );
    }

    @Override
    public void givePlayerTitleByName(GivePlayerTitleByNameRequest request, StreamObserver<GivePlayerTitleByNameResponse> responseObserver) {
        handlePunishment(() -> {
            playerApiService.giveTitle(request.getUserName(), request.getTitleId());
            return GivePlayerTitleByNameResponse.newBuilder().build();
        }, responseObserver);
    }

    @Override
    public void givePlayerJoinMessageByName(GivePlayerJoinMessageByNameRequest request, StreamObserver<GivePlayerJoinMessageByNameResponse> responseObserver) {
        handlePunishment(() -> {
            playerApiService.giveJoinMessage(request.getUserName(), request.getJoinMessageId());
            return GivePlayerJoinMessageByNameResponse.newBuilder().build();
        }, responseObserver);
    }

    @Override
    public void givePlayerBoosterByName(GivePlayerBoosterByNameRequest request, StreamObserver<GivePlayerBoosterByNameResponse> responseObserver) {
        handlePunishment(() -> {
            playerApiService.giveBooster(request.getUserName(), request.getBoosterType());
            return GivePlayerBoosterByNameResponse.newBuilder().build();
        }, responseObserver);
    }

    @Override
    public void giveSkyBlockGemsByPlayerName(GiveSkyBlockGemsByPlayerNameRequest request, StreamObserver<GiveSkyBlockGemsByPlayerNameResponse> responseObserver) {
        try {
            val packet = new SGiveSkyblockGems(request.getUserName(), request.getAmount());
            val server = ServerManager.IMP.getServer(Uncommon.SKYBLOCK);
            server.sendPacket(packet);
            respond(responseObserver, GiveSkyBlockGemsByPlayerNameResponse.newBuilder().build());
        } catch (Exception e) {
            respondError(responseObserver, Status.INTERNAL, "Failed to give SkyBlock gems: " + e.getMessage(), e);
        }
    }

    @Override
    public void getPlayerPartyById(GetPlayerPartyByIdRequest request, StreamObserver<GetPlayerPartyResponse> responseObserver) {
        handleParty(request.getUserId(), responseObserver);
    }

    @Override
    public void getPlayerPartyByName(GetPlayerPartyByNameRequest request, StreamObserver<GetPlayerPartyResponse> responseObserver) {
        handleParty(request.getUserName(), responseObserver);
    }

    private void handleParty(Object identifier, StreamObserver<GetPlayerPartyResponse> responseObserver) {
        val partyOpt = identifier instanceof Integer userId
                ? playerApiService.getPlayerParty(userId)
                : playerApiService.getPlayerParty((String) identifier);

        if (partyOpt.isEmpty()) {
            respondNotFound(responseObserver, "Party");
            return;
        }

        val party = partyOpt.get();
        val response = GetPlayerPartyResponse.newBuilder()
                .setPartyId(party.getId())
                .setIsPublic(party.isPublicStatus())
                .setPartyGamesEnabled(party.isPrivateGame())
                .setPublicSlots(party.getPublicSlots())
                .addAllMembers(party.getGamers().stream().map(this::buildPlayerInfo).toList())
                .setOwner(buildPlayerInfo(party.getLeader()))
                .build();

        respond(responseObserver, response);
    }

    @Override
    public void getPlayerFriendsById(GetPlayerFriendsByIdRequest request, StreamObserver<GetPlayerFriendsResponse> responseObserver) {
        handleFriends(request.getUserId(), responseObserver);
    }

    @Override
    public void getPlayerFriendsByName(GetPlayerFriendsByNameRequest request, StreamObserver<GetPlayerFriendsResponse> responseObserver) {
        handleFriends(request.getUsername(), responseObserver);
    }

    private void handleFriends(Object identifier, StreamObserver<GetPlayerFriendsResponse> responseObserver) {
        val friendsFuture = identifier instanceof Integer userId
                ? playerApiService.getPlayerFriends(userId)
                : playerApiService.getPlayerFriends((String) identifier);

        friendsFuture.thenAccept(friends -> {
                    val response = GetPlayerFriendsResponse.newBuilder()
                            .addAllFriends(friends.stream().map(this::buildPlayerInfo).toList())
                            .build();
                    respond(responseObserver, response);
                })
                .exceptionally(t -> {
                    respondError(responseObserver, Status.INTERNAL, "Failed to get friends", t.getCause());
                    return null;
                });
    }

    @Override
    public void getPlayerStatsById(GetPlayerStatsByIdRequest request, StreamObserver<GetPlayerStatsResponse> responseObserver) {
        handleStatsRequest(
                request.getUserId(),
                request.getMode(),
                responseObserver
        );
    }

    @Override
    public void getPlayerStatsByName(GetPlayerStatsByNameRequest request, StreamObserver<GetPlayerStatsResponse> responseObserver) {
        handleStatsRequest(
                request.getUsername(),
                request.getMode(),
                responseObserver
        );
    }

    private void handleStatsRequest(Object identifier, String mode, StreamObserver<GetPlayerStatsResponse> responseObserver) {
        playerApiService.findGamer(identifier).ifPresentOrElse(
                gamer -> playerApiService.getPlayerStats(gamer, mode).whenComplete((stats, throwable) -> {
                    if (throwable instanceof CompletionException cex) {
                        val original = cex.getCause();

                        if (original instanceof StatusRuntimeException status) {
                            respondError(
                                    responseObserver,
                                    status.getStatus(),
                                    status.getMessage(),
                                    original
                            );
                            return;
                        }
                    }

                    val response = GetPlayerStatsResponse.newBuilder()
                            .setMode(mode)
                            .putAllStats(stats)
                            .build();

                    respond(responseObserver, response);
                }),
                () -> respondError(
                        responseObserver,
                        Status.NOT_FOUND,
                        "Gamer not found",
                        Status.NOT_FOUND.getCause()
                )
        );
    }

    @Override
    public void getPlayerGameHistoryById(GetPlayerGameHistoryByIdRequest request, StreamObserver<GetGameHistoryResponse> responseObserver) {
        super.getPlayerGameHistoryById(request, responseObserver);
    }

    @Override
    public void getPlayerGameHistoryByName(GetPlayerGameHistoryByNameRequest request, StreamObserver<GetGameHistoryResponse> responseObserver) {
        CompletableFuture.runAsync(() -> handleWithGamer(request.getUsername(), responseObserver, gamer -> {
            val history = GameHistorySqlHandler.INSTANCE.getPlayerAllGameHistory(gamer.getPlayerId()).join();
            val response = GetGameHistoryResponse.newBuilder();

            response.addAllHistory(
                    history.stream().map(this::map).toList()
            );

            return response.build();
        }));
    }

    private GameHistoryRecordProto map(GameHistoryEntry entry) {
        val players = entry.getPlayers()
                .stream()
                .map(player -> NetworkSqlHandler.INSTANCE.getPlayerName(player.getPlayerId()))
                .toList();

        return GameHistoryRecordProto.newBuilder()
                .addAllPlayers(players)
                .setGame("%d-%s-%s".formatted(entry.getId(), entry.getMapName(), entry.getMini().name()))
                .setTimestamp(TimestampConverter.toProto(entry.getEndTIme().toLocalDateTime()))
                .build();
    }

    @Override
    public void getPlayerSessionById(GetPlayerSessionByIdRequest request, StreamObserver<GetPlayerSessionResponse> responseObserver) {
        respondError(responseObserver, Status.UNIMPLEMENTED, "Method not implemented", null);
    }

    @Override
    public void getPlayerSessionByName(GetPlayerSessionByNameRequest request, StreamObserver<GetPlayerSessionResponse> responseObserver) {
        respondError(responseObserver, Status.UNIMPLEMENTED, "Method not implemented", null);
    }

    @Override
    public void takePlayerRank(TakePlayerRankRequest request, StreamObserver<TakePlayerRankResponse> responseObserver) {
        respondError(responseObserver, Status.UNIMPLEMENTED, "Method not implemented", null);
    }

    @Override
    public void resetPassword(PasswordResetRequest request, StreamObserver<PasswordResetResponse> responseObserver) {
        respondError(responseObserver, Status.UNIMPLEMENTED, "Method not implemented", null);
    }

    private <T> void handleWithGamer(Object identifier, StreamObserver<T> responseObserver, Function<Gamer, T> handler) {
        playerApiService.findGamer(identifier).ifPresentOrElse(
                gamer -> {
                    try {
                        val result = handler.apply(gamer);
                        respond(responseObserver, result);
                    } catch (Exception e) {
                        respondError(responseObserver, Status.INTERNAL, "Error processing request: " + e.getMessage(), e);
                    }

                },
                () -> {
                    log.error("Received gamer related request with invalid identification");

                    respondError(
                            responseObserver,
                            Status.NOT_FOUND,
                            "Gamer not found",
                            Status.NOT_FOUND.getCause()
                    );
                }
        );
    }

    private <T> void handleServiceCall(Supplier<String> supplier, Function<String, T> mapper, StreamObserver<T> responseObserver) {
        try {
            val result = supplier.get();
            respond(responseObserver, mapper.apply(result));
        } catch (StatusRuntimeException e) {
            respondError(responseObserver, e.getStatus(), e.getStatus().getDescription(), e.getCause());
        } catch (Exception e) {
            respondError(responseObserver, Status.INTERNAL, "Error processing request: " + e.getMessage(), e);
        }
    }

    private <T> void handlePunishment(Supplier<T> action, StreamObserver<T> responseObserver) {
        try {
            val response = action.get();
            respond(responseObserver, response);
        } catch (StatusRuntimeException e) {
            respondError(responseObserver, e.getStatus(), e.getStatus().getDescription(), e.getCause());
        } catch (Exception e) {
            respondError(responseObserver, Status.INTERNAL, "Error processing punishment: " + e.getMessage(), e);
        }
    }

    private <T> void handleAsyncOperation(
            Supplier<CompletableFuture<Void>> futureSupplier,
            Supplier<T> responseSupplier,
            StreamObserver<T> responseObserver) {
        try {
            futureSupplier.get().whenComplete((v, throwable) -> {
                if (throwable != null) {
                    val cause = throwable instanceof CompletionException ce ? ce.getCause() : throwable;
                    if (cause instanceof StatusRuntimeException sre) {
                        respondError(responseObserver, sre.getStatus(), sre.getStatus().getDescription(), sre.getCause());
                    } else {
                        respondError(responseObserver, Status.INTERNAL, "Error processing request: " + cause.getMessage(), cause);
                    }
                } else {
                    respond(responseObserver, responseSupplier.get());
                }
            });
        } catch (StatusRuntimeException e) {
            respondError(responseObserver, e.getStatus(), e.getStatus().getDescription(), e.getCause());
        } catch (Exception e) {
            respondError(responseObserver, Status.INTERNAL, "Error processing request: " + e.getMessage(), e);
        }
    }

    private <T> void respond(StreamObserver<T> observer, T payload) {
        observer.onNext(payload);
        observer.onCompleted();
    }

    private void respondError(StreamObserver<?> observer, Status status, String message, Throwable cause) {
        val statusBuilder = status.withDescription(message);
        observer.onError(cause != null
                ? statusBuilder.withCause(cause).asRuntimeException()
                : statusBuilder.asRuntimeException());
    }

    private void respondNotFound(StreamObserver<?> observer, String entity) {
        respondError(observer, Status.NOT_FOUND, entity + " not found", null);
    }

    private PlayerInfoResponse buildPlayerInfo(Gamer gamer) {
        val online = gamer.isOnline();
        val playerSkin = gamer.getSkin();
        val showServer = !gamer.hasPermission("api.hideserver");
        val lastLogin = gamer.getLastSession() != null
                ? TimestampConverter.toProto(gamer.getLastSession().getLeftTimestamp())
                : Timestamp.getDefaultInstance();

        return PlayerInfoResponse.newBuilder()
                .setUserId(gamer.getPlayerId())
                .setUsername(gamer.getName())
                .setDisplayName(gamer.getDisplayName())
                .setCurrentServer(online && showServer ? gamer.getCurrentServerName() : "null")
                .setIsOnline(online)
                .setLastLogin(lastLogin)
                .setLanguage(gamer.getLanguage().name())
                .setSkin(
                        PlayerSkinProto.newBuilder()
                                .setName(playerSkin.getName())
                                .setUuid(playerSkin.getUuid())
                                .setValue(playerSkin.getValue())
                                .setSignature(playerSkin.getSignature())
                                .build()
                )
                .addAllRanks(gamer.getRanks().stream().map(this::mapRankToProto).toList())
                .setGold(gamer.getGold())
                .setVanished(gamer.isVanished())
                .build();
    }

    private RankInfoProto mapRankToProto(UserRank rank) {
        return RankInfoProto.newBuilder()
                .setRankId(rank.rank().getId())
                .setRankName(rank.rank().getName())
                .setExpiryTimestamp(rank.getExpirationTime(TimeUnit.MILLISECONDS))
                .setRankType(mapRankTypeToProto(rank.type()))
                .build();
    }

    private UserRankType mapRankTypeToProto(club.abelix.commons.data.rank.type.UserRankType type) {
        return switch (type) {
            case PERMANENT -> UserRankType.USER_RANK_TYPE_PERMANENT;
            case TEMPORARY -> UserRankType.USER_RANK_TYPE_TEMPORARY;
            case UNSPECIFIED -> UserRankType.USER_RANK_TYPE_UNSPECIFIED;
        };
    }
}