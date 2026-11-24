package club.abelix.api.server.game.map.selector.impl;

import club.abelix.api.base.util.location.RawLocation;
import club.abelix.api.base.util.slime.SlimeWorldUtil;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.map.selector.GameMapSelector;
import club.abelix.api.server.game.map.util.GameMapUtil;
import club.abelix.commons.util.exclusive.ExclusivePeriod;
import club.abelix.commons.util.password.PasswordHashing;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.map.GameMap;
import club.abelix.network.type.mode.Mini;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import lombok.NonNull;
import lombok.val;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SlimeMapSelector implements GameMapSelector {
    // ====================================================================
    public static final SlimeMapSelector INSTANCE = new SlimeMapSelector();
    // ====================================================================

    /**
     * Загрузка мира
     *
     * @param game игра
     * @return загруженный мир баккита
     */
    @Override
    public World loadWorld(@NonNull Game game, @NonNull RawLocation center) {
        String technicalName = game.getGameMap().getTechnicalName();
        game.getPlugin().getLogger().info(String.format("[%s] (SLimeMapSelector) Trying to load map %s (Center: %s)", game.getName(), technicalName, center));
        // Название локального мира, добавляем UUID, те почему-то возникают ошибки с удалением миров
        String servername = game.getName();
        String localWorldName = String.format("%s_%s", servername.replace("-", "").toUpperCase(), PasswordHashing.generateRandomPassword(6).toUpperCase());

        try {
            SlimePropertyMap properties = SlimeWorldUtil.getDefaultProperty();

            properties.setInt(SlimeProperties.SPAWN_X, (int) center.x());
            properties.setInt(SlimeProperties.SPAWN_Y, (int) center.y());
            properties.setInt(SlimeProperties.SPAWN_Z, (int) center.z());

            World bukkitWorld = SlimeWorldUtil.cloneSlimeWorld(technicalName, localWorldName, properties);

            if (bukkitWorld == null) {
                // Оповещаем в консоль, что всему пиздец
                game.getPlugin().getLogger().warning(
                        String.format("[%s] (SLimeMapSelector) CRITICAL ERROR :: We failed to load World %s (Game World %s)",
                                game.getName(), technicalName, localWorldName));
                return null;
            }

            bukkitWorld.setGameRuleValue("doTileDrops", "true");
            bukkitWorld.setGameRuleValue("doEntityDrops", "true");
            bukkitWorld.setGameRuleValue("mobGriefing", "true");

            return bukkitWorld;

        } catch (Exception ex) {

            game.getPlugin().getLogger().warning(String.format("[%s] Error load world to game", game.getName()));
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Отгрузка текущего мира игры
     *
     * @param game игра
     */
    @Override
    public void unloadAndDeleteWorld(@NonNull Game game) {
        if (game.getGameWorld() == null) {
            game.getPlugin().getLogger().warning(String.format("[%s] (SlimeMapSelector) Game world is null. Failed delete",
                    game.getName()));
            return;
        }

        game.getPlugin().getLogger().info(String.format("[%s] (SlimeMapSelector) Start unload game world %s in SWM system",
                game.getName(), game.getGameWorld().getName()));

        String worldName = game.getGameWorld().getName();
        SlimeWorldUtil.unloadLocalSlimeWorld(worldName);
        game.getPlugin().getLogger().info(String.format(
                "[%s] (SlimeMapSelector) Unload world %s", game.getName(), worldName));
    }

    /**
     * Выбор рандомной карты
     *
     * @param game игра
     * @return техническое название карты
     */
    @Override
    public GameMap selectMap(@NonNull Game game) {
        List<GameMap> maps = getAvailableMapList(game.getMini(), game.getPlugin());

        int mapIndex = ThreadLocalRandom.current().nextInt(maps.size());

        GameMap map = maps.get(mapIndex);

        game.getPlugin().getLogger().info(String.format("[%s] (SlimeMapSelector) Selected map #%d/%d (%s)",
                game.getName(), mapIndex, maps.size(), map.getDisplayName()));

        return map;
    }


    protected List<Mini> getAvailableMapMini(Plugin plugin) {
        val availableMaps = new File(plugin.getDataFolder().toPath().toFile(), "available_maps.yml");

        if (!availableMaps.exists()) {
            return Collections.emptyList();
        }

        FileConfiguration configuration = YamlConfiguration.loadConfiguration(availableMaps);

        List<Mini> result = new ArrayList<>();

        for (String key : configuration.getKeys(false)) {
            Logging.IMP.info(key.toUpperCase());
            try {
                result.add(Mini.valueOf(key.toUpperCase()));
            } catch (Exception ignore) {
            }
        }
        return result;
    }

    protected List<GameMap> getAvailableMapList(@NonNull Mini mini, Plugin plugin) {
        val availableMaps = new File(plugin.getDataFolder().toPath().toFile(), "available_maps.yml");

        Logging.IMP.info("[MapSelector] Attempt to load maps for {}", mini.getDisplayName());

        if (!availableMaps.exists()) {
            Logging.IMP.warn(String.format("[%s] (SlimeMapSelector) Available maps configuration not found!", mini.getName()));
            return Collections.emptyList();
        }

        FileConfiguration configuration = YamlConfiguration.loadConfiguration(availableMaps);

        String baseName = mini.name().toLowerCase();
        ConfigurationSection mapsSection = configuration.getConfigurationSection(baseName);
        if (mapsSection == null) {
            throw new RuntimeException(String.format("[MapSelector] Failed find map available section %s", baseName));
        }

        List<GameMap> resultMap = new ArrayList<>(GameMapUtil.parseMapByKeyAll(mapsSection));

        // Эксклюзивные карты под конкретный период
        List<ExclusivePeriod> exclusivePeriods = ExclusivePeriod.getCurrentExclusivePeriods();
        for (ExclusivePeriod exclusivePeriod : exclusivePeriods) {
            Logging.IMP.info("[MapSelector] try load {} period for {}",
                    exclusivePeriod.name(), mini.getDisplayName());
            // exclusive.new_year.mini
            String exclusiveKey = "exclusive." + exclusivePeriod.getExclusiveIdent().toLowerCase() + "." + baseName;

            if (configuration.contains(exclusiveKey)) {
                Logging.IMP.info("[MapSelector] Found exclusive period {} to {}",
                        exclusivePeriod.name(), mini.getDisplayName());
                resultMap.addAll(GameMapUtil.parseMapByKeyAll(
                        configuration.getConfigurationSection(exclusiveKey)
                ));
            } else {
                Logging.IMP.info("[MapSelector] No exclusive period {} found for {}",
                        exclusivePeriod.name(), mini.getDisplayName());
            }
        }

        Logging.IMP.info("[MapSelector] The maps have been successfully load for {}", mini.getDisplayName());
        return resultMap;
    }
}
