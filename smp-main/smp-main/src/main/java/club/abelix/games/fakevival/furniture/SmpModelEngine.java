package club.abelix.games.fakevival.furniture;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import java.util.List;

public final class SmpModelEngine {
    private static final Gson GSON = new Gson();

    public String serialize(SmpFurnitureModel model) { JsonObject root=(JsonObject)GSON.toJsonTree(model);root.addProperty("format","smp_model_v1");return GSON.toJson(root); }

    public SmpFurnitureModel parse(FurnitureDefinition definition) {
        try {
            JsonObject root=JsonParser.parseString(definition.entityJson()).getAsJsonObject();
            if (root.has("parts")) return GSON.fromJson(root,SmpFurnitureModel.class);
            SmpModelPart legacy=legacyPart(root);
            return new SmpFurnitureModel(definition.id(),definition.displayName(),definition.category(),definition.previewMaterial(),definition.price(),
                    List.of(legacy),1,2,SmpFurnitureModel.PlacementType.FLOOR,SmpFurnitureModel.InteractionType.NONE);
        } catch(Exception ignored) { return new SmpFurnitureModel(definition.id(),definition.displayName(),definition.category(),definition.previewMaterial(),definition.price(),List.of(),1,1,null,null); }
    }

    private SmpModelPart legacyPart(JsonObject root) {
        String material="ARMOR_STAND"; EquipmentSlot slot=EquipmentSlot.HEAD;
        for(String key:List.of("helmet","main_hand","off_hand","chestplate","leggings","boots")) if(root.has(key)){material=root.get(key).getAsString();slot=switch(key){case"main_hand"->EquipmentSlot.HAND;case"off_hand"->EquipmentSlot.OFF_HAND;case"chestplate"->EquipmentSlot.CHEST;case"leggings"->EquipmentSlot.LEGS;case"boots"->EquipmentSlot.FEET;default->EquipmentSlot.HEAD;};break;}
        Material type=Material.matchMaterial(material); if(type==null)type=Material.ARMOR_STAND;
        return new SmpModelPart(type,SmpModelPart.EntityType.ARMOR_STAND,slot,0,0,0,0,0,0,1,
                !bool(root,"invisible",true),bool(root,"small",false),bool(root,"marker",false),bool(root,"gravity",false),bool(root,"base_plate",false),bool(root,"arms",false));
    }
    private static boolean bool(JsonObject o,String key,boolean fallback){try{return o.has(key)?o.get(key).getAsBoolean():fallback;}catch(Exception ignored){return fallback;}}
}
