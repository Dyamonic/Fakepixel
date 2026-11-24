package club.abelix.core.velocity.listener;

import club.abelix.commons.util.time.CooldownUtil;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.packet.play.client.CTabcompleteRequest;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.AvailableCommandsPacket;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequestPacket;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponsePacket;
import io.github._4drian3d.vpacketevents.api.event.PacketReceiveEvent;
import io.github._4drian3d.vpacketevents.api.event.PacketSendEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TabCompleteListener {

    StarshipVelocity core;

    private static LiteralCommandNode createDummyNode(final String command) {
        return LiteralArgumentBuilder.<CommandSource>literal(command)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("args", StringArgumentType.greedyString())
                        .suggests(new AvailableCommandsPacket.ProtocolSuggestionProvider("minecraft:ask_server"))
                        .build())
                .build();
    }

    @Subscribe
    public void onPacketSending(final @NotNull PacketSendEvent event) {
        val packet = event.getPacket();

        if (!(packet instanceof AvailableCommandsPacket availableCommands)) return;

        handleAvailableCommands(event, availableCommands);
    }

    @Subscribe
    public void onPacketReceiving(final @NotNull PacketReceiveEvent event) {
        val packet = event.getPacket();

        if (!(packet instanceof TabCompleteRequestPacket request)) return;

        handleTabComplete(event, request);
    }

    private void handleAvailableCommands(
            final @NotNull PacketSendEvent ignored,
            final @NotNull AvailableCommandsPacket availableCommands
    ) {
        val rootNode = availableCommands.getRootNode();

        for (val command : core.getCommands()) {
            rootNode.addChild(createDummyNode(command));
        }
    }

//    public Set<String> matchCommands(final String needle) {
//        val proxy = core.getProxy();
//        val pluginManager = proxy.getPluginManager();
//
//        val coreCommands = core.getCommands().stream();
//
//        val bungeeCommands = proxy.getCommandManager().getAliases()
//                .stream();
//
//        return Stream.concat(coreCommands, bungeeCommands)
//                .map("/"::concat)
//                .filter(haystack -> haystack.regionMatches(true, 0, needle, 0, needle.length()))
//                .collect(Collectors.toSet());
//    }

    private void handleTabComplete(
            final @NotNull PacketReceiveEvent event,
            final @NotNull TabCompleteRequestPacket packet
    ) {
        val player = event.getPlayer();
        val gamer = Gamer.getGamer(player);

        if (gamer == null || !gamer.isLoggedIn()) {
            return;
        }

        val command = packet.getCommand();
        val commandSeparator = command.indexOf(' ');

        val isCommand = !command.isEmpty() && command.charAt(0) == '/';
        if (!isCommand || commandSeparator == -1) {
            return;
        }

        // Игрок ввел слишком мало
        if (command.length() < 2) {
            return;
        }

        // для начала проверим, есть ли команда, которую он собирается табкомплитить
        // если нет - то код ниже не выполняем
        if (!core.getCommands().contains(command.substring(1, commandSeparator).toLowerCase())) {
            return;
        }

        // В каком момент игрок нажал TAB, фильтруем
        // Для уменьшения количества пакетов на кор
        String[] parts = command.split(" +");
        String filter = command.endsWith(" ") ?
                // игрок нажал tab и ожидает список всех игроков: /cmd<SP><TAB>
                "" :
                // игрок нажал tab и ожидает список игроков с префиксом: /cmd<SP>pfx...<TAB>
                parts[parts.length - 1];

        if (!filter.isEmpty() && parts.length == 1) {
            // игрок нажал tab сразу после команды: /cmd<TAB>
            return;
        }

        // табкомплитом можно спамить, а на онлайне
        // если много игроков будут так делать - кору будет не очень
        // приятно, поэтому добавим кулдаун
        val cooldownKey = player.getUsername().concat("_tabcomplete");

        if (CooldownUtil.hasCooldown(cooldownKey)) {
            return;
        }

        CooldownUtil.addCooldown(cooldownKey, 300, TimeUnit.MILLISECONDS);

        // Отменяем, и отправялем на кор
        event.setResult(ResultedEvent.GenericResult.denied());
        Connection.awaitPacket(
                new CTabcompleteRequest(gamer.getPlayerId(), command)
        ).thenAccept(response -> {
            if (response.getSuggestions().isEmpty()) {
                return;
            }

            int startPos = packet.getCommand().lastIndexOf(' ') + 1;
            if (startPos > 0) {
                val responsePacket = new TabCompleteResponsePacket();

                responsePacket.setTransactionId(packet.getTransactionId());
                responsePacket.setStart(startPos);
                responsePacket.setLength(packet.getCommand().length() - startPos);

                responsePacket.getOffers().addAll(response.getSuggestions().stream()
                        .map(TabCompleteResponsePacket.Offer::new).toList());

                ((ConnectedPlayer) player).getConnection().write(responsePacket);
            }
        });
    }
}
