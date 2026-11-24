package club.abelix.api.nms.entity.type.object;

import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

public interface FakeEntityItem extends FakeEntityObject {

    void setItemStack(@NonNull ItemStack itemStack);

    /**
     * @param face Сторона блока, на котором находится рамка
     * @return Лицевая сторона
     */
    static byte faceToDirection(@NonNull BlockFace face) {
        switch (face) {
            case SOUTH:
                return 0;
            case WEST:
                return 1;
            case NORTH:
                return 2;
            case EAST:
                return 3;
        }

        return 0;
    }

    static Location frameLocation1_8(@NonNull BlockFace face, @NonNull Location location) {
        switch (face) {
            case SOUTH -> location.setPitch(0F);
            case WEST -> location.setPitch(90F);
            case NORTH -> location.setPitch(180F);
            case EAST -> location.setPitch(270F);
        }

        return location;
    }

    static Location frameLocation1_12AndUpper(@NonNull BlockFace face, @NonNull Location location) { //todo переделать потом
        switch (face) {
            case SOUTH -> location.setYaw(0F);
            case WEST -> location.setYaw(90F);
            case NORTH -> location.setYaw(180F);
            case EAST -> location.setYaw(270F);
        }

        return location;
    }

}
