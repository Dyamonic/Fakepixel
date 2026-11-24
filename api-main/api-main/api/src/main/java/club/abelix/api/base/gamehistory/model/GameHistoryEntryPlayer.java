package club.abelix.api.base.gamehistory.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameHistoryEntryPlayer {

    long playerId;
    String playerName;

    // Команда
    boolean hasTeam;
    int teamIndex;
    char teamCharacter;
    String teamColorName;

    // Пати
    boolean inParty;
    String partyLeaderName;
    long partyLeaderId;
    @Builder.Default
    List<Long> partyMemberIds = List.of();

    // Победитель
    boolean winner;

    // Дополнительные метаданные
    @Builder.Default
    Map<String, String> metadata = Map.of();
}
