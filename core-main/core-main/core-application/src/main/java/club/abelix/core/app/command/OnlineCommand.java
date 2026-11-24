package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.ParsedArgs;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.network.online.data.Ids;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.query.StatusQueries;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.Locale;
import java.util.Set;

@IgnoreLogs
@NamedCommand("online")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class OnlineCommand {

    StatusQueryExecutionService queryExecutionService;

    @Executor
    @Tabcomplete("[serverName]")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        val raw = session.getArgumentString(0);
        val parsed = ParsedArgs.parse(raw);

        val showRaw = parsed.hasFlag("raw");
        val refresh = parsed.hasFlag("refresh");

        if (session.getArgumentCount() == 0) {
            val query = StatusQueries.totalStatusQuery();

            queryExecutionService.executeAsync(query).thenAccept(res -> {
                if (showRaw) {
                    session.sendMessage(res.toString());
                }

                session.sendLocalizedMessage(
                        "core_total_online_result",
                        "%online%", res.getTotalOnline()
                );
            });
            return;
        }

        val detailed = parsed.hasFlag("detailed");
        val servers = Set.copyOf(parsed.getPositional());
        val builder = StatusQueries.parametrizedQuery();

        if (!servers.isEmpty()) {
            builder.serverIds(servers.stream().map(Ids::parseId).toList());
        }

        parsed.getSingleOption("kind").ifPresent(kindName -> {
            try {
                val kind = ServerKind.valueOf(kindName.toUpperCase(Locale.ROOT));
                builder.kind(kind);
            } catch (IllegalArgumentException ignored) {
            }
        });

        val tags = parsed.getOptionValues("tag");
        if (!tags.isEmpty()) {
            builder.tags(tags);
        }

        val modes = parsed.getOptionValues("mode");
        if (!modes.isEmpty()) {
            builder.modes(modes);
        }

        val query = builder.build();

        queryExecutionService.executeAsync(query, refresh).thenAccept(res -> {
            if (showRaw) {
                session.sendMessage(res.toString());
            }

            if (detailed) {
                session.sendLocalizedMessage(
                        "core_server_online_detailed_result",
                        "%online%", res.getTotalOnline(),
                        "%servers%", String.join(", ", res.getPerServer().keySet())
                );
            } else {
                session.sendLocalizedMessage(
                        "core_server_online_result",
                        "%online%", res.getTotalOnline()
                );
            }
        });
    }
}
