package club.abelix.api.server.lobby.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.network.type.mode.Master;

@WithHelp
@NamedCommand("experience")
public class ExperienceCommand {

    @SubcommandExecutor("main")
    @WithPermission("command.experience")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {

        Gamer gamer = session.getGamer(0);
        String action = session.getArgument(1);

        if (action.equals("add")) {
            gamer.addExperience(Integer.parseInt(session.getArgumentString(2)));

            session.getGamer().sendLocalizedMessage("command_experience_add_success", "%experience_added%", session.getArgumentString(1), "%experience_player%", gamer.getName());
        } else if (action.equals("set")) {
            gamer.setProperty("Experience", Integer.parseInt(session.getArgumentString(2)));

            session.getGamer().sendLocalizedMessage("command_experience_set_success", "%experience_set%", session.getArgumentString(1), "%experience_player%", gamer.getName());
        }
    }

}
