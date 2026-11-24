package club.abelix.api.server.lobby.tutorial;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;

@Data
@AllArgsConstructor
public class LobbyTutorialTitle {

    private String title;
    private String subtitle;

    private int fadeIn;
    private int stay;
    private int fadeOut;

    public LobbyTutorialTitle(ConfigurationSection section) {
        this(
                section.getString("title"),
                section.getString("subtitle"),
                section.getInt("fadeIn", 10),
                section.getInt("stay", 60),
                section.getInt("fadeOut", 1)
        );
    }

    public int getDuration() {
        return fadeIn + stay + fadeOut;
    }

}
