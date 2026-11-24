package club.abelix.api.base.util.jmx;

import org.bukkit.Bukkit;

public class PendingTasks implements PendingTasksMBean {

    @Override
    public int getPendingTasks() {
        return Bukkit.getScheduler().getPendingTasks().size();
    }
}
