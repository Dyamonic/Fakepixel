package club.abelix.core.app.data.party.event;

import club.abelix.core.app.api.event.Cancellable;
import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;

/**
 * Written by Vlundik for Party in 09 2016
 */
public class PartyInvite extends Event implements Cancellable {

    private final Party party;
    private final Gamer gamer;
    private boolean cancelled;

    public PartyInvite(Party party, Gamer gamer) {
        this.party = party;
        this.gamer = gamer;
    }

    public Party getParty() {
        return party;
    }

    public Gamer getGamer() {
        return gamer;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
