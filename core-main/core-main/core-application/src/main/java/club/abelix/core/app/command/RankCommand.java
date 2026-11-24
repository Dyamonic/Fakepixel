package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.UserRank;
import club.abelix.commons.data.rank.service.RankUpdateHistoryService;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.UserRanksService;
import club.abelix.commons.data.rank.type.UserRankType;
import club.abelix.commons.language.LanguageContext;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.PlayerRankAddEvent;
import club.abelix.core.app.event.gamer.PlayerRankRemoveEvent;
import club.abelix.network.packet.play.both.BUserRankRefreshPacket;
import club.abelix.ranks.commons.util.TimestampConverter;
import club.abelix.ranks.proto.RankUpdateHistoryEntityProto;
import club.abelix.ranks.proto.UserRankAddFlags;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.ErrorInfo;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@WithAlias("group")
@NamedCommand("rank")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class RankCommand {

    UserRanksService userRanksService;
    RanksService ranksService;
    RankUpdateHistoryService rankUpdateHistoryService;

    @WithAlias("+")
    @WithAlias("add")
    @SubcommandExecutor("give")
    @WithPermission("command.rank.give")
    public void doGive(CoreExecutionSession session) throws CommandTranslationException {
        val target = session.getOfflineGamer(0, gamer -> {
            gamer.loadPlayerId();
            gamer.loadRanks();
            gamer.loadPrefix();
        });

        val rank = resolveRank(session.getArgument(1));

        ensurePermission(session, rank);

        handleGive(session, target, rank);
    }

    @WithAlias("-")
    @WithAlias("remove")
    @SubcommandExecutor("take")
    @WithPermission("command.rank.take")
    public void doTake(CoreExecutionSession session) throws CommandTranslationException {
        val target = session.getOfflineGamer(0, gamer -> {
            gamer.loadPlayerId();
            gamer.loadRanks();
            gamer.loadPrefix();
        });
        val rank = resolveRank(session.getArgument(1));

        ensurePermission(session, rank);

        handleTake(session, target, rank);
    }

    @WithAlias("h")
    @SubcommandExecutor("history")
    @WithPermission("command.rank.history")
    public void doHistory(CoreExecutionSession session) throws CommandTranslationException {
        val target = session.getOfflineGamer(0, gamer -> {
            gamer.loadPlayerId();
            gamer.loadRanks();
            gamer.loadPrefix();
        });

        handleHistory(session, target);
    }

    @WithAlias("sync")
    @WithPermission("command.rank.refresh")
    @SubcommandExecutor("refresh")
    public void doRefresh(CoreExecutionSession session) throws CommandTranslationException {
        val target = session.getOfflineGamer(0, gamer -> {
            gamer.loadPlayerId();
            gamer.loadRanks();
            gamer.loadPrefix();
        });

        userRanksService.refreshUserRanks(target.getPlayerId()).whenComplete((r, e) -> {
            if (e != null) {
                e.printStackTrace();
                session.sendLocalizedMessage("command_rank_refresh_failed",
                        "%player%", target.getDisplayName());
                return;
            }

            target.loadRanks();
            target.updateDisplayName();
            notifyServers(target);

            session.sendLocalizedMessage("command_rank_refresh_success",
                    "%player%", target.getDisplayName());
        });
    }

    @WithPermission("command.rank.reload")
    @SubcommandExecutor("reload")
    public void doRefreshAll(CoreExecutionSession session) {
        session.sendMessage(ChatColor.DARK_GRAY + "Requesting...");
        ranksService.load();
        session.sendMessage(ChatColor.GREEN + "Reloaded all ranks!");
    }

    private void handleHistory(CoreExecutionSession session, Gamer target) {
        session.sendMessage(ChatColor.DARK_GRAY + "Requesting...");
        rankUpdateHistoryService.fetchHistory(target.getPlayerId()).thenAccept(response -> {
            session.sendMessage("Total records: " + response.getHistoryCount());

            for (RankUpdateHistoryEntityProto entityProto : response.getHistoryList()) {
                val oldRank = ranksService.findRankById(entityProto.getOldRankId()).orElseThrow();
                val newRank = ranksService.findRankById(entityProto.getNewRankId()).orElseThrow();
                val lang = session.getLanguage();

                val instant = TimestampConverter.fromProto(entityProto.getActionTime()).toInstant(ZoneOffset.UTC);
                val zoned = instant.atZone(ZoneId.systemDefault());
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

                session.sendLocalizedMessage("command_rank_history_id",
                        "%id%", entityProto.getId(),
                        "%old_rank%", oldRank.getDisplayName(lang),
                        "%new_rank%", newRank.getDisplayName(lang),
                        "%action%", entityProto.getAction(),
                        "%time%", formatter.format(zoned)
                );
            }
        });
    }

    private Rank resolveRank(String name) throws CommandTranslationException {
        return ranksService.findByName(name)
                .orElseThrow(() -> new CommandTranslationException("command_rank_invalid_rank"));
    }

    private void ensurePermission(CoreExecutionSession session, Rank rank) throws CommandTranslationException {
        if (!session.getSender().hasPermission(rank.getName().toLowerCase() + ".manage")) {
            throw new CommandTranslationException("command_rank_no_permission");
        }
    }

    @SneakyThrows
    private void handleGive(CoreExecutionSession session, Gamer target, Rank rank) throws CommandTranslationException {
        Rank playerGroup = session.getRanks().findPriorityRank().rank();

        if (playerGroup.getPriority() < rank.getPriority()) {
            throw new CommandTranslationException("command_rank_invalid_group");
        }

        long timeSec = -1;
        if (session.getArgumentCount() > 2) {
            try {
                timeSec = Patterns.parseTime(session.getArgument(2), TimeUnit.SECONDS);
            } catch (IllegalArgumentException e) {
                throw new CommandTranslationException("command_rank_invalid_time");
            }
        }

        final long finalTimeSec = timeSec;

        session.sendMessage(ChatColor.DARK_GRAY + "Requesting...");

        userRanksService.addUserRank(
                target.getPlayerId(),
                rank,
                timeSec > 0 ? LocalDateTime.now().plusSeconds(timeSec) : null
        ).whenComplete((response, throwable) -> {
            extractErrorInfo(throwable).ifPresent(info -> session.sendLocalizedMessage(
                    "rank_giving_failed",
                    "%rank%", rank.getDisplayName(session.getLanguage()),
                    "%reason%", session.getLanguage().getMessage(info.getReason()),
                    "%player%", target.getDisplayName()
            ));

            target.loadRanks();
            target.updateDisplayName();
            notifyServers(target);

            val flag = response.getFlag();
            val message = flag == UserRankAddFlags.EXTENDED ? "command_rank_extended_success" : "command_rank_give_success";

            val ctx = new LanguageContext(message, language -> {
                String timeString = finalTimeSec < 0
                        ? language.getMessage("eternity")
                        : Duration.durationWordsSec(language, finalTimeSec);

                return new ObjectLinkedBuf(
                        "%admin%", session.getDisplayName(),
                        "%target%", target.getDisplayName(),
                        "%time%", timeString,
                        "%group%", rank.getDisplayName(language)
                );
            });

            ServerManager.IMP.broadcastStaff("command.staffchat", ctx);

            val userRankProto = response.getUserRank();
            val userRank = new UserRank(
                    userRankProto.getId(),
                    userRankProto.getUserId(),
                    rank,
                    TimestampConverter.fromProto(userRankProto.getExpiresAt()),
                    UserRankType.fromProto(userRankProto.getRankType())
            );
            CoreApplication.getInstance().getPluginManager().callEvent(new PlayerRankAddEvent(target, userRank));
        });
    }

    @SneakyThrows
    private void handleTake(CoreExecutionSession session, Gamer target, Rank rank) throws CommandTranslationException {
        val userRank = target.getRanks().stream()
                .filter(r -> r.rank().getId() == rank.getId())
                .findFirst()
                .orElseThrow(() -> new CommandTranslationException("command_rank_target_no_rank"));

        session.sendMessage(ChatColor.DARK_GRAY + "Requesting...");

        userRanksService.removeUserRank(target.getPlayerId(), userRank.id())
                .whenComplete((response, throwable) -> {
                    extractErrorInfo(throwable).ifPresent(info -> {
                        session.sendLocalizedMessage(
                                "rank_taking_failed",
                                "%rank%", rank.getDisplayName(session.getLanguage()),
                                "%reason%", session.getLanguage().getMessage(info.getReason()),
                                "%player%", target.getDisplayName()
                        );
                    });

                    target.loadRanks();
                    target.updateDisplayName();

                    notifyServers(target);

                    val ctx = new LanguageContext("command_rank_take_broadcast", language -> new ObjectLinkedBuf(
                            "%admin%", session.getDisplayName(),
                            "%target%", target.getDisplayName(),
                            "%group%", userRank.rank().getDisplayName(language)
                    ));

                    ServerManager.IMP.broadcastStaff("command.staffchat", ctx);
                    CoreApplication.getInstance().getPluginManager().callEvent(
                            new PlayerRankRemoveEvent(target, userRank));
                });
    }

    private Optional<ErrorInfo> extractErrorInfo(Throwable throwable) {
        if (throwable == null) return Optional.empty();

        Throwable cause = (throwable instanceof CompletionException ce) ? ce.getCause() : throwable;
        if (!(cause instanceof StatusRuntimeException sre)) return Optional.empty();

        val rpcStatus = StatusProto.fromThrowable(sre);
        if (rpcStatus == null) return Optional.empty();

        return rpcStatus.getDetailsList().stream()
                .filter(d -> d.is(ErrorInfo.class))
                .findFirst()
                .flatMap(d -> {
                    try {
                        return Optional.of(d.unpack(ErrorInfo.class));
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void notifyServers(Gamer gamer) {
        val packet = new BUserRankRefreshPacket(gamer.getPlayerId());

        gamer.sendToProxy(packet);
        gamer.sendToServer(packet);
    }

}
