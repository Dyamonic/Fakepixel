package club.abelix.api.base.util.scoreboard;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.scoreboard.scroller.DefaultScroller;
import club.abelix.api.base.util.scoreboard.scroller.Scroller;
import club.abelix.api.base.util.scoreboard.task.BoardTask;
import club.abelix.api.base.util.scoreboard.task.BoardUpdater;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.scoreboard.objective.BoardObjective;
import club.abelix.api.nms.scoreboard.objective.ObjectiveActionMode;
import club.abelix.api.nms.scoreboard.objective.ObjectiveDisplayType;
import club.abelix.api.nms.scoreboard.objective.ObjectiveType;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.collection.CollectionUtil;
import club.abelix.network.type.mode.ServerMode;
import com.comphenix.protocol.events.PacketContainer;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Getter
public class Board {

    private static final Map<UUID, Board> PLAYER_BOARDS = new ConcurrentHashMap<>();
    private final Plugin plugin;
    @Getter
    private final Set<Player> receivers = CollectionUtil.createConcurrentSet();
    @Getter
    private final BoardObjective objective;
    private final BoardLine[] lines = new BoardLine[16];


    public Board(Plugin plugin) {
        this(plugin, "AGERA");
    }

    public Board(Plugin plugin, String displayName) {
        this.plugin = plugin;
        this.objective = new BoardObjective("ScoreBoard", displayName, ObjectiveType.INTEGER);
    }

    public static Board getPlayerBoard(@NonNull Player player) {
        return PLAYER_BOARDS.get(player.getUniqueId());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Board> void forEach(@NonNull Class<T> cls, @NonNull Consumer<T> boardConsumer) {
        for (Board board : PLAYER_BOARDS.values()) {
            if (board.getClass() == cls) {
                boardConsumer.accept((T) board);
            }
        }
    }

    public boolean haveBoardPlayers() {
        for (Player receiver : receivers) {
            if (receiver.isOnline()) {
                return true;
            }
        }

        return false;
    }

    public void remove() {
        removePlayers();
    }

    public String getDisplayName() {
        return objective.getDisplayName();
    }

    public void setDisplayName(String displayName) {
        objective.setDisplayName(displayName);

        PacketLibraryApi packetApi = FalconAPI.getNmsApi().getPacketLibraryApi();

        PacketContainer createObjectivePacket
                = packetApi.getScoreboardObjectivePacket(objective, ObjectiveActionMode.UPDATE);

        receivers.forEach(player -> packetApi.sendPacket(player, createObjectivePacket));
    }

    public void setBoardDateTime(int line, @NonNull Language language, @NonNull String text) {
        setLine(line, language.getMessage("board_datetime",
                "%time%", new SimpleDateFormat("§7dd/MM/yy")
                        .format(Calendar.getInstance().getTime()) + " §8" + text));
    }

    public void setBoardDateTime(int line, @NonNull Language language) {
        setBoardDateTime(line, language, CoreUtil.getServerName());
    }

    public void setBoardDateTime(int line, @NonNull Language language, @NonNull ServerMode serverMode) {
        setBoardDateTime(line, language, CoreUtil.generateDisplayName(serverMode));
    }

    public void setBoardServer(int line, @NonNull Language language, @NonNull String server) {
        setLine(line, language.getMessage("board_server", "%server%", server));
    }

    public void setBoardMode(int line, @NonNull Language language, @NonNull String mode) {
        setLine(line, language.getMessage("board_mode", "%mode%", mode));
    }

    public void setBoardMap(int line, @NonNull Language language, @NonNull String map) {
        setLine(line, language.getMessage("board_game_map", "%map%", map));
    }

    public void setBoardWebsite(int line, @NonNull Language language) {
        setLine(line, language.getMessage("board_website"));
    }

    public void setDisplayName(@NonNull Language language, @NonNull String text) {
        setDisplayName(language.getMessage("board_display_name", "%name%", text));
    }

    public void setAnimatedDisplayName(String displayName) {
        setAnimatedDisplayName(
                new DefaultScroller(displayName), 3
        );
    }

    public void setAnimatedDisplayName(Scroller scroller, int updateTick) {
        update(() -> setDisplayName(scroller.getNext(true)), updateTick);
    }

    public void addPlayer(Player player) {
        addPlayers(Collections.singleton(player));
    }

    public void addPlayer(Gamer gamer) {
        addPlayers(Collections.singleton(gamer.getPlayer()));
    }

    public void addPlayers(Collection<Player> toAdd) {
        Set<Player> send = new HashSet<>();

        for (Player player : toAdd) {
            if (receivers.add(player)) {
                Board board = getPlayerBoard(player);

                if (board != null) {
                    board.removePlayer(player);
                }

                send.add(player);

                PLAYER_BOARDS.put(player.getUniqueId(), this);
            }
        }

        PacketLibraryApi packetApi = FalconAPI.getNmsApi().getPacketLibraryApi();

        PacketContainer createObjectivePacket
                = packetApi.getScoreboardObjectivePacket(objective, ObjectiveActionMode.CREATE);
        PacketContainer createDisplayObjectivePacket
                = packetApi.getScoreboardDisplayObjectivePacket(objective, ObjectiveDisplayType.SIDEBAR);

        send.forEach(player -> {
            packetApi.sendPacket(player, createObjectivePacket);
            packetApi.sendPacket(player, createDisplayObjectivePacket);
        });

        for (BoardLine line : lines) {
            if (line == null) continue;
            line.show(send);
        }
    }

    public void removePlayer(Player player) {
        removePlayers(Collections.singleton(player));
    }

    public void removePlayers(Collection<Player> remove) {
        for (BoardLine line : lines) {
            if (line == null) continue;
            line.hide(remove);
        }

        receivers.removeAll(remove);

        for (Player player : remove) {
            PLAYER_BOARDS.remove(player.getUniqueId());
        }

        PacketLibraryApi packetApi = FalconAPI.getNmsApi().getPacketLibraryApi();

        PacketContainer removeObjectivePacket
                = packetApi.getScoreboardObjectivePacket(objective, ObjectiveActionMode.REMOVE);

        remove.forEach(player -> packetApi.sendPacket(player, removeObjectivePacket));
    }

    public void removePlayers() {
        removePlayers(receivers);
    }

    public void setLine(int num) {
        setLine(num, "§" + Character.forDigit(num, 16));
    }

    public void setLine(int num, @NonNull String text) {
        BoardLine line = lines[num];

        if (line == null) {
            line = new BoardLine(num, text, this);
            line.show(receivers);

            lines[num] = line;
        } else {
            line.setText(text);
        }
    }

    public void removeLine(int num) {
        BoardLine line = lines[num];

        if (line == null) {
            return;
        }

        lines[num] = null;
        line.hide(receivers);
    }

    protected void update(Runnable runnable, long ticks) {
        update(runnable, (int) ticks);
    }

    protected void update(Runnable runnable, int ticks) {
        update(runnable, ticks, false);
    }

    protected void update(Runnable runnable, int ticks, boolean now) {
        BoardUpdater updater = new BoardUpdater(runnable, ticks, this);

        if (now) {
            updater.call();
        }

        BoardTask.getInstance().addUpdater(updater);
    }
}