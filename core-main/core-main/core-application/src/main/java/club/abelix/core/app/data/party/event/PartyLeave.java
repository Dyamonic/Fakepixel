package club.abelix.core.app.data.party.event;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;

/**
 * Written by Vlundik for Party in 09 2016
 */
public class PartyLeave extends Event {

    private final Party party;
    private final Gamer gamer;

    public PartyLeave(Party party, Gamer gamer) {
        this.party = party;
        this.gamer = gamer;
    }

    public Party getParty() {
        return party;
    }

    public Gamer getGamer() {
        return gamer;
    }
}
