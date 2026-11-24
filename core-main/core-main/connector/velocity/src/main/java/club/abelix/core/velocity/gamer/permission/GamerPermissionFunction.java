package club.abelix.core.velocity.gamer.permission;

import club.abelix.core.velocity.gamer.Gamer;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GamerPermissionFunction implements PermissionFunction {
    Player player;

    @Override
    public Tristate getPermissionValue(String permission) {
        Gamer gamer = Gamer.getGamer(player);

        if (gamer == null || !gamer.isLoggedIn()) {
            return Tristate.FALSE;
        }

        return Tristate.fromBoolean(gamer.hasPermission(permission));
    }
}
