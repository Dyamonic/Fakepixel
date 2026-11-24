package club.abelix.games.buildffa.game.dto;

import lombok.*;
import org.bukkit.Material;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkinDTO {
    private int playerId;
    private String skinId;
    private Material material;
    private byte materialData;
}