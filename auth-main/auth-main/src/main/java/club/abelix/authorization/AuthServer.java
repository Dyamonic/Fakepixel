package club.abelix.authorization;

import club.abelix.authorization.nms.V_1_12.CommandApi_V_1_12;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@Log4j2
public class AuthServer extends JavaPlugin {

    private static Location stringToLocation(String line) {
        if (line == null) return null;

        val locationInfo = line.split(";");

        val world = SlimeWorldUtil.loadSlimeWorld(locationInfo[0]);

        if (world == null) {
            throw new RuntimeException("World with given name not found name=" + locationInfo[0]);
        }

        val location = new Location(world,
                Double.parseDouble(locationInfo[1]),
                Double.parseDouble(locationInfo[2]),
                Double.parseDouble(locationInfo[3]));

        if (locationInfo.length > 4) {
            location.setYaw(Float.parseFloat(locationInfo[4]));
            location.setPitch(Float.parseFloat(locationInfo[5]));
        }

        return location;
    }

    @Override
    public void onEnable() {
        log.info("Enabling authorization plugin...");
        val file = new File(getDataFolder(), "authorization.yml");

        if (!file.exists()) {
            log.warn("Can't find authorization.yml in server directory");
        } else {
            val configuration = YamlConfiguration.loadConfiguration(file);
            val spawnLocation = stringToLocation(configuration.getString("Spawn"));

            if (spawnLocation != null) {
                log.info("Using authentication lobby restrictions for this server");
                getServer().getPluginManager().registerEvents(new AuthListener(this, spawnLocation), this);
                val version = this.getServer().getClass().getPackage().getName().split("\\.")[3];

                log.info("Now clearing registered commands. Using NMS version {}", version);
                if ("v1_12_R1".equals(version)) {
                    new CommandApi_V_1_12().unregisterCommands();
                } else {
                    log.warn("This nms version is not supported");
                }
            }
        }
    }
}