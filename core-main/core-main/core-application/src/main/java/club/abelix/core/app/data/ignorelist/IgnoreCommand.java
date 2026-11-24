package club.abelix.core.app.data.ignorelist;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.FallbackExecutor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.util.paged.PagedIntList;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import gnu.trove.list.TIntList;

@WithHelp
@NamedCommand("ignore")
public class IgnoreCommand {

    /**
     * Максимальное количество игроков на 1 страницу игнор листа
     */
    private static final int PLAYERS_PER_PAGE = 5;

    @FallbackExecutor
    @SubcommandExecutor("add")
    @Tabcomplete("[playerName]")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @Async
    public static void add(CoreExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();
        Gamer target = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
        });

        if (gamer.isIgnored(target.getPlayerId())) {
            throw new CommandTranslationException("command_ignore_already_ignored");
        }

        if (gamer.getPlayerId() == target.getPlayerId()) {
            throw new CommandTranslationException("command_ignore_self");
        }

        gamer.addIgnoredPlayer(target.getPlayerId());

        session.sendLocalizedMessage("command_ignore_add", "%name%", target.getDisplayName());
    }

    @SubcommandExecutor("remove")
    @Tabcomplete("[playerName]")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @Async
    public static void remove(CoreExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();
        Gamer target = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
        });

        if (!gamer.isIgnored(target.getPlayerId())) {
            throw new CommandTranslationException("command_ignore_not_ignored");
        }

        if (gamer.getPlayerId() == target.getPlayerId()) {
            throw new CommandTranslationException("command_ignore_self");
        }

        gamer.removeIgnoredPlayer(target.getPlayerId());

        session.sendLocalizedMessage("command_ignore_remove", "%name%", target.getDisplayName());
    }

    @SubcommandExecutor("list")
    @WithCondition("disableConsole")
    @Async
    public static void list(CoreExecutionSession session) throws CommandException {
        Gamer gamer = session.getGamer();

        int page = session.getArgumentCount() > 0 ? session.getIntArgument(0, "page_argument") : 1;

        TIntList ignoreList = gamer.getIgnoreList();
        PagedIntList pagedIgnoreList = new PagedIntList(ignoreList, PLAYERS_PER_PAGE);

        if (!pagedIgnoreList.hasAnyPages()) {
            throw new CommandTranslationException("command_ignore_list_empty");
        }

        session.sendLocalizedMessage("command_ignore_list_title", "%page%", page, "%pages%", pagedIgnoreList.getPages());

        for (int playerId : pagedIgnoreList.getPage(page - 1).toArray()) {
            Gamer target = Gamer.getOfflineGamer(playerId, offline -> {
                offline.loadPlayerId();
                offline.loadRanks();
                offline.loadPrefix();
            });

            if (target == null) {
                continue;
            }

            session.sendLocalizedMessage("command_ignore_list_entry",
                    "%display%", target.getDisplayName(), "%name%", target.getName());
        }
    }

}
