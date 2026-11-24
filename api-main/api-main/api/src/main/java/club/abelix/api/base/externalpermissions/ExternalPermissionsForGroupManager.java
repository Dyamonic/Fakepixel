package club.abelix.api.base.externalpermissions;

import club.abelix.commons.data.permission.Group;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.ServerMode;
import lombok.NonNull;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>ExternalPermissionsForGroupManager creation on 14.01.2023 at 21:03</p>
 * <p>
 * Выдача групп в зависимости от группы игрока
 * Создано для интеграции со внешними плагинами
 */

public class ExternalPermissionsForGroupManager {
    // =====
    public static final ExternalPermissionsForGroupManager INSTANCE = new ExternalPermissionsForGroupManager();
    // =====

    private final HashMap<Group, ExternalPermissionsGroup> permissionsGroups = new HashMap<>();

    public void load(YamlConfiguration configuration) {
        if (configuration == null) {
            return;
        }

        configuration.getKeys(false).forEach(key -> {
            Group group = Group.fromLegacyGroup(key);
            permissionsGroups.put(group,
                    ExternalPermissionsGroup.getFromConfigSection(configuration.getConfigurationSection(key)));
            Logging.IMP.info("[EXTERNAL PERM] Loaded permissions settings for group {}", group.getName());
        });
    }


    public ExternalPermissionResult getExternalPermissionForGroup(@NonNull ServerMode serverMode, @NonNull Group gamerGroup) {
        ArrayList<String> appendPermissions = new ArrayList<>();
        ArrayList<String> removePermissions = new ArrayList<>();

        for (Group group : permissionsGroups.keySet()) {
            if (group.getLevel() <= gamerGroup.getLevel()) {
                ExternalPermissionsGroup permissionsGroup = permissionsGroups.get(group);

                if (serverMode.isLobby()) {
                    appendPermissions.addAll(permissionsGroup.lobby().appendList());
                    removePermissions.addAll(permissionsGroup.lobby().removeList());
                } else if (serverMode.isMini()) {
                    appendPermissions.addAll(permissionsGroup.mini().appendList());
                    removePermissions.addAll(permissionsGroup.mini().removeList());
                } else if (serverMode.isUncommon()) {
                    appendPermissions.addAll(permissionsGroup.uncommon().appendList());
                    removePermissions.addAll(permissionsGroup.uncommon().removeList());
                } else if (serverMode.isSpecial()) {
                    appendPermissions.addAll(permissionsGroup.special().appendList());
                    removePermissions.addAll(permissionsGroup.special().removeList());
                }
            }
        }
        return new ExternalPermissionResult(appendPermissions, removePermissions);
    }
}
