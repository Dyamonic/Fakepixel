package club.abelix.api.server.base.command;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.language.Lang;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@NamedCommand("testcustomname")
@RequiredArgsConstructor
public class TestCustomNameCommand {

    private final AbstractPlugin plugin;

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("command.test.custom.name")
    public void execute(BukkitExecutionSession session) {
        Player player = session.getPlayer();

        session.getLanguage().setTranslation("test_localization_key", "§eTime: §b%test%");

        Hologram hologram = new Hologram(player.getLocation().clone().add(0, 1, 0));
        hologram.addTextLine(Lang.of("test_localization_key").set("%test%", 0).compile());
        hologram.addReceiver(player);

        new AbstractTask(plugin) {

            private int secondsLived = 0;

            @Override
            protected void run() {
                hologram.modifyTextLine(1, Lang.of("test_localization_key")
                        .set("%test%", secondsLived).compile());

                if (secondsLived++ == 60) {
                    hologram.remove();
                    cancel();
                }
            }
        }.timer(100, 20);

        player.sendMessage("§eTest hologram created!");
    }

}
