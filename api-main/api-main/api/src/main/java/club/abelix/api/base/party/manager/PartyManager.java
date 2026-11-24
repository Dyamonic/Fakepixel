package club.abelix.api.base.party.manager;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.party.Party;
import club.abelix.api.base.party.event.PartyKickEvent;
import club.abelix.api.base.party.event.PartyLoadEvent;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.common.NetworkPlayerLoad;
import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;

public class PartyManager {

    private static final TIntObjectMap<Party> ID_PARTY = TCollections.synchronizedMap(new TIntObjectHashMap<>());

    public static Collection<Party> getParties() {
        return Collections.unmodifiableCollection(ID_PARTY.valueCollection());
    }

    // Игрока пригласили в Party (gamer != null)
    // загрузилась Party (gamer != null)
    public static Party joinParty(@NonNull Gamer gamer, int partyId, boolean leader, boolean isPrivate) {
        Party party = ID_PARTY.get(partyId);

        if (party == null) {
            party = new Party(partyId, gamer, leader, isPrivate);
            ID_PARTY.put(partyId, party);
        } else {
            party.addMember(gamer);

            if (leader) {
                party.setLeader(gamer);
            }
        }

        gamer.setParty(party);

        return party;
    }

    public static void changeLeader(@NonNull Gamer leader, int partyId) {
        Party party = ID_PARTY.get(partyId);

        if (party == null) {
            return;
        }

        party.setLeader(leader);
    }

    public static Party removeMember(@NonNull Gamer gamer) {
        Party party = gamer.getParty();

        if (party == null) {
            return null;
        }

        party.removeMember(gamer);
        gamer.setParty(null);

        if (party.getMemberCount() == 0) {
            removeParty(party);
        }

        return party;
    }

    public static void removeParty(int id) {
        Party party = ID_PARTY.remove(id);

        if (party != null) {
            _removeParty(party);
        }
    }

    public static void removeParty(Party party) {
        ID_PARTY.remove(party.getId());

        _removeParty(party);
    }

    private static void _removeParty(Party party) {
        for (Gamer gamer : party.getMembers()) {
            gamer.setParty(null);

            HeartBeat.callEvent(new PartyKickEvent(party, gamer));
        }
    }

    public static void handlePartyLoad(@NonNull Gamer gamer, NetworkPlayerLoad.PartyHolder partyHolder) {
        if (partyHolder == null) return;

        int partyId = partyHolder.getPartyId();

        boolean isLeader = partyHolder.getLeader() == gamer.getPlayerId();
        Party party = ID_PARTY.get(partyId);

        if (party == null) {
            party = new Party(partyId, gamer, isLeader, partyHolder.isPrivate());

            ID_PARTY.put(partyId, party);
        } else {
            party.addMember(gamer);

            if (isLeader) {
                party.setLeader(gamer);
            }
        }

        gamer.setParty(party);
        HeartBeat.callEvent(new PartyLoadEvent(party, gamer));
    }

}
