package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.data.purchase.PurchaseSqlHandler;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.data.stat.StatsSqlHandler;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import com.google.common.collect.Iterables;
import lombok.val;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Unidentified Person
 */
@NamedCommand("wipestat")
public class WipeStatCommand {

    private static final LocalDate PERMANENT_CLEAR_START = LocalDate.ofEpochDay(0);

    @Executor
    @WithCondition("minArgs: 1")
    @WithPermission("command.wipe.stat")
    public static void invoke(CoreExecutionSession session) throws CommandException {
        Gamer target = session.getOfflineGamer(0, gamer -> {
            gamer.loadPlayerId();
            gamer.loadRanks();
            gamer.loadPrefix();
        });
        boolean isOnline = target.isOnline();

        Collection<StatsMode> modes;

        if (session.getArgumentCount() > 1) {
            StatsMode mode = StatsManager.INSTANCE.tryGetMode(session.getArgument(1));

            if (mode == null) {
                throw new CommandTranslationException("wipestat_mode_not_found");
            }

            modes = Collections.singleton(mode);
        } else {
            modes = StatsManager.INSTANCE.getModes();
        }

        if (isOnline) {
            target.localizedKick("wipestat_kick");
        }

        for (StatsMode mode : modes) {
            StatsSqlHandler.INSTANCE.deleteStats(target.getPlayerId(), mode);
        }

        PurchaseSqlHandler.INSTANCE.removeRecentPurchases(
                target.getPlayerId(), PERMANENT_CLEAR_START, GameCurrency.COINS
        );

        if (modes.size() == 1) {
            val modeName = Iterables.getOnlyElement(modes).getDisplayName();

            ServerManager.IMP.broadcastStaff("wipestat_done_single",
                    "%name%", target.getDisplayName(),
                    "%admin%", session.getDisplayName(),
                    "%mode%", modeName);

            session.sendLocalizedMessage("wipestat_result_single",
                    "%target%", target.getDisplayName(),
                    "%mode%", modeName
            );
        } else {
            ServerManager.IMP.broadcastStaff("wipestat_done_all",
                    "%name%", target.getDisplayName(),
                    "%admin%", session.getDisplayName());

            session.sendLocalizedMessage("wipestat_result_all",
                    "%target%", target.getDisplayName()
            );
        }
    }
}
