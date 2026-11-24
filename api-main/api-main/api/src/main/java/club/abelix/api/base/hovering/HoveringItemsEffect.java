package club.abelix.api.base.hovering;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * More optimized version of floating item effect.
 * <p>
 * Made by ChatGPT
 *
 */
public class HoveringItemsEffect {
    private static final int ROT = 9;                 // шаг угла
    private static final double RADIUS = 0.7D;
    private static final double BASE_Y_OFFSET = -1D;
    private static final double RISE_PER_RUN = 0.08D; // подъём за один run()
    private static final double MAX_RISE = 2.0D;      // до какого Y поднимаем (от npcLocation)
    private static final int PERIOD_TICKS = 2;         // как у тебя
    private static final int SPAWN_EVERY_RUNS = 4;     // как у тебя (раз в 4 запуска)

    private static final double[] SIN = new double[360];
    private static final double[] COS = new double[360];

    static {
        for (int i = 0; i < 360; i++) {
            double r = Math.toRadians(i);
            SIN[i] = Math.sin(r);
            COS[i] = Math.cos(r);
        }
    }

    private final Location npcLocation;

    public HoveringItemsEffect(Location npcLocation) {
        this.npcLocation = npcLocation;
    }

    public void createEffect(
            Material material,
            Consumer<Player> action
    ) {
        final World world = npcLocation.getWorld();
        final double baseX = npcLocation.getX();
        final double baseY = npcLocation.getY();
        final double baseZ = npcLocation.getZ();

        final ItemStack headItem = ItemUtil.getBuilder(material).build();

        // Сколько одновременно “летит” предметов:
        // MAX_RISE / RISE_PER_RUN = ~25 run'ов жизни; спавним раз в 4 run => ~6-7 активных.
        // Берём с запасом 8.
        final int POOL_SIZE = 8;

        final FloatingItem[] pool = new FloatingItem[POOL_SIZE];
        for (int i = 0; i < POOL_SIZE; i++) {
            FloatingItem it = new FloatingItem();

            // Создаём armorstand один раз
            Location spawnLoc = new Location(world, baseX, baseY + BASE_Y_OFFSET, baseZ);
            FakeEntityArmorStand as = FalconAPI.getNmsApi().getFakeEntityApi().createArmorStand(spawnLoc);

            as.setSmall(true);
            as.setInvisible(true);
            as.setGravity(false); // ты сам двигаешь телепортом, гравитация не нужна
            as.setCustomNameVisible(false);
            as.setEquipment(EnumWrappers.ItemSlot.HEAD, headItem);
            as.setClickAction(action);

            // Сразу держим "выключенным" (не светим игрокам)
            as.setPublicStatus(false);

            it.armorStandWithItem = as;
            it.loc = spawnLoc; // будем переиспользовать этот Location (setX/Y/Z)
            it.active = false;

            pool[i] = it;
        }

        new AbstractTask(FalconAPI.getInstance()) {
            int globalAngle = 0;
            int runTick = 0;
            int nextIdx = 0;

            // опционально: если хочешь ещё меньше нагрузки — можно обновлять,
            // только когда рядом есть игроки. Проверять не каждый run, а раз в 20 тиков.
            boolean hasViewers = true;

            @Override
            public void run() {
                runTick++;

                // лёгкая “видимость”: раз в 20 тиков проверяем, есть ли игроки рядом.
                // Если нет — скрываем и не шлём телепорты.
                if ((runTick % 10) == 0) { // 10 запусков * 2 тика = раз в 20 тиков
                    hasViewers = anyPlayerNear(world.getPlayers(), baseX, baseY, baseZ, 32.0D);
                    if (!hasViewers) {
                        // спрячем активные и пропустим обновления
                        for (int i = 0; i < POOL_SIZE; i++) {
                            FloatingItem it = pool[i];
                            if (it.active) {
                                it.active = false;
                                it.armorStandWithItem.setPublicStatus(false);
                            }
                        }
                        return;
                    }
                }

                // обновляем активные
                for (int i = 0; i < POOL_SIZE; i++) {
                    FloatingItem it = pool[i];
                    if (!it.active) continue;

                    it.angle = (it.angle + ROT) % 360;
                    it.y += RISE_PER_RUN;

                    // вычисляем координаты без clone()
                    double x = baseX + SIN[it.angle] * RADIUS;
                    double z = baseZ + COS[it.angle] * RADIUS;
                    double y = (baseY + BASE_Y_OFFSET) + it.y;

                    it.loc.setX(x);
                    it.loc.setY(y);
                    it.loc.setZ(z);
                    it.armorStandWithItem.teleport(it.loc);

                    if (y > baseY + MAX_RISE) {
                        it.active = false;
                        it.armorStandWithItem.setPublicStatus(false); // вместо remove()
                    }
                }

                // угол для новых
                globalAngle -= ROT;
                if (globalAngle < 0) globalAngle += 360;

                // “спавн” раз в N запусков: просто реюзим следующий объект из пула
                if ((runTick % SPAWN_EVERY_RUNS) == 0 && hasViewers) {
                    FloatingItem it = pool[nextIdx];
                    nextIdx++;
                    if (nextIdx >= POOL_SIZE) nextIdx = 0;

                    it.y = 0.0D;
                    it.angle = globalAngle;

                    double x = baseX + SIN[it.angle] * RADIUS;
                    double z = baseZ + COS[it.angle] * RADIUS;
                    double y = baseY + BASE_Y_OFFSET;

                    it.loc.setX(x);
                    it.loc.setY(y);
                    it.loc.setZ(z);
                    it.armorStandWithItem.teleport(it.loc);

                    it.active = true;
                    it.armorStandWithItem.setPublicStatus(true);
                }
            }
        }.timer(2, PERIOD_TICKS);
    }

    private static boolean anyPlayerNear(List<Player> players, double x, double y, double z, double radius) {
        double r2 = radius * radius;
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Location l = p.getLocation(); // да, это объект; но мы вызываем редко (раз в ~20 тиков)
            double dx = l.getX() - x;
            double dy = l.getY() - y;
            double dz = l.getZ() - z;
            if ((dx * dx + dy * dy + dz * dz) <= r2) return true;
        }
        return false;
    }

    protected static final class FloatingItem {
        int angle;
        double y;
        boolean active;
        FakeEntityArmorStand armorStandWithItem;
        Location loc; // переиспользуем
    }
}