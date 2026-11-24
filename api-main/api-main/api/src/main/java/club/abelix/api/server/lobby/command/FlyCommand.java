package club.abelix.api.server.lobby.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.parkour.LobbyParkour;
import club.abelix.api.base.parkour.ParkourState;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.language.Language;

@NamedCommand("fly")
@IgnoreLogs
public class FlyCommand {

    @Executor
    @WithPermission("lobby.fly")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();
        Language language = gamer.getLanguage();

        ParkourState state = LobbyParkour.getParkourState(gamer);
        if (state != null){
            gamer.sendLocalizedMessage("parkour_fly_usage");
            return;
        }

        boolean playerFlying = gamer.getPlayer().getAllowFlight();

        gamer.getPlayer().setAllowFlight(!playerFlying);

        gamer.sendMessage(language.getMessage("fly_command_message", "%status%", playerFlying ? language.getMessage("off_status") : language.getMessage("on_status")));
    }
}
