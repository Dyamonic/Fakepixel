package net.fakepixel.housing.theme;

import java.util.Collections;
import java.util.List;

public final class ThemeDefinition {
    private final String id,displayName,icon,unlockType,permission,biome,sky,weather;
    private final int price;
    private final ThemeTemplateDefinition template;
    private final List<String> lore;
    public ThemeDefinition(String id,String displayName,String icon,String unlockType,int price,String permission,ThemeTemplateDefinition template,String biome,String sky,String weather,List<String> lore){this.id=id;this.displayName=displayName;this.icon=icon;this.unlockType=unlockType;this.price=price;this.permission=permission;this.template=template;this.biome=biome;this.sky=sky;this.weather=weather;this.lore=Collections.unmodifiableList(lore);}
    public String getId(){return id;} public String getDisplayName(){return displayName;} public String getIcon(){return icon;} public String getUnlockType(){return unlockType;} public int getPrice(){return price;} public String getPermission(){return permission;} public ThemeTemplateDefinition getTemplate(){return template;} public String getBiome(){return biome;} public String getSky(){return sky;} public String getWeather(){return weather;} public List<String> getLore(){return lore;}
}
