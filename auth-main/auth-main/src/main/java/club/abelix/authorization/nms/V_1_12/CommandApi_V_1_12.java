package club.abelix.authorization.nms.V_1_12;

import club.abelix.authorization.nms.CommandApi;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;

import java.lang.reflect.Field;

public class CommandApi_V_1_12 implements CommandApi {
    public void unregisterCommands() {
        CraftServer server = (CraftServer) Bukkit.getServer();

        try {
            Field field = CraftServer.class.getDeclaredField("commandMap");
            field.setAccessible(true);
            field.set(server, new CommandMap_1_12(server));
        } catch (IllegalAccessException | NoSuchFieldException var3) {
            var3.printStackTrace();
        }

    }
}