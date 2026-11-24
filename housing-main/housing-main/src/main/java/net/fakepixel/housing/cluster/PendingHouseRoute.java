package net.fakepixel.housing.cluster;

import java.util.UUID;

public final class PendingHouseRoute {
    private final UUID playerUuid;
    private final PendingRouteAction action;
    private final String houseId, targetServer;
    public PendingHouseRoute(UUID playerUuid,PendingRouteAction action,String houseId,String targetServer){this.playerUuid=playerUuid;this.action=action;this.houseId=houseId;this.targetServer=targetServer;}
    public UUID getPlayerUuid(){return playerUuid;} public PendingRouteAction getAction(){return action;}
    public String getHouseId(){return houseId;} public String getTargetServer(){return targetServer;}
}
