package club.abelix.api.nms.command;

import org.bukkit.command.Command;

public interface BukkitCommandMap {

    void unregisterCommand(String name);

    boolean register(String pluginName, Command command);

}
