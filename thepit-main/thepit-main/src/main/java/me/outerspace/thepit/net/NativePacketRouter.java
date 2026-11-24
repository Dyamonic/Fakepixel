package me.outerspace.thepit.net;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NativePacketRouter {
    private static final String HANDLER_PREFIX = "thepit_native_";
    private final ThePitPlugin plugin;
    private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();
    private final Class<?> inboundHandlerClass;

    public NativePacketRouter(ThePitPlugin plugin) {
        this.plugin = plugin;
        Class<?> resolved = null;
        try {
            resolved = Class.forName("io.netty.channel.ChannelInboundHandler");
        } catch (ClassNotFoundException ignored) {
        }
        inboundHandlerClass = resolved;
    }

    public boolean available() {
        return inboundHandlerClass != null;
    }

    public void subscribe(Player player, String owner, InboundListener listener) throws Exception {
        if (!available()) throw new IllegalStateException("Netty inbound handler API is unavailable");
        Connection connection = connections.computeIfAbsent(player.getUniqueId(), ignored -> new Connection());
        connection.listeners.put(owner, listener);
        ensureInjected(player, connection);
    }

    public void unsubscribe(Player player, String owner) {
        Connection connection = connections.get(player.getUniqueId());
        if (connection == null) return;
        connection.listeners.remove(owner);
        if (!connection.listeners.isEmpty() || !connections.remove(player.getUniqueId(), connection)) return;
        removeHandler(connection);
    }

    public boolean ready(Player player, String owner) {
        Connection connection = connections.get(player.getUniqueId());
        return connection != null && connection.ready && connection.listeners.containsKey(owner);
    }

    public void sendPacket(Player player, Object packet) throws Exception {
        Object handle = call(player, "getHandle");
        Object connection = findField(handle.getClass(), "playerConnection").get(handle);
        call(connection, "sendPacket", packet);
    }

    public void shutdown() {
        for (Connection connection : new ArrayList<>(connections.values())) {
            connection.listeners.clear();
            removeHandler(connection);
        }
        connections.clear();
    }

    private void ensureInjected(Player player, Connection connection) throws Exception {
        Object channel = channel(player);
        Object eventLoop = call(channel, "eventLoop");
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        String name = handlerName(playerId);
        call(eventLoop, "execute", (Runnable) () -> {
            try {
                if (connections.get(playerId) != connection || connection.listeners.isEmpty()) return;
                Object oldChannel = connection.channel;
                if (oldChannel != null && oldChannel != channel) removeHandlerFromChannel(oldChannel, connection);
                connection.channel = channel;
                connection.ready = false;
                Object pipeline = call(channel, "pipeline");
                Object existing = call(pipeline, "get", name);
                if (existing != null) {
                    if (existing == connection.handler) {
                        connection.ready = true;
                        return;
                    }
                    call(pipeline, "remove", name);
                }
                Object handler = Proxy.newProxyInstance(inboundHandlerClass.getClassLoader(), new Class[]{inboundHandlerClass},
                        (proxy, method, args) -> handlePacket(connection, name, method, args));
                call(pipeline, "addBefore", "packet_handler", name, handler);
                connection.handler = handler;
                connection.ready = true;
            } catch (Throwable failure) {
                connection.ready = false;
                connection.channel = null;
                connection.handler = null;
                connections.remove(playerId, connection);
                connection.listeners.clear();
                plugin.getLogger().warning("The Pit native packet handler could not be installed for " + playerName + ": " + describe(failure));
            }
        });
    }

    private void removeHandler(Connection connection) {
        connection.ready = false;
        Object channel = connection.channel;
        if (channel == null) return;
        try {
            Object eventLoop = call(channel, "eventLoop");
            call(eventLoop, "execute", (Runnable) () -> {
                try {
                    removeHandlerFromChannel(channel, connection);
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
            connection.channel = null;
            connection.handler = null;
        }
    }

    private void removeHandlerFromChannel(Object channel, Connection connection) throws Exception {
        Object handler = connection.handler;
        if (handler != null) {
            Object pipeline = call(channel, "pipeline");
            Object existing = call(pipeline, "context", handler);
            if (existing != null) call(pipeline, "remove", handler);
        }
        if (connection.channel == channel) connection.channel = null;
        connection.handler = null;
        connection.ready = false;
    }

    private Object handlePacket(Connection connection, String name, Method method, Object[] args) throws Exception {
        String methodName = method.getName();
        if (methodName.equals("channelRead") && args != null && args.length >= 2) {
            Object packet = args[1];
            for (InboundListener listener : new ArrayList<>(connection.listeners.values())) {
                try {
                    if (listener.receive(packet)) return null;
                } catch (Throwable failure) {
                    plugin.getLogger().warning("A The Pit native packet subscriber failed: " + describe(failure));
                }
            }
            call(args[0], "fireChannelRead", packet);
            return null;
        }
        if (methodName.equals("exceptionCaught") && args != null && args.length >= 2) {
            call(args[0], "fireExceptionCaught", args[1]);
            return null;
        }
        if (methodName.equals("toString")) return name;
        if (methodName.startsWith("channel") && args != null && args.length >= 1) {
            String fire = "fire" + Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
            Object[] forwarded = args.length == 1 ? new Object[0] : java.util.Arrays.copyOfRange(args, 1, args.length);
            try {
                call(args[0], fire, forwarded);
            } catch (NoSuchMethodException ignored) {
            }
            return null;
        }
        if (method.getReturnType() == boolean.class) return false;
        return null;
    }

    private Object channel(Player player) throws Exception {
        Object handle = call(player, "getHandle");
        Object playerConnection = findField(handle.getClass(), "playerConnection").get(handle);
        Object networkManager = findField(playerConnection.getClass(), "networkManager").get(playerConnection);
        return findField(networkManager.getClass(), "channel").get(networkManager);
    }

    private String handlerName(UUID playerId) {
        return HANDLER_PREFIX + playerId.toString().replace("-", "");
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Object call(Object target, String methodName, Object... args) throws Exception {
        Method selected = null;
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterTypes().length != args.length) continue;
            boolean matches = true;
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && !wrap(method.getParameterTypes()[i]).isAssignableFrom(args[i].getClass())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                selected = method;
                break;
            }
        }
        if (selected == null) throw new NoSuchMethodException(target.getClass().getName() + "." + methodName);
        selected.setAccessible(true);
        return selected.invoke(target, args);
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private String describe(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getClass().getSimpleName() + (cause.getMessage() == null ? "" : ": " + cause.getMessage());
    }

    public interface InboundListener {
        boolean receive(Object packet) throws Exception;
    }

    private static final class Connection {
        private final Map<String, InboundListener> listeners = new ConcurrentHashMap<>();
        private volatile Object channel;
        private volatile Object handler;
        private volatile boolean ready;
    }
}
