package club.abelix.api.nms.v1_8.player;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.player.PlayerApi;
import club.abelix.api.nms.reflection.ReflectionUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftArrow;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaBook;
import org.bukkit.craftbukkit.v1_8_R3.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftChatMessage;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.SpawnEgg;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeWrapper;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


@Slf4j
@RequiredArgsConstructor
public class PlayerApi1_8 implements PlayerApi {

    private static final Field ITEM_IN_USE;
    private static final Method CLEAR_ITEM_IN_USE;

    static {
        Field itemInUseField = null;
        for (String name : new String[]{"itemInUse", "bY", "bZ", "bV", "bW", "bX", "cb", "ca"}) {
            try {
                Field f = EntityLiving.class.getDeclaredField(name);
                f.setAccessible(true);
                itemInUseField = f;
                break;
            } catch (Throwable ignored) {}
        }
        if (itemInUseField == null) {
            for (Field f : EntityLiving.class.getDeclaredFields()) {
                if (f.getType() == net.minecraft.server.v1_8_R3.ItemStack.class) {
                    f.setAccessible(true);
                    itemInUseField = f;
                    break;
                }
            }
        }
        ITEM_IN_USE = itemInUseField;

        Method clearMethod = null;
        for (String name : new String[]{"clearItemInUse", "bA", "bB", "da", "db", "bn", "bm", "bk"}) {
            try {
                Method m = EntityLiving.class.getDeclaredMethod(name);
                m.setAccessible(true);
                clearMethod = m;
                break;
            } catch (Throwable ignored) {}
        }
        CLEAR_ITEM_IN_USE = clearMethod;
    }

    private final NmsApi api;

    @Override
    public String getEffectLocaleName(@NonNull PotionEffectType type) {
        if (type instanceof PotionEffectTypeWrapper) {
            type = ((PotionEffectTypeWrapper) type).getType();
        }

        return "effect_" + ((CraftPotionEffectType) type).getHandle().a().substring(7);
    }

    @Override
    public void removeArrows(@NonNull Player player) {
        ((CraftPlayer) player).getHandle().getDataWatcher().watch(9, (byte) 0);
    }

    @Override
    public boolean canSeeChunk(Player player, int x, int z) {
        return ((CraftWorld) player.getWorld()).getHandle().getPlayerChunkMap().a(((CraftPlayer) player).getHandle(), x, z);
    }

    @Override
    public void changePlayerSkin(Player player, String value, String signature) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        EntityPlayer nmsPlayer = craftPlayer.getHandle();

        GameProfile profile = nmsPlayer.getProfile();
        profile.getProperties().removeAll("textures");
        profile.getProperties().put("textures", new Property("textures", value, signature));

