package club.abelix.api.server.game.listener.game.fun;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.task.AbstractTickingLaterTask;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.listener.GameListener;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class CheatGameListener extends GameListener<Game> {

    private final static String CHEAT_PROPERTY = "Cheat/Mode";

    public CheatGameListener(@NonNull Game game) {
        super(game);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer((Player) event.getEntity().getShooter());
            val metadata = game.getMetadata();

            if (!metadata.has(gamer.getPlayer(), CHEAT_PROPERTY)) return;
            int cheatModeValue = metadata.get(gamer.getPlayer(), CHEAT_PROPERTY);

            double strength = (double) cheatModeValue / 100;

            val task = new HomingTask(this.game, event.getEntity(), strength);
            task.setTickDelay(0, 1).start(20 * 20);
        });
    }


    // Скажем спасибо ChatGPT и мне
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    private class HomingTask extends AbstractTickingLaterTask {
        Projectile projectile;
        double homingStrength;

        HomingTask(@NonNull Game game, Projectile projectile, double homingStrength) {
            super(game);
            this.projectile = projectile;
            this.homingStrength = homingStrength;
        }

        @Override
        protected void runTick() {
            if (projectile.isDead() || projectile.isOnGround()) {
                this.cancel();
                return;
            }

            Player nearestPlayer = getNearestPlayer(projectile);
            if (nearestPlayer != null) {
                val toPlayer = nearestPlayer.getLocation().add(0, 1.5, 0).subtract(projectile.getLocation()).toVector();
                val newVelocity = projectile.getVelocity().multiply(1 - homingStrength).add(toPlayer.normalize().multiply(homingStrength));
                projectile.setVelocity(newVelocity);
            }
        }

        private Player getNearestPlayer(Projectile entity) {
            Player nearest = null;
            for (Gamer player : game.getAlivePlayers()) {
                if (player.getPlayer() == entity.getShooter()) continue;
                val target = Gamer.getGamer(player.getPlayer());
                val shooterGamer = Gamer.getGamer((Player) entity.getShooter());

                if (target.isSpectator()) continue;

                val targetTeam = game.getTeamManager().getTeam(target);
                val shooterTeam = game.getTeamManager().getTeam(shooterGamer);

                if (targetTeam.equals(shooterTeam)) continue;

                double distance = player.getPlayer().getLocation().distance(entity.getLocation());
                if (distance <= 45) {
                    nearest = player.getPlayer();
                }
            }
            return nearest;
        }
    }
}
