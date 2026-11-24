package club.abelix.core.velocity.listener.player.permission;

import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.gamer.permission.GamerPermissionProvider;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>PlayerPermissionsListener creation on 29.07.2023 at 13:47</p>
 */

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerPermissionListener {
    StarshipVelocity core;
    GamerPermissionProvider gamerProvider = new GamerPermissionProvider();


    @Subscribe
    public void onPlayerSetProvider(PermissionsSetupEvent event) {
        if (event.getSubject() instanceof Player) {
            event.setProvider(gamerProvider);
        }
    }
}
