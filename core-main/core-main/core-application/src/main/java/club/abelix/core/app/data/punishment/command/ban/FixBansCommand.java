package club.abelix.core.app.data.punishment.command.ban;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.property.PropertySqlHandler;
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

@NamedCommand("fixbans")
public class FixBansCommand {

    @Executor
    @WithPermission("command.fixbans")
    public static void execute(CoreExecutionSession session) {
        int count = 0;

        for (BanMeta meta : PunishmentManager.INSTANCE.getBanMetas()) {
            if (meta.isExpired()) {
                continue;
            }

            for (int playerId : meta.getBannedIds()) {
                PropertySqlHandler.INSTANCE.setPropertyValue(playerId, "Banned", 1);
                count++;
            }
        }

        session.sendMessage("§eFixed §c" + count + " §ebans (property changed)");
    }

}
