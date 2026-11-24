package club.abelix.api.server.lobby.impl.lobby.quest;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.data.quest.Quest;
import club.abelix.commons.data.quest.QuestRequirement;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.TimeUtil;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class QuestsGui extends Gui {

    private static final int[] QUEST_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int[] BORDER_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17, 18, 26, 27, 35, 36,
            44, 45, 46, 47, 51, 52, 53
    };

    private static final ItemStack BORDER_ITEM = ItemUtil.getBuilder(Material.STAINED_GLASS_PANE)
            .setDurability(3)
            .build();

    private final List<Quest> quests;
    private final PagedZone<Quest> pagedZone;

    protected QuestsGui(@NonNull Player player, @NonNull List<Quest> quests) {
        super(player, "lang:quests_gui_title:%mode%:" + CoreUtil.getMaster().getDisplayName() + ";", 6);

        this.quests = quests;

        this.pagedZone = new PagedZone<Quest>(this) {
            {
                for (int slot : QUEST_SLOTS) {
                    addSlot(slot);
                }
            }

            @Override
            protected Collection<Quest> getItems() {
                return QuestsGui.this.quests;
            }

            @Override
            protected void drawItem(Quest item, int slot) {
                drawQuest(item, slot);
            }
        };

        draw();
    }

    @Override
    public void draw() {
        drawBorder();
        pagedZone.draw();
        pagedZone.drawButtons(48, 50);
        generateCloseItem();
    }

    private void drawBorder() {
        for (int slot : BORDER_SLOTS) {
            add(BORDER_ITEM, slot);
        }
    }

    private void drawQuest(@NonNull Quest quest, int slot) {
        Gamer gamer = getGamer();

        long beforeNextDay = TimeUnit.DAYS.toMillis(TimeUtil.getDayUTC() + 1) - System.currentTimeMillis();

        StatsMode statsMode = quest.getMode();

        List<QuestRequirement> requirements = new ArrayList<>(quest.getRequirements());
        List<QuestRequirement> completed = new ArrayList<>();

        int totalRequiredValue = 0;
        int totalCompletedValue = 0;

        for (QuestRequirement requirement : requirements) {
            int current = gamer.getTodayStat(requirement.getField());
            int required = requirement.getValue();

            if (current >= required) {
                completed.add(requirement);
            }

            totalCompletedValue += Math.min(current, required);
            totalRequiredValue += required;
        }

        requirements.removeAll(completed);

        ItemUtil.Builder builder;

        if (requirements.isEmpty()) {
            builder = ItemUtil.getBuilder(Material.MAP);
            builder.addItemFlags();
            builder.setName(
                    translated("quests_gui_completed_entry",
                            "%mode%", statsMode.getDisplayName()
                    )
            );
            builder.setLore(
                    translatedLore("quests_gui_completed_entry_lore",
                            "%time_before_next_day%", Duration.durationWordsMs(getLanguage(), beforeNextDay)
                    )
            );
        } else {
            double progress = totalRequiredValue <= 0
                    ? 0D
                    : totalCompletedValue / (double) totalRequiredValue;

            builder = ItemUtil.getBuilder(Material.PAPER);
            builder.glowing();
            builder.setName(
                    translated("quests_gui_uncompleted_entry",
                            "%mode%", statsMode.getDisplayName(),
                            "%master%", CoreUtil.getMaster().getDisplayName()
                    )
            );
            builder.setLore(
                    translatedLore("quests_gui_uncompleted_entry_lore",
                            "%progress%", String.valueOf((int) (progress * 100)),
                            "%time_before_next_day%", Duration.durationWordsMs(getLanguage(), beforeNextDay)
                    )
            );
        }

        for (QuestRequirement requirement : completed) {
            builder.addLore(
                    translated("quests_gui_completed_requirement",
                            "%name%", requirement.getQuestDescription(getLanguage()),
                            "%value%", requirement.getValue()
                    )
            );
        }

        for (QuestRequirement requirement : requirements) {
            int current = gamer.getTodayStat(requirement.getField());

            builder.addLore(
                    translated("quests_gui_uncompleted_requirement",
                            "%name%", requirement.getQuestDescription(getLanguage()),
                            "%total%", requirement.getValue(),
                            "%current%", current
                    )
            );
        }

        builder.addLore(
                translated("quests_gui_entry_reward",
                        "%coins%", Leveling.formatGrouped(quest.getCoinReward()),
                        "%exp%", Leveling.formatGrouped(quest.getExperienceReward())
                )
        );

        add(builder.build(), slot);
    }
}