package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.commons.data.skin.exception.SkinException;
import club.abelix.core.app.api.command.CoreExecutionSession;

import lombok.val;

@NamedCommand("refreshskin")
public class RefreshSkinCommand {

    @Executor
    @WithPermission("command.refreshskin")
    public void execute(CoreExecutionSession session) throws CommandTranslationException, SkinException {
        val gamer = session.getGamer(0);
        val skin = SkinsApi.getCachedSkin(gamer.getName(), true);

        if (skin == null) {
            return;
        }

        gamer.setSkin(skin);
        gamer.quit();
        session.sendLocalizedMessage("core_command_refreshskin_success", "%player_name%", gamer.getName());
    }

}
