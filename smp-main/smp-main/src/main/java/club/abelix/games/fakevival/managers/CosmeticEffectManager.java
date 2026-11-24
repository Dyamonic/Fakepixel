package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;

public final class CosmeticEffectManager implements Listener {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private BukkitTask task;

    public CosmeticEffectManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void start() {
        stop();
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("cosmetic-effects.enabled", true)) return;

        long interval = Math.max(1L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("cosmetic-effects.update-interval-ticks", 10L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                spawnSelectedEffect(player);
            }
        }, 20L, interval);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    private void spawnSelectedEffect(Player player) {
        String selected = selectedCosmetic(player);
        if (selected.isBlank()) return;

        Location base = player.getLocation().add(0.0D, 0.15D, 0.0D);
        World world = player.getWorld();
        switch (selected) {
            case "ember_trail", "crimson_flare" -> world.spawnParticle(Particle.FLAME, base, 5, 0.22D, 0.04D, 0.22D, 0.01D);
            case "aqua_aura", "prismarine_wake" -> world.spawnParticle(Particle.BUBBLE, base, 7, 0.28D, 0.08D, 0.28D, 0.02D);
            case "royal_glow", "honey_spark" -> world.spawnParticle(Particle.DUST, base, 6, 0.28D, 0.08D, 0.28D, new Particle.DustOptions(Color.fromRGB(255, 190, 40), 1.15F));
            case "shadow_mark", "void_pulse" -> world.spawnParticle(Particle.PORTAL, base, 10, 0.25D, 0.12D, 0.25D, 0.02D);
            case "frost_steps" -> world.spawnParticle(Particle.SNOWFLAKE, base, 6, 0.25D, 0.05D, 0.25D, 0.01D);
            case "blossom_burst" -> world.spawnParticle(Particle.CHERRY_LEAVES, base.add(0.0D, 0.55D, 0.0D), 4, 0.32D, 0.16D, 0.32D, 0.01D);
            case "storm_ring" -> spawnRing(player.getLocation().add(0.0D, 0.12D, 0.0D), Particle.ELECTRIC_SPARK);
            case "slime_splash" -> world.spawnParticle(Particle.ITEM_SLIME, base, 5, 0.24D, 0.05D, 0.24D, 0.01D);
            case "amethyst_chime" -> world.spawnParticle(Particle.DUST, base.add(0.0D, 0.45D, 0.0D), 5, 0.25D, 0.15D, 0.25D, new Particle.DustOptions(Color.fromRGB(178, 102, 255), 1.0F));
            case "starlight_dust" -> world.spawnParticle(Particle.END_ROD, base.add(0.0D, 0.5D, 0.0D), 4, 0.3D, 0.18D, 0.3D, 0.01D);
            default -> world.spawnParticle(Particle.ENCHANT, base.add(0.0D, 0.4D, 0.0D), 6, 0.25D, 0.12D, 0.25D, 0.01D);
        }
    }

    private void spawnRing(Location center, Particle particle) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < 10; i++) {
            double angle = (Math.PI * 2.0D / 10.0D) * i;
            Location point = center.clone().add(Math.cos(angle) * 0.55D, 0.0D, Math.sin(angle) * 0.55D);
            world.spawnParticle(particle, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private String selectedCosmetic(Player player) {
        String path = "players." + player.getUniqueId() + ".store.selected-cosmetic";
        return normalize(dataManager.getData().getString(path, ""));
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }
}
