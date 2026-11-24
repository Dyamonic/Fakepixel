package club.abelix.api.base.version;

import club.abelix.commons.version.GameVersion;
import com.viaversion.viaversion.api.Via;
import org.bukkit.entity.Player;

/**
 * @author WhileInside
 */
public class ViaVersionProvider implements PlayerVersionProvider {
    @Override
    public GameVersion getVersion(Player player) {
        return GameVersion.getByProtocolNumber(Via.getAPI().getPlayerVersion(player.getUniqueId()));
    }
}
