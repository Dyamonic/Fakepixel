package club.abelix.api.server.lobby.exclusive.newyear.command;

import club.abelix.api.FalconAPI;
import club.abelix.api.server.lobby.exclusive.newyear.FireworkScheduler;
import club.abelix.api.server.lobby.manager.LobbyManager;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;

@NamedCommand("newyearshow")
@IgnoreLogs
public final class TriggerNewYearShow {

    @Executor
    @WithPermission("command.newyearshow")
    public void execute() {
        new FireworkScheduler(FalconAPI.getInstance()).startShow(LobbyManager.INSTANCE.getLobby().getSpawnLocation().clone().add(0, 25, 0));
    }

}
