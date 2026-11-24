package club.abelix.core.app.data.party.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandMessageException;
import club.abelix.commons.command.exception.CommandRelayException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.FallbackExecutor;
import club.abelix.commons.command.execution.PrepareExecution;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.command.execution.help.WithoutHelp;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.language.Language;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;
import club.abelix.core.app.data.party.manager.PartyManager;
import club.abelix.network.packet.play.server.SPartyChangeLeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@WithHelp
@WithAlias("p")
@NamedCommand("party")
public final class PartyCommand {

    private static final PartyManager MANAGER = PartyManager.INSTANCE;

    @PrepareExecution
    public static void prepare(CoreExecutionSession session) {
        if (!session.isConsole()) {
            Party party = session.getGamer().getParty();

            session.provideObject(0, party);
        }
    }

    @WithAlias("setowner")
    @WithAlias("promote")
    @WithAlias("transfer")
    @WithAlias("give")
    @IgnoreLogs
    @Tabcomplete("[playerName]")
    @WithCondition("withPartyOnly")
    @WithCondition("partyLeaderOnly")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithCondition("notBanned")
    @SubcommandExecutor("setleader")
    public static void setLeader(CoreExecutionSession session) throws CommandMessageException {
        Gamer gamer = session.getGamer();
        Gamer target = session.getGamer(0);

        Party senderParty = session.getProvidedObject(0);

        if (senderParty != target.getRawParty()) {
            throw new CommandTranslationException("party_not_in_party");
        }

        for (Gamer member : senderParty.getGamers()) {
            member.sendLocalizedMessage("party_setleader_broadcast",
                    "%name%", gamer.getDisplayName(), "%target%", target.getDisplayName());
        }

        senderParty.setLeader(target);
        senderParty.sendToServers(new SPartyChangeLeader(senderParty.getId(), target.getPlayerId()));
    }

    @IgnoreLogs
    @WithCondition("withPartyOnly")
    @WithCondition("partyLeaderOnly")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @SubcommandExecutor("warp")
    public static void warp(CoreExecutionSession session) throws CommandTranslationException {
        AbstractServer server = session.getGamer().getServer();

        if (server.isSpecial() || server.getMode().isUnknown()) {
            throw new CommandTranslationException("party_warp_special_server");
        }

        List<Gamer> playersToTeleport = new ArrayList<>();

        Party senderParty = session.getProvidedObject(0);

        for (Gamer gamer : senderParty.getGamers()) {
            if (gamer.getServer() == server) {
                continue;
            }

            playersToTeleport.add(gamer);
        }

        session.sendLocalizedMessage("party_warp");

        playersToTeleport.forEach(player -> {
            player.sendLocalizedMessage("party_member_warped");
            player.sendTo(server);
        });
    }

    @IgnoreLogs
    @Tabcomplete("[playerName]")
    @WithCondition("withPartyOnly")
    @WithCondition("partyLeaderOnly")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithCondition("notBanned")
    @SubcommandExecutor("kick")
    public static void kick(CoreExecutionSession session) throws CommandException {
        Gamer gamer = session.getGamer();
        Gamer target = session.getGamer(0);

        if (target == session.getGamer()) {
            throw new CommandTranslationException("party_kick_self");
        }

        Party senderParty = session.getProvidedObject(0);

        if (senderParty != target.getRawParty()) {
            throw new CommandTranslationException("party_not_in_party");
        }

        MANAGER.removeMember(senderParty, target, true);

        for (Gamer member : senderParty.getGamers()) {
            member.sendLocalizedMessage("party_kick_broadcast",
                    "%name%", gamer.getDisplayName(), "%target%", target.getDisplayName());
        }

        target.sendLocalizedMessage("party_kick_target");
    }

    @IgnoreLogs
    @WithCondition("withPartyOnly")
    @WithCondition("partyLeaderOnly")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @SubcommandExecutor("disband")
    public static void disband(CoreExecutionSession session) throws CommandException {
        Party senderParty = session.getProvidedObject(0);

        MANAGER.removeParty(senderParty);
    }

