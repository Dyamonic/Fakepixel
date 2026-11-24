package club.abelix.network.common;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>NetworkPlayerLoad creation on 18.09.2022 at 00:37</p>
 */

@Getter
@Setter
public class NetworkPlayerLoad {
    public static final int IS_ONLINE = 1;
    public static final int HAS_PARTY = 2;
    public static final int LOAD_PARTY = 3;

    public static final int ALL_INFO = IS_ONLINE | LOAD_PARTY;

    @Data
    @RequiredArgsConstructor
    public static class PlayerHolder {
        private final boolean online;
        private final int playerId;
        private final PartyHolder party;

        private final String server;

        public PlayerHolder(int playerId) {
            this(false, playerId, null, null);
        }


        public PlayerHolder(int playerId, PartyHolder party, String server) {
            this(true, playerId, party, server);
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class PartyHolder {
        private final int partyId;
        private final boolean isPrivate;
        private final int leader;
        private final int[] members;
    }
}
