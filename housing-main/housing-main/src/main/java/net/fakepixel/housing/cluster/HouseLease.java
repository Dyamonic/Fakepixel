package net.fakepixel.housing.cluster;

public final class HouseLease {
    private final String houseId, lockedBy;
    private final long expiresAt;
    public HouseLease(String houseId,String lockedBy,long expiresAt){this.houseId=houseId;this.lockedBy=lockedBy;this.expiresAt=expiresAt;}
    public String getHouseId(){return houseId;} public String getLockedBy(){return lockedBy;} public long getExpiresAt(){return expiresAt;}
}
