package club.abelix.api.server.game.setting;

/**
 * Игровые настройки по умолчанию (точнее их названия)
 * Удобно использовать, если например придется что-то
 * из этого менять
 */
public enum GameSetting {

    // ----------- Общие игровые настройки ----------- //

    /**
     * Включить применение предметов при старте
     */
    APPLY_ITEMS,

    /**
     * Фиксированное игровое время
     */
    FIXED_WORLD_TIME,

    /**
     * Автоматическое поджегание тнт
     */
    ENABLE_AUTO_TNT_IGNITION,

    /**
     * Кастомный взрыв тнт
     */
    TNT_CUSTOM_EXPLODE,
    /**
     * Радиус взрыва тнт
     */
    TNT_CUSTOM_EXPLODE_SEARCH_RADIUS,
    /**
     * Высота кастомного взрыва
     */
    TNT_CUSTOM_EXPLODE_HEIGHT,
    /**
     * Радиус кастомного взрыва тнт
     */
    TNT_CUSTOM_EXPLODE_RADIUS,
    /**
     * Бонус к множителю расстояния/максимального расстояния (не трогайте)
     */
    TNT_CUSTOM_EXPLODE_DISTANCE_MULTIPLIER_BONUS,
    /**
     * Насколько быстрее велосити уменьшается при увеличении расстояния
     */
    TNT_CUSTOM_EXPLODE_DISTANCE_DECREASEMENT_MULTIPLIER,
    /**
     * Урон от взрыва тнт
     */
    TNT_DAMAGE,
    TNT_ENABLE_DAMAGE,

    /**
     * Включить группы на режиме
     */
    ENABLE_GROUPS,

    /**
     * Разрешить кидать файрболы
     */
    ENABLE_FIREBALLS,

    /**
     * Скорость фаерболов
     */
    FIREBALLS_SPEED,

    /**
     * Сила взрыва
     */
    FIREBALLS_YIElD,

    /**
     * Кулдаун на фаерболы
     */
    FIREBALLS_COOLDOWN,

    /**
     * Урон от фаерболла
     */
    FIREBALLS_DAMAGE,

    /**
     * Действует на энтити в радиусе
     */
    FIREBALLS_CUSTOM_EXPLODE_SEARCH_RADIUS,

    /**
     * Изменять велосити при взрыве фаербола
     */
    FIREBALLS_CUSTOM_EXPLODE,
    /**
     * Высота кастомного взрыва (Не ебу как работает)
     */
    FIREBALLS_CUSTOM_EXPLODE_HEIGHT,
    /**
     * Радиус кастомного взрыва фаербола (Не ебу как работает)
     */
    FIREBALLS_CUSTOM_EXPLODE_RADIUS,
    /**
     * Бонус к множителю расстояния/максимального расстояния (не трогайте)
     */
    FIREBALLS_CUSTOM_EXPLODE_DISTANCE_MULTIPLIER_BONUS,
    /**
     * Насколько быстрее велосити уменьшается при увеличении расстояния
     */
    FIREBALLS_CUSTOM_EXPLODE_DISTANCE_DECREASEMENT_MULTIPLIER,

    FIREBALLS_ENABLE_DAMAGE,

    /**
     * Удаление пузырька после выпитого зелья
     */
    ENABLE_AUTO_REMOVE_GLASS_BOTTLES,

    /**
     * Включение зажигалки
     */
    ENABLE_FLINT_AND_STEEL,

    /**
     * Включение движения воды и лавы
     */
    ENABLE_WATER_LAVA_FLOW,

    /**
     * Автоматическое поджегание тнт
     */
    ENABLE_BEAUTIFUL_EXPLOSIONS,

    /**
     * Минимальный Y, которого нужно достигнуть игроку, чтобы тот умер
     */
    VOID_FALL_MINIMUM_Y,

    /**
     * Будет ли после игры выполняться тэски
     */
    ENABLE_END_TASKS,

    /**
     * Задержка между установкой состояния STARTING и стартом игры
     * Проще говоря, задержка до старта игры если на сервере уже
     * все слоты заполнены
     */
    START_DELAY,

    /**
     * Задержка между окончанием игры и телепортом игроков в лобби
     * По умолчанию 10 секунд
     */
    END_DELAY,

    /**
     * Минимальное количество игроков для старта игры
     */
    PLAYERS_TO_START,

    /**
     * Количество игроков в каждой команде
     */
    PLAYERS_PER_TEAM,

    /**
     * Максимальное количество слотов
     */
    MAX_SLOTS,

    /**
     * Разрешить ли следить за этой игрой или нет
     */
    ALLOW_GAME_SPECTATING,

    /**
     * Делать ли игрока наблюдателем сразу при входе на арену
     * <p>
     * Если стоит false - при входе на арену игрок наблюдателем не станет,
     * а плагину игры придется самому обработать событие входа
     */
    MAKE_PLAYER_SPECTATOR_AUTOMATICALLY,

    /**
     * Выключить ли отображение названия игрового мира во всех скорбордах
     */
    ENABLE_WORLD_NAME_DISPLAY,

    /**
     * Выключить ли отображение названия подрежима в скорборде ожидания
     */
    ENABLE_MINI_NAME_DISPLAY,

    /**
     * Минимальная высота по Y на которую можно строится
     * Default value = 0
     */
    MIN_Y_HEIGHT,

