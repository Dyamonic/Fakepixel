package club.abelix.games.buildffa.game.manager.event;

import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.BiConsumer;

final class EventSaveFilter implements AutoCloseable {
    private final Object playerList;
    private final Field field;
    private final Object delegate;
    private final Object filter;

    EventSaveFilter(BiConsumer<Player, Runnable> sanitize) {
        try {
            Object server = MinecraftReflection.getMinecraftServerClass().getMethod("getServer").invoke(null);
            playerList = server.getClass().getMethod("getPlayerList").invoke(server);
            field = playerList.getClass().getField("playerFileData");
            delegate = field.get(playerList);
            filter = Proxy.newProxyInstance(field.getType().getClassLoader(), new Class<?>[]{field.getType()}, (proxy, method, args) -> {
                if (method.getName().equals("save") && args != null && args.length == 1) {
                    if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Player serialization must run on the server thread");
                    Object entity = args[0].getClass().getMethod("getBukkitEntity").invoke(args[0]);
                    if (entity instanceof Player) {
                        sanitize.accept((Player) entity, () -> {
                            try {
                                method.invoke(delegate, args);
                            } catch (InvocationTargetException exception) {
                                throw new IllegalStateException("Player save failed", exception.getCause());
                            } catch (ReflectiveOperationException exception) {
                                throw new IllegalStateException("Player save failed", exception);
                            }
                        });
                        return null;
                    }
                }
                try {
                    return method.invoke(delegate, args);
                } catch (InvocationTargetException exception) {
                    throw exception.getCause();
                }
            });
            field.set(playerList, filter);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot attach BuildFFA temporary-state save filter", exception);
        }
    }

    boolean isAttached() {
        try {
            return field.get(playerList) == filter;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public void close() {
        try {
            if (field.get(playerList) == filter) field.set(playerList, delegate);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot detach BuildFFA temporary-state save filter", exception);
        }
    }
}
