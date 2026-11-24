package club.abelix.core.velocity.skin;

import club.abelix.commons.data.skin.Skin;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.skin.command.SkinCommand;
import club.abelix.core.velocity.skin.listener.GlobalSkinListener;
import club.abelix.core.velocity.skin.listener.TemporarySkinListener;
import club.abelix.network.packet.play.client.CPlayerChangeSkin;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.util.GameProfile;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class SkinsManager {

    public final static String TEXTURE_PROPERTY = "textures";
    // ==== Каналы ====
    public final static MinecraftChannelIdentifier tempSkinChannel
            = MinecraftChannelIdentifier.create("axskin", "temps");
    public final static MinecraftChannelIdentifier customSkinChannel
            = MinecraftChannelIdentifier.create("axskin", "custom");
    public final static MinecraftChannelIdentifier updateSkinChannel
            = MinecraftChannelIdentifier.create("axskin", "update");
    private final StarshipVelocity starship;
    // ===========

    public static void setTexturesProperty(@NonNull final Player player, @NonNull final Skin skin) {
        player.setGameProfileProperties(updatePropertiesSkin(player.getGameProfileProperties(), toProperty(skin)));
    }

    public static void clearTexturesProperty(@NonNull final Player player) {
        player.getGameProfile().getProperties().removeIf(p -> p.getName().equals(TEXTURE_PROPERTY));
    }

    public static Skin getCurrentSkin(@NonNull Player player) {
        for (GameProfile.Property property : player.getGameProfileProperties()) {
            if (property.getName().equals(TEXTURE_PROPERTY)) {
                return new Skin(player.getUsername(), player.getUniqueId().toString(),
                        property.getValue(), property.getSignature());
            }
        }
        return null;
    }

    public static GameProfile.Property toProperty(final Skin skin) {
        return new GameProfile.Property(TEXTURE_PROPERTY, skin.getValue(), skin.getSignature());
    }

    private static List<GameProfile.Property> updatePropertiesSkin(List<GameProfile.Property> original, GameProfile.Property property) {
        List<GameProfile.Property> properties = new ArrayList<>(original);
        boolean applied = false;

        int i = 0;
        for (GameProfile.Property lProperty : properties) {
            if (TEXTURE_PROPERTY.equals(lProperty.getName())) {
                properties.set(i, property);
                applied = true;
            }
            i++;
        }

        if (!applied)
            properties.add(property);

        return properties;
    }

    public void initialize() {
        starship.getProxy().getEventManager().register(starship, new GlobalSkinListener(starship));
        starship.getProxy().getEventManager().register(starship, new TemporarySkinListener(starship));

        starship.getCommandManager().addCommand(new SkinCommand(starship));

        // Регистрация каналов
        starship.getProxy().getChannelRegistrar().register(tempSkinChannel, customSkinChannel, updateSkinChannel);
    }

    public void setSkin(@NonNull final Player player, @NonNull final Skin skin) {
        setTexturesProperty(player, skin);
        updateSkinOnBukkit(player, skin);
    }

    public void updateSkinOnBukkit(@NonNull final Player player, @NonNull final Skin skin) {
        updateSkinOnBukkit(player, skin, false);
    }

    public void updateSkinOnBukkit(@NonNull final Player player, @NonNull final Skin skin, final boolean forceUpdate) {
        Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();

        ByteArrayDataOutput response = ByteStreams.newDataOutput();

        response.writeUTF(skin.getName());
        response.writeUTF(skin.getValue());
        response.writeUTF(skin.getSignature());
        response.writeBoolean(forceUpdate);

        optionalServerConnection.ifPresent(serverConnection -> serverConnection.sendPluginMessage(updateSkinChannel, response.toByteArray()));

    }

    public GameProfile updateProfileSkin(GameProfile profile, Skin skin) {
        GameProfile.Property textures = toProperty(skin);
        List<GameProfile.Property> oldProperties = profile.getProperties();

        return new GameProfile(profile.getId(), profile.getName(), updatePropertiesSkin(oldProperties, textures));
    }

    public void notifyCore(int playerId) {
        starship.getConnection().sendPacket(new CPlayerChangeSkin(playerId));
    }


}
