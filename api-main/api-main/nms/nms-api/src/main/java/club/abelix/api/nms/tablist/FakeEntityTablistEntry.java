package club.abelix.api.nms.tablist;

import club.abelix.api.nms.entity.type.FakeEntity;
import org.bukkit.entity.Player;

import java.util.Collection;

public record FakeEntityTablistEntry(
        Collection<? extends Player> players,
        FakeEntity fakeEntity,
        long delay,
        long createdAt
) {

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt >= delay;
    }

}