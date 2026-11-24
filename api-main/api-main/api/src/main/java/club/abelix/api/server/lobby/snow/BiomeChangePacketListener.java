package club.abelix.api.server.lobby.snow;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import com.comphenix.protocol.events.PacketContainer;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk.ChunkMap;

import java.lang.reflect.Field;

public class BiomeChangePacketListener extends PacketAdapter {

    private static Set<String> snowEnabledPlayers = new HashSet<>();
    private Plugin plugin;

    public BiomeChangePacketListener(Plugin plugin) {
        super(plugin, PacketType.Play.Server.MAP_CHUNK);
        this.plugin = plugin;
    }

    public static void toggleSnowForPlayer(Player player) {
        if (snowEnabledPlayers.contains(player.getName())) {
            snowEnabledPlayers.remove(player.getName());
        } else {
            snowEnabledPlayers.add(player.getName());
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.MAP_CHUNK) {
            Player player = event.getPlayer();

            if (snowEnabledPlayers.contains(player.getName())) {
                PacketContainer packet = event.getPacket();

                try {
                    // Получаем оригинальный пакет NMS
                    PacketPlayOutMapChunk nmsPacket = (PacketPlayOutMapChunk) packet.getHandle();

                    // Получаем поле c - это ChunkMap
                    Field chunkMapField = nmsPacket.getClass().getDeclaredField("c");
                    chunkMapField.setAccessible(true);
                    ChunkMap chunkMap = (ChunkMap) chunkMapField.get(nmsPacket);

                    // Получаем данные биомов из ChunkMap
                    Field biomeDataField = chunkMap.getClass().getDeclaredField("biomeData");
                    biomeDataField.setAccessible(true);
                    byte[] biomeData = (byte[]) biomeDataField.get(chunkMap);

                    if (biomeData != null && biomeData.length == 256) {
                        // Изменяем данные биомов на снежный биом (ID 12 - Ice Plains)
                        for (int i = 0; i < biomeData.length; i++) {
                            biomeData[i] = (byte) 12;
                        }

                        // Устанавливаем изменённые данные обратно в ChunkMap
                        biomeDataField.set(chunkMap, biomeData);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
