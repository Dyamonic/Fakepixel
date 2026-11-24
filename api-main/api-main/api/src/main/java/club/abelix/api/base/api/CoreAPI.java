package club.abelix.api.base.api;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.*;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.concurrent.CompletableFuture;

@UtilityClass
public final class CoreAPI {
    public void dispatchCommand(String server, @NonNull String bukkitCommand) {
        Connection.sendPacket(new CDispatchCommand(server, bukkitCommand));
    }

    public void createParty(Gamer leader) {
        Connection.sendPacket(new CCreateParty(leader.getPlayerId()));
    }

    public void createParty(int leaderId) {
        Connection.sendPacket(new CCreateParty(leaderId));
    }

    public void addPartyMember(Gamer leader, Gamer gamer) {
        Connection.sendPacket(new CAddPartyMember(leader.getPlayerId(), gamer.getPlayerId()));
    }

    public void addPartyMember(int leaderId, int playerId) {
        Connection.sendPacket(new CAddPartyMember(leaderId, playerId));
    }

    public void removePartyMember(Gamer leader, Gamer gamer) {
        Connection.sendPacket(new CRemovePartyMember(leader.getPlayerId(), gamer.getPlayerId()));
    }

    public void removePartyMember(int leaderId, int playerId) {
        Connection.sendPacket(new CRemovePartyMember(leaderId, playerId));
    }

    public void disbandParty(Gamer leader) {
        Connection.sendPacket(new CDisbandParty(leader.getPlayerId()));
    }

    public void disbandParty(int leaderId) {
        Connection.sendPacket(new CDisbandParty(leaderId));
    }

    public CompletableFuture<PartyMemberResponce> getPartyMembers(int partyID) {
        return Connection.awaitPacket(new CListPartyMembers(partyID))
                .thenApply((response -> new PartyMemberResponce(response.getMembers(), response.getLeaderId())));

    }
}
