package club.abelix.games.fastbridge.cosmetic.api;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.GameItemStorage;
import club.abelix.api.base.item.predicate.GameItemPredicate;
import club.abelix.api.base.item.predicate.PredicateType;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.language.Language;
import club.abelix.games.fastbridge.cosmetic.api.category.FastBridgeCategory;
import club.abelix.games.fastbridge.cosmetic.api.predicate.CosmeticItemPurchases;
import club.abelix.games.fastbridge.cosmetic.item.block.BlocksCosmeticItem;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: GameCosmetics-NEW</p>
 * <p>GameCosmeticItem creation on 05.02.2023 at 02:44</p>
 */
@Log4j2
public class CosmeticItem extends GameItem {

    private final int coinPrice;

    public CosmeticItem(int numericId, String displayName, FastBridgeCategory category, Rarity rarity, int coinPrice) {
        super(numericId, "fast_bridge_item_" + displayName, category);

        this.coinPrice = coinPrice;

        String descriptionKey = "fast_bridge_item_" + displayName + "_description";

        for (Language language : Language.VALUES) {
            if (language.isMessageTranslated(descriptionKey)) {
                descr(language, language.getMessageList(descriptionKey));
            }
        }

        setRarity(rarity);
        purchaseOnCoins(coinPrice);

        GameItemStorage.INSTANCE.register(this);
        log.info("[GameCosmetic] Initialization {} (id: {}) | Category {}",
                displayName, numericId, category.getId());
    }

    public static void initItems() {
        BlocksCosmeticItem.init();
    }

    @Override
    public FastBridgeCategory getCategory() {
        return (FastBridgeCategory) super.getCategory();
    }

    @Override
    public void onSelect(@NonNull Player player) {
    }

    @Override
    public void onUnselect(@NonNull Player player) {
        GameItemStorage.INSTANCE.clearSelection(this.getCategory(), Gamer.getGamer(player));
    }


    // region setters

    protected CosmeticItem setIcon(@NonNull ItemStack icon) {
        icon(icon);
        return this;
    }

    @Override
    public CosmeticItem purchaseOn(int cost) {
        super.purchaseOnGold(cost);
        return this;
    }

    public CosmeticItem purchaseOnCoins(int coins) {
        super.purchaseOn(CoreUtil.getUncommon(), coins);
        return this;
    }

    protected CosmeticItem purchaseOn(@NonNull String permissionToPurchase) {
        purchaseOnPermission(permissionToPurchase);
        return this;
    }

    protected CosmeticItem ownershipOn(@NonNull String permissionToOwn) {
        ownershipOnPermission(permissionToOwn);
        return this;
    }

    @Override
    public GameItem purchaseOn(@NonNull GameItemPredicate predicate) {
        predicates(PredicateType.PURCHASE).add(predicate);

        if (!containsPredicate(predicates(PredicateType.OWNERSHIP), CosmeticItemPurchases.class)) {
            // добавляем проверку на владение вещью, если вещь куплена
            ownershipOn(new CosmeticItemPurchases(this.getId()));
        }

        return this;
    }

    @Override
    public void containsPredicate() {
        if (!containsPredicate(predicates(PredicateType.OWNERSHIP), CosmeticItemPurchases.class)) {
            // добавляем проверку на владение вещью, если вещь куплена
            ownershipOn(new CosmeticItemPurchases(this.getId()));
        }
    }

}
