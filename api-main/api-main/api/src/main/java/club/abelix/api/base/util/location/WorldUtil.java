package club.abelix.api.base.util.location;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

@UtilityClass
public class WorldUtil {

    public World getOrCreateWorld(String name) {
        World world;

        return (world = Bukkit.getWorld(name)) == null
                ? Bukkit.createWorld(new WorldCreator(name).generator("EmptyGenerator"))
                : world;
    }

}
