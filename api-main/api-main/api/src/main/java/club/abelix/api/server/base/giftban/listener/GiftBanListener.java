package club.abelix.api.server.base.giftban.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.base.giftban.GiftBanService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class GiftBanListener implements Listener {

    GiftBanService giftBanService;

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        val gamer = Gamer.getGamer(event.getPlayer());

        if (giftBanService.hasPendingGiftBan(gamer)) {
            giftBanService.openExist(gamer);
        }
    }

}
