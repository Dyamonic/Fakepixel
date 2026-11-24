package club.abelix.api.nms.player;

import lombok.NonNull;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public interface PlayerApi {

    /**
     * Проверить, может ли игрок видеть чанк
     */
    boolean canSeeChunk(@NonNull Player player, int x, int z);

    /**
     * Поменять скин игроку
     */
    void changePlayerSkin(@NonNull Player player, @NonNull String value, @NonNull String signature);

    /**
     * Поменять ник игроку
     */
    void changePlayerNick(@NonNull Player player, @NonNull String nick);

    // TODO: 30.03.2021 перенести в WorldApi, там была проблема с 1.16 :/
    void setArrowDamage(@NonNull Arrow arrow, double damage);

    double getArrowDamage(@NonNull Arrow arrow);

    /**
     * Отправить тайтл игроку
     */
    default void sendTitle(@NonNull Player player, @NonNull String title, @NonNull String subTitle) {
        sendTitle(player, title, subTitle, 0, 40, 20);
    }

    /**
     * Отправить тайтл игроку
     */
    void sendTitle(@NonNull Player player, @NonNull String title, String subTitle, int fadeIn, int stay, int fadeOut);

    /**
     * Отправить экшен бар игроку
     */
    void sendActionBar(@NonNull Player player, @NonNull String text);

    String getEffectLocaleName(@NonNull PotionEffectType type);

    /**
     * Получить имя предмета
     */
    String getCraftItemStackName(@NonNull ItemStack itemStack);

    EntityType getSpawnEggType(@NonNull ItemStack itemStack);

    void setSpawnEggType(@NonNull ItemStack itemStack, @NonNull EntityType type);

    /**
     * Получить имя зачарования
     */
    String getNmsEnchantmentName(@NonNull Enchantment enchantment);

    String getNmsEntityName(@NonNull EntityType entity);

    /**
     * Установить убийцу игроку
     */
    void setKiller(@NonNull Player player, @NonNull Player killer);

    void removeArrows(@NonNull Player player);

    ItemStack getPotion(@NonNull PotionType potionType, boolean splash, boolean extended, boolean upgraded, int amount);

    void setLocation(Player player, Location location);

    void addBookPage(ItemMeta meta, BaseComponent[] components);

    void openBook(Player player, ItemStack item);

    void resetTitle(Player player);

    void updatePlayerEquipment(Player player);

    void cancelItemUse(Player player);

}
