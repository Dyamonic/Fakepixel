package club.abelix.api.server.game.history;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamehistory.GameHistoryService;
import club.abelix.api.base.gamehistory.model.GameHistoryEntryPlayer;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.party.Party;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameChangeStateEvent;
import club.abelix.api.server.game.event.GameEndEvent;
import club.abelix.api.server.game.listener.GameListener;
import club.abelix.api.server.game.stats.GameStatsCache;
import club.abelix.api.server.game.team.GameTeam;
import club.abelix.api.server.game.team.color.TeamColor;
import club.abelix.commons.data.stat.StatsField;
import club.abelix.network.common.enums.GameState;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class GameHistoryListener extends GameListener<Game> {

    private long startTime;
    private List<GameHistoryEntryPlayer> entryPlayers;

    public GameHistoryListener(@NonNull Game game) {
        super(game);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameStart(GameChangeStateEvent event) {
        checkGame(event, () -> {
            if (event.getNewState() != GameState.GAME) {
                return;
            }

            startTime = System.currentTimeMillis();
            entryPlayers = game.getGamers().stream()
                    .filter(gamer -> !gamer.isVanished())
                    .map(this::buildEntryPlayer)
                    .toList();

            log.info("[History] Game {} started, captured {} players", game.getName(), entryPlayers.size());
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameEnd(GameEndEvent event) {
        checkGame(event, () -> {
            GameHistoryService historyService = FalconAPI.getInstance().getGameHistoryService();

            if (historyService == null) {
                log.warn("[History] GameHistoryService is not initialized, skipping save for {}", game.getName());
                return;
            }

            long endTime = System.currentTimeMillis();

            List<GameHistoryEntryPlayer> players = this.entryPlayers;
            if (players == null) {
                players = game.getGamers().stream()
                        .filter(gamer -> !gamer.isVanished())
                        .map(this::buildEntryPlayer)
                        .toList();
            }

            GameTeam winnerTeam = game.getWinner();
            Set<Gamer> winnerSet = winnerTeam != null
                    ? Set.copyOf(winnerTeam.getPlayers())
                    : Collections.emptySet();
            StatsField killsField = game.getStatsMode() == null ? null : game.getStatsMode().getField("kills");

            List<GameHistoryEntryPlayer> finalPlayers = players.stream()
                    .map(entry -> enrichEntry(entry, winnerSet, killsField))
                    .toList();

            log.info("[History] Game {} saving to history with {} players...", game.getName(), finalPlayers.size());

            historyService.saveGame(
                    game.getMini(),
                    game.getName(),
                    game.getGameMap(),
                    startTime,
                    endTime,
                    finalPlayers.size(),
                    game.isPrivateGame(),
                    finalPlayers
            ).whenComplete((gameId, error) -> {
                if (error != null) {
                    log.error("[History] Failed to save game {} to history", game.getName(), error);
                } else {
                    log.info("[History] Game {} saved to history with id: {}", game.getName(), gameId);
                }
            });
        });
    }

    private GameHistoryEntryPlayer buildEntryPlayer(Gamer gamer) {
        val builder = GameHistoryEntryPlayer.builder()
                .playerId(gamer.getPlayerId())
                .playerName(gamer.getPlayerName());

        try {
            val teamManager = game.getTeamManager();
            if (teamManager != null) {
                GameTeam team = teamManager.getTeam(gamer);
                if (team != null) {
                    builder.hasTeam(true)
                            .teamIndex(team.getIndex())
                            .teamCharacter(team.getCharacter());

                    TeamColor color = team.getColor();
                    if (color != null) {
                        builder.teamColorName(color.name());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[History] Could not resolve team for {}: {}", gamer.getPlayerName(), e.getMessage());
        }

        try {
            if (gamer.hasParty()) {
                Party party = gamer.getParty();
                if (party != null) {
                    builder.inParty(true);

                    Gamer leader = party.getLeader();
                    if (leader != null) {
                        builder.partyLeaderName(leader.getPlayerName())
                                .partyLeaderId(leader.getPlayerId());
                    }

                    builder.partyMemberIds(
                            party.getMembers().stream()
                                    .map(m -> (long) m.getPlayerId())
                                    .toList()
                    );
                }
            }
        } catch (Exception e) {
            log.debug("[History] Could not resolve party for {}: {}", gamer.getPlayerName(), e.getMessage());
        }

        return builder.build();
    }

    private GameHistoryEntryPlayer enrichEntry(GameHistoryEntryPlayer entry, Set<Gamer> winnerSet, StatsField killsField) {
        Map<String, String> metadata = new HashMap<>(entry.getMetadata());
        Integer kills = resolveKills(entry.getPlayerId(), killsField);

        if (kills != null) {
            metadata.put("kills", String.valueOf(kills));
        }

        return entry.toBuilder()
                .winner(winnerSet.stream().anyMatch(gamer -> gamer.getPlayerId() == entry.getPlayerId()))
                .metadata(metadata)
                .build();
    }

    private Integer resolveKills(long playerId, StatsField killsField) {
        if (killsField == null) {
            return null;
        }

        int intPlayerId;
        try {
            intPlayerId = Math.toIntExact(playerId);
        } catch (ArithmeticException ignored) {
            return null;
        }

        GameStatsCache cache = game.getStatsManager() == null
                ? null
                : game.getStatsManager().getCacheMap().get(intPlayerId);

        if (cache != null) {
            return cache.getCachedStat(killsField, 0);
        }

        Gamer gamer = Gamer.getGamer(intPlayerId);
        return gamer != null ? gamer.getCachedStat(killsField) : 0;
    }
}
