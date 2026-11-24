package club.abelix.api.base.party.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.party.Party;

public class PartyInviteEvent extends PartyEvent {

    public PartyInviteEvent(Party party, Gamer gamer) {
        super(party, gamer);
    }

}
