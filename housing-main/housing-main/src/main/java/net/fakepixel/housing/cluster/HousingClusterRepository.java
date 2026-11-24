package net.fakepixel.housing.cluster;

import net.fakepixel.housing.database.HousingDatabaseManager;
import java.sql.*;
import java.util.Collection;
import java.util.UUID;

public final class HousingClusterRepository {
    private final HousingDatabaseManager database;
    public HousingClusterRepository(HousingDatabaseManager database) { this.database = database; }

    public void heartbeat(HousingServerDefinition server, int players, Collection<String> loaded, long now) throws SQLException {
        String sql = "INSERT INTO housing_instances(server_id,role,player_count,loaded_house_count,max_loaded_houses,online,last_heartbeat) " +
                "VALUES(?,'INSTANCE',?,?,?,?,?) ON DUPLICATE KEY UPDATE role='INSTANCE',player_count=VALUES(player_count)," +
                "loaded_house_count=VALUES(loaded_house_count),max_loaded_houses=VALUES(max_loaded_houses),online=1,last_heartbeat=VALUES(last_heartbeat)";
        try (Connection c=database.connection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,server.getId()); p.setInt(2,players); p.setInt(3,loaded.size());
            p.setInt(4,HousingPerformanceRegistry.MAX_LOADED_HOUSES); p.setBoolean(5,true); p.setLong(6,now); p.executeUpdate();
        }
    }

    public HouseRoute route(String houseId, HousingServerDefinition local, long now) throws SQLException {
        try (Connection c=database.connection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p=c.prepareStatement("DELETE FROM housing_house_locks WHERE house_id=? AND lock_expires_at<=?")) { p.setString(1,houseId); p.setLong(2,now); p.executeUpdate(); }
                clearStaleAssignment(c,houseId,now);
                HouseRoute existing=findLocked(c,houseId);
                if (existing!=null) {
                    if(local.isInstance()&&local.getId().equals(existing.getInstanceId())){c.commit();return existing;}
                    if(isHealthy(c,existing.getInstanceId(),now-HousingPerformanceRegistry.INSTANCE_HEALTH_TIMEOUT_SECONDS*1000L)){c.commit();return existing;}
                    c.commit();return null;
                }
                HouseRoute selected=local.isInstance()?new HouseRoute(local.getId(),local.getId()):leastBusy(c,now-HousingPerformanceRegistry.INSTANCE_HEALTH_TIMEOUT_SECONDS*1000L);
                if (selected==null) { c.rollback(); return null; }
                try (PreparedStatement p=c.prepareStatement("INSERT INTO housing_house_locks(house_id,server_id,locked_at,lock_expires_at) VALUES(?,?,?,?)")) {
                    p.setString(1,houseId); p.setString(2,selected.getInstanceId()); p.setLong(3,now); p.setLong(4,now+HousingPerformanceRegistry.HOUSE_LOCK_SECONDS*1000L); p.executeUpdate();
                }
                try (PreparedStatement p=c.prepareStatement("INSERT INTO housing_house_locations(house_id,server_id,updated_at) VALUES(?,?,?) ON DUPLICATE KEY UPDATE server_id=VALUES(server_id),updated_at=VALUES(updated_at)")) {
                    p.setString(1,houseId); p.setString(2,selected.getInstanceId()); p.setLong(3,now); p.executeUpdate();
                }
                c.commit(); return selected;
            } catch (SQLException ex) {
                c.rollback(); throw ex;
            } finally { c.setAutoCommit(true); }
        }
    }
    private void clearStaleAssignment(Connection c,String houseId,long now)throws SQLException {
        String sql="SELECT h.server_id,l.lock_expires_at FROM housing_house_locations h LEFT JOIN housing_house_locks l ON l.house_id=h.house_id AND l.server_id=h.server_id WHERE h.house_id=? FOR UPDATE";
        String assigned=null;Long expires=null;try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,houseId);try(ResultSet r=p.executeQuery()){if(r.next()){assigned=r.getString(1);if(r.getObject(2)!=null)expires=r.getLong(2);}}}
        if(assigned!=null&&(expires==null||expires<=now))deleteAssignment(c,houseId);
    }
    private void deleteAssignment(Connection c,String houseId)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("DELETE FROM housing_house_locations WHERE house_id=?");PreparedStatement l=c.prepareStatement("DELETE FROM housing_house_locks WHERE house_id=?")){p.setString(1,houseId);p.executeUpdate();l.setString(1,houseId);l.executeUpdate();}
    }
    private HouseRoute findLocked(Connection c,String id)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT server_id FROM housing_house_locks WHERE house_id=? FOR UPDATE")){p.setString(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())return null;String serverId=r.getString(1);HousingServerDefinition registered=HousingServerRegistry.get(serverId);return registered!=null&&registered.isInstance()?new HouseRoute(serverId,serverId):null;}}
    }
    private boolean isHealthy(Connection c,String serverId,long cutoff)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM housing_instances WHERE server_id=? AND online=1 AND last_heartbeat>?")){p.setString(1,serverId);p.setLong(2,cutoff);try(ResultSet r=p.executeQuery()){return r.next();}}}
    public HouseRoute find(String id,long now)throws SQLException {
        try(Connection c=database.connection()){c.setAutoCommit(false);try{clearStaleAssignment(c,id,now);HouseRoute route=findLocked(c,id);c.commit();return route;}catch(SQLException ex){c.rollback();throw ex;}finally{c.setAutoCommit(true);}}
    }
    private HouseRoute leastBusy(Connection c,long cutoff)throws SQLException {
        String sql="SELECT server_id FROM housing_instances WHERE role='INSTANCE' AND online=1 AND last_heartbeat>? AND loaded_house_count<max_loaded_houses ORDER BY loaded_house_count,player_count,last_heartbeat DESC FOR UPDATE";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,cutoff);try(ResultSet r=p.executeQuery()){while(r.next()){String serverId=r.getString(1);HousingServerDefinition registered=HousingServerRegistry.get(serverId);if(registered!=null&&registered.isInstance())return new HouseRoute(serverId,serverId);}return null;}}
    }
    public void release(String houseId,String serverId)throws SQLException {
        try(Connection c=database.connection()){c.setAutoCommit(false);try(PreparedStatement p=c.prepareStatement("DELETE FROM housing_house_locations WHERE house_id=? AND server_id=?");PreparedStatement l=c.prepareStatement("DELETE FROM housing_house_locks WHERE house_id=? AND server_id=?")){p.setString(1,houseId);p.setString(2,serverId);p.executeUpdate();l.setString(1,houseId);l.setString(2,serverId);l.executeUpdate();c.commit();}catch(SQLException ex){c.rollback();throw ex;}finally{c.setAutoCommit(true);}}
    }
    public void offline(String id)throws SQLException {try(Connection c=database.connection();PreparedStatement p=c.prepareStatement("UPDATE housing_instances SET online=0,last_heartbeat=? WHERE server_id=?")){p.setLong(1,System.currentTimeMillis());p.setString(2,id);p.executeUpdate();}}
    public void savePendingRoute(UUID player,PendingRouteAction action,String houseId,String target,long now)throws SQLException {
        String sql="INSERT INTO housing_pending_routes(player_uuid,action,house_id,target_server,created_at,expires_at) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE action=VALUES(action),house_id=VALUES(house_id),target_server=VALUES(target_server),created_at=VALUES(created_at),expires_at=VALUES(expires_at)";
        try(Connection c=database.connection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,player.toString());p.setString(2,action.name());p.setString(3,houseId);p.setString(4,target);p.setLong(5,now);p.setLong(6,now+HousingPerformanceRegistry.PENDING_ROUTE_SECONDS*1000L);p.executeUpdate();}
    }
    public PendingHouseRoute findPendingRoute(UUID player,long now)throws SQLException {
        try(Connection c=database.connection()){try(PreparedStatement d=c.prepareStatement("DELETE FROM housing_pending_routes WHERE expires_at<=?")){d.setLong(1,now);d.executeUpdate();}try(PreparedStatement p=c.prepareStatement("SELECT action,house_id,target_server FROM housing_pending_routes WHERE player_uuid=? AND expires_at>?")){p.setString(1,player.toString());p.setLong(2,now);try(ResultSet r=p.executeQuery()){if(!r.next())return null;try{return new PendingHouseRoute(player,PendingRouteAction.valueOf(r.getString(1)),r.getString(2),r.getString(3));}catch(IllegalArgumentException ignored){return null;}}}}
    }
    public void deletePendingRoute(UUID player,String target)throws SQLException {try(Connection c=database.connection();PreparedStatement p=c.prepareStatement("DELETE FROM housing_pending_routes WHERE player_uuid=? AND target_server=?")){p.setString(1,player.toString());p.setString(2,target);p.executeUpdate();}}
    public void markHouseLoaded(String houseId,String serverId,long now)throws SQLException {
        try(Connection c=database.connection();PreparedStatement p=c.prepareStatement("INSERT INTO housing_house_locations(house_id,server_id,updated_at) VALUES(?,?,?) ON DUPLICATE KEY UPDATE server_id=VALUES(server_id),updated_at=VALUES(updated_at)")){p.setString(1,houseId);p.setString(2,serverId);p.setLong(3,now);p.executeUpdate();}
    }
    public HouseLease refreshLease(String houseId,String serverId,long now)throws SQLException {
        long expires=now+HousingPerformanceRegistry.HOUSE_LOCK_SECONDS*1000L;
        try(Connection c=database.connection()){try(PreparedStatement p=c.prepareStatement("UPDATE housing_house_locks SET lock_expires_at=? WHERE house_id=? AND server_id=?")){p.setLong(1,expires);p.setString(2,houseId);p.setString(3,serverId);if(p.executeUpdate()==1)return new HouseLease(houseId,serverId,expires);}return readLease(c,houseId);}
    }
    public HouseLease getLease(String houseId)throws SQLException{try(Connection c=database.connection()){return readLease(c,houseId);}}
    private HouseLease readLease(Connection c,String houseId)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT server_id,lock_expires_at FROM housing_house_locks WHERE house_id=?")){p.setString(1,houseId);try(ResultSet r=p.executeQuery()){return r.next()?new HouseLease(houseId,r.getString(1),r.getLong(2)):new HouseLease(houseId,"None",0L);}}}
}
