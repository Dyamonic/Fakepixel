package club.abelix.commons.language.parser;

import club.abelix.commons.language.Language;
import club.abelix.commons.language.value.LanguageValue;
import lombok.RequiredArgsConstructor;
import ru.whileinside.array.FlexibleCharArray;

import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor
public class I18nThreadParser implements I18nParser {

    private static final char[] LANG = "lang:".toCharArray();
    private static final char END = ';';
    private static final char ARG = ':';
    private static final char ESCAPE_BEGIN = '{';
    private static final char ESCAPE_END = '}';
    private static final char VALUE_BEGIN = '[';
    private static final char VALUE_END = ']';
    private static final char JS_ESCAPE = '$';
    private final I18nParserController controller;
    private final FlexibleCharArray result = new FlexibleCharArray();
    private final FlexibleCharArray lang = new FlexibleCharArray();

    @Override
    public I18nParser getParent() {
        return controller;
    }

    @Override
    public void registerValue(String name, LanguageValue value) {
        controller.registerValue(name, value);
    }

    @Override
    public LanguageValue getValue(String name) {
        return controller.getValue(name);
    }

    @Override
    public void clearCache() {
        controller.clearCache();
    }

    private String processValue(FlexibleCharArray in, Language language) {
        int beginPosition = in.position();

        LanguageValue languageValue = null;
        List<String> args = null;

        while (in.hasRemaining()) {
            int ch = in.read();

            switch (ch) {
                case ARG, VALUE_END -> {
                    String text = in.substring(beginPosition, in.position() - 1);
                    beginPosition = in.position();
                    if (languageValue == null) {
                        languageValue = getValue(text);
                    } else {
                        if (args == null) {
                            args = new LinkedList<>();
                        }

                        args.add(text);
                    }
                    if (ch == VALUE_END) {
                        if (languageValue == null) {
                            return null;
                        }

                        return languageValue.process(language, args == null ? null : args.toArray(new String[0]));
                    }
                }
            }
        }

        return null;
    }

    private String processLang(FlexibleCharArray in, Language language) {
        lang.reset();

        String key = null;
        List<String> values = null;

        int escapes = 0;
        boolean jsEscape = false;

        while (in.hasRemaining()) {
            int ch = in.read();

            if (escapes > 0 && ch == ESCAPE_END) {
                escapes--;
                if (escapes == 0) continue;
            }

            if (!jsEscape) {
                if (ch == ESCAPE_BEGIN) {
                    escapes++;
                    if (escapes == 1) continue;
                }
            } else jsEscape = false;

            if (escapes == 0) {
                switch (ch) {
                    case JS_ESCAPE -> jsEscape = true;
                    case VALUE_BEGIN -> {
                        if (lang.length() == 0 && values != null && (values.size() & 1) == 1) {
                            int pos = in.position();
                            String processedValue = processValue(in, language);
                            if (processedValue == null) {
                                int newPos = in.position();
                                int len = newPos - pos;

                                if (len != 0) {
                                    in.position(pos - 1);
                                    lang.write(in.substring(0, len + 1));
                                    in.position(newPos);
                                }
                            } else {
                                lang.write(processedValue);
                            }

                            continue;
                        }
                    }
                    case ARG, END -> {
                        String argValue = lang.toString();
                        lang.reset();
                        if (key == null) {
                            key = argValue;
                        } else {
                            if (values == null) {
                                values = new LinkedList<>();
                            }

                            values.add(argValue);
                        }
                        if (ch == END) {
                            return language.getMessage(key, values == null ? null : values.toArray());
                        }
                        continue;
                    }
                }
            }

            lang.write(ch);
        }

        return null;
    }

    public String parse(Language language, String message) {
        result.reset();

        // todo .toCharArray слишком медленный, нужно избавиться от него
        FlexibleCharArray in = new FlexibleCharArray(message.toCharArray());

        int idx = 0;

        while (in.hasRemaining()) {
            int ch = in.read();

            if (ch == LANG[idx]) {
                idx++;

                if (idx == LANG.length) {
                    int pos = in.position() - LANG.length;
                    String processedValue = processLang(in, language);

                    if (processedValue == null) {
                        int newPos = in.position();
                        int len = newPos - pos;

                        if (len != 0) {
                            in.position(pos);
                            result.write(in.substring(0, len));
                            in.position(newPos);
                        }
                    } else {
                        result.write(processedValue);
                    }
                    idx = 0;
                }

                continue;
            } else {
                if (idx > 0) {
                    result.write(LANG, 0, idx);
                    idx = 0;
                }
            }

            result.write(ch);
        }


        if (idx > 0) {
            result.write(LANG, 0, idx);
        }

        return result.toString();
    }
}
