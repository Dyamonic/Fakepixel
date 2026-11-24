package club.abelix.api.base.externalpermissions;

import lombok.NonNull;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>ExternalPermissionsGroup creation on 14.01.2023 at 21:04</p>
 */


public record ExternalPermissionsGroup(PermissionsSettingsUnit lobby, PermissionsSettingsUnit mini,
                                       PermissionsSettingsUnit uncommon, PermissionsSettingsUnit special) {
    public static ExternalPermissionsGroup getFromConfigSection(@NonNull ConfigurationSection section) {
        return new ExternalPermissionsGroup(
                PermissionsSettingsUnit.getFromConfigurationSection(section.getConfigurationSection("LOBBY")),
                PermissionsSettingsUnit.getFromConfigurationSection(section.getConfigurationSection("MINI")),
                PermissionsSettingsUnit.getFromConfigurationSection(section.getConfigurationSection("UNCOMMON")),
                PermissionsSettingsUnit.getFromConfigurationSection(section.getConfigurationSection("SPECIAL"))
        );

    }
}