package club.abelix.api.base.camera;

import club.abelix.api.base.camera.listener.PlayerCameraListener;
import club.abelix.api.base.camera.listener.PlayerCameraPacketListener;
import club.abelix.api.base.util.chat.MessageSender;
import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class PlayerCameraManager {

    //======================================================================
    public static final PlayerCameraManager IMP = new PlayerCameraManager();
    //======================================================================
    private final Map<Player, PlayerCamera> playerCameraMap = new HashMap<>();
    private boolean registered;

    public void registerListeners(Plugin plugin) {
        if (registered) {
            return;
        }

        registered = true;

        new PlayerCameraListener(plugin);

        ProtocolLibrary.getProtocolManager()
                .addPacketListener(new PlayerCameraPacketListener(plugin));
    }

    public boolean hasCamera(Player player) {
        return playerCameraMap.containsKey(player);
    }

    public void addCamera(PlayerCamera camera) {
        PlayerCamera oldCamera = playerCameraMap.put(camera.getPlayer(), camera);

        if (oldCamera != null) {
            oldCamera.destroy(CameraDestroyReason.ANOTHER_CAMERA_CREATED);
        }

        camera.start();
    }

    public PlayerCamera getCamera(Player player) {
        return playerCameraMap.get(player);
    }

    public void removeCamera(Player player, CameraDestroyReason reason) {
        PlayerCamera camera = playerCameraMap.remove(player);

        if (camera != null) {
            camera.destroy(reason);
        }

        // resetTitle() почему-то не работает
        MessageSender.sendTitle(player, "", "");
    }
}
