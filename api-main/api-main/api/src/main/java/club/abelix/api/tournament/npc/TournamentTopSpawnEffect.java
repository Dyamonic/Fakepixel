package club.abelix.api.tournament.npc;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.particle.ParticleEffect;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.nms.sound.type.YSound;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Магический эффект появления топа турнира.
 *
 * Фазы:
 *  1. Двойная спираль EnchantmentTable + цветное SPELL_MOB (золото / пурпур)
 *     + портальный вихрь в центре → поднимается вверх за ~2 секунды.
 *  2. Финальный взрыв: FIREWORKS_SPARK + CRIT_MAGIC + SPELL_WITCH.
 */
final class TournamentTopSpawnEffect {

    /** Тиков на фазу спирали (40 тиков = 2 с при 20 TPS). */
    private static final int SPIRAL_TICKS = 40;
    /** Высота спирали в блоках. */
    private static final double HEIGHT = 2.8;
    /** Радиус спирали в блоках. */
    private static final double RADIUS = 0.75;
    /** Угол поворота спирали за один тик (градусы). 20° * 40 тиков = 800° ≈ 2.2 оборота. */
    private static final double ANGLE_STEP_DEG = 20.0;

    // Цвета SPELL_MOB — золотисто-жёлтый и тёмно-фиолетовый
    private static final ParticleEffect.OrdinaryColor GOLD   = new ParticleEffect.OrdinaryColor(255, 210, 40);
    private static final ParticleEffect.OrdinaryColor PURPLE = new ParticleEffect.OrdinaryColor(170, 40,  255);

    private final Location base;

    TournamentTopSpawnEffect(Location base) {
        this.base = base.clone();
    }

    void play() {
        final var soundApi = FalconAPI.getNmsApi().getSoundApi();

        // Портальный звук — турнир активируется
        soundApi.playWorldSound(base, YSound.PORTAL_TRIGGER);

        final World world = base.getWorld();
        final double bx = base.getX();
        final double by = base.getY();
        final double bz = base.getZ();

        new AbstractTask(FalconAPI.getInstance()) {
            int tick = 0;

            @Override
            protected void run() {
                if (tick >= SPIRAL_TICKS) {
                    // ── Финальный взрыв ────────────────────────────────────────────────
                    Location mid = new Location(world, bx, by + HEIGHT * 0.5, bz);

                    // Широкий фейерверковый взрыв
                    ParticleEffect.FIREWORKS_SPARK.display(0.65f, 1.0f, 0.65f, 0.18f, 90, mid);
                    // Магические искры
                    ParticleEffect.CRIT_MAGIC.display(0.5f, 0.75f, 0.5f, 0.12f, 55, mid);
                    // Ведьминское свечение
                    ParticleEffect.SPELL_WITCH.display(0.4f, 0.6f, 0.4f, 0.1f, 30, mid);

                    soundApi.playWorldSound(base, YSound.LEVEL_UP);
                    cancel();
                    return;
                }

                // ── Спираль ────────────────────────────────────────────────────────────
                double progress = (double) tick / SPIRAL_TICKS;
                double y        = by + progress * HEIGHT;
                double angle    = Math.toRadians(tick * ANGLE_STEP_DEG);

                double cosA = Math.cos(angle);
                double sinA = Math.sin(angle);

                // Две нити спирали — диаметрально противоположные точки
                Location loc1 = new Location(world, bx + cosA * RADIUS, y, bz + sinA * RADIUS);
                Location loc2 = new Location(world, bx - cosA * RADIUS, y, bz - sinA * RADIUS);

                // Руны чародейского стола — основа спирали
                ParticleEffect.ENCHANTMENT_TABLE.display(0, 0.35f, 0, 0.06f, 3, loc1);
                ParticleEffect.ENCHANTMENT_TABLE.display(0, 0.35f, 0, 0.06f, 3, loc2);

                // Цветное магическое свечение: золото на нити 1, пурпур на нити 2
                ParticleEffect.SPELL_MOB.display(GOLD,   loc1, 48.0);
                ParticleEffect.SPELL_MOB.display(PURPLE, loc2, 48.0);

                // Портальный вихрь в центральной оси
                Location center = new Location(world, bx, y, bz);
                ParticleEffect.PORTAL.display(0, 0.15f, 0, 0.45f, 5, center);

                // Лёгкое заклинательное свечение — раз в 4 тика, чтобы не перегружать
                if (tick % 4 == 0) {
                    ParticleEffect.SPELL.display(0.3f, 0.2f, 0.3f, 0.0f, 4, center);
                }

                tick++;
            }
        }.timer(1, 1); // каждый тик
    }
}
