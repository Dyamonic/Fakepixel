package club.abelix.api.server.lobby.impl.hub.present.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.impl.hub.present.PresentGamerSession;
import club.abelix.api.server.lobby.impl.hub.present.storage.PresentStorage;
import club.abelix.core.bukkit.HeartBeat;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.FireworkMeta;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PresentListener implements Listener {

    static final FireworkEffect FIREWORK_EFFECT = FireworkEffect.builder()
            .withFlicker()
            .withColor(Color.WHITE)
            .with(FireworkEffect.Type.BALL)
            .build();

    PresentStorage presentStorage;

    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        val clickedBlock = event.getClickedBlock();

        if (clickedBlock == null)
            return;

        val location = clickedBlock.getLocation();
        val present = presentStorage.findPresentByLocation(location);

        if (present == null)
            return;

        val player = event.getPlayer();
        val gamer = Gamer.getGamer(player);
        val session = PresentGamerSession.get(gamer);

        if (session.isPresentFound(present)) {
            gamer.sendLocalizedMessage("already_found_present");
            return;
        }

        session.addFoundPresent(present);
        shootFireWork(location);

        if (session.getFoundPresentsCount() == presentStorage.count()) {
            handleAllPresentsFound(session, gamer);
        } else {
            handleSinglePresentFound(session, gamer);
        }
    }

    private void handleAllPresentsFound(PresentGamerSession session, Gamer gamer) {
        int experience = 1000;
        int total = presentStorage.count();

        gamer.addExperience(experience);
        gamer.sendLocalizedTitle(20,60, 20, "all_present_found_title",
                "all_present_found_subtitle");

        sendPresentFoundMessage(session, "all_present_found", experience, total);
    }

    private void handleSinglePresentFound(PresentGamerSession session, Gamer gamer) {
        int experience = 500;
        int total = presentStorage.count();

        gamer.addExperience(experience);
        gamer.sendActionBar("§b+500 XP");

        sendPresentFoundMessage(session, "present_found", experience, total);
    }

    private void sendPresentFoundMessage(
            PresentGamerSession session, String key,
            int experience, int total
    ) {
        session.getGamer().sendLocalizedMessage(key,
                "%total%", total, "%experience%", experience,
                "%found%", session.getFoundPresentsCount());
    }

    private void shootFireWork(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);

        FireworkMeta fireworkEffectMeta = firework.getFireworkMeta();
        fireworkEffectMeta.addEffect(FIREWORK_EFFECT);

        fireworkEffectMeta.setPower(0);
        firework.setFireworkMeta(fireworkEffectMeta);
        HeartBeat.runLaterSync(firework::detonate, 5L);
    }
}
