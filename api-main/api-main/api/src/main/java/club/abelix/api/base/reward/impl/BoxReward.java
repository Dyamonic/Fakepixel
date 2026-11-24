package club.abelix.api.base.reward.impl;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.box.Box;
import club.abelix.api.base.box.service.BoxesService;
import club.abelix.api.base.box.service.BoxesStorageService;
import club.abelix.api.base.box.type.BoxType;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.language.Language;
import club.abelix.network.type.mode.ServerMode;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@Slf4j
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class BoxReward implements Reward {

    static final BoxesService BOXES_SERVICE = FalconAPI.getInstance().getBoxesService();
    static final BoxesStorageService BOXES_STORAGE_SERVICE = FalconAPI.getInstance().getBoxStorageService();

    ServerMode mode;
    BoxType boxType;
    Rarity rarity;
    int amount;

    public BoxReward(final BoxType boxType) {
        this(boxType, 1);
    }

    public BoxReward(final BoxType boxType, Rarity rarity) {
        this.amount = 1;
        this.mode = null;
        this.boxType = boxType;
        this.rarity = rarity;
    }

    public BoxReward(
            final BoxType boxType,
            final int amount
    ) {
        this.amount = amount;
        this.mode = null;
        this.boxType = boxType;
        this.rarity = null;
    }

    @Override
    public boolean give(@NonNull Gamer gamer) {
        val box = findBox(boxType, rarity)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Box with rarity " + boxType.getName(gamer.getLanguage()) + " not found."
                ));

        boolean modeNameBound = mode != null && box.getMode() != null;
        val modeName = modeNameBound ? mode.getName() : null;

        if (amount < 1) {
            BOXES_SERVICE.giveBox(gamer, box.getRarity(), modeName, boxType.getId(), box.getType());
        } else {
            BOXES_SERVICE.giveBoxes(gamer, box.getRarity(), modeName, boxType.getId(), box.getType(), amount);
        }

        return true;
    }

    private Optional<Box> findBox(BoxType type, Rarity rarity) {
        return (rarity == null)
                ? BOXES_STORAGE_SERVICE.findByType(type)
                : BOXES_STORAGE_SERVICE.findByTypeAndRarity(type, rarity);
    }

    @Override
    public String getRewardName(@NonNull Language language) {
        return language.getMessage(
                "reward_case_key_name",
                "%type%", boxType.getName(language),
                "%amount%", amount
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.ENDER_CHEST);
    }
}
