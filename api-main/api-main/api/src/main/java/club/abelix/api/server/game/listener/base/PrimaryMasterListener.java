package club.abelix.api.server.game.listener.base;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.event.chat.AsyncChatFormatEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerAsyncLoadEvent;
import club.abelix.api.base.gamer.event.GamerInterruptLoadEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.party.Party;
import club.abelix.api.base.util.scoreboard.Board;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameConnectEvent;
import club.abelix.api.server.game.event.GameJoinEvent;
import club.abelix.api.server.game.event.GameQuitEvent;
import club.abelix.api.server.game.event.GameSpawnLocationEvent;
import club.abelix.api.server.game.manager.GameManager;
import club.abelix.api.server.game.map.selector.impl.CoreMapBalancerSelector;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.event.ConnectionEstablishedEvent;
import club.abelix.core.bukkit.event.ServerActiveEvent;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.client.CMakePrivateGameResponse;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.packet.play.server.SDisableGameStatistic;
import club.abelix.network.packet.play.server.SMovePlayerToMini;
import club.abelix.network.packet.play.server.SMakeGamePrivateRequest;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

/**
 * Основной листенер для Master-сервера
 * <br>Тут обрабатывается вход и выход игрока, входящие пакеты от кора,
 * <br>смена погоды и т.д
 */
@Slf4j
public class PrimaryMasterListener extends YListener implements PlayServerProcessor {

    //======================= Class Fields ============================
    private final GameManager manager;
    //=================================================================

    //======================= Constructors ============================
    public PrimaryMasterListener(AbstractPlugin plugin, GameManager manager) {
        super(plugin);
        this.manager = manager;
    }
    //=================================================================

    @Override
    public void process(SMovePlayerToMini packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());
        if (gamer == null) return;

        Game game = GameManager.INSTANCE.getGame(packet.getMiniServerName());
        if (game == null) return;

