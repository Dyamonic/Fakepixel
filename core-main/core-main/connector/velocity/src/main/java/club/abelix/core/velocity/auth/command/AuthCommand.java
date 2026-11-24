package club.abelix.core.velocity.auth.command;

import club.abelix.core.velocity.gamer.Gamer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class AuthCommand implements SimpleCommand {

    /**
     * Для некоторых команд, где нужно проверять пароль игрока
     */
    private static final String PASSWORD_SYMBOL_WHITELIST =
            " !\"#$%&'()*+,-./0123456789:;<=>?@" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_¸" +
                    "abcdefghijklmnopqrstuvwxyz{|}~" +
                    "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЫЭЮЯ" +
                    "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private final String[] aliases;
    private final String name;
    @Getter
    @Setter
    private boolean needLogin = false, needRegistration = false, needLegacyMode = false;

    protected AuthCommand(final String name, final String... aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    static boolean isPassValid(String message) {
        for (int i = 0; i < message.length(); i++) {
            if (PASSWORD_SYMBOL_WHITELIST.indexOf(message.charAt(i)) == -1) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();

        if (!(sender instanceof Player player)) {
            return;
        }

        Gamer gamer = Gamer.getGamer(player);

        if (needLogin && !gamer.isLoggedIn()) {
            gamer.sendLocalizedMessage("auth_not_authorized");
            return;
        }

        if (needRegistration && !gamer.isRegistered()) {
            gamer.sendLocalizedMessage("auth_not_registered");
            return;
        }

        if (needLegacyMode && gamer.isPremiumAuth()) {
            gamer.sendLocalizedMessage("auth_should_disable_license");
            return;
        }

        execute(gamer, invocation.arguments());
    }

    protected abstract void execute(Gamer gamer, String[] args);

}
