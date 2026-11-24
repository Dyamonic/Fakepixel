package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.guilds.GuildTagColor;
import club.abelix.games.fakevival.guilds.GuildPermission;
import club.abelix.games.fakevival.guilds.GuildRoleDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.sql.PreparedStatement;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class GuildManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;

    public GuildManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        migrateGuildTagColors();
        migrateGuildManagementData();
    }

    public boolean hasGuild(Player player) {
        return getGuildId(player) != null;
    }

    public String getGuildId(Player player) {
        return dataManager.getData().getString(playerPath(player) + ".guild");
    }

    public String getGuildName(Player player) {
        String guildId = getGuildId(player);
        if (guildId == null) return null;
        return getGuildName(guildId);
    }

    public String getGuildName(String guildId) {
        return guildId == null ? null : dataManager.getData().getString(guildPath(guildId) + ".name", guildId);
    }

    public boolean isOwner(Player player) {
        String guildId = getGuildId(player);
        return guildId != null && player.getUniqueId().toString().equals(dataManager.getData().getString(guildPath(guildId) + ".owner"));
    }

    public boolean canManage(Player player) {
        return player.hasPermission("fakevival.guild.admin") || isOwner(player) || hasPermission(player, GuildPermission.EDIT_SETTINGS);
    }

    public String getRank(Player player) {
        String guildId = getGuildId(player);
        if (guildId == null) return "";
        if (isOwner(player)) return "owner";
        return dataManager.getData().getString(guildPath(guildId) + ".ranks." + player.getUniqueId(), "member").toLowerCase(Locale.ROOT);
    }

    public boolean createGuild(Player player, String rawName) {
        if (hasGuild(player)) return false;
        String name = sanitizeName(rawName == null || rawName.isBlank() ? player.getName() + " Guild" : rawName);
        String guildId = normalize(name);
        if (guildId.isBlank()) guildId = player.getUniqueId().toString().substring(0, 8);
        if (dataManager.getData().isConfigurationSection(guildPath(guildId))) guildId = guildId + "_" + player.getUniqueId().toString().substring(0, 4).toLowerCase(Locale.ROOT);

        dataManager.getData().set(guildPath(guildId) + ".name", name);
        dataManager.getData().set(guildPath(guildId) + ".owner", player.getUniqueId().toString());
        dataManager.getData().set(guildPath(guildId) + ".open", true);
        dataManager.getData().set(guildPath(guildId) + ".motd", "Welcome to " + name + "!");
        dataManager.getData().set(guildPath(guildId) + ".tag", sanitizeTag(name));
        dataManager.getData().set(guildPath(guildId) + ".guild_tag_color", GuildTagColor.GRAY.name());
        dataManager.getData().set(guildPath(guildId) + ".members." + player.getUniqueId(), player.getName());
        dataManager.getData().set(guildPath(guildId) + ".ranks." + player.getUniqueId(), "owner");
        initializeGuild(guildId, player.getUniqueId(), player.getName());
        dataManager.getData().set(playerPath(player) + ".guild", guildId);
        dataManager.save();
        dataManager.saveGuildTagColorAsync(guildId, GuildTagColor.GRAY.name());
        return true;
    }

    public List<String> getGuildIds() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("guilds");
        if (section == null) return List.of();
        List<String> ids = new ArrayList<>(section.getKeys(false));
        ids.sort(Comparator.comparing(this::getGuildName, String.CASE_INSENSITIVE_ORDER));
        return ids;
    }

    public List<String> getOpenGuildIds() {
        List<String> open = new ArrayList<>();
        for (String id : getGuildIds()) if (isOpen(id)) open.add(id);
        return open;
    }

    public String findGuildId(String raw) {
        String normalized = normalize(raw);
        for (String id : getGuildIds()) {
            if (id.equalsIgnoreCase(normalized) || getGuildName(id).equalsIgnoreCase(raw)) return id;
        }
        return null;
    }

    public boolean isOpen(String guildId) {
        return getJoinType(guildId).equals("PUBLIC");
    }

    public void setOpen(Player actor, boolean open) {
        String guildId = getGuildId(actor);
        if (guildId == null || !canManage(actor)) return;
        dataManager.getData().set(guildPath(guildId) + ".open", open);
        dataManager.getData().set(guildPath(guildId) + ".settings.join-type", open ? "PUBLIC" : "INVITE_ONLY");
        dataManager.save();
        persistSettings(guildId);
    }

    public String getMotd(String guildId) {
        return dataManager.getData().getString(guildPath(guildId) + ".motd", "");
    }

    public boolean setMotd(Player player, String motd) {
        String guildId = getGuildId(player);
        if (guildId == null || !hasPermission(player, GuildPermission.EDIT_MOTD)) return false;
        dataManager.getData().set(guildPath(guildId) + ".motd", sanitizeMotd(motd));
        dataManager.save();
        return true;
    }

    public List<UUID> getMembers(String guildId) {
        ConfigurationSection section = dataManager.getData().getConfigurationSection(guildPath(guildId) + ".members");
        if (section == null) return List.of();
        List<UUID> members = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            try {
                members.add(UUID.fromString(key));
            } catch (Exception ignored) {
            }
        }
        return members;
    }

    public int getMemberCount(String guildId) {
        return getMembers(guildId).size();
    }

    public String getMemberName(UUID uuid) {
        String guildId = getGuildId(uuid);
        String stored = guildId == null ? null : dataManager.getData().getString(guildPath(guildId) + ".members." + uuid);
        if (stored != null && !stored.isBlank()) return stored;
        Player online = Bukkit.getPlayer(uuid);
        return online == null ? uuid.toString().substring(0, 8) : online.getName();
    }

    public boolean invite(Player actor, Player target) {
        String guildId = getGuildId(actor);
        if (guildId == null || !hasPermission(actor, GuildPermission.INVITE_MEMBERS) || hasGuild(target)) return false;
        dataManager.getData().set(guildPath(guildId) + ".invites." + target.getUniqueId(), System.currentTimeMillis());
        dataManager.save();
        return true;
    }

    public JoinResult join(Player player, String rawGuild) {
        if (hasGuild(player)) return JoinResult.ALREADY_IN_GUILD;
        String guildId = findGuildId(rawGuild);
        if (guildId == null) return JoinResult.NOT_FOUND;
        String joinType = getJoinType(guildId);
        if (hasInvite(player, guildId) || joinType.equals("PUBLIC")) {
            addMember(player, guildId);
            return JoinResult.JOINED;
        }
        if (joinType.equals("PRIVATE") || joinType.equals("INVITE_ONLY")) return JoinResult.INVITE_REQUIRED;
        dataManager.getData().set(guildPath(guildId) + ".requests." + player.getUniqueId(), player.getName());
        dataManager.save();
        return JoinResult.REQUESTED;
    }

    public boolean accept(Player actor, OfflinePlayer target) {
        String guildId = getGuildId(actor);
        if (guildId == null || !hasPermission(actor, GuildPermission.INVITE_MEMBERS) || target == null) return false;
        if (!dataManager.getData().contains(guildPath(guildId) + ".requests." + target.getUniqueId())) return false;
        dataManager.getData().set(guildPath(guildId) + ".requests." + target.getUniqueId(), null);
        addMember(target, guildId);
        return true;
    }

    public boolean leave(Player player) {
        String guildId = getGuildId(player);
        if (guildId == null || isOwner(player)) return false;
        removeMember(player.getUniqueId(), guildId);
        return true;
    }

    public boolean disband(Player player) {
        String guildId = getGuildId(player);
        if (guildId == null || !isOwner(player)) return false;
        for (UUID member : getMembers(guildId)) {
            dataManager.getData().set("players." + member + ".guild", null);
        }
        for (String claim : new ArrayList<>(getClaimKeys(guildId))) {
            dataManager.getData().set("guild-claims." + claim, null);
        }
        dataManager.getData().set(guildPath(guildId), null);
        dataManager.save();
        return true;
    }

    public boolean kick(Player actor, OfflinePlayer target) {
        String guildId = getGuildId(actor);
        if (guildId == null || !hasPermission(actor, GuildPermission.KICK_MEMBERS) || target == null) return false;
        if (target.getUniqueId().equals(actor.getUniqueId()) || target.getUniqueId().toString().equals(dataManager.getData().getString(guildPath(guildId) + ".owner"))) return false;
        if (!isMember(target.getUniqueId(), guildId)) return false;
        removeMember(target.getUniqueId(), guildId);
        return true;
    }

    public boolean setRank(Player actor, OfflinePlayer target, String rank) {
        String guildId = getGuildId(actor);
        rank = normalizeRank(rank);
        if (guildId == null || !isOwner(actor) || target == null || rank.equals("owner") || rank.isBlank()) return false;
        if (!isMember(target.getUniqueId(), guildId)) return false;
        dataManager.getData().set(guildPath(guildId) + ".ranks." + target.getUniqueId(), rank);
        dataManager.save();
        return true;
    }

    public boolean isMember(UUID uuid, String guildId) {
        return dataManager.getData().contains(guildPath(guildId) + ".members." + uuid);
    }

    public String getRank(String guildId, UUID uuid) {
        if (uuid.toString().equals(dataManager.getData().getString(guildPath(guildId) + ".owner"))) return "owner";
        return dataManager.getData().getString(guildPath(guildId) + ".ranks." + uuid, "member");
    }

    public String getGuildTag(Player player) {
        String guildId = getGuildId(player);
        return guildId == null ? "" : formatGuildTag(guildId);
    }

    public String getTag(String guildId) {
        return guildId == null ? "" : sanitizeTag(dataManager.getData().getString(guildPath(guildId) + ".tag", ""));
    }

    public String getTagColor(String guildId) {
        return getTagColorValue(guildId).code();
    }

    public String getTagColorId(String guildId) {
        return getTagColorValue(guildId).name();
    }

    public String getTagColorId(Player player) {
        String guildId = getGuildId(player);
        return guildId == null ? GuildTagColor.GRAY.name() : getTagColorId(guildId);
    }

    public String getRawGuildTag(Player player) {
        String guildId = getGuildId(player);
        return guildId == null ? "" : getTag(guildId);
    }

    public String formatGuildTag(String guildId) {
        String tag = getTag(guildId);
        if (tag.isBlank()) return "";
        return "&7[" + getTagColor(guildId) + tag + "&7]";
    }

    public TagResult setTag(Player player, String rawTag) {
        String guildId = getGuildId(player);
        if (guildId == null) return TagResult.NO_GUILD;
        if (!hasPermission(player, GuildPermission.EDIT_GUILD_TAG)) return TagResult.NOT_OWNER;
        String tag = sanitizeTag(rawTag);
        if (tag.isBlank()) return TagResult.INVALID;
        if (isBlockedTag(tag)) return TagResult.BLOCKED;
        dataManager.getData().set(guildPath(guildId) + ".tag", tag);
        dataManager.save();
        return TagResult.UPDATED;
    }

    public TagResult setTagColor(Player player, String color) {
        String guildId = getGuildId(player);
        if (guildId == null) return TagResult.NO_GUILD;
        if (!hasPermission(player, GuildPermission.EDIT_TAG_COLOR)) return TagResult.NOT_OWNER;
        GuildTagColor parsed = GuildTagColor.parse(color).orElse(null);
        if (parsed == null) return TagResult.INVALID;
        dataManager.getData().set(guildPath(guildId) + ".guild_tag_color", parsed.name());
        dataManager.getData().set(guildPath(guildId) + ".tag-color", null);
        dataManager.save();
        dataManager.saveGuildTagColorAsync(guildId, parsed.name());
        return TagResult.UPDATED;
    }

    public int getClaimLimit(Player player) {
        int base = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("guilds.base-claim-limit", 3));
        int extra = dataManager.getData().getInt("players." + player.getUniqueId() + ".store.upgrades.extra_guild_claims", 0);
        return base + Math.max(0, extra);
    }

    public int getClaimCount(Player player) {
        String guildId = getGuildId(player);
        if (guildId == null) return 0;
        ConfigurationSection claims = dataManager.getData().getConfigurationSection(guildPath(guildId) + ".claims");
        return claims == null ? 0 : claims.getKeys(false).size();
    }

    public List<String> getClaimKeys(Player player) {
        String guildId = getGuildId(player);
        if (guildId == null) return List.of();
        return getClaimKeys(guildId);
    }

    public List<String> getClaimKeys(String guildId) {
        ConfigurationSection claims = dataManager.getData().getConfigurationSection(guildPath(guildId) + ".claims");
        return claims == null ? List.of() : new ArrayList<>(claims.getKeys(false));
    }

    public boolean canClaimMore(Player player) {
        return getClaimCount(player) < getClaimLimit(player);
    }

    public ClaimResult claim(Player player, Location location) {
        String guildId = getGuildId(player);
        if (guildId == null) return ClaimResult.NO_GUILD;
        if (!isOwner(player)) return ClaimResult.NOT_OWNER;
        String key = chunkKey(location);
        if (key == null) return ClaimResult.INVALID;
        String existingGuild = getClaimOwner(key);
        if (existingGuild != null) {
            return existingGuild.equalsIgnoreCase(guildId) ? ClaimResult.ALREADY_YOURS : ClaimResult.ALREADY_CLAIMED;
        }
        if (!canClaimMore(player)) return ClaimResult.LIMIT_REACHED;

        dataManager.getData().set("guild-claims." + key, guildId);
        dataManager.getData().set(guildPath(guildId) + ".claims." + key, true);
        dataManager.save();
        return ClaimResult.CLAIMED;
    }

    public boolean unclaim(Player player, Location location) {
        String guildId = getGuildId(player);
        if (guildId == null || !isOwner(player)) return false;
        String key = chunkKey(location);
        if (key == null || !guildId.equalsIgnoreCase(getClaimOwner(key))) return false;
        dataManager.getData().set("guild-claims." + key, null);
        dataManager.getData().set(guildPath(guildId) + ".claims." + key, null);
        dataManager.save();
        return true;
    }

    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission("fakevival.guilds.bypass")) return true;
        String owner = getClaimOwner(chunkKey(location));
        if (owner == null) return true;
        String playerGuild = getGuildId(player);
        return playerGuild != null && playerGuild.equalsIgnoreCase(owner);
    }

    public String getClaimOwnerName(Location location) {
        String owner = getClaimOwner(chunkKey(location));
        return owner == null ? null : dataManager.getData().getString(guildPath(owner) + ".name", owner);
    }

    public String chunkKey(Location location) {
        if (location == null || location.getWorld() == null) return null;
        Chunk chunk = location.getChunk();
        return location.getWorld().getName().toLowerCase(Locale.ROOT) + ";" + chunk.getX() + ";" + chunk.getZ();
    }

    private String getClaimOwner(String key) {
        return key == null ? null : dataManager.getData().getString("guild-claims." + key);
    }

    private String sanitizeName(String name) {
        String clean = name.replace('&', ' ').replace(':', ' ').replace('.', ' ').trim();
        if (clean.length() > 24) clean = clean.substring(0, 24).trim();
        return clean.isBlank() ? "Guild" : clean;
    }

    private String sanitizeTag(String tag) {
        String clean = tag == null ? "" : tag.replaceAll("[^A-Za-z0-9]", "").trim();
        if (clean.length() > 10) clean = clean.substring(0, 10);
        return clean;
    }

    private boolean isBlockedTag(String tag) {
        String normalized = tag.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (String word : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("guilds.tag-filter.blocked-words")) {
            String blocked = word.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (!blocked.isBlank() && normalized.contains(blocked)) return true;
        }
        return false;
    }

    private GuildTagColor getTagColorValue(String guildId) {
        if (guildId == null) return GuildTagColor.GRAY;
        String path = guildPath(guildId);
        String stored = dataManager.getData().getString(path + ".guild_tag_color", "");
        if (stored.isBlank()) stored = dataManager.getData().getString(path + ".tag-color", "GRAY");
        return GuildTagColor.safe(stored);
    }

    private void migrateGuildTagColors() {
        boolean changed = false;
        for (String guildId : getGuildIds()) {
            GuildTagColor color = getTagColorValue(guildId);
            String path = guildPath(guildId);
            if (!color.name().equals(dataManager.getData().getString(path + ".guild_tag_color", ""))) {
                dataManager.getData().set(path + ".guild_tag_color", color.name());
                dataManager.getData().set(path + ".tag-color", null);
                changed = true;
            }
            dataManager.saveGuildTagColorAsync(guildId, color.name());
        }
        if (changed) dataManager.save();
    }

    private String sanitizeMotd(String motd) {
        String clean = motd == null ? "" : motd.replace('&', ' ').trim();
        if (clean.length() > 96) clean = clean.substring(0, 96).trim();
        return clean;
    }

    private boolean hasInvite(Player player, String guildId) {
        return dataManager.getData().contains(guildPath(guildId) + ".invites." + player.getUniqueId());
    }

    private void addMember(OfflinePlayer player, String guildId) {
        dataManager.getData().set(guildPath(guildId) + ".members." + player.getUniqueId(), player.getName() == null ? player.getUniqueId().toString() : player.getName());
        dataManager.getData().set(guildPath(guildId) + ".ranks." + player.getUniqueId(), "recruit");
        setMemberMetadata(guildId, player.getUniqueId(), player.getName(), "RECRUIT", System.currentTimeMillis());
        dataManager.getData().set("players." + player.getUniqueId() + ".guild", guildId);
        dataManager.save();
        persistMember(guildId, player.getUniqueId());
    }

    private void removeMember(UUID uuid, String guildId) {
        dataManager.getData().set(guildPath(guildId) + ".members." + uuid, null);
        dataManager.getData().set(guildPath(guildId) + ".ranks." + uuid, null);
        dataManager.getData().set(guildPath(guildId) + ".member-data." + uuid, null);
        dataManager.getData().set("players." + uuid + ".guild", null);
        dataManager.save();
        dataManager.runDatabaseAsync("remove-member", connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `smp_guild_members` WHERE `guild_id`=? AND `uuid`=?")) {
                statement.setString(1, guildId); statement.setString(2, uuid.toString()); statement.executeUpdate();
            }
        });
    }

    private String normalizeRank(String rank) {
        rank = rank == null ? "" : rank.toLowerCase(Locale.ROOT);
        return rank.equals("officer") || rank.equals("member") ? rank : "";
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private String playerPath(Player player) {
        return "players." + player.getUniqueId();
    }

    private String guildPath(String guildId) {
        return "guilds." + guildId.toLowerCase(Locale.ROOT);
    }

    public String getGuildId(UUID uuid) {
        return uuid == null ? null : dataManager.getData().getString("players." + uuid + ".guild");
    }

    public String getJoinType(String guildId) {
        String fallback = dataManager.getData().getBoolean(guildPath(guildId) + ".open", true) ? "PUBLIC" : "INVITE_ONLY";
        return safeChoice(dataManager.getData().getString(guildPath(guildId) + ".settings.join-type", fallback),
                Set.of("PRIVATE", "INVITE_ONLY", "REQUEST_TO_JOIN", "PUBLIC"), fallback);
    }

    public String getGuildSetting(String guildId, String key) {
        if (guildId == null || key == null) return "";
        return dataManager.getData().getString(guildPath(guildId) + ".settings." + key.toLowerCase(Locale.ROOT), "");
    }

    public boolean getGuildSettingBoolean(String guildId, String key, boolean fallback) {
        return guildId != null && dataManager.getData().getBoolean(guildPath(guildId) + ".settings." + key.toLowerCase(Locale.ROOT), fallback);
    }

    public boolean updateSetting(Player actor, String key, String value) {
        String guildId = getGuildId(actor);
        if (guildId == null || !hasPermission(actor, GuildPermission.EDIT_SETTINGS)) return false;
        key = key == null ? "" : key.toLowerCase(Locale.ROOT);
        String path = guildPath(guildId) + ".settings." + key;
        switch (key) {
            case "join-type" -> value = safeChoice(value, Set.of("PRIVATE", "INVITE_ONLY", "REQUEST_TO_JOIN", "PUBLIC"), "INVITE_ONLY");
            case "war-requests", "ally-requests" -> value = safeChoice(value, Set.of("ENABLED", "GUILD_MASTER_ONLY", "DISABLED"), "ENABLED");
            case "guild-chat", "public-member-list", "friendly-fire" -> value = String.valueOf(Boolean.parseBoolean(value));
            case "description" -> value = sanitizeText(value, 160);
            default -> { return false; }
        }
        dataManager.getData().set(path, value);
        if (key.equals("join-type")) dataManager.getData().set(guildPath(guildId) + ".open", value.equals("PUBLIC"));
        dataManager.save(); persistSettings(guildId); return true;
    }

    public String cycleSetting(Player actor, String key) {
        String guildId = getGuildId(actor);
        if (guildId == null || !hasPermission(actor, GuildPermission.EDIT_SETTINGS)) return null;
        String current = getGuildSetting(guildId, key);
        String next;
        if (key.equals("join-type")) next = next(current, List.of("PRIVATE", "INVITE_ONLY", "REQUEST_TO_JOIN", "PUBLIC"));
        else if (key.equals("war-requests") || key.equals("ally-requests")) next = next(current, List.of("ENABLED", "GUILD_MASTER_ONLY", "DISABLED"));
        else next = String.valueOf(!Boolean.parseBoolean(current));
        return updateSetting(actor, key, next) ? next : null;
    }

    public List<GuildRoleDefinition> getRoles(String guildId) {
        ConfigurationSection section = dataManager.getData().getConfigurationSection(guildPath(guildId) + ".role-definitions");
        if (section == null) return List.of();
        List<GuildRoleDefinition> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            String role = normalizeRole(id);
            result.add(new GuildRoleDefinition(role, section.getString(id + ".name", role), section.getInt(id + ".priority", 0), section.getString(id + ".color", "GRAY")));
        }
        result.sort(Comparator.comparingInt(GuildRoleDefinition::priority).reversed());
        return result;
    }

    public GuildRoleDefinition getRole(String guildId, String roleId) {
        String canonical = normalizeRole(roleId);
        return getRoles(guildId).stream().filter(role -> role.id().equals(canonical)).findFirst().orElse(null);
    }

    public String getMemberRole(String guildId, UUID uuid) {
        if (guildId == null || uuid == null) return "";
        if (uuid.toString().equals(dataManager.getData().getString(guildPath(guildId) + ".owner"))) return "GUILD_MASTER";
        String stored = dataManager.getData().getString(guildPath(guildId) + ".member-data." + uuid + ".role", "");
        if (!stored.isBlank()) return normalizeRole(stored);
        return getRank(guildId, uuid).equalsIgnoreCase("officer") ? "OFFICER" : "MEMBER";
    }

    public String getMemberRole(Player player) { String id = getGuildId(player); return id == null ? "" : getMemberRole(id, player.getUniqueId()); }
    public int getRolePriority(String guildId, String role) { GuildRoleDefinition definition = getRole(guildId, role); return definition == null ? 0 : definition.priority(); }
    public int getRoleMemberCount(String guildId, String role) { int count = 0; for (UUID uuid : getMembers(guildId)) if (getMemberRole(guildId, uuid).equals(normalizeRole(role))) count++; return count; }

    public Set<GuildPermission> getRolePermissions(String guildId, String role) {
        if (normalizeRole(role).equals("GUILD_MASTER")) return EnumSet.allOf(GuildPermission.class);
        Set<GuildPermission> result = EnumSet.noneOf(GuildPermission.class);
        for (GuildPermission permission : GuildPermission.values()) if (dataManager.getData().getBoolean(guildPath(guildId) + ".role-permissions." + normalizeRole(role) + "." + permission.name(), false)) result.add(permission);
        return Collections.unmodifiableSet(result);
    }

    public boolean hasPermission(Player player, GuildPermission permission) {
        if (player == null) return false;
        if (player.hasPermission("fakevival.guild.admin")) return true;
        String guildId = getGuildId(player);
        return guildId != null && getRolePermissions(guildId, getMemberRole(guildId, player.getUniqueId())).contains(permission);
    }

    public boolean toggleRolePermission(Player actor, String role, GuildPermission permission) {
        String guildId = getGuildId(actor); role = normalizeRole(role);
        if (guildId == null || !isOwner(actor) || role.equals("GUILD_MASTER") || getRole(guildId, role) == null) return false;
        String path = guildPath(guildId) + ".role-permissions." + role + "." + permission.name();
        dataManager.getData().set(path, !dataManager.getData().getBoolean(path)); dataManager.save(); persistRolePermissions(guildId, role); return true;
    }

    public RoleResult setMemberRole(Player actor, UUID target, String requestedRole) {
        String guildId = getGuildId(actor); String role = normalizeRole(requestedRole);
        if (guildId == null || !hasPermission(actor, GuildPermission.SET_MEMBER_ROLE)) return RoleResult.NO_PERMISSION;
        if (!isMember(target, guildId)) return RoleResult.NOT_MEMBER;
        GuildRoleDefinition desired = getRole(guildId, role);
        if (desired == null) return RoleResult.INVALID_ROLE;
        String owner = dataManager.getData().getString(guildPath(guildId) + ".owner", "");
        if (target.toString().equals(owner) || role.equals("GUILD_MASTER")) return RoleResult.OWNER_PROTECTED;
        int actorPriority = getRolePriority(guildId, getMemberRole(guildId, actor.getUniqueId()));
        int targetPriority = getRolePriority(guildId, getMemberRole(guildId, target));
        if (!isOwner(actor) && (desired.priority() >= actorPriority || targetPriority >= actorPriority)) return RoleResult.NO_PERMISSION;
        dataManager.getData().set(guildPath(guildId) + ".member-data." + target + ".role", role);
        dataManager.getData().set(guildPath(guildId) + ".ranks." + target, role.equals("OFFICER") ? "officer" : "member");
        dataManager.save(); persistMember(guildId, target); return RoleResult.UPDATED;
    }

    public long getJoinedAt(String guildId, UUID uuid) { return dataManager.getData().getLong(guildPath(guildId) + ".member-data." + uuid + ".joined-at", 0L); }
    public long getLastSeen(String guildId, UUID uuid) { return dataManager.getData().getLong(guildPath(guildId) + ".member-data." + uuid + ".last-seen", 0L); }
    public void touchMember(Player player) { String id = getGuildId(player); if (id == null) return; dataManager.getData().set(guildPath(id) + ".member-data." + player.getUniqueId() + ".last-seen", System.currentTimeMillis()); dataManager.getData().set(guildPath(id) + ".members." + player.getUniqueId(), player.getName()); dataManager.save(); persistMember(id, player.getUniqueId()); }
    public long getPlaytimeSeconds(UUID uuid) { long ticks = dataManager.getData().getLong("players." + uuid + ".playtime-ticks", -1L); if (ticks >= 0) return ticks / 20L; return dataManager.getData().getLong("players." + uuid + ".playtime-seconds", -1L); }
    public int getOnlineCount(String guildId) { int count = 0; for (UUID uuid : getMembers(guildId)) if (Bukkit.getPlayer(uuid) != null) count++; return count; }

    public RelationResult requestWar(Player actor, String otherName) { return requestRelation(actor, otherName, true); }
    public RelationResult requestAlly(Player actor, String otherName) { return requestRelation(actor, otherName, false); }
    public RelationResult acceptWar(Player actor, String otherName) { return respondRelation(actor, otherName, true, true); }
    public RelationResult denyWar(Player actor, String otherName) { return respondRelation(actor, otherName, true, false); }
    public RelationResult acceptAlly(Player actor, String otherName) { return respondRelation(actor, otherName, false, true); }
    public RelationResult denyAlly(Player actor, String otherName) { return respondRelation(actor, otherName, false, false); }
    public RelationResult cancelWar(Player actor, String otherName) { return cancelRelation(actor, otherName, true); }
    public RelationResult cancelAlly(Player actor, String otherName) { return cancelRelation(actor, otherName, false); }

    public RelationResult removeAlly(Player actor, String otherName) {
        String guildId = getGuildId(actor), other = findGuildId(otherName);
        if (guildId == null || !hasPermission(actor, GuildPermission.MANAGE_ALLIES)) return RelationResult.NO_PERMISSION;
        if (other == null) return RelationResult.NOT_FOUND;
        String id = pairId("alliance", guildId, other), base = "guild-alliances." + id;
        if (!dataManager.getData().getString(base + ".status", "").equals("ACTIVE")) return RelationResult.NOT_ACTIVE;
        dataManager.getData().set(base + ".status", "REMOVED"); dataManager.getData().set(base + ".ended-at", System.currentTimeMillis()); dataManager.save(); persistAlliance(id); return RelationResult.REMOVED;
    }

    public boolean areAllied(Player one, Player two) { String a = getGuildId(one), b = getGuildId(two); return a != null && b != null && !a.equals(b) && isActiveRelation("guild-alliances", pairId("alliance", a, b)); }
    public boolean isGuildDamageBlocked(Player one, Player two) { String a=getGuildId(one),b=getGuildId(two); if(a==null||b==null)return false; if(a.equals(b))return !getGuildSettingBoolean(a,"friendly-fire",false); return isActiveRelation("guild-alliances",pairId("alliance",a,b)); }
    public boolean areAtWar(String one, String two) { return one != null && two != null && isActiveRelation("guild-wars", pairId("war", one, two)); }
    public int getActiveWarCount(String guildId) { return countRelations("guild-wars", guildId, "ACTIVE"); }
    public int getPendingWarCount(String guildId) { return countRequests("guild-war-requests", guildId); }
    public int getActiveAllyCount(String guildId) { return countRelations("guild-alliances", guildId, "ACTIVE"); }
    public int getPendingAllyCount(String guildId) { return countRequests("guild-ally-requests", guildId); }

    public List<GuildRequest> getRequests(String guildId, boolean war, boolean incoming) {
        expireRequests(war);
        String root = war ? "guild-war-requests" : "guild-ally-requests";
        ConfigurationSection section = dataManager.getData().getConfigurationSection(root); if (section == null) return List.of();
        List<GuildRequest> result = new ArrayList<>();
        for (String id : section.getKeys(false)) { String base = root + "." + id; String from = dataManager.getData().getString(base + ".from"), to = dataManager.getData().getString(base + ".to"); if (!dataManager.getData().getString(base + ".status", "").equals("PENDING")) continue; if ((incoming && guildId.equals(to)) || (!incoming && guildId.equals(from))) result.add(new GuildRequest(id, from, to, dataManager.getData().getLong(base + ".created-at"), dataManager.getData().getLong(base + ".expires-at"))); }
        return result;
    }

    public List<GuildRelation> getRelations(String guildId, boolean war, boolean active) {
        String root = war ? "guild-wars" : "guild-alliances"; ConfigurationSection section = dataManager.getData().getConfigurationSection(root); if (section == null) return List.of(); List<GuildRelation> result = new ArrayList<>();
        for (String id : section.getKeys(false)) { String base = root + "." + id, status = dataManager.getData().getString(base + ".status", ""); String one = dataManager.getData().getString(base + ".one"), two = dataManager.getData().getString(base + ".two"); if (!guildId.equals(one) && !guildId.equals(two)) continue; if (active != status.equals("ACTIVE")) continue; result.add(new GuildRelation(id, one, two, status, dataManager.getData().getLong(base + ".started-at"), dataManager.getData().getLong(base + ".ended-at"), dataManager.getData().getInt(base + ".one-kills"), dataManager.getData().getInt(base + ".two-kills"))); }
        return result;
    }

    private RelationResult requestRelation(Player actor, String otherName, boolean war) {
        String own = getGuildId(actor), other = findGuildId(otherName); GuildPermission permission = war ? GuildPermission.MANAGE_WARS : GuildPermission.MANAGE_ALLIES;
        if (own == null || !hasPermission(actor, permission)) return RelationResult.NO_PERMISSION;
        if (other == null) return RelationResult.NOT_FOUND; if (own.equals(other)) return RelationResult.SAME_GUILD;
        if (war && isActiveRelation("guild-alliances", pairId("alliance", own, other))) return RelationResult.CONFLICT;
        if (!war && isActiveRelation("guild-wars", pairId("war", own, other))) return RelationResult.CONFLICT;
        if (isActiveRelation(war ? "guild-wars" : "guild-alliances", pairId(war ? "war" : "alliance", own, other))) return RelationResult.ALREADY_ACTIVE;
        String setting = getGuildSetting(other, war ? "war-requests" : "ally-requests"); if (setting.equals("DISABLED")) return RelationResult.DISABLED;
        String root = war ? "guild-war-requests" : "guild-ally-requests", id = (war ? "warreq_" : "allyreq_") + own + "_" + other, base = root + "." + id; long now = System.currentTimeMillis();
        if (dataManager.getData().getString(base + ".status", "").equals("PENDING") && dataManager.getData().getLong(base + ".expires-at") > now) return RelationResult.DUPLICATE;
        long last = dataManager.getData().getLong("guild-request-cooldowns." + id); if (now - last < 60_000L) return RelationResult.COOLDOWN;
        dataManager.getData().set(base + ".from", own); dataManager.getData().set(base + ".to", other); dataManager.getData().set(base + ".status", "PENDING"); dataManager.getData().set(base + ".created-at", now); dataManager.getData().set(base + ".expires-at", now + 300_000L); dataManager.getData().set("guild-request-cooldowns." + id, now); dataManager.save(); persistRequest(id, war); return RelationResult.REQUESTED;
    }

    private RelationResult respondRelation(Player actor, String otherName, boolean war, boolean accept) {
        String own = getGuildId(actor), other = findGuildId(otherName); GuildPermission permission = war ? (accept ? GuildPermission.ACCEPT_WARS : GuildPermission.DENY_WARS) : (accept ? GuildPermission.ACCEPT_ALLIES : GuildPermission.DENY_ALLIES);
        if (own == null || !hasPermission(actor, permission)) return RelationResult.NO_PERMISSION; if (other == null) return RelationResult.NOT_FOUND;
        String root = war ? "guild-war-requests" : "guild-ally-requests", id = (war ? "warreq_" : "allyreq_") + other + "_" + own, base = root + "." + id; long now = System.currentTimeMillis();
        if (!dataManager.getData().getString(base + ".status", "").equals("PENDING")) return RelationResult.NO_REQUEST;
        if (dataManager.getData().getLong(base + ".expires-at") <= now) { dataManager.getData().set(base + ".status", "EXPIRED"); dataManager.save(); persistRequest(id, war); return RelationResult.EXPIRED; }
        dataManager.getData().set(base + ".status", accept ? "ACCEPTED" : "DENIED");
        if (accept) { String relationId = pairId(war ? "war" : "alliance", own, other), relationRoot = war ? "guild-wars" : "guild-alliances", relationBase = relationRoot + "." + relationId; dataManager.getData().set(relationBase + ".one", own); dataManager.getData().set(relationBase + ".two", other); dataManager.getData().set(relationBase + ".status", "ACTIVE"); dataManager.getData().set(relationBase + ".started-at", now); dataManager.getData().set(relationBase + ".ended-at", null); if (war) persistWar(relationId); else persistAlliance(relationId); }
        dataManager.save(); persistRequest(id, war); return accept ? RelationResult.ACCEPTED : RelationResult.DENIED;
    }

    private RelationResult cancelRelation(Player actor, String otherName, boolean war) {
        String own = getGuildId(actor), other = findGuildId(otherName); if (own == null || !hasPermission(actor, war ? GuildPermission.MANAGE_WARS : GuildPermission.MANAGE_ALLIES)) return RelationResult.NO_PERMISSION; if (other == null) return RelationResult.NOT_FOUND;
        String id = (war ? "warreq_" : "allyreq_") + own + "_" + other, root = war ? "guild-war-requests" : "guild-ally-requests", base = root + "." + id; if (!dataManager.getData().getString(base + ".status", "").equals("PENDING")) return RelationResult.NO_REQUEST;
        dataManager.getData().set(base + ".status", "CANCELLED"); dataManager.save(); persistRequest(id, war); return RelationResult.CANCELLED;
    }

    private void migrateGuildManagementData() { long now = System.currentTimeMillis(); boolean changed = false; for (String guildId : getGuildIds()) { String ownerRaw = dataManager.getData().getString(guildPath(guildId) + ".owner", ""); UUID owner = null; try { owner = UUID.fromString(ownerRaw); } catch (Exception ignored) { } initializeGuild(guildId, owner, owner == null ? "Guild Master" : dataManager.getData().getString(guildPath(guildId) + ".members." + owner, "Guild Master")); for (UUID uuid : getMembers(guildId)) { String role = uuid.equals(owner) ? "GUILD_MASTER" : (getRank(guildId, uuid).equalsIgnoreCase("officer") ? "OFFICER" : "MEMBER"); String base = guildPath(guildId) + ".member-data." + uuid; if (!dataManager.getData().contains(base + ".joined-at")) { setMemberMetadata(guildId, uuid, dataManager.getData().getString(guildPath(guildId) + ".members." + uuid, uuid.toString()), role, now); changed = true; } persistMember(guildId, uuid); } persistSettings(guildId); persistAllRoles(guildId); } if (changed) dataManager.save(); }

    private void initializeGuild(String guildId, UUID owner, String ownerName) { String base = guildPath(guildId); if (!dataManager.getData().contains(base + ".settings.join-type")) { dataManager.getData().set(base + ".settings.join-type", dataManager.getData().getBoolean(base + ".open", true) ? "PUBLIC" : "INVITE_ONLY"); dataManager.getData().set(base + ".settings.description", ""); dataManager.getData().set(base + ".settings.guild-chat", true); dataManager.getData().set(base + ".settings.war-requests", "ENABLED"); dataManager.getData().set(base + ".settings.ally-requests", "ENABLED"); dataManager.getData().set(base + ".settings.public-member-list", true); dataManager.getData().set(base + ".settings.friendly-fire", false); }
        addDefaultRole(guildId, "GUILD_MASTER", 100, EnumSet.allOf(GuildPermission.class)); addDefaultRole(guildId, "OFFICER", 75, EnumSet.of(GuildPermission.INVITE_MEMBERS, GuildPermission.KICK_MEMBERS, GuildPermission.SET_MEMBER_ROLE, GuildPermission.EDIT_MOTD, GuildPermission.USE_GUILD_CHAT, GuildPermission.MUTE_GUILD_CHAT, GuildPermission.VIEW_AUDIT_LOG)); addDefaultRole(guildId, "MEMBER", 50, EnumSet.of(GuildPermission.USE_GUILD_CHAT, GuildPermission.DEPOSIT_GUILD_BANK)); addDefaultRole(guildId, "RECRUIT", 10, EnumSet.of(GuildPermission.USE_GUILD_CHAT)); if (owner != null) setMemberMetadata(guildId, owner, ownerName, "GUILD_MASTER", System.currentTimeMillis()); }
    private void addDefaultRole(String guildId, String id, int priority, Set<GuildPermission> permissions) { String base = guildPath(guildId) + ".role-definitions." + id; if (!dataManager.getData().contains(base)) { dataManager.getData().set(base + ".name", id.replace('_', ' ')); dataManager.getData().set(base + ".priority", priority); dataManager.getData().set(base + ".color", "GRAY"); dataManager.getData().set(base + ".created-at", System.currentTimeMillis()); for (GuildPermission permission : permissions) dataManager.getData().set(guildPath(guildId) + ".role-permissions." + id + "." + permission.name(), true); } }
    private void setMemberMetadata(String guildId, UUID uuid, String name, String role, long joinedAt) { String base = guildPath(guildId) + ".member-data." + uuid; dataManager.getData().set(base + ".name", name == null ? uuid.toString() : name); dataManager.getData().set(base + ".role", role); if (!dataManager.getData().contains(base + ".joined-at")) dataManager.getData().set(base + ".joined-at", joinedAt); dataManager.getData().set(base + ".last-seen", System.currentTimeMillis()); }

    private void persistMember(String guildId, UUID uuid) { String name = dataManager.getData().getString(guildPath(guildId) + ".members." + uuid, uuid.toString()), role = getMemberRole(guildId, uuid); long joined = getJoinedAt(guildId, uuid), seen = getLastSeen(guildId, uuid); dataManager.runDatabaseAsync("save-member", connection -> { try (PreparedStatement s = connection.prepareStatement("INSERT INTO `smp_guild_members` (`guild_id`,`uuid`,`player_name`,`guild_role`,`joined_at`,`last_seen`) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `player_name`=VALUES(`player_name`),`guild_role`=VALUES(`guild_role`),`joined_at`=VALUES(`joined_at`),`last_seen`=VALUES(`last_seen`)")) { s.setString(1,guildId);s.setString(2,uuid.toString());s.setString(3,name);s.setString(4,role);s.setLong(5,joined);s.setLong(6,seen);s.executeUpdate(); } }); }
    private void persistSettings(String guildId) { String description=getGuildSetting(guildId,"description"), join=getJoinType(guildId), war=getGuildSetting(guildId,"war-requests"), ally=getGuildSetting(guildId,"ally-requests"); boolean chat=getGuildSettingBoolean(guildId,"guild-chat",true), list=getGuildSettingBoolean(guildId,"public-member-list",true), friendly=getGuildSettingBoolean(guildId,"friendly-fire",false); dataManager.runDatabaseAsync("save-settings", c->{try(PreparedStatement s=c.prepareStatement("INSERT INTO `smp_guild_settings` (`guild_id`,`description`,`join_type`,`guild_chat_enabled`,`war_requests`,`ally_requests`,`public_member_list`,`friendly_fire`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `description`=VALUES(`description`),`join_type`=VALUES(`join_type`),`guild_chat_enabled`=VALUES(`guild_chat_enabled`),`war_requests`=VALUES(`war_requests`),`ally_requests`=VALUES(`ally_requests`),`public_member_list`=VALUES(`public_member_list`),`friendly_fire`=VALUES(`friendly_fire`),`updated_at`=VALUES(`updated_at`)")){s.setString(1,guildId);s.setString(2,description);s.setString(3,join);s.setBoolean(4,chat);s.setString(5,war);s.setString(6,ally);s.setBoolean(7,list);s.setBoolean(8,friendly);s.setLong(9,System.currentTimeMillis());s.executeUpdate();}}); }
    private void persistAllRoles(String guildId) { for (GuildRoleDefinition role : getRoles(guildId)) { persistRole(guildId, role); persistRolePermissions(guildId, role.id()); } }
    private void persistRole(String guildId, GuildRoleDefinition role) { long created=dataManager.getData().getLong(guildPath(guildId)+".role-definitions."+role.id()+".created-at",System.currentTimeMillis()); dataManager.runDatabaseAsync("save-role",c->{try(PreparedStatement s=c.prepareStatement("INSERT INTO `smp_guild_roles` (`guild_id`,`role_id`,`role_name`,`priority`,`color`,`created_at`) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`),`priority`=VALUES(`priority`),`color`=VALUES(`color`)")){s.setString(1,guildId);s.setString(2,role.id());s.setString(3,role.name());s.setInt(4,role.priority());s.setString(5,role.color());s.setLong(6,created);s.executeUpdate();}}); }
    private void persistRolePermissions(String guildId,String role) { Set<GuildPermission> permissions=new HashSet<>(getRolePermissions(guildId,role)); dataManager.runDatabaseAsync("save-role-permissions",c->{for(GuildPermission p:GuildPermission.values()){try(PreparedStatement s=c.prepareStatement("INSERT INTO `smp_guild_role_permissions` (`guild_id`,`role_id`,`permission_id`,`enabled`) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE `enabled`=VALUES(`enabled`)")){s.setString(1,guildId);s.setString(2,role);s.setString(3,p.name());s.setBoolean(4,permissions.contains(p));s.executeUpdate();}}}); }
    private void persistRequest(String id,boolean war){String root=war?"guild-war-requests":"guild-ally-requests",base=root+"."+id,table=war?"smp_guild_war_requests":"smp_guild_ally_requests";String from=dataManager.getData().getString(base+".from"),to=dataManager.getData().getString(base+".to"),status=dataManager.getData().getString(base+".status");long created=dataManager.getData().getLong(base+".created-at"),expires=dataManager.getData().getLong(base+".expires-at");dataManager.runDatabaseAsync("save-request",c->{try(PreparedStatement s=c.prepareStatement("INSERT INTO `"+table+"` (`request_id`,`from_guild_id`,`to_guild_id`,`status`,`created_at`,`expires_at`) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `status`=VALUES(`status`),`created_at`=VALUES(`created_at`),`expires_at`=VALUES(`expires_at`)")){s.setString(1,id);s.setString(2,from);s.setString(3,to);s.setString(4,status);s.setLong(5,created);s.setLong(6,expires);s.executeUpdate();}});}
    private void persistWar(String id){String b="guild-wars."+id;String one=dataManager.getData().getString(b+".one"),two=dataManager.getData().getString(b+".two"),status=dataManager.getData().getString(b+".status");long start=dataManager.getData().getLong(b+".started-at"),end=dataManager.getData().getLong(b+".ended-at");int a=dataManager.getData().getInt(b+".one-kills"),d=dataManager.getData().getInt(b+".two-kills");dataManager.runDatabaseAsync("save-war",c->{try(PreparedStatement s=c.prepareStatement("INSERT INTO `smp_guild_wars` (`war_id`,`guild_one_id`,`guild_two_id`,`status`,`started_at`,`ended_at`,`guild_one_kills`,`guild_two_kills`) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `status`=VALUES(`status`),`ended_at`=VALUES(`ended_at`),`guild_one_kills`=VALUES(`guild_one_kills`),`guild_two_kills`=VALUES(`guild_two_kills`)")){s.setString(1,id);s.setString(2,one);s.setString(3,two);s.setString(4,status);s.setLong(5,start);if(end<=0)s.setNull(6,java.sql.Types.BIGINT);else s.setLong(6,end);s.setInt(7,a);s.setInt(8,d);s.executeUpdate();}});}
    private void persistAlliance(String id){String b="guild-alliances."+id;String one=dataManager.getData().getString(b+".one"),two=dataManager.getData().getString(b+".two"),status=dataManager.getData().getString(b+".status");long start=dataManager.getData().getLong(b+".started-at"),end=dataManager.getData().getLong(b+".ended-at");dataManager.runDatabaseAsync("save-alliance",c->{try(PreparedStatement s=c.prepareStatement("INSERT INTO `smp_guild_alliances` (`alliance_id`,`guild_one_id`,`guild_two_id`,`status`,`started_at`,`ended_at`) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `status`=VALUES(`status`),`ended_at`=VALUES(`ended_at`)")){s.setString(1,id);s.setString(2,one);s.setString(3,two);s.setString(4,status);s.setLong(5,start);if(end<=0)s.setNull(6,java.sql.Types.BIGINT);else s.setLong(6,end);s.executeUpdate();}});}
    private boolean isActiveRelation(String root,String id){return dataManager.getData().getString(root+"."+id+".status","").equals("ACTIVE");}
    private int countRelations(String root,String guild,String status){ConfigurationSection s=dataManager.getData().getConfigurationSection(root);if(s==null)return 0;int count=0;for(String id:s.getKeys(false)){String b=root+"."+id;if(dataManager.getData().getString(b+".status","").equals(status)&&(guild.equals(dataManager.getData().getString(b+".one"))||guild.equals(dataManager.getData().getString(b+".two"))))count++;}return count;}
    private int countRequests(String root,String guild){ConfigurationSection s=dataManager.getData().getConfigurationSection(root);if(s==null)return 0;int count=0;for(String id:s.getKeys(false)){String b=root+"."+id;if(dataManager.getData().getString(b+".status","").equals("PENDING")&&(guild.equals(dataManager.getData().getString(b+".from"))||guild.equals(dataManager.getData().getString(b+".to")))&&dataManager.getData().getLong(b+".expires-at")>System.currentTimeMillis())count++;}return count;}
    private void expireRequests(boolean war){String root=war?"guild-war-requests":"guild-ally-requests";ConfigurationSection s=dataManager.getData().getConfigurationSection(root);if(s==null)return;long now=System.currentTimeMillis();for(String id:s.getKeys(false)){String b=root+"."+id;if(dataManager.getData().getString(b+".status","").equals("PENDING")&&dataManager.getData().getLong(b+".expires-at")<=now){dataManager.getData().set(b+".status","EXPIRED");persistRequest(id,war);}}}
    private String pairId(String prefix,String a,String b){return a.compareToIgnoreCase(b)<=0?prefix+"_"+a+"_"+b:prefix+"_"+b+"_"+a;}
    private String normalizeRole(String role){String clean=role==null?"":role.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+","_").replaceAll("^_+|_+$","");return clean.length()>32?clean.substring(0,32):clean;}
    private String safeChoice(String value,Set<String> choices,String fallback){String v=value==null?"":value.toUpperCase(Locale.ROOT);return choices.contains(v)?v:fallback;}
    private String sanitizeText(String text,int max){String clean=text==null?"":text.replaceAll("[\\r\\n]"," ").replace('&',' ').trim();return clean.length()>max?clean.substring(0,max):clean;}
    private String next(String current,List<String> values){int i=values.indexOf(current==null?"":current.toUpperCase(Locale.ROOT));return values.get((i+1+values.size())%values.size());}

    public record GuildRequest(String id,String fromGuildId,String toGuildId,long createdAt,long expiresAt) { }
    public record GuildRelation(String id,String guildOneId,String guildTwoId,String status,long startedAt,long endedAt,int guildOneKills,int guildTwoKills) { public String other(String guildId){return guildOneId.equals(guildId)?guildTwoId:guildOneId;} }
    public enum RoleResult { UPDATED, NO_PERMISSION, NOT_MEMBER, INVALID_ROLE, OWNER_PROTECTED }
    public enum RelationResult { REQUESTED, ACCEPTED, DENIED, CANCELLED, REMOVED, NO_PERMISSION, NOT_FOUND, SAME_GUILD, CONFLICT, ALREADY_ACTIVE, DUPLICATE, COOLDOWN, DISABLED, NO_REQUEST, EXPIRED, NOT_ACTIVE }

    public enum ClaimResult {
        CLAIMED,
        NO_GUILD,
        NOT_OWNER,
        ALREADY_YOURS,
        ALREADY_CLAIMED,
        LIMIT_REACHED,
        INVALID
    }

    public enum JoinResult {
        JOINED,
        REQUESTED,
        ALREADY_IN_GUILD,
        NOT_FOUND,
        INVITE_REQUIRED
    }

    public enum TagResult {
        UPDATED,
        NO_GUILD,
        NOT_OWNER,
        INVALID,
        BLOCKED
    }
}
