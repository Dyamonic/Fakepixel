package club.abelix.games.fastbridge.commons.config;

import club.abelix.commons.data.skin.Skin;
import club.abelix.games.fastbridge.commons.object.RelativeLocation;
import lombok.val;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * @author Migel077
 */
public record FakePlayerConfig(
        RelativeLocation relLocation,
        Skin skin
) {
    public static FakePlayerConfig create(final @NotNull ConfigurationSection section) {
        val location = RelativeLocation.fromString(section.getString("Location"));

        val skin = new Skin(
                "ax_fastbridge",
                "",
                section.getString("Value"),
                section.getString("Signature")
        );

        return new FakePlayerConfig(location, skin);
    }
}
