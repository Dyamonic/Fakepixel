package club.abelix.core.app.data.tunnel;

import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import club.abelix.core.app.event.gamer.PlayerRedirectEvent;
import club.abelix.core.app.event.gamer.PlayerVanishEvent;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.NonNull;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// TODO: FIX хуйня ломается

/**
 * Эта штука нужна для слежки за перемещениями между серверами определенного игрока
 * Создана для модераторов, которые спекают за челом, который тепается по минигеймам
 * Типо не писать все время /find <челик> а потом /server <сервер челика>
 * Просто жмякаешь по сообщению в чате (TODO)
 *
 * @author tomskiy
 */
public class TunnelManager implements Listener {

    // -----------------------------------------------------------------------------

    public static TunnelManager INSTANCE = new TunnelManager();

    // -----------------------------------------------------------------------------

    /**
     * K = Чел, который следит
     * V = Чел, за которым следят
     */
    private final Map<@NotNull Gamer, @NotNull TIntSet> observableGamersMap = new HashMap<>();

    /**
     * Добавляет челика в список наблюдаемых другого челика
     *
     * @param executor чилик, который следит
     * @param target   челик, за которым седят
     */
    public synchronized void addObservableGamer(final @NonNull Gamer executor, final @NonNull Gamer target) {

        if (observableGamersMap.containsKey(executor)
                && observableGamersMap.get(executor).contains(target.getPlayerId()))
            return; // если экзекутор уже следит за челиком

        // Инициализируем в мапе нашего наблюдателя (если вдруг его там нет)
        if (!observableGamersMap.containsKey(executor)) observableGamersMap.put(executor, new TIntHashSet());

        observableGamersMap.get(executor).add(target.getPlayerId());

    }

    /**
     * Удаляет челика из списка наблюдаемых другого челика
     *
     * @param executor чилик, который следит
     * @param target   челик, за которым седят
     */
    public synchronized void removeObservableGamer(final @NonNull Gamer executor, final @NonNull Gamer target) {
        if (!observableGamersMap.containsKey(executor)
                || !observableGamersMap.get(executor).contains(target.getPlayerId()))
            return; // если экзекутор не следит за челом

        observableGamersMap.get(executor).remove(target.getPlayerId());
        if (observableGamersMap.get(executor).isEmpty()) {
            observableGamersMap.remove(executor);
        }
    }

    public synchronized void removeGamer(final @NonNull Gamer gamer) {
        observableGamersMap.remove(gamer);

        Iterator<Map.Entry<Gamer, TIntSet>> iterator = observableGamersMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Gamer, TIntSet> entry = iterator.next();
            entry.getValue().remove(gamer.getPlayerId());
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * Возвращает сет геймеров, за которыми следит челик {@param observer}
     *
     * @param observer челик, у когорого мы берем челиков, за которыми он следит
     * @return сет с челиками
     */
    public synchronized @NotNull TIntSet getObservableGamers(final @NotNull Gamer observer) {
        if (!observableGamersMap.containsKey(observer)) return new TIntHashSet();
        return observableGamersMap.get(observer);
    }

    /**
     * Получаем сет челиков, которые следят за аргументом
     *
     * @param observable челик, за которым "следят"
     * @return сет челиков, которые следят за аргументом
     */
    public synchronized @NotNull Set<@NotNull Gamer> getGamerObservers(final @NotNull Gamer observable) {

        Set<Gamer> observers = new HashSet<>();

        observableGamersMap.keySet().forEach(iObserver -> {
            if (isObserving(iObserver, observable)) observers.add(iObserver);
        });

        return observers;
    }

    /**
     * @param observer   чел, который наблюдает
     * @param observable чел, за которым наблюдают
     * @return наблюдает ли челик за другим челиком
     */
    public boolean isObserving(final @NotNull Gamer observer, final @NonNull Gamer observable) {
        return getObservableGamers(observer).contains(observable.getPlayerId());
    }

    /**
     * Когда челик выходит, в туннель пишется, мол он вышел
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public synchronized void onPlayerQuit(final @NonNull PlayerQuitEvent event) {
        observableGamersMap.keySet().forEach(g -> {
            if (isObserving(g, event.getGamer()))
                g.sendMessage("§8[§3T§8] §f" + event.getGamer().getDisplayName() + ChatColor.WHITE + " disconnected.");
        });
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerQuitCleanup(final @NonNull PlayerQuitEvent event) {
        removeGamer(event.getGamer());
    }

    /**
     * Когда челик заходит, в туннель пишется, мол он зашел
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerLogin(final @NonNull GamerJoinEvent event) {
        Gamer gamer = event.getGamer();
        if (gamer == null) return;
        observableGamersMap.keySet().forEach(g -> {
            if (isObserving(g, gamer))
                g.sendMessage("§8[§3T§8] §f" + gamer.getDisplayName() + ChatColor.WHITE + " connected.");
        });
    }

    /**
     * Когда челик ванишится, в туннель пишется, мол заванишился
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerVanish(final @NonNull PlayerVanishEvent event) {
        observableGamersMap.keySet().forEach(g -> {
            if (isObserving(g, event.getGamer())) {
                if (event.isVanished())
                    g.sendMessage("§8[§3T§8] §f" + event.getGamer().getDisplayName() + ChatColor.WHITE + " vanished.");
                else
                    g.sendMessage("§8[§3T§8] §f" + event.getGamer().getDisplayName() + ChatColor.WHITE + " unvanished.");
            }
        });
    }

    /**
     * Когда челик меняет сервер, в туннель пишется, куда он мувнулся
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onServerSwitch(PlayerRedirectEvent event) {
        observableGamersMap.keySet().forEach(g -> {
            if (isObserving(g, event.getGamer())) {
                g.sendMessage("§8[§3T§8] §f" + event.getGamer().getDisplayName() +
                        "§f -> §e" + event.getTarget().getName() + (event.isCancelled() ? "§c[C]" : ""));

                // Отправляем на сервер
                g.sendTo(event.getTarget());
            }
        });
    }
}
