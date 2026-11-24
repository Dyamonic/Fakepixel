package club.abelix.api.server.lobby.shopkeeper;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ShopKeeperNpc {
    Skin skin;
    Location location;

    public void spawn() {
        FakeEntityPlayer npc = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(location, skin);
        npc.setClickAction((player, interactAction) -> Gamer.getGamer(player).dispatchCommand("cosmetics"));
        npc.setPublicStatus(true);

        Hologram infoHologram = new Hologram(location.clone().add(0, 1.0, 0));
        infoHologram.addTextLine(Lang.of("shoopkeeper_npc_hologram_title").compile());
        infoHologram.addTextLine(Lang.of("shoopkeeper_npc_hologram_action").compile());
        infoHologram.setPublicStatus(true);
    }


    public static ShopKeeperNpc of(ConfigurationSection configurationSection) {
        val skinSection = configurationSection.getConfigurationSection("Skin");
        val location = LocationUtil.stringToLocation(configurationSection.getString("Location"));
        val skin = new Skin("", "", skinSection.getString("Value"), skinSection.getString("Signature"));

        return new ShopKeeperNpc(skin, location);
    }
}