        PacketPlayOutPlayerInfo packetRemoveTab
                = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, nmsPlayer);

        PacketPlayOutPlayerInfo packetAddTab
                = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, nmsPlayer);

        WorldServer worldServer = (WorldServer) nmsPlayer.world;
        PacketPlayOutRespawn packetRespawn = new PacketPlayOutRespawn(
                nmsPlayer.dimension,
                worldServer.getDifficulty(),
                worldServer.getWorldData().getType(),
                nmsPlayer.playerInteractManager.getGameMode()
        );
        ReflectionUtil.setField(packetRespawn, "c", false);

        Location location = player.getLocation();
        PacketPlayOutPosition packetPosition = new PacketPlayOutPosition(
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(),
                new java.util.HashSet<>()
        );

        nmsPlayer.playerConnection.sendPacket(packetRemoveTab);
        nmsPlayer.playerConnection.sendPacket(packetAddTab);
        nmsPlayer.playerConnection.sendPacket(packetRespawn);
        nmsPlayer.playerConnection.sendPacket(packetPosition);

        player.updateInventory();

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target == player) {
                continue;
            }
            CraftPlayer craftTarget = (CraftPlayer) target;
            craftTarget.getHandle().playerConnection.sendPacket(packetRemoveTab);
            craftTarget.getHandle().playerConnection.sendPacket(packetAddTab);
            target.hidePlayer(player);
            target.showPlayer(player);
        }
    }

    @Override
    public void changePlayerNick(@NonNull Player player, @NonNull String nick) {
        EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();

        PacketPlayOutPlayerInfo removePlayer
                = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, nmsPlayer);

        PacketPlayOutPlayerInfo addPlayer
                = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, nmsPlayer);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.canSee(player)) {
                continue;
            }

            CraftPlayer craftTarget = (CraftPlayer) target;
            EntityPlayer nmsTarget = craftTarget.getHandle();

            nmsTarget.playerConnection.sendPacket(removePlayer);
            nmsTarget.playerConnection.sendPacket(addPlayer);

            target.hidePlayer(player);
            target.showPlayer(player);
        }
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();

        PacketPlayOutTitle times;
        if (title != null) {
            times = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, CraftChatMessage.fromString(title)[0]);
            nmsPlayer.playerConnection.sendPacket(times);
        }

        if (subtitle != null) {
            times = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, CraftChatMessage.fromString(subtitle)[0]);
            nmsPlayer.playerConnection.sendPacket(times);
        }

        times = new PacketPlayOutTitle(fadeIn, stay, fadeOut);
        nmsPlayer.playerConnection.sendPacket(times);
    }

    @Override
    public void setArrowDamage(@NonNull Arrow arrow, double damage) {
        ReflectionUtil.setField(((CraftArrow) arrow).getHandle(), "damage", damage);
    }

    @Override
    public double getArrowDamage(@NonNull Arrow arrow) {
        return ReflectionUtil.getField(((CraftArrow) arrow).getHandle(), "damage");
    }

    @Override
    public void sendActionBar(Player player, String text) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(new ChatComponentText(text), (byte) 2));
    }

    @Override
    public String getCraftItemStackName(@NonNull ItemStack bukkitItemStack) {
        return CraftItemStack.asNMSCopy(bukkitItemStack).a() + "_name";
    }

    @Override
    public EntityType getSpawnEggType(@NonNull ItemStack itemStack) {
        return ((SpawnEgg) itemStack.getData()).getSpawnedType();
    }

    @Override
    public void setSpawnEggType(@NonNull ItemStack itemStack, @NonNull EntityType type) {
        SpawnEgg data = (SpawnEgg) itemStack.getData();
        data.setSpawnedType(type);
        itemStack.setData(data);
    }

    @Override
    public String getNmsEnchantmentName(Enchantment bukkitEnchantment) {
        net.minecraft.server.v1_8_R3.Enchantment nmsEnchantment
                = net.minecraft.server.v1_8_R3.Enchantment.getById(bukkitEnchantment.getId());

        return nmsEnchantment != null ? nmsEnchantment.a() : "";
    }

    @Override
    public String getNmsEntityName(@NonNull EntityType entity) {
        return EntityTypes.b(entity.getTypeId()) + "_name";
    }

    @Override
    public void setKiller(Player player, Player killer) {
        ((CraftPlayer) player).getHandle().killer = ((CraftPlayer) killer).getHandle();
    }

    @Override
    public ItemStack getPotion(PotionType type, boolean splash, boolean extended, boolean upgraded, int amount) {
        Potion potion = new Potion(type);
        potion.setLevel(upgraded ? 2 : 1);
        potion.setSplash(splash);

        if (!type.isInstant()) {
            potion.setHasExtendedDuration(extended);
        }

        return potion.toItemStack(amount);

    }

    @Override
    public void setLocation(Player player, Location location) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        entityPlayer.setLocation(
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()
        );

        ((CraftWorld) player.getWorld()).getHandle().getPlayerChunkMap().movePlayer(entityPlayer);
    }

    @Override
    public void addBookPage(ItemMeta meta, BaseComponent[] components) {
        CraftMetaBook metaBook = (CraftMetaBook) meta;

        metaBook.pages.add(IChatBaseComponent.ChatSerializer.a(ComponentSerializer.toString(components)));
    }

    @Override
    public void openBook(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        ItemStack oldHand = inventory.getItemInHand();

        inventory.setItemInHand(item);
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);

        EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        nmsPlayer.openBook(nmsStack);
        inventory.setItemInHand(oldHand);
    }

    @Override
    public void resetTitle(Player player) {
        player.resetTitle();
    }

    @Override
    public void updatePlayerEquipment(Player player) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        int entityId = entityPlayer.getId();

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target == player) {
                continue;
            }

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                PacketPlayOutEntityEquipment equipment
                        = new PacketPlayOutEntityEquipment(entityId, slot.ordinal(), entityPlayer.getEquipment(slot.ordinal()));

                api.getPacketLibraryApi().sendPacket(target, equipment);
            }
        }
    }


    @Override
    public void cancelItemUse(Player player) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        if (entityPlayer == null) return;
        try {
            if (CLEAR_ITEM_IN_USE != null) {
                CLEAR_ITEM_IN_USE.invoke(entityPlayer);
                return;
            }
            if (ITEM_IN_USE != null) {
                ITEM_IN_USE.set(entityPlayer, null);
                return;
            }
            log.error("BowFix: itemInUse field and clearItemInUse method not found in EntityLiving");
        } catch (Throwable e) {
            log.error("BowFix: failed to cancel item use for player {}", player.getName(), e);
        }
    }
}
