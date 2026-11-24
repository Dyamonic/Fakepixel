package club.abelix.core.velocity.auth.command;

import club.abelix.commons.util.random.RandomUtil;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.packet.play.client.CTelegramToken;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import w.util.Env;

public class TelegramCommand extends AuthCommand {

    public static final String TELEGRAM_BOT_URL = Env.getString("TELEGRAM_BOT_URL");
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public TelegramCommand() {
        super("telegram", "tg", "2fa");

        setNeedLogin(true);
//        setNeedLegacyMode(true);
        setNeedRegistration(true);
    }

    @Override
    protected void execute(Gamer gamer, String[] args) {
        if (gamer.has2FA()) {
            gamer.sendLocalizedMessage("auth_telegram_command_already_have");
            return;
        }

        if (gamer.isTwoFactorRequested()) {
            gamer.sendLocalizedMessage("auth_telegram_command_already_requested");
            return;
        }

        if (!Connection.isConnected()) {
            gamer.sendLocalizedMessage("command_error_core");
            return;
        }

        val token = generateToken();
        val url = TELEGRAM_BOT_URL + "?start=" + token;

        val component = Component
                .text(gamer.getLanguage().getMessage("auth_telegram_command_request",
                        "%token%", token))
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text(gamer.getLanguage()
                        .getMessage("auth_telegram_command_request_hover"))));

        gamer.getPlayer().sendMessage(component);

        gamer.setTwoFactorRequested(true);

        Connection.sendPacket(new CTelegramToken(gamer.getPlayerId(), token));
    }

    public static String generateToken() {
        val token = new StringBuilder(6);

        for (int i = 0; i < 6; i++) {
            token.append(CHARS.charAt(RandomUtil.R.nextInt(CHARS.length())));
        }

        return token.toString();
    }

}