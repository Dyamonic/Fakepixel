package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.val;

import java.util.function.Consumer;

@WithAlias("migrate")
@NamedCommand("transfer")
public final class TransferCommand {

    static final Consumer<Gamer> LOAD_OFFLINE_GAMER_CONSUMER = offlineGamer -> {
        offlineGamer.loadRanks();
        offlineGamer.loadPrefix();
        offlineGamer.loadFakeName();

        offlineGamer.updateDisplayName();
    };

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.transfer")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        String oldName = session.getArgument(0);
        String newName = session.getArgument(1);

        val oldGamer = session.getOfflineGamer(0, LOAD_OFFLINE_GAMER_CONSUMER);

        if (oldGamer.isOnline()) {
            oldGamer.localizedKick("transfer_player_kick");
        }

        val newGamer = session.getOfflineGamer(1, Gamer::loadPlayerName);

        if (newGamer.isOnline()) {
            newGamer.localizedKick("transfer_session_reset");
        }

        ServerManager.IMP.broadcastStaff("command_transfer_broadcast",
                "%admin%", session.getDisplayName(), "%player%", oldGamer.getDisplayName(), "%new_name%", newName);

        NetworkSqlHandler.INSTANCE.migrateAccount(oldName, newName);

        // после того как мигрировали
        // удаляем оба ника из кеша оффлайн игроков
        // чтобы во whois и т.д обновилось СРАЗУ
        Gamer.invalidateOfflinePlayer(oldGamer);
        Gamer.invalidateOfflinePlayer(newGamer);
    }
}