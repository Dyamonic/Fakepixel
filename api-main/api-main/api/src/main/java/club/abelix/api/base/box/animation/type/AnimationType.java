package club.abelix.api.base.box.animation.type;

import club.abelix.api.base.box.animation.BoxOpenAnimation;
import club.abelix.api.base.box.animation.standard.StandardBlockBasedBoxOpenAnimation;
import club.abelix.api.base.box.animation.standard.StandardEntityBasedBoxOpenAnimation;
import club.abelix.api.base.box.context.BoxOpenContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;

import java.util.function.Function;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum AnimationType {

    BLOCK_CHEST(1, (ctx) -> new StandardBlockBasedBoxOpenAnimation(ctx, Material.ENDER_CHEST)),
    BLOCK_ENDER_PORTAL_FRAME(2, (ctx) -> new StandardBlockBasedBoxOpenAnimation(ctx, Material.ENDER_PORTAL_FRAME)),
    BLOCK_CAULDRON(3, (ctx) -> new StandardBlockBasedBoxOpenAnimation(ctx, Material.CAULDRON)),
    BLOCK_BEACON(4, (ctx) -> new StandardBlockBasedBoxOpenAnimation(ctx, Material.BEACON)),
    BLOCK_ENCHANT_TABLE(5, (ctx) -> new StandardBlockBasedBoxOpenAnimation(ctx, Material.ENCHANTMENT_TABLE)),
    ENTITY_CLONE(6, StandardEntityBasedBoxOpenAnimation::new)
    ;

    int id;
    Function<BoxOpenContext, BoxOpenAnimation> factory;

    private static final AnimationType DEFAULT = BLOCK_ENDER_PORTAL_FRAME;

    public BoxOpenAnimation create(BoxOpenContext ctx) {
        return factory.apply(ctx);
    }

    public static AnimationType getById(int id) {
        for (AnimationType type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return DEFAULT;
    }
}