    @IgnoreLogs
    @WithCondition("withPartyOnly")
    @WithCondition("partyLeaderOnly")
    @WithCondition("disableConsole")
    @SubcommandExecutor("kickoffline")
    public static void kickOffline(CoreExecutionSession session) throws CommandTranslationException {
        Party senderParty = session.getProvidedObject(0);

        Collection<Party.OfflineMember> offlineMembers = senderParty.getOfflineMembers();

        if (offlineMembers.isEmpty()) {
            throw new CommandTranslationException("party_kickoffline_no_players");
        }

        for (Gamer member : senderParty.getGamers()) {
            member.sendLocalizedMessage("party_kickoffline_broadcast",
                    "%name%", session.getDisplayName(), "%count%", offlineMembers.size());
        }

        offlineMembers.forEach(member -> MANAGER.removeMember(senderParty, member.getGamer(), false));
    }

    @IgnoreLogs
    @WithCondition("withPartyOnly")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @SubcommandExecutor("leave")
    public static void leave(CoreExecutionSession session) throws CommandException {
        Gamer gamer = session.getGamer();
        Party senderParty = session.getProvidedObject(0);

        if (senderParty.isLeader(gamer)) {
            throw new CommandTranslationException("party_leave_is_leader", "%name%", gamer.getDisplayName());
        }

        session.sendLocalizedMessage("party_leave");

        MANAGER.leaveParty(gamer);
    }

