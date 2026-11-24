package club.abelix.commons.data.gamehistory;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>GameHistoryPlayerEntry creation on 03.09.2023 at 20:50</p>
 */

@Data
@AllArgsConstructor
public class GameHistoryPlayerEntry {
    private final int playerId;
    private boolean winner;
}
