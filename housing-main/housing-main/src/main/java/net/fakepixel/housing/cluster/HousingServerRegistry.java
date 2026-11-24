package net.fakepixel.housing.cluster;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HousingServerRegistry {
    private static final Map<String, HousingServerDefinition> SERVERS = new LinkedHashMap<>();
    static {
        register(new HousingServerDefinition("housing", HousingServerRole.LOBBY));
        register(new HousingServerDefinition("hs-1", HousingServerRole.INSTANCE));
        register(new HousingServerDefinition("hs-2", HousingServerRole.INSTANCE));
        register(new HousingServerDefinition("hs-3", HousingServerRole.INSTANCE));
    }
    private HousingServerRegistry() {}
    private static void register(HousingServerDefinition server) {
        if (SERVERS.put(server.getId(), server) != null) throw new IllegalStateException("Duplicate Housing server id " + server.getId());
    }
    public static HousingServerDefinition get(String id) { return id == null ? null : SERVERS.get(id.trim()); }
    public static HousingServerDefinition current() {
        String id = System.getenv("HOUSING_SERVER_ID");
        if (id == null || id.trim().isEmpty()) id = System.getProperty("HOUSING_SERVER_ID");
        if (id == null || id.trim().isEmpty()) throw new IllegalStateException("Set HOUSING_SERVER_ID or -DHOUSING_SERVER_ID to a registered Housing server id");
        HousingServerDefinition server = get(id);
        if (server == null) throw new IllegalStateException("Unknown HOUSING_SERVER_ID '" + id + "'. Registered ids: " + SERVERS.keySet());
        return server;
    }
    public static HousingServerDefinition lobby() { return SERVERS.get("housing"); }
    public static Collection<HousingServerDefinition> all() { return Collections.unmodifiableCollection(SERVERS.values()); }
}
