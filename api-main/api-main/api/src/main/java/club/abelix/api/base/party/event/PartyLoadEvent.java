package club.abelix.api.base.party.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.party.Party;

/**
 * @author Unidentified Person
 */
public class PartyLoadEvent extends PartyEvent {

    public PartyLoadEvent(Party party, Gamer gamer) {
        super(party, gamer);
    }
}
