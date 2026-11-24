package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.player.FakeEntityCorpse;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.exception.SkinException;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class FakeCorpse1_8 extends FakePlayer1_8 implements FakeEntityCorpse {

    private final Location bedLocation;

    public FakeCorpse1_8(@NonNull NmsApi nmsApi, @NonNull String skin, @NonNull Location location) throws SkinException {
        super(nmsApi, FakeEntityPlayer.generateRandomName(), skin, location);

        this.bedLocation = location.clone();
        this.bedLocation.setY(255);
    }

    public FakeCorpse1_8(@NonNull NmsApi nmsApi, @NonNull Skin skin, @NonNull Location location) {
        super(nmsApi, FakeEntityPlayer.generateRandomName(), skin, location);

        this.bedLocation = location.clone();
        this.bedLocation.setY(255);
    }

    @Override
    public void spawnEntity(@NotNull Collection<? extends Player> players) {
        super.spawnEntity(players);

        PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();

        for (Player player : players) {
            player.sendBlockChange(bedLocation, Material.BED_BLOCK, (byte) 0);

            packetApi.sendPacket(player,
                    packetApi.getBedPacket(getEntityId(), bedLocation.getBlockX(), bedLocation.getBlockY(), bedLocation.getBlockZ()));

            packetApi.sendPacket(player,
                    packetApi.getEntityTeleportPacket(getEntityId(), getLocation()));
        }
    }

    @Override
    public void destroyEntity(@NotNull Collection<? extends Player> players) {
        super.destroyEntity(players);

        for (Player player : players) {
            player.sendBlockChange(bedLocation, Material.AIR, (byte) 0);
        }
    }
}
