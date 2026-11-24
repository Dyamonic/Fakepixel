package club.abelix.games.fakevival.furniture;

import org.bukkit.Material;

public record FurnitureCollisionBlock(int x,int y,int z,Material material) {
    public FurnitureCollisionBlock(int x,int y,int z){this(x,y,z,Material.BARRIER);}
    public FurnitureCollisionBlock {if(material==null||!material.isBlock())material=Material.BARRIER;}
}
