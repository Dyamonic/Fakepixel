package club.abelix.api.nms.command;

import lombok.NonNull;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class BaseCommandApi implements CommandApi {

    private CommandMap commandMap;

    public CommandMap getCommandMap() {
        if (commandMap == null) {
            try {
                val commandMapField = Bukkit.getServer().getClass()
                        .getDeclaredField("commandMap");

                commandMapField.setAccessible(true);
                commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
                commandMapField.setAccessible(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return commandMap;
    }

    @Override
    public void registerCommand(@NonNull Plugin plugin, @NonNull Command command) {
        getCommandMap().register(COMMAND_REGISTER_NAME, command);
    }

    @Override
    public void unregisterCommand(@NonNull String commandName) {
        try {
            val knownCommands = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommands.setAccessible(true);

            val map = (Map<String, Command>) knownCommands.get(getCommandMap());

            val command = map.remove(commandName.toLowerCase());

            if (command == null) {
                return;
            }

            command.getAliases().forEach(aliases -> map.remove(aliases.toLowerCase()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
