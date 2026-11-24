package club.abelix.api.base.vote.gui;

import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.vote.VotePlatform;
import club.abelix.commons.data.vote.VoteSqlHandler;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.TimeUtil;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.util.Collection;

public class VoteGui extends Gui {

    private final int[][] SLOTS = new int[][]{
            {toSlot(4, 1)},
            {toSlot(3, 1), toSlot(5, 1)},
            {toSlot(2, 1), toSlot(4, 1), toSlot(6, 1)},
            {toSlot(1, 1), toSlot(3, 1), toSlot(5, 1), toSlot(7, 1)},
            {toSlot(1, 1), toSlot(3, 1), toSlot(5, 1), toSlot(7, 1), toSlot(4, 2)},
            {toSlot(1, 1), toSlot(3, 1), toSlot(5, 1), toSlot(7, 1), toSlot(2, 2), toSlot(6, 2)},
            {toSlot(1, 1), toSlot(3, 1), toSlot(5, 1), toSlot(7, 1), toSlot(2, 2), toSlot(4, 2) ,toSlot(6, 2)},
            {toSlot(1, 1), toSlot(3, 1), toSlot(5, 1), toSlot(7, 1), toSlot(1, 3), toSlot(3, 3), toSlot(5, 3), toSlot(7, 3)},
    };

    private static final ItemStack INFO_ITEM = ItemUtil.getBuilder(Material.BOOK)
            .setName(Lang.of("vote_gui_info"))
            .setLore(Lang.of("vote_gui_info_lore"))
            .build();

    private final PagedZone<VotePlatform> zone;

    public VoteGui(@NonNull Player player) {
        super(player, Lang.of("vote_gui_title"), 5);

        val voteData = gamer.getPlayerVoteData();

        val totalComboVotes = gamer.getProperty("ComboVotes");
        val lastComboDay = gamer.getProperty("LastComboVote");

        String lastComboVote = lastComboDay == 0 ?
                language.getMessage("never") :
                TimeUtil.getDateString(language, LocalDate.ofEpochDay(lastComboDay));

        zone = new PagedZone<>(this) {
            @Override
            protected Collection<VotePlatform> getItems() {
                return VoteSqlHandler.INSTANCE.getActiveVotePlatforms();
            }

            @Override
            protected void drawItem(VotePlatform platform, int slot) {
                val voted = voteData.isVotedToday(platform);

                ClickAction action = null;

                val itemBuilder = ItemUtil.getBuilder(voted ? Material.MINECART : Material.STORAGE_MINECART);
                itemBuilder.setName(language.getMessage(
                        "vote_gui_entry_" + (voted ? "unavailable" : "available"),
                        "%platform%", platform.name()
                ));
                itemBuilder.addLore(
                        language.getMessageList("vote_gui_entry_lore",
                                "%combo_votes%", totalComboVotes,
                                "%last_combo_vote%", lastComboVote
                        )
                );

                if (voted) {
                    itemBuilder.addLore(language.getMessageList("vote_gui_entry_lore_already",
                            "%left_time%", Duration.durationWordsMs(language,
                                    VoteSqlHandler.VOTE_INTERVAL - voteData.getLastVoteTime(platform)))
                    );
                } else {
                    action = clickEvent -> {
                        val component = new TextComponent(
                                language.getMessage("vote_redirect_message")
                        );

                        component.setHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        new BaseComponent[]{
                                                new TextComponent(language.getMessage("vote_redirect_message_hover"))
                                        }
                                )
                        );

                        component.setClickEvent(
                                new ClickEvent(ClickEvent.Action.OPEN_URL, platform.url())
                        );

                        player.spigot().sendMessage(component);
                        player.closeInventory();
                    };

                    itemBuilder.addLore(
                            language.getMessageList(
                                    "vote_gui_entry_lore_available",
                                    "%votes%", voteData.getVoteCount(platform)
                            )
                    );
                }

                add(itemBuilder.build(), slot, action);
            }
        };

        int platformCount = VoteSqlHandler.INSTANCE.getActiveVotePlatforms().size();

        int[] slots = new int[0];
        if (platformCount > 0) {
            int platformIndex = platformCount - 1;
            slots = SLOTS[platformIndex];
        }

        for (int slot : slots) {
            this.zone.addSlot(slot);
        }

        draw();
    }

    @Override
    public void draw() {
        val votes = gamer.getProperty("Votes");

        val shopItem = ItemUtil.getBuilder(Material.EMERALD)
                .setName(language.getMessage("vote_gui_shop"))
                .setLore(language.getMessageList("vote_gui_shop_lore",
                        "%votes%", votes))
                .build();

        add(INFO_ITEM, toSlot(3, 4));
        add(shopItem, toSlot(5, 4), clickEvent -> new VoteShopGui(getPlayer(), this));

        //рисуем страницы
        this.zone.draw();
        //кнопки назад/вперед
        this.zone.drawButtons(toSlot(1, 3), toSlot(7, 3));
    }
}