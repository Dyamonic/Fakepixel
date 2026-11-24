package club.abelix.api.base.version;

import club.abelix.commons.version.GameVersion;
import org.bukkit.entity.Player;

/**
 * @author WhileInside
 */
public interface PlayerVersionProvider {

    GameVersion getVersion(Player player);

}
