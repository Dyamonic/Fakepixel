package club.abelix.games.fakevival.setup;

import club.abelix.games.fakevival.Game;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SMPSetupSnapshotService {
    private static final int FORMAT_VERSION=1;
    private static final DateTimeFormatter ID_TIME=DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss_SSS");
    private final Game game;
    private final Gson gson=new GsonBuilder().disableHtmlEscaping().create();
    private final SMPSetupSnapshotImporter importer;
    public SMPSetupSnapshotService(Game game){this.game=game;this.importer=new SMPSetupSnapshotImporter(game);}

    public void start(){if(!game.getDataManager().isMySqlEnabled())return;Bukkit.getScheduler().runTaskAsynchronously(game,()->{try{ensureTable();}catch(Exception e){game.getLogger().warning("[SMP Setup] Snapshot table setup failed: "+e.getMessage());}});Bukkit.getScheduler().runTaskLater(game,()->Bukkit.getScheduler().runTaskAsynchronously(game,importer::seedIfEmptyFromStaticRegistryData),40L);}

    public CompletableFuture<SMPSetupSnapshot> createSnapshot(UUID createdBy){
        CompletableFuture<SMPSetupSnapshot> future=new CompletableFuture<>();
        Bukkit.getScheduler().runTask(game,()->{JsonObject local=collectLocalSetup();Bukkit.getScheduler().runTaskAsynchronously(game,()->{try{ensureTable();SMPSetupSnapshot snapshot=buildSnapshot(local,createdBy);store(snapshot);future.complete(snapshot);}catch(Exception e){future.completeExceptionally(e);}});});
        return future;
    }

    public CompletableFuture<Path> exportLatestJava(){return CompletableFuture.supplyAsync(()->{try{SMPSetupSnapshot snapshot=latest();if(snapshot==null)throw new IllegalStateException("No setup snapshot exists.");Path file=game.getDataFolder().toPath().resolve("smp-network-setup.json");Files.createDirectories(file.getParent());Files.writeString(file,StaticRegistryDataExporter.javaSource(snapshot),StandardCharsets.UTF_8);return file;}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}});}

    private void ensureTable()throws Exception{try(Connection c=game.getDataManager().getConnection();Statement s=c.createStatement()){s.executeUpdate("CREATE TABLE IF NOT EXISTS smp_setup_snapshots (snapshot_id VARCHAR(96) PRIMARY KEY,snapshot_name VARCHAR(128) NOT NULL,format_version INT NOT NULL DEFAULT 1,created_by VARCHAR(36) NULL,created_at BIGINT NOT NULL,data_json LONGTEXT NOT NULL,data_hash VARCHAR(128) NULL,notes TEXT NULL)");}}

    private SMPSetupSnapshot buildSnapshot(JsonObject local,UUID creator)throws Exception{
        importer.syncNpcRows(local.getAsJsonObject("npcs"));
        long now=System.currentTimeMillis();String id="network_setup_"+LocalDateTime.now().format(ID_TIME);JsonObject root=new JsonObject();root.addProperty("formatVersion",FORMAT_VERSION);root.addProperty("createdAt",now);if(creator!=null)root.addProperty("createdBy",creator.toString());
        JsonObject groups=new JsonObject();groups.add("HUB",strings("smp-1","smp-2","smp-3","smp-4"));groups.add("OVERWORLD",strings("smpworld-1","smpworld-2","smpworld-3","smpworld-4"));groups.add("DUNGEON",strings("smpdun-1","smpdun-2","smpdun-3","smpdun-4"));groups.add("registered",query("SELECT server_id,server_type FROM smp_servers WHERE server_id REGEXP '^(smp|smpworld|smpdun)-[1-4]$' ORDER BY server_type,server_id"));root.add("serverGroups",groups);
        root.add("spawns",query("SELECT spawn_key,server_type,server_id,world_name,x,y,z,yaw,pitch,updated_by,updated_at FROM smp_server_spawns ORDER BY server_type,server_id,spawn_key"));
        root.add("rtpLocations",query("SELECT server_id,world_name,x,y,z,yaw,pitch,min_radius,max_radius,updated_by,updated_at FROM smp_rtp_locations ORDER BY server_id"));
        JsonArray warps=new JsonArray();for(String warp:List.of("hub","overworld","dungeon","dungeons")){JsonObject row=new JsonObject();row.addProperty("id",warp);row.addProperty("routing",true);row.addProperty("targetType",warp.startsWith("dungeon")?"DUNGEON":warp.toUpperCase());warps.add(row);}root.add("warps",warps);
        root.add("zones",local.get("zones"));root.add("npcs",local.get("npcs"));root.add("dungeons",local.get("dungeons"));root.add("dungeonLobby",spawnsFor(root.getAsJsonArray("spawns"),"dungeon_lobby"));root.add("portalSetup",local.get("portalSetup"));root.add("menuSetup",local.get("menuSetup"));
        JsonArray worlds=query("SELECT world_id,display_name,template,parent_template,state,world_type,environment,seed,spawn_x,spawn_y,spawn_z,spawn_yaw,spawn_pitch,created_at,updated_at,persistent,temporary,save_on_hubsave,source_format,requires_bridge FROM smp_worlds WHERE temporary=FALSE AND world_type<>'DUNGEON_INSTANCE' ORDER BY world_id");for(JsonElement element:worlds){JsonObject world=element.getAsJsonObject();String worldId=world.get("world_id").getAsString();String worldType=world.get("world_type").getAsString();world.addProperty("server_type",worldType.startsWith("DUNGEON")?"DUNGEON":worldId.equalsIgnoreCase("hub")?"HUB":"OVERWORLD");}root.add("smpWorlds",worlds);root.addProperty("worldDataReference","smp_world_data.world_id (binary world data intentionally excluded)");
        Map<String,Integer> counts=new LinkedHashMap<>();for(String name:List.of("spawns","rtpLocations","warps","smpWorlds"))counts.put(name,root.getAsJsonArray(name).size());counts.put("serverGroups",3);counts.put("zones",size(root,"zones"));counts.put("npcs",size(root,"npcs"));counts.put("dungeons",size(root,"dungeons"));String json=gson.toJson(root);return new SMPSetupSnapshot(id,"SMP Network Setup",FORMAT_VERSION,creator==null?null:creator.toString(),now,root,sha256(json),counts);
    }

    private JsonObject collectLocalSetup(){JsonObject out=new JsonObject();out.add("zones",section("map-zones"));out.add("npcs",normalizedNpcs(section("npcs")));out.add("dungeons",section("dungeons"));out.add("portalSetup",section("dungeon-spawn-portal"));JsonObject menus=new JsonObject();menus.add("main-menu",section("main-menu"));JsonArray reserved=new JsonArray();for(String id:List.of("hub","overworld","dungeons"))reserved.add(id);menus.add("waypoints",reserved);out.add("menuSetup",menus);return out;}
    private JsonObject normalizedNpcs(JsonObject npcs){for(Map.Entry<String,JsonElement> entry:npcs.entrySet()){if(!entry.getValue().isJsonObject())continue;JsonObject npc=entry.getValue().getAsJsonObject();String world=npc.has("world")?npc.get("world").getAsString():"";if(world.equalsIgnoreCase("lobby")){world="lobby";npc.addProperty("world",world);}if(!npc.has("server_type")||npc.get("server_type").isJsonNull()||npc.get("server_type").getAsString().isBlank())npc.addProperty("server_type",inferNpcServerType(world));else npc.addProperty("server_type",npc.get("server_type").getAsString().toUpperCase(java.util.Locale.ROOT));}return npcs;}
    private String inferNpcServerType(String world){if(world.equalsIgnoreCase("hub"))return "HUB";if(world.equalsIgnoreCase("lobby"))return "DUNGEON";if(world.equalsIgnoreCase("world")||world.equalsIgnoreCase("world_nether")||world.equalsIgnoreCase("world_the_end"))return "OVERWORLD";return game.getNetworkService().getCurrentServerType().name();}
    private JsonObject section(String path){ConfigurationSection section=game.getDataManager().getData().getConfigurationSection(path);return section==null?new JsonObject():toJson(section);}
    private JsonObject toJson(ConfigurationSection section){JsonObject out=new JsonObject();for(String key:section.getKeys(false)){Object value=section.get(key);if(value instanceof ConfigurationSection child)out.add(key,toJson(child));else out.add(key,gson.toJsonTree(value));}return out;}
    private JsonArray query(String sql)throws Exception{JsonArray rows=new JsonArray();try(Connection c=game.getDataManager().getConnection();PreparedStatement ps=c.prepareStatement(sql);ResultSet rs=ps.executeQuery()){ResultSetMetaData meta=rs.getMetaData();while(rs.next()){JsonObject row=new JsonObject();for(int i=1;i<=meta.getColumnCount();i++){String name=meta.getColumnLabel(i);Object value=rs.getObject(i);row.add(name,value==null?JsonNull.INSTANCE:gson.toJsonTree(value));}rows.add(row);}}return rows;}
    private void store(SMPSetupSnapshot snapshot)throws Exception{try(Connection c=game.getDataManager().getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO smp_setup_snapshots(snapshot_id,snapshot_name,format_version,created_by,created_at,data_json,data_hash,notes) VALUES(?,?,?,?,?,?,?,?)")){ps.setString(1,snapshot.snapshotId());ps.setString(2,snapshot.snapshotName());ps.setInt(3,snapshot.formatVersion());ps.setString(4,snapshot.createdBy());ps.setLong(5,snapshot.createdAt());ps.setString(6,gson.toJson(snapshot.data()));ps.setString(7,snapshot.dataHash());ps.setString(8,"Created by /smp hubsave; player and runtime queue data excluded.");ps.executeUpdate();}game.getLogger().info("[SMP Setup] Stored snapshot "+snapshot.snapshotId()+" counts="+snapshot.sectionCounts());}
    private SMPSetupSnapshot latest()throws Exception{try(Connection c=game.getDataManager().getConnection();PreparedStatement ps=c.prepareStatement("SELECT snapshot_id,snapshot_name,format_version,created_by,created_at,data_json,data_hash FROM smp_setup_snapshots ORDER BY created_at DESC LIMIT 1");ResultSet rs=ps.executeQuery()){if(!rs.next())return null;JsonObject data=com.google.gson.JsonParser.parseString(rs.getString("data_json")).getAsJsonObject();return new SMPSetupSnapshot(rs.getString("snapshot_id"),rs.getString("snapshot_name"),rs.getInt("format_version"),rs.getString("created_by"),rs.getLong("created_at"),data,rs.getString("data_hash"),Map.of());}}
    private JsonArray strings(String... values){JsonArray array=new JsonArray();for(String value:values)array.add(value);return array;}
    private JsonArray spawnsFor(JsonArray rows,String key){JsonArray result=new JsonArray();for(JsonElement element:rows)if(key.equalsIgnoreCase(element.getAsJsonObject().get("spawn_key").getAsString()))result.add(element);return result;}
    private int size(JsonObject root,String name){JsonElement value=root.get(name);return value!=null&&value.isJsonObject()?value.getAsJsonObject().size():0;}
    private String sha256(String value)throws Exception{byte[] hash=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));return java.util.HexFormat.of().formatHex(hash);}
}
