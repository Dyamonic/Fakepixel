package club.abelix.api.server.game.team.maker;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.manager.GameGroupSelectorManager;
import club.abelix.api.server.game.team.GameTeam;
import club.abelix.api.server.game.team.selector.GameGroup;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vlundik on 1:08 19.10.2017
 */
public class SplitTeamMaker implements TeamMaker {

    // Пытается распределить список игроков с учётом пати,
    // минимизируя разделение пати по разным тимам и
    // учитыая лимит на тимы и лимит на размер тимы

    private final int maxSize;
    private final int maxTeams;
    private final boolean createAll;

    public SplitTeamMaker(int maxSize, int maxTeams) {
        this(maxSize, maxTeams, false);
    }

    public SplitTeamMaker(int maxSize, int maxTeams, boolean createAll) {
        if (maxSize < 1) {
            throw new IllegalStateException();
        }

        this.maxSize = maxSize;
        this.maxTeams = maxTeams;

        this.createAll = createAll;
    }

    @Override
    public void make(@NonNull Game game, @NonNull List<Gamer> gamers) {
        if (maxTeams != -1 && maxSize * maxTeams < gamers.size()) {
            throw new IllegalArgumentException("Can't fit " + gamers.size() + " into " + maxTeams + " x " + maxSize);
        }

        GameGroupSelectorManager selectorManager = game.getSelectorManager();

        List<GameTeam> gameTeams = new ArrayList<>();
        beforeCreate(gameTeams);

        // Проверяем, включен ли выбор групп
        if (selectorManager != null) {
            // Перебираем каждую группу
            for (GameGroup group : selectorManager.getSortedGroups()) {
                // Добавляем эти группы в команды (одна группа может добавиться сразу в несколько команд)
                addGroup(game, gameTeams, group);
            }
        } else {
            // выбора групп нет, каждый игрок по одиночке
            for (Gamer gamer : gamers) {
                // создаём специально группу для одного игрока
                addGroup(game, gameTeams, new GameGroup(game, gamer));
            }
        }

        if (!createAll) {
            return;
        }

        if (gameTeams.size() < maxTeams) {
            int needSize = maxTeams - gameTeams.size();
            Logging.IMP.info("[GameTeam] Need crate {} empty teams", needSize);

            for (int i = 0; i < needSize; i++) {
                val emptyTeam = createTeam(game);
                game.getTeamManager().addTeam(emptyTeam);

                gameTeams.add(emptyTeam);
            }
        }
    }

    protected void beforeCreate(@NonNull List<GameTeam> gameTeams) {

    }

    private void addGroup(@NonNull Game game, @NonNull List<GameTeam> gameTeams, @NonNull GameGroup group) {
        // лучшая команда, которая может вместить в себя эту группу полностью
        GameTeam best = null;

        // потенциальная команда, которая может вместить максимум игроков из этой группы
        GameTeam potential = null;
        int potentialFreeSlots = 0;

        for (GameTeam gameTeam : gameTeams) {
            int total = gameTeam.size() + group.getGamerCount();

            // требуется слишком много игроков, чтобы войти в эту команду
            if (total > maxSize) {
                // смотрим сколько свободно мест в команде
                int free = maxSize - gameTeam.size();

                // если их больше, чем в потенциальной, то это - новая потенциальная
                if (potentialFreeSlots < free) {
                    potential = gameTeam;
                }

                continue;
            }

            // места хватило всем, так что это - лучшая тима
            if (total == maxSize) {
                best = gameTeam;
                break;
            }

            // лучшая команда не инициализирована или в лучшей игроков больше, чем в этой
            // значит эта - новая лучшая команда
            if (best == null || gameTeam.size() < best.size()) {
                best = gameTeam;
            }
        }

        GameTeam team;

        // лучшая команда  не найдена
        if (best == null) {

            // команд уже создано предостаточно, ищем потенциальную
            if (gameTeams.size() == maxTeams) {
                // потенциальная есть
                if (potential != null) {
                    // разделяем на команду на столько, сколько может вместить потенциальная
                    for (GameGroup split : group.splitPlayers(potentialFreeSlots)) {
                        addGroup(game, gameTeams, split);
                    }
                }

                throw new IllegalStateException(
                        "Failed to add group to teams. " +
                                "groupSize=" + group.getGamerCount() +
                                ", teamsCount=" + gameTeams.size() +
                                ", maxTeams=" + maxTeams +
                                ", maxSize=" + maxSize +
                                ", potentialTeam=" + (potential != null ? potential.getIndex() : "null") +
                                ", potentialFreeSlots=" + potentialFreeSlots
                );

            }

            // можно создать ещё команду
            team = createTeam(game);

            gameTeams.add(team);
        } else {
            // лучшая команда найдена
            team = best;
        }

        // добавляем в эту команду игроков в этой группе
        for (Gamer gamer : group.getGamers()) {
            team.add(gamer);
        }
    }

}
