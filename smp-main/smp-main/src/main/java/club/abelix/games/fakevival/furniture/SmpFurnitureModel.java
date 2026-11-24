package club.abelix.games.fakevival.furniture;

import org.bukkit.Material;
import java.util.*;

public record SmpFurnitureModel(String id, String displayName, FurnitureDefinition.Category category,
                                Material previewMaterial, double price, List<SmpModelPart> parts,
                                double hitboxWidth, double hitboxHeight, PlacementType placementType,
                                InteractionType interactionType, List<FurnitureCollisionBlock> collisionBlocks,
                                List<FurnitureSitPoint> sitPoints,List<FurnitureSitPoint> bedPoints,boolean interactable) {
    public enum PlacementType { FLOOR, WALL, CEILING }
    public enum InteractionType { NONE, SIT, STORAGE, CUSTOM }

    public SmpFurnitureModel {
        parts = parts == null ? List.of() : List.copyOf(parts.subList(0, Math.min(80, parts.size())));
        if (category == null) category = FurnitureDefinition.Category.FURNITURE;
        if (previewMaterial == null || previewMaterial.isAir()) previewMaterial = Material.ARMOR_STAND;
        hitboxWidth = Math.clamp(hitboxWidth, .25, 8); hitboxHeight = Math.clamp(hitboxHeight, .25, 8);
        if (placementType == null) placementType = PlacementType.FLOOR;
        if (interactionType == null) interactionType = InteractionType.NONE;
        collisionBlocks=collisionBlocks==null?inferCollision(parts,hitboxWidth):List.copyOf(collisionBlocks);
        String seatKey=(id+" "+displayName).toLowerCase();boolean bed=category==FurnitureDefinition.Category.BEDS||seatKey.contains("bed");
        boolean seat=bed||category==FurnitureDefinition.Category.CHAIRS||seatKey.matches(".*(chair|bench|stool|sofa|couch).*" );
        FurnitureSitPoint.SeatType seatType=bed?FurnitureSitPoint.SeatType.BED:seatKey.contains("bench")||seatKey.contains("sofa")||seatKey.contains("couch")?FurnitureSitPoint.SeatType.BENCH:seatKey.contains("stool")?FurnitureSitPoint.SeatType.STOOL:FurnitureSitPoint.SeatType.CHAIR;
        sitPoints=sitPoints==null||sitPoints.isEmpty()&&!seat?List.of():sitPoints==null||sitPoints.isEmpty()?List.of(new FurnitureSitPoint(0,bed?.55:.45,0,0,seatType)):List.copyOf(sitPoints);
        bedPoints=bedPoints==null||bedPoints.isEmpty()&&!bed?List.of():bedPoints==null||bedPoints.isEmpty()?List.of(new FurnitureSitPoint(0,.55,0,0,FurnitureSitPoint.SeatType.BED)):List.copyOf(bedPoints);
        interactable=interactable||!sitPoints.isEmpty()||!bedPoints.isEmpty();
    }

    public SmpFurnitureModel(String id,String displayName,FurnitureDefinition.Category category,Material previewMaterial,double price,List<SmpModelPart> parts,double hitboxWidth,double hitboxHeight,PlacementType placementType,InteractionType interactionType){this(id,displayName,category,previewMaterial,price,parts,hitboxWidth,hitboxHeight,placementType,interactionType,null,null,null,false);}
    private static List<FurnitureCollisionBlock> inferCollision(List<SmpModelPart> parts,double width){if(width<1.15||parts==null||parts.isEmpty())return List.of();Set<String> cells=new LinkedHashSet<>();for(SmpModelPart part:parts){int x=(int)Math.round(part.x()),z=(int)Math.round(part.z());if(Math.abs(x)<=4&&Math.abs(z)<=4)cells.add(x+":"+z);}if(cells.isEmpty())cells.add("0:0");return cells.stream().limit(16).map(value->{String[] split=value.split(":");return new FurnitureCollisionBlock(Integer.parseInt(split[0]),0,Integer.parseInt(split[1]));}).toList();}
}
