package club.abelix.api.base.roleplay;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.roleplay.event.GamerSitEvent;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.cooldown.WithCooldown;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.type.mode.Uncommon;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@NamedCommand("sit")
@WithAlias("сидеть")
@IgnoreLogs
public class SitCommand {

    @WithCooldown(8_000)
    @WithPermission("lobby.sit")
    @WithCondition("disableConsole")
    @Executor
    public static void executor(BukkitExecutionSession session) throws CommandTranslationException {
        Player player = session.getPlayer();
        Gamer gamer = session.getGamer();

        if (CoreUtil.getMode() == Uncommon.BUILDFFA) {
            player.sendMessage(ChatColor.RED + "You cannot sit here!");
            return;
        }

        if (SitManager.INSTANCE.isSitting(gamer)) {
            SitManager.INSTANCE.eject(gamer);
            return;
        }

        GamerSitEvent event = new GamerSitEvent(gamer);
        HeartBeat.callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        if (!player.isOnGround()) {
            throw new CommandTranslationException("sit_not_on_ground");
        }

        SitManager.INSTANCE.sit(gamer);
    }

}
