package me.outerspace.thepit.managers;

import me.outerspace.thepit.registry.definition.StoredLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArenaFeatureConfiguration {
    private ArenaFeatureConfiguration() {}

    public record LaunchPadDefaults(double horizontalPower, double verticalPower, long cooldownMillis,
                                    int fallProtectionSeconds, String particle, String sound) {}

    public record LaunchPad(String id, String worldName, int x, int y, int z, double horizontalPower,
                            double verticalPower, long cooldownMillis, int fallProtectionSeconds,
                            String particle, String sound) {}

    public record LaunchPads(boolean enabled, LaunchPadDefaults defaults, Map<String, LaunchPad> locations) {
        public LaunchPads {
            locations = Collections.unmodifiableMap(new LinkedHashMap<>(locations));
        }
    }

    public record CoinDefaults(int reward, int respawnSeconds, String displayName,
                               String pickupMessage, int maxActive) {}

    public record CoinSpawn(String id, StoredLocation location, int reward, int respawnSeconds) {}

    public record CoinSpawns(boolean enabled, CoinDefaults defaults, Map<String, CoinSpawn> locations) {
        public CoinSpawns {
            locations = Collections.unmodifiableMap(new LinkedHashMap<>(locations));
        }
    }

    public record CarePackageLocation(String id, StoredLocation location, double dropHeight) {}

    public record CarePackages(boolean enabled, Map<String, CarePackageLocation> locations) {
        public CarePackages {
            locations = Collections.unmodifiableMap(new LinkedHashMap<>(locations));
        }
    }
}
