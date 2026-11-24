package club.abelix.api.nms.v1_16.packet;

import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.packet.NmsPlayOutMapChunk;
import club.abelix.api.nms.packet.NmsPlayOutMapChunkBulk;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.scoreboard.objective.BoardObjective;
import club.abelix.api.nms.scoreboard.objective.ObjectiveActionMode;
import club.abelix.api.nms.scoreboard.objective.ObjectiveType;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.action.TeamAction;
import club.abelix.commons.data.skin.Skin;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.*;
import lombok.NonNull;
import lombok.val;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketPlayOutMapChunk;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PacketLibraryApi1_16 implements PacketLibraryApi {

    private static final WrappedChatComponent EMPTY = WrappedChatComponent.fromText("");

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
        return new NmsPlayOutMapChunk1_16((PacketPlayOutMapChunk) container.getHandle());
    }

    @Override
    public NmsPlayOutMapChunkBulk wrapChunkBulk(PacketContainer container) {
        throw new UnsupportedOperationException();
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
        packetContainer.getEffectTypes().write(0, PotionEffectType.getById(potionId));

        return packetContainer;
    }

    @Override
    public PacketContainer getScoreboardObjectivePacket(BoardObjective objective, ObjectiveActionMode objectiveActionMode) {
        PacketContainer packetContainer = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);

        packetContainer.getStrings().write(0, objective.getName());

        packetContainer.getIntegers().write(0, objectiveActionMode.ordinal());

        if (objectiveActionMode == ObjectiveActionMode.CREATE || objectiveActionMode == ObjectiveActionMode.UPDATE) {
            packetContainer.getChatComponents().write(0, WrappedChatComponent.fromText(objective.getDisplayName()));
            packetContainer.getEnumModifier(ObjectiveType.class, 2).write(0, objective.getObjectiveType());
        }

        return packetContainer;
    }

    @Override
    public PacketContainer getEntityEquipmentPacket(int entityId, EnumWrappers.ItemSlot itemSlot, ItemStack itemStack) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getItemSlots().write(0, itemSlot);
        packetContainer.getItemModifier().write(0, itemStack);

        return packetContainer;
    }

    @Override
    public PacketContainer getEntityTeleportPacket(int entityId, Location location) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

        packetContainer.getIntegers().write(0, entityId);

        packetContainer.getDoubles().write(0, location.getX());
        packetContainer.getDoubles().write(1, location.getY());
        packetContainer.getDoubles().write(2, location.getZ());

        writeRotationFields(packetContainer, location);

        return packetContainer;
    }

    @Override
    public PacketContainer getEntityLookPacket(int entityId, Location location) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_LOOK);

        packetContainer.getIntegers().write(0, entityId);

        writeRotationFields(packetContainer, location);

        return packetContainer;
    }

    private void writeRotationFields(@NonNull PacketContainer container, @NonNull Location location) {
        container.getBytes().write(0, (byte) (location.getYaw() * 256.0F / 360.0F));
        container.getBytes().write(1, (byte) (location.getPitch() * 256.0F / 360.0F));

        container.getBooleans().write(0,
                location.getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR);
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
    public PacketContainer getSpawnEntityPacket(int entityId, int spawnTypeId, int entityData, Location location, Vector velocity) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getIntegers().write(6, entityData);

        packetContainer.getUUIDs().write(0, UUID.randomUUID());

        if (spawnTypeId == 32) {
            packetContainer.getSpecificModifier(EntityTypes.class).write(0, EntityTypes.ITEM);
        }

        packetContainer.getDoubles().write(0, location.getX());
        packetContainer.getDoubles().write(1, location.getY());
        packetContainer.getDoubles().write(2, location.getZ());

        if (entityData > 0) {
            packetContainer.getIntegers().write(1, (int) (velocity.getX() * 8000.0D));
            packetContainer.getIntegers().write(2, (int) (velocity.getY() * 8000.0D));
            packetContainer.getIntegers().write(3, (int) (velocity.getZ() * 8000.0D));
        }

        packetContainer.getIntegers().write(4, (int) (location.getPitch() * 256.0F / 360.0F));
        packetContainer.getIntegers().write(5, (int) (location.getYaw() * 256.0F / 360.0F));

        return packetContainer;
    }


    @SuppressWarnings("deprecation")
    @Override
    public PacketContainer getSpawnLivingEntityPacket(int entityId,
                                                      @NonNull EntityType entityType,
                                                      @NonNull Location location,
                                                      @NonNull WrappedDataWatcher dataWatcher) {

        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SPAWN_ENTITY_LIVING);

        //ебаный 1.13 с его перепутанными id...
        int id = IRegistry.ENTITY_TYPE.a(EntityTypes.a(entityType.getName()).get());

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getIntegers().write(1, id);

        packetContainer.getUUIDs().write(0, UUID.randomUUID());

        packetContainer.getDoubles().write(0, location.getX());
        packetContainer.getDoubles().write(1, location.getY());
        packetContainer.getDoubles().write(2, location.getZ());

        packetContainer.getBytes().write(0, (byte) (location.getYaw() * 256.0F / 360.0F));
        packetContainer.getBytes().write(1, (byte) (location.getPitch() * 256.0F / 360.0F));

        //в 1.15 в spawn living entity нет datawatcher'a?

        return packetContainer;
    }

    @Override
    public PacketContainer getSpawnNamedEntityPacket(int entityId,
                                                     @NonNull UUID uuid,
                                                     @NonNull Location location,
                                                     @NonNull WrappedDataWatcher dataWatcher) {

        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);

        packetContainer.getIntegers().write(0, entityId);

        packetContainer.getUUIDs().write(0, uuid);

        packetContainer.getDoubles().write(0, location.getX());
        packetContainer.getDoubles().write(1, location.getY());
        packetContainer.getDoubles().write(2, location.getZ());

        packetContainer.getBytes().write(0, (byte) (location.getYaw() * 256.0F / 360.0F));
        packetContainer.getBytes().write(1, (byte) (location.getPitch() * 256.0F / 360.0F));

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
    public PacketContainer getEntityAttachmentPacket(int attachedEntityId, int holdingEntityId) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.MOUNT);

        packetContainer.getIntegers().write(0, holdingEntityId);
        packetContainer.getIntegerArrays().write(0, new int[]{attachedEntityId});

        return packetContainer;
    }

    private final Class<?> enumChatFormat = MinecraftReflection.getMinecraftClass("EnumChatFormat");

    @Override
    public PacketContainer getTeamPacket(TeamAction teamAction, FakeTeam fakeTeam, @NotNull List<String> players) {
        ChatColor color = fakeTeam.getColor();

        PacketContainer packet = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);

        packet.getStrings().write(0, fakeTeam.getName());
        packet.getStrings().write(1, fakeTeam.getTagVisibility().getRuleName());

        packet.getIntegers().write(0, teamAction.ordinal());
        packet.getIntegers().write(1, (int) fakeTeam.getMinecraftTeamFlags());

        if (teamAction == TeamAction.CREATE || teamAction == TeamAction.UPDATE) {
            packet.getChatComponents().write(0,
                    WrappedChatComponent.fromText(fakeTeam.getName()));

            packet.getChatComponents().write(1, fakeTeam.getPrefixComponent());
            packet.getChatComponents().write(2, fakeTeam.getSuffixComponent());

            packet.getStrings().write(2, fakeTeam.getCollisionRule().getRuleName());
        }

        packet.getEnumModifier(ChatColor.class, enumChatFormat)
                .write(0, color);

        packet.getSpecificModifier(Collection.class).write(0, players);

        return packet;
    }
}
