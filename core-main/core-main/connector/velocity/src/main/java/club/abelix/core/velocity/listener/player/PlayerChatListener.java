package club.abelix.core.velocity.listener.player;

import club.abelix.commons.util.time.CooldownUtil;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.event.PlayerCoreCommandEvent;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.packet.play.client.CPlayerCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>PlayerChatListener creation on 02.03.2023 at 02:09</p>
 */

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PlayerChatListener {

    StarshipVelocity starship;

    @Subscribe
    public void onChat(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }

        val gamer = Gamer.getGamer(player);

        if (gamer == null) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            return;
        }

        if (!gamer.isRegistered() || !gamer.isLoggedIn()) {
            return;
        }

        val playerId = gamer.getPlayerId();

        if (!gamer.hasPermission("chat.cooldown.bypass")) {
            if (CooldownUtil.hasCooldown(playerId + "_command_execute")) {
                gamer.sendLocalizedMessage("filter_fast_command_message");
                event.setResult(CommandExecuteEvent.CommandResult.denied());
                return;
            }

            CooldownUtil.addCooldown(playerId + "_command_execute", 1500, TimeUnit.MILLISECONDS);
        }

        String command = event.getCommand();

        if (!starship.getCommands().contains(command.split(" ")[0])) {
            return;
        }

        event.setResult(CommandExecuteEvent.CommandResult.denied());

        PlayerCoreCommandEvent coreCommandEvent = new PlayerCoreCommandEvent(player, command);
        starship.getProxy().getEventManager().fireAndForget(coreCommandEvent);

        if (coreCommandEvent.isCancelled()) {
            return;
        }

        Connection.sendPacket(new CPlayerCommand(gamer.getPlayerId(), "/" + command));

        log.info(
                "Player {}({}) executed command: /{}",
                player.getUsername(),
                player.getCurrentServer().get().getServerInfo().getName(),
                command
        );
    }
}
