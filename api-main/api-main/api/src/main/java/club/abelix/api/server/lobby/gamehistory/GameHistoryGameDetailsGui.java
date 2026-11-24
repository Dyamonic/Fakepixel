package club.abelix.api.server.lobby.gamehistory;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamehistory.model.GameInfo;
import club.abelix.api.base.gamehistory.model.PartyInfo;
import club.abelix.api.base.gamehistory.model.PlayerInfo;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.game.team.color.TeamColor;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GameHistoryGameDetailsGui extends Gui {

    private static final int ROWS = 6;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final GameHistoryLobbyGui previousGui;
    private final Gamer target;
    private final GameInfo game;
    private final Set<Long> winnerIds;
    private final List<PlayerInfo> players;
    private final List<PartyGroup> partyGroups;
    private final Map<Long, PartyGroup> partyGroupsByPlayerId;
    private final List<TeamGroup> teamGroups;

    private final PagedZone<PlayerInfo> pagedZone = new PagedZone<>(this) {
        @Override
        protected Collection<PlayerInfo> getItems() {
            return players;
        }

        @Override
        protected void drawItem(PlayerInfo player, int slot) {
            GameHistoryGameDetailsGui.this.drawPlayerItem(player, slot);
        }
    };

    public static void open(@NonNull GameHistoryLobbyGui previousGui,
                            @NonNull GameInfo game,
                            @NonNull Gamer target) {
        HeartBeat.runSync(() -> new GameHistoryGameDetailsGui(previousGui.getPlayer(), previousGui, target, game));
    }

    private GameHistoryGameDetailsGui(@NonNull Player viewer,
                                      @NonNull GameHistoryLobbyGui previousGui,
                                      @NonNull Gamer target,
                                      @NonNull GameInfo game) {
        super(viewer, resolveTitle(viewer), ROWS);

        this.previousGui = previousGui;
        this.target = target;
        this.game = game;
        this.winnerIds = resolveWinnerIds(game);
        this.players = sortPlayers(game.getPlayers() != null ? game.getPlayers() : Collections.emptyList());
        this.partyGroups = buildPartyGroups(players);
        this.partyGroupsByPlayerId = indexPartyGroups(partyGroups);
        this.teamGroups = buildTeamGroups(players);

        pagedZone.addSlotRect(1, 1, 7, 4);

        draw();
    }

    @Override
    public void draw() {
        for (int i = 0; i < getSize(); i++) {
            remove(i);
        }

        drawBorder(BORDER_SLOTS_FOR_SIX_ROWS_GUI);
        drawOverviewItem();
        drawWinnersItem();
        drawTeamsItem();
        drawPartiesItem();
        pagedZone.draw();
        pagedZone.drawButtons();
        generateBackItem(48, e -> previousGui.reopen());
        generateCloseItem(49);
    }

    private void drawOverviewItem() {
        boolean targetWon = winnerIds.contains(target.getPlayerId());
        PlayerInfo targetPlayer = findPlayer(target.getPlayerId());
        Material material = targetWon ? Material.DIAMOND_SWORD : Material.IRON_SWORD;

        add(ItemUtil.getBuilder(material)
                .setName(translated(targetWon
                                ? "game_history_details_overview_name_win"
                                : "game_history_details_overview_name_lose",
                        "%mode%", resolveModeDisplayName(),
                        "%map%", resolveMapDisplayName()))
                .setLore(translatedLore("game_history_details_overview_lore",
                        "%target%", fallback(target.getPlayerName()),
                        "%result%", translated(targetWon
                                ? "game_history_details_result_win"
                                : "game_history_details_result_lose"),
                        "%mode%", resolveModeDisplayName(),
                        "%map%", resolveMapDisplayName(),
                        "%started%", formatDate(game.getStartTime()),
                        "%ended%", formatDate(game.getEndTime()),
                        "%duration%", formatDuration(game.getEndTime() - game.getStartTime()),
                        "%players%", players.size(),
                        "%privacy%", translated(game.isPrivateGame()
                                ? "game_history_details_privacy_private"
                                : "game_history_details_privacy_public"),
                        "%server%", fallback(game.getServerName()),
                        "%game_id%", fallback(game.getGameId()),
                        "%target_team%", targetPlayer != null
                                ? describeTeam(targetPlayer)
                                : translated("game_history_details_team_none"),
                        "%target_party%", targetPlayer != null
                                ? describePartyShort(targetPlayer)
                                : translated("game_history_details_party_solo")))
                .build(), 1);
    }

    private void drawWinnersItem() {
        List<String> winnerNames = players.stream()
                .filter(this::isWinner)
                .map(this::playerName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        List<String> lore = new ArrayList<>(translatedLore("game_history_details_winners_lore",
                "%winners_count%", winnerNames.size()));

        if (winnerNames.isEmpty()) {
            lore.addAll(translatedLore("game_history_details_winners_empty"));
        } else {
            addNameChunks(lore, "game_history_details_winners_players_prefix", winnerNames, ChatColor.GREEN.toString());
        }

        add(ItemUtil.getBuilder(Material.NETHER_STAR)
                .setName(translated("game_history_details_winners_name"))
                .setLore(lore)
                .build(), 3);
    }

    private void drawTeamsItem() {
        List<String> lore = new ArrayList<>(translatedLore("game_history_details_teams_lore",
                "%teams_count%", teamGroups.size()));

        if (teamGroups.isEmpty()) {
            lore.addAll(translatedLore("game_history_details_teams_empty"));
        } else {
            for (TeamGroup group : teamGroups) {
                lore.add(translated("game_history_details_teams_group",
                        "%team%", describeTeam(group),
                        "%count%", group.members.size()));
                addNameChunks(lore, "game_history_details_teams_members_prefix", sortedNames(group.members), ChatColor.WHITE.toString());
            }
        }

        add(ItemUtil.getBuilder(Material.WOOL)
                .setName(translated("game_history_details_teams_name"))
                .setLore(lore)
                .build(), 5);
    }

    private void drawPartiesItem() {
        List<String> lore = new ArrayList<>(translatedLore("game_history_details_parties_lore",
                "%parties_count%", partyGroups.size()));

        if (partyGroups.isEmpty()) {
            lore.addAll(translatedLore("game_history_details_parties_empty"));
        } else {
            for (PartyGroup group : partyGroups) {
                lore.add(translated("game_history_details_parties_group",
                        "%party%", describeParty(group)));
                lore.add(translated("game_history_details_parties_group_id",
                        "%party_id%", hasText(group.partyId) ? group.partyId : noneValue()));
                addNameChunks(lore, "game_history_details_parties_members_prefix", sortedNames(group.members), ChatColor.AQUA.toString());
            }
        }

        List<String> soloNames = players.stream()
                .filter(player -> !isInParty(player))
                .map(this::playerName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        lore.addAll(translatedLore("game_history_details_parties_solo_lore",
                "%solo_count%", soloNames.size()));

        if (soloNames.isEmpty()) {
            lore.addAll(translatedLore("game_history_details_parties_solo_empty"));
        } else {
            addNameChunks(lore, "game_history_details_parties_solo_prefix", soloNames, ChatColor.WHITE.toString());
        }

        add(ItemUtil.getBuilder(Material.NAME_TAG)
                .setName(translated("game_history_details_parties_name"))
                .setLore(lore)
                .build(), 7);
    }

    private void drawPlayerItem(PlayerInfo player, int slot) {
        List<String> lore = new ArrayList<>(translatedLore("game_history_details_player_lore",
                "%player_id%", player.getPlayerId(),
                "%result%", translated(isWinner(player)
                        ? "game_history_details_result_win"
                        : "game_history_details_result_lose"),
                "%team%", describeTeam(player),
                "%party%", describePartyShort(player),
                "%kills%", formatKills(player)));

        PartyGroup partyGroup = partyGroupsByPlayerId.get(player.getPlayerId());
        if (partyGroup != null) {
            lore.add(translated("game_history_details_player_party_role",
                    "%role%", translated(isPartyLeader(player, partyGroup)
                            ? "game_history_details_party_role_leader"
                            : "game_history_details_party_role_member")));
            addNameChunks(lore, "game_history_details_player_party_members_prefix", sortedNames(partyGroup.members), ChatColor.AQUA.toString());
        }

        Map<String, String> metadata = metadataOf(player);
        if (!metadata.isEmpty() && getGamer().hasPermission("gamehistory.metadata")) {
            lore.addAll(translatedLore("game_history_details_player_metadata_header"));
            metadata.entrySet().stream()
                    .sorted((left, right) -> left.getKey().compareToIgnoreCase(right.getKey()))
                    .forEach(entry -> lore.add(translated("game_history_details_player_metadata_entry",
                            "%key%", entry.getKey(),
                            "%value%", entry.getValue())));
        }

        HeartBeat.runAsync(() -> {
            ItemUtil.Builder builder;
            Gamer onlineGamer = Gamer.getOrCreate(playerName(player), gamer -> {
                gamer.loadRanks();
                gamer.loadPrefixAndUpdateName();
                gamer.loadSkull();
            });
            if (onlineGamer != null) {
                builder = ItemUtil.getBuilder(onlineGamer.getSkull());
            } else {
                builder = ItemUtil.getBuilder(Material.SKULL_ITEM).setDurability(3);
            }

            HeartBeat.runSync(()-> {
                add(builder
                        .setName(playerDisplayName(player))
                        .setLore(lore)
                        .build(), slot);
                getGamer().getPlayer().updateInventory();
            });
        });

    }

    private List<PlayerInfo> sortPlayers(List<PlayerInfo> players) {
        return players.stream()
                .sorted(Comparator
                        .comparing((PlayerInfo player) -> player.getPlayerId() != target.getPlayerId())
                        .thenComparing((PlayerInfo player) -> !isWinner(player))
                        .thenComparing(this::playerName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private Set<Long> resolveWinnerIds(GameInfo game) {
        Set<Long> result = new HashSet<>();

        if (game.getWinners() != null) {
            game.getWinners().forEach(winner -> result.add(winner.getPlayerId()));
        }

        if (game.getPlayers() != null) {
            game.getPlayers().stream()
                    .filter(player -> booleanMetadata(player, "winner"))
                    .forEach(player -> result.add(player.getPlayerId()));
        }

        return result;
    }

    private List<PartyGroup> buildPartyGroups(List<PlayerInfo> players) {
        Map<String, List<PlayerInfo>> grouped = new LinkedHashMap<>();

        for (PlayerInfo player : players) {
            if (!isInParty(player)) {
                continue;
            }

            grouped.computeIfAbsent(resolvePartyKey(player), key -> new ArrayList<>()).add(player);
        }

        return grouped.values().stream()
                .map(this::buildPartyGroup)
                .sorted(Comparator
                        .comparingInt((PartyGroup group) -> -group.members.size())
                        .thenComparing(group -> group.leaderName != null ? group.leaderName : "", String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private PartyGroup buildPartyGroup(List<PlayerInfo> members) {
        List<PlayerInfo> sortedMembers = members.stream()
                .sorted(Comparator.comparing(this::playerName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        String partyId = sortedMembers.stream()
                .map(this::resolvePartyId)
                .filter(this::hasText)
                .findFirst()
                .orElse(null);

        Long leaderId = sortedMembers.stream()
                .map(this::resolvePartyLeaderId)
                .filter(id -> id != null && id > 0)
                .findFirst()
                .orElse(null);

        String leaderName = sortedMembers.stream()
                .map(this::resolvePartyLeaderName)
                .filter(this::hasText)
                .findFirst()
                .orElse(null);

        if (leaderId != null) {
            leaderName = sortedMembers.stream()
                    .filter(player -> player.getPlayerId() == leaderId)
                    .map(this::playerName)
                    .findFirst()
                    .orElse(leaderName);
        }

        if (!hasText(leaderName) && !sortedMembers.isEmpty()) {
            leaderName = playerName(sortedMembers.get(0));
        }

        return new PartyGroup(partyId, leaderId, leaderName, sortedMembers);
    }

    private Map<Long, PartyGroup> indexPartyGroups(List<PartyGroup> groups) {
        Map<Long, PartyGroup> result = new HashMap<>();
        for (PartyGroup group : groups) {
            for (PlayerInfo member : group.members) {
                result.put(member.getPlayerId(), group);
            }
        }
        return result;
    }

    private List<TeamGroup> buildTeamGroups(List<PlayerInfo> players) {
        Map<String, List<PlayerInfo>> grouped = new LinkedHashMap<>();

        for (PlayerInfo player : players) {
            if (!hasTeam(player)) {
                continue;
            }

            grouped.computeIfAbsent(resolveTeamKey(player), key -> new ArrayList<>()).add(player);
        }

        return grouped.values().stream()
                .map(this::buildTeamGroup)
                .sorted(Comparator
                        .comparing((TeamGroup group) -> group.index != null ? group.index : Integer.MAX_VALUE)
                        .thenComparing(group -> group.character != null ? group.character : "")
                        .thenComparing(group -> group.colorName != null ? group.colorName : "", String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private TeamGroup buildTeamGroup(List<PlayerInfo> members) {
        List<PlayerInfo> sortedMembers = members.stream()
                .sorted(Comparator.comparing(this::playerName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        PlayerInfo first = sortedMembers.get(0);
        return new TeamGroup(resolveTeamIndex(first), resolveTeamCharacter(first), resolveTeamColorName(first), sortedMembers);
    }

    private PlayerInfo findPlayer(long playerId) {
        return players.stream()
                .filter(player -> player.getPlayerId() == playerId)
                .findFirst()
                .orElse(null);
    }

    private boolean isWinner(PlayerInfo player) {
        return winnerIds.contains(player.getPlayerId()) || booleanMetadata(player, "winner");
    }

    private boolean hasTeam(PlayerInfo player) {
        return resolveTeamIndex(player) != null
                || hasText(resolveTeamCharacter(player))
                || hasText(resolveTeamColorName(player));
    }

    private String describeTeam(PlayerInfo player) {
        if (!hasTeam(player)) {
            return translated("game_history_details_team_none");
        }

        return describeTeam(new TeamGroup(
                resolveTeamIndex(player),
                resolveTeamCharacter(player),
                resolveTeamColorName(player),
                List.of(player)
        ));
    }

    private String describeTeam(TeamGroup group) {
        List<String> parts = new ArrayList<>();

        if (group.index != null) {
            parts.add("#" + group.index);
        }

        if (hasText(group.character)) {
            parts.add("[" + group.character + "]");
        }

        String colorName = formatTeamColor(group.colorName);
        if (hasText(colorName)) {
            parts.add(colorName);
        }

        return parts.isEmpty() ? translated("game_history_details_team_none") : String.join(" ", parts);
    }

    private String describePartyShort(PlayerInfo player) {
        if (!isInParty(player)) {
            return translated("game_history_details_party_solo");
        }

        PartyGroup group = partyGroupsByPlayerId.get(player.getPlayerId());
        if (group != null) {
            return describeParty(group);
        }

        String leaderName = resolvePartyLeaderName(player);
        if (hasText(leaderName)) {
            return leaderName;
        }

        return translated("game_history_details_party_generic");
    }

    private String describeParty(PartyGroup group) {
        String leaderName = hasText(group.leaderName) ? group.leaderName : translated("game_history_details_value_unknown");

        if (group.leaderId != null && group.leaderId > 0) {
            return translated("game_history_details_party_description_with_id",
                    "%leader%", leaderName,
                    "%size%", group.members.size(),
                    "%leader_id%", group.leaderId);
        }

        return translated("game_history_details_party_description",
                "%leader%", leaderName,
                "%size%", group.members.size());
    }

    private boolean isInParty(PlayerInfo player) {
        return player.getParty() != null || booleanMetadata(player, "in_party");
    }

    private boolean isPartyLeader(PlayerInfo player, PartyGroup group) {
        if (group.leaderId != null && group.leaderId > 0) {
            return group.leaderId == player.getPlayerId();
        }

        return hasText(group.leaderName) && group.leaderName.equalsIgnoreCase(playerName(player));
    }

    private String resolvePartyKey(PlayerInfo player) {
        if (hasText(resolvePartyId(player))) {
            return "id:" + resolvePartyId(player);
        }

        Long leaderId = resolvePartyLeaderId(player);
        if (leaderId != null && leaderId > 0) {
            return "leader:" + leaderId;
        }

        String leaderName = resolvePartyLeaderName(player);
        if (hasText(leaderName)) {
            return "leader-name:" + leaderName.toLowerCase(Locale.ROOT);
        }

        return "player:" + player.getPlayerId();
    }

    private String resolvePartyId(PlayerInfo player) {
        PartyInfo party = player.getParty();
        return party != null && hasText(party.getPartyId()) ? party.getPartyId() : null;
    }

    private String resolvePartyLeaderName(PlayerInfo player) {
        PartyInfo party = player.getParty();
        if (party != null && party.getLeader() != null && hasText(party.getLeader().getPlayerName())) {
            return party.getLeader().getPlayerName();
        }

        String leaderName = metadataOf(player).get("party_leader_name");
        return hasText(leaderName) ? leaderName : null;
    }

    private Long resolvePartyLeaderId(PlayerInfo player) {
        PartyInfo party = player.getParty();
        if (party != null && party.getLeader() != null && party.getLeader().getPlayerId() > 0) {
            return party.getLeader().getPlayerId();
        }

        return parseLong(metadataOf(player).get("party_leader_id"));
    }

    private Integer resolveTeamIndex(PlayerInfo player) {
        Long value = parseLong(metadataOf(player).get("team_index"));
        return value != null ? value.intValue() : null;
    }

    private String resolveTeamCharacter(PlayerInfo player) {
        String value = metadataOf(player).get("team_character");
        return hasText(value) ? value : null;
    }

    private String resolveTeamColorName(PlayerInfo player) {
        String value = metadataOf(player).get("team_color");
        return hasText(value) ? value : null;
    }

    private String resolveTeamKey(PlayerInfo player) {
        Integer teamIndex = resolveTeamIndex(player);
        if (teamIndex != null) {
            return "index:" + teamIndex;
        }

        String teamCharacter = resolveTeamCharacter(player);
        if (hasText(teamCharacter)) {
            return "character:" + teamCharacter;
        }

        return "color:" + resolveTeamColorName(player);
    }

    private String resolveModeDisplayName() {
        Mini mini = resolveMini(game.getMode());
        return mini != null ? mini.getMaster().getDisplayName() + " " + mini.getDisplayName() : fallback(game.getMode());
    }

    private String resolveMapDisplayName() {
        if (!hasText(game.getMapName())) {
            return noneValue();
        }

        String translatedMap = language.getMessage(game.getMapName());
        return hasText(translatedMap) ? translatedMap : game.getMapName();
    }

    private Mini resolveMini(String modeName) {
        if (!hasText(modeName)) {
            return null;
        }

        try {
            return Mini.valueOf(modeName);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            return Mini.valueOf(modeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return noneValue();
        }

        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date(timestamp));
        }
    }

    private String formatDuration(long ms) {
        if (ms <= 0) {
            return noneValue();
        }

        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes > 0) {
            return translated("game_history_duration_minutes_seconds",
                    "%minutes%", minutes,
                    "%seconds%", seconds);
        }

        return translated("game_history_duration_seconds", "%seconds%", seconds);
    }

    private String playerDisplayName(PlayerInfo player) {
        if (player.getPlayerId() == target.getPlayerId()) {
            return translated("game_history_details_player_name_target", "%player%", playerName(player));
        }

        if (isWinner(player)) {
            return translated("game_history_details_player_name_winner", "%player%", playerName(player));
        }

        return translated("game_history_details_player_name_default", "%player%", playerName(player));
    }

    private String playerName(PlayerInfo player) {
        return fallback(player.getPlayerName());
    }

    private String formatTeamColor(String colorName) {
        if (!hasText(colorName)) {
            return null;
        }

        try {
            TeamColor teamColor = TeamColor.valueOf(colorName.toUpperCase(Locale.ROOT));
            return teamColor.getColor() + prettifyName(teamColor.name());
        } catch (IllegalArgumentException ignored) {
            return prettifyName(colorName);
        }
    }

    private String prettifyName(String value) {
        String[] parts = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
    }

    private void addNameChunks(List<String> lore, String prefixKey, List<String> names, String color) {
        String continuePrefix = translated("game_history_details_chunk_continue_prefix");
        String separator = translated("game_history_details_names_separator");

        for (int i = 0; i < names.size(); i += 3) {
            List<String> chunk = names.subList(i, Math.min(names.size(), i + 3));
            String joined = chunk.stream()
                    .map(name -> color + name)
                    .collect(Collectors.joining(separator));

            lore.add((i == 0 ? translated(prefixKey) : continuePrefix) + joined);
        }
    }

    private List<String> sortedNames(List<PlayerInfo> players) {
        return players.stream()
                .map(this::playerName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private Map<String, String> metadataOf(PlayerInfo player) {
        return player.getMetadata() != null ? player.getMetadata() : Collections.emptyMap();
    }

    private boolean booleanMetadata(PlayerInfo player, String key) {
        String value = metadataOf(player).get(key);
        return value != null && Boolean.parseBoolean(value);
    }

    private Long parseLong(String value) {
        if (!hasText(value)) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String fallback(String value) {
        return hasText(value) ? value : noneValue();
    }

    private String formatKills(PlayerInfo player) {
        Integer kills = player.getKills();
        return kills != null ? String.valueOf(kills) : noneValue();
    }

    private String noneValue() {
        return translated("game_history_value_none");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String resolveTitle(Player viewer) {
        Gamer gamer = Gamer.getGamer(viewer);
        if (gamer != null && gamer.getLanguage() != null) {
            return gamer.getLanguage().getMessage("game_history_details_title");
        }

        return Lang.of("game_history_details_title").compile();
    }

    private static final class PartyGroup {
        private final String partyId;
        private final Long leaderId;
        private final String leaderName;
        private final List<PlayerInfo> members;

        private PartyGroup(String partyId, Long leaderId, String leaderName, List<PlayerInfo> members) {
            this.partyId = partyId;
            this.leaderId = leaderId;
            this.leaderName = leaderName;
            this.members = members;
        }
    }

    private static final class TeamGroup {
        private final Integer index;
        private final String character;
        private final String colorName;
        private final List<PlayerInfo> members;

        private TeamGroup(Integer index, String character, String colorName, List<PlayerInfo> members) {
            this.index = index;
            this.character = character;
            this.colorName = colorName;
            this.members = members;
        }
    }
}
