package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerGameModeChangeEvent;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.bukkit.HeartBeat;
import lombok.val;
import org.bukkit.GameMode;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@WithAlias("gm")
@NamedCommand("gamemode")
public class GameModeCommand {

    private static final Map<String, GameMode> GM_ALIASES = new HashMap<>();
    static {
        // SURVIVAL
        GM_ALIASES.put("0", GameMode.SURVIVAL);
        GM_ALIASES.put("s", GameMode.SURVIVAL);
        GM_ALIASES.put("survival", GameMode.SURVIVAL);

        // CREATIVE
        GM_ALIASES.put("1", GameMode.CREATIVE);
        GM_ALIASES.put("c", GameMode.CREATIVE);
        GM_ALIASES.put("crea", GameMode.CREATIVE);
        GM_ALIASES.put("creative", GameMode.CREATIVE);

        // ADVENTURE
        GM_ALIASES.put("2", GameMode.ADVENTURE);
        GM_ALIASES.put("a", GameMode.ADVENTURE);
        GM_ALIASES.put("adventure", GameMode.ADVENTURE);

        // SPECTATOR
        GM_ALIASES.put("3", GameMode.SPECTATOR);
        GM_ALIASES.put("sp", GameMode.SPECTATOR);
        GM_ALIASES.put("spec", GameMode.SPECTATOR);
        GM_ALIASES.put("spectator", GameMode.SPECTATOR);
    }

    private static GameMode parseGameMode(String raw) {
        if (raw == null) return null;

        String s = raw.trim().toLowerCase(Locale.ROOT);

        // алиасы и числовые строки
        GameMode byAlias = GM_ALIASES.get(s);
        if (byAlias != null) return byAlias;

        // явное число
        try {
            int n = Integer.parseInt(s);
            GameMode byValue = GameMode.getByValue(n);
            if (byValue != null) return byValue;
        } catch (NumberFormatException ignored) {}

        // enum по имени (например, "CREATIVE", "creative")
        try {
            return GameMode.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        return null;
    }

    @Executor
    @WithCondition("minArgs: 1")
    @WithCondition("disableConsole")
    @WithPermission("command.gamemode")
    public static void executor(BukkitExecutionSession session) throws Exception {
        // читаем первый аргумент как строку и парсим
        String arg0 = session.getArgument(0); // при необходимости замените на ваш метод получения строки
        GameMode gameMode = parseGameMode(arg0);
        if (gameMode == null) {
            throw new CommandTranslationException("gamemode_invalid");
        }

        if (!session.getSender().hasPermission("gamemode." + gameMode.name().toLowerCase(Locale.ROOT))) {
            throw new CommandTranslationException("gamemode_unavailable");
        }

        Gamer player = session.getGamer(), target;

        if (session.getArgumentCount() > 1) {
            if (!session.getSender().hasPermission("gamemode.others")) {
                throw new CommandTranslationException("gamemode_unavailable_others");
            }
            target = session.getGamer(1);
        } else {
            target = session.getGamer();
        }

        val event = new GamerGameModeChangeEvent(target, gameMode, target);
        HeartBeat.callEvent(event);
        if (event.isCancelled()) return;

        target.getPlayer().setGameMode(gameMode);

        if (player != target) {
            player.sendLocalizedMessage("gamemode_target_changed", "%target%", target.getDisplayName());
        }
        target.sendLocalizedMessage("gamemode_changed");
    }
}
