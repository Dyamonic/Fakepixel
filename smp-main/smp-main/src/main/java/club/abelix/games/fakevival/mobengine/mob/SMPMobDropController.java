package club.abelix.games.fakevival.mobengine.mob;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SMPMobDropController {
    public List<ItemStack> createDrops(SMPMob mob) {
        if (mob == null) return Collections.emptyList();
        if (mob.hasCustomDrops()) return new ArrayList<>(mob.getCustomDrops());
        if (mob.hasExternalDropController()) return Collections.emptyList();
        if (mob.isDebugMob()) return createDebugDrops(mob);
        return Collections.emptyList();
    }

    private List<ItemStack> createDebugDrops(SMPMob mob) {
        List<ItemStack> drops = new ArrayList<>();
        String id = mob.getMobId() == null ? "" : mob.getMobId().toLowerCase(java.util.Locale.ROOT);
        if (id.contains("skeleton")) {
            drops.add(new ItemStack(Material.BONE, 2));
        } else if (id.contains("spider")) {
            drops.add(new ItemStack(Material.STRING, 2));
        } else {
            drops.add(new ItemStack(Material.ROTTEN_FLESH, 2));
        }
        return drops;
    }
}
