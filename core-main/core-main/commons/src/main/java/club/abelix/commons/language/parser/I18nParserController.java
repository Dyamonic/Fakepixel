package club.abelix.commons.language.parser;

import club.abelix.commons.language.Language;
import club.abelix.commons.language.value.LanguageValue;
import club.abelix.commons.util.object.Pair;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.commons.util.word.CommonWordsLoader;
import club.abelix.commons.util.word.Noun;
import club.abelix.commons.util.word.WordBuilder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Unidentified Person
 */
public class I18nParserController implements I18nParser {

    private static final I18nParserController instance = new I18nParserController();

    private final ThreadLocal<I18nThreadParser> THREAD_PARSER
            = ThreadLocal.withInitial(() -> new I18nThreadParser(this));

    private final Map<String, LanguageValue> values = new ConcurrentHashMap<>() {
        {
            put("lang", (language, args) -> {
                if (args == null || args.length == 0) {
                    return null;
                }

                return language.getMessage(args[0], args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : null);
            });

            put("time", (language, args) -> {
                if (args == null || args.length == 0) {
                    return null;
                }

                return TimeUtil.getDateString(language, Long.parseLong(args[0]));
            });

            put("duration_sec", (language, args) -> {
                if (args == null || args.length == 0) {
                    return null;
                }

                return Duration.durationWordsSec(language, Long.parseLong(args[0]));
            });

            put("word", (language, args) -> {
                if (args == null || args.length == 0) {
                    return null;
                }

                Noun noun = CommonWordsLoader.getNoun(args[0]);
                WordBuilder word = noun.get(language);

                if (args.length > 1) {
                    word.withNumber(Long.parseLong(args[1]));
                }

                return word.toString();
            });
        }
    };

    private final Cache<Pair<Language, String>, String> cache = Caffeine.newBuilder()
            .maximumSize(100)
            .initialCapacity(100)
            .build();

    public static I18nParserController getInstance() {
        return instance;
    }

    @Override
    public void clearCache() {
        cache.invalidateAll();
    }

    @Override
    public String parse(Language language, String text) {
        try {
            return cache.get(new Pair<>(language, text), (pair) -> {
                I18nThreadParser parser = THREAD_PARSER.get();
                return parser.parse(language, text);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public I18nParser getParent() {
        return null;
    }

    @Override
    public void registerValue(String name, LanguageValue value) {
        this.values.put(name, value);
    }

    @Override
    public LanguageValue getValue(String name) {
        return values.get(name);
    }

}