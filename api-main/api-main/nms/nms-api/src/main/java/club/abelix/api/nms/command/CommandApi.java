package club.abelix.api.nms.command;

import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.plugin.Plugin;

public interface CommandApi {

    public static final String COMMAND_REGISTER_NAME = "ageracommand";

    /**
     * Регистрация команды на сервере
     *
     * @param plugin  - плагин, на который будет зарегистрирована команда
     *                (чтобы при нажатии таба было не yapi:, а допустим skyblock:)
     * @param command - команда
     */
    void registerCommand(@NonNull Plugin plugin, @NonNull Command command);

    /**
     * Удалить зареганную команду
     *
     * @param command - команда
     */
    void unregisterCommand(@NonNull String command);

}
