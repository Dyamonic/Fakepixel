package club.abelix.games.fakevival.network;

import club.abelix.games.fakevival.data.DataManager;

import java.sql.*;
import java.util.*;

public final class SMPNetworkRepository {
    private final DataManager dataManager;

    public SMPNetworkRepository(DataManager dataManager) { this.dataManager = dataManager; }

    public void ensureTables() throws SQLException {
        try (Connection c = dataManager.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_servers (server_id VARCHAR(64) PRIMARY KEY, server_type VARCHAR(32) NOT NULL, online_players INT NOT NULL DEFAULT 0, max_players INT NOT NULL DEFAULT 0, status VARCHAR(32) NOT NULL DEFAULT 'ONLINE', tps DOUBLE NOT NULL DEFAULT 20, last_heartbeat BIGINT NOT NULL, metadata_json LONGTEXT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_player_server_assignment (uuid CHAR(36) PRIMARY KEY, overworld_server_id VARCHAR(64) NULL, last_hub_server_id VARCHAR(64) NULL, last_dungeon_server_id VARCHAR(64) NULL, updated_at BIGINT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_dungeon_reservations (reservation_id VARCHAR(64) PRIMARY KEY, uuid CHAR(36) NOT NULL, dungeon_id VARCHAR(64) NOT NULL, server_id VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL, created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL, INDEX idx_smp_dungeon_reservation_uuid (uuid), INDEX idx_smp_dungeon_reservation_server (server_id), INDEX idx_smp_dungeon_reservation_expiry (expires_at))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_pending_rtp (player_uuid VARCHAR(36) PRIMARY KEY, source_server VARCHAR(64) NOT NULL, target_server VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'PENDING', created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_player_presence (player_uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(16) NOT NULL, server_id VARCHAR(64) NOT NULL, server_type VARCHAR(32) NOT NULL, world_name VARCHAR(64) NULL, x DOUBLE NULL, y DOUBLE NULL, z DOUBLE NULL, updated_at BIGINT NOT NULL, INDEX idx_smp_presence_name (player_name), INDEX idx_smp_presence_server (server_id))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_pending_staff_tp (staff_uuid VARCHAR(36) PRIMARY KEY, target_uuid VARCHAR(36) NOT NULL, target_name VARCHAR(16) NOT NULL, target_server VARCHAR(64) NOT NULL, created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_pending_dungeon_join (player_uuid VARCHAR(36) PRIMARY KEY, party_id VARCHAR(64) NULL, dungeon_id VARCHAR(64) NOT NULL, target_server VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'PENDING', created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_pending_warps (player_uuid VARCHAR(36) PRIMARY KEY, target_type VARCHAR(32) NOT NULL, target_server VARCHAR(64) NOT NULL, target_warp VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'PENDING', created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_pending_zone_warps (player_uuid VARCHAR(36) PRIMARY KEY, zone_id VARCHAR(64) NOT NULL, warp_id VARCHAR(64) NULL, target_server_type VARCHAR(32) NOT NULL DEFAULT 'HUB', status VARCHAR(32) NOT NULL DEFAULT 'PENDING', created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_server_spawns (spawn_key VARCHAR(64) NOT NULL, server_type VARCHAR(32) NOT NULL, server_id VARCHAR(64) NULL, world_name VARCHAR(64) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL, updated_by VARCHAR(36) NULL, updated_at BIGINT NOT NULL, PRIMARY KEY(spawn_key,server_type,server_id))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_rtp_locations (server_id VARCHAR(64) PRIMARY KEY, world_name VARCHAR(64) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL, min_radius INT NOT NULL DEFAULT 500, max_radius INT NOT NULL DEFAULT 5000, updated_by VARCHAR(36) NULL, updated_at BIGINT NOT NULL)");
        }
    }

    public void heartbeat(SMPServerIdentity identity, int online, int max, double tps, int activeRuns, SMPServerStatus requested) throws SQLException {
        SMPServerStatus status = requested == SMPServerStatus.ONLINE && max > 0 && online >= max ? SMPServerStatus.FULL : requested;
        String metadata = "{\"activeDungeonRuns\":" + Math.max(0, activeRuns) + "}";
        try (Connection c = dataManager.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO smp_servers(server_id,server_type,online_players,max_players,status,tps,last_heartbeat,metadata_json) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE server_type=VALUES(server_type),online_players=VALUES(online_players),max_players=VALUES(max_players),status=VALUES(status),tps=VALUES(tps),last_heartbeat=VALUES(last_heartbeat),metadata_json=VALUES(metadata_json)")) {
            ps.setString(1, identity.serverId()); ps.setString(2, identity.serverType().name()); ps.setInt(3, online); ps.setInt(4, max);
            ps.setString(5, status.name()); ps.setDouble(6, tps); ps.setLong(7, System.currentTimeMillis()); ps.setString(8, metadata); ps.executeUpdate();
        }
    }

    public void markStatus(String serverId, SMPServerStatus status) throws SQLException {
        try (Connection c = dataManager.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE smp_servers SET status=?,last_heartbeat=? WHERE server_id=?")) {
            ps.setString(1, status.name()); ps.setLong(2, System.currentTimeMillis()); ps.setString(3, serverId); ps.executeUpdate();
        }
    }

    public List<ServerRecord> servers() throws SQLException {
        long staleBefore = System.currentTimeMillis() - 35_000L;
        List<ServerRecord> result = new ArrayList<>();
        try (Connection c = dataManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT server_id,server_type,online_players,max_players,status,tps,last_heartbeat,metadata_json FROM smp_servers ORDER BY server_type,server_id"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SMPServerStatus status = SMPServerStatus.valueOf(rs.getString("status"));
                if (rs.getLong("last_heartbeat") < staleBefore && status != SMPServerStatus.MAINTENANCE) status = SMPServerStatus.OFFLINE;
                String serverId=SMPServerGroups.canonicalId(rs.getString("server_id"));
                result.add(new ServerRecord(serverId, serverType(serverId,rs.getString("server_type")), rs.getInt("online_players"), rs.getInt("max_players"), status, rs.getDouble("tps"), activeRuns(rs.getString("metadata_json"))));
            }
        }
        return result;
    }

    public Optional<String> assignedOverworld(UUID uuid) throws SQLException {
        try (Connection c = dataManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT overworld_server_id FROM smp_player_server_assignment WHERE uuid=?")) {
            ps.setString(1, uuid.toString()); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.ofNullable(SMPServerGroups.canonicalId(rs.getString(1))) : Optional.empty(); }
        }
    }

    public void updateAssignment(UUID uuid, SMPServerType type, String serverId) throws SQLException {
        if (type == SMPServerType.UNKNOWN) return;
        String column = switch (type) { case HUB -> "last_hub_server_id"; case OVERWORLD -> "overworld_server_id"; case DUNGEON -> "last_dungeon_server_id"; case UNKNOWN -> throw new IllegalStateException(); };
        String sql = "INSERT INTO smp_player_server_assignment(uuid," + column + ",updated_at) VALUES(?,?,?) ON DUPLICATE KEY UPDATE " + column + "=VALUES(" + column + "),updated_at=VALUES(updated_at)";
        try (Connection c = dataManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, SMPServerGroups.canonicalId(serverId)); ps.setLong(3, System.currentTimeMillis()); ps.executeUpdate();
        }
    }

    public Reservation reserveDungeon(UUID uuid, String dungeonId, String serverId) throws SQLException {
        long now = System.currentTimeMillis();
        Reservation reservation = new Reservation(UUID.randomUUID().toString(), uuid, dungeonId, serverId, "RESERVED", now, now + 60_000L);
        try (Connection c = dataManager.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO smp_dungeon_reservations(reservation_id,uuid,dungeon_id,server_id,status,created_at,expires_at) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, reservation.reservationId()); ps.setString(2, uuid.toString()); ps.setString(3, dungeonId); ps.setString(4, serverId); ps.setString(5, reservation.status()); ps.setLong(6, now); ps.setLong(7, reservation.expiresAt()); ps.executeUpdate();
        }
        try (Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO smp_pending_dungeon_join(player_uuid,party_id,dungeon_id,target_server,status,created_at,expires_at) VALUES(?,NULL,?,?,'PENDING',?,?) ON DUPLICATE KEY UPDATE party_id=NULL,dungeon_id=VALUES(dungeon_id),target_server=VALUES(target_server),status='PENDING',created_at=VALUES(created_at),expires_at=VALUES(expires_at)")) {
            ps.setString(1,uuid.toString());ps.setString(2,dungeonId);ps.setString(3,serverId);ps.setLong(4,now);ps.setLong(5,now+120_000L);ps.executeUpdate();
        }
        return reservation;
    }

    public Optional<String> consumeDungeonJoin(UUID uuid,String serverId)throws SQLException{try(Connection c=dataManager.getConnection()){c.setAutoCommit(false);String dungeon=null;try(PreparedStatement ps=c.prepareStatement("SELECT dungeon_id FROM smp_pending_dungeon_join WHERE player_uuid=? AND target_server=? AND status='PENDING' AND expires_at>=? FOR UPDATE")){ps.setString(1,uuid.toString());ps.setString(2,serverId);ps.setLong(3,System.currentTimeMillis());try(ResultSet rs=ps.executeQuery()){if(rs.next())dungeon=rs.getString(1);}}if(dungeon!=null)try(PreparedStatement ps=c.prepareStatement("DELETE FROM smp_pending_dungeon_join WHERE player_uuid=?")){ps.setString(1,uuid.toString());ps.executeUpdate();}c.commit();return Optional.ofNullable(dungeon);}}

    public void queueWarp(UUID uuid,SMPServerType type,String server,String warp)throws SQLException{long now=System.currentTimeMillis();try(Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO smp_pending_warps(player_uuid,target_type,target_server,target_warp,status,created_at,expires_at) VALUES(?,?,?,?, 'PENDING',?,?) ON DUPLICATE KEY UPDATE target_type=VALUES(target_type),target_server=VALUES(target_server),target_warp=VALUES(target_warp),status='PENDING',created_at=VALUES(created_at),expires_at=VALUES(expires_at)")){ps.setString(1,uuid.toString());ps.setString(2,type.name());ps.setString(3,server);ps.setString(4,warp);ps.setLong(5,now);ps.setLong(6,now+60_000L);ps.executeUpdate();}}
    public Optional<PendingWarp> consumeWarp(UUID uuid,String server)throws SQLException{try(Connection c=dataManager.getConnection()){c.setAutoCommit(false);PendingWarp value=null;try(PreparedStatement ps=c.prepareStatement("SELECT target_type,target_server,target_warp FROM smp_pending_warps WHERE player_uuid=? AND status='PENDING' AND expires_at>=? AND (target_server=? OR target_server='ANY') FOR UPDATE")){ps.setString(1,uuid.toString());ps.setLong(2,System.currentTimeMillis());ps.setString(3,server);try(ResultSet rs=ps.executeQuery()){if(rs.next())value=new PendingWarp(SMPServerType.parse(rs.getString(1)),rs.getString(2),rs.getString(3));}}if(value!=null)try(PreparedStatement ps=c.prepareStatement("DELETE FROM smp_pending_warps WHERE player_uuid=?")){ps.setString(1,uuid.toString());ps.executeUpdate();}c.commit();return Optional.ofNullable(value);}}
    public void queueZoneWarp(UUID uuid,String zoneId)throws SQLException{long now=System.currentTimeMillis();try(Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO smp_pending_zone_warps(player_uuid,zone_id,warp_id,target_server_type,status,created_at,expires_at) VALUES(?,?,NULL,'HUB','PENDING',?,?) ON DUPLICATE KEY UPDATE zone_id=VALUES(zone_id),warp_id=NULL,target_server_type='HUB',status='PENDING',created_at=VALUES(created_at),expires_at=VALUES(expires_at)")){ps.setString(1,uuid.toString());ps.setString(2,zoneId);ps.setLong(3,now);ps.setLong(4,now+60_000L);ps.executeUpdate();}}
    public Optional<PendingZoneWarp> consumeZoneWarp(UUID uuid)throws SQLException{try(Connection c=dataManager.getConnection()){c.setAutoCommit(false);PendingZoneWarp value=null;try(PreparedStatement ps=c.prepareStatement("SELECT zone_id,warp_id FROM smp_pending_zone_warps WHERE player_uuid=? AND target_server_type='HUB' AND status='PENDING' AND expires_at>=? FOR UPDATE")){ps.setString(1,uuid.toString());ps.setLong(2,System.currentTimeMillis());try(ResultSet rs=ps.executeQuery()){if(rs.next())value=new PendingZoneWarp(rs.getString(1),rs.getString(2));}}try(PreparedStatement ps=c.prepareStatement("DELETE FROM smp_pending_zone_warps WHERE player_uuid=?")){ps.setString(1,uuid.toString());ps.executeUpdate();}c.commit();return Optional.ofNullable(value);}}
    public void saveSpawn(String key,SMPServerIdentity identity,String world,double x,double y,double z,float yaw,float pitch,UUID updatedBy)throws SQLException{try(Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO smp_server_spawns(spawn_key,server_type,server_id,world_name,x,y,z,yaw,pitch,updated_by,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE world_name=VALUES(world_name),x=VALUES(x),y=VALUES(y),z=VALUES(z),yaw=VALUES(yaw),pitch=VALUES(pitch),updated_by=VALUES(updated_by),updated_at=VALUES(updated_at)")){ps.setString(1,key);ps.setString(2,identity.serverType().name());ps.setString(3,identity.serverId());ps.setString(4,world);ps.setDouble(5,x);ps.setDouble(6,y);ps.setDouble(7,z);ps.setFloat(8,yaw);ps.setFloat(9,pitch);ps.setString(10,updatedBy==null?null:updatedBy.toString());ps.setLong(11,System.currentTimeMillis());ps.executeUpdate();}}
    public Optional<ServerSpawn> findSpawn(String key,SMPServerIdentity identity)throws SQLException{try(Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("SELECT world_name,x,y,z,yaw,pitch FROM smp_server_spawns WHERE spawn_key=? AND server_type=? AND (server_id=? OR server_id IS NULL) ORDER BY server_id IS NULL ASC LIMIT 1")){ps.setString(1,key);ps.setString(2,identity.serverType().name());ps.setString(3,identity.serverId());try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(new ServerSpawn(rs.getString(1),rs.getDouble(2),rs.getDouble(3),rs.getDouble(4),rs.getFloat(5),rs.getFloat(6))):Optional.empty();}}}

    public void saveRtpLocation(String serverId,String world,double x,double y,double z,float yaw,float pitch,int minRadius,int maxRadius,UUID updatedBy)throws SQLException{try(Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO smp_rtp_locations(server_id,world_name,x,y,z,yaw,pitch,min_radius,max_radius,updated_by,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE world_name=VALUES(world_name),x=VALUES(x),y=VALUES(y),z=VALUES(z),yaw=VALUES(yaw),pitch=VALUES(pitch),min_radius=VALUES(min_radius),max_radius=VALUES(max_radius),updated_by=VALUES(updated_by),updated_at=VALUES(updated_at)")){ps.setString(1,serverId);ps.setString(2,world);ps.setDouble(3,x);ps.setDouble(4,y);ps.setDouble(5,z);ps.setFloat(6,yaw);ps.setFloat(7,pitch);ps.setInt(8,minRadius);ps.setInt(9,maxRadius);ps.setString(10,updatedBy==null?null:updatedBy.toString());ps.setLong(11,System.currentTimeMillis());ps.executeUpdate();}}
    public Optional<RtpLocation> findRtpLocation(String serverId)throws SQLException{try(Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("SELECT server_id,world_name,x,y,z,yaw,pitch,min_radius,max_radius,updated_by,updated_at FROM smp_rtp_locations WHERE server_id=?")){ps.setString(1,serverId);try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(new RtpLocation(rs.getString(1),rs.getString(2),rs.getDouble(3),rs.getDouble(4),rs.getDouble(5),rs.getFloat(6),rs.getFloat(7),rs.getInt(8),rs.getInt(9),rs.getString(10),rs.getLong(11))):Optional.empty();}}}

    public void queueRtp(UUID uuid, String source, String target) throws SQLException {
        long now = System.currentTimeMillis();
        try (Connection c=dataManager.getConnection(); PreparedStatement ps=c.prepareStatement("INSERT INTO smp_pending_rtp(player_uuid,source_server,target_server,status,created_at,expires_at) VALUES(?,?,?,'PENDING',?,?) ON DUPLICATE KEY UPDATE source_server=VALUES(source_server),target_server=VALUES(target_server),status='PENDING',created_at=VALUES(created_at),expires_at=VALUES(expires_at)")) {
            ps.setString(1,uuid.toString()); ps.setString(2,SMPServerGroups.canonicalId(source)); ps.setString(3,SMPServerGroups.canonicalId(target)); ps.setLong(4,now); ps.setLong(5,now+60_000L); ps.executeUpdate();
        }
    }

    public boolean consumeRtp(UUID uuid, String serverId) throws SQLException {
        long now=System.currentTimeMillis();
        try (Connection c=dataManager.getConnection()) {
            c.setAutoCommit(false);
            boolean found;
            try (PreparedStatement ps=c.prepareStatement("SELECT target_server FROM smp_pending_rtp WHERE player_uuid=? AND status='PENDING' AND expires_at>=? FOR UPDATE")) {
                ps.setString(1,uuid.toString()); ps.setLong(2,now);
                try(ResultSet rs=ps.executeQuery()){
                    if (rs.next()) {
                        String target = rs.getString(1);
                        found = "ANY_OVERWORLD".equalsIgnoreCase(target)
                                || SMPServerGroups.canonicalId(target).equals(SMPServerGroups.canonicalId(serverId));
                    } else found = false;
                }
            }
            if(found) try(PreparedStatement ps=c.prepareStatement("DELETE FROM smp_pending_rtp WHERE player_uuid=?")){ps.setString(1,uuid.toString());ps.executeUpdate();}
            c.commit(); return found;
        }
    }

    public void updatePresence(UUID uuid,String name,SMPServerIdentity identity,String world,double x,double y,double z) throws SQLException {
        try(Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO smp_player_presence(player_uuid,player_name,server_id,server_type,world_name,x,y,z,updated_at) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE player_name=VALUES(player_name),server_id=VALUES(server_id),server_type=VALUES(server_type),world_name=VALUES(world_name),x=VALUES(x),y=VALUES(y),z=VALUES(z),updated_at=VALUES(updated_at)")){
            ps.setString(1,uuid.toString());ps.setString(2,name);ps.setString(3,identity.serverId());ps.setString(4,identity.serverType().name());ps.setString(5,world);ps.setDouble(6,x);ps.setDouble(7,y);ps.setDouble(8,z);ps.setLong(9,System.currentTimeMillis());ps.executeUpdate();
        }
    }

    public void removePresence(UUID uuid,String serverId) throws SQLException { try(Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("DELETE FROM smp_player_presence WHERE player_uuid=? AND server_id=?")){ps.setString(1,uuid.toString());ps.setString(2,serverId);ps.executeUpdate();} }

    public Optional<PlayerPresence> findPresence(String nameOrUuid) throws SQLException {
        try(Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("SELECT player_uuid,player_name,server_id,server_type,world_name,x,y,z,updated_at FROM smp_player_presence WHERE (player_uuid=? OR LOWER(player_name)=LOWER(?)) AND updated_at>=? LIMIT 1")){
            ps.setString(1,nameOrUuid);ps.setString(2,nameOrUuid);ps.setLong(3,System.currentTimeMillis()-45_000L);try(ResultSet rs=ps.executeQuery()){if(!rs.next())return Optional.empty();return Optional.of(new PlayerPresence(UUID.fromString(rs.getString(1)),rs.getString(2),rs.getString(3),SMPServerType.parse(rs.getString(4)),rs.getString(5),rs.getDouble(6),rs.getDouble(7),rs.getDouble(8),rs.getLong(9)));}
        }
    }

    public void queueStaffTp(UUID staff,PlayerPresence target) throws SQLException { long now=System.currentTimeMillis();try(Connection c=dataManager.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO smp_pending_staff_tp(staff_uuid,target_uuid,target_name,target_server,created_at,expires_at) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE target_uuid=VALUES(target_uuid),target_name=VALUES(target_name),target_server=VALUES(target_server),created_at=VALUES(created_at),expires_at=VALUES(expires_at)")){ps.setString(1,staff.toString());ps.setString(2,target.uuid().toString());ps.setString(3,target.name());ps.setString(4,target.serverId());ps.setLong(5,now);ps.setLong(6,now+60_000L);ps.executeUpdate();} }
    public Optional<PendingStaffTp> consumeStaffTp(UUID staff,String serverId)throws SQLException{try(Connection c=dataManager.getConnection()){c.setAutoCommit(false);PendingStaffTp value=null;try(PreparedStatement ps=c.prepareStatement("SELECT target_uuid,target_name,target_server FROM smp_pending_staff_tp WHERE staff_uuid=? AND target_server=? AND expires_at>=? FOR UPDATE")){ps.setString(1,staff.toString());ps.setString(2,serverId);ps.setLong(3,System.currentTimeMillis());try(ResultSet rs=ps.executeQuery()){if(rs.next())value=new PendingStaffTp(UUID.fromString(rs.getString(1)),rs.getString(2),rs.getString(3));}}if(value!=null)try(PreparedStatement ps=c.prepareStatement("DELETE FROM smp_pending_staff_tp WHERE staff_uuid=?")){ps.setString(1,staff.toString());ps.executeUpdate();}c.commit();return Optional.ofNullable(value);}}

    private int activeRuns(String json) {
        if (json == null) return 0;
        int key = json.indexOf("activeDungeonRuns"); if (key < 0) return 0;
        int colon = json.indexOf(':', key); if (colon < 0) return 0;
        int end = colon + 1; while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try { return Integer.parseInt(json.substring(colon + 1, end)); } catch (Exception ignored) { return 0; }
    }

    private SMPServerType serverType(String serverId,String storedType) {
        String id=SMPServerGroups.canonicalId(serverId);
        if (id == null) id = "";
        if(id.matches("smpdun-[1-4]"))return SMPServerType.DUNGEON;
        if(id.matches("smpworld-[1-4]"))return SMPServerType.OVERWORLD;
        if(id.matches("smp-[1-4]"))return SMPServerType.HUB;
        return SMPServerType.parse(storedType);
    }

    public record ServerRecord(String serverId, SMPServerType type, int onlinePlayers, int maxPlayers, SMPServerStatus status, double tps, int activeDungeonRuns) {
        public boolean available() { return status == SMPServerStatus.ONLINE && (maxPlayers <= 0 || onlinePlayers < maxPlayers); }
    }
    public record Reservation(String reservationId, UUID uuid, String dungeonId, String serverId, String status, long createdAt, long expiresAt) {}
    public record PlayerPresence(UUID uuid,String name,String serverId,SMPServerType serverType,String worldName,double x,double y,double z,long updatedAt) {}
    public record PendingStaffTp(UUID targetUuid,String targetName,String targetServer) {}
    public record PendingWarp(SMPServerType targetType,String targetServer,String targetWarp) {}
    public record PendingZoneWarp(String zoneId,String warpId) {}
    public record ServerSpawn(String worldName,double x,double y,double z,float yaw,float pitch) {}
    public record RtpLocation(String serverId,String worldName,double x,double y,double z,float yaw,float pitch,int minRadius,int maxRadius,String updatedBy,long updatedAt) {}
}
