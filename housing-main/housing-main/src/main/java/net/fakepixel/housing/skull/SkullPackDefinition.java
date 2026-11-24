package net.fakepixel.housing.skull;

import java.util.Collections;
import java.util.List;

public final class SkullPackDefinition {
    private final String id, displayName, icon, unlockType;
    private final int price;
    private final List<SkullPackHeadDefinition> heads;
    public SkullPackDefinition(String id,String displayName,String icon,String unlockType,int price,List<SkullPackHeadDefinition> heads){
        this.id=id;this.displayName=displayName;this.icon=icon;this.unlockType=unlockType;this.price=price;this.heads=Collections.unmodifiableList(heads);
    }
    public String getId(){return id;} public String getDisplayName(){return displayName;} public String getIcon(){return icon;}
    public String getUnlockType(){return unlockType;} public int getPrice(){return price;} public List<SkullPackHeadDefinition> getHeads(){return heads;}
}
