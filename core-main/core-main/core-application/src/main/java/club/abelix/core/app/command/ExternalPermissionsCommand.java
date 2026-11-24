package club.abelix.core.app.command;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.data.externalpermissions.ExternalPermissionEntry;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>ExternalPermissionsCommand creation on 15.01.2023 at 15:47</p>
 */

@WithAlias("extperm")
@NamedCommand("externalpermissions")
public class ExternalPermissionsCommand {

    @Async
    @WithAlias("update")
    @WithAlias("add")
    @SubcommandExecutor("give")
    @WithCondition("minArgs: 2")
    @WithPermission("group: ADMINISTRATOR")
    public static void give(final CoreExecutionSession session) throws CommandTranslationException {

        Gamer offlineGamer = Gamer.getOfflineGamer(session.getArgument(0), offline -> {
            offline.loadPlayerId();
            offline.loadProperties();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadExternalPermissions();
        });

        if (offlineGamer == null) {
            throw new CommandTranslationException("player_not_found");
        }

        String bukkitPermission = session.getArgumentString(1);

        boolean isLobby = true;
        boolean isMini = false;
        boolean isUncommon = false;
        boolean isSpecial = false;

        try {
            isLobby = session.getIntArgument(2, "lobby") == 1;
            isMini = session.getIntArgument(3, "mini") == 1;
            isUncommon = session.getIntArgument(4, "uncommon") == 1;
            isSpecial = session.getIntArgument(5, "special") == 1;
        } catch (Exception ignored) {
        }

        ExternalPermissionEntry permissionEntry = new ExternalPermissionEntry(-1,
                offlineGamer.getPlayerId(), bukkitPermission, isLobby, isMini,
                isUncommon, isSpecial);

        offlineGamer.addExternalPermission(permissionEntry);

        session.sendLocalizedMessage("gamer_give_external_permission",
                "%player%", offlineGamer.getName(),
                "%permission%", permissionEntry.getPermission(),
                "%lobbyUse%", permissionEntry.isCanLobby(),
                "%miniUse%", permissionEntry.isCanMini(),
                "%uncommonUse%", permissionEntry.isCanUncommon(),
                "%specialUse%", permissionEntry.isCanSpecial());
    }

    @Async
    @WithAlias("take")
    @SubcommandExecutor("remove")
    @WithCondition("minArgs: 2")
    @WithCondition("group: administrator")
    public static void take(final CoreExecutionSession session) throws CommandTranslationException {
        Gamer offlineGamer = Gamer.getOfflineGamer(session.getArgument(0), offline -> {
            offline.loadPlayerId();
            offline.loadProperties();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadExternalPermissions();
        });

        if (offlineGamer == null) {
            throw new CommandTranslationException("player_not_found");
        }

        ExternalPermissionEntry externalPermissionEntry = offlineGamer.getExternalPermissionEntry(session.getArgument(1));

        if (externalPermissionEntry == null) {
            throw new CommandTranslationException("external_permission_not_found");
        }

        offlineGamer.removeExternalPermission(externalPermissionEntry);
        session.sendLocalizedMessage("external_permission_successfully_remove");

    }

}
