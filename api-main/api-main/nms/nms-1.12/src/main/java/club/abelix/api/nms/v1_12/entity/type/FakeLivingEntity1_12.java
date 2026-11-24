package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.interact.InteractAction;
import club.abelix.api.nms.entity.type.FakeLivingEntity;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.v1_12.entity.FakeEntityImpl1_12;
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
public class FakeLivingEntity1_12 extends FakeEntityImpl1_12 implements FakeLivingEntity {

    @Setter
    private BiConsumer<Player, InteractAction> clickAction;

    private final Set<PotionEffect> potionEffects = new HashSet<>();

    private boolean AI;
    private float health;

    public FakeLivingEntity1_12(@NonNull NmsApi nmsApi, @NonNull EntityType entityType, @NonNull Location location) {
        super(nmsApi, entityType, location);
    }

    @Override
    public synchronized void setAI(boolean b) {
        AI = b;

        // todo
    }

    @Override
    public synchronized void setHealth(float health) {
        this.health = health;

        // todo
    }

    private PacketContainer asPacket(PotionEffect effect) {
        return nmsApi.getPacketLibraryApi().getEntityEffect(getEntityId(), effect.getType().getId(),
                effect.getAmplifier(), effect.getDuration(), !effect.hasParticles());
    }

    @Override
    protected void addSpawnEntityPackets(@NonNull PacketLibraryApi packetApi, Collection<PacketContainer> packets) {
        super.addSpawnEntityPackets(packetApi, packets);

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
