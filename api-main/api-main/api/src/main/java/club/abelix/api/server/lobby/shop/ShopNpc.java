package club.abelix.api.server.lobby.shop;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.hovering.HoveringItemsEffect;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import com.comphenix.protocol.wrappers.EnumWrappers;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>ShopNpc creation on 10.03.2024 at 16:02</p>
 */

@RequiredArgsConstructor
public abstract class ShopNpc {
    private final Material material;
    private final String name;
    protected Location npcLocation;

    public void create(@NonNull ConfigurationSection section) {
        npcLocation = LocationUtil.stringToLocation(section.getString("Location"));

        ConfigurationSection skinSection = section.getConfigurationSection("Skin");

        String skinValue = skinSection.getString("Value");
        String skinSignature = skinSection.getString("Signature");

        ConfigurationSection salesSection = section.getConfigurationSection("Sales");
        boolean salesEnabled = salesSection.getBoolean("Enabled");
        List<String> salesLines = salesSection.getStringList("Lines");

        Skin skin = new Skin("", "", skinValue, skinSignature);

        FakeEntityPlayer npc = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(npcLocation, skin);
        npc.startPlayerLooker(2);
        npc.setClickAction(this::clickAction);
        npc.setPublicStatus(true);

        Hologram infoHologram = new Hologram(npcLocation.clone().add(0, 1.0, 0));

        if (salesEnabled) {
            salesLines.forEach(line -> infoHologram.addTextLine(Lang.of(line).compile()));
        }

        infoHologram.addTextLine(Lang.of(name + "_npc_hologram_title").compile());
        infoHologram.addTextLine(Lang.of(name + "_npc_hologram_action").compile());
        infoHologram.setPublicStatus(true);

        new HoveringItemsEffect(npcLocation.clone()).createEffect(material, this::clickAction);
    }

    public abstract void clickAction(Player player);

}
