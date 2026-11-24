package club.abelix.core.velocity.auth.command;

import club.abelix.commons.data.auth.AuthSqlHandler;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.core.velocity.auth.AuthManager;
import club.abelix.core.velocity.gamer.Gamer;
import lombok.NonNull;

public class LicenseCommand extends AuthCommand {

    private final AuthManager authManager;

    public LicenseCommand(@NonNull AuthManager velocityAuth) {
        super("license", "lic", "licenses", "online-mode", "premium", "prem");

        this.authManager = velocityAuth;

        setNeedLogin(true);
    }

    @Override
    protected void execute(Gamer gamer, String[] args) {
        if (authManager.isCheckingLicense(gamer.getName())) {
            gamer.sendLocalizedMessage("auth_license_enabled_status_checking");
            return;
        }

        if (gamer.isPremiumAuth()) {
            gamer.sendLocalizedMessage("auth_license_enabled_status_disabled");
            gamer.setPremiumAuth(false);

            AuthSqlHandler.INSTANCE.setPremiumStatus(gamer.getPlayerId(), false);
            authManager.setCheckingLicenseStatus(gamer.getName(), false);

            return;
        }

        if (!Patterns.isMojangNick(gamer.getName())) {
            gamer.sendLocalizedMessage("auth_license_only_mojang_nick");
            return;
        }

        authManager.setCheckingLicenseStatus(gamer.getName(), true);

        gamer.sendLocalizedMessage("auth_license_enabled_status_enabled");
    }
}