package club.abelix.api.nms.v1_8.entity.custom.fireball;

import club.abelix.api.nms.entity.custom.fireball.NmsFireball;
import net.minecraft.server.v1_8_R3.EntityLargeFireball;
import net.minecraft.server.v1_8_R3.World;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Fireball;
import org.bukkit.util.Vector;

/**
 * @author Unidentified Person
 */
public class NmsFireball1_8 extends EntityLargeFireball implements NmsFireball {

    private boolean disableRandomDirection = true;

    public NmsFireball1_8(World world, CraftPlayer player, Location location, Vector vector) {
        super(world, player.getHandle(), vector.getX(), vector.getY(), vector.getZ());

        setLocation(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        setDirection(vector.getX(), vector.getY(), vector.getZ());
    }

    @Override
    public final void setDirection(double d0, double d1, double d2) {
        if (disableRandomDirection) {
            this.dirX = d0 * 0.1;
            this.dirY = d1 * 0.1;
            this.dirZ = d2 * 0.1;
        } else {
            super.setDirection(d0, d1, d2);
        }
    }

    @Override
    public void disableRandomDirection() {
        disableRandomDirection = true;
    }

    @Override
    public Fireball asBukkitEntity() {
        return (Fireball) getBukkitEntity();
    }
}
