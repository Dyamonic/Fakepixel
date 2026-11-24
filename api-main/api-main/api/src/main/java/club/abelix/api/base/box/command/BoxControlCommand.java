package club.abelix.api.base.box.command;

import club.abelix.api.base.box.gui.BoxPurchaseGui;
import club.abelix.api.base.box.gui.PlayerBoxesGui;
import club.abelix.api.base.box.service.BoxesService;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.AllArgsConstructor;
import lombok.val;

@NamedCommand("box")
@WithAlias("dep")
@WithAlias("dodep")
@AllArgsConstructor
@IgnoreLogs
public class BoxControlCommand {

    BoxesService boxesService;

    @Executor
    public void openGui(final BukkitExecutionSession session) {
        new PlayerBoxesGui(session.getGamer());
    }

    @SubcommandExecutor("shop")
    public void shop(BukkitExecutionSession session) {
        new BoxPurchaseGui(session.getPlayer());
    }

    @WithPermission("command.box.give")
    @SubcommandExecutor("give")
    public void give(final BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer(0);
        int boxType = session.getIntArgument(1, "boxType");
        String mode = session.getArgument(2);
        val rarity = Rarity.valueOf(session.getArgument(3).toUpperCase());

        if (mode.equals("null")) {
            mode = null;
        }

        boxesService.giveBox(
                gamer,
                rarity,
                mode,
                boxType,
                null
        );

        session.sendMessage("Gave box of type " + boxType + " with rarity " + rarity + " to " + gamer.getName() + ".");
    }

}
