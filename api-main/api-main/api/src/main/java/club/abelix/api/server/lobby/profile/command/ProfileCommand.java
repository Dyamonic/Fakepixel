package club.abelix.api.server.lobby.profile.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.profile.ProfileGuisFactory;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.api.v1.bean.BeanService;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import lombok.val;

@NamedCommand("profile")
@IgnoreLogs
public class ProfileCommand {

    @Autowired
    BeanService beanService;

    public ProfileCommand() {
        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);
    }

    @Executor
    public void execute(BukkitExecutionSession session) throws CommandTranslationException {
        val gamer = session.getGamer();

        if (session.getArgumentCount() != 1) {
            beanService.withBean(ProfileGuisFactory.class, factory -> {
                factory.openProfileGui(gamer);
            });

            return;
        }

        beanService.withBean(ProfileGuisFactory.class, factory -> {
            factory.openPlayerProfileGui(gamer, Gamer.getOrCreate(session.getArgument(0), Gamer::load));
        });
    }

}