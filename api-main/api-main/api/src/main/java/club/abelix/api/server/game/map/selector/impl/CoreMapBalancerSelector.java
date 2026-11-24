package club.abelix.api.server.game.map.selector.impl;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.manager.GameManager;
import club.abelix.commons.util.random.RandomUtil;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.play.client.CMapBalancerRecovery;
import club.abelix.network.packet.play.client.CMapBalancerRegister;
import club.abelix.network.packet.play.client.CMapBalancerUpdateMap;
import club.abelix.network.type.map.GameMap;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;
import lombok.val;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>CoreMapSelector creation on 26.12.2023 at 22:21</p>
 */

public class CoreMapBalancerSelector extends SlimeMapSelector {

    // ====================================================================
    public static final CoreMapBalancerSelector INSTANCE = new CoreMapBalancerSelector();
    // ====================================================================

    private final ConcurrentHashMap<Mini, List<GameMap>> mapCache = new ConcurrentHashMap<>();

    private GameMap selectRandomMap(@NonNull Mini mini, AbstractPlugin plugin) {
        List<GameMap> mapsByMini = getAvailableMapList(mini, plugin);
        val randomMap = RandomUtil.getRandomElement(mapsByMini);

        Logging.IMP.info("[CoreMapBalancer] ({}) Select map random: {}, TN: {}",
                mini.getName(),
                randomMap.getDisplayName(), randomMap.getTechnicalName());
        return RandomUtil.getRandomElement(mapsByMini);
    }


    @Override
    public GameMap selectMap(@NonNull Game game) {
        Mini mini = game.getMini();

        if (mini == null) {
            throw new RuntimeException("CoreMapBalancerSelector unsupported games without a specific mini");
        }

        // В случае если соединение с кором почему-то утеряно
        if (!Connection.isActive() || !mapCache.containsKey(mini)) {

            Logging.IMP.warn("[CoreMapBalancer] " +
                            "The connection to CORE is broken or maps not registered," +
                            " so the card for the game {} was randomly selected",
                    game.getName());

            return selectRandomMap(mini, game.getPlugin());
        }

        Logging.IMP.info("[CoreMapBalancer] Send update packet name {}, map {}",
                game.getName(), game.getGameMap());

        return Connection.awaitPacket(new CMapBalancerUpdateMap(mini,
                        game.getGameMap()))
                .exceptionally(throwable -> {
                    Logging.IMP.error("[CoreMapBalancer] ({}) Network error (arena: {}): {}",
                            mini.getName(),
                            game.getName(), throwable);
                    return null;
                })
                .thenApply(response -> {
                    if (response == null) {
                        Logging.IMP.error("[CoreMapBalancer] Failed balance map (arena: {})",
                                game.getName());
                        return selectRandomMap(mini, game.getPlugin());
                    }

                    GameMap coreMap = response.getNewMap();
                    Logging.IMP.info("[CoreMapBalancer] Select map(CORE): {}, TN: {}",
                            coreMap.getDisplayName(), coreMap.getTechnicalName());

                    return coreMap;
                }).join(); // todo: Переписать на асинхрон
    }

    @Override
    public void unloadAndDeleteWorld(@NonNull Game game) {
        super.unloadAndDeleteWorld(game);

        if (Connection.isActive()) {
            Connection.sendPacket(new CMapBalancerUpdateMap(game.getMini(),
                    game.getGameMap()));
        }
        Logging.IMP.info("[CoreMapBalancer] Map {} stop...", game.getGameMap().getDisplayName());
    }

    /**
     *  == Дальше идет логика для синхронизации с кором  ==
     */

    // Регистрируем все карты при запуске
    public void registerAllToCoreMaps(AbstractPlugin plugin) {
        List<Mini> miniList = getAvailableMapMini(plugin);
        Logging.IMP.info("[CoreMapBalancer] Start caching mini({}) maps!", miniList);
        for (Mini mini : miniList) {
            List<GameMap> mapsByMini = getAvailableMapList(mini, plugin);
            mapCache.put(mini, mapsByMini);
            Connection.sendPacket(new CMapBalancerRegister(mini,
                    mapsByMini.toArray(new GameMap[0])));
            Logging.IMP.info("[CoreMapBalancer] Success load maps {}", mapsByMini.toString());
        }
    }

    // Восстанавливаем карты при реконнекте
    public void recoveryActiveMaps() {
        for (Game game : GameManager.INSTANCE.getRegisteredGames()) {
            if (game.getGameMap() == null) continue;

            Connection.sendPacket(new CMapBalancerRecovery(game.getMini(), game.getGameMap()));
        }
    }
}
