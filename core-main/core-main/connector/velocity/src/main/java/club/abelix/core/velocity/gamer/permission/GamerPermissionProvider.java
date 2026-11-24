package club.abelix.core.velocity.gamer.permission;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;

public class GamerPermissionProvider implements PermissionProvider {

    @Override
    public PermissionFunction createFunction(PermissionSubject permissionSubject) {
        Player player = null;

        if (permissionSubject instanceof Player) {
            player = (Player) permissionSubject;
        }

        if (player == null) {
            return null;
        }

        return new GamerPermissionFunction(player);
    }
}
