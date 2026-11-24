package club.abelix.api.server.game.team.selector;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SelectorGui extends Gui {

    private static final ItemStack EMPTY_PLAYER_SLOT_ITEM = ItemUtil.getBuilder(Material.STAINED_GLASS_PANE)
            .setName(" ")
            .setDurability(7)
            .build();

    private static final ItemStack NO_AVAILABLE_PLAYERS_ITEM = ItemUtil.getBuilder(Material.BUCKET)
            .setName(Lang.of("team_selector_empty_item_name"))
            .setLore(Lang.of("team_selector_empty_item_lore"))
            .build();

    private static final ItemStack TEAM_IS_FULL_ITEM = ItemUtil.getBuilder(Material.STAINED_GLASS_PANE)
            .setDurability(14)
            .setName(Lang.of("team_selector_full_item_name"))
            .setLore(Lang.of("team_selector_full_item_lore"))
            .build();

    private final Game game;

    @Getter
    private final Gamer gamer;

    private final PagedZone<Gamer> zone;

    private GameGroup group;

    private List<Gamer> otherPlayers = new ArrayList<>();

    public SelectorGui(Game game, Gamer gamer) {
        super(gamer.getPlayer(), Lang.of("team_selector_gui_title"), 6);

        this.game = game;
        this.gamer = gamer;
        this.group = game.getSelectorManager().getGroup(gamer);

        this.zone = new PagedZone<Gamer>(this) {

            @Override
            protected Collection<Gamer> getItems() {
                return otherPlayers;
            }

            @Override
            protected void drawItem(Gamer item, int slot) {
                if (gamer == group.getLeader()) {
                    drawSkull(
                            item, builder -> builder.setLore(translatedLore("team_selector_gui_click_to_invite")),
                            slot, event -> {
                                getPlayer().closeInventory();

                                game.getSelectorManager().sendInvite(gamer, item);
                            }
                    );
                } else {
                    drawSkull(item, slot);
                }
            }
        };

        zone.addSlotRect(1, 1, 7, 2);
        zone.drawButtons(toSlot(3, 3), toSlot(6, 3));

        draw();
    }

    public void updateGroup() {
        group = game.getSelectorManager().getGroup(gamer);
    }

    private void drawSkull(Gamer gamer, Consumer<ItemUtil.Builder> builderCallback, int slot, ClickAction action) {
        ItemUtil.Builder builder = ItemUtil.getBuilder(gamer.getSkull()).setName(gamer.getDisplayName());

        if (builderCallback != null) {
            builderCallback.accept(builder);
        }

        add(builder.build(), slot, action);
    }

    private void drawSkull(Gamer gamer, int slot) {
        drawSkull(gamer, null, slot, null);
    }

    @Override
    public void draw() {
        if (group == null) {
            getPlayer().closeInventory();
            return;
        }

        drawSkull(group.getLeader(), builder -> {
            builder.setLore(Lang.of("team_selector_team_item_lore"));

            if (group.getLeader() == gamer) {
                builder.setName(Lang.of("team_selector_team_leader_item_name"));
                builder.addLore(Lang.of("team_selector_team_leader_item_lore"));
            } else {
                builder.setName(Lang.of("team_selector_team_member_item_name"));
                builder.addLore("lang:team_selector_team_member_item_lore:%leader_name%:"
                        + group.getLeader().getDisplayName() + ";");
            }
        }, toSlot(1, 4), group.getLeader() == gamer ? null : event -> {
            game.getSelectorManager().resetGroup(gamer);

            gamer.sendLocalizedMessage(
                    "team_selector_leave",
                    "%leader_name%", group.getLeader().getDisplayName()
            );

            group.broadcastLocalized(
                    "team_selector_leave_broadcast",
                    member -> new ObjectLinkedBuf("%name%", gamer.getDisplayName())
            );
        });

        for (int i = 3; i < 8; i++) {
            add(EMPTY_PLAYER_SLOT_ITEM, toSlot(i, 4));
        }

        int x = 3;

        for (Gamer gamer : group.getGamers()) {
            if (gamer == group.getLeader()) {
                continue;
            }

            if (this.gamer == group.getLeader()) {
                drawSkull(gamer, builder -> builder.setLore(Lang.of("team_selector_click_to_kick")), toSlot(x++, 4), event -> {
                    if (event.getClick() != ClickType.RIGHT) {
                        return;
                    }

                    game.getSelectorManager().resetGroup(gamer);

                    gamer.sendLocalizedMessage(
                            "team_selector_kicked",
                            "%leader_name%", group.getLeader().getDisplayName()
                    );

                    group.broadcastLocalized(
                            "team_selector_kicked_broadcast",
                            member -> new ObjectLinkedBuf("%name%", gamer.getDisplayName())
                    );
                });
            } else {
                drawSkull(gamer, toSlot(x++, 4));
            }
        }

        if (group.getLeader() != gamer) {
            otherPlayers = Collections.emptyList();
            zone.draw(); // очищаем слоты

            add(ItemUtil.getBuilder(Material.STAINED_GLASS_PANE).setDurability(14)
                            .setName(Lang.of("team_selector_invite_unavailable_item_name"))
                            .setLore(
                                    "lang:team_selector_invite_unavailable_item_lore:%leader_name%:"
                                            + group.getLeader().getDisplayName() + ";")
                            .build(),
                    toSlot(4, 2)
            ); // добавляем предмет

            return;
        }

        if (group.getGamers().size() >= game.getSetting(GameSetting.PLAYERS_PER_TEAM, 1)) {
            otherPlayers = Collections.emptyList();
            zone.draw(); // очищаем слоты

            add(TEAM_IS_FULL_ITEM, toSlot(4, 2)); // добавляем предмет
            return;
        }

        otherPlayers = game.getGamers().stream()
                .filter(gamer -> {
                    if (gamer.isVanished()) {
                        return false;
                    }

                    GameGroup group = game.getSelectorManager().getGroup(gamer);

                    return !group.hasPlayer(this.gamer) && group.getGamers().size() == 1;
                })
                .collect(Collectors.toList());

        zone.draw();

        if (otherPlayers.isEmpty()) {
            add(NO_AVAILABLE_PLAYERS_ITEM, toSlot(4, 2));
        }
    }
}
