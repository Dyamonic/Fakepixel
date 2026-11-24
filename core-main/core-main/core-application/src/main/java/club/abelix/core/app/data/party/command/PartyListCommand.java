package club.abelix.core.app.data.party.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.PrepareExecution;
import club.abelix.commons.language.Language;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author ildar (quiu)
 * <p>PartyListCommand creation on 1/27/2024 at 6:03 PM</p>
 */

@IgnoreLogs
@NamedCommand("pl")
@WithAlias("plist")
public final class PartyListCommand {

    @PrepareExecution
    public static void prepare(CoreExecutionSession session) {
        if (!session.isConsole()) {
            Party party = session.getGamer().getParty();

            session.provideObject(0, party);
        }
    }

    @Executor
    @WithCondition("withPartyOnly")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    public static void execute(CoreExecutionSession session) {
        Language language = session.getLanguage();
        Party senderParty = session.getProvidedObject(0);

        //trigger
        List<Gamer> members = new ArrayList<>(senderParty.getGamers());
        members.remove(senderParty.getLeader());
        members.addAll(senderParty.getOfflineMembers().stream().map(Party.OfflineMember::getGamer).toList());
        members.sort(Comparator.<Gamer>comparingInt(player -> senderParty.isLeader(player) ? 0 : 1).thenComparing(Gamer::getName));

        StringBuilder partyListResult = new StringBuilder(language.getMessage("party_list_result",
                "%members%", senderParty.getSize(),
                "%leader%", senderParty.getLeader().getDisplayName())).append("\n")
                .append(language.getMessage("party_list_members_result"));

        for (Gamer member : members) {
            partyListResult.append(language.getMessage("party_list_member_row",
                    "%name%", member.getDisplayName(),
                    "%status%", member.isOnline() ? "§a•" : "§c•")).append(members.indexOf(member) != members.size() - 1 ? "§f  " : "");
        }

        session.sendMessage(partyListResult.toString());
    }
}
