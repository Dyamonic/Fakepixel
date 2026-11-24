package club.abelix.api.base.externalpermissions;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>PermissionsSettingsUnit creation on 14.01.2023 at 21:13</p>
 */

public record PermissionsSettingsUnit(List<String> appendList, List<String> removeList) {

    public static PermissionsSettingsUnit getFromConfigurationSection(ConfigurationSection section) {
        if (section == null) {
            return new PermissionsSettingsUnit(Collections.emptyList(), Collections.emptyList());
        }

        List<String> append = section.getStringList("append");
        List<String> remove = section.getStringList("remove");
        return new PermissionsSettingsUnit(append, remove);
    }


}
