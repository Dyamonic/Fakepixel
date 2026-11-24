package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.language.Lang;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;

@WithAlias("sct")
@WithAlias("togglestaffchat")
@WithAlias("adminchattoggle")
@WithAlias("act")
@NamedCommand("staffchattoggle")
public final class StaffChatToggleCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @WithPermission("staff")
    @WithCondition("notBanned")
    public static void execute(final CoreExecutionSession session) {

        Gamer gamer = session.getGamer();
        gamer.setActivePersonalChat(!gamer.isActivePersonalChat());

        gamer.sendLocalizedMessage("staffchat_toggle", "%enums%", Lang.of(gamer.isActivePersonalChat() ? "enabled" : "disabled"));

    }
}
