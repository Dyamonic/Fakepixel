package club.abelix.api.base.division;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.division.listener.DivisionListener;
import club.abelix.api.base.division.status.Status;
import club.abelix.api.base.division.status.event.UpdateGamerStatusEvent;
import club.abelix.api.base.division.status.listener.DivisionStatusListener;
import club.abelix.api.base.division.status.task.StatusUpdateTask;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.Mini;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>DivisionManager creation on 10.03.2024 at 01:52</p>
 * <p>Очень плохой код, но говнокод - наше все</p>
 */

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DivisionManager {

    // ====================== IMPL ======================
    public static DivisionManager IMP = new DivisionManager();
    // ====================== IMPL ======================

    @Setter
    @Getter
    boolean globalMode = false; // Одна дивизия на весь мастер

    HashMap<Mini, List<Division>> divisionsByMini = new HashMap<>();

    final List<Division> globalDivisions = new ArrayList<>();

    @Getter
    final List<DivisionStar> divisionStars = new ArrayList<>();

    private final static String PROPERTY_DIVISION_FILED_SELECTED = "DIVISION/SELECTED/%s"; // Мастер
    private final static String PROPERTY_DIVISION_FILED_SELECTED_STAR = "DIVISION/SELECTED/%s/STAR"; // Только Мастер

    // ==== Регистрация дивизий и всякой хуйни ====
    public void registerMini(@NonNull Mini mini) {
        if (globalMode) {
            throw new IllegalStateException("Mini global mode is enabled");
        }

        if (divisionsByMini.containsKey(mini)) {
            throw new IllegalStateException("Mini " + mini + " already registered");
        }

        divisionsByMini.put(mini, new ArrayList<>());
    }

    public void addDivision(@NonNull Mini mini, @NonNull Division division) {
        if (globalMode) {
            throw new IllegalStateException("Division global mode is enabled");
        }

        divisionsByMini.get(mini).add(division);
    }

    public void addDivision(@NonNull Division division) {
        if (!globalMode) {
            throw new IllegalStateException("Division global mode is disabled");
        }

        globalDivisions.add(division);
    }

    public void addDivisionStar(@NonNull DivisionStar divisionStar) {
        divisionStars.add(divisionStar);
    }

    public List<Division> getDivisions(@NonNull Mini mini) {
        if (globalMode) {
            throw new IllegalStateException("Division global mode is enabled");
        }

        return divisionsByMini.get(mini);
    }

    public List<Division> getDivisions() {
        if (!globalMode) {
            throw new IllegalStateException("Division global mode is disabled");
        }

        return globalDivisions;
    }

    // ==========
    // ==== Получение дивизий, подсчет и активация ====

    public void activateDivision(@NonNull Gamer gamer, @NonNull Division division,
                                 @Nullable Mini mini, boolean announcement) {
        gamer.setProperty(PROPERTY_DIVISION_FILED_SELECTED.formatted(CoreUtil.getMaster().getName()), division.getIdentifier());

        if (announcement) {
            gamer.sendLocalizedMessage("gamer_new_division", "%new_division%",
                    division.getName(gamer));
        }

        HeartBeat.callEvent(new UpdateGamerStatusEvent(gamer, new Status(getDisplayText(gamer))));
    }

    public void activateDivisionStar(@NonNull Gamer gamer, @NonNull DivisionStar divisionStar,
                                     boolean announcement) {
        gamer.setProperty(
                PROPERTY_DIVISION_FILED_SELECTED_STAR.formatted(CoreUtil.getMaster().getName()),
                divisionStar.getIdentifier());

        if (announcement) {
            gamer.sendLocalizedMessage("gamer_new_division_star",
                    "%star%", divisionStar.getDisplayName(gamer));
        }

        HeartBeat.callEvent(new UpdateGamerStatusEvent(gamer, new Status(getDisplayText(gamer))));
    }


    public Division getBestDivision(@NonNull Gamer gamer, @Nullable Mini mini) {
        if (globalMode) {
            return getBestDivision(gamer);
        }

        List<Division> divisions = divisionsByMini.get(mini);
        if (divisions == null) {
            Logging.IMP.warn("Mini " + mini + " not registered");
            return null;
        }

        StatsMode statsMode = StatsManager.INSTANCE.getMode(mini);

        Division bestDivision = null;
        for (Division division : divisions) {
            if (division.check(gamer, statsMode)) {
                if (bestDivision == null || division.getPriority() > bestDivision.getPriority()) {
                    bestDivision = division;
                }
            }
        }

        return bestDivision;
    }

    public Division getBestDivision(@NonNull Gamer gamer) {
        if (!globalMode) {
            throw new IllegalStateException("Division global mode is disabled");
        }

        StatsMode statsMode = StatsManager.INSTANCE.getMode(CoreUtil.getMaster());

        Division bestDivision = null;
        for (Division division : globalDivisions) {
            if (division.check(gamer, statsMode)) {
                if (bestDivision == null || division.getPriority() > bestDivision.getPriority()) {
                    bestDivision = division;
                }
            }
        }

        return bestDivision;
    }

    public DivisionStar getSelectedStar(@NonNull Gamer gamer) {
        val selectedStarId = gamer.getProperty(
                PROPERTY_DIVISION_FILED_SELECTED_STAR.formatted(CoreUtil.getMaster().getName()));
        return getStarByIdentifier(selectedStarId);
    }

    public Division getByIdentifier(int identifier) {
        if (identifier == 0) {
            return null;
        }

        if (globalMode) {
            for (Division division : globalDivisions) {
                if (division.getIdentifier() == identifier) {
                    return division;
                }
            }

            return null;
        } else {
            for (List<Division> divisions : divisionsByMini.values()) {
                for (Division division : divisions) {
                    if (division.getIdentifier() == identifier) {
                        return division;
                    }
                }
            }

            return null;
        }
    }


    private DivisionStar getStarByIdentifier(int identifier) {
        for (val star : divisionStars) {
            if (star.getIdentifier() == identifier) {
                return star;
            }
        }

        return null;
    }

    // ==== Глобальные штуки ====
    public Division getSelectedDivision(@NonNull Gamer gamer) {
        String selectedFiled = String.format(PROPERTY_DIVISION_FILED_SELECTED, CoreUtil.getMaster().getName());
        int divisionIdentifier = gamer.getProperty(selectedFiled);

        return getByIdentifier(divisionIdentifier);
    }

    // ==== Утилы и тд ====
    public String getDisplayText(@NonNull Gamer gamer) {
        // ЦВЕТ ДИВИЗИЯ ЗВЕЗДА

        Division division = getSelectedDivision(gamer);

        if (division == null) {
            if (isGlobalMode()) {
                division = getBestDivision(gamer);
            } else {
                division = getBestDivision(gamer, null);
            }
        }

        val star = getSelectedStar(gamer);

        if (division == null) {
            return null;
        }

        String result = "";

        if (!isGlobalMode()) {
            result += "§" + division.getColor(gamer).getColorCode() + division.getMini().getDisplayName() + " ";
        }

        result += "§" + division.getColor(gamer).getColorCode() + division.getDisplayText(gamer);

        if (star != null) {
            result += " " + star.getStar();
        }

        return result + "§f";
    }

    public void lobbyInit(AbstractPlugin plugin) {
        new DivisionListener(plugin);
        plugin.getCommandManager().addCommand(DivisionCommand.class);

        if (CoreUtil.isLobby()) {
            new DivisionStatusListener(plugin);
            new StatusUpdateTask().start();

        }
    }
}
