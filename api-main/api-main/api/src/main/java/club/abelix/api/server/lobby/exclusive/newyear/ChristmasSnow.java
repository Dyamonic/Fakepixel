package club.abelix.api.server.lobby.exclusive.newyear;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.particle.ParticleEffect;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.base.util.task.registration.TaskRegistrationProvider;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class ChristmasSnow extends AbstractTask {

    private static final String PROPERTY_NAME = "Exclusive/Snow";
    private static final int RADIUS = 32;

    public ChristmasSnow(@NonNull TaskRegistrationProvider provider) {
        super(provider);

       FalconAPI.getInstance().getCommandManager().addCommand(SnowCommand.class);
    }

    private static boolean isSnowDisabled(final Gamer gamer) {
        return gamer.getProperty(PROPERTY_NAME) == 1;
    }


    @Override
    public void run() {
        val random = ThreadLocalRandom.current();

        for (val player : Bukkit.getOnlinePlayers()) {
            val gamer = Gamer.getGamer(player);

            if (isSnowDisabled(gamer)) {
                continue;
            }

            val world = player.getWorld();
            val location = player.getLocation();

            val y = location.getBlockY();

            for (int i = 0; i < 300; i++) {
                val xAdditive = (random.nextFloat() - 0.5f) * RADIUS * 2.0f;
                val max = (float) Math.sqrt(RADIUS * RADIUS - xAdditive * xAdditive) * 2.0f;
                val yAdditive = (random.nextFloat() - 0.5f) * max;
                val zAdditive = (random.nextFloat() - 0.5f) * max;

                val effectLocation = new Location(
                        world,
                        location.getX() + xAdditive,
                        y + yAdditive,
                        location.getZ() + zAdditive
                );

                // Снег должен идти только если игрок находится не в помещении,
                // то есть если в радиусе 20 блоков по Y над локацией эффекта
                // есть блок, то просто ничего не делаем на этой локации

                boolean found = false;

                for (int newY = y; newY < y + 20; newY++) {
                    val block = world.getBlockAt(
                            effectLocation.getBlockX(),
                            newY,
                            effectLocation.getBlockZ()
                    );

                    if (block.getType() != Material.AIR) {
                        found = true;
                        break;
                    }
                }

                if (found) {
                    continue;
                }

                ParticleEffect.FIREWORKS_SPARK.display(effectLocation.getDirection(),
                        0.002f, effectLocation, Collections.singletonList(player)
                );
            }
        }
    }

}
