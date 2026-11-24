package club.abelix.core.velocity.auth.command;

import club.abelix.commons.data.action.ActionManager;
import club.abelix.commons.data.action.ActionType;
import club.abelix.commons.data.nickname.NickSqlHandler;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.common.logging.Logging;
import net.kyori.adventure.text.Component;

public class RegisterCommand extends AuthCommand {

    private final StarshipVelocity bungeeCore;

    public RegisterCommand(StarshipVelocity bungeeCore) {
        super("register", "reg", "рег", "куп");

        this.bungeeCore = bungeeCore;
    }

    @Override
    protected void execute(Gamer gamer, String[] args) {
        if (gamer.isLoggedIn()) {
            gamer.sendLocalizedMessage("auth_already_authorized");
            return;
        }

        if (gamer.isRegistered()) {
            gamer.sendLocalizedMessage("auth_already_registered");
            return;
        }

        if (args.length < 2) {
            gamer.sendCommandUsageMessage("auth_registration_format");
            return;
        }

        if (!args[0].equals(args[1])) {
            gamer.sendLocalizedMessage("auth_registration_passwords_different");
            return;
        }

        if (args[0].length() < 4) {
            gamer.sendLocalizedMessage("auth_min_password_length");
            return;
        }

        if (args[0].length() > 16) {
            gamer.sendLocalizedMessage("auth_max_password_length");
            return;
        }

        if (!isPassValid(args[0])) {
            gamer.sendLocalizedMessage("auth_password_has_illegal_symbols");
            return;
        }

        NickSqlHandler.INSTANCE.checkNicknameAvailability(gamer.getName()).thenAccept(result -> {
            Logging.IMP.info("Checking for nick usage: " + gamer.getName() + " - " + result);

            if (!result) {
                gamer.getPlayer().disconnect(
                        Component.text(gamer.getLanguage().getMessage("auth_nick_registration_disallowed"))
                );

                return;
            }

            gamer.sendLocalizedMessage("auth_registration_success");

            Logging.IMP.info("New player registered: {}", gamer.getName());

            ActionManager.save(gamer.getName(), gamer.getAddress().getHostAddress(), ActionType.REGISTER);

            gamer.register(args[0], () -> {
                NickSqlHandler.INSTANCE.disableNickname(gamer.getName());

                bungeeCore.getAuthManager().afterRegisterStage(gamer);
            });


        });
    }

}
