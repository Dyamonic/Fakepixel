package club.abelix.api.nms.world;

import club.abelix.api.nms.border.BorderColor;
import club.abelix.api.nms.entity.custom.fireball.NmsFireball;
import club.abelix.api.nms.nbt.tag.CompoundTag;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public interface WorldApi {

    NmsFireball launchFireball(Player player, Location location, Vector vector);

    /**
     * Удалить полностью все метадату у энтити
     *
     * @param entity - энтити
     */
    void removeEntityMetadata(@NonNull Entity entity);

    /**
     * Удалить полностью всю метадату у блока
     *
     * @param block - блок
     */
    void removeBlockMetadata(@NonNull Block block);

    /**
     * Установить заголовок сундуку
     */
    void setChestTitle(@NonNull Chest chest, @NonNull String title);

    /**
     * Открыть / закрыть сундук
     */
    void setChestOpenState(@NonNull Chest chest, boolean open);

    /**
     * Установить того кто поставил тнт
     */
    void setTntSource(@NonNull TNTPrimed tnt, @NonNull Player source);

    /**
     * Отправить игроку бордер с определённым центром, размером и цветом
     */
    void sendWorldBorder(@NonNull Player player, @NonNull Location center,
                         int size, @NonNull BorderColor borderColor);

    /**
     * Отправить игроку бордер с определённым цветом, размер и цветом через delay тиков
     *
     * @see WorldApi#sendWorldBorder(Player, Location, int, BorderColor)
     */
    void sendWorldBorder(@NonNull Player player, @NonNull Location center,
                         int size, @NonNull BorderColor borderColor, int delay);

    /**
     * Получить энтити из Bukkit по его ID
     */
    <T extends Entity> T getCraftEntityById(@NonNull World world, int entityId);

    /**
     * Получить энтити из NMS по его ID
     */
    <T> T getNmsEntityById(@NonNull World world, int entityId);

    /**
     * Подгрузить ID всех энтити в определённом мире
     */
    void loadWorldEntities(@NonNull World world);

    /**
     * Отгрузить ID всех энтити из определённого мира
     */
    void unloadWorldEntities(@NonNull World world);

    /**
     * Подгрузить NMS энтити
     * <p>
     * В мапу айди добавляется ид энтити и сам энтити
     * Нужно, чтобы получить ID энтити до его создания
     */
    void preloadEntity(@NonNull World world, @NonNull Object object);

    /**
     * Заспавнить эффекты смерти
     */
    void spawnDeathParticles(@NonNull Entity entity);

    void modifySpawnerType(@NonNull ItemStack egg, @NonNull EntityType type);

    void setBlockTag(Block block, CompoundTag tag);

    CompoundTag getBlockTag(Block block);

    void leakWorldUnload(@NonNull World world);

}
