package club.abelix.api.nms.v1_8.packet;

import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.packet.NmsPlayOutMapChunk;
import club.abelix.api.nms.packet.NmsPlayOutMapChunkBulk;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.action.TeamAction;
import club.abelix.commons.data.skin.Skin;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import lombok.val;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunkBulk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PacketLibraryApi1_8 implements PacketLibraryApi {

    @Override
    public void sendPacket(Player player, Object packet) {
        if (packet instanceof Packet) { //nms
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket((Packet<?>) packet);
        } else if (packet instanceof PacketContainer) {
            try {
                PROTOCOL_MANAGER.sendServerPacket(player, (PacketContainer) packet);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public NmsPlayOutMapChunk wrapChunk(PacketContainer container) {
        return new NmsPlayOutMapChunk1_8((PacketPlayOutMapChunk) container.getHandle());
    }

    @Override
    public NmsPlayOutMapChunkBulk wrapChunkBulk(PacketContainer container) {
        return new NmsPlayOutMapBulkChunk1_8((PacketPlayOutMapChunkBulk) container.getHandle());
    }

    @Override
    public PacketContainer getEntityEquipmentPacket(int entityId, EnumWrappers.ItemSlot itemSlot, ItemStack itemStack) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getIntegers().write(1, itemSlot.ordinal() >= 1 ? itemSlot.ordinal() - 1 : itemSlot.ordinal());
        packetContainer.getItemModifier().write(0, itemStack);

        return packetContainer;
    }

    @Override
    public PacketContainer getEntityEffect(int entityId, int potionId, int amplifier, int duration, boolean hideParticles) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_EFFECT);

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getBytes().write(0, (byte) (potionId & 255));
        packetContainer.getBytes().write(1, (byte) (amplifier & 255));
        packetContainer.getIntegers().write(1, duration);
        packetContainer.getBytes().write(2, (byte) (hideParticles ? 0 : 1));

        return packetContainer;
    }

    @Override
    public PacketContainer getRemoveEntityEffect(int entityId, int potionId) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.REMOVE_ENTITY_EFFECT);

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getIntegers().write(0, potionId);

        return packetContainer;
    }

    @Override
    public PacketContainer getEntityTeleportPacket(int entityId, Location location) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

        packetContainer.getIntegers().write(0, entityId);

        packetContainer.getIntegers().write(1, (int) (location.getX() * 32D));
        packetContainer.getIntegers().write(2, (int) (location.getY() * 32D));
        packetContainer.getIntegers().write(3, (int) (location.getZ() * 32D));

        packetContainer.getBytes().write(0, (byte) (location.getYaw() * 256.0F / 360.0F));
        packetContainer.getBytes().write(1, (byte) (location.getPitch() * 256.0F / 360.0F));

        return packetContainer;
    }

    @Override
    public PacketContainer getEntityLookPacket(int entityId, Location location) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_LOOK);

        packetContainer.getIntegers().write(0, entityId);

        packetContainer.getBytes().write(3, (byte) (location.getYaw() * 256.0F / 360.0F));
        packetContainer.getBytes().write(4, (byte) (location.getPitch() * 256.0F / 360.0F));

        return packetContainer;
    }

    @Override
    public PacketContainer getHeadRotationPacket(int entityId, float yaw) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);

        packetContainer.getIntegers().write(0, entityId);

        packetContainer.getBytes().write(0, (byte) ((int) (yaw * 256.0F / 360.0F)));

        return packetContainer;
    }

    @Override
    public PacketContainer getSpawnLivingEntityPacket(int entityId,
                                                      EntityType entityType, Location location,
                                                      WrappedDataWatcher dataWatcher) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SPAWN_ENTITY_LIVING);

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getIntegers().write(1, (int) entityType.getTypeId());

        packetContainer.getIntegers().write(2, (int) Math.floor(location.getX() * 32.0D));
        packetContainer.getIntegers().write(3, (int) Math.floor(location.getY() * 32.0D));
        packetContainer.getIntegers().write(4, (int) Math.floor(location.getZ() * 32.0D));

        packetContainer.getBytes().write(0, (byte) (location.getYaw() * 256.0F / 360.0F));
        packetContainer.getBytes().write(1, (byte) (location.getPitch() * 256.0F / 360.0F));

        packetContainer.getDataWatcherModifier().write(0, dataWatcher);

        return packetContainer;
    }

    @Override
    public PacketContainer getSpawnEntityPacket(int entityId, int spawnTypeId, int entityData, Location location, Vector velocity) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getIntegers().write(9, spawnTypeId);
        packetContainer.getIntegers().write(10, entityData);

        packetContainer.getIntegers().write(1, (int) Math.floor(location.getX() * 32.0D));
        packetContainer.getIntegers().write(2, (int) Math.floor(location.getY() * 32.0D));
        packetContainer.getIntegers().write(3, (int) Math.floor(location.getZ() * 32.0D));

        if (entityData > 0) {
            packetContainer.getIntegers().write(4, (int) (velocity.getX() * 8000.0D));
            packetContainer.getIntegers().write(5, (int) (velocity.getY() * 8000.0D));
            packetContainer.getIntegers().write(6, (int) (velocity.getZ() * 8000.0D));
        }

        packetContainer.getIntegers().write(7, (int) (location.getYaw() * 256.0F / 360.0F));
        packetContainer.getIntegers().write(8, (int) (location.getPitch() * 256.0F / 360.0F));

        return packetContainer;
    }

    @Override
    public PacketContainer getSpawnNamedEntityPacket(int entityId, UUID uuid, Location location, WrappedDataWatcher dataWatcher) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);

        packetContainer.getIntegers().write(0, entityId);

        packetContainer.getSpecificModifier(UUID.class).write(0, uuid);

        packetContainer.getIntegers().write(1, (int) Math.floor(location.getX() * 32.0D));
        packetContainer.getIntegers().write(2, (int) Math.floor(location.getY() * 32.0D));
        packetContainer.getIntegers().write(3, (int) Math.floor(location.getZ() * 32.0D));

        packetContainer.getBytes().write(0, (byte) (location.getYaw() * 256.0F / 360.0F));
        packetContainer.getBytes().write(1, (byte) (location.getPitch() * 256.0F / 360.0F));

        packetContainer.getDataWatcherModifier().write(0, dataWatcher);

        return packetContainer;
    }

    @Override
    public PacketContainer getPlayerInfoPacket(EnumWrappers.PlayerInfoAction action, FakeEntityPlayer fakeEntityPlayer) {
        WrappedGameProfile wrappedGameProfile
                = new WrappedGameProfile(fakeEntityPlayer.getUUID(), fakeEntityPlayer.getName());

        Skin skin = fakeEntityPlayer.getSkin();

        if (skin != null) {
            wrappedGameProfile.getProperties().put("textures", new WrappedSignedProperty("textures",
                    skin.getValue(), skin.getSignature()));
        }

        PlayerInfoData playerInfoData = new PlayerInfoData(wrappedGameProfile, 0,
                EnumWrappers.NativeGameMode.ADVENTURE, null);

        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.PLAYER_INFO);

        packetContainer.getPlayerInfoAction().write(0, action);

        val playerInfoList = new ArrayList<PlayerInfoData>(1);
        playerInfoList.add(playerInfoData);

        packetContainer.getPlayerInfoDataLists().write(0, playerInfoList);

        return packetContainer;
    }

    @Override
    public PacketContainer getTeamPacket(TeamAction teamAction, FakeTeam fakeTeam, @NotNull List<String> players) {
        PacketContainer packetContainer = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);

        packetContainer.getStrings().write(0, fakeTeam.getName());
        packetContainer.getStrings().write(4, fakeTeam.getTagVisibility().getRuleName());

        packetContainer.getIntegers().write(0, fakeTeam.getColorIndex());
        packetContainer.getIntegers().write(1, teamAction.ordinal());
        packetContainer.getIntegers().write(2, (int) fakeTeam.getMinecraftTeamFlags());

        if (teamAction == TeamAction.CREATE || teamAction == TeamAction.UPDATE) {
            packetContainer.getStrings().write(1, fakeTeam.getName());

            packetContainer.getStrings().write(2, fakeTeam.getPrefix() == null ? "" : fakeTeam.getPrefix());
            packetContainer.getStrings().write(3, fakeTeam.getSuffix() == null ? "" : fakeTeam.getSuffix());
        }

        packetContainer.getSpecificModifier(Collection.class).write(0, players);

        return packetContainer;
    }

    @Override
    public PacketContainer getEntityAttachmentPacket(int attachedEntityId, int holdingEntityId) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ATTACH_ENTITY);

        // 0 - leash
        packetContainer.getIntegers().write(1, attachedEntityId);
        packetContainer.getIntegers().write(2, holdingEntityId);

        return packetContainer;
    }

}
