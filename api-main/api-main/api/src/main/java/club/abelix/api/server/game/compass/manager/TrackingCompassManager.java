package club.abelix.api.server.game.compass.manager;

import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.compass.TargetSelector;
import club.abelix.api.server.game.listener.game.GameCompassListener;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrackingCompassManager {

    private final List<TargetSelector> registeredSelectors = new ArrayList<>();
    private final Map<UUID, TargetSelector> playerSelectors = new HashMap<>();

    public TrackingCompassManager(Game game) {
        game.registerListener(new GameCompassListener(game, this));
    }

    public void registerCustomSelector(TargetSelector targetSelector) {
        registeredSelectors.add(targetSelector);
    }

    public void unregisterSelector(String selectorKey) {
        if (registeredSelectors.size() - 1 <= 0) {
            throw new IllegalArgumentException("Can not remove target selector if no other exists");
        }

        registeredSelectors.removeIf(targetSelector -> targetSelector.getIdentifier().equals(selectorKey));
    }

    public List<TargetSelector> getRegisteredSelectors() {
        return Collections.unmodifiableList(registeredSelectors);
    }

    public TargetSelector findNextTargetSelector(TargetSelector currentSelector) {
        return registeredSelectors.stream()
                .filter(targetSelector -> targetSelector != currentSelector)
                .filter(TargetSelector::canSpectatorUse)
                .findFirst()
                .orElse(null);
    }

    public TargetSelector getTargetSelector(Player player) {
        return playerSelectors.computeIfAbsent(player.getUniqueId(), target -> null);
    }

    public void setTargetSelector(Player player, TargetSelector selector) {
        playerSelectors.put(player.getUniqueId(), selector);
    }
}
