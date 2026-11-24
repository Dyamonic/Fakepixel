package club.abelix.core.velocity.skin.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.FallbackExecutor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.data.skin.exception.SkinException;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.commons.util.time.CooldownUtil;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.command.CommandExecutionSession;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.concurrent.TimeUnit;

@NamedCommand("skin")
@RequiredArgsConstructor
public final class SkinCommand {

    private static final String COOLDOWN_KEY = "Skin/Cooldown";

    private final StarshipVelocity starship;

    @WithCondition("disableConsole")
    @WithCondition("onlyAuthorized")
    @SubcommandExecutor("reset")
    public void reset(final CommandExecutionSession session) throws CommandTranslationException {
        val gamer = session.getGamer();
        val playerId = gamer.getPlayerId();
        if (CooldownUtil.hasCooldown(playerId + "_skin_action")) {
            session.sendLocalizedMessage("fast_skin_action");
            return;
        }

        session.getGamer().resetSkin(starship.getSkinsManager());
        session.sendLocalizedMessage("skin_reset_ok");
        CooldownUtil.addCooldown(playerId + "_skin_action", 90, TimeUnit.SECONDS);
        starship.getSkinsManager().notifyCore(session.getGamer().getPlayerId());
    }

    @FallbackExecutor
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithCondition("onlyAuthorized")
    @SubcommandExecutor("set")
    public void set(final CommandExecutionSession session) throws CommandTranslationException {
        val gamer = session.getGamer();
        val playerId = gamer.getPlayerId();
        if (CooldownUtil.hasCooldown(playerId + "_skin_action")) {
            session.sendLocalizedMessage("fast_skin_action");
            return;
        }

        String skinOwner = session.getArgument(0);

        if (skinOwner.equalsIgnoreCase(session.getPlayer().getUsername())) {
            throw new CommandTranslationException("skin_set_error");
        }

        if (!Patterns.isMojangNick(skinOwner)) {
            throw new CommandTranslationException("skin_set_invalid");
        }

        try {
            session.getGamer().setSkin(starship.getSkinsManager(), skinOwner);
            session.sendLocalizedMessage("skin_set_ok", "%skin_name%", skinOwner);
            CooldownUtil.addCooldown(playerId + "_skin_action", 90, TimeUnit.SECONDS);
            starship.getSkinsManager().notifyCore(session.getGamer().getPlayerId());
        } catch (SkinException exception) {
            session.sendMessage(exception.getMessage());
        }
    }

}
