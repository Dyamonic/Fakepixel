package club.abelix.api.base.camera.listener;

import club.abelix.api.base.camera.PlayerCameraManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

import static com.comphenix.protocol.PacketType.Play.Server.SET_SLOT;
import static com.comphenix.protocol.PacketType.Play.Server.WINDOW_ITEMS;

public class PlayerCameraPacketListener extends PacketAdapter {

    private static final ItemStack AIR = new ItemStack(Material.AIR);
    private static final List<ItemStack> AIR_LIST = new ArrayList<>(45);

    static {
        for (int i = 0; i <= 44; i++) {
            AIR_LIST.add(AIR);
        }
    }

    public PlayerCameraPacketListener(Plugin plugin) {
        super(plugin, ListenerPriority.LOWEST,
                PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT,
                PacketType.Play.Client.WINDOW_CLICK, PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.USE_ITEM);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (PlayerCameraManager.IMP.hasCamera(event.getPlayer())) {
            PacketType type = event.getPacketType();
            PacketContainer container = event.getPacket();

            int windowId = container.getIntegers().read(0);

            if (windowId != 0) {
                return;
            }

            if (type == WINDOW_ITEMS) {
                container.getItemListModifier().write(0, AIR_LIST);
            } else if (type == SET_SLOT) {
                container.getItemModifier().write(0, AIR);
            }
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (PlayerCameraManager.IMP.hasCamera(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
