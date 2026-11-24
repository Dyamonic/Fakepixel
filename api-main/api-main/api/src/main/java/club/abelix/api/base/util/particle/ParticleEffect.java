package club.abelix.api.base.util.particle;

import club.abelix.core.bukkit.HeartBeat;
import lombok.val;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public enum ParticleEffect {

    EXPLOSION_NORMAL(EnumParticle.EXPLOSION_NORMAL, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    EXPLOSION_LARGE(EnumParticle.EXPLOSION_LARGE, new ParticleProperty[0]),
    EXPLOSION_HUGE(EnumParticle.EXPLOSION_HUGE, new ParticleProperty[0]),
    FIREWORKS_SPARK(EnumParticle.FIREWORKS_SPARK, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    WATER_BUBBLE(EnumParticle.WATER_BUBBLE, new ParticleProperty[]{ParticleProperty.DIRECTIONAL, ParticleProperty.REQUIRES_WATER}),
    WATER_SPLASH(EnumParticle.WATER_SPLASH, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    WATER_WAKE(EnumParticle.WATER_WAKE, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    SUSPENDED(EnumParticle.SUSPENDED, new ParticleProperty[]{ParticleProperty.REQUIRES_WATER}),
    SUSPENDED_DEPTH(EnumParticle.SUSPENDED_DEPTH, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    CRIT(EnumParticle.CRIT, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    CRIT_MAGIC(EnumParticle.CRIT_MAGIC, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    SMOKE_NORMAL(EnumParticle.SMOKE_NORMAL, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    SMOKE_LARGE(EnumParticle.SMOKE_LARGE, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    SPELL(EnumParticle.SPELL, new ParticleProperty[0]),
    SPELL_INSTANT(EnumParticle.SPELL_INSTANT, new ParticleProperty[0]),
    SPELL_MOB(EnumParticle.SPELL_MOB, new ParticleProperty[]{ParticleProperty.COLORABLE}),
    SPELL_MOB_AMBIENT(EnumParticle.SPELL_MOB_AMBIENT, new ParticleProperty[]{ParticleProperty.COLORABLE}),
    SPELL_WITCH(EnumParticle.SPELL_WITCH, new ParticleProperty[0]),
    DRIP_WATER(EnumParticle.DRIP_WATER, new ParticleProperty[0]),
    DRIP_LAVA(EnumParticle.DRIP_LAVA, new ParticleProperty[0]),
    VILLAGER_ANGRY(EnumParticle.VILLAGER_ANGRY, new ParticleProperty[0]),
    VILLAGER_HAPPY(EnumParticle.VILLAGER_HAPPY, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    TOWN_AURA(EnumParticle.TOWN_AURA, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    NOTE(EnumParticle.NOTE, new ParticleProperty[]{ParticleProperty.COLORABLE}),
    PORTAL(EnumParticle.PORTAL, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    ENCHANTMENT_TABLE(EnumParticle.ENCHANTMENT_TABLE, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    FLAME(EnumParticle.FLAME, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    LAVA(EnumParticle.LAVA, new ParticleProperty[0]),
    FOOTSTEP(EnumParticle.FOOTSTEP, new ParticleProperty[0]),
    CLOUD(EnumParticle.CLOUD, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    REDSTONE(EnumParticle.REDSTONE, new ParticleProperty[]{ParticleProperty.COLORABLE}),
    SNOWBALL(EnumParticle.SNOWBALL, new ParticleProperty[0]),
    SNOW_SHOVEL(EnumParticle.SNOW_SHOVEL, new ParticleProperty[]{ParticleProperty.DIRECTIONAL}),
    SLIME(EnumParticle.SLIME, new ParticleProperty[0]),
    HEART(EnumParticle.HEART, new ParticleProperty[0]),
    BARRIER(EnumParticle.BARRIER, new ParticleProperty[0]),
    ITEM_CRACK(EnumParticle.ITEM_CRACK, new ParticleProperty[]{ParticleProperty.DIRECTIONAL, ParticleProperty.REQUIRES_DATA}),
    BLOCK_CRACK(EnumParticle.BLOCK_CRACK, new ParticleProperty[]{ParticleProperty.REQUIRES_DATA}),
    BLOCK_DUST(EnumParticle.BLOCK_DUST, new ParticleProperty[]{ParticleProperty.DIRECTIONAL, ParticleProperty.REQUIRES_DATA}),
    WATER_DROP(EnumParticle.WATER_DROP, new ParticleProperty[0]),
    ITEM_TAKE(EnumParticle.ITEM_TAKE, new ParticleProperty[0]),
    MOB_APPEARANCE(EnumParticle.MOB_APPEARANCE, new ParticleProperty[0]);

    private final EnumParticle particle;
    private final Set<ParticleProperty> properties;

    ParticleEffect(EnumParticle particle, ParticleProperty[] properties) {
        this.particle = particle;

        this.properties = EnumSet.noneOf(ParticleProperty.class);
        Collections.addAll(this.properties, properties);
    }

    public void display(float offsetX, float offsetY, float offsetZ, float speed, int amount, Location center, double range) {
        new ParticlePacket(this, offsetX, offsetY, offsetZ, speed, amount).sendTo(center, range);
    }

    public void display(float offsetX, float offsetY, float offsetZ, float speed, int amount, Location center) {
        new ParticlePacket(this, offsetX, offsetY, offsetZ, speed, amount).sendTo(center, 128);
    }

    public void display(float speed, int amount, Location center) {
        new ParticlePacket(this, 0, 0, 0, speed, amount).sendTo(center, 128);
    }

    public void display(float offsetX, float offsetY, float offsetZ, float speed, int amount, Location center, List<Player> players) {
        new ParticlePacket(this, offsetX, offsetY, offsetZ, speed, amount).sendTo(center, players);
    }

    @Deprecated
    public void display(float offsetX, float offsetY, float offsetZ, float speed, int amount, Location center, Player... players) {
        this.display(offsetX, offsetY, offsetZ, speed, amount, center, Arrays.asList(players));
    }

    public void display(Vector direction, float speed, Location center, double range) {
        new ParticlePacket(this, direction, speed).sendTo(center, range);
    }

    public void display(Vector direction, float speed, Location center, List<Player> players) {
        new ParticlePacket(this, direction, speed).sendTo(center, players);
    }

    @Deprecated
    public void display(Vector direction, float speed, Location center, Player... players) {
        this.display(direction, speed, center, Arrays.asList(players));
    }

    public void display(ParticleColor color, Location center, double range) {
        new ParticlePacket(this, color).sendTo(center, range);
    }

    public void display(ParticleColor color, Location center, List<Player> players) {
        new ParticlePacket(this, color).sendTo(center, players);
    }

    @Deprecated
    public void display(ParticleColor color, Location center, Player... players) {
        this.display(color, center, Arrays.asList(players));
    }

    public void display(ParticleData data, float offsetX, float offsetY, float offsetZ, float speed, int amount, Location center, double range) {
        new ParticlePacket(this, offsetX, offsetY, offsetZ, speed, amount).sendTo(center, range);
    }

    public void display(ParticleData data, float offsetX, float offsetY, float offsetZ, float speed, int amount, Location center, List<Player> players) {

        new ParticlePacket(this, offsetX, offsetY, offsetZ, speed, amount).sendTo(center, players);
    }

    @Deprecated
    public void display(ParticleData data, float offsetX, float offsetY, float offsetZ, float speed, int amount, Location center, Player... players) {
        this.display(data, offsetX, offsetY, offsetZ, speed, amount, center, Arrays.asList(players));
    }

    public void display(ParticleData data, Vector direction, float speed, Location center, double range) {
        new ParticlePacket(this, direction, speed).sendTo(center, range);
    }

    public void display(ParticleData data, Vector direction, float speed, Location center, List<Player> players) {
        new ParticlePacket(this, direction, speed).sendTo(center, players);
    }

    @Deprecated
    public void display(ParticleData data, Vector direction, float speed, Location center, Player... players) {
        this.display(data, direction, speed, center, Arrays.asList(players));
    }

    public enum ParticleProperty {
        REQUIRES_WATER,
        REQUIRES_DATA,
        DIRECTIONAL,
        COLORABLE
    }

    public abstract static class ParticleData {

        private final Material material;
        private final byte data;

        ParticleData(Material material, byte data) {
            this.material = material;
            this.data = data;
        }

        public Material getMaterial() {
            return this.material;
        }

        public byte getData() {
            return this.data;
        }

    }

    public abstract static class ParticleColor {
        public abstract float getValueX();

        public abstract float getValueY();

        public abstract float getValueZ();
    }

    public static class OrdinaryColor extends ParticleColor {
        private final int red;
        private final int green;
        private final int blue;

        public OrdinaryColor(int red, int green, int blue) throws IllegalArgumentException {
            if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
                throw new IllegalArgumentException("Some value is ouf of bounds (" + red + ", " + green + ", " + blue + ")");
            }

            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public OrdinaryColor(Color color) {
            this(color.getRed(), color.getGreen(), color.getBlue());
        }

        @Override
        public float getValueX() {
            return this.red / 255.0f;
        }

        @Override
        public float getValueY() {
            return this.green / 255.0f;
        }

        @Override
        public float getValueZ() {
            return this.blue / 255.0f;
        }

        public int getRed() {
            return this.red;
        }

        public int getGreen() {
            return this.green;
        }

        public int getBlue() {
            return this.blue;
        }
    }

    public static class ItemData extends ParticleData {
        public ItemData(Material material, byte data) {
            super(material, data);
        }
    }

    public static class BlockData extends ParticleData {
        public BlockData(Material material, byte data) throws IllegalArgumentException {
            super(material, data);

            if (!material.isBlock()) {
                throw new IllegalArgumentException("The material is not a block");
            }
        }
    }

    public static class NoteColor extends ParticleColor {
        private final int note;

        public NoteColor(int note) throws IllegalArgumentException {
            if (note < 0) {
                throw new IllegalArgumentException("The note value is lower than 0");
            }

            if (note > 24) {
                throw new IllegalArgumentException("The note value is higher than 24");
            }

            this.note = note;
        }

        @Override
        public float getValueX() {
            return this.note / 24.0f;
        }

        @Override
        public float getValueY() {
            return 0.0f;
        }

        @Override
        public float getValueZ() {
            return 0.0f;
        }
    }

    public static final class ParticlePacket {
        private static boolean initialized;
        private final ParticleEffect effect;
        private final float offsetY;
        private final float offsetZ;
        private final float speed;
        private final int amount;
        private float offsetX;
        private Packet packet;

        public ParticlePacket(ParticleEffect effect, float offsetX, float offsetY, float offsetZ, float speed, int amount) throws IllegalArgumentException {
            initialize();

            this.effect = effect;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
            this.amount = amount;
        }

        public ParticlePacket(ParticleEffect effect, Vector direction, float speed) throws IllegalArgumentException {
            this(effect, (float) direction.getX(), (float) direction.getY(), (float) direction.getZ(), speed, 0);
        }

        public ParticlePacket(ParticleEffect effect, ParticleColor color) {
            this(effect, color.getValueX(), color.getValueY(), color.getValueZ(), 1, 0);

            if (effect == ParticleEffect.REDSTONE && color instanceof OrdinaryColor && ((OrdinaryColor) color).getRed() == 0) {
                offsetX = Float.MIN_NORMAL;
            }
        }

        public static void initialize() {
            if (initialized) {
                return;
            }

            initialized = true;
        }

        private void initializePacket(Location center) {
            if (packet != null) {
                return;
            }

            packet = new PacketPlayOutWorldParticles(
                    effect.particle,
                    true,
                    (float) center.getX(),
                    (float) center.getY(),
                    (float) center.getZ(),
                    offsetX, offsetY, offsetZ, speed, amount);
        }

        public void sendTo(Player player) {
            HeartBeat.runAsync(() -> ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet));
        }

        public void sendTo(Location center, List<Player> players) throws IllegalArgumentException {
            if (players.isEmpty()) {
                throw new IllegalArgumentException("The player list is empty");
            }

            initializePacket(center);

            players.forEach(this::sendTo);
        }

        public void sendTo(Location center, double range) throws IllegalArgumentException {
            String worldName = center.getWorld().getName();

            double squared = range * range;

            initializePacket(center);

            for (val player : Bukkit.getOnlinePlayers()) {
                val location = player.getLocation();

                if (!location.getWorld().getName().equals(worldName) || location.distanceSquared(center) > squared) {
                    continue;
                }

                sendTo(player);
            }
        }
    }
}