        changeGame(gamer, game);
    }

    @Override
    public void process(SDisableGameStatistic packet, ChannelProcessorContext ctx) {
        Game game = GameManager.INSTANCE.getGame(packet.getGameName());
        if (game == null) return;

        game.coreDisableStats(packet.getPlayers());
    }

    @Override
    public void process(SMakeGamePrivateRequest packet, ChannelProcessorContext ctx) {
        log.info("[Master] Make private game request: {}", packet.getMiniServerName());
        Game game = GameManager.INSTANCE.getGame(packet.getMiniServerName());
        if (game == null) {
            ctx.setResponse(new CMakePrivateGameResponse(false, packet.getMiniServerName()));
            return;
        }
        boolean success = game.enablePrivateGameMode(packet.getPartyId());
        ctx.setResponse(new CMakePrivateGameResponse(success, packet.getMiniServerName()));
        log.info("[Master] Make private game ({}) response: {}", game.getName(), success);

    }

    //===================== Event Handlers ============================

    /**
     * При активации канала сервера
     */
    @EventHandler
    public void onActive(ServerActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    /**
     * При переподключении заново отправляем игры
     */
    @EventHandler
    public void onConnect(ConnectionEstablishedEvent event) {
        // Приходится это делать здесь, тк новая система карт
        log.info("[Master] Connection established! Register games... (Reconnect: {})",
                event.isReconnect());

        // === При первом подключении и реконнекте ===
        CoreMapBalancerSelector.INSTANCE.registerAllToCoreMaps(plugin); // Регаем мапы для кора

        // === При Реконнекте ===
        if (!event.isReconnect()) return;

        // Первичная регистрация происходит в GameManager
        for (Game game : manager.getRegisteredGames()) {
            manager.sendCreate(game);
            game.getGameQueueManager().synchronization();
        }

        // Для балансера важно знать карты которые уже запущены
        CoreMapBalancerSelector.INSTANCE.recoveryActiveMaps();
        log.info("[Master] Reconnect: all data synced success!");

        // Приватные игры, заканчиваем их тк пати удалены
        HeartBeat.runSync(() -> {
            for (Game game : manager.getRegisteredGames()) {
                if (!game.isPrivateGame()) return;

                game.getGamers().forEach(privateGamer ->
                        privateGamer.sendLocalizedMessage("private_game_end_because_party_disband"));

                if (game.getGameState() == GameState.GAME) {
                    game.disablePrivateGameMode(false);
                    try {
                        game.stopGame();
                    } catch (Exception e) {
                        log.error("[Master] Stop private game ({}) error:", game.getName());
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * PlayerSpawnLocationEvent вызывается раньше, чем
     * <br>PlayerJoinEvent, поэтому здесь нельзя удалять игрока
     * <br>из playerFutureGameMap
     */
    @EventHandler
    public void onSpawnLocation(PlayerSpawnLocationEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());
        Game game = gamer.getGame();

        if (game == null) {
            return;
        }

        GameSpawnLocationEvent spawnLocationEvent = new GameSpawnLocationEvent(game, gamer, game.getGameWorld().getSpawnLocation());
        Bukkit.getPluginManager().callEvent(spawnLocationEvent);

        event.setSpawnLocation(spawnLocationEvent.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInterrupt(GamerInterruptLoadEvent event) {
        Gamer gamer = event.getGamer();
        Game game = gamer.getGame();

        if (game != null) {
            gamer.setGame(null);
            game.removeGamer(gamer);
        }
    }

    /**
     * Когда игрок только загружается
     * <br>нужно проверить, есть ли у него игра
     * <br>если нет, то какого хуя он тут делает?
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLoad(GamerAsyncLoadEvent event) {
        Gamer gamer = event.getGamer();
        JoinResult result = processGameJoin(gamer, gamer.getServer(), true);

        if (result != JoinResult.SUCCESS) {
            event.setCancelled(true);
            event.setCancelReason(result.getMessage(gamer.getServer(), gamer.getLanguage()));
        }
    }

    public JoinResult processGameJoin(Gamer gamer, String serverName, boolean login) {
        log.info("{} ({}) / {}", gamer.getName(), gamer.getPlayerId(), serverName);
        Game game = manager.getGame(serverName);

        if (game == null) return JoinResult.NOT_FOUND;
        if (game.getGameState() == GameState.ENDING) return JoinResult.ENDING;

        if (game.getGameState() != GameState.GAME) {

            Party party = gamer.getParty();

            if (game.isPrivateGame()
                    && party != null
                    && party.getId() == game.getPrivateGamePartyId()) {

                game.autoResizePrivateGameForParty(party);
            }

            int online = game.getNotVanishedOnline();
            int slots = game.getSlotsCount();

            if (online >= slots) {
                return JoinResult.IS_FULL;
            }
        }

        GameConnectEvent event = new GameConnectEvent(game, gamer);
        HeartBeat.callEvent(event);

        if (event.getResult() == JoinResult.SUCCESS) {
            if (!login) {
                changeGame(gamer, game);
            } else {
                gamer.setGame(game);
            }
        }

        return event.getResult();
    }

    /**
     * Игрок загрузился, игра у него есть,
     * <br>на всякий случай проверяем ещё раз,
     * <br>далее мы добавляем игрока в игру
     * <br>и устанавливаем игроку игру
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Gamer gamer = Gamer.getGamer(event.getPlayer());

        if (gamer == null) {
            return;
        }

        Game game = gamer.getGame();

        if (game == null) {
            return;
        }

        if ((!gamer.isVanished()) && game.getNotVanishedGamers().size() > game.getSlotsCount()) {
            game.removeGamer(gamer);
            gamer.setGame(null);
            event.getPlayer().kickPlayer(gamer.getLanguage().getMessage("arena_not_available_slots"));
            return;
        }

        for (Game anotherGame : GameManager.INSTANCE.getRegisteredGames()) {
            if (anotherGame == game) {
                continue;
            }

            for (Gamer another : anotherGame.getGamers()) {
                Player anotherPlayer = another.getPlayer();
                Player player = gamer.getPlayer();

                anotherPlayer.hidePlayer(player);
                player.hidePlayer(anotherPlayer);
            }
        }


        game.addGamer(gamer);

        HeartBeat.callEvent(new GameJoinEvent(game, gamer));
    }

    /**
     * Игрок вышел с сервера, удаляем его из игры
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);

        Gamer gamer = Gamer.getGamer(event.getPlayer());
        Game game = gamer.getGame();

        if (game == null) {
            return;
        }

        HeartBeat.callEvent(new GameQuitEvent(game, gamer, false));

        gamer.setGame(null);
        game.removeGamer(gamer);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatFormatEvent event) {
        Game senderGame = event.getSender().getGame();
        Game receiverGame = event.getReceiver().getGame();

        if (senderGame != receiverGame) {
            event.setCancelled(true);
        }
    }

    //=================================================================
    private void changeGame(Gamer gamer, Game game) {
        Game oldGame = gamer.getGame();

        Player player = gamer.getPlayer();

        if (oldGame == game) {
            return;
        }

        gamer.setGame(game);
        game.addGamer(gamer);

        HeartBeat.runSync(() -> {
            Board oldBoard = Board.getPlayerBoard(player);

            if (oldBoard != null) {
                oldBoard.removePlayer(player);
            }

            if (oldGame != null) {
                oldGame.removeGamer(gamer);

                HeartBeat.callEvent(new GameQuitEvent(oldGame, gamer, true));

                for (Gamer another : oldGame.getGamers()) {
                    if (another == gamer) {
                        continue;
                    }

                    Player anotherPlayer = another.getPlayer();

                    player.hidePlayer(anotherPlayer);
                    anotherPlayer.hidePlayer(player);
                }
            }

            if (game.getNotVanishedGamers().size() > game.getSlotsCount()) {
                game.removeGamer(gamer);
                gamer.setGame(null);
                gamer.getPlayer().kickPlayer(gamer.getLanguage().getMessage("arena_not_available_slots"));
                return;
            }

            GameSpawnLocationEvent spawnLocationEvent = new GameSpawnLocationEvent(game, gamer,
                    game.getGameWorld().getSpawnLocation());
            HeartBeat.callEvent(spawnLocationEvent);

            player.teleport(spawnLocationEvent.getLocation());
            player.setFireTicks(0);

            for (Gamer another : game.getGamers()) {
                if (another == gamer) {
                    continue;
                }

                Player anotherPlayer = another.getPlayer();

                if (gamer.getRanks().hasPermission("vanish.see") || !another.isVanished()) {
                    player.hidePlayer(anotherPlayer);
                    player.showPlayer(anotherPlayer);
                }

                if (another.getRanks().hasPermission("vanish.see") || !gamer.isVanished()) {
                    anotherPlayer.hidePlayer(player);
                    anotherPlayer.showPlayer(player);
                }
            }

            HeartBeat.callEvent(new GameJoinEvent(game, gamer));
        });
    }
    //=================================================================

}
