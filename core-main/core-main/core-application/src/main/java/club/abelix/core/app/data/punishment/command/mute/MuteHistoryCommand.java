package club.abelix.core.app.data.punishment.command.mute;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.ParsedArgs;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.FallbackExecutor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.HastebinUtil;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.commons.util.json.JSON;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;
import lombok.val;

import java.util.LinkedList;
import java.util.List;

@WithAlias("mhist")
@WithAlias("mhistory")
@WithAlias("muteh")
@NamedCommand("mutehistory")
public final class MuteHistoryCommand {

    @Dependency
    private PunishmentManager manager;

    @WithCondition("disableConsole")
    @WithPermission("command.mutehistory.remove")
    @WithCondition("minArgs: 1")
    @SubcommandExecutor("remove")
    public static void remove(CoreExecutionSession session) throws CommandException {
        int muteId = session.getIntArgument(0, "ID");

        MuteMeta meta = PunishmentManager.INSTANCE.findMuteMeta(muteId)
                .orElseThrow(() -> new CommandTranslationException("muteidinfo_empty_result"));

        PunishmentManager.INSTANCE.removeMuteMeta(meta);

        ServerManager.IMP.broadcastStaff("command.staffchat", "mutehistory_mute_removed",
                "%admin%", session.getDisplayName(), "%id%", muteId);
    }

    @SubcommandExecutor("show")
    @Async
    @FallbackExecutor
    @Tabcomplete("[playerName]")
    @WithPermission("command.mutehistory.show")
    @WithCondition("minArgs: 1")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        val start = System.currentTimeMillis();

        String targetName = session.getArgument(0);
        val parsed = ParsedArgs.parse(session.getArgumentString(1));

        Gamer target = Gamer.getOfflineGamer(targetName, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
        });

        if (target == null) {
            throw new CommandTranslationException("player_not_found");
        }

        List<MuteMeta> history = manager.getMuteHistory(target.getPlayerId());
        val showRaw = parsed.hasFlag("raw");

        Language language = session.getLanguage();

        if (history.isEmpty()) {
            throw new CommandTranslationException("mutehistory_never_was_muted", "%target%", target.getDisplayName());
        }

        List<String> result = new LinkedList<>();

        result.add(language.getMessage("mutehistory_result", "%target%", target.getDisplayName()));

        for (MuteMeta muteMeta : history) {
            val text = showRaw ?
                    manager.getMuteJsonText(muteMeta, session.getLanguage(), true)
                    :
                    manager.getMuteHastebinText(muteMeta, session.getLanguage(), true);

            result.add(text + "\n");
        }

        if (showRaw) {
            result.forEach(session::sendMessage);
        } else {
            val text = String.join("\n", result);
            HastebinUtil.paste(ChatColor.stripColor(text))
                    .whenComplete((url, throwable) -> {
                        if (throwable != null) {
                            session.sendLocalizedMessage("mute_history_load_failed");
                            throwable.printStackTrace();
                            return;
                        }

                        val message = language.getMessage("mute_history_loaded",
                                "%time%", System.currentTimeMillis() - start + "ms", "%url%", url);

                        session.sendMessage(String.format(JSON.CLICK_URL_LAYOUT, message, url));
                    });
        }

    }
}
