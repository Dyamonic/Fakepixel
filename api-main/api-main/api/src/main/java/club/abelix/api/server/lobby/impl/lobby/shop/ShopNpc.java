package club.abelix.api.server.lobby.impl.lobby.shop;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

@UtilityClass
public class ShopNpc {

    private final FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();

    public void create(ConfigurationSection configurationSection) {
        Location location = LocationUtil.stringToLocation(configurationSection.getString("Location"));

        ConfigurationSection skinSection = configurationSection.getConfigurationSection("Skin");

        String skinValue = skinSection.getString("Value");
        String skinSignature = skinSection.getString("Signature");

        Skin skin = new Skin("st" + CoreUtil.getMaster().name(),
                UUID.randomUUID().toString(), skinValue, skinSignature);

        FakeEntityPlayer fakeEntityPlayer = fakeEntityApi.createPlayer(location, skin);
        fakeEntityPlayer.startPlayerLooker(2);
//        fakeEntityPlayer.setClickAction(player -> LobbyHotbar.getOpenShop().accept(Gamer.getGamer(player)));
        fakeEntityPlayer.setPublicStatus(true);

        FalconAPI.getInstance().getLabyNpcEmotesService().addNPCEmotes(fakeEntityPlayer, 55);

        Hologram questHologram = new Hologram(location.clone().add(0, 1.0, 0));
        questHologram.addTextLine(Lang.of("shop_npc_hologram_title").compile());
        questHologram.addTextLine(Lang.of("shop_npc_hologram_action").compile());
        questHologram.setPublicStatus(true);
    }

}
