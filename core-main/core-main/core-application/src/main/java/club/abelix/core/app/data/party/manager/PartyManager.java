package club.abelix.core.app.data.party.manager;

import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.data.rank.permission.GroupPermission;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.util.PermissionUtil;
import club.abelix.commons.util.collection.CollectionUtil;
import club.abelix.core.app.Async;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;
import club.abelix.core.app.data.party.event.PartyJoin;
import club.abelix.core.app.data.party.event.PartyLeave;
import club.abelix.network.packet.play.server.SPartyChange;
import club.abelix.network.packet.play.server.SPartyRemove;
import com.google.common.collect.Multimap;
import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;
import lombok.val;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PartyManager {

    private static final Pattern LIMIT_PATTERN = Pattern.compile("^party\\.limit\\.(\\d+)$");
    public static PartyManager INSTANCE = new PartyManager();
    private final TIntObjectMap<Party> idPartyMap = TCollections.synchronizedMap(new TIntObjectHashMap<>());
    /*
     * Хранит все приглашения на каждую Party
     */
    private final Multimap<Gamer, Party> inviteMap = CollectionUtil.createConcurrentSetMultimap();

    public void registerParty(@NonNull Party party) {
        idPartyMap.put(party.getId(), party);
    }

    public boolean isRemoved(@NonNull Party party) {
        return !idPartyMap.containsKey(party.getId());
    }

    public Party getParty(int idParty) {
        return idPartyMap.get(idParty);
    }

    public Collection<Party> getParties() {
        return Collections.unmodifiableCollection(idPartyMap.valueCollection());
    }

    public void addRequest(@NonNull Party party, @NonNull Gamer gamer) {
        party.invite(gamer);

        Collection<Party> parties = inviteMap.get(gamer);
        parties.add(party);
    }

    public boolean removeRequest(@NonNull Party party, @NonNull Gamer gamer) {
        party.uninvite(gamer);

        return inviteMap.remove(gamer, party);
    }

    public Collection<Party> getRequests(@NonNull Gamer gamer) {
        return inviteMap.get(gamer);
    }

    //Удалить приглашения от Party
    public void removeRequestsFrom(@NonNull Party party) {
        Collection<Gamer> invited = party.getInvites();

        for (Gamer gamer : invited) {
            Collection<Party> parties = inviteMap.get(gamer);
            parties.remove(party);
        }
    }

    //Удалить приглашения игроку
    public void removeRequestsTo(@NonNull Gamer gamer) {
        for (Party party : inviteMap.removeAll(gamer)) {
            party.uninvite(gamer);
        }
    }

    //--------------- Packets ---------------

    public void createRequest(@NonNull Party party, @NonNull Gamer leader, @NonNull Gamer gamer) throws CommandException {
        if (getRequests(gamer).contains(party)) {
            throw new CommandTranslationException("party_already_invitation_sent",
                    "%name%", gamer.getDisplayName());
        }

        if (!leader.isFriend(gamer.getPlayerId())) {
            if (gamer.isSettingSet(Setting.FORBID_PARTY_REQUESTS)) {
                throw new CommandTranslationException("party_disabled_invitation_sent",
                        "%name%", gamer.getDisplayName());
            }
        }

        if (gamer.isIgnored(leader.getPlayerId())) {
            leader.sendLocalizedMessage("ignored_player_action",
                    "%name%", gamer.getDisplayName());
            return;
        }

        addRequest(party, gamer);

        gamer.sendLocalizedMessage("party_invitation",
                "%name%", leader.getDisplayName(), "%id%", party.getId());

        for (Gamer target : party.getGamers()) {
            target.sendLocalizedMessage("party_invitation_broadcast",
                    "%name%", leader.getDisplayName(), "%target%", gamer.getDisplayName());
        }

        Async.runLater(() -> {
            if (!removeRequest(party, gamer)) {
                return;
            }

            gamer.sendLocalizedMessage("party_invitation_expired_receiver",
                    "%name%", leader.getDisplayName());
            leader.sendLocalizedMessage("party_invitation_expired_sender",
                    "%name%", gamer.getDisplayName());
        }, 1L, TimeUnit.MINUTES);
    }

    public void acceptRequest(int partyId, Gamer gamer) throws CommandException {
        Party party = idPartyMap.get(partyId);
        Collection<Party> parties = getRequests(gamer);

        if (parties.isEmpty() || party == null || !parties.contains(party)) {
            throw new CommandTranslationException("party_invitations_not_found");
        }

        boolean hasMembers = party.getGamers().stream().anyMatch(target -> !target.isHelper());

        if (hasMembers && gamer.isVanished()) {
            throw new CommandTranslationException("party_invitation_receiver_vanished");
        }

        addMember(party, gamer);
    }

    public void rejectRequest(int partyId, Gamer gamer) throws CommandException {
        Party party = idPartyMap.get(partyId);
        Collection<Party> parties = getRequests(gamer);

        if (parties.isEmpty() || party == null || !parties.contains(party)) {
            throw new CommandTranslationException("party_invitations_not_found");
        }

        removeRequest(party, gamer);

        Gamer leader = party.getLeader();

        gamer.sendLocalizedMessage("party_invitation_rejected_receiver",
                "%name%", leader.getDisplayName());

        leader.sendLocalizedMessage("party_invitation_rejected_sender",
                "%name%", gamer.getDisplayName());
    }

    public void addMember(@NonNull Party party, @NonNull Gamer gamer) {
        Party old = gamer.getRawParty();

        if (old == null) {
            removeRequestsTo(gamer);
        } else {
            if (old.isValid()) {
                removeMember(old, gamer, false);
            } else {
                removeParty(old);
            }
        }

        if (!party.isValid()) {
            party.setValid(true);

            Gamer leader = party.getLeader();
            leader.setParty(party);

            party.sendToServers(new SPartyChange(party.getId(), leader.getPlayerId(), true, party.isPrivateGame()));

            callPartyJoinEvent(party, leader);
        }

        party.addMember(gamer);
        gamer.setParty(party);

        party.sendToServers(new SPartyChange(party.getId(), gamer.getPlayerId(), false, party.isPrivateGame()));

        callPartyJoinEvent(party, gamer);

        for (Gamer member : party.getGamers()) {
            member.sendLocalizedMessage("party_join_broadcast",
                    "%name%", gamer.getDisplayName());
        }

        // если группа была публичной, и в нее
        // зашел человек, заняв последний слот
        // закрываем публичность группы
        if (party.isPublicStatus() && party.isMembersLimit()) {
            togglePublicStatus(party, party.getLeader(), 0);
        }
    }

    //Убрать игрока из тимы
    public void removeMember(@NonNull Party party, @NonNull Gamer gamer, boolean kick) {
        Gamer leader = party.getLeader();

        if (leader == gamer || party.getSize() <= 2) {
            removeParty(party);
            return;
        }

        party.sendToServers(new SPartyChange(0, gamer.getPlayerId(), false, party.isPrivateGame()));

        if (kick) {
            party.kickMember(gamer);
        } else {
            party.removeMember(gamer);
        }

        party.removeOffline(gamer);

        gamer.setParty(null);

        callPartyLeaveEvent(party, gamer);
    }

    //Удалить тиму
    public void removeParty(@NonNull Party party) {
        idPartyMap.remove(party.getId());
        removeRequestsFrom(party);

        if (party.isValid()) {
            for (Gamer member : party.getGamers()) {
                member.setParty(null);
                member.sendMessage(member.getLanguage().getMessage("party_disband_broadcast"));

                callPartyLeaveEvent(party, member);
            }

            party.sendToServers(new SPartyRemove(party.getId()));
        } else {
            Gamer leader = party.getLeader();
            removeRequestsTo(leader);
            leader.setParty(null);
        }
    }

    public void leaveParty(@NonNull Gamer gamer) {
        Party party = gamer.getParty();

        for (Gamer member : party.getGamers()) {
            if (member == gamer) {
                continue;
            }

            member.sendLocalizedMessage("party_leave_broadcast",
                    "%name%", gamer.getDisplayName());
        }

        removeMember(party, gamer, false);
    }

    public void togglePublicStatus(@NonNull Party party, @NonNull Gamer gamer, int slots) {
        boolean publicStatus = !party.isPublicStatus();

        party.setPublicStatus(publicStatus);
        party.setPublicSlots(slots);

        for (Gamer member : party.getGamers()) {
            String localizationKey = "party_public_status_"
                    + (publicStatus ? "enabled" : "disabled") + "_broadcast";

            member.sendLocalizedMessage(localizationKey,
                    "%name%", gamer.getDisplayName(),
                    "%slots%", slots,
                    "%players%", party.getSize(),
                    "%leader%", party.getLeader().getName());
        }
    }


    private void callPartyJoinEvent(Party party, Gamer gamer) {
        PartyJoin event = new PartyJoin(party, gamer);
        CoreApplication.getInstance().getPluginManager().callEvent(event);
    }

    private void callPartyLeaveEvent(Party party, Gamer gamer) {
        PartyLeave event = new PartyLeave(party, gamer);
        CoreApplication.getInstance().getPluginManager().callEvent(event);
    }

    public void togglePrivateStatus(@NonNull Party party, boolean privateStatus) {
        party.setPrivateGame(privateStatus);
        party.sendToServers(
                new SPartyChange(party.getId(), party.getLeader().getPlayerId(), true, privateStatus)
        );
    }

    public int getPartyLimit(@NonNull UserRanks ranks) {
        return PermissionUtil.extractLimit(ranks, "party");
    }
}
