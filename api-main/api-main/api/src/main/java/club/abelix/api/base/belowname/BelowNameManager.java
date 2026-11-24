package club.abelix.api.base.belowname;

import club.abelix.commons.util.collection.CollectionUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.Set;

public class BelowNameManager {

    public static final BelowNameManager IMP = new BelowNameManager();
    @Getter
    private final Set<BelowName> belowNames = CollectionUtil.createConcurrentSet();
    private final Map<Player, BelowName> byPlayer = CollectionUtil.createConcurrentMap();
    private Objective healthObjective;

    void addBelowName(BelowName name) {
        belowNames.add(name);
    }

    void removeBelowName(BelowName name) {
        belowNames.remove(name);

        for (Player player : name.getPlayers()) {
            byPlayer.remove(player);
        }
    }

    BelowName getBelowName(Player player) {
        return byPlayer.get(player);
    }

    BelowName setBelowName(Player player, BelowName name) {
        BelowName old;

        if (name == null) {
            old = byPlayer.remove(player);
        } else {
            old = byPlayer.put(player, name);
        }

        return old;
    }

    public void createHealthTracker() {
        if (healthObjective != null) {
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        try {
            healthObjective =  board.registerNewObjective("showhealth", "health");
            healthObjective.setDisplayName("§4❤");
            healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);

            for (Player player : Bukkit.getOnlinePlayers()) {
                healthObjective.getScore(player.getName())
                        .setScore((int) player.getHealth());
            }
        } catch (Exception ignored){
        }
    }

    public void removeHealthTracker() {
        if (healthObjective != null) {
            healthObjective.unregister();
        }
    }

}
