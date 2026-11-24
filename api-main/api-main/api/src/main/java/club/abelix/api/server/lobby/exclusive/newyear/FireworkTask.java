package club.abelix.api.server.lobby.exclusive.newyear;

import club.abelix.api.server.lobby.manager.LobbyManager;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class FireworkTask extends BukkitRunnable {

    private static final FireworkEffect EFFECT = FireworkEffect.builder()
            .trail(true)
            .withColor(org.bukkit.Color.AQUA, org.bukkit.Color.WHITE)
            .withFade(org.bukkit.Color.BLUE, org.bukkit.Color.PURPLE)
            .with(FireworkEffect.Type.BALL_LARGE)
            .build();

    private static final int MAX_LAUNCHES = 35;
    private static final double RADIUS = 10.0;
    private static final double ANGLE_INCREMENT = Math.toRadians(360 / (2 * Math.PI * RADIUS / 3));

    private int launchCount = 0;
    private final Location spawnLocation = LobbyManager.INSTANCE.getLobby().getSpawnLocation();

    @Override
    public void run() {
        if (launchCount >= MAX_LAUNCHES) {
            this.cancel();
            return;
        }

        World world = spawnLocation.getWorld();
        if (world == null) return;

        // Запускаем фейерверки по кругу
        for (double angle = 0; angle < 2 * Math.PI; angle += ANGLE_INCREMENT) {
            double x = spawnLocation.getX() + RADIUS * Math.cos(angle);
            double z = spawnLocation.getZ() + RADIUS * Math.sin(angle);
            Location fireworkLocation = new Location(world, x, spawnLocation.getY(), z);

            Firework firework = world.spawn(fireworkLocation, Firework.class);

            FireworkMeta fireworkEffectMeta = firework.getFireworkMeta();
            fireworkEffectMeta.addEffect(EFFECT);
            fireworkEffectMeta.setPower(2);

            firework.setFireworkMeta(fireworkEffectMeta);
        }

        launchCount++;
    }
}