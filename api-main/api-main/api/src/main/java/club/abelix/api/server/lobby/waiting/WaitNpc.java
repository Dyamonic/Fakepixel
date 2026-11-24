package club.abelix.api.server.lobby.waiting;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.data.skin.Skin;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Location;

public class WaitNpc {

    public void createEntities(@NonNull Skin skin, @NonNull Location location) {
        FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();

        FakeEntityPlayer fakePlayer = fakeEntityApi.createPlayer(location, skin);
        fakePlayer.startPlayerLooker(5);
        fakePlayer.setPublicStatus(true);

        fakePlayer.setClickAction(player -> {
            val gamer = Gamer.getGamer(player);

            gamer.sendLocalizedMessage("Coming soon...");
            gamer.playSound(YSound.VILLAGER_HIT);
        });
    }

}