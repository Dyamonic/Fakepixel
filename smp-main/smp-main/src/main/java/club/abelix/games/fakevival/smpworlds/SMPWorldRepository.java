package club.abelix.games.fakevival.smpworlds;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SMPWorldRepository {
    void initialize() throws SQLException;
    boolean isAvailable();
    int countWorlds() throws SQLException;
    List<SMPWorldMetadata> listWorlds() throws SQLException;
    Optional<SMPWorldMetadata> findMetadata(String worldId) throws SQLException;
    Optional<SMPWorldData> findData(String worldId) throws SQLException;
    default boolean exists(String worldId) throws SQLException { return findMetadata(worldId).isPresent(); }
    void insertWorld(SMPWorldMetadata metadata, SMPWorldData data) throws SQLException;
    void saveWorld(SMPWorldMetadata metadata, SMPWorldData data) throws SQLException;
    void saveMetadata(SMPWorldMetadata metadata) throws SQLException;
    void saveData(SMPWorldData data) throws SQLException;
    void updateState(String worldId, SMPWorldState state) throws SQLException;
    void updateSaveMetadata(String worldId, String savedByUuid, String savedByName, String linkedDungeonId) throws SQLException;
    void cloneWorld(String templateId, String newWorldId) throws SQLException;
    void deleteWorld(String worldId) throws SQLException;
    void insertInstance(String instanceId, String templateId, String worldId, String ownerUuid,
                        String partyId, String serverId, Long expiresAt) throws SQLException;
}
