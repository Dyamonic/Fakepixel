package club.abelix.core.telegram.listener;

import club.abelix.commons.language.Language;
import club.abelix.core.app.Async;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.core.ClientActiveEvent;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.core.telegram.user.sql.BotUserSqlHandler;
import club.abelix.core.telegram.util.GeoLocationUtil;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.both.BCheckChannelSubscription;
import club.abelix.network.packet.play.client.CSetSlots;
import club.abelix.network.packet.play.client.CTelegramAuth;
import club.abelix.network.packet.play.client.CTelegramMessage;
import club.abelix.network.packet.play.client.CTelegramToken;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.type.telegram.SecurityUser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.GetChatMember;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetChatMemberResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class PacketListener implements Listener, PlayClientProcessor {

    private final TelegramBotPlugin plugin;

    /**
     * Кэш с подтверждениями телеги для игроков (LOGIN STATE)
     */
    public static final Cache<String, SecurityUser> CONFIRMATION_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(3L, TimeUnit.MINUTES)
            .build();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    @Subscribe
    public void onActive(final @NonNull ClientActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    @Override
    public void process(final @NonNull CTelegramAuth packet, ChannelProcessorContext ctx) {
        Async.run(() -> processAuthPacket(packet));
    }

    @Override
    public void process(final @NonNull CTelegramToken packet, ChannelProcessorContext ctx) {
        Logging.IMP.debug("Telegram token packet for {}, {}" + packet.getPlayerId(), packet.getToken());
        plugin.addToken(packet.getPlayerId(), packet.getToken());
    }

    private void processAuthPacket(final @NonNull CTelegramAuth packet) {
        val user = packet.getSecurityUser();

        val name = user.getName();
        val address = user.getAddress();

        val botUser = BotUserSqlHandler.INSTANCE.getBotUser(name);

        if (botUser == BotUser.EMPTY_BOT_USER) {
            return;
        }

        var language = botUser.getLanguage();

        if (language == null) {
            language = Language.getById(user.getLanguageId());

            botUser.setLanguage(language);
            BotUserSqlHandler.INSTANCE.setLanguage(botUser, language);
        }

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

        val confirmationMessage = language.getMessage("core_telegram_login_request_message",
                "%emoji_1%", "⚠️",
                "%emoji_2%", "\uD83D\uDC64",
                "%emoji_3%", "⌛️",
                "%emoji_4%", "\uD83D\uDDA5",
                "%emoji_5%", "♻️",
                "%emoji_6%", "\uD83D\uDCA1",
                "%emoji_7%", "⭕️",
                "%name%", user.getName(),
                "%ip%", user.getAddress().getHostAddress(),
                "%time%", DATE_FORMAT.format(new Date()),
                "%place%", location
        );

        val accept = new InlineKeyboardButton(language.getMessage("core_telegram_login_request_confirm_button",
                "%emoji_1%", "✅"));
        accept.callbackData("!loginallowedtrue " + name);

        val decline = new InlineKeyboardButton(language.getMessage("core_telegram_login_request_decline_button",
                "%emoji_1%", "❌"));
        decline.callbackData("!loginallowedfalse " + name);

        val chatId = botUser.getTelegramId();

        val message = new SendMessage(chatId, confirmationMessage);
        message.replyMarkup(new InlineKeyboardMarkup(accept, decline));

        CONFIRMATION_CACHE.put(name.toLowerCase(), user);

        plugin.printMessage(message);
    }

    @Override
    public void process(final @NonNull BCheckChannelSubscription.Request packet, ChannelProcessorContext ctx) {
        ctx.addLatch();

        processChannelSubscriptionPacket(packet).orTimeout(5, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> {
                    ctx.setResponse(new BCheckChannelSubscription.Response(result));
                    ctx.removeLatch();
                });
    }

    private CompletableFuture<BCheckChannelSubscription.SubscriptionStatus> processChannelSubscriptionPacket(
            final @NonNull BCheckChannelSubscription.Request packet
    ) {
        val playerId = packet.getPlayerId();
        val gamer = Gamer.getGamer(playerId);

        if (gamer == null) {
            return CompletableFuture.completedFuture(BCheckChannelSubscription.SubscriptionStatus.NOT_LINKED);
        }

        val name = gamer.getName();
        val botUser = BotUserSqlHandler.INSTANCE.getBotUser(name);

        if (botUser == BotUser.EMPTY_BOT_USER) {
            return CompletableFuture.completedFuture(BCheckChannelSubscription.SubscriptionStatus.NOT_LINKED);
        }

        val chatId = packet.getChatId();
        val userId = botUser.getTelegramId();

        if (botUser.getProperty("WasSubscribed/" + chatId) == 1) {
            return CompletableFuture.completedFuture(BCheckChannelSubscription.SubscriptionStatus.WAS_SUBSCRIBED);
        }

        val future = new CompletableFuture<BCheckChannelSubscription.SubscriptionStatus>();

        plugin.getBot().execute(new GetChatMember(chatId, userId), new Callback() {
            @Override
            public void onResponse(BaseRequest baseRequest, BaseResponse baseResponse) {
                try {
                    val response = (GetChatMemberResponse) baseResponse;
                    val chatMember = response.chatMember();

                    if (chatMember == null) {
                        future.complete(BCheckChannelSubscription.SubscriptionStatus.NOT_SUBSCRIBED);
                        return;
                    }

                    val status = chatMember.status();
                    var isMember = status != ChatMember.Status.kicked && status != ChatMember.Status.left;

                    future.complete(isMember
                            ? BCheckChannelSubscription.SubscriptionStatus.SUBSCRIBED
                            : BCheckChannelSubscription.SubscriptionStatus.NOT_SUBSCRIBED);

                    if (isMember) {
                        botUser.setProperty("WasSubscribed/" + chatId, 1);
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void onFailure(BaseRequest baseRequest, IOException e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}
