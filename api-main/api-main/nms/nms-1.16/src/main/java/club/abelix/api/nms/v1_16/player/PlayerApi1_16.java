package club.abelix.api.nms.v1_16.player;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.player.PlayerApi;
import club.abelix.api.nms.reflection.ReflectionUtil;
import club.abelix.commons.util.math.MathUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftArrow;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftMetaBook;
import org.bukkit.craftbukkit.v1_16_R3.potion.CraftPotionEffectType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeWrapper;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PlayerApi1_16 implements PlayerApi {

    private static final Method STOP_ITEM_USE_1_16;

    static {
        Method m = null;
        for (String name : new String[]{"releaseActiveItem", "clearActiveItem", "stopItemUse", "bI", "bJ", "bK", "bL"}) {
            try {
                Method found = EntityLiving.class.getDeclaredMethod(name);
                found.setAccessible(true);
                m = found;
                break;
            } catch (Throwable ignored) {}
        }
        STOP_ITEM_USE_1_16 = m;
    }

    private final NmsApi api;

    @Override
    public String getEffectLocaleName(@NonNull PotionEffectType type) {
        if (type instanceof PotionEffectTypeWrapper) {
            type = ((PotionEffectTypeWrapper) type).getType();
        }

        return ((CraftPotionEffectType) type).getHandle().c();
    }

    @Override
    public boolean canSeeChunk(@NonNull Player player, int x, int z) {
        Location location = player.getLocation();
        int cx = location.getBlockX() >> 4;
        int cz = location.getBlockZ() >> 4;
        int v = player.getClientViewDistance();

        return Math.sqrt(MathUtil.square(x - cx) + MathUtil.square(z - cz)) <= v;
    }

    @Override
    public void removeArrows(@NonNull Player player) {
        // TODO: 21.03.2021 do this
    }

    @Override
    public void changePlayerSkin(@NonNull Player player, @NonNull String value, @NonNull String signature) {
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
                worldServer.getDimensionManager(),
                worldServer.getDimensionKey(),
                BiomeManager.a(worldServer.getSeed()),
                nmsPlayer.playerInteractManager.getGameMode(),
                nmsPlayer.playerInteractManager.c(),
                worldServer.isDebugWorld(),
                worldServer.isFlatWorld(),
                true
        );

        Location location = player.getLocation();
        PacketPlayOutPosition packetPosition = new PacketPlayOutPosition(
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(),
                new java.util.HashSet<>(), 0
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
            target.hidePlayer(api.getPlugin(), player);
            target.showPlayer(api.getPlugin(), player);
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
    public void setArrowDamage(@NonNull Arrow arrow, double damage) {
        ReflectionUtil.setField(((CraftArrow) arrow).getHandle(), "damage", damage);
    }

    @Override
    public double getArrowDamage(@NonNull Arrow arrow) {
        return ReflectionUtil.getField(((CraftArrow) arrow).getHandle(), "damage");
    }

    @Override
    public void sendTitle(@NonNull Player player, @NonNull String title, @NonNull String subTitle,
                          int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subTitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendActionBar(@NonNull Player player, @NonNull String text) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(new ChatComponentText(text), ChatMessageType.GAME_INFO, null));
    }

    @Override
    public String getCraftItemStackName(@NonNull ItemStack bukkitItemStack) {
        return CraftItemStack.asNMSCopy(bukkitItemStack).j();
    }

    @Override
    public EntityType getSpawnEggType(@NonNull ItemStack itemStack) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);

        NBTTagCompound tag = nmsItem.getTag();

        if (tag == null) {
            return EntityType.ZOMBIE;
        }


        EntityTypes<?> type = EntityTypes.a(tag.getCompound("EntityTag")).orElse(null);

        if (type == null) {
            return EntityType.ZOMBIE;
        }

        return EntityType.fromName(type.f());
    }

    @Override
    public void setSpawnEggType(@NonNull ItemStack itemStack, @NonNull EntityType type) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);

        NBTTagCompound tag = nmsItem.getTag();

        NBTTagCompound entityTag = tag.getCompound("EntityTag");
        entityTag.setString("id", type.getKey().getKey());

        nmsItem.setTag(tag);
    }

    @Override
    public String getNmsEnchantmentName(@NonNull Enchantment bukkitEnchantment) {
        net.minecraft.server.v1_16_R3.Enchantment nmsEnchantment
                = ((CraftEnchantment) bukkitEnchantment).getHandle();

        return nmsEnchantment != null ? nmsEnchantment.g() : "";
    }

    @Override
    public String getNmsEntityName(@NonNull EntityType type) {
        EntityTypes<?> types = EntityTypes.a(type.getName()).orElse(null);

        if (types == null) {
            return type.getEntityClass().getSimpleName();
        }

        return types.f();
    }

    @Override
    public void setKiller(@NonNull Player player, @NonNull Player killer) {
        ((CraftPlayer) player).getHandle().killer = ((CraftPlayer) killer).getHandle();
    }

    @Override
    public ItemStack getPotion(PotionType type, boolean splash, boolean extended, boolean upgraded, int amount) {
        ItemStack itemStack = new ItemStack(splash ? Material.SPLASH_POTION : Material.POTION);

        PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
        potionMeta.setBasePotionData(new PotionData(type, extended, upgraded));

        itemStack.setItemMeta(potionMeta);

        return itemStack;
    }

    @Override
    public void setLocation(Player player, Location location) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        entityPlayer.setLocation(
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()
        );

        ((CraftWorld) player.getWorld()).getHandle()
                .getChunkProvider().playerChunkMap.movePlayer(entityPlayer);
    }

    @Override
    public void addBookPage(ItemMeta meta, BaseComponent[] components) {
        CraftMetaBook metaBook = (CraftMetaBook) meta;

        if (components != null) {
            metaBook.addPage(ComponentSerializer.toString(components));
        }

//        val component = IChatBaseComponent.ChatSerializer.a(ComponentSerializer.toString(components));
//
//        if (component != null) {
//            metaBook.spigot().addPage(component);
//        }
    }

    @Override
    public void openBook(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        ItemStack oldHand = inventory.getItemInHand();

        inventory.setItemInMainHand(item);
        net.minecraft.server.v1_16_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);

        EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        nmsPlayer.openBook(nmsStack, EnumHand.MAIN_HAND);
        inventory.setItemInMainHand(oldHand);
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

            List<Pair<EnumItemSlot, net.minecraft.server.v1_16_R3.ItemStack>> equipment
                    = new ArrayList<>();

            for (EnumItemSlot slot : EnumItemSlot.values()) {
                equipment.add(new Pair<>(slot, entityPlayer.getEquipment(slot)));
            }

            PacketPlayOutEntityEquipment entityEquipment
                    = new PacketPlayOutEntityEquipment(entityId, equipment);

            api.getPacketLibraryApi().sendPacket(target, entityEquipment);
        }
    }

    @Override
    public void cancelItemUse(Player player) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        if (!entityPlayer.isHandRaised()) return;
        try {
            if (STOP_ITEM_USE_1_16 != null) {
                STOP_ITEM_USE_1_16.invoke(entityPlayer);
                return;
            }
            log.error("BowFix: stopItemUse method not found in EntityLiving for 1.16");
        } catch (Throwable e) {
            log.error("BowFix: failed to cancel item use for player {}", player.getName(), e);
        }
    }
}
