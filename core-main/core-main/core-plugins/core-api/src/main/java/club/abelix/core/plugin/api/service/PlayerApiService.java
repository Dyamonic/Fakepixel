package club.abelix.core.plugin.api.service;

import club.abelix.commons.data.booster.BoosterType;
import club.abelix.commons.data.joinmessage.JoinMessageSqlHandler;
import club.abelix.commons.data.rank.UserRank;
import club.abelix.commons.data.rank.service.mapper.RanksMapper;
import club.abelix.commons.data.rank.type.UserRankType;
import club.abelix.commons.data.stat.StatsField;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsSqlHandler;
import club.abelix.commons.data.suffix.SuffixSqlHandler;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.ConsoleCommandSender;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;
import club.abelix.core.app.data.punishment.PunishmentManager;
import club.abelix.core.app.data.punishment.exception.PunishmentException;
import club.abelix.core.app.event.gamer.PlayerRankAddEvent;
import club.abelix.core.plugin.api.LocalizationUtils;
import club.abelix.network.packet.play.both.BUserRankRefreshPacket;
import club.abelix.ranks.commons.util.TimestampConverter;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import io.grpc.Status;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerApiService {

    private static final String GAMER_NOT_FOUND_MESSAGE = "Gamer not found";
    private static final Consumer<Gamer> GAMER_LOADER = gamer -> {
        gamer.load();
        gamer.updateDisplayName();
    };

    public Optional<Gamer> findGamer(Object identifier) {
        if (identifier instanceof Integer id) return Optional.ofNullable(Gamer.getOfflineGamer(id, GAMER_LOADER));
        if (!(identifier instanceof String stringIdentifier)) return Optional.empty();

        Gamer gamer = Gamer.getOfflineGamer(stringIdentifier, GAMER_LOADER);

        if (gamer == null) {
            try {
                val id = Integer.parseInt(stringIdentifier);
                return Optional.ofNullable(Gamer.getOfflineGamer(id, GAMER_LOADER));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        return Optional.of(gamer);
    }

    private Gamer requireGamer(int userId) {
        return findGamer(userId)
                .orElseThrow(() -> Status.NOT_FOUND.withDescription(GAMER_NOT_FOUND_MESSAGE).asRuntimeException());
    }

    private Gamer requireGamer(String userName) {
        return findGamer(userName)
                .orElseThrow(() -> Status.NOT_FOUND.withDescription(GAMER_NOT_FOUND_MESSAGE).asRuntimeException());
    }

    public boolean sendMessage(int userId, String message, boolean localize) {
        return findGamer(userId)
                .filter(Gamer::isOnline)
                .map(gamer -> {
                    val messageToSend = localize
                            ? LocalizationUtils.localize(message, gamer.getLanguage())
                            : message;
                    gamer.sendMessage(messageToSend);
                    return true;
                })
                .orElse(false);
    }

    public String getPlayerCurrentServer(String username) {
        return getPlayerCurrentServer(requireGamer(username).getPlayerId());
    }

    public String getPlayerCurrentServer(int userId) {
        return requireGamer(userId).getCurrentServerName();
    }

    public int getPlayerCoins(int userId) {
        return requireGamer(userId).getCoins();
    }

    public int getPlayerCoins(int userId, String mode) {
        return requireGamer(userId).getCoins(mode);
    }

    public CompletableFuture<List<Gamer>> getPlayerFriends(String username) {
        return getPlayerFriends(requireGamer(username).getPlayerId());
    }

    public CompletableFuture<List<Gamer>> getPlayerFriends(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            val gamer = requireGamer(userId);
            return Arrays.stream(gamer.getFriends().toArray())
                    .mapToObj(this::findGamer)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        });
    }

    public CompletableFuture<Map<String, String>> getPlayerStats(
            Gamer gamer,
            String modeName
    ) {
        return getLifetimeStatByMode(modeName, gamer).thenApply(statMap -> {
            if (!statMap.containsKey(0)) return Map.of();

            val stats = statMap.get(0);
            val result = new HashMap<String, String>();

            stats.forEachEntry((field, id) -> {
                result.put(field.getName(), String.valueOf(statMap.get(0).get(field)));
                return true;
            });

            return result;
        });
    }

    private CompletableFuture<TIntObjectMap<TObjectIntMap<StatsField>>> getLifetimeStatByMode(@NonNull String mode, @NonNull Gamer gamer) {
        val statsMode = StatsManager.INSTANCE.tryGetMode(mode);

        if (statsMode == null) return CompletableFuture.failedFuture(Status.INVALID_ARGUMENT.asRuntimeException());

        return StatsSqlHandler.INSTANCE.loadPlayerStats(
                statsMode,
                gamer.getPlayerId(),
                0
        );
    }

    public Optional<Party> getPlayerParty(String userName) {
        return getPlayerParty(requireGamer(userName).getPlayerId());
    }

    public Optional<Party> getPlayerParty(int userId) {
        return Optional.ofNullable(requireGamer(userId).getParty());
    }

    public void banPlayer(int userId, String reason, boolean localize, long duration) {
        executePunishment(userId, reason, localize,
                (name, localizedReason) -> PunishmentManager.INSTANCE.processBan(
                        ConsoleCommandSender.INSTANCE, name, localizedReason, duration));
    }

    public void mutePlayer(int userId, String reason, boolean localize, long duration) {
        executePunishment(userId, reason, localize,
                (name, localizedReason) -> PunishmentManager.INSTANCE.processMute(
                        ConsoleCommandSender.INSTANCE, name, localizedReason, duration));
    }

    public void kickPlayer(int userId, String reason, boolean localize) {
        executePunishment(userId, reason, localize,
                (name, localizedReason) -> PunishmentManager.INSTANCE.processKick(
                        ConsoleCommandSender.INSTANCE, name, localizedReason));
    }

    private void executePunishment(int userId, String reason, boolean localize,
                                   PunishmentAction action) {
        val gamer = requireGamer(userId);
        try {
            val localizedReason = localize
                    ? LocalizationUtils.localize(reason, gamer.getLanguage())
                    : reason;
            action.execute(gamer.getName(), localizedReason);
        } catch (PunishmentException e) {
            throw Status.INVALID_ARGUMENT.withCause(e.getCause()).asRuntimeException();
        }
    }

    public CompletableFuture<Void> givePlayerRank(int userId, int rankId, long duration) {
        return CompletableFuture.runAsync(() -> {
            val gamer = requireGamer(userId);
            val expiryTime = duration != -1 ? LocalDateTime.now().plusSeconds(duration) : null;
            val rankService = CoreApplication.getInstance().getRanksService();
            CoreApplication.getInstance()
                    .getUserRanksService()
                    .addUserRank(gamer.getPlayerId(), rankId, expiryTime)
                    .thenAccept(addUserRankResponse -> {
                        val userRankProto = addUserRankResponse.getUserRank();

                        val userRank = new UserRank(
                                userRankProto.getId(),
                                userRankProto.getUserId(),
                                rankService.findByName(RanksMapper.mapFromProto(userRankProto.getRank()).getName()).get(),
                                TimestampConverter.fromProto(userRankProto.getExpiresAt()),
                                UserRankType.fromProto(userRankProto.getRankType())
                        );

                        gamer.loadRanks();

                        CoreApplication.getInstance().getPluginManager().callEvent(
                                new PlayerRankAddEvent(gamer, userRank));


                        val packet = new BUserRankRefreshPacket(gamer.getPlayerId());
                        gamer.sendToProxy(packet);
                        gamer.sendToServer(packet);
                    }).join();
        });
    }

    public CompletableFuture<Void> givePlayerRank(String userName, int rankId, long duration) {
        return givePlayerRank(requireGamer(userName).getPlayerId(), rankId, duration);
    }

    public void giveTitle(String userName, int titleId) {
        requireGamer(userName).addSuffix(SuffixSqlHandler.INSTANCE.getSuffixById(titleId));
    }

    public void giveJoinMessage(String userName, int joinMessageId) {
        requireGamer(userName).addJoinMessage(JoinMessageSqlHandler.INSTANCE.getJoinMessageById(joinMessageId));
    }

    public void unmute(String userName) {
        executeUnpunishment(userName,
                name -> PunishmentManager.INSTANCE.processUnmute(ConsoleCommandSender.INSTANCE, name));
    }

    public void unban(String userName) {
        executeUnpunishment(userName,
                name -> PunishmentManager.INSTANCE.processUnban(ConsoleCommandSender.INSTANCE, name));
    }

    private void executeUnpunishment(String userName, UnpunishmentAction action) {
        val gamer = requireGamer(userName);
        try {
            action.execute(gamer.getName());
        } catch (PunishmentException e) {
            throw Status.INVALID_ARGUMENT.withCause(e.getCause()).asRuntimeException();
        }
    }

    public void giveBooster(String userName, String boosterType) {
        requireGamer(userName).addProperty(BoosterType.valueOf(boosterType).name(), 1);
    }

    @FunctionalInterface
    private interface PunishmentAction {
        void execute(String playerName, String reason) throws PunishmentException;
    }

    @FunctionalInterface
    private interface UnpunishmentAction {
        void execute(String playerName) throws PunishmentException;
    }
}
