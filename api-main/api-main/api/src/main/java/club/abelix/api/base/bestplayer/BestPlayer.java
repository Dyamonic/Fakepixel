package club.abelix.api.base.bestplayer;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.bestplayer.BestPlayerDonationEntry;
import club.abelix.commons.data.bestplayer.BestPlayerEntry;
import club.abelix.commons.data.bestplayer.BestPlayerSqlHandler;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.network.common.logging.Logging;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Getter
public class BestPlayer {

    /**
     * Базовая цена почетного игрока
     */
    public static final int BASE_COST = 10;

    /**
     * Максимальный размер пожертвования одному игроку
     */
    public static final int DONATE_SUM = 2;

    /**
     * Сумма становится дешевле с каждым днем на это количество клаудов
     */
    private static final int DAILY_DISCOUNT = 2;

    /**
     * Пока не пройдет это время с момента покупки, место нельзя перекупить
     */
    private static final long TIME_BEFORE_BUY = TimeUnit.HOURS.toMillis(3);

    private final BestPlayerManager manager;
    private final BestPlayerEntry entry;

    private final Gamer gamer;
    private final Location location;
    private final Set<BestPlayerDonationEntry> donateList = new HashSet<>();
    private BestPlayerNpc bestPlayerNpc;

    public void create() {
        bestPlayerNpc = new BestPlayerNpc(manager, gamer);
        bestPlayerNpc.create(location);
        bestPlayerNpc.startTask();
    }

    public int getPrice() {
        int price = entry.price() + (donateList.size() * DONATE_SUM);

        int startDay = (int) TimeUnit.MILLISECONDS.toDays(entry.time());
        int currentDay = (int) TimeUtil.getDayUTC();

        if (currentDay > startDay) {
            int daysElapsed = currentDay - startDay;

            price -= daysElapsed * DAILY_DISCOUNT;

            if (price < 5) {
                price = 10;
            }
        }

        return price;
    }

    public boolean canBuy() {
        return getTimeBeforeBuy() < 0;
    }

    public boolean isDonationsAllowed() {
        return entry.id() != 0;
    }

    public long getTimeBeforeBuy() {
        return (entry.time() + TIME_BEFORE_BUY) - System.currentTimeMillis();
    }

    public void remove() {
        bestPlayerNpc.remove();
        bestPlayerNpc = null;
    }

    public boolean canDonate(@NonNull Gamer gamer) {
        for (BestPlayerDonationEntry donation : getDonateList()) {
            if (gamer.getPlayerId() == donation.donaterId()) {
                return false;
            }
        }

        return true;
    }

    public void reloadDonations() {
        BestPlayerSqlHandler.INSTANCE.getPlayerDonations(entry.id()).whenComplete((bestPlayerDonationEntries, throwable) -> {
            if (throwable != null) {
                Logging.IMP.error("[BestPlayer] Failed reload donations. Detail:");
                throwable.printStackTrace();
                return;
            }

            donateList.clear();
            addDonations(bestPlayerDonationEntries);
        });
    }

    public void addDonations(@NonNull Set<BestPlayerDonationEntry> donations) {
        this.donateList.addAll(donations);
    }

    public void addDonation(@NonNull BestPlayerDonationEntry donation) {
        this.donateList.add(donation);
    }

}
