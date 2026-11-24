package club.abelix.api.base.gamehistory.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerInfo {
    long playerId;
    String playerName;
    PartyInfo party;
    @Builder.Default
    Map<String, String> metadata = Map.of();

    public Integer getKills() {
        String value = metadata.get("kills");
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
