package club.abelix.api.base.bowfix;

import club.abelix.api.AbstractPlugin;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BowFixListener extends PacketAdapter {

    private final Set<UUID> drawing = ConcurrentHashMap.newKeySet();
    private final Set<UUID> slotSwitchedFromBow = ConcurrentHashMap.newKeySet();

    public BowFixListener(AbstractPlugin plugin) {
        super(plugin, ListenerPriority.LOWEST,
                PacketType.Play.Client.HELD_ITEM_SLOT,
                PacketType.Play.Client.BLOCK_DIG,
                PacketType.Play.Client.USE_ITEM,
                PacketType.Play.Client.BLOCK_PLACE
        );
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType type = event.getPacketType();

        if (type == PacketType.Play.Client.USE_ITEM || type == PacketType.Play.Client.BLOCK_PLACE) {
            ItemStack inHand = player.getItemInHand();
            if (inHand != null && inHand.getType() == Material.BOW) {
                drawing.add(player.getUniqueId());
            }
            return;
        }

        if (type == PacketType.Play.Client.BLOCK_DIG) {
            try {
                Enum<?> digType = event.getPacket().getPlayerDigTypes().read(0);
                if (digType != null && "RELEASE_USE_ITEM".equals(digType.name())) {
                    drawing.remove(player.getUniqueId());
                }
            } catch (Throwable ignored) {}
            return;
        }

        if (type == PacketType.Play.Client.HELD_ITEM_SLOT) {
            int newSlot = event.getPacket().getIntegers().read(0);
            int currentSlot = player.getInventory().getHeldItemSlot();
            if (currentSlot == newSlot) return;

            ItemStack item = player.getInventory().getItem(currentSlot);
            if (item == null || item.getType() != Material.BOW) {
                drawing.remove(player.getUniqueId());
                return;
            }

            if (drawing.contains(player.getUniqueId())) {
                slotSwitchedFromBow.add(player.getUniqueId());
            }
            drawing.remove(player.getUniqueId());
        }
    }

    public boolean wasSlotSwitched(Player player) {
        return slotSwitchedFromBow.contains(player.getUniqueId());
    }

    public void clearSlotSwitch(Player player) {
        slotSwitchedFromBow.remove(player.getUniqueId());
    }

    public void clear(Player player) {
        drawing.remove(player.getUniqueId());
        slotSwitchedFromBow.remove(player.getUniqueId());
    }
}
