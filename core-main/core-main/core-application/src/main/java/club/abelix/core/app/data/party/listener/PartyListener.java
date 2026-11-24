package club.abelix.core.app.data.party.listener;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;
import club.abelix.core.app.data.party.manager.PartyManager;
import club.abelix.core.app.event.gamer.GamerLoadEvent;
import club.abelix.core.app.event.gamer.PlayerGameRedirectEvent;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import club.abelix.core.app.event.gamer.PlayerVanishEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PartyListener implements Listener {

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Gamer gamer = event.getGamer();
        Party party = gamer.getRawParty();

        if (party == null) {
            for (Party invite : PartyManager.INSTANCE.getRequests(gamer)) {
                invite.getLeader().sendLocalizedMessage("party_receiver_left_the_game", "%name%", gamer.getDisplayName());
            }

            PartyManager.INSTANCE.removeRequestsTo(gamer);
        } else {
            if (!party.isValid() || party.getSize() < 2) {
                PartyManager.INSTANCE.removeParty(party);
                return;
            }

            for (Gamer member : party.getGamers()) {
                member.sendLocalizedMessage("party_member_left_the_game",
                        "%name%", gamer.getDisplayName());
            }

            party.makeOffline(gamer).runTask();
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerRejoin(GamerLoadEvent event) {
        Gamer gamer = event.getGamer();

        Party party = null;

        // ищем любую Party в которой есть этот игрок
        for (Party target : PartyManager.INSTANCE.getParties()) {
            if (target.contains(gamer)) {
                party = target;
                break;
            }
        }

        if (party == null) {
            return;
        }

        // Party есть - возвращаем игрока в эту группу и оповещаем
        // всех о том что он перезашел
        for (Gamer target : party.getGamers()) {
            target.sendLocalizedMessage("party_member_rejoin",
                    "%name%", gamer.getDisplayName());
        }

        gamer.setParty(party);
        party.rejoin(gamer);
    }

    @Subscribe
    public void onVanish(PlayerVanishEvent event) {
        Gamer gamer = event.getGamer();

        Party party = gamer.getParty();

        if (event.isVanished() && party != null) {
            if (party.isLeader(gamer)) {
                PartyManager.INSTANCE.removeParty(party);
            } else {
                PartyManager.INSTANCE.leaveParty(gamer);
            }
        }
    }

//    @EventHandler(multiplier = EventPriority.HIGHEST)
//    public void onRedirect(PlayerRedirectEvent event) {
//        Gamer gamer = event.getGamer();
//        AbstractServer target = event.getTarget();
//
//        if (target == null) {
//            return;
//        }
//
//        Party party = gamer.getParty();
//
//        if (party == null || !party.isLeader(gamer)) {
//            return;
//        }
//
//        // Не сендим в лобби и на мини сервера. (мини теперь работают через очередь)
//        if (gamer.getServer().getKind().isLobby() || gamer.getServer().getKind().isMini()) return;
//
//        int i = 0;
//        for (Gamer member : party.getGamers()) {
//            if (member == gamer || member.getServer() == target)
//                continue;
//
//            i++;
//            if (target.getSpace() - i <= 0)
//                break;
//
//            member.sendTo(target);
//        }
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerRedirect(PlayerGameRedirectEvent event) {
        Party party = event.getGamer().getParty();

        if (party != null && party.isLeader(event.getGamer())) {
            event.getAdditionalGamerList().addAll(party.getGamers());
        }
    }

}
