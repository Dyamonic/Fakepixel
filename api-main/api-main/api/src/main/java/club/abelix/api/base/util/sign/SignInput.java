package club.abelix.api.base.util.sign;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.commons.version.GameVersion;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import lombok.val;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author WhileInside
 */
public class SignInput extends YListener {

    private static final Map<Player, Data> inputHandlers
            = new HashMap<>();

    public SignInput(Plugin plugin) {
        super(plugin);

        PacketAdapter signUpdateListener = new PacketAdapter(plugin, PacketType.Play.Client.UPDATE_SIGN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                val packet = event.getPacket();

                val blockPosition = packet.getBlockPositionModifier()
                        .read(0);

                val data = inputHandlers.get(event.getPlayer());

                String[] lines;

                // на 1.8 в пакете почему-то ChatComponent[]
                if (FalconAPI.getNmsApi().getProtocolNumber()
                        == GameVersion.MINECRAFT_1_8.getVersion()) {

                    val signLines = packet.getChatComponentArrays()
                            .read(0);

                    lines = new String[signLines.length];

                    for (int i = 0; i < signLines.length; i++) {
                        val components = ComponentSerializer.parse(signLines[i].getJson());

                        lines[i] = TextComponent.toPlainText(components);
                    }
                } else {
                    lines = packet.getStringArrays().read(0);
                }

                if (data != null && data.position.equals(blockPosition)) {
                    val player = event.getPlayer();

                    completeInput(player, lines);
                }
            }
        };

        PacketLibraryApi.PROTOCOL_MANAGER.addPacketListener(signUpdateListener);
    }

    public static void completeInput(Player player, String[] value) {
        Data handler = inputHandlers.remove(player);

        if (handler != null) {
            handler.result.complete(value);
        }
    }

    public static CompletableFuture<String[]> openInput(Player player) {
        return openInput(Gamer.getGamer(player));
    }

    public static CompletableFuture<String[]> openInput(Gamer gamer) {
        Player player = gamer.getPlayer();
        PacketLibraryApi api = FalconAPI.getNmsApi().getPacketLibraryApi();

        return inputHandlers.computeIfAbsent(player, x -> {
            Location playerLocation = player.getLocation();
            Block block = playerLocation.getBlock();

            BlockPosition pos = new BlockPosition(playerLocation.toVector());

            PacketContainer signEditor = new PacketContainer(PacketType.Play.Server.OPEN_SIGN_EDITOR);
            signEditor.getBlockPositionModifier().write(0, pos);

            CompletableFuture<String[]> future = new CompletableFuture<>();

            if (gamer.getVersion().getVersion() >= GameVersion.MINECRAFT_1_13.getVersion()) {
                player.sendBlockChange(playerLocation, Material.WALL_SIGN, (byte) 0);

                future.whenComplete(
                        (result, cause) -> player.sendBlockChange(playerLocation, block.getType(), block.getData())
                );
            }

            api.sendPacket(player, signEditor);

            return new Data(future, pos);
        }).result;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inputHandlers.remove(event.getPlayer());
    }

    private static class Data {
        private final CompletableFuture<String[]> result;
        private final BlockPosition position;

        public Data(CompletableFuture<String[]> result, BlockPosition position) {
            this.result = result;
            this.position = position;
        }
    }

}