    @IgnoreLogs
    @WithCondition("withPartyOnly")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @SubcommandExecutor("list")
    public static void list(CoreExecutionSession session) {
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

    @IgnoreLogs
    @WithoutHelp
    @WithCondition("withoutPartyOnly")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithCondition("notBanned")
    @SubcommandExecutor("accept")
    public static void accept(CoreExecutionSession session) throws CommandException {
        MANAGER.acceptRequest(session.getIntArgument(0, "ID"), session.getGamer());
    }

    @IgnoreLogs
    @WithoutHelp
    @WithAlias("public")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @WithCondition("partyLeaderOnly")
    @SubcommandExecutor("open")
    public static void makePublic(CoreExecutionSession session) throws CommandException {
        Gamer sender = session.getGamer();

        if (!session.getGamer().hasPermission("command.party.public")) {
            throw new CommandTranslationException("party_public_status_unavailable");
        }

        Party party = sender.getRawParty();

        if (party == null) {
            party = new Party(sender);
            sender.setParty(party);
        }

        int providerLimit = 2;
        if (session.getArgumentCount() > 0) {
            providerLimit = session.getIntArgument(0, "limit");

            if (providerLimit < 1) {
                providerLimit = 2;
            }
        }

        int limit = PartyManager.INSTANCE.getPartyLimit(party.getLeader().getRanks());

        if (providerLimit > limit || providerLimit > limit - (party.getSize() - 1)) {
            throw new CommandTranslationException("party_public_limit_exceeded");
        }


        MANAGER.togglePublicStatus(party, sender, providerLimit);
    }

    @IgnoreLogs
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithCondition("withoutPartyOnly")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @SubcommandExecutor("join")
    public static void join(CoreExecutionSession session) throws CommandException {
        Gamer sender = session.getGamer();
        Gamer target = session.getGamer(0);

        Party party = target.getParty();

        if (party == null || !party.isPublicStatus()) {
            throw new CommandTranslationException("party_not_public");
        }

        if (party.isKickedBefore(sender)) {
            throw new CommandTranslationException("party_kicked_before");
        }

        MANAGER.addMember(party, sender);
    }

    @IgnoreLogs
    @WithoutHelp
    @WithAlias("cancel")
    @WithCondition("withoutPartyOnly")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @SubcommandExecutor("reject")
    public static void reject(CoreExecutionSession session) throws CommandException {
        MANAGER.rejectRequest(session.getIntArgument(0, "ID"), session.getGamer());
    }

    @IgnoreLogs
    @WithAlias("requests")
    @WithCondition("withoutPartyOnly")
    @WithCondition("disableConsole")
    @WithCondition("notBanned")
    @SubcommandExecutor("invites")
    public static void invites(CoreExecutionSession session) throws CommandException {
        Collection<Party> parties = MANAGER.getRequests(session.getGamer());

        if (parties.isEmpty()) {
            throw new CommandTranslationException("party_invitations_not_found");
        }

        Language language = session.getLanguage();

        StringBuilder message = new StringBuilder();
        message.append(String.format("{\"text\":\"%s\",\"extra\":[", language.getMessage("party_invites_result")));

        for (Party party : parties) {
            String participants = "";

            if (party.isValid()) {
                participants = language.getMessage("party_invite_participants", "%count%", party.getGamers().size());
            }

            message.append("{\"text\":\"\\n");
            message.append(language.getMessage("party_invite_from", "%name%", party.getLeader().getDisplayName(), "%participants%", participants));
            message.append(
                    String.format(
                            "\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/party accept %s\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"%s\"}}}",
                            party.getId(), language.getMessage("party_invite_click", "%name%", party.getLeader().getDisplayName())
                    ).replace("\n", "\\n")
            );

            message.append(",");
        }
        message.deleteCharAt(message.length() - 1);
        message.append("]}");

        session.sendMessage(message.toString());
    }

    @IgnoreLogs
    @WithCondition("withPartyOnly")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithCondition("notBanned")
    @WithCondition("notMuted")
    @SubcommandExecutor("chat")
    public static void chat(CoreExecutionSession session) throws CommandException {
        throw new CommandRelayException("/pchat " + session.getArgumentString());
    }

    @IgnoreLogs
    @WithAlias("add")
    @Tabcomplete("[playerName]")
    @WithCondition("withoutPartyOrLeader")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithCondition("notBanned")
    @FallbackExecutor
    @SubcommandExecutor("invite")
    public static void invite(CoreExecutionSession session) throws CommandException {
        Gamer sender = session.getGamer();
        Gamer target = session.getGamer(0);

        if (sender.isVanished() && !target.isHelper()) {
            throw new CommandTranslationException("party_invite_staff_only");
        }

        if (target == sender) {
            throw new CommandTranslationException("party_invite_self");
        }

        Party senderParty = sender.getRawParty();
        Party playerParty = target.getRawParty();

        if (playerParty != null && playerParty.isValid()) {
            //Gamer has some party
            if (playerParty == senderParty) {
                sender.sendLocalizedMessage("party_invite_already_in_your_party");
            } else {
                sender.sendLocalizedMessage("party_invite_already_in_other_party");
            }

            return;
        }

        if (senderParty == null) {
            senderParty = new Party(sender);
            sender.setParty(senderParty);
        }

        MANAGER.createRequest(senderParty, sender, target);
    }

    @SubcommandExecutor("hijack")
    @Tabcomplete("[playerName]")
    @WithCondition("withoutPartyOnly")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithPermission("command.party.hijack")
    public static void hijack(CoreExecutionSession session) throws CommandException {
        Gamer target = session.getGamer(0);
        Party party = target.getRawParty();

        if (party == null) {
            throw new CommandTranslationException("party_not_in_party");
        }

        if(!party.getGamers().contains(session.getGamer())) {
            MANAGER.addMember(party, session.getGamer());
        }

        party.setLeader(session.getGamer());
        party.sendToServers(new SPartyChangeLeader(party.getId(), session.getGamer().getPlayerId()));
        party.getGamers().forEach(gamer -> {
            gamer.sendLocalizedMessage("party_hijack_broadcast", "%name%", session.getDisplayName());
        });
    }

    @SubcommandExecutor("forceadd")
    @Tabcomplete("[playerName]")
    @WithCondition("withoutPartyOnly")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithPermission("command.party.forceadd")
    public static void forceAdd(CoreExecutionSession session) throws CommandException {
        Gamer target = session.getGamer(0);
        Party party = target.getRawParty();

        if (party == null) {
            throw new CommandTranslationException("party_not_in_party");
        }

        if(!party.getGamers().contains(session.getGamer())) {
            MANAGER.addMember(party, session.getGamer());
        }

        party.sendToServers(new SPartyChangeLeader(party.getId(), session.getGamer().getPlayerId()));
        party.getGamers().forEach(gamer -> {
            gamer.sendLocalizedMessage("party_hijack_broadcast", "%name%", session.getDisplayName());
        });
    }

    @SubcommandExecutor("kidnap")
    @Tabcomplete("[playerName]")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithPermission("command.party.kidnap")
    public static void kidnap(CoreExecutionSession session) throws CommandException {
        Gamer target = session.getGamer(0);
        Party party = target.getParty();

        if (party != null) {
            MANAGER.leaveParty(target);
        }

        Party senderParty = session.getGamer().getParty();
        if(senderParty == null) {
            senderParty = new Party(session.getGamer());
            session.getGamer().setParty(senderParty);
        }
        MANAGER.addMember(senderParty, target);
        senderParty.getGamers().forEach(gamer -> {
            gamer.sendLocalizedMessage("party_kidnap_broadcast", "%name%", target.getDisplayName());
        });
    }
}
