package club.abelix.api.base.item;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.predicate.CoinsPredicate;
import club.abelix.api.base.item.predicate.GameItemPredicate;
import club.abelix.api.base.item.predicate.PermissionPredicate;
import club.abelix.api.base.item.predicate.PredicateType;
import club.abelix.api.base.item.predicate.PurchasePredicate;
import club.abelix.api.base.item.predicate.UpgradePredicate;
import club.abelix.api.base.item.visitor.GameItemVisitor;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.lobby.mysterybox.content.CaseContent;
import club.abelix.api.server.lobby.mysterybox.content.GameItemContent;
import club.abelix.api.server.lobby.mysterybox.item.CaseManager;
import club.abelix.commons.data.cases.CaseType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.exclusive.ExclusivePeriod;
import club.abelix.commons.util.math.MathUtil;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Uncommon;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public abstract class GameItem {

    @Getter
    private final int id;

    private final String displayNameLocalizationKey;

    @Getter
    private final GameItemCategory category;

    /**
     * Дополнительный лор иконки. Сюда можно засовывать описание китов
     */
    private final Map<Language, List<String>> description = new EnumMap<>(Language.class);

    /**
     * Кейсы, из которых выпадает данный предмет
     */
    private final Map<CaseType, CaseContent> caseTypeMap = new EnumMap<>(CaseType.class);
    /**
     * EnumMap для бедных
     */
    private final Map<PredicateType, List<GameItemPredicate>> predicates = new EnumMap<>(PredicateType.class);
    protected int level;

    @Getter
    protected Rarity rarity = Rarity.NONE;  // по дефолту у предмета нет рарити, он не выпадает ни из каких кейсов
    private ItemStack icon = new ItemStack(Material.APPLE);
    private ExclusivePeriod exclusivePeriod;
    private boolean cacheInitialized;
    private GameItem next;
    private GameItem prev;

    protected GameItem(int id, @NonNull String displayNameLocalizationKey, @NonNull GameItemCategory category) {
        this.id = id;
        this.displayNameLocalizationKey = displayNameLocalizationKey;
        this.category = category;

        for (PredicateType type : PredicateType.values()) {
            this.predicates.put(type, new ArrayList<>());
        }
    }

    public static boolean containsPredicate(List<GameItemPredicate> list, Class<? extends GameItemPredicate> cls) {
        for (GameItemPredicate predicate : list) {
            if (predicate.getClass() == cls) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        String name = getClass().getSimpleName();

        if (isUpgradeable()) {
            name += "[level=" + getLevel() + "]";
        }

        return name;
    }

    public GameItem getLastPurchased(Gamer gamer) {
        GameItemVisitor visitor = visitor();

        if (!visitor.movLastPurchased(gamer)) {
            return null;
        }

        return visitor.getItem();
    }

    public GameItem getFirst() {
        GameItem prev = getPrev();
        return prev == null ? this : prev.getFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof GameItem)) return false;

        GameItem item = (GameItem) o;
        return id == item.id && (!isUpgradeable() || o.getClass() == getClass());
    }

    public boolean isUpgradeable() {
        return level != 0;
    }

    private GameItem getItem(int id) {
        GameItem item = GameItemStorage.INSTANCE.getById(id);

        return item == null || item.getClass() != getClass() ? null : item;
    }

    private void initCache() {
        cacheInitialized = true;

        if (!isUpgradeable()) {
            return;
        }

        next = getItem(id + 1);
        prev = getItem(id - 1);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;

        if (level > 1) {
            purchaseOn(new UpgradePredicate(id - 1));
        }
    }

    public final GameItem getNext() {
        if (!cacheInitialized) initCache();
        return next;
    }

    public final GameItem getPrev() {
        if (!cacheInitialized) initCache();
        return prev;
    }

    public GameItemVisitor visitor() {
        return new GameItemVisitor(this);
    }

    public boolean isPurchasable() {
        return !predicates(PredicateType.PURCHASE).isEmpty();
    }

    /**
     * Получить все кейсы и рарити предмета в этих кейсах
     *
     * @return - кейсы с которых предмет выпадает
     */
    public List<Map.Entry<CaseType, CaseContent>> getCases() {
        List<Map.Entry<CaseType, CaseContent>> cases
                = new ArrayList<>(caseTypeMap.entrySet());

        cases.sort(Comparator.comparingInt(entry -> entry.getKey().getId()));

        return cases;
    }

    public String getDisplayName(@NonNull Language language) {
        return language.isMessageTranslated(displayNameLocalizationKey)
                ? language.getMessage(displayNameLocalizationKey) : displayNameLocalizationKey;
    }

    public List<String> getDescription(@NonNull Language language) {
        return description.get(language);
    }

    public ItemStack getIcon(@NonNull Language language) {
        String displayName = getDisplayName(language);

        if (isExclusive()) {
            displayName = language.getMessage("game_item_exclusive_name",
                    "%exclusive%", exclusivePeriod.getDisplayName(language),
                    "%name%", displayName);
        }

        String name = language.getMessage("game_item_name", "%name%", displayName);

        if (isUpgradeable()) {
            name += " " + MathUtil.getRoman(level);
        }

        List<String> lore = getDescription(language);

        // создавать новый ArrayList Нужно чтобы это не поменялось для всех
        // иначе каждое новое открытие добавит новый пробел :D
        if (lore != null && !lore.isEmpty()) {
            lore = new ArrayList<>(lore);
        }

        return ItemUtil.getBuilder(icon)
                .setName(name)
                .setLore(lore)
                .addItemFlags()
                .build();
    }

    // region selections

    public List<GameItemPredicate> predicates(@NonNull PredicateType type) {
        return this.predicates.get(type);
    }

    public boolean isSelectable() {
        return this.category.isSelectable();
    }

    public void onSelect(@NonNull Player player) {

    }

    // endregion

    // region configuring methods

    public void onUnselect(@NonNull Player player) {

    }


    public GameItem setRarity(@NonNull Rarity rarity) {
        this.rarity = rarity;

        return this;
    }

    public GameItem icon(@NonNull ItemStack icon) {
        this.icon = icon;

        return this;
    }

    public GameItem icon(@NonNull Material type) {
        this.icon = new ItemStack(type);

        return this;
    }

    private List<String> descr(Language language) {
        return description.computeIfAbsent(language, x -> new ArrayList<>());
    }

    public GameItem descr(@NonNull Language language, @NonNull String line) {
        descr(language).add(line);

        return this;
    }

    public GameItem descr(@NonNull String line) {
        for (Language language : Language.VALUES) descr(language, line);
        return this;
    }

    public GameItem descr(@NonNull Language language, @NonNull String... lines) {
        Collections.addAll(descr(language), lines);
        return this;
    }

    public GameItem descr(@NonNull Language language, @NonNull List<String> lines) {
        descr(language).addAll(lines);
        return this;
    }

    public GameItem ownershipOn(@NonNull GameItemPredicate predicate) {
        predicates(PredicateType.OWNERSHIP).add(predicate);

        return this;
    }

    public GameItem ownershipOnPermission(@NonNull String permission) {
        predicates(PredicateType.OWNERSHIP).add(PermissionPredicate.of(permission));

        return this;
    }

    public GameItem purchaseOnPermission(@NonNull String permission) {
        predicates(PredicateType.PURCHASE).add(PermissionPredicate.of(permission));

        if (!containsPredicate(predicates(PredicateType.OWNERSHIP), PermissionPredicate.class)) {
            ownershipOn(PermissionPredicate.of(permission));
        }

        return this;
    }

    public GameItem purchaseOn(@NonNull GameItemPredicate predicate) {
        predicates(PredicateType.PURCHASE).add(predicate);

        if (!containsPredicate(predicates(PredicateType.OWNERSHIP), PurchasePredicate.class)) {
            // добавляем проверку на владение вещью, если вещь куплена
            ownershipOn(new PurchasePredicate(this.id));
        }

        return this;
    }

    public GameItem purchaseOnGold(int clouds) {
        purchaseOn(new CoinsPredicate(GameCurrency.GOLD, clouds));
        return this;
    }

    public GameItem purchaseOn(int coins) {
        purchaseOn(new CoinsPredicate(GameCurrency.COINS, coins));
        return this;
    }

    public GameItem purchaseOn(@NonNull Uncommon uncommon, int coins) {
        purchaseOn(new CoinsPredicate(uncommon, coins));

        return this;
    }

    public GameItem purchaseOn(@NonNull Master master, int coins) {
        purchaseOn(new CoinsPredicate(master, coins));

        return this;
    }

    public GameItem selectOn(@NonNull GameItemPredicate predicate) {
        predicates(PredicateType.SELECT).add(predicate);

        return this;
    }

    public GameItem free() {
        return ownershipOn(new GameItemPredicate() {

            @Override
            public boolean match(Gamer gamer) {
                return true;
            }

            @Override
            public List<String> getRequirementLore(GameItem item, Gamer gamer) {
                throw new UnsupportedOperationException("Always true");
            }

            @Override
            public int withdraw(@NonNull Gamer gamer) {
                return 0;
            }

            @Override
            public GameCurrency getType() {
                return GameCurrency.COINS;
            }

        });
    }

    public CoinsPredicate getCoinsPredicate() {
        for (GameItemPredicate predicate : predicates(PredicateType.PURCHASE)) {
            if (predicate instanceof CoinsPredicate) {
                return (CoinsPredicate) predicate;
            }
        }

        return null;
    }

    public void containsPredicate() {
        if (!containsPredicate(predicates(PredicateType.OWNERSHIP), PurchasePredicate.class)) {
            // добавляем проверку на владение вещью, если вещь куплена
            ownershipOn(new PurchasePredicate(this.id));
        }
    }

    @Deprecated
    public final boolean has(@NonNull Player player) {
        return has(Gamer.getGamer(player));
    }

    public final boolean has(@NonNull Gamer gamer) {
        if (gamer.getRanks().hasPermission("gameitem.bypass")) {
            return true;
        }

        boolean hasPredicates = false;

        for (PredicateType predicateType : PredicateType.values()) {
            if (!predicates(predicateType).isEmpty()) {
                hasPredicates = true;
                break;
            }
        }

        if (!hasPredicates) {
            return true;
        }

        for (GameItemPredicate predicate : predicates(PredicateType.OWNERSHIP)) {
            if (predicate.match(gamer)) {
                return true;
            }
        }

        return false;
    }

    public final ExclusivePeriod getExclusivePeriod() {
        return exclusivePeriod;
    }

    public final boolean isExclusive() {
        return exclusivePeriod != null;
    }

    public final GameItem exclusive(@NonNull ExclusivePeriod period) {
        this.exclusivePeriod = period;
        return this;
    }

    public final GameItem addToCase(@NonNull CaseType type, @NonNull Rarity rarity) {
        CaseContent content = getCaseContent(rarity);

        caseTypeMap.put(type, content);
        CaseManager.IMP.addItem(type, content);

        return this;
    }

    protected CaseContent getCaseContent(@NonNull Rarity rarity) {
        return new GameItemContent(rarity, this);
    }

    public void clearPredicate(PredicateType type) {
        this.predicates.put(type, new ArrayList<>());
    }

    // endregion

}
