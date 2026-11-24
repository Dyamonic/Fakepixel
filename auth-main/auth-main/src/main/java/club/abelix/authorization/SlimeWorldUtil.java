package club.abelix.authorization;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>SlimeWorldUtil creation on 08.10.2023 at 00:39</p>
 */
@Log4j2
@UtilityClass
public class SlimeWorldUtil {

    private final SlimePlugin slimePlugin = (SlimePlugin) Bukkit.getPluginManager()
            .getPlugin("AbelixSlimeWorldManager");
    private final SlimeLoader loader = slimePlugin.getLoader("mongo");

    @Getter
    private final List<String> loadedLocalWorlds = new ArrayList<>();

    public SlimePropertyMap getDefaultProperty() {
        val worldProperty = new SlimePropertyMap();
        worldProperty.setString(SlimeProperties.DIFFICULTY, "normal");
        worldProperty.setString(SlimeProperties.ENVIRONMENT, "normal");

        worldProperty.setInt(SlimeProperties.SPAWN_X, 0);
        worldProperty.setInt(SlimeProperties.SPAWN_Y, 0);
        worldProperty.setInt(SlimeProperties.SPAWN_Z, 0);

        worldProperty.setBoolean(SlimeProperties.PVP, true);
        worldProperty.setBoolean(SlimeProperties.ALLOW_ANIMALS, true);
        worldProperty.setBoolean(SlimeProperties.ALLOW_MONSTERS, true);
        return worldProperty;
    }

    public void updateBukkitWorldRules(@NonNull World world) {
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doTileDrops", "true");
        world.setGameRuleValue("doMobLoot", "true");
        world.setGameRuleValue("doWeatherCycle", "false");
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("announceAdvancements", "false");
        world.setGameRuleValue("showDeathMessages", "false");
        world.setAutoSave(false);
    }

    public World cloneSlimeWorld(@NonNull String mysqlName,
                                 @NonNull String localName,
                                 @NonNull SlimePropertyMap property) {
        log.info("Trying to clone world mysql={} -> local={}", mysqlName, localName);

        try {
            val mysqlWorld = slimePlugin.loadWorld(loader, mysqlName, true, property);
            val localWorld = mysqlWorld.clone(localName);

            if (localWorld == null) {
                log.error("Failed to clone world mysql={} local={}", mysqlName, localName);
                return null;
            }

            log.info("Loading world from mysql! world={}", localName);
            slimePlugin.generateWorld(localWorld);

            // Обновляем значения уже в самом мире
            val bukkitWorld = Bukkit.getWorld(localWorld.getName());
            updateBukkitWorldRules(bukkitWorld);

            // Рот я ебал этого хардкора, я уже не ебу что с ним делать
            bukkitWorld.setDifficulty(Difficulty.NORMAL);

            log.info("Successfully loaded world {}", localName);

            return bukkitWorld;
        } catch (UnknownWorldException | IOException | CorruptedWorldException | NewerFormatException |
                 WorldInUseException slimeException) {
            log.error("Failed to load for mysql={} local={}", mysqlName, localName);
            slimeException.printStackTrace();
            return null;
        }
    }

    public World cloneSlimeWorld(@NonNull String mysqlName,
                                 @NonNull String localName) {
        return cloneSlimeWorld(mysqlName, localName, getDefaultProperty());
    }

    public World loadSlimeWorld(@NonNull String worldName, @NonNull SlimePropertyMap property) {
        log.info("Trying to load {}", worldName);

        try {
            val mysqlWorld = slimePlugin.loadWorld(loader, worldName, true, property);

            if (mysqlWorld == null) {
                log.error("Failed to load {} from mysql!", worldName);
                return null;
            }

            log.info("Generate mysql world {}", mysqlWorld.getName());
            slimePlugin.generateWorld(mysqlWorld);

            // Обновляем значения уже в самом мире
            val bukkitWorld = Bukkit.getWorld(mysqlWorld.getName());
            updateBukkitWorldRules(bukkitWorld);

            // Рот я ебал этого хардкора, я уже не ебу что с ним делать
            bukkitWorld.setDifficulty(Difficulty.PEACEFUL);

            log.info("Successfully loaded world from mysql {}", mysqlWorld);

            loadedLocalWorlds.add(bukkitWorld.getName());
            return bukkitWorld;
        } catch (UnknownWorldException | IOException | CorruptedWorldException | NewerFormatException |
                 WorldInUseException slimeException) {
            log.error("Failed load world {}", worldName);
            slimeException.printStackTrace();
            return null;
        }
    }


    public World loadSlimeWorld(@NonNull String worldName) {
        return loadSlimeWorld(worldName, getDefaultProperty());
    }

    public void unloadLocalSlimeWorld(@NonNull String localWorldName) {
        try {
            loader.unlockWorld(localWorldName);
        } catch (Exception ignore) {
        }

        // Пытвемся убрать говно за баккитом
        val world = Bukkit.getWorld(localWorldName);

        for (Player entity : world.getPlayers()) {
            entity.kickPlayer("Unloading");
        }

        for (LivingEntity entity : world.getLivingEntities()) {
            entity.remove();
        }

        for (Entity entity : world.getEntities()) {
            entity.remove();
        }

        for (Chunk chunk : world.getLoadedChunks()) {
            chunk.unload();
        }
        try {
            // Ебучая утечка бакита, даже на ебучей 1.19 это не исправлено, я в ахуе
            Bukkit.unloadWorld(world, true);

            loadedLocalWorlds.remove(localWorldName);
            log.info("Deleting local world {}", localWorldName);
        } catch (Throwable throwable) {
            log.trace("Failed to delete world {}", localWorldName, throwable);
        }
    }

    public void unloadLocalSlimeWorld(@NonNull World world) {
        unloadLocalSlimeWorld(world.getName());
    }
}
