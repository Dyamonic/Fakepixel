package net.fakepixel.housing.metadata;

import net.fakepixel.housing.database.HousingDatabaseManager;
import java.sql.*;
import java.util.*;

public final class HouseSummaryRepository {
    private final HousingDatabaseManager database;
    public HouseSummaryRepository(HousingDatabaseManager database){this.database=database;}
    public List<HouseSummary> loadAll()throws SQLException{List<HouseSummary> result=new ArrayList<>();String sql="SELECT owner_uuid,house_id,name,cookies,weekly_cookies,plot_size,visibility FROM housing_houses";try(Connection c=database.connection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){while(r.next())result.add(new HouseSummary(UUID.fromString(r.getString(1)),r.getString(2),r.getString(3),r.getInt(4),r.getInt(5),r.getInt(6),r.getString(7)));}return result;}
}
