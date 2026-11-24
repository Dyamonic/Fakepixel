package club.abelix.api.tournament.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.tournament.TournamentClient;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.tournament.grpc.RatingMode;
import io.grpc.StatusRuntimeException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@NamedCommand("resettournamentrating")
@WithAlias("resetrating")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class TournamentResetRatingCommand {

    TournamentClient client;

    @Executor
    @WithCondition("minArgs: 2")
    @WithPermission("command.tournament.reset")
    public void execute(BukkitExecutionSession session) throws CommandTranslationException {
        val gamer = Gamer.getOrCreate(session.getArgument(0), Gamer::load);

        if (gamer == null) {
            session.sendMessage("Gamer not found!");
            return;
        }

        long playerId = gamer.getPlayerId();

        RatingResetRequest request;
        try {
            request = parseRequest(session.getArgument(1));
        } catch (IllegalArgumentException exception) {
            sendUsage(session);
            session.sendMessage(exception.getMessage());
            return;
        }

        session.sendMessage("Resetting tournament rating...");

        client.resetPlayerRating(playerId, request.mode(), request.resetAll())
                .whenComplete((response, throwable) -> HeartBeat.runSync(() -> {
                    if (throwable != null) {
                        var cause = throwable.getCause() != null ? throwable.getCause() : throwable;

                        if (cause instanceof StatusRuntimeException statusException) {
                            session.sendMessage("Failed to reset rating: " + statusException.getStatus().getDescription());
                            return;
                        }

                        session.sendMessage("Failed to reset rating: " + cause.getMessage());
                        return;
                    }

                    if (request.resetAll()) {
                        session.sendMessage("Tournament rating fully reset for playerId=" + playerId);
                        return;
                    }

                    session.sendMessage("Tournament rating reset for playerId=" + playerId + ", mode=" + request.mode().name());
                }));
    }

    private static void sendUsage(BukkitExecutionSession session) {
        session.sendMessage("Usage: /resettournamentrating <playerId> <mode|all>");
        session.sendMessage("Example: /resettournamentrating 12345 bedwars");
        session.sendMessage("Example: /resettournamentrating 12345 all");
    }

    private static RatingResetRequest parseRequest(String rawMode) {
        String normalized = rawMode.trim().toUpperCase();

        if ("ALL".equals(normalized)) {
            // The remote service still validates `mode` even when `reset_all=true`.
            return new RatingResetRequest(RatingMode.RATING_MODE_BED_WARS, true);
        }

        return new RatingResetRequest(parseMode(normalized), false);
    }

    private static RatingMode parseMode(String rawMode) {
        return switch (rawMode) {
            case "BEDWARS", "BED_WARS", "RATING_MODE_BED_WARS" -> RatingMode.RATING_MODE_BED_WARS;
            case "SKYWARS", "SKY_WARS", "RATING_MODE_SKY_WARS" -> RatingMode.RATING_MODE_SKY_WARS;
            case "DUELS", "RATING_MODE_DUELS" -> RatingMode.RATING_MODE_DUELS;
            default -> throw new IllegalArgumentException(
                    "Unknown rating mode `" + rawMode + "`. Available: bedwars, skywars, duels, all"
            );
        };
    }

    private record RatingResetRequest(RatingMode mode, boolean resetAll) {
    }
}
