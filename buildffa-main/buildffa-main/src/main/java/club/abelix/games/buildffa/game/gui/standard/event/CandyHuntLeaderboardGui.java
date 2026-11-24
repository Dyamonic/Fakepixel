package club.abelix.games.buildffa.game.gui.event;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

public final class CandyHuntLeaderboardGui extends StandardEventLeaderboardGui {
    public CandyHuntLeaderboardGui(Player player, String eventName, Consumer<StandardEventLeaderboardGui> renderer) {
        super(player, eventName, renderer);
    }

    @Override
    protected String loreKey() {
        return "buildffa_event_stats_player_candy_hunt_lore";
    }

    @Override
    protected Map<String, String> statistics(int kills, int collected, int eventsPlayed) {
        Map<String, String> values = super.statistics(kills, collected, eventsPlayed);
        values.put("%event_candy_hunt_collected%", String.valueOf(Math.max(0, collected)));
        return values;
    }
}
