package club.abelix.core.app.data.privategame;

import club.abelix.core.app.Async;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.core.ClientActiveEvent;
import club.abelix.core.app.event.gamer.PlayerGameRedirectEvent;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.client.CGoPrivateGame;
import club.abelix.network.packet.play.client.CMakePrivateGameResponse;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SMakeGamePrivateRequest;
import club.abelix.network.type.mode.Mini;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrivateGamePacketListener implements PlayClientProcessor, Listener {

    ServerManager serverManager = ServerManager.IMP;

    private final List<Mini> SUPPORTED_MODES = List.of(
            Mini.BEDWARS_SOLO,
            Mini.BEDWARS_DOUBLES,
            Mini.BEDWARS_TRIO,
            Mini.BEDWARS_QUARDO,
            Mini.BOMB_LOBBERS,
            Mini.BLOCK_PARTY,
            Mini.TNT_TAG,
            Mini.TNT_RUN,
            Mini.SKYWARS_SOLO,
            Mini.SKYWARS_MINI,
            Mini.SKYWARS_TEAMS
    );

    private final List<Mini> RESIZABLE_PRIVATE_MODES = List.of(
            Mini.BEDWARS_SOLO,
            Mini.BEDWARS_DOUBLES,
            Mini.BEDWARS_TRIO,
            Mini.BEDWARS_QUARDO
    );

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onActive(@NonNull ClientActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    @Override
    public void process(CMakePrivateGameResponse packet, ChannelProcessorContext ctx) {
        if (packet.hasRequestId()) return;

        val server = serverManager.getServer(packet.getServerName());

        if (!(server instanceof ArenaServer)) {
            Logging.IMP.error("[PrivateGame] Failed change server {} private state to {}",
                    packet.getServerName(), packet.isSuccess());
            return;
        }

        server.withLock(() -> ((ArenaServer) server).setPrivateGame(packet.isSuccess()));
    }

    @Override
    public void process(CGoPrivateGame packet, ChannelProcessorContext ctx) {
        Async.run(() -> {
            val gamer = Gamer.getGamer(packet.getSenderId());
            val party = gamer.getParty();
            val mini = packet.getMode();

            if (!gamer.hasPermission("privategames.create")) {
                gamer.sendLocalizedMessage("private_game_create_minimal_group");
                return;
            }

            if (!SUPPORTED_MODES.contains(mini)) {
                gamer.sendLocalizedMessage("private_game_unsupported_mode",
                        "%mode%", mini.getDisplayName());
                return;
            }

            if (party == null || !party.isLeader(gamer)) {
                gamer.sendLocalizedMessage("private_game_not_party_or_leader");
                return;
            }

            gamer.sendLocalizedMessage("private_game_start_search_server",
                    "%mode%", mini.getDisplayName());

            val redirectEvent = new PlayerGameRedirectEvent(gamer);
            CoreApplication.getInstance().getPluginManager().callEvent(redirectEvent);
            Set<Gamer> redirectedGamers = redirectEvent.getAdditionalGamerList();

            val space = redirectedGamers.size();
            Logging.IMP.info("[PrivateGame] Prepare private game({}): {}", mini.getDisplayName(), space);

            val allowResize = RESIZABLE_PRIVATE_MODES.contains(mini);

            val bestServer = ServerManager.IMP.selectArenaServer(server ->
                            server.getState() == GameState.STAND_BY
                                    && server.getOnline() == 0
                                    && (allowResize || server.getSpace() >= space),
                    (best, server) -> best.getFillRate() <= server.getFillRate() ? best : server,
                    Collections.singletonList(mini)
            );

            if (bestServer == null) {
                gamer.sendLocalizedMessage("private_game_not_found_server",
                        "%mode%", mini.getDisplayName());
                return;
            }

            party.setPrivateGame(true);
            notifyParty(redirectEvent.getAdditionalGamerList(),
                    "private_game_prepare_start", bestServer, mini);

            bestServer.getConnection().awaitPacket(
                    new SMakeGamePrivateRequest(party.getId(), bestServer.getName()), 25000).whenComplete(
                    (cMakePrivateGameResponse, throwable) -> {
                        if (throwable != null) {
                            notifyParty(redirectEvent.getAdditionalGamerList(),
                                    "private_game_prepare_failed_error", bestServer, mini);
                            Logging.IMP.error("[PrivateGame] Failed prepare private game({}): {}",
                                    bestServer.getName(), throwable);
                            throwable.printStackTrace();
                            party.setPrivateGame(false);
                            return;
                        }

                        if (!cMakePrivateGameResponse.isSuccess()) {
                            notifyParty(redirectEvent.getAdditionalGamerList(),
                                    "private_game_prepare_failed", bestServer, mini);
                            party.setPrivateGame(false);
                            return;
                        }

                        notifyParty(redirectEvent.getAdditionalGamerList(),
                                "private_game_prepare_success", bestServer, mini);

                        bestServer.withLock(() -> {
                            bestServer.setPrivateGame(true);
                        });
                        redirectedGamers.forEach(pg -> pg.sendTo(bestServer));
                    }
            );
        });
    }

    private void notifyParty(Set<Gamer> gamers, String key, ArenaServer bestServer, Mini mini) {
        gamers.forEach(partyGamer -> {
            partyGamer.sendTitle(
                    partyGamer.getLanguage().getMessage(
                            key + "_title",
                            "%mode%", mini.getDisplayName(),
                            "%server%", bestServer.getName()),
                    partyGamer.getLanguage().getMessage(
                            key + "_subtitle",
                            "%mode%", mini.getDisplayName(),
                            "%server%", bestServer.getName())
            );
            partyGamer.sendLocalizedMessage(key + "_message",
                    "%mode%", mini.getDisplayName(),
                    "%server%", bestServer.getName()
            );

        });
    }
}