package club.abelix.api.server.lobby.actionnpc;

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
public class ActionNpc {

    Skin skin;
    Location location;
    String localizationKey;
    String command;
    String messageKey;

    public void spawn() {
        FakeEntityPlayer npc = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(location, skin);
        npc.startPlayerLooker(2);
        npc.setClickAction((player, __) -> {
            if (command != null && !command.isEmpty()) {
                player.performCommand(command);
            } else {
                val gamer = Gamer.getGamer(player);
                val message = gamer.getLanguage().getMessage(messageKey);

                try {
                    gamer.sendJsonMessage(message);
                } catch (Exception ignored) {
                    gamer.sendMessage(message);
                }
            }
        });
        npc.setPublicStatus(true);

        Hologram infoHologram = new Hologram(location.clone().add(0, 1.0, 0));
        infoHologram.addTextLine(Lang.of(localizationKey + "_action_npc_hologram_title").compile());
        infoHologram.addTextLine(Lang.of(localizationKey + "_action_npc_hologram_action").compile());
        infoHologram.setPublicStatus(true);
    }

    public static ActionNpc of(ConfigurationSection configurationSection) {
        val command = configurationSection.getString("Command");
        val message = configurationSection.getString("Message");
        val localizationKey = configurationSection.getString("LocalizationKey");
        val skinSection = configurationSection.getConfigurationSection("Skin");
        val location = LocationUtil.stringToLocation(configurationSection.getString("Location"));
        val skin = new Skin("", "", skinSection.getString("Value"), skinSection.getString("Signature"));

        return new ActionNpc(skin, location, localizationKey, command, message);
    }
}
