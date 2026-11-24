package club.abelix.games.fakevival.abilities;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatType;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WeaponAbilityExecutor {
    private static final Map<UUID, ProjectileCast> PROJECTILES = new HashMap<>();
    private static final Map<UUID, Double> PROJECTILE_FORCE = new HashMap<>();
    private static final Map<UUID, TimedTarget> ASSASSIN_MARKS = new HashMap<>();
    private static final Map<UUID, TimedTarget> HUNTER_MARKS = new HashMap<>();
    private static final Map<UUID, Long> SNIPER_FOCUS = new HashMap<>();
    private static final Map<UUID, Long> SILENT_KILLER = new HashMap<>();
    private static final ThreadLocal<Boolean> APPLYING_DAMAGE = ThreadLocal.withInitial(() -> false);

    private WeaponAbilityExecutor() {}

    static boolean execute(WeaponAbilityDefinition ability, AbilityContext c) {
        double damage = scaledDamage(ability, c);
        return switch (ability.id()) {
            case "ember_slash" -> cone(c, ability, damage, Particle.FLAME, target -> target.setFireTicks(Math.max(target.getFireTicks(), 60)));
            case "frostbite_strike" -> single(c, ability, damage, target -> slow(target, 60, 2), Particle.SNOWFLAKE);
            case "guardian_cut" -> {
                boolean hit = area(c, c.player().getLocation(), ability, damage, Particle.CRIT, null);
                buff(c, 5, Map.of(MMOStatType.DEFENSE, 25.0D));
                yield hit || true;
            }
            case "void_rend" -> single(c, ability, damage, target -> {
                if (isBoss(c, target)) safeDamage(c, target, damage * 0.25D);
            }, Particle.REVERSE_PORTAL);
            case "solar_cleave" -> area(c, c.player().getLocation(), ability, damage, Particle.END_ROD, target -> {
                if (isUndead(target)) target.setFireTicks(Math.max(target.getFireTicks(), 100));
            });
            case "earthsplit" -> line(c, ability, damage, Particle.DUST_PLUME, null);
            case "lifesteal_cut" -> heal(c, Math.max(1.0D, c.damage() * 0.05D));
            case "royal_execution" -> single(c, ability, damage, target -> {
                if (healthPercent(target) < 0.30D) safeDamage(c, target, damage * 0.6D);
            }, Particle.CRIT);
            case "blazing_arc" -> cone(c, ability, damage, Particle.FLAME, target -> target.setFireTicks(Math.max(target.getFireTicks(), 80)));
            case "iron_will" -> {
                buff(c, 8, Map.of(MMOStatType.DEFENSE, 25.0D));
                c.player().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 0, true, true));
                temporaryKnockbackResistance(c, 0.5D, 160L);
                yield feedback(c, Particle.CRIT, Sound.ITEM_SHIELD_BLOCK);
            }

            case "sakura_slice" -> cone(c, ability, damage, Particle.CHERRY_LEAVES, null);
            case "stormcut" -> stormcut(c, ability, damage);
            case "oni_rage" -> {
                buff(c, 8, Map.of(MMOStatType.STRENGTH, 30.0D, MMOStatType.CRIT_DAMAGE, 25.0D));
                yield feedback(c, Particle.ANGRY_VILLAGER, Sound.ENTITY_RAVAGER_ROAR);
            }
            case "moonlight_cut" -> line(c, ability, damage, Particle.END_ROD, null);
            case "bamboo_sweep" -> area(c, c.player().getLocation(), ability, damage, Particle.CLOUD, target -> knock(target, c.player().getLocation(), 0.8D, 0.2D));
            case "phantom_draw" -> dashStrike(c, ability, damage, true);
            case "crimson_counter" -> c.target() != null && Math.random() <= 0.30D && direct(c, c.target(), damage, Particle.DAMAGE_INDICATOR);
            case "wind_blade" -> line(c, ability, damage, Particle.CLOUD, null);
            case "spirit_edge" -> spiritEdge(c, ability, damage);
            case "dragon_cut" -> line(c, ability, damage, Particle.DRAGON_BREATH, null);

            case "shadow_sting" -> shadowSting(c, ability, damage);
            case "venom_cut" -> c.target() != null && effect(c.target(), PotionEffectType.POISON, 60, 0);
            case "piercing_fang" -> single(c, ability, damage * 1.2D, null, Particle.CRIT);
            case "blood_needle" -> bloodNeedle(c, ability, damage);
            case "nightfall_dash" -> {
                safeDash(c.player(), ability.range());
                c.player().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, true, false));
                yield feedback(c, Particle.SMOKE, Sound.ENTITY_ENDERMAN_TELEPORT);
            }
            case "assassin_mark" -> mark(c, ability, ASSASSIN_MARKS, Particle.CRIT);
            case "quickstep" -> {
                safeDash(c.player(), ability.range());
                yield feedback(c, Particle.CLOUD, Sound.ENTITY_BREEZE_JUMP);
            }
            case "backstab" -> backstab(c, damage);
            case "smoke_veil" -> smokeVeil(c, ability, damage);
            case "silent_killer" -> consumeSilentKiller(c, damage);

            case "windpierce", "ember_arrow", "frozen_arrow", "voidshot" -> prepareArrow(c, ability, damage);
            case "royal_bolt" -> c.target() != null && direct(c, c.target(), isDungeonMob(c, c.target()) ? damage * 1.35D : damage, Particle.CRIT);
            case "barrage" -> barrage(c, ability, damage);
            case "sniper_focus" -> {
                SNIPER_FOCUS.put(c.player().getUniqueId(), System.currentTimeMillis() + 20_000L);
                yield feedback(c, Particle.CRIT, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
            }
            case "chain_arrow" -> chain(c, ability, damage);
            case "explosive_tip" -> area(c, impact(c), ability, damage, Particle.EXPLOSION, null);
            case "hunter_mark" -> mark(c, ability, HUNTER_MARKS, Particle.HAPPY_VILLAGER);

            case "spark_bolt" -> magicProjectile(c, ability, damage, "spark", Particle.ELECTRIC_SPARK);
            case "frost_bolt" -> magicProjectile(c, ability, damage, "frost", Particle.SNOWFLAKE);
            case "toxic_burst" -> areaAtTarget(c, ability, damage, Particle.WITCH, target -> effect(target, PotionEffectType.POISON, 80, 0));
            case "void_pulse" -> areaAtTarget(c, ability, damage, Particle.REVERSE_PORTAL, null);
            case "sunbeam" -> line(c, ability, damage, Particle.END_ROD, target -> {
                if (isUndead(target)) safeDamage(c, target, damage * 0.35D);
            });
            case "arcane_shield" -> {
                buff(c, 6, Map.of(MMOStatType.DEFENSE, 35.0D));
                c.player().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120, 1, true, true));
                yield feedback(c, Particle.ENCHANT, Sound.ITEM_SHIELD_BLOCK);
            }
            case "mana_drain" -> manaDrain(c, ability, damage);
            case "rift_blink" -> blink(c, ability.range());
            case "crystal_spikes" -> areaAtTarget(c, ability, damage, Particle.ENCHANTED_HIT, null);
            case "healing_spark" -> healingSpark(c, ability);

            case "magic_missile" -> magicMissile(c, ability, damage);
            case "stormcall" -> stormcall(c, ability, damage);
            case "necrotic_wave" -> line(c, ability, damage, Particle.SOUL_FIRE_FLAME, target -> effect(target, PotionEffectType.WEAKNESS, 100, 0));
            case "crystal_beam" -> line(c, ability, damage, Particle.ENCHANTED_HIT, null);
            case "infernal_nova" -> area(c, c.player().getLocation(), ability, damage, Particle.FLAME, target -> target.setFireTicks(Math.max(target.getFireTicks(), 100)));
            case "earthen_wall" -> earthenWall(c, ability, damage);
            case "aqua_surge" -> line(c, ability, damage, Particle.SPLASH, target -> slow(target, 60, 1));
            case "starfall" -> starfall(c, ability, damage);
            case "gravity_well" -> gravityWell(c, ability, damage);
            case "celestial_blessing" -> {
                buff(c, 10, Map.of(MMOStatType.HEALTH, 40.0D, MMOStatType.DEFENSE, 35.0D, MMOStatType.MANA_REGEN, 20.0D));
                yield feedback(c, Particle.END_ROD, Sound.BLOCK_BEACON_ACTIVATE);
            }
            default -> false;
        };
    }

    public static void handleProjectileImpact(Game game, Player player, Projectile projectile, LivingEntity target, Location location) {
        ProjectileCast cast = PROJECTILES.remove(projectile.getUniqueId());
        Double storedForce = PROJECTILE_FORCE.remove(projectile.getUniqueId());
        double force = storedForce == null ? 1.0D : storedForce;
        if (player == null || !player.isOnline()) return;
        AbilityContext c = new AbilityContext(game, player, player.getInventory().getItemInMainHand(), cast == null ? "" : cast.itemId(),
                AbilityTrigger.PROJECTILE_HIT, target, location, 0.0D, null, projectile);
        if (cast == null) {
            if (target == null || !enemy(c, target)) return;
            double bonus = 6.0D + game.getMmoStatService().getTotalStats(player).get(MMOStatType.CRIT_DAMAGE) * 0.08D;
            boolean focused = force >= 0.95D && active(SNIPER_FOCUS, player.getUniqueId());
            TimedTarget hunter = HUNTER_MARKS.get(player.getUniqueId());
            if (focused || hunter != null && hunter.valid(target.getUniqueId())) {
                direct(c, target, Math.min(80.0D, bonus * (focused ? 1.5D : 1.3D)), Particle.CRIT);
                if (focused) SNIPER_FOCUS.remove(player.getUniqueId());
            }
            return;
        }
        if (cast.mode().equals("frost") && target != null && enemy(c, target)) slow(target, 60, 2);
        if (cast.mode().equals("spark") || cast.mode().equals("frost")) {
            if (target != null && enemy(c, target)) direct(c, target, cast.damage(), cast.mode().equals("frost") ? Particle.SNOWFLAKE : Particle.ELECTRIC_SPARK);
            return;
        }
        if (target == null || !enemy(c, target)) return;
        double damage = cast.damage();
        if (force >= 0.95D && active(SNIPER_FOCUS, player.getUniqueId())) {
            damage *= 1.5D;
            SNIPER_FOCUS.remove(player.getUniqueId());
        }
        TimedTarget hunter = HUNTER_MARKS.get(player.getUniqueId());
        if (hunter != null && hunter.valid(target.getUniqueId())) damage *= 1.3D;
        direct(c, target, damage, Particle.CRIT);
        if (cast.mode().equals("ember_arrow")) {
            target.setFireTicks(Math.max(target.getFireTicks(), 100));
            areaSimple(c, target.getLocation(), 3.0D, 4, damage * 0.35D, nearby -> nearby.setFireTicks(Math.max(nearby.getFireTicks(), 60)));
        } else if (cast.mode().equals("frozen_arrow")) {
            slow(target, 80, 2);
            areaSimple(c, target.getLocation(), 3.0D, 4, damage * 0.25D, nearby -> slow(nearby, 60, 1));
        }
    }

    public static void applyPendingOnHit(Game game, Player player, LivingEntity target) {
        if (game == null || player == null || target == null) return;
        AbilityContext c = new AbilityContext(game, player, player.getInventory().getItemInMainHand(), "", AbilityTrigger.ON_HIT,
                target, target.getLocation(), 0.0D, null, null);
        TimedTarget mark = ASSASSIN_MARKS.get(player.getUniqueId());
        if (mark != null && mark.valid(target.getUniqueId())) {
            MMOStatMap stats = game.getMmoStatService().getTotalStats(player);
            ASSASSIN_MARKS.remove(player.getUniqueId());
            direct(c, target, Math.min(80.0D, 6.0D + stats.get(MMOStatType.STRENGTH) * 0.25D), Particle.CRIT);
        }
    }

    public static boolean isApplyingAbilityDamage() {
        return APPLYING_DAMAGE.get();
    }

    public static void markSilentKiller(Player player) {
        if (player != null) SILENT_KILLER.put(player.getUniqueId(), System.currentTimeMillis() + 30_000L);
    }

    public static void clearPlayerState(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        ASSASSIN_MARKS.remove(id);
        HUNTER_MARKS.remove(id);
        SNIPER_FOCUS.remove(id);
        SILENT_KILLER.remove(id);
        PROJECTILES.entrySet().removeIf(entry -> {
            if (!entry.getValue().owner().equals(id)) return false;
            PROJECTILE_FORCE.remove(entry.getKey());
            return true;
        });
    }

    public static void discardProjectile(Projectile projectile) {
        if (projectile != null) {
            PROJECTILES.remove(projectile.getUniqueId());
            PROJECTILE_FORCE.remove(projectile.getUniqueId());
        }
    }

    public static void recordBowForce(Projectile projectile, double force) {
        if (projectile != null) PROJECTILE_FORCE.put(projectile.getUniqueId(), Math.clamp(force, 0.0D, 1.0D));
    }

    private static double scaledDamage(WeaponAbilityDefinition a, AbilityContext c) {
        MMOStatMap stats = c.plugin().getMmoStatService().getTotalStats(c.player());
        double value;
        if (a.allowedItemTypes().stream().anyMatch(type -> type == club.abelix.games.fakevival.mmo.MMOItemType.WAND || type == club.abelix.games.fakevival.mmo.MMOItemType.STAFF)) {
            value = a.baseDamage() + stats.get(MMOStatType.ABILITY_DAMAGE) * 0.25D + stats.get(MMOStatType.MANA) * 0.10D;
        } else if (a.allowedItemTypes().stream().anyMatch(type -> type == club.abelix.games.fakevival.mmo.MMOItemType.BOW || type == club.abelix.games.fakevival.mmo.MMOItemType.CROSSBOW)) {
            value = a.baseDamage() + stats.get(MMOStatType.DAMAGE) * 0.35D + stats.get(MMOStatType.CRIT_DAMAGE) * 0.08D;
        } else {
            value = a.baseDamage() + stats.get(MMOStatType.DAMAGE) * 0.35D + stats.get(MMOStatType.STRENGTH) * 0.20D;
        }
        if (a.baseDamage() > 0.0D && Math.random() * 100.0D < Math.clamp(stats.get(MMOStatType.CRIT_CHANCE), 0.0D, 100.0D)) {
            value *= 1.0D + Math.clamp(stats.get(MMOStatType.CRIT_DAMAGE), 0.0D, 300.0D) / 100.0D;
        }
        value *= c.plugin().getCustomEnchantManager().abilityPowerMultiplier(c.item(), a.id(), false);
        return Math.clamp(value, Math.max(0.0D, a.baseDamage()), 250.0D);
    }

    private static boolean single(AbilityContext c, WeaponAbilityDefinition a, double damage, Effect effect, Particle particle) {
        LivingEntity target = target(c, a.range());
        if (target == null) return false;
        if (effect != null) effect.apply(target);
        return direct(c, target, damage, particle);
    }

    private static boolean cone(AbilityContext c, WeaponAbilityDefinition a, double damage, Particle particle, Effect effect) {
        Vector forward = c.player().getEyeLocation().getDirection().normalize();
        List<LivingEntity> targets = nearby(c, c.player().getLocation(), a.radius(), a.maxTargets()).stream()
                .filter(entity -> forward.dot(direction(c.player().getEyeLocation(), entity.getEyeLocation())) >= 0.45D).toList();
        return hitList(c, targets, damage, particle, effect);
    }

    private static boolean line(AbilityContext c, WeaponAbilityDefinition a, double damage, Particle particle, Effect effect) {
        Vector start = c.player().getEyeLocation().toVector();
        Vector forward = c.player().getEyeLocation().getDirection().normalize();
        List<LivingEntity> targets = nearby(c, c.player().getLocation(), a.range(), 32).stream().filter(entity -> {
            Vector relative = entity.getEyeLocation().toVector().subtract(start);
            double along = relative.dot(forward);
            if (along < 0.0D || along > a.range()) return false;
            return relative.subtract(forward.clone().multiply(along)).lengthSquared() <= a.radius() * a.radius();
        }).limit(a.maxTargets()).toList();
        for (int i = 1; i <= Math.min(8, (int) Math.ceil(a.range())); i += 2) particle(c.player().getEyeLocation().add(forward.clone().multiply(i)), particle, 2, 0.15D);
        return hitList(c, targets, damage, particle, effect);
    }

    private static boolean area(AbilityContext c, Location center, WeaponAbilityDefinition a, double damage, Particle particle, Effect effect) {
        List<LivingEntity> targets = nearby(c, center, Math.max(1.0D, a.radius()), a.maxTargets());
        particle(center, particle, Math.min(24, 4 + a.maxTargets() * 2), Math.max(0.5D, a.radius() * 0.35D));
        return hitList(c, targets, damage, particle, effect);
    }

    private static boolean areaAtTarget(AbilityContext c, WeaponAbilityDefinition a, double damage, Particle particle, Effect effect) {
        LivingEntity target = target(c, a.range());
        Location center = target == null ? targetLocation(c, a.range()) : target.getLocation();
        return area(c, center, a, damage, particle, effect);
    }

    private static boolean hitList(AbilityContext c, List<LivingEntity> targets, double damage, Particle particle, Effect effect) {
        for (LivingEntity target : targets) {
            if (effect != null) effect.apply(target);
            direct(c, target, damage, particle);
        }
        return !targets.isEmpty();
    }

    private static boolean direct(AbilityContext c, LivingEntity target, double damage, Particle particle) {
        if (!enemy(c, target)) return false;
        safeDamage(c, target, damage);
        particle(target.getLocation().add(0, Math.min(1.0D, target.getHeight() * 0.5D), 0), particle, 5, 0.25D);
        return true;
    }

    private static void safeDamage(AbilityContext c, LivingEntity target, double requested) {
        double damage = Math.max(0.0D, requested);
        if (isBoss(c, target) || target.getMaxHealth() > 200.0D) damage = Math.min(damage, Math.max(2.0D, target.getMaxHealth() * 0.05D));
        if (damage > 0.0D) {
            boolean previous = APPLYING_DAMAGE.get();
            APPLYING_DAMAGE.set(true);
            try {
                target.damage(damage, c.player());
            } finally {
                APPLYING_DAMAGE.set(previous);
            }
        }
    }

    private static boolean enemy(AbilityContext c, LivingEntity entity) {
        if (entity == null || !entity.isValid() || entity.isDead() || entity.equals(c.player())) return false;
        if (entity.hasMetadata("NPC") || entity.hasMetadata("npc") || entity.hasMetadata("furniture")) return false;
        if (entity.getPersistentDataContainer().has(new NamespacedKey(c.plugin(), "fakevival_npc"), PersistentDataType.STRING)) return false;
        if (entity instanceof Player player) {
            return c.plugin().getPveManager().canDamagePlayer(c.player(), player, EntityDamageEvent.DamageCause.MAGIC, c.item());
        }
        return entity instanceof Monster || c.plugin().getZoneManager().isZoneMob(entity)
                || !c.plugin().getDungeonManager().combatContext(entity).dungeonId().equals("none");
    }

    private static List<LivingEntity> nearby(AbilityContext c, Location center, double radius, int max) {
        World world = center == null ? null : center.getWorld();
        if (world == null) return List.of();
        return world.getNearbyLivingEntities(center, radius, entity -> enemy(c, entity)).stream()
                .sorted(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(center)))
                .limit(Math.clamp(max, 1, 32)).toList();
    }

    private static LivingEntity target(AbilityContext c, double range) {
        RayTraceResult result = c.player().getWorld().rayTraceEntities(c.player().getEyeLocation(), c.player().getEyeLocation().getDirection(),
                Math.max(1.0D, range), 0.45D, entity -> entity instanceof LivingEntity living && enemy(c, living));
        if (result != null && result.getHitEntity() instanceof LivingEntity living) return living;
        return nearby(c, c.player().getLocation(), Math.max(1.0D, range), 8).stream()
                .filter(c.player()::hasLineOfSight).findFirst().orElse(null);
    }

    private static Location targetLocation(AbilityContext c, double range) {
        RayTraceResult result = c.player().getWorld().rayTraceBlocks(c.player().getEyeLocation(), c.player().getEyeLocation().getDirection(),
                Math.max(1.0D, range), FluidCollisionMode.NEVER, true);
        return result != null && result.getHitPosition() != null ? result.getHitPosition().toLocation(c.player().getWorld())
                : c.player().getEyeLocation().add(c.player().getEyeLocation().getDirection().multiply(Math.max(1.0D, range)));
    }

    private static boolean stormcut(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        LivingEntity first = target(c, a.range());
        if (first == null) return false;
        first.getWorld().strikeLightningEffect(first.getLocation());
        direct(c, first, damage, Particle.ELECTRIC_SPARK);
        List<LivingEntity> chain = nearby(c, first.getLocation(), a.radius(), a.maxTargets());
        for (LivingEntity target : chain) if (!target.equals(first)) direct(c, target, damage * 0.45D, Particle.ELECTRIC_SPARK);
        return true;
    }

    private static boolean spiritEdge(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        List<LivingEntity> targets = nearby(c, c.player().getLocation(), a.radius(), a.maxTargets());
        hitList(c, targets, damage, Particle.SOUL, null);
        if (!targets.isEmpty()) restoreMana(c, Math.min(25, targets.size() * 5));
        return !targets.isEmpty();
    }

    private static boolean shadowSting(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        LivingEntity target = target(c, a.range());
        if (target == null) return false;
        Location behind = target.getLocation().subtract(target.getLocation().getDirection().setY(0).normalize().multiply(1.2D));
        if (safeDestination(behind)) c.player().teleport(behind.setDirection(target.getLocation().toVector().subtract(behind.toVector())));
        return direct(c, target, damage, Particle.SMOKE);
    }

    private static boolean bloodNeedle(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        LivingEntity target = target(c, a.range());
        if (target == null) {
            c.player().sendMessage(club.abelix.games.fakevival.util.CC.color("&cNo valid target found."));
            return false;
        }
        Location start = c.player().getEyeLocation();
        Vector path = target.getEyeLocation().toVector().subtract(start.toVector());
        double length = path.length();
        if (length > 0.01D) {
            Vector step = path.normalize().multiply(0.45D);
            Location point = start.clone();
            for (double travelled = 0.0D; travelled < length; travelled += 0.45D) {
                point.add(step);
                point.getWorld().spawnParticle(Particle.DUST, point, 1, 0.02D, 0.02D, 0.02D, 0.0D,
                        new Particle.DustOptions(Color.fromRGB(190, 15, 35), 0.8F));
            }
        }
        double requested = Math.max(1.0D, damage * 1.25D);
        safeDamage(c, target, requested);
        double healed = c.plugin().getMmoStatService().healMmoHealth(c.player(), requested * 0.25D);
        c.player().sendActionBar(club.abelix.games.fakevival.util.CC.color("&cBlood Needle &7dealt &f"
                + Math.round(requested) + " &7damage and healed &c" + Math.round(healed) + "❤&7."));
        particle(target.getLocation().add(0, Math.min(1.0D, target.getHeight() * 0.5D), 0), Particle.DAMAGE_INDICATOR, 8, 0.25D);
        return true;
    }

    private static boolean mark(AbilityContext c, WeaponAbilityDefinition a, Map<UUID, TimedTarget> marks, Particle particle) {
        LivingEntity target = target(c, a.range());
        if (target == null) return false;
        marks.put(c.player().getUniqueId(), new TimedTarget(target.getUniqueId(), System.currentTimeMillis() + 15_000L));
        particle(target.getLocation().add(0, 1, 0), particle, 8, 0.3D);
        return true;
    }

    private static boolean backstab(AbilityContext c, double damage) {
        if (c.target() == null) return false;
        Vector targetForward = c.target().getLocation().getDirection().setY(0).normalize();
        Vector towardAttacker = c.player().getLocation().toVector().subtract(c.target().getLocation().toVector()).setY(0).normalize();
        return targetForward.dot(towardAttacker) < -0.35D && direct(c, c.target(), damage, Particle.CRIT);
    }

    private static boolean smokeVeil(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        List<LivingEntity> targets = nearby(c, c.player().getLocation(), a.radius(), a.maxTargets());
        for (LivingEntity target : targets) {
            slow(target, 80, 2);
            direct(c, target, damage, Particle.SMOKE);
        }
        c.player().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, true));
        particle(c.player().getLocation(), Particle.SMOKE, 20, 1.5D);
        return true;
    }

    private static boolean consumeSilentKiller(AbilityContext c, double damage) {
        if (c.target() == null || !active(SILENT_KILLER, c.player().getUniqueId())) return false;
        SILENT_KILLER.remove(c.player().getUniqueId());
        return direct(c, c.target(), damage, Particle.CRIT);
    }

    private static boolean prepareArrow(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        Projectile projectile = c.projectile();
        if (projectile == null) return false;
        PROJECTILES.put(projectile.getUniqueId(), new ProjectileCast(c.player().getUniqueId(), c.itemId(), a.id(), damage));
        if (projectile instanceof AbstractArrow arrow) {
            if (a.id().equals("windpierce")) arrow.setPierceLevel(3);
            if (a.id().equals("ember_arrow")) arrow.setFireTicks(100);
            arrow.setDamage(Math.max(arrow.getDamage(), Math.min(20.0D, damage * 0.30D)));
        }
        return true;
    }

    private static boolean barrage(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        Vector base = c.player().getEyeLocation().getDirection().normalize();
        for (int i = -2; i <= 2; i++) {
            Arrow arrow = c.player().launchProjectile(Arrow.class, base.clone().add(new Vector(i * 0.055D, Math.abs(i) * 0.01D, 0)).normalize().multiply(2.2D));
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setDamage(Math.min(8.0D, Math.max(1.0D, damage * 0.22D)));
        }
        return feedback(c, Particle.CRIT, Sound.ENTITY_ARROW_SHOOT);
    }

    private static boolean chain(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        if (c.target() == null) return false;
        List<LivingEntity> targets = nearby(c, c.target().getLocation(), a.radius(), a.maxTargets());
        return hitList(c, targets, damage, Particle.CRIT, null);
    }

    private static boolean magicProjectile(AbilityContext c, WeaponAbilityDefinition a, double damage, String mode, Particle particle) {
        Snowball projectile = c.player().launchProjectile(Snowball.class, c.player().getEyeLocation().getDirection().multiply(1.8D));
        PROJECTILES.put(projectile.getUniqueId(), new ProjectileCast(c.player().getUniqueId(), c.itemId(), mode, damage));
        particle(c.player().getEyeLocation(), particle, 5, 0.15D);
        return true;
    }

    private static boolean manaDrain(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        LivingEntity target = target(c, a.range());
        if (target == null) return false;
        direct(c, target, damage, Particle.WITCH);
        restoreMana(c, 18);
        return true;
    }

    private static boolean magicMissile(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        LivingEntity target = target(c, a.range());
        if (target == null) return false;
        return direct(c, target, damage, Particle.ENCHANTED_HIT);
    }

    private static boolean stormcall(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        LivingEntity first = target(c, a.range());
        Location center = first == null ? targetLocation(c, a.range()) : first.getLocation();
        List<LivingEntity> targets = nearby(c, center, a.radius(), a.maxTargets());
        for (LivingEntity target : targets) {
            target.getWorld().strikeLightningEffect(target.getLocation());
            direct(c, target, damage, Particle.ELECTRIC_SPARK);
        }
        return !targets.isEmpty();
    }

    private static boolean earthenWall(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        List<LivingEntity> targets = nearby(c, c.player().getLocation(), a.radius(), a.maxTargets());
        for (LivingEntity target : targets) {
            direct(c, target, damage, Particle.DUST_PLUME);
            knock(target, c.player().getLocation(), 1.1D, 0.3D);
        }
        buff(c, 6, Map.of(MMOStatType.DEFENSE, 30.0D));
        return true;
    }

    private static boolean starfall(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        LivingEntity first = target(c, a.range());
        Location center = first == null ? targetLocation(c, a.range()) : first.getLocation();
        List<LivingEntity> targets = new ArrayList<>(nearby(c, center, a.radius(), a.maxTargets()));
        for (LivingEntity target : targets) particle(target.getLocation().add(0, 3, 0), Particle.END_ROD, 5, 0.2D);
        c.plugin().getServer().getScheduler().runTaskLater(c.plugin(), () -> {
            for (LivingEntity target : targets) if (target.isValid() && !target.isDead()) direct(c, target, damage, Particle.END_ROD);
        }, 24L);
        return !targets.isEmpty();
    }

    private static boolean gravityWell(AbilityContext c, WeaponAbilityDefinition a, double damage) {
        Location center = targetLocation(c, a.range());
        List<LivingEntity> targets = nearby(c, center, a.radius(), a.maxTargets());
        for (LivingEntity target : targets) {
            Vector pull = center.toVector().subtract(target.getLocation().toVector());
            if (pull.lengthSquared() > 0.01D) target.setVelocity(pull.normalize().multiply(0.55D).setY(0.18D));
            direct(c, target, damage, Particle.REVERSE_PORTAL);
        }
        particle(center, Particle.REVERSE_PORTAL, 20, 1.5D);
        return !targets.isEmpty();
    }

    private static boolean healingSpark(AbilityContext c, WeaponAbilityDefinition a) {
        double healing = Math.clamp(5.0D + c.plugin().getMmoStatService().getTotalStats(c.player()).get(MMOStatType.ABILITY_DAMAGE) * 0.10D, 5.0D, 35.0D);
        heal(c, healing);
        int healed = 1;
        for (Player player : c.player().getWorld().getNearbyPlayers(c.player().getLocation(), a.radius())) {
            if (player.equals(c.player()) || player.getGameMode() == GameMode.SPECTATOR || healed >= a.maxTargets()) continue;
            if (c.plugin().getPveManager().canDamagePlayer(c.player(), player, EntityDamageEvent.DamageCause.MAGIC, c.item())) continue;
            c.plugin().getMmoStatService().healMmoHealth(player, healing * 0.65D);
            particle(player.getLocation().add(0, 1, 0), Particle.HEART, 4, 0.3D);
            healed++;
        }
        return true;
    }

    private static boolean dashStrike(AbilityContext c, WeaponAbilityDefinition a, double damage, boolean teleport) {
        LivingEntity target = lineTargets(c, a.range(), a.radius(), 1).stream().findFirst().orElse(null);
        if (teleport) safeDash(c.player(), Math.min(6.0D, a.range()));
        return target == null || direct(c, target, damage, Particle.CLOUD);
    }

    private static List<LivingEntity> lineTargets(AbilityContext c, double range, double radius, int max) {
        Vector start = c.player().getEyeLocation().toVector(), forward = c.player().getEyeLocation().getDirection().normalize();
        return nearby(c, c.player().getLocation(), range, 32).stream().filter(entity -> {
            Vector relative = entity.getEyeLocation().toVector().subtract(start);
            double along = relative.dot(forward);
            return along >= 0 && along <= range && relative.subtract(forward.clone().multiply(along)).lengthSquared() <= radius * radius;
        }).limit(max).toList();
    }

    private static boolean blink(AbilityContext c, double range) {
        Location start = c.player().getLocation();
        Vector direction = start.getDirection().normalize();
        Location best = start;
        for (double step = 1.0D; step <= range; step += 0.5D) {
            Location candidate = start.clone().add(direction.clone().multiply(step));
            if (!safeDestination(candidate)) break;
            best = candidate;
        }
        if (best.distanceSquared(start) < 0.25D) return false;
        c.player().teleport(best);
        return feedback(c, Particle.PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT);
    }

    private static void safeDash(Player player, double range) {
        Vector velocity = player.getLocation().getDirection().normalize().multiply(Math.clamp(range * 0.18D, 0.7D, 1.35D));
        player.setVelocity(velocity.setY(Math.max(0.12D, velocity.getY())));
    }

    private static boolean safeDestination(Location location) {
        return location != null && location.getWorld() != null && location.getBlock().isPassable()
                && location.clone().add(0, 1, 0).getBlock().isPassable()
                && !location.clone().subtract(0, 1, 0).getBlock().isPassable();
    }

    private static void buff(AbilityContext c, int seconds, Map<MMOStatType, Double> stats) {
        c.plugin().getCustomItemManager().grantTemporaryStatBuff(c.player(), stats, seconds);
    }

    private static void temporaryKnockbackResistance(AbilityContext c, double value, long ticks) {
        AttributeInstance attribute = c.player().getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (attribute == null) return;
        double previous = attribute.getBaseValue();
        double applied = Math.max(previous, Math.clamp(value, 0.0D, 1.0D));
        attribute.setBaseValue(applied);
        c.plugin().getServer().getScheduler().runTaskLater(c.plugin(), () -> {
            AttributeInstance current = c.player().getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (current != null && Math.abs(current.getBaseValue() - applied) < 0.0001D) current.setBaseValue(previous);
        }, ticks);
    }

    private static boolean heal(AbilityContext c, double amount) {
        c.plugin().getMmoStatService().healMmoHealth(c.player(),
                amount * c.plugin().getCustomEnchantManager().abilityPowerMultiplier(c.item(), "", true));
        particle(c.player().getLocation().add(0, 1, 0), Particle.HEART, 5, 0.3D);
        return true;
    }

    private static void restoreMana(AbilityContext c, int amount) {
        c.plugin().getServer().getScheduler().runTask(c.plugin(), () -> {
            if (!c.player().isOnline()) return;
            int current = c.plugin().getManaActionBarManager().getMana(c.player());
            c.plugin().getManaActionBarManager().setMana(c.player(),
                    Math.min(c.plugin().getManaActionBarManager().getMaxMana(c.player()), current + amount));
        });
    }

    private static boolean feedback(AbilityContext c, Particle particle, Sound sound) {
        particle(c.player().getLocation().add(0, 1, 0), particle, 10, 0.5D);
        c.player().playSound(c.player().getLocation(), sound, 0.8F, 1.1F);
        return true;
    }

    private static Location impact(AbilityContext c) {
        return c.location() == null ? c.player().getLocation() : c.location();
    }

    private static void areaSimple(AbilityContext c, Location center, double radius, int max, double damage, Effect effect) {
        for (LivingEntity target : nearby(c, center, radius, max)) {
            if (effect != null) effect.apply(target);
            direct(c, target, damage, Particle.CRIT);
        }
    }

    private static boolean effect(LivingEntity target, PotionEffectType type, int ticks, int amplifier) {
        target.addPotionEffect(new PotionEffect(type, ticks, amplifier, true, true));
        return true;
    }

    private static void slow(LivingEntity target, int ticks, int amplifier) {
        effect(target, PotionEffectType.SLOWNESS, ticks, amplifier);
    }

    private static void knock(LivingEntity target, Location from, double power, double y) {
        Vector vector = target.getLocation().toVector().subtract(from.toVector());
        if (vector.lengthSquared() > 0.01D) target.setVelocity(vector.normalize().multiply(power).setY(y));
    }

    private static boolean isUndead(LivingEntity entity) {
        String type = entity.getType().name();
        return type.contains("ZOMBIE") || type.contains("SKELETON") || type.contains("WITHER") || type.contains("HUSK")
                || type.contains("DROWNED") || type.contains("PHANTOM") || type.contains("ZOMBIFIED");
    }

    private static boolean isBoss(AbilityContext c, LivingEntity entity) {
        return c.plugin().getDungeonManager().isDungeonBossEntity(entity) || c.plugin().getZoneManager().isZoneBoss(entity)
                || entity.hasMetadata("boss") || entity.hasMetadata("zone_boss");
    }

    private static boolean isDungeonMob(AbilityContext c, LivingEntity entity) {
        return !c.plugin().getDungeonManager().combatContext(entity).dungeonId().equals("none");
    }

    private static double healthPercent(LivingEntity target) {
        return target.getMaxHealth() <= 0.0D ? 1.0D : target.getHealth() / target.getMaxHealth();
    }

    private static Vector direction(Location from, Location to) {
        Vector result = to.toVector().subtract(from.toVector());
        return result.lengthSquared() < 0.0001D ? new Vector() : result.normalize();
    }

    private static void particle(Location location, Particle particle, int count, double spread) {
        if (location == null || location.getWorld() == null) return;
        location.getWorld().spawnParticle(particle, location, Math.clamp(count, 1, 24), spread, spread * 0.6D, spread, 0.02D);
    }

    private static boolean active(Map<UUID, Long> values, UUID id) {
        long expires = values.getOrDefault(id, 0L);
        if (expires >= System.currentTimeMillis()) return true;
        values.remove(id);
        return false;
    }

    private record ProjectileCast(UUID owner, String itemId, String mode, double damage) {}
    private record TimedTarget(UUID target, long expiresAt) {
        boolean valid(UUID value) { return expiresAt >= System.currentTimeMillis() && target.equals(value); }
    }
    @FunctionalInterface private interface Effect { void apply(LivingEntity target); }
}
