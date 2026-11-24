package club.abelix.core.whitelist.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.command.tabcomplete.AbstractTabcompleter;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.command.tabcomplete.TabcompleteManager;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.whitelist.WhitelistCorePlugin;
import club.abelix.core.whitelist.WhitelistMode;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>WhitelistCommand creation on 21.11.2022 at 19:53</p>
 */

@RequiredArgsConstructor
@WithHelp
@NamedCommand("whitelist")
@WithAlias("wl")
public class WhitelistCommand {

    static {
        TabcompleteManager.IMP.addCompleter("whitelistMode", new AbstractTabcompleter() {
            @Override
            public List<String> complete(ICommandSender sender, String label) {
                return any(label, Arrays.stream(WhitelistMode.values()).map(WhitelistMode::name));
            }
        });
    }

    private final WhitelistCorePlugin whitelistCorePlugin;

    @SubcommandExecutor("switch")
    @Tabcomplete("[whitelistMode]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.whitelist.switch")
    public void switchMode(CoreExecutionSession session) {
        WhitelistMode newMode = WhitelistMode.valueOf(session.getArgument(0));

        if (newMode == this.whitelistCorePlugin.getCurrentlyMode()) {
            session.sendLocalizedMessage("whitelist_switch_mode_already", "%mode%", newMode.name());
            return;
        }

        this.whitelistCorePlugin.switchMode(newMode);
        session.sendLocalizedMessage("whitelist_switch_mode_success", "%newmode%", newMode.name());

    }

    @SubcommandExecutor("scheduleswitch")
    @WithAlias("schedule")
    @WithAlias("ss")
    @Tabcomplete("[whitelistMode]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.whitelist.scheduleswitch")
    public void scheduleSwitch(CoreExecutionSession session) throws CommandException {
        WhitelistMode newMode = WhitelistMode.valueOf(session.getArgument(0));
        int timerSeconds = session.getIntArgument(1, "timerSeconds");

        this.whitelistCorePlugin.timerSwitchMode(newMode, timerSeconds);

        session.sendLocalizedMessage("whitelist_switch_mode_success_timer", "%newmode%", newMode.name(),
                "%timerSeconds%", timerSeconds);


    }

    @SubcommandExecutor("disable")
    @WithPermission("command.whitelist.disable")
    public void disableWL(CoreExecutionSession session) {
        this.whitelistCorePlugin.switchMode(WhitelistMode.DISABLED);

        session.sendLocalizedMessage("whitelist_switch_mode_success", "%newmode%", WhitelistMode.DISABLED.name());
    }

    @SubcommandExecutor("status")
    @WithPermission("command.whitelist.status")
    public void getStatus(CoreExecutionSession session) {
        session.sendLocalizedMessage("whitelist_currently_mode", "%mode%", this.whitelistCorePlugin.getCurrentlyMode().name());
    }

}
