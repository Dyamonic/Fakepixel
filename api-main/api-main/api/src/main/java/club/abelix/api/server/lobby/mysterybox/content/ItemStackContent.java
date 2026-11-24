package club.abelix.api.server.lobby.mysterybox.content;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.nms.locale.LocaleUtil;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.language.Language;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ItemStackContent implements Reward, CaseContent {

    private static final EquivalentType RESTRICTED = new EquivalentType() {
        @Override
        public void sendDuplicateEquivalentMessage(@NonNull Gamer gamer, int equivalent) {

        }

        @Override
        public boolean giveDuplicateEquivalent(@NonNull Gamer gamer, int equivalent) {
            return false;
        }
    };
    ;

    @Getter
    private final Rarity rarity;
    private final ItemStack displayItem;
    private final List<ItemStack> items;
    private final String nameKey;

    public ItemStackContent(@NonNull Rarity rarity, @NonNull ItemStack displayItem) {
        this(rarity, displayItem, Collections.singletonList(displayItem.clone()), null);
    }

    public ItemStackContent(@NonNull Rarity rarity, @NonNull ItemStack displayItem, @NonNull List<ItemStack> items) {
        this(rarity, displayItem, items, null);

        Preconditions.checkArgument(!items.isEmpty(),
                "No items specified");
        Preconditions.checkArgument(items.size() == 1 || nameKey != null,
                "Specify name localization key if you planning to give multiple items for player!");
    }

    @Override
    public boolean give(@NonNull Gamer gamer) {
        boolean enough = true;

        PlayerInventory inventory = gamer.getPlayer().getInventory();

        for (ItemStack item : items) {
            if (ItemUtil.isInventoryFull(inventory, item)) {
                enough = false;
                break;
            }
        }

        if (!enough) {
            return false;
        }

        items.forEach(inventory::addItem);
        return true;
    }

    @Override
    public String getRewardName(@NonNull Language language) {
        return nameKey != null ?
                language.getMessage(nameKey) :
                rarity.getColor() + LocaleUtil.translateWithAmount(language, items.get(0));
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return displayItem;
    }

    // -------------------------------------------------------
    // Дубликат вещей невозможен. Код ниже нужен только для
    // того, чтобы выдача дубликата возвращала всегда false
    // Так система кейсов будет считать, что контент и дубликат
    // не выдался, поэтому открытие было неудачным, и не будет
    // начинать прокрутку
    // -------------------------------------------------------

    @Override
    public int getDuplicateEquivalent(@NonNull Gamer gamer) {
        return 1;
    }

    @Override
    public EquivalentType getEquivalentType() {
        return RESTRICTED;
    }
}
