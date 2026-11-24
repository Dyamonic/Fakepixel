package club.abelix.api.server.lobby.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.server.lobby.mode.npc.ModeNpcService;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.execution.Executor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@NamedCommand("refreshlookups")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RefreshLobbyLookupsCommand {

    ModeNpcService modeNpcService;

    @Executor
    public void execute(BukkitExecutionSession session) {
        modeNpcService.triggerOnlineUpdate();
        session.sendMessage("Lobby lookups refresh triggered.");
    }

}
