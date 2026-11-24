package club.abelix.api.base.util.jmx;

import org.bukkit.Bukkit;

public class ServerOnline implements ServerOnlineMBean {

    @Override
    public int getServerOnline() {
        return Bukkit.getOnlinePlayers().size();
    }
}
