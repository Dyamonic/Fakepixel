package club.abelix.api.nms.team;

import lombok.NonNull;
import org.bukkit.entity.Player;

public interface DefaultFakeTeamProvider {

    /**
     * Метод, отправляющий тиму по умолчанию игроку при входе
     */
    FakeTeam provideFakeTeam(@NonNull Player player);

}
