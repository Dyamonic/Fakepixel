package club.abelix.commons.data.bestplayer;

import java.util.concurrent.TimeUnit;


public record BestPlayerEntry(int id, int playerId, int price, long time) {

    public boolean isExpired() {
        return System.currentTimeMillis() > (time + TimeUnit.DAYS.toMillis(7));
    }

}
