package club.abelix.api.base.util.skin;

import club.abelix.api.FalconAPI;
import club.abelix.commons.data.skin.Skin;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.NonNull;
import org.bukkit.entity.Player;


public class SkinUtil {

    public static void selectSkinOnBungee(@NonNull Player player, @NonNull String skinName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(skinName);

        player.sendPluginMessage(FalconAPI.getInstance(), "axskin:custom", out.toByteArray());
    }

    public static void selectTemporarySkin(@NonNull Player player, @NonNull Skin skin) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(skin.getValue());
        out.writeUTF(skin.getSignature());

        player.sendPluginMessage(FalconAPI.getInstance(), "axskin:temps", out.toByteArray());
    }

}
