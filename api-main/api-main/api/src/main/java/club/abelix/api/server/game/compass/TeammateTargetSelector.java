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
public class TeammateTargetSelector implements TargetSelector {

    @Override
    public Optional<Pair<Player, Double>> select(@NonNull Game game, @NonNull Gamer gamer) {
        Player player = gamer.getPlayer();

        TeamManager teamManager = game.getTeamManager();
        GameTeam team = teamManager.getTeam(gamer);

        if (team == null) {
            return Optional.empty();
        }

        Player nearest = null;
        double nearestDistance = 0D;

        for (Player each : team.getAlivePlayers()) {
            if (each == player || each.getWorld() != player.getWorld()) {
                continue;
            }

            if (nearest == null) {
                nearest = each;
                nearestDistance = each.getLocation().distance(player.getLocation());
            } else if (nearestDistance > player.getLocation().distance(each.getLocation())) {
                nearest = each;
                nearestDistance = each.getLocation().distance(player.getLocation());
            }
        }

        return Optional.of(new Pair<>(nearest, nearestDistance));
    }

    @Override
    public String getIdentifier() {
        return "nearest_teammate";
    }

}
