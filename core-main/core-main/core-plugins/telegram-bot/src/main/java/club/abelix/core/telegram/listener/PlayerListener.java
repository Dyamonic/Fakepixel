package club.abelix.core.telegram.listener;

import club.abelix.commons.data.setting.Setting;
import club.abelix.core.app.api.event.EventPriority;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.core.telegram.user.sql.BotUserSqlHandler;
import club.abelix.core.telegram.util.GeoLocationUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@RequiredArgsConstructor
public final class PlayerListener implements Listener {

    private final TelegramBotPlugin plugin;

    @Subscribe(priority = EventPriority.HIGHEST, threadMode = ThreadMode.ASYNC)
    public void onPlayerJoin(final @NonNull GamerJoinEvent event) {
        process(event);
    }

    private void process(final @NonNull GamerJoinEvent event) {
        val gamer = event.getGamer();

        if (gamer.isSettingSet(Setting.DISABLE_TELEGRAM_LOGIN_NOTIFICATIONS)) {
            return;
        }

        val playerName = gamer.getName();
        val botUser = BotUserSqlHandler.INSTANCE.getBotUser(playerName);

        if (botUser == BotUser.EMPTY_BOT_USER) {
            return;
        }

        var language = botUser.getLanguage();

        if (language == null) {
            language = event.getGamer().getLanguage();
        }

        val address = event.getGamer().getAddress();
        val geoManager = plugin.getCore().getGeoLocationManager();

        var location = "N/A";

        if (geoManager != null) {
            val geoLocation = geoManager.lookup(address);
            val country = geoLocation.getCountry();

            if (country != null) {
                val countryFlag = GeoLocationUtil.getCountryFlag(country.getIsoCode());

                location = countryFlag + " " + country.getName();

                val city = geoLocation.getCity();

                if (city != null) {
                    location += ", " + city;
                }
            }
        }

        val message = language.getMessage(
                "core_telegram_account_joined",
                "%name%", playerName,
                "%location%", location,
                "%ip%", address.getHostAddress(),
                "%emoji_1%", "\uD83D\uDC6E"
        );

//        val banButton = new InlineKeyboardButton(language.getMessage(
//                "core_telegram_account_joined_button", "%emoji_1%", "❌"))
//                .callbackData("!восстановить " + playerName);

        plugin.printMessage(botUser.getTelegramId(), message);

//        plugin.printMessage(new SendMessage(botUser.getTelegramId(), message)
//                .replyMarkup(new InlineKeyboardMarkup(banButton)));
    }

}
