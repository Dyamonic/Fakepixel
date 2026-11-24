package club.abelix.network.channel;

import w.util.Env;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ChannelConstant creation on 22.01.2024 at 13:32</p>
 */

public class ChannelConstant {

    public static final int CONNECTION_TIMEOUT = Env.getInt("NETWORK_TIMEOUT", 15_000);
    public static final int SO_TIMEOUT = Env.getInt("NETWORK_SO_TIMEOUT", 30_000);
}
