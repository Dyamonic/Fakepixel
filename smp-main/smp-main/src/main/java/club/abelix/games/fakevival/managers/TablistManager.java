package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.hooks.AbelixHook;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatService;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.util.CC;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TablistManager {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final long ABELIX_CACHE_MILLIS = 30_000L;
    private static final int TAB_ROWS = 20;
    private static final int TAB_COLUMNS = 4;
    private static final int PLAYER_FIRST_ROW = 1;
    private static final int PLAYER_COLUMNS = 2;
    private static final int PLAYER_ROWS_PER_COLUMN = 10;
    private static final int PLAYER_CAPACITY = PLAYER_COLUMNS * PLAYER_ROWS_PER_COLUMN;
    private static final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> FAKE_ENTRY_ACTIONS = EnumSet.of(
            ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT
    );

    private final JavaPlugin plugin;
    private final AbelixHook abelixHook;
    private final SkillManager skillManager;
    private final JobManager jobManager;
    private final EconomyManager economyManager;
    private final BattlePassManager battlePassManager;
    private final GuildManager guildManager;
    private final PlayerLevelManager playerLevelManager;
    private final Map<UUID, AbelixCache> abelixCache = new ConcurrentHashMap<>();
    private final Set<UUID> abelixRefreshInProgress = ConcurrentHashMap.newKeySet();
    private final Set<UUID> bankRefreshInProgress = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ContextView> contextCache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> fakeSlotsByViewer = new ConcurrentHashMap<>();
    private final LocationDisplayResolver locationDisplayResolver;
    private BankManager bankManager;
    private ZoneManager zoneManager;
    private DungeonManager dungeonManager;
    private MMOStatService mmoStatService;
    private CompanionManager companionManager;
    private PlaytimeRewardManager playtimeRewardManager;
    private EventManager eventManager;

    public TablistManager(JavaPlugin plugin, AbelixHook abelixHook, SkillManager skillManager, JobManager jobManager, EconomyManager economyManager, BattlePassManager battlePassManager, GuildManager guildManager) {
        this(plugin, abelixHook, skillManager, jobManager, economyManager, battlePassManager, guildManager, null);
    }

    public TablistManager(JavaPlugin plugin, AbelixHook abelixHook, SkillManager skillManager, JobManager jobManager, EconomyManager economyManager, BattlePassManager battlePassManager, GuildManager guildManager, PlayerLevelManager playerLevelManager) {
        this.plugin = plugin;
        this.abelixHook = abelixHook;
        this.skillManager = skillManager;
        this.jobManager = jobManager;
        this.economyManager = economyManager;
        this.battlePassManager = battlePassManager;
        this.guildManager = guildManager;
        this.playerLevelManager = playerLevelManager;
        this.locationDisplayResolver = new LocationDisplayResolver(plugin);
    }

    public void configureWidgets(BankManager bankManager, ZoneManager zoneManager, DungeonManager dungeonManager,
                                 MMOStatService mmoStatService, ManaActionBarManager manaActionBarManager,
                                 CompanionManager companionManager, PlaytimeRewardManager playtimeRewardManager) {
        this.bankManager = bankManager;
        this.zoneManager = zoneManager;
        this.dungeonManager = dungeonManager;
        this.mmoStatService = mmoStatService;
        this.companionManager = companionManager;
        this.playtimeRewardManager = playtimeRewardManager;
        if (manaActionBarManager != null) manaActionBarManager.setPlaceholderParser(this::parse);
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void apply(Player player) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("tablist.enabled", true)) {
            clearFakeEntries(player);
            return;
        }
        warmBankCache(player);
        player.setPlayerListHeaderFooter(
                parse(player, "&bYou are playing on &6MC.FAKEPIXEL.ME"),
                parse(player, "&aRanks, Boosters & MORE! &cSTORE.FAKEPIXEL.ME")
        );
        player.setPlayerListName(playerListDisplay(player));
        updateColumns(player);
    }

    private String buildHeader(Player player) {
        List<String> lines = new ArrayList<>(List.of(
                "&6&lSMP",
                "&fStore: &6world.fakepixel.me",
                "",
                "&aProfile",
                "&fCoins: &6%coins%",
                "&fBank: &6%bank_balance%",
                "&fLevel: %smp_player_level_display%",
                "&fAdvebture Rank: %smp_adventure_rank_display%",
                "",
                "&aStats",
                "&fHealth: &c%current_mmo_hp%/%max_mmo_hp%❤",
                "&fDefense: &a%defense%/%max_defense%❈",
                "&fMana: &b%current_mana%/%max_mana%✎"
        ));

        DungeonView dungeon = contextView(player).dungeon();
        if (dungeon.active()) {
            lines.addAll(List.of(
                    "",
                    "&aDungeon",
                    "&fDungeon: &e%dungeon_name%",
                    "&fMobs Alive: &c%dungeon_mobs_alive%",
                    "&fBoss: &c%dungeon_boss_status%",
                    "&fProgress: &a%dungeon_progress%%"
            ));
        } else {
            lines.addAll(List.of(
                    "",
                    "&aSkills",
                    "&fCombat: &c%combat_level%",
                    "&fMining: &b%mining_level%",
                    "&fFarming: &a%farming_level%",
                    "&fFishing: &3%fishing_level%",
                    "&fForaging: &2%foraging_level%",
                    "",
                    "&aLocation",
                    "&fWorld: &e%world%",
                    "&fZone: &e%zone_name%",
                    "&fArea Level: &c%zone_level%"
            ));
        }
        return String.join("\n", lines);
    }

    private void updateColumns(Player viewer) {
        if (!(viewer instanceof CraftPlayer craftViewer) || !viewer.isOnline()) return;

        List<? extends Player> visiblePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(viewer::canSee)
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int shownPlayers = Math.min(visiblePlayers.size(), PLAYER_CAPACITY);

        String[] rawLines = new String[TAB_ROWS * TAB_COLUMNS];
        for (int column = 0; column < TAB_COLUMNS; column++) {
            for (int row = 0; row < TAB_ROWS; row++) {
                rawLines[slot(column, row)] = columnLine(viewer, column, row, visiblePlayers.size());
            }
        }
        String[] renderedLines = parse(viewer, String.join("\n", rawLines)).split("\n", -1);

        List<ClientboundPlayerInfoUpdatePacket.Entry> realEntries = new ArrayList<>(shownPlayers);
        Set<Integer> occupiedPlayerSlots = new HashSet<>();
        for (int index = 0; index < shownPlayers; index++) {
            Player target = visiblePlayers.get(index);
            int column = index / PLAYER_ROWS_PER_COLUMN;
            int row = PLAYER_FIRST_ROW + (index % PLAYER_ROWS_PER_COLUMN);
            int slot = slot(column, row);
            occupiedPlayerSlots.add(slot);
            GameProfile profile = ((CraftPlayer) target).getHandle().getGameProfile();
            realEntries.add(new ClientboundPlayerInfoUpdatePacket.Entry(
                    target.getUniqueId(), profile, true, 0, GameType.DEFAULT_MODE,
                    CraftChatMessage.fromStringOrEmpty(playerListDisplay(target)),
                    true, listOrder(slot), null));
        }
        if (!realEntries.isEmpty()) {
            craftViewer.getHandle().connection.send(new ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER,
                            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), realEntries));
        }

        Set<Integer> currentFakeSlots = new HashSet<>();
        Map<Integer, ClientboundPlayerInfoUpdatePacket.Entry> fakeEntries = new LinkedHashMap<>();
        for (int column = 0; column < TAB_COLUMNS; column++) {
            for (int row = 0; row < TAB_ROWS; row++) {
                int slot = slot(column, row);
                if (occupiedPlayerSlots.contains(slot)) continue;
                currentFakeSlots.add(slot);
                UUID uuid = fakeEntryUuid(viewer, slot);
                GameProfile profile = new GameProfile(uuid, fakeProfileName(slot));
                Component displayName = CraftChatMessage.fromStringOrEmpty(renderedLines[slot]);
                fakeEntries.put(slot, new ClientboundPlayerInfoUpdatePacket.Entry(
                        uuid, profile, true, 0, GameType.DEFAULT_MODE, displayName, false, listOrder(slot), null));
            }
        }

        Set<Integer> previousFakeSlots = fakeSlotsByViewer.getOrDefault(viewer.getUniqueId(), Set.of());
        List<UUID> removed = previousFakeSlots.stream()
                .filter(slot -> !currentFakeSlots.contains(slot))
                .map(slot -> fakeEntryUuid(viewer, slot))
                .toList();
        if (!removed.isEmpty()) {
            craftViewer.getHandle().connection.send(new ClientboundPlayerInfoRemovePacket(removed));
        }

        List<ClientboundPlayerInfoUpdatePacket.Entry> added = fakeEntries.entrySet().stream()
                .filter(entry -> !previousFakeSlots.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!added.isEmpty()) {
            craftViewer.getHandle().connection.send(new ClientboundPlayerInfoUpdatePacket(FAKE_ENTRY_ACTIONS, added));
        }
        List<ClientboundPlayerInfoUpdatePacket.Entry> retained = fakeEntries.entrySet().stream()
                .filter(entry -> previousFakeSlots.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!retained.isEmpty()) {
            craftViewer.getHandle().connection.send(new ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), retained));
        }
        fakeSlotsByViewer.put(viewer.getUniqueId(), Set.copyOf(currentFakeSlots));
    }

    private String columnLine(Player viewer, int column, int row, int visibleOnline) {
        if (column == 0) return row == 0 ? "&aPlayers &7(" + visibleOnline + ")" : "";
        if (column == 1) return "";
        if (column == 2) return infoColumnLine(viewer, row);
        return statsColumnLine(viewer, row);
    }

    private String infoColumnLine(Player viewer, int row) {
        ContextView context = contextView(viewer);
        BossView boss = context.boss();
        boolean petActive = companionManager != null && companionManager.isActive(viewer)
                && !companionManager.selected(viewer).isBlank();
        List<String> lines = new ArrayList<>(List.of(
                "&b&lInfo",
                context.zone() != null && context.zone().level() > 0
                        ? "&bArea: &7%zone_name% &8(Lv.%zone_level%)" : "&bArea: &a%zone_name%",
                "&fServer: &a%smp_server_name%",
                "&fCoins: &6%coins%",
                "&fGuild: &a%guild_name%",
                "",
                "&aProfile: &e%profile_name%",
                "&fSMP Level: %smp_player_level_display%",
                "&fAdvebture Rank: %smp_adventure_rank_display%",
                "&fBank: &6%bank_balance%",
                "&fPlaytime: &e%playtime%",
                "",
                "%smp_event_line_1%",
                "%smp_event_line_2%",
                "%smp_event_line_3%",
                "",
                "&aPet:",
                petActive ? "&7[Lvl %pet_level%] &a%pet_name%" : "&7None",
                petActive && companionManager.level(viewer, companionManager.selected(viewer)) >= companionManager.maxLevel(companionManager.selected(viewer))
                        ? "&bMAX LEVEL" : "",
                ""
        ));
        if (boss.active()) {
            lines.add("&cBoss: &7%boss_status%");
            lines.add("&f%boss_name%: &c%boss_hp%/%boss_max_hp%");
        } else {
            lines.add("&cBoss: &7None");
        }
        return row < lines.size() ? lines.get(row) : "";
    }

    private String statsColumnLine(Player viewer, int row) {
        ContextView context = contextView(viewer);
        DungeonView dungeon = context.dungeon();
        if (dungeon.active()) return line(row,
                "&b&lInfo",
                "&cDungeon: &e%dungeon_name%",
                "&fMobs Alive: &c%dungeon_mobs_alive%",
                "&fBoss: &c%dungeon_boss_status%",
                "&fProgress: &a%dungeon_progress%%",
                "&fDeaths: &c%dungeon_deaths%",
                "&fScore: &e%dungeon_score%",
                "",
                "&eSkills:",
                "&fFarming: &a%farming_level% &3%farming_progress%%",
                "&fMining: &b%mining_level% &3%mining_progress%%",
                "&fCombat: &c%combat_level% &3%combat_progress%%",
                "&fFishing: &3%fishing_level% &3%fishing_progress%%",
                "&fForaging: &2%foraging_level% &3%foraging_progress%%",
                "",
                "&eStats:",
                "&fSpeed: &f%speed%",
                "&fStrength: &c%strength%",
                "&fCrit Chance: &9%crit_chance%%",
                "&fCrit Damage: &9%crit_damage%%");
        if (context.boss().active()) return line(row,
                "&b&lInfo",
                "&cBoss: &e%boss_name%",
                "&fStatus: &c%boss_status%",
                "&fHealth: &c%boss_hp%/%boss_max_hp%",
                "&fArea: &e%zone_name%",
                "&fProgress: &a%area_progress%%",
                "", "",
                "&eSkills:",
                "&fFarming: &a%farming_level% &3%farming_progress%%",
                "&fMining: &b%mining_level% &3%mining_progress%%",
                "&fCombat: &c%combat_level% &3%combat_progress%%",
                "&fUnarmed: &2%unarmed_level% &3%unarmed_progress%%",
                "&fFishing: &3%fishing_level% &3%fishing_progress%%",
                "&fForaging: &2%foraging_level% &3%foraging_progress%%",
                "",
                "&eStats:",
                "&fSpeed: &f%speed%",
                "&fStrength: &c%strength%",
                "&fCrit Chance: &9%crit_chance%%",
                "&fCrit Damage: &9%crit_damage%%");
        return line(row,
                "&b&lInfo",
                "&eEvent: %smp_event_name_colored%",
                "%smp_event_line_2%",
                "%smp_event_line_3%",
                "", "",
                "&eSkills:",
                "&fFarming: &a%farming_level% &3%farming_progress%%",
                "&fMining: &b%mining_level% &3%mining_progress%%",
                "&fCombat: &c%combat_level% &3%combat_progress%%",
                "&fFishing: &3%fishing_level% &3%fishing_progress%%",
                "&fForaging: &2%foraging_level% &3%foraging_progress%%",
                "",
                "&eStats:",
                "&fSpeed: &f%speed%",
                "&fStrength: &c%strength%",
                "&fCrit Chance: &9%crit_chance%%",
                "&fCrit Damage: &9%crit_damage%%");
    }

    private String line(int row, String... lines) {
        return row < lines.length ? lines[row] : "";
    }

    private int slot(int column, int row) {
        return column * TAB_ROWS + row;
    }

    private int listOrder(int slot) {
        // Vanilla sorts this value descending, so the first visual cell needs the highest value.
        return TAB_ROWS * TAB_COLUMNS - slot;
    }

    private UUID fakeEntryUuid(Player viewer, int slot) {
        return UUID.nameUUIDFromBytes(("smp-tab:" + viewer.getUniqueId() + ':' + slot)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String fakeProfileName(int slot) {
        return String.format(Locale.ROOT, "!smp_tab_%02d", slot);
    }

    public String parse(Player player, String text) {
        String job = jobManager.getJob(player);
        text = text == null ? "" : text;
        text = text.replace("%player%", player.getName());
        text = text.replace("%displayname%", player.getDisplayName());
        text = text.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        text = text.replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        text = text.replace("%date%", LocalDate.now().format(DATE_FORMAT));
        text = text.replace("%world%", player.getWorld().getName());
        text = text.replace("%kills%", String.valueOf(player.getStatistic(Statistic.PLAYER_KILLS)));
        text = text.replace("%player_kills%", String.valueOf(player.getStatistic(Statistic.PLAYER_KILLS)));
        text = text.replace("%mob_kills%", String.valueOf(player.getStatistic(Statistic.MOB_KILLS)));
        text = text.replace("%deaths%", String.valueOf(player.getStatistic(Statistic.DEATHS)));
        text = text.replace("%job%", job == null ? "None" : pretty(job));
        text = text.replace("%guild%", guildManager == null || !guildManager.hasGuild(player) ? "None" : Objects.requireNonNull(guildManager.getGuildName(player)));
        text = text.replace("%guild_name%", guildManager == null || !guildManager.hasGuild(player) ? "None" : Objects.requireNonNull(guildManager.getGuildName(player)));
        text = text.replace("%guild_tag%", guildManager == null ? "" : guildManager.getGuildTag(player));
        text = text.replace("%smp_guild_tag_formatted%", guildManager == null ? "" : guildManager.getGuildTag(player));
        text = text.replace("%fakevival_guild_tag_formatted%", guildManager == null ? "" : guildManager.getGuildTag(player));
        text = text.replace("%smp_guild_tag_color%", guildManager == null ? "GRAY" : guildManager.getTagColorId(player));
        text = text.replace("%fakevival_guild_tag_color%", guildManager == null ? "GRAY" : guildManager.getTagColorId(player));
        text = text.replace("%smp_guild_tag%", guildManager == null ? "" : guildManager.getRawGuildTag(player));
        String guildId = guildManager == null ? null : guildManager.getGuildId(player);
        text = text.replace("%smp_guild_member_count%", guildId == null ? "0" : String.valueOf(guildManager.getMemberCount(guildId)));
        text = text.replace("%smp_guild_online_count%", guildId == null ? "0" : String.valueOf(guildManager.getOnlineCount(guildId)));
        text = text.replace("%smp_guild_role%", guildId == null ? "" : guildManager.getMemberRole(player));
        text = text.replace("%smp_guild_role_display%", guildId == null ? "" : guildManager.getMemberRole(player).replace('_', ' '));
        text = text.replace("%smp_guild_war_count%", guildId == null ? "0" : String.valueOf(guildManager.getActiveWarCount(guildId)));
        text = text.replace("%smp_guild_active_wars%", guildId == null ? "0" : String.valueOf(guildManager.getActiveWarCount(guildId)));
        text = text.replace("%smp_guild_pending_wars%", guildId == null ? "0" : String.valueOf(guildManager.getPendingWarCount(guildId)));
        text = text.replace("%smp_guild_ally_count%", guildId == null ? "0" : String.valueOf(guildManager.getActiveAllyCount(guildId)));
        text = text.replace("%smp_guild_active_allies%", guildId == null ? "0" : String.valueOf(guildManager.getActiveAllyCount(guildId)));
        text = text.replace("%smp_guild_pending_allies%", guildId == null ? "0" : String.valueOf(guildManager.getPendingAllyCount(guildId)));
        text = text.replace("%fakevival_guild_tag%", guildManager == null ? "" : guildManager.getRawGuildTag(player));
        if(plugin instanceof club.abelix.games.fakevival.Game game&&game.getAdventureGuildManager()!=null){
            AdventureGuildManager guild=game.getAdventureGuildManager();AdventureGuildManager.Profile profile=guild.profile(player);
            text=text.replace("%smp_adventure_rank%",profile.rank().name())
                    .replace("%smp_adventure_rank_display%",CC.color(profile.rank().color+profile.rank().name()))
                    .replace("%smp_adventure_active_quest_limit%",String.valueOf(guild.activeLimit(player)))
                    .replace("%smp_adventure_reward_multiplier%",String.valueOf(guild.rewardMultiplier(player)))
                    .replace("%smp_adventure_completed_quests%",String.valueOf(profile.quests()));
        }
        int playerLevel = playerLevelManager == null ? 1 : playerLevelManager.getLevel(player);
        String levelDisplay = playerLevelManager == null ? "" : playerLevelManager.getDisplay(player);
        String levelIcon = playerLevelManager == null ? "✦" : playerLevelManager.getIcon(playerLevel);
        String levelColor = playerLevelManager == null ? "&7" : playerLevelManager.getColor(playerLevel);
        long levelXp = playerLevelManager == null ? 0L : playerLevelManager.getXp(player);
        long levelXpNeeded = playerLevelManager == null ? 1000L : playerLevelManager.getXpNeeded(player);
        String levelProgress = playerLevelManager == null ? "0" : String.format(Locale.US, "%.1f", playerLevelManager.getProgress(player));
        int nextPlayerLevel = playerLevelManager == null ? 2 : Math.min(playerLevelManager.getMaxLevel(), playerLevel + 1);
        text = text.replace("%smp_player_level_display%", levelDisplay)
                .replace("%fakevival_player_level_display%", levelDisplay)
                .replace("%player_level_display%", levelDisplay)
                .replace("%smp_player_level_icon%", levelIcon)
                .replace("%smp_player_level_color%", levelColor)
                .replace("%smp_player_level_xp_needed%", String.valueOf(levelXpNeeded))
                .replace("%smp_player_level_xp%", String.valueOf(levelXp))
                .replace("%smp_player_level_progress%", levelProgress)
                .replace("%smp_player_level_next_display%", playerLevelManager == null ? "&7[✦2] " : playerLevelManager.getLevelDisplay(nextPlayerLevel) + " ")
                .replace("%smp_player_level_next_xp%", String.valueOf(levelXpNeeded))
                .replace("%smp_player_level_next_requirements%", playerLevelManager == null ? "0 skills, 0 items" : playerLevelManager.nextRequirementsPlaceholder(player))
                .replace("%smp_player_level_next_stats%", playerLevelManager == null ? "" : playerLevelManager.nextStatsSummary(player))
                .replace("%smp_player_level_requirements_met%", String.valueOf(playerLevelManager != null && playerLevelManager.cachedNextRequirementsMet(player)))
                .replace("%smp_player_level_next%", String.valueOf(nextPlayerLevel))
                .replace("%fakevival_player_level%", String.valueOf(playerLevel))
                .replace("%smp_player_level%", String.valueOf(playerLevel))
                .replace("%player_level%", String.valueOf(playerLevel));
        text = text.replace("%player_xp%", String.valueOf(totalSkillXp(player)));
        text = text.replace("%player_needed_xp%", String.valueOf(totalSkillXpNeeded(player)));
        if (plugin instanceof club.abelix.games.fakevival.Game game) {
            var mayor = game.getMayorManager();
            for (String placeholder : List.of("%smp_mayor_name%", "%smp_mayor_perk%", "%smp_mayor_time_left%",
                    "%smp_election_phase%", "%smp_election_time_left%", "%smp_election_candidate_count%")) {
                text = text.replace(placeholder, mayor.placeholder(placeholder));
            }
        }
        text = text.replace("%smp_server_name%", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("server-name", "SMP"));
        text = text.replace("%playtime%", formatDuration(playtimeSeconds(player)));
        text = text.replace("%jobs%", jobManager.getJobsDisplay(player));
        text = text.replace("%job_level%", String.valueOf(job == null ? 0 : jobManager.getLevel(player, job)));
        text = text.replace("%job_xp%", String.valueOf(job == null ? 0 : jobManager.getXp(player, job)));
        for (String joinedJob : jobManager.getJobs(player)) {
            text = text.replace("%" + joinedJob.toLowerCase() + "_job_level%", String.valueOf(jobManager.getLevel(player, joinedJob)));
            text = text.replace("%" + joinedJob.toLowerCase() + "_job_xp%", String.valueOf(jobManager.getXp(player, joinedJob)));
        }
        int totalSkillLevels = getTotalSkillLevels(player);
        text = text.replace("%skills%", String.valueOf(totalSkillLevels));
        text = text.replace("%skill_total_level%", String.valueOf(totalSkillLevels));
        text = text.replace("%mining_level%", String.valueOf(skillManager.getLevel(player, "mining")));
        text = text.replace("%mining_xp%", String.valueOf(skillManager.getXp(player, "mining")));
        text = text.replace("%woodcutting_level%", String.valueOf(skillManager.getLevel(player, "woodcutting")));
        text = text.replace("%woodcutting_xp%", String.valueOf(skillManager.getXp(player, "woodcutting")));
        text = text.replace("%foraging_level%", String.valueOf(skillManager.getLevel(player, "woodcutting")));
        text = text.replace("%combat_level%", String.valueOf(skillManager.getLevel(player, "combat")));
        text = text.replace("%combat_xp%", String.valueOf(skillManager.getXp(player, "combat")));
        text = text.replace("%farming_level%", String.valueOf(skillManager.getLevel(player, "farming")));
        text = text.replace("%farming_xp%", String.valueOf(skillManager.getXp(player, "farming")));
        text = text.replace("%fishing_level%", String.valueOf(skillManager.getLevel(player, "fishing")));
        text = text.replace("%fishing_xp%", String.valueOf(skillManager.getXp(player, "fishing")));
        for (String skill : List.of("farming", "mining", "combat", "fishing", "woodcutting")) {
            String placeholder = skill.equals("woodcutting") ? "foraging" : skill;
            text = text.replace("%" + placeholder + "_progress%", String.valueOf(skillProgress(player, skill)));
        }
        for (String skill : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("skills.enabled-skills")) {
            String key = skill.toLowerCase();
            text = text.replace("%" + key + "_level%", String.valueOf(skillManager.getLevel(player, key)));
            text = text.replace("%" + key + "_xp%", String.valueOf(skillManager.getXp(player, key)));
        }
        for (String skill : List.of("unarmed", "excavation", "archery", "taming", "acrobatics", "alchemy", "repair", "smelting", "enchanting")) {
            int level = skillManager.getXp(player, skill) <= 0 ? 0 : skillManager.getLevel(player, skill);
            long xp = Math.max(0L, skillManager.getXp(player, skill));
            int needed = level <= 0 ? 0 : Math.max(0, skillManager.getXpNeededForLevel(level));
            int progress = level <= 0 ? 0 : skillProgress(player, skill);
            for (String prefix : List.of("smp_", "fakevival_", "")) {
                text = text.replace("%" + prefix + skill + "_level%", String.valueOf(level));
                text = text.replace("%" + prefix + skill + "_xp%", String.valueOf(xp));
                text = text.replace("%" + prefix + skill + "_xp_needed%", String.valueOf(needed));
                text = text.replace("%" + prefix + skill + "_progress%", String.valueOf(progress));
            }
        }
        if (usesAbelixPlaceholders(text)) refreshAbelixCacheAsync(player);
        AbelixCache cachedAbelix = abelixCache.getOrDefault(player.getUniqueId(), AbelixCache.EMPTY);
        text = text.replace("%abelix_prefix%", resolvedPrefix(player, cachedAbelix.prefix));
        text = text.replace("%abelix_gold%", cachedAbelix.gold);
        text = text.replace("%gold%", cachedAbelix.gold);
        text = text.replace("%coins%", economyManager == null ? "0" : economyManager.format(economyManager.getCoins(player)));
        BankManager.BankAccount bank = bankManager == null ? null : bankManager.getCachedAccount(player.getUniqueId());
        text = text.replace("%bank_balance%", bank == null ? "0" : Objects.requireNonNull(economyManager).format(bank.bankBalance()));

        if (containsMmoPlaceholder(text)) {
            MMOStatService.MMOSnapshot snapshot = mmoStatService == null ? null : mmoStatService.getSnapshot(player);
            text = text.replace("%current_mmo_hp%", String.valueOf(snapshot == null ? 0 : snapshot.health()));
            text = text.replace("%max_mmo_hp%", String.valueOf(snapshot == null ? 0 : snapshot.maxHealth()));
            text = text.replace("%defense%", String.valueOf(snapshot == null ? 100 : snapshot.defense()));
            text = text.replace("%max_defense%", String.valueOf(snapshot == null ? 100 : snapshot.maxDefense()));
            text = text.replace("%smp_defense%", String.valueOf(snapshot == null ? 100 : snapshot.defense()));
            text = text.replace("%smp_max_defense%", String.valueOf(snapshot == null ? 100 : snapshot.maxDefense()));
            text = text.replace("%fakevival_defense%", String.valueOf(snapshot == null ? 100 : snapshot.defense()));
            text = text.replace("%fakevival_max_defense%", String.valueOf(snapshot == null ? 100 : snapshot.maxDefense()));
            text = text.replace("%current_mana%", String.valueOf(snapshot == null ? 0 : snapshot.mana()));
            text = text.replace("%max_mana%", String.valueOf(snapshot == null ? 0 : snapshot.maxMana()));
        }

        ContextView context = contextView(player);
        ZoneManager.ZoneSnapshot zone = context.zone();
        String locationName = locationDisplayResolver.resolve(zone, context.dungeon().active() ? context.dungeon().name() : null);
        text = text.replace("%zone_name%", locationName);
        text = text.replace("%location%", locationName);
        text = text.replace("%location_name%", locationName);
        text = text.replace("%smp_location%", locationName);
        text = text.replace("%smp_location_name%", locationName);
        text = text.replace("%fakevival_location%", locationName);
        text = text.replace("%fakevival_location_name%", locationName);
        text = text.replace("%zone_level%", String.valueOf(zone == null ? 0 : zone.level()));
        text = text.replace("%area_progress%", String.valueOf(zone == null ? 0 : zone.progress()));
        DungeonView dungeon = context.dungeon();
        text = text.replace("%dungeon_name%", dungeon.name());
        text = text.replace("%dungeon_mobs_alive%", String.valueOf(dungeon.mobsAlive()));
        text = text.replace("%dungeon_boss_status%", dungeon.bossStatus());
        text = text.replace("%dungeon_progress%", String.valueOf(dungeon.progress()));
        text = text.replace("%dungeon_deaths%", "0");
        text = text.replace("%dungeon_score%", String.valueOf(dungeon.progress() * 10));
        BossView boss = context.boss();
        text = text.replace("%boss_status%", boss.status());
        text = text.replace("%boss_name%", boss.name());
        text = text.replace("%boss_hp%", String.valueOf(boss.health()));
        text = text.replace("%boss_max_hp%", String.valueOf(boss.maxHealth()));
        String petId = companionManager == null || !companionManager.isActive(player) ? "" : companionManager.selected(player);
        text = text.replace("%pet_name%", petId.isBlank() ? "None" : stripColor(companionManager.displayName(petId)));
        text = text.replace("%pet_level%", String.valueOf(petId.isBlank() ? 0 : companionManager.level(player, petId)));

        if (containsDetailedMmoPlaceholder(text)) {
            MMOStatMap stats = mmoStatService == null ? MMOStatMap.empty() : mmoStatService.getTotalStats(player);
            text = text.replace("%speed%", formatStat(stats.get(MMOStatType.SPEED)));
            text = text.replace("%strength%", formatStat(stats.get(MMOStatType.STRENGTH)));
            text = text.replace("%crit_chance%", formatStat(stats.get(MMOStatType.CRIT_CHANCE)));
            text = text.replace("%crit_damage%", formatStat(stats.get(MMOStatType.CRIT_DAMAGE)));
            text = text.replace("%damage%", formatStat(stats.get(MMOStatType.DAMAGE)));
            text = text.replace("%smp_damage%", formatStat(stats.get(MMOStatType.DAMAGE)));
            text = text.replace("%smp_strength%", formatStat(stats.get(MMOStatType.STRENGTH)));
            text = text.replace("%smp_crit_chance%", formatStat(stats.get(MMOStatType.CRIT_CHANCE)));
            text = text.replace("%smp_crit_damage%", formatStat(stats.get(MMOStatType.CRIT_DAMAGE)));
        }
        if (battlePassManager != null) {
            text = text.replace("%battlepass_level%", String.valueOf(battlePassManager.getLevel(player)));
            text = text.replace("%battlepass_xp%", String.valueOf(battlePassManager.getXp(player)));
        }
        if (eventManager != null) {
            text = text.replace("%smp_event_name%", eventManager.placeholder(player, "name"));
            text = text.replace("%smp_event_name_colored%", eventManager.placeholder(player, "name_colored"));
            text = text.replace("%smp_event_status%", eventManager.placeholder(player, "status"));
            text = text.replace("%smp_event_state%", eventManager.placeholder(player, "state"));
            text = text.replace("%smp_event_time_left%", eventManager.placeholder(player, "time_left"));
            text = text.replace("%smp_event_next%", eventManager.placeholder(player, "next"));
            text = text.replace("%smp_event_next_name%", eventManager.placeholder(player, "next_name"));
            text = text.replace("%smp_event_next_time%", eventManager.placeholder(player, "next_time"));
            text = text.replace("%smp_event_next_minecraft_days%", eventManager.placeholder(player, "next_minecraft_days"));
            text = text.replace("%smp_event_duration%", eventManager.placeholder(player, "duration"));
            text = text.replace("%smp_event_progress%", eventManager.placeholder(player, "progress"));
            text = text.replace("%smp_event_score%", eventManager.placeholder(player, "score"));
            text = text.replace("%smp_event_player_score%", eventManager.placeholder(player, "player_score"));
            text = text.replace("%smp_event_server_scope%", eventManager.placeholder(player, "server_scope"));
            text = text.replace("%smp_event_line_1%", eventManager.placeholder(player, "line_1"));
            text = text.replace("%smp_event_line_2%", eventManager.placeholder(player, "line_2"));
            text = text.replace("%smp_event_line_3%", eventManager.placeholder(player, "line_3"));
            text = text.replace("%event_name%", eventManager.placeholder(player, "name"));
            text = text.replace("%event_progress%", eventManager.placeholder(player, "progress"));
        }
        text = applyPlaceholderApi(player, text);
        text = text.replace("%profile_name%", "Default");
        text = text.replace("%event_status%", eventManager == null ? "Inactive" : eventManager.placeholder(player, "status"));
        text = text.replace("%current_event%", eventManager == null ? "None" : eventManager.placeholder(player, "name"));
        text = text.replace("%next_event%", eventManager == null ? "None" : eventManager.placeholder(player, "next"));
        text = text.replace("%event_time_left%", eventManager == null ? "--" : eventManager.placeholder(player, "time_left"));
        return CC.color(text).replace("\\n", "\n");
    }

    private String playerListDisplay(Player player) {
        String base = parse(player, "%player_level_display%%abelix_prefix%%player%");
        String tag = guildManager == null ? "" : CC.color(guildManager.getGuildTag(player));
        return ChatColor.stripColor(tag).isBlank() ? base : base + " " + tag;
    }

    public String getConfigText(String path, String def) {
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isList(path)) {
            List<String> list = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path);
            return String.join("\n", list);
        }
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path, def);
    }

    private int getTotalSkillLevels(Player player) {
        int total = 0;
        List<String> skills = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("skills.enabled-skills");
        if (skills.isEmpty()) return 0;
        for (String skill : skills) total += skillManager.getLevel(player, skill);
        return total;
    }

    private boolean containsMmoPlaceholder(String text) {
        return text.contains("%current_mmo_hp%") || text.contains("%max_mmo_hp%")
                || text.contains("%defense%") || text.contains("%max_defense%") || text.contains("%smp_defense%") || text.contains("%smp_max_defense%")
                || text.contains("%fakevival_defense%") || text.contains("%fakevival_max_defense%")
                || text.contains("%current_mana%") || text.contains("%max_mana%");
    }

    private boolean containsDetailedMmoPlaceholder(String text) {
        return text.contains("%speed%") || text.contains("%strength%") || text.contains("%damage%")
                || text.contains("%crit_chance%") || text.contains("%crit_damage%") || text.contains("%smp_damage%")
                || text.contains("%smp_strength%") || text.contains("%smp_crit_chance%") || text.contains("%smp_crit_damage%");
    }

    private int skillProgress(Player player, String skill) {
        int needed = Math.max(1, skillManager.getXpNeededForLevel(skillManager.getLevel(player, skill)));
        return Math.clamp((int) Math.round(skillManager.getXpIntoLevel(player, skill) * 100.0D / needed), 0, 100);
    }

    private int totalSkillXp(Player player) {
        int total = 0;
        for (String skill : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("skills.enabled-skills")) {
            total += Math.max(0, skillManager.getXp(player, skill));
        }
        return total;
    }

    private int totalSkillXpNeeded(Player player) {
        int total = totalSkillXp(player);
        for (String skill : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("skills.enabled-skills")) {
            int level = skillManager.getLevel(player, skill);
            total += Math.max(0, skillManager.getXpNeededForLevel(level) - skillManager.getXpIntoLevel(player, skill));
        }
        return total;
    }

    private int playtimeSeconds(Player player) {
        if (playtimeRewardManager != null) return Math.max(0, playtimeRewardManager.getPlaytimeSeconds(player));
        return Math.max(0, player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20);
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = seconds % 3600 / 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }

    private String formatStat(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001D) return String.valueOf((int) Math.rint(value));
        return String.format(Locale.US, "%.1f", value);
    }

    private String stripColor(String text) {
        String stripped = ChatColor.stripColor(CC.color(text == null ? "" : text));
        return stripped == null || stripped.isBlank() ? "None" : stripped;
    }

    private void warmBankCache(Player player) {
        if (bankManager == null || bankManager.getCachedAccount(player.getUniqueId()) != null) return;
        UUID uuid = player.getUniqueId();
        if (!bankRefreshInProgress.add(uuid)) return;
        bankManager.getAccountAsync(uuid, ignored -> bankRefreshInProgress.remove(uuid));
    }

    private String resolvedPrefix(Player player, String abelixPrefix) {
        if (abelixPrefix != null && !abelixPrefix.isBlank() && !abelixPrefix.equalsIgnoreCase("null")) return abelixPrefix;
        String display = player.getDisplayName();
        if (display == null || display.isBlank()) return "&7";
        String clean = ChatColor.stripColor(display);
        if (clean == null || clean.equals(player.getName())) return "&7";
        int nameIndex = display.lastIndexOf(player.getName());
        return nameIndex <= 0 ? "&7" : display.substring(0, nameIndex);
    }

    private ContextView contextView(Player player) {
        long now = System.currentTimeMillis();
        ContextView cached = contextCache.get(player.getUniqueId());
        if (cached != null && now - cached.loadedAtMillis() < 1500L) return cached;
        ZoneManager.ZoneSnapshot zone = zoneManager == null ? null : zoneManager.getSnapshot(player);
        DungeonView dungeon = DungeonView.NONE;
        if (dungeonManager != null) {
            DungeonManager.DungeonSnapshot snapshot = dungeonManager.getSnapshot(player);
            if (snapshot.active()) {
                dungeon = new DungeonView(true, snapshot.name(), snapshot.mobsAlive(), snapshot.bossStatus(), snapshot.progress());
            }
        }
        if (!dungeon.active() && zone != null && zone.dungeon()) {
            dungeon = new DungeonView(true, zone.name(), zone.mobsAlive(), zone.bossStatus(), zone.progress());
        }
        BossView boss = BossView.NONE;
        if (dungeonManager != null) {
            DungeonManager.BossSnapshot snapshot = dungeonManager.getBossSnapshot(player);
            if (snapshot.active()) boss = new BossView(true, snapshot.name(), snapshot.status(), snapshot.health(), snapshot.maxHealth());
        }
        if (!boss.active() && zoneManager != null) {
            ZoneManager.BossSnapshot snapshot = zoneManager.getBossSnapshot(player);
            if (snapshot.active()) boss = new BossView(true, snapshot.name(), snapshot.status(), snapshot.health(), snapshot.maxHealth());
        }
        ContextView view = new ContextView(zone, dungeon, boss, now);
        contextCache.put(player.getUniqueId(), view);
        return view;
    }

    private String applyPlaceholderApi(Player player, String text) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return text;
        try {
            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Object parsed = api.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class)
                    .invoke(null, player, text);
            return parsed instanceof String value ? value : text;
        } catch (ReflectiveOperationException ignored) {
            return text;
        }
    }

    private String pretty(String text) {
        if (text == null || text.isEmpty()) return "None";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private boolean usesAbelixPlaceholders(String text) {
        return text.contains("%abelix_prefix%") || text.contains("%abelix_gold%") || text.contains("%gold%");
    }

    public void refreshAbelixNow(Player player) {
        if (player == null || !player.isOnline() || !abelixHook.isHooked()) {
            return;
        }
        abelixCache.put(player.getUniqueId(), new AbelixCache(abelixHook.getPrefix(player), abelixHook.getGold(player), System.currentTimeMillis()));
        abelixRefreshInProgress.remove(player.getUniqueId());
    }

    public String locationName(Player player) {
        ContextView context = contextView(player);
        return locationDisplayResolver.resolve(context.zone(), context.dungeon().active() ? context.dungeon().name() : null);
    }

    public LocationDisplayResolver.ValidationReport validateLocationResolver() {
        return locationDisplayResolver.validate();
    }

    public void remove(Player player) {
        if (player == null) return;
        clearFakeEntries(player);
        UUID uuid = player.getUniqueId();
        abelixCache.remove(uuid);
        abelixRefreshInProgress.remove(uuid);
        bankRefreshInProgress.remove(uuid);
        contextCache.remove(uuid);
    }

    public void stop() {
        for (Player player : Bukkit.getOnlinePlayers()) clearFakeEntries(player);
    }

    private void clearFakeEntries(Player viewer) {
        Set<Integer> slots = fakeSlotsByViewer.remove(viewer.getUniqueId());
        if (slots == null || slots.isEmpty() || !(viewer instanceof CraftPlayer craftViewer) || !viewer.isOnline()) return;
        craftViewer.getHandle().connection.send(new ClientboundPlayerInfoRemovePacket(
                slots.stream().map(slot -> fakeEntryUuid(viewer, slot)).toList()));
    }

    private void refreshAbelixCacheAsync(Player player) {
        if (player == null || !abelixHook.isHooked()) return;
        UUID uuid = player.getUniqueId();
        AbelixCache current = abelixCache.get(uuid);
        long now = System.currentTimeMillis();
        if (current != null && now - current.loadedAtMillis < ABELIX_CACHE_MILLIS) return;
        if (!abelixRefreshInProgress.add(uuid)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String prefix = "";
            String gold = "0";
            try {
                if (player.isOnline()) {
                    prefix = abelixHook.getPrefix(player);
                    gold = abelixHook.getGold(player);
                }
            } finally {
                abelixRefreshInProgress.remove(uuid);
            }
            abelixCache.put(uuid, new AbelixCache(prefix, gold, System.currentTimeMillis()));
        });
    }

    private record AbelixCache(String prefix, String gold, long loadedAtMillis) {
        private static final AbelixCache EMPTY = new AbelixCache("", "0", 0L);
    }

    private record DungeonView(boolean active, String name, int mobsAlive, String bossStatus, int progress) {
        private static final DungeonView NONE = new DungeonView(false, "None", 0, "None", 0);
    }

    private record BossView(boolean active, String name, String status, int health, int maxHealth) {
        private static final BossView NONE = new BossView(false, "None", "None", 0, 0);
    }

    private record ContextView(ZoneManager.ZoneSnapshot zone, DungeonView dungeon, BossView boss, long loadedAtMillis) {
    }
}
