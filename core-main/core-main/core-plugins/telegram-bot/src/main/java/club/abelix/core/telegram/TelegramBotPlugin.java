package club.abelix.core.telegram;

import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.core.telegram.bans.TelegramBanManager;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.command.ProxiedBotCommand;
import club.abelix.core.telegram.command.impl.*;
import club.abelix.core.telegram.handler.TelegramUpdatesHandler;
import club.abelix.core.telegram.listener.*;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.network.common.logging.Logging;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ReplyParameters;
import com.pengrad.telegrambot.request.ForwardMessage;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import w.util.Env;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TelegramBotPlugin extends CorePlugin {

    private static final Cache<String, Integer> tokensCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build();

    private final Map<String, BotCommand> commandMap = new HashMap<>();
    private final Map<String, ProxiedBotCommand> proxiedBotCommandMap = new HashMap<>();

    @Getter
    private static TelegramBotPlugin instance;

    @Getter
    private TelegramBot bot;

    @Getter
    @Setter
    private boolean paused;

    @Override
    public void onEnable() {
        instance = this;

        val twoFactorEnabled = Boolean.parseBoolean(Env.getString("2FA_ENABLED", "false"));

        if (!twoFactorEnabled) {
            return;
        }

        val pluginManager = getCore().getPluginManager();

        pluginManager.registerListener(this, new PacketListener(this));
        pluginManager.registerListener(this, new StaffMessageListener(this));

        pluginManager.registerListener(new CommandListener(this));
        pluginManager.registerListener(new PlayerListener(this));
        pluginManager.registerListener(new SecurityListener(this.getCore().getUserRanksService()));

        val botToken = Env.getString("TELEGRAM_BOT_TOKEN", "");

        this.bot = new TelegramBot(botToken);
        this.bot.setUpdatesListener(new TelegramUpdatesHandler(this));

        TelegramBanManager.INSTANCE.loadPunishments().join();

        registerCommand(new AdminAccountListCommand());
        registerCommand(new AccountLinkCommand());
        registerCommand(new AccountUnLinkCommand());
        registerCommand(new AccountInfoCommand());
        registerCommand(new AccountKickCommand());
        registerCommand(new AccountPrimaryCommand());
        registerCommand(new HelpCommand());
        registerCommand(new AccountLanguageCommand());
        registerCommand(new NickCommand());
        registerCommand(new PauseCommand());
        registerCommand(new InvalidateCacheCommand());
        registerCommand(new AccountRecoveryCommand());

        registerCommand(new TwoFactorAuthCommand("loginallowedtrue", true));
        registerCommand(new TwoFactorAuthCommand("loginallowedfalse", false));

        registerProxiedCommand("mute", "mute", "мут");
        registerProxiedCommand("ban", "ban", "бан");
        registerProxiedCommand("banip", "banip");
        registerProxiedCommand("tempban", "tempban", "tban");
        registerProxiedCommand("tbanip", "tbanip");
        registerProxiedCommand("unmute", "unmute", "размут");
        registerProxiedCommand("unban", "unban", "разбан");
        registerProxiedCommand("kick", "kick", "кик");
        registerProxiedCommand("findaccounts", "findaccounts", "dupeip");
        registerProxiedCommand("actions", "actions", "действия");
        registerProxiedCommand("online", "online", "онлайн");
        registerProxiedCommand("staff", "staff", "персонал");
        registerProxiedCommand("find", "find", "where");
        registerProxiedCommand("rank", "rank");
        registerProxiedCommand("bhist", "bhist", "banhist", "banh", "banhistory");
        registerProxiedCommand("mhist", "mhist", "mhistory", "muteh", "mutehistory");
        registerProxiedCommand("banidinfo", "banidinfo");
        registerProxiedCommand("muteidinfo", "muteidinfo");
        registerProxiedCommand("ee", "ee", "eaegleeye");
        registerProxiedCommand("apreset", "apreset", "adminresetpassword");
        registerProxiedCommand("asnwhitelist", "asnwhitelist");
        registerProxiedCommand("whois", "whois");
        registerProxiedCommand("removefromcore", "removefromcore", "rfc");
        registerProxiedCommand("acdump", "acdump");
        registerProxiedCommand("chatlog", "chatlog", "clog");
        registerProxiedCommand("gold", "gold");
    }

    @Override
    public void onDisable() {
        instance = null;

        if (bot != null) {
            bot.removeGetUpdatesListener();
            bot.shutdown();

            this.bot = null;
        }
    }

    public void printMessage(final @NonNull BotUser botUser, final @NonNull String message) {
        val sendMessage = new SendMessage(botUser.getTelegramId(), message);
        printMessage(sendMessage);
    }

    public void printMessage(final long chatId, final @NonNull String message) {
        val sendMessage = new SendMessage(chatId, message);
        printMessage(sendMessage);
    }

    public void replyMessage(final @NonNull Message reply, final @NonNull String message) {
        val sendMessage = new SendMessage(reply.chat().id(), message);
        sendMessage.replyParameters(new ReplyParameters(reply.messageId()));

        printMessage(sendMessage);
    }

    public void printMessage(final @NonNull SendMessage message) {
        this.bot.execute(message);
    }

    public void forwardMessage(final long targetChatId, final @NonNull Message messageToForward) {
        val forward = new ForwardMessage(
                Long.valueOf(targetChatId),
                messageToForward.chat().id(),
                messageToForward.messageId()
        );

        try {
            bot.execute(forward);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка пересылки сообщения: " + e.getMessage(), e);
        }
    }

    public void registerCommand(@NonNull BotCommand botCommand) {
        for (val alias : botCommand.getAliases()) {
            commandMap.put(alias.toLowerCase(), botCommand);
        }

        Logging.IMP.info("Registered Telegram Bot command -> {}", botCommand.getAliases()[0]);
    }

    public BotCommand getCommand(@NonNull String commandAlias) {
        return commandMap.get(commandAlias.toLowerCase());
    }

    public void registerProxiedCommand(final @NonNull String coreCommandName,
                                       final @NonNull String @NotNull ... aliases) {
        val botCommand = ProxiedBotCommand.create(coreCommandName, aliases);

        for (val alias : botCommand.getAliases()) {
            proxiedBotCommandMap.put(alias.toLowerCase(), botCommand);
        }

        Logging.IMP.info("Registered proxied Telegram Bot command -> {}", botCommand.getAliases()[0]);
    }

    public ProxiedBotCommand getProxiedCommand(@NonNull String commandAlias) {
        return proxiedBotCommandMap.get(commandAlias.toLowerCase());
    }

    public int getPlayerIdByToken(@NonNull String token) {
        tokensCache.cleanUp();

        return tokensCache.asMap().getOrDefault(token, 0);
    }

    public void addToken(int playerId, @NonNull String token) {
        tokensCache.put(token, playerId);
    }

    public void removeToken(@NonNull String token) {
        tokensCache.asMap().remove(token);
    }

}
