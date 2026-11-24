package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.server.base.shop.ShopGuiFactory;
import club.abelix.api.server.base.shop.ShopUtil;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;

@NamedCommand("donate")
@WithAlias("donat")
@WithAlias("ranks")
@WithAlias("store")
@WithAlias("shop")
@WithAlias("buyrank")
@WithAlias("донат")
@IgnoreLogs
public class DonateCommand {

    @WithCondition("disableConsole")
    @Executor
    public static void executor(BukkitExecutionSession session) {
        DiBukkitPlatform.withBeanOrElse(
                ShopGuiFactory.class,
                bean -> bean.open(session.getGamer(), null),
                () -> ShopUtil.open(session.getGamer())
        );
    }
}
