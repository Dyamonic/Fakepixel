package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.interact.InteractAction;
import club.abelix.api.nms.entity.type.FakeLivingEntity;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.v1_8.entity.FakeEntityImpl1_8;
import com.comphenix.protocol.events.PacketContainer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

@Getter
public class FakeLivingEntity1_8 extends FakeEntityImpl1_8 implements FakeLivingEntity {

    @Setter
    private BiConsumer<Player, InteractAction> clickAction;

    private final Set<PotionEffect> potionEffects = new HashSet<>();

    private boolean AI;
    private float health;

    public FakeLivingEntity1_8(@NonNull NmsApi nmsApi, @NonNull EntityType entityType, @NonNull Location location) {
        super(nmsApi, entityType, location);
    }

    @Override
    public void setAI(boolean b) {
        AI = b;

        getDataWatcher().setObject(15, (byte) (b ? 1 : 0));
        broadcastDataWatcherPacket();
    }

    @Override
    public void setHealth(float health) {
        this.health = health;

        getDataWatcher().setObject(6, health);
        broadcastDataWatcherPacket();
    }

    private PacketContainer asPacket(PotionEffect effect) {
        return nmsApi.getPacketLibraryApi().getEntityEffect(getEntityId(), effect.getType().getId(),
                effect.getAmplifier(), effect.getDuration(), !effect.hasParticles());
    }

    @Override
    protected void addSpawnEntityPackets(@NonNull PacketLibraryApi packetApi, Collection<PacketContainer> packets) {
        super.addSpawnEntityPackets(packetApi, packets);

        packets.add(packetApi.getEntityLookPacket(getEntityId(), getLocation()));
        packets.add(packetApi.getHeadRotationPacket(getEntityId(), getLocation().getYaw()));

        for (PotionEffect effect : potionEffects) {
            packets.add(asPacket(effect));
        }
    }

    @Override
    public void addPotionEffect(@NonNull PotionEffect effect) {
        potionEffects.add(effect);

        PacketContainer packet = asPacket(effect);

        for (Player player : getReceivers()) {
            nmsApi.getPacketLibraryApi().sendPacket(player, packet);
        }
    }

}
