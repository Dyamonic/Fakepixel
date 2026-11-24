package club.abelix.core.whitelist.listeners;

import club.abelix.commons.language.Language;
import club.abelix.core.app.api.event.EventPriority;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.PlayerLoginEvent;
import club.abelix.core.whitelist.WhitelistCorePlugin;
import club.abelix.network.common.logging.Logging;
import lombok.RequiredArgsConstructor;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>GlobalWhitelistListener creation on 21.11.2022 at 18:10</p>
 */

@RequiredArgsConstructor
public class GlobalWhitelistListener implements Listener {
    private final WhitelistCorePlugin plugin;

    @Subscribe(priority = EventPriority.HIGHEST, threadMode = ThreadMode.POSTING)
    public void onLogin(PlayerLoginEvent event) {
        if (!plugin.getWhitelistGlobalSettings().isEnable()) {
            return;
        }

        Gamer gamer = Gamer.getOfflineGamer(event.getPlayerName(), offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadLanguage();
        });

        if (WhitelistCorePlugin.getGamerAvailability(plugin.getWhitelistGlobalSettings(), gamer)) {
            Logging.IMP.info("[Whitelist] Global whitelist allowed entry {}", gamer.getName());
            return;
        }

        Language language = Language.defaultLanguage();
        if (gamer != null) {
            language = gamer.getLanguage();
        }

        event.setKickReason(language.getMessage(plugin.getWhitelistGlobalSettings().getKickMessageLocalizationKey()));
        event.setCancelled(true);
    }
}