    /**
     * Максимальная высота по Y, на которую можно строится
     * Default value - 255
     */
    MAX_Y_HEIGHT,

    /**
     * Локация лобби в которую будут попадать все игроки после старта
     */
    LOBBY_LOCATION,

    /**
     * Локация центральной точки карты, куда будет вставляться лобби ожидания
     */
    MAP_CENTER_LOCATION,

    /**
     * Включать ли "упрощенное" зачарование которое само будет подставлять
     * лазурит в инвентарь зачарования предметов
     */
    ENABLE_SIMPLE_ENCHANTMENT_TABLE,

    // ----------- Настройки, которые касаются командной игры ----------- //

    /**
     * Включить ли командный эндер-сундук или нет
     */
    ENABLE_TEAM_SHARED_CHEST,

    /**
     * Разрешить ли изменение погоды
     */
    ALLOW_WEATHER_CHANGE,

    /**
     * Разрешить ли спавн мобов
     */
    ALLOW_MOB_SPAWN,

    /**
     * Разрешить ли игрокам ломать блоки
     */
    ALLOW_BLOCK_BREAK,

    /**
     * Разрешить ли игрокам ставить блоки
     */
    ALLOW_BLOCK_PLACE,

    /**
     * Разрешить ли изменение уровня голода
     */
    ALLOW_FOOD_CHANGE,

    /**
     * Разрешить ли получение урона игроком
     */
    ALLOW_PLAYER_DAMAGE,

    /**
     * Разрешить ли игрокам выбрасывать вещи
     */
    ALLOW_ITEM_DROP,

    /**
     * Разрешить ли игрокам поднимать вещи
     */
    ALLOW_ITEM_PICKUP,

    /**
     * Включить ли физику у блоков
     */
    ALLOW_BLOCK_PHYSICS,

    /**
     * Включить ли физику после конца игры
     */
    ALLOW_PHYSICS_ON_GAME_END,

    /**
     * Писать ли топ по убийствам в чат при конце игры
     */
    BROADCAST_KILLS_TOP_ON_END,

    /**
     * Включить ли командный чат среди игроков или нет
     */
    ENABLE_DEFAULT_TEAM_CHAT,

    /**
     * Включить ли взрывы блоков поставленных тиммейтами
     */
    ENABLE_TEAM_BLOCK_EXPLOSIONS,

    /**
     * Разрешено ли ПВП среди членов своей команды
     */
    ALLOW_FRIENDLY_FIRE,

    // ----------- Настройки, которые касаются смертей и прочего кала ----------- //

    /**
     * Показывать ли ХП игрока в чате другим при попадании из лука
     */
    SHOW_HP_ON_ARROW_HIT,

    /**
     * Должен ли игрок "умирать" при падении в бездну (т.е вызываться эвент смерти)
     */
    ENABLE_DIE_ON_VOID_FALL,

    /**
     * Показывать ли скорборд, когда игрока делает спектатором?
     */
    AUTO_SHOW_SPECTATOR_BOARD,

    /**
     * Включить автоматическое удаление лобби ожидания при старте игры
     */
    ENABLE_LOBBY_REMOVE,

    /**
     * Включить автоматическое изменение статуса очереди арены
     */
    AUTO_QUEUE_STATE_EDIT,

    /**
     * Разрешить игрокам, у которых включено автовход в очередь в неё входить
     */
    ALLOW_GAMER_AUTO_QUEUE_IF_ENABLE,

    /**
     * Разрешить после игры отправлять игроков с сервера
     */
    ALLOW_SEND_AFTER_GAME,

    /**
     * Класс борда ожидания
     */
    WAITING_BOARD_CLASS,

    DISABLE_WAITING_LOBBY_PASTING,

    /**
     * Воспроизводить звук при приземление эндер-жемчуга
     */
    ENDER_PEARL_SOUND_EFFECT,

    /**
     * Использовать игровую зону
     */
    ENABLE_PLAYABLE_ZONE,


    /**
     * При респавне игрок не будет получать дамаг
     */
    RESPAWN_PROTECTION_DAMAGE,

    /**
     * Время сколько игрок не будет получать дамага
     */
    RESPAWN_PROTECTION_DAMAGE_TIME,

    /**
     * Удалять ли игрока из тимы и сетать ли ему фулл спектатора, если оно ему поставилось?
     */
    REMOVE_SPECTATOR_FROM_GAME,

    /**
     * Использовать слежение по компасу, даже елси он не в руке
     */
    COMPASS_TRACK_WITHOUT_COMPASS,

    DELAY_TNT_BLOCK_BREAK,

    /**
     * Колличество HP у игрока
     */
    PLAYER_HP,

    /**
     * Скорость игрока
     */
    PLAYER_SPEED,

    /**
     * Время респавна игрока
     */
    PLAYER_RESPAWN_TIME,

    WAITING_HIDE_JOIN_MESSAGE,

    WAITING_HIDE_QUIT_MESSAGE,

    WAITING_HIDE_NAMETAGS;


    private final boolean clearOnReset;

    GameSetting() {
        this(true);
    }

    GameSetting(boolean clearOnReset) {
        this.clearOnReset = clearOnReset;
    }

    public boolean isClearOnReset() {
        return clearOnReset;
    }
}
