package club.abelix.api.server.game.map.util;

import club.abelix.network.type.map.GameMap;
import lombok.experimental.UtilityClass;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>GameMapUtil creation on 26.12.2023 at 22:36</p>
 */

@UtilityClass
public class GameMapUtil {
    public GameMap parseMap(@NotNull ConfigurationSection mapSection) {
        String technicalName = mapSection.getString("technical");
        String displayName = mapSection.getString("display");
        int multiplier = mapSection.getInt("multiplier");

        return new GameMap(displayName, technicalName, multiplier);
    }

    public GameMap parseMapByKey(String key, @NotNull ConfigurationSection mapSection) {
        String displayName = mapSection.getString("display");
        int multiplier = mapSection.getInt("multiplier");

        return new GameMap(displayName, key, multiplier);
    }

    public List<GameMap> parseMapByKeyAll(@NotNull ConfigurationSection mapsSection) {
        List<GameMap> result = new ArrayList<>();
        for (String key : mapsSection.getKeys(false)) {
            result.add(parseMapByKey(key, mapsSection.getConfigurationSection(key)));
        }
        return result;
    }
}
