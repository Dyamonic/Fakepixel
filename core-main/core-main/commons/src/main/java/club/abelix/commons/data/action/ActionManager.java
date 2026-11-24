package club.abelix.commons.data.action;

import club.abelix.commons.language.Language;
import club.abelix.commons.util.IpAddressUtil;
import club.abelix.commons.util.time.Duration;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public class ActionManager {

    public static void save(
            final @NonNull String name,
            final @NonNull ActionType type,
            final int vk
    ) {
        save(name, null, type, vk);
    }

    public static void save(
            final @NonNull String name,
            final @Nullable String ip,
            final @NonNull ActionType type
    ) {
        save(name, ip, type, 0);
    }

    public static void save(
            final @NonNull String name,
            final @NonNull ActionType type
    ) {
        save(name, null, type, 0);
    }

    public static void save(
            final @NonNull String name,
            final @Nullable String ip,
            final @NonNull ActionType type,
            final int vk
    ) {
        val action = new Action(name, ip, LocalDateTime.now(), type, vk);
        ActionSqlHandler.INSTANCE.addAction(action);
    }

    public static String getActionText(
            final @NonNull Language language,
            final @NonNull Action action,
            final boolean isIp
    ) {
        val description = action.getType().getDescription(language);
        val timeString = Duration.wordsBetween(language, action.getTime(), LocalDateTime.now());

        String text = language.getMessage("action_text", "%description%", description, "%time%", timeString);

        if (isIp) {
            text += language.getMessage("action_text_name", "%name%", action.getName());
        } else if (action.hasIp()) {
            text += language.getMessage("action_text_ip", "%ip%", IpAddressUtil.hideLastPart(action.getIp()));
        }

        if (action.hasVk()) {
            text += language.getMessage("action_text_vk", "%vk_id%", action.getVk());
        }

        return text;
    }
}
