package club.abelix.commons.data.gamehistory;

import club.abelix.network.type.mode.Mini;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>GameHistoryEntry creation on 03.09.2023 at 20:39</p>
 */

@Data
public class GameHistoryEntry {
    private final Mini mini;
    private final String mapName;
    private int id;
    private Timestamp startTime;
    private Timestamp endTIme;
    @Getter
    private List<GameHistoryPlayerEntry> players = new ArrayList<>();

    public GameHistoryEntry(@NonNull Mini mini, @NonNull String mapName) {
        this.startTime = new Timestamp(System.currentTimeMillis());
        this.mini = mini;
        this.mapName = mapName;
    }

    public GameHistoryEntry(int id,
                            Timestamp startTime,
                            Timestamp endTIme,
                            Mini mini,
                            String mapName,
                            List<GameHistoryPlayerEntry> players) {
        this.id = id;
        this.startTime = startTime;
        this.endTIme = endTIme;
        this.mini = mini;
        this.mapName = mapName;
        this.players = players;
    }

    public void addPlayer(@NonNull GameHistoryPlayerEntry entry) {
        players.add(entry);
    }
}
