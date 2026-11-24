package club.abelix.games.fakevival.furniture;

public record FurnitureSitPoint(double x,double y,double z,float yaw,SeatType type,int maxRiders) {
    public enum SeatType {CHAIR,BED,BENCH,STOOL}
    public FurnitureSitPoint(double x,double y,double z,float yaw,SeatType type){this(x,y,z,yaw,type,1);}
    public FurnitureSitPoint {if(type==null)type=SeatType.CHAIR;maxRiders=Math.max(1,Math.min(4,maxRiders));}
}
