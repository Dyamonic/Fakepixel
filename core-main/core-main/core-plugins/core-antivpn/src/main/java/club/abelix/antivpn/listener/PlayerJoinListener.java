package club.abelix.antivpn.listener;

import club.abelix.antivpn.manager.AntiVPNManager;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.GamerLoadEvent;
import club.abelix.network.common.logging.Logging;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */

@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class PlayerJoinListener implements Listener {

    private static final long BYPASS_TIME = TimeUnit.HOURS.toSeconds(12);

    AntiVPNManager antiVPNManager;

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onPlayerJoin(GamerLoadEvent event) {
        val gamer = event.getGamer();
        val address = gamer.getAddress().getHostAddress();

        if (gamer.hasPermission("vpn.bypass") || gamer.getGlobalTime() > BYPASS_TIME) {
            return;
        }

        try {
            antiVPNManager.check(address).whenComplete((verdict, throwable) -> {
                if (throwable != null) {
                    Logging.IMP.error("[AntiVpn] Failed check {}. Details:", address);
                }

                if (verdict) {
                    event.setCancelled(true);
                    event.setCancelReason(gamer.getLanguage().getMessage("antivpn_kick", "%ip%", address));
                }
            }).get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException error) {
            Logging.IMP.error("[AntiVpn] Manager failed check {}. Details:", address);
        }
    }

}
