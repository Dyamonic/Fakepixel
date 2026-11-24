package club.abelix.api.base.box.repo;

import club.abelix.api.base.box.entity.BoxDto;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
@UtilityClass
public class PlayerBoxesRepository {

    private final static MySQL DATABASE = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    private final static String FIND_ALL_PLAYER_BOXES = "SELECT * FROM player_boxes WHERE player_id = ?";
    private final static String ADD_PLAYER_BOX = "INSERT INTO player_boxes (player_id, box_id, mode, rarity, obtained_at) VALUES (?, ?, ?, ?, ?)";
    private final static String TAKE_BOX = "DELETE FROM player_boxes WHERE id = ? and player_id = ?";
    private final static String REMOVE_ALL_BOXES_BY_PLAYER_ID = "DELETE FROM player_boxes WHERE player_id = ?";

    @SneakyThrows
    public List<BoxDto> findAllBoxByPlayerId(int playerId) {
        return DATABASE.executeQuery(FIND_ALL_PLAYER_BOXES, playerId)
                .exceptionally(t -> {
                    log.error("An error occurred while loading player boxes {}", playerId, t);
                    throw new RuntimeException(t);
                })
                .get(500, TimeUnit.MILLISECONDS)
                .stream()
                .map(PlayerBoxesRepository::map)
                .toList();
    }

    public void removeAllBoxesByPlayerId(int playerId) {
        DATABASE.executeVoidUpdate(REMOVE_ALL_BOXES_BY_PLAYER_ID, playerId).exceptionally(t -> {
            log.error("An error occurred while removing all boxes from player {}", playerId);
            throw new RuntimeException(t);
        });
    }

    public Optional<BoxDto> insertPlayerBox(
            int playerId,
            Rarity rarity,
            int boxId,
            String mode
    ) {
        return DATABASE.executeUpdate(ADD_PLAYER_BOX, playerId, boxId, mode, rarity.getId(), LocalDateTime.now())
                .thenApply(buff -> Optional.of(new BoxDto(
                        buff.getGeneratedKey(),
                        playerId,
                        boxId,
                        mode,
                        rarity,
                        LocalDateTime.now()
                ))).exceptionally(t -> {
                    log.error("An error occurred while inserting boxes for player {}", playerId, t);
                    return Optional.empty();
                }).join();
    }

    public List<BoxDto> insertPlayerBoxes(
            int playerId,
            Rarity rarity,
            int boxId,
            String mode,
            int amount
    ) {
        if (amount <= 0) {
            return Collections.emptyList();
        }

        List<BoxDto> result = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            insertPlayerBox(playerId, rarity, boxId, mode)
                    .ifPresent(result::add);
        }

        return result;
    }

    public void takeBox(
            int playerId,
            int boxId
    ) {
        DATABASE.executeVoidUpdate(TAKE_BOX, boxId, playerId).exceptionally(t -> {
            log.error("An error occurred while removing box from player {}", playerId);
            throw new RuntimeException(t);
        });
    }

    private BoxDto map(final BufferedRow row) {
        return new BoxDto(
                row.getRequiredInt("id"),
                row.getRequiredInt("player_id"),
                row.getRequiredInt("box_id"),
                row.getString("mode").orElse(null),
                Rarity.getById(row.getRequiredInt("rarity")),
                row.getRequiredTimestamp("obtained_at")
        );
    }

}
