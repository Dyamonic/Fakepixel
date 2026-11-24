package club.abelix.games.fastbridge.commons.config;

import club.abelix.api.AbstractPlugin;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Getter
public class FastBridgeConfig {

    private final AbstractPlugin plugin;

    private final Set<IslandConfig> islandConfigs = new LinkedHashSet<>();

    public FastBridgeConfig(AbstractPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        val configuration = plugin.loadConfig("config");

        islandConfigs.clear();

        log.info("Configuration: [{}]", StringUtils.join(configuration.getKeys(false), ","));

        for (String gameType : configuration.getKeys(false)) {
            try {
                islandConfigs.add(IslandConfig.create(configuration.getConfigurationSection(gameType)));
            } catch (Exception exception) {
                log.warn("Section {} is not island", gameType);
                log.trace(exception);
            }
        }

        if (islandConfigs.isEmpty()) log.error("None of the island configurations have been loaded");
    }

    public Optional<IslandConfig> findConfigForWorld(@NonNull String worldName) {
        return islandConfigs.stream().filter(config -> config.worldName().equals(worldName)).findAny();
    }

}
