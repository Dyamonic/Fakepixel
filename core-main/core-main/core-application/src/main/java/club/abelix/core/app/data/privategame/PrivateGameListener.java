package club.abelix.core.app.data.privategame;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.data.party.event.PartyLeave;
import club.abelix.core.app.data.party.manager.PartyManager;
import club.abelix.core.app.event.gamer.PlayerRedirectEvent;
import club.abelix.network.type.mode.Lobby;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PrivateGameListener implements Listener {

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPartyLeave(PartyLeave event) {
        val gamer = event.getGamer();
        val party = event.getParty();

        if (!party.isPrivateGame()) return;

        Lobby lobby = Lobby.HUB;
        if (gamer.getServer() instanceof ArenaServer arenaServer) {
            lobby = arenaServer.getMaster().getLobby();
        }

        gamer.sendTo(lobby);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void disablePrivateMode(PlayerRedirectEvent event) {
        val gamer = event.getGamer();
        val party = event.getGamer().getParty();

        if (party == null || !party.isLeader(gamer)) return;

        if (event.getTarget() instanceof ArenaServer arenaServer) {
            if (arenaServer.isPrivateGame()) {
                return;
            }
        }

        PartyManager.INSTANCE.togglePrivateStatus(party, false);
    }
}
