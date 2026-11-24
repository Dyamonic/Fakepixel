package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>MysteryDustManagerCommand creation on 16.01.2023 at 00:37</p>
 */

@WithHelp
@NamedCommand("mysterydust")
public class MysteryDustManagerCommand {
    @SubcommandExecutor("give")
    @WithAlias("+")
    @WithAlias("add")
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.mysterydust.give")
    public static void give(CoreExecutionSession session) throws CommandException {
        Gamer target = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadProperties();
        });
        int amount = session.getIntArgument(1, "amount");

        target.addMysteryDust(amount);
        session.getGamer().sendLocalizedMessage("core_command_mystery_dust_add", "%gamer%",
                target.getDisplayName(), "%amount%", amount, "%balance%", target.getMysteryDust());
    }

    @SubcommandExecutor("take")
    @WithAlias("remove")
    @WithAlias("-")
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.mysterydust.take")
    public static void take(CoreExecutionSession session) throws CommandException {
        Gamer target = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadProperties();
        });
        int amount = session.getIntArgument(1, "amount");

        target.takeMysteryDust(amount);
        session.getGamer().sendLocalizedMessage("core_command_mystery_dust_take", "%gamer%",
                target.getDisplayName(), "%amount%", amount, "%balance%", target.getMysteryDust());
    }

    @SubcommandExecutor("balance")
    @WithAlias("check")
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.mysterydust.balance")
    public static void balance(CoreExecutionSession session) throws CommandTranslationException {
        Gamer target = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadProperties();
        });

        session.getGamer().sendLocalizedMessage("core_command_mystery_dust_balance", "%gamer%",
                target.getDisplayName(), "%balance%", target.getMysteryDust());
    }

    @SubcommandExecutor("zeroing")
    @WithAlias("reset")
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.mysterydust.reset")
    public static void zeroing(CoreExecutionSession session) throws CommandException {
        Gamer target = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadProperties();
        });

        target.setProperty("Lobby/MysteryDust", 0);

        session.getGamer().sendLocalizedMessage("core_command_mystery_dust_zeroing", "%gamer%",
                target.getDisplayName(), "%balance%", target.getMysteryDust());
    }
}
