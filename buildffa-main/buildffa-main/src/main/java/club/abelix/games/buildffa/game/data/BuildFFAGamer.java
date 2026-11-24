package club.abelix.games.buildffa.game.data;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamersession.GamerSession;
import club.abelix.api.base.gamersession.GamerSessionManager;
import club.abelix.games.buildffa.game.division.AbstractDivision;
import club.abelix.games.buildffa.game.manager.progression.PrestigeManager;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.entity.Player;

@Setter
@Getter
public class BuildFFAGamer extends GamerSession {
    public static final String BEST_KILL_STREAK_PROPERTY_NAME = "buildffa_best_kill_streak";
    public static final String SELECTED_KIT_PROPERTY_NAME = "buildffa_selected_kit";
    public static final String PRESTIGE_PROPERTY_NAME = "buildffa_prestige";


    private int currentKillStreak;

    public BuildFFAGamer(Gamer gamer) {
        super(gamer);
    }

    public static BuildFFAGamer get(@NonNull Gamer gamer) {
        return (BuildFFAGamer) GamerSessionManager.INSTANCE.getProvider(BuildFFAGamer.class).getSession(gamer);
    }

    public static BuildFFAGamer get(@NonNull Player player) {
        return (BuildFFAGamer) GamerSessionManager.INSTANCE.getProvider(BuildFFAGamer.class)
                .getSession(Gamer.getGamer(player));
    }

    @Override
    public void load() {
        // Load additional data if needed
    }

    public AbstractDivision getRank() {
        return AbstractDivision.getRankForRating(getGamer().getLifetimeStat("rating"));
    }

    public int getPrestige() {
        return getGamer().getProperty(PRESTIGE_PROPERTY_NAME);
    }

    public String getPrestigeIcon() {
        return getPrestigeIcon(getPrestige());
    }

    public static String getPrestigeIcon(int prestige) {
        if (prestige > 10) return getPrestigeIcon((Math.min(prestige, PrestigeManager.MAX_PRESTIGE) - 1) % 10 + 1);
        switch (prestige) {
            case 1: return "§a✦ ";
            case 2: return "§2✦ ";
            case 3: return "§3✦ ";
            case 4: return "§4✦ ";
            case 5: return "§5✦ ";
            case 6: return "§6◆ ";
            case 7: return "§c◆ ";
            case 8: return "§d◆ ";
            case 9: return "§e✹ ";
            case 10: return "§9✹ ";
            default: return "";
        }
    }

    public int getKills() {
        return getGamer().getLifetimeStat("kills");
    }

    public int getDeaths() {
        return getGamer().getLifetimeStat("deaths");
    }

    public int getBestKillStreak() {
        return getGamer().getProperty(BEST_KILL_STREAK_PROPERTY_NAME);
    }

    public int incrementKillStreak() {
        setCurrentKillStreak(currentKillStreak + 1);

        if (currentKillStreak > getBestKillStreak()) {
            getGamer().setProperty(BEST_KILL_STREAK_PROPERTY_NAME, currentKillStreak);
        }

        return currentKillStreak;
    }

    public void resetKillStreak() {
        currentKillStreak = 0;
    }

    public double getKD() {
        int kills = getKills();
        int deaths = getDeaths();
        return deaths == 0 ? kills : (double) kills / deaths;
    }
}
