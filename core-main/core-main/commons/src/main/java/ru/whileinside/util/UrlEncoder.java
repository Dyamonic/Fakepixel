package ru.whileinside.util;

import ru.whileinside.array.FlexibleCharArray;

import java.nio.charset.Charset;

/**
 * Более удобная и оптимизированная версия URLEncoder'а
 *
 * <table>
 *     <tr>
 *         <th>Benchmark</th>
 *         <th>Score</th>
 *         <th>Units</th>
 *     </tr>
 *
 *     <tr>
 *         <td>UrlEncoder</td>
 *         <td>3108,156</td>
 *         <td>ops/ms</td>
 *     </tr>
 *
 *     <tr>
 *         <td>URLEncoder</td>
 *         <td>1481,977</td>
 *         <td>ops/ms</td>
 *     </tr>
 * </table>
 * <p>
 * Тестилось на Java 8, какой результат на Java 12 - хз
 *
 * @author Unidentified Person
 */
public final class UrlEncoder {

    // Одна из оптимизаций
    // вместо BitSet используем массив boolean
    // Да, размер в памяти будет не 256 бита, а 256 байта
    // Но боже, экономить память в наши дни? 256 байт? Серьёзно?
    // (Сказал человек, который высчитывает каждую наносекунду)
    private static final boolean[] SKIP_CHARACTERS;

    static {
        SKIP_CHARACTERS = new boolean[256];
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            SKIP_CHARACTERS[i] = true;
        }
        for (i = 'A'; i <= 'Z'; i++) {
            SKIP_CHARACTERS[i] = true;
        }
        for (i = '0'; i <= '9'; i++) {
            SKIP_CHARACTERS[i] = true;
        }
        SKIP_CHARACTERS[' '] = true;
        SKIP_CHARACTERS['-'] = true;
        SKIP_CHARACTERS['_'] = true;
        SKIP_CHARACTERS['.'] = true;
        SKIP_CHARACTERS['*'] = true;
    }

    // Вместо того, чтобы указывать "UTF-8"
    // и ловить UnsupportedCharsetException, можно
    // просто передать StandardCharsets.UTF-8 и не париться
    // Быстро и удобно
    private final Charset charset;
    // Вторая оптимизация
    // Зачем каждый раз создавать массив из символов,
    // если их можно хранить в отдельной переменной?
    // Плюсы:
    //  - Быстро и удобно
    // Минусы:
    //  - Своя реализация массива (ибо StringBuilder и CharArrayWriter медленные)
    //    > StringBuilder при setLength(0) итерируется по всему массиву и ставит значение на 0
    //    > CharArrayWriter бесмысленные локи
    //  - Нет потокобезосности
    private final FlexibleCharArray result
            = new FlexibleCharArray(16);
    private final FlexibleCharArray hex
            = new FlexibleCharArray(16);

    public UrlEncoder(Charset charset) {
        this.charset = charset;
    }

    public UrlEncoder() {
        this(Charset.defaultCharset());
    }

    private static boolean skip(char v) {
        return v <= SKIP_CHARACTERS.length && SKIP_CHARACTERS[v];
    }

    public String encode(String text) {
        try {
            boolean modified = false;

            for (int i = 0; i < text.length(); ) {
                char c = text.charAt(i);

                if (c == ' ') {
                    modified = true;
                    result.write('+');
                    i++;
                } else if (skip(c)) {
                    result.write(c);
                    i++;
                } else {
                    do {
                        hex.write(c);

                        if (c >= 0xD800 && c <= 0xDBFF) {
                            if ((i + 1) < text.length()) {
                                char d = text.charAt(i + 1);

                                if (d >= 0xDC00 && d <= 0xDFFF) {
                                    hex.write(d);
                                    i++;
                                }
                            }
                        }
                        i++;
                    } while (i < text.length() && !skip((c = text.charAt(i))) && c != ' ');

                    for (byte b : hex.getBytes(charset)) {
                        result.write('%');

                        // Ещё одна оптимизация
                        // Не понимаю, в чём был смысл сначала получать допустим a0
                        // а потом проверять каждый символ, является ли он буквой, а потом
                        // делать её большой?
                        // Почему сразу не высчитывать в верхнем регистре?
                        // Загадка...
                        int codePoint = (b >> 4) & 0xF;
                        result.write(codePoint > 9 ? 'A' + (codePoint - 10) : '0' + codePoint);
                        codePoint = b & 0xF;
                        result.write(codePoint > 9 ? 'A' + (codePoint - 10) : '0' + codePoint);
                    }

                    hex.reset();

                    modified = true;
                }
            }

            return modified ? result.toString() : text;
        } finally {
            result.reset();
        }
    }


}
