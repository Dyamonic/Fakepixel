package club.abelix.api.server.lobby.linkage.npc;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.server.lobby.linkage.LinkageRewardManager;
import club.abelix.api.server.lobby.linkage.gui.LinkageRewardGui;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import lombok.val;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public final class LinkageRewardNPC {

    public LinkageRewardNPC(final @NonNull LinkageRewardManager linkageRewardManager,
                            final @NonNull ConfigurationSection section) {
        val location = LocationUtil.stringToLocation(section.getString("Location"));

        Consumer<Player> clickAction =
                player -> new LinkageRewardGui(player, linkageRewardManager, null);

        val skinSection = section.getConfigurationSection("Skin");

        val fakePlayer = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(location,
                new Skin("", "",
                        skinSection.getString("Value"),
                        skinSection.getString("Signature")
                ));

        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(clickAction);
        fakePlayer.setPublicStatus(true);

        val hologram = new Hologram(location.clone().add(0, 1, 0));

        hologram.addTextLine(Lang.of("linkage_npc_hologram_title").compile());
        hologram.addTextLine(Lang.of("linkage_npc_hologram_action").compile());

        hologram.setPublicStatus(true);
    }
}
