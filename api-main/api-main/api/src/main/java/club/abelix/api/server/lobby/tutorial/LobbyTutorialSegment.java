package club.abelix.api.server.lobby.tutorial;

import club.abelix.api.base.util.location.LocationUtil;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

@Data
public class LobbyTutorialSegment {

    private String message;
    private List<Location> followingLocations;
    private LobbyTutorialTitle title;
    private int duration;

    public void loadSegment(ConfigurationSection section) {
        message = section.getString("Message");
        followingLocations = new ArrayList<>(LocationUtil.stringToLocation(section.getStringList("Locations")));

        ConfigurationSection titleSection = section.getConfigurationSection("Title");

        if (titleSection != null) {
            title = new LobbyTutorialTitle(titleSection);
        }

        duration = section.getInt("Duration");
    }

}
