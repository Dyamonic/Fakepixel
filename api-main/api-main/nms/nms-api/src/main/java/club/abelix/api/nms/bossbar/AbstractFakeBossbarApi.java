package club.abelix.api.nms.bossbar;

import club.abelix.api.nms.NmsApi;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * @author WhileInside
 */
public abstract class AbstractFakeBossbarApi implements FakeBossbarApi {
    protected final NmsApi api;

    private final Set<FakeBossbar> bossbars
            = Sets.newConcurrentHashSet();
    private final Map<Player, Set<FakeBossbar>> byPlayer
            = Maps.newConcurrentMap();

    public AbstractFakeBossbarApi(NmsApi api) {
        this.api = api;
    }

    @Override
    public Collection<FakeBossbar> getBossbars() {
        return Collections.unmodifiableSet(bossbars);
    }

    @Override
    public Collection<FakeBossbar> getBossbars(Player player) {
        return byPlayer.getOrDefault(player, Collections.emptySet());
    }

    @Override
    public void removeBossbar(FakeBossbar bossbar) {
        bossbar.remove();
        bossbars.remove(bossbar);
    }

    @Override
    public void resetBossbars() {
        bossbars.forEach(FakeBossbar::remove);
        bossbars.clear();
    }

    @Override
    public Collection<FakeBossbar> setBossbar(Player player, FakeBossbar bossbar) {
        Set<FakeBossbar> newSet = new HashSet<>();
        newSet.add(bossbar);

        return byPlayer.put(player, newSet);
    }

    @Override
    public void addBossbar(Player player, FakeBossbar bossbar) {
        byPlayer.computeIfAbsent(player, x -> new HashSet<>()).add(bossbar);
    }

    protected abstract FakeBossbar newBossbar(String title, float v);

    @Override
    public FakeBossbar createBossbar(String title, float v) {
        FakeBossbar bossbar = newBossbar(title, v);
        bossbars.add(bossbar);

        return bossbar;
    }

    @Override
    public void removeBossbars(@NonNull Player player) {
        byPlayer.remove(player);
    }

    @Override
    public void removeBossbar(@NonNull Player player, @NonNull FakeBossbar bossbar) {
        Collection<FakeBossbar> bossbars = byPlayer.get(player);

        if (bossbars == null) {
            return;
        }

        bossbars.remove(bossbar);

        if (bossbars.isEmpty()) {
            byPlayer.remove(player);
        }
    }
}
