package club.abelix.core.whitelist;

import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Uncommon;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>WhitelistUtility creation on 03.12.2022 at 22:26</p>
 */

@UtilityClass
public class WhitelistUtility {

    public ServerMode getServerMode(@NonNull String rawName) {
        Master masterResult = Master.getByName(rawName);
        if (masterResult != null) {
            return masterResult;
        }
        Uncommon uncommonResult = Uncommon.getByName(rawName);
        if (uncommonResult != null) {
            return uncommonResult;
        }

        throw new IllegalArgumentException("Failed get server mode");
    }
}
