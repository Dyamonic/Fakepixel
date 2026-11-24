package club.abelix.core.velocity.alpha.listener;

import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.alpha.AlphaServerWhitelist;
import club.abelix.core.velocity.event.GamerLoginEvent;
import club.abelix.core.velocity.gamer.Gamer;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>AlphaServerListener creation on 30.07.2023 at 15:42</p>
 */

@RequiredArgsConstructor
public class AlphaServerListener {

    private final StarshipVelocity starship;

    @Subscribe
    public void onJoin(GamerLoginEvent event) {
        Gamer gamer = event.getGamer();
        String username = gamer.getName();
        AlphaServerWhitelist alphaServerWhitelist = starship.getAlphaServerWhitelist();

        if (alphaServerWhitelist != null) {
            if (alphaServerWhitelist.getEnabled() && alphaServerWhitelist.getAvailableNickname().contains(username.toLowerCase())) {
                return;
            }
        } else {
            if (gamer.getRanks() != null && gamer.hasPermission("alpha.access")) {
                return;
            }
        }

        event.setResult(
                ResultedEvent.ComponentResult.denied(
                        Component.text(event.getGamer().getLanguage().getMessage("alpha_server_join"))
                )
        );
    }
}
