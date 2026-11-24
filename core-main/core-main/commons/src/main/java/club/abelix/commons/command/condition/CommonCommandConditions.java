package club.abelix.commons.command.condition;

import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.remote.RemoteRankService;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.language.Language;
import club.abelix.di.core.DiStaticAccess;
import club.abelix.di.core.StandardDiFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author WhileInside
 */
@Slf4j
public final class CommonCommandConditions {

    private CommonCommandConditions() {
        throw new UnsupportedOperationException();
    }

    @ConditionProvider(value = "permission", priority = -1)
    public static void permissionCondition(ExecutionSession session, String meta)
            throws CommandTranslationException {

        val disallowedPermission = meta + ".disallowed";
        AtomicBoolean disallowed = new AtomicBoolean(false);

        DiStaticAccess.BEAN_SERVICE.withBean(RanksService.class, rankService -> {
            disallowed.set(session.getRanks().stream()
                    .map(r -> rankService.findRawById(r.rank().getId()))
                    .flatMap(Optional::stream)
                    .map(RemoteRankService.RankRecord::entity)
                    .anyMatch(p -> p.getName().equalsIgnoreCase(disallowedPermission)));
        });

        if (disallowed.get()) {
            throw new CommandTranslationException("not_allowed");
        }

        if (!session.getSender().hasPermission(meta)) {
            throw new CommandTranslationException("no_permission", "%permission%", meta);
        }
    }

    @ConditionProvider(value = "group", priority = -1)
    public static void groupCondition(ExecutionSession session, String meta)
            throws CommandTranslationException {

        log.info("This condition is deprecated, use 'permission' condition instead");
        throw new CommandTranslationException("command_condition_deprecated");
    }

    @ConditionProvider(value = "minArgs", priority = 0)
    public static void minArgsCondition(ExecutionSession session, String meta)
            throws CommandTranslationException {
        if (session.getArgumentCount() < Integer.parseInt(meta)) {
            Language language = session.getLanguage();

            String localizationKey = session.getExecutor().getLocalizationKey();
            String usageKey = localizationKey + "_usage";

            if (language.isMessageTranslated(usageKey)) {
                throw new CommandTranslationException(
                        "command_usage",
                        "%usage%", session.getExecutor().toString() + " " + language.getMessage(usageKey)
                );
            } else {
                throw new CommandTranslationException("command_incorrect_usage");
            }
        }
    }

    @ConditionProvider(value = "haveProperty", priority = 0)
    public static void havePropertyCondition(ExecutionSession session, String meta)
            throws CommandTranslationException {
        String[] propertyLst = meta.split(", ");
        IGamer gamer = session.getGamer();
        for (String property : propertyLst) {
            if (gamer.getProperty(property) == 0) {
                throw new CommandTranslationException("command_not_access");
            }
        }
    }

    @ConditionProvider(value = "disableConsole", priority = 0)
    public static void disableConsoleCondition(ExecutionSession session, String meta)
            throws CommandTranslationException {
        if (session.isConsole()) {
            throw new CommandTranslationException("command_unavailable_console");
        }
    }

    @ConditionProvider(value = "disableStaff", priority = 0)
    public static void disableStaffCondition(ExecutionSession session, String meta)
            throws CommandTranslationException {

        val gamer = session.getGamer();
        val ranks = gamer.getRanks();

        if (gamer.hasPermission("core.staff.bypass")) return;

        if (ranks.hasPermission("core.staff")) {
            throw new CommandTranslationException("command_not_access");
        }
    }

    @ConditionProvider(value = "consoleOnly", priority = 0)
    public static void consoleOnlyCondition(ExecutionSession session, String meta)
            throws CommandTranslationException {
        if (!session.isConsole()) {
            throw new CommandTranslationException("command_available_only_from_console");
        }
    }
}
