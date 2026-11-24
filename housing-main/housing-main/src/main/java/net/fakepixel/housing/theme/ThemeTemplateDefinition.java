package net.fakepixel.housing.theme;

public final class ThemeTemplateDefinition {
    private final String folderName;
    private final int minX,maxX,minY,maxY,minZ,maxZ;
    public ThemeTemplateDefinition(String folderName,int minX,int maxX,int minY,int maxY,int minZ,int maxZ){this.folderName=folderName;this.minX=minX;this.maxX=maxX;this.minY=minY;this.maxY=maxY;this.minZ=minZ;this.maxZ=maxZ;}
    public String getFolderName(){return folderName;} public int getMinX(){return minX;} public int getMaxX(){return maxX;} public int getMinY(){return minY;} public int getMaxY(){return maxY;} public int getMinZ(){return minZ;} public int getMaxZ(){return maxZ;}
}
