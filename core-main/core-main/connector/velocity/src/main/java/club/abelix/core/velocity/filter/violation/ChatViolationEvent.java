package club.abelix.core.velocity.filter.violation;

import club.abelix.core.velocity.filter.data.PlayerData;
import com.velocitypowered.api.proxy.Player;

public record ChatViolationEvent(Player player, PlayerData data, ViolationType type) {

}
