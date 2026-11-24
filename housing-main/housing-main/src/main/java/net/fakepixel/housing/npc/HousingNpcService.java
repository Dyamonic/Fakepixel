package net.fakepixel.housing.npc;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HousingNpcService implements Listener {
    private static final AtomicInteger ENTITY_IDS = new AtomicInteger(200000);

    private final HousingPlugin plugin;
    private final List<HousingNpc> npcs = new ArrayList<>();
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();
    private final Map<UUID, UUID> selectedNpcs = new HashMap<>();
    private final Map<UUID, List<ArmorStand>> nameplates = new HashMap<>();
    private final Map<UUID, Consumer<Player>> clickHandlers = new HashMap<>();
    private int lookTaskId = -1;

    public HousingNpcService(HousingPlugin plugin) {
        this.plugin = plugin;
        startLookTask();
    }

    public void load() {
        clearNameplates();
        npcs.clear();
        clickHandlers.clear();
        loadConfigured();
        loadSaved();
    }

    private void loadSaved() {
        if (plugin.getHousingRepository() == null) return;
        try { for (String data : plugin.getHousingRepository().loadNpcData()) { String[] p=data.split("\\|",-1); if(p.length<13)continue; World world=Bukkit.getWorld(dec(p[2])); if(world==null)continue; Location location=new Location(world,Double.parseDouble(p[3]),Double.parseDouble(p[4]),Double.parseDouble(p[5]),Float.parseFloat(p[6]),Float.parseFloat(p[7])); npcs.add(new HousingNpc(ENTITY_IDS.incrementAndGet(),UUID.fromString(p[0]),npcName(dec(p[1])),location,dec(p[8]),dec(p[9]),dec(p[10]),Boolean.parseBoolean(p[11]),true)); } }
        catch (Exception ex) { plugin.getLogger().warning("Could not load runtime NPCs from MariaDB: " + ex.getMessage()); }
    }

    private void loadConfigured() {
        ConfigurationSection section = plugin.getStaticConfig().getConfigurationSection("housing-npcs.configured");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null || !child.getBoolean("enabled", true)) continue;
            World world = Bukkit.getWorld(child.getString("world", plugin.getStaticConfig().getString("settings.lobby-world", "housinglobby")));
            if (world == null) {
                plugin.getLogger().warning("Skipping configured housing NPC '" + key + "': world is not loaded.");
                continue;
            }

            Location location = configuredLocation(world, child);
            HousingNpc npc = new HousingNpc(
                    child.getInt("entity-id", configuredEntityId(key)),
                    UUID.fromString(child.getString("uuid", UUID.nameUUIDFromBytes(("housing-npc:" + key).getBytes(StandardCharsets.UTF_8)).toString())),
                    npcName(child.getString("name", key)),
                    location,
                    child.getString("skin-value", ""),
                    child.getString("skin-signature", ""),
                    child.getString("command", ""),
                    child.getBoolean("look-at-players", true),
                    false
            );
            npcs.add(npc);
            String skinName = child.getString("skin-name", "");
            if ((npc.skinValue.isEmpty() || npc.skinSignature.isEmpty()) && skinName != null && !skinName.trim().isEmpty()) {
                fetchSkinLater(npc, skinName.trim());
            }
        }
    }

    private Location configuredLocation(World world, ConfigurationSection section) {
        ConfigurationSection location = section.getConfigurationSection("location");
        ConfigurationSection source = location == null ? section : location;
        return new Location(
                world,
                source.getDouble("x"),
                source.getDouble("y"),
                source.getDouble("z"),
                (float) source.getDouble("yaw"),
                (float) source.getDouble("pitch")
        );
    }

    private int configuredEntityId(String key) {
        int hash = Math.abs(key.toLowerCase(Locale.ROOT).hashCode() % 100000);
        return 300000 + hash;
    }

    public void spawnAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            inject(player);
            spawnVisible(player);
        }
    }

    public UUID spawnManagedNpc(String name, Location location, String skinName, boolean lookAtPlayers, Consumer<Player> clickHandler) {
        if (location == null || location.getWorld() == null) return null;
        HousingNpc npc = new HousingNpc(
                ENTITY_IDS.incrementAndGet(),
                UUID.randomUUID(),
                npcName(name),
                location.clone(),
                "",
                "",
                "",
                lookAtPlayers,
                false
        );
        npcs.add(npc);
        if (clickHandler != null) clickHandlers.put(npc.uuid, clickHandler);
        if (skinName != null && !skinName.trim().isEmpty()) {
            fetchSkinLater(npc, skinName.trim());
        }
        spawnAll();
        return npc.uuid;
    }

    public void removeManagedNpc(UUID uuid) {
        HousingNpc npc = uuid == null ? null : findByUuid(uuid);
        if (npc == null) return;
        destroy(npc);
        npcs.remove(npc);
        clickHandlers.remove(uuid);
        selectedNpcs.values().removeIf(selected -> selected.equals(uuid));
    }



    public void ensureCarpenter(World world) {
        if (world == null || !plugin.getStaticConfig().getBoolean("carpenter-npc.enabled", true)) return;
        House house = plugin.getHouseManager() == null ? null : plugin.getHouseManager().getHouseByWorld(world.getName());
        if (house != null && house.isCarpenterRemoved()) {
            removeCarpenterNpc(world);
            return;
        }
        String name = plugin.getStaticConfig().getString("carpenter-npc.name", "&aCarpenter");
        String command = plugin.getStaticConfig().getString("carpenter-npc.command", "housing carpenter");
        String skinName = plugin.getStaticConfig().getString("carpenter-npc.skin-name", "");
        double x = plugin.getStaticConfig().getDouble("carpenter-npc.location.x", -3.0D);
        double y = plugin.getStaticConfig().getDouble("carpenter-npc.location.y", 110.0D);
        double z = plugin.getStaticConfig().getDouble("carpenter-npc.location.z", -58.0D);
        float yaw = (float) plugin.getStaticConfig().getDouble("carpenter-npc.location.yaw", 0.0D);
        float pitch = (float) plugin.getStaticConfig().getDouble("carpenter-npc.location.pitch", 0.0D);
        Location wanted = new Location(world, x, y, z, yaw, pitch);

        for (HousingNpc npc : npcs) {
            if (!npc.location.getWorld().equals(world)) continue;
            if (!"housing carpenter".equalsIgnoreCase(npc.command) && !"/housing carpenter".equalsIgnoreCase(npc.command)) continue;
            boolean moved = npc.location.distanceSquared(wanted) > 0.01D || npc.location.getYaw() != yaw || npc.location.getPitch() != pitch;
            npc.name = npcName(name);
            npc.command = command;
            npc.lookAtPlayers = plugin.getStaticConfig().getBoolean("carpenter-npc.look-at-players", true);
            if ((npc.skinValue.isEmpty() || npc.skinSignature.isEmpty()) && skinName != null && !skinName.trim().isEmpty()) {
                fetchSkinLater(npc, skinName.trim());
            }
            if (moved) {
                destroy(npc);
                npc.location = wanted;
            }
            spawnAll();
            return;
        }

        HousingNpc carpenter = new HousingNpc(
                ENTITY_IDS.incrementAndGet(),
                UUID.randomUUID(),
                npcName(name),
                wanted,
                plugin.getStaticConfig().getString("carpenter-npc.skin-value", ""),
                plugin.getStaticConfig().getString("carpenter-npc.skin-signature", ""),
                command,
                plugin.getStaticConfig().getBoolean("carpenter-npc.look-at-players", true),
                false
        );
        npcs.add(carpenter);
        if ((carpenter.skinValue.isEmpty() || carpenter.skinSignature.isEmpty()) && skinName != null && !skinName.trim().isEmpty()) {
            fetchSkinLater(carpenter, skinName.trim());
        }
        spawnAll();
    }

    public boolean removeCarpenter(House house) {
        if (house == null) return false;
        World world = Bukkit.getWorld(house.getWorldName());
        if (world != null) removeCarpenterNpc(world);
        house.setCarpenterRemoved(true);
        if (plugin.getHouseManager() != null) plugin.getHouseManager().saveAll();
        return true;
    }

    private boolean removeCarpenterNpc(World world) {
        if (world == null) return false;
        boolean removed = false;
        Iterator<HousingNpc> iterator = npcs.iterator();
        while (iterator.hasNext()) {
            HousingNpc npc = iterator.next();
            if (npc.location == null || npc.location.getWorld() == null || !npc.location.getWorld().equals(world)) continue;
            if (!isCarpenter(npc)) continue;
            destroy(npc);
            iterator.remove();
            selectedNpcs.values().removeIf(uuid -> uuid.equals(npc.uuid));
            removed = true;
        }
        return removed;
    }

    private boolean isCarpenter(HousingNpc npc) {
        if (npc == null) return false;
        return "housing carpenter".equalsIgnoreCase(npc.command) || "/housing carpenter".equalsIgnoreCase(npc.command);
    }


    public void create(Player creator) {
        create(creator, plugin.getStaticConfig().getString("housing-npcs.default-name", "Housing"));
    }

    public void create(Player creator, String name) {
        SkinData skin = readSkin(creator);
        HousingNpc npc = new HousingNpc(
                ENTITY_IDS.incrementAndGet(),
                UUID.randomUUID(),
                npcName(name),
                creator.getLocation(),
                skin.value,
                skin.signature,
                "",
                false,
                true
        );
        npcs.add(npc);
        save();
        spawnAll();
        creator.sendMessage(plugin.color("&aCreated housing NPC &e" + npc.name + "&a."));
    }

    public boolean removeNearest(Player player) {
        HousingNpc nearest = nearest(player);
        if (nearest == null) return false;
        return removeNpc(nearest);
    }

    public boolean renameNearest(Player player, String name) {
        HousingNpc npc = nearest(player);
        if (npc == null) return false;
        destroy(npc);
        npc.name = npcName(name);
        save();
        spawnAll();
        return true;
    }

    public boolean setCommandNearest(Player player, String command) {
        HousingNpc npc = nearest(player);
        if (npc == null) return false;
        npc.command = command == null ? "" : command.trim();
        save();
        return true;
    }

    public void setSkinNearest(final Player player, final String skinName) {
        final HousingNpc npc = nearest(player);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo housing NPC found nearby."));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final SkinData skin = fetchSkin(skinName);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (skin.value.isEmpty() || skin.signature.isEmpty()) {
                    player.sendMessage(plugin.color("&cCould not find skin for &e" + skinName + "&c."));
                    return;
                }
                destroy(npc);
                npc.skinValue = skin.value;
                npc.skinSignature = skin.signature;
                save();
                spawnAll();
                player.sendMessage(plugin.color("&aUpdated nearest housing NPC skin to &e" + skinName + "&a."));
            });
        });
    }

    public void select(Player player, String name) {
        HousingNpc npc = findByName(player, name);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo housing NPC named &e" + name + "&c found."));
            return;
        }
        selectedNpcs.put(player.getUniqueId(), npc.uuid);
        player.sendMessage(plugin.color("&aSelected housing NPC &e" + displayName(npc) + "&a."));
    }

    public void selectLookedAtOrNearest(Player player) {
        HousingNpc npc = lookedAt(player);
        if (npc == null) npc = nearest(player);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo housing NPC found nearby or in sight."));
            return;
        }
        selectedNpcs.put(player.getUniqueId(), npc.uuid);
        player.sendMessage(plugin.color("&aSelected housing NPC &e" + displayName(npc) + "&a."));
    }

    public boolean remove(Player player, String name) {
        HousingNpc npc = findByName(player, name);
        return npc != null && removeNpc(npc);
    }

    public boolean removeSelected(Player player) {
        HousingNpc npc = selectedOrLookedAtOrNearest(player);
        return npc != null && removeNpc(npc);
    }

    public boolean renameSelected(Player player, String name) {
        HousingNpc npc = selectedOrLookedAtOrNearest(player);
        if (npc == null) return false;
        destroy(npc);
        npc.name = npcName(name);
        save();
        spawnAll();
        return true;
    }

    public boolean setCommandSelected(Player player, String command) {
        HousingNpc npc = selectedOrLookedAtOrNearest(player);
        if (npc == null) return false;
        npc.command = command == null ? "" : command.trim();
        save();
        return true;
    }

    public void setSkinSelected(final Player player, final String skinName) {
        final HousingNpc npc = selectedOrLookedAtOrNearest(player);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo housing NPC selected or nearby."));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final SkinData skin = fetchSkin(skinName);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (skin.value.isEmpty() || skin.signature.isEmpty()) {
                    player.sendMessage(plugin.color("&cCould not find skin for &e" + skinName + "&c."));
                    return;
                }
                destroy(npc);
                npc.skinValue = skin.value;
                npc.skinSignature = skin.signature;
                save();
                spawnAll();
                player.sendMessage(plugin.color("&aUpdated selected housing NPC skin to &e" + skinName + "&a."));
            });
        });
    }

    public boolean teleport(Player player, String name, Location location) {
        HousingNpc npc = findByName(player, name);
        if (npc == null) return false;
        return teleportNpc(npc, location);
    }

    public boolean teleportSelected(Player player, Location location) {
        HousingNpc npc = selectedOrLookedAtOrNearest(player);
        return npc != null && teleportNpc(npc, location);
    }

    public void toggleLook(Player player) {
        HousingNpc npc = selectedOrLookedAtOrNearest(player);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo housing NPC selected or nearby."));
            return;
        }
        npc.lookAtPlayers = !npc.lookAtPlayers;
        save();
        player.sendMessage(plugin.color(npc.lookAtPlayers ? "&aNPC look mode enabled." : "&cNPC look mode disabled."));
    }

    public void list(Player player) {
        player.sendMessage(plugin.color("&6Housing NPCs"));
        int count = 0;
        for (HousingNpc npc : npcs) {
            if (!npc.location.getWorld().equals(player.getWorld())) continue;
            count++;
            player.sendMessage(plugin.color("&7- &e" + displayName(npc) + " &8(" + Math.round(npc.location.distance(player.getLocation())) + " blocks)"));
        }
        if (count == 0) player.sendMessage(plugin.color("&7No NPCs in this world."));
    }

    public List<String> names(Player player) {
        List<String> names = new ArrayList<>();
        for (HousingNpc npc : npcs) {
            if (player == null || npc.location.getWorld().equals(player.getWorld())) names.add(displayName(npc));
        }
        return names;
    }

    private HousingNpc selectedOrLookedAtOrNearest(Player player) {
        UUID selected = selectedNpcs.get(player.getUniqueId());
        HousingNpc npc = selected == null ? null : findByUuid(selected);
        if (npc != null && npc.location.getWorld().equals(player.getWorld())) return npc;
        npc = lookedAt(player);
        return npc == null ? nearest(player) : npc;
    }

    private boolean removeNpc(HousingNpc npc) {
        destroy(npc);
        npcs.remove(npc);
        clickHandlers.remove(npc.uuid);
        selectedNpcs.values().removeIf(uuid -> uuid.equals(npc.uuid));
        save();
        return true;
    }

    private boolean teleportNpc(HousingNpc npc, Location location) {
        destroy(npc);
        npc.location = location.clone();
        save();
        spawnAll();
        return true;
    }

    private HousingNpc nearest(Player player) {
        HousingNpc nearest = null;
        double best = 16.0D;
        for (HousingNpc npc : npcs) {
            if (!npc.location.getWorld().equals(player.getWorld())) continue;
            double distance = npc.location.distanceSquared(player.getLocation());
            if (distance < best) {
                best = distance;
                nearest = npc;
            }
        }
        return nearest;
    }

    public int size() {
        return npcs.size();
    }

    public void reload() {
        for (HousingNpc npc : npcs) {
            destroy(npc);
        }
        load();
        spawnAll();
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        inject(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnVisible(event.getPlayer()), 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnVisible(event.getPlayer()), 60L);
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        inject(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnVisible(event.getPlayer()), 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnVisible(event.getPlayer()), 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnVisible(event.getPlayer()), 60L);
    }

    @EventHandler
    public void onWorldLoad(final WorldLoadEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : event.getWorld().getPlayers()) {
                inject(player);
                spawnVisible(player);
            }
        }, 20L);
    }

    private void save() {
        for (HousingNpc npc : npcs) {
            if (!npc.saveToConfig) continue;
            if (plugin.getHousingRepository() == null) continue;
            String data=npc.uuid+"|"+enc(npc.name)+"|"+enc(npc.location.getWorld().getName())+"|"+npc.location.getX()+"|"+npc.location.getY()+"|"+npc.location.getZ()+"|"+npc.location.getYaw()+"|"+npc.location.getPitch()+"|"+enc(npc.skinValue)+"|"+enc(npc.skinSignature)+"|"+enc(npc.command)+"|"+npc.lookAtPlayers+"|1";
            try { plugin.getHousingRepository().saveNpc(npc.uuid.toString(), null, data); }
            catch (Exception ex) { plugin.getLogger().warning("Could not save runtime NPC: " + ex.getMessage()); }
        }
    }

    private static String enc(String value){return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString((value==null?"":value).getBytes(StandardCharsets.UTF_8));}
    private static String dec(String value){return new String(java.util.Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8);}

    private void fetchSkinLater(final HousingNpc npc, final String skinName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final SkinData skin = fetchSkin(skinName);
            if (skin.value.isEmpty() || skin.signature.isEmpty()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!npcs.contains(npc)) return;
                destroy(npc);
                npc.skinValue = skin.value;
                npc.skinSignature = skin.signature;
                spawnAll();
            });
        });
    }

    private void spawnVisible(final Player viewer) {
        for (final HousingNpc npc : npcs) {
            if (!npc.location.getWorld().equals(viewer.getWorld())) continue;
            try {
                ensureNameplate(npc);
                Object entityPlayer = createEntityPlayer(npc);
                applyNpcTeam(viewer, npc);
                sendPacket(viewer, playerInfoPacket(entityPlayer));
                sendPacket(viewer, spawnPacket(entityPlayer));
                sendPacket(viewer, newHeadRotationPacket(entityPlayer, npc.location.getYaw()));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        Object entityPlayer1 = createEntityPlayer(npc);
                        sendPacket(viewer, playerInfoRemovePacket(entityPlayer1, npc.uuid));
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Could not hide NPC from tab list: " + describe(ex));
                    }
                }, 40L);
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not spawn housing NPC: " + describe(ex));
            }
        }
    }

    public void applyHiddenNpcTeams(Scoreboard board) {
        if (board == null) return;
        for (HousingNpc npc : npcs) {
            applyNpcTeam(board, npc);
        }
    }

    private void destroy(HousingNpc npc) {
        removeNameplate(npc);
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                sendPacket(player, entityDestroyPacket(npc.entityId));
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not remove housing NPC: " + describe(ex));
            }
        }
    }

    private void startLookTask() {
        if (lookTaskId != -1) return;
        lookTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (HousingNpc npc : npcs) {
                if (!npc.lookAtPlayers) continue;
                updateLook(npc);
            }
        }, 20L, 10L).getTaskId();
    }

    private void updateLook(HousingNpc npc) {
        Player nearest = null;
        double best = 100.0D;
        for (Player player : npc.location.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(npc.location);
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        if (nearest == null) return;
        Location look = npc.location.clone();
        Vector direction = nearest.getEyeLocation().toVector().subtract(look.toVector());
        look.setDirection(direction);
        npc.location.setYaw(look.getYaw());
        npc.location.setPitch(look.getPitch());
        for (Player viewer : npc.location.getWorld().getPlayers()) {
            try {
                Object entityPlayer = createEntityPlayer(npc);
                sendPacket(viewer, newHeadRotationPacket(entityPlayer, npc.location.getYaw()));
            } catch (Exception ignored) {
            }
        }
    }

    private void inject(final Player player) {
        try {
            Object channel = channel(player);
            Object pipeline = call(channel, "pipeline");
            String handlerName = handlerName(player);
            if (call(pipeline, "get", handlerName) != null) return;
            Object handler = Proxy.newProxyInstance(
                    Class.forName("io.netty.channel.ChannelInboundHandler").getClassLoader(),
                    new Class[]{Class.forName("io.netty.channel.ChannelInboundHandler")},
                    (proxy, method, args) -> handleChannelMethod(player, handlerName, method, args)
            );
            call(pipeline, "addBefore", "packet_handler", handlerName, handler);
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not inject housing NPC packet listener for " + player.getName() + ": " + describe(ex));
        }
    }

    private String handlerName(Player player) {
        return "housing_npc_" + player.getUniqueId().toString().replace("-", "");
    }

    private boolean handlePacket(final Player player, Object packet) {
        if (!isInteractPacket(packet)) return false;
        final HousingNpc npc = findByEntityId(readEntityId(packet));
        if (npc == null) return false;
        final Consumer<Player> clickHandler = clickHandlers.get(npc.uuid);
        if (npc.command.isEmpty() && clickHandler == null) return true;
        long now = System.currentTimeMillis();
        Long last = clickCooldowns.get(player.getUniqueId());
        if (last != null && now - last < 500L) return true;
        clickCooldowns.put(player.getUniqueId(), now);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (clickHandler != null) {
                clickHandler.accept(player);
                return;
            }
            String command = npc.command.replace("%player%", player.getName()).trim();
            if (command.startsWith("/")) command = command.substring(1);
            player.performCommand(command);
        });
        return true;
    }

    private boolean isInteractPacket(Object packet) {
        String packetName = packet.getClass().getSimpleName();
        String packetClass = packet.getClass().getName();
        return packetName.equals("PacketPlayInUseEntity")
                || packetName.equals("ServerboundInteractPacket")
                || packetClass.endsWith(".PacketPlayInUseEntity")
                || packetClass.endsWith(".ServerboundInteractPacket");
    }

    private HousingNpc findByEntityId(int entityId) {
        for (HousingNpc npc : npcs) {
            if (npc.entityId == entityId) return npc;
        }
        return null;
    }

    private HousingNpc findByUuid(UUID uuid) {
        for (HousingNpc npc : npcs) {
            if (npc.uuid.equals(uuid)) return npc;
        }
        return null;
    }

    private HousingNpc findByName(Player player, String name) {
        String wanted = cleanName(name);
        for (HousingNpc npc : npcs) {
            if (!npc.location.getWorld().equals(player.getWorld())) continue;
            if (cleanName(displayName(npc)).equalsIgnoreCase(wanted) || cleanName(npc.name).equalsIgnoreCase(wanted)) return npc;
        }
        return null;
    }

    private HousingNpc lookedAt(Player player) {
        Vector eye = player.getEyeLocation().toVector();
        Vector direction = player.getEyeLocation().getDirection().normalize();
        HousingNpc bestNpc = null;
        double bestDistance = 32.0D;
        for (HousingNpc npc : npcs) {
            if (!npc.location.getWorld().equals(player.getWorld())) continue;
            Vector toNpc = npc.location.clone().add(0.0D, 1.6D, 0.0D).toVector().subtract(eye);
            double distance = toNpc.length();
            if (distance > bestDistance || distance <= 0.0D) continue;
            double angle = direction.dot(toNpc.normalize());
            if (angle < 0.98D) continue;
            bestDistance = distance;
            bestNpc = npc;
        }
        return bestNpc;
    }

    private String displayName(HousingNpc npc) {
        return profileBaseName(npc == null ? null : npc.name);
    }

    private String cleanName(String name) {
        String clean = ChatColor.stripColor(plugin.color(name == null ? "" : name));
        if (clean == null) return "";
        return clean.replaceAll("(?i)\\s*RIGHT\\s+CLICK\\s*$", "").trim();
    }

    private int readEntityId(Object packet) {
        try {
            Object value = call(packet, "getEntityId");
            if (value instanceof Number) return ((Number) value).intValue();
        } catch (Exception ignored) {
        }
        try {
            Field field = findField(packet.getClass(), "a");
            field.setAccessible(true);
            Object value = field.get(packet);
            return value instanceof Number ? ((Number) value).intValue() : -1;
        } catch (Exception ignored) {
            for (Field field : packet.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(packet);
                    if (value instanceof Number) return ((Number) value).intValue();
                } catch (Exception ignoredAgain) {
                }
            }
        }
        return -1;
    }


    private void ensureNameplate(HousingNpc npc) {
        if (npc == null || npc.location == null || npc.location.getWorld() == null) return;
        List<ArmorStand> existing = nameplates.get(npc.uuid);
        if (existing != null && existing.size() == 2 && existing.get(0).isValid() && existing.get(1).isValid()) return;
        removeNameplate(npc);

        String[] lines = npcNameLines(npc.name);
        ArmorStand top = spawnNameLine(npc.location.clone().add(0.0D, 2.35D, 0.0D), lines[0]);
        ArmorStand bottom = spawnNameLine(npc.location.clone().add(0.0D, 2.10D, 0.0D), lines[1]);
        nameplates.put(npc.uuid, Arrays.asList(top, bottom));
    }

    private ArmorStand spawnNameLine(Location location, String text) {
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        configureNameLine(stand);
        stand.setCustomName(plugin.color(text));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (stand.isValid() && !stand.isDead()) configureNameLine(stand);
        }, 1L);
        return stand;
    }

    private void configureNameLine(ArmorStand stand) {
        stand.setCustomNameVisible(true);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setCanPickupItems(false);
        stand.setRemoveWhenFarAway(false);
        tryCall(stand, "setInvisible", true);
        tryCall(stand, "setInvulnerable", true);
        tryCall(stand, "setCollidable", false);
        tryCall(stand, "setSilent", true);
        try { stand.setMarker(true); } catch (Throwable ignored) {}
        try { stand.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false)); } catch (Throwable ignored) {}
    }

    private void removeNameplate(HousingNpc npc) {
        if (npc == null) return;
        List<ArmorStand> stands = nameplates.remove(npc.uuid);
        if (stands == null) return;
        for (ArmorStand stand : stands) {
            if (stand != null && !stand.isDead()) stand.remove();
        }
    }

    private void clearNameplates() {
        for (List<ArmorStand> stands : nameplates.values()) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) stand.remove();
            }
        }
        nameplates.clear();
    }

    private String[] npcNameLines(String configured) {
        String formatted = plugin.color(configured == null ? "" : configured.replace("\\n", "\n"));
        String[] raw = formatted.split("\\n");
        String first = raw.length > 0 && !raw[0].trim().isEmpty() ? raw[0] : "&b" + profileBaseName(configured);
        String second = raw.length > 1 && !raw[1].trim().isEmpty() ? raw[1] : "&e&lRIGHT CLICK";
        return new String[]{first, second};
    }

    private String profileBaseName(String configured) {
        String text = configured == null ? "Housing" : configured.replace("\\n", "\n");
        String firstLine = text.split("\\n")[0];
        String clean = ChatColor.stripColor(plugin.color(firstLine));
        if (clean == null || clean.trim().isEmpty()) clean = ChatColor.stripColor(plugin.color(text));
        if (clean == null || clean.trim().isEmpty()) clean = "Housing";
        clean = clean.replaceAll("(?i)\\s*RIGHT\\s+CLICK\\s*$", "").trim();
        return clean.isEmpty() ? "Housing" : clean;
    }

    private Object createEntityPlayer(HousingNpc npc) throws Exception {
        if (classExists("net.minecraft.server.level.ServerPlayer")) return createModernServerPlayer(npc);

        Object minecraftServer = call(Bukkit.getServer(), "getServer");
        Object worldServer = call(npc.location.getWorld(), "getHandle");
        Object profile = newGameProfile(npc);
        Constructor<?> interactManagerConstructor = nmsClass("PlayerInteractManager").getConstructor(nmsClass("World"));
        Object interactManager = interactManagerConstructor.newInstance(worldServer);
        Object entityPlayer = nmsClass("EntityPlayer")
                .getConstructor(nmsClass("MinecraftServer"), nmsClass("WorldServer"), classForName("com.mojang.authlib.GameProfile"), nmsClass("PlayerInteractManager"))
                .newInstance(minecraftServer, worldServer, profile, interactManager);

        call(entityPlayer, "setLocation", npc.location.getX(), npc.location.getY(), npc.location.getZ(), npc.location.getYaw(), npc.location.getPitch());
        setEntityId(entityPlayer, npc.entityId);
        return entityPlayer;
    }

    private Object createModernServerPlayer(HousingNpc npc) throws Exception {
        Object minecraftServer = call(Bukkit.getServer(), "getServer");
        Object serverLevel = call(npc.location.getWorld(), "getHandle");
        Object profile = newGameProfile(npc);
        Object clientInformation = call(classForName("net.minecraft.server.level.ClientInformation"), "createDefault");
        Object entityPlayer = classForName("net.minecraft.server.level.ServerPlayer")
                .getConstructor(classForName("net.minecraft.server.MinecraftServer"), classForName("net.minecraft.server.level.ServerLevel"), classForName("com.mojang.authlib.GameProfile"), classForName("net.minecraft.server.level.ClientInformation"))
                .newInstance(minecraftServer, serverLevel, profile, clientInformation);
        moveEntity(entityPlayer, npc.location);
        setEntityId(entityPlayer, npc.entityId);
        return entityPlayer;
    }

    private void moveEntity(Object entity, Location location) {
        Object moved = tryCall(entity, "moveTo", location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        if (moved != null) return;
        moved = tryCall(entity, "absMoveTo", location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        if (moved != null) return;
        tryCall(entity, "setPos", location.getX(), location.getY(), location.getZ());
        tryCall(entity, "setYRot", location.getYaw());
        tryCall(entity, "setXRot", location.getPitch());
    }

    private Object newGameProfile(HousingNpc npc) throws Exception {
        String profileName = profileName(profileBaseName(npc.name));
        if (!npc.skinValue.isEmpty() && !npc.skinSignature.isEmpty()) {
            Object property = newTextureProperty(npc);
            Object profile = newGameProfileWithProperties(npc.uuid, profileName, property);
            if (profile != null) {
                return profile;
            }

            profile = classForName("com.mojang.authlib.GameProfile")
                    .getConstructor(UUID.class, String.class)
                    .newInstance(npc.uuid, profileName);
            if (!addProfileProperty(profile, property)) {
                plugin.getLogger().warning("Could not apply skin to housing NPC '" + profileBaseName(npc.name) + "'. Spawning it without a skin.");
            }
            return profile;
        }
        return classForName("com.mojang.authlib.GameProfile")
                .getConstructor(UUID.class, String.class)
                .newInstance(npc.uuid, profileName);
    }

    private Object newTextureProperty(HousingNpc npc) throws Exception {
        return classForName("com.mojang.authlib.properties.Property")
                .getConstructor(String.class, String.class, String.class)
                .newInstance("textures", npc.skinValue, npc.skinSignature);
    }

    private Object newGameProfileWithProperties(UUID uuid, String name, Object property) {
        try {
            Object multimap = call(classForName("com.google.common.collect.HashMultimap"), "create");
            Method put = multimap.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(multimap, "textures", property);
            Object propertyMap = classForName("com.mojang.authlib.properties.PropertyMap")
                    .getConstructor(classForName("com.google.common.collect.Multimap"))
                    .newInstance(multimap);
            return classForName("com.mojang.authlib.GameProfile")
                    .getConstructor(UUID.class, String.class, classForName("com.mojang.authlib.properties.PropertyMap"))
                    .newInstance(uuid, name, propertyMap);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean addProfileProperty(Object profile, Object property) {
        try {
            Object properties = profileProperties(profile);
            Method put = properties.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(properties, "textures", property);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private SkinData readSkin(Player player) {
        try {
            Object profile = call(player, "getProfile");
            Object properties = profileProperties(profile);
            Object textures = call(properties, "get", "textures");
            Object iterator = call(textures, "iterator");
            if (!(Boolean) call(iterator, "hasNext")) return new SkinData("", "");
            Object texture = call(iterator, "next");
            return new SkinData(propertyValue(texture), propertySignature(texture));
        } catch (Exception ex) {
            return new SkinData("", "");
        }
    }

    private Object profileProperties(Object profile) throws Exception {
        try {
            return call(profile, "getProperties");
        } catch (NoSuchMethodException ignored) {
            return call(profile, "properties");
        }
    }

    private String propertyValue(Object property) throws Exception {
        try {
            return (String) call(property, "getValue");
        } catch (NoSuchMethodException ignored) {
            return (String) call(property, "value");
        }
    }

    private String propertySignature(Object property) throws Exception {
        try {
            return (String) call(property, "getSignature");
        } catch (NoSuchMethodException ignored) {
            return (String) call(property, "signature");
        }
    }

    private SkinData fetchSkin(String name) {
        try {
            String profile = readUrl("https://api.mojang.com/users/profiles/minecraft/" + name);
            String id = match(profile, "\"id\"\\s*:\\s*\"([^\"]+)\"");
            if (id.isEmpty()) return new SkinData("", "");
            String session = readUrl("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false");
            String value = match(session, "\"value\"\\s*:\\s*\"([^\"]+)\"");
            String signature = match(session, "\"signature\"\\s*:\\s*\"([^\"]+)\"");
            return new SkinData(value, signature);
        } catch (Exception ex) {
            return new SkinData("", "");
        }
    }

    private String readUrl(String url) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(URI.create(url).toURL().openStream(), StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) builder.append(line);
        reader.close();
        return builder.toString();
    }

    private String match(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private Object playerInfoPacket(Object entityPlayer) throws Exception {
        if (classExists("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket")) {
            Class<?> packetClass = classForName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Class<?> actionClass = classForName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            Object actionValue = enumConstant(actionClass, "ADD_PLAYER");
            @SuppressWarnings({"rawtypes", "unchecked"})
            EnumSet<?> actions = EnumSet.of((Enum) actionValue);

            Object entry = playerInfoEntry(entityPlayer);
            try {
                return packetClass.getConstructor(EnumSet.class, Collection.class).newInstance(actions, new ArrayList<>(Collections.singletonList(entry)));
            } catch (Throwable ignored) {
            }

            try {
                return packetClass.getConstructor(EnumSet.class, List.class).newInstance(actions, new ArrayList<>(Collections.singletonList(entry)));
            } catch (Throwable ignored) {
            }

            return packetClass.getConstructor(EnumSet.class, List.class).newInstance(actions, new ArrayList<>(Collections.singletonList(entityPlayer)));
        }
        return oldPlayerInfoPacket("ADD_PLAYER", entityPlayer);
    }

    private Object playerInfoEntry(Object entityPlayer) throws Exception {
        Class<?> entryClass = classForName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");
        Class<?> gameTypeClass = classForName("net.minecraft.world.level.GameType");
        Class<?> componentClass = classForName("net.minecraft.network.chat.Component");
        Class<?> chatSessionDataClass = classForName("net.minecraft.network.chat.RemoteChatSession$Data");
        Object gameMode = enumConstant(gameTypeClass, "SURVIVAL");
        Object profile = call(entityPlayer, "getGameProfile");
        UUID uuid = (UUID) call(entityPlayer, "getUUID");
        return entryClass.getConstructor(UUID.class, classForName("com.mojang.authlib.GameProfile"), boolean.class, int.class, gameTypeClass, componentClass, boolean.class, int.class, chatSessionDataClass)
                .newInstance(uuid, profile, true, 0, gameMode, null, true, 0, null);
    }

    private Object oldPlayerInfoPacket(String action, Object entityPlayer) throws Exception {
        Class<?> packetClass = nmsClass("PacketPlayOutPlayerInfo");
        Class<?> actionClass = nmsClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
        Object actionValue = enumConstant(actionClass, action);
        Object players = newEntityPlayerArray();
        Array.set(players, 0, entityPlayer);
        return packetClass.getConstructor(actionClass, players.getClass()).newInstance(actionValue, players);
    }

    private Object playerInfoRemovePacket(Object entityPlayer, UUID uuid) throws Exception {
        if (classExists("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket")) {
            return classForName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket")
                    .getConstructor(List.class)
                    .newInstance(new ArrayList<>(Collections.singletonList(uuid)));
        }
        return oldPlayerInfoPacket("REMOVE_PLAYER", entityPlayer);
    }

    private Object spawnPacket(Object entityPlayer) throws Exception {
        if (classExists("net.minecraft.network.protocol.game.ClientboundAddEntityPacket")) {
            Class<?> packetClass = classForName("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
            Class<?> entityClass = classForName("net.minecraft.world.entity.Entity");

            try {
                return packetClass.getConstructor(entityClass, int.class).newInstance(entityPlayer, 0);
            } catch (Throwable ignored) {
            }
            try {
                return packetClass.getConstructor(entityClass).newInstance(entityPlayer);
            } catch (Throwable ignored) {
            }

            Class<?> entityTypeClass = classForName("net.minecraft.world.entity.EntityType");
            Class<?> vec3Class = classForName("net.minecraft.world.phys.Vec3");
            Object movement = zeroVec3(vec3Class);
            Object entityType = call(entityPlayer, "getType");
            int entityId = ((Number) call(entityPlayer, "getId")).intValue();
            UUID uuid = (UUID) call(entityPlayer, "getUUID");
            double x = ((Number) call(entityPlayer, "getX")).doubleValue();
            double y = ((Number) call(entityPlayer, "getY")).doubleValue();
            double z = ((Number) call(entityPlayer, "getZ")).doubleValue();
            float xRot = ((Number) call(entityPlayer, "getXRot")).floatValue();
            float yRot = ((Number) call(entityPlayer, "getYRot")).floatValue();
            return packetClass.getConstructor(int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, entityTypeClass, int.class, vec3Class, double.class)
                    .newInstance(entityId, uuid, x, y, z, xRot, yRot, entityType, 0, movement, (double) yRot);
        }
        return newPacket("PacketPlayOutNamedEntitySpawn", nmsClass("EntityHuman")).newInstance(entityPlayer);
    }

    private Object zeroVec3(Class<?> vec3Class) throws Exception {
        Field zero = vec3Class.getField("ZERO");
        return zero.get(null);
    }

    private Object entityDestroyPacket(int entityId) throws Exception {
        if (classExists("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket")) {
            return classForName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket")
                    .getConstructor(int[].class)
                    .newInstance((Object) new int[]{entityId});
        }
        return newPacket("PacketPlayOutEntityDestroy", int[].class).newInstance((Object) new int[]{entityId});
    }

    private Object newHeadRotationPacket(Object entity, float yaw) throws Exception {
        if (classExists("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket")) {
            return classForName("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket")
                    .getConstructor(classForName("net.minecraft.world.entity.Entity"), byte.class)
                    .newInstance(entity, angle(yaw));
        }
        return nmsClass("PacketPlayOutEntityHeadRotation").getConstructor(nmsClass("Entity"), byte.class).newInstance(entity, angle(yaw));
    }

    private byte angle(float value) {
        return (byte) ((int) (value * 256.0F / 360.0F));
    }

    private String describe(Throwable throwable) {
        Throwable current = throwable;
        if (current instanceof InvocationTargetException && ((InvocationTargetException) current).getTargetException() != null) {
            current = ((InvocationTargetException) current).getTargetException();
        }
        StringBuilder builder = new StringBuilder(current.getClass().getSimpleName());
        if (current.getMessage() != null && !current.getMessage().isEmpty()) {
            builder.append(": ").append(current.getMessage());
        }
        Throwable cause = current.getCause();
        if (cause != null && cause != current) {
            builder.append(" (caused by ").append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                builder.append(": ").append(cause.getMessage());
            }
            builder.append(")");
        }
        StackTraceElement location = firstUsefulFrame(current);
        if (location != null) {
            builder.append(" at ").append(location.getClassName()).append(".").append(location.getMethodName()).append(":").append(location.getLineNumber());
        }
        return builder.toString();
    }

    private StackTraceElement firstUsefulFrame(Throwable throwable) {
        if (throwable == null) return null;
        for (StackTraceElement element : throwable.getStackTrace()) {
            if (element.getClassName().startsWith("net.fakepixel.housing.")) {
                return element;
            }
        }
        StackTraceElement[] stack = throwable.getStackTrace();
        return stack.length == 0 ? null : stack[0];
    }

    private String npcName(String configured) {
        String clean = configured == null || configured.trim().isEmpty() ? "Housing" : configured.trim();
        String stripped = ChatColor.stripColor(plugin.color(clean));
        if (stripped != null && stripped.toUpperCase().contains("RIGHT CLICK")) return clean;
        String format = plugin.getStaticConfig().getString("housing-npcs.name-format", "%npcname% &e&lRIGHT CLICK");
        if (format == null || format.trim().isEmpty()) return clean;
        return format.replace("%npcname%", clean).replace("\\n", "\n").trim();
    }

    private String profileName(String displayName) {
        String clean = ChatColor.stripColor(plugin.color(displayName)).replaceAll("[^A-Za-z0-9_]", "");
        if (clean.isEmpty()) clean = "NPC";
        return clean.length() > 16 ? clean.substring(0, 16) : clean;
    }

    private void applyNpcTeam(Player viewer, HousingNpc npc) {
        applyNpcTeam(viewer.getScoreboard(), npc);
    }

    private void applyNpcTeam(Scoreboard board, HousingNpc npc) {
        if (board == null || npc == null) return;
        String teamName = ("hnpc" + npc.entityId);
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        String profileName = profileName(profileBaseName(npc.name));
        team.setPrefix("");
        team.setSuffix("");
        hideTeamNameTag(team);
        if (!team.hasEntry(profileName)) team.addEntry(profileName);
    }


    private void hideTeamNameTag(Team team) {
        try {
            Class<?> optionClass = Class.forName("org.bukkit.scoreboard.Team$Option");
            Class<?> statusClass = Class.forName("org.bukkit.scoreboard.Team$OptionStatus");
            Object option = enumConstant(optionClass, "NAME_TAG_VISIBILITY");
            Object status = enumConstant(statusClass, "NEVER");
            Team.class.getMethod("setOption", optionClass, statusClass).invoke(team, option, status);
            return;
        } catch (Throwable ignored) {
        }
        try {
            Class<?> visibilityClass = Class.forName("org.bukkit.scoreboard.NameTagVisibility");
            Object never = enumConstant(visibilityClass, "NEVER");
            Team.class.getMethod("setNameTagVisibility", visibilityClass).invoke(team, never);
        } catch (Throwable ignored) {
        }
    }

    private String trim(String text) {
        if (text == null || text.length() <= 16) return text == null ? "" : text;
        return text.substring(0, 16);
    }

    private Object handleChannelMethod(Player player, String handlerName, Method method, Object[] args) throws Exception {
        if (method.getName().equals("channelRead") && args != null && args.length >= 2) {
            if (handlePacket(player, args[1])) return null;
            call(args[0], "fireChannelRead", args[1]);
            return null;
        }
        if (method.getName().equals("exceptionCaught") && args != null && args.length >= 2) {
            call(args[0], "fireExceptionCaught", args[1]);
            return null;
        }
        if (method.getName().equals("toString")) return handlerName;
        if (method.getReturnType() == boolean.class) return false;
        return null;
    }

    private Constructor<?> newPacket(String name, Class<?>... parameterTypes) throws Exception {
        return nmsClass(name).getConstructor(parameterTypes);
    }

    private Object enumConstant(Class<?> enumClass, String name) {
        for (Object constant : enumClass.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) return constant;
        }
        throw new IllegalArgumentException(name);
    }

    private Object newEntityPlayerArray() throws ClassNotFoundException {
        return Array.newInstance(nmsClass("EntityPlayer"), 1);
    }

    private void sendPacket(Player player, Object packet) throws Exception {
        Object connection = playerConnection(call(player, "getHandle"));
        try {
            call(connection, "send", packet);
        } catch (Exception ex) {
            call(connection, "sendPacket", packet);
        }
    }

    private Object call(Object target, String method, Object... args) throws Exception {
        if (target instanceof Class<?>) {
            return callStatic((Class<?>) target, method, args);
        }
        Method found = null;
        for (Method candidate : target.getClass().getMethods()) {
            if (!candidate.getName().equals(method) || candidate.getParameterTypes().length != args.length) continue;
            if (!accepts(candidate.getParameterTypes(), args)) continue;
            found = candidate;
            break;
        }
        if (found == null) throw new NoSuchMethodException(method);
        found.setAccessible(true);
        return found.invoke(target, args);
    }

    private Object tryCall(Object target, String method, Object... args) {
        try {
            return call(target, method, args);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object callStatic(Class<?> target, String method, Object... args) throws Exception {
        Method found = null;
        for (Method candidate : target.getMethods()) {
            if (!candidate.getName().equals(method) || candidate.getParameterTypes().length != args.length) continue;
            if (!accepts(candidate.getParameterTypes(), args)) continue;
            found = candidate;
            break;
        }
        if (found == null) throw new NoSuchMethodException(method);
        found.setAccessible(true);
        return found.invoke(null, args);
    }

    private boolean accepts(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) continue;
            if (!wrap(types[i]).isAssignableFrom(args[i].getClass())) return false;
        }
        return true;
    }

    private Class<?> wrap(Class<?> type) {
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

    private Object playerConnection(Object entityPlayer) throws Exception {
        Field field = findFieldOrNull(entityPlayer.getClass(), "playerConnection");
        if (field == null) field = findFieldOrNull(entityPlayer.getClass(), "connection");
        if (field == null) field = findFieldByTypeName(entityPlayer.getClass(), "ServerGamePacketListenerImpl");
        if (field == null) throw new NoSuchFieldException("playerConnection/connection");
        field.setAccessible(true);
        return field.get(entityPlayer);
    }

    private Object channel(Player player) throws Exception {
        Object connection = playerConnection(call(player, "getHandle"));
        Field networkManagerField = findFieldOrNull(connection.getClass(), "networkManager");
        if (networkManagerField == null) networkManagerField = findFieldOrNull(connection.getClass(), "connection");
        if (networkManagerField == null) networkManagerField = findFieldByTypeName(connection.getClass(), "net.minecraft.network.Connection");
        if (networkManagerField == null) throw new NoSuchFieldException("networkManager/connection");
        networkManagerField.setAccessible(true);
        Object networkManager = networkManagerField.get(connection);
        Field channelField = findFieldOrNull(networkManager.getClass(), "channel");
        if (channelField == null) channelField = findFieldByTypeName(networkManager.getClass(), "io.netty.channel.Channel");
        if (channelField == null) throw new NoSuchFieldException("channel");
        channelField.setAccessible(true);
        return channelField.get(networkManager);
    }

    private void setEntityId(Object entityPlayer, int id) throws Exception {
        Field field = findField(entityPlayer.getClass(), "id");
        field.setAccessible(true);
        field.set(entityPlayer, id);
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Field field = findFieldOrNull(type, name);
        if (field != null) return field;
        throw new NoSuchFieldException(name);
    }

    private Field findFieldOrNull(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Field findFieldByTypeName(Class<?> type, String typeName) {
        Class<?> current = type;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType().getName().equals(typeName) || field.getType().getSimpleName().equals(typeName)) return field;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Class<?> nmsClass(String name) throws ClassNotFoundException {
        try {
            return classForName("net.minecraft.server." + version() + "." + name);
        } catch (ClassNotFoundException ignored) {
            String modern = modernClassName(name);
            if (modern != null) return classForName(modern);
            throw ignored;
        }
    }

    private Class<?> classForName(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    private String version() {
        String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        return parts.length > 3 ? parts[3] : "";
    }

    private String modernClassName(String oldName) {
        if (oldName.equals("MinecraftServer")) return "net.minecraft.server.MinecraftServer";
        if (oldName.equals("WorldServer")) return "net.minecraft.server.level.ServerLevel";
        if (oldName.equals("World")) return "net.minecraft.world.level.Level";
        if (oldName.equals("EntityPlayer")) return "net.minecraft.server.level.ServerPlayer";
        if (oldName.equals("EntityHuman")) return "net.minecraft.world.entity.player.Player";
        if (oldName.equals("Entity")) return "net.minecraft.world.entity.Entity";
        if (oldName.equals("PlayerInteractManager")) return "net.minecraft.server.level.ServerPlayerGameMode";
        return null;
    }

    private boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static class HousingNpc {
        private final int entityId;
        private final UUID uuid;
        private String name;
        private Location location;
        private String skinValue;
        private String skinSignature;
        private String command;
        private boolean lookAtPlayers;
        private final boolean saveToConfig;

        private HousingNpc(int entityId, UUID uuid, String name, Location location, String skinValue, String skinSignature, String command, boolean lookAtPlayers, boolean saveToConfig) {
            this.entityId = entityId;
            this.uuid = uuid;
            this.name = name;
            this.location = location;
            this.skinValue = skinValue == null ? "" : skinValue;
            this.skinSignature = skinSignature == null ? "" : skinSignature;
            this.command = command == null ? "" : command;
            this.lookAtPlayers = lookAtPlayers;
            this.saveToConfig = saveToConfig;
        }
    }

    private static class SkinData {
        private final String value;
        private final String signature;

        private SkinData(String value, String signature) {
            this.value = value == null ? "" : value;
            this.signature = signature == null ? "" : signature;
        }
    }
}
