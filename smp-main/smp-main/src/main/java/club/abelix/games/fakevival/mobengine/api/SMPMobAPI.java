package club.abelix.games.fakevival.mobengine.api;

import club.abelix.games.fakevival.mobengine.mob.SMPMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public interface SMPMobAPI {
    boolean isSMPMob(Entity entity);
    Optional<SMPMob> getSMPMob(Entity entity);
    void spawnSMPMob(String mobId, Location location);
    LivingEntity spawnMob(String mobId, Location location, EntityType type, String displayName, double maxMmoHealth, double attackDamage, int level, java.util.List<ItemStack> drops);
    void removeSMPMob(UUID uuid);
}
