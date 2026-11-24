package net.fakepixel.housing.house;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.world.WorldService;
import net.fakepixel.housing.database.HousingRepository;
import net.fakepixel.housing.cluster.PendingRouteAction;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;

import java.util.*;

public class HouseManager {
    private final HousingPlugin plugin;
    private final WorldService worldService;
    private final Map<String, House> houses = new HashMap<>();
    private final HousingRepository repository;
    private final Map<UUID, UUID> invites = new HashMap<>();
    private final Set<String> dirtyHouses = new HashSet<>();
    private int dirtySaveTask = -1;
    private final Set<String> maintenanceHouses = new HashSet<>();
    private volatile java.util.List<House> browseCache = Collections.emptyList();

    public HouseManager(HousingPlugin plugin, WorldService worldService, HousingRepository repository) {
        this.plugin = plugin;
        this.worldService = worldService;
        this.repository = repository;
        loadAll();
        if(repository!=null)try{invites.putAll(repository.loadInvites());}catch(Exception ex){plugin.getLogger().warning("Could not load Housing invites: "+ex.getMessage());}
        dirtySaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::flushDirtyHouses, 100L, 100L).getTaskId();
    }

    public void loadAll() {
        houses.clear();
        if (repository == null) return;
        try { for (House house : repository.loadAllHouses()) houses.put(key(house.getOwner(), house.getPlotId()), house); refreshBrowseCacheFrom(houses.values()); }
        catch (Exception ex) { plugin.getLogger().severe("Failed to load houses from MariaDB: " + ex.getMessage()); }
    }

    public void saveAll() {
        if (repository == null) return;
        try { repository.saveHouses(houses.values()); }
        catch (Exception ex) { plugin.getLogger().severe("Could not save houses to MariaDB: " + ex.getMessage()); }
    }
    public void markDirty(House house) { if (house != null) dirtyHouses.add(house.getOwner()+"#"+house.getPlotId()); }
    public void flushDirtyHouses() { if(repository==null||dirtyHouses.isEmpty())return;List<House> pending=new ArrayList<>();for(String id:new HashSet<>(dirtyHouses)){House house=getHouseById(id);if(house!=null)pending.add(house);}try{repository.saveHouses(pending);dirtyHouses.removeAll(new HashSet<>(dirtyHouses));}catch(Exception ex){plugin.getLogger().warning("Could not flush dirty houses: "+ex.getMessage());}}
    public void shutdown(){if(dirtySaveTask!=-1)Bukkit.getScheduler().cancelTask(dirtySaveTask);flushDirtyHouses();saveAll();}

    public House getHouse(UUID uuid) { return getHouse(uuid, 1); }

    public House getHouse(UUID uuid, int plotId) { return houses.get(key(uuid, plotId)); }

    public Collection<House> getHouses() { return Collections.unmodifiableCollection(houses.values()); }
    public House getHouseById(String houseId) {
        if (houseId == null) return null;
        for (House house : houses.values()) if (houseId.equals(house.getOwner() + "#" + house.getPlotId())) return house;
        return null;
    }
    public void registerHouseMetadata(House house) {
        if (house != null) houses.put(key(house.getOwner(), house.getPlotId()), house);
    }

    public java.util.List<House> getPublicHouses() {
        if (plugin.getCurrentServer() != null && plugin.getCurrentServer().isLobby()) return new java.util.ArrayList<>(browseCache);
        java.util.List<House> list = new java.util.ArrayList<>();
        for (House house : houses.values()) {
            if (house.isPublicHouse()) list.add(house);
        }
        list.sort((a, b) -> {
            int byCookies = Integer.compare(b.getCookies(), a.getCookies());
            if (byCookies != 0) return byCookies;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return list;
    }

    public void refreshBrowseCache() {
        if (repository == null) return;
        try { refreshBrowseCacheFrom(repository.loadAllHouses()); }
        catch (Exception ex) { plugin.getLogger().warning("Could not refresh Housing browse cache: " + ex.getMessage()); }
    }
    private void refreshBrowseCacheFrom(Collection<House> source) {
        java.util.List<House> list=new java.util.ArrayList<>(); for(House h:source)if(h.isPublicHouse())list.add(h);
        list.sort((a,b)->{int c=Integer.compare(b.getCookies(),a.getCookies());return c!=0?c:a.getName().compareToIgnoreCase(b.getName());});
        browseCache=Collections.unmodifiableList(list);
    }

    public java.util.List<House> getTopCookieHouses(int limit) {
        java.util.List<House> list = new java.util.ArrayList<>(houses.values());
        list.sort((a, b) -> Integer.compare(b.getCookies(), a.getCookies()));
        if (list.size() > limit) return new java.util.ArrayList<>(list.subList(0, limit));
        return list;
    }

    public House getRandomPublicHouse() {
        java.util.List<House> list = getPublicHouses();
        if (list.isEmpty()) return null;
        return list.get(new java.util.Random().nextInt(list.size()));
    }

    public int maxPlots() {
        return Math.max(1, plugin.getStaticConfig().getInt("housing-plots.max-plots", 6));
    }

    public int defaultOwnedPlots() {
        return Math.max(1, Math.min(maxPlots(), plugin.getStaticConfig().getInt("housing-plots.default-owned", 1)));
    }

    public boolean isPlotOwned(UUID owner, int plotId) {
        int cleanPlot = normalizePlotId(plotId);
        return cleanPlot <= defaultOwnedPlots() || houses.containsKey(key(owner, cleanPlot));
    }

    public int plotPrice(int plotId) {
        return Math.max(0, plugin.getStaticConfig().getInt("housing-plots.prices." + plotId, 0));
    }

    public boolean purchasePlot(Player player, int plotId) {
        if (player == null) return false;
        int cleanPlot = normalizePlotId(plotId);
        if (isPlotOwned(player.getUniqueId(), cleanPlot)) return true;
        int price = plotPrice(cleanPlot);
        if (plugin.getAbelixHook() == null || !plugin.getAbelixHook().takeGold(player, price)) return false;
        getOrCreate(player, cleanPlot);
        saveAll();
        return true;
    }

    public House getOrCreate(Player player) {
        return getOrCreate(player, 1);
    }

    public House getOrCreate(Player player, int plotId) {
        int cleanPlot = normalizePlotId(plotId);
        House house = houses.get(key(player.getUniqueId(), cleanPlot));
        if (house != null) {
            house.setOwnerName(player.getName());
            return house;
        }
        String template = plugin.getStaticConfig().getString("settings.default-template", "basic");
        java.util.List<String> staticWorlds = plugin.getStaticConfig().getStringList("settings.static-worlds");
        if (staticWorlds.contains(template)) {
            java.util.List<String> allowedTemplates = plugin.getStaticConfig().getStringList("slimeworldmanager.template-worlds");
            template = allowedTemplates.isEmpty() ? "basic" : allowedTemplates.get(0);
            plugin.getLogger().warning("settings.default-template was set to a static world. Using " + template + " instead.");
        }
        String prefix = plugin.getStaticConfig().getString("slimeworldmanager.world-prefix", "house_");
        String worldName = prefix + player.getUniqueId().toString().replace("-", "")
                + (cleanPlot <= 1 ? "" : "_" + cleanPlot) + "_" + UUID.randomUUID().toString().substring(0, 8);
        house = new House(player.getUniqueId(), player.getName(), template, worldName, cleanPlot);
        house.setPlotSize(31);
        house.setScoreboardEnabled(true);
        if (cleanPlot > 1) house.setName(player.getName() + "'s House #" + cleanPlot);
        house.setMode(plugin.defaultHouseMode());
        houses.put(key(player.getUniqueId(), cleanPlot), house);
        saveAll();
        return house;
    }

    public void sendToOwnHouse(Player player) {
        try {
            player.sendMessage(plugin.msg("loading"));
            boolean existed = getHouse(player.getUniqueId(), 1) != null;
            House house = getOrCreate(player);
            if (plugin.getClusterService() != null && !plugin.getClusterService().ensureLocal(player, house, existed ? PendingRouteAction.OWN_HOUSE : PendingRouteAction.CREATE_HOUSE)) return;
            World world = worldService.loadHouseWorld(house);
            prepareHouseWorld(world);
            ensureBorder(world, true);
            plugin.getHousingMenu().applyHouseWeather(house);
            plugin.getHousingMenu().applyHouseTime(house);
            plugin.getHousingMenu().applyHouseBiomeSky(house);
            plugin.getHousingNpcService().ensureCarpenter(world);
            if (plugin.getHologramService() != null) plugin.getHologramService().spawnAll(house, world);
            if (plugin.getHouseNpcService() != null) plugin.getHouseNpcService().spawnAll(house, world);
            house.setLastOpened(System.currentTimeMillis());
            saveAll();
            java.util.List<Player> alreadyInside = new java.util.ArrayList<>(world.getPlayers());
            alreadyInside.remove(player);
            House leavingHouse = getHouseByWorld(player.getWorld().getName());
            if (leavingHouse != null && !leavingHouse.getWorldName().equals(house.getWorldName())) sendLeaveMessages(player, leavingHouse);
            clearInventory(player);
            teleport(player, house, world, false);
            if (house.isPvpEnabled() && house.hasPvpLayout()) applyPvpLayout(player, house);
            else giveOwnerMenuItem(player);
            sendJoinMessages(player, house, alreadyInside);
            player.sendMessage(plugin.msg("created"));
        } catch (Exception ex) {
            player.sendMessage(plugin.color("&cCould not load your house: " + ex.getMessage()));
            plugin.getLogger().warning("House load failed for " + player.getName() + ": " + ex.getMessage());
            if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) plugin.getLogger().log(java.util.logging.Level.WARNING, "House error", ex);
        }
    }

    public void sendToHousePlot(Player player, int plotId) {
        try {
            player.sendMessage(plugin.msg("loading"));
            boolean existed = getHouse(player.getUniqueId(), plotId) != null;
            House house = getOrCreate(player, plotId);
            if (plugin.getClusterService() != null && !plugin.getClusterService().ensureLocal(player, house, existed ? PendingRouteAction.OWN_HOUSE : PendingRouteAction.CREATE_HOUSE)) return;
            World world = worldService.loadHouseWorld(house);
            prepareHouseWorld(world);
            ensureBorder(world, true);
            plugin.getHousingMenu().applyHouseWeather(house);
            plugin.getHousingMenu().applyHouseTime(house);
            plugin.getHousingMenu().applyHouseBiomeSky(house);
            plugin.getHousingNpcService().ensureCarpenter(world);
            if (plugin.getHologramService() != null) plugin.getHologramService().spawnAll(house, world);
            if (plugin.getHouseNpcService() != null) plugin.getHouseNpcService().spawnAll(house, world);
            house.setLastOpened(System.currentTimeMillis());
            saveAll();
            java.util.List<Player> alreadyInside = new java.util.ArrayList<>(world.getPlayers());
            alreadyInside.remove(player);
            House leavingHouse = getHouseByWorld(player.getWorld().getName());
            if (leavingHouse != null && !leavingHouse.getWorldName().equals(house.getWorldName())) sendLeaveMessages(player, leavingHouse);
            clearInventory(player);
            teleport(player, house, world, false);
            if (house.isPvpEnabled() && house.hasPvpLayout()) applyPvpLayout(player, house);
            else giveOwnerMenuItem(player);
            sendJoinMessages(player, house, alreadyInside);
            player.sendMessage(plugin.msg("created"));
        } catch (Exception ex) {
            player.sendMessage(plugin.color("&cCould not load your house: " + ex.getMessage()));
            plugin.getLogger().warning("House load failed for " + player.getName() + ": " + ex.getMessage());
        }
    }

    public void visit(Player player, House house) {
        try {
            player.sendMessage(plugin.msg("visiting").replace("%owner%", house.getOwnerName()));
            if (house.isBanned(player.getUniqueId()) && !isStaff(player)) {
                player.sendMessage(plugin.color("&cYou are banned from that house."));
                return;
            }
            if (!canEnter(player, house)) {
                player.sendMessage(plugin.color("&cYou cannot visit that house right now."));
                return;
            }
            if (plugin.getHousingMenu() != null) plugin.getHousingMenu().recordRecentVisit(player, house);
            if (plugin.getClusterService() != null && !plugin.getClusterService().ensureLocal(player, house, PendingRouteAction.VISIT_HOUSE)) return;
            World world = worldService.loadHouseWorld(house);
            prepareHouseWorld(world);
            ensureBorder(world, true);
            plugin.getHousingMenu().applyHouseWeather(house);
            plugin.getHousingMenu().applyHouseTime(house);
            plugin.getHousingMenu().applyHouseBiomeSky(house);
            plugin.getHousingNpcService().ensureCarpenter(world);
            if (plugin.getHologramService() != null) plugin.getHologramService().spawnAll(house, world);
            if (plugin.getHouseNpcService() != null) plugin.getHouseNpcService().spawnAll(house, world);
            house.setLastOpened(System.currentTimeMillis());
            saveAll();
            java.util.List<Player> alreadyInside = new java.util.ArrayList<>(world.getPlayers());
            alreadyInside.remove(player);
            House leavingHouse = getHouseByWorld(player.getWorld().getName());
            if (leavingHouse != null && !leavingHouse.getWorldName().equals(house.getWorldName())) sendLeaveMessages(player, leavingHouse);
            clearInventory(player);
            teleport(player, house, world, true);
            if (house.isPvpEnabled()) applyPvpLayout(player, house);
            else updateVisitItems(player);
            enforceVisitorState(player);
            sendJoinMessages(player, house, alreadyInside);
        } catch (Exception ex) {
            player.sendMessage(plugin.color("&cCould not visit that house: " + ex.getMessage()));
        }
    }

    public boolean completePendingRoute(Player player, House house, PendingRouteAction action) {
        if (player == null || house == null || action == null) return false;
        if (action == PendingRouteAction.VISIT_HOUSE) visit(player, house);
        else if (player.getUniqueId().equals(house.getOwner())) sendToHousePlot(player, house.getPlotId());
        else visit(player, house);
        return player.getWorld() != null && house.getWorldName().equals(player.getWorld().getName());
    }

    public void enforceVisitorState(Player player) {
        House house = player == null ? null : getHouseByWorld(player.getWorld().getName());
        if (house == null || house.getOwner().equals(player.getUniqueId())) return;
        if (player.getGameMode() != GameMode.SURVIVAL) player.setGameMode(GameMode.SURVIVAL);
    }

    public boolean isVisitingOtherHouse(Player player) {
        House house = player == null ? null : getHouseByWorld(player.getWorld().getName());
        return house != null && !house.getOwner().equals(player.getUniqueId());
    }

    public boolean canBuildInCurrentHouse(Player player) {
        House house = player == null ? null : getHouseByWorld(player.getWorld().getName());
        return house == null || plugin.canBuildInHouseMode(player, house);
    }

    public House findByOwnerName(String name) {
        for (House house : houses.values()) {
            if (house.getOwnerName().equalsIgnoreCase(name)) return house;
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return getHouse(online.getUniqueId());
        return null;
    }

    private void teleport(Player player, House house, World world, boolean guestPreferred) {
        player.teleport(spawnLocation(house, world, guestPreferred));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            House current = getHouseByWorld(player.getWorld().getName());
            if (current != null) {
                refreshBorderVisibility(player, current);
                if (plugin.getHousingScoreboard() != null) plugin.getHousingScoreboard().showHouseScoreboard(player, current);
            }
        }, 2L);
    }

    public Location spawnLocation(House house, World world, boolean guestPreferred) {
        if (world == null && house != null) world = Bukkit.getWorld(house.getWorldName());
        if (world == null) world = Bukkit.getWorlds().get(0);
        Location saved = null;
        if (house != null && guestPreferred) saved = house.getGuestSpawn();
        if (saved == null && house != null) saved = house.getSpawn();
        if (saved != null) {
            Location fixed = saved.clone();
            fixed.setWorld(world);
            if (fixed.getY() > 1.0D) return fixed;
        }
        double x = plugin.getStaticConfig().getDouble("settings.house-spawn.x", -3.0);
        double y = plugin.getStaticConfig().getDouble("settings.house-spawn.y", 110.0);
        double z = plugin.getStaticConfig().getDouble("settings.house-spawn.z", -57.0);
        float yaw = (float) plugin.getStaticConfig().getDouble("settings.house-spawn.yaw", 0.0);
        float pitch = (float) plugin.getStaticConfig().getDouble("settings.house-spawn.pitch", 0.0);
        if (y <= 1.0D) y = Math.max(2.0D, world.getSpawnLocation().getY());
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void leave(Player player) {
        if (plugin.getCurrentServer() != null && plugin.getCurrentServer().isInstance()) {
            if (plugin.getClusterService() != null) plugin.getClusterService().sendPlayerToLobby(player, "Housing leave", plugin.msg("left"));
            return;
        }
        player.sendMessage(plugin.color("&eYou are already in Housing."));
    }

    public void teleportToLobby(Player player, boolean sendMessage) {
        if (plugin.getCurrentServer() != null && plugin.getCurrentServer().isInstance()) {
            if (plugin.getClusterService() != null) plugin.getClusterService().sendPlayerToLobby(player, "Local lobby teleport intercepted", sendMessage ? plugin.msg("left") : "");
            return;
        }
        House leavingHouse = getHouseByWorld(player.getWorld().getName());
        if (leavingHouse != null) sendLeaveMessages(player, leavingHouse);
        Location spawn = lobbySpawnLocation();
        clearInventory(player);
        player.teleport(spawn);
        player.setFallDistance(0.0F);
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        giveLobbyItems(player);
        if (sendMessage) player.sendMessage(plugin.msg("left"));
    }

    public Location lobbySpawnLocation() {
        World lobby = Bukkit.getWorld(plugin.getStaticConfig().getString("settings.lobby-world", "world"));
        if (lobby == null) lobby = Bukkit.getWorld(plugin.getStaticConfig().getString("settings.fallback-spawn-world", "world"));
        if (lobby == null) lobby = Bukkit.getWorlds().get(0);
        double x = plugin.getStaticConfig().getDouble("settings.lobby-spawn.x", lobby.getSpawnLocation().getX());
        double y = plugin.getStaticConfig().getDouble("settings.lobby-spawn.y", lobby.getSpawnLocation().getY());
        double z = plugin.getStaticConfig().getDouble("settings.lobby-spawn.z", lobby.getSpawnLocation().getZ());
        float yaw = (float) plugin.getStaticConfig().getDouble("settings.lobby-spawn.yaw", 0.0);
        float pitch = (float) plugin.getStaticConfig().getDouble("settings.lobby-spawn.pitch", 0.0);
        Location configured = new Location(lobby, x, y, z, yaw, pitch);
        return safeLobbySpawn(configured);
    }

    private Location safeLobbySpawn(Location location) {
        if (location == null || location.getWorld() == null) return location;
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        world.loadChunk(x >> 4, z >> 4);
        int y = Math.max(1, Math.min(world.getMaxHeight() - 2, location.getBlockY()));
        for (int i = 0; i < 8 && (isUnsafeSpawnBlock(world, x, y, z) || isUnsafeSpawnBlock(world, x, y + 1, z)); i++) {
            y++;
            if (y >= world.getMaxHeight() - 2) break;
        }
        if (isUnsafeSpawnBlock(world, x, y, z) || isUnsafeSpawnBlock(world, x, y + 1, z)) {
            y = Math.max(y, world.getHighestBlockYAt(x, z) + 1);
        }
        Location safe = location.clone();
        safe.setY(Math.min(world.getMaxHeight() - 2, y));
        return safe;
    }

    private boolean isUnsafeSpawnBlock(World world, int x, int y, int z) {
        if (world == null || y < 0 || y >= world.getMaxHeight()) return true;
        org.bukkit.Material type = world.getBlockAt(x, y, z).getType();
        if (type == null) return false;
        String name = type.name();
        return type.isSolid() || name.contains("WATER") || name.contains("LAVA") || name.contains("FIRE");
    }

    private void sendJoinMessages(Player player, House house, java.util.List<Player> alreadyInside) {
        if (player == null || house == null) return;
        if (house.isJoinMessageEnabled()) {
            player.sendMessage(formatVisitMessage(house.getJoinMessage(), player, house));
        }
        if (!house.isAnnounceJoinsEnabled() || alreadyInside == null) return;
        String message = formatVisitMessage("&7[&a+&7] %abelix_prefix%%player% &ejoined the house.", player, house);
        for (Player recipient : alreadyInside) {
            if (recipient != null && recipient.isOnline() && recipient.getWorld().getName().equals(house.getWorldName())) {
                recipient.sendMessage(message);
            }
        }
    }

    private void sendLeaveMessages(Player player, House house) {
        if (player == null || house == null) return;
        if (house.isLeaveMessageEnabled()) {
            player.sendMessage(formatVisitMessage(house.getLeaveMessage(), player, house));
        }
        if (!house.isAnnounceLeavesEnabled()) return;
        String message = formatVisitMessage("&7[&c-&7] %abelix_prefix%%player% &eleft the house.", player, house);
        World world = Bukkit.getWorld(house.getWorldName());
        if (world == null) return;
        for (Player recipient : world.getPlayers()) {
            if (!recipient.getUniqueId().equals(player.getUniqueId())) recipient.sendMessage(message);
        }
    }

    private String formatVisitMessage(String raw, Player player, House house) {
        String message = raw == null ? "" : raw;
        String playerName = "";
        String abelixPrefix = "";
        if (player != null) {
            playerName = player.getName();
            if (plugin.getAbelixHook() != null) {
                abelixPrefix = plugin.getAbelixHook().getPrefix(player);
                String nameColor = plugin.getAbelixHook().getNameColor(player.getUniqueId(), player.getName());
                if (nameColor != null && !nameColor.trim().isEmpty()) playerName = nameColor + playerName;
            }
        }
        message = message.replace("%abelix_prefix%", abelixPrefix == null ? "" : abelixPrefix);
        message = message.replace("%player%", playerName);
        message = message.replace("%owner%", house == null ? "" : house.getOwnerName());
        message = message.replace("%house_name%", house == null ? "" : house.getName());
        message = message.replace("%guests%", house == null ? "0" : String.valueOf(getGuests(house)));
        message = message.replace("%cookies%", house == null ? "0" : String.valueOf(house.getCookies()));
        message = message.replace("%weekly_cookies%", house == null ? "0" : String.valueOf(house.getWeeklyCookies()));
        message = message.replace("%housing_group%", house == null || player == null ? "" : house.groupSuffix(house.getGroup(player.getUniqueId())));
        return plugin.color(message);
    }

    public House getHouseByWorld(String worldName) {
        for (House house : houses.values()) if (house.getWorldName().equals(worldName)) return house;
        return null;
    }

    public boolean giveCookie(Player visitor, House house) {
        if (house == null) return false;
        boolean given = house.giveCookie(visitor.getUniqueId(), cookieAmount(visitor));
        if (given) saveAll();
        return given;
    }

    private int cookieAmount(Player player) {
        int amount = plugin.getStaticConfig().getInt("cookies.default", 1);
        java.util.Set<String> rankIds = new java.util.HashSet<>();
        if (plugin.getAbelixHook() != null) {
            for (String rank : plugin.getAbelixHook().getRankIdentifiers(player)) rankIds.add(normalizeRankId(rank));
        }
        org.bukkit.configuration.ConfigurationSection section = plugin.getStaticConfig().getConfigurationSection("cookies");
        if (section == null) return Math.max(1, amount);
        for (String key : section.getKeys(false)) {
            if ("item-slot".equalsIgnoreCase(key) || !plugin.getStaticConfig().isInt("cookies." + key)) continue;
            String normalizedKey = normalizeRankId(key);
            boolean matchesRank = rankIds.contains(normalizedKey);
            boolean matchesPermission = player.hasPermission("abelix.rank." + key)
                    || player.hasPermission("rank." + key)
                    || player.hasPermission("group." + key)
                    || player.hasPermission("housing.cookies." + key);
            if (matchesRank || matchesPermission) {
                amount = Math.max(amount, plugin.getStaticConfig().getInt("cookies." + key, 1));
            }
        }
        return Math.max(1, amount);
    }

    private String normalizeRankId(String rank) {
        if (rank == null) return "";
        String clean = org.bukkit.ChatColor.stripColor(plugin.color(rank)).toLowerCase(java.util.Locale.ENGLISH);
        clean = clean.replace("++", "_plus_plus").replace("+", "_plus");
        return clean.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public int getGuests(House house) {
        if (house == null) return 0;
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(house.getWorldName());
        if (world == null) return 0;
        int guests = 0;
        for (Player player : world.getPlayers()) {
            if (!house.getOwner().equals(player.getUniqueId())) guests++;
        }
        return guests;
    }


    public void updateVisitItems(Player player) {
        if (plugin.getClusterService() != null && plugin.getClusterService().isLoadingHouse(player)) return;
        clearVisitItems(player);
        House house = getHouseByWorld(player.getWorld().getName());
        if (house != null && house.isPvpEnabled() && house.hasPvpLayout()) {
            applyPvpLayout(player, house);
            return;
        }
        if (house == null) {
            clearVisitItems(player);
            if (isLobbyWorld(player.getWorld().getName())) giveLobbyItems(player);
            else clearLobbyItems(player);
            return;
        }
        clearLobbyItems(player);
        if (house.canManage(player.getUniqueId())) { giveOwnerMenuItem(player); return; }
        if (isStaff(player)) {
            giveAdminCookieItem(player, house);
            giveStaffMenuItem(player);
            giveGuestMenuItem(player);
        } else {
            giveCookieItem(player, house);
            giveGuestMenuItem(player);
        }
    }

    public void applyPvpLayout(Player player, House house) {
        if (player == null || house == null || !house.isPvpEnabled()) return;
        if (!house.hasPvpLayout()) {
            updateVisitItemsWithoutPvp(player, house);
            return;
        }
        ItemStack[] layout = house.getPvpLayout();
        ItemStack[] contents = new ItemStack[player.getInventory().getSize()];
        for (int i = 0; i < layout.length && i < contents.length; i++) {
            contents[i] = layout[i] == null ? null : layout[i].clone();
        }
        player.getInventory().setContents(contents);
        giveHouseControlItems(player, house);
    }

    public void applyPvpLayoutToHouse(House house) {
        if (house == null || !house.isPvpEnabled()) return;
        World world = Bukkit.getWorld(house.getWorldName());
        if (world == null) return;
        for (Player player : world.getPlayers()) applyPvpLayout(player, house);
    }

    private void updateVisitItemsWithoutPvp(Player player, House house) {
        if (player == null) return;
        if (house == null) {
            updateVisitItems(player);
            return;
        }
        clearLobbyItems(player);
        giveHouseControlItems(player, house);
    }

    private void giveHouseControlItems(Player player, House house) {
        if (player == null || house == null) return;
        clearVisitItems(player);
        if (house.canManage(player.getUniqueId())) {
            giveOwnerMenuItem(player);
            return;
        }
        if (isStaff(player)) {
            giveAdminCookieItem(player, house);
            giveStaffMenuItem(player);
            giveGuestMenuItem(player);
        } else {
            giveCookieItem(player, house);
            giveGuestMenuItem(player);
        }
    }

    public void clearVisitItems(Player player) {
        clearCookieItem(player);
        clearAdminCookieItem(player);
        clearStaffMenuItem(player);
        clearOwnerMenuItem(player);
        clearGuestMenuItem(player);
    }

    public void ensureHouseMenuItem(Player player) {
        House house = player == null ? null : getHouseByWorld(player.getWorld().getName());
        if (house == null) return;
        if (house.canManage(player.getUniqueId())) {
            giveOwnerMenuItem(player);
        } else {
            giveGuestMenuItem(player);
        }
    }

    public void giveLobbyItems(Player player) {
        if (plugin.getCurrentServer() != null && plugin.getCurrentServer().isInstance()) return;
        if (!plugin.getStaticConfig().getBoolean("lobby-items.enabled", true) || !isLobbyWorld(player.getWorld().getName())) return;
        player.getInventory().setItem(plugin.getStaticConfig().getInt("lobby-items.game-menu-slot", 0), item(net.fakepixel.housing.util.Compat.mat("COMPASS"), plugin.getStaticConfig().getString("lobby-items.game-menu-name", "&aGame Menu &7(Right Click)")));
        player.getInventory().setItem(plugin.getStaticConfig().getInt("lobby-items.profile-slot", 1), skull(plugin.getStaticConfig().getString("lobby-items.profile-name", "&bMy Profile &7(Right Click)")));
        player.getInventory().setItem(plugin.getStaticConfig().getInt("lobby-items.cosmetics-slot", 4), item(net.fakepixel.housing.util.Compat.mat("CHEST"), plugin.getStaticConfig().getString("lobby-items.cosmetics-name", "&dCosmetics &7(Right Click)")));
        player.getInventory().setItem(plugin.getStaticConfig().getInt("lobby-items.players-visible-slot", 7), dye(plugin.getStaticConfig().getString("lobby-items.players-visible-name", "&aPlayers: Visible &7(Right Click)")));
        player.getInventory().setItem(plugin.getStaticConfig().getInt("lobby-items.lobby-selector-slot", 8), item(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"), plugin.getStaticConfig().getString("lobby-items.lobby-selector-name", "&bLobby Selector &7(Right Click)")));
    }

    public void clearLobbyItems(Player player) {
        clearLobbyItem(player, plugin.getStaticConfig().getInt("lobby-items.game-menu-slot", 0));
        clearLobbyItem(player, plugin.getStaticConfig().getInt("lobby-items.profile-slot", 1));
        clearLobbyItem(player, plugin.getStaticConfig().getInt("lobby-items.cosmetics-slot", 4));
        clearLobbyItem(player, plugin.getStaticConfig().getInt("lobby-items.players-visible-slot", 7));
        clearLobbyItem(player, plugin.getStaticConfig().getInt("lobby-items.lobby-selector-slot", 8));
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        try {
            player.getInventory().setItemInHand(null);
        } catch (Throwable ignored) {
        }
    }

    public boolean isLobbyWorld(String worldName) {
        return worldName.equalsIgnoreCase(plugin.getStaticConfig().getString("settings.lobby-world", "housinglobby"));
    }

    public void prepareHouseWorld(World world) {
        if (world == null || getHouseByWorld(world.getName()) == null) return;
        net.fakepixel.housing.util.Compat.setGameRule(world, "doMobSpawning", "false");
        net.fakepixel.housing.util.Compat.setGameRule(world, "doMobLoot", "false");
        for (org.bukkit.entity.Entity entity : new java.util.ArrayList<>(world.getEntities())) {
            if (entity instanceof org.bukkit.entity.LivingEntity
                    && !(entity instanceof Player)
                    && !(entity instanceof org.bukkit.entity.ArmorStand)) {
                entity.remove();
            }
        }
    }

    public boolean isLockedLobbyItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        String name = item.getItemMeta().getDisplayName();
        return name.contains("Game Menu") || name.contains("My Profile") || name.contains("Cosmetics") || name.contains("Players: Visible") || name.contains("Lobby Selector");
    }

    public boolean isLobbyItemSlot(int slot) {
        return slot == plugin.getStaticConfig().getInt("lobby-items.game-menu-slot", 0)
                || slot == plugin.getStaticConfig().getInt("lobby-items.profile-slot", 1)
                || slot == plugin.getStaticConfig().getInt("lobby-items.cosmetics-slot", 4)
                || slot == plugin.getStaticConfig().getInt("lobby-items.players-visible-slot", 7)
                || slot == plugin.getStaticConfig().getInt("lobby-items.lobby-selector-slot", 8);
    }

    private void clearLobbyItem(Player player, int slot) {
        org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
        if (isLockedLobbyItem(item)) player.getInventory().setItem(slot, null);
    }

    private org.bukkit.inventory.ItemStack item(org.bukkit.Material material, String name) {
        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        stack.setItemMeta(meta);
        return stack;
    }

    private org.bukkit.inventory.ItemStack skull(String name) {
        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(net.fakepixel.housing.util.Compat.mat("PLAYER_HEAD"));
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        stack.setItemMeta(meta);
        return stack;
    }

    private org.bukkit.inventory.ItemStack dye(String name) {
        org.bukkit.inventory.ItemStack stack = net.fakepixel.housing.util.Compat.limeDye();
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isStaff(Player player) {
        return player.hasPermission("housing.staff") || player.hasPermission("housing.admin") || player.hasPermission("housing.staff.admin");
    }

    public boolean isInsideBuildBorder(Location location) {
        if (!plugin.getStaticConfig().getBoolean("border.enabled", true)) return true;
        return isInsidePlotBounds(location);
    }

    public boolean isInsidePlotBounds(Location location) {
        if (location == null || location.getWorld() == null) return true;
        House house = getHouseByWorld(location.getWorld().getName());
        if (house == null) return true;
        BorderBounds bounds = borderBounds(house);
        return location.getBlockX() > bounds.minX && location.getBlockX() < bounds.maxX && location.getBlockZ() > bounds.minZ && location.getBlockZ() < bounds.maxZ;
    }

    @SuppressWarnings("deprecation")
    public void ensureBorder(World world) {
        ensureBorder(world, true);
    }

    @SuppressWarnings("deprecation")
    private void ensureBorder(World world, boolean loadChunks) {
        if (!plugin.getStaticConfig().getBoolean("border.enabled", true) || world == null) return;
        House house = getHouseByWorld(world.getName());
        BorderBounds bounds = borderBounds(house);
        int y = borderY(house);
        BorderMaterial material = borderMaterial(plugin.getStaticConfig().getString("border.material", "RED_WOOL"));
        clearSavedBorder(world, house, material, loadChunks);
        clearOldBorders(world, house, material, bounds, y, loadChunks);
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            setBorderBlock(world, x, y, bounds.minZ, material, loadChunks);
            setBorderBlock(world, x, y, bounds.maxZ, material, loadChunks);
        }
        for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
            setBorderBlock(world, bounds.minX, y, z, material, loadChunks);
            setBorderBlock(world, bounds.maxX, y, z, material, loadChunks);
        }
        if (house != null) house.setPlotBorder(bounds.minX, bounds.maxX, bounds.minZ, bounds.maxZ, y);
    }

    public void clearPlotBorder(World world) {
        if (world == null) return;
        House house = getHouseByWorld(world.getName());
        BorderMaterial material = borderMaterial(plugin.getStaticConfig().getString("border.material", "RED_WOOL"));
        clearSavedBorder(world, house, material, true);
        for (BorderSnapshot old : knownBorderSnapshots(house)) {
            clearBorderRing(world, old.y, material, old.bounds, true);
        }
    }

    public void setPlotSize(House house, int plotSize) {
        if (house == null) return;
        World world = Bukkit.getWorld(house.getWorldName());
        if (world != null) {
            BorderMaterial material = borderMaterial(plugin.getStaticConfig().getString("border.material", "RED_WOOL"));
            clearSavedBorder(world, house, material, true);
            for (BorderSnapshot old : knownBorderSnapshots(house)) {
                clearBorderRing(world, old.y, material, old.bounds, true);
            }
        }
        house.setPlotSize(plotSize);
        if (world != null) {
            BorderMaterial material = borderMaterial(plugin.getStaticConfig().getString("border.material", "RED_WOOL"));
            BorderBounds newBounds = borderBounds(house);
            int newY = borderY(house);
            drawBorderRing(world, newY, material, newBounds, true);
            house.setPlotBorder(newBounds.minX, newBounds.maxX, newBounds.minZ, newBounds.maxZ, newY);
            for (Player player : world.getPlayers()) refreshBorderVisibility(player, house);
        }
        saveAll();
    }

    private void drawBorderRing(World world, int y, BorderMaterial material, BorderBounds bounds, boolean loadChunks) {
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            setBorderBlock(world, x, y, bounds.minZ, material, loadChunks);
            setBorderBlock(world, x, y, bounds.maxZ, material, loadChunks);
        }
        for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
            setBorderBlock(world, bounds.minX, y, z, material, loadChunks);
            setBorderBlock(world, bounds.maxX, y, z, material, loadChunks);
        }
    }

    @SuppressWarnings("deprecation")
    public void refreshBorderVisibility(Player player, House house) {
        if (player == null || house == null || player.getWorld() == null
                || !player.getWorld().getName().equals(house.getWorldName())) return;
        BorderBounds bounds = borderBounds(house);
        int y = borderY(house);
        boolean visible = house.isBorderVisible(player.getUniqueId());
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            sendBorderBlock(player, x, y, bounds.minZ, visible);
            sendBorderBlock(player, x, y, bounds.maxZ, visible);
        }
        for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
            sendBorderBlock(player, bounds.minX, y, z, visible);
            sendBorderBlock(player, bounds.maxX, y, z, visible);
        }
    }

    @SuppressWarnings("deprecation")
    private void sendBorderBlock(Player player, int x, int y, int z, boolean visible) {
        org.bukkit.block.Block block = player.getWorld().getBlockAt(x, y, z);
        player.sendBlockChange(block.getLocation(), visible ? block.getType() : net.fakepixel.housing.util.Compat.mat("AIR"),
                visible ? block.getData() : (byte) 0);
    }

    public BorderBounds borderBounds(House house) {
        int size = house == null ? 31 : house.getPlotSize();
        BorderBounds configured = configuredPlotBounds(size, house);
        if (configured != null) return configured;
        int halfBuild = (Math.max(31, size) - 1) / 2;
        int centerX = house == null ? 0 : house.getOrigin().getBlockX();
        int centerZ = house == null ? 0 : house.getOrigin().getBlockZ();
        return new BorderBounds(centerX - halfBuild - 1, centerX + halfBuild + 1, centerZ - halfBuild - 1, centerZ + halfBuild + 1);
    }

    public int borderY(House house) {
        int size = house == null ? 31 : house.getPlotSize();
        String path = "plot-sizes." + size + ".border-y";
        if (plugin.getStaticConfig().isSet(path)) return plugin.getStaticConfig().getInt(path, plugin.getStaticConfig().getInt("border.y", 103));
        return plugin.getStaticConfig().getInt("border.y", 103);
    }

    private BorderBounds configuredPlotBounds(int size, House house) {
        String path = "plot-sizes." + size + ".";
        if (!plugin.getStaticConfig().isConfigurationSection("plot-sizes." + size)) return null;
        int minX = plugin.getStaticConfig().getInt(path + "min-x");
        int maxX = plugin.getStaticConfig().getInt(path + "max-x");
        int minZ = plugin.getStaticConfig().getInt(path + "min-z");
        int maxZ = plugin.getStaticConfig().getInt(path + "max-z");
        int originX = house == null ? 0 : house.getOrigin().getBlockX();
        int originZ = house == null ? 0 : house.getOrigin().getBlockZ();
        return new BorderBounds(originX + Math.min(minX, maxX), originX + Math.max(minX, maxX), originZ + Math.min(minZ, maxZ), originZ + Math.max(minZ, maxZ));
    }

    private int configuredCenterX() {
        int min = Math.min(plugin.getStaticConfig().getInt("border.min-x", -21), plugin.getStaticConfig().getInt("border.max-x", 17));
        int max = Math.max(plugin.getStaticConfig().getInt("border.min-x", -21), plugin.getStaticConfig().getInt("border.max-x", 17));
        return (int) Math.floor((min + max) / 2.0D);
    }

    private int configuredCenterZ() {
        int min = Math.min(plugin.getStaticConfig().getInt("border.min-z", -36), plugin.getStaticConfig().getInt("border.max-z", 7));
        int max = Math.max(plugin.getStaticConfig().getInt("border.min-z", -36), plugin.getStaticConfig().getInt("border.max-z", 7));
        return (int) Math.floor((min + max) / 2.0D);
    }

    private BorderBounds configuredBorderBounds() {
        int minX = Math.min(plugin.getStaticConfig().getInt("border.min-x", -21), plugin.getStaticConfig().getInt("border.max-x", 17));
        int maxX = Math.max(plugin.getStaticConfig().getInt("border.min-x", -21), plugin.getStaticConfig().getInt("border.max-x", 17));
        int minZ = Math.min(plugin.getStaticConfig().getInt("border.min-z", -36), plugin.getStaticConfig().getInt("border.max-z", 7));
        int maxZ = Math.max(plugin.getStaticConfig().getInt("border.min-z", -36), plugin.getStaticConfig().getInt("border.max-z", 7));
        return new BorderBounds(minX, maxX, minZ, maxZ);
    }

    private void clearSavedBorder(World world, House house, BorderMaterial material, boolean loadChunks) {
        if (house == null || !house.hasPlotBorder()) return;
        clearBorderRing(world, house.getPlotBorderY(), material,
                new BorderBounds(house.getPlotBorderMinX(), house.getPlotBorderMaxX(), house.getPlotBorderMinZ(), house.getPlotBorderMaxZ()),
                loadChunks);
    }

    private void clearOldBorders(World world, House house, BorderMaterial material, BorderBounds current, int currentY, boolean loadChunks) {
        for (BorderSnapshot old : knownBorderSnapshots(house)) {
            if (old.y == currentY && old.bounds.equals(current)) continue;
            clearBorderRing(world, old.y, material, old.bounds, loadChunks);
        }
    }

    public java.util.List<BorderBounds> knownBorderBounds(House house) {
        java.util.LinkedHashSet<BorderBounds> bounds = new java.util.LinkedHashSet<>();
        for (BorderSnapshot snapshot : knownBorderSnapshots(house)) bounds.add(snapshot.bounds);
        return new java.util.ArrayList<>(bounds);
    }

    private java.util.List<BorderSnapshot> knownBorderSnapshots(House house) {
        java.util.List<BorderSnapshot> snapshots = new java.util.ArrayList<>();
        snapshots.add(new BorderSnapshot(configuredBorderBounds(), plugin.getStaticConfig().getInt("border.y", 103)));
        java.util.LinkedHashSet<Integer> sizes = new java.util.LinkedHashSet<>();
        int[] defaults = new int[]{31, 41, 51, 101, 151, 255};
        for (int size : defaults) sizes.add(size);
        org.bukkit.configuration.ConfigurationSection section = plugin.getStaticConfig().getConfigurationSection("plot-sizes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    sizes.add(Integer.parseInt(key));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        for (int size : sizes) snapshots.add(new BorderSnapshot(borderBoundsForSize(size, house), borderYForSize(size)));
        return snapshots;
    }

    private BorderBounds borderBoundsForSize(int size, House house) {
        BorderBounds configured = configuredPlotBounds(size, house);
        if (configured != null) return configured;
        int halfBuild = (Math.max(31, size) - 1) / 2;
        int centerX = house == null ? 0 : house.getOrigin().getBlockX();
        int centerZ = house == null ? 0 : house.getOrigin().getBlockZ();
        return new BorderBounds(centerX - halfBuild - 1, centerX + halfBuild + 1, centerZ - halfBuild - 1, centerZ + halfBuild + 1);
    }

    private int borderYForSize(int size) {
        String path = "plot-sizes." + size + ".border-y";
        if (plugin.getStaticConfig().isSet(path)) return plugin.getStaticConfig().getInt(path, plugin.getStaticConfig().getInt("border.y", 103));
        return plugin.getStaticConfig().getInt("border.y", 103);
    }

    @SuppressWarnings("deprecation")
    private void clearBorderRing(World world, int y, BorderMaterial material, BorderBounds bounds, boolean loadChunks) {
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            clearBorderBlock(world, x, y, bounds.minZ, material, loadChunks);
            clearBorderBlock(world, x, y, bounds.maxZ, material, loadChunks);
        }
        for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
            clearBorderBlock(world, bounds.minX, y, z, material, loadChunks);
            clearBorderBlock(world, bounds.maxX, y, z, material, loadChunks);
        }
    }

    @SuppressWarnings("deprecation")
    private void clearBorderBlock(World world, int x, int y, int z, BorderMaterial material, boolean loadChunks) {
        if (!loadChunks && !world.isChunkLoaded(x >> 4, z >> 4)) return;
        org.bukkit.block.Block block = world.getBlockAt(x, y, z);
        if (block.getTypeId() == material.material.getId() && block.getData() == material.data) {
            block.setType(net.fakepixel.housing.util.Compat.mat("AIR"));
        }
    }

    @SuppressWarnings("deprecation")
    private void setBorderBlock(World world, int x, int y, int z, BorderMaterial material, boolean loadChunks) {
        if (!loadChunks && !world.isChunkLoaded(x >> 4, z >> 4)) return;
        org.bukkit.block.Block block = world.getBlockAt(x, y, z);
        block.setTypeIdAndData(material.material.getId(), material.data, false);
    }

    private BorderMaterial borderMaterial(String configured) {
        if (configured == null || configured.trim().isEmpty()) configured = "RED_WOOL";
        String[] split = configured.trim().toUpperCase(Locale.ENGLISH).split(":", 2);
        byte data = split.length > 1 ? parseData(split[1]) : colorData(split[0]);
        org.bukkit.Material material = org.bukkit.Material.matchMaterial(split[0]);
        if (material == null) material = net.fakepixel.housing.util.Compat.mat(split[0]);
        return new BorderMaterial(material, data);
    }

    private byte parseData(String raw) {
        try {
            return Byte.parseByte(raw);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private byte colorData(String materialName) {
        if ("ORANGE_WOOL".equals(materialName)) return 1;
        if ("MAGENTA_WOOL".equals(materialName)) return 2;
        if ("LIGHT_BLUE_WOOL".equals(materialName)) return 3;
        if ("YELLOW_WOOL".equals(materialName)) return 4;
        if ("LIME_WOOL".equals(materialName)) return 5;
        if ("PINK_WOOL".equals(materialName)) return 6;
        if ("GRAY_WOOL".equals(materialName)) return 7;
        if ("LIGHT_GRAY_WOOL".equals(materialName)) return 8;
        if ("CYAN_WOOL".equals(materialName)) return 9;
        if ("PURPLE_WOOL".equals(materialName)) return 10;
        if ("BLUE_WOOL".equals(materialName)) return 11;
        if ("BROWN_WOOL".equals(materialName)) return 12;
        if ("GREEN_WOOL".equals(materialName)) return 13;
        if ("RED_WOOL".equals(materialName)) return 14;
        if ("BLACK_WOOL".equals(materialName)) return 15;
        return 0;
    }

    private static class BorderMaterial {
        private final org.bukkit.Material material;
        private final byte data;

        private BorderMaterial(org.bukkit.Material material, byte data) {
            this.material = material;
            this.data = data;
        }
    }

    private static class BorderSnapshot {
        private final BorderBounds bounds;
        private final int y;

        private BorderSnapshot(BorderBounds bounds, int y) {
            this.bounds = bounds;
            this.y = y;
        }
    }

    public static class BorderBounds {
        public final int minX;
        public final int maxX;
        public final int minZ;
        public final int maxZ;

        public BorderBounds(int minX, int maxX, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof BorderBounds)) return false;
            BorderBounds other = (BorderBounds) object;
            return minX == other.minX && maxX == other.maxX && minZ == other.minZ && maxZ == other.maxZ;
        }

        @Override
        public int hashCode() {
            return Objects.hash(minX, maxX, minZ, maxZ);
        }
    }

    public void giveAdminCookies(Player staff, House house) {
        if (house == null) {
            staff.sendMessage(plugin.color("&cYou are not inside a player house."));
            return;
        }
        int amount = plugin.getStaticConfig().getInt("staff.admin-cookie-amount", 50);
        house.addCookies(amount);
        saveAll();
        String message = plugin.color("&d&lHOUSING AWARD! &e" + staff.getName() + " &7gave &a" + house.getName() + " &e" + amount + " &7admin cookies!");
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(house.getWorldName());
        if (world != null) {
            for (Player player : world.getPlayers()) player.sendMessage(message);
        }
        staff.sendMessage(plugin.color("&aGave &e" + amount + " &aadmin cookies to &e" + house.getName() + "&a."));
    }

    public void resetHouse(Player staff, House house) {
        if (house == null) {
            staff.sendMessage(plugin.color("&cYou are not inside a player house."));
            return;
        }
        String houseId=house.getOwner()+"#"+house.getPlotId();if(!maintenanceHouses.add(houseId)){staff.sendMessage(plugin.color("&eThat house is already being reset."));return;}
        evacuateForMaintenance(house,"&cThis house is being reset. Returning you to Housing.");
        Bukkit.getScheduler().runTaskLater(plugin,()->performReset(staff,house,0),10L);
    }

    private void performReset(Player staff,House house,int attempt){World world=Bukkit.getWorld(house.getWorldName());if(world!=null&&!world.getPlayers().isEmpty()&&attempt<5){evacuateForMaintenance(house,"&cThis house is being reset. Returning you to Housing.");Bukkit.getScheduler().runTaskLater(plugin,()->performReset(staff,house,attempt+1),10L);return;}try{if(world!=null){for(Player player:new ArrayList<>(world.getPlayers()))player.kickPlayer(plugin.color("&cThis house is being reset."));world.save();if(!Bukkit.unloadWorld(world,true))throw new IllegalStateException("Could not unload occupied house world");}if(plugin.getClusterService()!=null)plugin.getClusterService().releaseHouseLease(house);if(plugin.getClusterService()!=null&&!plugin.getClusterService().acquireHouseLease(house))throw new IllegalStateException("Could not reacquire house lock");world=worldService.loadHouseWorld(house);prepareHouseWorld(world);resetInsideBorderFromTemplate(house,world);ensureBorder(world);house.setLastOpened(System.currentTimeMillis());repository.saveHouse(house);world.save();if(!Bukkit.unloadWorld(world,true))throw new IllegalStateException("Could not unload reset house world");if(plugin.getClusterService()!=null)plugin.getClusterService().releaseHouseLease(house);if(staff.isOnline())staff.sendMessage(plugin.color("&aReset &e"+house.getName()+"&a."));}catch(Exception ex){house.setPublicHouse(false);saveAll();if(staff.isOnline())staff.sendMessage(plugin.color("&cCould not reset house: "+ex.getMessage()));plugin.getLogger().warning("Could not reset house "+house.getWorldName()+": "+ex.getMessage());}finally{maintenanceHouses.remove(house.getOwner()+"#"+house.getPlotId());}}

    private void evacuateForMaintenance(House house,String message){World world=Bukkit.getWorld(house.getWorldName());if(world==null)return;for(Player player:new ArrayList<>(world.getPlayers())){if(plugin.getClusterService()!=null)plugin.getClusterService().sendPlayerToLobby(player,"House maintenance "+house.getOwner()+"#"+house.getPlotId(),message);}}

    public void deleteHouse(Player staff, House house) {
        if (house == null) {
            staff.sendMessage(plugin.color("&cThat house could not be found."));
            return;
        }
        String houseId=house.getOwner()+"#"+house.getPlotId();if(!maintenanceHouses.add(houseId)){staff.sendMessage(plugin.color("&eThat house is already being deleted."));return;}evacuateForMaintenance(house,"&cThis house is being reset. Returning you to Housing.");Bukkit.getScheduler().runTaskLater(plugin,()->deleteHouseNow(staff,house,0),10L);
    }

    private void deleteHouseNow(Player staff,House house,int attempt) {
        String worldName = house.getWorldName();
        String houseKey = key(house.getOwner(), house.getPlotId());
        String repositoryId = house.getOwner() + "#" + house.getPlotId();

        World world = Bukkit.getWorld(worldName);
        if(world!=null&&!world.getPlayers().isEmpty()&&attempt<5){evacuateForMaintenance(house,"&cThis house is being reset. Returning you to Housing.");Bukkit.getScheduler().runTaskLater(plugin,()->deleteHouseNow(staff,house,attempt+1),10L);return;}
        if(world!=null)for(Player player:new ArrayList<>(world.getPlayers()))player.kickPlayer(plugin.color("&cThis house is being deleted."));
        if (plugin.getHouseNpcService() != null) plugin.getHouseNpcService().despawnHouseEntities(house);
        if (plugin.getHologramService() != null) {
            for (String hologramId : new ArrayList<>(house.getHolograms().keySet())) {
                plugin.getHologramService().deleteHologram(house, hologramId);
            }
        }
        if (world != null && !Bukkit.unloadWorld(world, false)) {
            plugin.getLogger().warning("Could not unload house world " + worldName + " before deleting it.");
        }
        if(plugin.getClusterService()!=null)plugin.getClusterService().releaseHouseLease(house);

        houses.remove(houseKey);
        invites.values().removeIf(owner -> owner.equals(house.getOwner()));
        try {
            if (repository != null) repository.deleteHouse(repositoryId);
        } catch (Exception ex) {
            plugin.getLogger().severe("Could not delete saved house data " + repositoryId + ": " + ex.getMessage());
        }
        try {
            deleteSlimeWorld(worldName);
            deleteHouseFiles(worldName);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Could not fully remove SlimeWorld " + worldName + ": " + throwable.getMessage());
        }
        staff.sendMessage(plugin.color(staff.getUniqueId().equals(house.getOwner())
                ? "&aYour house has been deleted."
                : "&aDeleted &e" + house.getOwnerName() + "&a's housing."));
        maintenanceHouses.remove(repositoryId);
    }

    private void deleteHouseFiles(String worldName) {
        deleteDirectory(new File(Bukkit.getWorldContainer(), worldName));

        File storage = new File(plugin.getStaticConfig().getString("slimeworldmanager.storage-folder", "slime_worlds"));
        deleteDirectory(new File(storage, worldName));
        deleteDirectory(new File(storage, worldName + ".slime"));
        deleteDirectory(new File(storage, worldName + ".slime.gz"));
        deleteDirectory(new File(storage, worldName + ".slime.tmp"));
        deleteDirectory(new File(storage, worldName + ".swm"));
        deleteDirectory(new File(storage, worldName + ".dat"));
    }

    private void deleteSlimeWorld(String worldName) {
        deleteAdvancedSlimePaperWorld(worldName);
        deleteLegacySlimeWorld(worldName);
    }

    private void deleteAdvancedSlimePaperWorld(String worldName) {
        try {
            Class<?> apiClass = Class.forName("com.infernalsuite.asp.api.AdvancedSlimePaperAPI");
            Object api = apiClass.getMethod("instance").invoke(null);
            Object loader = createAdvancedFileLoader();
            if (loader == null) return;

            // Try the common ASP delete/unload method shapes. Different builds expose
            // different names, so this stays reflection based like the world loader.
            invokeAny(api, "unloadWorld", worldName, false);
            invokeAny(api, "unloadWorld", worldName);
            invokeAny(api, "deleteWorld", loader, worldName);
            invokeAny(api, "deleteWorld", worldName, loader);
            invokeAny(api, "deleteWorld", worldName);
            invokeAny(loader, "deleteWorld", worldName);
            invokeAny(loader, "delete", worldName);
            invokeAny(loader, "deleteWorld", worldName, false);
            invokeAny(loader, "deleteWorld", worldName, true);
        } catch (Throwable ignored) {
        }
    }

    private Object createAdvancedFileLoader() {
        String folderPath = plugin.getStaticConfig().getString("slimeworldmanager.storage-folder", "slime_worlds");
        String[] classNames = new String[]{
                "com.infernalsuite.asp.loaders.file.FileLoader",
                "net.fakepixel.housing.libs.asp.loaders.file.FileLoader"
        };
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                try { return clazz.getConstructor(String.class).newInstance(folderPath); } catch (NoSuchMethodException ignored) {}
                try { return clazz.getConstructor(File.class).newInstance(new File(folderPath)); } catch (NoSuchMethodException ignored) {}
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private void deleteLegacySlimeWorld(String worldName) {
        try {
            org.bukkit.plugin.Plugin swm = Bukkit.getPluginManager().getPlugin(plugin.getStaticConfig().getString("slimeworldmanager.plugin", "SlimeWorldManager"));
            if (swm == null) swm = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
            if (swm == null) swm = Bukkit.getPluginManager().getPlugin("SlimeWorldPlugin");
            if (swm == null) return;

            Object loader = null;
            try { loader = swm.getClass().getMethod("getLoader", String.class).invoke(swm, plugin.getStaticConfig().getString("slimeworldmanager.loader", "file")); } catch (Throwable ignored) {}
            if (loader != null) {
                invokeAny(loader, "deleteWorld", worldName);
                invokeAny(loader, "delete", worldName);
            }
            invokeAny(swm, "deleteWorld", worldName);
            invokeAny(swm, "deleteWorld", loader, worldName);
            invokeAny(swm, "deleteWorld", worldName, loader);
        } catch (Throwable ignored) {
        }
    }

    private void invokeAny(Object target, String methodName, Object... args) {
        if (target == null) return;
        for (java.lang.reflect.Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterTypes().length != args.length) continue;
            if (!methodAccepts(method.getParameterTypes(), args)) continue;
            try {
                method.setAccessible(true);
                method.invoke(target, args);
                return;
            } catch (Throwable ignored) {}
        }
    }

    private boolean methodAccepts(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) continue;
            Class<?> type = wrapPrimitive(types[i]);
            if (!type.isAssignableFrom(args[i].getClass())) return false;
        }
        return true;
    }

    private Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private void resetInsideBorderFromTemplate(House house, World targetWorld) throws Exception {
        File template = templateFolder(house);
        File tempWorldFolder = new File(Bukkit.getWorldContainer(), "__housing_reset_" + house.getOwner().toString().replace("-", ""));
        World templateWorld = null;
        try {
            if (tempWorldFolder.exists()) deleteDirectory(tempWorldFolder);
            copyDirectory(template, tempWorldFolder);
            deleteDirectory(new File(tempWorldFolder, "uid.dat"));
            templateWorld = Bukkit.createWorld(new org.bukkit.WorldCreator(tempWorldFolder.getName()));
            if (templateWorld == null) throw new IllegalStateException("Could not load template world " + template.getName());
            copyInsideBorder(templateWorld, targetWorld);
        } finally {
            if (templateWorld != null) Bukkit.unloadWorld(templateWorld, false);
            deleteDirectory(tempWorldFolder);
        }
    }

    private File templateFolder(House house) {
        File template = new File(plugin.getDataFolder(), "templates/" + effectiveTemplateName(house));
        if (!template.exists()) {
            template = new File(plugin.getDataFolder(), "templates/" + plugin.getStaticConfig().getString("settings.default-template", "basic"));
        }
        if (!template.exists()) throw new IllegalStateException("Missing template folder " + template.getPath());
        return template;
    }

    private String effectiveTemplateName(House house) {
        String configuredDefault = plugin.getStaticConfig().getString("settings.default-template", "basic");
        String template = house.getTemplate();
        if (template == null || template.trim().isEmpty()) return configuredDefault;
        if (template.equalsIgnoreCase("air") && !configuredDefault.equalsIgnoreCase("air")) return configuredDefault;
        return template;
    }

    @SuppressWarnings("deprecation")
    private void copyInsideBorder(World source, World target) {
        House house = getHouseByWorld(target.getName());
        BorderBounds bounds = borderBounds(house);
        int minX = bounds.minX + 1;
        int maxX = bounds.maxX - 1;
        int minZ = bounds.minZ + 1;
        int maxZ = bounds.maxZ - 1;
        int minY = Math.max(0, plugin.getStaticConfig().getInt("reset.min-y", 0));
        int maxY = Math.min(256 - 1, plugin.getStaticConfig().getInt("reset.max-y", 256 - 1));

        removeResetEntities(target, minX, maxX, minY, maxY, minZ, maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                source.getChunkAt(x >> 4, z >> 4).load();
                target.getChunkAt(x >> 4, z >> 4).load();
                for (int y = minY; y <= maxY; y++) {
                    org.bukkit.block.Block sourceBlock = source.getBlockAt(x, y, z);
                    org.bukkit.block.Block targetBlock = target.getBlockAt(x, y, z);
                    targetBlock.setTypeIdAndData(sourceBlock.getTypeId(), sourceBlock.getData(), false);
                    copyBlockState(sourceBlock, targetBlock);
                }
            }
        }
    }

    private void removeResetEntities(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (org.bukkit.entity.Entity entity : new java.util.ArrayList<>(world.getEntities())) {
            if (entity instanceof Player) continue;
            Location location = entity.getLocation();
            if (location.getBlockX() >= minX && location.getBlockX() <= maxX
                    && location.getBlockY() >= minY && location.getBlockY() <= maxY
                    && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ) {
                entity.remove();
            }
        }
    }

    private void copyBlockState(org.bukkit.block.Block sourceBlock, org.bukkit.block.Block targetBlock) {
        org.bukkit.block.BlockState sourceState = sourceBlock.getState();
        org.bukkit.block.BlockState targetState = targetBlock.getState();
        if (sourceState instanceof org.bukkit.inventory.InventoryHolder && targetState instanceof org.bukkit.inventory.InventoryHolder) {
            org.bukkit.inventory.Inventory sourceInventory = ((org.bukkit.inventory.InventoryHolder) sourceState).getInventory();
            org.bukkit.inventory.Inventory targetInventory = ((org.bukkit.inventory.InventoryHolder) targetState).getInventory();
            targetInventory.setContents(sourceInventory.getContents());
        }
        if (sourceState instanceof org.bukkit.block.Sign && targetState instanceof org.bukkit.block.Sign) {
            org.bukkit.block.Sign sourceSign = (org.bukkit.block.Sign) sourceState;
            org.bukkit.block.Sign targetSign = (org.bukkit.block.Sign) targetState;
            for (int i = 0; i < 4; i++) targetSign.setLine(i, sourceSign.getLine(i));
            targetSign.update(true, false);
        }
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) throw new IOException("Could not create " + target.getPath());
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) copyDirectory(child, new File(target, child.getName()));
            }
            return;
        }
        java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteDirectory(java.io.File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            if (children != null) for (java.io.File child : children) deleteDirectory(child);
        }
        if (!file.delete()) {
            plugin.getLogger().warning("Could not delete " + file.getPath());
        }
    }


    private void giveGuestMenuItem(Player player) {
        House house = player == null ? null : getHouseByWorld(player.getWorld().getName());
        if (house == null || house.canManage(player.getUniqueId())) return;
        clearGuestMenuItem(player);
        org.bukkit.inventory.ItemStack door = new org.bukkit.inventory.ItemStack(doorMaterial());
        org.bukkit.inventory.meta.ItemMeta meta = door.getItemMeta();
        meta.setDisplayName(plugin.color("&aGuest Menu &7(Right Click)"));
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(plugin.color("&7Right-click to open the guest housing menu."));
        meta.setLore(lore);
        door.setItemMeta(meta);
        player.getInventory().setItem(8, door);
        player.updateInventory();
    }

    private org.bukkit.Material doorMaterial() {
        return org.bukkit.Material.WOOD_DOOR;
    }

    private void clearGuestMenuItem(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.getType().name().contains("DOOR") && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    && (item.getItemMeta().getDisplayName().contains("Housing Menu") || item.getItemMeta().getDisplayName().contains("Guest Menu") || item.getItemMeta().getDisplayName().contains("Players"))) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    private void giveCookieItem(Player player, House house) {
        if (house == null || house.getOwner().equals(player.getUniqueId())) return;
        org.bukkit.inventory.ItemStack cookie = new org.bukkit.inventory.ItemStack(net.fakepixel.housing.util.Compat.mat("COOKIE"));
        org.bukkit.inventory.meta.ItemMeta meta = cookie.getItemMeta();
        meta.setDisplayName(plugin.color("&6Give Cookie"));
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(plugin.color("&7Right click to give this house a cookie."));
        lore.add(plugin.color("&7One cookie per house."));
        meta.setLore(lore);
        cookie.setItemMeta(meta);
        player.getInventory().setItem(plugin.getStaticConfig().getInt("cookies.item-slot", 4), cookie);
    }


    private void giveAdminCookieItem(Player player, House house) {
        org.bukkit.inventory.ItemStack cookie = new org.bukkit.inventory.ItemStack(net.fakepixel.housing.util.Compat.mat("COOKIE"));
        org.bukkit.inventory.meta.ItemMeta meta = cookie.getItemMeta();
        int amount = plugin.getStaticConfig().getInt("staff.admin-cookie-amount", 50);
        meta.setDisplayName(plugin.color("&cADMIN Cookies &7(Right Click)"));
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(plugin.color("&7Right-click to give the owner of"));
        lore.add(plugin.color("&7this house a pack of &cADMIN Cookies&7."));
        lore.add(plugin.color(""));
        lore.add(plugin.color("&7Cookies are used as a rating system,"));
        lore.add(plugin.color("&7other players can sort homes based on them!"));
        lore.add(plugin.color(""));
        lore.add(plugin.color("&7A pack of &cADMIN Cookies &7contains &b" + amount + " &7cookies."));
        meta.setLore(lore);
        cookie.setItemMeta(meta);
        player.getInventory().setItem(plugin.getStaticConfig().getInt("staff.admin-cookie-slot", 4), cookie);
    }

    private void giveStaffMenuItem(Player player) {
        org.bukkit.inventory.ItemStack nugget = new org.bukkit.inventory.ItemStack(net.fakepixel.housing.util.Compat.mat("GOLD_NUGGET"));
        org.bukkit.inventory.meta.ItemMeta meta = nugget.getItemMeta();
        meta.setDisplayName(plugin.color("&bHousing Staff Menu &7(Right Click)"));
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(plugin.color("&7Right-click to open staff tools."));
        meta.setLore(lore);
        nugget.setItemMeta(meta);
        player.getInventory().setItem(plugin.getStaticConfig().getInt("staff.menu-item-slot", 7), nugget);
    }

    private void clearCookieItem(Player player) {
        int slot = plugin.getStaticConfig().getInt("cookies.item-slot", 4);
        org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
        if (item != null && item.getType() == net.fakepixel.housing.util.Compat.mat("COOKIE") && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains("Give Cookie")) {
            player.getInventory().setItem(slot, null);
        }
    }

    private void clearAdminCookieItem(Player player) {
        int slot = plugin.getStaticConfig().getInt("staff.admin-cookie-slot", 4);
        org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
        if (item != null && item.getType() == net.fakepixel.housing.util.Compat.mat("COOKIE") && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains("ADMIN Cookies")) {
            player.getInventory().setItem(slot, null);
        }
    }

    private void clearStaffMenuItem(Player player) {
        int slot = plugin.getStaticConfig().getInt("staff.menu-item-slot", 7);
        org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
        if (item != null && item.getType() == net.fakepixel.housing.util.Compat.mat("GOLD_NUGGET") && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains("Housing Staff Menu")) {
            player.getInventory().setItem(slot, null);
        }
    }

    public boolean invite(Player owner, Player target) {
        House house = getHouse(owner.getUniqueId());
        if (house == null || !house.canManage(owner.getUniqueId())) return false;
        invites.put(target.getUniqueId(), house.getOwner());
        if(repository!=null)try{repository.saveInvite(target.getUniqueId(),house.getOwner(),house.getOwner()+"#"+house.getPlotId(),System.currentTimeMillis()+300000L);}catch(Exception ex){plugin.getLogger().warning("Could not save Housing invite: "+ex.getMessage());}
        target.sendMessage(plugin.color("&6Housing &8> &e" + owner.getName() + " &7invited you to &a" + house.getName() + "&7. Use &e/housing join " + owner.getName() + "&7."));
        owner.sendMessage(plugin.color("&aInvited &e" + target.getName() + " &ato your house."));
        return true;
    }

    public boolean hasInvite(Player player, House house) {
        UUID owner = invites.get(player.getUniqueId());
        return owner != null && house != null && owner.equals(house.getOwner());
    }

    public boolean canEnter(Player player, House house) {
        if (house == null) return false;
        if (isStaff(player)) return true;
        if (house.isBanned(player.getUniqueId())) return false;
        if (house.canManage(player.getUniqueId()) || hasInvite(player, house)) return true;
        String rule = house.getVisitingRule();
        if ("public".equals(rule)) return true;
        return false;
    }

    public boolean isPunishmentProtected(Player player) {
        if (player == null) return false;
        if (player.hasPermission("housing.staff") || player.hasPermission("housing.admin") || player.hasPermission("housing.bypass.housepunish")) return true;
        if (plugin.getRankPermissionService() != null && plugin.getRankPermissionService().isHousingStaff(player)) return true;
        String rank = plugin.getAbelixHook() == null ? "" : plugin.getAbelixHook().getRankName(player);
        String clean = rank == null ? "" : rank.trim().toLowerCase(java.util.Locale.ENGLISH).replace("_", "").replace("-", "").replace(" ", "");
        return clean.contains("admin") || clean.contains("owner") || clean.contains("manager") || clean.contains("developer")
                || clean.contains("mod") || clean.contains("helper") || clean.contains("staff") || clean.contains("youtube") || clean.contains("yt");
    }

    private String key(UUID owner, int plotId) {
        return owner.toString() + "#" + normalizePlotId(plotId);
    }

    private int normalizePlotId(int plotId) {
        return Math.max(1, Math.min(maxPlots(), plotId));
    }

    private String storageKey(House house) {
        return house.getOwner().toString() + (house.getPlotId() <= 1 ? "" : "_plot_" + house.getPlotId());
    }

    public void kick(Player sender, Player target, House house) {
        if (house == null || !house.canManage(sender.getUniqueId())) { sender.sendMessage(plugin.msg("no-permission")); return; }
        if (target.getUniqueId().equals(house.getOwner())) { sender.sendMessage(plugin.color("&cYou cannot kick the owner.")); return; }
        if (isPunishmentProtected(target)) { sender.sendMessage(plugin.color("&cYou cannot punish staff members.")); return; }
        leave(target);
        target.sendMessage(plugin.color("&cYou were kicked from &e" + house.getName() + "&c."));
        sender.sendMessage(plugin.color("&aKicked &e" + target.getName() + "&a."));
    }

    public void kickGuests(Player sender, House house) {
        if (house == null || !house.canManage(sender.getUniqueId())) { sender.sendMessage(plugin.msg("no-permission")); return; }
        World world = Bukkit.getWorld(house.getWorldName());
        if (world == null) return;
        int count = 0;
        for (Player player : new ArrayList<>(world.getPlayers())) {
            if (house.canManage(player.getUniqueId())) continue;
            leave(player);
            count++;
        }
        sender.sendMessage(plugin.color("&aKicked &e" + count + " &aguests."));
    }

    public void kickAll(Player sender, House house) {
        if (house == null || !house.canManage(sender.getUniqueId())) { sender.sendMessage(plugin.msg("no-permission")); return; }
        World world = Bukkit.getWorld(house.getWorldName());
        if (world == null) return;
        int count = 0;
        for (Player player : new ArrayList<>(world.getPlayers())) {
            if (player.equals(sender)) continue;
            leave(player);
            count++;
        }
        sender.sendMessage(plugin.color("&aKicked &e" + count + " &aplayers."));
    }

    public void giveOwnerMenuItem(Player player) {
        clearVisitItems(player);
        org.bukkit.inventory.ItemStack star = new org.bukkit.inventory.ItemStack(net.fakepixel.housing.util.Compat.mat("NETHER_STAR"));
        org.bukkit.inventory.meta.ItemMeta meta = star.getItemMeta();
        meta.setDisplayName(plugin.color("&bHousing Menu &7(Right Click)"));
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(plugin.color("&7Manage your house."));
        meta.setLore(lore);
        star.setItemMeta(meta);
        player.getInventory().setItem(plugin.getStaticConfig().getInt("owner-menu.item-slot", 8), star);
    }

    private void clearOwnerMenuItem(Player player) {
        int slot = plugin.getStaticConfig().getInt("owner-menu.item-slot", 8);
        org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
        if (item != null && item.getType() == net.fakepixel.housing.util.Compat.mat("NETHER_STAR") && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains("Housing Menu")) {
            player.getInventory().setItem(slot, null);
        }
    }

}
