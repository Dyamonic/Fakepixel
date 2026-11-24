package club.abelix.commons.data.punishment.meta;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class MuteMeta extends PunishmentMeta {

    private final int playerId;

    public MuteMeta(int id, int playerId, @NonNull String banBy, long time, long expire, @NonNull String reason) {
        this(id, playerId, banBy, null, time, expire, reason);
    }

    public MuteMeta(int id, int playerId, @NonNull String adminName, String unBanBy, long issueDate, long expirationDate, @NonNull String reason) {
        super(id, adminName, unBanBy, reason, issueDate, expirationDate);

        this.playerId = playerId;
    }

}
