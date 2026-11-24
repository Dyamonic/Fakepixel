package club.abelix.api.base.util.scoreboard.scroller;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReflectScroller extends DefaultScroller {

    private static final String DEFAULT_PREF_COLOR = "§d";
    private static final String DEFAULT_SCROLLER_COLOR = "§f";
    private static final String DEFAULT_SCROLLER_PREF_COLOR = "§5";

    boolean isBold;

    String prefixColor;
    String scrollerColor;
    String scrollerPrefixColor;

    public ReflectScroller(boolean isBold, String prefixColor, String scrollerColor, String scrollerPrefixColor, String word) {
        super(generateAnimationLines(isBold, prefixColor, scrollerColor, scrollerPrefixColor, word));

        this.isBold = isBold;

        this.prefixColor = prefixColor;
        this.scrollerColor = scrollerColor;
        this.scrollerPrefixColor = scrollerPrefixColor;
    }

    public ReflectScroller(boolean isBold, String word) {
        this(isBold, DEFAULT_PREF_COLOR, DEFAULT_SCROLLER_COLOR, DEFAULT_SCROLLER_PREF_COLOR, word);
    }

    public ReflectScroller(String word) {
        this(true, ChatColor.stripColor(word));
    }

    private static List<String> generateAnimationLines(boolean isBold, String prefixColor, String scrollerColor, String scrollerPrefixColor, String word) {
        List<String> list = new ArrayList<>();

        prefixColor += (isBold ? "§l" : "");
        scrollerColor += (isBold ? "§l" : "");
        scrollerPrefixColor += (isBold ? "§l" : "");

        // перед началом добавим несколько одинаковых строк для сохранения задержки перед анимацией
        for (int begin = 0; begin < 7; begin++) {
            list.add(prefixColor + word);
        }

        char[] charsArray = word.toCharArray();
        for (int in = charsArray.length - 1; in >= 0; in--) {

            // обозначаем знак, который будет ведущим для скроллера
            char sign = charsArray[in];

            // Создаем строку
            StringBuilder lineBuilder = new StringBuilder(
                    prefixColor + word.substring(0, in)
            );

            // добавляем цвет для ведущей буквы
            lineBuilder.append(scrollerPrefixColor);
            lineBuilder.append(sign);

            // затем обрезаем и добавляем 3 буквы самого скроллера
            lineBuilder.append(scrollerColor);

            String scrolledSigns = word.substring(in + 1);
            String beforeSigns = null;

            if (scrolledSigns.length() > 3) {
                beforeSigns = scrolledSigns.substring(3);
                scrolledSigns = scrolledSigns.substring(0, 3);
            }

            lineBuilder.append(scrolledSigns);

            // остальные буквы после скроллера окрашиваем в цвет префикса всей строки
            if (beforeSigns != null) {
                lineBuilder.append(prefixColor).append(beforeSigns);
            }

            // ну и добавляем ее в итоговый лист анимации
            list.add(lineBuilder.toString());
        }

        // доводим оставшиеся 3 буквы скроллера до ума
        String lastScroll = word.substring(0, 3);
        list.add(scrollerColor + lastScroll + prefixColor + word.substring(3));

        for (int in = 2; in >= 0; in--) {

            char lastSign = lastScroll.charAt(in);
            lastScroll = lastScroll.substring(0, lastScroll.length() - 1);

            String beforeScroll = lastSign + word.substring(lastScroll.length() + 1);

            list.add((lastScroll.isEmpty() ? "" : scrollerColor) + lastScroll + prefixColor + beforeScroll);
        }

        return list;
    }
}
