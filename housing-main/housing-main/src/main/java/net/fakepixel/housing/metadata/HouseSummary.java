package net.fakepixel.housing.metadata;

import java.util.UUID;

public final class HouseSummary {
    private final UUID owner; private final String houseId,name,visibility; private final int cookies,weeklyCookies,plotSize;
    public HouseSummary(UUID owner,String houseId,String name,int cookies,int weeklyCookies,int plotSize,String visibility){this.owner=owner;this.houseId=houseId;this.name=name;this.cookies=cookies;this.weeklyCookies=weeklyCookies;this.plotSize=plotSize;this.visibility=visibility;}
    public UUID getOwner(){return owner;} public String getHouseId(){return houseId;} public String getName(){return name;}
    public int getCookies(){return cookies;} public int getWeeklyCookies(){return weeklyCookies;} public int getPlotSize(){return plotSize;} public String getVisibility(){return visibility;}
}
