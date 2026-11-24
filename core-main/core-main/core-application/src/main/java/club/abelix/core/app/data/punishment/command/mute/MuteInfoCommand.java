package club.abelix.core.app.data.punishment.command.mute;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;

@NamedCommand("muteinfo")
public final class MuteInfoCommand {

    @Dependency
    private PunishmentManager manager;

    @Async
    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("disableConsole")
    @WithPermission("command.muteinfo")
    @WithCondition("minArgs: 1")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        String targetName = session.getArgument(0);

        Gamer target = Gamer.getOfflineGamer(targetName, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
        });

        if (target == null) {
            throw new CommandTranslationException("player_not_found");
        }

        MuteMeta mute = manager.getActiveIdMute(target.getPlayerId());

        if (mute == null) {
            throw new CommandTranslationException("unmute_not_muted");
        }

        session.sendLocalizedMessage("muteinfo_result", "%target%", target.getDisplayName());
        session.sendMessage(manager.getMuteJsonText(mute, session.getLanguage(), session.getGamer().isHelper()));
    }
}
