package net.fakepixel.housing.database;

import net.fakepixel.housing.house.House;
import org.bukkit.Location;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Map;

public interface HousingRepository {
    House createHouse(UUID ownerUuid) throws Exception;
    House loadHouse(String houseId) throws Exception;
    List<House> loadHouseByOwner(UUID ownerUuid) throws Exception;
    List<House> loadAllHouses() throws Exception;
    List<House> loadAllPublicHouses() throws Exception;
    void saveHouse(House house) throws Exception;
    void saveHouses(Collection<House> houses) throws Exception;
    void deleteHouse(String houseId) throws Exception;
    void saveGuestSpawn(String houseId, Location location) throws Exception;
    void saveTheme(String houseId, String themeId) throws Exception;
    void savePlotSize(String houseId, int size) throws Exception;
    void saveGroup(String houseId, String groupId, String displayName, String tag, String permissionsJson) throws Exception;
    void saveNpc(String npcId, String houseId, String dataJson) throws Exception;
    List<String> loadNpcData() throws Exception;
    void saveHologram(String hologramId, String houseId, String dataJson) throws Exception;
    void saveTeleportPad(String padId, String houseId, String dataJson) throws Exception;
    void saveParkourData(String houseId, String dataJson) throws Exception;
    void saveParkourTime(String houseId, UUID playerUuid, long timeMillis) throws Exception;
    List<String> loadBrowseData() throws Exception;
    void saveBrowseData(UUID playerUuid, String data) throws Exception;
    boolean hasUnlock(UUID playerUuid, String type, String id) throws Exception;
    void saveUnlock(UUID playerUuid, String type, String id) throws Exception;
    Map<UUID,UUID> loadInvites() throws Exception;
    void saveInvite(UUID playerUuid, UUID ownerUuid, String houseId, long expiresAt) throws Exception;
}
