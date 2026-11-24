package club.abelix.api.server.lobby.impl.hub.present.storage;

import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.server.lobby.impl.hub.present.Present;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.configuration.Configuration;

@Log4j2
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class YamlPresentStorage extends AbstractPresentStorage {

    Configuration configuration;

    @Override
    public void load() {
        log.info("Starting to load presents from config...");
        int id = 1;

        val presentLocations = configuration.getStringList("Presents");
        log.info("Found {} present locations in config", presentLocations.size());

        for (val presentLocation : presentLocations) {
            val location = LocationUtil.stringToLocation(presentLocation);

            val present = new Present(id++, location);
            addPresent(location, present);
        }

        log.info("Total loaded presents: {}", count());
    }
}
