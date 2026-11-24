package club.abelix.core.app.data.punishment.command.ban;

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
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.commons.util.HastebinUtil;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.commons.util.json.JSON;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.punishment.PunishmentManager;
import lombok.val;

import java.util.LinkedList;
import java.util.List;

@WithAlias("bhist")
@WithAlias("banhist")
@WithAlias("banh")
@NamedCommand("banhistory")
public final class BanHistoryCommand {

    @Dependency
    private PunishmentManager manager;

    @WithCondition("disableConsole")
    @WithPermission("command.banhistory.remove")
    @WithCondition("minArgs: 1")
    @SubcommandExecutor("remove")
    public static void remove(CoreExecutionSession session) throws CommandException {
        int banId = session.getIntArgument(0, "ID");

        BanMeta meta = PunishmentManager.INSTANCE.findBanMeta(banId)
                .orElseThrow(() -> new CommandTranslationException("banidinfo_empty_result"));

        PunishmentManager.INSTANCE.removeBanMeta(meta);

        ServerManager.IMP.broadcastStaff("command.staffchat", "banhistory_ban_removed",
                "%admin%", session.getDisplayName(), "%id%", banId);
    }

    @SubcommandExecutor("show")
    @FallbackExecutor
    @Tabcomplete("[playerName]")
    @WithPermission("command.banhistory.show")
    @WithCondition("minArgs: 1")
    public void history(CoreExecutionSession session) throws CommandTranslationException {
        val start = System.currentTimeMillis();

        boolean full = session.getSender().hasPermission("command.banhistory.show");

        String targetName = session.getArgument(0);
        val parsed = ParsedArgs.parse(session.getArgumentString(1));

        boolean isIp = Patterns.isIPV4(targetName);

        if (isIp && !full) {
            throw new CommandTranslationException("banhistory_ip_unavailable");
        }

        List<BanMeta> history = manager.getBanHistory(targetName);
        List<String> result = new LinkedList<>();

        if (history.isEmpty()) {
            throw new CommandTranslationException("banhistory_" + (isIp ? "ip" : "name") + "_never_was_banned", "%target%", targetName);
        }

        val language = session.getLanguage();
        val showRaw = parsed.hasFlag("raw");

        result.add(language.getMessage("banhistory_" + (isIp ? "ip" : "name") + "_result", "%target%", targetName));

        int count = 0;

        for (BanMeta banMeta : history) {
            val text = showRaw ?
                    manager.getBanJsonText(banMeta, session.getLanguage(), true, full)
                    :
                    manager.getBanHastebinText(banMeta, session.getLanguage(), true, full);

            result.add(text + "\n");

            if (banMeta.getRemovedPunishmentName() == null && banMeta.isExpired()) {
                count++;
            }
        }

        result.add(language.getMessage("banhistory_ban_count", "%count%", count));


        if (showRaw) {
            result.forEach(session::sendMessage);
        } else {
            val text = String.join("\n", result);
            HastebinUtil.paste(ChatColor.stripColor(text))
                    .whenComplete((url, throwable) -> {
                        if (throwable != null) {
                            session.sendLocalizedMessage("ban_history_load_failed");
                            throwable.printStackTrace();
                            return;
                        }

                        val message = language.getMessage("ban_history_loaded",
                                "%time%", System.currentTimeMillis() - start + "ms", "%url%", url);

                        session.sendMessage(String.format(JSON.CLICK_URL_LAYOUT, message, url));
                    });
        }
    }
}
