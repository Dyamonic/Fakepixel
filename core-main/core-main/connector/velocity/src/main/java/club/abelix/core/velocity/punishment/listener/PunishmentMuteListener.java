package club.abelix.core.velocity.punishment.listener;

import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.commons.language.Language;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.core.velocity.punishment.PunishmentsManager;
import club.abelix.core.velocity.unsigned.event.UnsignedChatEvent;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PunishmentMuteListener {

    PunishmentsManager punishmentsManager;

    @Subscribe
    public void onPlayerChat(final @NotNull UnsignedChatEvent event) {
        handleMute(event.getPlayer(), event);
    }

    private void handleMute(Player player, final ResultedEvent<UnsignedChatEvent.ChatResult> cancellable) {
        Gamer gamer = Gamer.getGamer(player);
        if (gamer == null) {
            cancellable.setResult(UnsignedChatEvent.ChatResult.denied());
            return;
        }
        MuteMeta muteMeta = punishmentsManager.getActiveMute(gamer);

        if (muteMeta == null) {
            return;
        }
        cancellable.setResult(UnsignedChatEvent.ChatResult.denied());
        Language language = gamer.getLanguage();

        gamer.sendLocalizedMessage("muted_message_format",
                "%time_left%", muteMeta.getExpirationString(language),
                "%meta_id%", muteMeta.getId(),
                "%reason%", muteMeta.getReason(language));
    }

    @Subscribe(priority = 50)
    public void onPlayerQuit(DisconnectEvent event) {
        val player = event.getPlayer();
        val gamer = Gamer.getGamer(player);

        if (gamer != null) {
            punishmentsManager.removeActiveMute(gamer.getPlayerId());
        }
    }

}
