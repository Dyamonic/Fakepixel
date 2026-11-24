package club.abelix.core.velocity.auth.command;

import club.abelix.commons.data.action.ActionManager;
import club.abelix.commons.data.action.ActionType;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.auth.bans.AuthBanManager;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.packet.play.client.CTwoFactorAuthentication;
import lombok.val;
import net.kyori.adventure.text.Component;

public class LoginCommand extends AuthCommand {

    private final StarshipVelocity starship;

    public LoginCommand(StarshipVelocity starship) {
        super("login", "l");

        this.starship = starship;

        setNeedRegistration(true);
    }

    @Override
    protected void execute(Gamer gamer, String[] args) {
        if (gamer.isLoggedIn()) {
            gamer.sendLocalizedMessage("auth_already_authorized");
            return;
        }

        if (gamer.isTwoFactorRequested()) {
            return;
        }

        if (args.length != 1) {
            gamer.sendCommandUsageMessage("auth_login_format");
            return;
        }

        String ip = gamer.getPlayer().getRemoteAddress().getAddress().getHostAddress();

        if (!gamer.checkPassword(args[0])) {
            ActionManager.save(gamer.getName(), ip, ActionType.FAIL_LOGIN);

            if (gamer.incPasswordErrors()) {
                gamer.getPlayer().disconnect(Component.text("§cВы слишком много раз ввели неверный пароль. Ваш IP временно заблокирован"));
                AuthBanManager.addBannedAddress(ip);

                return;
            } else {
                gamer.sendLocalizedMessage("auth_invalid_password");
            }

            return;
        }

        val velocityAuth = starship.getAuthManager();
        val twoFactorEnabled = starship.isTwoFactorEnabled();

        if (twoFactorEnabled && gamer.has2FA()) {
            velocityAuth.send2faPacket(gamer);
        } else {
            gamer.login();

            gamer.sendLocalizedMessage("auth_login_success");

            if (twoFactorEnabled && !gamer.isPremiumAuth()) {
                gamer.sendLocalizedMessage("auth_telegram_recommendation");
            }

            if (gamer.getVkId() == 0) {
                gamer.sendLocalizedMessage("vk_warning");
            }

            starship.getAuthManager().afterAuthenticationStage(gamer);
        }
    }
}
