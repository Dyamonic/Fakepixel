package club.abelix.api.base.bestplayer;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.bestplayer.BestPlayerDonationEntry;
import club.abelix.commons.data.bestplayer.BestPlayerEntry;
import club.abelix.commons.data.bestplayer.BestPlayerSqlHandler;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.play.both.BBestPlayerDonate;
import club.abelix.network.packet.play.both.BBestPlayerUpdate;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Location;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class BestPlayerManager {

    @Getter
    private final AbstractPlugin plugin;

    private final Location location;

    private boolean updaterStarted = false;

    @Getter
    private BestPlayer currentBestPlayer;

    public BestPlayerManager(@NonNull AbstractPlugin plugin, @NonNull Location location) {
        this.plugin = plugin;
        this.location = location;

        new BestPlayerListener(this);
    }

    private void runUpdater() {
        if (updaterStarted) {
            return;
        }

        updaterStarted = true;

        HeartBeat.runTimerAsync(this::load, HeartBeat.TEN_MINUTES, HeartBeat.TEN_MINUTES);
    }

    public void load() {
        loadRecord().whenComplete((bestPlayerEntry, throwable) -> {
            if (throwable != null) {
                Logging.IMP.error("[BestPlayer] Failed initialization. Details:");
                throwable.printStackTrace();
                return;
            }

            boolean recreateEntity = true;

            if (currentBestPlayer != null) {
                if (bestPlayerEntry.playerId() == currentBestPlayer.getGamer().getPlayerId()) {
                    recreateEntity = false;
                }
            }

            if (recreateEntity) {
                val newBestPlayer = loadBestPlayer(bestPlayerEntry);

                if (newBestPlayer == null) {
                    return;
                }

                newBestPlayer.create();

                if (currentBestPlayer != null) {
                    currentBestPlayer.remove();
                }

                currentBestPlayer = newBestPlayer;
            }

            BestPlayerSqlHandler.INSTANCE.getPlayerDonations(bestPlayerEntry.id()).thenAccept(
                    bestPlayerDonationEntries ->
                            currentBestPlayer.addDonations(bestPlayerDonationEntries));
        });

        runUpdater();
    }

    public CompletableFuture<BestPlayerEntry> loadRecord() {
        return BestPlayerSqlHandler.INSTANCE.getBestPlayer().exceptionally(throwable -> {
            Logging.IMP.error("[BestPlayer] Failed load player. Details:");
            throwable.printStackTrace();
            return null;
        }).thenApply(bestPlayerEntry ->
                Objects.requireNonNullElseGet(bestPlayerEntry,
                        () -> new BestPlayerEntry(0, 1, BestPlayer.BASE_COST, 0)));
    }

    public BestPlayer loadBestPlayer(BestPlayerEntry entry) {
        Gamer gamer = Gamer.getOrCreate(entry.playerId(), offline -> {
            offline.loadRanks();
            offline.loadPrefixAndUpdateName();
            offline.loadSuffix();
            offline.loadSkull();
            offline.loadProperties();
        });

        if (gamer == null) {
            Logging.IMP.error("[BestPlayer] Failed load gamer. Id {}", entry.playerId());
            return null;
        }

        Logging.IMP.info("[BestPlayer] Success load, username={}, id={}",
                gamer.getDisplayName(), gamer.getPlayerId());

        return new BestPlayer(this, entry, gamer, location);
    }

    public void update(@NonNull Gamer gamer, int price) {
        HeartBeat.runAsync(() -> BestPlayerSqlHandler.INSTANCE.changeBestPlayer(gamer.getPlayerId(), price).thenAccept(bufferedExecution -> {
            if (Connection.isActive()) {
                Connection.sendPacket(new BBestPlayerUpdate(currentBestPlayer.getGamer().getPlayerId()));
            }

            load();
        }));
    }

    public void addDonation(@NonNull Gamer gamer) {
        final BestPlayerDonationEntry donationEntry = new BestPlayerDonationEntry(gamer.getPlayerId(), System.currentTimeMillis());
        currentBestPlayer.addDonation(donationEntry);

        BestPlayerSqlHandler.INSTANCE.addDonation(currentBestPlayer.getEntry().id(), donationEntry);

        if (Connection.isActive()) {
            Connection.sendPacket(new BBestPlayerDonate(currentBestPlayer.getGamer().getPlayerId(), gamer.getPlayerId(),
                    BestPlayer.DONATE_SUM));
        }
    }


}
