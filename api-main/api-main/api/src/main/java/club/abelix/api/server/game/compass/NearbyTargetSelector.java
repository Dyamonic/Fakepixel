package club.abelix.api.server.game.compass;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.manager.TeamManager;
import club.abelix.api.server.game.team.GameTeam;
import club.abelix.commons.util.object.Pair;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Optional;

@RequiredArgsConstructor
public class NearbyTargetSelector implements TargetSelector {

    public static final NearbyTargetSelector INSTANCE = new NearbyTargetSelector();

    @Override
    public Optional<Pair<Player, Double>> select(@NonNull Game game, @NonNull Gamer gamer) {
        Player player = gamer.getPlayer();

        TeamManager teamManager = game.getTeamManager();
        GameTeam team = teamManager.getTeam(gamer);

        Player nearest = null;
        double nearestDistance = 0D;

        for (Gamer alive : game.getAlivePlayers()) {
            Player alivePlayer = alive.getPlayer();

            if (alive == gamer) {
                continue;
            }

            if (team != null && team.getAliveGamers().contains(alive)) {
                continue;
            }

            if (alivePlayer.getWorld() != player.getWorld()) {
                continue;
            }

            if (nearest == null || nearestDistance > player.getLocation().distance(alivePlayer.getLocation())) {
                nearest = alivePlayer;
                nearestDistance = nearest.getLocation().distance(player.getLocation());
            }
        }

        return Optional.of(new Pair<>(nearest, nearestDistance));
    }

    @Override
    public String getIdentifier() {
        return "nearest_player";
    }

    @Override
    public boolean canSpectatorUse() {
        return true;
    }
}
