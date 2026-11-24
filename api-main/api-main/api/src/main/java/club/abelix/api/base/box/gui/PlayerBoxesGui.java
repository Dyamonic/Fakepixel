package club.abelix.api.base.box.gui;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.box.Box;
import club.abelix.api.base.box.entity.BoxDto;
import club.abelix.api.base.box.reward.BoxReward;
import club.abelix.api.base.box.util.RngUtil;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.base.gui.ConfirmationGui;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import lombok.val;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerBoxesGui extends Gui {

    private static final String UNAVAILABLE_SKULL_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWJiZmVmZTk3ODY0ODY1YTE5YWFhZDBmN2RjNDdiNmMzMTE3OTFiNGMzY2E4MTE1ZWYwN2FkOTlhYjU1NjU3ZCJ9fX0=";

    private static final String SHOP_SKULL_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTVmMWFkYjU4ZGI2ZTJlNTRhODQ3MzliMmM3OWRkZDQwMTRiODVmNzY1MTFkZjQxYTkyNzhkNzE1MWY2Y2RiZiJ9fX0=";

    private static final int[] BORDER = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 53};

    private final PagedZone<BoxDto> zone;
    private final List<BoxDto> boxes;

    public PlayerBoxesGui(@NonNull Gamer gamer) {
        super(gamer, Lang.of("player_boxes_gui"), 6);

        this.boxes = new ArrayList<>(gamer.getBoxes());
        this.zone = new PagedZone<>(this) {
            @Override
            protected Collection<BoxDto> getItems() {
                return boxes;
            }

            @Override
            protected void drawItem(BoxDto dto, int slot) {
                var boxOpt = FalconAPI.getInstance()
                        .getBoxStorageService()
                        .findByTypeAndRarity(dto.getBoxTypeId(), dto.getRarity());

                if (boxOpt.isEmpty()) {
                    add(createItem(dto, null), slot, __ -> {
                        gamer.sendLocalizedMessage("box_unavailable_on_server");
                        gamer.playSound(YSound.VILLAGER_NO);
                    });
                    return;
                }

                var box = boxOpt.get();

                add(createItem(dto, box), slot, __ -> {
                    if (!isAvailable(dto)) {
                        gamer.sendLocalizedMessage("box_unavailable_on_server");
                        gamer.playSound(YSound.VILLAGER_NO);
                        return;
                    }

                    gamer.playSound(YSound.CLICK);
                    getPlayer().closeInventory();

                    FalconAPI.getInstance().getBoxOpenService().open(
                            getGamer(),
                            dto,
                            box
                    );
                });
            }
        };

        drawBorder(BORDER);
        this.zone.addSlotRect(1, 1, 7, 4);
        this.draw();
    }

    @Override
    public void draw() {
        if (zone != null) {
            zone.draw();
            zone.drawButtons(48, 50);
        }

        add(ItemUtil.getSkullBuilder(SHOP_SKULL_TEXTURE)
                .setName(translated("box_shop_item_name"))
                .setLore(translatedLore("box_shop_item_lore"))
                .addItemFlags()
                .build(), 46, __ -> player.chat("/box shop"));

        val item = ItemUtil.getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTI3MjRkZjI5NjY2ZGEwMjc0NzM1Y2M5YzI5Y2FkMWZkZDk2ZTVhYWE5YTBiYzAyNDZmOWEyYjQ1Mjg1MjEyYyJ9fX0=");
        boolean canOpen = gamer.hasBoxes() && gamer.getRanks().hasPermission("box.mass.open");

        item.setName(translated("box_mass_open_item_name"));
        item.addLore(translatedLore("box_mass_open_item_lore"));

        if (canOpen) {
            item.addLore(translatedLore("box_boxes_available", "%available%", gamer.getBoxes().size()));
        } else {
            item.addLore(translatedLore("box_mass_open_no_boxes"));
        }

        item.addItemFlags();

        add(item.build(), 52, __ -> {
            if (!gamer.hasBoxes()) {
                gamer.playSound(YSound.VILLAGER_NO);
                gamer.sendLocalizedMessage("box_mass_open_no_boxes");
                return;
            }

            if (!gamer.getRanks().hasPermission("box.mass.open")) {
                gamer.playSound(YSound.VILLAGER_NO);
                gamer.sendLocalizedMessage("box_mass_open_ultra_required");
                return;
            }

            gamer.playSound(YSound.CLICK);

            new ConfirmationGui(player, Lang.of("box_mass_open_confirmation_gui"), action -> {
                val result = new ArrayList<BoxDto>();

                for (val box : gamer.getBoxes()) {
                    if (box.getServerMode() != null) {
                        if (CoreUtil.getMaster() != null) {
                            if (CoreUtil.getMaster().getName().equals(box.getServerMode())) {
                                result.add(box);
                            }
                        }
                    } else {
                        result.add(box);
                    }
                }

                int totalOpened = 0;

                for (BoxDto boxDto : result) {
                    val boxOptional = FalconAPI.getInstance()
                            .getBoxStorageService()
                            .findByTypeAndRarity(boxDto.getBoxTypeId(), boxDto.getRarity());

                    if (boxOptional.isEmpty()) {
                        gamer.sendLocalizedMessage("box_not_found",
                                "%boxType%", boxDto.getBoxTypeId(),
                                "%rarity%", boxDto.getRarity().name(),
                                "%id%", boxDto.getId()
                        );
                        continue;
                    }

                    val box = boxOptional.get();

                    if (!isAvailable(boxDto)) continue;

                    val reward = RngUtil.pick(box);

                    if (!reward.apply(gamer)) {
                        val equivalentType = reward.getEquivalentType();
                        val equivalent = reward.getEquivalent(gamer);

                        equivalentType.giveDuplicateEquivalent(gamer, equivalent);
                        equivalentType.sendDuplicateEquivalentMessage(gamer, equivalent);
                    }

                    totalOpened++;
                }

                gamer.sendLocalizedMessage("box_mass_open_totally_opened", "%total%", totalOpened);
                gamer.clearBoxes();
            });

        });

        generateCloseItem(49);
    }

    private ItemStack createItem(@NonNull BoxDto dto, Box box) {
        ItemUtil.Builder builder;

        if (!isAvailable(dto) || box == null) {
            builder = ItemUtil.getSkullBuilder(UNAVAILABLE_SKULL_TEXTURE);
            builder.setName(translated("box_unavailable_on_server_name"));
            builder.setLore(translatedLore("box_lore_unavailable_on_server",
                    "%availableOn%", dto.getServerMode()));

            return builder.build();
        }

        var nameKey = box.getNameKey();
        var rarity = box.getRarity();

        builder = ItemUtil.getBuilder(box.getIcon(getGamer()));
        builder.setLore(translatedLore("box_" + nameKey + "_lore"));

        builder.setName(translated("box_" + nameKey + "_name",
                "%stars%", rarity.getStars()));
        builder.addItemFlags();
        builder.glowing(rarity == Rarity.LEGENDARY);

        return builder.build();
    }

    private boolean isAvailable(@NonNull BoxDto dto) {
        var mode = dto.getServerMode();
        if (mode == null) return true;

        var master = CoreUtil.getMaster();
        return master != null && mode.equalsIgnoreCase(master.getName());
    }
}
