package club.abelix.core.velocity.skin.listener;

import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.skin.SkinsManager;
import club.abelix.network.common.logging.Logging;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GlobalSkinListener {

    StarshipVelocity starship;

    @Subscribe(priority = 100)
    public EventTask onGameProfileRequest(GameProfileRequestEvent event) {
        int playerId = NetworkSqlHandler.INSTANCE.getPlayerId(event.getUsername());

        val future = SkinsApi.getPlayerSkin(playerId, event.getUsername()).whenComplete((skin, throwable) -> {
            if (throwable != null) {
                Logging.IMP.error("Failed get player {} skin. Details: ", event.getUsername());
                throwable.printStackTrace();
                return;
            }

            if (skin == null) {
                skin = SkinsApi.DEFAULT_SKIN;
            }

            event.setGameProfile(starship.getSkinsManager().updateProfileSkin(event.getGameProfile(), skin));
            Logging.IMP.info("[SKIN] User {} success loaded skin!", event.getUsername());
        });

        return EventTask.resumeWhenComplete(future);
    }

    @Subscribe(priority = 100)
    public void onPluginMessage(final PluginMessageEvent event) {
        if (event.getIdentifier() != SkinsManager.updateSkinChannel) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

}
