package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.server.lobby.profile.ProfileGuisFactory;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandMessageException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.data.stat.StatsField;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.api.v1.bean.BeanService;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;

@WithAlias("statistics")
@WithAlias("statistic")
@NamedCommand("stats")
@IgnoreLogs
public class StatsCommand {

    @Autowired
    BeanService beanService;

    public StatsCommand() {
        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);
    }

    @Executor
    @WithPermission("command.stats.view")
    public void execute(BukkitExecutionSession session) throws CommandMessageException {
        Gamer gamer = session.getGamer();
        Gamer target = null;

        if (session.getArgumentCount() > 0) {
            if (!session.getRanks().hasPermission("stats.others")) {
                throw new CommandTranslationException("stats_unavailable_group");
            }

            target = Gamer.getOrCreate(session.getArgument(0), offlineGamer -> {
                offlineGamer.loadPlayerId();
                offlineGamer.loadRanks();
                offlineGamer.loadPrefixAndUpdateName();
                offlineGamer.loadStats();
                offlineGamer.loadProperties();
                offlineGamer.loadSkull();
            });

            if (target == null || !target.hasPlayerId()) {
                throw new CommandTranslationException("player_not_found");
            }

            if (target == gamer) {
                target = null;
            }
        }

        if (CoreUtil.isMaster() || CoreUtil.isUncommon()) {
            StatsMode statsMode = gamer.getStatsMode();

            if (statsMode == null) {
                return;
            }

            Gamer statisticGamer = target == null ? gamer : target;

            gamer.sendLocalizedMessage("stats_result",
                    "%name%", statisticGamer.getRealDisplayName(), "%mode%", statsMode.getDisplayName());

            for (StatsField statsField : statsMode.getFields()) {
                if (!statsField.hasDescription()) {
                    continue;
                }

                gamer.sendLocalizedMessage("stats_row",
                        "%description%", statsField.getDescription(gamer.getLanguage()),
                        "%stat%", statsField.format(statisticGamer.getLifetimeStat(statsField))
                );
            }
        } else {
            Gamer finalTarget = target;
            beanService.withBean(ProfileGuisFactory.class, f -> f.openStatsGui(gamer, finalTarget, null));
        }
    }
}
