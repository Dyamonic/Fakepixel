package club.abelix.commons.data.externalpermissions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>ExternalPermissionEntry creation on 14.01.2023 at 19:24</p>
 */

@AllArgsConstructor
@Getter
public class ExternalPermissionEntry {
    @Setter
    private int permissionId;
    private int gamerId;
    private String permission;
    // Настройка по серверам
    private boolean canLobby;
    private boolean canMini;
    private boolean canUncommon;
    private boolean canSpecial;
}
