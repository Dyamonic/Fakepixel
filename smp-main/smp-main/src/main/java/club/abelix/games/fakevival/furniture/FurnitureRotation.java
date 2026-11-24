package club.abelix.games.fakevival.furniture;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public final class FurnitureRotation {
    private FurnitureRotation() {}

    public static int cardinal(BlockFace face) {
        return switch (face) {case NORTH->0;case EAST->90;case SOUTH->180;case WEST->270;default->0;};
    }

    public static int snap(float yaw) {
        return Math.floorMod(Math.round(yaw/90f)*90,360);
    }

    public static Vector rotate(Vector offset,int degrees) {
        double x=offset.getX(),y=offset.getY(),z=offset.getZ();
        return switch(Math.floorMod(degrees,360)) {
            case 0->new Vector(x,y,z);case 90->new Vector(-z,y,x);case 180->new Vector(-x,y,-z);case 270->new Vector(z,y,-x);
            default->{double radians=Math.toRadians(degrees),sin=Math.sin(radians),cos=Math.cos(radians);yield new Vector(x*cos-z*sin,y,x*sin+z*cos);}
        };
    }

    public static float normalize(float yaw){return (float)Math.floorMod(Math.round(yaw*1000),360000)/1000f;}
    public static float toBukkitYaw(float modelYaw){return normalize(modelYaw+180f);}
}
