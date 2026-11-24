package club.abelix.api.server.lobby.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.gift.GiftManager;
import club.abelix.api.server.lobby.gift.gui.GiftGui;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;

@WithAlias("giftrank")
@NamedCommand("gift")
public class GiftCommand {

    @Executor
    @WithCondition("minArgs: 1")
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();
        String name = session.getArgument(0);

        switch (name.toLowerCase()) {

            case "deny" -> {
                if (!GiftManager.INSTANCE.hasReceivedGift(gamer)) {
                    throw new CommandTranslationException("gift_no_pending");
                }

                GiftManager.INSTANCE.denyGift(gamer);
            }

            case "accept" -> {
                if (!GiftManager.INSTANCE.hasReceivedGift(gamer)) {
                    throw new CommandTranslationException("gift_no_pending");
                }

                GiftManager.INSTANCE.acceptGift(gamer);
            }

            default -> {
                Gamer targetGamer = session.getGamer(0);
                if (targetGamer == null) {
                    throw new CommandTranslationException("player_not_found");
                }

                if (gamer.equals(targetGamer)) {
                    throw new CommandTranslationException("gift_self");
                }

                if (GiftManager.INSTANCE.hasSentGift(gamer)) {
                    throw new CommandTranslationException("gift_already_sent");
                }

                if (GiftManager.INSTANCE.hasReceivedGift(targetGamer)) {
                    throw new CommandTranslationException("gift_already_pending");
                }

                new GiftGui(gamer, targetGamer);
            }

        }
    }

}
