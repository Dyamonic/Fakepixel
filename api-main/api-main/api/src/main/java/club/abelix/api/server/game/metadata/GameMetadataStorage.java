package club.abelix.api.server.game.metadata;

import club.abelix.api.server.game.Game;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class GameMetadataStorage {

    private final Game game;

    /**
     * Метадата для блоков. HashMap
     */
    private final Map<Block, Map<String, Object>> blockMetadata = new HashMap<>();

    /**
     * Метадата для энтити. WeakMap
     */
    private final Map<Entity, Map<String, Object>> entityMetadata = new WeakHashMap<>();

    /**
     * Метадата игры
     */
    private final Map<String, Object> gameMetadata = new HashMap<>();

    public GameMetadataStorage(Game game) {
        this.game = game;
    }

    private Map<String, Object> getMetadata(Block block) {
        synchronized (blockMetadata) {
            return blockMetadata.computeIfAbsent(block, x -> new HashMap<>());
        }
    }

    private Optional<Map<String, Object>> getOptionalMetadata(Block block) {
        synchronized (blockMetadata) {
            return Optional.ofNullable(blockMetadata.get(block));
        }
    }

    private Map<String, Object> getMetadata(Entity entity) {
        synchronized (entityMetadata) {
            return entityMetadata.computeIfAbsent(entity, x -> new HashMap<>());
        }
    }

    private Optional<Map<String, Object>> getOptionalMetadata(Entity entity) {
        synchronized (entityMetadata) {
            return Optional.ofNullable(entityMetadata.get(entity));
        }
    }

    /**
     * Установить метадату блоку
     */
    public void set(Block block, String key, Object object) {
        getMetadata(block).put(key, object);
    }

    /**
     * Получить метадату блока
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Block block, String key) {
        return getOptionalMetadata(block).map(map -> (T) map.get(key)).orElse(null);
    }

    /**
     * Проверить, есть ли метадата (любая) у блока
     */
    public boolean has(Block block) {
        synchronized (blockMetadata) {
            return blockMetadata.containsKey(block);
        }
    }

    /**
     * Проверить, есть ли метадата (key) у блока
     */
    public boolean has(Block block, String key) {
        return getOptionalMetadata(block).map(map -> map.containsKey(key)).orElse(Boolean.FALSE);
    }

    /**
     * Удалить всю метадату у блока
     */
    public void remove(Block block) {
        synchronized (blockMetadata) {
            blockMetadata.remove(block);
        }
    }

    /**
     * Удалить метадату key у блока
     */
    public void remove(Block block, String key) {
        getOptionalMetadata(block).ifPresent(map -> {
            if (map.remove(key) != null && map.isEmpty()) {
                remove(block);
            }
        });
    }

    /**
     * Установить метадату энтити
     */
    public void set(Entity entity, String key, Object object) {
        getMetadata(entity).put(key, object);
    }

    /**
     * Проверить, есть ли метадата (key) у энтити
     */
    public boolean has(Entity entity, String key) {
        return getOptionalMetadata(entity).map(map -> map.containsKey(key)).orElse(Boolean.FALSE);
    }

    /**
     * Проверить, есть ли метадата (любая) у энтити
     */
    public boolean has(Entity entity) {
        synchronized (entityMetadata) {
            return entityMetadata.containsKey(entity);
        }
    }

    /**
     * Удалить всю метадату энтити
     */
    public void remove(Entity entity) {
        synchronized (entityMetadata) {
            entityMetadata.remove(entity);
        }
    }

    /**
     * Удалить метадату key у энтити
     */
    public void remove(Entity entity, String key) {
        getOptionalMetadata(entity).ifPresent(map -> {
            if (map.remove(key) != null && map.isEmpty()) {
                remove(entity);
            }
        });
    }

    /**
     * Получить метадату энтити
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Entity entity, String key) {
        return getOptionalMetadata(entity).map(map -> (T) map.get(key)).orElse(null);
    }

    /**
     * Установить метадату игре
     */
    public void set(String key, Object object) {
        gameMetadata.put(key, object);
    }

    /**
     * Получить метадату игры
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) gameMetadata.get(key);
    }

    /**
     * Проверить, есть ли метадата (key) у игры
     */
    public boolean has(String key) {
        return gameMetadata.containsKey(key);
    }

    /**
     * Удалить метадату у игры
     */
    public void remove(String key) {
        gameMetadata.remove(key);
    }

    /**
     * Очистить всю метадату
     */
    public void clear() {
        synchronized (blockMetadata) {
            blockMetadata.clear();
        }

        synchronized (entityMetadata) {
            entityMetadata.clear();
        }

        synchronized (gameMetadata) {
            gameMetadata.clear();
        }
    }

    @Override
    public String toString() {
        return "GameMetadataStorage[game=" + game + ']';
    }

    @Override
    public int hashCode() {
        return game.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof GameMetadataStorage)) return false;

        return game == ((GameMetadataStorage) o).game;
    }

}
