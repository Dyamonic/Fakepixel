package club.abelix.api.server.game.listener.game;

import club.abelix.api.base.event.chat.AsyncChatFormatEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import club.abelix.api.base.gamer.state.GamerState;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameConnectEvent;
import club.abelix.api.server.game.event.GameJoinEvent;
import club.abelix.api.server.game.event.GameQuitEvent;
import club.abelix.api.server.game.event.GameSpawnLocationEvent;
import club.abelix.api.server.game.listener.GameListener;
import club.abelix.api.server.game.listener.base.JoinResult;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.commons.util.random.RandomUtil;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.common.enums.GameState;
import lombok.NonNull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;

import java.util.List;

public class GameSpectatorListener extends GameListener<Game> {

    public GameSpectatorListener(@NonNull Game game) {
        super(game);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLogin(GameConnectEvent event) {
        checkGame(event, () -> {
            if (!game.<Boolean>getSetting(GameSetting.ALLOW_GAME_SPECTATING, true)) {
                event.setResult(JoinResult.CANT_SPECTATE);
            }
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onJoin(GameJoinEvent event) {
        checkGame(event, () -> {
            Gamer gamer = event.getGamer();
            Player player = gamer.getPlayer();

            if (gamer.isVanished()) {
                gamer.sendLocalizedMessage("game_vanished");
                gamer.sendLocalizedTitle("game_vanished_title", "game_vanished_subtitle");

                game.getSpectatorManager().addSpectator(gamer);

                return;
            }

            if (!game.getSetting(GameSetting.MAKE_PLAYER_SPECTATOR_AUTOMATICALLY, true)) {
                return;
            }

            if (game.getGameState() == GameState.GAME && !CoreUtil.isUncommon()) {
                gamer.sendLocalizedMessage("spectator_game_started");

                game.getSpectatorManager().addSpectator(gamer);

                // тп к рандомному живому игроку
                List<Gamer> alive = game.getAlivePlayers();

                //если список пуст, код ниже этого условия тпшнет его на центр карты
                if (!alive.isEmpty()) {
                    player.teleport(RandomUtil.getRandomElement(alive).getPlayer().getLocation());
                    return;
                }

                player.teleport(game.getCenter());
            }
        });
    }

    @EventHandler
    public void onSpawnLocationRequest(GameSpawnLocationEvent event) {
        checkGame(event, () -> {
            //фикс от невидимых игроков
            event.setLocation(game.getSpawnLocation());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        checkGame(event, () -> {
            Player player = event.getPlayer();
            Entity vehicle = player.getVehicle();

            if (vehicle != null) {
                player.leaveVehicle();
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPotionSplash(PotionSplashEvent event) {
        checkGame(event, () -> {
            for (LivingEntity entity : event.getAffectedEntities()) {
                if (!(entity instanceof Player)) {
                    continue;
                }

                Gamer gamer = Gamer.getGamer((Player) entity);

                if (gamer.isSpectator()) {
                    event.setIntensity(entity, 0);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatFormatEvent event) {
        if (event.isFormatModified()) {
            return;
        }

        checkGame(event, () -> {
            if (game.getGameState() != GameState.GAME) {
                return;
            }

            if (!event.getSender().isSpectator()) {
                return;
            }

            Gamer receiver = event.getReceiver();

            if (!receiver.isSpectator()) {
                event.setCancelled(true);
                return;
            }

            Game receiverGame = event.getReceiver().getGame();

            if (receiverGame != game) {
                event.setCancelled(true);
                return;
            }

            event.unsafeAppendFormat(receiver.getLanguage().getMessage("game_spectator_chat_format"));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer(event.getPlayer());

            if (gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer(event.getPlayer());

            if (gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(PlayerPickupItemEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer(event.getPlayer());

            if (gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTarget(EntityTargetEvent event) {
        checkGame(event, () -> {
            Entity entity = event.getTarget();

            if (!(entity instanceof Player)) {
                return;
            }

            Gamer gamer = Gamer.getGamer((Player) entity);

            if (gamer.isSpectator()) {
                event.setTarget(null);
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer(event.getPlayer());

            if (gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer((Player) event.getEntity());

            if (gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        checkGame(event, () -> {
            if (event.getTo().getY() < 0) {
                Gamer gamer = Gamer.getGamer(event.getPlayer());

                if (!gamer.isSpectator()) {
                    return;
                }

                event.setTo(game.getCenter());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFish(PlayerFishEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer(event.getPlayer());

            if (gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer(event.getPlayer());

            if (gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        checkGame(event, () -> {
            Gamer gamer = Gamer.getGamer(event.getPlayer());

            if (gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        checkGame(event, () -> {
            Entity clicker = event.getWhoClicked();

            if (!(clicker instanceof Player)) {
                return;
            }

            Gamer gamer = Gamer.getGamer((Player) clicker);
            ClickType clickType = event.getClick();

            if (gamer.isSpectator() && clickType == ClickType.NUMBER_KEY) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        checkGame(event, () -> {
            Entity entity = event.getEntity();

            if (!(entity instanceof Player)) {
                return;
            }

            Gamer gamer = Gamer.getGamer((Player) entity);

            if (gamer != null && gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        checkGame(event, () -> {
            Entity entity = event.getDamager();

            if (!(entity instanceof Player)) {
                return;
            }

            Gamer gamer = Gamer.getGamer((Player) entity);

            if (gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleDestroy(VehicleDamageEvent event) {
        checkGame(event, () -> {
            Entity entity = event.getAttacker();

            if (!(entity instanceof Player)) {
                return;
            }

            Gamer gamer = Gamer.getGamer((Player) entity);

            if (gamer.isSpectator()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleDestroy(VehicleEntityCollisionEvent event) {
        checkGame(event, () -> {
            Entity entity = event.getEntity();

            if (!(entity instanceof Player)) {
                return;
            }

            Gamer gamer = Gamer.getGamer((Player) entity);

            if (gamer.isSpectator()) {
                event.setCancelled(true);
                event.setPickupCancelled(true);
                event.setCollisionCancelled(true);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(GameQuitEvent event) {
        checkGame(event, () -> {
            Gamer gamer = event.getGamer();

            game.getSpectatorManager().removeSpectator(gamer);
            gamer.setGamerState(GamerState.PLAYER);

            HeartBeat.callEvent(new GamerDisplayNameUpdateEvent(gamer));
        });
    }
}
