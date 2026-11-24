package club.abelix.api.server.lobby.profile.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.server.lobby.profile.ProfileGuisFactory;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.api.v1.bean.BeanService;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;

@NamedCommand("awards")
@IgnoreLogs
public class AwardsCommand {

    @Autowired
    BeanService beanService;

    public AwardsCommand() {
        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);
    }

    @Executor
    @WithCondition("disableConsole")
    public void executor(BukkitExecutionSession session) {
        beanService.withBean(ProfileGuisFactory.class, factory -> factory.openLevelRewardsGui(session.getGamer(), null));
    }
}
