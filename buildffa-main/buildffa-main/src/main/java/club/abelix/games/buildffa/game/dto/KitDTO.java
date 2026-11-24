package club.abelix.games.buildffa.game.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitDTO {
    private int playerId, kitId;
    private Map<Integer, Integer> itemSlots;
    private int itemHash;
    private int slot;
}