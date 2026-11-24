package club.abelix.api.server.lobby.information;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.util.time.CooldownUtil;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>InformationNpc creation on 10.11.2023 at 15:34</p>
 */

public class InformationNpc {

    private final FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();
    private final Location location;
    private final List<String> informationLines;
    private final String message;
    private final Skin skin;

    public InformationNpc(@NonNull Location location, @NonNull List<String> informationLines,
                          @NonNull String message, @NonNull Skin skin) {

        this.location = location;
        this.informationLines = informationLines;
        this.message = message;
        this.skin = skin;

    }


    public void create() {
        FakeEntityPlayer fakePlayer = fakeEntityApi.createPlayer(location, this.skin);
        fakePlayer.setGlowing(true);
        fakePlayer.setGlowingColor(ChatColor.GOLD);
        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(player -> {
            Gamer gamer = Gamer.getGamer(player);
            gamer.sendJsonMessage(message);

            if (!CooldownUtil.hasCooldown(player.getName() + "informationnpc")){

                Location location = player.getLocation();

                Firework firework = location.getWorld().spawn(location, Firework.class);

                FireworkMeta fireworkEffectMeta = firework.getFireworkMeta();
                fireworkEffectMeta.addEffect(FireworkEffect.builder()
                        .withColor(Color.YELLOW).withFade(Color.ORANGE).withFlicker().with(FireworkEffect.Type.BALL)
                        .build());

                firework.setFireworkMeta(fireworkEffectMeta);
                CooldownUtil.addCooldown(player.getName() + "informationnpc", 5, TimeUnit.SECONDS);
            }
        });

        Hologram hologram = new Hologram(location.clone().add(0, 1.0, 0));

        for (String line : informationLines) {
            hologram.addTextLine(line);
        }

        fakePlayer.setPublicStatus(true);
        hologram.setPublicStatus(true);
    }


}
