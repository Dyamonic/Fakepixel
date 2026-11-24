package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import org.bukkit.entity.Player;

@NamedCommand("speed")
public class SpeedCommand {

    @Executor
    @WithCondition("minArgs: 1")
    @WithPermission("command.speed")
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        float speed;

        try {
            speed = Float.parseFloat(session.getArgument(0));
        } catch (NumberFormatException exception) {
            throw new CommandTranslationException("command_speed_invalid_speed");
        }

        if (speed < 0 || speed > 10) {
            throw new CommandTranslationException("command_speed_invalid_speed");
        }

        Player player = session.getPlayer();

        float flySpeed = getRealMoveSpeed(speed, true);
        float walkSpeed = getRealMoveSpeed(speed, false);

        String mode = "all";

        if (session.getArgumentCount() > 1) {
            mode = session.getArgument(1);
        }

        switch (mode) {
            case "fly": {
                player.setFlySpeed(flySpeed);
                break;
            }
            case "walk": {
                player.setWalkSpeed(walkSpeed);
                break;
            }
            default: {
                mode = "all";

                player.setWalkSpeed(walkSpeed);
                player.setFlySpeed(flySpeed);
            }
        }

        session.sendLocalizedMessage("command_speed_set_" + mode, "%speed%", speed);
    }

    private static float getRealMoveSpeed(float userSpeed, boolean isFly) {
        float defaultSpeed = isFly ? 0.1f : 0.2f;

        if (userSpeed < 1f) {
            return defaultSpeed * userSpeed;
        } else {
            float ratio = ((userSpeed - 1) / 9) * (1f - defaultSpeed);

            return ratio + defaultSpeed;
        }
    }

}
