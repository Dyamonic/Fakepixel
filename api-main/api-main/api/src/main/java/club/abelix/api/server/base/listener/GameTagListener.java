package club.abelix.api.server.base.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameEndEvent;
import club.abelix.api.server.game.event.GameJoinEvent;
import club.abelix.api.server.game.event.GameQuitEvent;
import club.abelix.network.common.enums.GameState;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GameTagListener extends TagListener {

    private final Game game;

    public GameTagListener(@NonNull Game game) {
        super(game.getPlugin());

        this.game = game;
    }

    private static boolean containsState(GameState state, GameState... states) {
        return ArrayUtils.contains(states, state);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEnd(GameEndEvent event) {
        if (event.getGame() == game) {
            for (Gamer gamer : game.getGamers()) {
                Player player = gamer.getPlayer();

                removeTeam(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(GameJoinEvent event) {
        if (event.getGame() == game && containsState(game.getGameState(), GameState.STAND_BY, GameState.STARTING)) {
            Gamer gamer = event.getGamer();

            sendCreateTeams(gamer.getPlayer());
            addTeam(gamer);
        }
    }

    @EventHandler
    public void onQuit(GameQuitEvent event) {
//        if (event.getGame() == game && containsState(game.getGameState(), GameState.STAND_BY, GameState.STARTING)) {
        if (event.getGame() == game) {
            Gamer gamer = event.getGamer();

            removeTeam(gamer);
            sendRemoveTeams(gamer.getPlayer());
        }
    }

    @EventHandler
    public void onGroupUpdate(GamerDisplayNameUpdateEvent event) {
        Game game = event.getGamer().getGame();

        if (game == this.game && containsState(game.getGameState(), GameState.STAND_BY, GameState.STARTING)) {
            addTeam(event.getGamer());
        }
    }

    @Override
    public Collection<Player> getPlayers() {
        List<Player> players = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Gamer gamer = Gamer.getGamer(player);

            if (gamer != null && gamer.getGame() == game) {
                players.add(player);
            }
        }

        return players;
    }

}