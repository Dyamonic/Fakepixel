package club.abelix.api.base.bestplayer;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;


/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>BestPlayerNPC creation on 03.09.2023 at 17:00</p>
 */

@RequiredArgsConstructor
public class BestPlayerNpc {

    private final BestPlayerManager manager;
    private final Gamer gamer;

    private AbstractTask task;

    private Hologram hologram;
    private FakeEntityPlayer fakePlayer;

    public void create(@NonNull Location location) {
        HeartBeat.runLaterSync(() -> {
            fakePlayer = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(location, gamer.getSkin());
            fakePlayer.setSneaking(false);
            fakePlayer.setGlowingColor(ChatColor.GREEN);
            fakePlayer.setGlowing(true);
            fakePlayer.startPlayerLooker(2);
            fakePlayer.setClickAction(player -> new BestPlayerGui(player, manager));

            FalconAPI.getInstance().getLabyNpcEmotesService().addNPCEmotes(fakePlayer, 76);

            hologram = new Hologram(location.clone().add(0, 1.5, 0));
            hologram.addTextLine(Lang.of("best_player_hologram_line_1").compile());
            hologram.addTextLine(Lang.of("best_player_hologram_line_2").set("%name%", gamer.getDisplayName()).compile());
            hologram.addTextLine(Lang.of("best_player_hologram_line_3").compile());

            fakePlayer.setPublicStatus(true);
            hologram.setPublicStatus(true);
        }, 45);
    }

    public void startTask() {

        if (task != null) return;
        task = new AbstractTask(FalconAPI.getInstance()) {

            @Override
            protected void run() {
                if (fakePlayer == null) return;

                boolean sneaking = !fakePlayer.isSneaking();

                fakePlayer.setSneaking(sneaking);

                double dy = sneaking ? -0.375 : 0.375;

                hologram.teleport(hologram.getLocation().clone().add(0, dy, 0));
            }
        }.timer(0, 20);
    }

    public void remove() {
        if (task != null) task.cancel();

        fakePlayer.remove();
        hologram.remove();
    }
}
