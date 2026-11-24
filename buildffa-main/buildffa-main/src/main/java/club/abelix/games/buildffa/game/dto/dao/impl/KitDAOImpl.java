package club.abelix.games.buildffa.game.dto.dao.impl;

import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.games.buildffa.game.dto.KitDTO;
import club.abelix.games.buildffa.game.dto.dao.KitDAO;
import club.abelix.games.buildffa.game.kit.BaseKit;
import club.abelix.games.buildffa.game.kit.registry.KitRegistry;
import lombok.val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class KitDAOImpl implements KitDAO {
    public static final KitDAOImpl INSTANCE = new KitDAOImpl();
    private static final String LOAD_PLAYER_KITS = "SELECT `kit_id` FROM `player_kits` WHERE `player_id` = ?";
    private static final String ADD_PLAYER_KIT = "INSERT INTO `player_kits` (`player_id`, `kit_id`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `kit_id` = `kit_id`";
    private static final String DELETE_PLAYER_KIT = "DELETE FROM `player_kits` WHERE `player_id` = ? AND `kit_id` = ?";
    private static final String IS_KIT_PURCHASED = "SELECT 1 FROM `player_kits` WHERE `player_id` = ? AND `kit_id` = ?";
    private static final String LOAD_ITEM_SLOTS = "SELECT `item_hash`, `slot` FROM `kit_item_slots` WHERE `player_id` = ? AND `kit_id` = ?";
    private static final String SAVE_ITEM_SLOT = "INSERT INTO `kit_item_slots` (`player_id`, `kit_id`, `item_hash`, `slot`) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `slot` = VALUES(`slot`)";
    private static final String CREATE_TABLE_KIT_ITEM_SLOTS = "CREATE TABLE IF NOT EXISTS `kit_item_slots` (" +
            "`player_id` int(11) NOT NULL, " +
            "`kit_id` varchar(255) NOT NULL, " +
            "`item_hash` int(11) NOT NULL, " +
            "`slot` int(11) NOT NULL, " +
            "PRIMARY KEY (`player_id`,`kit_id`, `item_hash`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
    private static final String DELETE_ITEM_SLOTS = "DELETE FROM `kit_item_slots` WHERE `player_id` = ? AND `kit_id` = ?";
    private static final String DELETE_PLAYER_KITS = "DELETE FROM `player_kits` WHERE `player_id` = ?";
    private static final String DELETE_PLAYER_SLOTS = "DELETE FROM `kit_item_slots` WHERE `player_id` = ?";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("game_buildffa"));

    public KitDAOImpl() {
        createKitItemSlotsTable();
    }

    private void createKitItemSlotsTable() {
        database.executeVoidUpdate(CREATE_TABLE_KIT_ITEM_SLOTS);
    }

    @Override
    public CompletableFuture<List<BaseKit>> getPlayerKits(int playerId) {
        return database.executeQuery(LOAD_PLAYER_KITS, playerId)
                .thenApply(rows -> {
                    val kits = new ArrayList<BaseKit>();

                    for (val row : rows) {
                        val kitId = row.getRequiredInt("kit_id");
                        val kit = KitRegistry.REGISTRY.getKitById(kitId);

                        if (kit != null) kits.add(kit);
                    }

                    return kits;
                });
    }

    @Override
    public CompletableFuture<Boolean> isKitPurchased(int playerId, String kitId) {
        return database.executeQuery(IS_KIT_PURCHASED, playerId, kitId)
                .thenApply(rows -> !rows.isEmpty());
    }

    @Override
    public void addPlayerKit(int playerId, String kitId) {
        database.executeVoidUpdate(ADD_PLAYER_KIT, playerId, kitId);
    }

    @Override
    public void deletePlayerKit(int playerId, String kitId) {
        database.executeVoidUpdate(DELETE_PLAYER_KIT, playerId, kitId);
    }

    @Override
    public void resetPlayerKit(int playerId, String kitId) {
        database.executeVoidUpdate(DELETE_ITEM_SLOTS, playerId, kitId);
    }

    @Override
    public void saveItemSlot(KitDTO kitDTO) {
        saveItemSlotAsync(kitDTO);
    }

    public CompletableFuture<Void> saveItemSlotAsync(KitDTO kitDTO) {
        return database.executeVoidUpdate(SAVE_ITEM_SLOT,
                kitDTO.getPlayerId(),
                kitDTO.getKitId(),
                kitDTO.getItemHash(),
                kitDTO.getSlot()).thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Map<Integer, Integer>> loadItemSlots(int playerId, String kitId) {
        return database.executeQuery(LOAD_ITEM_SLOTS, playerId, kitId)
                .thenApply(rows -> {
                    Map<Integer, Integer> slots = new HashMap<>();
                    for (val row : rows) {
                        slots.put(row.getRequiredInt("item_hash"), row.getRequiredInt("slot"));
                    }
                    return slots;
                });
    }

    @Override
    public CompletableFuture<Void> wipePlayer(int playerId) {
        return database.executeVoidUpdate(DELETE_PLAYER_SLOTS, playerId)
                .thenCompose(ignored -> database.executeVoidUpdate(DELETE_PLAYER_KITS, playerId))
                .thenApply(ignored -> null);
    }
}
