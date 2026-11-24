package club.abelix.core.app.data.stream;

import club.abelix.commons.language.Language;
import club.abelix.commons.util.json.JSON;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.core.app.Async;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.stream.detail.AbstractStreamDetails;
import club.abelix.core.app.data.stream.exception.StreamException;
import club.abelix.core.app.data.stream.platform.StreamPlatform;
import club.abelix.core.app.data.stream.platform.TwitchStreamPlatform;
import club.abelix.core.app.data.stream.platform.YouTubeStreamPlatform;
import club.abelix.core.app.data.stream.platform.settings.StreamPlatformSettings;
import club.abelix.network.common.logging.Logging;
import gnu.trove.TCollections;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;
import lombok.val;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StreamManager {

    public static final StreamManager INSTANCE = new StreamManager();
    /**
     * Интервал оповещения игроков (в мс) о проходящих сейчас стримах
     */
    private static final int STREAM_BROADCAST_INTERVAL = 60_000 * 5;
    /**
     * Время задержки оповещения (в мс), если недавно был добавлен стрим
     */
    private static final int RECENTLY_ADDED_STREAM_COOLDOWN = 60_000;

    // ----------------------------------------------------------------
    /**
     * Время интервала между выходом игрока из сети и удалением стрима
     * как стрима оффлайн-игрока (в мс)
     * <p>
     * Проще говоря, даем возможность стримеру перезайти за минуту
     */
    private static final int AUTO_OFFLINE_STREAM_REMOVE_INTERVAL = 60_000;

    // ----------------------------------------------------------------
    /**
     * Доступные платформы для добавления стримов
     */
    private final List<StreamPlatform<?>> availableStreamPlatforms = new ArrayList<>() {{
        add(new YouTubeStreamPlatform(StreamPlatformSettings.YOUTUBE_API_KEY));
        add(new TwitchStreamPlatform(StreamPlatformSettings.TWITCH_CLIENT_ID));
    }};
    /**
     * Мапа с текущими стримами игроком
     */
    private final TIntObjectMap<AbstractStreamDetails> activeStreamMap
            = TCollections.synchronizedMap(new TIntObjectHashMap<>());

    public StreamManager() {
        final AtomicLong lastBroadcastTime = new AtomicLong(System.currentTimeMillis());

        TIntLongMap playerQuitTimeMap = new TIntLongHashMap();

        //запускаем поток для обновления всех стримов
        Async.runTimer(() -> {
            try {
                //костыль, чтобы избежать ConcurrentModificationException...
                TIntList playerIdsToRemove = new TIntArrayList();

                activeStreamMap.forEachEntry((playerId, streamDetails) -> {
                    Gamer gamer = Gamer.getGamer(playerId);

                    //удаляем стрим оффлайн-игрока
                    if (gamer == null) {
                        //тут делаем проще. есть время, когда игрок вышел. есть текущее время
                        //если времени прошло межу текущим временем и временем когда игрок вышел
                        //больше, чем AUTO_OFFLINE_STREAM_REMOVE_INTERVAL, то тогда удаляем. Иначе
                        //этого игрока не трогаем

                        if (!playerQuitTimeMap.containsKey(playerId)) {
                            playerQuitTimeMap.put(playerId, System.currentTimeMillis());
                        } else if (System.currentTimeMillis() - playerQuitTimeMap.get(playerId) >= AUTO_OFFLINE_STREAM_REMOVE_INTERVAL) {
                            playerIdsToRemove.add(playerId);
                            playerQuitTimeMap.remove(playerId);
                        }

                        return true;
                    }

                    //информацию обновлять пока не нужно
                    if (streamDetails.isActual() && !streamDetails.shouldUpdate()) {
                        return true;
                    }

                    StreamPlatform<?> platform = streamDetails.getStreamPlatform();

                    //делаем запрос на обновление
                    Object response = null;

                    try {
                        response = platform.makeRequest(streamDetails.getIdentity());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                    //чет пошло не так - удаляем стрим, он ведь больше не обновляется
                    if (response == null) {
                        playerIdsToRemove.add(playerId);
                        return true;
                    }

                    //меняем время последнего обновления
                    streamDetails.setLastUpdateTime(System.currentTimeMillis());

                    Language language = gamer.getLanguage();

                    //обновляем информацию о стриме
                    try {
                        //если до обновления инфа была не актуальной - это новый стрим
                        //поэтому будем оповещать о нем всех игроков
                        boolean wasUnActual = !streamDetails.isActual();

                        platform.updateStreamDetails(streamDetails, response);

                        if (wasUnActual) {
                            for (Gamer target : Gamer.getGamers()) {
                                Language targetLang = target.getLanguage();

                                // todo сделать 1 пакетом, а то 4*online пакетов чёт дохуя
                                target.sendMessage(" ");
                                target.sendLocalizedMessage("stream_new_stream_first_line",
                                        "%gamer%", gamer.getDisplayName(), "%platform%", platform.getDisplayName());
                                target.sendMessage(String.format(JSON.CLICK_URL_LAYOUT,
                                        targetLang.getMessage("stream_new_stream_second_line"),
                                        platform.makeBeautifulUrl(streamDetails)));
                                target.sendMessage(" ");
                            }
                        }
                    } catch (StreamException e) {
                        e.printStackTrace();

                        gamer.sendLocalizedMessage("stream_update_error",
                                "%exception%", e.getMessage(language));

                        if (streamDetails.isActual()) {
                            gamer.sendLocalizedMessage("stream_update_removed");
                        }

                        playerIdsToRemove.add(playerId);
                    }

                    return true;
                });

                //удаляем все не нужные нам стримы...
                playerIdsToRemove.forEach(playerId -> {
                    activeStreamMap.remove(playerId);
                    return true;
                });

                //каждые 5 минут сообщаем игрокам о том сколько стримов идет
                //для начала проверим есть ли они вообще
                if (activeStreamMap.isEmpty()) {
                    return;
                }

                long currentTimeMillis = System.currentTimeMillis();

                //не будем также оповещать игроков, если с момента добавления последнего стрима прошло менее 1 минуты
                for (val streamDetails : activeStreamMap.valueCollection()) {
                    if ((currentTimeMillis - RECENTLY_ADDED_STREAM_COOLDOWN) <= streamDetails.getStartedAtServerTime()) {
                        return;
                    }
                }

                //теперь проверим дату последней отправки инфы, если интервал прошел - оповещаем
                if (currentTimeMillis - lastBroadcastTime.get() >= STREAM_BROADCAST_INTERVAL) {
                    lastBroadcastTime.set(System.currentTimeMillis());

                    int streams = activeStreamMap.size();

                    for (Gamer gamer : Gamer.getGamers()) {
                        Language language = gamer.getLanguage();

                        gamer.sendLocalizedMessage("stream_online_streams",
                                "%stream_format%", CommonWords.PLAYER.get(language).withNumber(streams),
                                "%player_format%", language.getMessage("stream_" + (streams == 1 ? "single" : "plural")));
                    }
                }
            } catch (Exception exception) {
                Logging.IMP.info("Exception while updating stream info {}", exception.getMessage());
                exception.printStackTrace();
            }

        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Получить информацию об активном стриме игрока
     */
    public AbstractStreamDetails getActiveStream(@NonNull Gamer gamer) {
        return activeStreamMap.get(gamer.getPlayerId());
    }

    /**
     * Получить информацию о стрме по его ID
     */
    public AbstractStreamDetails getActiveStream(@NonNull String identity) {
        for (AbstractStreamDetails details : activeStreamMap.values(new AbstractStreamDetails[0])) {
            if (details.getIdentity().equals(identity)) {
                return details;
            }
        }

        return null;
    }

    /**
     * Добавить кулдаун на добавление стрима
     */
    public void addPlayerCooldown(Gamer gamer) {
        gamer.setCooldown("StreamRemoveCooldown", 5, TimeUnit.MINUTES);
    }

    /**
     * Проверить наличие кулдауна на добавление стрима у игрока
     */
    public boolean hasStreamCooldown(Gamer gamer) {
        return gamer.hasPermission("cooldown.bypass")
                ? false
                : gamer.getCooldown("StreamRemoveCooldown").isActive();
    }

    /**
     * Проверить наличие стрима
     */
    public boolean hasStream(Gamer gamer) {
        return activeStreamMap.containsKey(gamer.getPlayerId());
    }

    /**
     * Добавить информацию о новом стриме
     */
    public void addPlayerStream(@NonNull Gamer gamer, @NonNull AbstractStreamDetails details) {
        activeStreamMap.put(gamer.getPlayerId(), details);
    }

    /**
     * Удаляем информацию о стриме игрока
     */
    public void removePlayerStream(@NonNull Gamer gamer) {
        activeStreamMap.remove(gamer.getPlayerId());
    }

    /**
     * Получить информацию об активных стримах
     */
    public TIntObjectMap<AbstractStreamDetails> getActiveStreams() {
        return TCollections.unmodifiableMap(activeStreamMap);
    }

    /**
     * Получить доступные платформы для стримов
     */
    public List<StreamPlatform<?>> getAvailableStreamPlatforms() {
        return Collections.unmodifiableList(availableStreamPlatforms);
    }

}
