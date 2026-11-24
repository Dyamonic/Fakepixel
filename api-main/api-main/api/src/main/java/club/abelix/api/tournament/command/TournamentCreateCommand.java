package club.abelix.api.tournament.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.tournament.RemoteTournamentClient;
import club.abelix.api.tournament.TournamentClient;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.tournament.grpc.RatingMode;
import io.grpc.StatusRuntimeException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;

@NamedCommand("createtournament")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class TournamentCreateCommand {

    TournamentClient client;

    @Executor
    @WithPermission("command.tournament.create")
    public void execute(BukkitExecutionSession session) {
        var title = normalizeText(session.getArgument(0));
        long startEpochSeconds = Instant.now().getEpochSecond();
        long endEpochSeconds;

        try {
            endEpochSeconds = Long.parseLong(session.getArgument(1));
        } catch (NumberFormatException exception) {
            session.sendMessage("Usage: /createtournament <title> <endEpochSeconds> <mode...>");
            session.sendMessage("Example: /createtournament mega_device_torunament 1775854800 bedwars duels");
            return;
        }

        if (endEpochSeconds <= startEpochSeconds) {
            session.sendMessage("`endEpochSeconds` must be greater than `startEpochSeconds`.");
            return;
        }

        EnumSet<RatingMode> ratingModes;
        try {
            ratingModes = parseModes(Arrays.copyOfRange(session.getArguments(), 2, session.getArguments().length));
        } catch (IllegalArgumentException exception) {
            session.sendMessage(exception.getMessage());
            return;
        }

        var startTime = Instant.ofEpochSecond(startEpochSeconds);
        var endTime = Instant.ofEpochSecond(endEpochSeconds);

        session.sendMessage("Creating tournament...");

        client.createTournament(title, title + "_description", startTime, endTime, ratingModes)
                .whenComplete((response, throwable) -> HeartBeat.runSync(() -> {
                    if (throwable != null) {
                        var cause = throwable.getCause() != null ? throwable.getCause() : throwable;

                        if (cause instanceof StatusRuntimeException statusException) {
                            session.sendMessage("Failed to create tournament: " + statusException.getStatus().getDescription());
                            return;
                        }

                        session.sendMessage("Failed to create tournament: " + cause.getMessage());
                        return;
                    }

                    if (client instanceof RemoteTournamentClient remoteTournamentClient) {
                        remoteTournamentClient.invalidateCurrentTournamentCache();
                    }

                    session.sendMessage("Tournament created: id=" + response.getTournamentId()
                            + ", title=" + title
                            + ", modes=" + ratingModes);
                }));
    }

    private static String normalizeText(String value) {
        return "null".equalsIgnoreCase(value) ? "" : value;
    }

    private static EnumSet<RatingMode> parseModes(String[] rawModes) {
        EnumSet<RatingMode> modes = EnumSet.noneOf(RatingMode.class);

        for (String rawMode : rawModes) {
            if (rawMode == null || rawMode.isBlank()) {
                continue;
            }

            for (String token : rawMode.split(",")) {
                String normalized = token.trim();

                if (normalized.isEmpty()) {
                    continue;
                }

                modes.add(parseMode(normalized));
            }
        }

        if (modes.isEmpty()) {
            throw new IllegalArgumentException("At least one rating mode is required. Available: bedwars, skywars, duels");
        }

        return modes;
    }

    private static RatingMode parseMode(String rawMode) {
        return switch (rawMode.trim().toUpperCase()) {
            case "BEDWARS", "BED_WARS", "RATING_MODE_BED_WARS" -> RatingMode.RATING_MODE_BED_WARS;
            case "SKYWARS", "SKY_WARS", "RATING_MODE_SKY_WARS" -> RatingMode.RATING_MODE_SKY_WARS;
            case "DUELS", "RATING_MODE_DUELS" -> RatingMode.RATING_MODE_DUELS;
            default -> throw new IllegalArgumentException(
                    "Unknown rating mode `" + rawMode + "`. Available: bedwars, skywars, duels"
            );
        };
    }
}
