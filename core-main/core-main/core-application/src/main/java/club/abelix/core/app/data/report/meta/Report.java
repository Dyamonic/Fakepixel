package club.abelix.core.app.data.report.meta;

import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.type.report.ReportReason;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Report {

    private final Map<Gamer, ReportReason> senders = new HashMap<>();
    @Setter
    private Gamer gamer;
    //время последней жалобы
    private long lastReportTime;

    public Report(final Gamer gamer) {
        this.gamer = gamer;
    }

    public void addSender(@NonNull Gamer gamer, @NonNull ReportReason reason) {
        senders.put(gamer, reason);
        lastReportTime = System.currentTimeMillis();
    }

    public boolean hasSender(@NonNull Gamer gamer) {
        return senders.containsKey(gamer);
    }

}
