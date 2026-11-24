package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.cooldown.WithCooldown;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.concurrent.ThreadLocalRandom;

@NamedCommand("firework")
@WithAlias("fw")
@IgnoreLogs
public class FireWorkCommand {

    private static final Color[] FIREWORK_COLORS = new Color[]{
            Color.AQUA, Color.FUCHSIA, Color.ORANGE, Color.GREEN,
            Color.YELLOW, Color.RED, Color.SILVER
    };

    @Executor
    @WithCooldown(15000)
    @WithPermission("command.fw")
    @WithCondition("disableConsole")
    public static void executor(BukkitExecutionSession session) throws Exception {
        if (!CoreUtil.isLobby()) {
            throw new CommandTranslationException("command_firework_ingame");
        }

        Location location = session.getPlayer().getLocation();

        Firework firework = location.getWorld().spawn(location, Firework.class);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Color color = FIREWORK_COLORS[random.nextInt(FIREWORK_COLORS.length)];

        FireworkMeta fireworkEffectMeta = firework.getFireworkMeta();
        fireworkEffectMeta.addEffect(FireworkEffect.builder()
                .withColor(color).withFade(color).withFlicker().with(FireworkEffect.Type.BALL)
                .build());

        firework.setFireworkMeta(fireworkEffectMeta);
    }
}
