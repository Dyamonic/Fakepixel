package club.abelix.api.server.lobby.mysterybox.item;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.impl.GameItemReward;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.lobby.mysterybox.content.*;
import club.abelix.commons.data.cases.CaseType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.data.suffix.Suffix;
import club.abelix.commons.data.suffix.SuffixSqlHandler;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.exclusive.ExclusivePeriod;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CaseManager {

    public static final CaseManager IMP = new CaseManager();

    // -----------------------------------------------------

    @Getter
    // <case item, item chance>
    private final Map<CaseType, Map<Rarity, List<CaseContent>>> caseItems
            = new LinkedHashMap<>();

    /**
     * Выдать награду игроку
     *
     * @param gamer   - игрок
     * @param content - контент для выдачи
     * @return - награда
     */
    public ContentIssueResult giveReward(@NonNull Gamer gamer,
                                         @NonNull CaseContent content) {

        if (!content.give(gamer)) {
            boolean result = content.getEquivalentType()
                    .giveDuplicateEquivalent(gamer, content.getDuplicateEquivalent(gamer));

            if (result) {
                return ContentIssueResult.ALREADY_HAS;
            }

            return ContentIssueResult.FAIL;
        }

        return ContentIssueResult.SUCCESS;
    }

    public void sendRewardMessage(@NonNull Gamer gamer,
                                  @NonNull CaseType type,
                                  @NonNull CaseContent content,
                                  boolean duplicate) {

        int dust = content.getDuplicateEquivalent(gamer);

        Language language = gamer.getLanguage();

        String name = content.getRewardName(language);

        if (duplicate) {
            gamer.sendLocalizedMessage("case_reward_duplicate", "%item%", name);

            if (dust > 0) {
                content.getEquivalentType()
                        .sendDuplicateEquivalentMessage(gamer, dust);
            }

            return;
        }

        Rarity rarity = content.getRarity();

        gamer.sendLocalizedMessage("case_reward_given",
                "%rarity%", rarity.getColor() + rarity.getName(language), "%item%", name);

        if (gamer.getProperty(type.name()) == 0) {
            gamer.sendLocalizedMessage("case_out_of_cases");
        }

        if (gamer.isVanished() || rarity.ordinal() < Rarity.EPIC.ordinal()) {
            return;
        }

        for (Gamer target : Gamer.getGamers()) {
            if (gamer == target) {
                continue;
            }

            Language lang = target.getLanguage();

            target.sendLocalizedMessage("case_broadcast",
                    "%name%", gamer.getDisplayName(),
                    "%rarity%", rarity.getColor() + rarity.getName(lang),
                    "%item%", content.getRewardName(lang)
            );

            if (target.getProperty(type.name()) > 0) {
                continue;
            }

            target.sendLocalizedMessage("case_purchase_advertise");
        }
    }

    public void initCaseItems() {

    }

    public int getItemsCount(@NonNull CaseType caseType) {
        return caseItems.computeIfAbsent(caseType, r -> new LinkedHashMap<>()).size();
    }

    public List<CaseContent> getItems(@NonNull CaseType caseType, @NonNull Rarity rarity) {
        List<CaseContent> rewards = caseItems.computeIfAbsent(caseType, r -> new EnumMap<>(Rarity.class))
                .computeIfAbsent(rarity, r -> new ArrayList<>());

        //удаляем эксклюзивные предметы
        rewards.removeIf(reward -> {
            if (!(reward instanceof GameItemReward)) {
                return false;
            }

            ExclusivePeriod period = ((GameItemReward) reward).getGameItem().getExclusivePeriod();

            return period != null && !period.isAvailable();
        });

        return rewards;
    }

    public void addItem(@NonNull CaseType caseType, @NonNull CaseContent content) {
        Rarity rarity = content.getRarity();

        if (rarity == Rarity.NONE) {
            return;
        }

        Map<Rarity, List<CaseContent>> itemMap = caseItems
                .computeIfAbsent(caseType, r -> new LinkedHashMap<>());

        List<CaseContent> rewards = itemMap.computeIfAbsent(rarity, r -> new ArrayList<>());

        //добавляем награду в список
        rewards.add(content);
    }

    public Rarity getRandomRarity(@NonNull CaseType type, int pseudoRandom) {
        List<Rarity> rarities = Lists.newArrayList(Rarity.VALUES);

        rarities.sort(Comparator.comparingDouble(Rarity::getChance));
        rarities.removeIf(rarity -> getItems(type, rarity).isEmpty());

        double randChance = Math.random();

        for (Rarity rarity : rarities) {
            if (rarity.getChance() + rarity.getChangeChance() * pseudoRandom > randChance) {
                return rarity;
            }
        }

        return rarities.isEmpty() ? null : rarities.get(rarities.size() - 1);
    }

    public ItemStack getIcon(@NonNull CaseType caseType, @NonNull Language language) {
        return ItemUtil.getBuilder(Material.SKULL_ITEM)
                .setDurability(3)
                .setSkullValue(caseType.getTexture())
                .setName(language.getMessage("case_gui_name", "%case%", caseType.getDisplayName(language)))
                .setLore(caseType.getDescription(language))
                .build();
    }

    public enum ContentIssueResult {
        SUCCESS,
        ALREADY_HAS,
        FAIL
    }
}