package club.abelix.api.base.box.hotbar;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.box.animation.state.BoxAnimationState;
import club.abelix.api.base.hotbar_new.ActionType;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.base.hotbar_new.builder.HotbarBuilder;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.api.annotation.PostConstruct;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import lombok.val;

public class BoxOpenHotbar {

    public static final String HOTBAR_ID = "box_open_hotbar";

    @Autowired
    private HotbarService hotbarService;

    public BoxOpenHotbar() {
        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);
    }

    @PostConstruct
    public void register() {
        val hotbar = HotbarBuilder.of(HOTBAR_ID)
                .slot(4)
                .item(ItemUtil.getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjEyZjNlZmU4NGEwZjY2NDZhODBkNDVjZWZlNDE4ZTE5OWQ5NjE5ZjhjMWZiNWY1YzVjMDA4YzYwMzA1OWFjMyJ9fX0=")
                        .setName(Lang.of("box_cancel_opening"))
                        .build())
                .on(ActionType.RIGHT_CLICK, action -> {
                    val gamer = action.gamer();
                    val contextOptional = FalconAPI.getInstance().getAnimationService().getOpeningContext(gamer);

                    if (contextOptional.isEmpty()) {
                        // Should not happen
                        return;
                    }

                    val context = contextOptional.get();

                    if (context.getState() == BoxAnimationState.REWARD_CHOSEN) {
                        FalconAPI.getInstance().getAnimationService().finishAnimation(context);
                        return;
                    }

                    gamer.sendLocalizedMessage("box_cant_cancel_now");
                })
                .done()
                .build();

        hotbarService.registerHotbar(hotbar);
    }
}