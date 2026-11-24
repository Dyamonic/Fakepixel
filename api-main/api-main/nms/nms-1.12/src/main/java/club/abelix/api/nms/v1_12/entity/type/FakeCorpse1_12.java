package club.abelix.api.nms.v1_12.entity.type;

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

public class FakeCorpse1_12 extends FakePlayer1_12 implements FakeEntityCorpse {

    public FakeCorpse1_12(@NonNull NmsApi nmsApi, @NonNull Skin skin, @NonNull Location location) {
        super(nmsApi, FakeEntityPlayer.generateRandomName(), skin, location);
    }

    public FakeCorpse1_12(NmsApi nmsApi, String skin, Location location) throws SkinException {
        super(nmsApi, FakeEntityPlayer.generateRandomName(), skin, location);
    }

    @Override
    public synchronized void spawnEntity(@NotNull Collection<? extends Player> players) {
        super.spawnEntity(players);

        Location bedLocation = getLocation().clone();
        bedLocation.setY(0);

        PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();

        for (Player player : players) {
            player.sendBlockChange(bedLocation, Material.BED_BLOCK, (byte) 0);

            packetApi.sendPacket(player,
                    packetApi.getBedPacket(getEntityId(), bedLocation.getBlockX(), bedLocation.getBlockY(), bedLocation.getBlockZ()));

            packetApi.sendPacket(player,
                    packetApi.getEntityTeleportPacket(getEntityId(), getLocation()));
        }
    }

}
