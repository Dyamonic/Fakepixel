package club.abelix.commons.util.string;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class StringUtil {

    private final int LINE_LENGTH = 70; //длина строки для StringToCenter

    // remove non-printable and non-cyrillic chars
    public String retainAsciiAndCyrillic(@NonNull String str) {
        // ' -}' means all chars from space to '}'
        return str.replaceAll("[^ -}а-яА-ЯёЁ]|['`]", "").trim();
    }

    public boolean startsWith(String haystack, String needle, boolean ignoreCase) {
        return haystack.regionMatches(ignoreCase, 0, needle, 0, needle.length());
    }

    public String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // получает слова из данной строки
    public String[] getWords(@NonNull String str) {
        List<String> words = new ArrayList<>(Arrays.asList(str.trim().toLowerCase().split("[^а-яёa-z]")));

        words.removeIf(s -> s.length() < 2);

        return words.toArray(new String[0]);
    }

    // проверка на совпадение первых N символов подряд
    public static boolean hasEqualStart(String line, String str, int n) {
        if (line.equals(str)) {
            return true;
        }

        char[] wordCharacters = line.toCharArray();
        char[] targetCharacters = str.toCharArray();

        // если длина меньше чем нам надо совпадающих символов - обрезаем лимит
        if (wordCharacters.length < n) {
            n = wordCharacters.length;
        }

        int similarChars = 0;

        for (int i = 0; i < wordCharacters.length; i++) {
            // если позиция текущего символа больше длины другой строки
            // или если символы не совпадают - больше итераций нам не нужно
            if (i >= targetCharacters.length || targetCharacters[i] != wordCharacters[i]) {
                break;
            }

            similarChars++;
        }

        // если совпадающих символов подряд >= N, true
        // если нет - false
        return similarChars >= n;
    }

    // сравнивает 2 слова на похожесть
    public boolean isWordSimilar(@NonNull String word, @NonNull String str) {
        if (word.equals(str)) {
            return true;
        }

        // рандомно выбранный коэффициент, мб требуется подстройка
        return LevenshteinDistance.compute(word, str) < Math.max(word.length(), str.length()) * 0.33;
    }

    // сравнивает слова из данной строки с указанными словами
    public boolean isSimilar(@NonNull String str, @NonNull String[] words) {
        String[] w = getWords(str);

        int similar = 0;
        for (String word : words) {
            for (String otherWord : w) {
                if (isWordSimilar(word, otherWord)) {
                    similar++;
                    break;
                }
            }
        }

        return similar >= (w.length + words.length) * 0.5 * 0.6;
    }

    public static String concat(@NonNull String[] parts, @NonNull String delim) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            builder.append(parts[i]);

            if (i < parts.length - 1) {
                builder.append(delim);
            }
        }

        return builder.toString();
    }

    // получить количество похожих слов
    public static int getSimilarCount(@NonNull String[] w, @NonNull String[] words) {
        int similar = 0;
        for (String word : words) {
            for (String otherWord : w) {
                if (isWordSimilar(word, otherWord)) {
                    similar++;
                    break;
                }
            }
        }

        return similar;
    }

    public String stringToCenter(String text) {
        if (text != null && text.length() <= LINE_LENGTH) {
            return repeat(' ', (LINE_LENGTH - textLength(text)) / 2) + text;
        }
        return text;
    }

    public String repeat(final char value, final int length) {
        val chars = new char[length];
        Arrays.fill(chars, value);

        return new String(chars);
    }

    private int textLength(String text) {
        int count = 0;
        char[] array = text.toCharArray();
        for (char symbol : array) {
            if (symbol == '§') {
                count += 2;
            }
        }

        return text.length() - count;
    }
}
