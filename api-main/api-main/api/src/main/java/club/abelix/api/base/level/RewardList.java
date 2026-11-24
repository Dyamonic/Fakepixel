package club.abelix.api.base.level;

import club.abelix.api.base.box.type.StandardBoxType;
import club.abelix.api.base.item.GameItemStorage;
import club.abelix.api.base.reward.Reward;
import club.abelix.api.base.reward.impl.*;
import club.abelix.commons.data.booster.BoosterType;
import club.abelix.commons.data.joinmessage.JoinMessageSqlHandler;
import club.abelix.commons.data.suffix.Suffix;
import club.abelix.commons.data.suffix.SuffixSqlHandler;
import club.abelix.network.type.mode.Master;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Награды за уровни. Список может изменяться
 * другими плагинами
 */
@UtilityClass
public class RewardList {

    final List<Entry> ENTRIES = new ArrayList<>();

    static {
        addEntry(2, new CoinReward(Master.DUELS, 1500), new CoinReward(Master.BW, 1500));
        addEntry(3, new BoosterReward(BoosterType.BOOSTER_COINS_1));
        addEntry(4, new BoosterReward(BoosterType.BOOSTER_EXPERIENCE_2));
        addEntry(5, new CoinReward(Master.DUELS, 1500), new CoinReward(Master.BW, 1500), new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(2515))); // Голова "НОТЧ"
        addEntry(6, new CoinReward(Master.DUELS, 1500), new CoinReward(Master.BW, 1500));
        addEntry(7, new CoinReward(Master.DUELS, 1500), new CoinReward(Master.BW, 1500));
        addEntry(8, new BoosterReward(BoosterType.BOOSTER_COINS_3));
        addEntry(9, new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(2537)), new CoinReward(Master.DUELS, 2500), new CoinReward(Master.BW, 2500)); // Голова "Бургер"
        addEntry(10, new PlazmaReward(10));
        addEntry(11, new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(1506))); // Баннер "Дедпул"
        addEntry(12, new CoinReward(Master.DUELS, 2500), new CoinReward(Master.BW, 2500));
        addEntry(13, new CoinReward(Master.DUELS, 2500), new CoinReward(Master.BW, 2500));
        addEntry(14, new CoinReward(Master.DUELS, 2500), new CoinReward(Master.BW, 2500));
        addEntry(15, new BoxReward(StandardBoxType.LOBBY_COSMETIC, 1));
        addEntry(16, new CoinReward(Master.DUELS, 2500), new CoinReward(Master.BW, 2500));
        addEntry(17, new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(2545)), new CoinReward(Master.DUELS, 2500), new CoinReward(Master.BW, 2500)); // Голова "Шрек"
        addEntry(18, getSuffixReward(123)); // Титул "Мега-мозг"
        addEntry(19, new CoinReward(Master.DUELS, 2500), new CoinReward(Master.BW, 2500));
        addEntry(20, new PlazmaReward(20));
        addEntry(21, new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(1513))); // Баннер "Покебол"
        addEntry(22, new CoinReward(Master.DUELS, 5000), new CoinReward(Master.BW, 5000));
        addEntry(23, new BoosterReward(BoosterType.BOOSTER_COINS_2), new CoinReward(Master.DUELS, 5000), new CoinReward(Master.BW, 5000));
        addEntry(24, new BoxReward(StandardBoxType.EXPERIENCE, 1));
        addEntry(25, new PlazmaReward(10), new BoosterReward(BoosterType.BOOSTER_EXPERIENCE_2));
        addEntry(26, new CoinReward(Master.DUELS, 5000), new CoinReward(Master.BW, 5000));
        addEntry(27, new CoinReward(Master.DUELS, 5000), new CoinReward(Master.BW, 5000));
        addEntry(28, new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(2547))); // Голова "Торт"
        addEntry(29, new CoinReward(Master.DUELS, 5000), new CoinReward(Master.BW, 5000));
        addEntry(30, getSuffixReward(138), new CoinReward(Master.DUELS, 7500), new CoinReward(Master.BW, 7500)); // Титул "Вечный"
        addEntry(31, new CoinReward(Master.DUELS, 7500), new CoinReward(Master.BW, 7500));
        addEntry(32, new BoxReward(StandardBoxType.LOBBY_COSMETIC, 1), new BoxReward(StandardBoxType.EXPERIENCE, 1));
        addEntry(33, new CoinReward(Master.DUELS, 7500), new CoinReward(Master.BW, 7500));
        addEntry(34, new CoinReward(Master.DUELS, 7500), new CoinReward(Master.BW, 7500));
        addEntry(35, new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(1516)), new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000)); // Баннер "Железный человек"
        addEntry(36, new CoinReward(Master.DUELS, 8000), new CoinReward(Master.BW, 8000));
        addEntry(37, new BoosterReward(BoosterType.BOOSTER_COINS_1), new CoinReward(Master.DUELS, 8000), new CoinReward(Master.BW, 8000));
        addEntry(38, new CoinReward(Master.DUELS, 8000), new CoinReward(Master.BW, 8000));
        addEntry(39, new CoinReward(Master.DUELS, 8000), new CoinReward(Master.BW, 8000));
        addEntry(40, new JoinMessageReward(JoinMessageSqlHandler.INSTANCE.getJoinMessageById(22)), new BoosterReward(BoosterType.BOOSTER_EXPERIENCE_3)); // Сообщение интернет
        addEntry(41, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(42, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(43, new BoosterReward(BoosterType.BOOSTER_COINS_1), new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(44, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(45, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(46, new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(1508)), new PlazmaReward(10)); // Баннер "радужный"
        addEntry(47, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(48, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(49, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(50, new BoxReward(StandardBoxType.GOLD, 1), new BoxReward(StandardBoxType.EXPERIENCE, 1));
        addEntry(51, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(52, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(53, new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(2541)), new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000)); // Голова "Самурай"
        addEntry(54, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(55, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(56, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(57, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(58, new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(2544)), new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000)); // Голова "Король"
        addEntry(59, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(60, new PlazmaReward(12), new CoinReward(Master.DUELS, 12000), new CoinReward(Master.BW, 12000));
        addEntry(61, new BoxReward(StandardBoxType.TITLES, 1));
        addEntry(62, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(63, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(64, new BoosterReward(BoosterType.BOOSTER_COINS_3), new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(65, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(66, new BoosterReward(BoosterType.BOOSTER_COINS_2), new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(67, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(68, new BoosterReward(BoosterType.BOOSTER_EXPERIENCE_3), new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(69, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(70, getSuffixReward(119), new BoosterReward(BoosterType.BOOSTER_EXPERIENCE_3)); // Титул "Тапочек"
        addEntry(71, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(72, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(73, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(74, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(75, new PlazmaReward(10), new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(2553)), new BoosterReward(BoosterType.BOOSTER_COINS_3)); // Голова "Патрик"
        addEntry(76, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(77, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(78, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(79, new CoinReward(Master.DUELS, 10000), new CoinReward(Master.BW, 10000));
        addEntry(80, new BoxReward(StandardBoxType.GOLD, 1), new BoxReward(StandardBoxType.EXPERIENCE, 2));
        addEntry(81, new CoinReward(Master.DUELS, 12000), new CoinReward(Master.BW, 12000));
        addEntry(82, new CoinReward(Master.DUELS, 12000), new CoinReward(Master.BW, 12000));
        addEntry(83, new CoinReward(Master.DUELS, 12000), new CoinReward(Master.BW, 12000));
        addEntry(84, new CoinReward(Master.DUELS, 12000), new CoinReward(Master.BW, 12000));
        addEntry(85, new LazyGameItemReward(() -> GameItemStorage.INSTANCE.getById(1511)), new CoinReward(Master.DUELS, 12000), new CoinReward(Master.BW, 12000)); // Баннер "Утка"
        addEntry(86, new CoinReward(Master.DUELS, 12000), new CoinReward(Master.BW, 12000));
        addEntry(87, new CoinReward(Master.DUELS, 12000), new CoinReward(Master.BW, 12000));
        addEntry(88, new CoinReward(Master.DUELS, 12000), new CoinReward(Master.BW, 12000));
        addEntry(89, new CoinReward(Master.DUELS, 12000), new CoinReward(Master.BW, 12000));
        addEntry(90, new PlazmaReward(30));
        addEntry(91, new CoinReward(Master.DUELS, 20000), new CoinReward(Master.BW, 20000));
        addEntry(92, new BoosterReward(BoosterType.BOOSTER_COINS_3), new CoinReward(Master.DUELS, 20000), new CoinReward(Master.BW, 20000));
        addEntry(93, new CoinReward(Master.DUELS, 20000), new CoinReward(Master.BW, 20000));
        addEntry(94, new CoinReward(Master.DUELS, 20000), new CoinReward(Master.BW, 20000));
        addEntry(95, getSuffixReward(131), new CoinReward(Master.DUELS, 20000), new CoinReward(Master.BW, 20000)); // Титул "Абоба"
        addEntry(96, new BoosterReward(BoosterType.BOOSTER_EXPERIENCE_3), new CoinReward(Master.DUELS, 20000), new CoinReward(Master.BW, 20000));
        addEntry(97, new CoinReward(Master.DUELS, 25000), new CoinReward(Master.BW, 25000));
        addEntry(98, new CoinReward(Master.DUELS, 25000), new CoinReward(Master.BW, 25000));
        addEntry(99, new CoinReward(Master.DUELS, 25000), new CoinReward(Master.BW, 25000));
        addEntry(100, new BoxReward(StandardBoxType.RANKS, 1));
    }

    public void put(int level, Reward... rewards) {
        Entry existingEntry = findEntryByLevel(level);

        if (existingEntry != null) {
            ENTRIES.remove(existingEntry);
        }

        ENTRIES.add(new Entry(level, Arrays.stream(rewards)
                .filter(Objects::nonNull)
                .toList()));
    }

    public void add(int level, Reward... rewards) {
        Entry existingEntry = findEntryByLevel(level);

        if (existingEntry != null) {
            List<Reward> rewardList = new ArrayList<>(existingEntry.rewards());
            rewardList.addAll(Arrays.stream(rewards)
                    .filter(Objects::nonNull)
                    .toList());

            ENTRIES.remove(existingEntry);
            ENTRIES.add(new Entry(level, rewardList));
        } else {
            ENTRIES.add(new Entry(level, Arrays.stream(rewards)
                    .filter(Objects::nonNull)
                    .toList()));
        }
    }

    public List<Reward> getRewardsFor(int level) {
        Entry entry = findEntryByLevel(level);
        return entry != null ? new ArrayList<>(entry.rewards()) : new ArrayList<>();
    }

    public List<Entry> getAllEntries() {
        return ENTRIES.stream()
                .map(entry -> new Entry(entry.level(), new ArrayList<>(entry.rewards())))
                .toList();
    }

    public List<Entry> getSortedEntries() {
        return ENTRIES.stream()
                .sorted(Comparator.comparingInt(Entry::level))
                .map(entry -> new Entry(entry.level(), new ArrayList<>(entry.rewards())))
                .toList();
    }

    private Entry findEntryByLevel(int level) {
        return ENTRIES.stream()
                .filter(entry -> entry.level() == level)
                .findFirst()
                .orElse(null);
    }

    private void addEntry(int level, Reward... rewards) {
        ENTRIES.add(new Entry(level, Arrays.stream(rewards)
                .filter(Objects::nonNull)
                .toList()));
    }

    private SuffixReward getSuffixReward(Integer suffixId) {
        Suffix suffix = SuffixSqlHandler.INSTANCE.getSuffixById(suffixId);
        return suffix != null ? new SuffixReward(suffix) : null;
    }

    public record Entry(
            int level,
            List<Reward> rewards
    ) {}
}