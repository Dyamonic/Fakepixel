package club.abelix.core.app.data.report;

import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.report.meta.Report;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public final class ReportPlayerListener implements Listener {

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerJoin(final GamerJoinEvent event) {
        final Gamer gamer = event.getGamer();
        final Report gamerReport = ReportManager.INSTANCE.getReport(gamer);

        // Обновляем геймера в мапе, ибо ссылка на него старая
        if (gamerReport != null) {
            gamerReport.setGamer(gamer);
        }
    }
}
