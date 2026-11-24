package club.abelix.games.fakevival.proxy;

import java.sql.*;
import java.util.*;

public final class ProxyDatabase {
    private final String url, user, password;
    public ProxyDatabase() {
        String host = value("SMP_DATABASE_HOST", "localhost");
        String port = value("SMP_DATABASE_PORT", "3306");
        String database = value("SMP_DATABASE_NAME", "mmorpg");
        this.user = value("SMP_DATABASE_USER", "username");
        this.password = value("SMP_DATABASE_PASSWORD", "password");
        this.url = "jdbc:mariadb://" + host + ":" + port + "/" + database + "?useUnicode=true&characterEncoding=utf8&useSSL=false";
    }
    public Connection connection() throws SQLException { return DriverManager.getConnection(url, user, password); }
    public List<ServerInfo> available(String type) throws SQLException {
        long stale = System.currentTimeMillis() - 35_000L;
        List<ServerInfo> out = new ArrayList<>();
        try (Connection c=connection(); PreparedStatement ps=c.prepareStatement("SELECT server_id,server_type,online_players,max_players,status,tps,last_heartbeat,metadata_json FROM smp_servers WHERE server_type=? AND status='ONLINE' AND last_heartbeat>=? AND (max_players<=0 OR online_players<max_players)")) {
            ps.setString(1,type); ps.setLong(2,stale); try(ResultSet rs=ps.executeQuery()){ while(rs.next()) out.add(new ServerInfo(rs.getString(1),rs.getString(2),rs.getInt(3),rs.getInt(4),rs.getString(5),rs.getDouble(6),activeRuns(rs.getString(8)))); }
        }
        return out;
    }
    public Optional<String> assignedOverworld(UUID uuid) throws SQLException {
        try(Connection c=connection(); PreparedStatement ps=c.prepareStatement("SELECT overworld_server_id FROM smp_player_server_assignment WHERE uuid=?")){ ps.setString(1,uuid.toString()); try(ResultSet rs=ps.executeQuery()){ return rs.next()?Optional.ofNullable(rs.getString(1)):Optional.empty(); }}
    }
    public void assign(UUID uuid,String type,String server) throws SQLException {
        String column=switch(type){case "HUB"->"last_hub_server_id";case "DUNGEON"->"last_dungeon_server_id";default->"overworld_server_id";};
        try(Connection c=connection();PreparedStatement ps=c.prepareStatement("INSERT INTO smp_player_server_assignment(uuid,"+column+",updated_at) VALUES(?,?,?) ON DUPLICATE KEY UPDATE "+column+"=VALUES("+column+"),updated_at=VALUES(updated_at)")){ps.setString(1,uuid.toString());ps.setString(2,server);ps.setLong(3,System.currentTimeMillis());ps.executeUpdate();}
    }
    public String reserve(UUID uuid,String dungeonId,String server) throws SQLException {
        String id=UUID.randomUUID().toString(); long now=System.currentTimeMillis();
        try(Connection c=connection();PreparedStatement ps=c.prepareStatement("INSERT INTO smp_dungeon_reservations(reservation_id,uuid,dungeon_id,server_id,status,created_at,expires_at) VALUES(?,?,?,?,?,?,?)")){ps.setString(1,id);ps.setString(2,uuid.toString());ps.setString(3,dungeonId);ps.setString(4,server);ps.setString(5,"RESERVED");ps.setLong(6,now);ps.setLong(7,now+60000L);ps.executeUpdate();} return id;
    }
    public Optional<ServerInfo> select(UUID uuid,String type) throws SQLException { return select(uuid,type,id->true); }
    public Optional<ServerInfo> select(UUID uuid,String type,java.util.function.Predicate<String> serverExists) throws SQLException {
        List<ServerInfo> servers=available(type).stream().filter(s -> serverExists.test(s.id())).toList();
        if(type.equals("OVERWORLD")){Optional<String> assigned=assignedOverworld(uuid);if(assigned.isPresent()){Optional<ServerInfo> found=servers.stream().filter(s->s.id().equals(assigned.get())).findFirst();if(found.isPresent())return found;}}
        Comparator<ServerInfo> c=type.equals("DUNGEON")?Comparator.comparingInt(ServerInfo::activeRuns).thenComparingInt(ServerInfo::online):Comparator.comparingInt(ServerInfo::online);
        if(type.equals("HUB")) c=Comparator.<ServerInfo>comparingInt(s->s.id().equalsIgnoreCase("smp-1")&&s.online()<Math.max(1,s.max()*3/4)?0:1).thenComparingInt(ServerInfo::online);
        return servers.stream().min(c);
    }
    private static int activeRuns(String json){if(json==null)return 0;int k=json.indexOf("activeDungeonRuns");if(k<0)return 0;int c=json.indexOf(':',k),e=c+1;while(e<json.length()&&Character.isDigit(json.charAt(e)))e++;try{return Integer.parseInt(json.substring(c+1,e));}catch(Exception ignored){return 0;}}
    private static String value(String key,String fallback){String v=System.getProperty(key);if(v==null||v.isBlank())v=System.getenv(key);return v==null||v.isBlank()?fallback:v;}
    public record ServerInfo(String id,String type,int online,int max,String status,double tps,int activeRuns){}
}
