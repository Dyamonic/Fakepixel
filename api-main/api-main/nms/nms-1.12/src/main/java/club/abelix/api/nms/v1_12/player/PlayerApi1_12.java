package club.abelix.api.nms.v1_12.player;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.player.PlayerApi;
import club.abelix.api.nms.reflection.ReflectionUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftArrow;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftMetaBook;
import org.bukkit.craftbukkit.v1_12_R1.potion.CraftPotionEffectType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeWrapper;
import org.bukkit.potion.PotionType;


@RequiredArgsConstructor
public class PlayerApi1_12 implements PlayerApi {

    private final NmsApi api;

    @Override
    public String getEffectLocaleName(@NonNull PotionEffectType type) {
        if (type instanceof PotionEffectTypeWrapper) {
            type = ((PotionEffectTypeWrapper) type).getType();
        }

        return "effect_" + ((CraftPotionEffectType) type).getHandle().a().substring(7);
    }

    @Override
    public boolean canSeeChunk(@NonNull Player player, int x, int z) {
        return ((CraftWorld) player.getWorld()).getHandle().getPlayerChunkMap().a(((CraftPlayer) player).getHandle(), x, z);
    }

    @Override
    public void removeArrows(@NonNull Player player) {
        // TODO: 21.03.2021 later
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
                nmsPlayer.dimension,
                worldServer.getDifficulty(),
                worldServer.getWorldData().getType(),
                nmsPlayer.playerInteractManager.getGameMode()
        );
        ReflectionUtil.setField(packetRespawn, "c", false);

        Location location = player.getLocation();

        nmsPlayer.playerConnection.sendPacket(packetRemoveTab);
        nmsPlayer.playerConnection.sendPacket(packetAddTab);
        nmsPlayer.playerConnection.sendPacket(packetRespawn);
        nmsPlayer.playerConnection.a(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), new java.util.HashSet<>());

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
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(new ChatComponentText(text), ChatMessageType.GAME_INFO));
    }

    @Override
    public String getCraftItemStackName(@NonNull ItemStack bukkitItemStack) {
        return CraftItemStack.asNMSCopy(bukkitItemStack).a() + "_name";
    }

    @Override
    public EntityType getSpawnEggType(@NonNull ItemStack itemStack) {
        SpawnEggMeta meta = (SpawnEggMeta) itemStack.getItemMeta();

        return meta.getSpawnedType();
    }

    @Override
    public void setSpawnEggType(@NonNull ItemStack itemStack, @NonNull EntityType type) {
        SpawnEggMeta meta = (SpawnEggMeta) itemStack.getItemMeta();
        meta.setSpawnedType(type);

        itemStack.setItemMeta(meta);
    }

    @Override
    public String getNmsEnchantmentName(@NonNull Enchantment bukkitEnchantment) {
        net.minecraft.server.v1_12_R1.Enchantment nmsEnchantment
                = net.minecraft.server.v1_12_R1.Enchantment.c(bukkitEnchantment.getId());

        return nmsEnchantment != null ? nmsEnchantment.a() : "";
    }

    @Override
    public String getNmsEntityName(@NonNull EntityType entity) {
        return EntityTypes.a(new MinecraftKey(entity.getName())) + "_name";
    }

    @Override
    public void setKiller(@NonNull Player player, @NonNull Player killer) {
        ((CraftPlayer) player).getHandle().killer = ((CraftPlayer) killer).getHandle();
    }

    @Override
    public ItemStack getPotion(PotionType type, boolean splash, boolean extended, boolean upgraded, int amount) {
        ItemStack itemStack = new ItemStack(splash ? Material.SPLASH_POTION : Material.POTION, amount);

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

        entityPlayer.x().getPlayerChunkMap().movePlayer(entityPlayer);
    }

    @Override
    public void addBookPage(ItemMeta meta, BaseComponent[] components) {
        CraftMetaBook metaBook = (CraftMetaBook) meta;

        metaBook.pages.add(IChatBaseComponent.ChatSerializer.b(ComponentSerializer.toString(components)));
    }

    @Override
    public void openBook(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        ItemStack oldHand = inventory.getItemInMainHand();

        inventory.setItemInMainHand(item);
        net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);

        EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        nmsPlayer.a(nmsStack, EnumHand.MAIN_HAND);
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

            for (EnumItemSlot slot : EnumItemSlot.values()) {
                PacketPlayOutEntityEquipment equipment
                        = new PacketPlayOutEntityEquipment(entityId, slot, entityPlayer.getEquipment(slot));

                api.getPacketLibraryApi().sendPacket(target, equipment);
            }
        }
    }

    @Override
    public void cancelItemUse(Player player) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        try {
            java.lang.reflect.Method isHandRaised = findMethod(entityPlayer.getClass(), "isHandRaised");
            if (isHandRaised == null) return;
            isHandRaised.setAccessible(true);
            if (!(boolean) isHandRaised.invoke(entityPlayer)) return;
            java.lang.reflect.Method stopMethod = findMethod(entityPlayer.getClass(), "stopItemUse");
            if (stopMethod != null) {
                stopMethod.setAccessible(true);
                stopMethod.invoke(entityPlayer);
            }
        } catch (Exception ignored) {
        }
    }


    private static java.lang.reflect.Method findMethod(Class<?> cls, String name) {
        while (cls != null) {
            try {
                return cls.getDeclaredMethod(name);
            } catch (NoSuchMethodException e) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }
}
