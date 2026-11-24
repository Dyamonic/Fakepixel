package club.abelix.api.base.playertop.type;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.playertop.entry.PlayerTopEntry;
import club.abelix.api.base.playertop.identifier.PlayerTopIdentifier;
import club.abelix.api.base.playertop.provider.PlayerTopProvider;
import club.abelix.api.base.playertop.view.PlayerTopView;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.common.logging.Logging;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.*;

public abstract class PlayerTop<T extends PlayerTopView> {

    protected static final PlayerTopIdentifier EMPTY_IDENTIFIER = new PlayerTopIdentifier() {
    };

    // -----------------------------------------------------------
    /**
     * Максимальное количество игроков в топе
     */
    @Getter
    protected final int maxPlayers;
    /**
     * Индивидуальные топы игроков (инфа о голограммах и прочем говне)
     */
    private final Map<Player, T> playerTopViewMap = new HashMap<>();
    /**
     * Кешированные топы игроков по определенному режиму
     */
    private final Map<PlayerTopIdentifier, CachedEntryList> cachedEntries = new HashMap<>();

    public PlayerTop(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    /**
     * Начать процесс автоматического обновления топа
     *
     * @param interval интервал в тиках
     */
    public void startUpdating(int interval) {
        HeartBeat.runTimerAsync(this::instantUpdate, 0, interval);
    }

    /**
     * Обновление топа игроков
     * <p>
     * Внимание! Вызывать асинхронно!
     */
    public void instantUpdate() {
        for (CachedEntryList cachedPlayerTopInfo : cachedEntries.values()) {
            cachedPlayerTopInfo.update();
        }

        playerTopViewMap.values().forEach(PlayerTopView::update);
    }

    public CachedEntryList getPlayerTop() {
        return getPlayerTop(EMPTY_IDENTIFIER);
    }

    /**
     * Получить кешированный топ игроков по определенному провайдеру
     *
     * @param topIdentifier - идентификатор типа топа
     * @return - топ игроков. Вернется пустой список,
     * если информации с таким идентификатором нет
     */
    public CachedEntryList getPlayerTop(@NonNull PlayerTopIdentifier topIdentifier) {
        return cachedEntries.get(topIdentifier);
    }

    /**
     * Добавить топ без идентификатора (будет использован пустой)
     * Можно использовать, если в этом топе только 1 провайдер
     *
     * @param provider - провайдер информации для обновления
     */
    public void register(@NonNull PlayerTopProvider provider) {
        Preconditions.checkState(cachedEntries.isEmpty(),
                "Can not register more then 1 top provider without identifier");

        register(EMPTY_IDENTIFIER, provider);
    }

    /**
     * Добавить топ по определенному идентификатору
     *
     * @param identifier - идентификатор типа топа
     * @param provider   - провайдер информации для обновления
     */
    public void register(@NonNull PlayerTopIdentifier identifier, @NonNull PlayerTopProvider provider) {
        cachedEntries.put(identifier, new CachedEntryList(provider));
    }

    protected abstract T createPlayerTopView(@NonNull Gamer gamer);

    public Collection<T> getPlayerViews() {
        return playerTopViewMap.values();
    }

    /**
     * Показать топ определенному игроку
     *
     * @param gamer - игрок
     */
    public final void showTo(@NonNull Gamer gamer) {
        Logging.IMP.info("[TOP] Show to {}", gamer.getName());
        T playerTopView = createPlayerTopView(gamer);
        playerTopView.create();

        playerTopViewMap.put(gamer.getPlayer(), playerTopView);

        showToPlayer(gamer);
    }

    /**
     * Спрятать топ определенному игроку
     *
     * @param gamer - игрок
     */
    public final void hideFrom(@NonNull Gamer gamer) {
        T playerTopView = playerTopViewMap.remove(gamer.getPlayer());

        if (playerTopView != null) {
            playerTopView.remove();
        }

        hideFromPlayer(gamer);
    }

    /**
     * Вызывается при попытке показать топ определенному игроку
     *
     * @param gamer - игрок
     */
    protected void showToPlayer(@NonNull Gamer gamer) {

    }

    /**
     * Вызывается при попытке спрятать топ от игрока
     *
     * @param gamer - игрок
     */
    protected void hideFromPlayer(@NonNull Gamer gamer) {

    }

    @RequiredArgsConstructor
    @Getter
    public class CachedEntryList {

        private final PlayerTopProvider infoProvider;

        private final List<PlayerTopEntry> playerTopEntries = new ArrayList<>(maxPlayers);

        private void update() {
            playerTopEntries.clear();

            try {
                playerTopEntries.addAll(infoProvider.updatePlayerTop());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

    }

}
