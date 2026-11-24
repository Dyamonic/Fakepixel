package club.abelix.api.server.lobby.impl.lobby.stats.npc;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.api.server.lobby.impl.lobby.GameLobbyServer;
import club.abelix.api.server.lobby.manager.LobbyManager;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Uncommon;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Location;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatsNpc {
    final FakeEntityApi api = FalconAPI.getNmsApi().getFakeEntityApi();

    @NonNull
    final Location location;
    @NonNull
    final Gamer gamer;
    @NonNull
    final List<String> statsLine;
    final double holoYAdd;

    FakeEntityPlayer player;
    Hologram hologram;

    public void spawn() {
        player = createFakePlayer();
        hologram = createHologram();
        Logging.IMP.info("NPC spawned for gamer " + gamer.getPlayer().getName() + " at location " + location.toString());
    }

    public void remove() {
        api.removeEntity(player);
        hologram.remove();
        Logging.IMP.info("NPC removed for gamer " + gamer.getPlayer().getName());
    }

    private FakeEntityPlayer createFakePlayer() {
        val fakePlayer = api.createPlayer(location, gamer.getSkin());
        fakePlayer.setPublicStatus(false);
        fakePlayer.addReceiver(gamer.getPlayer());
        fakePlayer.setClickAction(action -> {
            BaseLobbyServer lobbyServer = LobbyManager.INSTANCE.getLobby();

            if (!(lobbyServer instanceof GameLobbyServer)) {
                gamer.sendLocalizedMessage("internal_error");
                return;
            }

            ((GameLobbyServer) lobbyServer).openDetailedStatsGui(gamer);
        });
        return fakePlayer;
    }

    private Hologram createHologram() {
        val holo = new Hologram(location.clone().add(0.0, holoYAdd + 0.2, 0.0));

        holo.addTextLine(gamer.getLanguage().getMessage("stats_npc_header",
                "%mode%", getServerModeDisplayName()));
        statsLine.forEach(holo::addTextLine);
        holo.addTextLine(gamer.getLanguage().getMessage("stats_npc_footer"));

        holo.addReceiver(gamer.getPlayer());
        return holo;
    }

    private String getServerModeDisplayName() {
        ServerMode mode = CoreUtil.getMaster();
        return mode.getDisplayName();
    }
}
