package club.abelix.core.velocity.alpha.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.command.CommandExecutionSession;
import lombok.RequiredArgsConstructor;

@WithHelp
@NamedCommand("alpha")
@RequiredArgsConstructor
public class AlphaCommand {

    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithPermission("command.alpha.addtemp")
    @WithCondition("onlyAuthorized")
    @SubcommandExecutor("addtemp")
    public void addTemp(final CommandExecutionSession session) {
        String target = session.getArgument(0);


        if (StarshipVelocity.getInstance().getAlphaServerWhitelist() != null && StarshipVelocity.getInstance().getAlphaServerWhitelist().getEnabled()) {
            StarshipVelocity.getInstance().getAlphaServerWhitelist().addTemporarily(target);
            session.sendLocalizedMessage("alpha_add_temp_ok", "%target%", target);
        } else {
            session.sendLocalizedMessage("alpha_not_enabled");
        }
    }

    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithPermission("command.alpha.removetemp")
    @WithCondition("onlyAuthorized")
    @SubcommandExecutor("removetemp")
    public void removeTemp(final CommandExecutionSession session) {
        String target = session.getArgument(0);


        if (StarshipVelocity.getInstance().getAlphaServerWhitelist() != null && StarshipVelocity.getInstance().getAlphaServerWhitelist().getEnabled()) {
            StarshipVelocity.getInstance().getAlphaServerWhitelist().removeTemporarily(target);
            session.sendLocalizedMessage("alpha_remove_temp_ok", "%target%", target);
        } else {
            session.sendLocalizedMessage("alpha_not_enabled");
        }
    }
}
