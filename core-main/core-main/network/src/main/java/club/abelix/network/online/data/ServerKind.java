package club.abelix.network.online.data;

/**
 * Describe the kind of server.
 * <p>
 * e.g. Servers like bw-a1 bw-a2 are ARENA kind, while lobby-1, hub-1, bw-1 are PHYSICAL kind.
 * @see club.abelix.network.type.mode.ServerMode
 */
public enum ServerKind {

    /**
     * Describe the mini servers running on single bukkit instance.
     * */
    ARENA,

    /**
     * Describe the physical bukkit server.
     * */
    PHYSICAL,

    /**
     * Describe the proxy server.
     * */
    PROXY,

}
