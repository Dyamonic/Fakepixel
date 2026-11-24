package club.abelix.games.fakevival.abilities;

import club.abelix.games.fakevival.mmo.MMOItemType;
import club.abelix.games.fakevival.mmo.MMOStatType;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CustomAbilityRegistry {
    private final Map<String, CustomItemAbility> abilities = new LinkedHashMap<>();
    private final Set<String> duplicateIds = new java.util.LinkedHashSet<>();

    public CustomAbilityRegistry() {
        registerDefaults();
        registerWeaponAbilities();
    }

    public Optional<CustomItemAbility> get(String id) {
        return Optional.ofNullable(abilities.get(normalize(id)));
    }

    public Collection<CustomItemAbility> all() {
        return List.copyOf(abilities.values());
    }

    public boolean contains(String id) {
        return abilities.containsKey(normalize(id));
    }

    public Set<String> duplicateIds() {
        return Set.copyOf(duplicateIds);
    }

    private void register(CustomItemAbility ability) {
        if (ability == null || ability.id() == null || ability.id().isBlank()) return;
        String id = normalize(ability.id());
        CustomItemAbility existing = abilities.get(id);
        boolean replacesBootstrapPlaceholder = existing != null && existing.description().stream()
                .anyMatch(line -> line != null && line.toLowerCase(Locale.ROOT).contains("safe placeholder"));
        if (existing != null && !replacesBootstrapPlaceholder) duplicateIds.add(id);
        abilities.put(id, ability);
    }

    private void registerWeaponAbilities() {
        for (String id : List.of(
                "ember_slash", "frostbite_strike", "guardian_cut", "void_rend", "solar_cleave",
                "shadow_sting", "venom_cut", "piercing_fang", "blood_needle", "nightfall_dash",
                "sakura_slice", "stormcut", "oni_rage", "moonlight_cut", "bamboo_sweep",
                "windpierce", "ember_arrow", "frozen_arrow", "royal_bolt", "voidshot",
                "spark_bolt", "frost_bolt", "toxic_burst", "void_pulse", "sunbeam",
                "magic_missile", "stormcall", "necrotic_wave", "crystal_beam", "infernal_nova")) {
            if (contains(id)) continue;
            String display = java.util.Arrays.stream(id.split("_"))
                    .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(java.util.stream.Collectors.joining(" "));
            register(ability(id, display, AbilityTrigger.RIGHT_CLICK, 0,
                    List.of("&7This ability is registered as a safe placeholder."), context -> {
                        context.player().sendMessage("§7That ability is not active yet.");
                        return true;
                    }));
        }
        registerAllWeaponAbilities();
    }

    private void registerDefaults() {
        register(ability("prospect", "Prospect", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Reveals nearby ores for a short time."), ToolAbilityExecutor::prospect));
        register(ability("vein_burst", "Vein Burst", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Breaks a connected vein of matching ore."), ToolAbilityExecutor::veinBurst));
        register(ability("stone_pulse", "Stone Pulse", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Breaks a small area of stone-like blocks."), ToolAbilityExecutor::stonePulse));
        register(ability("treasure_sense", "Treasure Sense", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Highlights nearby treasure and ores."), ToolAbilityExecutor::treasureSense));
        register(ability("earthquake", "Earthquake", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Damages and knocks back nearby hostile mobs."), ToolAbilityExecutor::earthquake));
        register(ability("excavate", "Excavate", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Excavates a small patch of soft blocks."), ToolAbilityExecutor::excavate));
        register(ability("harvest_sweep", "Harvest Sweep", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Harvests and replants nearby mature crops."), ToolAbilityExecutor::harvestSweep));
        register(ability("growth_aura", "Growth Aura", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Advances the growth of nearby crops."), ToolAbilityExecutor::growthAura));
        register(ability("timber_cleave", "Timber Cleave", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Breaks connected logs above the target."), ToolAbilityExecutor::timberCleave));
        register(ability("leaf_storm", "Leaf Storm", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Clears a limited number of nearby leaves."), ToolAbilityExecutor::leafStorm));
        register(ability("root_snare", "Root Snare", AbilityTrigger.RIGHT_CLICK, 0, List.of("&7Slows and lightly damages nearby hostile mobs."), ToolAbilityExecutor::rootSnare));
        register(ability("flame_burst", "Flame Burst", AbilityTrigger.RIGHT_CLICK, 35, List.of("&7Burn nearby enemies and deal fire damage."), context -> {
            for (LivingEntity entity : nearbyEnemies(context, 5.0)) {
                entity.setFireTicks(Math.max(entity.getFireTicks(), 80));
                entity.damage(4.0, context.player());
            }
            particle(context.player().getLocation(), Particle.FLAME, 60, 3, 1, 3);
            sound(context.player(), Sound.ITEM_FIRECHARGE_USE);
            return true;
        }));
        register(ability("frost_nova", "Frost Nova", AbilityTrigger.RIGHT_CLICK, 40, List.of("&7Slow and damage nearby enemies."), context -> {
            for (LivingEntity entity : nearbyEnemies(context, 5.0)) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                entity.damage(3.5, context.player());
            }
            particle(context.player().getLocation(), Particle.SNOWFLAKE, 80, 4, 1, 4);
            sound(context.player(), Sound.BLOCK_GLASS_BREAK);
            return true;
        }));
        register(ability("thunder_strike", "Thunder Strike", AbilityTrigger.RIGHT_CLICK, 50, List.of("&7Strike your target with lightning damage."), context -> {
            Location location = target(context, 24).map(LivingEntity::getLocation).orElse(targetLocation(context, 24));
            context.player().getWorld().strikeLightningEffect(location);
            for (LivingEntity entity : enemiesNear(context, location, 3.0)) entity.damage(7.0, context.player());
            return true;
        }));
        register(ability("shadow_step", "Shadow Step", AbilityTrigger.RIGHT_CLICK, 30, List.of("&7Teleport behind your target or dash forward."), context -> {
            Optional<LivingEntity> target = target(context, 18);
            Location destination = target.map(entity -> entity.getLocation().clone().subtract(entity.getLocation().getDirection().normalize().multiply(1.5)))
                    .orElse(context.player().getLocation().add(context.player().getLocation().getDirection().normalize().multiply(6)));
            if (!destination.getBlock().isPassable() || !destination.clone().add(0, 1, 0).getBlock().isPassable()) return false;
            context.player().teleport(destination);
            particle(destination, Particle.PORTAL, 50, 1, 1, 1);
            sound(context.player(), Sound.ENTITY_ENDERMAN_TELEPORT);
            return true;
        }));
        register(ability("vampire_bite", "Vampire Bite", AbilityTrigger.HIT_ENTITY, 0, List.of("&7Heal for part of your melee damage."), context -> {
            context.player().setHealth(Math.min(context.player().getMaxHealth(), context.player().getHealth() + Math.max(1.0, context.damage() * 0.25)));
            particle(context.player().getLocation(), Particle.HEART, 8, 0.5, 0.8, 0.5);
            return true;
        }));
        register(ability("earthquake_slam", "Earthquake Slam", AbilityTrigger.RIGHT_CLICK, 55, List.of("&7Sneak and right click to slam nearby enemies."), context -> {
            if (!context.player().isSneaking()) return false;
            for (LivingEntity entity : nearbyEnemies(context, 6.0)) {
                entity.damage(6.0, context.player());
                knock(entity, context.player().getLocation(), 1.2, 0.5);
            }
            particle(context.player().getLocation(), Particle.EXPLOSION, 8, 2, 0.2, 2);
            sound(context.player(), Sound.ENTITY_GENERIC_EXPLODE);
            return true;
        }));
        register(ability("poison_tip", "Poison Tip", AbilityTrigger.HIT_ENTITY, 0, List.of("&7Poison enemies you strike."), context -> effectTarget(context, PotionEffectType.POISON, 100, 1)));
        register(ability("wither_curse", "Wither Curse", AbilityTrigger.HIT_ENTITY, 0, List.of("&7Wither and weaken enemies you strike."), context -> {
            if (context.target() == null) return false;
            context.target().addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0));
            context.target().addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0));
            return true;
        }));
        register(ability("guardian_shield", "Guardian Shield", AbilityTrigger.RIGHT_CLICK, 45, List.of("&7Gain absorption and resistance briefly."), context -> {
            context.player().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 160, 1));
            context.player().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 0));
            sound(context.player(), Sound.ITEM_SHIELD_BLOCK);
            return true;
        }));
        register(ability("mana_burst", "Mana Burst", AbilityTrigger.RIGHT_CLICK, 35, List.of("&7Fire a magic projectile that explodes safely."), context -> {
            Fireball fireball = context.player().launchProjectile(Fireball.class);
            fireball.setYield(0);
            fireball.setIsIncendiary(false);
            return true;
        }));
        register(ability("chain_lightning", "Chain Lightning", AbilityTrigger.HIT_ENTITY, 35, List.of("&7Lightning jumps to nearby enemies."), context -> {
            if (context.target() == null) return false;
            int jumps = 0;
            for (LivingEntity entity : enemiesNear(context, context.target().getLocation(), 7.0)) {
                if (entity.equals(context.target())) continue;
                entity.getWorld().strikeLightningEffect(entity.getLocation());
                entity.damage(4.5, context.player());
                if (++jumps >= 4) break;
            }
            return true;
        }));
        register(ability("soul_reaper", "Soul Reaper", AbilityTrigger.KILL_ENTITY, 0, List.of("&7Restore health and mana on kill."), context -> {
            context.player().setHealth(Math.min(context.player().getMaxHealth(), context.player().getHealth() + 4.0));
            context.plugin().getManaActionBarManager().setMana(context.player(), context.plugin().getManaActionBarManager().getMana(context.player()) + 25);
            return true;
        }));
        register(ability("berserker_rage", "Berserker Rage", AbilityTrigger.TAKE_DAMAGE, 0, List.of("&7Low health grants strength and speed."), context -> {
            if (context.player().getHealth() - context.damage() > context.player().getMaxHealth() * 0.35) return false;
            context.player().addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 1));
            context.player().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 1));
            return true;
        }));
        register(ability("healing_pulse", "Healing Pulse", AbilityTrigger.RIGHT_CLICK, 40, List.of("&7Heal yourself and nearby allies."), context -> {
            if (!context.player().isOnline() || context.player().isDead()) return false;
            double max = context.plugin().getMmoStatService().maxMmoHealth(context.player());
            double requested = 100.0D + (max * 0.15D);
            double totalHealed = context.plugin().getMmoStatService().healMmoHealth(context.player(), requested);
            for (Player player : context.player().getWorld().getNearbyPlayers(context.player().getLocation(), 6.0)) {
                if (player.equals(context.player()) || player.isDead()) continue;
                totalHealed += context.plugin().getMmoStatService().healMmoHealth(player, requested * 0.60D);
                particle(player.getLocation(), Particle.HEART, 5, 0.5, 0.8, 0.5);
            }
            if (totalHealed <= 0.0D) return false;
            context.plugin().getMmoStatService().refreshPlayerMMOStats(context.player());
            context.player().sendMessage(club.abelix.games.fakevival.util.CC.color("&aHealed for &c"
                    + Math.round(totalHealed) + "❤&a!"));
            return true;
        }));
        register(ability("explosive_arrow", "Explosive Arrow", AbilityTrigger.PROJECTILE_HIT, 25, List.of("&7Arrows explode without breaking blocks."), context -> {
            Location location = context.location() == null ? context.player().getLocation() : context.location();
            location.getWorld().createExplosion(location, 2.2F, false, false, context.player());
            for (LivingEntity entity : enemiesNear(context, location, 4.0)) entity.damage(5.0, context.player());
            return true;
        }));
        register(ability("gravity_pull", "Gravity Pull", AbilityTrigger.RIGHT_CLICK, 40, List.of("&7Pull nearby enemies toward you."), context -> {
            for (LivingEntity entity : nearbyEnemies(context, 8.0)) entity.setVelocity(context.player().getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.9).setY(0.25));
            particle(context.player().getLocation(), Particle.REVERSE_PORTAL, 80, 4, 1, 4);
            return true;
        }));
        register(ability("dash_attack", "Dash Attack", AbilityTrigger.RIGHT_CLICK, 25, List.of("&7Dash forward and damage enemies hit."), context -> {
            context.player().setVelocity(context.player().getLocation().getDirection().normalize().multiply(1.6).setY(0.25));
            for (LivingEntity entity : enemiesNear(context, context.player().getLocation().add(context.player().getLocation().getDirection().multiply(3)), 3.0)) entity.damage(5.0, context.player());
            return true;
        }));
        register(ability("meteor_call", "Meteor Call", AbilityTrigger.RIGHT_CLICK, 75, List.of("&7Call a delayed safe meteor strike."), context -> {
            Location location = targetLocation(context, 28);
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                location.getWorld().createExplosion(location, 3.0F, false, false, context.player());
                for (LivingEntity entity : enemiesNear(context, location, 5.0)) entity.damage(8.0, context.player());
            }, 30L);
            particle(location, Particle.FLAME, 60, 1, 2, 1);
            return true;
        }));
        register(ability("ice_prison", "Ice Prison", AbilityTrigger.RIGHT_CLICK, 45, List.of("&7Trap your target in chilling slowness."), context -> target(context, 20).map(entity -> {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 6));
            particle(entity.getLocation(), Particle.SNOWFLAKE, 50, 1, 1, 1);
            return true;
        }).orElse(false)));
        register(ability("venom_cloud", "Venom Cloud", AbilityTrigger.RIGHT_CLICK, 45, List.of("&7Create a poison cloud at target location."), context -> {
            Location location = targetLocation(context, 20);
            for (LivingEntity entity : enemiesNear(context, location, 4.5)) entity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, 1));
            particle(location, Particle.SPORE_BLOSSOM_AIR, 80, 3, 1, 3);
            return true;
        }));
        register(ability("lifeline", "Lifeline", AbilityTrigger.TAKE_DAMAGE, 0, List.of("&7Prevent lethal damage and heal slightly."), context -> {
            if (context.damageEvent() == null || context.player().getHealth() - context.damage() > 0) return false;
            context.damageEvent().setCancelled(true);
            context.player().setHealth(Math.min(context.player().getMaxHealth(), 8.0));
            sound(context.player(), Sound.ITEM_TOTEM_USE);
            return true;
        }));
        register(ability("treasure_hunter", "Treasure Hunter", AbilityTrigger.KILL_ENTITY, 0, List.of("&7Small chance for bonus coins or loot."), context -> {
            if (Math.random() > 0.18) return false;
            context.plugin().getEconomyManager().giveCoins(context.player().getUniqueId(), 25.0, "treasure_hunter");
            context.player().sendMessage("§6Treasure Hunter §8> §fFound §e25 coins§f.");
            return true;
        }));
        register(ability("molten_core", "Molten Core", AbilityTrigger.PASSIVE, 0, List.of("&7Grants fire resistance while equipped."), context -> {
            context.player().addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false, true));
            return true;
        }));
        register(ability("swift_wind", "Swift Wind", AbilityTrigger.PASSIVE, 0, List.of("&7Grants speed while equipped."), context -> {
            context.player().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, true));
            return true;
        }));
        register(ability("arcane_focus", "Arcane Focus", AbilityTrigger.PASSIVE, 0, List.of("&7Slightly reduces mana costs while equipped."), context -> true));
        register(ability("skull_crusher", "Skull Crusher", AbilityTrigger.HIT_ENTITY, 0, List.of("&7Chance to briefly stun enemies."), context -> {
            if (context.target() == null || Math.random() > 0.35) return false;
            context.target().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 8));
            return true;
        }));
        register(ability("holy_smite", "Holy Smite", AbilityTrigger.RIGHT_CLICK, 30, List.of("&7Deal bonus damage to undead mobs."), context -> {
            for (LivingEntity entity : nearbyEnemies(context, 8.0)) {
                String type = entity.getType().name();
                if (type.contains("ZOMBIE") || type.contains("SKELETON") || type.contains("WITHER") || type.contains("HUSK") || type.contains("DROWNED")) {
                    entity.damage(8.0, context.player());
                    particle(entity.getLocation(), Particle.END_ROD, 20, 0.5, 1, 0.5);
                }
            }
            return true;
        }));
        register(ability("void_rift", "Void Rift", AbilityTrigger.RIGHT_CLICK, 60, List.of("&7Create a dark implosion that damages enemies."), context -> {
            Location location = targetLocation(context, 16);
            for (LivingEntity entity : enemiesNear(context, location, 5.0)) {
                entity.damage(7.0, context.player());
                entity.setVelocity(location.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.7));
            }
            particle(location, Particle.SQUID_INK, 70, 3, 1, 3);
            return true;
        }));
        register(ability("ranger_focus", "Ranger Focus", AbilityTrigger.PROJECTILE_HIT, 20, List.of("&7Focused arrows deal increased damage."), context -> {
            if (context.trigger() == AbilityTrigger.LEFT_CLICK || context.trigger() == AbilityTrigger.LEFT_CLICK_AIR
                    || context.trigger() == AbilityTrigger.LEFT_CLICK_BLOCK) {
                context.player().sendMessage("§aRanger Focus §8> §fYour next shot is focused.");
                sound(context.player(), Sound.ENTITY_ARROW_SHOOT);
                return true;
            }
            if (context.target() == null) return false;
            context.target().damage(5.0, context.player());
            return true;
        }));
        register(ability("dragon_roar", "Dragon Roar", AbilityTrigger.RIGHT_CLICK, 70, List.of("&7Cone attack that damages and knocks back enemies."), context -> {
            Vector forward = context.player().getLocation().getDirection().normalize();
            for (LivingEntity entity : nearbyEnemies(context, 8.0)) {
                Vector to = entity.getLocation().toVector().subtract(context.player().getLocation().toVector()).normalize();
                if (forward.dot(to) < 0.45) continue;
                entity.damage(8.0, context.player());
                knock(entity, context.player().getLocation(), 1.5, 0.35);
            }
            sound(context.player(), Sound.ENTITY_ENDER_DRAGON_GROWL);
            return true;
        }));
    }

    private void registerAllWeaponAbilities() {
        registerSwordAbilities();
        registerKatanaAbilities();
        registerDaggerAbilities();
        registerBowAbilities();
        registerWandAbilities();
        registerStaffAbilities();
    }

    private void registerSwordAbilities() {
        Set<MMOItemType> t = Set.of(MMOItemType.SWORD);
        w("ember_slash", "Ember Slash", "Hits up to 5 enemies in a short fiery cone.", t, AbilityTrigger.RIGHT_CLICK, 40, 8, 7, MMOStatType.STRENGTH, 5, 7, 5);
        w("frostbite_strike", "Frostbite Strike", "Damages one target and slows it for 3 seconds.", t, AbilityTrigger.RIGHT_CLICK, 45, 10, 9, MMOStatType.STRENGTH, 0, 8, 1);
        w("guardian_cut", "Guardian Cut", "Cuts nearby enemies and grants +25 Defense for 5 seconds.", t, AbilityTrigger.RIGHT_CLICK, 35, 12, 6, MMOStatType.STRENGTH, 4, 0, 5);
        w("void_rend", "Void Rend", "Deals heavy single-target damage with a boss bonus.", t, AbilityTrigger.RIGHT_CLICK, 80, 18, 15, MMOStatType.STRENGTH, 0, 10, 1);
        w("solar_cleave", "Solar Cleave", "Cleaves nearby enemies and burns undead targets.", t, AbilityTrigger.RIGHT_CLICK, 65, 14, 10, MMOStatType.STRENGTH, 4.5, 0, 5);
        w("earthsplit", "Earthsplit", "Sends a ground shockwave through enemies in a line.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 70, 20, 12, MMOStatType.STRENGTH, 2, 10, 6);
        w("lifesteal_cut", "Lifesteal Cut", "Heals 5% of damage dealt on a successful hit.", t, AbilityTrigger.ON_HIT, 0, 6, 0, MMOStatType.STRENGTH, 0, 0, 1);
        w("royal_execution", "Royal Execution", "Deals bonus damage to targets below 30% health.", t, AbilityTrigger.RIGHT_CLICK, 90, 25, 16, MMOStatType.STRENGTH, 0, 9, 1);
        w("blazing_arc", "Blazing Arc", "A compact fire arc strikes up to 4 enemies.", t, AbilityTrigger.RIGHT_CLICK, 55, 10, 8, MMOStatType.STRENGTH, 4, 7, 4);
        w("iron_will", "Iron Will", "Grants Defense and knockback resistance for 8 seconds.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 50, 30, 0, MMOStatType.DEFENSE, 0, 0, 1);
    }

    private void registerKatanaAbilities() {
        Set<MMOItemType> t = Set.of(MMOItemType.KATANA);
        w("sakura_slice", "Sakura Slice", "A fast slash damages up to 3 nearby enemies.", t, AbilityTrigger.RIGHT_CLICK, 35, 7, 7, MMOStatType.STRENGTH, 4, 6, 3);
        w("stormcut", "Stormcut", "Calls lightning on a target and chains small damage.", t, AbilityTrigger.RIGHT_CLICK, 60, 12, 10, MMOStatType.STRENGTH, 5, 12, 4);
        w("oni_rage", "Oni Rage", "Grants +30 Strength and +25 Crit Damage for 8 seconds.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 100, 35, 0, MMOStatType.STRENGTH, 0, 0, 1);
        w("moonlight_cut", "Moonlight Cut", "Launches a ranged moon slash at enemies.", t, AbilityTrigger.RIGHT_CLICK, 55, 12, 9, MMOStatType.STRENGTH, 1.5, 14, 4);
        w("bamboo_sweep", "Bamboo Sweep", "A low-damage sweep knocks enemies away.", t, AbilityTrigger.RIGHT_CLICK, 25, 6, 4, MMOStatType.STRENGTH, 4, 0, 4);
        w("phantom_draw", "Phantom Draw", "Dashes forward and strikes the first enemy hit.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 75, 18, 12, MMOStatType.STRENGTH, 1.5, 8, 1);
        w("crimson_counter", "Crimson Counter", "Has a chance to counterattack on contact.", t, AbilityTrigger.ON_HIT, 0, 10, 5, MMOStatType.STRENGTH, 0, 0, 1);
        w("wind_blade", "Wind Blade", "A wind blade pierces up to 4 enemies.", t, AbilityTrigger.RIGHT_CLICK, 50, 9, 8, MMOStatType.STRENGTH, 1.5, 15, 4);
        w("spirit_edge", "Spirit Edge", "Damages enemies and restores a little mana per hit.", t, AbilityTrigger.RIGHT_CLICK, 70, 16, 9, MMOStatType.STRENGTH, 4.5, 0, 5);
        w("dragon_cut", "Dragon Cut", "A powerful line attack strikes up to 8 enemies.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 120, 40, 18, MMOStatType.STRENGTH, 2, 16, 8);
    }

    private void registerDaggerAbilities() {
        Set<MMOItemType> t = Set.of(MMOItemType.DAGGER);
        w("shadow_sting", "Shadow Sting", "Teleports behind a nearby target and strikes it.", t, AbilityTrigger.RIGHT_CLICK, 35, 8, 8, MMOStatType.STRENGTH, 0, 10, 1);
        w("venom_cut", "Venom Cut", "Poisons the struck target briefly.", t, AbilityTrigger.ON_HIT, 0, 5, 2, MMOStatType.STRENGTH, 0, 0, 1);
        w("piercing_fang", "Piercing Fang", "A focused strike partially ignores defense.", t, AbilityTrigger.RIGHT_CLICK, 30, 7, 9, MMOStatType.STRENGTH, 0, 8, 1);
        w("blood_needle", "Blood Needle", "Fires a blood-red needle and restores 25% of its damage.", t,
                AbilityTrigger.RIGHT_CLICK, 35, 8, 10, MMOStatType.STRENGTH, 0, 12, 1);
        w("nightfall_dash", "Nightfall Dash", "Dashes forward and grants invisibility for 2 seconds.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 50, 14, 0, MMOStatType.SPEED, 0, 6, 1);
        w("assassin_mark", "Assassin Mark", "Marks a target so the next hit deals bonus damage.", t, AbilityTrigger.RIGHT_CLICK, 60, 18, 0, MMOStatType.STRENGTH, 0, 12, 1);
        w("quickstep", "Quickstep", "Provides a short controlled movement burst.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 35, 10, 0, MMOStatType.SPEED, 0, 5, 1);
        w("backstab", "Backstab", "Deals bonus damage when attacking from behind.", t, AbilityTrigger.ON_HIT, 0, 6, 6, MMOStatType.STRENGTH, 0, 0, 1);
        w("smoke_veil", "Smoke Veil", "Slows nearby enemies and grants a brief speed boost.", t, AbilityTrigger.RIGHT_CLICK, 65, 25, 3, MMOStatType.STRENGTH, 5, 0, 5);
        w("silent_killer", "Silent Killer", "A kill empowers one later hit with bonus crit damage.", t, AbilityTrigger.ON_HIT, 0, 12, 8, MMOStatType.CRIT_DAMAGE, 0, 0, 1);
    }

    private void registerBowAbilities() {
        Set<MMOItemType> bow = Set.of(MMOItemType.BOW), crossbow = Set.of(MMOItemType.CROSSBOW);
        Set<MMOItemType> both = Set.of(MMOItemType.BOW, MMOItemType.CROSSBOW);
        w("windpierce", "Windpierce", "The fired arrow can pierce up to 3 enemies.", bow, AbilityTrigger.BOW_SHOOT, 30, 8, 6, MMOStatType.CRIT_DAMAGE, 0, 0, 3);
        w("ember_arrow", "Ember Arrow", "The fired arrow burns its target and nearby enemies.", bow, AbilityTrigger.BOW_SHOOT, 35, 9, 6, MMOStatType.CRIT_DAMAGE, 3, 0, 4);
        w("frozen_arrow", "Frozen Arrow", "The fired arrow slows its target and nearby enemies.", bow, AbilityTrigger.BOW_SHOOT, 40, 10, 5, MMOStatType.CRIT_DAMAGE, 3, 0, 4);
        w("royal_bolt", "Royal Bolt", "A crossbow bolt deals bonus damage to dungeon mobs.", crossbow, AbilityTrigger.PROJECTILE_HIT, 35, 8, 8, MMOStatType.CRIT_DAMAGE, 0, 0, 1);
        w("voidshot", "Voidshot", "Fires one high-damage void arrow.", bow, AbilityTrigger.BOW_SHOOT, 75, 18, 14, MMOStatType.CRIT_DAMAGE, 0, 0, 1);
        w("barrage", "Barrage", "Fires 5 low-damage arrows in a controlled spread.", bow, AbilityTrigger.RIGHT_CLICK, 80, 22, 4, MMOStatType.CRIT_DAMAGE, 0, 18, 5);
        w("sniper_focus", "Sniper Focus", "The next fully charged arrow deals bonus damage.", bow, AbilityTrigger.SHIFT_RIGHT_CLICK, 50, 20, 0, MMOStatType.CRIT_DAMAGE, 0, 0, 1);
        w("chain_arrow", "Chain Arrow", "Arrow impact chains damage to 3 nearby enemies.", bow, AbilityTrigger.PROJECTILE_HIT, 45, 12, 7, MMOStatType.CRIT_DAMAGE, 6, 0, 3);
        w("explosive_tip", "Explosive Tip", "Creates a small block-safe explosion on impact.", both, AbilityTrigger.PROJECTILE_HIT, 60, 18, 9, MMOStatType.CRIT_DAMAGE, 4, 0, 5);
        w("hunter_mark", "Hunter Mark", "Marks the nearest visible enemy for bonus arrow damage.", bow, AbilityTrigger.RIGHT_CLICK, 40, 16, 0, MMOStatType.CRIT_DAMAGE, 0, 20, 1);
    }

    private void registerWandAbilities() {
        Set<MMOItemType> t = Set.of(MMOItemType.WAND);
        w("spark_bolt", "Spark Bolt", "Fires a small, fast magic bolt.", t, AbilityTrigger.RIGHT_CLICK, 25, 4, 5, MMOStatType.ABILITY_DAMAGE, 0, 16, 1);
        w("frost_bolt", "Frost Bolt", "Fires a slowing magic projectile.", t, AbilityTrigger.RIGHT_CLICK, 35, 6, 6, MMOStatType.ABILITY_DAMAGE, 0, 16, 1);
        w("toxic_burst", "Toxic Burst", "Poisons enemies around the targeted area.", t, AbilityTrigger.RIGHT_CLICK, 45, 9, 6, MMOStatType.ABILITY_DAMAGE, 4, 18, 5);
        w("void_pulse", "Void Pulse", "Deals area magic damage around the target.", t, AbilityTrigger.RIGHT_CLICK, 70, 14, 10, MMOStatType.ABILITY_DAMAGE, 5, 18, 5);
        w("sunbeam", "Sunbeam", "A straight magic beam deals bonus damage to undead.", t, AbilityTrigger.RIGHT_CLICK, 65, 12, 9, MMOStatType.ABILITY_DAMAGE, 1.5, 18, 5);
        w("arcane_shield", "Arcane Shield", "Grants absorption and Defense for 6 seconds.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 80, 30, 0, MMOStatType.DEFENSE, 0, 0, 1);
        w("mana_drain", "Mana Drain", "Damages a target and restores a small amount of mana.", t, AbilityTrigger.RIGHT_CLICK, 50, 16, 8, MMOStatType.ABILITY_DAMAGE, 0, 16, 1);
        w("rift_blink", "Rift Blink", "Safely teleports a short distance forward.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 60, 20, 0, MMOStatType.INTELLIGENCE, 0, 7, 1);
        w("crystal_spikes", "Crystal Spikes", "Strikes enemies around a targeted area.", t, AbilityTrigger.RIGHT_CLICK, 75, 18, 11, MMOStatType.ABILITY_DAMAGE, 5, 18, 5);
        w("healing_spark", "Healing Spark", "Heals the caster and nearby allies.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 70, 25, 0, MMOStatType.ABILITY_DAMAGE, 6, 0, 5);
    }

    private void registerStaffAbilities() {
        Set<MMOItemType> t = Set.of(MMOItemType.STAFF);
        w("magic_missile", "Magic Missile", "Seeks the nearest visible enemy with a magic hit.", t, AbilityTrigger.RIGHT_CLICK, 35, 5, 6, MMOStatType.ABILITY_DAMAGE, 0, 18, 1);
        w("stormcall", "Stormcall", "Calls lightning over up to 6 enemies.", t, AbilityTrigger.RIGHT_CLICK, 90, 20, 11, MMOStatType.ABILITY_DAMAGE, 6, 20, 6);
        w("necrotic_wave", "Necrotic Wave", "A dark wave damages and weakens enemies.", t, AbilityTrigger.RIGHT_CLICK, 100, 24, 12, MMOStatType.ABILITY_DAMAGE, 2, 16, 6);
        w("crystal_beam", "Crystal Beam", "A focused beam pierces up to 5 enemies.", t, AbilityTrigger.RIGHT_CLICK, 85, 18, 12, MMOStatType.ABILITY_DAMAGE, 1.5, 20, 5);
        w("infernal_nova", "Infernal Nova", "A fiery nova strikes up to 8 nearby enemies.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 130, 35, 15, MMOStatType.ABILITY_DAMAGE, 7, 0, 8);
        w("earthen_wall", "Earthen Wall", "Creates a temporary defensive knockback pulse.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 90, 30, 4, MMOStatType.DEFENSE, 5, 0, 6);
        w("aqua_surge", "Aqua Surge", "A water surge damages and slows enemies in a line.", t, AbilityTrigger.RIGHT_CLICK, 75, 16, 10, MMOStatType.ABILITY_DAMAGE, 2, 16, 6);
        w("starfall", "Starfall", "Delayed stars strike up to 6 selected enemies.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 150, 45, 16, MMOStatType.ABILITY_DAMAGE, 8, 22, 6);
        w("gravity_well", "Gravity Well", "Pulls up to 8 enemies toward a damaging center.", t, AbilityTrigger.RIGHT_CLICK, 120, 35, 10, MMOStatType.ABILITY_DAMAGE, 7, 18, 8);
        w("celestial_blessing", "Celestial Blessing", "Temporarily grants Health, Defense, and Mana Regen.", t, AbilityTrigger.SHIFT_RIGHT_CLICK, 140, 45, 0, MMOStatType.ABILITY_DAMAGE, 0, 0, 1);
    }

    private void w(String id, String name, String description, Set<MMOItemType> types, AbilityTrigger trigger,
                   int mana, int cooldown, double baseDamage, MMOStatType scaling, double radius, double range, int maxTargets) {
        register(new WeaponAbilityDefinition(id, name, List.of("&7" + description), types, trigger, mana, cooldown,
                baseDamage, scaling, radius, range, maxTargets));
    }

    private SimpleCustomItemAbility ability(String id, String name, AbilityTrigger trigger, int mana, List<String> description, SimpleCustomItemAbility.AbilityExecutor executor) {
        return new SimpleCustomItemAbility(id, name, description, trigger, mana, executor);
    }

    private Optional<LivingEntity> target(AbilityContext context, double range) {
        RayTraceResult result = context.player().getWorld().rayTraceEntities(context.player().getEyeLocation(), context.player().getEyeLocation().getDirection(), range,
                entity -> entity instanceof LivingEntity living && isEnemy(context, living));
        return result != null && result.getHitEntity() instanceof LivingEntity living ? Optional.of(living) : Optional.empty();
    }

    private Location targetLocation(AbilityContext context, double range) {
        RayTraceResult result = context.player().getWorld().rayTraceBlocks(context.player().getEyeLocation(), context.player().getEyeLocation().getDirection(), range, FluidCollisionMode.NEVER, true);
        return result == null || result.getHitPosition() == null
                ? context.player().getEyeLocation().add(context.player().getEyeLocation().getDirection().multiply(range))
                : result.getHitPosition().toLocation(context.player().getWorld());
    }

    private List<LivingEntity> nearbyEnemies(AbilityContext context, double radius) {
        return enemiesNear(context, context.player().getLocation(), radius);
    }

    private List<LivingEntity> enemiesNear(AbilityContext context, Location location, double radius) {
        World world = location.getWorld();
        if (world == null) return List.of();
        return world.getNearbyLivingEntities(location, radius, entity -> isEnemy(context, entity)).stream().toList();
    }

    private boolean isEnemy(AbilityContext context, LivingEntity entity) {
        return entity != null && entity.isValid() && !entity.equals(context.player()) && !(entity instanceof Player) && entity instanceof Monster;
    }

    private boolean effectTarget(AbilityContext context, PotionEffectType type, int ticks, int amplifier) {
        if (context.target() == null) return false;
        context.target().addPotionEffect(new PotionEffect(type, ticks, amplifier));
        return true;
    }

    private void knock(LivingEntity entity, Location from, double power, double y) {
        entity.setVelocity(entity.getLocation().toVector().subtract(from.toVector()).normalize().multiply(power).setY(y));
    }

    private void particle(Location location, Particle particle, int count, double x, double y, double z) {
        try {
            if (location.getWorld() != null) location.getWorld().spawnParticle(particle, location, count, x, y, z, 0.05);
        } catch (Throwable ignored) {
        }
    }

    private void sound(Player player, Sound sound) {
        try {
            player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
        } catch (Throwable ignored) {
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }
}
