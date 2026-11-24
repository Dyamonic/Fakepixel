package club.abelix.core.app.data.punishment.listeners;

import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;
import club.abelix.core.app.data.punishment.exception.PunishmentException;
import club.abelix.core.app.event.core.ClientActiveEvent;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.app.event.gamer.PlayerLoginEvent;
import club.abelix.core.app.util.LanguageUtil;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.client.CBanPlayer;
import club.abelix.network.packet.play.client.CMutePlayer;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import lombok.RequiredArgsConstructor;
import org.greenrobot.eventbus.Subscribe;

@RequiredArgsConstructor
public class PunishmentListener implements Listener, PlayClientProcessor {

    private static final PunishmentManager MANAGER = PunishmentManager.INSTANCE;

    @Subscribe
    public void onBanLogin(PlayerLoginEvent event) {
        if (event.getState() == PlayerLoginEvent.State.POST) {
            return;
        }

        BanMeta ban = MANAGER.getActiveBan(event.getPlayerName(), event.getAddress());

        if (ban == null) {
            return;
        }

        Language language;
        if (CoreApplication.getInstance().getGeoLocationManager() != null) {
            language = LanguageUtil.getLanguage(CoreApplication.getInstance().getGeoLocationManager(), event.getAddress());
        } else {
            language = Language.defaultLanguage();
        }

        event.setKickReason(MANAGER.getPlayerDisconnectReason(language, ban));
        event.setCancelled(true);
    }

    @Subscribe
    public void onBannedPropertyResetJoin(GamerJoinEvent event) {
        Gamer gamer = event.getGamer();

        // сбрасываем значение Banned у игрока, чтобы
        // он снова мог отображаться в топах и т.д.
        if (gamer.getProperty("Banned") == 1) {
            gamer.dropProperty("Banned");
        }
    }

    @Subscribe
    public void onMutedPlayerJoin(GamerJoinEvent event) {
        Gamer player = event.getGamer();

        MuteMeta muteMeta = MANAGER.getActiveIdMute(player.getPlayerId());

        if (muteMeta != null) {
            //отправляем данные на банжу игрока
            MANAGER.sendBungeeMutePacket(player, muteMeta);
        }
    }

    @Subscribe
    public void onActive(ClientActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    @Override
    public void process(CMutePlayer packet, ChannelProcessorContext ctx) {
        int playerId = packet.getPlayerId();

        Gamer target = Gamer.getOfflineGamer(playerId, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
        });

        if (target == null) {
            Logging.IMP.warn("Игрок для выдачи мута (ID {}) не найден!", playerId);
            return;
        }

        try {
            MANAGER.processMute(packet.getAdmin(), target.getName(), packet.getReason(), packet.getTime());
        } catch (PunishmentException e) {
            Logging.IMP.warn("Не удалось выдать мут игроку {}", target.getName());
            e.printStackTrace();
        }
    }


    @Override
    public void process(CBanPlayer packet, ChannelProcessorContext ctx) {
        Gamer target = Gamer.getOfflineGamer(packet.getTarget(), offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
        });

        if (target == null && !Patterns.isIPV4(packet.getTarget())) {
            Logging.IMP.warn("Игрок для выдачи бана ({}) не найден!", packet.getTarget());
            return;
        }

        try {
            MANAGER.processBan(packet.getAdmin(), packet.getTarget(), packet.getReason(), packet.getTime());
        } catch (PunishmentException e) {
            Logging.IMP.warn("Не удалось выдать бан игроку {}", packet.getTarget());
            e.printStackTrace();
        }
    }
}
