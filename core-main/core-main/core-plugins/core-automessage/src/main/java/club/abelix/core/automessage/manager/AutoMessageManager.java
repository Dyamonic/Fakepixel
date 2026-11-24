package club.abelix.core.automessage.manager;

import club.abelix.commons.language.Language;
import club.abelix.core.app.api.config.Configuration;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.automessage.AutoMessage;
import club.abelix.core.automessage.AutoMessageCorePlugin;
import club.abelix.network.common.logging.Logging;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class AutoMessageManager {

    private final AutoMessageCorePlugin plugin;

    private ScheduledExecutorService autoMessageExecutor;

    public void reloadConfiguration() {
        plugin.loadConfig();

        Configuration configuration = plugin.getConfig();

        //интервал авто-сообщений
        int interval = configuration.getInt("interval");

        List<AutoMessage> messages = new ArrayList<>();

        for (String key : configuration.getStringList("message_keys")) {
            Map<Language, String> translatedMessages = new HashMap<>();

            for (Language language : Language.VALUES) {
                translatedMessages.put(language, language.getMessage(key));
            }

            messages.add(new AutoMessage(key, translatedMessages));
        }

        Iterator<AutoMessage> messageIterator = messages.iterator();

        if (autoMessageExecutor != null) {
            autoMessageExecutor.shutdown();
            autoMessageExecutor = null;
        }

        autoMessageExecutor = Executors.newSingleThreadScheduledExecutor();
        autoMessageExecutor.scheduleAtFixedRate(() -> {
            if (!messageIterator.hasNext()) {
                reloadConfiguration();
            }

            AutoMessage message = messageIterator.next();

            // Проверяем в каких языках он есть
            List<Language> avaliableLanguage = new ArrayList<>();

            for (Language language : Language.VALUES) {
                if (language.isMessageTranslated(message.getLocalizationKey())) {
                    avaliableLanguage.add(language);
                }
            }

            // Отправляем только доступным языкам или всем
            if (avaliableLanguage.size() == Language.VALUES.length) {
                ServerManager.IMP.broadcastChatLocalized(message.getLocalizationKey());
            } else {
                for (Language language : avaliableLanguage) {
                    ServerManager.IMP.broadcastChatLocalized(language, message.getLocalizationKey());
                }

            }

            Logging.IMP.info("Auto message by key {} successfully sent to {} players",
                    message.getLocalizationKey(), Gamer.getGamers().size());
        }, interval, interval, TimeUnit.SECONDS);
    }

    public void stop() {
        if (autoMessageExecutor != null) {
            autoMessageExecutor.shutdown();
            autoMessageExecutor = null;
        }
    }
}
