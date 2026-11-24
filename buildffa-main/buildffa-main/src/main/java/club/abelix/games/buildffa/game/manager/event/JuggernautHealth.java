package club.abelix.games.buildffa.game.manager.event;

import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.entity.Player;

import java.util.UUID;

final class JuggernautHealth {
    private static final UUID BONUS = UUID.fromString("32d7b807-106b-4584-8b2e-36905a17d179");

    private JuggernautHealth() {
    }

    static void add(Player player) {
        try {
            String namespace = MinecraftReflection.getMinecraftPackage();
            Class<?> type = Class.forName(namespace + ".AttributeInstance");
            Class<?> modifier = Class.forName(namespace + ".AttributeModifier");
            Object attribute = attribute(player);
            if (type.getMethod("a", UUID.class).invoke(attribute, BONUS) != null) return;
            double previous = player.getMaxHealth();
            Object bonus = modifier.getConstructor(UUID.class, String.class, double.class, int.class)
                    .newInstance(BONUS, "buildffa.juggernaut", 8.0D, 0);
            modifier.getMethod("a", boolean.class).invoke(bonus, false);
            type.getMethod("b", modifier).invoke(attribute, bonus);
            double delta = player.getMaxHealth() - previous;
            if (Math.abs(delta - 8.0D) > 0.001D) {
                type.getMethod("c", modifier).invoke(attribute, bonus);
                if (delta <= 0.0D) throw new IllegalStateException("Maximum health cannot accommodate Juggernaut bonus");
                bonus = modifier.getConstructor(UUID.class, String.class, double.class, int.class)
                        .newInstance(BONUS, "buildffa.juggernaut", 64.0D / delta, 0);
                modifier.getMethod("a", boolean.class).invoke(bonus, false);
                type.getMethod("b", modifier).invoke(attribute, bonus);
                if (Math.abs(player.getMaxHealth() - previous - 8.0D) > 0.001D) {
                    type.getMethod("c", modifier).invoke(attribute, bonus);
                    throw new IllegalStateException("Maximum health cannot accommodate four additional hearts");
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot apply Juggernaut health bonus", exception);
        }
    }

    static void remove(Player player) {
        try {
            String namespace = MinecraftReflection.getMinecraftPackage();
            Class<?> type = Class.forName(namespace + ".AttributeInstance");
            Class<?> modifier = Class.forName(namespace + ".AttributeModifier");
            Object attribute = attribute(player);
            Object bonus = type.getMethod("a", UUID.class).invoke(attribute, BONUS);
            if (bonus != null) type.getMethod("c", modifier).invoke(attribute, bonus);
            if (!player.isDead()) player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot remove Juggernaut health bonus", exception);
        }
    }

    private static Object attribute(Player player) throws ReflectiveOperationException {
        String namespace = MinecraftReflection.getMinecraftPackage();
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object maximum = Class.forName(namespace + ".GenericAttributes").getField("maxHealth").get(null);
        return handle.getClass().getMethod("getAttributeInstance", Class.forName(namespace + ".IAttribute")).invoke(handle, maximum);
    }
}
