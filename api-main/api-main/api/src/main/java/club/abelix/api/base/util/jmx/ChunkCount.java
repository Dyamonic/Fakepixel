package club.abelix.api.base.util.jmx;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class ChunkCount implements ChunkCountMBean {

    @Override
    public int getChunkCount() {
        int result = 0;

        for (World world : Bukkit.getWorlds()) {
            result += world.getLoadedChunks().length;
        }

        return result;
    }
}
