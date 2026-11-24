package club.abelix.api.base.util.jmx;

import org.bukkit.Bukkit;

public class WorldCount implements WorldCountMBean {

    @Override
    public int getWorldCount() {
        return Bukkit.getWorlds().size();
    }
}
