package club.abelix.api.base.util.item;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;

@UtilityClass
public class MaterialUtil {

    public boolean isLiquid(@NonNull Material type) {
        switch (type) {
            case WATER:
            case STATIONARY_WATER:
            case LAVA:
            case STATIONARY_LAVA: {
                return true;
            }
        }

        return false;
    }

    public boolean isPassable(@NonNull Material material) {
        return isPassable(material, 0);
    }

    public boolean isPassable(@NonNull Material material, int data) {
        return material == Material.WEB || isLiquid(material) || isFreelyPassable(material, data);
    }

    public boolean isFreelyPassable(@NonNull Material material) {
        return isFreelyPassable(material, 0);
    }

    public boolean isFreelyPassable(@NonNull Material material, int data) {
        switch (material) {
            case AIR:
            case SAPLING:
            case LONG_GRASS:
            case DEAD_BUSH:
            case RED_ROSE:
            case YELLOW_FLOWER:
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
            case TORCH:
            case FIRE:
            case LEVER:
            case GOLD_PLATE:
            case IRON_PLATE:
            case STONE_PLATE:
            case WOOD_PLATE:
            case STONE_BUTTON:
            case WOOD_BUTTON:
            case REDSTONE_TORCH_OFF:
            case REDSTONE_TORCH_ON:
            case VINE:
            case TRIPWIRE_HOOK:
            case DOUBLE_PLANT:
            case DIODE_BLOCK_OFF:
            case DIODE_BLOCK_ON:
            case REDSTONE_COMPARATOR_OFF:
            case REDSTONE_COMPARATOR_ON:
            case SIGN_POST:
            case WALL_SIGN:
            case STANDING_BANNER:
            case WALL_BANNER: {
                return true;
            }
            case ACACIA_FENCE_GATE:
            case BIRCH_FENCE_GATE:
            case DARK_OAK_FENCE_GATE:
            case FENCE_GATE:
            case JUNGLE_FENCE_GATE:
            case SPRUCE_FENCE_GATE: {
                return (data & 4) > 0;
            }
        }

        return false;
    }

    public boolean hasDurability(@NonNull Material material) {
        return !material.isBlock() && material.getData() != MaterialData.class && material.getMaxDurability() > 0;
    }

    public boolean isTool(@NonNull Material mat) {
        if (mat == Material.SHEARS) {
            return true;
        }

        return isPickaxe(mat) || isAxe(mat) || isSpade(mat) || isSword(mat);
    }

    public boolean isPickaxe(@NonNull Material material) {
        switch (material) {
            case DIAMOND_PICKAXE:
            case GOLD_PICKAXE:
            case IRON_PICKAXE:
            case STONE_PICKAXE:
            case WOOD_PICKAXE: {
                return true;
            }
        }

        return false;
    }

    public boolean isAxe(@NonNull Material material) {
        switch (material) {
            case DIAMOND_AXE:
            case GOLD_AXE:
            case IRON_AXE:
            case STONE_AXE:
            case WOOD_AXE: {
                return true;
            }
        }

        return false;
    }

    public boolean isSpade(@NonNull Material material) {
        switch (material) {
            case DIAMOND_SPADE:
            case GOLD_SPADE:
            case IRON_SPADE:
            case STONE_SPADE:
            case WOOD_SPADE: {
                return true;
            }
        }

        return false;
    }

    public boolean isSword(@NonNull Material material) {
        switch (material) {
            case DIAMOND_SWORD:
            case GOLD_SWORD:
            case IRON_SWORD:
            case STONE_SWORD:
            case WOOD_SWORD: {
                return true;
            }
        }

        return false;
    }

    public boolean isContainer(@NonNull Material material) {
        switch (material) {
            case ENDER_CHEST:
            case CHEST:
            case ENCHANTMENT_TABLE:
            case BREWING_STAND:
            case ANVIL:
                return true;
        }

        return false;
    }

    public boolean isArmor(@NonNull Material material) {
        return isHelmet(material) || isChestplate(material) || isLeggings(material) || isBoots(material);
    }

    public boolean isHelmet(@NonNull Material material) {
        switch (material) {
            case LEATHER_HELMET:
            case CHAINMAIL_HELMET:
            case GOLD_HELMET:
            case IRON_HELMET:
            case DIAMOND_HELMET:
                return true;
        }

        return false;
    }

    public boolean isChestplate(@NonNull Material material) {
        switch (material) {
            case LEATHER_CHESTPLATE:
            case CHAINMAIL_CHESTPLATE:
            case GOLD_CHESTPLATE:
            case IRON_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
                return true;
        }

        return false;
    }

    public boolean isLeggings(@NonNull Material material) {
        switch (material) {
            case LEATHER_LEGGINGS:
            case CHAINMAIL_LEGGINGS:
            case GOLD_LEGGINGS:
            case IRON_LEGGINGS:
            case DIAMOND_LEGGINGS:
                return true;
        }

        return false;
    }

    public boolean isBoots(@NonNull Material material) {
        switch (material) {
            case LEATHER_BOOTS:
            case CHAINMAIL_BOOTS:
            case GOLD_BOOTS:
            case IRON_BOOTS:
            case DIAMOND_BOOTS:
                return true;
        }

        return false;
    }

    public boolean isWearingArmorByType(@NonNull Player player, @NonNull ItemStack itemStack) {
        return getWearingArmorByType(player, itemStack) != null;
    }

    public void wearArmor(@NonNull Player player, @NonNull ItemStack itemStack) {
        Material material = itemStack.getType();

        PlayerInventory inventory = player.getInventory();

        if (isHelmet(material)) {
            inventory.setHelmet(itemStack);
        } else if (isChestplate(material)) {
            inventory.setChestplate(itemStack);
        } else if (isLeggings(material)) {
            inventory.setLeggings(itemStack);
        } else if (isBoots(material)) {
            inventory.setBoots(itemStack);
        }
    }

    public ItemStack getWearingArmorByType(@NonNull Player player, @NonNull ItemStack itemStack) {
        Material material = itemStack.getType();

        PlayerInventory inventory = player.getInventory();

        if (isHelmet(material)) {
            return inventory.getHelmet();
        }

        if (isChestplate(material)) {
            return inventory.getChestplate();
        }

        if (isLeggings(material)) {
            return inventory.getLeggings();
        }

        if (isBoots(material)) {
            return inventory.getBoots();
        }

        return null;
    }
}
