package club.abelix.api.server.game.manager;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.team.GameTeam;
import club.abelix.api.server.game.team.maker.TeamMaker;
import club.abelix.network.common.enums.GameState;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TeamManager {

    private final Game game;
    /**
     * Общий список команд
     */
    private final List<GameTeam> gameTeamList = new ArrayList<>();
    /**
     * Команды каждого Gamer'a
     */
    private final ConcurrentHashMap<String, GameTeam> gamerTeamMap = new ConcurrentHashMap<>();
    /**
     * Счётчик тим (для таба)
     */
    private final AtomicInteger teamIndexCounter = new AtomicInteger();
    @Getter
    private boolean initialized = false;

    public void initialize() {
        initialized = true;
    }

    /**
     * Создать тимы из игроков, которые в данный момент онлайн
     *
     * @param maker Создатель тим
     */
    public void makeTeams(@NonNull TeamMaker maker) {
        Preconditions.checkState(!initialized, "Teams are already created");
        Preconditions.checkState(game.getGameState() != GameState.STAND_BY, "Game is not started yet");

        Logger logger = game.getPlugin().getLogger();

        logger.info("[GameTeam] Making teams with " + maker.getClass().getSimpleName());

        List<Gamer> gamers = new ArrayList<>(game.getNotVanishedGamers());

        Preconditions.checkState(!gamers.isEmpty(), "There are no players online");

        maker.make(game, gamers);

        Preconditions.checkState(!gamerTeamMap.isEmpty() && !gameTeamList.isEmpty(), "No teams were created by " + maker);

        // Чтобы избежать ситуации когда в одной тиме находятся все игроки с арены
        // мы будем делить их пополам вне зависимости от пати (как повезет крч)
        // ибо нехуй запускать игру с одной тимой

        // количество команд в которых вообще есть игроки
        GameTeam teamWithPlayers = null;

        int count = 0;

        for (GameTeam gameTeam : gameTeamList) {
            // тима пустая - в счет не идет
            if (gameTeam.isEmpty()) {
                continue;
            }

            // тимы уже две нашлось - значит дальше не чекаем
            if (teamWithPlayers == null) {
                teamWithPlayers = gameTeam;
            }

            count++;
        }

        // если заполненная команда только одна, то делим ее на две команды
        if (teamWithPlayers != null && count == 1) {
            int playersPerTeam = teamWithPlayers.size() / 2;

            // создаем новую тиму
            GameTeam newTeam = null;
            for (val potentialTeam : gameTeamList) {
                if (potentialTeam.getAliveGamers().isEmpty()) {
                    newTeam = potentialTeam;
                    break;
                }
            }

            if (newTeam == null) {
                newTeam = maker.createTeam(game);
            }

            Iterator<Gamer> oldTeamGamers = new ArrayList<>(teamWithPlayers.getPlayers())
                    .iterator();

            for (int i = 0; i < playersPerTeam; i++) {
                Gamer gamer = oldTeamGamers.next();

                teamWithPlayers.remove(gamer);
                newTeam.add(gamer);
            }
        }

        logger.info("[GameTeam] Gamers detected: " + getPlayerNames());
        logger.info("[GameTeam] Teams were created: " + gameTeamList);

        initialize();
    }

    public int nextIndex() {
        return teamIndexCounter.getAndIncrement();
    }

    public void reset() {
        teamIndexCounter.set(0);

        gameTeamList.forEach(GameTeam::unregister);
        gameTeamList.clear();

        gamerTeamMap.clear();

        initialized = false;

        game.getPlugin().getLogger().info("[GameTeam] Teams reset");
    }

    public void addTeam(@NonNull GameTeam gameTeam) {
        if (!gameTeamList.contains(gameTeam)) {
            gameTeamList.add(gameTeam);
        }
    }

    /**
     * Установить тиму игроку
     */
    public void setTeam(@NonNull Gamer gamer, GameTeam gameTeam) {
        String name = gamer.getPlayer().getName().toLowerCase();

        if (gameTeam == null) {
            gamerTeamMap.remove(name);
        } else {
            gamerTeamMap.put(name, gameTeam);
        }
    }

    /**
     * Получить тиму игрока
     */
    public <T extends GameTeam> T getTeam(@NonNull Player player) {
        return getTeam(player.getName());
    }

    /**
     * Получить тиму игрока
     */
    public <T extends GameTeam> T getTeam(@NonNull Gamer gamer) {
        return getTeam(gamer.getPlayer());
    }

    public <T extends GameTeam> T getTeam(@NonNull String name) {
        return (T) gamerTeamMap.get(name.toLowerCase());
    }

    /**
     * Получить список всех тим
     */
    public <T extends GameTeam> List<T> getTeams() {
        return (List<T>) Collections.unmodifiableList(gameTeamList);
    }

    public <T extends GameTeam> List<T> getAliveTeams() {
        return (List<T>) gameTeamList.stream()
                .filter(GameTeam::isAlive)
                .collect(Collectors.toList());
    }

    /**
     * Получить список имен игроков во всех тимах на момент создания тим
     */
    public Set<String> getPlayerNames() {
        return Collections.unmodifiableSet(gamerTeamMap.keySet());
    }

    /**
     * Получить список геймеров во всех тимах на момент создания тим
     */
    public Set<Gamer> getPlayers() {
        Set<Gamer> gamers = new HashSet<>();

        for (GameTeam gameTeam : gamerTeamMap.values()) {
            gamers.addAll(gameTeam.getPlayers());
        }

        return gamers;
    }

    /**
     * Получить список онлайн геймеров
     */
    public Set<Gamer> getOnlinePlayers() {
        return getPlayers().stream()
                .filter(Gamer::isOnline)
                .collect(Collectors.toSet());
    }

    public void removePlayerFromAlive(@NonNull Player player) {
        GameTeam gameTeam = getTeam(player);

        if (gameTeam != null) {
            gameTeam.removePlayer(player);
        }
    }

}
