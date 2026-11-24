package club.abelix.core.velocity.filter;

import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.filter.listener.ChatFilterListener;
import club.abelix.core.velocity.filter.violation.ViolationListener;
import club.abelix.core.velocity.filter.violation.ViolationType;
import club.abelix.network.common.logging.Logging;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ChatFilterManager {

    @Getter
    private final StarshipVelocity starship;

    private final Map<Player, club.abelix.core.velocity.filter.data.PlayerData> playerData = new ConcurrentHashMap<>();

    @Getter
    private int capacity, violationThreshold;
    @Getter
    private long checkTime;

    public club.abelix.core.velocity.filter.data.PlayerData getPlayerData(@NonNull Player player) {
        return playerData.computeIfAbsent(player, p -> new club.abelix.core.velocity.filter.data.PlayerData());
    }

    public void removePlayerData(@NonNull Player player) {
        playerData.remove(player);
    }

    public void clearPlayerData() {
        playerData.clear();
    }

    public void initialize() {
        var config = starship.loadConfiguration("filter.yml");
        capacity          = config.getInt("capacity", 6);
        checkTime         = config.getInt("checkTime", 90000);
        violationThreshold = config.getInt("violationThreshold", 30);

        Logging.IMP.info("[ChatFilter] Settings loaded (violationThreshold=" + violationThreshold + ")");

        starship.getProxy().getEventManager().register(starship, new ChatFilterListener(this, starship.getNatsChatPublisher()));
        starship.getProxy().getEventManager().register(starship, new ViolationListener(this));
    }
}
