package club.abelix.api.server.base.command;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.nms.entity.type.player.FakeEntityCorpse;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@NamedCommand("testfakecorpse")
@RequiredArgsConstructor
public class TestCorpseCommand {

    private final AbstractPlugin plugin;

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("command.test.corpse")
    public void execute(BukkitExecutionSession session) {
        Gamer gamer = session.getGamer();
        Player player = gamer.getPlayer();

        FakeEntityCorpse corpse = FalconAPI.getNmsApi().getFakeEntityApi().createCorpse(
                gamer.getPlayer().getLocation(), gamer.getSkin()
        );
        corpse.addReceiver(player);

        new AbstractTask(plugin) {
            @Override
            protected void run() {
                if (player.isOnline()) {
                    session.sendPrefixedMessage("Corpse removed");
                }

                corpse.remove();
            }
        }.later(20 * 30);

        session.sendPrefixedMessage("Created test corpse!");
    }

}
