package club.abelix.core.app.data.friend;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.FallbackExecutor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.commons.util.paged.PagedList;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.data.gamer.Gamer;
import gnu.trove.TIntCollection;

import java.util.ArrayList;
import java.util.Comparator;

@WithHelp
@WithAlias("f")
@WithAlias("friends")
@IgnoreLogs
@NamedCommand("friend")
public final class FriendCommand {

    private static final FriendManager MANAGER = FriendManager.INSTANCE;

    @WithAlias("cancel")
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithCondition("player_not_vanished")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @SubcommandExecutor("reject")
    public static void reject(CoreExecutionSession session) throws CommandException {
        Gamer gamer = session.getGamer();
        Gamer target = Gamer.getGamer(session.getArgument(0));

        if (target == null) {
            throw new CommandTranslationException("player_offline");
        }

        if (target == gamer) {
            throw new CommandTranslationException("friend_reject_self");
        }

        if (gamer.isFriend(target.getPlayerId())) {
            throw new CommandTranslationException("friend_reject_already", "%name%", session.getArgument(0));
        }

        MANAGER.rejectInvite(gamer, target);
    }

    @Async
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @SubcommandExecutor("list")
    public static void list(CoreExecutionSession session) throws CommandException {
        Gamer gamer = session.getGamer();
        TIntCollection friends = gamer.getFriends();

        if (friends.isEmpty()) {
            throw new CommandTranslationException("friend_empty_list");
        }

        StringBuilder builder = new StringBuilder();

        PagedList<Gamer> gamers = new PagedList<>(
                new ArrayList<>(friends.size()), 7
        );

        for (int playerId : friends.toArray()) {
            Gamer target = Gamer.getOfflineGamer(playerId, offline -> {
                offline.loadPlayerId();
                offline.loadRanks();
                offline.loadPrefix();

                offline.updateDisplayName();
            });

            if (target == null) {
                continue;
            }

            gamers.add(target);
        }

        gamers.sort(Comparator.comparing(Gamer::getLowerName));
        gamers.sort(Comparator.comparingInt(target -> target.isOnline() && (gamer.canSeeVanish() || !target.isVanished()) ? 0 : 1));

        int page = 1;
        int pages = gamers.getPages();

        if (session.getArgumentCount() > 0) {
            page = session.getIntArgument(0, "page");
        }

        if (page < 1 || page > pages) {
            throw new CommandTranslationException("friend_list_invalid_page");
        }

        builder.append(session.getLanguage().getMessage("friend_list_result",
                "%page%", page, "%pages%", pages));

        for (Gamer target : gamers.getPage(page - 1)) {
            boolean online = target.isOnline() && (gamer.canSeeVanish() || !target.isVanished());

            String name = target.getDisplayName();

            builder.append("\n").append(online
                    ? gamer.getLanguage().getMessage("friend_list_online_row", "%name%", name)
                    : gamer.getLanguage().getMessage("friend_list_offline_row", "%name%", name));

            if (online) {
                if (target.isVanished()) {
                    builder.append(gamer.getLanguage().getMessage("friend_list_vanished_row"));
                }

                AbstractServer server = target.getServer();

                if (server != null) {
                    builder.append(gamer.getLanguage().getMessage("friend_list_server", "%server%", server.getMode().getDisplayName()));
                }
            }
        }

        gamer.sendMessage(builder.toString());
    }

    @Async
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithCondition("player_not_vanished")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @SubcommandExecutor("remove")
    public static void remove(CoreExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        int playerId = NetworkSqlHandler.INSTANCE.getPlayerId(session.getArgument(0));

        if (!gamer.isFriend(playerId)) {
            throw new CommandTranslationException("player_not_friend");
        }

        Gamer friend = Gamer.getOfflineGamer(playerId, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
        });

        if (friend == null) {
            throw new CommandTranslationException("player_not_found");
        }

        MANAGER.removeFriend(gamer, friend);
    }

    @WithAlias("invite")
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithCondition("player_not_vanished")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @FallbackExecutor
    @SubcommandExecutor("add")
    public static void add(CoreExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        if (gamer.getFriends().size() >= gamer.getFriendLimit()) {
            throw new CommandTranslationException("friend_add_limit");
        }

        Gamer target = session.getGamer(0);

        if (target == gamer) {
            throw new CommandTranslationException("friend_add_self");
        }

        if (gamer.isFriend(target.getPlayerId())) {
            throw new CommandTranslationException("friend_add_already");
        }

        if (target.getFriends().size() >= target.getFriendLimit()) {
            throw new CommandTranslationException("friend_add_target_limit");
        }

        MANAGER.createInvite(gamer, target);
    }
}
