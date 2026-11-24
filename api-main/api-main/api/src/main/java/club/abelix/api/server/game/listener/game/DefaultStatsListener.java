package club.abelix.api.server.game.listener.game;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameEndEvent;
import club.abelix.api.server.game.event.GameQuitEvent;
import club.abelix.api.server.game.listener.GameListener;
import club.abelix.commons.util.object.FractionAccumulator;
import club.abelix.network.common.logging.Logging;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by saharNooby on 14:09 21.11.2017
 */
public class DefaultStatsListener extends GameListener<Game> {

    private final Map<Player, FractionAccumulator> distances = new HashMap<>();

    private final Set<Gamer> participants = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public DefaultStatsListener(Game game) {
        super(game);

        participants.addAll(game.getAlivePlayers());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        checkGame(event, () -> {
            Player player = event.getPlayer();
            Gamer gamer = Gamer.getGamer(player);

            if (gamer.isSpectator()) {
                return;
            }

            Location a = event.getFrom();
            Location b = event.getTo();

            double dx = a.getX() - b.getX();
            double dz = a.getZ() - b.getZ();

            if (dx == 0 && dz == 0) {
                return;
            }

            double dist = Math.sqrt(dx * dx + dz * dz);

            FractionAccumulator acc = distances.computeIfAbsent(player, k -> new FractionAccumulator());

            int delta = acc.add(dist);

            if (delta > 0) {
                game.addStat(gamer, "blocks_traveled", delta);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(GameQuitEvent event) {
        checkGame(event, () -> this.distances.remove(event.getGamer().getPlayer()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer(event.getPlayer());

            game.addStat(gamer, "blocks_placed");
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer(event.getPlayer());

            game.addStat(gamer, "blocks_broken");
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        checkGame(event, () -> {
            if (event.getEntity() instanceof Player) {
                Gamer gamer = Gamer.getGamer((Player) event.getEntity());

                game.addStat(gamer, "bow_shots");
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        checkGame(event, () -> {
            if (!(event instanceof EntityDamageByEntityEvent ev)) {
                return;
            }

            if (ev.getDamager() instanceof Arrow arrow) {

                if (arrow.getShooter() instanceof Player) {
                    Gamer gamer = Gamer.getGamer((Player) arrow.getShooter());

                    game.addStat(gamer, "bow_hits");
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        checkGame(event, () -> {
            if (!(event.getEntity() instanceof EnderPearl)) {
                return;
            }

            ProjectileSource shooter = event.getEntity().getShooter();

            if (shooter instanceof Player) {
                Gamer gamer = Gamer.getGamer((Player) shooter);

                game.addStat(gamer, "pearl_throws");
            }
        });
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(GameEndEvent event) {
        checkGame(event, () -> {
            val game = event.getGame();
            val winner = game.getWinner();
            val masterStatsMode = game.getMasterStatsMode();
            val miniStatsMode = game.getMiniStatsMode();

            Logging.IMP.info("[Stats] Game end fetch {}", game.getName());

            // Игра закончилась в ничью, игнорим и идем дальше
            if (winner == null || game.isStatsSavingDisabled()) {
                return;
            }

            val durationSeconds = ChronoUnit.SECONDS.between(
                    game.getStartTime(), LocalDateTime.now()
            );

            if (durationSeconds <= 7) {
                Logging.IMP.warn("[Stats] Game {} is not saved because it lasted less than 7 seconds!",
                        game.getName());
                return;
            }


            for (val gamer: participants) {
                if (winner.contains(gamer)) {
                    gamer.addCoins(CoreUtil.getMaster(), 76, true, null);
                    gamer.addExperience(25, null);

                    /*
                     * Adding Overall and Mode win
                     * Adding Overall and Mode Current Winstreak
                     */
                    game.addStat(gamer, "wins");
                    game.addStat(gamer, "winstreak");

                    /*
                     * Getting current Overall, Mode Winstreak
                     * Getting best Overall, Mode Winstreak
                     */
                    int currWinStreakOverall = gamer.getLifetimeStat(masterStatsMode, "winstreak");
                    int currWinStreakMini = gamer.getLifetimeStat("winstreak");

                    int bestWinStreakOverall = gamer.getLifetimeStat(masterStatsMode, "best_winstreak");
                    int bestWinStreakMini = gamer.getLifetimeStat("best_winstreak");

                    /*
                     * Checking if Current Mode Win Streak more than best Mode Win Streak
                     * If so, set Best Mode Win Streak to Current Mode Win Streak
                     */
                    if (currWinStreakMini > bestWinStreakMini) {
                        gamer.resetStat(miniStatsMode, "best_winstreak");
                        gamer.addStat(miniStatsMode, "best_winstreak", currWinStreakMini);
                    }

                    /*
                     * Checking if Current Overall Win Streak more than best Overall Win Streak
                     * If so, set Best Overall Win Streak to Current Overall Win Streak
                     */
                    if (currWinStreakOverall > bestWinStreakOverall) {
                        gamer.resetStat(masterStatsMode, "best_winstreak");
                        gamer.addStat(masterStatsMode, "best_winstreak", currWinStreakOverall);
                    }
                } else {
                    gamer.addCoins(CoreUtil.getMaster(), 30, true, null);
                    gamer.addExperience(10, null);
                    game.addStat(gamer, "losses"); // Adding player losses to Overall and Mode.
                    game.resetStat(gamer, "winstreak"); // Reseting player Overall and Mode current WinStreak! :(
                }
            }
        });

    }
}
