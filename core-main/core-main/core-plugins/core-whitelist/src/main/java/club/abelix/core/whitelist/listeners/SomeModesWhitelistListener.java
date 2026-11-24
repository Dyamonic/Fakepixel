package club.abelix.core.whitelist.listeners;

import club.abelix.core.app.api.event.EventPriority;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.PlayerRedirectEvent;
import club.abelix.core.whitelist.WhitelistCorePlugin;
import club.abelix.core.whitelist.settings.WhitelistSettings;
import club.abelix.network.common.logging.Logging;
import lombok.RequiredArgsConstructor;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>SomeModesWhitelistListener creation on 21.11.2022 at 18:10</p>
 */

@RequiredArgsConstructor
public class SomeModesWhitelistListener implements Listener {

    private final WhitelistCorePlugin plugin;

    @Subscribe(priority = EventPriority.HIGHEST, threadMode = ThreadMode.POSTING)
    public void onRedirect(PlayerRedirectEvent event) {
        WhitelistSettings settings = plugin.getSomeModesSettings().get(event.getTarget().getMode());

        if (settings == null || !settings.isEnable()) {
            return;
        }

        if (WhitelistCorePlugin.getGamerAvailability(settings, event.getGamer())) {
            Logging.IMP.info("[Whitelist] Mode whitelist {} allowed entry {}",
                    event.getTarget().getMode().getName(), event.getGamer().getName());
            return;
        }

        event.getGamer().sendLocalizedMessage(settings.getKickMessageLocalizationKey());
        event.setCancelled(true);
    }

}
