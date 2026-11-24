package club.abelix.api.server.game.listener.game;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.compass.TargetSelector;
import club.abelix.api.server.game.compass.manager.TrackingCompassManager;
import club.abelix.api.server.game.listener.GameListener;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.commons.util.object.Pair;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

public class GameCompassListener extends GameListener<Game> {

    private final TrackingCompassManager trackingCompassManager;

    public GameCompassListener(Game game, TrackingCompassManager trackingCompassManager) {
        super(game);

        this.trackingCompassManager = trackingCompassManager;
    }

//    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
//    public void onClick(PlayerInteractEvent event) {
//        checkGame(event, () -> {
//            if (!event.hasItem()) {
//                return;
//            }
//
//            Player player = event.getPlayer();
//            Gamer gamer = Gamer.getGamer(player);
//
//            if (gamer.isSpectator()) {
//                return;
//            }
//
//            if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
//                    && event.getItem().getType() == Material.COMPASS) {
//
//                TargetSelector targetSelector = trackingCompassManager.getTargetSelector(player);
//                if (targetSelector == null) {
//                    return;
//                }
//
//                TargetSelector foundSelector
//                        = trackingCompassManager.findNextTargetSelector(targetSelector);
//
//                if (foundSelector != null) {
//                    trackingCompassManager.setTargetSelector(player, foundSelector);
//
//                    gamer.sendLocalizedMessage("game_tracker_mode_changed",
//                            "%mode%", foundSelector.getDisplayName(gamer.getLanguage()));
//                }
//            }
//        });
//    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        checkGame(event, () -> {
            Player player = event.getPlayer();
            Gamer gamer = Gamer.getGamer(player);

            if (player.getInventory().getItemInHand().getType() == Material.COMPASS
                    || game.getSetting(GameSetting.COMPASS_TRACK_WITHOUT_COMPASS, false)) {
                TargetSelector targetSelector = trackingCompassManager.getTargetSelector(player);

                if (targetSelector == null) {
                    return;
                }

                Optional<Pair<Player, Double>> nearestPlayer = targetSelector.select(game, gamer);

                Player target = null;

                if (nearestPlayer.isEmpty() || (target = nearestPlayer.get().getFirst()) == null) {
                    gamer.sendLocalizedActionBar("game_nearest_player_not_found");
                } else {
                    double distance = nearestPlayer.get().getSecond();

                    player.setCompassTarget(target.getLocation());

                    gamer.sendLocalizedActionBar("game_nearest_player_bar",
                            "%name%", target.getDisplayName(),
                            "%distance%", String.format("%.2f", distance));
                }
            }
        });
    }


}
