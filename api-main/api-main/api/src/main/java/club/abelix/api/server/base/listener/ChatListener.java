package club.abelix.api.server.base.listener;

import club.abelix.api.base.event.chat.AsyncChatFormatEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.commons.language.Language;
import club.abelix.commons.translate.api.TranslateLanguage;
import club.abelix.commons.translate.api.TranslateRequest;
import club.abelix.commons.translate.google.GoogleTranslateLanguage;
import club.abelix.commons.translate.google.GoogleTranslateRequestProcessor;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.commons.util.language.LanguageUtil;
import club.abelix.commons.util.string.StringUtil;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.common.enums.GameState;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class ChatListener extends YListener {

    private static final String DEFAULT_CHAT_FORMAT = "*display**suffix**doublecolor*: *color**message*";

    private final String format;

    public ChatListener(@NonNull Plugin plugin) {
        this(plugin, DEFAULT_CHAT_FORMAT);
    }

    public ChatListener(@NonNull Plugin plugin, @NonNull String format) {
        super(plugin);

        this.format = format;
    }

    public Language getLanguage(CharSequence sequence) {
        float maxPercentage = 0;
        Language found = null;

        for (Language language : Language.VALUES) {
            float percentage = getLanguagePercentage(language, sequence);

            if (percentage == 1F) {
                return language;
            }

            if (maxPercentage < percentage) {
                maxPercentage = percentage;
                found = language;
            }
        }

        return found;
    }

    public float getLanguagePercentage(Language language, CharSequence sequence) {
        int alphabetChars = 0;

        for (int i = 0; i < sequence.length(); i++) {
            if (language.isInAlphabet(sequence.charAt(i))) {
                alphabetChars++;
            }
        }

        return alphabetChars / (float) sequence.length();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());

        if (gamer == null) {
            return;
        }

        event.setCancelled(true);

        String message = event.getMessage();
        String consoleFormat = replaceFormat(gamer);

        Bukkit.getConsoleSender().sendMessage(consoleFormat.replace("*message*", message));

        if (!gamer.getRanks().hasPermission("chat.unfiltered")) {
            message = StringUtil.retainAsciiAndCyrillic(message);
        }

        for (Player receiver : event.getRecipients()) {
            Gamer receiverGamer = Gamer.getGamer(receiver);

            String format = consoleFormat;

            AsyncChatFormatEvent formatEvent = new AsyncChatFormatEvent(gamer, receiverGamer, format, format, message);

            // TODO: Create ignore list check here
            if (receiverGamer.getIgnoreList().contains(gamer.getPlayerId())) continue;

            HeartBeat.callEvent(formatEvent);

            if (formatEvent.isCancelled()) {
                continue;
            }

            format = formatEvent.getFormat().replace("*message*", formatEvent.getMessage());

            if (gamer.getRanks().hasPermission("chat.color")) {
                format = ChatColor.translateAlternateColorCodes('&', format);
            }

            /*
            String sendMessage = formatEvent.getMessage();

            if (sendMessage.length() > 3 && receiverGamer != gamer) {
                ChatMessage chatMessage = ChatMessage.parse(sendMessage);

                if (chatMessage.getLength() > 3) {
                    Language receiverLanguage = receiverGamer.getLanguage();
                    Language messageLanguage = chatMessage.getLanguage();

                    if (messageLanguage != null && receiverLanguage != messageLanguage) {
                        BaseComponent[] textComponent = TextComponent.fromLegacyText(format);

                        TranslatableChatMessage translatableChatMessage = ChatTranslationManager.INSTANCE.handleMessage(sendMessage, messageLanguage);
                        BaseComponent[] component = TextComponent.fromLegacyText(receiverLanguage.getMessage("translate_button"));

                        for (BaseComponent comp : component) {
                            comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/translate " + translatableChatMessage.getId()));
                            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    TextComponent.fromLegacyText(receiverLanguage.getMessage("translate_button_description"))));

                            textComponent[textComponent.length - 1].addExtra(comp);
                        }

                        receiverGamer.sendJsonMessage(ComponentSerializer.toString(textComponent));
                        continue;
                    }
                }
            } */

            val translateLang = receiverGamer.getProperty("Chat/Translate");

            if (translateLang != 0) {
                val language = Language.getById(translateLang);
                val detectedLanguage = LanguageUtil.detectLanguage(formatEvent.getMessage());

                if (detectedLanguage != null && detectedLanguage.equals(language)) {
                    receiverGamer.sendMessage(format);
                    continue;
                }

                val requestProcessor = GoogleTranslateRequestProcessor.create();

                val request = new TranslateRequest(
                        GoogleTranslateLanguage.AUTO,
                        TranslateLanguage.of(language.getShortName()),
                        formatEvent.getMessage()
                );

                requestProcessor.processRequest(request)
                        .thenAccept(response -> {
                            val translatedFormat = formatEvent.getFormat().replace("*message*",
                                    formatEvent.getMessage() + " §7-> " + response.result());

                            receiverGamer.sendMessage(translatedFormat);
                        });
                continue;
            }

            receiverGamer.sendMessage(format);
        }
    }

    private String replaceFormat(@NonNull Gamer gamer) {
        String format = this.format;

        String gamerName = gamer.getDisplayName();
        if (gamer.getGame() != null && gamer.getGame().getGameState() == GameState.GAME) {
            gamerName = gamer.getColoredName();
        }

        format = format.replace("*display*", gamerName);
        format = format.replace("*color*", gamer.getColorInChat());
        format = ChatColor.translateAlternateColorCodes('&', format);

        if (!gamer.getRanks().hasPermission("chat.color.double")
                || (gamer.getFakeName() != null && !gamer.getFakeName().getGroup().hasPermission("chat.color.double"))) {
            format = format.replace("*doublecolor*", "§7");
        } else {
            format = format.replace("*doublecolor*", "§f");
        }

        String suffix = gamer.getSuffixText();

        if (suffix.length() > 1) {
            suffix = " " + gamer.getSuffixText();
        }

        format = format.replace("*suffix*", suffix);
        return format;
    }
}