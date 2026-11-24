package club.abelix.api.nms.team.listener;

import club.abelix.api.nms.NmsConstant;
import club.abelix.api.nms.team.DefaultFakeTeamProvider;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.FakeTeamApi;
import club.abelix.api.nms.team.rule.FakeTeamFlag;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class FakeTeamListener implements Listener {

    private final FakeTeamApi fakeTeamApi;

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        if (NmsConstant.c_customTeamSending) {
            return;
        }

        Player player = event.getPlayer();

        for (FakeTeam fakeTeam : fakeTeamApi.getRegisteredTeams()) {
            if (fakeTeam.isPublicStatus()) {
                fakeTeam.addReceiver(player);
            }

            if (fakeTeam.hasFlag(FakeTeamFlag.ADD_PLAYER_ON_JOIN)) {
                fakeTeam.addPlayer(player.getName());
            }
        }

        DefaultFakeTeamProvider defaultFakeTeamProvider = fakeTeamApi.getDefaultFakeTeamProvider();

        if (defaultFakeTeamProvider != null) {
            defaultFakeTeamProvider.provideFakeTeam(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for (FakeTeam team : fakeTeamApi.getRegisteredTeams()) {
            if (team.hasReceiver(player)) {
                team.removeReceiver(player);
            }

            if (team.hasFlag(FakeTeamFlag.REMOVE_PLAYER_ON_QUIT)) {
                team.removePlayer(player.getName());
            }
        }
    }

}
