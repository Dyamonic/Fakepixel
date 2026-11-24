package club.abelix.commons.language;

import club.abelix.commons.data.currency.Currency;
import club.abelix.commons.language.download.LocalizationDownloadService;
import club.abelix.commons.language.download.S3LocalizationDownloadService;
import club.abelix.commons.language.parser.I18nParserController;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.network.common.locale.UserLocale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.yaml.snakeyaml.Yaml;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Типы языков
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public enum Language implements UserLocale {

    // ================== Языки ======================
    RUSSIAN("Русский", "ru", Currency.RUB, 'а', 'я'),
    ENGLISH("English", "en", Currency.RUB, 'a', 'z');
    // ===============================================

    public static final Language[] VALUES = values();
    private static final String TRANSLATION_NOT_FOUND_STRING = "§cNo translation (%s)";
    private static final Pattern CURSOR_PATTERN = Pattern.compile("\\$\\w+(?i)(:LOWER|:UPPER|:FIRST_UP|:WORD_UP)?\\$");
    private static final Yaml YAML = newSnakeYaml();
    private static final LocalizationDownloadService DOWNLOAD_SERVICE = S3LocalizationDownloadService.create();
    private final int mask = 1 << ordinal();
    /**
     * Полное имя языка
     *
     * <ul style="margin-left: 15px;">
     *     <li>Русский</li>
     *     <li>English</li>
     *     <li>Український</li>
     *     <li>Беларускі</li>
     *     <li>Italiano</li>
     *     <li>Deutsch</li>
     *     <li>etc...</li>
     * </ul>
     */
    private final String languageName;

    /**
     * Краткое имя языка, например:
     *
     * <ul style="margin-left: 15px;">
     *     <li>ru - Русский</li>
     *     <li>en - Английский</li>
     *     <li>ua - Украинский</li>
     *     <li>by - Белорусский</li>
     *     <li>it - Итальянский</li>
     *     <li>de - Немецкий</li>
     *     <li>etc...</li>
     * </ul>
     */
    private final String shortName;

    private final Currency currency;

    private final char alphabetStart, alphabetEnd;
    private final char upperAlphabetStart, upperAlphabetEnd;
    private final float translationPercentage = 100;
    private final Map<String, String> messageTranslations = new ConcurrentHashMap<>();

    Language(String languageName, String shortName, Currency currency, char alphabetStart, char alphabetEnd) {
        this.languageName = languageName;
        this.shortName = shortName;
        this.currency = currency;
        this.alphabetStart = alphabetStart;
        this.upperAlphabetStart = Character.toUpperCase(alphabetStart);

        this.alphabetEnd = alphabetEnd;
        this.upperAlphabetEnd = Character.toUpperCase(alphabetEnd);
    }

    public static Language defaultLanguage() {
        return Project.CURRENT.getDefaultLanguage();
    }

    private static String format(@NonNull String string, Object... objects) {
        if (objects == null) {
            return string;
        }

        if (objects.length % 2 != 0) {
            log.warn("{}[{}]: {}", string, objects.length, Arrays.toString(objects));

            return string;
        }

        for (int i = 0; i < objects.length; i = i + 2) {
            Object key = objects[i];

            if (key == null) {
                throw new IllegalStateException("Key can't be null");
            }

            Object value = objects[i + 1];

            string = string.replace(key.toString(), value == null ? "null" : value.toString());
        }

        return string;
    }

    @Deprecated
    public static void addFlag(String flag) {
    }

    /**
     * Перезагрузить сообщения языков
     */
    @SneakyThrows
    public static void reloadLanguages() {
        I18nParserController.getInstance().clearCache();

        Map<String, List<String>> languageEntries = new HashMap<>();
        Yaml yaml = new Yaml();

        try (InputStream raw = DOWNLOAD_SERVICE.download()) {
            if (raw instanceof ResponseInputStream) {
                var resp = (ResponseInputStream<GetObjectResponse>) raw;
                var r = resp.response();
                log.info("S3 object: contentType={}, contentEncoding={}, contentLength={}, metadata={}",
                        r.contentType(), r.contentEncoding(), r.contentLength(), r.metadata());
            } else {
                log.info("S3 object: no response metadata available (plain InputStream)");
            }

            try (PushbackInputStream pb = new PushbackInputStream(raw, 8)) {
                byte[] sig = pb.readNBytes(4);
                pb.unread(sig);
                boolean isZip = sig.length >= 4 && sig[0] == 'P' && sig[1] == 'K' && sig[2] == 3 && sig[3] == 4;
                if (!isZip) {
                    throw new IllegalStateException("Object is not ZIP file. Signature: " +
                            Arrays.toString(sig));
                }

                try (BufferedInputStream bis = new BufferedInputStream(pb, 8192);
                     ZipInputStream zis = new ZipInputStream(bis)) {

                    ZipEntry ze;
                    byte[] buf = new byte[8192];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(16 * 1024);

                    while ((ze = zis.getNextEntry()) != null) {
                        if (ze.isDirectory()) {
                            log.debug("Skip dir: {}", ze.getName());
                            zis.closeEntry();
                            continue;
                        }

                        String name = ze.getName();
                        if (!name.toLowerCase(Locale.ROOT).endsWith(".yml")) {
                            log.debug("Skip non-yml: {}", name);
                            zis.closeEntry();
                            continue;
                        }

                        baos.reset();
                        int n;
                        while ((n = zis.read(buf)) != -1) {
                            baos.write(buf, 0, n);
                        }
                        String text = baos.toString(StandardCharsets.UTF_8);

                        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                            text = text.substring(1);
                        }

                        try {
                            yaml.load(text);
                        } catch (org.yaml.snakeyaml.error.YAMLException ye) {
                            log.error("YAML parse error in '{}': {}", name, ye.getMessage());
                            zis.closeEntry();
                            continue;
                        }

                        String[] path = name.split("/");
                        if (path.length < 2) {
                            log.debug("Skip short path: {}", name);
                            zis.closeEntry();
                            continue;
                        }

                        String lang = (path.length >= 3) ? path[1] : path[0];
                        languageEntries.computeIfAbsent(lang, __ -> new ArrayList<>()).add(text);

                        log.debug("Loaded {} ({} bytes) -> lang={}", name, text.length(), lang);
                        zis.closeEntry();
                    }
                }
            }

            RUSSIAN.reload(Collections.emptyMap(),
                    languageEntries.getOrDefault(RUSSIAN.getShortName(), Collections.emptyList()));
            ENGLISH.reload(Collections.emptyMap(),
                    languageEntries.getOrDefault(ENGLISH.getShortName(), Collections.emptyList()));

            log.info("Localization updated. langs={}, counts={ru={}, en={}}",
                    languageEntries.keySet(),
                    languageEntries.getOrDefault(RUSSIAN.getShortName(), Collections.emptyList()).size(),
                    languageEntries.getOrDefault(ENGLISH.getShortName(), Collections.emptyList()).size());

        } catch (Exception e) {
            log.error("Localization reload failed: {}", e.toString());
            throw e;
        }
    }


    /**
     * Получить язык по ID
     */
    public static Language getById(int id) {
        return id >= 1 && id <= VALUES.length ? VALUES[id - 1] : defaultLanguage();
    }

    public static Language getByName(String name) {
        for (Language language : VALUES) {
            if (language.shortName.equalsIgnoreCase(name)) {
                return language;
            }
        }

        return defaultLanguage();
    }

    public static Yaml newSnakeYaml() {
        try {
            Class<?> yamlClass = Class.forName("org.yaml.snakeyaml.Yaml");
            Class<?> ctorClass = Class.forName("org.yaml.snakeyaml.constructor.Constructor");
            Class<?> propUtilsClass = Class.forName("org.yaml.snakeyaml.introspector.PropertyUtils");

            Object propUtils = propUtilsClass.getDeclaredConstructor().newInstance();
            try {
                propUtilsClass.getMethod("setSkipMissingProperties", boolean.class)
                        .invoke(propUtils, true);
            } catch (NoSuchMethodException ignore) {
            }

            Object ctor;

            try {
                Class<?> loaderOptionsClass = Class.forName("org.yaml.snakeyaml.LoaderOptions");
                Object loaderOptions = loaderOptionsClass.getDeclaredConstructor().newInstance();
                try {
                    loaderOptionsClass.getMethod("setAllowDuplicateKeys", boolean.class)
                            .invoke(loaderOptions, true);
                } catch (NoSuchMethodException ignore) {
                }

                try {
                    ctor = ctorClass.getConstructor(loaderOptionsClass).newInstance(loaderOptions);
                } catch (NoSuchMethodException e1) {
                    try {
                        ctor = ctorClass.getConstructor(Class.class, loaderOptionsClass)
                                .newInstance(Object.class, loaderOptions);
                    } catch (NoSuchMethodException e2) {
                        ctor = ctorClass.getDeclaredConstructor().newInstance();
                    }
                }
            } catch (ClassNotFoundException e) {
                ctor = ctorClass.getDeclaredConstructor().newInstance();
            }

            try {
                ctorClass.getMethod("setPropertyUtils", propUtilsClass).invoke(ctor, propUtils);
            } catch (NoSuchMethodException ignore) {
            }

            Object yaml;
            try {
                yaml = yamlClass.getConstructor(ctorClass).newInstance(ctor);
            } catch (NoSuchMethodException e) {
                yaml = yamlClass.getDeclaredConstructor().newInstance();
            }
            return (Yaml) yaml;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to construct SnakeYAML compatible instance", e);
        }
    }

    public boolean isInAlphabet(char value) {
        return (value >= alphabetStart && value <= alphabetEnd)
                || (value >= upperAlphabetStart && value <= upperAlphabetEnd);
    }

    @Override
    public String getName() {
        return name();
    }

    public int getId() {
        return ordinal() + 1;
    }

    public String getMessage(@NonNull String key, Object... objects) {
        return getMessage(key, true, objects);
    }

    public String getMessage(@NonNull String key, boolean useDefault, Object[] objects) {
        String message = messageTranslations.get(key.toLowerCase());

        if (message == null) {
            if (useDefault) {
                message = defaultLanguage().getMessage(key, false, objects);
            }
        }

        if (message == null) {
            log.warn("Translation from key {} not found! Language: {} (Use default {})", key, languageName, useDefault);
            return TRANSLATION_NOT_FOUND_STRING.formatted(key.toLowerCase());
        }
        return format(message, objects);
    }

    public List<String> getMessageList(@NonNull String key, Object... objects) {
        return getMessageList(key, true, objects);
    }

    public List<String> getMessageList(@NonNull String key, boolean useDefault, Object[] objects) {
        return Arrays.asList(getMessage(key, useDefault, objects).split("\n"));
    }

    public boolean isMessageTranslated(@NonNull String key) {
        boolean translated = messageTranslations.containsKey(key.toLowerCase());

        // чтобы не было рекурсии если сообщение
        // в основном языке не переведено
        if (this == defaultLanguage()) {
            return translated;
        }

        return translated || defaultLanguage().isMessageTranslated(key);
    }

    public String getEnding(int amount, String key) {
        return TimeUtil.getEnding(amount, getMessage(key + "_single"), getMessage(key + "_onemore"), getMessage(key + "_plural"));
    }

    private void reload(final Map<String, String> defaultTranslations, final List<String> entries) {
        if (entries.isEmpty()) {
            return;
        }

        final Map<String, String> rawMessageMap = entries.stream()
                .map(text -> {
                    synchronized (YAML) {
                        val result = YAML.<Map<?, ?>>load(text);

                        if (result == null) {
                            System.out.println(text);
                        }

                        return result;
                    }
                })
                .filter(Objects::nonNull)
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .collect(Collectors.toMap(
                        entry -> Objects.toString(entry.getKey()).toLowerCase(),
                        entry -> Objects.toString(entry.getValue()),
                        (a, b) -> a
                ));
        rawMessageMap.replaceAll((key, value) -> processCursors(value, defaultTranslations, rawMessageMap));

        this.messageTranslations.putAll(rawMessageMap);
    }

    public void setTranslation(String key, String value) {
        messageTranslations.put(key, value);
    }

    public String formatTranslationPercentage() {
        return String.format("%.2f", getTranslationPercentage());
    }

    private String processCursors(@NonNull String value,
                                  Map<String, String> defaultMessages,
                                  Map<String, String> messages) {
        Matcher matcher = CURSOR_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String cursor = matcher.group();
            cursor = cursor.substring(1, cursor.length() - 1).toLowerCase();

            String localizationKey;
            CursorReplacer replacer;

            int separator = cursor.indexOf(':');

            if (separator == -1) {
                localizationKey = cursor;
                replacer = null;
            } else {
                replacer = CursorReplacer.get(cursor.substring(separator + 1));
                localizationKey = cursor.substring(0, separator);
            }

            try {
                String replacement = Optional.ofNullable(messages.get(localizationKey))
                        .orElse(defaultMessages.get(localizationKey));

                if (replacer != null) {
                    replacement = replacer.replace(replacement);
                }

                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                log.warn("Failed get localization key, translation don't exist: {}", localizationKey);
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
