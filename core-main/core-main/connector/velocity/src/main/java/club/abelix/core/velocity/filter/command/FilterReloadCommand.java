package club.abelix.core.velocity.filter.command;

import club.abelix.core.velocity.auth.command.AuthCommand;
import club.abelix.core.velocity.filter.ChatFilterManager;
import club.abelix.core.velocity.gamer.Gamer;
import lombok.NonNull;

public class FilterReloadCommand extends AuthCommand {

    private final ChatFilterManager chatFilterManager;

    public FilterReloadCommand(@NonNull ChatFilterManager chatFilterManager) {
        super("chatreload");

        this.chatFilterManager = chatFilterManager;
    }

    @Override
    public void execute(@NonNull Gamer gamer, @NonNull String[] args) {
        if (!gamer.hasPermission("chatfilter.reload")) {
            gamer.sendLocalizedMessage("no_permission");
            return;
        }

        chatFilterManager.clearPlayerData();

        gamer.sendLocalizedMessage("configuration_reloaded");
    }

}
