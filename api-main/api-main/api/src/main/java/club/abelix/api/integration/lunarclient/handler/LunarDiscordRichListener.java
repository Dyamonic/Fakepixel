package club.abelix.api.integration.lunarclient.handler;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameChangeStateEvent;
import club.abelix.api.server.game.event.GameJoinEvent;
import club.abelix.network.common.enums.GameState;
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.event.ApolloListener;
import com.lunarclient.apollo.event.EventBus;
import com.lunarclient.apollo.event.Listen;
import com.lunarclient.apollo.event.player.ApolloRegisterPlayerEvent;
import com.lunarclient.apollo.module.richpresence.RichPresenceModule;
import com.lunarclient.apollo.module.richpresence.ServerRichPresence;
import com.lunarclient.apollo.player.ApolloPlayer;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.event.EventHandler;

import java.util.Optional;

@Log4j2
public class LunarDiscordRichListener extends YListener implements ApolloListener {
    private final RichPresenceModule richPresenceModule = Apollo.getModuleManager().getModule(RichPresenceModule.class);

    public LunarDiscordRichListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
        EventBus.getBus().register(this);
    }

    private void resetServerRichPresence(ApolloPlayer apolloPlayer) {
        richPresenceModule.resetServerRichPresence(apolloPlayer);
    }

    private void buildServerRichPresence(Gamer gamer, Game game) {
        Optional<ApolloPlayer> apolloPlayerOpt = Apollo.getPlayerManager().getPlayer(gamer.getPlayer().getUniqueId());
        ApolloPlayer apolloPlayer = apolloPlayerOpt.orElse(null);
        if (apolloPlayer == null) return;
        log.info("Hook {} lunar discord RCP", gamer.getDisplayName());

        val language = gamer.getLanguage();

        val mini = game.getMini();
        val master = mini.getMaster();

        val gameStateKey = "map_cluster_state_" + game.getGameState().toString().toLowerCase();

        val serverRich = ServerRichPresence.builder()
                .gameName(master.getDisplayName())
                .gameVariantName(mini.getDisplayName())
                .gameState(language.getMessage(gameStateKey))
                .mapName(game.getGameMap().getDisplayName())
                .subServerName(game.getName());

        if (game.getGameState() != GameState.GAME) {
            val gamerStateKey = "lunarclient_discord_gamer_state_"  + game.getGameState().toString().toLowerCase();
            serverRich.playerState(language.getMessage(gamerStateKey));
        } else {
            if (gamer.isSpectator()) {
                serverRich.playerState(language.getMessage("lunarclient_discord_gamer_state_spectator"));
            } else {
                serverRich.playerState(language.getMessage("lunarclient_discord_gamer_state_in_game"));

                val currentTeam = game.getTeamManager().getTeam(gamer);
                serverRich.teamCurrentSize(currentTeam.size());
                serverRich.teamMaxSize(game.getPlayersPerTeam());
            }
        }

        this.richPresenceModule.overrideServerRichPresence(apolloPlayer, serverRich.build());
    }

    @Listen
    public void onServerJoin(ApolloRegisterPlayerEvent event) {
        resetServerRichPresence(event.getPlayer());
    }

    @EventHandler
    public void onGameJoin(GameJoinEvent event) {
        buildServerRichPresence(event.getGamer(), event.getGame());
    }

    @EventHandler
    public void onGameChangeState(GameChangeStateEvent event) {
        val game = event.getGame();

        for (Gamer gamer : game.getGamers()) {
            buildServerRichPresence(gamer, game);
        }
    }
}
