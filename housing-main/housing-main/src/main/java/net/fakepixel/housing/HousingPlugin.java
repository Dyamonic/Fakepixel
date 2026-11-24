package net.fakepixel.housing;

import net.fakepixel.housing.command.HousingCommand;
import net.fakepixel.housing.command.CustomCommandService;
import net.fakepixel.housing.command.NpcCommand;
import net.fakepixel.housing.action.ActionButtonService;
import net.fakepixel.housing.action.ActionPadService;
import net.fakepixel.housing.function.FunctionService;
import net.fakepixel.housing.hook.AbelixHook;
import net.fakepixel.housing.house.HouseManager;
import net.fakepixel.housing.jukebox.JukeboxService;
import net.fakepixel.housing.listener.HousingListener;
import net.fakepixel.housing.hologram.HologramService;
import net.fakepixel.housing.menu.HousingMenu;
import net.fakepixel.housing.menu.StaffMenu;
import net.fakepixel.housing.menu.GroupsMenu;
import net.fakepixel.housing.nametag.NametagService;
import net.fakepixel.housing.npc.HouseNpcService;
import net.fakepixel.housing.npc.HousingNpcService;
import net.fakepixel.housing.parkour.ParkourService;
import net.fakepixel.housing.region.RegionService;
import net.fakepixel.housing.build.ProToolsService;
import net.fakepixel.housing.build.SelectionService;
import net.fakepixel.housing.scoreboard.CookieLeaderboard;
import net.fakepixel.housing.rank.RankPermissionService;
import net.fakepixel.housing.scoreboard.HousingScoreboard;
import net.fakepixel.housing.tablist.TablistService;
import net.fakepixel.housing.world.WorldService;
import net.fakepixel.housing.world.WorldServiceFactory;
import net.fakepixel.housing.world.LobbyWorldService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.fakepixel.housing.config.HousingDefaults;
import net.fakepixel.housing.config.StaticConfigRegistry;
import net.fakepixel.housing.database.*;
import net.fakepixel.housing.cluster.*;
import net.fakepixel.housing.metadata.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class HousingPlugin extends JavaPlugin {

    private AbelixHook abelixHook;
    private HouseManager houseManager;
    private WorldService worldService;
    private HousingScoreboard housingScoreboard;
    private TablistService tablistService;
    private CookieLeaderboard cookieLeaderboard;
    private StaffMenu staffMenu;
    private RankPermissionService rankPermissionService;
    private GroupsMenu groupsMenu;
    private SelectionService selectionService;
    private ProToolsService proToolsService;
    private NametagService nametagService;
    private HousingNpcService housingNpcService;
    private HouseNpcService houseNpcService;
    private HousingMenu housingMenu;
    private HologramService hologramService;
    private ParkourService parkourService;
    private ActionButtonService actionButtonService;
    private ActionPadService actionPadService;
    private RegionService regionService;
    private CustomCommandService customCommandService;
    private FunctionService functionService;
    private JukeboxService jukeboxService;
    private final Set<UUID> borderBypass = new HashSet<>();
    private final Set<UUID> adminBuildMode = new HashSet<>();
    private HousingDatabaseManager databaseManager;
    private HousingRepository housingRepository;
    private HousingServerDefinition currentServer;
    private HousingClusterService clusterService;
    private HouseMetaCache houseMetaCache;

    @Override
    public void onEnable() {
        try { this.currentServer = HousingServerRegistry.current(); }
        catch (IllegalStateException ex) { getLogger().severe(ex.getMessage()); Bukkit.getPluginManager().disablePlugin(this); return; }

        if (currentServer.isLobby()) {
            saveResourceFolder("worlds");
            installAndLoadStaticWorlds();
        } else {
            saveResourceFolder("templates");
        }

        HousingDatabaseSettings databaseSettings = HousingDatabaseSettings.fromEnvironment();
        this.databaseManager = new HousingDatabaseManager(getLogger());
        if (databaseManager.connect(databaseSettings)) {
            try {
                new HousingDatabaseMigrator(databaseManager, getLogger()).migrate();
                this.housingRepository = new JdbcHousingRepository(databaseManager);
            } catch (Exception ex) {
                getLogger().severe("Housing database migration failed: " + ex.getMessage());
                databaseManager.close();
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        } else if (databaseSettings.isEnabled()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (housingRepository == null) { getLogger().severe("Registered multi-server Housing requires MariaDB."); Bukkit.getPluginManager().disablePlugin(this); return; }

        this.abelixHook = new AbelixHook(this);
        this.nametagService = new NametagService(this, abelixHook);
        this.housingNpcService = new HousingNpcService(this);
        this.housingNpcService.load();
        this.rankPermissionService = new RankPermissionService(this);
        this.rankPermissionService.load();
        this.worldService = currentServer.isLobby() ? new LobbyWorldService() : WorldServiceFactory.create(this);
        this.houseManager = new HouseManager(this, worldService, housingRepository);
        this.housingMenu = new HousingMenu(this, houseManager, abelixHook);
        HousingMenu housingMenu = this.housingMenu;
        this.clusterService = new HousingClusterService(this, currentServer, new HousingClusterRepository(databaseManager));
        this.clusterService.start();

        if (currentServer.isInstance()) {
            this.hologramService = new HologramService(this, houseManager);
            this.houseNpcService = new HouseNpcService(this, houseManager);
            this.actionButtonService = new ActionButtonService(this, houseManager);
            this.actionPadService = new ActionPadService(this, houseManager);
            this.regionService = new RegionService(this, houseManager);
            this.customCommandService = new CustomCommandService(this, houseManager);
            this.functionService = new FunctionService(this, houseManager);
            this.jukeboxService = new JukeboxService(this);
            this.staffMenu = new StaffMenu(this, houseManager);
            this.groupsMenu = new GroupsMenu(this, houseManager);
            this.selectionService = new SelectionService(this);
            this.proToolsService = new ProToolsService(this, houseManager);
            this.parkourService = new ParkourService(this, houseManager);
        }

        HousingCommand command = new HousingCommand(this, houseManager, housingMenu, abelixHook);
        Objects.requireNonNull(getCommand("housing")).setExecutor(command);
        Objects.requireNonNull(getCommand("housing")).setTabCompleter(command);
        NpcCommand npcCommand = new NpcCommand(this);
        Objects.requireNonNull(getCommand("npc")).setExecutor(npcCommand);
        Objects.requireNonNull(getCommand("npc")).setTabCompleter(npcCommand);
        if (currentServer.isInstance()) { Objects.requireNonNull(getCommand("parkour")).setExecutor(parkourService); Objects.requireNonNull(getCommand("parkour")).setTabCompleter(parkourService); }
        String[] roleCommands = currentServer.isLobby() ? new String[]{"visit", "housingtag"} : new String[]{"visit", "gmc", "gms", "gma", "tp", "tphere", "wand", "set", "walls", "undo", "copy", "pos1", "pos2", "housingtag", "visitingrule", "groupname", "grouptag", "fly", "time", "adminbuild"};
        for (String name : roleCommands) {
            Objects.requireNonNull(getCommand(name)).setExecutor(command);
            Objects.requireNonNull(getCommand(name)).setTabCompleter(command);
        }
        Objects.requireNonNull(getCommand("borderbypass")).setExecutor((sender, command1, label, args) -> {
            if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
            Player player = (Player) sender;
            if (!player.hasPermission("housing.staff.bypassborder") && !player.hasPermission("housing.admin")) {
                player.sendMessage(msg("no-permission"));
                return true;
            }
            player.sendMessage(color(toggleBorderBypass(player) ? "&aHousing border bypass enabled." : "&cHousing border bypass disabled."));
            return true;
        });

        Bukkit.getPluginManager().registerEvents(housingMenu, this);
        Bukkit.getPluginManager().registerEvents(rankPermissionService, this);
        Bukkit.getPluginManager().registerEvents(housingNpcService, this);
        if (currentServer.isInstance()) {
            Bukkit.getPluginManager().registerEvents(new HousingListener(this, houseManager, abelixHook), this);
            Bukkit.getPluginManager().registerEvents(staffMenu, this); Bukkit.getPluginManager().registerEvents(groupsMenu, this);
            Bukkit.getPluginManager().registerEvents(proToolsService, this); Bukkit.getPluginManager().registerEvents(hologramService, this);
            Bukkit.getPluginManager().registerEvents(houseNpcService, this); Bukkit.getPluginManager().registerEvents(actionButtonService, this);
            Bukkit.getPluginManager().registerEvents(actionPadService, this); Bukkit.getPluginManager().registerEvents(regionService, this);
            Bukkit.getPluginManager().registerEvents(customCommandService, this); Bukkit.getPluginManager().registerEvents(functionService, this);
            Bukkit.getPluginManager().registerEvents(parkourService, this);
        }

        if (currentServer.isLobby()) { this.houseMetaCache=new HouseMetaCache(this,new HouseSummaryRepository(databaseManager));this.houseMetaCache.start(); }
        this.housingScoreboard = new HousingScoreboard(this, houseManager); this.housingScoreboard.start();
        if (currentServer.isLobby()) { this.cookieLeaderboard = new CookieLeaderboard(this, houseManager); this.cookieLeaderboard.start(); }
        if (currentServer.isInstance()) {
            this.tablistService = new TablistService(this); this.tablistService.start(); this.nametagService.start();
        }
        this.housingNpcService.spawnAll();

        int housesLoaded = houseManager.getHouses().size();
        int npcRecords = databaseManager.isConnected() ? databaseManager.count("housing_npcs") : 0;
        int parkourRecords = databaseManager.isConnected() ? databaseManager.count("housing_parkour") : 0;
        getLogger().info("Loaded " + housesLoaded + " houses, " + npcRecords + " NPCs, " + parkourRecords + " parkour records.");

        getLogger().info("HousingCore enabled as " + currentServer.getId() + " (" + currentServer.getRole() + ") using " + worldService.getName() + ".");
    }

    @Override
    public void onDisable() {
        if (clusterService != null) clusterService.stop();
        if (houseMetaCache != null) houseMetaCache.stop();
        if (nametagService != null) nametagService.stop();
        if (cookieLeaderboard != null) cookieLeaderboard.stop();
        if (housingScoreboard != null) housingScoreboard.stop();
        if (tablistService != null) tablistService.stop();
        if (hologramService != null) hologramService.despawnAll();
        if (houseNpcService != null) houseNpcService.despawnAll();
        if (rankPermissionService != null) rankPermissionService.shutdown();
        if (houseManager != null) {
            houseManager.shutdown();
        }
        if (databaseManager != null) databaseManager.close();
    }

    public AbelixHook getAbelixHook() {
        return abelixHook;
    }

    public HousingDefaults getStaticConfig() {
        return StaticConfigRegistry.defaults();
    }
    public HousingRepository getHousingRepository() { return housingRepository; }
    public HousingDatabaseManager getDatabaseManager() { return databaseManager; }
    public HousingServerDefinition getCurrentServer() { return currentServer; }
    public HousingClusterService getClusterService() { return clusterService; }
    public HouseMetaCache getHouseMetaCache() { return houseMetaCache; }

    public HouseManager getHouseManager() {
        return houseManager;
    }

    public WorldService getWorldService() {
        return worldService;
    }

    public StaffMenu getStaffMenu() {
        return staffMenu;
    }

    public RankPermissionService getRankPermissionService() {
        return rankPermissionService;
    }

    public GroupsMenu getGroupsMenu() { return groupsMenu; }
    public SelectionService getSelectionService() { return selectionService; }
    public ProToolsService getProToolsService() { return proToolsService; }
    public NametagService getNametagService() { return nametagService; }
    public HousingScoreboard getHousingScoreboard() { return housingScoreboard; }
    public HousingNpcService getHousingNpcService() { return housingNpcService; }
    public HousingMenu getHousingMenu() { return housingMenu; }
    public HologramService getHologramService() { return hologramService; }
    public HouseNpcService getHouseNpcService() { return houseNpcService; }
    public ActionButtonService getActionButtonService() { return actionButtonService; }
    public ActionPadService getActionPadService() { return actionPadService; }
    public RegionService getRegionService() { return regionService; }
    public CustomCommandService getCustomCommandService() { return customCommandService; }
    public FunctionService getFunctionService() { return functionService; }
    public ParkourService getParkourService() { return parkourService; }
    public JukeboxService getJukeboxService() { return jukeboxService; }
    public TablistService getTablistService() { return tablistService; }

    public boolean isBorderBypass(Player player) {
        return player != null && borderBypass.contains(player.getUniqueId());
    }

    public boolean toggleBorderBypass(Player player) {
        if (borderBypass.remove(player.getUniqueId())) return false;
        borderBypass.add(player.getUniqueId());
        return true;
    }

    public boolean isAdminBuildMode(Player player) {
        return player != null && adminBuildMode.contains(player.getUniqueId()) && player.hasPermission("housing.admin.buildmode");
    }

    public boolean toggleAdminBuildMode(Player player) {
        if (adminBuildMode.remove(player.getUniqueId())) return false;
        adminBuildMode.add(player.getUniqueId());
        return true;
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public String defaultHouseMode() {
        return net.fakepixel.housing.house.House.normalizeMode(getStaticConfig().getString("mode.default", "SOCIAL"));
    }

    public String houseModeName(net.fakepixel.housing.house.House house) {
        boolean build = house != null && house.isBuildMode();
        return color(getStaticConfig().getString(build ? "mode.build-mode-name" : "mode.social-mode-name", build ? "&aBuild Mode" : "&bSocial Mode"));
    }

    public void sendHouseModeChanged(Player player, net.fakepixel.housing.house.House house) {
        if (player == null || house == null) return;
        if (house.isBuildMode()) {
            player.sendMessage(color("&aChanged mode to &eBuild Mode"));
            player.sendMessage(color("&7This means the world can be modified."));
        } else {
            player.sendMessage(color("&aChanged mode to &eSocial Mode"));
            player.sendMessage(color("&7This means the world cannot be modified."));
        }
    }

    public boolean canBypassHouseMode(Player player) {
        return player != null && (isAdminBuildMode(player) || player.hasPermission("housing.admin") || player.hasPermission("housing.staff.build") || player.hasPermission("housing.bypass.build"));
    }

    public boolean canBuildInHouseMode(Player player, net.fakepixel.housing.house.House house) {
        if (player == null || house == null) return false;
        if (house.getOwner().equals(player.getUniqueId())) return true;
        if (house.hasGroupPermission(player.getUniqueId(), "build")) return true;
        if (canBypassHouseMode(player)) return true;
        return false;
    }

    public boolean canUseProToolsInHouseMode(Player player, net.fakepixel.housing.house.House house) {
        if (player == null || house == null) return false;
        if (house.getOwner().equals(player.getUniqueId())) return true;
        if (house.hasGroupPermission(player.getUniqueId(), "build") || house.hasGroupPermission(player.getUniqueId(), "pro-tools")) return true;
        if (canBypassHouseMode(player)) return true;
        return false;
    }

    public boolean canEditHousingObjects(Player player, net.fakepixel.housing.house.House house) {
        if (player == null || house == null) return false;
        if (house.getOwner().equals(player.getUniqueId())) return true;
        if (house.hasGroupPermission(player.getUniqueId(), "build")) return true;
        if (canBypassHouseMode(player)) return true;
        return false;
    }


    public String applyPlaceholders(org.bukkit.entity.Player player, String text) {
        if (text == null) return "";
        String result = text;
        String abelixPrefix = abelixHook == null ? "" : abelixHook.getPrefix(player);
        String housingPrefix = "";
        String houseName = "";
        String houseOwner = "";
        String visitingRules = "";
        String guests = "0";
        String cookies = "0";
        try {
            if (houseManager != null && player != null) {
                net.fakepixel.housing.house.House house = houseManager.getHouseByWorld(player.getWorld().getName());
                if (house != null) {
                    housingPrefix = house.groupSuffix(house.getGroup(player.getUniqueId()));
                    houseName = house.getName();
                    houseOwner = house.getOwnerName();
                    visitingRules = house.getVisitingRuleDisplay();
                    guests = String.valueOf(houseManager.getGuests(house));
                    cookies = String.valueOf(house.getCookies());
                }
            }
        } catch (Exception ignored) { }
        result = result.replace("%abelix_prefix%", abelixPrefix);
        result = result.replace("%housing_prefix%", housingPrefix);
        result = result.replace("%group_suffix%", housingPrefix);
        result = result.replace("%house%", houseName);
        result = result.replace("%house_name%", houseName);
        result = result.replace("%house_owner%", houseOwner);
        result = result.replace("%owner%", houseOwner);
        result = result.replace("%visiting_rules%", visitingRules);
        result = result.replace("%guests%", guests);
        result = result.replace("%cookies%", cookies);
        if (player != null) {
            result = result.replace("%player%", player.getName());
            result = result.replace("%display_name%", player.getDisplayName());
            result = result.replace("%rank%", abelixHook == null ? "" : abelixHook.getRankName(player));
            result = result.replace("%gold%", String.valueOf(abelixHook == null ? 0 : abelixHook.getGold(player)));
        }
        result = result.replace("%server%", getStaticConfig().getString("server-name", getStaticConfig().getString("settings.server-name", "Housing")));
        result = result.replace("%store%", getStaticConfig().getString("store", ""));
        return color(result);
    }

    public String msg(String path) {
        return color(getStaticConfig().getString("messages.prefix", "&6Housing &8> &7") + getStaticConfig().getString("messages." + path, path));
    }

    private void installAndLoadStaticWorlds() {
        java.util.List<String> worlds = getStaticConfig().getStringList("settings.static-worlds");
        if (worlds.isEmpty()) return;

        for (String worldName : worlds) {
            if (worldName == null || worldName.trim().isEmpty()) continue;
            try {
                java.io.File bundled = new java.io.File(getDataFolder(), "worlds/" + worldName);
                java.io.File target = new java.io.File(Bukkit.getWorldContainer(), worldName);

                if (!target.exists() && bundled.exists()) {
                    copyDirectory(bundled, target, false);
                    deleteWorldRuntimeFiles(target);
                }

                if (Bukkit.getWorld(worldName) == null && target.exists()) {
                    org.bukkit.World world = Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));
                    if (world != null) {
                        net.fakepixel.housing.util.Compat.setGameRule(world, "doMobSpawning", "false");
                        net.fakepixel.housing.util.Compat.setGameRule(world, "doDaylightCycle", "false");
                        world.setTime(6000L);
                        world.setStorm(false);
                    }
                }
            } catch (Exception ex) {
                getLogger().warning("Could not load static housing world " + worldName + ": " + ex.getMessage());
            }
        }
    }

    private void deleteWorldRuntimeFiles(java.io.File worldFolder) {
        deleteFile(new java.io.File(worldFolder, "session.lock"));
        deleteFile(new java.io.File(worldFolder, "uid.dat"));
        deleteDirectory(new java.io.File(worldFolder, "playerdata"));
    }

    private void deleteDirectory(java.io.File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            if (children != null) {
                for (java.io.File child : children) deleteDirectory(child);
            }
        }
        deleteFile(file);
    }

    private void deleteFile(java.io.File file) {
        if (file.exists() && !file.delete()) {
            getLogger().warning("Could not delete " + file.getPath());
        }
    }

    private boolean ensureDirectory(java.io.File directory) {
        if (directory.exists()) return false;
        if (directory.mkdirs()) return false;
        getLogger().warning("Could not create directory " + directory.getPath());
        return true;
    }

    private void saveResourceFolder(String folder) {
        try {
            java.io.File target = new java.io.File(getDataFolder(), folder);
            if (target.exists()) return;
            if (ensureDirectory(target)) return;

            java.net.URL url = getClassLoader().getResource(folder);
            if (url != null && "file".equals(url.getProtocol())) {
                copyDirectory(new java.io.File(url.toURI()), target, false);
                return;
            }

            java.io.File jar = new java.io.File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jar.isFile()) return;
            java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jar);
            java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(folder + "/") || entry.isDirectory()) continue;
                java.io.File out = new java.io.File(getDataFolder(), name);
                if (out.exists()) continue;
                if (ensureDirectory(out.getParentFile())) continue;
                java.io.InputStream in = jarFile.getInputStream(entry);
                java.nio.file.Files.copy(in, out.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                in.close();
            }
            jarFile.close();
        } catch (Exception ex) {
            getLogger().warning("Could not copy bundled templates: " + ex.getMessage());
        }
    }

    private void copyDirectory(java.io.File source, java.io.File target, boolean replace) throws java.io.IOException {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) throw new java.io.IOException("Could not create " + target.getPath());
            java.io.File[] children = source.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    copyDirectory(child, new java.io.File(target, child.getName()), replace);
                }
            }
            return;
        }
        if (target.exists() && !replace) return;
        java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
