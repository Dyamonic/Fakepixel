package club.abelix.games.buildffa.game.config;

import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.commons.data.skin.Skin;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public record FakePlayerConfig(
        Location location,
        Skin skin
) {
    public static FakePlayerConfig create(final @NotNull ConfigurationSection section) {
        val location = LocationUtil.stringToLocation(section.getString("Location"));
        val skin = new Skin(
                "agera001",
                "",
                section.getString("Value"),
                section.getString("Signature")
        );

        return new FakePlayerConfig(location, skin);
    }
}
