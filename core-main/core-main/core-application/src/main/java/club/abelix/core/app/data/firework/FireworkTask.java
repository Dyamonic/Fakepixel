package club.abelix.core.app.data.firework;

import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.common.logging.Logging;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public class FireworkTask implements Runnable {

    public static void schedule(LocalDateTime targetTime) {
        if (LocalDateTime.now().isAfter(targetTime)) {
            Logging.IMP.warn("The target time is in the past. Firework task will not be scheduled.");
            return;
        }

        CoreApplication.getInstance()
                .getScheduler()
                .schedule(new FireworkTask(), calculateDelay(targetTime), TimeUnit.MILLISECONDS);
    }

    private static long calculateDelay(LocalDateTime targetTime) {
        LocalDateTime now = LocalDateTime.now();
        long millisNow = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long millisTarget = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        long delayMillis = millisTarget - millisNow;

        if (delayMillis < 0) {
            throw new IllegalArgumentException("Target time must be in the future.");
        }

        return delayMillis; // Перевод миллисекунд в тики (1 тик = 50 мс)
    }

    @Override
    public void run() {
        Gamer.getGamers().forEach(gamer -> {
            gamer.sendLocalizedMessage("firework_new_year");
        });
    }
}