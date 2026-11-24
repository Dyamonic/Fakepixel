package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.commons.data.skin.SkinsSqlHandler;
import club.abelix.core.app.api.command.CoreExecutionSession;
import lombok.val;

@NamedCommand("forceskin")
public final class ForceSkinCommand {

    @Executor
    @WithPermission("command.forceskin")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        val target = session.getGamer(0);
        val defaultSkin = SkinsApi.DEFAULT_SKIN;

        target.setSkin(defaultSkin);

        SkinsSqlHandler.INSTANCE.setCustomSkin(target.getPlayerId(), defaultSkin.getName());
        target.quit();
    }
}
