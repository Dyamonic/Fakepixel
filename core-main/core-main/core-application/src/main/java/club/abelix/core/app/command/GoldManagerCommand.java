package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.discord.DiscordHook;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;

import java.awt.Color;
import java.time.Instant;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>GoldManagerCommand creation on 22.08.2022 at 23:54</p>
 */

@WithHelp
@NamedCommand("gold")
public class GoldManagerCommand {

    private final static String WEBHOOK = "https://discord.com/api/webhooks/1522491337847476254/1CddYfgvU4hzyFV3xzyteqTA8cKxqyhlpNr5C06Wq7P88cqTD-IveMjCZnz2oHrGplly";

    @SubcommandExecutor("give")
    @WithAlias("+")
    @WithAlias("add")
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.gold.give")
    public static void give(CoreExecutionSession session) throws CommandException {
        Gamer target = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadProperties();
            offline.updateDisplayName();
        });
        int amount = session.getIntArgument(1, "amount");

        target.addGold(amount);
        session.sendLocalizedMessage("core_command_gold_add", "%gamer%",
                target.getDisplayName(), "%amount%", amount, "%balance%", target.getGold());

        sendDiscordLog(session, target, "added", amount);

        ServerManager.IMP.broadcastStaff("command.staffchat", "gold_add_staff",
                "%target%", target.getName(),
                "%sender%", session.getGamer().getDisplayName(),
                "%amount%", amount,
                "%balance%", target.getGold()
        );
    }

    @SubcommandExecutor("take")
    @WithAlias("remove")
    @WithAlias("-")
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.gold.take")
    public static void take(CoreExecutionSession session) throws CommandException {
        Gamer target = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadProperties();
            offline.updateDisplayName();
        });
        int amount = session.getIntArgument(1, "amount");

        target.takeGold(amount);
        session.sendLocalizedMessage("core_command_gold_take", "%gamer%",
                target.getDisplayName(), "%amount%", amount, "%balance%", target.getGold());

        sendDiscordLog(session, target, "removed", amount);
        ServerManager.IMP.broadcastStaff("command.staffchat", "gold_took_staff",
                "%target%", target.getName(),
                "%sender%", session.getGamer().getDisplayName(),
                "%amount%", amount,
                "%balance%", target.getGold()
        );
    }

    @SubcommandExecutor("balance")
    @WithAlias("getbalacne")
    @WithAlias("check")
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.gold.balance")
    public static void balance(CoreExecutionSession session) throws CommandTranslationException {
        Gamer target = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadProperties();
            offline.updateDisplayName();
        });

        session.sendLocalizedMessage("core_command_gold_balance", "%gamer%",
                target.getDisplayName(), "%balance%", target.getGold());
    }

    @SubcommandExecutor("zeroing")
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.gold.zeroing")
    public static void zeroing(CoreExecutionSession session) throws CommandException {
        Gamer target = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadProperties();
            offline.updateDisplayName();
        });
        int amount = target.getGold();

        target.setProperty("Donate/Gold", 0);

        session.sendLocalizedMessage("core_command_gold_zeroing", "%gamer%",
                target.getDisplayName(), "%balance%", target.getGold());

        sendDiscordLog(session, target, "reset", amount);

        ServerManager.IMP.broadcastStaff("command.staffchat", "gold_reset_staff",
                "%target%", target.getName(),
                "%sender%", session.getGamer().getDisplayName()
        );
    }

    private static void sendDiscordLog(CoreExecutionSession session, Gamer target, String action, int amount) {
        DiscordHook hook = new DiscordHook(WEBHOOK);
        DiscordHook.EmbedObject embed = new DiscordHook.EmbedObject()
                .setTitle("Gold " + action)
                .setColor(new Color(255, 183, 0))
                .setTimestamp(Instant.now())
                .addField("Sender", session.getSender().getName(), true)
                .addField("Target", target.getName(), true)
                .addField("Amount", String.valueOf(amount), true);

        hook.setUsername("Gold logger");
        hook.addEmbed(embed);
        hook.execute();
    }

}
