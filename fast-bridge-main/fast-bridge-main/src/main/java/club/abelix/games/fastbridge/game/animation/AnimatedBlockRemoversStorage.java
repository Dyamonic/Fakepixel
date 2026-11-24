package club.abelix.games.fastbridge.game.animation;

import club.abelix.games.fastbridge.game.animation.standard.DefaultBlockRemoverAnimation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnimatedBlockRemoversStorage {

    HashMap<Class<? extends AnimatedBlockRemover>, AnimatedBlockRemover> blockRemoversByClass;

    public static @NotNull AnimatedBlockRemoversStorage create() {
        val def = new DefaultBlockRemoverAnimation();
        val storage = new AnimatedBlockRemoversStorage(
                new HashMap<>()
        );

        storage.register(def);
        return storage;
    }

    public void register(final AnimatedBlockRemover remover) {
        blockRemoversByClass.put(remover.getClass(), remover);
    }

    public @NotNull AnimatedBlockRemover findDefault() {
        return blockRemoversByClass.get(DefaultBlockRemoverAnimation.class);
    }

    public AnimatedBlockRemover findByClass(@NotNull final Class<? extends AnimatedBlockRemover> cls) {
        return blockRemoversByClass.getOrDefault(cls, findDefault());
    }

    public AnimatedBlockRemover findByClassName(@NotNull final String className) {
        AnimatedBlockRemover result = null;

        for (val entry : blockRemoversByClass.entrySet()) {
            if (entry.getKey().getSimpleName().equals(className)) {
                result = entry.getValue();
                break;
            }
        }

        return result;
    }
}
