package club.abelix.core.velocity.util.reflection;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.util.GameProfile;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;

@UtilityClass
public final class PlayerReflectionUtil {

    @SneakyThrows(IllegalAccessException.class)
    public void updateProfile(@NonNull Player player, GameProfile profile) throws NoSuchFieldException {
        val field = player.getClass().getDeclaredField("profile");

        field.setAccessible(true);
        field.set(player, profile);
        field.setAccessible(false);
    }

    @SneakyThrows(IllegalAccessException.class)
    public void updateServerConnection(@NonNull Player player, ServerConnection connection) throws NoSuchFieldException {
        val field = player.getClass().getDeclaredField("connectedServer");

        field.setAccessible(true);
        field.set(player, connection);
        field.setAccessible(false);
    }
}