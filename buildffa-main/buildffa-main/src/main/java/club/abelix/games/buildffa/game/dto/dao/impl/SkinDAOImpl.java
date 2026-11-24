package club.abelix.games.buildffa.game.dto.dao.impl;

import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.games.buildffa.game.dto.SkinDTO;
import club.abelix.games.buildffa.game.dto.dao.SkinDAO;
import lombok.Getter;
import lombok.val;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SkinDAOImpl implements SkinDAO {
    private static final String LOAD_PLAYER_SKINS = "SELECT `skin_id` FROM `player_skins` WHERE `player_id` = ?";
    private static final String ADD_PLAYER_SKIN = "INSERT INTO `player_skins` (`player_id`, `skin_id`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `skin_id` = `skin_id`";
    private static final String IS_SKIN_PURCHASED = "SELECT 1 FROM `player_skins` WHERE `player_id` = ? AND `skin_id` = ?";
    private static final String GET_SELECTED_SKIN = "SELECT `selected_skin`, `material`, `material_data` FROM `player_selected_skin` WHERE `player_id` = ?";
    private static final String SET_SELECTED_SKIN = "INSERT INTO `player_selected_skin` (`player_id`, `selected_skin`, `material`, `material_data`) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `selected_skin` = VALUES(`selected_skin`), `material` = VALUES(`material`), `material_data` = VALUES(`material_data`)";
    private static final String DELETE_PLAYER_SKINS = "DELETE FROM `player_skins` WHERE `player_id` = ?";
    private static final String DELETE_SELECTED_SKIN = "DELETE FROM `player_selected_skin` WHERE `player_id` = ?";
    private static final String DROP_TABLE_PLAYER_SKINS = "DROP TABLE IF EXISTS `player_skins`";
    private static final String DROP_TABLE_SELECTED_SKIN = "DROP TABLE IF EXISTS `player_selected_skin`";
    private static final String CREATE_TABLE_PLAYER_SKINS = "CREATE TABLE IF NOT EXISTS `player_skins` (" +
            "`player_id` int(11) NOT NULL, " +
            "`skin_id` varchar(255) NOT NULL, " +
            "PRIMARY KEY (`player_id`, `skin_id`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
    private static final String CREATE_TABLE_SELECTED_SKIN = "CREATE TABLE IF NOT EXISTS `player_selected_skin` (" +
            "`player_id` int(11) NOT NULL, " +
            "`selected_skin` varchar(255) NOT NULL, " +
            "`material` varchar(255) NOT NULL, " +
            "`material_data` tinyint NOT NULL, " +
            "PRIMARY KEY (`player_id`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
    @Getter
    private static final SkinDAOImpl instance;

    static {
        instance = new SkinDAOImpl();
    }

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("game_buildffa"));

    @Override
    public void recreateTables() {
        
    }

    @Override
    public CompletableFuture<Boolean> isSkinPurchased(int playerId, String skinId) { 
        return database.executeQuery(IS_SKIN_PURCHASED, playerId, skinId)
                .thenApply(rows -> !rows.isEmpty());
    }

    @Override
    public void addPlayerSkin(int playerId, String skinId) {
        database.executeVoidUpdate(ADD_PLAYER_SKIN, playerId, skinId);
    }

    @Override
    public CompletableFuture<SkinDTO> getSelectedSkin(int playerId) {
        return database.executeQuery(GET_SELECTED_SKIN, playerId)
                .thenApply(rows -> {
                    if (rows.isEmpty()) return null;

                    val row = rows.get(0);

                    return SkinDTO.builder()
                            .playerId(playerId)
                            .skinId(row.getString("selected_skin").orElse(null))
                            .material(Material.valueOf(row.getString("material").orElse("AIR")))
                            .materialData((byte) row.getInt("material_data").orElse(0))
                            .build();
                });
    }

    @Override
    public CompletableFuture<Void> setSelectedSkin(SkinDTO skinDTO) {
        String cleanSkinId = skinDTO.getSkinId().toUpperCase();
        String materialName = skinDTO.getMaterial().name().toUpperCase();
        return database.executeVoidUpdate(SET_SELECTED_SKIN, skinDTO.getPlayerId(), cleanSkinId, materialName, skinDTO.getMaterialData())
                .thenApply(result -> null);
    }

    @Override
    public CompletableFuture<List<String>> getPlayerSkins(int playerId) {
        return database.executeQuery(LOAD_PLAYER_SKINS, playerId)
                .thenApply(rows -> {
                    List<String> skins = new ArrayList<>();
                    for (val row : rows) {
                        skins.add(row.getRequiredString("skin_id"));
                    }
                    return skins;
                });
    }

    @Override
    public CompletableFuture<Void> wipePlayer(int playerId) {
        return database.executeVoidUpdate(DELETE_SELECTED_SKIN, playerId)
                .thenCompose(ignored -> database.executeVoidUpdate(DELETE_PLAYER_SKINS, playerId))
                .thenApply(ignored -> null);
    }
}
