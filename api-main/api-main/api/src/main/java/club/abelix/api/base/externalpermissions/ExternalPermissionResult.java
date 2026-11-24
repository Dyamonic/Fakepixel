package club.abelix.api.base.externalpermissions;

import lombok.NonNull;

import java.util.ArrayList;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>ExternalPermissionResult creation on 15.01.2023 at 00:50</p>
 */

public record ExternalPermissionResult(@NonNull ArrayList<String> appendPermissions,
                                       @NonNull ArrayList<String> removePermissions) {
}