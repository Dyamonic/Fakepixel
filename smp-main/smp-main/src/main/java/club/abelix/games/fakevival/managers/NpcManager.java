package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.network.SMPServerType;
import club.abelix.games.fakevival.util.CC;
import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class NpcManager {
    private static final Duration SKIN_REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final double LOOK_RADIUS = 12.0D;

    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final NamespacedKey npcKey;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<UUID, String> spawnedEntityIds = new HashMap<>();
    private final Map<String, PacketNpc> packetNpcs = new HashMap<>();
    private final Map<String, PropertyMap> skinCache = new ConcurrentHashMap<>();
    private final Map<String, CachedSkin> cachedSkins = new ConcurrentHashMap<>();
    private final Queue<SkinRequest> skinQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> queuedSkinNames = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> pendingSkinNpcs = new ConcurrentHashMap<>();
    private final Set<String> failedSkinWarnings = ConcurrentHashMap.newKeySet();
    private final Deque<Long> remoteSkinRequests = new ArrayDeque<>();
    private final AtomicBoolean remoteSkinRequestActive = new AtomicBoolean(false);
    private final Set<String> pendingSpawnRetries = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<String>> sentTeamsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastTeamSentByPlayer = new ConcurrentHashMap<>();
    private final AtomicInteger duplicateTeamCreatesPrevented = new AtomicInteger();
    private volatile boolean skinCachePreloadComplete;
    private volatile int dbCachedSkinCount;
    private volatile long skinBackoffUntil;
    private volatile long lastSkin429At;
    private volatile long nextRemoteSkinRequestAt;
    private volatile long nextSkinDatabaseWarningAt;
    private volatile int consecutiveSkin429s;
    private BukkitTask lookTask;
    private BukkitTask repairTask;
    private BukkitTask skinQueueTask;

    public NpcManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.npcKey = new NamespacedKey(plugin, "fakevival_npc");
    }

    public void loadNpcs() {
        startSkinQueueTask();
        preloadSkinCache();
        startLookTask();
        startRepairTask();
        removeSpawnedNpcs();
        SMPServerType currentType = currentServerType();
        plugin.getLogger().info("[SMP] Current server type=" + currentType);
        if (currentType == SMPServerType.DUNGEON && plugin instanceof Game game) game.getNetworkService().ensureDungeonLobbyLoaded();
        ConfigurationSection section = dataManager.getData().getConfigurationSection("npcs");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            if (!isRelevantNpc(id)) {
                plugin.getLogger().info("[SMP] Skipping " + npcServerType(id).name().toLowerCase(Locale.ROOT) + " NPC " + id
                        + " on " + currentType + " server");
                continue;
            }
            spawnNpc(id);
        }
    }

    public boolean createNpc(String id, String name, String skin, Location location) {
        if (id == null || id.isBlank() || dataManager.getData().contains("npcs." + id)) return false;
        saveNpc(id.toLowerCase(), name, skin, location);
        spawnNpc(id.toLowerCase());
        dataManager.save();
        return true;
    }

    public boolean deleteNpc(String id) {
        if (!dataManager.getData().contains("npcs." + id)) return false;
        removePacketNpc(id);
        for (Entity entity : getSpawnedNpcEntities(id)) entity.remove();
        dataManager.getData().set("npcs." + id, null);
        dataManager.save();
        return true;
    }

    public boolean teleportNpc(String id, Location location) {
        if (!dataManager.getData().contains("npcs." + id)) return false;
        saveNpc(id, getNpcName(id), getNpcSkin(id), location);
        removePacketNpc(id);
        for (Entity entity : getSpawnedNpcEntities(id)) entity.remove();
        spawnNpc(id);
        dataManager.save();
        return true;
    }

    public boolean setSkin(String id, String skin) {
        if (!dataManager.getData().contains("npcs." + id)) return false;
        dataManager.getData().set("npcs." + id + ".skin", skin);
        respawnNpc(id);
        dataManager.save();
        return true;
    }

    public boolean setName(String id, String name) {
        if (!dataManager.getData().contains("npcs." + id)) return false;
        dataManager.getData().set("npcs." + id + ".name", name);
        respawnNpc(id);
        dataManager.save();
        return true;
    }

    public boolean setAction(String id, String action) {
        if (!dataManager.getData().contains("npcs." + id)) return false;
        dataManager.getData().set("npcs." + id + ".action", action == null ? "" : action.trim());
        dataManager.save();
        return true;
    }

    public boolean setLookAtPlayer(String id, boolean look) {
        if (!dataManager.getData().contains("npcs." + id)) return false;
        dataManager.getData().set("npcs." + id + ".look", look);
        dataManager.save();
        return true;
    }

    public boolean setType(String id, String typeName) {
        if (!dataManager.getData().contains("npcs." + id)) return false;
        EntityType type = parseNpcType(typeName);
        if (type == null) return false;
        dataManager.getData().set("npcs." + id + ".type", type.name());
        removePacketNpc(id);
        for (Entity entity : getSpawnedNpcEntities(id)) entity.remove();
        spawnNpc(id);
        dataManager.save();
        return true;
    }

    public String getNearestNpcId(Location location, double radius) {
        if (location == null || location.getWorld() == null) return null;
        String nearest = null;
        double nearestDistance = radius * radius;
        for (String id : getNpcIds()) {
            Location npcLocation = getLocation(id);
            if (npcLocation == null || npcLocation.getWorld() == null || !npcLocation.getWorld().equals(location.getWorld())) continue;
            double distance = npcLocation.distanceSquared(location);
            if (distance <= nearestDistance) {
                nearest = id;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public List<String> getNpcIds() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("npcs");
        return section == null ? new ArrayList<>() : new ArrayList<>(section.getKeys(false));
    }

    public String getNpcId(Entity entity) {
        if (entity == null) return null;
        String pdc = entity.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
        if (pdc != null) return pdc;
        return spawnedEntityIds.get(entity.getUniqueId());
    }

    public String getNpcName(String id) {
        return dataManager.getData().getString("npcs." + id + ".name", id);
    }

    public String getNpcSkin(String id) {
        return dataManager.getData().getString("npcs." + id + ".skin", id);
    }

    public String getNpcAction(String id) {
        return dataManager.getData().getString("npcs." + id + ".action", "");
    }

    public String getEffectiveNpcAction(String id) {
        if (id == null || id.isBlank()) return "";
        String key = id.trim().toLowerCase(java.util.Locale.ROOT);
        String action = getNpcAction(key);
        if (!action.isBlank()) return action.trim();
        action = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getString("npcs.talking." + key + ".action", "");
        if (!action.isBlank()) return action.trim();
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getString("npcs.shops." + key + ".action", "").trim();
    }

    public EntityType getNpcType(String id) {
        return parseNpcType(dataManager.getData().getString("npcs." + id + ".type", "PLAYER"));
    }

    public boolean isLookingAtPlayer(String id) {
        return dataManager.getData().getBoolean("npcs." + id + ".look", false);
    }

    private void saveNpc(String id, String name, String skin, Location location) {
        dataManager.getData().set("npcs." + id + ".name", name);
        dataManager.getData().set("npcs." + id + ".skin", skin);
        if (!dataManager.getData().contains("npcs." + id + ".type")) dataManager.getData().set("npcs." + id + ".type", "PLAYER");
        if (!dataManager.getData().contains("npcs." + id + ".look")) dataManager.getData().set("npcs." + id + ".look", false);
        String world = Objects.requireNonNull(location.getWorld()).getName();
        dataManager.getData().set("npcs." + id + ".world", world.equalsIgnoreCase("lobby") ? "lobby" : world);
        SMPServerType serverType = currentServerType();
        dataManager.getData().set("npcs." + id + ".server_type", serverType.name());
        if (plugin instanceof Game game) dataManager.getData().set("npcs." + id + ".server_id", game.getNetworkService().getCurrentServerId());
        dataManager.getData().set("npcs." + id + ".x", location.getX());
        dataManager.getData().set("npcs." + id + ".y", location.getY());
        dataManager.getData().set("npcs." + id + ".z", location.getZ());
        dataManager.getData().set("npcs." + id + ".yaw", location.getYaw());
        dataManager.getData().set("npcs." + id + ".pitch", location.getPitch());
    }

    private void spawnNpc(String id) {
        if (!isRelevantNpc(id)) return;
        Location location = getLocation(id);
        if (location == null || location.getWorld() == null) {
            scheduleSpawnRetry(id, 1);
            return;
        }
        if (!packetNpcs.containsKey(id.toLowerCase(Locale.ROOT)) && getSpawnedNpcEntities(id).isEmpty()) {
            String type = npcServerType(id) == SMPServerType.DUNGEON ? "dungeon lobby" : npcServerType(id).name().toLowerCase(Locale.ROOT);
            plugin.getLogger().info("[SMP] Spawning " + type + " NPC " + id + " in world " + location.getWorld().getName());
        }
        keepNpcChunkLoaded(location);
        if (isPlayerNpc(id)) {
            spawnClickHitbox(id, location);
            spawnHologram(id, location);
            spawnPacketNpc(id, location);
            return;
        }
        spawnMobNpc(id, location);
    }

    private void spawnClickHitbox(String id, Location location) {
        Entity hitbox = location.getWorld().spawnEntity(location, EntityType.INTERACTION);
        hitbox.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, id.toLowerCase(Locale.ROOT));
        spawnedEntityIds.put(hitbox.getUniqueId(), id.toLowerCase(Locale.ROOT));
        hitbox.setInvulnerable(true);
        hitbox.setPersistent(true);
        if (hitbox instanceof Interaction interaction) {
            interaction.setInteractionWidth(0.9F);
            interaction.setInteractionHeight(1.9F);
            interaction.setResponsive(true);
        }
    }

    private void spawnHologram(String id, Location location) {
        spawnHologramLine(id, location.clone().add(0.0D, 2.25D, 0.0D), getNpcName(id) + "\n&e&lCLICK");
    }

    private void spawnHologramLine(String id, Location location, String text) {
        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        display.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, id.toLowerCase(Locale.ROOT));
        spawnedEntityIds.put(display.getUniqueId(), id.toLowerCase(Locale.ROOT));
        display.setText(CC.color(text));
        display.setBillboard(Display.Billboard.CENTER);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setSeeThrough(false);
        display.setShadowed(true);
        display.setInvulnerable(true);
        display.setPersistent(true);
    }

    private void spawnMobNpc(String id, Location location) {
        EntityType type = getNpcType(id);
        if (type == null || type == EntityType.PLAYER) return;
        Entity entity = location.getWorld().spawnEntity(location, type);
        entity.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, id.toLowerCase());
        spawnedEntityIds.put(entity.getUniqueId(), id.toLowerCase());
        entity.setCustomName(CC.color(getNpcName(id)));
        entity.setCustomNameVisible(false);
        entity.setInvulnerable(true);
        entity.setPersistent(true);
        entity.setSilent(true);
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setRemoveWhenFarAway(false);
            livingEntity.setCollidable(false);
        }
        if (entity instanceof Mob mob) {
            mob.setAI(false);
        }
        spawnHologram(id, location);
    }

    private void spawnPacketNpc(String id, Location location) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel level = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
        GameProfile profile = resolveProfile(id);
        ServerPlayer npc = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        npc.snapTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        npc.setYHeadRot(location.getYaw());
        npc.setYBodyRot(location.getYaw());
        npc.setCustomName(Component.literal(ChatColor.stripColor(CC.color(getNpcName(id)))));
        npc.setCustomNameVisible(true);
        npc.getEntityData().set(Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7F);

        PacketNpc packetNpc = new PacketNpc(id.toLowerCase(), npc);
        packetNpcs.put(id.toLowerCase(), packetNpc);
        for (org.bukkit.entity.Player viewer : Bukkit.getOnlinePlayers()) showPacketNpc(viewer, packetNpc);
    }

    private Location getLocation(String id) {
        String world = dataManager.getData().getString("npcs." + id + ".world");
        org.bukkit.World loadedWorld = findWorldIgnoreCase(world);
        if (loadedWorld == null) return null;
        return new Location(
                loadedWorld,
                dataManager.getData().getDouble("npcs." + id + ".x"),
                dataManager.getData().getDouble("npcs." + id + ".y"),
                dataManager.getData().getDouble("npcs." + id + ".z"),
                (float) dataManager.getData().getDouble("npcs." + id + ".yaw"),
                (float) dataManager.getData().getDouble("npcs." + id + ".pitch")
        );
    }

    private void removeSpawnedNpcs() {
        for (String id : new ArrayList<>(packetNpcs.keySet())) removePacketNpc(id);
        spawnedEntityIds.clear();
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (getNpcId(entity) != null) entity.remove();
            }
        }
    }

    public void showNpcs(org.bukkit.entity.Player player) {
        for (PacketNpc packetNpc : packetNpcs.values()) showPacketNpc(player, packetNpc);
    }

    public void forgetViewer(org.bukkit.entity.Player player) {
        if (player == null) return;
        UUID viewerId = player.getUniqueId();
        sentTeamsByPlayer.remove(viewerId);
        lastTeamSentByPlayer.remove(viewerId);
    }

    public void shutdown() {
        if (lookTask != null) {
            lookTask.cancel();
            lookTask = null;
        }
        if (repairTask != null) {
            repairTask.cancel();
            repairTask = null;
        }
        if (skinQueueTask != null) {
            skinQueueTask.cancel();
            skinQueueTask = null;
        }
        skinQueue.clear();
        queuedSkinNames.clear();
        pendingSkinNpcs.clear();
        removeSpawnedNpcs();
        sentTeamsByPlayer.clear();
        lastTeamSentByPlayer.clear();
        removeNpcChunkTickets();
    }

    private void respawnNpc(String id) {
        Location location = getLocation(id);
        if (location == null) return;
        removePacketNpc(id);
        for (Entity entity : getSpawnedNpcEntities(id)) entity.remove();
        spawnNpc(id);
    }

    private void removePacketNpc(String id) {
        PacketNpc packetNpc = packetNpcs.remove(id.toLowerCase());
        if (packetNpc == null) return;
        String teamName = npcTeamName(packetNpc.id());
        for (org.bukkit.entity.Player viewer : Bukkit.getOnlinePlayers()) {
            Set<String> sent = sentTeamsByPlayer.get(viewer.getUniqueId());
            if (sent != null && sent.remove(teamName)) {
                send(viewer, ClientboundSetPlayerTeamPacket.createRemovePacket(hiddenNameTagTeam(packetNpc)));
            }
            send(viewer, new ClientboundRemoveEntitiesPacket(packetNpc.player().getId()));
            send(viewer, new ClientboundPlayerInfoRemovePacket(List.of(packetNpc.player().getUUID())));
        }
    }

    private void showPacketNpc(org.bukkit.entity.Player viewer, PacketNpc packetNpc) {
        ServerPlayer npc = packetNpc.player();
        Component displayName = Component.literal(ChatColor.stripColor(CC.color(getNpcName(packetNpc.id()))));
        ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                npc.getUUID(),
                npc.getGameProfile(),
                false,
                0,
                GameType.SURVIVAL,
                displayName,
                true,
                0,
                null
        );
        sendNpcTeam(viewer, packetNpc);
        send(viewer, new ClientboundPlayerInfoUpdatePacket(EnumSet.of(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
        ), entry));
        send(viewer, new ClientboundAddEntityPacket(
                npc.getId(),
                npc.getUUID(),
                npc.getX(),
                npc.getY(),
                npc.getZ(),
                npc.getXRot(),
                npc.getYRot(),
                net.minecraft.world.entity.EntityType.PLAYER,
                0,
                Vec3.ZERO,
                npc.getYHeadRot()
        ));
        List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> data = npc.getEntityData().getNonDefaultValues();
        if (data != null && !data.isEmpty()) send(viewer, new ClientboundSetEntityDataPacket(npc.getId(), data));
        send(viewer, new ClientboundRotateHeadPacket(npc, angleToByte(npc.getYHeadRot())));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (viewer.isOnline()) send(viewer, new ClientboundPlayerInfoRemovePacket(List.of(npc.getUUID())));
        }, 40L);
    }

    private void sendNpcTeam(org.bukkit.entity.Player viewer, PacketNpc packetNpc) {
        if (viewer == null || !viewer.isOnline()) return;
        String teamName = npcTeamName(packetNpc.id());
        Set<String> sent = sentTeamsByPlayer.computeIfAbsent(viewer.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet());
        PlayerTeam team = hiddenNameTagTeam(packetNpc);
        try {
            if (sent.add(teamName)) {
                // A proxy can retain the previous backend's scoreboard state. Clear that unknown state once
                // before the first CREATE from this backend, then use UPDATE for every later refresh.
                send(viewer, ClientboundSetPlayerTeamPacket.createRemovePacket(team));
                send(viewer, ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
            } else {
                duplicateTeamCreatesPrevented.incrementAndGet();
                send(viewer, ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, false));
            }
            lastTeamSentByPlayer.put(viewer.getUniqueId(), teamName);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("[SMP Scoreboard] Duplicate team prevented: " + teamName);
        }
    }

    private void startLookTask() {
        if (lookTask != null) return;
        lookTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickNpcLook, 10L, 5L);
    }

    private void startRepairTask() {
        if (repairTask != null) return;
        repairTask = Bukkit.getScheduler().runTaskTimer(plugin, this::repairNpcs, 100L, 100L);
    }

    private void repairNpcs() {
        if (!plugin.isEnabled()) return;
        for (String id : getNpcIds()) {
            if (!isRelevantNpc(id)) continue;
            Location location = getLocation(id);
            if (location == null || location.getWorld() == null) continue;
            keepNpcChunkLoaded(location);
            int expectedEntities = 2;
            if (getSpawnedNpcEntities(id).size() < expectedEntities || (isPlayerNpc(id) && !packetNpcs.containsKey(id.toLowerCase()))) {
                respawnNpc(id);
                continue;
            }
            if (isPlayerNpc(id)) {
                PacketNpc packetNpc = packetNpcs.get(id.toLowerCase());
                if (packetNpc != null) {
                    for (org.bukkit.entity.Player viewer : Bukkit.getOnlinePlayers()) {
                        if (viewer.getWorld().equals(location.getWorld()) && viewer.getLocation().distanceSquared(location) <= 4096.0D) {
                            showPacketNpc(viewer, packetNpc);
                        }
                    }
                }
            }
        }
    }

    private void tickNpcLook() {
        if (!plugin.isEnabled()) return;
        for (String id : getNpcIds()) {
            if (!isRelevantNpc(id)) continue;
            if (!isLookingAtPlayer(id)) continue;
            Location location = getLocation(id);
            if (location == null || location.getWorld() == null) continue;
            org.bukkit.entity.Player target = nearestPlayer(location);
            if (target == null) continue;
            rotateNpcToward(id, location, target.getEyeLocation());
        }
    }

    private void keepNpcChunkLoaded(Location location) {
        if (location == null || location.getWorld() == null) return;
        location.getChunk().addPluginChunkTicket(plugin);
    }

    private void removeNpcChunkTickets() {
        for (String id : getNpcIds()) {
            if (!isRelevantNpc(id)) continue;
            Location location = getLocation(id);
            if (location != null && location.getWorld() != null) {
                location.getChunk().removePluginChunkTicket(plugin);
            }
        }
    }

    private org.bukkit.entity.Player nearestPlayer(Location location) {
        org.bukkit.entity.Player nearest = null;
        double nearestDistance = NpcManager.LOOK_RADIUS * NpcManager.LOOK_RADIUS;
        for (org.bukkit.entity.Player player : location.getWorld().getPlayers()) {
            if (!player.isOnline() || player.isDead()) continue;
            double distance = player.getLocation().distanceSquared(location);
            if (distance <= nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void rotateNpcToward(String id, Location location, Location target) {
        Location rotated = location.clone();
        rotated.setDirection(target.toVector().subtract(location.toVector()));
        if (isPlayerNpc(id)) {
            PacketNpc packetNpc = packetNpcs.get(id.toLowerCase());
            if (packetNpc == null) return;
            ServerPlayer npc = packetNpc.player();
            npc.setYRot(rotated.getYaw());
            npc.setXRot(rotated.getPitch());
            npc.setYHeadRot(rotated.getYaw());
            npc.setYBodyRot(rotated.getYaw());
            byte yaw = angleToByte(rotated.getYaw());
            byte pitch = angleToByte(rotated.getPitch());
            for (org.bukkit.entity.Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.getWorld().equals(location.getWorld())) {
                    send(viewer, new ClientboundMoveEntityPacket.Rot(npc.getId(), yaw, pitch, false));
                    send(viewer, new ClientboundRotateHeadPacket(npc, yaw));
                }
            }
            return;
        }
        for (Entity entity : getSpawnedNpcEntities(id)) {
            Location entityLocation = entity.getLocation();
            entityLocation.setYaw(rotated.getYaw());
            entityLocation.setPitch(rotated.getPitch());
            entity.teleport(entityLocation);
        }
    }

    private void send(org.bukkit.entity.Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    private byte angleToByte(float angle) {
        return (byte) ((int) (angle * 256.0F / 360.0F));
    }

    private GameProfile resolveProfile(String id) {
        String skin = getNpcSkin(id);
        String profileName = profileName(getNpcName(id));
        if (skin != null && !skin.isBlank()) {
            String key = skinCacheKey(skin);
            PropertyMap properties = skinCache.get(key);
            if (hasTextures(properties)) {
                CachedSkin cached = cachedSkins.get(key);
                if (cached == null || !cached.isFresh(cacheTtlMillis())) loadSkinAsync(id, skin);
                else touchCachedSkinAsync(key);
                return new GameProfile(npcUuid(id), profileName, properties);
            }
            loadSkinAsync(id, skin);
        }
        return new GameProfile(npcUuid(id), profileName);
    }

    private void loadSkinAsync(String id, String skin) {
        String key = skinCacheKey(skin);
        pendingSkinNpcs.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(id.toLowerCase(Locale.ROOT));
        CachedSkin cached = cachedSkins.get(key);
        if (cached != null && cached.isFresh(cacheTtlMillis())) {
            touchCachedSkinAsync(key);
            return;
        }
        if (skinCachePreloadComplete) enqueueSkinLookup(key, skin, 0, 0L);
    }

    private CachedSkin fetchSkin(String skin) throws Exception {
        JsonObject profile = requestJson("https://api.mojang.com/users/profiles/minecraft/" + URLEncoder.encode(skin, StandardCharsets.UTF_8));
        if (profile == null || !profile.has("id")) return null;
        String uuid = profile.get("id").getAsString();

        JsonObject session = requestJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
        if (session == null || !session.has("properties")) return null;

        JsonArray properties = session.getAsJsonArray("properties");
        for (JsonElement element : properties) {
            JsonObject property = element.getAsJsonObject();
            if (!"textures".equals(property.get("name").getAsString())) continue;
            String value = property.get("value").getAsString();
            String signature = property.has("signature") ? property.get("signature").getAsString() : "";
            long now = System.currentTimeMillis();
            return new CachedSkin(skinCacheKey(skin), uuid, value, signature, now, now);
        }
        return null;
    }

    private JsonObject requestJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(SKIN_REQUEST_TIMEOUT)
                .header("User-Agent", "Fakevival-SMP")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 204 || response.statusCode() == 404) return null;
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new SkinHttpException(response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private boolean hasTextures(PropertyMap properties) {
        return properties != null && properties.containsKey("textures");
    }

    private void startSkinQueueTask() {
        if (skinQueueTask != null) return;
        skinQueueTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processSkinQueue, 20L, 20L);
    }

    private void processSkinQueue() {
        if (!plugin.isEnabled() || !skinCachePreloadComplete || remoteSkinRequestActive.get()) return;
        long now = System.currentTimeMillis();
        if (now < skinBackoffUntil || now < nextRemoteSkinRequestAt) return;
        int limit = Math.max(1, runtimeInt("skinCache.maxRemoteSkinRequestsPerMinute", 20));
        synchronized (remoteSkinRequests) {
            while (!remoteSkinRequests.isEmpty() && now - remoteSkinRequests.peekFirst() >= 60_000L) remoteSkinRequests.removeFirst();
            if (remoteSkinRequests.size() >= limit) return;
        }
        SkinRequest request = skinQueue.peek();
        if (request == null || request.notBefore() > now) return;
        skinQueue.poll();
        if (!remoteSkinRequestActive.compareAndSet(false, true)) {
            skinQueue.offer(request);
            return;
        }
        synchronized (remoteSkinRequests) { remoteSkinRequests.addLast(now); }
        nextRemoteSkinRequestAt = now + Math.max(1L, runtimeLong("skinCache.delayBetweenSkinRequestsTicks", 60L)) * 50L;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runRemoteSkinRequest(request));
    }

    private void runRemoteSkinRequest(SkinRequest request) {
        try {
            CachedSkin fetched = fetchSkin(request.skinName());
            if (fetched == null) {
                finishSkinFailure(request, "profile not found", false);
                return;
            }
            boolean newlyCached = !cachedSkins.containsKey(request.key());
            cachedSkins.put(request.key(), fetched);
            skinCache.put(request.key(), fetched.properties());
            persistCachedSkin(fetched);
            if (newlyCached) dbCachedSkinCount++;
            consecutiveSkin429s = 0;
            queuedSkinNames.remove(request.key());
            applySkinToPendingNpcs(request.key());
        } catch (SkinHttpException exception) {
            if (exception.statusCode() == 429) {
                lastSkin429At = System.currentTimeMillis();
                consecutiveSkin429s++;
                long seconds = backoffSeconds(consecutiveSkin429s);
                skinBackoffUntil = lastSkin429At + seconds * 1000L;
                skinQueue.offer(new SkinRequest(request.key(), request.skinName(), request.attempts(), skinBackoffUntil));
                plugin.getLogger().warning("[SMP NPC Skins] Skin API rate limited. Pausing remote lookups for " + seconds + "s.");
            } else {
                finishSkinFailure(request, "HTTP " + exception.statusCode(), exception.statusCode() >= 500);
            }
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            boolean transientFailure = message.toLowerCase(Locale.ROOT).contains("eof")
                    || message.toLowerCase(Locale.ROOT).contains("timeout")
                    || message.toLowerCase(Locale.ROOT).contains("closed");
            finishSkinFailure(request, message, transientFailure);
        } finally {
            remoteSkinRequestActive.set(false);
        }
    }

    private void finishSkinFailure(SkinRequest request, String reason, boolean retryable) {
        if (retryable && request.attempts() < 3) {
            long retryAt = System.currentTimeMillis() + Math.min(300_000L, 30_000L * (1L << request.attempts()));
            skinQueue.offer(new SkinRequest(request.key(), request.skinName(), request.attempts() + 1, retryAt));
            return;
        }
        queuedSkinNames.remove(request.key());
        if (failedSkinWarnings.add(request.key()))
            plugin.getLogger().warning("[SMP NPC Skins] Could not refresh unique skin '" + request.skinName() + "' after final retry: " + reason + ". Keeping fallback skin.");
    }

    private void enqueueSkinLookup(String key, String skin, int attempts, long notBefore) {
        if (!queuedSkinNames.add(key)) return;
        skinQueue.offer(new SkinRequest(key, skin, attempts, notBefore));
    }

    private void preloadSkinCache() {
        if (!dataManager.isMySqlEnabled()) {
            skinCachePreloadComplete = true;
            queueMissingNpcSkins();
            return;
        }
        skinCachePreloadComplete = false;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int loaded = 0;
            try (Connection connection = dataManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT `skin_name`,`uuid`,`texture_value`,`texture_signature`,`fetched_at`,`last_used_at` FROM `smp_npc_skin_cache`");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    CachedSkin cached = new CachedSkin(result.getString(1), result.getString(2), result.getString(3), result.getString(4), result.getLong(5), result.getLong(6));
                    if (cached.value() == null || cached.value().isBlank()) continue;
                    String key = skinCacheKey(cached.skinName());
                    cachedSkins.put(key, cached);
                    skinCache.put(key, cached.properties());
                    loaded++;
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("[SMP NPC Skins] Could not preload MariaDB skin cache; using fallbacks and the rate-limited queue: " + exception.getMessage());
            }
            int count = loaded;
            dbCachedSkinCount = loaded;
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!plugin.isEnabled()) return;
                skinCachePreloadComplete = true;
                plugin.getLogger().info("[SMP NPC Skins] Loaded " + count + " skins from DB cache.");
                queueMissingNpcSkins();
            });
        });
    }

    private void queueMissingNpcSkins() {
        int before = skinQueue.size();
        int fallbackNpcs = 0;
        long ttl = cacheTtlMillis();
        for (String id : getNpcIds()) {
            if (!isRelevantNpc(id) || !isPlayerNpc(id)) continue;
            String skin = getNpcSkin(id);
            if (skin == null || skin.isBlank()) continue;
            String key = skinCacheKey(skin);
            pendingSkinNpcs.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(id.toLowerCase(Locale.ROOT));
            CachedSkin cached = cachedSkins.get(key);
            if (cached == null) fallbackNpcs++;
            if (cached == null || !cached.isFresh(ttl)) enqueueSkinLookup(key, skin, 0, 0L);
            else applySkinToPendingNpcs(key);
        }
        int queued = Math.max(0, skinQueue.size() - before);
        plugin.getLogger().info("[SMP NPC Skins] Queued " + queued + " missing skins for rate-limited remote lookup.");
        plugin.getLogger().info("[SMP NPC Skins] Using fallback skins for " + fallbackNpcs + " NPCs until lookup completes.");
    }

    private void applySkinToPendingNpcs(String key) {
        Set<String> ids = pendingSkinNpcs.remove(key);
        if (ids == null || ids.isEmpty()) return;
        if (!plugin.isEnabled()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!plugin.isEnabled()) return;
            for (String id : ids) {
                if (!dataManager.getData().contains("npcs." + id)) continue;
                if (key.equals(skinCacheKey(getNpcSkin(id)))) respawnNpc(id);
            }
        });
    }

    private void persistCachedSkin(CachedSkin skin) {
        if (!dataManager.isMySqlEnabled()) return;
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `smp_npc_skin_cache` (`skin_name`,`uuid`,`texture_value`,`texture_signature`,`fetched_at`,`last_used_at`) VALUES (?,?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE `uuid`=VALUES(`uuid`),`texture_value`=VALUES(`texture_value`),`texture_signature`=VALUES(`texture_signature`),`fetched_at`=VALUES(`fetched_at`),`last_used_at`=VALUES(`last_used_at`)")) {
            statement.setString(1, skin.skinName()); statement.setString(2, skin.uuid()); statement.setString(3, skin.value());
            statement.setString(4, skin.signature()); statement.setLong(5, skin.fetchedAt()); statement.setLong(6, skin.lastUsedAt());
            statement.executeUpdate();
        } catch (SQLException exception) {
            warnSkinDatabaseOnce("Could not persist cached skins: " + exception.getMessage());
        }
    }

    private void touchCachedSkinAsync(String key) {
        CachedSkin cached = cachedSkins.get(key);
        if (cached == null || !dataManager.isMySqlEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - cached.lastUsedAt() < 3_600_000L) return;
        cachedSkins.put(key, cached.withLastUsed(now));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "UPDATE `smp_npc_skin_cache` SET `last_used_at`=? WHERE `skin_name`=?")) {
                statement.setLong(1, now); statement.setString(2, key); statement.executeUpdate();
            } catch (SQLException ignored) { }
        });
    }

    public SkinCacheStatus skinCacheStatus() {
        long now = System.currentTimeMillis();
        int recent;
        synchronized (remoteSkinRequests) {
            while (!remoteSkinRequests.isEmpty() && now - remoteSkinRequests.peekFirst() >= 60_000L) remoteSkinRequests.removeFirst();
            recent = remoteSkinRequests.size();
        }
        return new SkinCacheStatus(skinCache.size(), dbCachedSkinCount, skinQueue.size(), recent,
                Math.max(1, runtimeInt("skinCache.maxRemoteSkinRequestsPerMinute", 20)), now < skinBackoffUntil,
                Math.max(0L, skinBackoffUntil - now), lastSkin429At);
    }

    public String skinCacheInfo(String skinName) {
        if (skinName == null || skinName.isBlank()) return "Invalid skin name";
        CachedSkin cached = cachedSkins.get(skinCacheKey(skinName));
        if (cached == null) return "MISS (not present in memory/DB preload)";
        long age = Math.max(0L, System.currentTimeMillis() - cached.fetchedAt());
        return (cached.isFresh(cacheTtlMillis()) ? "VALID" : "EXPIRED") + " uuid=" + cached.uuid() + " age=" + formatDuration(age);
    }

    public void clearSkinCache(String skinName) {
        String key = skinCacheKey(skinName);
        skinCache.remove(key);
        if (cachedSkins.remove(key) != null) dbCachedSkinCount = Math.max(0, dbCachedSkinCount - 1);
        queuedSkinNames.remove(key); failedSkinWarnings.remove(key);
        skinQueue.removeIf(request -> request.key().equals(key));
        if (dataManager.isMySqlEnabled()) Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM `smp_npc_skin_cache` WHERE `skin_name`=?")) {
                statement.setString(1, key); statement.executeUpdate();
            } catch (SQLException exception) {
                warnSkinDatabaseOnce("Could not clear cached skin '" + key + "': " + exception.getMessage());
            }
        });
    }

    public void reloadSkins() { preloadSkinCache(); }

    public SkinValidation validateSkinLoading() {
        return new SkinValidation(skinCachePreloadComplete, true,
                queuedSkinNames.size() <= skinQueue.size() + (remoteSkinRequestActive.get() ? 1 : 0), true, true);
    }

    public TeamViewerDebug teamDebug(UUID viewerId) {
        Set<String> sent = viewerId == null ? Set.of() : sentTeamsByPlayer.getOrDefault(viewerId, Set.of());
        return new TeamViewerDebug(sent.size(), 0, lastTeamSentByPlayer.getOrDefault(viewerId, "none"),
                duplicateTeamCreatesPrevented.get());
    }

    public List<NpcTeamDebug> npcTeamDebug() {
        List<NpcTeamDebug> result = new ArrayList<>();
        for (String id : getNpcIds()) {
            String normalized = id.toLowerCase(Locale.ROOT);
            String teamName = npcTeamName(normalized);
            int viewers = (int) sentTeamsByPlayer.values().stream().filter(teams -> teams.contains(teamName)).count();
            result.add(new NpcTeamDebug(normalized, teamName, packetNpcs.containsKey(normalized), viewers));
        }
        return List.copyOf(result);
    }

    public TeamValidation validateNpcTeams() {
        Map<String, Integer> counts = new HashMap<>();
        int tooLong = 0;
        for (String id : getNpcIds()) {
            String team = npcTeamName(id);
            counts.merge(team, 1, Integer::sum);
            if (team.length() > 16) tooLong++;
        }
        int collisions = (int) counts.values().stream().filter(count -> count > 1).count();
        boolean stable = getNpcIds().stream().allMatch(id -> npcTeamName(id).equals(npcTeamName(id)));
        boolean namespaceIsolated = counts.keySet().stream().allMatch(name -> name.startsWith("fvnpc"));
        return new TeamValidation(counts.size(), collisions, tooLong, stable, true, true, namespaceIsolated);
    }

    private long cacheTtlMillis() { return Math.max(1L, runtimeLong("skinCache.ttlDays", 30L)) * 86_400_000L; }
    private int runtimeInt(String path, int fallback) { return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path, fallback); }
    private long runtimeLong(String path, long fallback) { return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong(path, fallback); }
    private long backoffSeconds(int strikes) {
        long first = Math.max(1L, runtimeLong("skinCache.first429BackoffSeconds", 60L));
        long second = Math.max(first, runtimeLong("skinCache.second429BackoffSeconds", 300L));
        long max = Math.max(second, runtimeLong("skinCache.max429BackoffSeconds", 1800L));
        if (strikes <= 1) return first;
        if (strikes == 2) return second;
        return Math.min(max, second * (1L << Math.min(10, strikes - 2)));
    }

    private String formatDuration(long millis) {
        if (millis <= 0L) return "0s";
        long seconds = millis / 1000L;
        if (seconds < 60L) return seconds + "s";
        return (seconds / 60L) + "m " + (seconds % 60L) + "s";
    }

    private void warnSkinDatabaseOnce(String message) {
        long now = System.currentTimeMillis();
        if (now < nextSkinDatabaseWarningAt) return;
        nextSkinDatabaseWarningAt = now + 60_000L;
        plugin.getLogger().warning("[SMP NPC Skins] " + message);
    }

    public record SkinCacheStatus(int memoryCached, int dbCached, int queued, int requestsThisMinute, int requestLimit,
                                  boolean backoffActive, long backoffRemainingMillis, long last429At) { }
    public record SkinValidation(boolean cachePreloaded, boolean asyncRemoteLookups, boolean duplicateRequestsDeduplicated,
                                 boolean fallbacksSupported, boolean transientFailuresSafe) { }
    public record TeamViewerDebug(int npcTeams, int pendingPackets, String lastTeamSent,
                                  int duplicateCreatesPrevented) { }
    public record NpcTeamDebug(String npcId, String teamName, boolean registered, int viewers) { }
    public record TeamValidation(int uniqueTeamNames, int collisions, int namesOver16, boolean stableNames,
                                 boolean createTrackedPerViewer, boolean initialProxyReset,
                                 boolean tablistNamespaceIsolated) { }
    private record SkinRequest(String key, String skinName, int attempts, long notBefore) { }
    private record CachedSkin(String skinName, String uuid, String value, String signature, long fetchedAt, long lastUsedAt) {
        private PropertyMap properties() {
            Property texture = signature == null || signature.isBlank() ? new Property("textures", value) : new Property("textures", value, signature);
            return new PropertyMap(ImmutableMultimap.of("textures", texture));
        }
        private boolean isFresh(long ttl) { return fetchedAt > 0L && System.currentTimeMillis() - fetchedAt <= ttl; }
        private CachedSkin withLastUsed(long timestamp) { return new CachedSkin(skinName, uuid, value, signature, fetchedAt, timestamp); }
    }
    private static final class SkinHttpException extends Exception {
        private final int statusCode;
        private SkinHttpException(int statusCode) { super("HTTP " + statusCode); this.statusCode = statusCode; }
        private int statusCode() { return statusCode; }
    }

    private PlayerTeam hiddenNameTagTeam(PacketNpc packetNpc) {
        Scoreboard scoreboard = new Scoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam(npcTeamName(packetNpc.id()));
        team.setNameTagVisibility(Team.Visibility.NEVER);
        team.setCollisionRule(Team.CollisionRule.NEVER);
        scoreboard.addPlayerToTeam(packetNpc.player().getGameProfile().name(), team);
        return team;
    }

    private String npcTeamName(String id) {
        String normalized = id == null ? "npc" : id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        String teamName = "fvnpc" + Integer.toUnsignedString(normalized.hashCode(), 36);
        return teamName.length() > 16 ? teamName.substring(0, 16) : teamName;
    }

    private String skinCacheKey(String skin) {
        return skin.toLowerCase(Locale.ROOT);
    }

    private UUID npcUuid(String id) {
        return UUID.nameUUIDFromBytes(("fakevival:npc:" + id.toLowerCase()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String profileName(String value) {
        String clean = ChatColor.stripColor(CC.color(value == null ? "NPC" : value)).replaceAll("[^A-Za-z0-9_]", "");
        if (clean.isBlank()) clean = "NPC";
        return clean.length() > 16 ? clean.substring(0, 16) : clean;
    }

    private boolean isPlayerNpc(String id) {
        return getNpcType(id) == EntityType.PLAYER;
    }

    private void scheduleSpawnRetry(String id, int attempt) {
        String key = id.toLowerCase(Locale.ROOT);
        if (!pendingSpawnRetries.add(key)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingSpawnRetries.remove(key);
            if (!plugin.isEnabled() || !dataManager.getData().contains("npcs." + id) || !isRelevantNpc(id)) return;
            Location location = getLocation(id);
            if (location != null && location.getWorld() != null) {
                if (!packetNpcs.containsKey(key) && getSpawnedNpcEntities(id).isEmpty()) spawnNpc(id);
            } else if (attempt < 10) {
                scheduleSpawnRetry(id, attempt + 1);
            }
        }, 20L);
    }

    private org.bukkit.World findWorldIgnoreCase(String name) {
        if (name == null || name.isBlank()) return null;
        if (name.equalsIgnoreCase("lobby")) {
            org.bukkit.World lobby = Bukkit.getWorld("lobby");
            if (lobby != null) return lobby;
        }
        org.bukkit.World exact = Bukkit.getWorld(name);
        if (exact != null) return exact;
        for (org.bukkit.World world : Bukkit.getWorlds()) if (world.getName().equalsIgnoreCase(name)) return world;
        return null;
    }

    private boolean isRelevantNpc(String id) {
        SMPServerType current = currentServerType();
        SMPServerType stored = npcServerType(id);
        String world = dataManager.getData().getString("npcs." + id + ".world", "");
        return switch (current) {
            case HUB -> stored == SMPServerType.HUB || world.equalsIgnoreCase("hub");
            case DUNGEON -> stored == SMPServerType.DUNGEON || world.equalsIgnoreCase("lobby");
            case OVERWORLD -> stored == SMPServerType.OVERWORLD;
            case UNKNOWN -> findWorldIgnoreCase(world) != null;
        };
    }

    private SMPServerType npcServerType(String id) {
        String path = "npcs." + id;
        String configured = dataManager.getData().getString(path + ".server_type", "");
        SMPServerType parsed = SMPServerType.parse(configured);
        if (parsed != SMPServerType.UNKNOWN) return parsed;
        String world = dataManager.getData().getString(path + ".world", "");
        if (world.equalsIgnoreCase("hub")) return SMPServerType.HUB;
        if (world.equalsIgnoreCase("lobby")) return SMPServerType.DUNGEON;
        if (world.equalsIgnoreCase("world") || world.equalsIgnoreCase("world_nether") || world.equalsIgnoreCase("world_the_end")) return SMPServerType.OVERWORLD;
        return SMPServerType.UNKNOWN;
    }

    private SMPServerType currentServerType() {
        return plugin instanceof Game game ? game.getNetworkService().getCurrentServerType() : SMPServerType.UNKNOWN;
    }

    private EntityType parseNpcType(String typeName) {
        if (typeName == null || typeName.isBlank()) return EntityType.PLAYER;
        try {
            EntityType type = EntityType.valueOf(typeName.toUpperCase(Locale.ROOT));
            if (type == EntityType.PLAYER) return EntityType.PLAYER;
            return type.isAlive() && type.isSpawnable() ? type : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<Entity> getSpawnedNpcEntities(String id) {
        List<Entity> entities = new ArrayList<>();
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (id.equalsIgnoreCase(getNpcId(entity))) entities.add(entity);
            }
        }
        return entities;
    }

    private record PacketNpc(String id, ServerPlayer player) {
    }
}
