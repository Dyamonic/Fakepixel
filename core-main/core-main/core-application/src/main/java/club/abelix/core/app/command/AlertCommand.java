package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;

@NamedCommand("alert")
public final class AlertCommand {

    @Executor
    @WithCondition("minArgs: 1")
    @WithPermission("command.alert")
    public static void execute(final CoreExecutionSession session) {
        String message = ChatColor.translateAlternateColorCodes('&', session.getArgumentString());

        // Отправялем всем онлайн игрокам
        ServerManager.IMP.broadcastChatLocalized("core_alert_message_format", "%message%", message);

//        // отправляем в консоль
//        sendAlertMessage(ConsoleCommandSender.INSTANCE, message);
    }

//    private static void sendAlertMessage(final CommandSender sender, final String message) {
//        sender.sendLocalizedMessage("core_alert_message_format",
//                "%message%", message
//        );
//    }

}
