package club.abelix.api.server.game.listener.waiting;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.event.chat.AsyncChatFormatEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerVanishEvent;
import club.abelix.api.base.gamer.state.GamerState;
import club.abelix.api.base.hotbar.manager.HotbarManager;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.rule.FakeTeamFlag;
import club.abelix.api.nms.team.rule.TagVisibility;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameJoinEvent;
import club.abelix.api.server.game.event.GameQuitEvent;
import club.abelix.api.server.game.event.GameSpawnLocationEvent;
import club.abelix.api.server.game.listener.GameListener;
import club.abelix.api.server.game.manager.GameManager;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.core.bukkit.health.HealthRestartRequestedEvent;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.common.enums.GameState;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

@Slf4j
public class WaitingListener extends GameListener<Game> {

    FakeTeam tagHidingTeam;

    @Autowired
    private HotbarService hotbarService;

    public WaitingListener(Game game) {
        super(game);

        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);

        this.tagHidingTeam = game.createTeam(api -> api.createTeam("Invisible", "§f", ""));
        this.tagHidingTeam.setTagVisibility(TagVisibility.NEVER);
        this.tagHidingTeam.setFlag(FakeTeamFlag.REMOVE_PLAYER_ON_QUIT);
        this.tagHidingTeam.setFriendlyFire(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatFormatEvent event) {
        checkGame(event, () -> {
            Game receiverGame = event.getReceiver().getGame();

            if (game != receiverGame) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onSpawn(GameSpawnLocationEvent event) {
        checkGame(event, () -> event.setLocation(game.getSpawnLocation()));
    }

    @EventHandler
    public void onJoin(GameJoinEvent event) {
        checkGame(event, () -> handlePlayerJoin(event.getGamer()));
    }

    @EventHandler
    public void onQuit(GameQuitEvent event) {
        checkGame(event, () -> {
            Gamer gamer = event.getGamer();

            if (!gamer.isVanished()) {
                handlePlayerQuit(gamer, event.isChange() ? 0 : 1);
            }
        });
    }

    private void handlePlayerJoin(@NonNull Gamer gamer) {
        Player player = gamer.getPlayer();

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0);
        player.setFallDistance(0);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.spigot().setCollidesWithEntities(true);

        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        FalconAPI.getNmsApi().getPlayerApi().removeArrows(player);

        hotbarService.apply(game.getDefaultHotbar(), gamer);

        int slots = game.<Integer>getSetting(GameSetting.MAX_SLOTS);
        int online = game.getNotVanishedOnline();

        game.createWaitingBoard(gamer);

        gamer.setGamerState(GamerState.PLAYER);

        if (gamer.isVanished()) {
            return;
        }

        // По идее мы кикаем ещё в primary, но а вдруг...
        if (online > slots) {
            player.kickPlayer(gamer.getLanguage().getMessage("arena_not_available_slots"));
            return;
        }

        val hideJoinMessage = game.getSetting(GameSetting.WAITING_HIDE_JOIN_MESSAGE, false);

        if (hideJoinMessage != true) {
            game.broadcastLocalized("game_player_join", receiver -> new ObjectLinkedBuf(
                    "%name%", gamer.getDisplayName(), "%online%", online, "%slots%", slots
            ));
        }

        val hideNameTags = game.getSetting(GameSetting.WAITING_HIDE_NAMETAGS, false);

        if (hideNameTags) {
            tagHidingTeam.addPlayer(player.getName());
            tagHidingTeam.addReceiver(player);
        }

        //if STARTING, do nothing
        //if WAITING, give Tier & check online
        if (game.getGameState() == GameState.STAND_BY) {
            if (online == slots) {
                game.getWaitingManager().start();
                return;
            }

            if (online >= game.getPlayersToStart()) {
                game.getWaitingManager().startTimer();
            }
        }
    }

    private void handlePlayerQuit(@NonNull Gamer gamer, int offset) {
        int online = game.getNotVanishedOnline() - offset;

        val setting = game.getSetting(GameSetting.WAITING_HIDE_QUIT_MESSAGE, false);

        if (setting != true) {
            game.broadcastLocalized("game_player_quit", receiver -> new ObjectLinkedBuf(
                    "%name%", gamer.getColoredName(), "%online%", online, "%slots%", game.getSlotsCount()
            ));
        }

        if (online < game.getPlayersToStart()) {
            game.getWaitingManager().cancelTimer();

            if (game.getGameState() == GameState.STARTING) {
                game.broadcastLocalized("waiting_not_enough_players");
                game.setGameState(GameState.STAND_BY);
            }
        }

        tagHidingTeam.removeReceiver(gamer.getPlayer());
        tagHidingTeam.removePlayer(gamer.getPlayer().getName());
    }

    @EventHandler
    public void onVanish(GamerVanishEvent event) {
        checkGame(event, () -> {
            Gamer gamer = event.getGamer();

            Player player = gamer.getPlayer();

            if (player == null) {
                return;
            }

            //сообщаем всем что игрок "покинул игру"
            if (event.isVanished()) {
                handlePlayerQuit(gamer, 0);
            } else {
                handlePlayerJoin(gamer);

                hotbarService.apply(game.getDefaultHotbar(), gamer);
            }
        });
    }

    @EventHandler
    public void onPlayerFall(PlayerMoveEvent event) {
        checkGame(event, () -> {
            if (event.getTo().getBlockY() < 0) {
                event.getPlayer().teleport(game.<Location>getSetting(GameSetting.LOBBY_LOCATION));
            }
        });
    }

    @EventHandler
    public void onRestartScheduled(HealthRestartRequestedEvent event) {
        GameManager.INSTANCE.getRegisteredGames().forEach(game -> {
            if (game.getGameState() != GameState.STAND_BY) return;

            log.info("Restart scheduled, kicking players from game {}", game.getName());
            game.getGamers().forEach(g -> g.disconnect("restart_scheduled"));
        });
    }
}
