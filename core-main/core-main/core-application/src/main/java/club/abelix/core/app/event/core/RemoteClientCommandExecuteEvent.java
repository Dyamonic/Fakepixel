package club.abelix.core.app.event.core;

import club.abelix.core.app.api.command.CommandSender;
import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.connection.server.AbstractServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class RemoteClientCommandExecuteEvent extends Event {

    private final CommandSender sender;
    private final AbstractServer server;
    private final String command;

}
