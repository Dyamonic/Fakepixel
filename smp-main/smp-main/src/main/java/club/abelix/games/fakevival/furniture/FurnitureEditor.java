package club.abelix.games.fakevival.furniture;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class FurnitureEditor {
    private final JavaPlugin plugin; private final DataManager data; private final FurnitureRegistry registry;
    private final SmpModelEngine engine=new SmpModelEngine(); private final SmpModelImporter importer=new SmpModelImporter();
    private static final int MAX_DECODED_BYTES=1_000_000;
    private final Map<UUID,Session> sessions=new ConcurrentHashMap<>();
    public FurnitureEditor(JavaPlugin plugin,DataManager data,FurnitureRegistry registry){this.plugin=plugin;this.data=data;this.registry=registry;}

    public String create(Player player,String rawId,String rawName){String id=clean(rawId);if(id.isBlank())return"&cUse a valid furniture id.";if(registry.get(id).isPresent())return"&cThat furniture id already exists.";sessions.put(player.getUniqueId(),new Session(id,rawName.replace('_',' ').trim(),FurnitureDefinition.Category.FURNITURE,Material.ARMOR_STAND,100,new ArrayList<>(),1,2));return"&aCreated model draft &e"+id+"&a. Hold a vanilla item and use &e/smp furniture addpart&a.";}
    public String editor(Player player){Session s=sessions.get(player.getUniqueId());return s==null?"&cStart with /smp furniture create <id> <displayName>.":"&aEditing &e"+s.id+"&a: &f"+s.parts.size()+"/30 parts&e, "+s.category.displayName()+"&e, "+s.price+" Coins&a.";}
    public String addPart(Player player){Session s=session(player);if(s==null)return"&cNo active model draft.";if(s.parts.size()>=30)return"&cModels are limited to 30 parts.";Material material=player.getInventory().getItemInMainHand().getType();if(material.isAir())return"&cHold a vanilla item or block.";s.parts.add(SmpModelPart.defaultPart(material));if(s.preview==Material.ARMOR_STAND)s.preview=material;return"&aAdded part #&e"+(s.parts.size()-1)+" &ausing &e"+material+"&a.";}
    public String removePart(Player p,int index){Session s=session(p);if(!valid(s,index))return"&cInvalid model part index.";s.parts.remove(index);return"&aRemoved model part &e"+index+"&a.";}
    public String movePart(Player p,int i,double x,double y,double z){Session s=session(p);if(!valid(s,i))return"&cInvalid model part index.";SmpModelPart a=s.parts.get(i);s.parts.set(i,new SmpModelPart(a.material(),a.entityType(),a.slot(),x,y,z,a.yaw(),a.pitch(),a.roll(),a.scale(),a.visible(),a.small(),a.marker(),a.gravity(),a.basePlate(),a.arms()));return"&aMoved part &e"+i+"&a.";}
    public String rotatePart(Player p,int i,float yaw,float pitch,float roll){Session s=session(p);if(!valid(s,i))return"&cInvalid model part index.";SmpModelPart a=s.parts.get(i);s.parts.set(i,new SmpModelPart(a.material(),a.entityType(),a.slot(),a.x(),a.y(),a.z(),yaw,pitch,roll,a.scale(),a.visible(),a.small(),a.marker(),a.gravity(),a.basePlate(),a.arms()));return"&aRotated part &e"+i+"&a.";}
    public String material(Player p,int i,String value){Session s=session(p);Material material=Material.matchMaterial(value);if(!valid(s,i)||material==null||material.isAir())return"&cInvalid part or vanilla material.";SmpModelPart a=s.parts.get(i);s.parts.set(i,new SmpModelPart(material,a.entityType(),a.slot(),a.x(),a.y(),a.z(),a.yaw(),a.pitch(),a.roll(),a.scale(),a.visible(),a.small(),a.marker(),a.gravity(),a.basePlate(),a.arms()));return"&aChanged part material to &e"+material+"&a.";}
    public String type(Player p,int i,String value){Session s=session(p);if(!valid(s,i))return"&cInvalid model part index.";try{SmpModelPart.EntityType type=SmpModelPart.EntityType.valueOf(value.toUpperCase(Locale.ROOT));SmpModelPart a=s.parts.get(i);s.parts.set(i,new SmpModelPart(a.material(),type,a.slot(),a.x(),a.y(),a.z(),a.yaw(),a.pitch(),a.roll(),a.scale(),a.visible(),a.small(),a.marker(),a.gravity(),a.basePlate(),a.arms()));return"&aChanged part type to &e"+type+"&a.";}catch(Exception ignored){return"&cUse ARMOR_STAND, ITEM_DISPLAY, or BLOCK_DISPLAY.";}}
    public String setPrice(Player p,double price){Session s=session(p);if(s==null)return"&cNo active model draft.";s.price=Math.max(0,price);return"&aSet shop price to &e"+s.price+" Coins&a.";}
    public String setCategory(Player p,String category){Session s=session(p);if(s==null)return"&cNo active model draft.";s.category=FurnitureDefinition.Category.parse(category);return"&aSet category to &e"+s.category.displayName()+"&a.";}
    public String cancel(Player p){return sessions.remove(p.getUniqueId())==null?"&cNo active model draft.":"&eCancelled the model draft.";}
    public Optional<SmpFurnitureModel> draft(Player p){Session s=session(p);return s==null?Optional.empty():Optional.of(s.model());}

    public void save(Player p,Consumer<String> callback){Session current=session(p);if(current==null){callback.accept("&cNo active model draft.");return;}if(current.parts.isEmpty()){callback.accept("&cAdd at least one part.");return;}SmpFurnitureModel model=current.model();saveAsync(model,"local:editor",p.getUniqueId().toString(),message->{if(message.startsWith("&a"))sessions.remove(p.getUniqueId(),current);callback.accept(message);});}
    public void folderImportDisabled(Consumer<String> callback){callback.accept("&cFolder imports are disabled on this server. Use built-in furniture or database import.");}
    public void importBuiltin(Player player,String rawId,Consumer<String> callback){
        String id=clean(rawId);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            String result;
            try{
                Optional<SmpFurnitureModel> builtIn=registry.builtInModel(id);
                if(builtIn.isEmpty())result="&cBuilt-in furniture model not found: &e"+id;
                else if(isSaved(id))result="&cThat furniture is already registered.";
                else{saveBlocking(builtIn.get(),"builtin:java-registry",player.getUniqueId().toString());result="&aImported built-in furniture model &e"+id+"&a.";}
            }catch(Exception e){result="&cCould not import model: "+e.getMessage();}
            completeImport(callback,result);
        });
    }

    public void importJson(Player player,String rawId,String encoded,Consumer<String> callback){String id=clean(rawId);if(encoded.length()>1_400_000){callback.accept("&cPasted furniture data exceeds the input limit.");return;}Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{String result;try{byte[] bytes=Base64.getDecoder().decode(encoded);if(bytes.length>MAX_DECODED_BYTES)throw new IllegalArgumentException("decoded model exceeds 1 MB");String json=new String(bytes,StandardCharsets.UTF_8);SmpFurnitureModel model=importer.parse(id,json);saveBlocking(model,"local:base64-json",player.getUniqueId().toString());result="&aImported furniture model &e"+id+" &afrom pasted data.";}catch(IllegalArgumentException e){result="&cInvalid or unsafe furniture model data: "+e.getMessage();}catch(SQLException e){result="&cCould not save furniture model: "+e.getMessage();}completeImport(callback,result);});}
    public List<String> builtinIds(){return registry.builtInIds();}
    private boolean isSaved(String id)throws SQLException{if(!data.isMySqlEnabled())return true;try(Connection c=data.getConnection();PreparedStatement statement=c.prepareStatement("SELECT 1 FROM smp_furniture_entries WHERE furniture_id=? LIMIT 1")){statement.setString(1,id);try(ResultSet rows=statement.executeQuery()){return rows.next();}}}
    private void completeImport(Consumer<String> callback,String result){Bukkit.getScheduler().runTask(plugin,()->{registry.reload();callback.accept(result);});}
    private void saveAsync(SmpFurnitureModel model,String source,String createdBy,Consumer<String> callback){Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{String result;try{saveBlocking(model,source,createdBy);result="&aSaved furniture &e"+model.displayName()+"&a.";}catch(SQLException e){result="&cCould not save furniture: "+e.getMessage();}String message=result;Bukkit.getScheduler().runTask(plugin,()->{registry.reload();callback.accept(message);});});}
    private void saveBlocking(SmpFurnitureModel model,String source,String createdBy)throws SQLException{String json=engine.serialize(model);long now=System.currentTimeMillis();try(Connection c=data.getConnection()){boolean auto=c.getAutoCommit();c.setAutoCommit(false);try(PreparedStatement pack=c.prepareStatement("REPLACE INTO smp_furniture_packs(pack_id,source_url,source_json,enabled,installed_at) VALUES(?,?,?,?,?)");PreparedStatement item=c.prepareStatement("REPLACE INTO smp_furniture_entries VALUES(?,?,?,?,?,?,?,?,?,?,?,?)");PreparedStatement meta=c.prepareStatement("REPLACE INTO smp_furniture_model_meta(furniture_id,created_by,created_at) VALUES(?,?,?)")){pack.setString(1,model.id());pack.setString(2,source);pack.setString(3,json);pack.setBoolean(4,true);pack.setLong(5,now);pack.executeUpdate();item.setString(1,model.id());item.setString(2,model.id());item.setString(3,model.displayName());item.setString(4,model.category().name());item.setString(5,model.id());item.setString(6,json);item.setString(7,model.previewMaterial().name());item.setDouble(8,model.price());item.setString(9,"");item.setBoolean(10,true);item.setBoolean(11,true);item.setBoolean(12,true);item.executeUpdate();meta.setString(1,model.id());meta.setString(2,createdBy);meta.setLong(3,now);meta.executeUpdate();c.commit();}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(auto);}}}
    private Session session(Player p){return sessions.get(p.getUniqueId());}private static boolean valid(Session s,int i){return s!=null&&i>=0&&i<s.parts.size();}private static String clean(String v){return v==null?"":v.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_").replaceAll("_+","_");}
    private static final class Session{final String id,name;FurnitureDefinition.Category category;Material preview;double price;final List<SmpModelPart> parts;final double width,height;Session(String id,String name,FurnitureDefinition.Category category,Material preview,double price,List<SmpModelPart> parts,double width,double height){this.id=id;this.name=name;this.category=category;this.preview=preview;this.price=price;this.parts=parts;this.width=width;this.height=height;}SmpFurnitureModel model(){return new SmpFurnitureModel(id,name,category,preview,price,new ArrayList<>(parts),width,height,null,null);}}
}
