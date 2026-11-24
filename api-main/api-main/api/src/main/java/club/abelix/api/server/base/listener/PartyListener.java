package club.abelix.api.server.base.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.party.Party;
import club.abelix.api.base.party.event.PartyInviteEvent;
import club.abelix.api.base.party.event.PartyKickEvent;
import club.abelix.api.base.party.manager.PartyManager;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.event.ConnectionEstablishedEvent;
import club.abelix.core.bukkit.event.ServerActiveEvent;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.packet.play.server.SPartyChange;
import club.abelix.network.packet.play.server.SPartyChangeLeader;
import club.abelix.network.packet.play.server.SPartyRemove;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class PartyListener extends YListener implements PlayServerProcessor {

    public PartyListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onActive(ServerActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    @EventHandler
    public void onEstablished(ConnectionEstablishedEvent event) {
        HeartBeat.runSync(() -> {
            for (Party party : PartyManager.getParties()) {
                party.sendMessage("party_removed_by_core");

                PartyManager.removeParty(party.getId());
            }
        });
    }

    @Override
    public void process(SPartyChange packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getMemberId());

        if (gamer == null) {
            return;
        }

        int partyId = packet.getPartyId();

        if (partyId == 0) {
            Party oldParty = PartyManager.removeMember(gamer);

            if (oldParty == null) {
                return;
            }

            HeartBeat.callEvent(new PartyKickEvent(oldParty, gamer));
            return;
        }

        //add member(leader or not) or change leader
        boolean leader = packet.isLeader();
        boolean isPrivate = packet.isPrivate();
        Party party = PartyManager.joinParty(gamer, partyId, leader, isPrivate);

        HeartBeat.callEvent(new PartyInviteEvent(party, gamer));
    }

    @Override
    public void process(SPartyRemove packet, ChannelProcessorContext ctx) {
        PartyManager.removeParty(packet.getPartyId());
    }

    @Override
    public void process(SPartyChangeLeader packet, ChannelProcessorContext ctx) {
        Gamer leader = Gamer.getGamer(packet.getMemberId());

        if (leader == null) {
            return;
        }

        PartyManager.changeLeader(leader, packet.getPartyId());
    }

    // Вызывается при выходе владельца Party на сервере
    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        Gamer gamer = Gamer.getGamer(e.getPlayer());

        PartyManager.removeMember(gamer);
    }

}
