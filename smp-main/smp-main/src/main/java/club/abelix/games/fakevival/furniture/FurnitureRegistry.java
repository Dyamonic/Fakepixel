package club.abelix.games.fakevival.furniture;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class FurnitureRegistry {
    private final JavaPlugin plugin;
    private final DataManager data;
    private final Map<String, FurnitureDefinition> entries = new ConcurrentHashMap<>();
    private final Set<String> builtInIds = ConcurrentHashMap.newKeySet();
    private boolean loggedBuiltInCount;

    public FurnitureRegistry(JavaPlugin plugin, DataManager data) { this.plugin = plugin; this.data = data; }

    public void reload() {
        entries.clear();
        builtInIds.clear();
        registerBuiltIns();
        if (!data.isMySqlEnabled()) return;
        try (java.sql.Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT e.* FROM smp_furniture_entries e JOIN smp_furniture_packs p ON p.pack_id=e.pack_id WHERE p.enabled=TRUE")) {
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    Material material = Material.matchMaterial(row.getString("preview_material"));
                    FurnitureDefinition definition = new FurnitureDefinition(row.getString("furniture_id"), row.getString("pack_id"),
                            row.getString("display_name"), FurnitureDefinition.Category.parse(row.getString("category")),
                            row.getString("source_id"), row.getString("entity_json"), material == null ? Material.ARMOR_STAND : material,
                            row.getDouble("price"), row.getString("permission"), row.getBoolean("placeable"),
                            row.getBoolean("player_breakable"), row.getBoolean("spawn_protected"));
                    if (hasLocalModelData(definition.entityJson())) entries.put(definition.id().toLowerCase(Locale.ROOT), definition);
                }
            }
        } catch (SQLException exception) { plugin.getLogger().severe("Could not load furniture registry: " + exception.getMessage()); }
    }

    private void registerBuiltIns() {
        builtIn("wooden_stool", "Wooden Stool", FurnitureDefinition.Category.CHAIRS, Material.OAK_STAIRS, "OAK_STAIRS", 75);
        builtIn("small_table", "Small Table", FurnitureDefinition.Category.TABLES, Material.OAK_SLAB, "OAK_SLAB", 100);
        builtIn("lantern_stand", "Lantern Stand", FurnitureDefinition.Category.LAMPS, Material.LANTERN, "LANTERN", 125);
        builtIn("potted_plant", "Potted Plant", FurnitureDefinition.Category.PLANTS, Material.FLOWER_POT, "FLOWER_POT", 90);
        builtInModel(new SmpFurnitureModel("wooden_chair","Wooden Chair",FurnitureDefinition.Category.CHAIRS,Material.OAK_STAIRS,175,List.of(
                SmpModelPart.defaultPart(Material.OAK_STAIRS),new SmpModelPart(Material.OAK_TRAPDOOR,SmpModelPart.EntityType.BLOCK_DISPLAY,null,0,.75,.35,0,0,0,.8f,true,false,true,false,false,false)),1,1.5,null,null));
        builtInModel(new SmpFurnitureModel("bar_table","Bar Table",FurnitureDefinition.Category.TABLES,Material.OAK_SLAB,250,List.of(
                new SmpModelPart(Material.OAK_FENCE,SmpModelPart.EntityType.BLOCK_DISPLAY,null,0,0,0,0,0,0,1,true,false,true,false,false,false),
                new SmpModelPart(Material.OAK_SLAB,SmpModelPart.EntityType.BLOCK_DISPLAY,null,0,.9,0,0,0,0,1.2f,true,false,true,false,false,false)),1.5,1.25,null,null));
        builtInModel(new SmpFurnitureModel("big_bed_v01","Big Bed",FurnitureDefinition.Category.BEDS,Material.RED_BED,400,List.of(
                new SmpModelPart(Material.RED_BED,SmpModelPart.EntityType.ITEM_DISPLAY,null,0,.25,0,0,0,0,1.5f,true,false,true,false,false,false),
                new SmpModelPart(Material.OAK_SLAB,SmpModelPart.EntityType.BLOCK_DISPLAY,null,0,0,.8,0,0,0,1.5f,true,false,true,false,false,false)),2,1,null,null));
        int generated = FurnitureRegistryData.registerAll(this);
        if (!loggedBuiltInCount) {
            plugin.getLogger().info("Registered " + (generated + 7) + " built-in furniture models from Java registry.");
            loggedBuiltInCount = true;
        }
    }

    private void builtIn(String id, String name, FurnitureDefinition.Category category, Material icon, String helmet, double price) {
        String entity = "{\"invisible\":true,\"gravity\":false,\"base_plate\":false,\"arms\":false,\"small\":false,\"marker\":false,\"helmet\":\"" + helmet + "\"}";
        FurnitureDefinition definition = new FurnitureDefinition(id, "built_in", name, category, id, entity, icon, price, "", true, true, true);
        entries.put(id, definition);
        builtInIds.add(id);
    }
    private void builtInModel(SmpFurnitureModel model){registerBuiltIn(model);}
    void registerBuiltIn(SmpFurnitureModel model){String id=model.id().toLowerCase(Locale.ROOT);String json=new SmpModelEngine().serialize(model);FurnitureDefinition definition=new FurnitureDefinition(id,"built_in",model.displayName(),model.category(),id,json,model.previewMaterial(),model.price(),"",true,true,true);entries.put(id,definition);builtInIds.add(id);}

    public List<String> builtInIds() { return builtInIds.stream().sorted().toList(); }
    public Optional<SmpFurnitureModel> builtInModel(String id) { String key=id==null?"":id.toLowerCase(Locale.ROOT);if(!builtInIds.contains(key))return Optional.empty();return get(key).map(new SmpModelEngine()::parse); }

    public Optional<FurnitureDefinition> get(String id) { return Optional.ofNullable(entries.get(id.toLowerCase(Locale.ROOT))); }
    public List<FurnitureDefinition> all() { return entries.values().stream().sorted(Comparator.comparing(FurnitureDefinition::displayName)).toList(); }
    public List<FurnitureDefinition> category(FurnitureDefinition.Category category) { return all().stream().filter(e -> e.category() == category).toList(); }
    public Map<String, Long> packCounts() {
        Map<String, Long> counts = new TreeMap<>();
        for (FurnitureDefinition entry : entries.values()) counts.merge(entry.packId(), 1L, Long::sum);
        return counts;
    }

    private boolean hasLocalModelData(String json) {
        try {
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            return !data.isEmpty() && (data.has("helmet") || data.has("chestplate") || data.has("leggings") || data.has("boots")
                    || data.has("main_hand") || data.has("off_hand") || data.has("parts"));
        } catch (Exception ignored) { return false; }
    }

    public void delete(String id, java.util.function.Consumer<Boolean> callback) {
        String key = id == null ? "" : id.toLowerCase(Locale.ROOT);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean deleted = false;
            try (java.sql.Connection connection = data.getConnection()) {
                try (PreparedStatement placed = connection.prepareStatement("SELECT 1 FROM smp_placed_furniture WHERE furniture_id=? LIMIT 1")) {
                    placed.setString(1, key); try (ResultSet rows = placed.executeQuery()) { if (rows.next()) throw new SQLException("furniture is currently placed"); }
                }
                boolean auto = connection.getAutoCommit(); connection.setAutoCommit(false);
                try (PreparedStatement entry = connection.prepareStatement("DELETE FROM smp_furniture_entries WHERE furniture_id=?");
                     PreparedStatement pack = connection.prepareStatement("DELETE FROM smp_furniture_packs WHERE pack_id=? AND NOT EXISTS (SELECT 1 FROM smp_furniture_entries WHERE pack_id=?)");
                     PreparedStatement meta = connection.prepareStatement("DELETE FROM smp_furniture_model_meta WHERE furniture_id=?")) {
                    entry.setString(1,key); deleted=entry.executeUpdate()>0; pack.setString(1,key); pack.setString(2,key); pack.executeUpdate();meta.setString(1,key);meta.executeUpdate();connection.commit();
                } catch (SQLException exception) { connection.rollback(); } finally { connection.setAutoCommit(auto); }
            } catch (SQLException ignored) {}
            boolean result=deleted; plugin.getServer().getScheduler().runTask(plugin,()->{reload();callback.accept(result);});
        });
    }
}
