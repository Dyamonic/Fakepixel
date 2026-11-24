package club.abelix.api.base.util.jmx;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class EntityCount implements EntityCountMBean {

    @Override
    public int getEntityCount() {
        int result = 0;

        for (World world : Bukkit.getWorlds()) {
            result += world.getEntities().size();
        }

        return result;
    }
}
