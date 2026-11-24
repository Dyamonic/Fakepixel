package club.abelix.api.nms.v1_12.entity.custom.fireball;

import club.abelix.api.nms.entity.custom.fireball.NmsFireball;
import net.minecraft.server.v1_12_R1.EntityLargeFireball;
import net.minecraft.server.v1_12_R1.World;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Fireball;
import org.bukkit.util.Vector;

/**
 * @author Unidentified Person
 */
public class NmsFireball1_12 extends EntityLargeFireball implements NmsFireball {

    private boolean disableRandomDirection;

    public NmsFireball1_12(World world, CraftPlayer player, Location location, Vector vector) {
        super(world, player.getHandle(), vector.getX(), vector.getY(), vector.getZ());

        setPosition(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public final void setDirection(double d0, double d1, double d2) {
        if (disableRandomDirection) {
            this.dirX = d0;
            this.dirY = d1;
            this.dirZ = d2;
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
