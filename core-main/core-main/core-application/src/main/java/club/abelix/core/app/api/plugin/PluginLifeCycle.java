package club.abelix.core.app.api.plugin;

import club.abelix.commons.util.color.ChatColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>PluginLifeCycle creation on 12.09.2022 at 09:29</p>
 */

@Getter
@RequiredArgsConstructor
public enum PluginLifeCycle {
    DISABLE(ChatColor.GRAY), // Плагин ожидает запуска
    LOAD(ChatColor.AQUA), // Загружен
    ENABLE(ChatColor.GREEN), // Запущен
    ENABLE_ERROR(ChatColor.RED); // Ошибка при запусук

    private final ChatColor color;

    public String getColoredName() {
        return String.format("%s%s", color.toString(), name());
    }

}
