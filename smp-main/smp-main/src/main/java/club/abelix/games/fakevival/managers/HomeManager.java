package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.Game;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.Consumer;

public final class HomeManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;

    public HomeManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public boolean setHome(Player player, String name, Location location) {
        String homeName = normalize(name);
        if (!hasHome(player, homeName) && getHomes(player).size() >= getMaxHomes(player)) return false;
        saveLocation(path(player.getUniqueId(), homeName), location);
        String base = path(player.getUniqueId(), homeName);
        String serverId = plugin instanceof Game game ? game.getNetworkService().getCurrentServerId() : "local";
        String serverType = plugin instanceof Game game ? game.getNetworkService().getCurrentServerType().name() : "OVERWORLD";
        long now = System.currentTimeMillis();
        dataManager.getData().set(base + ".server-id", serverId);
        dataManager.getData().set(base + ".server-type", serverType);
        if (!dataManager.getData().contains(base + ".created-at")) dataManager.getData().set(base + ".created-at", now);
        dataManager.getData().set(base + ".updated-at", now);
        dataManager.save();
        persistHome(player.getUniqueId(), homeName);
        return true;
    }

    public Location getHome(Player player, String name) {
        return loadLocation(path(player.getUniqueId(), normalize(name)));
    }

    public HomeRecord getHomeRecord(Player player, String name) {
        return player == null ? null : getHomeRecord(player.getUniqueId(), name);
    }

    public void resolveHomeAsync(Player player, String name, Consumer<HomeRecord> callback) {
        String homeName = normalize(name); HomeRecord cached = getHomeRecord(player, homeName);
        if (cached != null && (!cached.serverId().isBlank() || !dataManager.isMySqlEnabled())) { callback.accept(cached); return; }
        if (!dataManager.isMySqlEnabled()) { callback.accept(cached); return; }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HomeRecord found = null;
            try (var connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("SELECT `server_id`,`server_type`,`world`,`x`,`y`,`z`,`yaw`,`pitch`,`created_at`,`updated_at` FROM `smp_homes` WHERE `uuid`=? AND `home_name`=?")) {
                statement.setString(1,player.getUniqueId().toString());statement.setString(2,homeName);
                try(ResultSet result=statement.executeQuery()){if(result.next())found=new HomeRecord(homeName,result.getString("server_id"),result.getString("server_type"),result.getString("world"),result.getDouble("x"),result.getDouble("y"),result.getDouble("z"),result.getFloat("yaw"),result.getFloat("pitch"),result.getLong("created_at"),result.getLong("updated_at"));}
            } catch (Exception exception) { plugin.getLogger().warning("[SMP] Could not load home " + homeName + ": " + exception.getMessage()); }
            HomeRecord resolved=found;
            Bukkit.getScheduler().runTask(plugin,()->{if(resolved!=null)cacheHome(player.getUniqueId(),resolved);callback.accept(resolved==null?cached:resolved);});
        });
    }

    private HomeRecord getHomeRecord(UUID uuid, String name) {
        String homeName = normalize(name), base = path(uuid, homeName);
        String world = dataManager.getData().getString(base + ".world");
        if (world == null) return null;
        return new HomeRecord(homeName,
                dataManager.getData().getString(base + ".server-id", ""),
                dataManager.getData().getString(base + ".server-type", ""), world,
                dataManager.getData().getDouble(base + ".x"), dataManager.getData().getDouble(base + ".y"),
                dataManager.getData().getDouble(base + ".z"), (float) dataManager.getData().getDouble(base + ".yaw"),
                (float) dataManager.getData().getDouble(base + ".pitch"),
                dataManager.getData().getLong(base + ".created-at"), dataManager.getData().getLong(base + ".updated-at"));
    }

    public boolean deleteHome(Player player, String name) {
        String homeName = normalize(name);
        if (!hasHome(player, homeName)) return false;
        dataManager.getData().set(path(player.getUniqueId(), homeName), null);
        dataManager.save();
        deletePersistedHome(player.getUniqueId(), homeName);
        return true;
    }

    public boolean renameHome(Player player, String oldName, String newName) {
        String normalizedOld = normalize(oldName);
        String normalizedNew = normalize(newName);
        if (!hasHome(player, normalizedOld) || hasHome(player, normalizedNew)) return false;
        Location location = getHome(player, normalizedOld);
        if (location == null) return false;
        dataManager.getData().set(path(player.getUniqueId(), normalizedOld), null);
        saveLocation(path(player.getUniqueId(), normalizedNew), location);
        dataManager.save();
        persistHome(player.getUniqueId(), normalizedNew);
        deletePersistedHome(player.getUniqueId(), normalizedOld);
        return true;
    }

    public List<String> getHomes(Player player) {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("homes." + player.getUniqueId());
        if (section == null) return Collections.emptyList();
        List<String> homes = new ArrayList<>(section.getKeys(false));
        homes.sort(String.CASE_INSENSITIVE_ORDER);
        return homes;
    }

    public String normalize(String name) {
        if (name == null || name.isBlank()) return "home";
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }

    public int getMaxHomes(Player player) {
        if (player.hasPermission("fakevival.command.home.unlimited")) return Integer.MAX_VALUE;
        int base = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("homes.max-homes", 5));
        int extra = dataManager.getData().getInt("players." + player.getUniqueId() + ".store.upgrades.extra_homes", 0);
        return Math.max(1, base + Math.max(0, extra));
    }

    private boolean hasHome(Player player, String name) {
        return dataManager.getData().isConfigurationSection(path(player.getUniqueId(), normalize(name)));
    }

    private String path(UUID uuid, String name) {
        return "homes." + uuid + "." + name;
    }

    private void saveLocation(String path, Location location) {
        dataManager.getData().set(path + ".world", location.getWorld().getName());
        dataManager.getData().set(path + ".x", location.getX());
        dataManager.getData().set(path + ".y", location.getY());
        dataManager.getData().set(path + ".z", location.getZ());
        dataManager.getData().set(path + ".yaw", location.getYaw());
        dataManager.getData().set(path + ".pitch", location.getPitch());
    }

    private Location loadLocation(String path) {
        String worldName = dataManager.getData().getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                dataManager.getData().getDouble(path + ".x"),
                dataManager.getData().getDouble(path + ".y"),
                dataManager.getData().getDouble(path + ".z"),
                (float) dataManager.getData().getDouble(path + ".yaw"),
                (float) dataManager.getData().getDouble(path + ".pitch"));
    }

    private void persistHome(UUID uuid, String homeName) {
        HomeRecord home = getHomeRecord(uuid, homeName);
        if (home == null) return;
        dataManager.runDatabaseAsync("save-home", connection -> {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO `smp_homes` (`uuid`,`home_name`,`server_id`,`server_type`,`world`,`x`,`y`,`z`,`yaw`,`pitch`,`created_at`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `server_id`=VALUES(`server_id`),`server_type`=VALUES(`server_type`),`world`=VALUES(`world`),`x`=VALUES(`x`),`y`=VALUES(`y`),`z`=VALUES(`z`),`yaw`=VALUES(`yaw`),`pitch`=VALUES(`pitch`),`updated_at`=VALUES(`updated_at`)")) {
                statement.setString(1, uuid.toString()); statement.setString(2, homeName); statement.setString(3, home.serverId()); statement.setString(4, home.serverType()); statement.setString(5, home.world()); statement.setDouble(6, home.x()); statement.setDouble(7, home.y()); statement.setDouble(8, home.z()); statement.setFloat(9, home.yaw()); statement.setFloat(10, home.pitch()); statement.setLong(11, home.createdAt()); statement.setLong(12, home.updatedAt()); statement.executeUpdate();
            }
        });
    }

    private void deletePersistedHome(UUID uuid, String homeName) { dataManager.runDatabaseAsync("delete-home", connection -> { try (PreparedStatement statement=connection.prepareStatement("DELETE FROM `smp_homes` WHERE `uuid`=? AND `home_name`=?")){statement.setString(1,uuid.toString());statement.setString(2,homeName);statement.executeUpdate();} }); }

    private void cacheHome(UUID uuid,HomeRecord home){String base=path(uuid,home.name());dataManager.getData().set(base+".server-id",home.serverId());dataManager.getData().set(base+".server-type",home.serverType());dataManager.getData().set(base+".world",home.world());dataManager.getData().set(base+".x",home.x());dataManager.getData().set(base+".y",home.y());dataManager.getData().set(base+".z",home.z());dataManager.getData().set(base+".yaw",home.yaw());dataManager.getData().set(base+".pitch",home.pitch());dataManager.getData().set(base+".created-at",home.createdAt());dataManager.getData().set(base+".updated-at",home.updatedAt());}

    public record HomeRecord(String name, String serverId, String serverType, String world, double x, double y,
                             double z, float yaw, float pitch, long createdAt, long updatedAt) {
        public boolean isOverworld() { return "OVERWORLD".equalsIgnoreCase(serverType) && serverId != null && !serverId.isBlank(); }
    }
}
