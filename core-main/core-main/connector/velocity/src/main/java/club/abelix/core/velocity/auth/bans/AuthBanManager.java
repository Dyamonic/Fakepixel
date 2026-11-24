package club.abelix.core.velocity.auth.bans;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import lombok.NonNull;

import java.util.concurrent.TimeUnit;

public class AuthBanManager {

    private static final TObjectLongMap<String> BANNED_ADDRESSES = new TObjectLongHashMap<>();

    public static void addBannedAddress(@NonNull final String address) {
        BANNED_ADDRESSES.put(address, System.currentTimeMillis());
    }

    public static boolean isBanExpired(@NonNull final String address) {
        long time = BANNED_ADDRESSES.get(address);

        if (System.currentTimeMillis() > time + TimeUnit.MINUTES.toMillis(15)) {
            BANNED_ADDRESSES.remove(address);

            return true;
        }

        return false;
    }

}
