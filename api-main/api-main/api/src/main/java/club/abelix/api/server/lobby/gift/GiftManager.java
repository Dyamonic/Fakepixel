package club.abelix.api.server.lobby.gift;

import club.abelix.api.base.discord.DiscordSyncQueueSqlHandler;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.gift.data.Gift;
import club.abelix.api.server.lobby.gift.data.GiftType;
import club.abelix.api.server.lobby.gift.gui.GiftBookGui;
import club.abelix.api.server.lobby.gift.sql.GiftSqlHandler;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.both.BUserRankRefreshPacket;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Log4j2
@NoArgsConstructor
public class GiftManager {

    public static final GiftManager INSTANCE = new GiftManager();

    private final Map<Gamer, PendingGift> PENDING_GIFTS = new HashMap<>();

    public PendingGift sendGift(Gamer sender, Gamer receiver, GiftType type) {
        PendingGift gift = new PendingGift(sender, receiver, type);
        PENDING_GIFTS.put(sender, gift);

        sender.sendLocalizedMessage("gift_sent", "%player%", receiver.getDisplayName());
        new GiftBookGui(receiver, gift);

        HeartBeat.runLaterSync(() -> {
            if (!PENDING_GIFTS.containsValue(gift)) {
                return;
            }

            PENDING_GIFTS.remove(sender, gift);

            sender.sendLocalizedMessage("gift_expired");
            receiver.sendLocalizedMessage("gift_expired");
        }, 20 * 60); // gifts expire in 1 minute

        return gift;
    }

    public boolean hasSentGift(Gamer gamer) {
        return PENDING_GIFTS.containsKey(gamer);
    }

    public boolean hasReceivedGift(Gamer gamer) {
        return PENDING_GIFTS.values().stream()
                .anyMatch(gift -> gift.receiver.equals(gamer));
    }

    public PendingGift getGiftByReceiver(Gamer gamer) {
        return PENDING_GIFTS.values().stream()
                .filter(gift -> gift.receiver.equals(gamer))
                .findFirst()
                .orElse(null);
    }

    public PendingGift getGiftBySender(Gamer gamer) {
        return PENDING_GIFTS.values().stream()
                .filter(gift -> gift.sender.equals(gamer))
                .findFirst()
                .orElse(null);
    }

    public Gamer getSender(Gamer gamer) {
        return getGiftByReceiver(gamer).sender;
    }

    public void acceptGift(@NonNull Gamer gamer) {
        PendingGift gift = getGiftByReceiver(gamer);
        GiftType type = gift.type;
        Gamer sender = gift.sender;

        sender.takeGold(type.getPrice());

        val databaseGift = new Gift(sender.getPlayerId(),
                gamer.getPlayerId(), type, LocalDateTime.now());

        GiftSqlHandler.INSTANCE.insert(databaseGift).join();

        GiftSqlHandler.INSTANCE.selectBySender(sender.getPlayerId())
                .thenAccept(gifts -> {
                    for (val reward : GiftBonuses.resolve(gifts)) {
                        reward.give(sender);
                    }
                });

        gamer.addProperty("Gifted", 1);

        type.getReward().give(sender);

        val time = type.getTime();
        val rank = StarshipBukkit.getInstance().getRanksService().findRankById(type.getRankId())
                .orElse(null);

        LocalDateTime expires = null;

        if (time > 0) {
            expires = LocalDateTime.now().plusDays(time);
        }

        if (rank != null) {
            StarshipBukkit.getInstance()
                    .getUserRanksService()
                    .addUserRank(gamer.getPlayerId(), rank, expires)
                    .whenComplete((r, t) -> {
                        if (t != null) {
                            gamer.sendLocalizedMessage("error_rank_add_ask_support");
                            log.error("Failed to add rank {} to gamer {}: {}", rank.getId(), gamer.getName(), t.getMessage(), t);
                            return;
                        }

                        gamer.loadRanks();
                        Connection.sendPacket(new BUserRankRefreshPacket(gamer.getPlayerId()));
                        DiscordSyncQueueSqlHandler.INSTANCE
                                .enqueuePlayerSync(gamer.getPlayerId(), "rank_change")
                                .exceptionally(queueError -> {
                                    log.error("Failed to enqueue Discord sync for gamer {} after adding rank {}: {}",
                                            gamer.getName(), rank.getId(), queueError.getMessage(), queueError);
                                    return null;
                                });
                    });

            Gamer.getGamers().forEach(g -> {
                g.sendLocalizedMessage("gift_accepted",
                        "%player%", gamer.getDisplayName(),
                        "%sender%", sender.getDisplayName(),
                        "%gift%", rank.getDisplayName(g.getLanguage())
                );
            });

            PENDING_GIFTS.remove(sender);
        }
    }

    public void denyGift(Gamer gamer) {
        PendingGift gift = getGiftByReceiver(gamer);
        Gamer sender = gift.sender;

        PENDING_GIFTS.remove(sender);

        sender.sendLocalizedMessage("gift_denied", "%player%", gamer.getDisplayName());
    }

    public record PendingGift(Gamer sender, Gamer receiver, GiftType type) {
    }

}
