package club.abelix.api.base.party.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerEvent;
import club.abelix.api.base.party.Party;

public class PartyEvent extends GamerEvent {

    protected final Party party;

    public PartyEvent(Party party, Gamer gamer) {
        super(gamer);

        this.party = party;
    }

    public Party getParty() {
        return party;
    }
}
