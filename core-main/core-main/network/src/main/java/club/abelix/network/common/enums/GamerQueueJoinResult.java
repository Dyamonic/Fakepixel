package club.abelix.network.common.enums;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>GamerQueueJoinResult creation on 18.09.2022 at 00:21</p>
 */

public enum GamerQueueJoinResult {
    SUCCESS,
    FAIL,
    QUEUE_NOT_REGISTERED,
    ONLY_PARTY_LEADER,
    GAMER_HAVE_OTHER_QUEUE;

    public String formatToLocaleKey() {
        return "queue_result_" + name().toLowerCase();
    }
}
