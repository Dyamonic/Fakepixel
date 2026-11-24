package net.fakepixel.housing.cluster;
public final class HouseRoute {
    private final String instanceId, serverName;
    public HouseRoute(String instanceId,String serverName){this.instanceId=instanceId;this.serverName=serverName;}
    public String getInstanceId(){return instanceId;} public String getServerName(){return serverName;}
}
