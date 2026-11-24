package club.abelix.games.fastbridge.commands;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.game.animation.AnimatedBlockRemoversStorage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@NamedCommand("animation")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SelectAnimationCommand {

    AnimatedBlockRemoversStorage animationStorage;

    @Executor
    @WithCondition("group: ADMINISTRATOR")
    public void handle(final BukkitExecutionSession session) {
        val animationType = session.getArgument(0);

        if (animationType == null) {
            session.sendLocalizedMessage("invalid_animation_name");
            return;
        }

        val remover = animationStorage.findByClassName(animationType);
        FastBridgeGamer.get(session.getGamer()).setBlockRemover(remover);
        session.sendLocalizedMessage("animation_selected", "%animation%", animationType);
    }

}
