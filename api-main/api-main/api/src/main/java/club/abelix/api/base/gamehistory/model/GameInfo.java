package club.abelix.api.base.gamehistory.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameInfo {
    String gameId;
    String mode;
    String serverName;
    String mapName;
    long startTime;
    long endTime;
    int totalPlayersCount;
    boolean privateGame;
    @Builder.Default
    List<PlayerInfo> players = List.of();
    @Builder.Default
    List<PlayerInfo> winners = List.of();
}
