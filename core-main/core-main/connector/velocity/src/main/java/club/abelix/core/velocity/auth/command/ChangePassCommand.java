package club.abelix.core.velocity.auth.command;


import club.abelix.commons.data.action.ActionManager;
import club.abelix.commons.data.action.ActionType;
import club.abelix.core.velocity.gamer.Gamer;

public class ChangePassCommand extends AuthCommand {

    public ChangePassCommand() {
        super("changepassword", "changepass", "passwd", "password");

        setNeedLogin(true);
        setNeedLegacyMode(true);
    }

    @Override
    protected void execute(Gamer gamer, String[] args) {
        if (args.length != 2) {
            gamer.sendCommandUsageMessage("auth_changepassword_format");
            return;
        }

        String oldPassword = args[0];
        String newPassword = args[1];

        if (oldPassword.equals(newPassword)) {
            gamer.sendLocalizedMessage("auth_changepassword_passwords_equal");
            return;
        }

        if (!gamer.checkPassword(oldPassword)) {
            gamer.sendLocalizedMessage("auth_invalid_password");
            return;
        }

        if (newPassword.length() < 4) {
            gamer.sendLocalizedMessage("auth_min_password_length");
            return;
        }

        if (newPassword.length() > 16) {
            gamer.sendLocalizedMessage("auth_max_password_length");
            return;
        }

        if (!isPassValid(newPassword)) {
            gamer.sendLocalizedMessage("auth_password_has_illegal_symbols");
            return;
        }

        gamer.setPassword(newPassword);
        gamer.sendLocalizedMessage("auth_changepassword_success");

        ActionManager.save(gamer.getName(), gamer.getAddress().getHostAddress(), ActionType.CHANGE_PASS);
    }

}
