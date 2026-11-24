package club.abelix.api.base.util.chat;

import club.abelix.api.base.gamer.Gamer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@AllArgsConstructor
@RequiredArgsConstructor
public class Broadcaster {

    /**
     * Длина строки для сообщений в чате
     */
    public static final int LINE_LENGTH = 75;
    public static final String[] SPACES = new String[LINE_LENGTH];

    static {
        String space = "";

        for (int i = 0; i < LINE_LENGTH; i++) {
            SPACES[i] = space;

            space += ' ';
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private Supplier<Collection<Gamer>> gamersSupplier = Gamer::getGamers;

    /**
     * Добавляет пустую строку, которая будет выведена всем игрокам.
     */
    public void add() {
        add("");
    }

    /**
     * Добавеляет строку, которая будет выведена всем игрокаь.
     *
     * @param text Текст строки
     */
    public void add(@NonNull String text) {
        this.lines.add(new Line(text, Align.LEFT, null));
    }

    public void addCenter(@NonNull String text) {
        this.lines.add(new Line(text, Align.CENTER, null));
    }

    /**
     * Возвращает все линии отправляемые игроку
     *
     * @param target - игрок
     * @return - линии для игрока
     */
    public List<Line> getLines(@NonNull Player target) {
        return lines.stream().filter(line -> line.target == target)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает все отпраляемые линии
     *
     * @return - линии для игроков
     */
    public List<Line> getLines() {
        return lines;
    }

    /**
     * Добавлеяет новую строку с указанным текстом, которая будет выводится только указанному игроку.
     *
     * @param player Игрок
     * @param text   Текст строки
     */
    public void add(@NonNull Player player, @NonNull String text) {
        this.lines.add(new Line(text, Align.LEFT, player));
    }

    /**
     * Добавлеяет новую строку с указанным текстом, которая будет выводится только указанному игроку.
     *
     * @param gamer Игрок
     * @param text  Текст строки
     */
    public void add(@NonNull Gamer gamer, @NonNull String text) {
        add(gamer.getPlayer(), text);
    }

    /**
     * Добавлеяет новую строку с указанным текстом, которая будет выводится только указанному игроку.
     *
     * @param player Игрок
     * @param text   Текст строки
     */
    public void addCenter(@NonNull Player player, @NonNull String text) {
        this.lines.add(new Line(text, Align.CENTER, player));
    }

    /**
     * Добавлеяет новую строку с указанным текстом, которая будет выводится только указанному игроку.
     *
     * @param gamer Игрок
     * @param text  Текст строки
     */
    public void addCenter(@NonNull Gamer gamer, @NonNull String text) {
        addCenter(gamer.getPlayer(), text);
    }

    /**
     * Добавлеяет новую строку с указанным текстом, которая будет выводится только указанному игроку.
     *
     * @param player Игрок
     * @param lines  Текст строки
     */
    public void addAll(@NonNull Player player, @NonNull Iterable<String> lines) {
        for (String line : lines) {
            add(player, line);
        }
    }

    /**
     * Добавлеяет новую строку с указанным текстом, которая будет выводится только указанному игроку.
     *
     * @param gamer Игрок
     * @param lines Текст строки
     */
    public void addAll(@NonNull Gamer gamer, @NonNull Iterable<String> lines) {
        addAll(gamer.getPlayer(), lines);
    }

    /**
     * Выравнивает последнюю добавленную строку.
     *
     * @param align Тип выравнивания
     */
    public void align(@NonNull Align align) {
        last().align = align;
    }

    protected Line last() {
        if (this.lines.isEmpty()) {
            throw new IllegalStateException("No lines present");
        }

        return this.lines.get(this.lines.size() - 1);
    }

    public int size() {
        return this.lines.size();
    }

    /**
     * Отправляет все указанные строки.
     */
    public void broadcast() {
        for (Gamer gamer : gamersSupplier.get()) {
            Player player = gamer.getPlayer();

            List<String> lines = new ArrayList<>(this.lines.size());
            List<Align> aligns = new ArrayList<>(this.lines.size());

            for (Line line : this.lines) {
                if (line.target == player || line.target == null) {
                    lines.add(line.text);
                    aligns.add(line.align);
                }
            }

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Align align = aligns.get(i);

                switch (align) {
                    case LEFT -> player.sendMessage(line);
                    case CENTER ->
                            player.sendMessage(SPACES[(LINE_LENGTH - ChatColor.stripColor(line).length()) / 2] + line);
                    case RIGHT ->
                            player.sendMessage(SPACES[LINE_LENGTH - ChatColor.stripColor(line).length()] + line);
                }
            }
        }
    }

    public enum Align {

        LEFT, CENTER, RIGHT

    }

    @Getter
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class Line {

        private final String text;
        private Align align;
        private final Player target;

    }

}