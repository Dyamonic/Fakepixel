package club.abelix.api.server.lobby.exclusive.newyear;

import club.abelix.api.server.lobby.manager.LobbyManager;
import club.abelix.network.common.logging.Logging;
import lombok.val;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireworkScheduler {

    private final JavaPlugin plugin;

    public FireworkScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void scheduleFireworkTask() {
        val mskZone = ZoneId.of("Europe/Moscow");

        val targetTime = ZonedDateTime.of(2026, Month.JANUARY.getValue(), 1,
                0, 0, 0, 0, mskZone);

        val now = Instant.now();
        val targetInstant = targetTime.toInstant();

        if (!targetInstant.isAfter(now)) {
            Logging.IMP.warn("The target time is in the past. Firework task will not be scheduled. target=" + targetTime);
            return;
        }

        long delayTicks = calculateDelayInTicks(targetTime);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            startShow(LobbyManager.INSTANCE.getLobby().getSpawnLocation().clone().add(0, 10, 0));
        }, delayTicks);

        Logging.IMP.info("Scheduled firework task to run at " + targetTime
                + " (serverNow=" + ZonedDateTime.now(ZoneId.systemDefault()) + ")");
    }

    private long calculateDelayInTicks(ZonedDateTime targetTime) {
        long delayMillis = Duration.between(Instant.now(), targetTime.toInstant()).toMillis();

        if (delayMillis < 0) {
            throw new IllegalArgumentException("Target time must be in the future.");
        }

        return delayMillis / 50L;
    }

    public void startShow(Location textAnchor) {
        World w = textAnchor.getWorld();

        final double radius = 28.0;     // увеличенный радиус
        final int ringPoints = 12;      // сколько точек по окружности
        final double launchY = -10.0;   // относительно anchor, откуда стартуем

        List<Location> launchPads = buildRing(textAnchor.clone().add(0, launchY, 0), radius, ringPoints);

        // Запускаем залпы по кольцу 20 секунд
        new BukkitRunnable() {
            int iterations = 0;

            @Override
            public void run() {
                if (iterations >= 20) {
                    cancel();
                    return;
                }

                // каждый тик шоу можем запускать не все точки, а часть (иначе будет много фейерверков)
                // например, половину точек в шахматном порядке
                for (int i = 0; i < launchPads.size(); i++) {
                    if ((i + iterations) % 2 == 0) {
                        spawnPrettyFirework(launchPads.get(i));
                    }
                }
                iterations++;
            }
        }.runTaskTimer(plugin, 0L, 40L);

        // Один центральный "акцент"
        Firework fw = w.spawn(textAnchor.clone().subtract(0, 10, 0), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder()
                .trail(true)
                .withColor(Color.RED, Color.WHITE)
                .withFade(Color.PURPLE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .build();
        meta.addEffect(effect);
        meta.setPower(1);
        fw.setFireworkMeta(meta);

        // Через 2.5 сек — большой жирный текст на 10 сек
        new BukkitRunnable() {
            @Override
            public void run() {
                drawTextForSeconds("2026", textAnchor, 10.0);
            }
        }.runTaskLater(plugin, 50L);
    }

    private void spawnPrettyFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        FireworkEffect effect = FireworkEffect.builder()
                .trail(true)
                .withColor(org.bukkit.Color.AQUA, org.bukkit.Color.WHITE)
                .withFade(org.bukkit.Color.BLUE, org.bukkit.Color.PURPLE)
                .with(FireworkEffect.Type.BURST)
                .build();

        meta.addEffect(effect);
        meta.setPower(2); // высота
        fw.setFireworkMeta(meta);

//         Bukkit.getScheduler().runTaskLater(this, fw::detonate, 30L);
    }

    private List<Location> buildRing(Location center, double radius, int points) {
        List<Location> out = new ArrayList<>(points);
        double cx = center.getX();
        double cy = center.getY();
        double cz = center.getZ();

        for (int i = 0; i < points; i++) {
            double a = 2.0 * Math.PI * i / points;
            double x = cx + radius * Math.cos(a);
            double z = cz + radius * Math.sin(a);
            out.add(new Location(center.getWorld(), x, cy, z));
        }
        return out;
    }

    private void drawTextForSeconds(String text, Location anchor, double seconds) {
        final int ticksTotal = (int) Math.round(seconds * 20.0);

        final double scale = 0.70;     // было 0.70
        final double spacing = 0.75;   // было 0.65 (чуть больше — меньше плотность)
        final int fat = 1;             // было 1 (это ключ к снижению лагов)
        final int depthLayers = 3;     // было 6
        final double depthStep = 0.24; // можно чуть больше, чтобы "толщина" визуально осталась

        Map<Character, String[]> font = font5x7();
        List<Location> points = buildTextPoints(text, anchor, font, scale, spacing, fat, depthLayers, depthStep);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t++ > ticksTotal) {
                    cancel();
                    return;
                }

                for (Player pl : anchor.getWorld().getPlayers()) {
                    if (pl.getLocation().distanceSquared(anchor) > 120 * 120) continue; // расширим видимость

                    for (Location pt : points) {
                        // чуть более "золотой" цвет
                        sendColoredRedstone(pl, pt, 255, 200, 40);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }


    private List<Location> buildTextPoints(String text, Location anchor, Map<Character, String[]> font,
                                           double scale, double spacing, int fat,
                                           int depthLayers, double depthStep) {

        List<Location> out = new ArrayList<>();

        int charW = 5, charH = 7;

        // ширина текста для центрирования (учитываем spacing)
        double totalW = 0;
        for (char c : text.toCharArray()) totalW += (charW + spacing);

        double startX = anchor.getX() - (totalW * scale) / 2.0;
        double startY = anchor.getY() + (charH * scale) / 2.0;
        double baseZ = anchor.getZ();

        double cursorX = startX;

        for (char c : text.toCharArray()) {
            String[] glyph = font.getOrDefault(c, blank5x7());

            for (int row = 0; row < glyph.length; row++) {
                String line = glyph[row];

                for (int col = 0; col < line.length(); col++) {
                    if (line.charAt(col) != '#') continue;

                    double x0 = cursorX + (col * scale);
                    double y0 = startY - (row * scale);

                    // 1) "жирность" в плоскости: добавляем точки вокруг (квадрат (2*fat+1)^2)
                    for (int dx = -fat; dx <= fat; dx++) {
                        for (int dy = -fat; dy <= fat; dy++) {

                            double x = x0 + dx * (scale * 0.35);
                            double y = y0 + dy * (scale * 0.35);

                            // 2) глубина по Z: несколько слоёв
                            for (int k = 0; k < depthLayers; k++) {
                                double z = baseZ + k * depthStep;
                                out.add(new Location(anchor.getWorld(), x, y, z));
                            }
                        }
                    }
                }
            }

            cursorX += (charW + spacing) * scale;
        }

        return out;
    }

    private void sendColoredRedstone(Player pl, Location loc, int r, int g, int b) {
        // В 1.8 цвет задаётся через offsetX/Y/Z, а count=0.
        float rf = Math.max(0f, Math.min(1f, r / 255f));
        float gf = Math.max(0f, Math.min(1f, g / 255f));
        float bf = Math.max(0f, Math.min(1f, b / 255f));

        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
                EnumParticle.REDSTONE,
                true,
                (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(),
                rf, gf, bf,
                1.0f,   // speed
                0       // count (0 = особый режим для REDSTONE в 1.8)
        );

        ((CraftPlayer) pl).getHandle().playerConnection.sendPacket(packet);
    }

    private Map<Character, String[]> font5x7() {
        Map<Character, String[]> m = new HashMap<>();
        m.put('0', new String[]{
                ".###.",
                "#...#",
                "#...#",
                "#...#",
                "#...#",
                "#...#",
                ".###."
        });
        m.put('2', new String[]{
                ".###.",
                "#...#",
                "....#",
                "...#.",
                "..#..",
                ".#...",
                "#####"
        });
        m.put('6', new String[]{
                ".###.",
                "#...#",
                "#....",
                "####.",
                "#...#",
                "#...#",
                ".###."
        });
        return m;
    }

    private String[] blank5x7() {
        return new String[]{
                ".....",
                ".....",
        };
    }
}