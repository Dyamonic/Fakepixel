package club.abelix.authorization.nms.V_1_12;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;

import java.util.List;

public class CommandMap_1_12 extends SimpleCommandMap {
    public CommandMap_1_12(Server server) {
        super(server);
    }

    public boolean register(String fallbackPrefix, Command command) {
        return true;
    }

    public boolean register(String label, String fallbackPrefix, Command command) {
        return true;
    }

    public void registerAll(String fallbackPrefix, List<Command> commands) {
    }

    public void registerServerAliases() {
    }
}