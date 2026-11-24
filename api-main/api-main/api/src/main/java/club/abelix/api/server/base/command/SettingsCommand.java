package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.server.lobby.profile.ProfileGuisFactory;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.api.v1.bean.BeanService;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;

@NamedCommand("settings")
@WithAlias("настройки")
@WithAlias("set")
@WithAlias("pref")
@WithAlias("preferences")
@IgnoreLogs
public class SettingsCommand {

    @Autowired
    BeanService beanService;

    public SettingsCommand() {
        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);
    }

    @Executor
    @WithCondition("disableConsole")
    public void execute(BukkitExecutionSession session) {
        beanService.withBean(ProfileGuisFactory.class, f -> f.openSettingsGui(session.getGamer(), null));
    }

}
