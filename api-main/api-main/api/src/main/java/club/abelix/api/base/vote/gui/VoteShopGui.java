package club.abelix.api.base.vote.gui;

import club.abelix.api.base.box.type.StandardBoxType;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.reward.Reward;
import club.abelix.api.base.reward.impl.*;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.core.bukkit.StarshipBukkit;
import lombok.NonNull;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class VoteShopGui extends Gui {

    private static final Map<Reward, Integer> VOTE_REWARDS
            = new LinkedHashMap<>();

    static {
        VOTE_REWARDS.put(new ExperienceReward(2500), 12);
        VOTE_REWARDS.put(new ExperienceReward(9500), 40);
        VOTE_REWARDS.put(new ExperienceReward(19000), 60);

        VOTE_REWARDS.put(new BoxReward(StandardBoxType.EXPERIENCE, Rarity.COMMON), 60);
        VOTE_REWARDS.put(new BoxReward(StandardBoxType.TITLES, Rarity.COMMON), 70);
        VOTE_REWARDS.put(new BoxReward(StandardBoxType.TITLES, Rarity.RARE), 90);

        VOTE_REWARDS.put(new GroupReward(StarshipBukkit.getInstance().getRanksService().findRankById(2).orElseThrow(), 604800), 120);
        VOTE_REWARDS.put(new GroupReward(StarshipBukkit.getInstance().getRanksService().findRankById(3).orElseThrow(), 604800), 160);
    }

    private final PagedZone<Reward> zone;

    public VoteShopGui(@NonNull Player player, Gui parent) {
        super(player, Lang.of("vote_shop_gui_title"), 5);

        setParent(parent);

        val gamer = getGamer();
        val language = getLanguage();

        val votes = gamer.getVotes();

        zone = new PagedZone<Reward>(this) {
            @Override
            protected Collection<Reward> getItems() {
                return VOTE_REWARDS.keySet();
            }

            @Override
            protected void drawItem(Reward reward, int slot) {
                int cost = VOTE_REWARDS.get(reward);

                ClickAction action;

                val rewardName = ChatColor.stripColor(reward.getRewardName(language));

                val builder = ItemUtil.getBuilder(reward.getRewardIcon(language));
                builder.addItemFlags();
                builder.setName(translated("vote_shop_gui_entry", "%reward%", rewardName));
                builder.addLore(translatedLore("vote_shop_gui_entry_lore",
                        "%cost%", CommonWords.VOTE.get(language).withNumber(cost),
                        "%balance%", CommonWords.VOTE.get(language).withNumber(votes)
                ));

                if (cost == 0 || votes < cost) {
                    builder.addLore(translatedLore("vote_shop_gui_entry_lore_unavailable"));

                    action = clickEvent -> gamer.playSound(YSound.VILLAGER_NO);
                } else {
                    builder.addLore(translatedLore("vote_shop_gui_entry_lore_available"));

                    action = clickEvent -> {
                        if (!reward.give(gamer)) {
                            gamer.sendLocalizedMessage("vote_shop_reward_unavailable");
                        } else {
                            gamer.takeVotes(cost);
                            gamer.sendLocalizedMessage("vote_shop_reward_purchased",
                                    "%reward%", reward.getRewardName(getLanguage()));
                        }

                        gamer.playSound(YSound.NOTE_PLING);

                        player.closeInventory();
                    };
                }

                add(builder.build(), slot, action);
            }
        };

        //слоты для магазина
        this.zone.addSlotRect(1, 1, 7, 2);

        draw();
    }

    @Override
    public void draw() {
        //рисуем гуи
        this.zone.draw();
        //кнопки назад/вперед
        this.zone.drawButtons(toSlot(2, 3), toSlot(6, 3));

        val parent = getParent();

        if (parent != null) {
            //рисуем кнопку перехода в предыдущее меню
            generateBackItem(toSlot(4, 3), event -> getParent().reopen());
        }
    }
}