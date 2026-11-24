package club.abelix.api.base.util.nms.death;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.event.death.PostPlayerDeathEvent;
import club.abelix.api.base.event.death.PrePlayerDeathEvent;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;

import static org.bukkit.Material.AIR;

@UtilityClass
public class DeathUtil {

    public void createDeathParticles(Entity entity) {
        FalconAPI.getNmsApi().getWorldApi().spawnDeathParticles(entity);
    }

    private void drop(@NonNull Player player, @Nullable ItemStack item) {
        if (item != null && item.getType() != AIR) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    public void simulateDeath(@NonNull Player player, @Nullable Player killer, boolean dropItems) {
        EntityDamageEvent event = player.getLastDamageCause();

        Bukkit.getPluginManager().callEvent(new PrePlayerDeathEvent(player, event == null ? null : event.getCause()));

        // спавним партиклы смерти
        createDeathParticles(player);

        // ресетим хп
        player.setMaxHealth(20);
        player.setHealth(20);

        // ресетим еду
        player.setExhaustion(0);
        player.setSaturation(0);
        player.setFoodLevel(20);

        // ресетим огонь
        player.setFireTicks(0);

        // ресетим падение
        player.setFallDistance(0);

        // ресетим полёт
        player.setFlying(false);

        // ресетим скорость
        player.setVelocity(new Vector(0, 0, 0));

        // закрываем инвентарь
        player.closeInventory();

        // ресетим эффекты зелий
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // если нужно дропаем предметы
        if (dropItems) {
            PlayerInventory inventory = player.getInventory();

            // дропаем браню
            for (ItemStack armor : inventory.getArmorContents()) {
                drop(player, armor);
            }

            // очищаем броню игрока
            inventory.setArmorContents(new ItemStack[4]);

            // дропаем инвентарь
            for (ItemStack item : inventory.getContents()) {
                drop(player, item);
            }

            // очищаем инвентарь
            inventory.clear();

            // спавним опыт
            ExperienceOrb orb = player.getWorld().spawn(player.getLocation(), ExperienceOrb.class);
            orb.setExperience(player.getTotalExperience() / 4);

            // убираем опыт
            player.setLevel(0);
            player.setExp(0);
            player.setTotalExperience(0);
        }

        if (killer != null) {
            FalconAPI.getNmsApi().getPlayerApi().setKiller(player, killer);
        }

        Bukkit.getPluginManager().callEvent(new PostPlayerDeathEvent(player, event == null ? null : event.getCause()));

        player.setLastDamageCause(null);
        player.setLastDamage(0);
    }

}
