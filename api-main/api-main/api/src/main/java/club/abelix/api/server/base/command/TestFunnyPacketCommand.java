package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CBroadcastStaff;
import org.jetbrains.annotations.NotNull;

@NamedCommand("testfunnypacket")
public class TestFunnyPacketCommand {

    @Async
    @Executor
    @WithPermission("command.test.packet")
    public void execute(final @NotNull BukkitExecutionSession es) {

        es.getSender().sendMessage(ChatColor.GRAY + "Executing test funny packet ...");

        Connection.sendPacket(new CBroadcastStaff(CBroadcastStaff.GroupWrapper.HELPER.getValue(),
                "Executed funny packet test by " + es.getDisplayName() +
                        (es.getSender().isConsole()
                                ? ""
                                : " [" + es.getGamer().getServer() + "]"))
        );

        es.getSender().sendMessage(ChatColor.GRAY + "Done executing test funny packet.");

    }

}
