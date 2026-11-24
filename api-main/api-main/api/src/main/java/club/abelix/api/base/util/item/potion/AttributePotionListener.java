package club.abelix.api.base.util.item.potion;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.nms.nbt.tag.CompoundTag;
import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author Unidentified Person
 */
public class AttributePotionListener extends YListener {

    public AttributePotionListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPotionConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.POTION) return;

        CompoundTag tag = FalconAPI.getNmsApi().getNbtApi().getItemTag(item);

        CompoundTag attributePotion = tag.getCompound("attribute-potion");
        if (attributePotion == null) return;

        String name = attributePotion.getString("name");
        double mod = attributePotion.getDouble("mod");
        PotionEffectType effectType = PotionEffectType.getById(attributePotion.getByte("effect"));
        int duration = attributePotion.getInt("duration");

        Player player = event.getPlayer();
        player.addPotionEffect(new PotionEffect(effectType, duration, 1, false, true));
        CraftPlayer craftPlayer = (CraftPlayer) player;
        EntityPlayer nmsPlayer = craftPlayer.getHandle();

        MobEffect effect = new MobEffect(effectType.getId(), duration, 1);
        nmsPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEffect(nmsPlayer.getId(), effect));

        AttributeInstance instance = nmsPlayer.getAttributeMap().a(name);
        double oldValue = instance.getValue();
        instance.setValue(oldValue * mod);

        HeartBeat.runLaterSync(() -> {
            nmsPlayer.playerConnection.sendPacket(new PacketPlayOutRemoveEntityEffect(nmsPlayer.getId(), effect));
            instance.setValue(oldValue);
        }, duration);
    }
}
