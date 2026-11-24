package club.abelix.games.buildffa.game.board;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.scoreboard.Board;
import club.abelix.commons.language.Language;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.network.type.mode.Uncommon;
import lombok.val;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.WeakHashMap;

public class BuildFFABoard extends Board {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final Map<Gamer, BuildFFABoard> BOARDS = new WeakHashMap<>();

    private final Gamer gamer;
    private final Language language;
    private final BuildFFAGame game;

    public BuildFFABoard(BuildFFAGame game, Gamer gamer) {
        super(game.getPlugin(), "BuildFFA");

        this.gamer = gamer;
        this.game = game;
        this.language = gamer.getLanguage();
        BOARDS.put(gamer, this);

        initializeBoard();
        startBoardUpdater();
        addPlayer(gamer.getPlayer());
    }

    private void initializeBoard() {
        BuildFFAGamer buildFFAGamer = BuildFFAGamer.get(gamer);

        setAnimatedDisplayName("BuildFFA");
        setStaticLines(buildFFAGamer);
    }

    private void setStaticLines(BuildFFAGamer buildFFAGamer) {
        setLine(0, language.getMessage("website_url"));
        setLine(1, "");
        setLine(2, language.getMessage("map_scoreboard", "%map%", getCurrentMap()));
        setLine(3, "");
        setLine(4, language.getMessage("money_scoreboard", "%money%", getMoney()));
        setLine(5, language.getMessage("gold_scoreboard", "%gold%", getGold()));
        setLine(6, "");
        setLine(7, language.getMessage("kd_scoreboard", "%kd%", DECIMAL_FORMAT.format(buildFFAGamer.getKD())));
        setLine(8, language.getMessage("deaths_scoreboard", "%deaths%", buildFFAGamer.getDeaths()));
        setLine(9, language.getMessage("kills_scoreboard", "%kills%", buildFFAGamer.getKills()));
        setLine(10, "");
        setLine(11, getRankLine(buildFFAGamer));
    }

    private String getCurrentMap() {
        return game.getWorldConfig().displayName();
    }

    private int getMoney() {
        return gamer.getCoins(Uncommon.BUILDFFA);
    }

    private int getGold() {
        return gamer.getGold();
    }

    private void startBoardUpdater() {
        update(this::updateDynamicLines, 40, true);
    }

    private void updateDynamicLines() {
        val buildFFAGamer = BuildFFAGamer.get(gamer);

        setLine(4, language.getMessage("money_scoreboard", "%money%", getMoney()));
        setLine(5, language.getMessage("gold_scoreboard", "%gold%", getGold()));
        setLine(7, language.getMessage("kd_scoreboard", "%kd%", DECIMAL_FORMAT.format(buildFFAGamer.getKD())));
        setLine(8, language.getMessage("deaths_scoreboard", "%deaths%", buildFFAGamer.getDeaths()));
        setLine(9, language.getMessage("kills_scoreboard", "%kills%", buildFFAGamer.getKills()));
        setLine(2, language.getMessage("map_scoreboard", "%map%", getCurrentMap()));
        setLine(11, getRankLine(buildFFAGamer));
    }

    private String getRankLine(BuildFFAGamer buildFFAGamer) {
        String icon = buildFFAGamer.getPrestigeIcon();
        String rank = buildFFAGamer.getRank().getName(gamer.getLanguage());
        String line = language.getMessage("rank_scoreboard", "%prestige_icon%", icon, "%rank%", rank);
        if (!icon.isEmpty() && !line.contains(icon)) line = line.replace(rank, icon + rank);
        return line;
    }

    public static void refresh(Gamer gamer) {
        BuildFFABoard board = BOARDS.get(gamer);
        if (board != null) board.updateDynamicLines();
    }
}
