package club.abelix.api.base.gamehistory.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PartyInfo {
    String partyId;
    PlayerInfo leader;
    @Builder.Default
    List<PlayerInfo> members = List.of();
}
