package club.abelix.core.app.data.party.command;

import club.abelix.commons.chat.ChatPublisherHolder;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.PrepareExecution;
import club.abelix.commons.command.localization.WithLocalizationKey;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;

@WithAlias("pc")
@WithAlias("зс")
@NamedCommand("pchat")
public final class PartyChatCommand {

    @PrepareExecution
    public static void prepare(CoreExecutionSession session) {
        if (!session.isConsole()) {
            Party party = session.getGamer().getParty();

            session.provideObject(0, party);
        }
    }

    @Executor
    @WithCondition("minArgs: 1")
    @WithCondition("withPartyOnly")
    @WithCondition("notBanned")
    @WithCondition("notMuted")
    @WithLocalizationKey("command_party_chat")
    public static void execute(CoreExecutionSession session) {
        Party senderParty = session.getProvidedObject(0);
        String message = ChatColor.stripColor(session.getArgumentString());

        var publisher = ChatPublisherHolder.get();
        if (publisher != null) {
            Gamer gamer = session.getGamer();
            String serverName = (gamer.getServer() != null) ? gamer.getServer().getName() : "unknown";
            publisher.publish("party", gamer.getName(), serverName, message);
        }

        for (Gamer gamer : senderParty.getGamers()) {
            gamer.sendLocalizedMessage("party_chat",
                    "%name%", session.getGamer().getDisplayName(), "%message%", message
            );
        }
    }
}
