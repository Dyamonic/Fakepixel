package net.fakepixel.housing.house;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class House {
    private double originX = 0.0D;
    private double originY = 103.0D;
    private double originZ = 0.0D;
    private final UUID owner;
    private String ownerName;
    private String name;
    private final String template;
    private final String worldName;
    private final int plotId;
    private long createdAt;
    private long lastOpened;
    private boolean publicHouse;
    private String visitingRule = "public";
    private final Set<UUID> trusted = new HashSet<>();
    private Location spawn;
    private Location guestSpawn;
    private int cookies;
    private final Set<UUID> cookieGivers = new HashSet<>();
    private final Set<UUID> banned = new HashSet<>();
    private final Set<UUID> muted = new HashSet<>();
    private final Set<UUID> hiddenBorderViewers = new HashSet<>();
    private final Map<UUID, String> groups = new HashMap<>();
    private final Set<String> customGroups = new LinkedHashSet<>();
    private final Map<String, Set<String>> groupPermissions = new HashMap<>();
    private final Map<String, String> groupColors = new HashMap<>();
    private String weather = "sunny";
    private long timeTicks = -1L;
    private String biome = "plains";
    private String sky = "overworld";
    private String theme = "default";
    private final Set<String> tags = new LinkedHashSet<>();
    private boolean plotBorderSaved = false;
    private int plotBorderMinX;
    private int plotBorderMaxX;
    private int plotBorderMinZ;
    private int plotBorderMaxZ;
    private int plotBorderY;
    private final Map<String, String> groupDisplayNames = new HashMap<>();
    private final Map<String, String> groupTags = new HashMap<>();
    private final Map<String, String> parkourPads = new LinkedHashMap<>();
    private final Map<String, String> parkourPadHolograms = new HashMap<>();
    private final Map<UUID, Long> parkourBestTimes = new HashMap<>();
    private final Map<UUID, String> parkourBestNames = new HashMap<>();
    private String parkourLeaderboardHologramId = "";
    private final Map<String, TeleportPad> teleportPads = new LinkedHashMap<>();
    private final Map<String, Hologram> holograms = new LinkedHashMap<>();
    private final Map<String, HouseNpc> houseNpcs = new LinkedHashMap<>();
    private final Map<String, ActionButton> actionButtons = new LinkedHashMap<>();
    private final Map<String, ActionPad> actionPads = new LinkedHashMap<>();
    private final Map<String, Region> regions = new LinkedHashMap<>();
    private final Map<String, CustomCommand> customCommands = new LinkedHashMap<>();
    private final Map<String, Function> functions = new LinkedHashMap<>();
    private final Set<String> playerEditedBlocks = new HashSet<>();
    private boolean flyEnabled = true;
    private int plotSize = 31;
    private boolean pvpEnabled = false;
    private boolean fallDamageEnabled = false;
    private boolean fireDamageEnabled = false;
    private boolean hungerEnabled = false;
    private boolean instantRespawnEnabled = false;
    private boolean killDeathMessagesEnabled = true;
    private final Map<Integer, ItemStack> pvpLayout = new HashMap<>();
    private boolean joinMessageEnabled = true;
    private String joinMessage = "&aWelcome to &e%owner%'s House&a!";
    private boolean leaveMessageEnabled = false;
    private String leaveMessage = "&7You left &e%owner%'s House&7.";
    private boolean announceJoinsEnabled = true;
    private boolean announceLeavesEnabled = true;
    private boolean carpenterRemoved = false;
    private String mode = "SOCIAL";
    private boolean jukeboxEnabled = false;
    private String jukeboxSong = "";
    private boolean jukeboxShuffle = false;
    private int jukeboxVolume = 100;
    private boolean scoreboardEnabled = true;
    private String scoreboardTitle = "&b&lHOUSING";
    private int scoreboardUpdateTicks = 40;
    private boolean scoreboardPlaceholdersEnabled = true;
    private final java.util.List<String> scoreboardLines = new java.util.ArrayList<>();

    public House(UUID owner, String ownerName, String template, String worldName) {
        this(owner, ownerName, template, worldName, 1);
    }

    public House(UUID owner, String ownerName, String template, String worldName, int plotId) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.template = template;
        this.worldName = worldName;
        this.plotId = Math.max(1, plotId);
        this.name = ownerName + "'s House";
        this.createdAt = System.currentTimeMillis();
        this.lastOpened = System.currentTimeMillis();
        this.publicHouse = true;
        resetScoreboardLines();
    }

    public static House fromConfig(String key, ConfigurationSection section) {
        UUID owner = ownerFromStorageKey(key, section);
        int plotId = section.getInt("plot-id", plotIdFromStorageKey(key));
        String defaultWorld = "house_" + owner.toString().replace("-", "") + (plotId <= 1 ? "" : "_" + plotId);
        House house = new House(owner, section.getString("owner-name", "Unknown"), section.getString("template", "basic"), section.getString("world", defaultWorld), plotId);
        house.originX = section.getDouble("house-origin.x", 0.0D);
        house.originY = section.getDouble("house-origin.y", 103.0D);
        house.originZ = section.getDouble("house-origin.z", 0.0D);
        house.name = section.getString("name", house.name);
        house.createdAt = section.getLong("created-at", System.currentTimeMillis());
        house.lastOpened = section.getLong("last-opened", 0L);
        house.publicHouse = section.getBoolean("public", true);
        house.visitingRule = normalizeVisitingRule(section.getString("visiting-rule", house.publicHouse ? "public" : "private"));
        house.publicHouse = "public".equals(house.visitingRule);
        house.weather = normalizeWeather(section.getString("weather", "sunny"));
        house.timeTicks = section.getLong("time", -1L);
        house.biome = normalizeBiome(section.getString("biome", "plains"));
        house.sky = normalizeSky(section.getString("sky", "overworld"));
        house.theme = normalizeTheme(section.getString("theme", "default"));
        ConfigurationSection borderSection = section.getConfigurationSection("plot-border");
        if (borderSection != null) {
            house.plotBorderSaved = true;
            house.plotBorderMinX = borderSection.getInt("min-x");
            house.plotBorderMaxX = borderSection.getInt("max-x");
            house.plotBorderMinZ = borderSection.getInt("min-z");
            house.plotBorderMaxZ = borderSection.getInt("max-z");
            house.plotBorderY = borderSection.getInt("y");
        }
        house.spawn = parseFullLocation(section.getString("spawn", ""), house.worldName);
        house.guestSpawn = parseFullLocation(section.getString("guest-spawn", ""), house.worldName);
        house.tags.addAll(section.getStringList("tags"));
        house.flyEnabled = section.getBoolean("fly-enabled", true);
        house.plotSize = normalizePlotSize(section.getInt("plot-size", 31));
        house.pvpEnabled = section.getBoolean("pvp.enabled", false);
        house.fallDamageEnabled = section.getBoolean("pvp.fall-damage", false);
        house.fireDamageEnabled = section.getBoolean("pvp.fire-damage", false);
        house.hungerEnabled = section.getBoolean("pvp.hunger", false);
        house.instantRespawnEnabled = section.getBoolean("pvp.instant-respawn", false);
        house.killDeathMessagesEnabled = section.getBoolean("pvp.kill-death-messages", true);
        house.joinMessageEnabled = section.getBoolean("visiting-messages.join-message-enabled", true);
        house.joinMessage = section.getString("visiting-messages.join-message", house.joinMessage);
        house.leaveMessageEnabled = section.getBoolean("visiting-messages.leave-message-enabled", false);
        house.leaveMessage = section.getString("visiting-messages.leave-message", house.leaveMessage);
        house.announceJoinsEnabled = section.getBoolean("visiting-messages.announce-joins", true);
        house.announceLeavesEnabled = section.getBoolean("visiting-messages.announce-leaves", true);
        house.carpenterRemoved = section.getBoolean("carpenter.removed", false);
        house.mode = normalizeMode(section.getString("mode", house.mode));
        house.jukeboxEnabled = section.getBoolean("jukebox.enabled", false);
        house.jukeboxSong = cleanJukeboxSong(section.getString("jukebox.song", ""));
        house.jukeboxShuffle = section.getBoolean("jukebox.shuffle", false);
        house.jukeboxVolume = normalizeJukeboxVolume(section.getInt("jukebox.volume", 100));
        house.scoreboardEnabled = section.getBoolean("scoreboard.enabled", true);
        ConfigurationSection scoreboardSection = section.getConfigurationSection("scoreboard");
        if (scoreboardSection != null) {
            house.scoreboardEnabled = scoreboardSection.getBoolean("enabled", true);
            house.scoreboardTitle = scoreboardSection.getString("title", "&b&lHOUSING");
            house.scoreboardUpdateTicks = normalizeScoreboardUpdateTicks(scoreboardSection.getInt("update-ticks", 40));
            house.scoreboardPlaceholdersEnabled = scoreboardSection.getBoolean("placeholders-enabled", true);
            house.scoreboardLines.clear();
            house.scoreboardLines.addAll(scoreboardSection.getStringList("lines"));
        }
        if (house.scoreboardLines.isEmpty()) house.resetScoreboardLines();
        ConfigurationSection pvpLayoutSection = section.getConfigurationSection("pvp-layout");
        if (pvpLayoutSection != null) {
            for (String keySlot : pvpLayoutSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(keySlot);
                    Object value = pvpLayoutSection.get(keySlot);
                    if (slot >= 0 && slot < 36 && value instanceof ItemStack) {
                        house.pvpLayout.put(slot, ((ItemStack) value).clone());
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        house.cookies = section.getInt("cookies", 0);
        for (String uuid : section.getStringList("cookie-givers")) {
            try { house.cookieGivers.add(UUID.fromString(uuid)); } catch (IllegalArgumentException ignored) {}
        }
        for (String uuid : section.getStringList("trusted")) {
            try { house.trusted.add(UUID.fromString(uuid)); } catch (IllegalArgumentException ignored) {}
        }
        for (String uuid : section.getStringList("banned")) {
            try { house.banned.add(UUID.fromString(uuid)); } catch (IllegalArgumentException ignored) {}
        }
        for (String uuid : section.getStringList("muted")) {
            try { house.muted.add(UUID.fromString(uuid)); } catch (IllegalArgumentException ignored) {}
        }
        for (String uuid : section.getStringList("hidden-border-viewers")) {
            try { house.hiddenBorderViewers.add(UUID.fromString(uuid)); } catch (IllegalArgumentException ignored) {}
        }
        ConfigurationSection groups = section.getConfigurationSection("groups");
        if (groups != null) {
            for (String uuid : groups.getKeys(false)) {
                try { house.groups.put(UUID.fromString(uuid), groups.getString(uuid, "guest")); } catch (IllegalArgumentException ignored) {}
            }
        }
        for (String group : section.getStringList("custom-groups")) {
            String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
            if (!clean.equals(net.fakepixel.housing.group.HouseGroups.OWNER)
                    && !clean.equals(net.fakepixel.housing.group.HouseGroups.CO_OWNER)
                    && !clean.equals(net.fakepixel.housing.group.HouseGroups.RESIDENT)
                    && !clean.equals(net.fakepixel.housing.group.HouseGroups.GUEST)) {
                house.customGroups.add(clean);
            }
        }
        ConfigurationSection permissionSection = section.getConfigurationSection("group-permissions");
        if (permissionSection != null) {
            for (String group : permissionSection.getKeys(false)) {
                String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
                house.groupPermissions.put(clean, new HashSet<>(permissionSection.getStringList(group)));
            }
        }
        ConfigurationSection colorSection = section.getConfigurationSection("group-colors");
        if (colorSection != null) {
            for (String group : colorSection.getKeys(false)) {
                String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
                house.groupColors.put(clean, colorSection.getString(group, net.fakepixel.housing.group.HouseGroups.defaultColor(clean)));
            }
        }
        ConfigurationSection nameSection = section.getConfigurationSection("group-names");
        if (nameSection != null) {
            for (String group : nameSection.getKeys(false)) {
                String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
                house.groupDisplayNames.put(clean, nameSection.getString(group, net.fakepixel.housing.group.HouseGroups.displayName(clean)));
            }
        }
        ConfigurationSection tagSection = section.getConfigurationSection("group-tags");
        if (tagSection != null) {
            for (String group : tagSection.getKeys(false)) {
                String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
                house.groupTags.put(clean, tagSection.getString(group, ""));
            }
        }
        ConfigurationSection parkourSection = section.getConfigurationSection("parkour-pads");
        if (parkourSection != null) {
            for (String type : parkourSection.getKeys(false)) {
                String location = parkourSection.getString(type, "");
                if (location != null && !location.trim().isEmpty()) house.parkourPads.put(type, location);
            }
        }
        ConfigurationSection parkourHologramSection = section.getConfigurationSection("parkour-pad-holograms");
        if (parkourHologramSection != null) {
            for (String padKey : parkourHologramSection.getKeys(false)) {
                String hologramId = parkourHologramSection.getString(padKey, "");
                if (hologramId != null && !hologramId.trim().isEmpty()) house.parkourPadHolograms.put(padKey, hologramId);
            }
        }
        house.parkourLeaderboardHologramId = section.getString("parkour-leaderboard-hologram", "");
        ConfigurationSection bestSection = section.getConfigurationSection("parkour-best-times");
        if (bestSection != null) {
            for (String uuid : bestSection.getKeys(false)) {
                try {
                    UUID playerUuid = UUID.fromString(uuid);
                    long time;
                    String lastKnownName = "";
                    ConfigurationSection entrySection = bestSection.getConfigurationSection(uuid);
                    if (entrySection != null) {
                        time = entrySection.getLong("time", entrySection.getLong("millis", 0L));
                        lastKnownName = entrySection.getString("last-known-name", entrySection.getString("name", ""));
                    } else {
                        time = bestSection.getLong(uuid, 0L);
                    }
                    if (time > 0L) {
                        house.parkourBestTimes.put(playerUuid, time);
                        if (lastKnownName != null && !lastKnownName.trim().isEmpty()) {
                            house.parkourBestNames.put(playerUuid, lastKnownName.trim());
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        ConfigurationSection teleportSection = section.getConfigurationSection("teleport-pads");
        if (teleportSection != null) {
            for (String id : teleportSection.getKeys(false)) {
                ConfigurationSection padSection = teleportSection.getConfigurationSection(id);
                if (padSection == null) continue;
                String location = padSection.getString("location", "");
                if (location == null || location.trim().isEmpty()) continue;
                TeleportPad pad = new TeleportPad(id,
                        padSection.getString("name", "Teleport Pad"),
                        location,
                        padSection.getString("linked", ""),
                        padSection.getBoolean("particles", true),
                        padSection.getBoolean("sound", true));
                house.teleportPads.put(id, pad);
            }
            house.normalizeTeleportPadLinks();
        }
        ConfigurationSection hologramSection = section.getConfigurationSection("holograms");
        if (hologramSection != null) {
            for (String id : hologramSection.getKeys(false)) {
                ConfigurationSection holoSection = hologramSection.getConfigurationSection(id);
                if (holoSection == null) continue;
                String location = holoSection.getString("location", "");
                if (location == null || location.trim().isEmpty()) continue;
                java.util.List<String> lines = holoSection.getStringList("lines");
                if (lines.isEmpty()) lines.add("&eHologram");
                Hologram hologram = new Hologram(id, location, lines, holoSection.getBoolean("visible", true), holoSection.getStringList("entities"),
                        holoSection.getString("type", "CUSTOM"), holoSection.getString("linked-object", ""));
                house.holograms.put(id, hologram);
            }
        }
        ConfigurationSection npcSection = section.getConfigurationSection("house-npcs");
        if (npcSection != null) {
            for (String id : npcSection.getKeys(false)) {
                ConfigurationSection child = npcSection.getConfigurationSection(id);
                if (child == null) continue;
                String location = child.getString("location", "");
                if (location == null || location.trim().isEmpty()) continue;
                HouseNpc npc = new HouseNpc(id,
                        child.getString("name", "&aNPC"),
                        child.getString("skin", ""),
                        normalizeNpcAction(child.getString("action", "NONE")),
                        child.getStringList("messages"),
                        location,
                        child.getString("teleport-location", ""),
                        child.getBoolean("look-at-players", true),
                        child.getBoolean("visible", true));
                house.houseNpcs.put(id, npc);
            }
        }
        ConfigurationSection buttonSection = section.getConfigurationSection("action-buttons");
        if (buttonSection != null) {
            for (String id : buttonSection.getKeys(false)) {
                ConfigurationSection child = buttonSection.getConfigurationSection(id);
                if (child == null) continue;
                String location = child.getString("location", "");
                if (location == null || location.trim().isEmpty()) continue;
                ActionButton button = new ActionButton(id,
                        child.getString("name", "Action Button"),
                        location,
                        child.getLong("cooldown-ms", 0L),
                        child.getBoolean("one-time", false),
                        child.getBoolean("particles", true),
                        child.getBoolean("sound", true));
                ConfigurationSection actions = child.getConfigurationSection("actions");
                if (actions != null) {
                    java.util.List<String> keys = new java.util.ArrayList<>(actions.getKeys(false));
                    java.util.Collections.sort(keys);
                    for (String keyAction : keys) {
                        ConfigurationSection actionSection = actions.getConfigurationSection(keyAction);
                        if (actionSection == null) continue;
                        String type = normalizeButtonAction(actionSection.getString("type", "MESSAGE"));
                        button.getActions().add(new ButtonAction(type, actionValue(type, actionSection)));
                    }
                }
                for (String uuid : child.getStringList("used-by")) {
                    try { button.getUsedBy().add(UUID.fromString(uuid)); } catch (IllegalArgumentException ignored) {}
                }
                house.actionButtons.put(id, button);
            }
        }
        ConfigurationSection actionPadSection = section.getConfigurationSection("action-pads");
        if (actionPadSection != null) {
            for (String id : actionPadSection.getKeys(false)) {
                ConfigurationSection child = actionPadSection.getConfigurationSection(id);
                if (child == null) continue;
                String location = parseActionPadLocation(child, house.worldName);
                if (location == null || location.trim().isEmpty()) continue;
                long cooldownMs = child.contains("cooldown-seconds")
                        ? child.getLong("cooldown-seconds", 2L) * 1000L
                        : child.getLong("cooldown-ms", 2000L);
                ActionPad pad = new ActionPad(id,
                        child.getString("name", "&aAction Pad"),
                        location,
                        cooldownMs,
                        child.getBoolean("one-time-use", child.getBoolean("one-time", false)),
                        child.getBoolean("particles", true),
                        child.getBoolean("sound", true));
                ConfigurationSection actions = child.getConfigurationSection("actions");
                if (actions != null) {
                    java.util.List<String> keys = new java.util.ArrayList<>(actions.getKeys(false));
                    java.util.Collections.sort(keys);
                    for (String keyAction : keys) {
                        ConfigurationSection actionSection = actions.getConfigurationSection(keyAction);
                        if (actionSection == null) continue;
                        String type = normalizeButtonAction(actionSection.getString("type", "MESSAGE"));
                        pad.getActions().add(new ButtonAction(type, actionValue(type, actionSection)));
                    }
                }
                for (String uuid : child.getStringList("used-by")) {
                    try { pad.getUsedBy().add(UUID.fromString(uuid)); } catch (IllegalArgumentException ignored) {}
                }
                house.actionPads.put(id, pad);
            }
        }
        ConfigurationSection regionSection = section.getConfigurationSection("regions");
        if (regionSection != null) {
            for (String id : regionSection.getKeys(false)) {
                ConfigurationSection child = regionSection.getConfigurationSection(id);
                if (child == null) continue;
                Region region = new Region(id,
                        child.getString("name", id),
                        child.getString("world", house.worldName),
                        child.getInt("min-x"),
                        child.getInt("max-x"),
                        child.getInt("min-y"),
                        child.getInt("max-y"),
                        child.getInt("min-z"),
                        child.getInt("max-z"),
                        child.getInt("priority", 0));
                ConfigurationSection rules = child.getConfigurationSection("rules");
                if (rules != null) {
                    region.setPvp(rules.getBoolean("pvp", region.isPvp()));
                    region.setBuild(rules.getBoolean("build", region.isBuild()));
                    region.setBlockBreaking(rules.getBoolean("block-breaking", region.isBlockBreaking()));
                    region.setFly(rules.getBoolean("fly", region.isFly()));
                    region.setDamage(rules.getBoolean("damage", region.isDamage()));
                    region.setFallDamage(rules.getBoolean("fall-damage", region.isFallDamage()));
                    region.setHunger(rules.getBoolean("hunger", region.isHunger()));
                    region.setPotionEffects(rules.getString("potion-effects", ""));
                    region.setGameMode(rules.getString("gamemode", ""));
                    region.setApplyPvpKit(rules.getBoolean("apply-pvp-kit", region.isApplyPvpKit()));
                    region.setAllowProjectiles(rules.getBoolean("allow-projectiles", region.isAllowProjectiles()));
                    region.setAllowItemDrops(rules.getBoolean("allow-item-drops", region.isAllowItemDrops()));
                    region.setAllowItemPickup(rules.getBoolean("allow-item-pickup", region.isAllowItemPickup()));
                }
                loadRegionActions(region.getEnterActions(), child.getConfigurationSection("enter-actions"));
                loadRegionActions(region.getLeaveActions(), child.getConfigurationSection("leave-actions"));
                loadRegionActions(region.getWhileInsideActions(), child.getConfigurationSection("while-inside-actions"));
                house.regions.put(region.getId(), region);
            }
        }
        ConfigurationSection commandSection = section.getConfigurationSection("custom-commands");
        if (commandSection != null) {
            for (String id : commandSection.getKeys(false)) {
                ConfigurationSection child = commandSection.getConfigurationSection(id);
                if (child == null) continue;
                CustomCommand command = new CustomCommand(id,
                        child.getBoolean("enabled", true),
                        child.getStringList("aliases"),
                        child.contains("cooldown-seconds") ? child.getLong("cooldown-seconds", 0L) * 1000L : child.getLong("cooldown-ms", 0L),
                        child.getBoolean("guests-allowed", false));
                loadRegionActions(command.getActions(), child.getConfigurationSection("actions"));
                house.customCommands.put(command.getName(), command);
            }
        }
        ConfigurationSection functionSection = section.getConfigurationSection("functions");
        if (functionSection != null) {
            for (String id : functionSection.getKeys(false)) {
                ConfigurationSection child = functionSection.getConfigurationSection(id);
                if (child == null) continue;
                Function function = new Function(id,
                        child.getString("display-name", id),
                        child.contains("cooldown-seconds") ? child.getLong("cooldown-seconds", 0L) * 1000L : child.getLong("cooldown-ms", 0L));
                loadRegionActions(function.getActions(), child.getConfigurationSection("actions"));
                house.functions.put(function.getId(), function);
            }
        }
        house.playerEditedBlocks.addAll(section.getStringList("player-edited-blocks"));
        return house;
    }

    public void save(ConfigurationSection section) {
        section.set("owner-name", ownerName);
        section.set("owner", owner.toString());
        section.set("name", name);
        section.set("template", template);
        section.set("world", worldName);
        section.set("plot-id", plotId);
        section.set("created-at", createdAt);
        section.set("last-opened", lastOpened);
        section.set("public", publicHouse);
        section.set("visiting-rule", visitingRule);
        section.set("weather", weather);
        section.set("time", timeTicks);
        section.set("biome", biome);
        section.set("sky", sky);
        section.set("theme", theme);
        section.set("house-origin.x", originX);
        section.set("house-origin.y", originY);
        section.set("house-origin.z", originZ);
        if (plotBorderSaved) {
            section.set("plot-border.min-x", plotBorderMinX);
            section.set("plot-border.max-x", plotBorderMaxX);
            section.set("plot-border.min-z", plotBorderMinZ);
            section.set("plot-border.max-z", plotBorderMaxZ);
            section.set("plot-border.y", plotBorderY);
        } else {
            section.set("plot-border", null);
        }
        section.set("spawn", spawn == null ? "" : serializeSpawnLocation(spawn));
        section.set("guest-spawn", guestSpawn == null ? "" : serializeSpawnLocation(guestSpawn));
        section.set("tags", new java.util.ArrayList<>(tags));
        section.set("fly-enabled", flyEnabled);
        section.set("plot-size", plotSize);
        section.set("pvp.enabled", pvpEnabled);
        section.set("pvp.fall-damage", fallDamageEnabled);
        section.set("pvp.fire-damage", fireDamageEnabled);
        section.set("pvp.hunger", hungerEnabled);
        section.set("pvp.instant-respawn", instantRespawnEnabled);
        section.set("pvp.kill-death-messages", killDeathMessagesEnabled);
        section.set("visiting-messages.join-message-enabled", joinMessageEnabled);
        section.set("visiting-messages.join-message", joinMessage);
        section.set("visiting-messages.leave-message-enabled", leaveMessageEnabled);
        section.set("visiting-messages.leave-message", leaveMessage);
        section.set("visiting-messages.announce-joins", announceJoinsEnabled);
        section.set("visiting-messages.announce-leaves", announceLeavesEnabled);
        section.set("carpenter.removed", carpenterRemoved);
        section.set("mode", mode);
        section.set("jukebox.enabled", jukeboxEnabled);
        section.set("jukebox.song", jukeboxSong);
        section.set("jukebox.shuffle", jukeboxShuffle);
        section.set("jukebox.volume", jukeboxVolume);
        section.set("scoreboard.enabled", scoreboardEnabled);
        section.set("scoreboard.title", scoreboardTitle);
        section.set("scoreboard.update-ticks", scoreboardUpdateTicks);
        section.set("scoreboard.placeholders-enabled", scoreboardPlaceholdersEnabled);
        section.set("scoreboard.lines", new java.util.ArrayList<>(scoreboardLines));
        section.set("pvp-layout", null);
        ConfigurationSection pvpLayoutSection = section.createSection("pvp-layout");
        for (Map.Entry<Integer, ItemStack> entry : pvpLayout.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 0 || entry.getKey() >= 36 || entry.getValue() == null) continue;
            pvpLayoutSection.set(String.valueOf(entry.getKey()), entry.getValue().clone());
        }
        section.set("cookies", cookies);
        java.util.List<String> cookieList = new java.util.ArrayList<>();
        for (UUID uuid : cookieGivers) cookieList.add(uuid.toString());
        section.set("cookie-givers", cookieList);
        java.util.List<String> list = new java.util.ArrayList<>();
        for (UUID uuid : trusted) list.add(uuid.toString());
        section.set("trusted", list);
        java.util.List<String> bannedList = new java.util.ArrayList<>();
        for (UUID uuid : banned) bannedList.add(uuid.toString());
        section.set("banned", bannedList);
        java.util.List<String> mutedList = new java.util.ArrayList<>();
        for (UUID uuid : muted) mutedList.add(uuid.toString());
        section.set("muted", mutedList);
        java.util.List<String> hiddenBorderList = new java.util.ArrayList<>();
        for (UUID uuid : hiddenBorderViewers) hiddenBorderList.add(uuid.toString());
        section.set("hidden-border-viewers", hiddenBorderList);
        section.set("groups", null);
        ConfigurationSection groupSection = section.createSection("groups");
        for (Map.Entry<UUID, String> entry : groups.entrySet()) groupSection.set(entry.getKey().toString(), entry.getValue());
        section.set("custom-groups", new java.util.ArrayList<>(customGroups));
        section.set("group-permissions", null);
        ConfigurationSection permissionSection = section.createSection("group-permissions");
        for (Map.Entry<String, Set<String>> entry : groupPermissions.entrySet()) {
            if (net.fakepixel.housing.group.HouseGroups.OWNER.equals(net.fakepixel.housing.group.HouseGroups.normalize(entry.getKey()))) continue;
            permissionSection.set(entry.getKey(), new java.util.ArrayList<>(entry.getValue()));
        }
        section.set("group-colors", null);
        ConfigurationSection colorSection = section.createSection("group-colors");
        for (Map.Entry<String, String> entry : groupColors.entrySet()) {
            colorSection.set(entry.getKey(), entry.getValue());
        }
        section.set("group-names", null);
        ConfigurationSection nameSection = section.createSection("group-names");
        for (Map.Entry<String, String> entry : groupDisplayNames.entrySet()) {
            nameSection.set(entry.getKey(), entry.getValue());
        }
        section.set("group-tags", null);
        ConfigurationSection tagSection = section.createSection("group-tags");
        for (Map.Entry<String, String> entry : groupTags.entrySet()) {
            tagSection.set(entry.getKey(), entry.getValue());
        }
        section.set("parkour-pads", null);
        ConfigurationSection parkourSection = section.createSection("parkour-pads");
        for (Map.Entry<String, String> entry : parkourPads.entrySet()) {
            parkourSection.set(entry.getKey(), entry.getValue());
        }
        section.set("parkour-pad-holograms", null);
        ConfigurationSection parkourHologramSection = section.createSection("parkour-pad-holograms");
        for (Map.Entry<String, String> entry : parkourPadHolograms.entrySet()) {
            if (parkourPads.containsKey(entry.getKey()) && entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                parkourHologramSection.set(entry.getKey(), entry.getValue());
            }
        }
        section.set("parkour-leaderboard-hologram", parkourLeaderboardHologramId == null ? "" : parkourLeaderboardHologramId);
        section.set("parkour-best-times", null);
        ConfigurationSection bestSection = section.createSection("parkour-best-times");
        for (Map.Entry<UUID, Long> entry : parkourBestTimes.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0L) continue;
            ConfigurationSection playerSection = bestSection.createSection(entry.getKey().toString());
            playerSection.set("time", entry.getValue());
            String lastKnownName = parkourBestNames.get(entry.getKey());
            if (lastKnownName != null && !lastKnownName.trim().isEmpty()) {
                playerSection.set("last-known-name", lastKnownName);
            }
        }
        section.set("teleport-pads", null);
        ConfigurationSection teleportSection = section.createSection("teleport-pads");
        for (TeleportPad pad : teleportPads.values()) {
            ConfigurationSection padSection = teleportSection.createSection(pad.getId());
            padSection.set("name", pad.getName());
            padSection.set("location", pad.getLocation());
            padSection.set("linked", pad.getLinkedPadId());
            padSection.set("particles", pad.isParticles());
            padSection.set("sound", pad.isSound());
        }
        section.set("holograms", null);
        ConfigurationSection hologramSection = section.createSection("holograms");
        for (Hologram hologram : holograms.values()) {
            ConfigurationSection holoSection = hologramSection.createSection(hologram.getId());
            holoSection.set("location", hologram.getLocation());
            holoSection.set("lines", new java.util.ArrayList<>(hologram.getLines()));
            holoSection.set("visible", hologram.isVisible());
            holoSection.set("entities", new java.util.ArrayList<>(hologram.getEntityIds()));
            holoSection.set("type", hologram.getType());
            holoSection.set("linked-object", hologram.getLinkedObjectId());
        }
        section.set("house-npcs", null);
        ConfigurationSection npcSection = section.createSection("house-npcs");
        for (HouseNpc npc : houseNpcs.values()) {
            ConfigurationSection child = npcSection.createSection(npc.getId());
            child.set("custom-id", npc.getId());
            child.set("name", npc.getName());
            child.set("skin", npc.getSkin());
            child.set("action", npc.getAction());
            child.set("messages", new java.util.ArrayList<>(npc.getMessages()));
            child.set("location", npc.getLocation());
            child.set("teleport-location", npc.getTeleportLocation());
            child.set("look-at-players", npc.isLookAtPlayers());
            child.set("visible", npc.isVisible());
        }
        section.set("action-buttons", null);
        ConfigurationSection buttonSection = section.createSection("action-buttons");
        for (ActionButton button : actionButtons.values()) {
            ConfigurationSection child = buttonSection.createSection(button.getId());
            child.set("name", button.getName());
            child.set("location", button.getLocation());
            child.set("cooldown-ms", button.getCooldownMs());
            child.set("one-time", button.isOneTime());
            child.set("particles", button.isParticles());
            child.set("sound", button.isSound());
            java.util.List<String> used = new java.util.ArrayList<>();
            for (UUID uuid : button.getUsedBy()) used.add(uuid.toString());
            child.set("used-by", used);
            ConfigurationSection actions = child.createSection("actions");
            for (int i = 0; i < button.getActions().size(); i++) {
                ButtonAction action = button.getActions().get(i);
                ConfigurationSection actionSection = actions.createSection(String.format("%03d", i));
                actionSection.set("type", action.getType());
                saveActionValue(actionSection, action);
            }
        }
        section.set("action-pads", null);
        ConfigurationSection actionPadSection = section.createSection("action-pads");
        for (ActionPad pad : actionPads.values()) {
            ConfigurationSection child = actionPadSection.createSection(pad.getId());
            child.set("name", pad.getName());
            saveActionPadLocation(child.createSection("location"), pad.getLocation(), worldName);
            child.set("cooldown-seconds", pad.getCooldownMs() / 1000L);
            child.set("one-time-use", pad.isOneTime());
            child.set("particles", pad.isParticles());
            child.set("sound", pad.isSound());
            java.util.List<String> used = new java.util.ArrayList<>();
            for (UUID uuid : pad.getUsedBy()) used.add(uuid.toString());
            child.set("used-by", used);
            ConfigurationSection actions = child.createSection("actions");
            for (int i = 0; i < pad.getActions().size(); i++) {
                ButtonAction action = pad.getActions().get(i);
                ConfigurationSection actionSection = actions.createSection(String.format("%03d", i));
                actionSection.set("type", action.getType());
                saveActionValue(actionSection, action);
            }
        }
        section.set("regions", null);
        ConfigurationSection regionSection = section.createSection("regions");
        for (Region region : regions.values()) {
            ConfigurationSection child = regionSection.createSection(region.getId());
            child.set("name", region.getName());
            child.set("priority", region.getPriority());
            child.set("world", region.getWorld());
            child.set("min-x", region.getMinX());
            child.set("max-x", region.getMaxX());
            child.set("min-y", region.getMinY());
            child.set("max-y", region.getMaxY());
            child.set("min-z", region.getMinZ());
            child.set("max-z", region.getMaxZ());
            ConfigurationSection rules = child.createSection("rules");
            rules.set("pvp", region.isPvp());
            rules.set("build", region.isBuild());
            rules.set("block-breaking", region.isBlockBreaking());
            rules.set("fly", region.isFly());
            rules.set("damage", region.isDamage());
            rules.set("fall-damage", region.isFallDamage());
            rules.set("hunger", region.isHunger());
            rules.set("potion-effects", region.getPotionEffects());
            rules.set("gamemode", region.getGameMode());
            rules.set("apply-pvp-kit", region.isApplyPvpKit());
            rules.set("allow-projectiles", region.isAllowProjectiles());
            rules.set("allow-item-drops", region.isAllowItemDrops());
            rules.set("allow-item-pickup", region.isAllowItemPickup());
            saveRegionActions(child.createSection("enter-actions"), region.getEnterActions());
            saveRegionActions(child.createSection("leave-actions"), region.getLeaveActions());
            saveRegionActions(child.createSection("while-inside-actions"), region.getWhileInsideActions());
        }
        section.set("custom-commands", null);
        ConfigurationSection commandSection = section.createSection("custom-commands");
        for (CustomCommand command : customCommands.values()) {
            ConfigurationSection child = commandSection.createSection(command.getName());
            child.set("enabled", command.isEnabled());
            child.set("aliases", new java.util.ArrayList<>(command.getAliases()));
            child.set("cooldown-seconds", command.getCooldownMs() / 1000L);
            child.set("guests-allowed", command.isGuestsAllowed());
            saveRegionActions(child.createSection("actions"), command.getActions());
        }
        section.set("functions", null);
        ConfigurationSection functionSection = section.createSection("functions");
        for (Function function : functions.values()) {
            ConfigurationSection child = functionSection.createSection(function.getId());
            child.set("display-name", function.getDisplayName());
            child.set("cooldown-seconds", function.getCooldownMs() / 1000L);
            saveRegionActions(child.createSection("actions"), function.getActions());
        }
        section.set("player-edited-blocks", new java.util.ArrayList<>(playerEditedBlocks));
    }

    public UUID getOwner() { return owner; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTemplate() { return template; }
    public String getWorldName() { return worldName; }
    public int getPlotId() { return plotId; }
    public long getLastOpened() { return lastOpened; }
    public void setLastOpened(long lastOpened) { this.lastOpened = lastOpened; }
    public long getCreatedAt() { return createdAt; }
    public int getWeeklyCookies() { return cookies; }
    public boolean isPublicHouse() { return publicHouse; }
    public void setPublicHouse(boolean publicHouse) { this.publicHouse = publicHouse; this.visitingRule = publicHouse ? "public" : "private"; }
    public String getVisitingRule() { return visitingRule; }
    public void setVisitingRule(String visitingRule) { this.visitingRule = normalizeVisitingRule(visitingRule); this.publicHouse = "public".equals(this.visitingRule); }
    public Set<UUID> getTrusted() { return trusted; }
    public Location getSpawn() { return spawn; }
    public void setSpawn(Location spawn) { this.spawn = spawn; }
    public Location getGuestSpawn() { return guestSpawn; }
    public void setGuestSpawn(Location guestSpawn) { this.guestSpawn = guestSpawn; }
    public Location getOrigin() { return new Location(Bukkit.getWorld(worldName), originX, originY, originZ); }
    public Location getPlotCenter() { return getOrigin(); }
    public int getCookies() { return cookies; }
    public void setCookies(int cookies) { this.cookies = Math.max(0, cookies); }
    public void addCookies(int amount) { this.cookies = Math.max(0, this.cookies + amount); }
    public Set<UUID> getCookieGivers() { return cookieGivers; }
    public Set<UUID> getBanned() { return banned; }
    public Set<UUID> getMuted() { return muted; }
    public boolean isBorderVisible(UUID uuid) { return !hiddenBorderViewers.contains(uuid); }
    public boolean toggleBorderVisibility(UUID uuid) {
        if (hiddenBorderViewers.remove(uuid)) return true;
        hiddenBorderViewers.add(uuid);
        return false;
    }
    public Map<UUID, String> getGroups() { return groups; }
    public Set<String> getCustomGroups() { return customGroups; }
    public Map<String, Set<String>> getGroupPermissions() { return groupPermissions; }
    public Map<String, String> getGroupColors() { return groupColors; }
    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = normalizeWeather(weather); }
    public long getTimeTicks() { return timeTicks; }
    public void setTimeTicks(long timeTicks) { this.timeTicks = Math.max(-1L, timeTicks); }
    public String getBiome() { return biome; }
    public void setBiome(String biome) { this.biome = normalizeBiome(biome); }
    public String getSky() { return sky; }
    public void setSky(String sky) { this.sky = normalizeSky(sky); }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = normalizeTheme(theme); }
    public boolean hasPlotBorder() { return plotBorderSaved; }
    public int getPlotBorderMinX() { return plotBorderMinX; }
    public int getPlotBorderMaxX() { return plotBorderMaxX; }
    public int getPlotBorderMinZ() { return plotBorderMinZ; }
    public int getPlotBorderMaxZ() { return plotBorderMaxZ; }
    public int getPlotBorderY() { return plotBorderY; }
    public void setPlotBorder(int minX, int maxX, int minZ, int maxZ, int y) {
        this.plotBorderSaved = true;
        this.plotBorderMinX = Math.min(minX, maxX);
        this.plotBorderMaxX = Math.max(minX, maxX);
        this.plotBorderMinZ = Math.min(minZ, maxZ);
        this.plotBorderMaxZ = Math.max(minZ, maxZ);
        this.plotBorderY = y;
    }
    public Set<String> getTags() { return tags; }
    public boolean hasTag(String tag) { return tags.contains(normalizeTag(tag)); }
    public void setHousingTag(String tag) { tags.clear(); tags.add(normalizeTag(tag)); }
    public boolean isFlyEnabled() { return flyEnabled; }
    public void setFlyEnabled(boolean flyEnabled) { this.flyEnabled = flyEnabled; }
    public int getPlotSize() { return plotSize; }
    public void setPlotSize(int plotSize) { this.plotSize = normalizePlotSize(plotSize); }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    public boolean isFallDamageEnabled() { return fallDamageEnabled; }
    public void setFallDamageEnabled(boolean fallDamageEnabled) { this.fallDamageEnabled = fallDamageEnabled; }
    public boolean isFireDamageEnabled() { return fireDamageEnabled; }
    public void setFireDamageEnabled(boolean fireDamageEnabled) { this.fireDamageEnabled = fireDamageEnabled; }
    public boolean isHungerEnabled() { return hungerEnabled; }
    public void setHungerEnabled(boolean hungerEnabled) { this.hungerEnabled = hungerEnabled; }
    public boolean isInstantRespawnEnabled() { return instantRespawnEnabled; }
    public void setInstantRespawnEnabled(boolean instantRespawnEnabled) { this.instantRespawnEnabled = instantRespawnEnabled; }
    public boolean isKillDeathMessagesEnabled() { return killDeathMessagesEnabled; }
    public void setKillDeathMessagesEnabled(boolean killDeathMessagesEnabled) { this.killDeathMessagesEnabled = killDeathMessagesEnabled; }
    public boolean isJoinMessageEnabled() { return joinMessageEnabled; }
    public void setJoinMessageEnabled(boolean joinMessageEnabled) { this.joinMessageEnabled = joinMessageEnabled; }
    public String getJoinMessage() { return joinMessage; }
    public void setJoinMessage(String joinMessage) { this.joinMessage = cleanMessage(joinMessage, "&aWelcome to &e%owner%'s House&a!"); }
    public boolean isLeaveMessageEnabled() { return leaveMessageEnabled; }
    public void setLeaveMessageEnabled(boolean leaveMessageEnabled) { this.leaveMessageEnabled = leaveMessageEnabled; }
    public String getLeaveMessage() { return leaveMessage; }
    public void setLeaveMessage(String leaveMessage) { this.leaveMessage = cleanMessage(leaveMessage, "&7You left &e%owner%'s House&7."); }
    public boolean isAnnounceJoinsEnabled() { return announceJoinsEnabled; }
    public void setAnnounceJoinsEnabled(boolean announceJoinsEnabled) { this.announceJoinsEnabled = announceJoinsEnabled; }
    public boolean isAnnounceLeavesEnabled() { return announceLeavesEnabled; }
    public void setAnnounceLeavesEnabled(boolean announceLeavesEnabled) { this.announceLeavesEnabled = announceLeavesEnabled; }
    public boolean isCarpenterRemoved() { return carpenterRemoved; }
    public void setCarpenterRemoved(boolean carpenterRemoved) { this.carpenterRemoved = carpenterRemoved; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = normalizeMode(mode); }
    public boolean isBuildMode() { return "BUILD".equals(mode); }
    public void toggleMode() { mode = isBuildMode() ? "SOCIAL" : "BUILD"; }
    public boolean isJukeboxEnabled() { return jukeboxEnabled; }
    public void setJukeboxEnabled(boolean jukeboxEnabled) { this.jukeboxEnabled = jukeboxEnabled; }
    public String getJukeboxSong() { return jukeboxSong; }
    public void setJukeboxSong(String jukeboxSong) { this.jukeboxSong = cleanJukeboxSong(jukeboxSong); }
    public boolean isJukeboxShuffle() { return jukeboxShuffle; }
    public void setJukeboxShuffle(boolean jukeboxShuffle) { this.jukeboxShuffle = jukeboxShuffle; }
    public int getJukeboxVolume() { return jukeboxVolume; }
    public void setJukeboxVolume(int jukeboxVolume) { this.jukeboxVolume = normalizeJukeboxVolume(jukeboxVolume); }
    public boolean isScoreboardEnabled() { return scoreboardEnabled; }
    public void setScoreboardEnabled(boolean scoreboardEnabled) { this.scoreboardEnabled = scoreboardEnabled; }
    public String getScoreboardTitle() { return scoreboardTitle; }
    public void setScoreboardTitle(String scoreboardTitle) { this.scoreboardTitle = scoreboardTitle == null || scoreboardTitle.trim().isEmpty() ? "&b&lHOUSING" : trim(scoreboardTitle, 32); }
    public int getScoreboardUpdateTicks() { return scoreboardUpdateTicks; }
    public void setScoreboardUpdateTicks(int scoreboardUpdateTicks) { this.scoreboardUpdateTicks = normalizeScoreboardUpdateTicks(scoreboardUpdateTicks); }
    public boolean isScoreboardPlaceholdersEnabled() { return scoreboardPlaceholdersEnabled; }
    public void setScoreboardPlaceholdersEnabled(boolean scoreboardPlaceholdersEnabled) { this.scoreboardPlaceholdersEnabled = scoreboardPlaceholdersEnabled; }
    public java.util.List<String> getScoreboardLines() { return scoreboardLines; }
    public void setScoreboardLine(int index, String line) {
        if (index >= 0 && index < scoreboardLines.size()) scoreboardLines.set(index, trim(line == null ? "" : line, 64));
    }
    public boolean addScoreboardLine(String line) {
        if (scoreboardLines.size() >= 15) return false;
        scoreboardLines.add(trim(line == null ? "" : line, 64));
        return true;
    }
    public void removeScoreboardLine(int index) {
        if (index >= 0 && index < scoreboardLines.size()) scoreboardLines.remove(index);
    }
    public void moveScoreboardLine(int index, int direction) {
        int target = index + direction;
        if (index < 0 || index >= scoreboardLines.size() || target < 0 || target >= scoreboardLines.size()) return;
        java.util.Collections.swap(scoreboardLines, index, target);
    }
    public void resetScoreboard() {
        scoreboardEnabled = true;
        scoreboardTitle = "&b&lHOUSING";
        scoreboardUpdateTicks = 40;
        scoreboardPlaceholdersEnabled = true;
        resetScoreboardLines();
    }
    private void resetScoreboardLines() {
        scoreboardLines.clear();
        scoreboardLines.add("&7%date%");
        scoreboardLines.add("");
        scoreboardLines.add("&fHouse name");
        scoreboardLines.add("&a%house_name%");
        scoreboardLines.add("");
        scoreboardLines.add("&fVisiting Rules");
        scoreboardLines.add("&r%housing_rules%");
        scoreboardLines.add("");
        scoreboardLines.add("&fGuests: &a%guests%");
        scoreboardLines.add("");
        scoreboardLines.add("&fCookies: &6%cookies%");
        scoreboardLines.add("");
        scoreboardLines.add("&emc.fakepixel.me");
    }
    public ItemStack[] getPvpLayout() {
        ItemStack[] contents = new ItemStack[36];
        for (Map.Entry<Integer, ItemStack> entry : pvpLayout.entrySet()) {
            if (entry.getKey() != null && entry.getKey() >= 0 && entry.getKey() < contents.length && entry.getValue() != null) {
                contents[entry.getKey()] = entry.getValue().clone();
            }
        }
        return contents;
    }
    public void setPvpLayout(ItemStack[] contents) {
        pvpLayout.clear();
        if (contents == null) return;
        for (int i = 0; i < contents.length && i < 36; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != net.fakepixel.housing.util.Compat.mat("AIR")) pvpLayout.put(i, item.clone());
        }
    }
    public void clearPvpLayout() { pvpLayout.clear(); }
    public boolean hasPvpLayout() { return !pvpLayout.isEmpty(); }
    public Map<String, String> getParkourPads() { return parkourPads; }
    public String setParkourPad(String type, Location location) {
        if (type == null || location == null) return null;
        String clean = type.trim().toLowerCase(java.util.Locale.ENGLISH);
        String serialized = serializeBlockLocation(location);
        String key = "checkpoint".equals(clean) ? "checkpoint:" + serialized : clean;
        parkourPads.put(key, serialized);
        return key;
    }
    public String removeParkourPad(Location location) {
        if (location == null) return null;
        String serialized = serializeBlockLocation(location);
        String removedKey = null;
        java.util.Iterator<Map.Entry<String, String>> iterator = parkourPads.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (!serialized.equals(entry.getValue())) continue;
            removedKey = entry.getKey();
            iterator.remove();
            break;
        }
        return removedKey;
    }
    public String getParkourStart() { return parkourPads.get("start"); }
    public String getParkourFinish() { return parkourPads.get("finish"); }
    public java.util.List<String> getParkourCheckpoints() {
        java.util.List<String> checkpoints = new java.util.ArrayList<>();
        String legacy = parkourPads.get("checkpoint");
        if (legacy != null && !legacy.trim().isEmpty()) checkpoints.add(legacy);
        for (Map.Entry<String, String> entry : parkourPads.entrySet()) {
            if (entry.getKey().startsWith("checkpoint:") && entry.getValue() != null && !entry.getValue().trim().isEmpty()) checkpoints.add(entry.getValue());
        }
        return checkpoints;
    }
    public String parkourPadTypeAt(Location location) {
        if (location == null) return null;
        String serialized = serializeBlockLocation(location);
        if (serialized.equals(getParkourStart())) return "start";
        if (serialized.equals(getParkourFinish())) return "finish";
        for (String checkpoint : getParkourCheckpoints()) if (serialized.equals(checkpoint)) return "checkpoint";
        return null;
    }
    public String parkourPadKeyAt(Location location) {
        if (location == null) return null;
        String serialized = serializeBlockLocation(location);
        for (Map.Entry<String, String> entry : parkourPads.entrySet()) {
            if (serialized.equals(entry.getValue())) return entry.getKey();
        }
        return null;
    }
    public int parkourCheckpointNumber(String padKey) {
        if (padKey == null || !padKey.startsWith("checkpoint:")) return 0;
        int number = 0;
        for (String key : parkourPads.keySet()) {
            if (!key.startsWith("checkpoint:")) continue;
            number++;
            if (key.equals(padKey)) return number;
        }
        return 0;
    }
    public Map<String, String> getParkourPadHolograms() { return parkourPadHolograms; }
    public String getParkourPadHologram(String padKey) { return parkourPadHolograms.get(padKey); }
    public void setParkourPadHologram(String padKey, String hologramId) {
        if (padKey == null || hologramId == null || hologramId.trim().isEmpty()) return;
        parkourPadHolograms.put(padKey, hologramId);
    }
    public String removeParkourPadHologram(String padKey) {
        return padKey == null ? null : parkourPadHolograms.remove(padKey);
    }
    public String getParkourLeaderboardHologramId() { return parkourLeaderboardHologramId; }
    public void setParkourLeaderboardHologramId(String hologramId) {
        this.parkourLeaderboardHologramId = hologramId == null ? "" : hologramId;
    }
    public void clearParkourLeaderboardHologramId() { this.parkourLeaderboardHologramId = ""; }
    public Map<UUID, Long> getParkourBestTimes() { return parkourBestTimes; }
    public Map<UUID, String> getParkourBestNames() { return parkourBestNames; }
    public Long getParkourBestTime(UUID uuid) { return parkourBestTimes.get(uuid); }
    public String getParkourBestName(UUID uuid) { return parkourBestNames.get(uuid); }
    public boolean setParkourBestTime(UUID uuid, long millis) {
        return setParkourBestTime(uuid, null, millis);
    }
    public boolean setParkourBestTime(UUID uuid, String lastKnownName, long millis) {
        if (uuid == null || millis <= 0L) return false;
        if (lastKnownName != null && !lastKnownName.trim().isEmpty()) parkourBestNames.put(uuid, lastKnownName.trim());
        Long current = parkourBestTimes.get(uuid);
        if (current != null && current <= millis) return false;
        parkourBestTimes.put(uuid, millis);
        return true;
    }

    private String serializeBlockLocation(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    public Map<String, TeleportPad> getTeleportPads() { return teleportPads; }
    public TeleportPad getTeleportPad(String id) { return teleportPads.get(id); }
    public TeleportPad getTeleportPadAt(Location location) {
        if (location == null) return null;
        String serialized = serializeBlockLocation(location);
        for (TeleportPad pad : teleportPads.values()) {
            if (serialized.equals(pad.getLocation())) return pad;
        }
        return null;
    }
    public TeleportPad addTeleportPad(Location location) {
        if (location == null) return null;
        String serialized = serializeBlockLocation(location);
        TeleportPad existing = getTeleportPadAt(location);
        if (existing != null) return existing;
        String id = "pad-" + UUID.randomUUID().toString().substring(0, 8);
        TeleportPad pad = new TeleportPad(id, "Teleport Pad " + (teleportPads.size() + 1), serialized, "", true, true);
        teleportPads.put(id, pad);
        TeleportPad open = firstUnlinkedTeleportPad(id);
        if (open != null) linkTeleportPads(open.getId(), id);
        return pad;
    }
    public void removeTeleportPad(String id) {
        if (id == null) return;
        teleportPads.remove(id);
        for (TeleportPad pad : teleportPads.values()) {
            if (id.equals(pad.getLinkedPadId())) pad.setLinkedPadId("");
        }
    }
    public void removeTeleportPad(Location location) {
        TeleportPad pad = getTeleportPadAt(location);
        if (pad != null) removeTeleportPad(pad.getId());
    }
    public void linkTeleportPads(String firstId, String secondId) {
        if (firstId == null || secondId == null || firstId.equals(secondId)) return;
        TeleportPad first = teleportPads.get(firstId);
        TeleportPad second = teleportPads.get(secondId);
        if (first == null || second == null) return;
        unlinkTeleportPad(firstId);
        unlinkTeleportPad(secondId);
        first.setLinkedPadId(secondId);
        second.setLinkedPadId(firstId);
    }
    public void unlinkTeleportPad(String id) {
        if (id == null) return;
        TeleportPad pad = teleportPads.get(id);
        if (pad != null) pad.setLinkedPadId("");
        for (TeleportPad other : teleportPads.values()) {
            if (id.equals(other.getLinkedPadId())) other.setLinkedPadId("");
        }
    }
    private TeleportPad firstUnlinkedTeleportPad(String exceptId) {
        for (TeleportPad pad : teleportPads.values()) {
            if (pad.getId().equals(exceptId)) continue;
            if (pad.getLinkedPadId() == null || pad.getLinkedPadId().trim().isEmpty() || teleportPads.get(pad.getLinkedPadId()) == null) return pad;
        }
        return null;
    }
    private void normalizeTeleportPadLinks() {
        for (TeleportPad pad : teleportPads.values()) {
            String linkedId = pad.getLinkedPadId();
            if (linkedId == null || linkedId.trim().isEmpty() || linkedId.equals(pad.getId()) || !teleportPads.containsKey(linkedId)) {
                pad.setLinkedPadId("");
            }
        }
        for (TeleportPad pad : teleportPads.values()) {
            String linkedId = pad.getLinkedPadId();
            if (linkedId == null || linkedId.trim().isEmpty()) continue;
            TeleportPad linked = teleportPads.get(linkedId);
            if (linked != null && (linked.getLinkedPadId() == null || linked.getLinkedPadId().trim().isEmpty() || pad.getId().equals(linked.getLinkedPadId()))) {
                linked.setLinkedPadId(pad.getId());
            } else {
                pad.setLinkedPadId("");
            }
        }
    }

    public Map<String, Hologram> getHolograms() { return holograms; }
    public Hologram getHologram(String id) { return holograms.get(id); }
    public Hologram addHologram(Location location) {
        if (location == null) return null;
        String id = "hologram-" + UUID.randomUUID().toString().substring(0, 8);
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("&eHologram");
        Hologram hologram = new Hologram(id, serializePreciseLocation(location), lines, true, new java.util.ArrayList<>(), "CUSTOM", "");
        holograms.put(id, hologram);
        return hologram;
    }
    public Hologram addHologram(Location location, java.util.List<String> lines) {
        Hologram hologram = addHologram(location);
        if (hologram != null && lines != null && !lines.isEmpty()) hologram.setLines(lines);
        return hologram;
    }
    public Hologram addHologram(Location location, java.util.List<String> lines, String type, String linkedObjectId) {
        Hologram hologram = addHologram(location, lines);
        if (hologram != null) hologram.setIdentity(type, linkedObjectId);
        return hologram;
    }
    public void removeHologram(String id) {
        if (id != null) holograms.remove(id);
    }
    public void moveHologram(String id, Location location) {
        Hologram hologram = getHologram(id);
        if (hologram != null && location != null) hologram.setLocation(serializePreciseLocation(location));
    }

    private String serializePreciseLocation(Location location) {
        return location.getX() + "," + location.getY() + "," + location.getZ();
    }

    public Map<String, HouseNpc> getHouseNpcs() { return houseNpcs; }
    public HouseNpc getHouseNpc(String id) { return houseNpcs.get(id); }
    public HouseNpc addHouseNpc(Location location) {
        if (location == null) return null;
        String id = "npc-" + UUID.randomUUID().toString().substring(0, 8);
        java.util.List<String> messages = new java.util.ArrayList<>();
        messages.add("&eHello, %player%!");
        HouseNpc npc = new HouseNpc(id, "&aNPC", "", "NONE", messages, serializeFullLocation(location), "", true, true);
        houseNpcs.put(id, npc);
        return npc;
    }
    public void removeHouseNpc(String id) {
        if (id != null) houseNpcs.remove(id);
    }
    public void moveHouseNpc(String id, Location location) {
        HouseNpc npc = getHouseNpc(id);
        if (npc != null && location != null) npc.setLocation(serializeFullLocation(location));
    }

    public Map<String, ActionButton> getActionButtons() { return actionButtons; }
    public ActionButton getActionButton(String id) { return actionButtons.get(id); }
    public ActionButton getActionButtonAt(Location location) {
        if (location == null) return null;
        String serialized = serializeBlockLocation(location);
        for (ActionButton button : actionButtons.values()) {
            if (serialized.equals(button.getLocation())) return button;
        }
        return null;
    }
    public ActionButton addActionButton(Location location) {
        if (location == null) return null;
        ActionButton existing = getActionButtonAt(location);
        if (existing != null) return existing;
        String id = "button-" + UUID.randomUUID().toString().substring(0, 8);
        ActionButton button = new ActionButton(id, "Action Button " + (actionButtons.size() + 1), serializeBlockLocation(location), 0L, false, true, true);
        actionButtons.put(id, button);
        return button;
    }
    public void removeActionButton(String id) { if (id != null) actionButtons.remove(id); }
    public void removeActionButton(Location location) {
        ActionButton button = getActionButtonAt(location);
        if (button != null) removeActionButton(button.getId());
    }

    public Map<String, ActionPad> getActionPads() { return actionPads; }
    public ActionPad getActionPad(String id) { return actionPads.get(id); }
    public ActionPad getActionPadAt(Location location) {
        if (location == null) return null;
        String serialized = serializeBlockLocation(location);
        for (ActionPad pad : actionPads.values()) {
            if (serialized.equals(pad.getLocation())) return pad;
        }
        return null;
    }
    public ActionPad addActionPad(Location location) {
        if (location == null) return null;
        ActionPad existing = getActionPadAt(location);
        if (existing != null) return existing;
        String id = "pad-" + UUID.randomUUID().toString().substring(0, 8);
        ActionPad pad = new ActionPad(id, "&aAction Pad " + (actionPads.size() + 1), serializeBlockLocation(location), 2000L, false, true, true);
        actionPads.put(id, pad);
        return pad;
    }
    public void removeActionPad(String id) { if (id != null) actionPads.remove(id); }
    public void removeActionPad(Location location) {
        ActionPad pad = getActionPadAt(location);
        if (pad != null) removeActionPad(pad.getId());
    }

    public Map<String, Region> getRegions() { return regions; }
    public Region getRegion(String id) { return id == null ? null : regions.get(cleanRegionId(id)); }
    public java.util.List<Region> getRegionsAt(Location location) {
        java.util.List<Region> matches = new java.util.ArrayList<>();
        if (location == null || location.getWorld() == null) return matches;
        for (Region region : regions.values()) {
            if (region.contains(location)) matches.add(region);
        }
        matches.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return matches;
    }
    public Region getRegionAt(Location location) {
        java.util.List<Region> matches = getRegionsAt(location);
        return matches.isEmpty() ? null : matches.get(0);
    }
    public Region addRegion(String name, Location a, Location b) {
        if (name == null || a == null || b == null || a.getWorld() == null || b.getWorld() == null) return null;
        if (!a.getWorld().getName().equals(b.getWorld().getName())) return null;
        String id = cleanRegionId(name);
        if (id.isEmpty() || regions.containsKey(id)) return null;
        Region region = new Region(id, cleanRegionName(name), a.getWorld().getName(),
                Math.min(a.getBlockX(), b.getBlockX()), Math.max(a.getBlockX(), b.getBlockX()),
                Math.min(a.getBlockY(), b.getBlockY()), Math.max(a.getBlockY(), b.getBlockY()),
                Math.min(a.getBlockZ(), b.getBlockZ()), Math.max(a.getBlockZ(), b.getBlockZ()), 0);
        regions.put(region.getId(), region);
        return region;
    }
    public void removeRegion(String id) {
        if (id != null) regions.remove(cleanRegionId(id));
    }

    public Map<String, CustomCommand> getCustomCommands() { return customCommands; }
    public CustomCommand getCustomCommand(String nameOrAlias) {
        String clean = cleanCommandName(nameOrAlias);
        if (clean.isEmpty()) return null;
        CustomCommand command = customCommands.get(clean);
        if (command != null) return command;
        for (CustomCommand existing : customCommands.values()) {
            if (existing.hasAlias(clean)) return existing;
        }
        return null;
    }
    public CustomCommand addCustomCommand(String name) {
        String clean = cleanCommandName(name);
        if (clean.isEmpty() || customCommands.containsKey(clean)) return null;
        CustomCommand command = new CustomCommand(clean, true, new java.util.ArrayList<>(), 0L, false);
        customCommands.put(clean, command);
        return command;
    }
    public boolean renameCustomCommand(String oldName, String newName) {
        String oldClean = cleanCommandName(oldName);
        String newClean = cleanCommandName(newName);
        if (oldClean.isEmpty() || newClean.isEmpty() || (!oldClean.equals(newClean) && customCommands.containsKey(newClean))) return false;
        CustomCommand command = customCommands.remove(oldClean);
        if (command == null) return false;
        command.setName(newClean);
        customCommands.put(newClean, command);
        return true;
    }
    public void removeCustomCommand(String name) {
        if (name != null) customCommands.remove(cleanCommandName(name));
    }

    public Map<String, Function> getFunctions() { return functions; }
    public Function getFunction(String id) {
        String clean = cleanFunctionId(id);
        return clean.isEmpty() ? null : functions.get(clean);
    }
    public Function addFunction(String id) {
        String clean = cleanFunctionId(id);
        if (clean.isEmpty() || functions.containsKey(clean)) return null;
        Function function = new Function(clean, "&a" + clean, 0L);
        functions.put(clean, function);
        return function;
    }
    public boolean renameFunction(String oldId, String newId) {
        String oldClean = cleanFunctionId(oldId);
        String newClean = cleanFunctionId(newId);
        if (oldClean.isEmpty() || newClean.isEmpty() || (!oldClean.equals(newClean) && functions.containsKey(newClean))) return false;
        Function function = functions.remove(oldClean);
        if (function == null) return false;
        function.setId(newClean);
        if (function.getDisplayName() == null || function.getDisplayName().trim().isEmpty() || function.getDisplayName().equals(oldClean)) function.setDisplayName(newClean);
        functions.put(newClean, function);
        return true;
    }
    public void removeFunction(String id) {
        if (id != null) functions.remove(cleanFunctionId(id));
    }

    public boolean isPlayerEditedBlock(int x, int y, int z) {
        return playerEditedBlocks.contains(blockKey(x, y, z));
    }

    public void markPlayerEditedBlock(Location location) {
        if (location != null) playerEditedBlocks.add(blockKey(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    public void markPlayerEditedBlock(int x, int y, int z) {
        playerEditedBlocks.add(blockKey(x, y, z));
    }

    private static String blockKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private String serializeFullLocation(Location location) {
        return location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    private String serializeSpawnLocation(Location location) {
        String world = location.getWorld() == null ? worldName : location.getWorld().getName();
        return world + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    private static Location parseFullLocation(String raw, String fallbackWorld) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String[] split = raw.split(",");
        try {
            if (split.length == 6) {
                org.bukkit.World world = Bukkit.getWorld(split[0]);
                if (world == null) world = Bukkit.getWorld(fallbackWorld);
                return new Location(world,
                        Double.parseDouble(split[1]),
                        Double.parseDouble(split[2]),
                        Double.parseDouble(split[3]),
                        Float.parseFloat(split[4]),
                        Float.parseFloat(split[5]));
            }
            if (split.length == 5) {
                org.bukkit.World world = Bukkit.getWorld(fallbackWorld);
                return new Location(world,
                        Double.parseDouble(split[0]),
                        Double.parseDouble(split[1]),
                        Double.parseDouble(split[2]),
                        Float.parseFloat(split[3]),
                        Float.parseFloat(split[4]));
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static UUID ownerFromStorageKey(String key, ConfigurationSection section) {
        String storedOwner = section.getString("owner", "");
        if (storedOwner != null && !storedOwner.trim().isEmpty()) return UUID.fromString(storedOwner);
        String ownerPart = key == null ? "" : key;
        int plotIndex = ownerPart.indexOf("_plot_");
        if (plotIndex > 0) ownerPart = ownerPart.substring(0, plotIndex);
        return UUID.fromString(ownerPart);
    }

    private static int plotIdFromStorageKey(String key) {
        if (key == null) return 1;
        int plotIndex = key.indexOf("_plot_");
        if (plotIndex < 0) return 1;
        try {
            return Math.max(1, Integer.parseInt(key.substring(plotIndex + "_plot_".length())));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static String normalizeNpcAction(String action) {
        if (action == null) return "NONE";
        String clean = action.trim().toUpperCase(java.util.Locale.ENGLISH);
        if (clean.equals("MESSAGE") || clean.equals("TELEPORT") || clean.equals("CALL_FUNCTION")) return clean;
        return "NONE";
    }

    private static String normalizeButtonAction(String action) {
        if (action == null) return "MESSAGE";
        String clean = action.trim().toUpperCase(java.util.Locale.ENGLISH);
        if (clean.equals("MESSAGE") || clean.equals("TITLE") || clean.equals("ACTION_BAR") || clean.equals("TELEPORT") || clean.equals("PLAY_SOUND")
                || clean.equals("GIVE_ITEM") || clean.equals("POTION_EFFECT") || clean.equals("START_PARKOUR")
                || clean.equals("RESET_PARKOUR") || clean.equals("CHECKPOINT_PARKOUR") || clean.equals("OPEN_MENU")
                || clean.equals("CHANGE_TIME") || clean.equals("CHANGE_WEATHER") || clean.equals("CALL_FUNCTION")) return clean;
        return "MESSAGE";
    }

    private static String parseActionPadLocation(ConfigurationSection section, String fallbackWorld) {
        ConfigurationSection location = section.getConfigurationSection("location");
        if (location != null) {
            String world = location.getString("world", fallbackWorld);
            return location.getInt("x") + "," + location.getInt("y") + "," + location.getInt("z");
        }
        return section.getString("location", "");
    }

    private static void saveActionPadLocation(ConfigurationSection section, String raw, String fallbackWorld) {
        String[] split = raw == null ? new String[0] : raw.split(",");
        section.set("world", fallbackWorld);
        if (split.length >= 3) {
            try {
                section.set("x", Integer.parseInt(split[0]));
                section.set("y", Integer.parseInt(split[1]));
                section.set("z", Integer.parseInt(split[2]));
                return;
            } catch (NumberFormatException ignored) {
            }
        }
        section.set("x", 0);
        section.set("y", 0);
        section.set("z", 0);
    }

    private static String actionValue(String type, ConfigurationSection section) {
        String value = section.getString("value", null);
        if (value != null) return value;
        if ("MESSAGE".equals(type) || "ACTION_BAR".equals(type)) return section.getString("message", "");
        if ("TITLE".equals(type)) {
            String title = section.getString("title", "");
            String subtitle = section.getString("subtitle", "");
            return subtitle == null || subtitle.isEmpty() ? title : title + "\n" + subtitle;
        }
        if ("PLAY_SOUND".equals(type)) return section.getString("sound", "");
        if ("GIVE_ITEM".equals(type)) return section.getString("item", "");
        if ("POTION_EFFECT".equals(type)) return section.getString("effect", "");
        if ("OPEN_MENU".equals(type)) return section.getString("menu", "");
        if ("CHANGE_TIME".equals(type)) return section.getString("time", "");
        if ("CHANGE_WEATHER".equals(type)) return section.getString("weather", "");
        if ("CALL_FUNCTION".equals(type)) return section.getString("function", section.getString("value", ""));
        if ("TELEPORT".equals(type)) {
            ConfigurationSection loc = section.getConfigurationSection("location");
            if (loc == null) loc = section;
            if (loc.contains("x") && loc.contains("y") && loc.contains("z")) {
                return loc.getString("world", "") + "," + loc.getDouble("x") + "," + loc.getDouble("y") + "," + loc.getDouble("z") + "," + (float) loc.getDouble("yaw", 0.0D) + "," + (float) loc.getDouble("pitch", 0.0D);
            }
        }
        return "";
    }

    private static void saveActionValue(ConfigurationSection section, ButtonAction action) {
        String type = action.getType();
        String value = action.getValue();
        if ("MESSAGE".equals(type) || "ACTION_BAR".equals(type)) section.set("message", value);
        else if ("TITLE".equals(type)) {
            String[] split = value == null ? new String[0] : value.split("\\n", 2);
            section.set("title", split.length > 0 ? split[0] : "");
            if (split.length > 1) section.set("subtitle", split[1]);
        }
        else if ("PLAY_SOUND".equals(type)) section.set("sound", value);
        else if ("GIVE_ITEM".equals(type)) section.set("item", value);
        else if ("POTION_EFFECT".equals(type)) section.set("effect", value);
        else if ("OPEN_MENU".equals(type)) section.set("menu", value);
        else if ("CHANGE_TIME".equals(type)) section.set("time", value);
        else if ("CHANGE_WEATHER".equals(type)) section.set("weather", value);
        else if ("CALL_FUNCTION".equals(type)) section.set("function", value);
        else if ("TELEPORT".equals(type)) {
            String[] split = value == null ? new String[0] : value.split(",");
            if (split.length >= 6) {
                section.set("world", split[0]);
                try {
                    section.set("x", Double.parseDouble(split[1]));
                    section.set("y", Double.parseDouble(split[2]));
                    section.set("z", Double.parseDouble(split[3]));
                    section.set("yaw", Float.parseFloat(split[4]));
                    section.set("pitch", Float.parseFloat(split[5]));
                    return;
                } catch (NumberFormatException ignored) {
                }
            }
            section.set("value", value);
        } else if (value != null && !value.isEmpty()) section.set("value", value);
    }

    private static void loadRegionActions(java.util.List<ButtonAction> target, ConfigurationSection section) {
        if (section == null) return;
        java.util.List<String> keys = new java.util.ArrayList<>(section.getKeys(false));
        java.util.Collections.sort(keys);
        for (String key : keys) {
            ConfigurationSection actionSection = section.getConfigurationSection(key);
            if (actionSection == null) continue;
            String type = normalizeButtonAction(actionSection.getString("type", "MESSAGE"));
            target.add(new ButtonAction(type, actionValue(type, actionSection)));
        }
    }

    private static void saveRegionActions(ConfigurationSection section, java.util.List<ButtonAction> actions) {
        for (int i = 0; i < actions.size(); i++) {
            ButtonAction action = actions.get(i);
            ConfigurationSection actionSection = section.createSection(String.format("%03d", i));
            actionSection.set("type", action.getType());
            saveActionValue(actionSection, action);
        }
    }

    private static String cleanRegionId(String name) {
        if (name == null) return "";
        String clean = name.trim().toLowerCase(java.util.Locale.ENGLISH).replaceAll("[^a-z0-9_-]", "-");
        while (clean.contains("--")) clean = clean.replace("--", "-");
        if (clean.startsWith("-")) clean = clean.substring(1);
        if (clean.endsWith("-")) clean = clean.substring(0, clean.length() - 1);
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    private static String cleanRegionName(String name) {
        if (name == null || name.trim().isEmpty()) return "Region";
        String clean = name.trim();
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    private static String cleanCommandName(String name) {
        if (name == null) return "";
        String clean = name.trim().toLowerCase(java.util.Locale.ENGLISH);
        if (clean.startsWith("/")) clean = clean.substring(1);
        int space = clean.indexOf(' ');
        if (space >= 0) clean = clean.substring(0, space);
        clean = clean.replaceAll("[^a-z0-9_-]", "");
        return clean.length() > 24 ? clean.substring(0, 24) : clean;
    }

    public static String cleanFunctionId(String name) {
        if (name == null) return "";
        String clean = name.trim().toLowerCase(java.util.Locale.ENGLISH);
        if (clean.startsWith("/")) clean = clean.substring(1);
        int space = clean.indexOf(' ');
        if (space >= 0) clean = clean.substring(0, space);
        clean = clean.replaceAll("[^a-z0-9_-]", "");
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    public static class TeleportPad {
        private final String id;
        private String name;
        private final String location;
        private String linkedPadId;
        private boolean particles;
        private boolean sound;

        public TeleportPad(String id, String name, String location, String linkedPadId, boolean particles, boolean sound) {
            this.id = id;
            this.name = name == null || name.trim().isEmpty() ? "Teleport Pad" : name;
            this.location = location;
            this.linkedPadId = linkedPadId == null ? "" : linkedPadId;
            this.particles = particles;
            this.sound = sound;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name == null || name.trim().isEmpty() ? "Teleport Pad" : (name.length() > 32 ? name.substring(0, 32) : name); }
        public String getLocation() { return location; }
        public String getLinkedPadId() { return linkedPadId; }
        public void setLinkedPadId(String linkedPadId) { this.linkedPadId = linkedPadId == null ? "" : linkedPadId; }
        public boolean isParticles() { return particles; }
        public void setParticles(boolean particles) { this.particles = particles; }
        public boolean isSound() { return sound; }
        public void setSound(boolean sound) { this.sound = sound; }
    }

    public String getGroupColor(String group) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        String color = groupColors.get(clean);
        return color == null ? net.fakepixel.housing.group.HouseGroups.defaultColor(clean) : color;
    }

    public void setGroupColor(String group, String color) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        groupColors.put(clean, normalizeColor(color, net.fakepixel.housing.group.HouseGroups.defaultColor(clean)));
    }

    public String groupSuffix(String group) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        return getGroupColor(clean) + "[" + getGroupDisplayName(clean) + "]";
    }

    public String getGroupDisplayName(String group) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        String custom = groupDisplayNames.get(clean);
        return custom == null || custom.trim().isEmpty() ? net.fakepixel.housing.group.HouseGroups.displayName(clean) : custom;
    }

    public boolean setGroupDisplayName(String group, String name) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        if (net.fakepixel.housing.group.HouseGroups.OWNER.equals(clean)) return false;
        if (name == null || name.trim().isEmpty()) return false;
        groupDisplayNames.put(clean, name.trim());
        return true;
    }

    public String getGroupTag(String group) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        return groupTags.getOrDefault(clean, "");
    }

    public boolean setGroupTag(String group, String tag) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        if (net.fakepixel.housing.group.HouseGroups.OWNER.equals(clean)) return false;
        groupTags.put(clean, tag == null ? "" : tag.trim());
        return true;
    }

    public boolean addCustomGroup(String group) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        if (clean.equals(net.fakepixel.housing.group.HouseGroups.OWNER)
                || clean.equals(net.fakepixel.housing.group.HouseGroups.CO_OWNER)
                || clean.equals(net.fakepixel.housing.group.HouseGroups.RESIDENT)
                || clean.equals(net.fakepixel.housing.group.HouseGroups.GUEST)) return false;
        boolean added = customGroups.add(clean);
        if (added) {
            groupPermissions.put(clean, new HashSet<>(defaultPermissions(clean)));
            groupColors.put(clean, net.fakepixel.housing.group.HouseGroups.defaultColor(clean));
        }
        return added;
    }

    public boolean removeCustomGroup(String group) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        groups.entrySet().removeIf(uuidStringEntry -> net.fakepixel.housing.group.HouseGroups.normalize(uuidStringEntry.getValue()).equals(clean));
        groupPermissions.remove(clean);
        groupColors.remove(clean);
        return customGroups.remove(clean);
    }

    public boolean isBanned(UUID uuid) { return banned.contains(uuid); }
    public boolean isMuted(UUID uuid) { return muted.contains(uuid); }
    public String getGroup(UUID uuid) {
        if (owner.equals(uuid)) return net.fakepixel.housing.group.HouseGroups.OWNER;
        return groups.getOrDefault(uuid, net.fakepixel.housing.group.HouseGroups.GUEST);
    }
    public void setGroup(UUID uuid, String group) {
        if (owner.equals(uuid)) return;
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        if (net.fakepixel.housing.group.HouseGroups.GUEST.equals(clean)) {
            groups.remove(uuid);
            trusted.remove(uuid);
        } else {
            groups.put(uuid, clean);
            if (hasGroupPermission(clean, "build") || net.fakepixel.housing.group.HouseGroups.RESIDENT.equals(clean) || net.fakepixel.housing.group.HouseGroups.CO_OWNER.equals(clean)) {
                trusted.add(uuid);
            } else {
                trusted.remove(uuid);
            }
        }
    }

    public boolean canManage(UUID uuid) {
        if (owner.equals(uuid)) return true;
        return hasGroupPermission(getGroup(uuid), "manage") || net.fakepixel.housing.group.HouseGroups.canManage(getGroup(uuid));
    }

    public boolean giveCookie(UUID uuid) {
        return giveCookie(uuid, 1);
    }

    public boolean giveCookie(UUID uuid, int amount) {
        if (owner.equals(uuid) || cookieGivers.contains(uuid)) return false;
        cookieGivers.add(uuid);
        cookies += Math.max(1, amount);
        return true;
    }

    public boolean canBuild(UUID uuid) {
        if (owner.equals(uuid)) return true;
        return hasGroupPermission(getGroup(uuid), "build");
    }

    public boolean hasGroupPermission(UUID uuid, String permission) {
        if (owner.equals(uuid)) return true;
        return hasGroupPermission(getGroup(uuid), permission);
    }

    public boolean hasGroupPermission(String group, String permission) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        if (net.fakepixel.housing.group.HouseGroups.OWNER.equals(clean)) return true;
        return permissionsFor(clean).contains(normalizePermission(permission));
    }

    public boolean setGroupPermission(String group, String permission, boolean enabled) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        if (net.fakepixel.housing.group.HouseGroups.OWNER.equals(clean)) return false;
        Set<String> perms = permissionsFor(clean);
        String perm = normalizePermission(permission);
        if (enabled) perms.add(perm); else perms.remove(perm);
        groupPermissions.put(clean, perms);
        return true;
    }

    public boolean toggleGroupPermission(String group, String permission) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        if (net.fakepixel.housing.group.HouseGroups.OWNER.equals(clean)) return false;
        String perm = normalizePermission(permission);
        boolean enabled = !hasGroupPermission(clean, perm);
        setGroupPermission(clean, perm, enabled);
        return enabled;
    }

    private Set<String> permissionsFor(String group) {
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        Set<String> perms = groupPermissions.get(clean);
        if (perms == null) {
            perms = new HashSet<>(defaultPermissions(clean));
            groupPermissions.put(clean, perms);
        }
        return perms;
    }

    private Set<String> defaultPermissions(String group) {
        Set<String> perms = new HashSet<>();
        String clean = net.fakepixel.housing.group.HouseGroups.normalize(group);
        if (net.fakepixel.housing.group.HouseGroups.OWNER.equals(clean) || net.fakepixel.housing.group.HouseGroups.CO_OWNER.equals(clean)) {
            perms.add("gamemode"); perms.add("build"); perms.add("kick"); perms.add("mute"); perms.add("ban"); perms.add("chat"); perms.add("teleport"); perms.add("fly"); perms.add("manage"); perms.add("settings");
        } else if (net.fakepixel.housing.group.HouseGroups.RESIDENT.equals(clean)) {
            perms.add("build"); perms.add("chat"); perms.add("teleport");
        } else if (net.fakepixel.housing.group.HouseGroups.GUEST.equals(clean)) {
            perms.add("chat");
        } else {
            perms.add("chat");
        }
        return perms;
    }

    private String normalizePermission(String permission) {
        return permission == null ? "" : permission.trim().toLowerCase().replace(' ', '-').replace('_', '-');
    }

    private String cleanMessage(String message, String fallback) {
        if (message == null || message.trim().isEmpty()) return fallback;
        return message.length() > 160 ? message.substring(0, 160) : message;
    }

    private static String cleanJukeboxSong(String song) {
        if (song == null) return "";
        String clean = song.trim().toLowerCase(java.util.Locale.ENGLISH).replace(" ", "-").replace("_", "-");
        if (clean.length() > 32) clean = clean.substring(0, 32);
        return clean;
    }

    public static String normalizeMode(String mode) {
        if (mode == null) return "SOCIAL";
        String clean = mode.trim().toUpperCase(java.util.Locale.ENGLISH).replace("-", "_").replace(" ", "_");
        if (clean.equals("BUILD") || clean.equals("BUILD_MODE")) return "BUILD";
        return "SOCIAL";
    }

    private static int normalizeJukeboxVolume(int volume) {
        if (volume < 0) return 0;
        return Math.min(volume, 100);
    }

    private static int normalizeScoreboardUpdateTicks(int ticks) {
        if (ticks < 10) return 10;
        return Math.min(ticks, 200);
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }

    public static String normalizeVisitingRule(String rule) {
        if (rule == null) return "public";
        String clean = rule.trim().toLowerCase(java.util.Locale.ENGLISH).replace(" ", "-").replace("_", "-");
        if (clean.equals("friend") || clean.equals("friends")) return "friends";
        if (clean.equals("party")) return "party";
        if (clean.equals("guild")) return "guild";
        if (clean.equals("private")) return "private";
        return "public";
    }

    public String getVisitingRuleDisplay() {
        if ("friends".equals(visitingRule)) return "FRIENDS";
        if ("party".equals(visitingRule)) return "PARTY";
        if ("guild".equals(visitingRule)) return "GUILD";
        if ("private".equals(visitingRule)) return "PRIVATE";
        return "PUBLIC";
    }

    private static String normalizeWeather(String weather) {
        if (weather == null) return "sunny";
        String clean = weather.trim().toLowerCase().replace(' ', '-').replace('_', '-');
        if (clean.equals("raining") || clean.equals("rain")) return "raining";
        if (clean.equals("storming") || clean.equals("storm")) return "storming";
        if (clean.equals("thunder") || clean.equals("thundering")) return "thunder";
        return "sunny";
    }


    private static String normalizeBiome(String biome) {
        if (biome == null) return "plains";
        String clean = biome.trim().toLowerCase().replace(' ', '-').replace('_', '-');
        if (clean.equals("roofed-forest") || clean.equals("dark-forest")) return "roofed-forest";
        if (clean.equals("cold-taiga") || clean.equals("snowy-taiga")) return "cold-taiga";
        if (clean.equals("mesa") || clean.equals("badlands")) return "mesa";
        if (clean.equals("hell") || clean.equals("nether")) return "hell";
        if (clean.equals("sky") || clean.equals("end") || clean.equals("the-end")) return "sky";
        if (clean.equals("jungle")) return "jungle";
        if (clean.equals("forest")) return "forest";
        if (clean.equals("taiga")) return "taiga";
        if (clean.equals("desert")) return "desert";
        return "plains";
    }

    private static String normalizeSky(String sky) {
        if (sky == null) return "overworld";
        String clean = sky.trim().toLowerCase().replace(' ', '-').replace('_', '-');
        if (clean.equals("nether") || clean.equals("hell")) return "nether";
        if (clean.equals("end") || clean.equals("the-end") || clean.equals("sky")) return "end";
        return "overworld";
    }


    private static String normalizeTag(String tag) {
        if (tag == null) return "new-houses";
        String clean = tag.trim().toLowerCase().replace("_", "-").replace(" ", "-");
        if (clean.equals("roleplay") || clean.equals("rp")) return "roleplay";
        if (clean.equals("parkour")) return "parkour";
        if (clean.equals("building")) return "building";
        if (clean.equals("chatting") || clean.equals("chat")) return "chatting";
        if (clean.equals("maze")) return "maze";
        if (clean.equals("competition")) return "competition";
        if (clean.equals("event")) return "event";
        if (clean.equals("guild-hangout") || clean.equals("guildhangout")) return "guild-hangout";
        if (clean.equals("minigame")) return "minigame";
        if (clean.equals("new") || clean.equals("new-houses") || clean.equals("newhouses")) return "new-houses";
        return clean;
    }

    private static int normalizePlotSize(int size) {
        if (size >= 255) return 255;
        if (size >= 151) return 151;
        if (size >= 101) return 101;
        if (size >= 51) return 51;
        if (size >= 41) return 41;
        return 31;
    }

    private static String normalizeTheme(String theme) {
        if (theme == null) return "default";
        String clean = theme.trim();
        if (clean.isEmpty()) return "default";
        clean = clean.replace(' ', '_').replaceAll("[^A-Za-z0-9_-]", "");
        return clean.isEmpty() ? "default" : clean;
    }

    private String normalizeColor(String color, String fallback) {
        if (color == null || color.trim().isEmpty()) return fallback;
        String c = color.trim();
        if (c.length() == 1) return "&" + c;
        if (c.length() == 2 && (c.charAt(0) == '&' || c.charAt(0) == '§')) return "&" + c.charAt(1);
        return fallback;
    }

    public static class Hologram {
        private final String id;
        private String location;
        private final java.util.List<String> lines;
        private final java.util.List<String> entityIds;
        private boolean visible;
        private String type;
        private String linkedObjectId;

        public Hologram(String id, String location, java.util.List<String> lines, boolean visible, java.util.List<String> entityIds) {
            this(id, location, lines, visible, entityIds, "CUSTOM", "");
        }

        public Hologram(String id, String location, java.util.List<String> lines, boolean visible, java.util.List<String> entityIds, String type, String linkedObjectId) {
            this.id = id;
            this.location = location;
            this.lines = new java.util.ArrayList<>(lines == null || lines.isEmpty() ? java.util.Collections.singletonList("&eHologram") : lines);
            this.entityIds = new java.util.ArrayList<>(entityIds == null ? java.util.Collections.emptyList() : entityIds);
            this.visible = visible;
            this.type = type == null || type.trim().isEmpty() ? "CUSTOM" : type;
            this.linkedObjectId = linkedObjectId == null ? "" : linkedObjectId;
        }

        public String getId() { return id; }
        public String getLocation() { return location; }
        public void setLocation(String location) { if (location != null && !location.trim().isEmpty()) this.location = location; }
        public java.util.List<String> getLines() { return lines; }
        public void setLines(java.util.List<String> newLines) {
            lines.clear();
            if (newLines == null || newLines.isEmpty()) lines.add("&eHologram");
            else lines.addAll(newLines);
        }
        public java.util.List<String> getEntityIds() { return entityIds; }
        public void setEntityIds(java.util.List<String> ids) {
            entityIds.clear();
            if (ids != null) entityIds.addAll(ids);
        }
        public boolean isVisible() { return visible; }
        public String getType() { return type; }
        public String getLinkedObjectId() { return linkedObjectId; }
        public void setIdentity(String type, String linkedObjectId) {
            this.type = type == null || type.trim().isEmpty() ? "CUSTOM" : type;
            this.linkedObjectId = linkedObjectId == null ? "" : linkedObjectId;
        }
        public void setVisible(boolean visible) { this.visible = visible; }
        public void setLine(int index, String text) {
            if (index < 0 || index >= lines.size()) return;
            lines.set(index, text == null || text.trim().isEmpty() ? "&f" : text);
        }
        public void addLine(String text) { lines.add(text == null || text.trim().isEmpty() ? "&fNew Line" : text); }
        public void removeLastLine() { if (lines.size() > 1) lines.remove(lines.size() - 1); }
    }

    public static class HouseNpc {
        private final String id;
        private String name;
        private String skin;
        private String action;
        private final java.util.List<String> messages;
        private String location;
        private String teleportLocation;
        private boolean lookAtPlayers;
        private boolean visible;

        public HouseNpc(String id, String name, String skin, String action, java.util.List<String> messages, String location, String teleportLocation, boolean lookAtPlayers, boolean visible) {
            this.id = id;
            this.name = cleanText(name, "&aNPC", 48);
            this.skin = skin == null ? "" : skin;
            this.action = normalizeNpcAction(action);
            this.messages = new java.util.ArrayList<>(messages == null || messages.isEmpty() ? java.util.Collections.singletonList("&eHello, %player%!") : messages);
            this.location = location;
            this.teleportLocation = teleportLocation == null ? "" : teleportLocation;
            this.lookAtPlayers = lookAtPlayers;
            this.visible = visible;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = cleanText(name, "&aNPC", 48); }
        public String getSkin() { return skin; }
        public void setSkin(String skin) { this.skin = skin == null ? "" : cleanText(skin, "", 32); }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = normalizeNpcAction(action); }
        public java.util.List<String> getMessages() { return messages; }
        public void setMessages(java.util.List<String> lines) {
            messages.clear();
            if (lines != null) {
                for (String line : lines) if (line != null && !line.trim().isEmpty()) messages.add(cleanText(line, "", 160));
            }
            if (messages.isEmpty()) messages.add("&eHello, %player%!");
        }
        public String getLocation() { return location; }
        public void setLocation(String location) { if (location != null && !location.trim().isEmpty()) this.location = location; }
        public String getTeleportLocation() { return teleportLocation; }
        public void setTeleportLocation(String teleportLocation) { this.teleportLocation = teleportLocation == null ? "" : teleportLocation; }
        public boolean isLookAtPlayers() { return lookAtPlayers; }
        public void setLookAtPlayers(boolean lookAtPlayers) { this.lookAtPlayers = lookAtPlayers; }
        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }

        private static String cleanText(String text, String fallback, int max) {
            if (text == null || text.trim().isEmpty()) return fallback;
            return text.length() > max ? text.substring(0, max) : text;
        }
    }

    public static class ActionButton {
        private final String id;
        private String name;
        private final String location;
        private long cooldownMs;
        private boolean oneTime;
        private boolean particles;
        private boolean sound;
        private final java.util.List<ButtonAction> actions = new java.util.ArrayList<>();
        private final Set<UUID> usedBy = new HashSet<>();

        public ActionButton(String id, String name, String location, long cooldownMs, boolean oneTime, boolean particles, boolean sound) {
            this.id = id;
            this.name = name == null || name.trim().isEmpty() ? "Action Button" : (name.length() > 32 ? name.substring(0, 32) : name);
            this.location = location;
            this.cooldownMs = Math.max(0L, cooldownMs);
            this.oneTime = oneTime;
            this.particles = particles;
            this.sound = sound;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name == null || name.trim().isEmpty() ? "Action Button" : (name.length() > 32 ? name.substring(0, 32) : name); }
        public String getLocation() { return location; }
        public long getCooldownMs() { return cooldownMs; }
        public void setCooldownMs(long cooldownMs) { this.cooldownMs = Math.max(0L, cooldownMs); }
        public boolean isOneTime() { return oneTime; }
        public void setOneTime(boolean oneTime) { this.oneTime = oneTime; }
        public boolean isParticles() { return particles; }
        public void setParticles(boolean particles) { this.particles = particles; }
        public boolean isSound() { return sound; }
        public void setSound(boolean sound) { this.sound = sound; }
        public java.util.List<ButtonAction> getActions() { return actions; }
        public Set<UUID> getUsedBy() { return usedBy; }
    }

    public static class ActionPad {
        private final String id;
        private String name;
        private final String location;
        private long cooldownMs;
        private boolean oneTime;
        private boolean particles;
        private boolean sound;
        private final java.util.List<ButtonAction> actions = new java.util.ArrayList<>();
        private final Set<UUID> usedBy = new HashSet<>();

        public ActionPad(String id, String name, String location, long cooldownMs, boolean oneTime, boolean particles, boolean sound) {
            this.id = id;
            this.name = name == null || name.trim().isEmpty() ? "&aAction Pad" : (name.length() > 32 ? name.substring(0, 32) : name);
            this.location = location;
            this.cooldownMs = Math.max(0L, cooldownMs);
            this.oneTime = oneTime;
            this.particles = particles;
            this.sound = sound;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name == null || name.trim().isEmpty() ? "&aAction Pad" : (name.length() > 32 ? name.substring(0, 32) : name); }
        public String getLocation() { return location; }
        public long getCooldownMs() { return cooldownMs; }
        public void setCooldownMs(long cooldownMs) { this.cooldownMs = Math.max(0L, cooldownMs); }
        public boolean isOneTime() { return oneTime; }
        public void setOneTime(boolean oneTime) { this.oneTime = oneTime; }
        public boolean isParticles() { return particles; }
        public void setParticles(boolean particles) { this.particles = particles; }
        public boolean isSound() { return sound; }
        public void setSound(boolean sound) { this.sound = sound; }
        public java.util.List<ButtonAction> getActions() { return actions; }
        public Set<UUID> getUsedBy() { return usedBy; }
    }

    public static class ButtonAction {
        private String type;
        private String value;

        public ButtonAction(String type, String value) {
            this.type = normalizeButtonAction(type);
            this.value = value == null ? "" : value;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = normalizeButtonAction(type); }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value == null ? "" : value; }
    }

    public static class CustomCommand {
        private String name;
        private boolean enabled;
        private boolean guestsAllowed;
        private final java.util.List<String> aliases = new java.util.ArrayList<>();
        private long cooldownMs;
        private final java.util.List<ButtonAction> actions = new java.util.ArrayList<>();

        public CustomCommand(String name, boolean enabled, java.util.List<String> aliases, long cooldownMs, boolean guestsAllowed) {
            this.name = cleanCommandName(name);
            this.enabled = enabled;
            this.guestsAllowed = guestsAllowed;
            setAliases(aliases);
            this.cooldownMs = Math.max(0L, cooldownMs);
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = cleanCommandName(name); }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isGuestsAllowed() { return guestsAllowed; }
        public void setGuestsAllowed(boolean guestsAllowed) { this.guestsAllowed = guestsAllowed; }
        public java.util.List<String> getAliases() { return aliases; }
        public void setAliases(java.util.List<String> values) {
            aliases.clear();
            if (values == null) return;
            for (String value : values) {
                String clean = cleanCommandName(value);
                if (!clean.isEmpty() && !clean.equals(name) && !aliases.contains(clean)) aliases.add(clean);
            }
        }
        public boolean hasAlias(String alias) { return aliases.contains(cleanCommandName(alias)); }
        public long getCooldownMs() { return cooldownMs; }
        public void setCooldownMs(long cooldownMs) { this.cooldownMs = Math.max(0L, cooldownMs); }
        public java.util.List<ButtonAction> getActions() { return actions; }
    }

    public static class Function {
        private String id;
        private String displayName;
        private long cooldownMs;
        private final java.util.List<ButtonAction> actions = new java.util.ArrayList<>();

        public Function(String id, String displayName, long cooldownMs) {
            this.id = cleanFunctionId(id);
            this.displayName = displayName == null || displayName.trim().isEmpty() ? this.id : displayName;
            this.cooldownMs = Math.max(0L, cooldownMs);
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = cleanFunctionId(id); }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName == null || displayName.trim().isEmpty() ? id : displayName; }
        public long getCooldownMs() { return cooldownMs; }
        public void setCooldownMs(long cooldownMs) { this.cooldownMs = Math.max(0L, cooldownMs); }
        public java.util.List<ButtonAction> getActions() { return actions; }
    }

    public static class Region {
        private final String id;
        private String name;
        private final String world;
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;
        private int priority;
        private boolean pvp = false;
        private boolean build = false;
        private boolean blockBreaking = false;
        private boolean fly = false;
        private boolean damage = false;
        private boolean fallDamage = false;
        private boolean hunger = false;
        private String potionEffects = "";
        private String gameMode = "";
        private boolean applyPvpKit = false;
        private boolean allowProjectiles = false;
        private boolean allowItemDrops = false;
        private boolean allowItemPickup = false;
        private final java.util.List<ButtonAction> enterActions = new java.util.ArrayList<>();
        private final java.util.List<ButtonAction> leaveActions = new java.util.ArrayList<>();
        private final java.util.List<ButtonAction> whileInsideActions = new java.util.ArrayList<>();

        public Region(String id, String name, String world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, int priority) {
            this.id = cleanRegionId(id);
            this.name = cleanRegionName(name);
            this.world = world;
            this.minX = Math.min(minX, maxX);
            this.maxX = Math.max(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.maxY = Math.max(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxZ = Math.max(minZ, maxZ);
            this.priority = priority;
        }

        public boolean contains(Location location) {
            if (location == null || location.getWorld() == null || !location.getWorld().getName().equals(world)) return false;
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = cleanRegionName(name); }
        public String getWorld() { return world; }
        public int getMinX() { return minX; }
        public int getMaxX() { return maxX; }
        public int getMinY() { return minY; }
        public int getMaxY() { return maxY; }
        public int getMinZ() { return minZ; }
        public int getMaxZ() { return maxZ; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public boolean isPvp() { return pvp; }
        public void setPvp(boolean pvp) { this.pvp = pvp; }
        public boolean isBuild() { return build; }
        public void setBuild(boolean build) { this.build = build; }
        public boolean isBlockBreaking() { return blockBreaking; }
        public void setBlockBreaking(boolean blockBreaking) { this.blockBreaking = blockBreaking; }
        public boolean isFly() { return fly; }
        public void setFly(boolean fly) { this.fly = fly; }
        public boolean isDamage() { return damage; }
        public void setDamage(boolean damage) { this.damage = damage; }
        public boolean isFallDamage() { return fallDamage; }
        public void setFallDamage(boolean fallDamage) { this.fallDamage = fallDamage; }
        public boolean isHunger() { return hunger; }
        public void setHunger(boolean hunger) { this.hunger = hunger; }
        public String getPotionEffects() { return potionEffects; }
        public void setPotionEffects(String potionEffects) { this.potionEffects = potionEffects == null ? "" : potionEffects; }
        public String getGameMode() { return gameMode; }
        public void setGameMode(String gameMode) { this.gameMode = gameMode == null ? "" : gameMode.trim().toUpperCase(java.util.Locale.ENGLISH); }
        public boolean isApplyPvpKit() { return applyPvpKit; }
        public void setApplyPvpKit(boolean applyPvpKit) { this.applyPvpKit = applyPvpKit; }
        public boolean isAllowProjectiles() { return allowProjectiles; }
        public void setAllowProjectiles(boolean allowProjectiles) { this.allowProjectiles = allowProjectiles; }
        public boolean isAllowItemDrops() { return allowItemDrops; }
        public void setAllowItemDrops(boolean allowItemDrops) { this.allowItemDrops = allowItemDrops; }
        public boolean isAllowItemPickup() { return allowItemPickup; }
        public void setAllowItemPickup(boolean allowItemPickup) { this.allowItemPickup = allowItemPickup; }
        public java.util.List<ButtonAction> getEnterActions() { return enterActions; }
        public java.util.List<ButtonAction> getLeaveActions() { return leaveActions; }
        public java.util.List<ButtonAction> getWhileInsideActions() { return whileInsideActions; }
    }
}
