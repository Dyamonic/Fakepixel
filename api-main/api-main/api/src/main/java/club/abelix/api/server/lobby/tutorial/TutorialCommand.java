package club.abelix.api.server.lobby.tutorial;

import club.abelix.api.base.camera.PlayerCameraManager;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import lombok.NonNull;
import org.bukkit.entity.Player;

@NamedCommand("tutorial")
@IgnoreLogs
public class TutorialCommand {

    private final LobbyTutorial tutorial;

    public TutorialCommand(@NonNull LobbyTutorial tutorial) {
        this.tutorial = tutorial;
    }

    @Executor
    public void execute(BukkitExecutionSession session) throws CommandTranslationException {
        Player player = session.getPlayer();

        if (PlayerCameraManager.IMP.hasCamera(player)) {
            throw new CommandTranslationException("tutorial_already_started");
        }

        tutorial.startTutorial(player);
    }

}
