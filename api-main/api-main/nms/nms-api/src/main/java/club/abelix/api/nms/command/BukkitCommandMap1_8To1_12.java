package club.abelix.api.nms.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;

public class BukkitCommandMap1_8To1_12 extends SimpleCommandMap implements BukkitCommandMap {

    public BukkitCommandMap1_8To1_12(SimpleCommandMap original) {
        super(Bukkit.getServer());

        for (Command command : original.getCommands()) {
            knownCommands.put(command.getLabel(), command);
        }
    }

    @Override
    public void unregisterCommand(String name) {
        Command command = knownCommands.remove(name);

        if (command != null) {
            command.unregister(this);
        }
    }


}
