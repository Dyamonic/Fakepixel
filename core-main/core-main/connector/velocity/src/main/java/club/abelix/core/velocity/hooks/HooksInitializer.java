/*
 * Copyright (C) 2021 - 2023 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package club.abelix.core.velocity.hooks;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.connection.backend.BackendPlaySessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author hevav
 */
public class HooksInitializer {

    @SuppressWarnings("unchecked")
    public static void init(ProxyServer server) {
        try {
            var cm = MethodHandles.privateLookupIn(server.getClass(), MethodHandles.lookup())
                    .findGetter(server.getClass(), "cm", ConnectionManager.class);

            var serverChannelInitializer = ((ConnectionManager) cm.invoke(server)).getServerChannelInitializer();
            var initializerField = serverChannelInitializer.getClass().getDeclaredField("initializer");
            initializerField.setAccessible(true);

            PluginMessageHook.SERVER_CONNECTION_FIELD = MethodHandles
                    .privateLookupIn(BackendPlaySessionHandler.class, MethodHandles.lookup())
                    .findGetter(BackendPlaySessionHandler.class, "serverConn", VelocityServerConnection.class);

            var versionsField = MethodHandles.privateLookupIn(StateRegistry.PacketRegistry.class, MethodHandles.lookup())
                    .findGetter(StateRegistry.PacketRegistry.class, "versions", Map.class);

            var packetIdToSupplierField = MethodHandles
                    .privateLookupIn(StateRegistry.PacketRegistry.ProtocolRegistry.class, MethodHandles.lookup())
                    .findGetter(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetIdToSupplier", IntObjectMap.class);

            var packetClassToIdField = MethodHandles
                    .privateLookupIn(StateRegistry.PacketRegistry.ProtocolRegistry.class, MethodHandles.lookup())
                    .findGetter(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetClassToId", Object2IntMap.class);

            var hooks = new ArrayList<PacketHook>();
            hooks.add(new PluginMessageHook());

            BiConsumer<? super ProtocolVersion, ? super StateRegistry.PacketRegistry.ProtocolRegistry> consumer = (version, registry) -> {
                try {
                    var packetIdToSupplier = (IntObjectMap<Supplier<? extends MinecraftPacket>>) packetIdToSupplierField.invoke(registry);

                    var packetClassToId = (Object2IntMap<Class<? extends MinecraftPacket>>) packetClassToIdField.invoke(registry);

                    hooks.forEach(hook -> {
                        int packetId = packetClassToId.getInt(hook.getType());
                        packetClassToId.put(hook.getHookClass(), packetId);
                        packetIdToSupplier.remove(packetId);
                        packetIdToSupplier.put(packetId, hook.getHook());
                    });
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };

            var clientboundGetter = MethodHandles.privateLookupIn(StateRegistry.class, MethodHandles.lookup())
                    .findGetter(StateRegistry.class, "clientbound", StateRegistry.PacketRegistry.class);

            var serverboundGetter = MethodHandles.privateLookupIn(StateRegistry.class, MethodHandles.lookup())
                    .findGetter(StateRegistry.class, "serverbound", StateRegistry.PacketRegistry.class);

            var playClientbound = (StateRegistry.PacketRegistry) clientboundGetter.invokeExact(StateRegistry.PLAY);
            var handshakeServerbound = (StateRegistry.PacketRegistry) serverboundGetter.invokeExact(StateRegistry.HANDSHAKE);

            ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versionsField.invokeExact(playClientbound)).forEach(consumer);
            ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versionsField.invokeExact(handshakeServerbound)).forEach(consumer);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


}
